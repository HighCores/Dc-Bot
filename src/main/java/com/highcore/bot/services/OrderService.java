package com.highcore.bot.services;

import com.highcore.bot.database.SupabaseClient;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.modals.Modal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class OrderService extends ListenerAdapter {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    private static final Map<String, String> userOrderType = new ConcurrentHashMap<>();

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("order")) return;
        
        String type = event.getOption("type", OptionMapping::getAsString);
        if (type == null) {
            event.reply("Please specify a service type.").setEphemeral(true).queue();
            return;
        }

        userOrderType.put(event.getUser().getId(), type.toUpperCase());

        TextInput name = TextInput.create("name", "Project Name", TextInputStyle.SHORT)
                .setPlaceholder("e.g. My Website")
                .setRequired(true)
                .build();
        TextInput budget = TextInput.create("budget", "Estimated Budget ($)", TextInputStyle.SHORT)
                .setPlaceholder("e.g. 100")
                .setRequired(true)
                .build();
        TextInput description = TextInput.create("description", "Detailed Requirements", TextInputStyle.PARAGRAPH)
                .setPlaceholder("Describe your vision here...")
                .setRequired(true)
                .build();
        TextInput discord = TextInput.create("discord", "Discord Contact", TextInputStyle.SHORT)
                .setPlaceholder("e.g. user#0000")
                .setRequired(true)
                .build();

        Modal modal = Modal.create("modal_order", "ORDER WIZARD: " + type.toUpperCase())
                .addActionRow(name)
                .addActionRow(budget)
                .addActionRow(description)
                .addActionRow(discord)
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

        SupabaseClient.createOrder(event.getUser().getId(), type, name, desc, budget, contact);
        
        com.highcore.bot.utils.Container c = com.highcore.bot.utils.EmbedUtil.success("ORDER SUBMITTED", 
                "### \uD83D\uDCE6 Transmission Received\n" +
                "**Project:** `" + name + "`\n" +
                "**Type:** `" + type + "`\n" +
                "**Budget:** `$" + budget + "`\n" +
                "**Contact:** `" + contact + "`\n\n" +
                "Our team will review your requirements and reach out on Discord shortly.");
        
        event.getHook().sendMessageEmbeds(c.build()).setEphemeral(true).queue();
        
        LogManager.log(event.getGuild(), "NEW ORDER", 
                "User: " + event.getUser().getAsMention() + "\n" +
                "Type: " + type + "\n" +
                "Budget: $" + budget, com.highcore.bot.utils.EmbedUtil.WARNING);
        
        userOrderType.remove(event.getUser().getId());
    }
}
