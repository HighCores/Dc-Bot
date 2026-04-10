package com.highcore.bot.services;

import com.google.gson.JsonObject;
import com.highcore.bot.database.SupabaseClient;
import com.highcore.bot.utils.EmbedUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.components.mediagallery.MediaGallery;
import net.dv8tion.jda.api.components.mediagallery.MediaGalleryItem;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.separator.Separator.Spacing;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.utils.FileUpload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;
import java.util.stream.Collectors;

public class TicketService {
    private static final Logger log = LoggerFactory.getLogger(TicketService.class);

    private static final String TICKET_CAT_ID         = "1488795130881249404";
    private static final String TRANSCRIPT_CHANNEL_ID = "1488795131019526147";
    private static final String ADMIN_ROLE_ID          = "1488795130008961040";
    private static final String TICKET_IMAGE           =
        "https://cdn.discordapp.com/attachments/1488900668042510568/1492305839736750230/IMG_20260411_023024.png" +
        "?ex=69dad99d&is=69d9881d&hm=9df0283d5f26dc60385980e7f3d713966c15e2505d78aa2b9da35f9359901046&";

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

                String header = switch (type.toUpperCase()) {
                    case "SUPPORT"   -> "Support Center";
                    case "COMPLAINT" -> "Complaint Board";
                    default          -> "Ticket";
                };

                String infoLine = "**Type:** `" + type + "` · **Priority:** `" + priority + "` · **Subject:** `" + subject + "`"
                    + (details != null && !details.isBlank() ? "\n" + details : "");

                List<ContainerChildComponent> children = new ArrayList<>();
                children.add(MediaGallery.of(MediaGalleryItem.fromUrl(TICKET_IMAGE)));
                children.add(TextDisplay.of("## " + header + " | Active Ticket\n<@&" + ADMIN_ROLE_ID + ">"));
                children.add(Separator.createDivider(Spacing.SMALL));
                children.add(TextDisplay.of("Welcome " + event.getUser().getAsMention() + " \uD83D\uDC4B\n\n" + infoLine));
                children.add(Separator.createDivider(Spacing.SMALL));
                children.add(TextDisplay.of("A staff member will be with you shortly — please describe your issue in full detail."));
                children.add(ActionRow.of(getTicketButtons("open")));

                PanelService.reply(channel, Container.of(children));

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

                String infoLine =
                    "**Case ID:** `" + ticketId + "` · **Project:** `" + pName + "` · **Client:** `" + cName + "`\n" +
                    "**Contact:** `" + contact + "` · **Delivery:** `" + eta + "`";

                List<ContainerChildComponent> children = new ArrayList<>();
                children.add(MediaGallery.of(MediaGalleryItem.fromUrl(TICKET_IMAGE)));
                children.add(TextDisplay.of("## Order Pipeline | Active Ticket\n<@&" + ADMIN_ROLE_ID + ">"));
                children.add(Separator.createDivider(Spacing.SMALL));
                children.add(TextDisplay.of(
                    "Welcome " + user.getAsMention() + " \uD83D\uDC4B\n\n" +
                    infoLine + "\n\n" +
                    "\u26A0\uFE0F **Channel locked** — your ticket will be unlocked once payment is confirmed."));
                children.add(Separator.createDivider(Spacing.SMALL));
                children.add(TextDisplay.of("Please review your invoice below and choose a payment method."));
                children.add(ActionRow.of(getTicketButtons("open")));

                PanelService.reply(channel, Container.of(children));

                // Invoice section
                List<ContainerChildComponent> invChildren = new ArrayList<>();
                invChildren.add(TextDisplay.of("## \uD83E\uDDFE Invoice — Payment Required"));
                invChildren.add(Separator.createDivider(Spacing.SMALL));
                invChildren.add(ActionRow.of(getPaymentButtons(ticketId)));
                PanelService.reply(channel, Container.of(invChildren));

                byte[] invoiceData = InvoiceService.generateInvoice(cName, pName, items);
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
        String roles = claimer.getRoles().stream()
            .filter(r -> !r.isPublicRole())
            .limit(4)
            .map(Role::getName)
            .collect(Collectors.joining(" · "));

        List<ContainerChildComponent> children = new ArrayList<>();
        children.add(TextDisplay.of(
            "\uD83D\uDCE1 **Ticket Claimed** · " + claimer.getAsMention() +
            (roles.isEmpty() ? "" : "\n`" + roles + "`")));
        children.add(Separator.createDivider(Spacing.SMALL));
        children.add(ActionRow.of(
            Button.danger("ticket_close", "Close Ticket").withEmoji(Emoji.fromUnicode("\uD83D\uDD12"))
        ));

        PanelService.reply(channel, Container.of(children));
    }

    public static void reopenTicket(TextChannel channel, Member reopener) {
        List<ContainerChildComponent> children = new ArrayList<>();
        children.add(TextDisplay.of(
            "\uD83D\uDD04 **Ticket Reopened** · " + reopener.getAsMention() +
            "\n`Back in queue — ready to assist.`"));
        children.add(Separator.createDivider(Spacing.SMALL));
        children.add(ActionRow.of(getTicketButtons("open")));

        PanelService.reply(channel, Container.of(children));
    }

    public static void closeTicket(TextChannel channel, Member closer) {
        List<ContainerChildComponent> children = new ArrayList<>();
        children.add(TextDisplay.of(
            "\u26A0\uFE0F **Close Ticket** — requested by " + closer.getAsMention() +
            "\n\nChoose an action below:"));
        children.add(Separator.createDivider(Spacing.SMALL));
        children.add(ActionRow.of(
            Button.success("order_status_update_TRANSCRIPT", "Save Transcript")
                    .withEmoji(Emoji.fromUnicode("\uD83D\uDCDC")),
            Button.secondary("ticket_reopen", "Reopen Ticket")
                    .withEmoji(Emoji.fromUnicode("\uD83D\uDD04")),
            Button.danger("ticket_delete", "Delete Immediately")
                    .withEmoji(Emoji.fromUnicode("\uD83D\uDDD1\uFE0F"))
        ));

        PanelService.reply(channel, Container.of(children));
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

                PanelService.reply(logCh, Container.of(TextDisplay.of(
                    "\uD83D\uDCCE **Transcript Saved** · `#" + ticketId + "` (`" + channel.getName() + "`)\n" +
                    "Closed by: **" + closedBy + "**")));

                if (html != null) {
                    logCh.sendFiles(FileUpload.fromData(html, "transcript-" + ticketId + ".html")).queue();
                }

                SupabaseClient.updateTicketStatus(ticketId, "closed", closedBy);
            }
        }

        List<ContainerChildComponent> children = new ArrayList<>();
        children.add(TextDisplay.of("\uD83D\uDD12 **Ticket Closed** · `Channel is now locked.`"));
        children.add(Separator.createDivider(Spacing.SMALL));
        children.add(ActionRow.of(getTicketButtons("closed")));

        PanelService.reply(channel, Container.of(children));
    }
}
