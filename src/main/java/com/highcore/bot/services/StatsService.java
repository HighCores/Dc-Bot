package com.highcore.bot.services;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.highcore.bot.database.SupabaseClient;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

public class StatsService {

    public static Map<String, Integer> getTicketStats() {
        Map<String, Integer> stats = new HashMap<>();

        JsonArray open = SupabaseClient.getTicketsByStatus("open");
        JsonArray claimed = SupabaseClient.getTicketsByStatus("claimed");
        JsonArray closed = SupabaseClient.getTicketsByStatus("closed");
        JsonArray reopened = SupabaseClient.getTicketsByStatus("reopened");

        stats.put("open", open != null ? open.size() : 0);
        stats.put("claimed", claimed != null ? claimed.size() : 0);
        stats.put("closed", closed != null ? closed.size() : 0);
        stats.put("reopened", reopened != null ? reopened.size() : 0);
        stats.put("total", stats.get("open") + stats.get("claimed") +
                stats.get("closed") + stats.get("reopened"));

        return stats;
    }

    public static String getTopStaff(int days) {
        String since = Instant.now().minus(days, ChronoUnit.DAYS).toString();
        JsonArray staffStats = SupabaseClient.getStaffStats(since);

        if (staffStats == null || staffStats.size() == 0) return null;

        Map<String, Integer> staffCounts = new HashMap<>();
        for (var element : staffStats) {
            JsonObject stat = element.getAsJsonObject();
            String userId = stat.get("user_id").getAsString();
            staffCounts.merge(userId, 1, Integer::sum);
        }

        return staffCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(e -> "<@" + e.getKey() + "> (" + e.getValue() + " تكت)")
                .orElse(null);
    }
}
