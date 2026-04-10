package com.highcore.bot.services;

import com.google.gson.JsonObject;
import com.highcore.bot.database.SupabaseClient;
import com.highcore.bot.utils.EmbedUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.utils.FileUpload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;

public class TicketService {
    private static final Logger log = LoggerFactory.getLogger(TicketService.class);

    private static final String TICKET_CAT_ID         = "1488795130881249404";
    private static final String TRANSCRIPT_CHANNEL_ID = "1488795131019526147";

    // ── Create ticket (no extra details) ─────────────────────────────────────
    public static void createTicket(net.dv8tion.jda.api.interactions.callbacks.IReplyCallback event,
                                    String subject, String priority, String type) {
        createTicket(event, subject, priority, type, null);
    }

    // ── Create ticket (with details from modal) ───────────────────────────────
    public static void createTicket(net.dv8tion.jda.api.interactions.callbacks.IReplyCallback event,
                                    String subject, String priority, String type, String details) {
        Guild guild = event.getGuild();
        if (guild == null) return;
        Category cat = guild.getCategoryById(TICKET_CAT_ID);
        if (cat == null) {
            log.warn("Ticket category {} not found — creating fallback.", TICKET_CAT_ID);
            guild.createCategory("Agency Tickets").queue(c ->
                    proceedWithTicket(event, c, subject, priority, type, details));
            return;
        }
        proceedWithTicket(event, cat, subject, priority, type, details);
    }

    private static void proceedWithTicket(net.dv8tion.jda.api.interactions.callbacks.IReplyCallback event,
                                          Category cat, String subject, String priority, String type, String details) {
        long timestamp = System.currentTimeMillis();
        String ticketId   = String.format("%d", timestamp).substring(9);
        String channelName = type.toLowerCase() + "-" + ticketId;

        cat.createTextChannel(channelName)
            .addPermissionOverride(event.getMember(),
                    EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND, Permission.MESSAGE_ATTACH_FILES), null)
            .addPermissionOverride(cat.getGuild().getPublicRole(),
                    null, EnumSet.of(Permission.VIEW_CHANNEL))
            .queue(channel -> {
                SupabaseClient.createTicket(ticketId, event.getUser().getId(),
                        channel.getId(), type, subject, priority, null);

                String banner = type.equalsIgnoreCase("SUPPORT") ? EmbedUtil.BANNER_SUPPORT : EmbedUtil.BANNER_MAIN;
                String header = switch (type.toUpperCase()) {
                    case "SUPPORT"   -> "Support Center";
                    case "COMPLAINT" -> "Complaint Board";
                    default          -> "Ticket";
                };

                String body = String.format(
                    "Hey %s, welcome to **High Core Agency** support.\n\n" +
                    "A member of our team will be with you shortly. " +
                    "Please describe your issue in as much detail as possible so we can help you faster.\n\n" +
                    "**Type:** `%s`  ·  **Priority:** `%s`\n" +
                    "**Subject:** `%s`" +
                    "%s",
                    event.getUser().getAsMention(), type, priority, subject,
                    (details != null && !details.isBlank() ? "\n\n" + details : ""));

                PanelService.reply(channel,
                        EmbedUtil.containerBranded(header, "Active Ticket", body, banner, null,
                                ActionRow.of(getTicketButtons("open"))));

                event.getHook().sendMessage(
                    "Your ticket has been opened: " + channel.getAsMention())
                    .setEphemeral(true).queue();
            });
    }

    // ── High-end order ticket ─────────────────────────────────────────────────
    public static void createHighEndOrderTicket(Guild guild, User user, String pName, String cName,
                                                String contact, String eta,
                                                List<InvoiceService.OrderItem> items) {
        Category cat = guild.getCategoryById(TICKET_CAT_ID);
        long timestamp = System.currentTimeMillis();
        String ticketId   = String.format("%d", timestamp).substring(9);
        String channelName = "order-" + ticketId;

        guild.createTextChannel(channelName, cat)
            .addPermissionOverride(guild.getMember(user),
                    EnumSet.of(Permission.VIEW_CHANNEL),
                    EnumSet.of(Permission.MESSAGE_SEND))
            .queue(channel -> {

                String bodyInfo = String.format(
                    "Hey %s, thank you for placing an order with **High Core Agency**.\n\n" +
                    "⚠️ **This channel is currently locked.**\n" +
                    "You won't be able to send messages until the invoice below has been settled. " +
                    "Once payment is confirmed, our team will unlock this channel and start working immediately.\n\n" +
                    "**Case ID:** `%s`\n" +
                    "**Project:** `%s`\n" +
                    "**Client:** `%s`\n" +
                    "**Contact:** `%s`\n" +
                    "**Estimated Delivery:** `%s`\n\n" +
                    "Please review your invoice below and choose a payment method.",
                    user.getAsMention(), ticketId, pName, cName, contact, eta);

                PanelService.reply(channel,
                        EmbedUtil.containerBranded("Order Pipeline", "New Order", bodyInfo,
                                EmbedUtil.BANNER_ORDER_TIK, null,
                                ActionRow.of(getTicketButtons("open"))));

                byte[] invoiceData = InvoiceService.generateInvoice(cName, pName, items);
                PanelService.reply(channel,
                        EmbedUtil.containerBrandedRows("Invoice", "Payment Required",
                                "Please choose your preferred payment method using the buttons below.",
                                EmbedUtil.BANNER_INVOICE,
                                ActionRow.of(getPaymentButtons(ticketId))));

                if (invoiceData != null) {
                    channel.sendFiles(FileUpload.fromData(invoiceData, "invoice.png")).queue();
                }
            });
    }

    // ── Ticket button sets ────────────────────────────────────────────────────
    public static List<Button> getTicketButtons(String status) {
        if (status.equals("open")) return List.of(
            Button.primary("ticket_claim", "Claim Ticket")
                    .withEmoji(Emoji.fromUnicode("\uD83D\uDCE1")),
            Button.danger("ticket_close", "Close Ticket")
                    .withEmoji(Emoji.fromUnicode("\uD83D\uDD12"))
        );
        if (status.equals("claimed")) return List.of(
            Button.danger("ticket_close", "Close Ticket")
                    .withEmoji(Emoji.fromUnicode("\uD83D\uDD12"))
        );
        // closed
        return List.of(
            Button.success("ticket_reopen", "Reopen Ticket")
                    .withEmoji(Emoji.fromUnicode("\uD83D\uDD04")),
            Button.danger("ticket_delete", "Delete Ticket")
                    .withEmoji(Emoji.fromUnicode("\uD83D\uDDD1\uFE0F"))
        );
    }

    public static List<Button> getPaymentButtons(String id) {
        return List.of(
            Button.secondary("pay_paypal_" + id, "PayPal")
                    .withEmoji(Emoji.fromUnicode("\uD83D\uDCB3")),
            Button.secondary("pay_stripe_" + id, "Stripe")
                    .withEmoji(Emoji.fromUnicode("\uD83D\uDDA5\uFE0F"))
        );
    }

    // ── Ticket actions ────────────────────────────────────────────────────────
    public static void claimTicket(TextChannel channel, Member claimer) {
        String body = String.format(
            "This ticket has been picked up by **%s**.\n" +
            "They'll be assisting you from this point on — feel free to share any details.",
            claimer.getEffectiveName());
        PanelService.reply(channel,
                EmbedUtil.containerBranded("Ticket Claimed", "Staff Assigned", body,
                        EmbedUtil.BANNER_SUPPORT, null,
                        ActionRow.of(getTicketButtons("claimed"))));
    }

    public static void reopenTicket(TextChannel channel, Member reopener) {
        String body = String.format(
            "This ticket has been reopened by **%s**.\n" +
            "Our team is ready to continue assisting you.",
            reopener.getEffectiveName());
        PanelService.reply(channel,
                EmbedUtil.containerBranded("Ticket Reopened", "Back in Queue", body,
                        EmbedUtil.BANNER_MAIN, null,
                        ActionRow.of(getTicketButtons("open"))));
    }

    public static void closeTicket(TextChannel channel, Member closer) {
        String body = String.format(
            "Closure requested by **%s**.\n\n" +
            "Would you like to save a transcript before closing?\n" +
            "Choose **Save Transcript** to export the conversation, or **Delete** to remove it immediately.",
            closer.getEffectiveName());
        PanelService.reply(channel,
                EmbedUtil.containerBranded("Close Ticket", "Confirm Action", body,
                        EmbedUtil.BANNER_MAIN, null,
                        ActionRow.of(
                            Button.success("order_status_update_TRANSCRIPT", "Save Transcript")
                                    .withEmoji(Emoji.fromUnicode("\uD83D\uDCDC")),
                            Button.danger("ticket_delete", "Delete Immediately")
                                    .withEmoji(Emoji.fromUnicode("\uD83D\uDDD1\uFE0F"))
                        )));
    }

    public static void finalizeClose(TextChannel channel, Member closer, String status) {
        if (status.equals("TRANSCRIPT")) {
            JsonObject ticket = SupabaseClient.getTicketByChannel(channel.getId());
            TextChannel logCh  = channel.getGuild().getTextChannelById(TRANSCRIPT_CHANNEL_ID);

            if (ticket != null && logCh != null) {
                String ticketId  = ticket.has("ticket_id")  ? ticket.get("ticket_id").getAsString()  : channel.getName();
                String type      = ticket.has("type")       ? ticket.get("type").getAsString()       : "—";
                String tickStatus= ticket.has("status")     ? ticket.get("status").getAsString()     : "closed";
                String openedAt  = ticket.has("created_at") ? ticket.get("created_at").getAsString() : "—";
                String closedBy  = closer.getEffectiveName();

                byte[] html = TranscriptService.generate(ticketId, channel.getName(),
                        type, tickStatus, openedAt, closedBy);

                String logBody = String.format(
                    "A transcript has been saved for ticket **#%s** (`%s`).\n" +
                    "Closed by: **%s**",
                    ticketId, channel.getName(), closedBy);

                PanelService.reply(logCh,
                        EmbedUtil.containerBranded("Transcript Saved", "Archive", logBody,
                                EmbedUtil.BANNER_MAIN));

                if (html != null) {
                    logCh.sendFiles(FileUpload.fromData(html, "transcript-" + ticketId + ".html")).queue();
                }

                SupabaseClient.updateTicketStatus(ticketId, "closed", closedBy);
            }
        }

        String body = "This ticket has been closed and the channel is now locked.\n" +
                      "Press **Delete** to permanently remove the channel.";
        PanelService.reply(channel,
                EmbedUtil.containerBranded("Ticket Closed", "Session Ended", body,
                        EmbedUtil.BANNER_MAIN, null,
                        ActionRow.of(getTicketButtons("closed"))));
    }
}
