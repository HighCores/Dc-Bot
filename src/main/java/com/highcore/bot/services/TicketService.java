package com.highcore.bot.services;

import com.google.gson.JsonObject;
import com.highcore.bot.database.SupabaseClient;
import com.highcore.bot.services.InvoiceService;
import com.highcore.bot.utils.EmbedUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.mediagallery.MediaGallery;
import net.dv8tion.jda.api.components.mediagallery.MediaGalleryItem;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.utils.FileUpload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;
import java.io.*;

public class TicketService {
    private static final Logger log = LoggerFactory.getLogger(TicketService.class);

    public static net.dv8tion.jda.api.requests.RestAction<TextChannel> createTicket(Guild guild, User user, String subject, String priority, String type) {
        Member member = guild.getMember(user);
        if (member == null) return null;
        
        String ticketId = String.format("%03d", SupabaseClient.getNextTicketNumber());
        String channelName = type + "-" + ticketId;

        Category cat = guild.getCategoriesByName("TICKETS", true).stream().findFirst().orElse(null);
        if (cat == null) {
            log.error("Ticket category 'TICKETS' not found in guild '{}'", guild.getName());
            return null;
        }

        return guild.createTextChannel(channelName, cat)
                .addPermissionOverride(member, EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND, Permission.MESSAGE_ATTACH_FILES), null)
                .addPermissionOverride(guild.getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL))
                .onSuccess(channel -> {
                    SupabaseClient.createTicket(ticketId, user.getId(), channel.getId(), type, subject, priority, null);
                    finalizeTicket(channel, user, ticketId, type, subject, priority);
                });
    }

    // ── High-end order ticket (locked until payment) ──────────────────────────
    public static void createHighEndOrderTicket(Guild guild, User user, String pName, String cName,
                                                String contact, String eta,
                                                List<InvoiceService.OrderItem> items) {
        Member member = guild.getMember(user);
        if (member == null) return;

        String ticketId    = String.format("%03d", SupabaseClient.getNextTicketNumber());
        String channelName = "order-" + ticketId;

        Category cat = guild.getCategoriesByName("TICKETS", true).stream().findFirst().orElse(null);
        if (cat == null) { log.error("Ticket category not found"); return; }

        guild.createTextChannel(channelName, cat)
            // User can see but NOT send — locked until payment
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

                // ── Welcome message (locked notice) ──────────────────────────
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

                // ── Invoice + Payment options ─────────────────────────────────
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

    public static void createOrderTicket(Guild guild, User user, String type, int total, String wizId, JsonObject orderData) {
        Member member = guild.getMember(user);
        if (member == null) return;
        
        String ticketId = String.format("%03d", SupabaseClient.getNextTicketNumber());
        String channelName = "order-" + ticketId;

        Category cat = guild.getCategoriesByName("TICKETS", true).stream().findFirst().orElse(null);
        if (cat == null) return;

        guild.createTextChannel(channelName, cat)
                .addPermissionOverride(member, EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND, Permission.MESSAGE_ATTACH_FILES), null)
                .addPermissionOverride(guild.getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL))
                .queue(channel -> {
                    SupabaseClient.createTicket(ticketId, user.getId(), channel.getId(), type, "Project Order", "High", orderData);
                    
                    String summary = "Total: **$" + total + "**\nWizard: **" + wizId + "**";
                    channel.sendMessageComponents(
                        getWelcomeContainer(ticketId, user.getName(), type, summary, 
                            ActionRow.of(getTicketButtons("open")), 
                            ActionRow.of(getPaymentButtons(ticketId)))
                    ).useComponentsV2(true).queue();
                });
    }

    private static void finalizeTicket(TextChannel channel, User user, String ticketId, String type, String subject, String priority) {
        String body = "Subject: **" + subject + "**\nPriority: **" + priority + "**";
        channel.sendMessageComponents(
            getWelcomeContainer(ticketId, user.getName(), type, body, ActionRow.of(getTicketButtons("open")))
        ).useComponentsV2(true).queue();
    }

    private static List<Button> getTicketButtons(String status) {
        List<Button> buttons = new ArrayList<>();
        if (status.equals("open")) {
            buttons.add(Button.primary("ticket_claim", "Claim Ticket \uD83D\uDCE1"));
            buttons.add(Button.danger("ticket_close", "Close Ticket \u26D4"));
        } else if (status.equals("claimed")) {
            buttons.add(Button.danger("ticket_close", "Close Ticket \u26D4"));
        } else if (status.equals("closed")) {
            buttons.add(Button.success("ticket_reopen", "Reopen Ticket \uD83D\uDD04"));
            buttons.add(Button.secondary("ticket_transcript", "Save Transcript \u231B"));
            buttons.add(Button.danger("ticket_delete", "Delete Channel \u2623\uFE0F"));
        }
        return buttons;
    }

    private static List<Button> getPaymentButtons(String ticketId) {
        return List.of(
            Button.primary("pay_paypal_"   + ticketId, "PayPal")
                .withEmoji(Emoji.fromUnicode("\uD83D\uDCB3")),
            Button.primary("pay_stripe_"   + ticketId, "Stripe")
                .withEmoji(Emoji.fromUnicode("\uD83C\uDF10")),
            Button.primary("pay_bank_"     + ticketId, "Bank Transfer")
                .withEmoji(Emoji.fromUnicode("\uD83C\uDFE6")),
            Button.primary("pay_usdt_"     + ticketId, "USDT")
                .withEmoji(Emoji.fromUnicode("\uD83D\uDCB0")),
            Button.primary("pay_stcpay_"   + ticketId, "STC Pay")
                .withEmoji(Emoji.fromUnicode("\uD83D\uDCF1"))
        );
    }

    private static String getTypeName(String type) {
        return switch (type) {
            case "tech_support" -> "Technical Support"; case "inquiry" -> "General Inquiry"; case "purchase" -> "Purchase Service";
            case "order_status" -> "Order Status"; case "apply_management" -> "Management App"; case "apply_team" -> "Team App"; default -> "General"; };
    }

    private static Container getWelcomeContainer(String ticketId, String userName, String type, String customBody, ActionRow... rows) {
        String body = switch (type) {
            case "tech_support" -> """
                    \uD83D\uDD27 **TECHNICAL SUPPORT:**
                    \u25B8 Describe the issue in detail.
                    \u25B8 Mention when the issue started.
                    \u25B8 Attach relevant screenshots or files.
                    """;
            case "inquiry" -> """
                    \u2753 **GENERAL INQUIRY:**
                    \u25B8 Specify the department of interest.
                    \u25B8 Detail project scope or pricing query.
                    """;
            case "purchase" -> """
                    \uD83D\uDED2 **PURCHASE REQUEST:**
                    \u25B8 Confirm service allocation.
                    \u25B8 Detail project specifications.
                    """;
            default -> """
                    \uD83D\uDCCC **FEEDBACK/COMPLAINT:**
                    \u25B8 Provide full situational context.
                    """;
        };

        return EmbedUtil.containerBranded("SESSION", "Case #" + ticketId, body + "\n" + customBody, EmbedUtil.BANNER_SUPPORT, Emoji.fromUnicode("\uD83D\uDCC1"), rows);
    }

    public static void claimTicket(TextChannel channel, Member claimer) {
        channel.getManager().setTopic("Dealt with by: " + claimer.getEffectiveName()).queue();
        channel.sendMessageComponents(
            EmbedUtil.containerBranded("NOTICE", "Agent Assigned", "Operative **" + claimer.getEffectiveName() + "** claimed this session.", EmbedUtil.BANNER_SUPPORT, Emoji.fromUnicode("\u2705"),
                ActionRow.of(getTicketButtons("claimed")))
        ).useComponentsV2(true).queue();
    }

    public static void closeTicket(TextChannel channel, Member closer) {
        channel.sendMessageComponents(
            EmbedUtil.containerBranded("CLOSING", "Closure Request", "A request to close this ticket has been made by **" + closer.getEffectiveName() + "**.\nPlease confirm if the service is complete.", EmbedUtil.BANNER_SUPPORT),
            ActionRow.of(
                Button.success("order_status_update_DELIVERED", "Delivered"),
                Button.secondary("order_status_update_CANCELLED", "Cancelled"),
                Button.danger("order_status_update_CLOSED", "Close Ticket")
            )
        ).useComponentsV2(true).queue();
    }

    public static void finalizeClose(TextChannel channel, Member closer, String status) {
        channel.getManager().setTopic("Status: " + status + " | Closed by: " + closer.getEffectiveName()).queue();
        
        JsonObject ticket = SupabaseClient.getTicketByChannel(channel.getId());
        if (ticket != null) {
            String tid = ticket.get("ticket_id").getAsString();
            SupabaseClient.updateTicketStatus(tid, status.toUpperCase(), closer.getEffectiveName());
            
            if (status.equals("DELIVERED") && ticket.has("metadata") && !ticket.get("metadata").isJsonNull()) {
                sendVisualReceipt(channel, ticket.get("metadata").getAsJsonObject());
            }
        }
        
        channel.sendMessageComponents(
            EmbedUtil.containerBranded("ARCHIVE", "Ticket Closed", "Status: **" + status + "**\nThis channel is now archived.", EmbedUtil.BANNER_SUPPORT, Emoji.fromUnicode("\uD83D\uDD12"),
                ActionRow.of(getTicketButtons("closed")))
        ).useComponentsV2(true).queue();

        String ownerId = ticket != null ? ticket.get("user_id").getAsString() : null;
        if (ownerId != null) {
            Member owner = channel.getGuild().getMemberById(ownerId);
            if (owner != null) channel.upsertPermissionOverride(owner).deny(Permission.VIEW_CHANNEL).queue();
        }
    }

    public static void reopenTicket(TextChannel channel, Member reopener) {
        String ownerId = SupabaseClient.getTicketOwner(channel.getId());
        if (ownerId != null) {
            Member owner = channel.getGuild().getMemberById(ownerId);
            if (owner != null) channel.upsertPermissionOverride(owner).grant(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND).queue();
        }
        
        channel.sendMessageComponents(
            EmbedUtil.containerBranded("RESTORED", "Ticket Reopened", "Access to this ticket has been restored by **" + reopener.getEffectiveName() + "**.", EmbedUtil.BANNER_SUPPORT, Emoji.fromUnicode("\uD83D\uDD04"),
                ActionRow.of(getTicketButtons("open")))
        ).useComponentsV2(true).queue();
    }

    public static void sendVisualReceipt(TextChannel channel, JsonObject data) {
        try {
            byte[] receiptData = ReceiptService.generateReceipt(data, channel.getName());
            FileUpload upload = FileUpload.fromData(receiptData, "invoice.png");
            
            Container receipt = Container.of(
                EmbedUtil.v2Header("PAYMENT", "Finance"),
                Separator.createDivider(Separator.Spacing.SMALL),
                MediaGallery.of(MediaGalleryItem.fromFile(upload)),
                TextDisplay.of("**Payment Verified:** Confirmed by Highcore Billing System.\n**Total:** `$" + data.get("total").getAsInt() + "`"),
                EmbedUtil.v2Footer()
            ).withAccentColor(EmbedUtil.SUCCESS.getRGB() & 0xFFFFFF);

            channel.sendMessageComponents(receipt).useComponentsV2(true).queue();
        } catch (Exception e) {
            log.error("Failed to generate/send visual receipt", e);
        }
    }

    public static Container generateV2Receipt(JsonObject data, String receiptUrl) {
        return Container.of(
            EmbedUtil.v2Header("PAYMENT", "Finance"),
            Separator.createDivider(Separator.Spacing.SMALL),
            MediaGallery.of(MediaGalleryItem.fromUrl(receiptUrl)),
            TextDisplay.of("**Payment Verified:** Confirmed by Highcore Billing System.\n**Total:** `$" + data.get("total").getAsInt() + "`"),
            EmbedUtil.v2Footer()
        ).withAccentColor(EmbedUtil.SUCCESS.getRGB() & 0xFFFFFF);
    }

    public static void sendTranscript(TextChannel channel) {
        channel.getIterableHistory().takeAsync(100).thenAccept(msgs -> {
            try {
                File f = new File("transcript-" + channel.getName() + ".txt");
                PrintWriter pw = new PrintWriter(f);
                for (var m : msgs) pw.println("[" + m.getTimeCreated() + "] " + m.getAuthor().getName() + ": " + m.getContentRaw());
                pw.close();
                channel.sendFiles(FileUpload.fromData(f)).queue(s -> f.delete());
            } catch (Exception e) { log.error("Transcript generation failure", e); }
        });
    }
}
