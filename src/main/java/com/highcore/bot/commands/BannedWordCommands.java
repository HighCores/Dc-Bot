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
import java.util.List;
import java.util.ArrayList;

public class BannedWordCommands extends ListenerAdapter {
    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("bannedwords")) return;
        if (!Config.isAdmin(event.getMember())) { PanelService.reply(event, EmbedUtil.accessDenied()); return; }
        sendPanel(event, false);
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        if (!event.getComponentId().equals("bw_add")) return;
        if (!Config.isAdmin(event.getMember())) return;
        TextInput word = TextInput.create("word", TextInputStyle.SHORT).setPlaceholder("Banned term...").setRequired(true).build();
        event.replyModal(Modal.create("modal_bw_add", "Banned Word").addComponents(net.dv8tion.jda.api.components.label.Label.of("Banned Word", word)).build()).queue();
    }

    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        if (!event.getModalId().equals("modal_bw_add")) return;
        String w = event.getValue("word").getAsString();
        SupabaseClient.addBannedWord(w);
        sendPanel(event, true);
    }

    private void sendPanel(Object event, boolean edit) {
        List<String> words = SupabaseClient.getBannedWords();
        StringBuilder sb = new StringBuilder("### Banned Terms Registry\n\n");
        if (words.isEmpty()) sb.append("*No terms indexed.*");
        else words.forEach(w -> sb.append("▫️ `").append(w).append("`\n"));

        ActionRow row = ActionRow.of(Button.secondary("bw_add", "Add Term"));
        var c = EmbedUtil.containerBranded("MODERATION", "Filter Hub", sb.toString(), null, row);
        if (edit) {
            if (event instanceof ButtonInteractionEvent) ((ButtonInteractionEvent)event).editComponents(c).queue();
            else if (event instanceof ModalInteractionEvent) ((ModalInteractionEvent)event).editComponents(c).queue();
        } else PanelService.replyEphemeral((SlashCommandInteractionEvent)event, c);
    }
}
