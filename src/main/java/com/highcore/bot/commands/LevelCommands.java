package com.highcore.bot.commands;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.highcore.bot.config.Config;
import com.highcore.bot.database.SupabaseClient;
import com.highcore.bot.services.LevelService;
import com.highcore.bot.utils.EmbedUtil;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

public class LevelCommands extends ListenerAdapter {
    private static final Logger log = LoggerFactory.getLogger(LevelCommands.class);

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        switch (event.getName()) {
            case "level" -> handleLevel(event);
            case "level-leaderboard" -> handleLeaderboard(event);
            case "level-panel" -> handlePanel(event);
            case "rank" -> handleRank(event);
            case "top" -> handleTop(event);
            case "setxp" -> handleSetXp(event);
            case "setlevel" -> handleSetLevel(event);
            case "reset" -> handleReset(event);
        }
    }

    // High admin roles that can set xp/level/reset
    private static final Set<String> HIGH_ADMIN_ROLES = Set.of(
            "1488795130025607323", "1488795130034000036", "1488795130034000035",
            Config.ROLE_HIGH != null ? Config.ROLE_HIGH : "", Config.ROLE_FOUNDER != null ? Config.ROLE_FOUNDER : "");

    private boolean isHighAdmin(Member m) {
        if (m == null) return false;
        return m.getRoles().stream().anyMatch(r -> HIGH_ADMIN_ROLES.contains(r.getId()));
    }

    private void handleLevel(SlashCommandInteractionEvent event) {
        Member target = event.getOption("member", OptionMapping::getAsMember);
        if (target == null) target = event.getMember();

        JsonObject data = LevelService.getUserLevel(target.getId(), event.getGuild().getId());
        int xp = data != null && data.has("xp") ? data.get("xp").getAsInt() : 0;
        int level = data != null && data.has("level") ? data.get("level").getAsInt() : 0;
        int messages = data != null && data.has("messages") ? data.get("messages").getAsInt() : 0;
        int nextLevelXp = LevelService.xpForLevel(level + 1);
        int currentLevelXp = LevelService.xpForLevel(level);
        int progress = nextLevelXp - currentLevelXp > 0 ? (int) (((double) (xp - currentLevelXp) / (nextLevelXp - currentLevelXp)) * 100) : 100;
        if (progress < 0) progress = 0;
        if (progress > 100) progress = 100;

        // Progress bar
        int filled = progress / 10;
        String bar = "\u2588".repeat(filled) + "\u2591".repeat(10 - filled);

        event.replyEmbeds(EmbedUtil.base().setColor(EmbedUtil.ACCENT_TEAL)
                .setAuthor(target.getUser().getName(), null, target.getEffectiveAvatarUrl())
                .setThumbnail(target.getEffectiveAvatarUrl())
                .setDescription("### \u2B50 Level Card")
                .addField("\uD83C\uDFC5 Level", "**" + level + "**", true)
                .addField("\u2728 XP", "**" + xp + "** / " + nextLevelXp, true)
                .addField("\uD83D\uDCAC Messages", "**" + messages + "**", true)
                .addField("Progress", "`" + bar + "` " + progress + "%", false)
                .build()).queue();
    }

    private void handleLeaderboard(SlashCommandInteractionEvent event) {
        event.deferReply().queue();
        JsonArray top = SupabaseClient.getTopLevels(event.getGuild().getId(), 15);
        if (top == null || top.size() == 0) {
            event.getHook().editOriginalEmbeds(EmbedUtil.info("Leaderboard", "No level data yet.")).queue();
            return;
        }
        StringBuilder sb = new StringBuilder();
        int rank = 1;
        for (var el : top) {
            JsonObject p = el.getAsJsonObject();
            String emoji = rank == 1 ? "\uD83E\uDD47" : rank == 2 ? "\uD83E\uDD48" : rank == 3 ? "\uD83E\uDD49" : "`" + rank + ".`";
            int lvl = p.has("level") ? p.get("level").getAsInt() : 0;
            int xp = p.has("xp") ? p.get("xp").getAsInt() : 0;
            sb.append(emoji).append(" <@").append(p.get("user_id").getAsString()).append("> \u2014 Level **")
                    .append(lvl).append("** (").append(xp).append(" XP)\n");
            rank++;
        }
        event.getHook().editOriginalEmbeds(EmbedUtil.base().setColor(EmbedUtil.ACCENT_TEAL)
                .setDescription("## \uD83C\uDFC6 Level Leaderboard\n\n" + sb).build()).queue();
    }

    // ==================== RANK CARD ====================
    private void handleRank(SlashCommandInteractionEvent event) {
        Member target = event.getOption("member", OptionMapping::getAsMember);
        if (target == null) target = event.getMember();
        String guildId = event.getGuild().getId();

        JsonObject data = LevelService.getUserLevel(target.getId(), guildId);
        int xp = data != null && data.has("xp") ? data.get("xp").getAsInt() : 0;
        int level = data != null && data.has("level") ? data.get("level").getAsInt() : 0;
        int messages = data != null && data.has("messages") ? data.get("messages").getAsInt() : 0;
        int voiceMin = data != null && data.has("voice_minutes") ? data.get("voice_minutes").getAsInt() : 0;
        int nextLevelXp = LevelService.xpForLevel(level + 1);
        int currentLevelXp = LevelService.xpForLevel(level);
        int progress = nextLevelXp - currentLevelXp > 0 ? (int) (((double) (xp - currentLevelXp) / (nextLevelXp - currentLevelXp)) * 100) : 100;
        if (progress < 0) progress = 0; if (progress > 100) progress = 100;

        // Calculate rank position
        JsonArray allUsers = SupabaseClient.getTopLevels(guildId, 500);
        int rankPos = 1;
        if (allUsers != null) {
            for (var el : allUsers) {
                if (el.getAsJsonObject().get("user_id").getAsString().equals(target.getId())) break;
                rankPos++;
            }
        }

        int filled = progress / 5;
        String bar = "\u2588".repeat(Math.min(filled, 20)) + "\u2591".repeat(Math.max(20 - filled, 0));

        event.replyEmbeds(EmbedUtil.base().setColor(EmbedUtil.ACCENT_TEAL)
                .setAuthor("Rank #" + rankPos + " \u2022 " + target.getUser().getName(), null, target.getEffectiveAvatarUrl())
                .setThumbnail(target.getEffectiveAvatarUrl())
                .addField("\uD83C\uDFC5 Level", "**" + level + "**", true)
                .addField("\u2728 XP", "**" + xp + "** / " + nextLevelXp, true)
                .addField("\uD83C\uDFC6 Rank", "**#" + rankPos + "**", true)
                .addField("\uD83D\uDCAC Messages", "**" + messages + "**", true)
                .addField("\uD83D\uDD0A Voice", "**" + voiceMin + "** min", true)
                .addField("\u200B", "\u200B", true)
                .addField("Progress to Level " + (level + 1), "`" + bar + "` " + progress + "%", false)
                .build()).queue();
    }

    // ==================== TOP ====================
    private void handleTop(SlashCommandInteractionEvent event) {
        String type = event.getOption("type", "text", OptionMapping::getAsString);
        event.deferReply().queue();
        JsonArray top = SupabaseClient.getTopLevels(event.getGuild().getId(), 15);
        if (top == null || top.size() == 0) {
            event.getHook().editOriginalEmbeds(EmbedUtil.info("Top", "No data yet.")).queue();
            return;
        }
        StringBuilder sb = new StringBuilder();
        int rank = 1;
        for (var el : top) {
            JsonObject p = el.getAsJsonObject();
            String emoji = rank == 1 ? "\uD83E\uDD47" : rank == 2 ? "\uD83E\uDD48" : rank == 3 ? "\uD83E\uDD49" : "`" + rank + ".`";
            if (type.equals("voice")) {
                int voiceMin = p.has("voice_minutes") ? p.get("voice_minutes").getAsInt() : 0;
                sb.append(emoji).append(" <@").append(p.get("user_id").getAsString()).append("> \u2014 **")
                        .append(voiceMin).append("** min\n");
            } else {
                int msgs = p.has("messages") ? p.get("messages").getAsInt() : 0;
                int lvl = p.has("level") ? p.get("level").getAsInt() : 0;
                sb.append(emoji).append(" <@").append(p.get("user_id").getAsString()).append("> \u2014 Lv.**")
                        .append(lvl).append("** (" + msgs + " msgs)\n");
            }
            rank++;
        }
        String title = type.equals("voice") ? "\uD83D\uDD0A Voice Leaderboard" : "\uD83D\uDCAC Text Leaderboard";
        event.getHook().editOriginalEmbeds(EmbedUtil.base().setColor(EmbedUtil.ACCENT_TEAL)
                .setDescription("## " + title + "\n\n" + sb).build()).queue();
    }

    // ==================== SETXP ====================
    private void handleSetXp(SlashCommandInteractionEvent event) {
        if (!isHighAdmin(event.getMember())) { event.replyEmbeds(EmbedUtil.error("Unauthorized", "High admin only.")).setEphemeral(true).queue(); return; }
        Member target = event.getOption("member", OptionMapping::getAsMember);
        int xp = event.getOption("amount", 0, OptionMapping::getAsInt);
        if (target == null) { event.replyEmbeds(EmbedUtil.error("Error", "Member not found.")).setEphemeral(true).queue(); return; }
        SupabaseClient.setUserXp(target.getId(), event.getGuild().getId(), xp);
        int newLevel = LevelService.calculateLevel(xp);
        event.replyEmbeds(EmbedUtil.success("XP Set", target.getAsMention() + " now has **" + xp + " XP** (Level **" + newLevel + "**)")).queue();
    }

    // ==================== SETLEVEL ====================
    private void handleSetLevel(SlashCommandInteractionEvent event) {
        if (!isHighAdmin(event.getMember())) { event.replyEmbeds(EmbedUtil.error("Unauthorized", "High admin only.")).setEphemeral(true).queue(); return; }
        Member target = event.getOption("member", OptionMapping::getAsMember);
        int level = event.getOption("level", 0, OptionMapping::getAsInt);
        if (target == null) { event.replyEmbeds(EmbedUtil.error("Error", "Member not found.")).setEphemeral(true).queue(); return; }
        SupabaseClient.setUserLevel(target.getId(), event.getGuild().getId(), level);
        event.replyEmbeds(EmbedUtil.success("Level Set", target.getAsMention() + " is now Level **" + level + "** (" + LevelService.xpForLevel(level) + " XP)")).queue();
    }

    // ==================== RESET ====================
    private void handleReset(SlashCommandInteractionEvent event) {
        if (!isHighAdmin(event.getMember())) { event.replyEmbeds(EmbedUtil.error("Unauthorized", "High admin only.")).setEphemeral(true).queue(); return; }
        String sub = event.getSubcommandName();
        if (sub == null) return;
        switch (sub) {
            case "user" -> {
                Member target = event.getOption("member", OptionMapping::getAsMember);
                if (target == null) { event.replyEmbeds(EmbedUtil.error("Error", "Member not found.")).setEphemeral(true).queue(); return; }
                SupabaseClient.resetUserLevel(target.getId(), event.getGuild().getId());
                event.replyEmbeds(EmbedUtil.success("Reset", "Reset XP for " + target.getAsMention())).queue();
            }
            case "all" -> {
                event.deferReply().queue();
                SupabaseClient.resetAllLevels(event.getGuild().getId());
                event.getHook().editOriginalEmbeds(EmbedUtil.success("Reset", "All XP/levels have been reset.")).queue();
            }
        }
    }

    private void handlePanel(SlashCommandInteractionEvent event) {
        if (!isAdmin(event.getMember())) { event.replyEmbeds(EmbedUtil.error("Unauthorized", "Admin only.")).setEphemeral(true).queue(); return; }

        // Show current rewards
        JsonArray rewards = SupabaseClient.getLevelRewards(event.getGuild().getId());
        StringBuilder rewardList = new StringBuilder();
        if (rewards != null && rewards.size() > 0) {
            for (var el : rewards) {
                JsonObject r = el.getAsJsonObject();
                rewardList.append("> Level **").append(r.get("level").getAsInt()).append("** \u2192 <@&")
                        .append(r.get("role_id").getAsString()).append(">\n");
            }
        } else {
            rewardList.append("> No rewards configured yet.\n");
        }

        event.replyEmbeds(EmbedUtil.branded().setColor(EmbedUtil.ACCENT_TEAL)
                .setDescription("## \u2B50 Level System Management\n\n" +
                        "> Members earn XP by chatting (15-25 XP per message, 1 min cooldown).\n" +
                        "> Level formula: `level = sqrt(XP / 100)`\n" +
                        "> Level 1 = 100 XP, Level 5 = 2500 XP, Level 10 = 10000 XP\n\n" +
                        "### \uD83C\uDF81 Role Rewards\n" + rewardList + "\n" +
                        "**Management:**\n" +
                        "> Click **Add Reward** to assign a role at a specific level\n" +
                        "> Click **Remove Reward** to delete a level reward\n" +
                        "> Click **View Leaderboard** to see top members")
                .build())
                .addComponents(ActionRow.of(
                        Button.success("lvl_add_reward", "\u2795 Add Reward"),
                        Button.danger("lvl_remove_reward", "\u2796 Remove Reward"),
                        Button.primary("lvl_leaderboard", "\uD83C\uDFC6 Leaderboard")))
                .queue();
    }

    // ===== PANEL BUTTONS =====
    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        switch (event.getComponentId()) {
            case "lvl_add_reward" -> {
                if (!isAdmin(event.getMember())) { event.reply("Admin only.").setEphemeral(true).queue(); return; }
                TextInput level = TextInput.create("lvl_level", "Level number", TextInputStyle.SHORT)
                        .setPlaceholder("e.g. 5").setRequired(true).build();
                TextInput roleId = TextInput.create("lvl_role_id", "Role ID", TextInputStyle.SHORT)
                        .setPlaceholder("Right-click role → Copy Role ID").setRequired(true).build();
                event.replyModal(Modal.create("lvl_modal_add", "\u2795 Add Level Reward")
                        .addComponents(ActionRow.of(level), ActionRow.of(roleId)).build()).queue();
            }
            case "lvl_remove_reward" -> {
                if (!isAdmin(event.getMember())) { event.reply("Admin only.").setEphemeral(true).queue(); return; }
                TextInput level = TextInput.create("lvl_level", "Level number to remove", TextInputStyle.SHORT)
                        .setPlaceholder("e.g. 5").setRequired(true).build();
                event.replyModal(Modal.create("lvl_modal_remove", "\u2796 Remove Level Reward")
                        .addComponents(ActionRow.of(level)).build()).queue();
            }
            case "lvl_leaderboard" -> {
                event.deferReply(true).queue();
                JsonArray top = SupabaseClient.getTopLevels(event.getGuild().getId(), 10);
                if (top == null || top.size() == 0) {
                    event.getHook().editOriginalEmbeds(EmbedUtil.info("Leaderboard", "No data yet.")).queue();
                    return;
                }
                StringBuilder sb = new StringBuilder();
                int rank = 1;
                for (var el : top) {
                    JsonObject p = el.getAsJsonObject();
                    String emoji = rank == 1 ? "\uD83E\uDD47" : rank == 2 ? "\uD83E\uDD48" : rank == 3 ? "\uD83E\uDD49" : "`" + rank + ".`";
                    sb.append(emoji).append(" <@").append(p.get("user_id").getAsString()).append("> \u2014 Lv.**")
                            .append(p.has("level") ? p.get("level").getAsInt() : 0).append("**\n");
                    rank++;
                }
                event.getHook().editOriginalEmbeds(EmbedUtil.base().setColor(EmbedUtil.ACCENT_TEAL)
                        .setDescription("### \uD83C\uDFC6 Level Leaderboard\n\n" + sb).build()).queue();
            }
            case "pts_leaderboard" -> {
                // Points leaderboard from panel button
                event.deferReply(true).queue();
                JsonArray top = SupabaseClient.getTopPoints(event.getGuild().getId(), 10);
                if (top == null || top.size() == 0) {
                    event.getHook().editOriginalEmbeds(EmbedUtil.info("Leaderboard", "No data yet.")).queue();
                    return;
                }
                StringBuilder sb = new StringBuilder();
                int rank = 1;
                for (var el : top) {
                    JsonObject p = el.getAsJsonObject();
                    String emoji = rank == 1 ? "\uD83E\uDD47" : rank == 2 ? "\uD83E\uDD48" : rank == 3 ? "\uD83E\uDD49" : "`" + rank + ".`";
                    sb.append(emoji).append(" <@").append(p.get("user_id").getAsString()).append("> \u2014 **")
                            .append(p.get("points").getAsInt()).append("** pts\n");
                    rank++;
                }
                event.getHook().editOriginalEmbeds(EmbedUtil.base().setColor(EmbedUtil.GOLD)
                        .setDescription("### \uD83C\uDFC6 Points Leaderboard\n\n" + sb).build()).queue();
            }
        }
    }

    // ===== MODAL SUBMIT =====
    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        if (event.getModalId().equals("lvl_modal_add")) {
            int level;
            String roleId;
            try {
                level = Integer.parseInt(event.getValue("lvl_level").getAsString().trim());
                roleId = event.getValue("lvl_role_id").getAsString().trim();
            } catch (Exception e) {
                event.replyEmbeds(EmbedUtil.error("Error", "Invalid input.")).setEphemeral(true).queue();
                return;
            }
            Role role = event.getGuild().getRoleById(roleId);
            if (role == null) {
                event.replyEmbeds(EmbedUtil.error("Error", "Role not found. Check the ID.")).setEphemeral(true).queue();
                return;
            }
            SupabaseClient.setLevelReward(event.getGuild().getId(), level, roleId);
            event.replyEmbeds(EmbedUtil.success("Reward Added", "Level **" + level + "** \u2192 " + role.getAsMention())).setEphemeral(true).queue();
        } else if (event.getModalId().equals("lvl_modal_remove")) {
            int level;
            try { level = Integer.parseInt(event.getValue("lvl_level").getAsString().trim()); }
            catch (Exception e) { event.replyEmbeds(EmbedUtil.error("Error", "Invalid level.")).setEphemeral(true).queue(); return; }
            SupabaseClient.removeLevelReward(event.getGuild().getId(), level);
            event.replyEmbeds(EmbedUtil.success("Reward Removed", "Removed reward for Level **" + level + "**")).setEphemeral(true).queue();
        }
    }

    private boolean isAdmin(Member m) { return m != null && m.getRoles().stream().anyMatch(r -> Config.getAdminRoles().contains(r.getId())); }
}
