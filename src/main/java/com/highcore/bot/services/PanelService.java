package com.highcore.bot.services;

import com.highcore.bot.utils.EmbedUtil;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.components.MessageTopLevelComponent;
import net.dv8tion.jda.api.components.container.Container;
import java.util.*;

public class PanelService {

    /**
     * Unified response handler for JDA 6 Components V2.
     * Automatically handles replies, edits, and standalone channel messages.
     */
    public static void reply(Object interaction, Object content, ActionRow... rows) {
        reply(interaction, content, false, rows);
    }

    public static void reply(Object interaction, Object content, boolean ephemeral, ActionRow... rows) {
        List<MessageTopLevelComponent> components = new ArrayList<>();
        MessageEmbed embed = null;
        boolean useV2 = false;

        if (content instanceof Container c) {
            components.add(c);
            useV2 = true;
        } else if (content instanceof MessageEmbed me) {
            embed = me;
        }

        if (rows != null) {
            for (ActionRow row : rows) {
                if (row != null) components.add(row);
            }
        }

        if (interaction instanceof IMessageEditCallback editCallback && !((IReplyCallback)interaction).isAcknowledged()) {
            // For component interactions, we usually want to EDIT the message
            var edit = editCallback.editMessage("");
            if (embed != null) edit.setEmbeds(embed);
            edit.setComponents(components);
            if (useV2) edit.useComponentsV2(true);
            edit.queue();
        } else if (interaction instanceof IReplyCallback replyCallback) {
            if (replyCallback.isAcknowledged()) {
                var hook = replyCallback.getHook().editOriginal("");
                if (embed != null) hook.setEmbeds(embed);
                hook.setComponents(components);
                if (useV2) hook.useComponentsV2(true);
                hook.queue();
            } else {
                var replier = replyCallback.reply("").setEphemeral(ephemeral);
                if (embed != null) replier.setEmbeds(embed);
                replier.setComponents(components);
                if (useV2) replier.useComponentsV2(true);
                replier.queue();
            }
        } else if (interaction instanceof net.dv8tion.jda.api.entities.channel.middleman.MessageChannel channel) {
            var sender = channel.sendMessage("");
            if (embed != null) sender.setEmbeds(embed);
            sender.setComponents(components);
            if (useV2) sender.useComponentsV2(true);
            sender.queue();
        }
    }

    // ========== COMPONENT GENERATORS ==========

    public static ActionRow[] getTicketComponents() {
        return new ActionRow[]{ActionRow.of(
            StringSelectMenu.create("ticket_type_select")
                .setPlaceholder("Select a secure support category...")
                .addOption("Order / Purchase", "purchase", "Request a new agency service", Emoji.fromUnicode("\uD83D\uDED2"))
                .addOption("Technical Support", "tech_support", "Hardware, software, connectivity", Emoji.fromUnicode("\uD83D\uDD27"))
                .addOption("Complaint / Report", "complaint", "Report an issue or violation", Emoji.fromUnicode("\u26A0\uFE0F"))
                .build()
        ), ActionRow.of(Button.secondary("menu_main", "Return to Hub").withEmoji(Emoji.fromUnicode("\u2B05\uFE0F")))};
    }

    public static ActionRow[] getMainMenuComponents() {
        return new ActionRow[]{ActionRow.of(
            Button.success("menu_tickets", "Tickets").withEmoji(Emoji.fromUnicode("\uD83C\uDFAB")),
            Button.primary("menu_services", "Services").withEmoji(Emoji.fromUnicode("\uD83D\uDED2")),
            Button.secondary("menu_points", "Merit Hub").withEmoji(Emoji.fromUnicode("\u2B50")),
            Button.secondary("menu_stats", "Network Stats").withEmoji(Emoji.fromUnicode("\uD83D\uDCCA"))
        )};
    }

    // ========== PANEL SENDING METHODS ==========

    public static void sendMainMenu(Object target) {
        reply(target, EmbedUtil.mainMenu(), getMainMenuComponents());
    }

    public static void sendTicketPanel(Object target) {
        reply(target, EmbedUtil.ticketPanel(), getTicketComponents());
    }

    public static void sendServicesPanel(Object target) {
        reply(target, EmbedUtil.services(), ActionRow.of(Button.secondary("menu_main", "Return to Hub").withEmoji(Emoji.fromUnicode("\u2B05\uFE0F"))));
    }

    public static void sendTeamPanel(Object target) {
        reply(target, EmbedUtil.team(), ActionRow.of(Button.secondary("menu_main", "Return to Hub").withEmoji(Emoji.fromUnicode("\u2B05\uFE0F"))));
    }

    public static void sendPointsPanel(Object target) {
        reply(target, EmbedUtil.pointsPanel(), ActionRow.of(
            Button.primary("points_check", "Merit Audit").withEmoji(Emoji.fromUnicode("\uD83D\uDCCA")),
            Button.secondary("menu_main", "Return to Hub").withEmoji(Emoji.fromUnicode("\u2B05\uFE0F"))
        ));
    }

    public static void sendGiveawayPanel(Object target) {
        reply(target, EmbedUtil.giveawayPanel(), ActionRow.of(
            Button.success("gw_create", "Create").withEmoji(Emoji.fromUnicode("\uD83C\uDF81")),
            Button.secondary("menu_main", "Return to Hub").withEmoji(Emoji.fromUnicode("\u2B05\uFE0F"))
        ));
    }

    public static void sendStartupPanel(Object target) {
        reply(target, EmbedUtil.startupPanel(), ActionRow.of(
            Button.primary("menu_services", "Services & Prices").withEmoji(Emoji.fromUnicode("\uD83D\uDED2")),
            Button.success("order_start", "Project Request").withEmoji(Emoji.fromUnicode("\uD83D\uDCC4")),
            Button.secondary("view_rules", "Rules").withEmoji(Emoji.fromUnicode("\uD83D\uDCDC"))
        ));
    }

    public static void sendStatsPanel(Object target) {
        reply(target, EmbedUtil.stats(0, 0, 0, 0, "N/A"), ActionRow.of(Button.secondary("menu_main", "Return to Hub").withEmoji(Emoji.fromUnicode("\u2B05\uFE0F"))));
    }
}
