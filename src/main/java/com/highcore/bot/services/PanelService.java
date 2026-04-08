package com.highcore.bot.services;

import com.highcore.bot.utils.EmbedUtil;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
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
        if (content instanceof Container c) {
            components.add(c);
        } else if (content instanceof List<?> list) {
            for (Object obj : list) {
                if (obj instanceof Container c) components.add(c);
                else if (obj instanceof MessageTopLevelComponent mtc) components.add(mtc);
            }
        }

        if (interaction instanceof IReplyCallback replyCallback) {
            if (!replyCallback.isAcknowledged()) {
                replyCallback.deferReply(ephemeral).queue(hook -> {
                    try {
                        var edit = hook.editOriginal("");
                        if (!components.isEmpty()) {
                            edit.setComponents(components);
                            edit.useComponentsV2(true);
                        }
                        edit.queue();
                    } catch (Exception e) {
                        try { hook.editOriginal("### \u26A0 STABILITY PROTECTOR\nError: `" + e.getMessage() + "`").queue(); } catch (Exception ignored) {}
                    }
                });
            } else {
                var hook = replyCallback.getHook();
                try {
                    var edit = hook.editOriginal("");
                    if (!components.isEmpty()) {
                        edit.setComponents(components);
                        edit.useComponentsV2(true);
                    }
                    edit.queue();
                } catch (Exception e) {
                    try { hook.sendMessage("### \u26A0 STABILITY ERROR\n`" + e.getMessage() + "`").setEphemeral(true).queue(); } catch (Exception ignored) {}
                }
            }
        }
    }

    public static void sendStartupHub(Object target) {
        String body = "### \u25C8 High Core The Unlimited Agency\nGlobal infrastructure logic. Use the protocols below.";
        ActionRow row1 = ActionRow.of(Button.primary("hub_map", " \u25C8            NAVIGATE SERVER MAP            \u25C8 ").withEmoji(Emoji.fromUnicode("\uD83D\uDDFA\uFE0F")));
        ActionRow row2 = ActionRow.of(
            Button.success("hub_pings", "NOTIFY RULE").withEmoji(Emoji.fromUnicode("\uD83D\uDCE2")),
            Button.primary("hub_colors", "IDENTITY CALIBRATION").withEmoji(Emoji.fromUnicode("\uD83C\uDFA8"))
        );
        ActionRow row3 = ActionRow.of(
            Button.secondary("hub_rules", "SERVER RULES").withEmoji(Emoji.fromUnicode("\uD83D\uDCDC")),
            Button.secondary("hub_social", "SOCIAL MEDIA").withEmoji(Emoji.fromUnicode("\uD83C\uDF10"))
        );
        handleReply(target, EmbedUtil.containerBranded("HUB", "Main Dashboard", body, EmbedUtil.BANNER_MAIN, null, row1, row2, row3), false);
    }

    public static void sendRulesPanel(Object target) {
        replyEphemeral(target, EmbedUtil.rulesPanel());
    }

    public static void sendServerMap(Object target) {
        String body = """
                ### \uD83D\uDDFA\uFE0F SERVER INFRASTRUCTURE
                **\uD83D\uDCE1 MAIN OPERATIONS:**
                \u25B8 <#1488795130470072321> \u231B Start Up
                \u25B8 <#1489158831916454070> \uD83D\uDCCB Service Terms
                \u25B8 <#1488797040732278814> \uD83D\uDD14 Updates
                \u25B8 <#1490334592375324772> \uD83E\uDD1D Partners
                \u25B8 <#1490334823565365308> \uD83C\uDF81 Giveaway
                **\uD83D\uDDAF\uFE0F USER FEEDBACK:**
                \u25B8 <#1490431863494545598> \uD83C\uDFA8 Design FeedBack
                \u25B8 <#1490783479342960640> \uD83D\uDCBB Developer FeedBack
                \u25B8 <#1490783523873882294> \u26CF\uFE0F Minecraft FeedBack
                **\uD83D\uDCB3 COST MATRIX:**
                \u25B8 <#1488800669375795272> \uD83D\uDCBB Developer Price
                \u25B8 <#1488800570629427251> \uD83C\uDFA8 Design Price
                \u25B8 <#1488795131019526151> \u26CF\uFE0F Minecraft Price
                **\uD83C\uDD98 TECHNICAL OPS:**
                \u25B8 <#1488798547947159612> \u2709\uFE0F Support Tickets
                """;
        replyEphemeral(target, EmbedUtil.containerBranded("MAP", "Logistics Index", body, EmbedUtil.BANNER_MAP));
    }

    public static void sendSocialPanel(Object target) {
        String body = "### \uD83D\uDDA5\uFE0F CONNECT WITH US\nStay updated through our official frequencies.";
        // SAFE EMOJIS (Unicode) to prevent "Thinking..." hang until IDs are fixed
        ActionRow row1 = ActionRow.of(
            Button.link("https://x.com/CoreHigh70331", "X").withEmoji(Emoji.fromUnicode("\uD83D\uDDA4")),
            Button.link("https://www.tiktok.com/@highcoreagency", "TikTok").withEmoji(Emoji.fromUnicode("\uD83D\uDCF1")),
            Button.link("https://www.instagram.com/high_core_agency/", "Instagram").withEmoji(Emoji.fromUnicode("\uD83D\uDCF7"))
        );
        ActionRow row2 = ActionRow.of(
            Button.link("https://www.threads.com/@high_core_agency", "Threads").withEmoji(Emoji.fromUnicode("\uD83D\uDD30")),
            Button.link("https://t.me/Beta_Team1/1", "Telegram").withEmoji(Emoji.fromUnicode("\uD83D\uDCE2"))
        );
        replyEphemeral(target, EmbedUtil.containerBranded("SOCIAL", "Media Links", body, EmbedUtil.BANNER_SOCIAL, null, row1, row2));
    }

    public static void sendPingsPanel(Object target) {
        String body = "### \uD83D\uDCE2 PING NOTIFICATIONS\nSelect frequencies for alerts.";
        ActionRow r1 = ActionRow.of(
            Button.secondary("ping_1488916736639238357", "Server Updates").withEmoji(Emoji.fromUnicode("\uD83D\uDCE2")),
            Button.secondary("ping_1488916921687736421", "Giveaway Notify").withEmoji(Emoji.fromUnicode("\uD83C\uDF81"))
        );
        ActionRow r2 = ActionRow.of(
            Button.secondary("ping_1488916879186596081", "Offers & Promotions").withEmoji(Emoji.fromUnicode("\uD83D\uDCAF")),
            Button.secondary("ping_1489764018989301840", "Start Hiring").withEmoji(Emoji.fromUnicode("\uD83D\uDCBC"))
        );
        replyEphemeral(target, EmbedUtil.containerBranded("PINGS", "Notification Matrix", body, EmbedUtil.BANNER_PINGS, null, r1, r2));
    }

    public static void sendColorsPanel(Object target) {
        ActionRow r1 = ActionRow.of(Button.secondary("color_1489744978719543408", "Sunset Orange"), Button.secondary("color_1489744984092442704", "Emerald Green"), Button.secondary("color_1489744981835911238", "Ocean Blue"));
        ActionRow r2 = ActionRow.of(Button.secondary("color_1489744986424479927", "Royal Purple"), Button.secondary("color_1489744990962716732", "Golden Yellow"), Button.secondary("color_1489744988936867880", "Rose Pink"));
        replyEphemeral(target, EmbedUtil.containerBranded("IDENTITY", "Color Selection", "Define your presence.", EmbedUtil.BANNER_COLORS, null, r1, r2));
    }

    public static void sendServicesCategory(Object target) {
        StringSelectMenu menu = StringSelectMenu.create("view_services_cat").setPlaceholder("Select Capability...").addOption("Design Sector", "cat_designer").addOption("Code Sector", "cat_developer").addOption("Media Sector", "cat_editor").addOption("Minecraft Sector", "cat_minecraft").build();
        replyEphemeral(target, EmbedUtil.containerBranded("CAPABILITIES", "Agency Assets", "Examine modules.", EmbedUtil.BANNER_MAIN, null, ActionRow.of(menu), ActionRow.of(Button.secondary("menu_main", "Return to Hub"))));
    }

    public static void sendPricesCategory(Object target) {
        StringSelectMenu menu = StringSelectMenu.create("view_prices_cat").setPlaceholder("Select Sector...").addOption("Design Fees", "price_designer").addOption("Dev Fees", "price_developer").addOption("Media Fees", "price_editor").addOption("Minecraft Fees", "price_minecraft").build();
        replyEphemeral(target, EmbedUtil.containerBranded("LOGISTICS", "Cost Structure", "Review professional service rates.", EmbedUtil.BANNER_MAIN, null, ActionRow.of(menu)));
    }

    public static void sendStatsPanel(Object target) {
        reply(target, EmbedUtil.containerBranded("TELEMETRY", "System Data", "Status: `Operational` | Matrix v1.2.0", EmbedUtil.BANNER_MAIN, null, ActionRow.of(Button.secondary("menu_main", "Return to Hub"))));
    }

    public static void sendTicketPanel(Object target) {
        StringSelectMenu menu = StringSelectMenu.create("ticket_type_select").setPlaceholder("Case Type...").addOption("Order Placement", "purchase").addOption("Technical Ops", "tech_support").addOption("General Report", "complaint").build();
        reply(target, EmbedUtil.containerBranded("SESSIONS", "Initiate Request", "Establish a secure link.", EmbedUtil.BANNER_SUPPORT, null, ActionRow.of(menu)));
    }

    public static void sendGiveawayPanel(Object target) {
        ActionRow row = ActionRow.of(Button.success("giveaway_start", "Deploy New Event"), Button.danger("giveaway_end", "Terminate Active Event"));
        reply(target, EmbedUtil.containerBranded("EVENTS", "Distribution Control", "Manage system rewards.", EmbedUtil.BANNER_GIVEAWAY, null, row));
    }
}
