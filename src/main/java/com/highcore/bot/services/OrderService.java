package com.highcore.bot.services;

import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.modals.Modal;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import com.highcore.bot.utils.EmbedUtil;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class OrderService {

    // ── Data model ────────────────────────────────────────────────────────────
    public static class ServiceItem {
        public final String id, name, price;
        public ServiceItem(String id, String name, String price) {
            this.id = id; this.name = name; this.price = price;
        }
    }

    // ── Session ───────────────────────────────────────────────────────────────
    public static class OrderSession {
        public String category;
        public List<String> selectedServices = new ArrayList<>();
        public List<String> selectedAddons   = new ArrayList<>();
        public OrderSession(String category) { this.category = category; }
    }

    public static final Map<String, OrderSession> sessions = new ConcurrentHashMap<>();

    // ── All items lookup (id → [name, price]) ─────────────────────────────────
    public static final Map<String, String>   ITEM_NAMES  = new ConcurrentHashMap<>();
    public static final Map<String, double[]> ITEM_PRICES = new ConcurrentHashMap<>();
    static {
        // Designer main
        put("ds_logo",     "Logo Design",                       30.0);
        put("ds_identity", "Full Visual Identity",              60.0);
        put("ds_posters",  "Posters & Ads",                     90.0);
        put("ds_social",   "Social Media Design",               20.0);
        put("ds_discord",  "Discord Welcome Pack",              20.0);
        put("ds_banners",  "Covers & Banners",                  30.0);
        put("ds_print",    "Prints & Brochures",                25.0);
        put("ds_motion",   "Motion Graphic",                    90.0);
        put("ds_uiux",     "UI/UX Design",                     120.0);
        put("ds_info",     "Infographic",                       40.0);
        put("ds_emoji",    "Emoji / Stickers",                  30.0);
        // Designer add-ons
        put("da_rev",      "Additional Revisions (Quote)",       0.0);
        put("da_rush",     "Rush Delivery",                     45.0);
        put("da_source",   "Source Files (AI/PSD)",            250.0);
        put("da_colors",   "Color Variants",                    35.0);
        put("da_anim",     "Add Animation",                    200.0);
        put("da_2rev",     "2 Revisions After Delivery",        35.0);
        put("da_logosize", "Additional Logo Size",              10.0);
        put("da_copy",     "Copywriting",                       25.0);
        // Developer main
        put("dv_web",      "Web Developer",                     50.0);
        put("dv_bots",     "Bots Developer",                    50.0);
        put("dv_full",     "Full-Stack Developer",             100.0);
        put("dv_front",    "Front-End",                         30.0);
        put("dv_back",     "Back-End",                          40.0);
        put("dv_ai",       "AI & Automation",                  100.0);
        put("dv_db",       "Database Administrator",            30.0);
        // Developer add-ons
        put("dva_rev",     "Additional Revisions (Quote)",       0.0);
        put("dva_rush",    "Rush Delivery",                     70.0);
        put("dva_source",  "Source Files",                     150.0);
        put("dva_2rev",    "2 Revisions After Delivery",       180.0);
        // Editor main
        put("ed_reels",    "Reels / Shorts Editor",             60.0);
        put("ed_long",     "Long-form Video Editor",           120.0);
        put("ed_anim",     "Animation Editor",                 150.0);
        put("ed_gaming",   "Gaming Editor",                    150.0);
        // Editor add-ons
        put("eda_rev",     "Additional Revisions (Quote)",       0.0);
        put("eda_rush",    "Rush Delivery",                     45.0);
        put("eda_source",  "Source Files (AI/PSD)",            250.0);
        put("eda_colors",  "Color Variants",                    35.0);
        put("eda_anim",    "Add Animation",                    200.0);
        put("eda_2rev",    "2 Revisions After Delivery",        35.0);
        put("eda_size",    "Additional Size",                   10.0);
        put("eda_copy",    "Copywriting",                       25.0);
        // Minecraft main
        put("mc_plugin",   "Plugin Developer",                  50.0);
        put("mc_config",   "Configuration Specialist",          80.0);
        put("mc_map",      "Map Maker / Builder",               30.0);
        put("mc_pixel",    "Pixel Artist / Texture Creator",   130.0);
        put("mc_3d",       "3D Modeler (Blockbench)",           65.0);
        put("mc_admin",    "Technical Admin / SysAdmin",        55.0);
        // Minecraft add-ons
        put("mca_rev",     "Additional Revisions (Quote)",       0.0);
        put("mca_rush",    "Rush Delivery",                     45.0);
        put("mca_source",  "Source Files (AI/PSD)",            250.0);
        put("mca_colors",  "Color Variants",                    35.0);
        put("mca_anim",    "Add Animation",                    200.0);
        put("mca_2rev",    "2 Revisions After Delivery",        35.0);
        put("mca_mod",     "Additional Modification",           10.0);
        put("mca_copy",    "Copywriting",                       25.0);
    }
    private static void put(String id, String name, double price) {
        ITEM_NAMES.put(id, name);
        ITEM_PRICES.put(id, new double[]{price});
    }

    // ── Convert selected IDs to InvoiceService items ──────────────────────────
    public static List<InvoiceService.OrderItem> resolveItems(List<String> ids) {
        List<InvoiceService.OrderItem> out = new ArrayList<>();
        for (String id : ids) {
            String name  = ITEM_NAMES.get(id);
            double[] p   = ITEM_PRICES.get(id);
            if (name != null && p != null) out.add(new InvoiceService.OrderItem(name, p[0]));
        }
        return out;
    }

    // ═══════════════════════════════════════════════════════════
    // STEP 1 — Category select (triggered by ticket_type_select → "purchase")
    // ═══════════════════════════════════════════════════════════
    public static void startWizard(Object event) {
        StringSelectMenu menu = StringSelectMenu.create("order_wiz_cat")
            .setPlaceholder("Select a service category...")
            .setMinValues(1).setMaxValues(1)
            .addOption("Designer",           "wiz_designer",  "Logos, Branding, UI/UX, Motion",     Emoji.fromUnicode("\uD83C\uDFA8"))
            .addOption("Developer",          "wiz_developer", "Web, Bots, Full-Stack, AI",           Emoji.fromUnicode("\uD83D\uDCBB"))
            .addOption("Editor & Animation", "wiz_editor",    "Reels, Long-form, Gaming, Animation", Emoji.fromUnicode("\uD83C\uDFAC"))
            .addOption("Minecraft Developer","wiz_minecraft", "Plugins, Maps, Models, SysAdmin",     Emoji.fromUnicode("\u26CF\uFE0F"))
            .build();

        PanelService.replyEphemeral(event, List.of(EmbedUtil.containerBranded(
            "NEW ORDER", "Select Department",
            "Choose the service category that matches your project.",
            EmbedUtil.BANNER_MAIN), ActionRow.of(menu)));
    }

    // ═══════════════════════════════════════════════════════════
    // STEP 2 — Main services multi-select
    // ═══════════════════════════════════════════════════════════
    public static void handleCategory(StringSelectInteractionEvent event) {
        String cat = event.getValues().get(0).replace("wiz_", "");
        sessions.put(event.getUser().getId(), new OrderSession(cat));
        sendServiceSelection(event, cat);
    }

    private static void sendServiceSelection(Object event, String cat) {
        String catLabel = switch (cat) {
            case "designer"  -> "\uD83C\uDFA8 Designer";
            case "developer" -> "\uD83D\uDCBB Developer";
            case "editor"    -> "\uD83C\uDFAC Editor & Animation";
            case "minecraft" -> "\u26CF\uFE0F Minecraft Developer";
            default -> cat;
        };

        StringSelectMenu.Builder b = StringSelectMenu.create("wiz_sel_services")
            .setPlaceholder("Select services (multiple allowed)...")
            .setMinValues(1);

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
                new String[]{"dv_web",   "Web Developer",            "$50"},
                new String[]{"dv_bots",  "Bots Developer",           "$50"},
                new String[]{"dv_full",  "Full-Stack Developer",     "$100"},
                new String[]{"dv_front", "Front-End",                "$30"},
                new String[]{"dv_back",  "Back-End",                 "$40"},
                new String[]{"dv_ai",    "AI & Automation",          "$100"},
                new String[]{"dv_db",    "Database Administrator",   "$30"}
            );
            case "editor" -> List.of(
                new String[]{"ed_reels",  "Reels / Shorts Editor",   "$60"},
                new String[]{"ed_long",   "Long-form Video Editor",  "$120"},
                new String[]{"ed_anim",   "Animation Editor",        "$150"},
                new String[]{"ed_gaming", "Gaming Editor",           "$150"}
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
        b.setMaxValues(items.size());

        PanelService.reply(event, List.of(EmbedUtil.containerBranded(
            "SELECT SERVICES", catLabel,
            "Choose one or more services. Prices are shown as descriptions.",
            EmbedUtil.BANNER_MAIN), ActionRow.of(b.build())));
    }

    // ═══════════════════════════════════════════════════════════
    // STEP 3 — Add-ons multi-select (+ Confirm button)
    // ═══════════════════════════════════════════════════════════
    public static void handleMultiSelection(StringSelectInteractionEvent event) {
        OrderSession session = sessions.get(event.getUser().getId());
        if (session == null) {
            PanelService.replyEphemeral(event, EmbedUtil.error("SESSION EXPIRED", "Session lost. Please restart via the ticket panel."));
            return;
        }

        String id = event.getComponentId();

        if (id.equals("wiz_sel_services")) {
            session.selectedServices = new ArrayList<>(event.getValues());
            sendAddonSelection(event, session);
        } else if (id.equals("wiz_sel_addons")) {
            session.selectedAddons = new ArrayList<>(event.getValues());
            sendConfirmView(event, session);
        }
    }

    private static void sendAddonSelection(Object event, OrderSession session) {
        String prefix = switch (session.category) {
            case "developer" -> "dva";
            case "editor"    -> "eda";
            case "minecraft" -> "mca";
            default          -> "da";
        };
        boolean isDev = session.category.equals("developer");

        StringSelectMenu.Builder b = StringSelectMenu.create("wiz_sel_addons")
            .setPlaceholder("Select add-ons (optional)...")
            .setMinValues(0);

        List<String[]> items = isDev ? List.of(
            new String[]{prefix + "_rev",    "Additional Revisions",        "Quote"},
            new String[]{prefix + "_rush",   "Rush Delivery",               "$70"},
            new String[]{prefix + "_source", "Source Files",                "$150"},
            new String[]{prefix + "_2rev",   "2 Revisions After Delivery",  "$180"}
        ) : List.of(
            new String[]{prefix + "_rev",    "Additional Revisions",        "Quote"},
            new String[]{prefix + "_rush",   "Rush Delivery",               "$45"},
            new String[]{prefix + "_source", "Source Files (AI/PSD)",       "$250"},
            new String[]{prefix + "_colors", "Color Variants",              "$35"},
            new String[]{prefix + "_anim",   "Add Animation",               "$200"},
            new String[]{prefix + "_2rev",   "2 Revisions After Delivery",  "$35"},
            new String[]{prefix + (session.category.equals("minecraft") ? "_mod" : session.category.equals("editor") ? "_size" : "_logosize"),
                         session.category.equals("minecraft") ? "Additional Modification" : session.category.equals("editor") ? "Additional Size" : "Additional Logo Size",
                         "$10"},
            new String[]{prefix + "_copy",   "Copywriting",                 "$25"}
        );

        for (String[] item : items) b.addOption(item[1], item[0], item[2]);
        b.setMaxValues(items.size());

        String mainSummary = session.selectedServices.stream()
            .map(sid -> ITEM_NAMES.getOrDefault(sid, sid))
            .collect(Collectors.joining(", "));

        PanelService.reply(event, List.of(EmbedUtil.containerBranded(
            "ADD-ONS", "Optional Enhancements",
            "**Selected:** " + mainSummary + "\n\n" +
            "Choose any add-ons, or click **Confirm Order** to skip.",
            EmbedUtil.BANNER_MAIN),
            ActionRow.of(b.build()),
            ActionRow.of(Button.success("wiz_finish", "Confirm Order \u2192").withEmoji(Emoji.fromUnicode("\uD83D\uDCDD")))));
    }

    private static void sendConfirmView(Object event, OrderSession session) {
        String mainSummary = session.selectedServices.stream()
            .map(sid -> ITEM_NAMES.getOrDefault(sid, sid))
            .collect(Collectors.joining(", "));
        String addonSummary = session.selectedAddons.stream()
            .map(sid -> ITEM_NAMES.getOrDefault(sid, sid))
            .collect(Collectors.joining(", "));

        PanelService.reply(event, List.of(EmbedUtil.containerBranded(
            "ORDER SUMMARY", "Review & Submit",
            "**Services:** " + mainSummary + "\n" +
            "**Add-ons:** " + (addonSummary.isEmpty() ? "None" : addonSummary) + "\n\n" +
            "Click **Confirm Order** to fill in your project details.",
            EmbedUtil.BANNER_MAIN),
            ActionRow.of(Button.success("wiz_finish", "Confirm Order \u2192").withEmoji(Emoji.fromUnicode("\uD83D\uDCDD")))));
    }

    // ═══════════════════════════════════════════════════════════════
    // STEP 4 — Final details modal (triggered by wiz_finish button)
    // ═══════════════════════════════════════════════════════════════
    public static void finishWizard(net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent event) {
        TextInput projectInput = TextInput.create("o_project", TextInputStyle.SHORT).setPlaceholder("e.g. Brand Identity for XYZ Corp").setRequired(true).build();
        TextInput nameInput    = TextInput.create("o_name", TextInputStyle.SHORT).setPlaceholder("Ghabs - Enter your name").setRequired(true).build();
        TextInput contactInput = TextInput.create("o_contact", TextInputStyle.SHORT).setPlaceholder("Handle @ platform or clear email").setRequired(true).build();
        TextInput phoneInput   = TextInput.create("o_phone", TextInputStyle.SHORT).setPlaceholder("+966 50 000 0000").setRequired(true).build();
        TextInput etaInput     = TextInput.create("o_eta", TextInputStyle.SHORT).setPlaceholder("e.g. 3 days, 1 week").setRequired(true).build();

        Modal modal = Modal.create("order_modal", "Order Details")
            .addComponents(
                net.dv8tion.jda.api.components.label.Label.of("Project Description", projectInput),
                net.dv8tion.jda.api.components.label.Label.of("Authorized Full Name", nameInput),
                net.dv8tion.jda.api.components.label.Label.of("Contact Handle (Platform)", contactInput),
                net.dv8tion.jda.api.components.label.Label.of("Phone Number", phoneInput),
                net.dv8tion.jda.api.components.label.Label.of("Delivery ETA", etaInput)
            )
            .build();

        event.replyModal(modal).queue();
    }

    // Navigation helpers (kept for compatibility)
    public static void handlePhaseJump(ButtonInteractionEvent event, String phase) {
        OrderSession session = sessions.get(event.getUser().getId());
        if (session == null) { PanelService.replyEphemeral(event, EmbedUtil.error("SESSION EXPIRED", "Restart via the ticket panel.")); return; }
        if (phase.equals("ADDONS")) sendAddonSelection(event, session);
    }

    public static void handleNav(ButtonInteractionEvent event, int direction) {
        OrderSession session = sessions.get(event.getUser().getId());
        if (session == null) { PanelService.replyEphemeral(event, EmbedUtil.error("SESSION EXPIRED", "Restart via the ticket panel.")); return; }
        if (direction > 0) sendAddonSelection(event, session);
        else sendServiceSelection(event, session.category);
    }
}
