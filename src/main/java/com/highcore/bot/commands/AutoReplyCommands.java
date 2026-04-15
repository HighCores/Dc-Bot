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

        sendPanel(event);
    }

    public static void sendPanel(net.dv8tion.jda.api.interactions.callbacks.IReplyCallback event) {
        JsonArray responses = SupabaseClient.getAutoResponses();
        StringBuilder sb = new StringBuilder();
        sb.append("### 🤖 Assistant: Auto-Replies\n");
        sb.append("Current saved responses in the active list:\n\n");

        if (responses == null || responses.isEmpty()) {
            sb.append("`NO SAVED RESPONSES FOUND`\n");
        } else {
            responses.forEach(el -> {
                var obj = el.getAsJsonObject();
                sb.append("▫️ **").append(obj.get("keyword").getAsString()).append("**: ")
                  .append("`").append(obj.get("response_text").getAsString()).append("`\n");
            });
        }

        sb.append("\n_Use the control panel below to add, edit or remove responses._");
        var container = EmbedUtil.containerBranded("MANAGEMENT", "Response Center", sb.toString(), EmbedUtil.BANNER_MAIN, ActionRow.of(
                Button.success("ar_add", "➕ Add Response"),
                Button.secondary("ar_edit", "📝 Edit Response"),
                Button.danger("ar_manage", "🗑️ Delete Response")
        ));
        PanelService.reply(event, container);
    }

    public static void updatePanel(net.dv8tion.jda.api.interactions.InteractionHook hook) {
        JsonArray responses = SupabaseClient.getAutoResponses();
        StringBuilder sb = new StringBuilder();
        sb.append("### 🤖 Assistant: Auto-Replies\n");
        sb.append("Current saved responses in the active list:\n\n");
        if (responses != null) {
            responses.forEach(el -> {
                var obj = el.getAsJsonObject();
                sb.append("▫️ **").append(obj.get("keyword").getAsString()).append("**: ")
                  .append("`").append(obj.get("response_text").getAsString()).append("`\n");
            });
        }
        sb.append("\n_Use the control panel below to add, edit or remove responses._");
        var container = EmbedUtil.containerBranded("MANAGEMENT", "Response Center", sb.toString(), EmbedUtil.BANNER_MAIN);
        hook.editOriginalComponents(container, ActionRow.of(
                Button.success("ar_add", "➕ Add Response"),
                Button.secondary("ar_edit", "📝 Edit Response"),
                Button.danger("ar_manage", "🗑️ Delete Response")
        )).useComponentsV2(true).queue();
    }

    public static void refreshChannel(net.dv8tion.jda.api.entities.channel.middleman.MessageChannel channel) {
        channel.getHistory().retrievePast(20).queue(msgs -> {
            for (var m : msgs) {
                boolean isPanel = m.getComponents().stream().anyMatch(c -> {
                    String s = c.toString();
                    return s.contains("ar_add") || s.contains("ar_edit") || s.contains("ar_manage");
                });
                if (m.getAuthor().getId().equals(channel.getJDA().getSelfUser().getId()) && isPanel) {
                    updatePanelMessage(m);
                    return;
                }
            }
        });
    }

    private static void updatePanelMessage(net.dv8tion.jda.api.entities.Message m) {
        JsonArray responses = SupabaseClient.getAutoResponses();
        StringBuilder sb = new StringBuilder();
        sb.append("### 🤖 Assistant: Auto-Replies\n");
        sb.append("Current saved responses in the active list:\n\n");
        if (responses != null) {
            responses.forEach(el -> {
                var obj = el.getAsJsonObject();
                sb.append("▫️ **").append(obj.get("keyword").getAsString()).append("**: ")
                  .append("`").append(obj.get("response_text").getAsString()).append("`\n");
            });
        }
        sb.append("\n_Use the control panel below to add, edit or remove responses._");
        var container = EmbedUtil.containerBranded("MANAGEMENT", "Response Center", sb.toString(), EmbedUtil.BANNER_MAIN, ActionRow.of(
            Button.success("ar_add", "➕ Add Response"),
            Button.secondary("ar_edit", "📝 Edit Response"),
            Button.danger("ar_manage", "🗑️ Delete Response")
        ));
        PanelService.reply(m, container);
    }
}
