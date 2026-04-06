package com.highcore.bot.services;

import com.google.gson.JsonObject;
import com.highcore.bot.database.SupabaseClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SettingSyncService {
    private static final Logger log = LoggerFactory.getLogger(SettingSyncService.class);
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    
    // Cached global configurations
    public static JsonObject modConfig = new JsonObject();
    public static JsonObject automodConfig = new JsonObject();
    public static JsonObject logConfig = new JsonObject();
    public static JsonObject ticketConfig = new JsonObject();
    public static JsonObject globalSettings = new JsonObject();

    public static void start() {
        // Sync immediately, then every 30 seconds to stay in sync with the Dashboard
        sync();
        scheduler.scheduleAtFixedRate(SettingSyncService::sync, 30, 30, TimeUnit.SECONDS);
        log.info("Neural Sync Service started: Fetching Dashboard configurations.");
    }

    public static void sync() {
        try {
            // Fetch global settings
            JsonObject mod = SupabaseClient.getModerationConfig("global");
            if (mod != null) modConfig = mod;

            JsonObject lc = SupabaseClient.getLogConfig("global");
            if (lc != null) logConfig = lc;

            com.google.gson.JsonArray ticketArr = SupabaseClient.get("dc_ticket_config", "guild_id=eq.global&limit=1");
            if (ticketArr != null && ticketArr.size() > 0) ticketConfig = ticketArr.get(0).getAsJsonObject();

            com.google.gson.JsonArray settingsArr = SupabaseClient.get("dc_settings", "guild_id=eq.global&limit=1");
            if (settingsArr != null && settingsArr.size() > 0) globalSettings = settingsArr.get(0).getAsJsonObject();
            
        } catch (Exception e) {
            log.error("Neural Sync loop error: " + e.getMessage());
        }
    }

    /**
     * Helper to get the Dashboard's configured log channel.
     * Use fallback if not set.
     */
    public static String getModerationLogChannel() {
        return getString(modConfig, "log_channel_id", null);
    }

    public static String getTicketCategory() {
        return getString(ticketConfig, "category_id", com.highcore.bot.config.Config.TICKET_CATEGORY_ID);
    }

    public static String getTranscriptChannel() {
        return getString(ticketConfig, "transcript_channel_id", com.highcore.bot.config.Config.TRANSCRIPT_CHANNEL_ID);
    }

    public static String getString(JsonObject obj, String key, String def) {
        if (obj != null && obj.has(key) && !obj.get(key).isJsonNull()) {
            return obj.get(key).getAsString();
        }
        return def;
    }
}
