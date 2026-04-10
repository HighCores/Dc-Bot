package com.highcore.bot.services;

import com.highcore.bot.utils.EmbedUtil;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.components.MessageTopLevelComponent;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.modals.Modal;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PanelService {

    // ── Order session state ───────────────────────────────────────────────────
    public static class OrderSession {
        public String sector;
        public List<String> mainServices = new ArrayList<>();
        public List<String> addons       = new ArrayList<>();
    }
    public static final Map<String, OrderSession> ORDER_SESSIONS = new ConcurrentHashMap<>();

    // ── Service catalog ───────────────────────────────────────────────────────
    public static InvoiceService.OrderItem getOrderItem(String value) {
        return switch (value) {
            // Designer — main
            case "des_logo"       -> new InvoiceService.OrderItem("Logo Design", 30);
            case "des_identity"   -> new InvoiceService.OrderItem("Full Visual Identity", 60);
            case "des_posters"    -> new InvoiceService.OrderItem("Posters & Ads", 90);
            case "des_social"     -> new InvoiceService.OrderItem("Social Media Design", 20);
            case "des_discord"    -> new InvoiceService.OrderItem("Discord Welcome Pack", 20);
            case "des_covers"     -> new InvoiceService.OrderItem("Covers & Banners", 30);
            case "des_print"      -> new InvoiceService.OrderItem("Print (Cards / Brochure)", 25);
            case "des_motion"     -> new InvoiceService.OrderItem("Motion Graphic", 90);
            case "des_uiux"       -> new InvoiceService.OrderItem("UI/UX Design", 120);
            case "des_info"       -> new InvoiceService.OrderItem("Infographic", 40);
            case "des_emoji"      -> new InvoiceService.OrderItem("Emoji / Stickers Design", 30);
            // Designer — add-ons
            case "des_revisions"  -> new InvoiceService.OrderItem("Additional Revisions", 0);
            case "des_rush"       -> new InvoiceService.OrderItem("Rush Delivery", 45);
            case "des_source"     -> new InvoiceService.OrderItem("Source Files (AI/PSD)", 250);
            case "des_colors"     -> new InvoiceService.OrderItem("Multiple Color Variants", 35);
            case "des_anim"       -> new InvoiceService.OrderItem("Add Animation", 200);
            case "des_2rev"       -> new InvoiceService.OrderItem("2 Revisions After Delivery", 35);
            case "des_logosize"   -> new InvoiceService.OrderItem("Additional Logo Size", 10);
            case "des_copy"       -> new InvoiceService.OrderItem("Copywriting", 25);
            // Developer — main
            case "dev_web"        -> new InvoiceService.OrderItem("Web Developer", 50);
            case "dev_bot"        -> new InvoiceService.OrderItem("Bots Developer", 50);
            case "dev_fullstack"  -> new InvoiceService.OrderItem("Full-Stack", 100);
            case "dev_frontend"   -> new InvoiceService.OrderItem("Front-End", 30);
            case "dev_backend"    -> new InvoiceService.OrderItem("Back-End", 40);
            case "dev_ai"         -> new InvoiceService.OrderItem("AI & Automation", 100);
            case "dev_db"         -> new InvoiceService.OrderItem("Database Administrator", 30);
            // Developer — add-ons
            case "dev_revisions"  -> new InvoiceService.OrderItem("Additional Revisions", 0);
            case "dev_rush"       -> new InvoiceService.OrderItem("Rush Delivery", 70);
            case "dev_source"     -> new InvoiceService.OrderItem("Source Files", 150);
            case "dev_2rev"       -> new InvoiceService.OrderItem("2 Revisions After Delivery", 180);
            // Editor — main
            case "edit_reels"     -> new InvoiceService.OrderItem("Reels / Shorts Editor", 60);
            case "edit_longform"  -> new InvoiceService.OrderItem("Long-form Video Editor", 120);
            case "edit_animation" -> new InvoiceService.OrderItem("Animation Editor", 150);
            case "edit_gaming"    -> new InvoiceService.OrderItem("Gaming Editor", 150);
            // Editor — add-ons
            case "edit_revisions" -> new InvoiceService.OrderItem("Additional Edits", 0);
            case "edit_rush"      -> new InvoiceService.OrderItem("Rush Delivery", 45);
            case "edit_source"    -> new InvoiceService.OrderItem("Source Files (AI/PSD)", 250);
            case "edit_colors"    -> new InvoiceService.OrderItem("Multiple Color Variants", 35);
            case "edit_anim"      -> new InvoiceService.OrderItem("Add Animation", 200);
            case "edit_2rev"      -> new InvoiceService.OrderItem("2 Revisions After Delivery", 35);
            case "edit_logosize"  -> new InvoiceService.OrderItem("Additional Logo Size", 10);
            case "edit_copy"      -> new InvoiceService.OrderItem("Copywriting", 25);
            // Minecraft — main
            case "mc_plugin"      -> new InvoiceService.OrderItem("Plugin Developer", 50);
            case "mc_config"      -> new InvoiceService.OrderItem("Configuration Specialist", 80);
            case "mc_map"         -> new InvoiceService.OrderItem("Map Maker / Builder", 30);
            case "mc_pixel"       -> new InvoiceService.OrderItem("Pixel Artist / Texture Creator", 130);
            case "mc_3d"          -> new InvoiceService.OrderItem("3D Modeler (Blockbench)", 65);
            case "mc_admin"       -> new InvoiceService.OrderItem("Technical Admin / SysAdmin", 55);
            // Minecraft — add-ons
            case "mc_revisions"   -> new InvoiceService.OrderItem("Additional Modifications", 0);
            case "mc_rush"        -> new InvoiceService.OrderItem("Rush Delivery", 45);
            case "mc_source"      -> new InvoiceService.OrderItem("Source Files (AI/PSD)", 250);
            case "mc_colors"      -> new InvoiceService.OrderItem("Multiple Color Variants", 35);
            case "mc_anim"        -> new InvoiceService.OrderItem("Add Animation", 200);
            case "mc_2rev"        -> new InvoiceService.OrderItem("2 Revisions After Delivery", 35);
            case "mc_logosize"    -> new InvoiceService.OrderItem("Additional Logo Size", 10);
            case "mc_copy"        -> new InvoiceService.OrderItem("Copywriting", 25);
            default -> null;
        };
    }

    // ── Core reply helpers ────────────────────────────────────────────────────
    public static void reply(Object target, Object content) { handleReply(target, content, false); }
    public static void replyEphemeral(Object target, Object content) { handleReply(target, content, true); }

    private static void handleReply(Object target, Object content, boolean ephemeral) {
        List<MessageTopLevelComponent> components = new ArrayList<>();
        List<net.dv8tion.jda.api.entities.MessageEmbed> embeds = new ArrayList<>();

        if (content instanceof net.dv8tion.jda.api.entities.MessageEmbed me) embeds.add(me);
        else if (content instanceof MessageTopLevelComponent mtc) components.add(mtc);
        else if (content instanceof List<?> list) {
            for (Object obj : list) {
                if (obj instanceof MessageTopLevelComponent mtc) components.add(mtc);
                else if (obj instanceof net.dv8tion.jda.api.entities.MessageEmbed me) embeds.add(me);
            }
        }

        if (embeds.isEmpty() && components.isEmpty()) {
            components.add(EmbedUtil.info("High Core Agency", "No content provided."));
        }

        if (target instanceof IReplyCallback event) {
            if (event.isAcknowledged()) {
                InteractionHook hook = event.getHook();
                hook.editOriginal(new net.dv8tion.jda.api.utils.messages.MessageEditBuilder()
                    .setEmbeds(embeds).setComponents(components).useComponentsV2(true).build()).queue();
            } else {
                event.reply(new net.dv8tion.jda.api.utils.messages.MessageCreateBuilder()
                    .addEmbeds(embeds).setComponents(components).useComponentsV2(true).build()).setEphemeral(ephemeral).queue();
            }
        } else if (target instanceof net.dv8tion.jda.api.entities.channel.middleman.MessageChannel channel) {
            channel.sendMessage(new net.dv8tion.jda.api.utils.messages.MessageCreateBuilder()
                .addEmbeds(embeds).setComponents(components).useComponentsV2(true).build()).queue();
        }
    }

    // ── Hub panels (unchanged) ────────────────────────────────────────────────
    public static void sendStartupHub(IReplyCallback event) {
        ActionRow row = ActionRow.of(Button.secondary("hub_highcore", "Map"), Button.secondary("hub_about", "About"));
        reply(event, EmbedUtil.eliteContainer("High Core Agency", "Global operations hub.", null, row));
    }

    public static void sendServerMap(IReplyCallback event) {
        ActionRow row = ActionRow.of(Button.success("hub_pings", "Alerts"), Button.secondary("hub_rules", "Rules"));
        replyEphemeral(event, EmbedUtil.eliteContainer("Map", "Internal navigation active.", null, row));
    }

    public static void sendAboutUs(IReplyCallback event) {
        replyEphemeral(event, EmbedUtil.eliteContainer("About Us", "Elite creative identity.", null, ActionRow.of(Button.link("https://x.com/CoreHigh70331", "X"))));
    }

    public static void sendPartnersPanel(IReplyCallback event) { replyEphemeral(event, EmbedUtil.eliteContainer("Partners", "Strategic collaborations.", null)); }
    public static void sendPingsPanel(IReplyCallback event) { replyEphemeral(event, EmbedUtil.eliteContainer("Pings", "Select notification layers.", null)); }
    public static void sendStatsPanel(IReplyCallback event) { replyEphemeral(event, EmbedUtil.eliteContainer("Telemetry", "Systems Operational.", null)); }
    public static void sendPricesCategory(IReplyCallback event) { replyEphemeral(event, EmbedUtil.eliteContainer("Pricing", "Service modules processing.", null)); }
    public static void sendServicesCategory(IReplyCallback event) { replyEphemeral(event, EmbedUtil.eliteContainer("Services", "Agency assets online.", null)); }

    // ── /tickets command ──────────────────────────────────────────────────────
    public static void sendTicketPanel(IReplyCallback event) {
        String body =
            "📜 **RULES & GUIDELINES**\n\n" +
            "**Mutual Respect** — Please respect all staff members. " +
                "Any form of offensive behavior or harassment will not be tolerated.\n\n" +
            "**One Ticket** — Open only one ticket per issue. Do not open multiple tickets " +
                "for the same problem, inquiry, or to follow up on an existing ticket.\n\n" +
            "**Clarity** — Please fully describe your issue or request before a staff member " +
                "responds, to speed up the process.\n\n" +
            "**Content** — Spam and external links are strictly prohibited without staff authorization.\n\n" +
            "**Mentions** — Pinging or mentioning the staff member inside the ticket is strictly " +
                "forbidden under any circumstances.";

        ActionRow row = ActionRow.of(
            Button.primary("ticket_init_support",   "Technical Support").withEmoji(Emoji.fromUnicode("\uD83D\uDCA1")),
            Button.success("ticket_init_order",     "New Order")        .withEmoji(Emoji.fromUnicode("\uD83D\uDED2")),
            Button.danger("ticket_init_complaint",  "File Complaint")   .withEmoji(Emoji.fromUnicode("\u26A0\uFE0F"))
        );
        reply(event, EmbedUtil.containerBranded("TICKET SUPPORT", "High Core Agency", body, EmbedUtil.BANNER_TICKET_PANEL, null, row));
    }

    // ── Support flow — modal with 2 fields ───────────────────────────────────
    public static void handleSupportFlow(IReplyCallback event) {
        if (event instanceof net.dv8tion.jda.api.interactions.callbacks.IModalCallback modal) {
            TextInput issueInput   = TextInput.create("issue_desc",   TextInputStyle.PARAGRAPH).build();
            TextInput serviceInput = TextInput.create("service_type", TextInputStyle.SHORT).build();
            modal.replyModal(Modal.create("modal_support_init", "Technical Support")
                .addComponents(Label.of("What is your issue?", issueInput))
                .addComponents(Label.of("Which service? (Designer / Developer / Editor / Minecraft)", serviceInput))
                .build()).queue();
        }
    }

    // ── Complaint flow — modal with 3 fields ─────────────────────────────────
    public static void handleComplaintFlow(IReplyCallback event) {
        if (event instanceof net.dv8tion.jda.api.interactions.callbacks.IModalCallback modal) {
            TextInput issueTypeInput = TextInput.create("comp_type",   TextInputStyle.SHORT).build();
            TextInput personInput    = TextInput.create("comp_person", TextInputStyle.SHORT).build();
            TextInput descInput      = TextInput.create("comp_desc",   TextInputStyle.PARAGRAPH).build();
            modal.replyModal(Modal.create("modal_complaint_init", "File a Complaint")
                .addComponents(Label.of("Is this about a delivery issue or a specific staff member?", issueTypeInput))
                .addComponents(Label.of("Who is the staff member / person involved? (Write 'N/A' if none)", personInput))
                .addComponents(Label.of("Please describe the issue in full detail", descInput))
                .build()).queue();
        }
    }

    // ── Order flow — Step 1: choose sector ───────────────────────────────────
    public static void handleOrderFlow(IReplyCallback event) {
        StringSelectMenu menu = StringSelectMenu.create("order_sector_select")
            .setPlaceholder("— Select your service category —")
            .addOption("Designer",            "sect_designer",  "Logo, Identity, UI/UX, Motion, Emoji...")
            .addOption("Developer",           "sect_developer", "Web, Bot, Full-Stack, AI, Database...")
            .addOption("Editor & Animation",  "sect_editor",    "Reels, Long-form, Animation, Gaming...")
            .addOption("Minecraft Developer", "sect_minecraft", "Plugins, Maps, Texture, Blockbench...")
            .build();
        replyEphemeral(event, EmbedUtil.eliteContainer("New Order",
            "Select your required service category to begin.", null, ActionRow.of(menu)));
    }

    // ── Order flow — Step 2: choose main services ─────────────────────────────
    public static void handleSectorSelection(net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent event, String val) {
        String userId = event.getUser().getId();
        OrderSession session = ORDER_SESSIONS.computeIfAbsent(userId, k -> new OrderSession());
        session.sector = val.replace("sect_", "");
        session.mainServices.clear();
        session.addons.clear();

        StringSelectMenu.Builder builder = StringSelectMenu
            .create("order_service_select_" + session.sector)
            .setPlaceholder("— Select services (multiple allowed) —")
            .setMinValues(1);

        switch (session.sector) {
            case "designer" -> builder.setMaxValues(11)
                .addOption("Logo Design",              "des_logo",      "$30")
                .addOption("Full Visual Identity",     "des_identity",  "$60")
                .addOption("Posters & Ads",            "des_posters",   "$90")
                .addOption("Social Media Design",      "des_social",    "$20")
                .addOption("Discord Welcome Pack",     "des_discord",   "$20")
                .addOption("Covers & Banners",         "des_covers",    "$30")
                .addOption("Print (Cards / Brochure)", "des_print",     "$25")
                .addOption("Motion Graphic",           "des_motion",    "$90")
                .addOption("UI/UX Design",             "des_uiux",      "$120")
                .addOption("Infographic",              "des_info",      "$40")
                .addOption("Emoji / Stickers Design",  "des_emoji",     "$30");
            case "developer" -> builder.setMaxValues(7)
                .addOption("Web Developer",            "dev_web",       "$50")
                .addOption("Bots Developer",           "dev_bot",       "$50")
                .addOption("Full-Stack",               "dev_fullstack", "$100")
                .addOption("Front-End",                "dev_frontend",  "$30")
                .addOption("Back-End",                 "dev_backend",   "$40")
                .addOption("AI & Automation",          "dev_ai",        "$100")
                .addOption("Database Administrator",   "dev_db",        "$30");
            case "editor" -> builder.setMaxValues(4)
                .addOption("Reels / Shorts Editor",    "edit_reels",    "$60")
                .addOption("Long-form Video Editor",   "edit_longform", "$120")
                .addOption("Animation Editor",         "edit_animation","$150")
                .addOption("Gaming Editor",            "edit_gaming",   "$150");
            case "minecraft" -> builder.setMaxValues(6)
                .addOption("Plugin Developer",                  "mc_plugin",  "$50")
                .addOption("Configuration Specialist",          "mc_config",  "$80")
                .addOption("Map Maker / Builder",               "mc_map",     "$30")
                .addOption("Pixel Artist / Texture Creator",    "mc_pixel",   "$130")
                .addOption("3D Modeler (Blockbench)",           "mc_3d",      "$65")
                .addOption("Technical Admin / SysAdmin",        "mc_admin",   "$55");
        }

        String label = switch (session.sector) {
            case "designer"  -> "Designer";
            case "developer" -> "Developer";
            case "editor"    -> "Editor & Animation";
            case "minecraft" -> "Minecraft Developer";
            default          -> "Services";
        };
        reply(event, EmbedUtil.eliteContainer(label + " — Main Services",
            "Select one or more services. Prices are shown under each option.", null,
            ActionRow.of(builder.build())));
    }

    // ── Order flow — Step 3: choose add-ons ──────────────────────────────────
    public static void handleServiceSelection(net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent event, List<String> vals) {
        String userId = event.getUser().getId();
        OrderSession session = ORDER_SESSIONS.computeIfAbsent(userId, k -> new OrderSession());
        session.mainServices = new ArrayList<>(vals);
        String sector = event.getComponentId().replace("order_service_select_", "");

        StringSelectMenu addonMenu = switch (sector) {
            case "designer"  -> buildDesignerAddonMenu();
            case "developer" -> buildDeveloperAddonMenu();
            case "editor"    -> buildEditorAddonMenu();
            case "minecraft" -> buildMinecraftAddonMenu();
            default          -> buildDesignerAddonMenu();
        };

        reply(event, EmbedUtil.eliteContainer("Add-Ons & Extras",
            "Select any optional add-ons, or skip directly to project details.",
            null,
            ActionRow.of(addonMenu),
            ActionRow.of(Button.success("order_final_meta", "Skip & Proceed to Details \u2192")
                .withEmoji(Emoji.fromUnicode("\u2705")))));
    }

    // ── Order flow — Step 4: confirm add-ons, show summary ────────────────────
    public static void handleAddonSelection(net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent event, List<String> vals) {
        String userId = event.getUser().getId();
        OrderSession session = ORDER_SESSIONS.computeIfAbsent(userId, k -> new OrderSession());
        session.addons = new ArrayList<>(vals);

        double total = 0;
        StringBuilder mainLines = new StringBuilder();
        StringBuilder addonLines = new StringBuilder();

        for (String s : session.mainServices) {
            InvoiceService.OrderItem item = getOrderItem(s);
            if (item == null) continue;
            if (item.price > 0) {
                mainLines.append("`$").append(String.format("%-6.0f", item.price)).append("` ").append(item.name).append("\n");
                total += item.price;
            } else {
                mainLines.append("`TBD  ` ").append(item.name).append("\n");
            }
        }
        for (String a : session.addons) {
            InvoiceService.OrderItem item = getOrderItem(a);
            if (item == null) continue;
            if (item.price > 0) {
                addonLines.append("`$").append(String.format("%-6.0f", item.price)).append("` ").append(item.name).append("\n");
                total += item.price;
            } else {
                addonLines.append("`TBD  ` ").append(item.name).append("\n");
            }
        }

        StringBuilder summary = new StringBuilder();
        summary.append("Here's a review of everything you selected.\n\n");
        if (mainLines.length() > 0) {
            summary.append("**\uD83D\uDCCC Main Services**\n").append(mainLines);
        }
        if (addonLines.length() > 0) {
            summary.append("\n**\u2795 Add-Ons**\n").append(addonLines);
        }
        if (total > 0) {
            summary.append("\n**\uD83D\uDCB0 Estimated Total: `$").append(String.format("%.0f", total)).append("+`**\n");
        }
        summary.append("\nWhen you're ready, click **Proceed** to enter your project details and open the ticket.");

        reply(event, EmbedUtil.eliteContainer("Order Summary", summary.toString(), null,
            ActionRow.of(Button.success("order_final_meta", "Proceed \u2192")
                .withEmoji(Emoji.fromUnicode("\u2705")))));
    }

    // ── Order flow — Step 5: project details modal ────────────────────────────
    public static void handleOrderMetaModal(net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent event) {
        TextInput pName   = TextInput.create("p_name",    TextInputStyle.SHORT).build();
        TextInput cName   = TextInput.create("p_client",  TextInputStyle.SHORT).build();
        TextInput contact = TextInput.create("p_contact", TextInputStyle.SHORT).build();
        TextInput eta     = TextInput.create("p_eta",     TextInputStyle.SHORT).build();
        event.replyModal(Modal.create("modal_order_finalize", "Project Details")
            .addComponents(Label.of("Project Name",                              pName))
            .addComponents(Label.of("Your Full Name",                            cName))
            .addComponents(Label.of("Contact (Phone / Email)",                   contact))
            .addComponents(Label.of("Expected Delivery Period (e.g. 7-14 days)", eta))
            .build()).queue();
    }

    // ── Add-on menu builders ──────────────────────────────────────────────────
    private static StringSelectMenu buildDesignerAddonMenu() {
        return StringSelectMenu.create("order_addon_select_designer")
            .setPlaceholder("— Select add-ons (optional) —")
            .setMinValues(0).setMaxValues(8)
            .addOption("Additional Revisions",       "des_revisions", "Price: Varies")
            .addOption("Rush Delivery",              "des_rush",      "$45")
            .addOption("Source Files (AI/PSD)",      "des_source",    "$250")
            .addOption("Multiple Color Variants",    "des_colors",    "$35")
            .addOption("Add Animation",              "des_anim",      "$200")
            .addOption("2 Revisions After Delivery", "des_2rev",      "$35")
            .addOption("Additional Logo Size",       "des_logosize",  "$10")
            .addOption("Copywriting",                "des_copy",      "$25")
            .build();
    }

    private static StringSelectMenu buildDeveloperAddonMenu() {
        return StringSelectMenu.create("order_addon_select_developer")
            .setPlaceholder("— Select add-ons (optional) —")
            .setMinValues(0).setMaxValues(4)
            .addOption("Additional Revisions",       "dev_revisions", "Price: Varies")
            .addOption("Rush Delivery",              "dev_rush",      "$70")
            .addOption("Source Files",               "dev_source",    "$150")
            .addOption("2 Revisions After Delivery", "dev_2rev",      "$180")
            .build();
    }

    private static StringSelectMenu buildEditorAddonMenu() {
        return StringSelectMenu.create("order_addon_select_editor")
            .setPlaceholder("— Select add-ons (optional) —")
            .setMinValues(0).setMaxValues(8)
            .addOption("Additional Edits",           "edit_revisions", "Price: Varies")
            .addOption("Rush Delivery",              "edit_rush",      "$45")
            .addOption("Source Files (AI/PSD)",      "edit_source",    "$250")
            .addOption("Multiple Color Variants",    "edit_colors",    "$35")
            .addOption("Add Animation",              "edit_anim",      "$200")
            .addOption("2 Revisions After Delivery", "edit_2rev",      "$35")
            .addOption("Additional Logo Size",       "edit_logosize",  "$10")
            .addOption("Copywriting",                "edit_copy",      "$25")
            .build();
    }

    private static StringSelectMenu buildMinecraftAddonMenu() {
        return StringSelectMenu.create("order_addon_select_minecraft")
            .setPlaceholder("— Select add-ons (optional) —")
            .setMinValues(0).setMaxValues(8)
            .addOption("Additional Modifications",   "mc_revisions", "Price: Varies")
            .addOption("Rush Delivery",              "mc_rush",      "$45")
            .addOption("Source Files (AI/PSD)",      "mc_source",    "$250")
            .addOption("Multiple Color Variants",    "mc_colors",    "$35")
            .addOption("Add Animation",              "mc_anim",      "$200")
            .addOption("2 Revisions After Delivery", "mc_2rev",      "$35")
            .addOption("Additional Logo Size",       "mc_logosize",  "$10")
            .addOption("Copywriting",                "mc_copy",      "$25")
            .build();
    }
}
