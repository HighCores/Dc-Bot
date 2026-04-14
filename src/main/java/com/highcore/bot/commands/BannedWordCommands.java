package com.highcore.bot.commands;

import com.google.gson.JsonArray;
import com.highcore.bot.database.SupabaseClient;
import com.highcore.bot.services.PanelService;
import com.highcore.bot.utils.EmbedUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public class BannedWordCommands extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!event.getName().equals("banned-words")) return;

        if (!event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
            PanelService.replyEphemeral(event, EmbedUtil.accessDenied());
            return;
        }

        event.deferReply(false).queue();

        JsonArray words = SupabaseClient.getWordFilter();
        StringBuilder sb = new StringBuilder();
        sb.append("### 🛡️ Firewall Protocol: Word Filter v1.0\n");
        sb.append("Current restricted terminology in effect:\n\n");

        if (words == null || words.isEmpty()) {
            sb.append("`FIREWALL DEACTIVATED — NO RESTRICTED TERMS`\n");
        } else {
            words.forEach(el -> {
                sb.append("▫️ `").append(el.getAsJsonObject().get("word").getAsString()).append("` ");
            });
            sb.append("\n");
        }

        sb.append("\n_Use the interface to reinforce the firewall or purge specific terms._");

        var container = EmbedUtil.containerBranded("SECURITY", "Term Filtering Node", sb.toString(), EmbedUtil.BANNER_MAIN);
        PanelService.reply(event, container, ActionRow.of(
                Button.danger("bw_add", "➕ Reinforce Firewall"),
                Button.secondary("bw_remove", "🗑️ Purge Entry")
        ));
    }
}
