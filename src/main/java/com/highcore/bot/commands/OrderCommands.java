package com.highcore.bot.commands;

import com.google.gson.JsonObject;
import com.highcore.bot.database.SupabaseClient;
import com.highcore.bot.services.PanelService;
import com.highcore.bot.utils.EmbedUtil;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.jetbrains.annotations.NotNull;

public class OrderCommands extends ListenerAdapter {

    public static SlashCommandData getCommandData() {
        return Commands.slash("order-status", "Query the status of your project at Haikore Agency")
                .addOption(OptionType.STRING, "number", "Your project ID (Example: HC-007)", true)
                .setDefaultPermissions(DefaultMemberPermissions.ENABLED);
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!event.getName().equals("order-status")) return;

        String num = event.getOption("number").getAsString().toUpperCase();
        if (!num.startsWith("HC-")) num = "HC-" + num;

        JsonObject order = SupabaseClient.getOrder(num);
        if (order == null) {
            PanelService.replyEphemeral(event, EmbedUtil.error("Data Error", "### \u274C Project Not Found\n" + 
                    "Project ID `" + num + "` was not found in our records."));
            return;
        }

        String statusRaw = order.get("status").getAsString();
        String cat = order.has("category") && !order.get("category").isJsonNull() ? order.get("category").getAsString() : "General";
        String name = order.has("specs") && order.get("specs").isJsonObject() && order.getAsJsonObject("specs").has("name") 
                        ? order.getAsJsonObject("specs").get("name").getAsString() : "Custom Project";

        String statusEn = switch (statusRaw) {
            case "COMPLETED" -> "Successfully Completed";
            case "IN_PROGRESS" -> "Currently In Progress";
            case "CANCELLED" -> "Cancelled";
            case "PENDING" -> "Awaiting Review";
            default -> statusRaw;
        };

        String emoji = switch (statusRaw) {
            case "COMPLETED" -> "\u2705";
            case "IN_PROGRESS" -> "\u2699\uFE0F";
            case "CANCELLED" -> "\u274C";
            default -> "\u23F3";
        };

        String body = "## " + emoji + " Status: " + statusEn + "\n\n"
                + "**Project ID:** `" + num + "`\n"
                + "**Service Name:** " + name + "\n"
                + "**Category:** " + cat.toUpperCase() + "\n"
                + "**Notes:** " + (order.has("status_notes") && !order.get("status_notes").isJsonNull() ? order.get("status_notes").getAsString() : "No status updates available at this time.");

        PanelService.reply(event, EmbedUtil.containerBranded("Records", "Project Update", body, EmbedUtil.BANNER_MAIN));
    }
}
