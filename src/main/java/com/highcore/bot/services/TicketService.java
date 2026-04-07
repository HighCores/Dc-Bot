package com.highcore.bot.services;

import com.google.gson.JsonObject;
import com.highcore.bot.database.SupabaseClient;
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
        
        String ticketId = String.format("%04d", new Random().nextInt(10000));
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

    public static void createOrderTicket(Guild guild, User user, String type, int total, String wizId, JsonObject orderData) {
        Member member = guild.getMember(user);
        if (member == null) return;
        
        String ticketId = String.format("%04d", new Random().nextInt(10000));
        String channelName = "order-" + ticketId;
        Category cat = guild.getCategoriesByName("TICKETS", true).stream().findFirst().orElse(null);
        if (cat == null) return;

        guild.createTextChannel(channelName, cat)
                .addPermissionOverride(member, EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND, Permission.MESSAGE_ATTACH_FILES), null)
                .addPermissionOverride(guild.getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL))
                .queue(channel -> {
                    SupabaseClient.createTicket(ticketId, user.getId(), channel.getId(), type, "Project Order", "High", orderData);
                    String summary = "Total: **$" + total + "**\nWizard Session: **" + wizId + "**";
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
            buttons.add(Button.danger("ticket_delete", "Delete Channel \u2623\uFE0F"));
        }
        return buttons;
    }

    private static List<Button> getPaymentButtons(String ticketId) {
        return List.of(
            Button.secondary("pay_paypal_" + ticketId, "PayPal Account"),
            Button.secondary("pay_stripe_" + ticketId, "Stripe Portal"),
            Button.secondary("pay_local_" + ticketId, "Manual Gateway")
        );
    }

    private static Container getWelcomeContainer(String ticketId, String userName, String type, String customBody, ActionRow... rows) {
        String body = "Operative: " + userName + "\nCase: #" + ticketId + "\n\n" + customBody;
        return EmbedUtil.containerBranded("TICKET", "Support Session Created", body, EmbedUtil.BANNER_SUPPORT, Emoji.fromUnicode("\uD83D\uDCC1"), rows);
    }

    public static void claimTicket(TextChannel channel, Member claimer) {
        channel.getManager().setTopic("Assigned: " + claimer.getEffectiveName()).queue();
        channel.sendMessageComponents(
            EmbedUtil.containerBranded("CLAIM", "Agent Notified", "Operative **" + claimer.getEffectiveName() + "** has entered the session.", EmbedUtil.BANNER_SUPPORT, Emoji.fromUnicode("\u2705"),
                ActionRow.of(getTicketButtons("claimed")))
        ).useComponentsV2(true).queue();
    }

    public static void closeTicket(TextChannel channel, Member closer) {
        channel.sendMessageComponents(
            EmbedUtil.containerBranded("LOGISTICS", "Closure Sequence", "End session request initialized by **" + closer.getEffectiveName() + "**.\nConfirm project status below.", EmbedUtil.BANNER_SUPPORT),
            ActionRow.of(
                Button.success("order_status_update_DELIVERED", "Delivered"),
                Button.secondary("order_status_update_CANCELLED", "Cancelled"),
                Button.danger("order_status_update_CLOSED", "Terminate Session")
            )
        ).useComponentsV2(true).queue();
    }

    public static void finalizeClose(TextChannel channel, Member closer, String status) {
        channel.getManager().setTopic("Status: " + status + " | Operator: " + closer.getEffectiveName()).queue();
        JsonObject ticket = SupabaseClient.getTicketByChannel(channel.getId());
        if (ticket != null) {
            String tid = ticket.get("ticket_id").getAsString();
            SupabaseClient.updateTicketStatus(tid, status.toUpperCase(), closer.getEffectiveName());
        }
        channel.sendMessageComponents(
            EmbedUtil.containerBranded("ARCHIVE", "Session Finalized", "Status: **" + status + "**\nThis channel is now locked.", EmbedUtil.BANNER_SUPPORT, Emoji.fromUnicode("\uD83D\uDD12"),
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
            EmbedUtil.containerBranded("RESTORED", "Session Reopened", "Access to this node has been restored by **" + reopener.getEffectiveName() + "**.", EmbedUtil.BANNER_SUPPORT, Emoji.fromUnicode("\uD83D\uDD04"),
                ActionRow.of(getTicketButtons("open")))
        ).useComponentsV2(true).queue();
    }

    public static void sendVisualReceipt(TextChannel channel, JsonObject data) {
        try {
            byte[] receiptData = ReceiptService.generateReceipt(data, channel.getName());
            FileUpload upload = FileUpload.fromData(receiptData, "invoice.png");
            channel.sendMessageComponents(
                EmbedUtil.containerBranded("FINANCE", "Payment Protocol Verified", "Total: **$" + data.get("total").getAsInt() + "**\nReceipt attached below.", EmbedUtil.BANNER_GIVEAWAY, Emoji.fromUnicode("\uD83D\uDCB3"))
            ).addFiles(upload).useComponentsV2(true).queue();
        } catch (Exception e) { log.error("Receipt failure", e); }
    }
}
