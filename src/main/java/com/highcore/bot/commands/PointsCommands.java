package com.highcore.bot.commands;

import com.highcore.bot.database.SupabaseClient;
import com.highcore.bot.services.PanelService;
import com.highcore.bot.utils.EmbedUtil;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.jetbrains.annotations.NotNull;

public class PointsCommands extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!event.getName().equals("points") && !event.getName().equals("leaderboard")) return;

        if (event.getName().equals("leaderboard")) {
            handleLeaderboard(event);
            return;
        }

        String sub = event.getSubcommandName();
        if (sub == null) return;

        Member target = event.getOption("member") != null ? event.getOption("member").getAsMember() : event.getMember();
        if (target == null) return;

        switch (sub) {
            case "check" -> {
                int pts = SupabaseClient.getPoints(target.getId(), event.getGuild().getId());
                PanelService.reply(event, EmbedUtil.containerBranded("MERIT", "Operational Standing", 
                    "### \u2B50 Identity Summary\nOperative: **" + target.getUser().getName() + "**\nTotal Merit: **" + pts + "** units.", EmbedUtil.BANNER_MAIN));
            }
            case "add" -> {
                if (!event.getMember().hasPermission(net.dv8tion.jda.api.Permission.ADMINISTRATOR)) {
                    PanelService.replyEphemeral(event, EmbedUtil.accessDenied());
                    return;
                }
                int amt = event.getOption("amount", OptionMapping::getAsInt);
                SupabaseClient.addPoints(target.getId(), event.getGuild().getId(), amt, "Admin Allocation", event.getUser().getName());
                PanelService.reply(event, EmbedUtil.success("MERIT ALLOCATED", "Subject: **" + target.getUser().getName() + "**\nValue: **+" + amt + "** units."));
            }
            case "remove" -> {
                if (!event.getMember().hasPermission(net.dv8tion.jda.api.Permission.ADMINISTRATOR)) {
                    PanelService.replyEphemeral(event, EmbedUtil.accessDenied());
                    return;
                }
                int amt = event.getOption("amount", OptionMapping::getAsInt);
                SupabaseClient.addPoints(target.getId(), event.getGuild().getId(), -amt, "Admin Deallocation", event.getUser().getName());
                PanelService.reply(event, EmbedUtil.success("MERIT DEALLOCATED", "Subject: **" + target.getUser().getName() + "**\nValue: **-" + amt + "** units."));
            }
            case "reset" -> {
                if (!event.getMember().hasPermission(net.dv8tion.jda.api.Permission.ADMINISTRATOR)) {
                    PanelService.replyEphemeral(event, EmbedUtil.accessDenied());
                    return;
                }
                SupabaseClient.setPoints(target.getId(), event.getGuild().getId(), 0, "Admin Reset", event.getUser().getName());
                PanelService.reply(event, EmbedUtil.success("MERIT RESET", "Subject: **" + target.getUser().getName() + "**\nProtocol: ZERO-POINT INITIALIZATION"));
            }
        }
    }

    private void handleLeaderboard(SlashCommandInteractionEvent event) {
        com.google.gson.JsonArray top = SupabaseClient.getLeaderboard(event.getGuild().getId());
        StringBuilder sb = new StringBuilder("### \uD83C\uDFC6 Top Operative Ranking\n");
        for (int i = 0; i < top.size(); i++) {
            com.google.gson.JsonObject op = top.get(i).getAsJsonObject();
            String name = op.has("user_name") ? op.get("user_name").getAsString() : "Unknown Operative";
            sb.append(i + 1).append(". **").append(name).append("** \u2192 **").append(op.get("points").getAsInt()).append("** units\n");
        }
        PanelService.reply(event, EmbedUtil.containerBranded("RANKING", "Performance Ledger", sb.toString(), EmbedUtil.BANNER_MAIN));
    }
}
