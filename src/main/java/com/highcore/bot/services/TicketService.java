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
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
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

    public static void createTicket(IReplyCallback event, String subject, String type, String body) {
        Guild guild = event.getGuild();
        if (guild == null)
            return;
        Category cat = guild.getCategoryById(TICKET_CAT_ID);
        if (cat == null)
            cat = guild.getCategoriesByName("TICKETS", true).stream().findFirst().orElse(null);

        int num = SupabaseClient.getNextTicketNumber(type);
        String tid = String.format("%04d", num);
        String channelName = type.toLowerCase() + "-" + tid;
        User user = event.getUser();

        guild.createTextChannel(channelName, cat)
                .addPermissionOverride(guild.getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL))
                .addPermissionOverride(guild.getMember(user), EnumSet.of(Permission.VIEW_CHANNEL), null)
                .queue(ch -> {
                    SupabaseClient.createTicket(tid, user.getId(), user.getName(), ch.getId(), type, subject);

                    JsonObject ticket = new JsonObject();
                    ticket.addProperty("ticket_id", tid);
                    ticket.addProperty("user_id", user.getId());
                    ticket.addProperty("channel_id", ch.getId());
                    ticket.addProperty("type", type);

                    JsonObject meta = new JsonObject();
                    meta.addProperty("category", type);
                    meta.addProperty("subject", subject);
                    meta.addProperty("body", body);
                    meta.addProperty("body", body);
                    ticket.add("metadata", meta);
                    ticketCache.put(ch.getId(), ticket);

                    SupabaseClient.saveTicketMeta(tid, meta);

                    LogManager.logEmbed(guild, Config.LOG_TICKETS,
                            EmbedUtil.createOldLogEmbed("ticket-create",
                                    "Action: Ticket Created\nCase: #" + tid + "\nType: " + type + "\nClient: "
                                            + user.getAsMention() + "\nChannel: " + ch.getAsMention(),
                                    guild.getMember(user), null, null, EmbedUtil.GOLD));

                    ch.sendMessageComponents(rebuildWelcomeContainer(ticket, false, null, ch)).useComponentsV2(true)
                            .queue();
                    event.reply("✅ Ticket created: " + ch.getAsMention()).setEphemeral(true).queue();
                });
    }

    public static void createHighEndOrderTicket(Guild guild, User user, String pName, String cName, String contact,
            String phone, String category, List<InvoiceService.OrderItem> mainItems,
            List<InvoiceService.OrderItem> addOnItems, String voucherCode, String eta) {
        Category cat = guild.getCategoryById(TICKET_CAT_ID);
        if (cat == null)
            cat = guild.getCategoriesByName("TICKETS", true).stream().findFirst().orElse(null);
        if (cat == null)
            return;

        int num = SupabaseClient.getNextTicketNumber("ORDER");
        String tid = String.format("%04d", num);
        String channelName = "order-" + tid;
        Member member = guild.getMember(user);
        if (member == null)
            return;

        List<InvoiceService.OrderItem> allItems = new ArrayList<>(mainItems);
        allItems.addAll(addOnItems);

        int globalDiscount = SupabaseClient.getGlobalDiscountPercentage();
        int vPercent = 0, vAmount = 0;
        if (voucherCode != null && !voucherCode.isBlank()) {
            String cleanV = voucherCode.trim().toUpperCase();
            JsonObject v = SupabaseClient.getVoucherByCode(cleanV);
            if (v != null) {
                String vUserId = v.has("user_id") && !v.get("user_id").isJsonNull() ? v.get("user_id").getAsString()
                        : "";
                boolean isUsed = v.has("is_used") && v.get("is_used").getAsBoolean();
                boolean userMatches = vUserId.isEmpty() || vUserId.equalsIgnoreCase("GLOBAL")
                        || vUserId.equals(user.getId());

                if (userMatches && !isUsed) {
                    String vt = v.has("type") ? v.get("type").getAsString() : "PERCENT";
                    int va = 0;
                    if (v.has("percentage"))
                        va = v.get("percentage").getAsInt();
                    else if (v.has("amount"))
                        va = v.get("amount").getAsInt();

                    if (vt.equalsIgnoreCase("PERCENT") || vt.toLowerCase().contains("discount")) {
                        vPercent = va;
                    } else {
                        vAmount = va;
                    }
                    log.info("[VOUCHER SUCCESS] Code {} applied: {}% / ${}", cleanV, vPercent, vAmount);
                } else {
                    log.warn("[VOUCHER REJECT D] Code {}: userMatches={}, isUsed={}", cleanV, userMatches, isUsed);
                }
            } else {
                log.warn("[VOUCHER NOT FOUND] Code: {}", cleanV);
            }
        }

        final double subTotal = allItems.stream().mapToDouble(i -> i.price).sum();
        final int fPerc = Math.max(globalDiscount, vPercent);
        final double totalDisc = (subTotal * (fPerc / 100.0)) + vAmount;

        final double finalVAmount = vAmount;
        final int finalVPercent = vPercent;

        guild.createTextChannel(channelName, cat)
                .addPermissionOverride(guild.getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL))
                .addPermissionOverride(member, EnumSet.of(Permission.VIEW_CHANNEL), null)
                .queue(channel -> {
                    log.info("[TICKET DEBUG] Channel queue callback started for: {}", channel.getName());
                    try {
                        JsonArray itemsArr = new JsonArray();
                        for (var i : allItems) {
                            JsonObject o = new JsonObject();
                            o.addProperty("name", i.name);
                            o.addProperty("price", i.price);
                            itemsArr.add(o);
                        }

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

                        SupabaseClient.createTicket(tid, user.getId(), cName, channel.getId(), "ORDER", pName);
                        SupabaseClient.saveTicketMeta(tid, meta);

                        if (voucherCode != null && !voucherCode.isBlank()) {
                            String cleanCode = voucherCode.trim().toUpperCase();
                            log.info("Marking voucher as used: {}", cleanCode);
                            SupabaseClient.markVoucherAsUsed(cleanCode);
                        }

                        channel.sendMessageComponents(rebuildWelcomeContainer(ticket, false, null, channel))
                                .useComponentsV2(true).queue();
                    } catch (Exception e) {
                        log.error("[FATAL TICKET ERROR] Error in ticket creation callback", e);
                    }

                    double voucherDeduction = finalVAmount + (subTotal * (finalVPercent / 100.0));
                    byte[] inv = InvoiceService.generateInvoice(tid, cName, pName, allItems, addOnItems, false,
                            user.getEffectiveAvatarUrl(), user.getEffectiveName(), category, contact, totalDisc, fPerc,
                            voucherDeduction, phone);
                    if (inv != null) {
                        channel.sendMessageComponents(
                                EmbedUtil.containerBranded("\uD83D\uDCC3 Invoice \u2014 Payment Required", "",
                                        "Review your order and choose a payment method.", "attachment://invoice.png",
                                        ActionRow.of(
                                                Button.secondary("ticket_pay_binance_" + tid, "Binance").withEmoji(
                                                        Emoji.fromCustom("Binance", 1496974881030541533L, false)),
                                                Button.secondary("ticket_pay_payoneer_" + tid, "Payoneer").withEmoji(
                                                        Emoji.fromCustom("Payoneer", 1496974382172733490L, false)),
                                                Button.secondary("ticket_pay_cliq_" + tid, "CliQ").withEmoji(
                                                        Emoji.fromCustom("Cliq", 1496974857118679162L, false)),
                                                Button.secondary("ticket_pay_paypal_" + tid, "PayPal").withEmoji(
                                                        Emoji.fromCustom("Paypal", 1496974360286593134L, false)),
                                                Button.secondary("ticket_pay_friendi_" + tid, "Friendi Pay").withEmoji(
                                                        Emoji.fromCustom("friendipay", 1496974547969376479L, false))),
                                        ActionRow.of(
                                                Button.secondary("ticket_pay_rajhi_" + tid, "Al Rajhi")
                                                        .withEmoji(Emoji.fromCustom("BankTransfer",
                                                                1496974903490904278L, false)))))
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
        if (name.startsWith("ORDER"))
            type = "ORDER";
        else if (name.startsWith("COMPLAINT"))
            type = "COMPLAINT";

        if ("ORDER".equalsIgnoreCase(type)) {
            return buildOrderPipelineContainer(ticket, claimed, staff);
        }

        StringBuilder b = new StringBuilder();
        b.append("<@&").append(com.highcore.bot.config.Config.ROLE_STAFF).append(">\n\n");
        b.append("Welcome <@").append(userId).append("> \uD83D\uDC4B\n\n");

        if (meta != null) {
            b.append("\n");

            if (meta.has("subject"))
                b.append("**Subject:** `").append(meta.get("subject").getAsString()).append("` \u2022 ");

            if (meta.has("type"))
                b.append("**Type:** `").append(meta.get("type").getAsString()).append("`\n\n");
            else
                b.append("\n\n");

            String details = "N/A";
            if (meta.has("body"))
                details = meta.get("body").getAsString();
            else if (meta.has("client_name"))
                details = meta.get("client_name").getAsString();
            else if (meta.has("target"))
                details = meta.get("target").getAsString();

            b.append("**Details:** `").append(details).append("`\n");
        }

        b.append("\nA staff member will be with you shortly \u2014 please describe your issue in full detail.");

        ActionRow row;
        if (!claimed) {
            row = ActionRow.of(
                    Button.secondary("ticket_claim", "Claim Ticket"),
                    Button.secondary("ticket_close", "Close Ticket"));
        } else {
            row = ActionRow.of(
                    Button.secondary("ticket_unclaim", "Unclaim Ticket"),
                    Button.secondary("ticket_close", "Close Ticket"));
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

        b.append("\n");

        String subject = "gh";
        if (meta != null && meta.has("project_name"))
            subject = meta.get("project_name").getAsString();
        b.append("**Subject:** `").append(subject).append("`\n\n");

        b.append("\uD83D\uDCCB **Questionnaire Data**\n");
        if (meta != null) {
            b.append("**Project:** `").append(meta.get("project_name").getAsString()).append("` \u2022 ");
            b.append("**Client:** `").append(meta.get("client_name").getAsString()).append("` \u2022 ");
            b.append("**Contact:** `").append(meta.has("contact") ? meta.get("contact").getAsString() : "N/A")
                    .append("` \u2022 ");
            b.append("**ETA:** `").append(meta.has("eta") ? meta.get("eta").getAsString() : "N/A").append("`\n");

            if (meta.has("items")) {
                StringBuilder svs = new StringBuilder();
                JsonArray items = meta.getAsJsonArray("items");
                for (int i = 0; i < items.size(); i++) {
                    svs.append(items.get(i).getAsJsonObject().get("name").getAsString());
                    if (i < items.size() - 1)
                        svs.append(", ");
                }
                b.append("**Services Requested:** ").append(svs.toString()).append("\n");
            }
        }
        b.append("\u26A0\uFE0F **Your ticket is locked** \u2014 it will be unlocked once payment is confirmed.");

        ActionRow row;
        row = ActionRow.of(
                Button.secondary("ticket_verify", "Verify Payment"),
                Button.secondary("ticket_close", "Close Ticket"));

        return EmbedUtil.containerBranded("Order Pipeline", "Case #" + tid, b.toString(), EmbedUtil.BANNER_ORDER_TICKET,
                row);
    }

    public static void claimTicket(TextChannel ch, Member member, ButtonInteractionEvent event) {
        JsonObject ticket = ticketCache.get(ch.getId());
        if (ticket == null)
            ticket = SupabaseClient.getTicketAndMetaByChannel(ch.getId());
        if (ticket == null) {
            PanelService.replyEphemeral(event, "Session data missing.");
            return;
        }

        String tid = ticket.get("ticket_id").getAsString();
        SupabaseClient.claimTicket(tid, member.getEffectiveName());

        LogManager.logEmbed(ch.getGuild(), Config.LOG_TICKETS,
                EmbedUtil.createOldLogEmbed("ticket-claim",
                        "Action: Ticket Claimed\nCase: #" + tid + "\nAgent: "
                                + member.getAsMention() + "\nChannel: " + ch.getAsMention(),
                        member, null, null, EmbedUtil.SUCCESS));

        // Update the original message buttons immediately
        event.editMessage(new MessageEditBuilder()
                .setComponents(List.of(rebuildWelcomeContainer(ticket, true, member, ch)))
                .useComponentsV2(true)
                .build()).queue();

        // Rename channel
        String type = ch.getName().split("-")[0];
        ch.getManager().setName(type + "-" + member.getEffectiveName()).queue();

        // Send separate notification
        ch.sendMessageComponents(EmbedUtil.brandedNotice("▶ NOTICE • Claimed",
                "\uD83D\uDCE1 Ticket Handled By: " + member.getAsMention()))
                .useComponentsV2(true).queue();
    }

    public static void unclaimTicket(TextChannel ch, Member member, ButtonInteractionEvent event) {
        JsonObject ticket = ticketCache.get(ch.getId());
        if (ticket == null)
            ticket = SupabaseClient.getTicketAndMetaByChannel(ch.getId());
        if (ticket == null) {
            event.reply("Session data missing.").setEphemeral(true).queue();
            return;
        }

        SupabaseClient.unclaimTicket(ticket.get("ticket_id").getAsString());

        LogManager.logEmbed(ch.getGuild(), Config.LOG_TICKETS,
                EmbedUtil.createOldLogEmbed("ticket-unclaim",
                        "Action: Agent Withdrawal\nCase: #" + ticket.get("ticket_id").getAsString() + "\nAgent: "
                                + member.getAsMention() + "\nChannel: " + ch.getAsMention(),
                        member, null, null, EmbedUtil.WARNING));

        // Update the original message buttons immediately
        event.editMessage(new MessageEditBuilder()
                .setComponents(List.of(rebuildWelcomeContainer(ticket, false, null, ch)))
                .useComponentsV2(true)
                .build()).queue();

        // Send separate notification
        ch.sendMessageComponents(EmbedUtil.brandedNotice("▶ NOTICE • Unclaimed",
                "\u2935\uFE0F Ticket Unclaimed By: " + member.getAsMention()))
                .useComponentsV2(true).queue();
    }

    // Metadata is now managed via database lookups rather than channel topics.

    public static void requestCloseConfirmation(ButtonInteractionEvent event) {
        event.replyComponents(EmbedUtil.containerBranded("CONFIRMATION", "Close Ticket",
                "### Are you sure you want to close this ticket?",
                EmbedUtil.BANNER_SUPPORT,
                ActionRow.of(
                        Button.secondary("ticket_close_final", "Confirm Close"),
                        Button.secondary("ticket_close_cancel", "Cancel"))))
                .setEphemeral(true).useComponentsV2(true).queue();
    }

    public static void closeTicket(ButtonInteractionEvent event, Member member) {
        event.deferEdit().queue();
        closeTicketInternal(event.getChannel().asTextChannel(), member, event);
    }

    private static void closeTicketInternal(TextChannel ch, Member member, ButtonInteractionEvent event) {
        JsonObject ticket = SupabaseClient.getTicketAndMetaByChannel(ch.getId());
        if (ticket == null) {
            log.warn("Ticket data not found in DB for channel: {}", ch.getId());
            return;
        }
        String tid = ticket.get("ticket_id").getAsString();

        String userId = ticket.get("user_id").getAsString();
        Member client = ch.getGuild().getMemberById(userId);

        // Feedback Trigger (For Order tickets) - Public in channel
        String type = ticket.has("type") && !ticket.get("type").isJsonNull() ? ticket.get("type").getAsString() : "";
        if (type.isEmpty() && ticket.has("metadata")) {
            JsonElement metaEl = ticket.get("metadata");
            if (metaEl.isJsonObject()) {
                JsonObject meta = metaEl.getAsJsonObject();
                if (meta.has("type")) type = meta.get("type").getAsString();
            }
        }

        log.info("[FEEDBACK DEBUG] Ticket #{} type detected: '{}'", tid, type);

        if ("ORDER".equalsIgnoreCase(type)) {
            ch.sendMessageComponents(com.highcore.bot.utils.EmbedUtil.containerBranded("Feedback", "Order Completed", 
                "How was your experience with Highcore Agency?", 
                "https://imgur.com/UyWt6Jr.png",
                ActionRow.of(
                    Button.secondary("feedback_star_1", "⭐"),
                    Button.secondary("feedback_star_2", "⭐⭐"),
                    Button.secondary("feedback_star_3", "⭐⭐⭐"),
                    Button.secondary("feedback_star_4", "⭐⭐⭐⭐"),
                    Button.secondary("feedback_star_5", "⭐⭐⭐⭐⭐")
                ))).useComponentsV2(true).queue();
        }

        // 1. Remove client write access but keep view access
        if (client != null) {
            ch.getManager().putMemberPermissionOverride(client.getIdLong(), 
                    EnumSet.of(Permission.VIEW_CHANNEL), 
                    EnumSet.of(Permission.MESSAGE_SEND)).queue();
        } else {
            ch.getManager()
                    .putPermissionOverride(ch.getGuild().getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL))
                    .queue();
        }

        // 2. Update DB status
        SupabaseClient.updateTicketStatus(tid, "closed", member.getEffectiveName());

        LogManager.logEmbed(ch.getGuild(), Config.LOG_TICKETS,
                EmbedUtil.createOldLogEmbed("ticket-close",
                        "Action: Ticket Closed\nCase: #" + tid + "\nAgent: " + member.getAsMention()
                                + "\nChannel: " + ch.getAsMention(),
                        member, null, null, EmbedUtil.DANGER));

        // 3. Rename channel
        String currentName = ch.getName();
        if (!currentName.endsWith("-C")) {
            ch.getManager().setName(currentName + "-C").queue();
        }

        // 4. Auto Transcript
        transcriptTicket(ch, member, event);

        // 5. Send Control Panel
        Container panel = EmbedUtil.containerBranded("ARCHIVES", "Control Panel",
                "### TICKET CLOSED\nAgent **"
                        + member.getEffectiveName() + "** has closed this ticket.\n\nSelect an action below.",
                EmbedUtil.BANNER_SUPPORT,
                ActionRow.of(
                        Button.secondary("ticket_reopen", "Reopen"),
                        Button.secondary("ticket_transcript", "Transcript")
                                .withEmoji(Emoji.fromCustom("Transcript", 1496974091318722561L, false)),
                        Button.secondary("ticket_delete_init", "Delete")
                                .withEmoji(Emoji.fromCustom("Delete", 1496974827754487988L, false))));

        if (event != null) {
            event.getHook().sendMessageComponents(panel).setEphemeral(true).useComponentsV2(true).queue();
        } else {
            ch.sendMessageComponents(panel).useComponentsV2(true).queue();
        }
    }

    public static void reopenTicket(TextChannel ch, Member member, ButtonInteractionEvent event) {
        String tid = ch.getName().split("-")[ch.getName().split("-").length - 1];
        JsonObject ticket = SupabaseClient.getTicketAndMetaByChannel(ch.getId());
        if (ticket == null) {
            PanelService.replyEphemeral(event, "Session data missing.");
            return;
        }

        String userId = ticket.get("user_id").getAsString();

        // 1. Restore client access
        ch.getManager().putMemberPermissionOverride(Long.parseLong(userId),
                EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND), null).queue();

        // 2. Update DB status
        SupabaseClient.updateTicketStatus(tid, "open", null);

        LogManager.logEmbed(ch.getGuild(), Config.LOG_TICKETS,
                EmbedUtil.createOldLogEmbed("ticket-reopen",
                        "Action: Session Restoration\nCase: #" + tid + "\nAgent: " + member.getAsMention()
                                + "\nChannel: " + ch.getAsMention(),
                        member, null, null, EmbedUtil.INFO));

        PanelService.reply(event, "✅ Ticket reopened. Access restored.");
    }

    public static void requestDeleteConfirmation(ButtonInteractionEvent event) {
        event.replyComponents(EmbedUtil.containerBranded("DANGER", "Delete Channel",
                "### Are you sure?\nThis action will permanently delete this channel and cannot be undone.",
                EmbedUtil.BANNER_SUPPORT,
                ActionRow.of(
                        Button.secondary("ticket_delete_final", "Confirm Delete")
                                .withEmoji(Emoji.fromCustom("Delete", 1496974827754487988L, false)),
                        Button.secondary("ticket_delete_cancel", "Cancel"))))
                .setEphemeral(true).useComponentsV2(true).queue();
    }

    public static void transcriptTicket(TextChannel ch, Member member, ButtonInteractionEvent event) {
        JsonObject ticket = SupabaseClient.getTicketAndMetaByChannel(ch.getId());
        if (ticket == null) {
            if (event != null)
                PanelService.replyEphemeral(event, "Session data missing.");
            return;
        }
        String tid = ticket.get("ticket_id").getAsString();

        JsonArray msgs = SupabaseClient.getTicketMessages(tid);

        // Try to find opened_at or fallback
        String openedAt = ticket.has("created_at") ? ticket.get("created_at").getAsString() : Instant.now().toString();
        String opener = ticket.has("user_name") ? ticket.get("user_name").getAsString() : "Unknown";
        String type = ch.getName().toUpperCase().split("-")[0];

        byte[] html = TranscriptService.buildHtml(tid, ch.getName(), type, "closed", openedAt, opener,
                ticket.has("claimed_by") && !ticket.get("claimed_by").isJsonNull()
                        ? ticket.get("claimed_by").getAsString()
                        : "None",
                member.getEffectiveName(), msgs);

        TextChannel logCh = ch.getGuild().getTextChannelById("1488795131019526147");
        if (logCh != null) {
            String userId = ticket.get("user_id").getAsString();
            String url = "https://high-core-dc-bot-production.up.railway.app/view/transcript/" + tid;
            String logBody = "▶ **TRANSCRIPT • Archive \u2014 Case #" + tid + "**\n\n" +
                    "**User:** <@" + userId + "> (" + opener + ")\n" +
                    "**Closed By:** " + member.getAsMention();

            logCh.sendMessage(logBody)
                    .setAllowedMentions(java.util.Collections.emptyList())
                    .setComponents(ActionRow
                            .of(Button.link(url, "View Web Transcript").withEmoji(Emoji.fromUnicode("\uD83D\uDCDC"))))
                    .addFiles(net.dv8tion.jda.api.utils.FileUpload.fromData(html, "transcript-" + tid + ".html"))
                    .queue();
            if (event != null) {
                PanelService.replyEphemeral(event, "✅ Transcript has been uploaded to the management sector.");
            }
        } else {
            if (event != null) {
                PanelService.replyEphemeral(event, "❌ Management sector not found.");
            }
        }
    }

    public static void deleteTicket(TextChannel ch) {
        ch.delete().queue();
    }

    public static void markAsPaid(TextChannel ch, String tid, Member member) {
        SupabaseClient.logStat("PAYMENT", member.getId(), "Ticket #" + tid + " marked paid");
    }

    public static void finalizeClose(TextChannel ch, Member m, String status) {
        closeTicketInternal(ch, m, null);
    }
}
