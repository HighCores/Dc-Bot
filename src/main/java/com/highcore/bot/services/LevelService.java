package com.highcore.bot.services;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.highcore.bot.config.Config;
import com.highcore.bot.database.SupabaseClient;
import com.highcore.bot.utils.EmbedUtil;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Random;

public class LevelService {
    private static final Logger log = LoggerFactory.getLogger(LevelService.class);
    private static final Random random = new Random();

    // XP settings
    private static final int XP_MIN = 15;
    private static final int XP_MAX = 25;
    private static final int XP_COOLDOWN_SECONDS = 60; // 1 minute between XP gains

    /**
     * Called on every message. Awards XP if cooldown passed.
     */
    public static void processMessage(Guild guild, Member member) {
        if (member == null || member.getUser().isBot()) return;
        String userId = member.getId();
        String guildId = guild.getId();

        // Check cooldown
        JsonObject data = SupabaseClient.getLevelData(userId, guildId);
        if (data != null && data.has("last_xp_at") && !data.get("last_xp_at").isJsonNull()) {
            try {
                Instant lastXp = Instant.parse(data.get("last_xp_at").getAsString());
                if (lastXp.plus(XP_COOLDOWN_SECONDS, ChronoUnit.SECONDS).isAfter(Instant.now())) {
                    // Just increment message count
                    SupabaseClient.incrementMessages(userId, guildId);
                    return;
                }
            } catch (Exception e) { /* proceed */ }
        }

        // Award random XP
        int xpGain = random.nextInt(XP_MAX - XP_MIN + 1) + XP_MIN;
        int currentXp = data != null && data.has("xp") ? data.get("xp").getAsInt() : 0;
        int currentLevel = data != null && data.has("level") ? data.get("level").getAsInt() : 0;
        int newXp = currentXp + xpGain;
        int newLevel = calculateLevel(newXp);

        SupabaseClient.updateLevelData(userId, guildId, newXp, newLevel);

        // Level up!
        if (newLevel > currentLevel) {
            onLevelUp(guild, member, newLevel);
        }
    }

    /**
     * Calculate level from total XP. Formula: level = floor(sqrt(xp / 100))
     * Level 1 = 100 XP, Level 2 = 400 XP, Level 3 = 900 XP, etc.
     */
    public static int calculateLevel(int xp) {
        return (int) Math.floor(Math.sqrt(xp / 100.0));
    }

    /**
     * XP needed for a specific level.
     */
    public static int xpForLevel(int level) {
        return level * level * 100;
    }

    private static void onLevelUp(Guild guild, Member member, int newLevel) {
        // Check for role reward
        JsonObject reward = SupabaseClient.getLevelReward(guild.getId(), newLevel);
        if (reward != null && reward.has("role_id")) {
            String roleId = reward.get("role_id").getAsString();
            Role role = guild.getRoleById(roleId);
            if (role != null) {
                guild.addRoleToMember(member, role).queue(null, e -> {});
            }
        }

        // Send level-up notification in the log channel
        TextChannel logCh = LogManager.get(guild, Config.LOG_UPDATES);
        if (logCh != null) {
            logCh.sendMessageEmbeds(EmbedUtil.base().setColor(EmbedUtil.GOLD)
                    .setAuthor(member.getUser().getName(), null, member.getEffectiveAvatarUrl())
                    .setDescription("### \u2B50 Level Up!\n" + member.getAsMention() + " reached **Level " + newLevel + "**!")
                    .build()).queue();
        }
    }

    public static JsonObject getUserLevel(String userId, String guildId) {
        return SupabaseClient.getLevelData(userId, guildId);
    }
}
