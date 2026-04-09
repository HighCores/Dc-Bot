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
import net.dv8tion.jda.api.utils.FileUpload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;

public class TicketService {
    private static final Logger log = LoggerFactory.getLogger(TicketService.class);
    
    private static final String TICKET_CAT_ID = "1488795130881249404";
    private static final String TRANSCRIPT_CHANNEL_ID = "1488795131019526147";

    public static void createTicket(net.dv8tion.jda.api.interactions.callbacks.IReplyCallback event, String subject, String priority, String type) {
        Guild guild = event.getGuild();
        if (guild == null) return;
        
        Category cat = guild.getCategoryById(TICKET_CAT_ID);
        if (cat == null) {
            log.warn("Elite category not found. Creating fallback.");
            guild.createCategory("Tickets").queue(c -> proceedWithTicket(event, c, subject, priority, type));
            return;
        }
        proceedWithTicket(event, cat, subject, priority, type);
    }

    private static void proceedWithTicket(net.dv8tion.jda.api.interactions.callbacks.IReplyCallback event, Category cat, String subject, String priority, String type) {
        String ticketId = String.format("%04d", new Random().nextInt(10000));
        String channelName = type.toLowerCase() + "-" + ticketId;

        cat.createTextChannel(channelName)
            .addPermissionOverride(event.getMember(), EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND, Permission.MESSAGE_ATTACH_FILES), null)
            .addPermissionOverride(cat.getGuild().getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL))
            .queue(channel -> {
                SupabaseClient.createTicket(ticketId, event.getUser().getId(), channel.getId(), type, subject, priority, null);
                
                String welcome = "Welcome " + event.getUser().getAsMention() + ", our team will be with you shortly regarding: **" + subject + "**";
                channel.sendMessage(welcome)
                       .setComponents(EmbedUtil.eliteContainer("Support Ticket", "Active session created for your request.", EmbedUtil.BANNER_SUPPORT, ActionRow.of(getTicketButtons("open"))))
                       .queue();

                if (event.isAcknowledged()) {
                    event.getHook().sendMessage("Success! Your ticket is ready: " + channel.getAsMention()).setEphemeral(true).queue();
                } else {
                    event.reply("Success! Your ticket is ready: " + channel.getAsMention()).setEphemeral(true).queue();
                }
            });
    }

    public static void createHighEndOrderTicket(Guild guild, User user, String pName, String cName, String contact, String eta, List<InvoiceService.OrderItem> items) {
        Category cat = guild.getCategoryById(TICKET_CAT_ID);
        String ticketId = String.format("%04d", new Random().nextInt(10000));
        String channelName = "order-" + ticketId;

        guild.createTextChannel(channelName, cat)
            .addPermissionOverride(guild.getMember(user), EnumSet.of(Permission.VIEW_CHANNEL), EnumSet.of(Permission.MESSAGE_SEND)) 
            .queue(channel -> {
                // Message 1: Project Details
                String welcome = "Hello " + user.getAsMention() + ",\n\n" +
                                "Project: **" + pName + "**\nEstimated delivery: **" + eta + "**\n\n" +
                                "*Note: This chat will remain locked until the payment below is verified.*";
                
                channel.sendMessage(welcome)
                       .setComponents(EmbedUtil.eliteContainer("Project Request", "Session created. Awaiting payment to initialize development.", EmbedUtil.BANNER_ORDER_TIK, ActionRow.of(getTicketButtons("open"))))
                       .queue(m -> {
                            // Message 2: Invoice & Payment
                            byte[] invoiceData = InvoiceService.generateInvoice(cName, pName, items);
                            net.dv8tion.jda.api.utils.messages.MessageCreateBuilder m2 = new net.dv8tion.jda.api.utils.messages.MessageCreateBuilder();
                            m2.addContent("### Invoice details");
                            if (invoiceData != null) m2.addFiles(FileUpload.fromData(invoiceData, "invoice.png"));
                            m2.setComponents(ActionRow.of(getPaymentButtons(ticketId)));
                            channel.sendMessage(m2.build()).queue();
                       });
            });
    }

    public static List<Button> getTicketButtons(String status) {
        List<Button> buttons = new ArrayList<>();
        if (status.equals("open")) {
            buttons.add(Button.primary("ticket_claim", "Claim Ticket"));
            buttons.add(Button.danger("ticket_close", "Close Ticket"));
        } else if (status.equals("claimed")) {
            buttons.add(Button.danger("ticket_close", "Close Ticket"));
        } else if (status.equals("closed")) {
            buttons.add(Button.success("ticket_reopen", "Reopen Ticket"));
            buttons.add(Button.danger("ticket_delete", "Delete Ticket"));
        }
        return buttons;
    }

    public static List<Button> getPaymentButtons(String ticketId) {
        return List.of(
            Button.secondary("pay_paypal_" + ticketId, "Paypal"),
            Button.secondary("pay_stripe_" + ticketId, "Stripe"),
            Button.secondary("pay_local_" + ticketId, "Local transfer")
        );
    }

    public static void claimTicket(TextChannel channel, Member claimer) {
        channel.getManager().setTopic("Handled by: " + claimer.getEffectiveName()).queue();
        channel.sendMessage("Welcome! **" + claimer.getEffectiveName() + "** has joined the chat and will assist you shortly.")
               .setComponents(ActionRow.of(getTicketButtons("claimed")))
               .queue();
    }

    public static void closeTicket(TextChannel channel, Member closer) {
        channel.sendMessage("Preparation for closure finalized by **" + closer.getEffectiveName() + "**. Please confirm the next action:")
               .setComponents(ActionRow.of(
                   Button.success("order_status_update_TRANSCRIPT", "Transcript"),
                   Button.danger("ticket_delete", "Delete Ticket")
               )).queue();
    }

    public static void finalizeClose(TextChannel channel, Member closer, String status) {
        if (status.equals("TRANSCRIPT")) {
            TextChannel log = channel.getGuild().getTextChannelById(TRANSCRIPT_CHANNEL_ID);
            if (log != null) {
                log.sendMessage("Archive recorded: **" + channel.getName() + "** | Operator: " + closer.getEffectiveName()).queue();
            }
        }
        
        channel.getManager().setTopic("Status: Closed").queue();
        channel.sendMessage("### Ticket finalized\nThe session is now archived.")
               .setComponents(ActionRow.of(getTicketButtons("closed")))
               .queue();

        JsonObject ticket = SupabaseClient.getTicketByChannel(channel.getId());
        if (ticket != null) {
            String uid = ticket.get("user_id").getAsString();
            Member owner = channel.getGuild().getMemberById(uid);
            if (owner != null) channel.upsertPermissionOverride(owner).deny(Permission.VIEW_CHANNEL).queue();
        }
    }

    public static void reopenTicket(TextChannel channel, Member reopener) {
        JsonObject ticket = SupabaseClient.getTicketByChannel(channel.getId());
        if (ticket != null) {
            String uid = ticket.get("user_id").getAsString();
            Member owner = channel.getGuild().getMemberById(uid);
            if (owner != null) channel.upsertPermissionOverride(owner).grant(Permission.VIEW_CHANNEL).queue();
        }
        channel.sendMessage("Ticket reopened by **" + reopener.getEffectiveName() + "**. Communication restored.")
               .setComponents(ActionRow.of(getTicketButtons("open")))
               .queue();
    }

    public static void sendVisualReceipt(TextChannel channel, JsonObject data) {
        try {
            byte[] receipt = ReceiptService.generateReceipt(data, channel.getName());
            channel.sendFiles(FileUpload.fromData(receipt, "invoice.png")).queue();
            channel.sendMessage("Payment confirmed. Your project is now being initialized.").queue();
        } catch (Exception e) { log.error("Receipt failure", e); }
    }
}
