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
        sb.append("### 🛡️ Language Protection: Word Filter\n");
        sb.append("Current banned words in the database:\n\n");

        if (words == null || words.isEmpty()) {
            sb.append("`FILTER STATUS: NO BANNED WORDS`\n");
        } else {
            words.forEach(el -> {
                sb.append("▫️ `").append(el.getAsJsonObject().get("word").getAsString()).append("` ");
            });
            sb.append("\n");
        }

        sb.append("\n_You can add or remove words from the filter using the buttons below._");

        var container = EmbedUtil.containerBranded("MODERATION", "Word List", sb.toString(), EmbedUtil.BANNER_MAIN);
        PanelService.reply(event, container, ActionRow.of(
                Button.danger("bw_add", "➕ Add Word"),
                Button.secondary("bw_remove", "🗑️ Delete Word")
        ));
    }
}
