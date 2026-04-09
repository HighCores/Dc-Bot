package com.highcore.bot.services;

import com.google.gson.JsonObject;
import com.highcore.bot.database.SupabaseClient;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.modals.Modal;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class OrderService extends ListenerAdapter {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    private static final Map<String, String> userOrderType = new ConcurrentHashMap<>();

    public static class ServiceItem {
        public String id, name, price;
        public ServiceItem(String id, String name, String price) { this.id = id; this.name = name; this.price = price; }
    }

    public static final Map<String, List<ServiceItem>> SERVICES = new HashMap<>();
    public static final Map<String, List<ServiceItem>> ADDONS = new HashMap<>();

    static {
        // DESIGNER
        SERVICES.put("DESIGNER", List.of(
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
        ADDONS.put("DESIGNER", List.of(
            new ServiceItem("ad_rush", "Rush Delivery", "25"),
            new ServiceItem("ad_source", "Source Files (AI/PSD)", "30")
        ));

        // DEVELOPER
        SERVICES.put("DEVELOPER", List.of(
            new ServiceItem("dv_web", "Web Developer", "30"),
            new ServiceItem("dv_bot", "Bots Developer", "40")
        ));
        ADDONS.put("DEVELOPER", List.of(
            new ServiceItem("ad_automation", "Automation Engine", "35")
        ));
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("order")) return;
        
        String type = event.getOption("type", OptionMapping::getAsString);
        if (type == null) {
            event.reply("Please specify a service type.").setEphemeral(true).queue();
            return;
        }

        userOrderType.put(event.getUser().getId(), type.toUpperCase());

        // JDA 6.4.1 FINAL CORRECT PATTERN: TextInput(id, style).build() -> Label.of("Text", input)
        TextInput nameInput = TextInput.create("name", TextInputStyle.SHORT).build();
        TextInput budgetInput = TextInput.create("budget", TextInputStyle.SHORT).build();
        TextInput descInput = TextInput.create("description", TextInputStyle.PARAGRAPH).build();
        TextInput discordInput = TextInput.create("discord", TextInputStyle.SHORT).build();

        Modal modal = Modal.create("modal_order", "ORDER WIZARD: " + type.toUpperCase())
                .addComponents(Label.of("Project Name", nameInput))
                .addComponents(Label.of("Estimated Budget ($)", budgetInput))
                .addComponents(Label.of("Detailed Requirements", descInput))
                .addComponents(Label.of("Discord Contact", discordInput))
                .build();

        event.replyModal(modal).queue();
    }

    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        if (!event.getModalId().equals("modal_order")) return;

        String type = userOrderType.getOrDefault(event.getUser().getId(), "SERVICE");
        String name = event.getValue("name").getAsString();
        String budget = event.getValue("budget").getAsString();
        String desc = event.getValue("description").getAsString();
        String contact = event.getValue("discord").getAsString();

        event.deferReply(true).queue();

        // FIX: Pass JsonObject as required by SupabaseClient
        JsonObject orderBody = new JsonObject();
        orderBody.addProperty("user_id", event.getUser().getId());
        orderBody.addProperty("type", type);
        orderBody.addProperty("project_name", name);
        orderBody.addProperty("description", desc);
        orderBody.addProperty("budget", budget);
        orderBody.addProperty("contact", contact);
        
        SupabaseClient.createOrder(orderBody);
        
        MessageEmbed me = com.highcore.bot.utils.EmbedUtil.success("ORDER SUBMITTED", 
                "### \uD83D\uDCE6 Transmission Received\n" +
                "**Project:** `" + name + "`\n" +
                "**Type:** `" + type + "`\n" +
                "**Budget:** `$" + budget + "`\n" +
                "**Contact:** `" + contact + "`\n\n" +
                "Our team will review your requirements and reach out on Discord shortly.");
        
        event.getHook().sendMessageEmbeds(me).setEphemeral(true).queue();
        
        LogManager.log(event.getGuild(), "NEW ORDER", 
                "User: " + event.getUser().getAsMention() + "\n" +
                "Type: " + type + "\n" +
                "Budget: $" + budget, com.highcore.bot.utils.EmbedUtil.WARNING);
        
        userOrderType.remove(event.getUser().getId());
    }
}
