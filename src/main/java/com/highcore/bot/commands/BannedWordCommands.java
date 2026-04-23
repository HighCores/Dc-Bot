package com.highcore.bot.commands;

import com.highcore.bot.config.Config;
import com.highcore.bot.services.PanelService;
import com.highcore.bot.services.WordFilterService;
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
import java.util.List;
import java.util.ArrayList;

public class BannedWordCommands extends ListenerAdapter {
    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("banned-words")) return;
        if (!Config.isAdmin(event.getMember())) { PanelService.reply(event, EmbedUtil.accessDenied()); return; }
        sendPanel(event, false);
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String id = event.getComponentId();
        if (!Config.isAdmin(event.getMember())) return;
        
        if (id.equals("bw_add")) {
            TextInput word = TextInput.create("word", TextInputStyle.SHORT).setPlaceholder("Banned term...").setRequired(true).build();
            event.replyModal(Modal.create("modal_bw_add", "Banned Word").addComponents(net.dv8tion.jda.api.components.label.Label.of("Banned Word", word)).build()).queue();
        } else if (id.equals("bw_remove")) {
            TextInput word = TextInput.create("word", TextInputStyle.SHORT).setPlaceholder("Word to remove...").setRequired(true).build();
            event.replyModal(Modal.create("modal_bw_remove", "Remove Banned Word").addComponents(net.dv8tion.jda.api.components.label.Label.of("Remove Banned Word", word)).build()).queue();
        }
    }

    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        String id = event.getModalId();
        if (id.equals("modal_bw_add")) {
            String w = event.getValue("word").getAsString();
            WordFilterService.addWord(w);
            sendPanel(event, true);
        } else if (id.equals("modal_bw_remove")) {
            String w = event.getValue("word").getAsString();
            WordFilterService.removeWord(w);
            sendPanel(event, true);
        }
    }

    private void sendPanel(Object event, boolean edit) {
        // Fetch from dc_word_filter (via service cache or direct DB)
        com.google.gson.JsonArray arr = SupabaseClient.getWordFilter();
        List<String> words = new ArrayList<>();
        if (arr != null) {
            arr.forEach(el -> {
                if (el.getAsJsonObject().has("word")) {
                    words.add(el.getAsJsonObject().get("word").getAsString());
                }
            });
        }

        StringBuilder sb = new StringBuilder("### Banned Terms Registry\n\n");
        if (words.isEmpty()) sb.append("*No terms indexed.*");
        else words.forEach(w -> sb.append("\u25AB\uFE0F `").append(w).append("`\n"));

        ActionRow row = ActionRow.of(
            Button.secondary("bw_add", "Add Term").withEmoji(net.dv8tion.jda.api.entities.emoji.Emoji.fromCustom("AddWord", 1496974927931113483L, false)),
            Button.secondary("bw_remove", "Remove Term").withEmoji(net.dv8tion.jda.api.entities.emoji.Emoji.fromCustom("RemoveWord", 1496974275951984852L, false))
        );
        var c = EmbedUtil.containerBranded("MODERATION", "Filter Hub", sb.toString(), null, row);
        if (edit) {
            if (event instanceof ButtonInteractionEvent) ((ButtonInteractionEvent)event).editComponents(c).queue();
            else if (event instanceof ModalInteractionEvent) ((ModalInteractionEvent)event).editComponents(c).queue();
        } else if (event instanceof SlashCommandInteractionEvent) {
            PanelService.reply((SlashCommandInteractionEvent)event, c);
        }
    }
}
