package com.highcore.bot.commands;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.highcore.bot.config.Config;
import com.highcore.bot.database.SupabaseClient;
import com.highcore.bot.services.PanelService;
import com.highcore.bot.services.PointsService;
import com.highcore.bot.utils.EmbedUtil;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PointsCommands extends ListenerAdapter {
    private static final Logger log = LoggerFactory.getLogger(PointsCommands.class);

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().startsWith("points")) return;

        switch (event.getName()) {
            case "points" -> handlePoints(event);
            case "points-manage" -> handlePoints(event);
            case "points-reset" -> handlePointsReset(event);
            case "points-leaderboard" -> handleLeaderboard(event);
        }
    }

    private void handlePoints(SlashCommandInteractionEvent event) {
        String sub = event.getSubcommandName();
        if (sub == null) {
            Member target = event.getOption("member", OptionMapping::getAsMember);
            if (target == null) target = event.getMember();
            int currentPoints = SupabaseClient.getPoints(target.getId(), event.getGuild().getId());
            PanelService.reply(event, EmbedUtil.containerBranded("ADMIN AUDIT", "Merit Balance", 
                    target.getAsMention() + " has **" + currentPoints + "** merit points.", EmbedUtil.BANNER_MAIN));
            return;
        }

        switch (sub) {
            case "add" -> {
                if (!isAdmin(event.getMember())) { PanelService.reply(event, EmbedUtil.accessDenied()); return; }
                Member target = event.getOption("member", OptionMapping::getAsMember);
                int amount = event.getOption("amount", 0, OptionMapping::getAsInt);
                if (target == null) { PanelService.reply(event, EmbedUtil.error("TERMINAL ERROR", "Member not found.")); return; }
                PointsService.addPoints(target.getId(), event.getGuild().getId(), amount, "Admin add", event.getUser().getName());
                int total = SupabaseClient.getPoints(target.getId(), event.getGuild().getId());
                PanelService.reply(event, EmbedUtil.success("Merit Added", "**+" + amount + "** points to " + target.getAsMention() + "\nTotal: **" + total + "**"));
            }
            case "remove" -> {
                if (!isAdmin(event.getMember())) { PanelService.reply(event, EmbedUtil.accessDenied()); return; }
                Member target = event.getOption("member", OptionMapping::getAsMember);
                int amount = event.getOption("amount", 0, OptionMapping::getAsInt);
                if (target == null) { PanelService.reply(event, EmbedUtil.error("TERMINAL ERROR", "Member not found.")); return; }
                PointsService.removePoints(target.getId(), event.getGuild().getId(), amount, "Admin remove", event.getUser().getName());
                int total = SupabaseClient.getPoints(target.getId(), event.getGuild().getId());
                PanelService.reply(event, EmbedUtil.success("Merit Removed", "**-" + amount + "** points from " + target.getAsMention() + "\nTotal: **" + total + "**"));
            }
            case "set" -> {
                if (!isAdmin(event.getMember())) { PanelService.reply(event, EmbedUtil.accessDenied()); return; }
                Member target = event.getOption("member", OptionMapping::getAsMember);
                int amount = event.getOption("amount", 0, OptionMapping::getAsInt);
                if (target == null) { PanelService.reply(event, EmbedUtil.error("TERMINAL ERROR", "Member not found.")); return; }
                PointsService.setPoints(target.getId(), event.getGuild().getId(), amount, "Admin set", event.getUser().getName());
                PanelService.reply(event, EmbedUtil.success("Points Set", target.getAsMention() + " now has **" + amount + "** points."));
            }
            case "check" -> {
                Member target = event.getOption("member", OptionMapping::getAsMember);
                if (target == null) target = event.getMember();
                int currentPoints = SupabaseClient.getPoints(target.getId(), event.getGuild().getId());
                PanelService.reply(event, EmbedUtil.containerBranded("PERSONAL AUDIT", "Merit Balance", 
                        target.getAsMention() + " has **" + currentPoints + "** merit points.", EmbedUtil.BANNER_MAIN));
            }
        }
    }

    private void handlePointsReset(SlashCommandInteractionEvent event) {
        if (!isAdmin(event.getMember())) { PanelService.reply(event, EmbedUtil.accessDenied()); return; }
        String sub = event.getSubcommandName();
        if (sub == null) return;
        switch (sub) {
            case "user" -> {
                Member target = event.getOption("member", OptionMapping::getAsMember);
                if (target == null) { PanelService.reply(event, EmbedUtil.error("TERMINAL ERROR", "Member not found.")); return; }
                PointsService.resetUserPoints(target.getId(), event.getGuild().getId());
                PanelService.reply(event, EmbedUtil.success("Reset", "Points reset for " + target.getAsMention()));
            }
            case "all" -> {
                PointsService.resetAllPoints(event.getGuild().getId());
                PanelService.reply(event, EmbedUtil.success("Reset", "All points have been reset."));
            }
        }
    }

    private void handleLeaderboard(SlashCommandInteractionEvent event) {
        event.deferReply().queue();
        JsonArray top = SupabaseClient.getTopPoints(event.getGuild().getId(), 15);
        if (top == null || top.size() == 0) {
            PanelService.reply(event, EmbedUtil.info("LEADERBOARD", "No merit data detected within this sector cycle."));
            return;
        }
        StringBuilder sb = new StringBuilder();
        int rank = 1;
        for (var el : top) {
            JsonObject p = el.getAsJsonObject();
            String emoji = rank == 1 ? "\uD83E\uDD47" : rank == 2 ? "\uD83E\uDD48" : rank == 3 ? "\uD83E\uDD49" : "`" + rank + ".`";
            sb.append(emoji).append(" <@").append(p.get("user_id").getAsString()).append("> \u2014 **")
                    .append(p.get("points").getAsInt()).append("** merit points\n");
            rank++;
        }
        PanelService.reply(event, EmbedUtil.containerBranded("RANKINGS", "Merit Leaderboard", sb.toString(), EmbedUtil.BANNER_MAIN)
                .withAccentColor(EmbedUtil.GOLD.getRGB() & 0xFFFFFF));
    }

    private boolean isAdmin(Member m) { return m != null && Config.isAdmin(m); }
}
