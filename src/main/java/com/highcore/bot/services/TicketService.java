package com.highcore.bot.services;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.highcore.bot.database.SupabaseClient;
import com.highcore.bot.utils.EmbedUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.utils.FileUpload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.components.mediagallery.MediaGallery;
import net.dv8tion.jda.api.components.mediagallery.MediaGalleryItem;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.separator.Separator.Spacing;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;
import net.dv8tion.jda.api.entities.emoji.Emoji;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TicketService {
    private static final Logger log = LoggerFactory.getLogger(TicketService.class);
    private static final String TICKET_CAT_ID = "1346083363328495676";
    private static final String ADMIN_ROLE_ID = "1488795130767736853";
    public static final Map<String, JsonObject> ticketCache = new ConcurrentHashMap<>();

    public static void createTicket(IReplyCallback event, String subject, String priority, String type, String body) {
        Guild guild = event.getGuild();
        if (guild == null) return;
        Category cat = guild.getCategoryById(TICKET_CAT_ID);
        if (cat == null) cat = guild.getCategoriesByName("TICKETS", true).stream().findFirst().orElse(null);
        
        int num = SupabaseClient.getNextTicketNumber();
        String tid = String.format("%04d", num);
        String channelName = type.toLowerCase() + "-" + tid;
        User user = event.getUser();
        
        guild.createTextChannel(channelName, cat)
            .addPermissionOverride(guild.getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL))
            .addPermissionOverride(guild.getMember(user), EnumSet.of(Permission.VIEW_CHANNEL), null)
            .queue(ch -> {
                JsonObject ticket = new JsonObject();
                ticket.addProperty("ticket_id", tid);
                ticket.addProperty("user_id", user.getId());
                ticket.addProperty("channel_id", ch.getId());
                ticket.addProperty("type", type);
                
                JsonObject meta = new JsonObject();
                meta.addProperty("category", type);
                meta.addProperty("subject", subject);
                meta.addProperty("body", body);
                meta.addProperty("priority", priority);
                ticket.add("metadata", meta);
                ticketCache.put(ch.getId(), ticket);

                SupabaseClient.createTicket(tid, user.getId(), user.getName(), ch.getId(), type, subject, priority);
                
                ch.sendMessageComponents(rebuildWelcomeContainer(ticket, false, null)).useComponentsV2(true).queue();
                event.reply("✅ Ticket created: " + ch.getAsMention()).setEphemeral(true).queue();
            });
    }

    public static void createHighEndOrderTicket(Guild guild, User user, String pName, String cName, String contact, String phone, String category, List<InvoiceService.OrderItem> items, String voucherCode, String eta) {
        Category cat = guild.getCategoryById(TICKET_CAT_ID);
        if (cat == null) cat = guild.getCategoriesByName("TICKETS", true).stream().findFirst().orElse(null);
        if (cat == null) return;

        int num = SupabaseClient.getNextTicketNumber();
        String tid = String.format("%04d", num);
        String channelName = "order-" + tid;
        Member member = guild.getMember(user);
        if (member == null) return;
        
        int globalDiscount = SupabaseClient.getGlobalDiscountPercentage();
        int vPercent = 0, vAmount = 0;
        if (voucherCode != null && !voucherCode.isBlank()) {
            JsonObject v = SupabaseClient.getVoucherByCode(voucherCode);
            if (v != null && v.get("user_id").getAsString().equals(user.getId()) && !v.get("is_used").getAsBoolean()) {
                String vt = v.has("type") ? v.get("type").getAsString() : "PERCENT";
                int va = v.has("amount") ? v.get("amount").getAsInt() : 0;
                if (vt.equalsIgnoreCase("PERCENT")) vPercent = va; else vAmount = va;
                SupabaseClient.markVoucherAsUsed(voucherCode);
            }
        }

        final double subTotal = items.stream().mapToDouble(i -> i.price).sum();
        final int fPerc = Math.max(globalDiscount, vPercent);
        final double totalDisc = (subTotal * (fPerc/100.0)) + vAmount;

        guild.createTextChannel(channelName, cat)
            .addPermissionOverride(guild.getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL))
            .addPermissionOverride(member, EnumSet.of(Permission.VIEW_CHANNEL), null)
            .queue(channel -> {
                JsonArray itemsArr = new JsonArray();
                for (var i : items) { JsonObject o = new JsonObject(); o.addProperty("name", i.name); o.addProperty("price", i.price); itemsArr.add(o); }

                JsonObject meta = new JsonObject();
                meta.addProperty("client_name", cName);
                meta.addProperty("project_name", pName);
                meta.addProperty("category", category);
                meta.addProperty("contact", contact);
                meta.addProperty("phone", phone);
                meta.addProperty("eta", eta);
                meta.addProperty("total_discount", totalDisc);
                meta.add("items", itemsArr);
                
                JsonObject ticket = new JsonObject();
                ticket.addProperty("ticket_id", tid);
                ticket.addProperty("user_id", user.getId());
                ticket.addProperty("channel_id", channel.getId());
                ticket.addProperty("type", "ORDER");
                ticket.add("metadata", meta);
                ticketCache.put(channel.getId(), ticket);

                channel.getManager().setTopic("||META:" + meta.toString()).queue();
                SupabaseClient.createTicket(tid, user.getId(), cName, channel.getId(), "ORDER", pName, "HIGH");

                channel.sendMessageComponents(rebuildWelcomeContainer(ticket, false, null)).useComponentsV2(true).queue();

                byte[] inv = InvoiceService.generateInvoice(tid, cName, pName, items, false, user.getEffectiveAvatarUrl(), user.getEffectiveName(), category, contact, totalDisc, phone);
                if (inv != null) {
                    channel.sendMessageComponents(EmbedUtil.containerBranded("\uD83D\uDCC3 Invoice \u2014 Payment Required", "", "Review your order and choose a payment method.", "attachment://invoice.png",
                        ActionRow.of(
                            Button.secondary("ticket_pay_paypal_" + tid, "PayPal").withEmoji(Emoji.fromUnicode("\uD83D\uDCB3")),
                            Button.secondary("ticket_pay_stripe_" + tid, "Stripe").withEmoji(Emoji.fromUnicode("\uD83C\uDF10")),
                            Button.secondary("ticket_pay_bank_" + tid, "Bank Transfer").withEmoji(Emoji.fromUnicode("\uD83C\uDFE6")),
                            Button.secondary("ticket_pay_usdt_" + tid, "USDT").withEmoji(Emoji.fromUnicode("\uD83D\uDCB0")),
                            Button.secondary("ticket_pay_stc_" + tid, "STC Pay").withEmoji(Emoji.fromUnicode("\uD83D\uDCD8"))
                        )))
                        .useComponentsV2(true).addFiles(FileUpload.fromData(inv, "invoice.png")).queue();
                }
            });
    }

    private static Container rebuildWelcomeContainer(JsonObject ticket, boolean claimed, Member staff) {
        JsonObject meta = ticket.getAsJsonObject("metadata");
        String tid = ticket.get("ticket_id").getAsString();
        String type = ticket.get("type").getAsString();
        String cat = (meta != null && meta.has("category")) ? meta.get("category").getAsString() : "Support";
        
        if ("ORDER".equalsIgnoreCase(type)) {
            return buildOrderPipelineContainer(ticket, claimed, staff);
        }

        StringBuilder desc = new StringBuilder();
        desc.append("### \uD83D\uDCE9 SESSION DETAILS\n");
        if (meta != null) {
            if (meta.has("project_name")) desc.append("**Project:** ").append(meta.get("project_name").getAsString()).append("\n");
            if (meta.has("subject")) desc.append("**Subject:** ").append(meta.get("subject").getAsString()).append("\n");
            if (meta.has("client_name")) desc.append("**Client:** ").append(meta.get("client_name").getAsString()).append("\n");
            if (meta.has("body")) desc.append("**Details:** ").append(meta.get("body").getAsString()).append("\n");
            if (meta.has("priority")) desc.append("**Priority:** ").append(meta.get("priority").getAsString()).append("\n");
        }
        desc.append("\n---\n\n");
        desc.append("\uD83D\uDCC1 **Status:** ").append(claimed ? "Active / In Progress" : "Awaiting Response").append("\n");
        desc.append("\uD83D\uDC82 **Agent:** ").append(claimed && staff != null ? staff.getAsMention() : "Unassigned / <@&" + ADMIN_ROLE_ID + ">");

        ActionRow row;
        if (!claimed) {
            row = ActionRow.of(
                Button.primary("ticket_claim", "Claim Project").withEmoji(Emoji.fromUnicode("\uD83D\uDCE1")),
                Button.danger("ticket_close", "Archive Session").withEmoji(Emoji.fromUnicode("\uD83D\uDD12"))
            );
        } else {
            row = ActionRow.of(
                Button.secondary("ticket_unclaim", "Release Project").withEmoji(Emoji.fromUnicode("\u2935\uFE0F")),
                Button.danger("ticket_close", "Archive Session").withEmoji(Emoji.fromUnicode("\uD83D\uDD12"))
            );
        }

        return EmbedUtil.containerBranded(cat.toUpperCase(), "Case #" + tid, desc.toString(), EmbedUtil.getCategoryBanner(cat), row);
    }

    private static Container buildOrderPipelineContainer(JsonObject ticket, boolean claimed, Member staff) {
        JsonObject meta = ticket.getAsJsonObject("metadata");
        String tid = ticket.get("ticket_id").getAsString();
        String userId = ticket.get("user_id").getAsString();
        
        StringBuilder b = new StringBuilder();
        b.append("@Highcore Team\n\n");
        b.append("Welcome <@").append(userId).append("> \uD83D\uDC4B\n\n");
        
        String prio = "HIGH";
        if (meta != null && meta.has("priority")) prio = meta.get("priority").getAsString();
        b.append("**Priority:** `").append(prio.toUpperCase()).append("`\n");
        
        String subject = "gh";
        if (meta != null && meta.has("project_name")) subject = meta.get("project_name").getAsString();
        b.append("**Subject:** `").append(subject).append("`\n\n");
        
        b.append("\uD83D\uDCCB **Questionnaire Data**\n");
        if (meta != null) {
            b.append("**Project:** `").append(meta.get("project_name").getAsString()).append("`\n");
            b.append("**Client:** `").append(meta.get("client_name").getAsString()).append("`\n");
            b.append("**Contact:** `").append(meta.has("contact") ? meta.get("contact").getAsString() : "N/A").append("`\n");
            b.append("**ETA:** `").append(meta.has("eta") ? meta.get("eta").getAsString() : "N/A").append("`\n");
            
            if (meta.has("items")) {
                StringBuilder svs = new StringBuilder();
                JsonArray items = meta.getAsJsonArray("items");
                for (int i=0; i<items.size(); i++) {
                    svs.append(items.get(i).getAsJsonObject().get("name").getAsString());
                    if (i < items.size()-1) svs.append(", ");
                }
                b.append("**Services Requested:** ").append(svs.toString()).append("\n");
            }
        }
        b.append("\n---\n");
        b.append("\u26A0\uFE0F **Your ticket is locked** \u2014 it will be unlocked once payment is confirmed.");

        ActionRow row;
        if (!claimed) {
            row = ActionRow.of(
                Button.success("ticket_verify", "Verify Payment").withEmoji(Emoji.fromUnicode("\u2705")),
                Button.primary("ticket_claim", "Claim Ticket").withEmoji(Emoji.fromUnicode("\uD83D\uDCE1")),
                Button.danger("ticket_close", "Close Ticket").withEmoji(Emoji.fromUnicode("\uD83D\uDD12"))
            );
        } else {
            row = ActionRow.of(
                Button.success("ticket_verify", "Verify Payment").withEmoji(Emoji.fromUnicode("\u2705")),
                Button.secondary("ticket_unclaim", "Unclaim Ticket").withEmoji(Emoji.fromUnicode("\u2935\uFE0F")),
                Button.danger("ticket_close", "Close Ticket").withEmoji(Emoji.fromUnicode("\uD83D\uDD12"))
            );
        }

        return EmbedUtil.containerBranded("Order Pipeline", "Case #" + tid, b.toString(), "https://cdn.discordapp.com/attachments/1488900668042510568/1495893318217764884/Order.jpg", row);
    }

    public static void claimTicket(TextChannel ch, Member member, ButtonInteractionEvent event) {
        JsonObject ticket = ticketCache.get(ch.getId());
        if (ticket == null) ticket = resolveFromTopic(ch);
        if (ticket == null) { event.reply("Session data missing.").setEphemeral(true).queue(); return; }
        
        SupabaseClient.claimTicket(ticket.get("ticket_id").getAsString(), member.getEffectiveName());
        event.deferEdit().queue();
        event.getHook().editOriginal(new MessageEditBuilder().setComponents(rebuildWelcomeContainer(ticket, true, member)).build()).queue();
        
        Container notice = EmbedUtil.containerBranded("\u25B6\uFE0F NOTICE \u2022 Claimed", "", "\uD83D\uDCE1 **Ticket Handled By:** " + member.getAsMention(), null);
        event.getHook().sendMessageComponents(notice).useComponentsV2(true).queue();
    }

    public static void unclaimTicket(TextChannel ch, Member member, ButtonInteractionEvent event) {
        JsonObject ticket = ticketCache.get(ch.getId());
        if (ticket == null) ticket = resolveFromTopic(ch);
        if (ticket == null) { event.reply("Session data missing.").setEphemeral(true).queue(); return; }

        SupabaseClient.unclaimTicket(ticket.get("ticket_id").getAsString());
        event.deferEdit().queue();
        event.getHook().editOriginal(new MessageEditBuilder().setComponents(rebuildWelcomeContainer(ticket, false, null)).build()).queue();
        
        Container notice = EmbedUtil.containerBranded("\u25B6\uFE0F NOTICE \u2022 Unclaimed", "", "\u2935\uFE0F **Ticket Unclaimed By:** " + member.getAsMention(), null);
        event.getHook().sendMessageComponents(notice).useComponentsV2(true).queue();
    }

    private static JsonObject resolveFromTopic(TextChannel ch) {
        String topic = ch.getTopic();
        if (topic != null && topic.startsWith("||META:")) {
            JsonObject ticket = new JsonObject();
            String name = ch.getName();
            String tid = name.substring(name.lastIndexOf("-") + 1);
            ticket.addProperty("ticket_id", tid);
            try {
                ticket.add("metadata", com.google.gson.JsonParser.parseString(topic.replace("||META:", "")));
                return ticket;
            } catch (Exception e) { return null; }
        }
        return null;
    }

    public static void closeTicket(TextChannel ch, Member member) {
        String tid = ch.getName().split("-")[ch.getName().split("-").length - 1];
        SupabaseClient.updateTicketStatus(tid, "closed", member.getEffectiveName());
        ch.sendMessage("⚠️ Transmission ending. Archiving sector...").queue(m -> ch.delete().queueAfter(5, java.util.concurrent.TimeUnit.SECONDS));
    }

    public static void markAsPaid(TextChannel ch, String tid, Member member) {
        Container notice = EmbedUtil.containerBranded("\u2705 Payment Verified \u2022 Ticket Unlocked", "", "", null);
        ch.sendMessageComponents(notice).useComponentsV2(true).queue();
        SupabaseClient.logStat("PAYMENT", member.getId(), "Ticket #" + tid + " marked paid");
    }

    public static void finalizeClose(TextChannel ch, Member m, String status) {
        closeTicket(ch, m);
    }
}
