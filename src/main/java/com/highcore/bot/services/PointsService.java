package com.highcore.bot.services;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.highcore.bot.config.Config;
import com.highcore.bot.database.SupabaseClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PointsService {
    private static final Logger log = LoggerFactory.getLogger(PointsService.class);

    /**
     * Award points to a staff member for ticket actions.
     * Only if they have the team role.
     */
    public static void awardTicketPoints(String userId, String guildId, int amount, String reason) {
        SupabaseClient.addPoints(userId, guildId, amount, reason, "system");
    }

    /**
     * Check if user has the team role (for auto-points).
     */
    public static boolean isTeamMember(net.dv8tion.jda.api.entities.Member member) {
        return Config.isStaff(member);
    }

    public static int getPoints(String userId, String guildId) {
        return SupabaseClient.getPoints(userId, guildId);
    }

    public static void setPoints(String userId, String guildId, int amount, String reason, String givenBy) {
        SupabaseClient.setPoints(userId, guildId, amount, reason, givenBy);
    }

    public static void addPoints(String userId, String guildId, int amount, String reason, String givenBy) {
        SupabaseClient.addPoints(userId, guildId, amount, reason, givenBy);
    }

    public static void removePoints(String userId, String guildId, int amount, String reason, String givenBy) {
        SupabaseClient.addPoints(userId, guildId, -amount, reason, givenBy);
    }

    public static void resetAllPoints(String guildId) {
        SupabaseClient.resetAllPoints(guildId);
    }

    public static void resetUserPoints(String userId, String guildId) {
        SupabaseClient.setPoints(userId, guildId, 0, "Reset", "system");
    }
}
