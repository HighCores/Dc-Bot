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

    public static void createTicket(net.dv8tion.jda.api.interactions.callbacks.IReplyCallback event, String subject, String priority, String type) {
        Guild guild = event.getGuild();
        if (guild == null) return;
        
        getOrCreateCategory(guild).queue(cat -> {
            String ticketId = String.format("%04d", new Random().nextInt(10000));
            String channelName = type + "-" + ticketId;

            guild.createTextChannel(channelName, cat)
                .addPermissionOverride(event.getMember(), EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND, Permission.MESSAGE_ATTACH_FILES), null)
                .addPermissionOverride(guild.getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL))
                .queue(channel -> {
                    SupabaseClient.createTicket(ticketId, event.getUser().getId(), channel.getId(), type, subject, priority, null);
                    finalizeTicket(channel, event.getUser(), ticketId, type, subject, priority);
                    if (event.isAcknowledged()) {
                        event.getHook().sendMessage("Terminal session established: " + channel.getAsMention()).setEphemeral(true).queue();
                    } else {
                        event.reply("Terminal session established: " + channel.getAsMention()).setEphemeral(true).queue();
                    }
                });
        });
    }

    private static net.dv8tion.jda.api.requests.RestAction<Category> getOrCreateCategory(Guild guild) {
        Category cat = guild.getCategoriesByName("TICKETS", true).stream().findFirst().orElse(null);
        if (cat != null) return new net.dv8tion.jda.internal.requests.CompletedRestAction<>(guild.getJDA(), cat);
        return guild.createCategory("TICKETS");
    }

    public static void createOrderTicket(Guild guild, User user, String type, int total, String wizId, JsonObject orderData) {
        Member member = guild.getMember(user);
        if (member == null) return;
        
        getOrCreateCategory(guild).queue(cat -> {
            String ticketId = String.format("%04d", new Random().nextInt(10000));
            String channelName = "order-" + ticketId;

            guild.createTextChannel(channelName, cat)
                .addPermissionOverride(member, EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND, Permission.MESSAGE_ATTACH_FILES), null)
                .addPermissionOverride(guild.getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL))
                .queue(channel -> {
                    SupabaseClient.createTicket(ticketId, user.getId(), channel.getId(), type, "Project Order", "High", orderData);
                    String summary = "Total: **$" + total + "**\nWizard Session: **" + wizId + "**";
                    
                    net.dv8tion.jda.api.utils.messages.MessageCreateBuilder mcb = new net.dv8tion.jda.api.utils.messages.MessageCreateBuilder();
                    mcb.setComponents(getWelcomeContainer(ticketId, user.getName(), type, summary, 
                             ActionRow.of(getTicketButtons("open")), 
                             ActionRow.of(getPaymentButtons(ticketId))));
                    mcb.useComponentsV2(true);
                    channel.sendMessage(mcb.build()).queue();
                });
        });
    }

    private static void finalizeTicket(TextChannel channel, User user, String ticketId, String type, String subject, String priority) {
        String body = "Subject: **" + subject + "**\nPriority: **" + priority + "**";
        net.dv8tion.jda.api.utils.messages.MessageCreateBuilder mcb = new net.dv8tion.jda.api.utils.messages.MessageCreateBuilder();
        mcb.setComponents(getWelcomeContainer(ticketId, user.getName(), type, body, ActionRow.of(getTicketButtons("open"))));
        mcb.useComponentsV2(true);
        channel.sendMessage(mcb.build()).queue();
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
        net.dv8tion.jda.api.utils.messages.MessageCreateBuilder mcb = new net.dv8tion.jda.api.utils.messages.MessageCreateBuilder();
        mcb.setComponents(EmbedUtil.containerBranded("CLAIM", "Agent Notified", "Operative **" + claimer.getEffectiveName() + "** has entered the session.", EmbedUtil.BANNER_SUPPORT, Emoji.fromUnicode("\u2705"),
                ActionRow.of(getTicketButtons("claimed"))));
        mcb.useComponentsV2(true);
        channel.sendMessage(mcb.build()).queue();
    }

    public static void closeTicket(TextChannel channel, Member closer) {
        net.dv8tion.jda.api.utils.messages.MessageCreateBuilder mcb = new net.dv8tion.jda.api.utils.messages.MessageCreateBuilder();
        mcb.setComponents(
            EmbedUtil.containerBranded("LOGISTICS", "Closure Sequence", "End session request initialized by **" + closer.getEffectiveName() + "**.\nConfirm project status below.", EmbedUtil.BANNER_SUPPORT),
            ActionRow.of(
                Button.success("order_status_update_DELIVERED", "Delivered"),
                Button.secondary("order_status_update_CANCELLED", "Cancelled"),
                Button.danger("order_status_update_CLOSED", "Terminate Session")
            )
        );
        mcb.useComponentsV2(true);
        channel.sendMessage(mcb.build()).queue();
    }

    public static void finalizeClose(TextChannel channel, Member closer, String status) {
        channel.getManager().setTopic("Status: " + status + " | Operator: " + closer.getEffectiveName()).queue();
        JsonObject ticket = SupabaseClient.getTicketByChannel(channel.getId());
        if (ticket != null) {
            String tid = ticket.get("ticket_id").getAsString();
            SupabaseClient.updateTicketStatus(tid, status.toUpperCase(), closer.getEffectiveName());
        }
        
        net.dv8tion.jda.api.utils.messages.MessageCreateBuilder mcb = new net.dv8tion.jda.api.utils.messages.MessageCreateBuilder();
        mcb.setComponents(EmbedUtil.containerBranded("ARCHIVE", "Session Finalized", "Status: **" + status + "**\nThis channel is now locked.", EmbedUtil.BANNER_SUPPORT, Emoji.fromUnicode("\uD83D\uDD12"),
                ActionRow.of(getTicketButtons("closed"))));
        mcb.useComponentsV2(true);
        channel.sendMessage(mcb.build()).queue();

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
        net.dv8tion.jda.api.utils.messages.MessageCreateBuilder mcb = new net.dv8tion.jda.api.utils.messages.MessageCreateBuilder();
        mcb.setComponents(EmbedUtil.containerBranded("RESTORED", "Session Reopened", "Access to this node has been restored by **" + reopener.getEffectiveName() + "**.", EmbedUtil.BANNER_SUPPORT, Emoji.fromUnicode("\uD83D\uDD04"),
                ActionRow.of(getTicketButtons("open"))));
        mcb.useComponentsV2(true);
        channel.sendMessage(mcb.build()).queue();
    }

    public static void createHighEndOrderTicket(Guild guild, User user, String pName, String cName, String contact, String eta, List<InvoiceService.OrderItem> items) {
        Member member = guild.getMember(user);
        if (member == null) return;

        getOrCreateCategory(guild).queue(cat -> {
            String ticketId = String.format("%04d", new Random().nextInt(10000));
            String channelName = "order-" + ticketId;

            guild.createTextChannel(channelName, cat)
                .addPermissionOverride(guild.getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL))
                .addPermissionOverride(member, EnumSet.of(Permission.VIEW_CHANNEL), EnumSet.of(Permission.MESSAGE_SEND)) // LOCKED UNTIL PAYMENT
                .queue(channel -> {
                    String summary = "Project: **" + pName + "**\nETA: **" + eta + "**\n\n**STATUS: PENDING PAYMENT**\nChat is disabled until the transaction is verified.";
                    
                    net.dv8tion.jda.api.utils.messages.MessageCreateBuilder mcb = new net.dv8tion.jda.api.utils.messages.MessageCreateBuilder();
                    mcb.setComponents(getWelcomeContainer(ticketId, user.getName(), "order", summary, 
                        ActionRow.of(getTicketButtons("open")), 
                        ActionRow.of(getPaymentButtons(ticketId))));
                    mcb.useComponentsV2(true);
                    channel.sendMessage(mcb.build()).queue();

                    // Generate and Send Invoice
                    byte[] invoiceImg = InvoiceService.generateInvoice(cName, pName, items);
                    if (invoiceImg != null) {
                        channel.sendFiles(FileUpload.fromData(invoiceImg, "invoice.png")).queue();
                    }
                });
        });
    }

    public static void sendVisualReceipt(TextChannel channel, JsonObject data) {
        try {
            byte[] receiptData = ReceiptService.generateReceipt(data, channel.getName());
            FileUpload upload = FileUpload.fromData(receiptData, "invoice.png");
            net.dv8tion.jda.api.utils.messages.MessageCreateBuilder mcb = new net.dv8tion.jda.api.utils.messages.MessageCreateBuilder();
            mcb.setComponents(EmbedUtil.containerBranded("FINANCE", "Payment Protocol Verified", "Total: **$" + data.get("total").getAsInt() + "**\nReceipt attached below.", EmbedUtil.BANNER_GIVEAWAY, Emoji.fromUnicode("\uD83D\uDCB3")));
            mcb.addFiles(upload);
            mcb.useComponentsV2(true);
            channel.sendMessage(mcb.build()).queue();
        } catch (Exception e) { log.error("Receipt failure", e); }
    }
}
