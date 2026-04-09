package com.highcore.bot.services;

import com.google.gson.JsonObject;
import com.highcore.bot.database.SupabaseClient;
import com.highcore.bot.utils.EmbedUtil;
import net.dv8tion.jda.api.EmbedBuilder;
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
    
    private static final String TICKET_CAT_ID = "1488795130881249404";
    private static final String TRANSCRIPT_CHANNEL_ID = "1488795131019526147";

    public static void createTicket(net.dv8tion.jda.api.interactions.callbacks.IReplyCallback event, String subject, String priority, String type) {
        Guild guild = event.getGuild();
        if (guild == null) return;
        Category cat = guild.getCategoryById(TICKET_CAT_ID);
        if (cat == null) {
            guild.createCategory("Agency Tickets").queue(c -> proceedWithTicket(event, c, subject, priority, type));
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
                
                net.dv8tion.jda.api.entities.MessageEmbed banner = new EmbedBuilder().setImage(EmbedUtil.BANNER_SUPPORT).setColor(EmbedUtil.ACCENT).build();
                channel.sendMessageEmbeds(banner).queue();

                net.dv8tion.jda.api.utils.messages.MessageCreateBuilder mcb = new net.dv8tion.jda.api.utils.messages.MessageCreateBuilder()
                    .setComponents(EmbedUtil.eliteContainer("Active Session", "Communication line initialized.", null, ActionRow.of(getTicketButtons("open"))))
                    .useComponentsV2(true);
                
                channel.sendMessage(mcb.build()).queue();
                event.getHook().sendMessage("Success! Your session is active: " + channel.getAsMention()).setEphemeral(true).queue();
            });
    }

    public static void createHighEndOrderTicket(Guild guild, User user, String pName, String cName, String contact, String eta, List<InvoiceService.OrderItem> items) {
        Category cat = guild.getCategoryById(TICKET_CAT_ID);
        String ticketId = String.format("%04d", new Random().nextInt(10000));
        String channelName = "order-" + ticketId;

        guild.createTextChannel(channelName, cat)
            .addPermissionOverride(guild.getMember(user), EnumSet.of(Permission.VIEW_CHANNEL), EnumSet.of(Permission.MESSAGE_SEND)) 
            .queue(channel -> {
                net.dv8tion.jda.api.entities.MessageEmbed banner = new EmbedBuilder().setImage(EmbedUtil.BANNER_ORDER_TIK).setColor(EmbedUtil.ACCENT).build();
                channel.sendMessageEmbeds(banner).queue();

                net.dv8tion.jda.api.utils.messages.MessageCreateBuilder mcb = new net.dv8tion.jda.api.utils.messages.MessageCreateBuilder()
                    .setComponents(EmbedUtil.eliteContainer("Project Initiation", "Session created. Awaiting payment settlement.", null, ActionRow.of(getTicketButtons("open"))))
                    .useComponentsV2(true);
                
                channel.sendMessage(mcb.build()).queue(m -> {
                    byte[] invoiceData = InvoiceService.generateInvoice(cName, pName, items);
                    net.dv8tion.jda.api.utils.messages.MessageCreateBuilder m2 = new net.dv8tion.jda.api.utils.messages.MessageCreateBuilder()
                        .setContent("### Official Invoice")
                        .setComponents(ActionRow.of(getPaymentButtons(ticketId)))
                        .useComponentsV2(true);
                    if (invoiceData != null) m2.addFiles(FileUpload.fromData(invoiceData, "invoice.png"));
                    channel.sendMessage(m2.build()).queue();
                });
            });
    }

    public static List<Button> getTicketButtons(String status) {
        if (status.equals("open")) return List.of(
            Button.primary("ticket_claim", "Claim").withEmoji(Emoji.fromUnicode("\uD83D\uDCE1")),
            Button.danger("ticket_close", "Close").withEmoji(Emoji.fromUnicode("\uD83D\uDD12"))
        );
        if (status.equals("claimed")) return List.of(Button.danger("ticket_close", "Close").withEmoji(Emoji.fromUnicode("\uD83D\uDD12")));
        return List.of(
            Button.success("ticket_reopen", "Reopen").withEmoji(Emoji.fromUnicode("\uD83D\uDD04")),
            Button.danger("ticket_delete", "Delete").withEmoji(Emoji.fromUnicode("\uD83D\uDDD1\uFE0F"))
        );
    }

    public static List<Button> getPaymentButtons(String id) {
        return List.of(
            Button.secondary("pay_paypal_" + id, "PayPal").withEmoji(Emoji.fromUnicode("\uD83D\uDCB3")),
            Button.secondary("pay_stripe_" + id, "Stripe").withEmoji(Emoji.fromUnicode("\uD83D\uDDA5\uFE0F"))
        );
    }

    public static void claimTicket(TextChannel channel, Member claimer) {
        channel.sendMessage("### Agent Notification\n**" + claimer.getEffectiveName() + "** has entered the session.")
               .setComponents(ActionRow.of(getTicketButtons("claimed")))
               .queue();
    }

    public static void reopenTicket(TextChannel channel, Member reopener) {
        channel.sendMessage("### Session Restored\nReactivated by **" + reopener.getEffectiveName() + "**.")
               .setComponents(ActionRow.of(getTicketButtons("open")))
               .queue();
    }

    public static void closeTicket(TextChannel channel, Member closer) {
        channel.sendMessage("Closure initialized by **" + closer.getEffectiveName() + "**. Select protocol:")
               .setComponents(ActionRow.of(
                   Button.success("order_status_update_TRANSCRIPT", "Transcript").withEmoji(Emoji.fromUnicode("\uD83D\uDCDC")),
                   Button.danger("ticket_delete", "Delete Ticket").withEmoji(Emoji.fromUnicode("\uD83D\uDDD1\uFE0F"))
               )).queue();
    }

    public static void finalizeClose(TextChannel channel, Member closer, String status) {
        if (status.equals("TRANSCRIPT")) {
            TextChannel log = channel.getGuild().getTextChannelById(TRANSCRIPT_CHANNEL_ID);
            if (log != null) log.sendMessage("Archive Recorded: **" + channel.getName() + "**").queue();
        }
        channel.sendMessage("### Ticket Archived\nCommunication line secured.")
               .setComponents(ActionRow.of(getTicketButtons("closed")))
               .queue();
    }
}
