package com.highcore.bot.services;

import com.highcore.bot.database.SupabaseClient;
import com.highcore.bot.utils.EmbedUtil;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.components.MessageTopLevelComponent;
import net.dv8tion.jda.api.components.container.Container;
import java.util.*;

public class PanelService {

    public static void reply(Object interaction, Object content) { handleReply(interaction, content, false); }
    public static void replyEphemeral(Object interaction, Object content) { handleReply(interaction, content, true); }

    private static void handleReply(Object interaction, Object content, boolean ephemeral) {
        List<MessageTopLevelComponent> components = new ArrayList<>();
        MessageEmbed embed = null;
        if (content instanceof Container c) components.add(c);
        else if (content instanceof MessageEmbed me) embed = me;

        if (interaction instanceof ModalInteractionEvent modalEvent) {
            if (!modalEvent.isAcknowledged()) modalEvent.deferReply(ephemeral).queue();
            var hook = modalEvent.getHook().editOriginal("");
            if (embed != null) hook.setEmbeds(embed);
            hook.setComponents(components);
            hook.useComponentsV2(true);
            hook.queue();
        } else if (interaction instanceof IMessageEditCallback editCallback && !((IReplyCallback)interaction).isAcknowledged()) {
            var edit = editCallback.editMessage("");
            if (embed != null) edit.setEmbeds(embed);
            edit.setComponents(components);
            edit.useComponentsV2(true);
            edit.queue();
        } else if (interaction instanceof IReplyCallback replyCallback) {
            if (replyCallback.isAcknowledged()) {
                var hook = replyCallback.getHook().editOriginal("");
                if (embed != null) hook.setEmbeds(embed);
                hook.setComponents(components);
                hook.useComponentsV2(true);
                hook.queue();
            } else {
                var replier = replyCallback.reply("").setEphemeral(ephemeral);
                if (embed != null) replier.setEmbeds(embed);
                replier.setComponents(components);
                replier.useComponentsV2(true);
                replier.queue();
            }
        } else if (interaction instanceof net.dv8tion.jda.api.entities.channel.middleman.MessageChannel channel) {
            var sender = channel.sendMessage("");
            if (embed != null) sender.setEmbeds(embed);
            sender.setComponents(components);
            sender.useComponentsV2(true);
            sender.queue();
        }
    }

    public static void sendStartupHub(Object target) {
        String body = "### \u25C8 Welcome Operative\nHighcore Agency is a premier collective delivering high-fidelity digital solutions.\nInitialize your session via the modules below.";
        ActionRow row1 = ActionRow.of(Button.secondary("hub_map", "Server Map").withEmoji(Emoji.fromUnicode("\uD83D\uDDFA\uFE0F")), Button.primary("hub_services", "Our Services").withEmoji(Emoji.fromUnicode("\uD83D\uDED2")), Button.primary("hub_prices", "Our Prices").withEmoji(Emoji.fromUnicode("\uD83D\uDCA2")));
        ActionRow row2 = ActionRow.of(Button.success("hub_stats", "Statistics").withEmoji(Emoji.fromUnicode("\uD83D\uDCCA")), Button.secondary("hub_colors", "Identity Colors").withEmoji(Emoji.fromUnicode("\uD83C\uDFA8")), Button.secondary("hub_rules", "Server Rules").withEmoji(Emoji.fromUnicode("\uD83D\uDCDC")));
        ActionRow row3 = ActionRow.of(Button.secondary("hub_social", "Social Media").withEmoji(Emoji.fromUnicode("\uD83D\uDDA5\uFE0F")), Button.success("order_start", "Start Order").withEmoji(Emoji.fromUnicode("\uD83D\uDCC4")));
        reply(target, EmbedUtil.containerBranded("HUB", "Main Onboarding Node", body, EmbedUtil.BANNER_MAIN, null, row1, row2, row3));
    }

    public static void sendServerMap(Object target) {
        String body = "### \uD83D\uDDFA\uFE0F Server Navigation Map\n\u25CF Reception Node \u2192 Welcome\n\u25CF Protocol \u2192 Rules\n\u25CF Logistics \u2192 Orders\n\u25CF Square \u2192 Social";
        replyEphemeral(target, EmbedUtil.containerBranded("MAP", "Digital Topography", body, EmbedUtil.BANNER_MAIN, null, ActionRow.of(Button.secondary("menu_main", "Return to Hub"))));
    }

    public static void sendSocialPanel(Object target) {
        String body = "### \uD83D\uDDA5\uFE0F Agency Presence\nOfficial Highcore digital channels.";
        ActionRow row = ActionRow.of(Button.link("https://x.com/CoreHigh70331", "X"), Button.link("https://www.tiktok.com/@highcoreagency", "TikTok"), Button.link("https://www.instagram.com/high_core_agency/", "Instagram"), Button.link("https://www.threads.com/@high_core_agency", "Threads"));
        replyEphemeral(target, EmbedUtil.containerBranded("SOCIAL", "Connectivity", body, EmbedUtil.BANNER_MAIN, null, row));
    }

    public static void sendColorsPanel(Object target) {
        ActionRow r1 = ActionRow.of(Button.secondary("color_1489744978719543408", "Sunset Orange"), Button.secondary("color_1489744984092442704", "Emerald Green"), Button.secondary("color_1489744981835911238", "Ocean Blue"));
        ActionRow r2 = ActionRow.of(Button.secondary("color_1489744986424479927", "Royal Purple"), Button.secondary("color_1489744990962716732", "Golden Yellow"), Button.secondary("color_1489744988936867880", "Rose Pink"));
        replyEphemeral(target, EmbedUtil.containerBranded("IDENTITY", "Spectrum Calibration", "Select your operative status color.", EmbedUtil.BANNER_MAIN, null, r1, r2));
    }

    public static void sendServicesCategory(Object target) {
        StringSelectMenu menu = StringSelectMenu.create("view_services_cat").setPlaceholder("Capability Sector...").addOption("Designer", "cat_designer").addOption("Developer", "cat_developer").addOption("Editor", "cat_editor").addOption("Minecraft", "cat_minecraft").build();
        replyEphemeral(target, EmbedUtil.containerBranded("DIRECTORY", "Capability Map", "Examine agency assets.", EmbedUtil.BANNER_MAIN, null, ActionRow.of(menu)));
    }

    public static void sendPricesCategory(Object target) {
        StringSelectMenu menu = StringSelectMenu.create("view_prices_cat").setPlaceholder("Accounting Sector...").addOption("Designer Prices", "price_designer").addOption("Developer Prices", "price_developer").addOption("Editor Prices", "price_editor").addOption("Minecraft Prices", "price_minecraft").build();
        replyEphemeral(target, EmbedUtil.containerBranded("ACCOUNTING", "Price Matrix", "Examine financial requirements.", EmbedUtil.BANNER_MAIN, null, ActionRow.of(menu)));
    }

    public static void sendStatsPanel(Object target) {
        reply(target, EmbedUtil.containerBranded("TELEMETRY", "Health Analytics", "Status: `ONLINE` | v2.5", EmbedUtil.BANNER_MAIN, null, ActionRow.of(Button.secondary("menu_main", "Return to Hub"))));
    }

    public static void sendTicketPanel(Object target) {
        StringSelectMenu menu = StringSelectMenu.create("ticket_type_select").setPlaceholder("Support Category...").addOption("Order", "purchase").addOption("Support", "tech_support").addOption("Complaint", "complaint").build();
        reply(target, EmbedUtil.containerBranded("LOGISTICS", "Ticket Node", "Initialize session below.", EmbedUtil.BANNER_SUPPORT, null, ActionRow.of(menu)));
    }

    public static void sendGiveawayPanel(Object target) {
        ActionRow row = ActionRow.of(Button.success("giveaway_start", "Launch New Sweepstakes"), Button.danger("giveaway_end", "Terminate Active Giveaway"));
        reply(target, EmbedUtil.containerBranded("SWEEPSTAKES", "Control Node", "Operate rewards distribution system.", EmbedUtil.BANNER_GIVEAWAY, null, row));
    }
}
