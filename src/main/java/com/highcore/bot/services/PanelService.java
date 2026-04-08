package com.highcore.bot.services;

import com.highcore.bot.database.SupabaseClient;
import com.highcore.bot.utils.EmbedUtil;
import net.dv8tion.jda.api.EmbedBuilder;
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
        List<MessageEmbed> embeds = new ArrayList<>();
        
        // 1. BRANDING: Use the verified Discord CDN banner for 100% display
        embeds.add(new EmbedBuilder()
            .setImage(EmbedUtil.BANNER_MAIN)
            .setColor(EmbedUtil.GOLD)
            .build());

        if (content instanceof Container c) {
            components.add(c);
        } else if (content instanceof MessageEmbed me) {
            embeds.add(me);
        } else if (content instanceof List<?> list) {
            for (Object obj : list) {
                if (obj instanceof Container c) components.add(c);
                else if (obj instanceof MessageEmbed me) embeds.add(me);
            }
        }

        if (interaction instanceof IReplyCallback replyCallback) {
            // THE DIRECT STRIKE PROTOCOL: Reply immediately to bypass Discord "Thinking" hangs
            if (!replyCallback.isAcknowledged()) {
                var reply = replyCallback.reply("` [+] High Core System Protocol Initiated `")
                    .setEphemeral(ephemeral)
                    .setEmbeds(embeds);
                
                if (!components.isEmpty()) {
                    reply.setComponents(components);
                    reply.useComponentsV2(true);
                }
                reply.queue();
            } else {
                // FALLBACK IF ALREADY DEFERRED (Rare)
                var hook = replyCallback.getHook();
                var edit = hook.editOriginal("` [+] High Core System Protocol Updated `")
                    .setEmbeds(embeds);
                
                if (!components.isEmpty()) {
                    edit.setComponents(components);
                    edit.useComponentsV2(true);
                }
                edit.queue();
            }
        }
    }

    public static void sendStartupHub(Object target) {
        String body = "### \u25C8 High Core Unified Terminal\nGlobal infrastructure at your fingertips. Use the protocols below to navigate our sectors.";
        
        // Creative Layout: Large Header Button + Dual Grid
        ActionRow row1 = ActionRow.of(
            Button.primary("hub_map", " \u25C8            NAVIGATE SERVER MAP            \u25C8 ").withEmoji(Emoji.fromUnicode("\uD83D\uDDFA\uFE0F"))
        );
        
        ActionRow row2 = ActionRow.of(
            Button.success("hub_pings", "NOTIFICATION NODES").withEmoji(Emoji.fromUnicode("\uD83D\uDCE2")),
            Button.primary("hub_colors", "IDENTITY CALIBRATION").withEmoji(Emoji.fromUnicode("\uD83C\uDFA8"))
        );
        
        ActionRow row3 = ActionRow.of(
            Button.secondary("hub_rules", "SERVER PROTOCOLS").withEmoji(Emoji.fromUnicode("\uD83D\uDCDC")),
            Button.secondary("hub_social", "OFFICIAL CHANNELS").withEmoji(Emoji.fromUnicode("\uD83D\uDDA5\uFE0F"))
        );
        
        // Startup remains public (false = NOT ephemeral), sub-panels remain ephemeral
        handleReply(target, EmbedUtil.containerBranded("HUB", "Main Dashboard", body, EmbedUtil.BANNER_MAIN, null, row1, row2, row3), false);
    }

    public static void sendServerMap(Object target) {
        String body = """
                ### 🗺️ SERVER INFRASTRUCTURE
                
                **📡 MAIN OPERATIONS:**
                \u25B8 <#1488795130470072321> \u231B Start Up
                \u25B8 <#1489158831916454070> 📋 Service Terms
                \u25B8 <#1488797040732278814> 🔔 Updates
                \u25B8 <#1490334592375324772> 🤝 Partners
                \u25B8 <#1490334823565365308> 🎁 Giveaway
                
                **🗯️ USER FEEDBACK:**
                \u25B8 <#1490431863494545598> 🎨 Design FeedBack
                \u25B8 <#1490783479342960640> 💻 Developer FeedBack
                \u25B8 <#1490783523873882294> ⛏️ Minecraft FeedBack
                
                **💳 COST MATRIX:**
                \u25B8 <#1488800669375795272> 💻 Developer Price
                \u25B8 <#1488800570629427251> 🎨 Design Price
                \u25B8 <#1488795131019526151> ⛏️ Minecraft Price
                
                **🆘 TECHNICAL OPS:**
                \u25B8 <#1488798547947159612> ✉️ Support Tickets
                """;
        replyEphemeral(target, EmbedUtil.containerBranded("MAP", "Logistics Index", body, EmbedUtil.BANNER_MAIN));
    }

    public static void sendSocialPanel(Object target) {
        String body = "### \uD83D\uDDA5\uFE0F CONNECT WITH US\nFollow High Core across all official industrial channels.";
        ActionRow row = ActionRow.of(
            Button.link("https://x.com/CoreHigh70331", "X").withEmoji(Emoji.fromUnicode("\uD83D\uDC26")), 
            Button.link("https://www.tiktok.com/@highcoreagency", "TikTok").withEmoji(Emoji.fromUnicode("\uD83D\uDCF9")), 
            Button.link("https://www.instagram.com/high_core_agency/", "Instagram").withEmoji(Emoji.fromUnicode("\uD83D\uDCF7")), 
            Button.link("https://www.threads.com/@high_core_agency", "Threads").withEmoji(Emoji.fromUnicode("\uD83D\uDD30"))
        );
        replyEphemeral(target, EmbedUtil.containerBranded("SOCIAL", "Media Links", body, EmbedUtil.BANNER_MAIN, null, row));
    }

    public static void sendPingsPanel(Object target) {
        String body = "### \uD83D\uDCE2 PING NOTIFICATIONS\nSelect the frequencies you wish to receive alerts for. Multiple selections are authorized.";
        
        ActionRow r1 = ActionRow.of(
            Button.secondary("ping_1488916736639238357", "Server Updates").withEmoji(Emoji.fromUnicode("\uD83D\uDCE2")),
            Button.secondary("ping_1488916921687736421", "Giveaway Notify").withEmoji(Emoji.fromUnicode("\uD83C\uDF81"))
        );
        ActionRow r2 = ActionRow.of(
            Button.secondary("ping_1488916879186596081", "Offers & Promotions").withEmoji(Emoji.fromUnicode("\uD83D\uDCAF")),
            Button.secondary("ping_1489764018989301840", "Start Hiring").withEmoji(Emoji.fromUnicode("\uD83D\uDCBC"))
        );
        
        replyEphemeral(target, EmbedUtil.containerBranded("PINGS", "Notification Matrix", body, EmbedUtil.BANNER_MAIN, null, r1, r2));
    }

    public static void sendColorsPanel(Object target) {
        ActionRow r1 = ActionRow.of(Button.secondary("color_1489744978719543408", "Sunset Orange"), Button.secondary("color_1489744984092442704", "Emerald Green"), Button.secondary("color_1489744981835911238", "Ocean Blue"));
        ActionRow r2 = ActionRow.of(Button.secondary("color_1489744986424479927", "Royal Purple"), Button.secondary("color_1489744990962716732", "Golden Yellow"), Button.secondary("color_1489744988936867880", "Rose Pink"));
        replyEphemeral(target, EmbedUtil.containerBranded("IDENTITY", "Color Selection", "Define your presence within the grid.", EmbedUtil.BANNER_MAIN, null, r1, r2));
    }

    public static void sendServicesCategory(Object target) {
        StringSelectMenu menu = StringSelectMenu.create("view_services_cat").setPlaceholder("Select Capability...").addOption("Design Sector", "cat_designer").addOption("Code Sector", "cat_developer").addOption("Media Sector", "cat_editor").addOption("Minecraft Sector", "cat_minecraft").build();
        replyEphemeral(target, EmbedUtil.containerBranded("CAPABILITIES", "Agency Assets", "Examine our operational modules.", EmbedUtil.BANNER_MAIN, null, ActionRow.of(menu), ActionRow.of(Button.secondary("menu_main", "Return to Hub"))));
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
        reply(target, EmbedUtil.containerBranded("SESSIONS", "Initiate Request", "Establish a secure communication link with our team.", EmbedUtil.BANNER_SUPPORT, null, ActionRow.of(menu)));
    }

    public static void sendGiveawayPanel(Object target) {
        ActionRow row = ActionRow.of(Button.success("giveaway_start", "Deploy New Event"), Button.danger("giveaway_end", "Terminate Active Event"));
        reply(target, EmbedUtil.containerBranded("EVENTS", "Distribution Control", "Manage system reward deployments.", EmbedUtil.BANNER_GIVEAWAY, null, row));
    }
}
