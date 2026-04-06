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
        return Commands.slash("order-status", "Query the registry for project status")
                .addOption(OptionType.STRING, "number", "The project ID (e.g. HC-007)", true)
                .setDefaultPermissions(DefaultMemberPermissions.ENABLED);
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!event.getName().equals("order-status")) return;

        String num = event.getOption("number").getAsString().toUpperCase();
        if (!num.startsWith("HC-")) num = "HC-" + num;

        JsonObject order = SupabaseClient.getOrder(num);
        if (order == null) {
            PanelService.reply(event, EmbedUtil.error("REGISTRY ERROR", "### \u274C Project Not Found\n" + 
                    "The project designation ID `" + num + "` does not exist in the Highcore neural registry."), true);
            return;
        }

        String status = order.get("status").getAsString();
        String cat = order.has("category") && !order.get("category").isJsonNull() ? order.get("category").getAsString() : "GENERAL";
        String name = order.has("specs") && order.get("specs").isJsonObject() && order.getAsJsonObject("specs").has("name") 
                        ? order.getAsJsonObject("specs").get("name").getAsString() : "Classified Project";

        String emoji = switch (status) {
            case "COMPLETED" -> "\u2705";
            case "IN_PROGRESS" -> "\u2699\uFE0F";
            case "CANCELLED" -> "\u274C";
            default -> "\u23F3";
        };

        String body = "## " + emoji + " Status: " + status + "\n\n"
                + "**Project ID:** `" + num + "`\n"
                + "**Designation:** " + name + "\n"
                + "**Sector:** " + cat.toUpperCase() + "\n"
                + "**Operational Notes:** " + (order.has("status_notes") && !order.get("status_notes").isJsonNull() ? order.get("status_notes").getAsString() : "No public telemetry data available.");

        PanelService.reply(event, EmbedUtil.containerBranded("REGISTRY AUDIT", "Project Manifest", body, EmbedUtil.BANNER_MAIN));
    }
}
