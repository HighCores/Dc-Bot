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

public class AutoReplyCommands extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!event.getName().equals("replay")) return;

        if (!event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
            PanelService.replyEphemeral(event, EmbedUtil.accessDenied());
            return;
        }

        event.deferReply(false).queue();

        JsonArray responses = SupabaseClient.getAutoResponses();
        StringBuilder sb = new StringBuilder();
        sb.append("### 🤖 Customer Service: Auto-Replies\n");
        sb.append("Current saved responses in the database:\n\n");

        if (responses == null || responses.isEmpty()) {
            sb.append("`NO SAVED RESPONSES FOUND`\n");
        } else {
            responses.forEach(el -> {
                var obj = el.getAsJsonObject();
                sb.append("▫️ **").append(obj.get("keyword").getAsString()).append("**: ")
                  .append("`").append(obj.get("response_text").getAsString()).append("`\n");
            });
        }

        sb.append("\n_You can add or delete responses using the control panel below._");

        var container = EmbedUtil.containerBranded("MANAGEMENT", "Response Center", sb.toString(), EmbedUtil.BANNER_MAIN);
        PanelService.reply(event, container, ActionRow.of(
                Button.success("ar_add", "➕ Add Response"),
                Button.secondary("ar_manage", "⚙️ Delete Response")
        ));
    }
}
