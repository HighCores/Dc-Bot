package com.highcore.bot.services;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.highcore.bot.config.Config;
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

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TicketService {
    private static final Logger log = LoggerFactory.getLogger(TicketService.class);
    private static final String TICKET_CAT_ID = "1488795130881249404";
    private static final String ADMIN_ROLE_ID = "1488795130767736853";
    public static final Map<String, JsonObject> ticketCache = new ConcurrentHashMap<>();

    public static void createTicket(IReplyCallback event, String subject, String priority, String type, String body) {
        Guild guild = event.getGuild();
        if (guild == null) return;
        Category cat = guild.getCategoryById(TICKET_CAT_ID);
        if (cat == null) cat = guild.getCategoriesByName("TICKETS", true).stream().findFirst().orElse(null);
        
        int num = SupabaseClient.getNextTicketNumber(type);
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
                SupabaseClient.saveTicketMeta(tid, meta);
                
                LogManager.logEmbed(guild, Config.LOG_TICKETS, 
                    EmbedUtil.createOldLogEmbed("ticket-create", 
                        "Action: Session Established\nCase: #" + tid + "\nType: " + type + "\nClient: " + user.getAsMention() + "\nChannel: " + ch.getAsMention(), 
                        guild.getMember(user), null, null, EmbedUtil.GOLD));

                ch.sendMessageComponents(rebuildWelcomeContainer(ticket, false, null, ch)).useComponentsV2(true).queue();
                event.reply("✅ Ticket created: " + ch.getAsMention()).setEphemeral(true).queue();
            });
    }

    public static void createHighEndOrderTicket(Guild guild, User user, String pName, String cName, String contact, String phone, String category, List<InvoiceService.OrderItem> mainItems, List<InvoiceService.OrderItem> addOnItems, String voucherCode, String eta) {
        Category cat = guild.getCategoryById(TICKET_CAT_ID);
        if (cat == null) cat = guild.getCategoriesByName("TICKETS", true).stream().findFirst().orElse(null);
        if (cat == null) return;

        int num = SupabaseClient.getNextTicketNumber("ORDER");
        String tid = String.format("%04d", num);
        String channelName = "order-" + tid;
        Member member = guild.getMember(user);
        if (member == null) return;
        
        List<InvoiceService.OrderItem> allItems = new ArrayList<>(mainItems);
        allItems.addAll(addOnItems);

        int globalDiscount = SupabaseClient.getGlobalDiscountPercentage();
        int vPercent = 0, vAmount = 0;
        if (voucherCode != null && !voucherCode.isBlank()) {
            JsonObject v = SupabaseClient.getVoucherByCode(voucherCode);
            if (v != null && v.get("user_id").getAsString().equals(user.getId()) && !v.get("is_used").getAsBoolean()) {
                String vt = v.has("type") ? v.get("type").getAsString() : "PERCENT";
                int va = v.has("amount") ? v.get("amount").getAsInt() : 0;
                if (vt.equalsIgnoreCase("PERCENT") || vt.toLowerCase().contains("discount")) vPercent = va; else vAmount = va;
                SupabaseClient.markVoucherAsUsed(voucherCode);
            }
        }

        final double subTotal = allItems.stream().mapToDouble(i -> i.price).sum();
        final int fPerc = Math.max(globalDiscount, vPercent);
        final double totalDisc = (subTotal * (fPerc/100.0)) + vAmount;

        guild.createTextChannel(channelName, cat)
            .addPermissionOverride(guild.getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL))
            .addPermissionOverride(member, EnumSet.of(Permission.VIEW_CHANNEL), null)
            .queue(channel -> {
                JsonArray itemsArr = new JsonArray();
                for (var i : allItems) { JsonObject o = new JsonObject(); o.addProperty("name", i.name); o.addProperty("price", i.price); itemsArr.add(o); }

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

                SupabaseClient.createTicket(tid, user.getId(), cName, channel.getId(), "ORDER", pName, "HIGH");
                SupabaseClient.saveTicketMeta(tid, meta);

                channel.sendMessageComponents(rebuildWelcomeContainer(ticket, false, null, channel)).useComponentsV2(true).queue();

                byte[] inv = InvoiceService.generateInvoice(tid, cName, pName, allItems, addOnItems, false, user.getEffectiveAvatarUrl(), user.getEffectiveName(), category, contact, totalDisc, phone);
                if (inv != null) {
                    channel.sendMessageComponents(EmbedUtil.containerBranded("\uD83D\uDCC3 Invoice \u2014 Payment Required", "", "Review your order and choose a payment method.", "attachment://invoice.png",
                        ActionRow.of(
                            Button.secondary("ticket_pay_binance_" + tid, "Binance"),
                            Button.secondary("ticket_pay_patreon_" + tid, "Patreon"),
                            Button.secondary("ticket_pay_cliq_" + tid, "CliQ"),
                            Button.secondary("ticket_pay_paypal_" + tid, "PayPal"),
                            Button.secondary("ticket_pay_friendi_" + tid, "Friendi Pay")
                        ),
                        ActionRow.of(
                            Button.secondary("ticket_pay_rajhi_" + tid, "Al Rajhi")
                        )))
                        .useComponentsV2(true).addFiles(FileUpload.fromData(inv, "invoice.png")).queue();
                }
            });
    }

    private static Container rebuildWelcomeContainer(JsonObject ticket, boolean claimed, Member staff, TextChannel ch) {
        JsonObject meta = ticket.getAsJsonObject("metadata");
        String tid = ticket.get("ticket_id").getAsString();
        String userId = ticket.get("user_id").getAsString();
        
        String name = ch.getName().toUpperCase();
        String type = "SUPPORT";
        if (name.startsWith("ORDER")) type = "ORDER";
        else if (name.startsWith("COMPLAINT")) type = "COMPLAINT";
        
        if ("ORDER".equalsIgnoreCase(type)) {
            return buildOrderPipelineContainer(ticket, claimed, staff);
        }

        StringBuilder b = new StringBuilder();
        b.append("<@&").append(com.highcore.bot.config.Config.ROLE_STAFF).append(">\n\n");
        b.append("Welcome <@").append(userId).append("> \uD83D\uDC4B\n\n");

        if (meta != null) {
            String prio = meta.has("priority") ? meta.get("priority").getAsString() : "HIGH";
            b.append("**Priority:** `").append(prio.toUpperCase()).append("` \u2022 ");
            
            if (meta.has("subject")) b.append("**Subject:** `").append(meta.get("subject").getAsString()).append("` \u2022 ");
            
            if (meta.has("type")) b.append("**Type:** `").append(meta.get("type").getAsString()).append("`\n\n");
            else b.append("\n\n");
            
            String details = "N/A";
            if (meta.has("body")) details = meta.get("body").getAsString();
            else if (meta.has("client_name")) details = meta.get("client_name").getAsString();
            else if (meta.has("target")) details = meta.get("target").getAsString();
            
            b.append("**Details:** `").append(details).append("`\n");
        }
        
        b.append("\nA staff member will be with you shortly \u2014 please describe your issue in full detail.");

        ActionRow row;
        if (!claimed) {
            row = ActionRow.of(
                Button.secondary("ticket_claim", "Claim Ticket"),
                Button.secondary("ticket_close", "Close Ticket")
            );
        } else {
            row = ActionRow.of(
                Button.secondary("ticket_unclaim", "Unclaim Ticket"),
                Button.secondary("ticket_close", "Close Ticket")
            );
        }

        String title = type.equalsIgnoreCase("COMPLAINT") ? "Complaint Board" : "Support Center";
        String banner = type.equalsIgnoreCase("COMPLAINT") ? EmbedUtil.BANNER_COMPLAINT : EmbedUtil.BANNER_SUPPORT;

        return EmbedUtil.containerBranded(title, "Case #" + tid, b.toString(), banner, row);
    }

    private static Container buildOrderPipelineContainer(JsonObject ticket, boolean claimed, Member staff) {
        JsonObject meta = ticket.getAsJsonObject("metadata");
        String tid = ticket.get("ticket_id").getAsString();
        String userId = ticket.get("user_id").getAsString();
        
        StringBuilder b = new StringBuilder();
        b.append("Welcome <@").append(userId).append("> \uD83D\uDC4B\n\n");
        
        String prio = "HIGH";
        if (meta != null && meta.has("priority")) prio = meta.get("priority").getAsString();
        b.append("**Priority:** `").append(prio.toUpperCase()).append("` \u2022 ");
        
        String subject = "gh";
        if (meta != null && meta.has("project_name")) subject = meta.get("project_name").getAsString();
        b.append("**Subject:** `").append(subject).append("`\n\n");
        
        b.append("\uD83D\uDCCB **Questionnaire Data**\n");
        if (meta != null) {
            b.append("**Project:** `").append(meta.get("project_name").getAsString()).append("` \u2022 ");
            b.append("**Client:** `").append(meta.get("client_name").getAsString()).append("` \u2022 ");
            b.append("**Contact:** `").append(meta.has("contact") ? meta.get("contact").getAsString() : "N/A").append("` \u2022 ");
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
        b.append("\u26A0\uFE0F **Your ticket is locked** \u2014 it will be unlocked once payment is confirmed.");

        ActionRow row;
        if (!claimed) {
            row = ActionRow.of(
                Button.secondary("ticket_verify", "Verify Payment"),
                Button.secondary("ticket_claim", "Claim Ticket"),
                Button.secondary("ticket_close", "Close Ticket")
            );
        } else {
            row = ActionRow.of(
                Button.secondary("ticket_verify", "Verify Payment"),
                Button.secondary("ticket_unclaim", "Unclaim Ticket"),
                Button.secondary("ticket_close", "Close Ticket")
            );
        }

        return EmbedUtil.containerBranded("Order Pipeline", "Case #" + tid, b.toString(), EmbedUtil.BANNER_ORDER_TICKET, row);
    }

    public static void claimTicket(TextChannel ch, Member member, ButtonInteractionEvent event) {
        JsonObject ticket = ticketCache.get(ch.getId());
        if (ticket == null) ticket = SupabaseClient.getTicketAndMetaByChannel(ch.getId());
        if (ticket == null) { event.reply("Session data missing.").setEphemeral(true).queue(); return; }
        
        SupabaseClient.claimTicket(ticket.get("ticket_id").getAsString(), member.getEffectiveName());
        
        LogManager.logEmbed(ch.getGuild(), Config.LOG_TICKETS, 
            EmbedUtil.createOldLogEmbed("ticket-claim", 
                "Action: Agent Assignment\nCase: #" + ticket.get("ticket_id").getAsString() + "\nAgent: " + member.getAsMention() + "\nChannel: " + ch.getAsMention(), 
                member, null, null, EmbedUtil.SUCCESS));

        // Update the original message buttons immediately
        event.editMessage(new MessageEditBuilder()
            .setComponents(List.of(rebuildWelcomeContainer(ticket, true, member, ch)))
            .useComponentsV2(true)
            .build()).queue();
        
        // Send separate notification
        ch.sendMessageComponents(EmbedUtil.brandedNotice("▶ NOTICE • Claimed", "\uD83D\uDCE1 Ticket Handled By: " + member.getAsMention()))
          .useComponentsV2(true).queue();
    }

    public static void unclaimTicket(TextChannel ch, Member member, ButtonInteractionEvent event) {
        JsonObject ticket = ticketCache.get(ch.getId());
        if (ticket == null) ticket = SupabaseClient.getTicketAndMetaByChannel(ch.getId());
        if (ticket == null) { event.reply("Session data missing.").setEphemeral(true).queue(); return; }

        SupabaseClient.unclaimTicket(ticket.get("ticket_id").getAsString());

        LogManager.logEmbed(ch.getGuild(), Config.LOG_TICKETS, 
            EmbedUtil.createOldLogEmbed("ticket-unclaim", 
                "Action: Agent Withdrawal\nCase: #" + ticket.get("ticket_id").getAsString() + "\nAgent: " + member.getAsMention() + "\nChannel: " + ch.getAsMention(), 
                member, null, null, EmbedUtil.WARNING));

        // Update the original message buttons immediately
        event.editMessage(new MessageEditBuilder()
            .setComponents(List.of(rebuildWelcomeContainer(ticket, false, null, ch)))
            .useComponentsV2(true)
            .build()).queue();

        // Send separate notification
        ch.sendMessageComponents(EmbedUtil.brandedNotice("▶ NOTICE • Unclaimed", "\u2935\uFE0F Ticket Unclaimed By: " + member.getAsMention()))
          .useComponentsV2(true).queue();
    }

    // Metadata is now managed via database lookups rather than channel topics.

    public static void closeTicket(ButtonInteractionEvent event, Member member) {
        event.deferEdit().queue();
        closeTicketInternal(event.getChannel().asTextChannel(), member);
    }

    private static void closeTicketInternal(TextChannel ch, Member member) {
        String tid = ch.getName().split("-")[ch.getName().split("-").length - 1];
        JsonObject ticket = SupabaseClient.getTicketAndMetaByChannel(ch.getId());
        if (ticket == null) return;
        
        String userId = ticket.get("user_id").getAsString();
        Member client = ch.getGuild().getMemberById(userId);

        // 1. Remove client access
        if (client != null) {
            ch.getManager().putMemberPermissionOverride(client.getIdLong(), null, EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND)).queue();
        } else {
            ch.getManager().putPermissionOverride(ch.getGuild().getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL)).queue();
        }

        // 2. Update DB status
        SupabaseClient.updateTicketStatus(tid, "closed", member.getEffectiveName());

        LogManager.logEmbed(ch.getGuild(), Config.LOG_TICKETS, 
            EmbedUtil.createOldLogEmbed("ticket-close", 
                "Action: Session Termination\nCase: #" + tid + "\nAgent: " + member.getAsMention() + "\nChannel: " + ch.getAsMention(), 
                member, null, null, EmbedUtil.DANGER));

        // 3. Send Control Panel
        ch.sendMessageComponents(EmbedUtil.containerBranded("ARCHIVES", "Control Panel",
            "### TICKET CLOSED\nAgent **" + member.getEffectiveName() + "** has closed this ticket.\n\nSelect an action below.", EmbedUtil.BANNER_SUPPORT,
            ActionRow.of(
                Button.secondary("ticket_reopen", "Reopen"),
                Button.secondary("ticket_transcript", "Transcript"),
                Button.secondary("ticket_delete_init", "Delete")
            )))
            .useComponentsV2(true).queue();
    }

    public static void reopenTicket(TextChannel ch, Member member, ButtonInteractionEvent event) {
        String tid = ch.getName().split("-")[ch.getName().split("-").length - 1];
        JsonObject ticket = SupabaseClient.getTicketAndMetaByChannel(ch.getId());
        if (ticket == null) { event.reply("Session data missing.").setEphemeral(true).queue(); return; }

        String userId = ticket.get("user_id").getAsString();

        // 1. Restore client access
        ch.getManager().putMemberPermissionOverride(Long.parseLong(userId), EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND), null).queue();

        // 2. Update DB status
        SupabaseClient.updateTicketStatus(tid, "open", null);

        LogManager.logEmbed(ch.getGuild(), Config.LOG_TICKETS, 
            EmbedUtil.createOldLogEmbed("ticket-reopen", 
                "Action: Session Restoration\nCase: #" + tid + "\nAgent: " + member.getAsMention() + "\nChannel: " + ch.getAsMention(), 
                member, null, null, EmbedUtil.INFO));

        event.reply("✅ Ticket reopened. Access restored.").queue();
    }

    public static void requestDeleteConfirmation(ButtonInteractionEvent event) {
        event.replyComponents(EmbedUtil.containerBranded("DANGER", "Delete Channel",
            "### Are you sure?\nThis action will permanently delete this channel and cannot be undone.", EmbedUtil.BANNER_SUPPORT,
            ActionRow.of(
                Button.secondary("ticket_delete_final", "Confirm Delete"),
                Button.secondary("ticket_delete_cancel", "Cancel")
            ))).setEphemeral(true).useComponentsV2(true).queue();
    }

    public static void transcriptTicket(TextChannel ch, Member member, ButtonInteractionEvent event) {
        String tid = ch.getName().split("-")[ch.getName().split("-").length - 1];
        JsonObject ticket = SupabaseClient.getTicketAndMetaByChannel(ch.getId());
        if (ticket == null) { event.reply("Session data missing.").setEphemeral(true).queue(); return; }
        
        JsonArray msgs = SupabaseClient.getTicketMessages(tid);
        
        // Try to find opened_at or fallback
        String openedAt = ticket.has("created_at") ? ticket.get("created_at").getAsString() : Instant.now().toString();
        String opener = ticket.has("user_name") ? ticket.get("user_name").getAsString() : "Unknown";
        String type = ch.getName().toUpperCase().split("-")[0];
        
        byte[] html = TranscriptService.buildHtml(tid, ch.getName(), type, "closed", openedAt, opener, 
            ticket.has("claimed_by") && !ticket.get("claimed_by").isJsonNull() ? ticket.get("claimed_by").getAsString() : "None",
            member.getEffectiveName(), msgs);
            
        TextChannel logCh = ch.getGuild().getTextChannelById("1488795131019526147");
        if (logCh != null) {
            String userId = ticket.get("user_id").getAsString();
            String url = "https://high-core-dc-bot-production.up.railway.app/view/transcript/" + tid;
            String logBody = "▶ **TRANSCRIPT • Archive \u2014 Case #" + tid + "**\n\n" +
                             "**Opener:** <@" + userId + "> (" + opener + ")\n" +
                             "**Closed By:** " + member.getAsMention();
            
            logCh.sendMessage(logBody)
                .setComponents(ActionRow.of(Button.link(url, "View Web Transcript").withEmoji(Emoji.fromUnicode("\uD83D\uDCDC"))))
                .addFiles(net.dv8tion.jda.api.utils.FileUpload.fromData(html, "transcript-" + tid + ".html"))
                .queue();
            event.reply("✅ Transcript has been uploaded to the management sector.").setEphemeral(true).queue();
        } else {
            event.reply("❌ Management sector not found.").setEphemeral(true).queue();
        }
    }

    public static void deleteTicket(TextChannel ch) {
        ch.delete().queue();
    }

    public static void markAsPaid(TextChannel ch, String tid, Member member) {
        SupabaseClient.logStat("PAYMENT", member.getId(), "Ticket #" + tid + " marked paid");
    }

    public static void finalizeClose(TextChannel ch, Member m, String status) {
        closeTicketInternal(ch, m);
    }
}
