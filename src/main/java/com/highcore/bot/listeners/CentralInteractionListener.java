package com.highcore.bot.listeners;

import com.google.gson.JsonObject;
import com.highcore.bot.database.SupabaseClient;
import com.highcore.bot.services.LogManager;
import com.highcore.bot.services.PanelService;
import com.highcore.bot.services.BroadcastService;
import com.highcore.bot.utils.EmbedUtil;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.modals.Modal;

import java.util.EnumSet;

public class CentralInteractionListener extends ListenerAdapter {

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String id = event.getComponentId();
        Member member = event.getMember();
        if (member == null) return;

        if (id.equals("ticket_report")) {
            // JDA 6.4.1 GOLDEN PATTERN: Label.of("Text", input)
            TextInput reasonInput = TextInput.create("reason", "Report Reason", TextInputStyle.PARAGRAPH)
                    .setPlaceholder("Explain your situation...")
                    .setRequired(true)
                    .build();

            Modal modal = Modal.create("modal_report", "REPORT USER")
                    .addComponents(Label.of("Report Reason", reasonInput))
                    .build();
            event.replyModal(modal).queue();
        } else if (id.equals("ticket_support")) {
            // JDA 6.4.1 GOLDEN PATTERN: Label.of("Text", input)
            TextInput subjectInput = TextInput.create("subject", "Support Subject", TextInputStyle.SHORT)
                    .setPlaceholder("What do you need help with?")
                    .setRequired(true)
                    .build();

            Modal modal = Modal.create("modal_support", "SUPPORT TICKET")
                    .addComponents(Label.of("Support Subject", subjectInput))
                    .build();
            event.replyModal(modal).queue();
        }
    }

    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent event) {
        String id = event.getComponentId();
        if (id.equals("menu_services")) {
            PanelService.sendServicesPanel(event);
        }
    }

    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        String id = event.getModalId();
        if (id.equals("modal_support")) {
            String sub = event.getValue("subject").getAsString();
            handleCreateTicket(event, "SUPPORT", sub);
        } else if (id.equals("modal_report")) {
            String reason = event.getValue("reason").getAsString();
            handleCreateTicket(event, "REPORT", reason);
        } else if (id.equals("modal_bc")) {
            com.highcore.bot.commands.SlashCommands.BcSession session = com.highcore.bot.commands.SlashCommands.BC_SESSIONS.get("bc_" + event.getUser().getId());
            if (session != null) {
                String msg = event.getValue("message").getAsString();
                BroadcastService.startBroadcast(event.getGuild(), msg, session.roleId, session.attUrl);
                event.reply("Broadcast transmission initiated successfully.").setEphemeral(true).queue();
                com.highcore.bot.commands.SlashCommands.BC_SESSIONS.remove("bc_" + event.getUser().getId());
            }
        } else if (id.equals("modal_boter")) {
            com.highcore.bot.commands.SlashCommands.BoterSession session = com.highcore.bot.commands.SlashCommands.BOTER_SESSIONS.get("boter_" + event.getUser().getId());
            if (session != null) {
                String msg = event.getValue("message").getAsString();
                TextChannel channel = event.getGuild().getTextChannelById(session.channelId);
                if (channel != null) {
                    channel.sendMessage(msg).queue();
                    event.reply("User emulation successful.").setEphemeral(true).queue();
                }
                com.highcore.bot.commands.SlashCommands.BOTER_SESSIONS.remove("boter_" + event.getUser().getId());
            }
        }
    }

    private void handleCreateTicket(ModalInteractionEvent event, String type, String reason) {
        event.deferReply(true).queue();
        Category cat = event.getGuild().getCategoryById(com.highcore.bot.config.Config.TICKET_CATEGORY_ID);
        if (cat == null) {
            event.getHook().sendMessage("Ticket category not found. Please contact staff.").setEphemeral(true).queue();
            return;
        }

        String ticketName = "ticket-" + event.getUser().getName().toLowerCase();
        event.getGuild().createTextChannel(ticketName, cat)
                .addPermissionOverride(event.getMember(), EnumSet.of(net.dv8tion.jda.api.Permission.VIEW_CHANNEL, net.dv8tion.jda.api.Permission.MESSAGE_SEND), null)
                .addPermissionOverride(event.getGuild().getPublicRole(), null, EnumSet.of(net.dv8tion.jda.api.Permission.VIEW_CHANNEL))
                .queue(ch -> {
                    SupabaseClient.createTicket(ch.getId(), event.getUser().getId(), ch.getId(), type, reason, "MEDIUM", new JsonObject());
                    ch.sendMessage(event.getUser().getAsMention() + " Welcome to your " + type + " ticket. Our staff will assist you shortly.\n**Reason:** " + reason).queue();
                    event.getHook().sendMessage("Ticket created: " + ch.getAsMention()).setEphemeral(true).queue();
                });
    }
}
