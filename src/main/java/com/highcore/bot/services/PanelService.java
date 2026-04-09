package com.highcore.bot.services;

import com.highcore.bot.utils.EmbedUtil;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.components.MessageTopLevelComponent;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.modals.Modal;
import com.highcore.bot.services.InvoiceService;
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
            try {
                InteractionHook hook = replyCallback.getHook();
                if (ephemeral) {
                    net.dv8tion.jda.api.utils.messages.MessageCreateBuilder mcb = new net.dv8tion.jda.api.utils.messages.MessageCreateBuilder();
                    if (!components.isEmpty()) {
                        mcb.setComponents(components);
                        mcb.useComponentsV2(true);
                    } else {
                        mcb.setContent("\u200B");
                    }
                    hook.sendMessage(mcb.build()).setEphemeral(true).queue(null, e -> {});
                } else {
                    net.dv8tion.jda.api.utils.messages.MessageEditBuilder meb = new net.dv8tion.jda.api.utils.messages.MessageEditBuilder();
                    if (!components.isEmpty()) {
                        meb.setComponents(components);
                        meb.useComponentsV2(true);
                    } else {
                        meb.setContent("\u200B");
                    }
                    hook.editOriginal(meb.build()).queue(null, e -> {});
                }
            } catch (Exception e) {}
        }
    }

    public static void sendStartupHub(Object target) {
        String title = "HIGH CORE AGENCY";
        String bodySection1 = "PROFESSIONAL MULTI-SECTOR AGENCY OPERATIONS\n\nEstablishing the definitive global standard for advanced digital infrastructure and elite creative operations. HIGH CORE provides high-fidelity solutions across system development, visual architecture, and strategic media management.\n\nOur internal systems are engineered for absolute brand dominance and superior technological precision. We merge artistic vision with technical mastery to deliver the unlimited potential of the agency directly to our global partners.";
        String bodySection2 = "By establishing a connection with our operational modules, you gain access to a spectrum of specialized digital assets designed for performance and reliability at scale.\n\nSYSTEM NAVIGATION PROTOCOLS\nExamine our operational modules and establish a secure connection using the authorized protocols below.";
        
        ActionRow row = ActionRow.of(
            Button.secondary("hub_highcore", "Highcore"),
            Button.secondary("hub_about", "About Us"),
            Button.secondary("hub_partners", "Partners"),
            Button.link("https://discord.com/channels/1488795129996116212/1488798547947159612", "Support")
        );
        
        List<ContainerChildComponent> layout = new ArrayList<>();
        layout.add(net.dv8tion.jda.api.components.mediagallery.MediaGallery.of(net.dv8tion.jda.api.components.mediagallery.MediaGalleryItem.fromUrl(EmbedUtil.BANNER_MAIN)));
        layout.add(Separator.createDivider(Separator.Spacing.SMALL));
        layout.add(TextDisplay.of("**" + title + "**"));
        layout.add(Separator.createDivider(Separator.Spacing.SMALL));
        layout.add(TextDisplay.of(bodySection1));
        layout.add(Separator.createDivider(Separator.Spacing.SMALL));
        layout.add(TextDisplay.of(bodySection2));
        layout.add(Separator.createDivider(Separator.Spacing.LARGE));
        layout.add(row);
        
        handleReply(target, Container.of(layout), false);
    }

    public static void sendRulesPanel(Object target) {
        replyEphemeral(target, EmbedUtil.rulesPanel());
    }

    public static void sendServerMap(Object target) {
        String body = """
                SERVER ARCHITECTURE
                
                MAIN OPERATIONS
                - Start Up: <#1488795130470072321>
                - Service Terms: <#1489158831916454070>
                - Updates: <#1488797040732278814>
                - Partners: <#1490334592375324772>
                - Giveaway: <#1490334823565365308>
                
                USER FEEDBACK
                - Design Feedback: <#1490431863494545598>
                - Developer Feedback: <#1490783479342960640>
                - Minecraft Feedback: <#1490783523873882294>
                
                COST MATRIX
                - Developer Price: <#1488800669375795272>
                - Design Price: <#1488800570629427251>
                - Minecraft Price: <#1488795131019526151>
                
                TECHNICAL OPS
                - Support Tickets: <#1488798547947159612>
                """;
        
        ActionRow configRow = ActionRow.of(
            Button.success("hub_pings", "NOTIFY RULE"),
            Button.secondary("hub_rules", "SERVER RULES")
        );
        
        replyEphemeral(target, EmbedUtil.containerBranded("MAP", "Infrastructure Index", body, EmbedUtil.BANNER_MAIN, null, configRow));
    }

    public static void sendAboutUs(Object target) {
        String body = "AGENCY IDENTITY\nHigh Core is an elite multi-sector agency delivering superior digital infrastructure and creative solutions.";
        
        ActionRow row1 = ActionRow.of(
            Button.link("https://x.com/CoreHigh70331", "X"),
            Button.link("https://www.tiktok.com/@highcoreagency", "TikTok"),
            Button.link("https://www.instagram.com/high_core_agency/", "Instagram")
        );
        ActionRow row2 = ActionRow.of(
            Button.link("https://www.threads.com/@high_core_agency", "Threads"),
            Button.link("https://t.me/Beta_Team1/1", "Telegram")
        );
        
        replyEphemeral(target, EmbedUtil.containerBranded("ABOUT", "The Unlimited Agency", body, EmbedUtil.BANNER_MAIN, null, row1, row2));
    }

    public static void sendPartnersPanel(Object target) {
        String body = "STRATEGIC PARTNERS\nOur strategic network of partners and collaborators. Connectivity established soon.";
        replyEphemeral(target, EmbedUtil.containerBranded("PARTNERS", "Collaborative Network", body, EmbedUtil.BANNER_MAIN));
    }

    public static void sendPingsPanel(Object target) {
        String body = "PING NOTIFICATIONS\nSelect frequencies for alerts.";
        ActionRow r1 = ActionRow.of(
            Button.secondary("ping_1488916736639238357", "Server Updates"),
            Button.secondary("ping_1488916921687736421", "Giveaway Notify")
        );
        ActionRow r2 = ActionRow.of(
            Button.secondary("ping_1488916879186596081", "Offers & Promotions"),
            Button.secondary("ping_1489764018989301840", "Start Hiring")
        );
        replyEphemeral(target, EmbedUtil.containerBranded("PINGS", "Notification Matrix", body, EmbedUtil.BANNER_MAIN, null, r1, r2));
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
        reply(target, EmbedUtil.containerBranded("TELEMETRY", "System Status", "Status: Operational | Matrix v1.2.0", EmbedUtil.BANNER_MAIN, null, ActionRow.of(Button.secondary("menu_main", "Return to Hub"))));
    }

    public static void sendTicketPanel(Object target) {
        reply(target, EmbedUtil.containerBranded("TICKETS", "Logistics Hub", EmbedUtil.TICKET_RULES, EmbedUtil.BANNER_SUPPORT, null,
                ActionRow.of(
                    Button.primary("ticket_init_support", "Technical Support \uD83D\uDEE1\uFE0F"),
                    Button.success("ticket_init_order", "New Order \uD83D\uDED2"),
                    Button.danger("ticket_init_complaint", "File Complaint \u26A0\uFE0F")
                )
        ));
    }

    public static void handleSupportFlow(Object target) {
        TextInput issue = TextInput.create("issue_desc", TextInputStyle.PARAGRAPH).build();
        TextInput svc = TextInput.create("issue_service", TextInputStyle.SHORT).build(); // placeholder omitted due to API strictness
        
        if (target instanceof IReplyCallback cb) {
            cb.replyModal(Modal.create("modal_support_init", "TECHNICAL SUPPORT")
                .addComponents(Label.of("What is the issue?", issue), Label.of("Which service?", svc))
                .build()).queue();
        }
    }

    public static void handleComplaintFlow(Object target) {
        TextInput targetAdmin = TextInput.create("comp_target", TextInputStyle.SHORT).build();
        TextInput reason = TextInput.create("comp_reason", TextInputStyle.PARAGRAPH).build();

        if (target instanceof IReplyCallback cb) {
            cb.replyModal(Modal.create("modal_complaint_init", "OFFICIAL COMPLAINT")
                .addComponents(Label.of("Who is involved?", targetAdmin), Label.of("Reason/Behavior?", reason))
                .build()).queue();
        }
    }

    public static void handleOrderMetaModal(net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent event) {
        TextInput pName = TextInput.create("p_name", TextInputStyle.SHORT).build();
        TextInput cName = TextInput.create("p_client", TextInputStyle.SHORT).build();
        TextInput contact = TextInput.create("p_contact", TextInputStyle.SHORT).build();
        TextInput eta = TextInput.create("p_eta", TextInputStyle.SHORT).build();

        event.replyModal(Modal.create("modal_order_finalize", "PROJECT CONFIGURATION")
            .addComponents(
                Label.of("Project Name", pName), 
                Label.of("Client Name", cName), 
                Label.of("Contact Info", contact), 
                Label.of("Expected ETA", eta)
            ).build()).queue();
    }

    public static void handleOrderFlow(Object target) {
        StringSelectMenu menu = StringSelectMenu.create("order_sector_select")
            .setPlaceholder("Select Creative Sector...")
            .addOption("Designer Sector", "sect_designer")
            .addOption("Developer Sector", "sect_developer")
            .addOption("Editing & Animation", "sect_editor")
            .addOption("Minecraft Developer", "sect_mcdev")
            .build();
        
        replyEphemeral(target, EmbedUtil.containerBranded("ORDER", "Sector Selection", "Identify the specialized department for your request.", EmbedUtil.BANNER_MAIN, null, ActionRow.of(menu)));
    }

    public static void handleSectorSelection(net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent event, String val) {
        StringSelectMenu.Builder menu = StringSelectMenu.create("order_service_select_" + val)
            .setPlaceholder("Select Services (Multiple Allowed)...")
            .setMinValues(1).setMaxValues(5);

        switch (val) {
            case "sect_designer" -> {
                menu.addOption("Scary Logos ($30)", "Logo-30")
                    .addOption("Visual Identity ($60)", "Identity-60")
                    .addOption("Posters & Ads ($90)", "Posters-90")
                    .addOption("Social Media ($20)", "Social-20")
                    .addOption("Welcome/Discord Packs ($20)", "Discord-20")
                    .addOption("Covers & Banners ($30)", "Covers-30")
                    .addOption("Prints ($25)", "Prints-25")
                    .addOption("Motion Graphics ($90)", "Motion-90")
                    .addOption("UI/UX Design ($120)", "UIUX-120")
                    .addOption("Infographic ($40)", "Info-40")
                    .addOption("Emoji / Stickers ($30)", "Emoji-30");
            }
            case "sect_developer" -> {
                menu.addOption("Web Developer ($50)", "Web-50")
                    .addOption("Bot Developer ($50)", "Bots-50")
                    .addOption("Full-Stack Engine ($100)", "FullStack-100")
                    .addOption("Front-End ($30)", "Front-30")
                    .addOption("Back-End ($40)", "Back-40")
                    .addOption("AI & Automation ($100)", "AI-100")
                    .addOption("Database Admin ($30)", "DB-30");
            }
            case "sect_editor" -> {
                menu.addOption("Reels/Shorts Editor ($60)", "Shorts-60")
                    .addOption("Long-form Editor ($120)", "Long-120")
                    .addOption("Animation Editor ($150)", "Anim-150")
                    .addOption("Gaming Editor ($150)", "Game-150");
            }
            case "sect_mcdev" -> {
                menu.addOption("Plugin Developer ($50)", "Plugin-50")
                    .addOption("Config Specialist ($80)", "Config-80")
                    .addOption("Map Maker ($30)", "Map-30")
                    .addOption("Texture Creator ($130)", "Tex-130")
                    .addOption("3D Modeler ($65)", "3D-65")
                    .addOption("Technical Admin ($55)", "Sys-55");
            }
        }
        
        reply(event, EmbedUtil.containerBranded("CATALOG", "Service Matrix", "Select your primary technical requirements.", EmbedUtil.BANNER_MAIN, null, ActionRow.of(menu.build())));
    }

    public static void handleServiceSelection(net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent event, List<String> vals) {
        StringSelectMenu.Builder menu = StringSelectMenu.create("order_extras_select")
            .setPlaceholder("Select Extras (Optional)...")
            .setMinValues(0).setMaxValues(5);

        menu.addOption("Rush Delivery ($45-70)", "Rush-70")
            .addOption("Source Files (AI/PSD) ($150-250)", "Source-250")
            .addOption("Animation Add-on ($200)", "AnimExtra-200")
            .addOption("Multi-Color Variants ($35)", "Variants-35")
            .addOption("Extra Revisions ($35-180)", "Revisions-180")
            .addOption("Copywriting ($25)", "Copy-25");
        
        String summary = "You have selected " + vals.size() + " items. Proceed to refine with extras.";
        reply(event, EmbedUtil.containerBranded("EXTRAS", "The Seconds", summary, EmbedUtil.BANNER_MAIN, null, ActionRow.of(menu.build()), ActionRow.of(Button.success("order_final_meta", "Finalize Details"))));
    }

    public static void sendOrderPanel(Object target) {
        String body = """
                HIGH CORE SERVICES
                Select a sector to view professional rates and modules.
                
                DESIGNER SECTOR
                - Logos (30$)
                - Visual Identity (60$)
                - Posters & Ads (90$)
                - Social Media (20$)
                - Welcome Package (20$)
                - Covers (30$)
                - Prints (25$)
                - Motion (90$)
                - UI/UX (120$)
                - Infographic (40$)
                
                DEVELOPER SECTOR
                - Web (50$)
                - Bots (50$)
                - Full-Stack (100$)
                - Front-End (30$)
                - Back-End (40$)
                - AI Automation (100$)
                
                EDITING SECTOR
                - Reels (60$)
                - Long Video (120$)
                - Animation (150$)
                
                MINECRAFT SECTOR
                - Plugin (50$)
                - Config (80$)
                - Map (30$)
                """;
        
        ActionRow row = ActionRow.of(
            Button.success("order_initiate", "START ORDER"),
            Button.link("https://discord.com/channels/1488795130470072320/1488795131019526146", "ORDER ROOM")
        );
        
        reply(target, EmbedUtil.containerBranded("ORDER", "Service Portfolio", body, EmbedUtil.BANNER_MAIN, null, row));
    }

    public static void sendServiceTerms(Object target) {
        String body = """
                SERVICE TERMS
                Professional engagement protocols for High Core Agency.
                
                PROTOCOLS
                1. Payments are upfront.
                2. Deadlines are fixed.
                3. Revisions vary by sector.
                4. Source files cost extra.
                
                By initiating an order, you agree to the terms.
                """;
        reply(target, EmbedUtil.containerBranded("LEGAL", "Terms of Operation", body, EmbedUtil.BANNER_MAIN));
    }

    public static void sendGiveawayPanel(Object target) {
        ActionRow row = ActionRow.of(Button.success("giveaway_start", "Deploy New Event"), Button.danger("giveaway_end", "Terminate Active Event"));
        reply(target, EmbedUtil.containerBranded("EVENTS", "Distribution", "Manage system rewards.", EmbedUtil.BANNER_MAIN, null, row));
    }
}
