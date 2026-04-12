package com.highcore.bot.services;

import com.highcore.bot.utils.EmbedUtil;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.interactions.callbacks.IModalCallback;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.MessageTopLevelComponent;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.components.mediagallery.MediaGallery;
import net.dv8tion.jda.api.components.mediagallery.MediaGalleryItem;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.separator.Separator.Spacing;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.modals.Modal;
import net.dv8tion.jda.api.entities.emoji.Emoji;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class PanelService {

    // ── Order session state ───────────────────────────────────────────────────
    public static class OrderSession {
        public String category;
        public List<String> mainIds  = new ArrayList<>();
        public List<String> addonIds = new ArrayList<>();
    }
    public static final Map<String, OrderSession> SESSIONS = new ConcurrentHashMap<>();

    // ── Service item lookup (id → [displayName, price]) ──────────────────────
    // Used to build InvoiceService.OrderItem list after the user finalises
    public static final Map<String, Object[]> ALL_ITEMS = new HashMap<>();
    static {
        // ── Designer — main
        ALL_ITEMS.put("ds_logo",     new Object[]{"Logo Design",                       30.0});
        ALL_ITEMS.put("ds_identity", new Object[]{"Full Visual Identity",              60.0});
        ALL_ITEMS.put("ds_posters",  new Object[]{"Posters & Ads",                     90.0});
        ALL_ITEMS.put("ds_social",   new Object[]{"Social Media Design",               20.0});
        ALL_ITEMS.put("ds_discord",  new Object[]{"Discord Welcome Pack",              20.0});
        ALL_ITEMS.put("ds_banners",  new Object[]{"Covers & Banners",                  30.0});
        ALL_ITEMS.put("ds_print",    new Object[]{"Prints & Brochures",                25.0});
        ALL_ITEMS.put("ds_motion",   new Object[]{"Motion Graphic",                    90.0});
        ALL_ITEMS.put("ds_uiux",     new Object[]{"UI/UX Design",                     120.0});
        ALL_ITEMS.put("ds_info",     new Object[]{"Infographic",                       40.0});
        ALL_ITEMS.put("ds_emoji",    new Object[]{"Emoji / Stickers",                  30.0});
        // ── Designer — add-ons
        ALL_ITEMS.put("da_revisions",new Object[]{"Additional Revisions (Quote)",       0.0});
        ALL_ITEMS.put("da_rush",     new Object[]{"Rush Delivery",                     45.0});
        ALL_ITEMS.put("da_source",   new Object[]{"Source Files (AI/PSD)",            250.0});
        ALL_ITEMS.put("da_colors",   new Object[]{"Color Variants",                    35.0});
        ALL_ITEMS.put("da_anim",     new Object[]{"Add Animation",                    200.0});
        ALL_ITEMS.put("da_2rev",     new Object[]{"2 Revisions After Delivery",        35.0});
        ALL_ITEMS.put("da_logosize", new Object[]{"Additional Logo Size",              10.0});
        ALL_ITEMS.put("da_copy",     new Object[]{"Copywriting",                       25.0});
        // ── Developer — main
        ALL_ITEMS.put("dv_web",      new Object[]{"Web Developer",                     50.0});
        ALL_ITEMS.put("dv_bots",     new Object[]{"Bots Developer",                    50.0});
        ALL_ITEMS.put("dv_full",     new Object[]{"Full-Stack Developer",             100.0});
        ALL_ITEMS.put("dv_front",    new Object[]{"Front-End",                         30.0});
        ALL_ITEMS.put("dv_back",     new Object[]{"Back-End",                          40.0});
        ALL_ITEMS.put("dv_ai",       new Object[]{"AI & Automation",                  100.0});
        ALL_ITEMS.put("dv_db",       new Object[]{"Database Administrator",            30.0});
        // ── Developer — add-ons
        ALL_ITEMS.put("dva_revisions",new Object[]{"Additional Revisions (Quote)",      0.0});
        ALL_ITEMS.put("dva_rush",    new Object[]{"Rush Delivery",                     70.0});
        ALL_ITEMS.put("dva_source",  new Object[]{"Source Files",                     150.0});
        ALL_ITEMS.put("dva_2rev",    new Object[]{"2 Revisions After Delivery",       180.0});
        // ── Editor — main
        ALL_ITEMS.put("ed_reels",    new Object[]{"Reels / Shorts Editor",             60.0});
        ALL_ITEMS.put("ed_long",     new Object[]{"Long-form Video Editor",           120.0});
        ALL_ITEMS.put("ed_anim",     new Object[]{"Animation Editor",                 150.0});
        ALL_ITEMS.put("ed_gaming",   new Object[]{"Gaming Editor",                    150.0});
        // ── Editor — add-ons
        ALL_ITEMS.put("eda_revisions",new Object[]{"Additional Revisions (Quote)",      0.0});
        ALL_ITEMS.put("eda_rush",    new Object[]{"Rush Delivery",                     45.0});
        ALL_ITEMS.put("eda_source",  new Object[]{"Source Files (AI/PSD)",            250.0});
        ALL_ITEMS.put("eda_colors",  new Object[]{"Color Variants",                    35.0});
        ALL_ITEMS.put("eda_anim",    new Object[]{"Add Animation",                    200.0});
        ALL_ITEMS.put("eda_2rev",    new Object[]{"2 Revisions After Delivery",        35.0});
        ALL_ITEMS.put("eda_size",    new Object[]{"Additional Size",                   10.0});
        ALL_ITEMS.put("eda_copy",    new Object[]{"Copywriting",                       25.0});
        // ── Minecraft — main
        ALL_ITEMS.put("mc_plugin",   new Object[]{"Plugin Developer",                  50.0});
        ALL_ITEMS.put("mc_config",   new Object[]{"Configuration Specialist",          80.0});
        ALL_ITEMS.put("mc_map",      new Object[]{"Map Maker / Builder",               30.0});
        ALL_ITEMS.put("mc_pixel",    new Object[]{"Pixel Artist / Texture Creator",   130.0});
        ALL_ITEMS.put("mc_3d",       new Object[]{"3D Modeler (Blockbench)",           65.0});
        ALL_ITEMS.put("mc_admin",    new Object[]{"Technical Admin / SysAdmin",        55.0});
        // ── Minecraft — add-ons
        ALL_ITEMS.put("mca_revisions",new Object[]{"Additional Revisions (Quote)",      0.0});
        ALL_ITEMS.put("mca_rush",    new Object[]{"Rush Delivery",                     45.0});
        ALL_ITEMS.put("mca_source",  new Object[]{"Source Files (AI/PSD)",            250.0});
        ALL_ITEMS.put("mca_colors",  new Object[]{"Color Variants",                    35.0});
        ALL_ITEMS.put("mca_anim",    new Object[]{"Add Animation",                    200.0});
        ALL_ITEMS.put("mca_2rev",    new Object[]{"2 Revisions After Delivery",        35.0});
        ALL_ITEMS.put("mca_mod",     new Object[]{"Additional Modification",           10.0});
        ALL_ITEMS.put("mca_copy",    new Object[]{"Copywriting",                       25.0});
    }

    // Resolve a list of item IDs to InvoiceService.OrderItem (price=0 items kept for display)
    public static List<InvoiceService.OrderItem> resolveItems(List<String> ids) {
        List<InvoiceService.OrderItem> out = new ArrayList<>();
        for (String id : ids) {
            Object[] meta = ALL_ITEMS.get(id);
            if (meta != null) out.add(new InvoiceService.OrderItem((String) meta[0], (double) meta[1]));
        }
        return out;
    }

    // ── Core reply helpers ────────────────────────────────────────────────────
    public static void reply(Object target, Object content) { handleReply(target, content, false); }
    public static void replyEphemeral(Object target, Object content) { handleReply(target, content, true); }

    private static void handleReply(Object target, Object content, boolean ephemeral) {
        List<MessageTopLevelComponent> components = new ArrayList<>();
        if (content instanceof MessageTopLevelComponent mtc) {
            components.add(mtc);
        } else if (content instanceof List<?> list) {
            for (Object obj : list)
                if (obj instanceof MessageTopLevelComponent mtc) components.add(mtc);
        }
        if (components.isEmpty()) {
            components.add(EmbedUtil.info("High Core Agency", "No content provided."));
        }
        if (target instanceof IReplyCallback event) {
            if (event.isAcknowledged()) {
                event.getHook().editOriginalComponents(components).useComponentsV2(true).queue();
            } else {
                event.replyComponents(components).useComponentsV2(true).setEphemeral(ephemeral).queue();
            }
        } else if (target instanceof net.dv8tion.jda.api.entities.channel.middleman.MessageChannel channel) {
            channel.sendMessageComponents(components).useComponentsV2(true).queue();
        }
    }

    // ── Hub panels ────────────────────────────────────────────────────────────
    public static void sendStartupHub(IReplyCallback event) {
        ActionRow row = ActionRow.of(Button.secondary("hub_highcore", "Map"), Button.secondary("hub_about", "About"));
        reply(event, EmbedUtil.eliteContainer("High Core Agency", "Global operations hub.", null, row));
    }

    public static void sendServerMap(IReplyCallback event) {
        ActionRow row = ActionRow.of(Button.success("hub_pings", "Alerts"), Button.secondary("hub_rules", "Rules"));
        replyEphemeral(event, EmbedUtil.eliteContainer("Map", "Internal navigation active.", null, row));
    }

    public static void sendAboutUs(IReplyCallback event) {
        replyEphemeral(event, EmbedUtil.eliteContainer("About Us", "Elite creative identity.", null,
                ActionRow.of(Button.link("https://x.com/CoreHigh70331", "X"))));
    }

    public static void sendPartnersPanel(IReplyCallback event) { replyEphemeral(event, EmbedUtil.eliteContainer("Partners", "Strategic collaborations.", null)); }
    public static void sendPingsPanel(IReplyCallback event)    { replyEphemeral(event, EmbedUtil.eliteContainer("Pings", "Select notification layers.", null)); }
    public static void sendStatsPanel(IReplyCallback event)    { replyEphemeral(event, EmbedUtil.eliteContainer("Telemetry", "Systems Operational.", null)); }
    public static void sendPricesCategory(IReplyCallback event){ replyEphemeral(event, EmbedUtil.eliteContainer("Pricing", "Service modules processing.", null)); }
    public static void sendServicesCategory(IReplyCallback event){ replyEphemeral(event, EmbedUtil.eliteContainer("Services", "Agency assets online.", null)); }

    // ── /tickets command ──────────────────────────────────────────────────────
    public static void sendTicketPanel(IReplyCallback event) {
        String imageUrl = "https://cdn.discordapp.com/attachments/1488900668042510568/1492305839736750230/IMG_20260411_023024.png" +
                          "?ex=69dad99d&is=69d9881d&hm=9df0283d5f26dc60385980e7f3d713966c15e2505d78aa2b9da35f9359901046&";
        String rules =
            "\uD83D\uDCDC **RULES & GUIDELINES**\n\n" +
            "**Mutual Respect** — Please respect all staff members. Any form of offensive behavior or harassment will not be tolerated.\n\n" +
            "**One Ticket** — Open only one ticket per issue. Do not open multiple tickets for the same problem.\n\n" +
            "**Clarity** — Please fully describe your issue or request before a staff member responds.\n\n" +
            "**Content** — Spam and external links are strictly prohibited without staff authorization.\n\n" +
            "**Mentions** — Pinging or mentioning the staff member inside the ticket is strictly forbidden.";

        List<ContainerChildComponent> children = new ArrayList<>();
        children.add(MediaGallery.of(MediaGalleryItem.fromUrl(imageUrl)));
        children.add(TextDisplay.of("## TICKET SUPPORT | High Core Agency"));
        children.add(Separator.createDivider(Spacing.SMALL));
        children.add(TextDisplay.of(rules));
        children.add(Separator.createDivider(Spacing.SMALL));
        children.add(ActionRow.of(
            Button.primary("ticket_init_support",  "\uD83D\uDCA1 Technical Support"),
            Button.success("ticket_init_order",    "\uD83D\uDED2 New Order"),
            Button.danger("ticket_init_complaint", "\u26A0\uFE0F File Complaint")
        ));
        reply(event, Container.of(children));
    }

    // ── Support flow ──────────────────────────────────────────────────────────
    public static void handleSupportFlow(IReplyCallback event) {
        if (event instanceof IModalCallback modal) {
            TextInput issueInput   = TextInput.create("issue_desc",   TextInputStyle.PARAGRAPH).build();
            TextInput serviceInput = TextInput.create("service_type", TextInputStyle.SHORT).build();
            modal.replyModal(Modal.create("modal_support_init", "Technical Support")
                .addComponents(Label.of("Describe your issue", issueInput))
                .addComponents(Label.of("Service (Designer/Dev/Editor/MC)", serviceInput))
                .build()).queue();
        }
    }

    // ── Complaint flow ────────────────────────────────────────────────────────
    public static void handleComplaintFlow(IReplyCallback event) {
        if (event instanceof IModalCallback modal) {
            TextInput issueTypeInput = TextInput.create("comp_type",   TextInputStyle.SHORT).build();
            TextInput personInput    = TextInput.create("comp_person", TextInputStyle.SHORT).build();
            TextInput descInput      = TextInput.create("comp_desc",   TextInputStyle.PARAGRAPH).build();
            modal.replyModal(Modal.create("modal_complaint_init", "File a Complaint")
                .addComponents(Label.of("Delivery issue or staff member?", issueTypeInput))
                .addComponents(Label.of("Staff / person involved (or N/A)", personInput))
                .addComponents(Label.of("Describe the issue in full detail", descInput))
                .build()).queue();
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // ORDER FLOW — Step 1: Category select
    // ═══════════════════════════════════════════════════════════════════
    public static void handleOrderFlow(IReplyCallback event) {
        List<ContainerChildComponent> children = new ArrayList<>();
        children.add(TextDisplay.of(
            "## \uD83D\uDED2 New Order — Select Category\n" +
            "Choose the service category that matches your project."));
        children.add(Separator.createDivider(Spacing.SMALL));
        children.add(ActionRow.of(
            StringSelectMenu.create("order_service_select")
                .setPlaceholder("Choose a service category...")
                .setMinValues(1).setMaxValues(1)
                .addOption("Designer",           "designer",  "Logos, Branding, UI/UX, Motion",     Emoji.fromUnicode("\uD83C\uDFA8"))
                .addOption("Developer",          "developer", "Web, Bots, Full-Stack, AI",           Emoji.fromUnicode("\uD83D\uDCBB"))
                .addOption("Editor & Animation", "editor",    "Reels, Long-form, Gaming, Animation", Emoji.fromUnicode("\uD83C\uDFAC"))
                .addOption("Minecraft Developer","minecraft", "Plugins, Maps, Models, SysAdmin",     Emoji.fromUnicode("\u26CF\uFE0F"))
                .build()
        ));
        replyEphemeral(event, Container.of(children));
    }

    // ═══════════════════════════════════════════════════════════════════
    // ORDER FLOW — Step 2: Main services multi-select
    // Called after order_service_select fires
    // ═══════════════════════════════════════════════════════════════════
    public static void handleCategorySelected(IReplyCallback event, String userId, String category) {
        OrderSession session = new OrderSession();
        session.category = category;
        SESSIONS.put(userId, session);

        String catLabel = switch (category) {
            case "designer"  -> "\uD83C\uDFA8 Designer";
            case "developer" -> "\uD83D\uDCBB Developer";
            case "editor"    -> "\uD83C\uDFAC Editor & Animation";
            case "minecraft" -> "\u26CF\uFE0F Minecraft Developer";
            default          -> category;
        };

        StringSelectMenu mainMenu = buildMainMenu(category);

        List<ContainerChildComponent> children = new ArrayList<>();
        children.add(TextDisplay.of(
            "## " + catLabel + " — Select Services\n" +
            "Select one or more services. Prices are shown below each option."));
        children.add(Separator.createDivider(Spacing.SMALL));
        children.add(ActionRow.of(mainMenu));
        reply(event, Container.of(children));
    }

    // ═══════════════════════════════════════════════════════════════════
    // ORDER FLOW — Step 3: Add-ons multi-select
    // Called after order_main_select fires
    // ═══════════════════════════════════════════════════════════════════
    public static void handleMainSelected(IReplyCallback event, String userId, List<String> selected) {
        OrderSession session = SESSIONS.computeIfAbsent(userId, k -> new OrderSession());
        session.mainIds = new ArrayList<>(selected);

        // Build summary line
        String summary = selected.stream()
            .map(id -> {
                Object[] meta = ALL_ITEMS.get(id);
                return meta != null ? (String) meta[0] : id;
            })
            .collect(Collectors.joining(", "));

        StringSelectMenu addonMenu = buildAddonMenu(session.category);

        List<ContainerChildComponent> children = new ArrayList<>();
        children.add(TextDisplay.of(
            "## Add-ons (Optional)\n" +
            "**Selected services:** " + summary + "\n\n" +
            "Now choose any add-ons, or click **Confirm Order** to skip."));
        children.add(Separator.createDivider(Spacing.SMALL));
        children.add(ActionRow.of(addonMenu));
        children.add(Separator.createDivider(Spacing.SMALL));
        children.add(ActionRow.of(
            Button.success("order_open_ticket", "Confirm Order \u2192")
                .withEmoji(Emoji.fromUnicode("\uD83D\uDCDD"))
        ));
        reply(event, Container.of(children));
    }

    // ═══════════════════════════════════════════════════════════════════
    // ORDER FLOW — Step 3b: Add-ons selected, update view
    // Called after order_addon_select fires
    // ═══════════════════════════════════════════════════════════════════
    public static void handleAddonsSelected(IReplyCallback event, String userId, List<String> selected) {
        OrderSession session = SESSIONS.computeIfAbsent(userId, k -> new OrderSession());
        session.addonIds = new ArrayList<>(selected);

        String mainSummary = session.mainIds.stream()
            .map(id -> { Object[] m = ALL_ITEMS.get(id); return m != null ? (String) m[0] : id; })
            .collect(Collectors.joining(", "));
        String addonSummary = selected.stream()
            .map(id -> { Object[] m = ALL_ITEMS.get(id); return m != null ? (String) m[0] : id; })
            .collect(Collectors.joining(", "));

        List<ContainerChildComponent> children = new ArrayList<>();
        children.add(TextDisplay.of(
            "## Order Summary\n" +
            "**Services:** " + mainSummary + "\n" +
            "**Add-ons:** " + addonSummary + "\n\n" +
            "Click **Confirm Order** to fill in your project details and open your ticket."));
        children.add(Separator.createDivider(Spacing.SMALL));
        children.add(ActionRow.of(
            Button.success("order_open_ticket", "Confirm Order \u2192")
                .withEmoji(Emoji.fromUnicode("\uD83D\uDCDD"))
        ));
        reply(event, Container.of(children));
    }

    // ═══════════════════════════════════════════════════════════════════
    // ORDER FLOW — Step 4: Final details modal
    // Triggered by order_open_ticket button
    // ═══════════════════════════════════════════════════════════════════
    public static void handleOrderFinalModal(IReplyCallback event) {
        if (event instanceof IModalCallback modal) {
            TextInput projectInput  = TextInput.create("o_project",  TextInputStyle.SHORT).build();
            TextInput nameInput     = TextInput.create("o_name",     TextInputStyle.SHORT).build();
            TextInput contactInput  = TextInput.create("o_contact",  TextInputStyle.SHORT).build();
            TextInput etaInput      = TextInput.create("o_eta",      TextInputStyle.SHORT).build();
            modal.replyModal(Modal.create("modal_order_final", "Order Details")
                .addComponents(Label.of("Project Name",             projectInput))
                .addComponents(Label.of("Your Full Name",           nameInput))
                .addComponents(Label.of("Contact (Phone / Email)",  contactInput))
                .addComponents(Label.of("Expected Delivery Time",   etaInput))
                .build()).queue();
        }
    }

    // ── Select menu builders ──────────────────────────────────────────────────
    private static StringSelectMenu buildMainMenu(String cat) {
        StringSelectMenu.Builder b = StringSelectMenu.create("order_main_select")
            .setPlaceholder("Select services (multiple allowed)...");

        List<String[]> items = switch (cat) {
            case "designer" -> List.of(
                new String[]{"ds_logo",     "Logo Design",                   "$30"},
                new String[]{"ds_identity", "Full Visual Identity",           "$60"},
                new String[]{"ds_posters",  "Posters & Ads",                  "$90"},
                new String[]{"ds_social",   "Social Media Design",            "$20"},
                new String[]{"ds_discord",  "Discord Welcome Pack",           "$20"},
                new String[]{"ds_banners",  "Covers & Banners",               "$30"},
                new String[]{"ds_print",    "Prints & Brochures",             "$25"},
                new String[]{"ds_motion",   "Motion Graphic",                 "$90"},
                new String[]{"ds_uiux",     "UI/UX Design",                   "$120"},
                new String[]{"ds_info",     "Infographic",                    "$40"},
                new String[]{"ds_emoji",    "Emoji / Stickers",               "$30"}
            );
            case "developer" -> List.of(
                new String[]{"dv_web",   "Web Developer",             "$50"},
                new String[]{"dv_bots",  "Bots Developer",            "$50"},
                new String[]{"dv_full",  "Full-Stack Developer",      "$100"},
                new String[]{"dv_front", "Front-End",                 "$30"},
                new String[]{"dv_back",  "Back-End",                  "$40"},
                new String[]{"dv_ai",    "AI & Automation",           "$100"},
                new String[]{"dv_db",    "Database Administrator",    "$30"}
            );
            case "editor" -> List.of(
                new String[]{"ed_reels",  "Reels / Shorts Editor",    "$60"},
                new String[]{"ed_long",   "Long-form Video Editor",   "$120"},
                new String[]{"ed_anim",   "Animation Editor",         "$150"},
                new String[]{"ed_gaming", "Gaming Editor",            "$150"}
            );
            case "minecraft" -> List.of(
                new String[]{"mc_plugin", "Plugin Developer",               "$50"},
                new String[]{"mc_config", "Configuration Specialist",       "$80"},
                new String[]{"mc_map",    "Map Maker / Builder",            "$30"},
                new String[]{"mc_pixel",  "Pixel Artist / Texture Creator", "$130"},
                new String[]{"mc_3d",     "3D Modeler (Blockbench)",        "$65"},
                new String[]{"mc_admin",  "Technical Admin / SysAdmin",     "$55"}
            );
            default -> List.of();
        };

        for (String[] item : items) b.addOption(item[1], item[0], item[2]);
        b.setMinValues(1).setMaxValues(items.size());
        return b.build();
    }

    private static StringSelectMenu buildAddonMenu(String cat) {
        StringSelectMenu.Builder b = StringSelectMenu.create("order_addon_select")
            .setPlaceholder("Select add-ons (optional, multiple allowed)...");

        List<String[]> items = switch (cat) {
            case "developer" -> List.of(
                new String[]{"dva_revisions", "Additional Revisions",        "Quote"},
                new String[]{"dva_rush",      "Rush Delivery",               "$70"},
                new String[]{"dva_source",    "Source Files",                "$150"},
                new String[]{"dva_2rev",      "2 Revisions After Delivery",  "$180"}
            );
            default -> {
                String prefix = switch (cat) {
                    case "designer"  -> "da";
                    case "editor"    -> "eda";
                    case "minecraft" -> "mca";
                    default          -> "da";
                };
                String lastLabel = switch (cat) {
                    case "designer"  -> "Additional Logo Size — $10";
                    case "editor"    -> "Additional Size — $10";
                    case "minecraft" -> "Additional Modification — $10";
                    default          -> "Additional Size — $10";
                };
                String lastId = switch (cat) {
                    case "designer"  -> prefix + "_logosize";
                    case "editor"    -> prefix + "_size";
                    case "minecraft" -> prefix + "_mod";
                    default          -> prefix + "_size";
                };
                yield List.of(
                    new String[]{prefix + "_revisions", "Additional Revisions",        "Quote"},
                    new String[]{prefix + "_rush",      "Rush Delivery",               cat.equals("editor") ? "$45" : "$45"},
                    new String[]{prefix + "_source",    "Source Files (AI/PSD)",       "$250"},
                    new String[]{prefix + "_colors",    "Color Variants",              "$35"},
                    new String[]{prefix + "_anim",      "Add Animation",               "$200"},
                    new String[]{prefix + "_2rev",      "2 Revisions After Delivery",  "$35"},
                    new String[]{lastId,                 lastLabel.split(" — ")[0],     lastLabel.split(" — ")[1]},
                    new String[]{prefix + "_copy",      "Copywriting",                 "$25"}
                );
            }
        };

        for (String[] item : items) b.addOption(item[1], item[0], item[2]);
        b.setMinValues(0).setMaxValues(items.size());
        return b.build();
    }
}
