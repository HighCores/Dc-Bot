package com.highcore.bot.services;

import com.google.gson.JsonObject;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.modals.Modal;
import com.highcore.bot.utils.EmbedUtil;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class OrderService {
    
    public static class ServiceItem {
        public String id, name, price;
        public ServiceItem(String id, String name, String price) { this.id = id; this.name = name; this.price = price; }
    }

    public static final Map<String, List<ServiceItem>> SERVICES = new HashMap<>();
    public static final Map<String, List<ServiceItem>> ADDONS = new HashMap<>();

    static {
        // DESIGNER
        SERVICES.put("designer", List.of(
            new ServiceItem("ds_logo", "Logo Design", "20"),
            new ServiceItem("ds_id", "Full Visual Identity", "50"),
            new ServiceItem("ds_social", "Social Media Design", "25"),
            new ServiceItem("ds_discord", "Discord Welcome/Packs", "20"),
            new ServiceItem("ds_banner", "Covers & Banners", "25"),
            new ServiceItem("ds_print", "Prints & Brochures", "30"),
            new ServiceItem("ds_motion", "Motion Graphics", "45"),
            new ServiceItem("ds_uiux", "UI/UX Design", "60"),
            new ServiceItem("ds_info", "Infographics", "25"),
            new ServiceItem("ds_emoji", "Emoji / Stickers", "15")
        ));
        ADDONS.put("designer", List.of(
            new ServiceItem("ad_rush", "Rush Delivery", "25"),
            new ServiceItem("ad_source", "Source Files (AI/PSD)", "30")
        ));

        // DEVELOPER
        SERVICES.put("developer", List.of(
            new ServiceItem("dv_web", "Web Developer", "30"),
            new ServiceItem("dv_bot", "Bots Developer", "40")
        ));
        ADDONS.put("developer", List.of(
            new ServiceItem("ad_automation", "Automation Engine", "35")
        ));

        // MINECRAFT
        SERVICES.put("minecraft", List.of(
            new ServiceItem("mc_plugin", "Plugin Development", "30"),
            new ServiceItem("mc_config", "Config Specialist", "30")
        ));
        ADDONS.put("minecraft", List.of(
            new ServiceItem("ad_rush", "Rush Delivery", "50")
        ));

        // EDITOR
        SERVICES.put("editor", List.of(
            new ServiceItem("ed_short", "Reels/Shorts Editor", "20"),
            new ServiceItem("ed_long", "Long-form Video", "30")
        ));
        ADDONS.put("editor", List.of(
            new ServiceItem("ad_rush", "Rush Delivery", "20")
        ));
    }

    public static class OrderSession {
        public String category;
        public Set<String> selectedServices = new HashSet<>();
        public Set<String> selectedAddons = new HashSet<>();
        public OrderSession(String category) { this.category = category; }
    }

    public static final Map<String, OrderSession> sessions = new ConcurrentHashMap<>();

    public static void startWizard(Object event) {
        StringSelectMenu menu = StringSelectMenu.create("order_wiz_cat")
                .setPlaceholder("Select Department...")
                .addOption("Digital Designer", "wiz_designer", net.dv8tion.jda.api.entities.emoji.Emoji.fromUnicode("\uD83C\uDFA8"))
                .addOption("Software Developer", "wiz_developer", net.dv8tion.jda.api.entities.emoji.Emoji.fromUnicode("\u2699\uFE0F"))
                .addOption("Minecraft Specialist", "wiz_minecraft", net.dv8tion.jda.api.entities.emoji.Emoji.fromUnicode("\u26CF\uFE0F"))
                .addOption("Video Editor", "wiz_editor", net.dv8tion.jda.api.entities.emoji.Emoji.fromUnicode("\uD83C\uDFAC"))
                .build();

        PanelService.reply(event, EmbedUtil.containerBranded("SERVICE HUB", "Service Selector", 
                "### \uD83D\uDE80 Category Selection\nSelect the primary department for your project to begin our planning and resource allocation.", EmbedUtil.BANNER_MAIN),
                ActionRow.of(menu));
    }

    public static void handleCategory(StringSelectInteractionEvent event) {
        String cat = event.getValues().get(0).replace("wiz_", "");
        sessions.put(event.getUser().getId(), new OrderSession(cat));
        sendServiceSelection(event, cat);
    }

    public static void handleMultiSelection(StringSelectInteractionEvent event) {
        OrderSession session = sessions.get(event.getUser().getId());
        if (session == null) { PanelService.replyEphemeral(event, EmbedUtil.error("SESSION EXPIRED", "Connection lost. Please restart the selection process.")); return; }
        
        String id = event.getComponentId();
        List<String> values = event.getValues();
        
        if (id.equals("wiz_sel_services")) {
            session.selectedServices.clear();
            session.selectedServices.addAll(values);
            sendAddonSelection(event, session.category);
        } else if (id.equals("wiz_sel_addons")) {
            session.selectedAddons.clear();
            session.selectedAddons.addAll(values);
            sendFinalReview(event, session);
        }
    }

    public static void handlePhaseJump(ButtonInteractionEvent event, String phase) {
        OrderSession session = sessions.get(event.getUser().getId());
        if (session == null) { PanelService.replyEphemeral(event, EmbedUtil.error("SESSION EXPIRED", "Connection lost. Please restart the selection process.")); return; }
        if (phase.equals("ADDONS")) sendAddonSelection(event, session.category);
    }

    public static void handleNav(ButtonInteractionEvent event, int direction) {
        OrderSession session = sessions.get(event.getUser().getId());
        if (session == null) { PanelService.replyEphemeral(event, EmbedUtil.error("SESSION EXPIRED", "Connection lost. Please restart the selection process.")); return; }
        if (direction > 0) sendAddonSelection(event, session.category);
        else sendServiceSelection(event, session.category);
    }

    private static void sendServiceSelection(IReplyCallback event, String cat) {
        List<ServiceItem> items = SERVICES.get(cat);
        StringSelectMenu.Builder menu = StringSelectMenu.create("wiz_sel_services")
                .setPlaceholder("Select services (Multiple allowed)")
                .setMinValues(1).setMaxValues(Math.min(items.size(), 10));
        items.forEach(i -> menu.addOption(i.name + " ($" + i.price + ")", i.id));

        PanelService.reply(event, EmbedUtil.containerBranded("DEPARTMENT ANALYSIS", cat.toUpperCase() + " DEPARTMENT", 
                "### \uD83D\uDCE1 Service Allocation\nSelect one or more core services for your project in this department.", EmbedUtil.BANNER_MAIN),
                ActionRow.of(menu.build()));
    }

    private static void sendAddonSelection(IMessageEditCallback event, String cat) {
        List<ServiceItem> items = ADDONS.get(cat);
        StringSelectMenu.Builder menu = StringSelectMenu.create("wiz_sel_addons")
                .setPlaceholder("Select addons (Optional)")
                .setMinValues(0).setMaxValues(Math.min(items.size(), 10));
        items.forEach(i -> menu.addOption(i.name + " (+$" + i.price + ")", i.id));

        PanelService.reply(event, EmbedUtil.containerBranded("SERVICE ADD-ONS", "Optional Enhancements", 
                "### \uD83D\uDD04 Advanced Features\nEnhance your project with these specialized modules and expedited services.", EmbedUtil.BANNER_MAIN),
                ActionRow.of(menu.build()));
    }

    private static void sendFinalReview(IMessageEditCallback event, OrderSession session) {
        JsonObject orderData = new JsonObject();
        orderData.addProperty("category", session.category);
        orderData.addProperty("total", calculateTotal(session));
        
        PanelService.reply(event, EmbedUtil.orderLog(orderData), 
            ActionRow.of(Button.success("wiz_finish", "Finalize & Submit \uD83D\uDCE4")));
    }

    public static int calculateTotal(OrderSession session) {
        int total = 0;
        List<ServiceItem> srvs = SERVICES.get(session.category);
        List<ServiceItem> adds = ADDONS.get(session.category);
        if (srvs != null) for (ServiceItem i : srvs) if (session.selectedServices.contains(i.id)) total += Integer.parseInt(i.price);
        if (adds != null) for (ServiceItem i : adds) if (session.selectedAddons.contains(i.id)) total += Integer.parseInt(i.price);
        return total;
    }

    public static void finishWizard(ButtonInteractionEvent event) {
        TextInput details = TextInput.create("order_details", TextInputStyle.PARAGRAPH)
                .setPlaceholder("Project details / Requirements...")
                .setRequired(true).build();
        TextInput qty = TextInput.create("order_qty", TextInputStyle.SHORT).setPlaceholder("Quantity (e.g. 1)").setRequired(true).setValue("1").build();
        TextInput deadline = TextInput.create("order_deadline", TextInputStyle.SHORT).setPlaceholder("Deadline (e.g. 3 days)").setRequired(false).build();
        TextInput promo = TextInput.create("order_promo", TextInputStyle.SHORT).setPlaceholder("Discount Code").setRequired(false).build();

        Modal modal = Modal.create("order_modal", "PROJECT SPECIFICATIONS")
                .addComponents(
                    Label.of("Detailed Requirements", details),
                    Label.of("Project Quantity", qty),
                    Label.of("Preferred Deadline", deadline),
                    Label.of("Promo Code", promo)
                )
                .build();
        event.replyModal(modal).queue();
    }
}
