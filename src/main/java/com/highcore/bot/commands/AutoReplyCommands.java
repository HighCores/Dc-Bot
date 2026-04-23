package com.highcore.bot.commands;

import com.highcore.bot.config.Config;
import com.highcore.bot.services.PanelService;
import com.highcore.bot.utils.EmbedUtil;
import com.highcore.bot.database.SupabaseClient;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.modals.Modal;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class AutoReplyCommands extends ListenerAdapter {
    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("autoreply")) return;
        if (!Config.isAdmin(event.getMember())) { PanelService.reply(event, EmbedUtil.accessDenied()); return; }
        sendPanel(event, false);
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String id = event.getComponentId();
        if (!Config.isAdmin(event.getMember())) return;
        
        if (id.equals("ar_add")) {
            TextInput tr = TextInput.create("trigger", TextInputStyle.SHORT).setPlaceholder("Keyword...").setRequired(true).build();
            TextInput re = TextInput.create("reply", TextInputStyle.PARAGRAPH).setPlaceholder("Bot reply...").setRequired(true).build();
            event.replyModal(Modal.create("modal_ar_add", "Auto Reply")
                .addComponents(net.dv8tion.jda.api.components.label.Label.of("Trigger", tr), net.dv8tion.jda.api.components.label.Label.of("Response", re))
                .build()).queue();
        } else if (id.equals("ar_manage")) {
            TextInput tr = TextInput.create("trigger", TextInputStyle.SHORT).setPlaceholder("Trigger to remove...").setRequired(true).build();
            event.replyModal(Modal.create("modal_ar_remove", "Delete Auto Reply")
                .addComponents(net.dv8tion.jda.api.components.label.Label.of("Trigger", tr))
                .build()).queue();
        }
    }

    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        String id = event.getModalId();
        if (id.equals("modal_ar_add")) {
            String t = event.getValue("trigger").getAsString();
            String r = event.getValue("reply").getAsString();
            SupabaseClient.createAutoResponse(t, r, event.getUser().getName());
            sendPanel(event, true);
        } else if (id.equals("modal_ar_remove")) {
            String t = event.getValue("trigger").getAsString();
            SupabaseClient.removeAutoResponse(t);
            sendPanel(event, true);
        }
    }

    private void sendPanel(Object event, boolean edit) {
        JsonArray replies = SupabaseClient.getAutoResponses();
        StringBuilder sb = new StringBuilder("### Autonomous Response Registry\n\n");
        if (replies == null || replies.size() == 0) sb.append("*No triggers indexed.*");
        else {
            for (var el : replies) {
                JsonObject o = el.getAsJsonObject();
                sb.append("\u25AB\uFE0F **").append(o.get("keyword").getAsString()).append(":** ").append(o.get("response_text").getAsString()).append("\n");
            }
        }

        ActionRow row = ActionRow.of(
            Button.secondary("ar_add", "Add Response").withEmoji(net.dv8tion.jda.api.entities.emoji.Emoji.fromCustom("Reply", 1496974225104175334L, false)),
            Button.secondary("ar_manage", "Delete Response").withEmoji(net.dv8tion.jda.api.entities.emoji.Emoji.fromCustom("DeleteResponce", 1496974794518954035L, false))
        );
        var c = EmbedUtil.containerBranded("AUTOMATION", "Logic Hub", sb.toString(), null, row);
        if (edit) {
            if (event instanceof ButtonInteractionEvent) ((ButtonInteractionEvent)event).editComponents(c).queue();
            else if (event instanceof ModalInteractionEvent) ((ModalInteractionEvent)event).editComponents(c).queue();
        } else if (event instanceof SlashCommandInteractionEvent) {
            PanelService.replyEphemeral((SlashCommandInteractionEvent)event, c);
        }
    }
}
