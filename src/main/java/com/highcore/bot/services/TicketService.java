package com.highcore.bot.services;

import com.google.gson.JsonObject;
import com.highcore.bot.database.SupabaseClient;
import com.highcore.bot.services.InvoiceService;
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
import net.dv8tion.jda.api.entities.Message;
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

    public static void createTicket(net.dv8tion.jda.api.interactions.callbacks.IReplyCallback event,
                                    String subject, String priority, String type) {
        createTicket(event, subject, priority, type, null);
    }

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
        String ticketId    = String.format("%04d", SupabaseClient.getNextTicketNumber());
        String channelName = type.toLowerCase() + "-" + ticketId;

        Role adminRole = cat.getGuild().getRoleById(ADMIN_ROLE_ID);
        var builder = cat.createTextChannel(channelName)
            .addPermissionOverride(event.getMember(),
                    EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND, Permission.MESSAGE_ATTACH_FILES), null)
            .addPermissionOverride(cat.getGuild().getPublicRole(),
                    null, EnumSet.of(Permission.VIEW_CHANNEL));
        if (adminRole != null) {
            builder = builder.addPermissionOverride(adminRole,
                    EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND,
                               Permission.MESSAGE_HISTORY, Permission.MESSAGE_ATTACH_FILES), null);
        }
        builder.queue(channel -> {
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

    public static void createHighEndOrderTicket(Guild guild, User user, String pName, String cName,
                                                String contact, String eta,
                                                List<InvoiceService.OrderItem> items) {
        Member member = guild.getMember(user);
        if (member == null) return;

        String ticketId    = String.format("%03d", SupabaseClient.getNextTicketNumber());
        String channelName = "order-" + ticketId;

        Category cat = guild.getCategoriesByName("TICKETS", true).stream().findFirst().orElse(null);
        if (cat == null) cat = guild.getCategoryById(TICKET_CAT_ID);
        if (cat == null) { log.error("Ticket category not found"); return; }

        guild.createTextChannel(channelName, cat)
            .addPermissionOverride(member,
                EnumSet.of(Permission.VIEW_CHANNEL),
                EnumSet.of(Permission.MESSAGE_SEND))
            .addPermissionOverride(guild.getPublicRole(),
                null, EnumSet.of(Permission.VIEW_CHANNEL))
            .queue(channel -> {
                SupabaseClient.createTicket(ticketId, user.getId(), channel.getId(),
                    "ORDER", pName, "HIGH", null);

                String infoLine =
                    "**Case ID:** `" + ticketId + "` · **Project:** `" + pName + "` · **Client:** `" + cName + "`\n" +
                    "**Contact:** `" + contact + "` · **Delivery:** `" + eta + "`";

                Container welcome = EmbedUtil.containerBranded(
                    "ORDER PIPELINE", "Active Ticket",
                    "Welcome " + user.getAsMention() + " \uD83D\uDC4B\n\n" +
                    infoLine + "\n\n" +
                    "\u26A0\uFE0F **Your ticket is locked** — it will be unlocked once payment is confirmed.\n" +
                    "A staff member will review your order and reach out shortly.\n\n" +
                    "> \uD83D\uDCCC Please do **not** ping staff. We'll get back to you as soon as possible.",
                    EmbedUtil.BANNER_ORDER_TICKET,
                    Emoji.fromUnicode("\uD83D\uDCE6"),
                    ActionRow.of(getTicketButtons("open")));

                channel.sendMessageComponents(welcome).useComponentsV2(true).queue();

                byte[] invoiceData = InvoiceService.generateInvoice(cName, pName, items);

                Container invoiceContainer = EmbedUtil.containerBranded(
                    "INVOICE", "Payment Required",
                    "\uD83E\uDDFE **Review your invoice below and choose a payment method.**\n\n" +
                    "**Available Methods:**\n" +
                    "> \uD83D\uDCB3 **PayPal** — `billing@highcore.agency`\n" +
                    "> \uD83C\uDF10 **Stripe** — Contact staff for link\n" +
                    "> \uD83C\uDFE6 **Bank Transfer (Al-Rajhi)** — `SA29 8000 0000 1234 5678 1234`\n" +
                    "> \uD83D\uDCB5 **USDT (TRC20)** — `THighCoreAgencyWallet9xR3mZ`\n" +
                    "> \uD83D\uDCB0 **STC Pay** — `+966 5X XXX XXXX`\n\n" +
                    "After payment, send proof to this channel. Staff will verify and unlock.",
                    EmbedUtil.BANNER_INVOICE,
                    Emoji.fromUnicode("\uD83E\uDDFE"),
                    ActionRow.of(getPaymentButtons(ticketId)));

                channel.sendMessageComponents(invoiceContainer).useComponentsV2(true).queue();

                if (invoiceData != null) {
                    channel.sendFiles(FileUpload.fromData(invoiceData, "invoice-" + ticketId + ".png")).queue();
                }
            });
    }

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
        return List.of(
            Button.success("ticket_reopen", "Reopen Ticket")
                    .withEmoji(Emoji.fromUnicode("\uD83D\uDD04")),
            Button.danger("ticket_delete", "Delete Ticket")
                    .withEmoji(Emoji.fromUnicode("\uD83D\uDDD1\uFE0F"))
        );
    }

    public static List<Button> getPaymentButtons(String id) {
        return List.of(
            Button.primary("pay_paypal_"  + id, "PayPal")
                    .withEmoji(Emoji.fromUnicode("\uD83D\uDCB3")),
            Button.primary("pay_stripe_"  + id, "Stripe")
                    .withEmoji(Emoji.fromUnicode("\uD83C\uDF10")),
            Button.primary("pay_bank_"    + id, "Bank Transfer")
                    .withEmoji(Emoji.fromUnicode("\uD83C\uDFE6")),
            Button.primary("pay_usdt_"    + id, "USDT")
                    .withEmoji(Emoji.fromUnicode("\uD83D\uDCB0")),
            Button.primary("pay_stcpay_"  + id, "STC Pay")
                    .withEmoji(Emoji.fromUnicode("\uD83D\uDCF1"))
        );
    }

    public static void claimTicket(TextChannel channel, Member claimer) {
        List<ContainerChildComponent> children = new ArrayList<>();
        children.add(TextDisplay.of(
            "\uD83D\uDCE1 **Ticket Claimed** · " + claimer.getAsMention()));
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
        String overrideOpenerName = channel.getPermissionOverrides().stream()
            .filter(po -> po.isMemberOverride() && po.getMember() != null
                       && !po.getMember().getUser().isBot())
            .map(po -> po.getMember().getEffectiveName())
            .findFirst().orElse(null);

        channel.getPermissionOverrides().stream()
            .filter(po -> po.isMemberOverride())
            .forEach(po -> po.delete().queue());

        List<ContainerChildComponent> closed = new ArrayList<>();
        closed.add(TextDisplay.of("\uD83D\uDD12 **Ticket Closed** · `Channel is now locked.`"));
        closed.add(Separator.createDivider(Spacing.SMALL));
        closed.add(ActionRow.of(getTicketButtons("closed")));
        PanelService.reply(channel, Container.of(closed));

        if (!status.equals("TRANSCRIPT")) return;

        JsonObject ticket  = SupabaseClient.getTicketByChannel(channel.getId());
        String closedBy    = closer.getEffectiveName();
        String ticketId    = channel.getName();
        String type        = channel.getName().split("-")[0].toUpperCase();
        String openedAt    = channel.getTimeCreated().toInstant().toString();
        String openerId    = null;
        if (ticket != null) {
            if (ticket.has("ticket_id")) ticketId = ticket.get("ticket_id").getAsString();
            if (ticket.has("user_id") && !ticket.get("user_id").isJsonNull())
                openerId = ticket.get("user_id").getAsString();
        }

        String openerName = overrideOpenerName != null ? overrideOpenerName : "Unknown";
        if (openerId != null) {
            try {
                Member openerMember = channel.getGuild().retrieveMemberById(openerId).complete();
                if (openerMember != null) openerName = openerMember.getEffectiveName();
            } catch (Exception ignored) { }
        }

        final String fTicketId   = ticketId;
        final String fType       = type;
        final String fOpenedAt   = openedAt;
        final String fOpenerName = openerName;
        final TextChannel logCh  = channel.getGuild().getTextChannelById(TRANSCRIPT_CHANNEL_ID);

        channel.getHistory().retrievePast(100).queue(raw -> {
            List<Message> ordered = new ArrayList<>(raw);
            Collections.reverse(ordered);

            byte[] html = TranscriptService.generateFromMessages(
                fTicketId, channel.getName(), fType, "closed", fOpenedAt, fOpenerName, closedBy, ordered);

            if (logCh != null) {
                PanelService.reply(logCh, Container.of(TextDisplay.of(
                    "\uD83D\uDCCE **Transcript Saved** · `#" + fTicketId + "` · `" + channel.getName() + "`\n" +
                    "Opened by: **" + fOpenerName + "** · Closed by: **" + closedBy + "** · **" + ordered.size() + "** messages")));
                if (html != null) {
                    logCh.sendFiles(FileUpload.fromData(html, "transcript-" + fTicketId + ".html")).queue();
                }
            }

            if (ticket != null) SupabaseClient.updateTicketStatus(fTicketId, "closed", closedBy);

        }, err -> log.error("Failed to fetch history for transcript in {}", channel.getName(), err));
    }
}
