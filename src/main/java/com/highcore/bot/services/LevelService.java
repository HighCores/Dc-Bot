package com.highcore.bot.services;

import com.google.gson.JsonObject;
import com.highcore.bot.config.Config;
import com.highcore.bot.database.SupabaseClient;
import com.highcore.bot.utils.EmbedUtil;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.components.container.Container;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Random;

public class LevelService {
    private static final Random random = new Random();

    private static final int XP_MIN = 15;
    private static final int XP_MAX = 25;
    private static final int XP_COOLDOWN_SECONDS = 60;

    public static void processMessage(Guild guild, Member member) {
        if (member == null || member.getUser().isBot()) return;
        String userId = member.getId();
        String guildId = guild.getId();

        JsonObject data = SupabaseClient.getLevelData(userId, guildId);
        if (data != null && data.has("last_xp_at") && !data.get("last_xp_at").isJsonNull()) {
            try {
                Instant lastXp = Instant.parse(data.get("last_xp_at").getAsString());
                if (lastXp.plus(XP_COOLDOWN_SECONDS, ChronoUnit.SECONDS).isAfter(Instant.now())) {
                    SupabaseClient.incrementMessages(userId, guildId);
                    return;
                }
            } catch (Exception e) { /* proceed */ }
        }

        int xpGain = random.nextInt(XP_MAX - XP_MIN + 1) + XP_MIN;
        int currentXp = data != null && data.has("xp") ? data.get("xp").getAsInt() : 0;
        int currentLevel = data != null && data.has("level") ? data.get("level").getAsInt() : 0;
        int newXp = currentXp + xpGain;
        int newLevel = calculateLevel(newXp);

        SupabaseClient.updateLevelData(userId, guildId, newXp, newLevel);

        if (newLevel > currentLevel) {
            onLevelUp(guild, member, newLevel);
        }
    }

    public static int calculateLevel(int xp) {
        return (int) Math.floor(Math.sqrt(xp / 100.0));
    }

    public static int xpForLevel(int level) {
        return level * level * 100;
    }

    private static void onLevelUp(Guild guild, Member member, int newLevel) {
        JsonObject rewardRaw = SupabaseClient.getLevelReward(guild.getId(), newLevel);
        String roleMention = "";
        if (rewardRaw != null && rewardRaw.has("role_id")) {
            String roleId = rewardRaw.get("role_id").getAsString();
            Role role = guild.getRoleById(roleId);
            if (role != null) {
                guild.addRoleToMember(member, role).queue(null, e -> {});
                roleMention = "\n> **New Designation:** " + role.getAsMention();
            }
        }

        TextChannel logCh = LogManager.get(guild, Config.LOG_UPDATES);
        if (logCh != null) {
            String body = "### \u2B50 Clearance Elevated!\n" +
                    "**Subject:** " + member.getAsMention() + "\n" +
                    "**New Level:** **" + newLevel + "**" + 
                    roleMention + "\n\n" +
                    "*Neural synchronization complete. Sector clearance updated.*";

            Container c = EmbedUtil.containerBranded("PROMOTION", "Level Up", body, member.getEffectiveAvatarUrl());
            c.withAccentColor(EmbedUtil.GOLD.getRGB() & 0xFFFFFF);

            logCh.sendMessageComponents(c).useComponentsV2(true).queue();
        }
    }

    public static JsonObject getUserLevel(String userId, String guildId) {
        return SupabaseClient.getLevelData(userId, guildId);
    }
}
