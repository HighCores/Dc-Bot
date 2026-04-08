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
        String body = "◈ Global digital infrastructure & professional multi-sector agency services. High fidelity solutions for the modern era.";
        
        ActionRow row1 = ActionRow.of(
            Button.primary("hub_highcore", "◈ HIGHCORE").withEmoji(Emoji.fromUnicode("\uD83D\uDDA5\uFE0F")),
            Button.primary("hub_about", "◈ ABOUT US").withEmoji(Emoji.fromUnicode("\uD83D\uDCD6"))
        );
        ActionRow row2 = ActionRow.of(
            Button.secondary("hub_partners", "◈ PARTNERS").withEmoji(Emoji.fromUnicode("\uD83E\uDD1D")),
            Button.link("https://discord.com/channels/1488795130470072320/1488798547947159612", "◈ SUPPORT").withEmoji(Emoji.fromUnicode("\u26D1\uFE0F"))
        );
        
        handleReply(target, EmbedUtil.containerBranded("HIGH CORE THE UNLIMITED AGENCY", null, body, EmbedUtil.BANNER_MAIN, null, row1, row2), false);
    }

    public static void sendRulesPanel(Object target) {
        replyEphemeral(target, EmbedUtil.rulesPanel());
    }

    public static void sendServerMap(Object target) {
        String body = """
                ### \uD83D\uDDA5\uFE0F SERVER ARCHITECTURE
                \u2014\u2014\u2014\u2014\u2014\u2014\u2014\u2014\u2014\u2014\u2014\u2014\u2014\u2014\u2014\u2014\u2014\u2014\u2014
                
                ### \uD83D\uDCE1 MAIN OPERATIONS
                \u2213 <#1488795130470072321> \u231B Start Up
                \u2508\u2508\u2508\u2508\u2508\u2508\u2508\u2508\u2508\u2508\u2508\u2508\u2508\u2508\u2508\u2508\u2508\u2508\u2508
                \u2213 <#1489158831916454070> \uD83D\uDCCB Service Terms
                \u2508\u2508\u2508\u2508\u2508\u2508\u2508\u2508\u2508\u2508\u2508\u2508\u2508\u2508\u2508\u2508\u2508\u2508\u2508
                \u2213 <#1488797040732278814> \uD83D\uDD14 Updates
                \u2508\u2508\u2508\u2508\u2508\u2508\u2508\u2508\u2508\u2508\u2508\u2508\u2508\u2508\u2508\u2508\u2508\u2508\u2508
                \u2213 <#1490334592375324772> \u231B Partners
                \u2508\u2508\u2508\u2508\u2508\u2508\u2508\u2508\u2508\u2508\u2508\u2508\u2508\u2508\u2508\u2508\u2508\u2508\u2508
                \u2213 <#1490334823565365308> \u231B Giveaway
                
                ### \uD83D\uDDAF\uFE0F USER FEEDBACK
                \u2213 <#1490431863494545598> \u231B Design Feedback
                \u2508\u2508\u2508\u2508\u2508\u2508\u2508\u2508\u2508\u2508\u2508\u2508\u2508\u2508\u2508\u2508\u2508\u2508\u2508
                \u2213 <#1490783479342960640> \u231B Developer Feedback
                \u2508\u2508\u2508\u2508\u2508\u2508\u2508\u2508\u2508\u2508\u2508\u2508\u2508\u2508\u2508\u2508\u2508\u2508\u2508
                \u2213 <#1490783523873882294> \u231B Minecraft Feedback
                
                ### \uD83D\uDCB3 COST MATRIX
                \u2213 <#1488800669375795272> \u231B Developer Price
                \u2508\u2508\u2508\u2508\u2508\u2508\u2508\u2508\u2508\u2508\u2508\u2508\u2508\u2508\u2508\u2508\u2508\u2508\u2508
                \u2213 <#1488800570629427251> \u231B Design Price
                \u2508\u2508\u2508\u2508\u2508\u2508\u2508\u2508\u2508\u2508\u2508\u2508\u2508\u2508\u2508\u2508\u2508\u2508\u2508
                \u2213 <#1488795131019526151> \u231B Minecraft Price
                
                ### \u26D1\uFE0F TECHNICAL OPS
                \u2213 <#1488798547947159612> \u2709\uFE0F Support Tickets
                """;
        
        ActionRow configRow = ActionRow.of(
            Button.success("hub_pings", "NOTIFY RULE").withEmoji(Emoji.fromUnicode("\uD83D\uDCE2")),
            Button.primary("hub_colors", "IDENTITY CALIBRATION").withEmoji(Emoji.fromUnicode("\uD83C\uDFA8")),
            Button.secondary("hub_rules", "SERVER RULES").withEmoji(Emoji.fromUnicode("\uD83D\uDCDC"))
        );
        
        replyEphemeral(target, EmbedUtil.containerBranded("MAP", "Infrastructure Index", body, EmbedUtil.BANNER_MAP, null, configRow));
    }

    public static void sendAboutUs(Object target) {
        String body = "### \uD83D\uDCD6 AGENCY IDENTITY\n◈ High Core is an elite multi-sector agency delivering superior digital infrastructure and creative solutions.";
        
        // Using standard emojis for stability unless IDs are explicitly provided and verified
        ActionRow row1 = ActionRow.of(
            Button.link("https://x.com/CoreHigh70331", "X").withEmoji(Emoji.fromUnicode("\uD83D\uDD35")),
            Button.link("https://www.tiktok.com/@highcoreagency", "TikTok").withEmoji(Emoji.fromUnicode("\u26AB")),
            Button.link("https://www.instagram.com/high_core_agency/", "Instagram").withEmoji(Emoji.fromUnicode("\uD83C\uDF10"))
        );
        ActionRow row2 = ActionRow.of(
            Button.link("https://www.threads.com/@high_core_agency", "Threads").withEmoji(Emoji.fromUnicode("\u1F5D2")),
            Button.link("https://t.me/Beta_Team1/1", "Telegram").withEmoji(Emoji.fromUnicode("\u2708\uFE0F"))
        );
        
        replyEphemeral(target, EmbedUtil.containerBranded("ABOUT", "The Unlimited Agency", body, EmbedUtil.BANNER_SOCIAL, null, row1, row2));
    }

    public static void sendPartnersPanel(Object target) {
        String body = "### \uD83E\uDD1D STRATEGIC PARTNERS\n◈ Our strategic network of partners and collaborators. Connectivity established soon.";
        replyEphemeral(target, EmbedUtil.containerBranded("PARTNERS", "Collaborative Network", body, EmbedUtil.BANNER_MAIN));
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

    public static void sendOrderPanel(Object target) {
        String body = """
                ### \uD83D\uDDA5\uFE0F HIGH CORE SERVICES
                ◈ Select a sector to view professional rates and modules.
                
                \u2014\u2014\u2014\u2014\u2014\u2014\u2014\u2014\u2014\u2014\u2014\u2014\u2014\u2014\u2014\u2014\u2014\u2014\u2014
                
                ### \uD83C\uDFA8 DESIGNER SECTOR
                \u2213 Logos (30$)
                \u2213 Visual Identity (60$)
                \u2213 Posters & Ads (90$)
                \u2213 Social Media Design (20$)
                \u2213 Welcome Packages (20$)
                \u2213 Covers & Banners (30$)
                \u2213 Prints & Brochure (25$)
                \u2213 Motion Graphics (90$)
                \u2213 UI/UX Design (120$)
                \u2213 Infographic (40$)
                \u2213 Emojis / Stickers (30$)
                **Secondes:** Rush (45$), AI/PSD (250$), Colors (35$), Animation (200$), Edit (35$), Size (10$), Text (25$)
                
                ### \uD83D\uDCBB DEVELOPER SECTOR
                \u2213 Web Developer (50$)
                \u2213 Bots Developer (50$)
                \u2213 Full-Stack (100$)
                \u2213 Front-End (30$)
                \u2213 Back-End (40$)
                \u2213 AI & Automation (100$)
                \u2213 Database Admin (30$)
                **Secondes:** Rush (70$), AI/PSD (150$), 2x Revisions (180$)
                
                ### \uD83C\uDFAC EDITING & ANIMATION
                \u2213 Reels/Shorts (60$)
                \u2213 Long-form Video (120$)
                \u2213 Animation (150$)
                \u2213 Gaming Editor (150$)
                **Secondes:** Rush (45$), AI/PSD (250$), Colors (35$), Animation (200$), Edit (35$), Size (10$), Text (25$)
                
                ### \u26CF\uFE0F MINECRAFT DEVELOPER
                \u2213 Plugin Developer (50$)
                \u2213 Config Specialist (80$)
                \u2213 Map Maker / Builder (30$)
                \u2213 Texture Creator (130$)
                \u2213 3D Modeler (65$)
                \u2213 SysAdmin (55$)
                **Secondes:** Rush (45$), AI/PSD (250$), Colors (35$), Animation (200$), Edit (35$), Size (10$), Text (25$)
                """;
        
        ActionRow row = ActionRow.of(
            Button.success("order_initiate", "◈ START ORDER").withEmoji(Emoji.fromUnicode("\uD83D\uDCC4")),
            Button.link("https://discord.com/channels/1488795130470072320/1488795131019526146", "◈ ORDER ROOM").withEmoji(Emoji.fromUnicode("\uD83D\uDCE1"))
        );
        
        reply(target, EmbedUtil.containerBranded("ORDER", "Service Portfolio", body, EmbedUtil.BANNER_SUPPORT, null, row));
    }

    public static void sendServiceTerms(Object target) {
        String body = """
                ### \u2696\uFE0F SERVICE TERMS
                ◈ Professional engagement protocols for High Core Agency.
                
                \u2014\u2014\u2014\u2014\u2014\u2014\u2014\u2014\u2014\u2014\u2014\u2014\u2014\u2014\u2014\u2014\u2014\u2014\u2014
                
                ### \uD83D\uDCDC PROTOCOLS
                1. Payments are upfront unless stated otherwise.
                2. Deadlines are fixed upon project initiation.
                3. Revisions are subject to the specific sector rules.
                4. Source files require premium upgrades.
                
                \u203B By initiating an order, you agree to the full spectral terms of the agency.
                """;
        reply(target, EmbedUtil.containerBranded("LEGAL", "Terms of Operation", body, EmbedUtil.BANNER_RULES));
    }

    public static void sendGiveawayPanel(Object target) {
        ActionRow row = ActionRow.of(Button.success("giveaway_start", "Deploy New Event"), Button.danger("giveaway_end", "Terminate Active Event"));
        reply(target, EmbedUtil.containerBranded("EVENTS", "Distribution Control", "Manage system rewards.", EmbedUtil.BANNER_GIVEAWAY, null, row));
    }
}
