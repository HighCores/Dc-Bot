package com.highcore.bot.commands;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.highcore.bot.config.Config;
import com.highcore.bot.database.SupabaseClient;
import com.highcore.bot.services.PointsService;
import com.highcore.bot.utils.EmbedUtil;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PointsCommands extends ListenerAdapter {
    private static final Logger log = LoggerFactory.getLogger(PointsCommands.class);

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        switch (event.getName()) {
            case "points" -> handlePoints(event);
            case "points-reset" -> handlePointsReset(event);
            case "points-panel" -> handlePanel(event);
            case "points-leaderboard" -> handleLeaderboard(event);
        }
    }

    private void handlePoints(SlashCommandInteractionEvent event) {
        String sub = event.getSubcommandName();
        if (sub == null) return;

        switch (sub) {
            case "add" -> {
                if (!isAdmin(event.getMember())) { event.replyEmbeds(EmbedUtil.error("Unauthorized", "Admin only.")).setEphemeral(true).queue(); return; }
                Member target = event.getOption("member", OptionMapping::getAsMember);
                int amount = event.getOption("amount", 0, OptionMapping::getAsInt);
                if (target == null) { event.replyEmbeds(EmbedUtil.error("Error", "Member not found.")).setEphemeral(true).queue(); return; }
                PointsService.addPoints(target.getId(), event.getGuild().getId(), amount, "Admin add", event.getUser().getName());
                int total = PointsService.getPoints(target.getId(), event.getGuild().getId());
                event.replyEmbeds(EmbedUtil.success("Points Added", "**+" + amount + "** points to " + target.getAsMention() + "\nTotal: **" + total + "**")).queue();
            }
            case "remove" -> {
                if (!isAdmin(event.getMember())) { event.replyEmbeds(EmbedUtil.error("Unauthorized", "Admin only.")).setEphemeral(true).queue(); return; }
                Member target = event.getOption("member", OptionMapping::getAsMember);
                int amount = event.getOption("amount", 0, OptionMapping::getAsInt);
                if (target == null) { event.replyEmbeds(EmbedUtil.error("Error", "Member not found.")).setEphemeral(true).queue(); return; }
                PointsService.removePoints(target.getId(), event.getGuild().getId(), amount, "Admin remove", event.getUser().getName());
                int total = PointsService.getPoints(target.getId(), event.getGuild().getId());
                event.replyEmbeds(EmbedUtil.success("Points Removed", "**-" + amount + "** points from " + target.getAsMention() + "\nTotal: **" + total + "**")).queue();
            }
            case "set" -> {
                if (!isAdmin(event.getMember())) { event.replyEmbeds(EmbedUtil.error("Unauthorized", "Admin only.")).setEphemeral(true).queue(); return; }
                Member target = event.getOption("member", OptionMapping::getAsMember);
                int amount = event.getOption("amount", 0, OptionMapping::getAsInt);
                if (target == null) { event.replyEmbeds(EmbedUtil.error("Error", "Member not found.")).setEphemeral(true).queue(); return; }
                PointsService.setPoints(target.getId(), event.getGuild().getId(), amount, "Admin set", event.getUser().getName());
                event.replyEmbeds(EmbedUtil.success("Points Set", target.getAsMention() + " now has **" + amount + "** points.")).queue();
            }
            case "check" -> {
                Member target = event.getOption("member", OptionMapping::getAsMember);
                if (target == null) target = event.getMember();
                int points = PointsService.getPoints(target.getId(), event.getGuild().getId());
                event.replyEmbeds(EmbedUtil.base().setColor(EmbedUtil.INFO)
                        .setAuthor(target.getUser().getName(), null, target.getEffectiveAvatarUrl())
                        .setDescription("### \u2B50 Points\n**" + target.getUser().getName() + "** has **" + points + "** points.").build())
                        .setEphemeral(true).queue();
            }
        }
    }

    private void handlePointsReset(SlashCommandInteractionEvent event) {
        if (!isAdmin(event.getMember())) { event.replyEmbeds(EmbedUtil.error("Unauthorized", "Admin only.")).setEphemeral(true).queue(); return; }
        String sub = event.getSubcommandName();
        if (sub == null) return;
        switch (sub) {
            case "user" -> {
                Member target = event.getOption("member", OptionMapping::getAsMember);
                if (target == null) { event.replyEmbeds(EmbedUtil.error("Error", "Member not found.")).setEphemeral(true).queue(); return; }
                PointsService.resetUserPoints(target.getId(), event.getGuild().getId());
                event.replyEmbeds(EmbedUtil.success("Reset", "Points reset for " + target.getAsMention())).queue();
            }
            case "all" -> {
                PointsService.resetAllPoints(event.getGuild().getId());
                event.replyEmbeds(EmbedUtil.success("Reset", "All points have been reset.")).queue();
            }
        }
    }

    private void handlePanel(SlashCommandInteractionEvent event) {
        if (!isAdmin(event.getMember())) { event.replyEmbeds(EmbedUtil.error("Unauthorized", "Admin only.")).setEphemeral(true).queue(); return; }
        event.replyEmbeds(EmbedUtil.branded().setColor(EmbedUtil.ACCENT_PURPLE)
                .setDescription("""
                        ## \u2B50 Points Management Panel

                        > The points system rewards team members for their work.

                        **How it works:**
                        > \u25B8 **Team members** (role <@&1488795130008961040>) earn points automatically on ticket actions
                        > \u25B8 **Regular members** only get points via admin commands
                        > \u25B8 Points are separate from the leveling system

                        **Commands:**
                        > `/points add` \u2014 Give points to a member
                        > `/points remove` \u2014 Remove points from a member
                        > `/points set` \u2014 Set exact points for a member
                        > `/points check` \u2014 Check someone's points
                        > `/points-reset user` \u2014 Reset a member's points
                        > `/points-reset all` \u2014 Reset everyone's points
                        > `/points-leaderboard` \u2014 View top members
                        """).build())
                .addComponents(ActionRow.of(
                        Button.primary("pts_leaderboard", "\uD83C\uDFC6 Leaderboard")))
                .queue();
    }

    private void handleLeaderboard(SlashCommandInteractionEvent event) {
        event.deferReply().queue();
        JsonArray top = SupabaseClient.getTopPoints(event.getGuild().getId(), 15);
        if (top == null || top.size() == 0) {
            event.getHook().editOriginalEmbeds(EmbedUtil.info("Leaderboard", "No points data yet.")).queue();
            return;
        }
        StringBuilder sb = new StringBuilder();
        int rank = 1;
        for (var el : top) {
            JsonObject p = el.getAsJsonObject();
            String emoji = rank == 1 ? "\uD83E\uDD47" : rank == 2 ? "\uD83E\uDD48" : rank == 3 ? "\uD83E\uDD49" : "`" + rank + ".`";
            sb.append(emoji).append(" <@").append(p.get("user_id").getAsString()).append("> \u2014 **")
                    .append(p.get("points").getAsInt()).append("** points\n");
            rank++;
        }
        event.getHook().editOriginalEmbeds(EmbedUtil.base().setColor(EmbedUtil.GOLD)
                .setDescription("## \uD83C\uDFC6 Points Leaderboard\n\n" + sb).build()).queue();
    }

    private boolean isAdmin(Member m) { return m != null && m.getRoles().stream().anyMatch(r -> Config.getAdminRoles().contains(r.getId())); }
}
