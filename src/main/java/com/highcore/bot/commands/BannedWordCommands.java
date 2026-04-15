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

        sendPanel(event);
    }

    public static void sendPanel(net.dv8tion.jda.api.interactions.callbacks.IReplyCallback event) {
        JsonArray words = SupabaseClient.getWordFilter();
        StringBuilder sb = new StringBuilder();
        sb.append("### 🛡️ Word Monitor: Forbidden Terms\n");
        sb.append("Current restricted terminology in the active list:\n\n");

        if (words == null || words.isEmpty()) {
            sb.append("`FILTER BLACKLIST IS CURRENTLY EMPTY`\n");
        } else {
            words.forEach(el -> {
                sb.append("▫️ `").append(el.getAsJsonObject().get("word").getAsString()).append("` ");
            });
            sb.append("\n");
        }

        sb.append("\n_Use the buttons below to manage terms._");

        var container = EmbedUtil.containerBranded("MODERATION", "Word Filter", sb.toString(), EmbedUtil.BANNER_MAIN);
        PanelService.reply(event, container, ActionRow.of(
                Button.success("bw_add", "➕ Add Term"),
                Button.secondary("bw_remove", "🗑️ Delete Term")
        ));
    }

    public static void updatePanel(net.dv8tion.jda.api.interactions.InteractionHook hook) {
        JsonArray words = SupabaseClient.getWordFilter();
        StringBuilder sb = new StringBuilder();
        sb.append("### 🛡️ Word Monitor: Forbidden Terms\n");
        sb.append("Current restricted terminology in the active list:\n\n");

        if (words == null || words.isEmpty()) {
            sb.append("`FILTER BLACKLIST IS CURRENTLY EMPTY`\n");
        } else {
            words.forEach(el -> {
                sb.append("▫️ `").append(el.getAsJsonObject().get("word").getAsString()).append("` ");
            });
            sb.append("\n");
        }

        sb.append("\n_Use the buttons below to manage terms._");

        var container = EmbedUtil.containerBranded("MODERATION", "Word Filter", sb.toString(), EmbedUtil.BANNER_MAIN);
        hook.editOriginal("").setComponents(container, ActionRow.of(
                Button.success("bw_add", "➕ Add Term"),
                Button.secondary("bw_remove", "🗑️ Delete Term")
        )).useComponentsV2(true).queue();
    }
}
