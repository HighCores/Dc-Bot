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
        
        // Dynamic Dashboard Fetch
        JsonObject config = SupabaseClient.getLevelGuildConfig();
        if (config == null || !config.has("is_active") || !config.get("is_active").getAsBoolean()) return;
        
        float xpRate = config.has("xp_rate") && !config.get("xp_rate").isJsonNull() ? config.get("xp_rate").getAsFloat() : 1.0f;

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

        // Award random XP applied with dynamic multiplier from Dashboard!
        int baseGain = random.nextInt(XP_MAX - XP_MIN + 1) + XP_MIN;
        int xpGain = Math.round(baseGain * xpRate);

        int currentXp = data != null && data.has("xp") && !data.get("xp").isJsonNull() ? data.get("xp").getAsInt() : 0;
        int currentLevel = data != null && data.has("level") && !data.get("level").isJsonNull() ? data.get("level").getAsInt() : 0;
        
        int newXp = currentXp + xpGain;
        int newLevel = calculateLevel(newXp);

        SupabaseClient.updateLevelData(userId, guildId, newXp, newLevel);

        // Level up!
        if (newLevel > currentLevel) {
            onLevelUp(guild, member, newLevel, currentLevel, config);
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

    private static void onLevelUp(Guild guild, Member member, int newLevel, int oldLevel, JsonObject config) {
        // Advanced Dynamic Auto-Roles from Vetox logic!
        try {
            JsonArray roleRewards = config.has("rewards") && !config.get("rewards").isJsonNull() ? config.get("rewards").getAsJsonArray() : new JsonArray();
            for (var el : roleRewards) {
                JsonObject reward = el.getAsJsonObject();
                int targetLevel = reward.get("level").getAsInt();
                String roleId = reward.get("role").getAsString();
                // If they reached the target level, give them the role
                if (newLevel >= targetLevel) {
                    Role role = guild.getRoleById(roleId);
                    if (role != null) guild.addRoleToMember(member, role).queue(null, e -> {});
                }
            }
        } catch (Exception ignored) {}

        // Global Announcement Setup
        String announceChannelId = config.has("announce_channel") && !config.get("announce_channel").isJsonNull() ? config.get("announce_channel").getAsString() : null;
        String announceMessage = config.has("announce_message") && !config.get("announce_message").isJsonNull() ? config.get("announce_message").getAsString() : "GG {user}, you just advanced to level {level}!";

        announceMessage = announceMessage
                .replace("{user}", member.getAsMention())
                .replace("{level}", String.valueOf(newLevel))
                .replace("{old_level}", String.valueOf(oldLevel))
                .replace("{server}", guild.getName())
                .replace("\\n", "\n");

        if (announceChannelId != null && !announceChannelId.isEmpty()) {
            TextChannel annCh = guild.getTextChannelById(announceChannelId);
            if (annCh != null) {
                annCh.sendMessage(announceMessage).queue();
            }
        } else {
            // If empty, notify them in general/log channel or we just let it be silent depending on dashboard setting!
            TextChannel logCh = LogManager.get(guild, Config.LOG_UPDATES);
            if (logCh != null) {
                logCh.sendMessageEmbeds(EmbedUtil.base().setColor(EmbedUtil.GOLD)
                        .setAuthor(member.getUser().getName(), null, member.getEffectiveAvatarUrl())
                        .setDescription("### ⭐ Level Up!\n" + member.getAsMention() + " reached **Level " + newLevel + "**!")
                        .build()).queue();
            }
        }
    }

    public static JsonObject getUserLevel(String userId, String guildId) {
        return SupabaseClient.getLevelData(userId, guildId);
    }
}
