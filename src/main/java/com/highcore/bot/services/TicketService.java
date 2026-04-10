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
    
    private static final String TICKET_CAT_ID        = "1488795131296354460";
    private static final String TRANSCRIPT_CHANNEL_ID = "1488795131019526147";

    public static void createTicket(net.dv8tion.jda.api.interactions.callbacks.IReplyCallback event, String subject, String priority, String type) {
        createTicket(event, subject, priority, type, null);
    }

    public static void createTicket(net.dv8tion.jda.api.interactions.callbacks.IReplyCallback event, String subject, String priority, String type, String details) {
        Guild guild = event.getGuild();
        if (guild == null) return;
        Category cat = guild.getCategoryById(TICKET_CAT_ID);
        if (cat == null) {
            log.warn("Ticket category not found, creating in temporary space.");
            guild.createCategory("Agency Tickets").queue(c -> proceedWithTicket(event, c, subject, priority, type, details));
            return;
        }
        proceedWithTicket(event, cat, subject, priority, type, details);
    }

    private static void proceedWithTicket(net.dv8tion.jda.api.interactions.callbacks.IReplyCallback event, Category cat, String subject, String priority, String type, String details) {
        long timestamp = System.currentTimeMillis();
        String ticketId = String.format("%d", timestamp).substring(9);
        String channelName = type.toLowerCase() + "-" + ticketId;

        cat.createTextChannel(channelName)
            .addPermissionOverride(event.getMember(), EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND, Permission.MESSAGE_ATTACH_FILES), null)
            .addPermissionOverride(cat.getGuild().getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL))
            .queue(channel -> {
                SupabaseClient.createTicket(ticketId, event.getUser().getId(), channel.getId(), type, subject, priority, null);

                String banner = type.equalsIgnoreCase("SUPPORT") ? EmbedUtil.BANNER_SUPPORT : EmbedUtil.BANNER_MAIN;
                String header = type.equalsIgnoreCase("SUPPORT") ? "Logistics Hub" : (type.equalsIgnoreCase("COMPLAINT") ? "Judicial Board" : "Project Records");

                String body = String.format(
                    "Welcome %s to **High Core Agency**.\n\n" +
                    "Our support team will be with you shortly. " +
                    "Please provide all relevant details to help us assist you faster.\n\n" +
                    "**Type:** `%s` | **Priority:** `%s`\n" +
                    "**Subject:** `%s`" +
                    "%s",
                    event.getUser().getAsMention(), type, priority, subject,
                    (details != null && !details.isBlank() ? "\n\n" + details : ""));

                PanelService.reply(channel, EmbedUtil.containerBranded(header, "Active Operations", body, banner, null, ActionRow.of(getTicketButtons("open"))));

                event.getHook().sendMessage("Your ticket has been created: " + channel.getAsMention()).setEphemeral(true).queue();
            });
    }

    public static void createHighEndOrderTicket(Guild guild, User user, String pName, String cName, String contact, String eta, List<InvoiceService.OrderItem> items) {
        Category cat = guild.getCategoryById(TICKET_CAT_ID);
        long timestamp = System.currentTimeMillis();
        String ticketId = String.format("%d", timestamp).substring(9);
        String channelName = "order-" + ticketId;

        guild.createTextChannel(channelName, cat)
            .addPermissionOverride(guild.getMember(user), EnumSet.of(Permission.VIEW_CHANNEL), EnumSet.of(Permission.MESSAGE_SEND)) 
            .queue(channel -> {
                // 1. Welcome Embed (channel is locked until payment)
                String bodyInfo = String.format(
                    "Welcome %s to **High Core Agency**.\n\n" +
                    "⚠️ **This channel is currently locked.**\n" +
                    "You will not be able to send messages or interact with staff " +
                    "until the invoice below has been settled.\n\n" +
                    "**#Case ID:** `%s`\n" +
                    "**Project:** `%s`\n" +
                    "**Client:** `%s`\n" +
                    "**Contact:** `%s`\n" +
                    "**Est. Delivery:** `%s`\n\n" +
                    "Please review the invoice and proceed with payment via the options below.\n" +
                    "Once payment is confirmed, our team will unlock this channel and begin work immediately.",
                    user.getAsMention(), ticketId, pName, cName, contact, eta);

                PanelService.reply(channel, EmbedUtil.containerBranded("Order Pipeline", "Strategic Acquisition", bodyInfo, EmbedUtil.BANNER_ORDER_TIK, null, ActionRow.of(getTicketButtons("open"))));
                
                // 2. Invoice Embed (Image + Pay Buttons) - Professional Split
                byte[] invoiceData = InvoiceService.generateInvoice(cName, pName, items);
                if (invoiceData != null) {
                    PanelService.reply(channel, EmbedUtil.containerBrandedRows("Accounting", "Commercial Invoice", 
                            "يرجى اختيار وسيلة الدفع المناسبة لك من الأزرار أدناه لإتمام عملية الشراء.", 
                            EmbedUtil.BANNER_INVOICE, ActionRow.of(getPaymentButtons(ticketId))));
                    channel.sendFiles(FileUpload.fromData(invoiceData, "invoice.png")).queue();
                } else {
                    PanelService.reply(channel, EmbedUtil.containerBrandedRows("Accounting", "Commercial Invoice", 
                            "يرجى اختيار وسيلة الدفع المناسبة لك من الأزرار أدناه لإتمام عملية الشراء.", 
                            EmbedUtil.BANNER_INVOICE, ActionRow.of(getPaymentButtons(ticketId))));
                }
            });
    }

    public static List<Button> getTicketButtons(String status) {
        if (status.equals("open")) return List.of(
            Button.primary("ticket_claim", "Claim Ticket").withEmoji(Emoji.fromUnicode("\uD83D\uDCE1")),
            Button.danger("ticket_close", "Terminate Session").withEmoji(Emoji.fromUnicode("\uD83D\uDD12"))
        );
        if (status.equals("claimed")) return List.of(Button.danger("ticket_close", "Terminate Session").withEmoji(Emoji.fromUnicode("\uD83D\uDD12")));
        return List.of(
            Button.success("ticket_reopen", "Restabilize").withEmoji(Emoji.fromUnicode("\uD83D\uDD04")),
            Button.danger("ticket_delete", "Delete Ticket").withEmoji(Emoji.fromUnicode("\uD83D\uDDD1\uFE0F"))
        );
    }

    public static List<Button> getPaymentButtons(String id) {
        return List.of(
            Button.secondary("pay_paypal_" + id, "PayPal").withEmoji(Emoji.fromUnicode("\uD83D\uDCB3")),
            Button.secondary("pay_stripe_" + id, "Stripe").withEmoji(Emoji.fromUnicode("\uD83D\uDDA5\uFE0F"))
        );
    }

    public static void claimTicket(TextChannel channel, Member claimer) {
        String body = String.format("تم تولي هذه المهمة من قبل الإداري **%s**. سيقوم بمساعدتك الآن.", claimer.getEffectiveName());
        PanelService.reply(channel, EmbedUtil.containerBranded("Identity Link", "Agent Linked", body, EmbedUtil.BANNER_MAIN, null, ActionRow.of(getTicketButtons("claimed"))));
    }

    public static void reopenTicket(TextChannel channel, Member reopener) {
        String body = String.format("تمت إعادة تنشيط قناة التواصل بواسطة **%s**.", reopener.getEffectiveName());
        PanelService.reply(channel, EmbedUtil.containerBranded("Operational Hub", "Line Restored", body, EmbedUtil.BANNER_MAIN, null, ActionRow.of(getTicketButtons("open"))));
    }

    public static void closeTicket(TextChannel channel, Member closer) {
        String body = String.format("طلب إنهاء الجلسة بواسطة **%s**. يرجى تأكيد البروتوكول المطلوب أدناه.", closer.getEffectiveName());
        PanelService.reply(channel, EmbedUtil.containerBranded("Security Hub", "Closure Sequence", body, EmbedUtil.BANNER_MAIN, null, ActionRow.of(
                   Button.success("order_status_update_TRANSCRIPT", "Transcript").withEmoji(Emoji.fromUnicode("\uD83D\uDCDC")),
                   Button.danger("ticket_delete", "Delete Ticket").withEmoji(Emoji.fromUnicode("\uD83D\uDDD1\uFE0F"))
               )));
    }

    public static void finalizeClose(TextChannel channel, Member closer, String status) {
        if (status.equals("TRANSCRIPT")) {
            TextChannel logCh = channel.getGuild().getTextChannelById(TRANSCRIPT_CHANNEL_ID);
            if (logCh != null) {
                PanelService.reply(logCh, EmbedUtil.containerBranded("Security Archive", "Transcript Extracted", 
                        "تم حفظ سجلات المحادثة الخاصة بالقناة: **" + channel.getName() + "**\nبواسطة: **" + closer.getEffectiveName() + "**", EmbedUtil.BANNER_MAIN));
            }
        }
        String body = "تم إنهاء الجلسة وتأمين قناة التواصل. لإزالة الغرفة نهائياً يرجى الضغط على زر الحذف.";
        PanelService.reply(channel, EmbedUtil.containerBranded("Archive Hub", "Line Secured", body, EmbedUtil.BANNER_MAIN, null, ActionRow.of(getTicketButtons("closed"))));
    }
}
