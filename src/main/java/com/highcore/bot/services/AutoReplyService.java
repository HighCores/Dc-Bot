package com.highcore.bot.services;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.highcore.bot.database.SupabaseClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AutoReplyService {

    private static final Logger log = LoggerFactory.getLogger(AutoReplyService.class);

    private static final Map<String, String> cache = new ConcurrentHashMap<>();
    private static long lastRefresh = 0;
    private static final long CACHE_TTL = 5 * 60 * 1000; // 5 minutes

    public static String getResponse(String message) {
        refreshCacheIfNeeded();

        String lowerMessage = message.toLowerCase().trim();

        // Exact match first
        if (cache.containsKey(lowerMessage)) {
            return cache.get(lowerMessage);
        }

        // Contains match
        for (Map.Entry<String, String> entry : cache.entrySet()) {
            if (lowerMessage.contains(entry.getKey())) {
                return entry.getValue();
            }
        }

        return null;
    }

    public static void refreshCache() {
        try {
            JsonArray responses = SupabaseClient.getAutoResponses();
            if (responses == null) return;

            cache.clear();
            for (var element : responses) {
                JsonObject obj = element.getAsJsonObject();
                String keyword = obj.get("keyword").getAsString().toLowerCase();
                String response = obj.get("response_text").getAsString();
                cache.put(keyword, response);
            }
            lastRefresh = System.currentTimeMillis();
            log.info("Auto-reply cache refreshed: {} entries", cache.size());
        } catch (Exception e) {
            log.error("Failed to refresh auto-reply cache: {}", e.getMessage());
        }
    }

    private static void refreshCacheIfNeeded() {
        if (System.currentTimeMillis() - lastRefresh > CACHE_TTL) {
            refreshCache();
        }
    }

    public static void addResponse(String keyword, String response, String createdBy) {
        SupabaseClient.createAutoResponse(keyword.toLowerCase(), response, createdBy);
        cache.put(keyword.toLowerCase(), response);
    }

    public static void removeResponse(String keyword) {
        SupabaseClient.deleteAutoResponse(keyword.toLowerCase());
        cache.remove(keyword.toLowerCase());
    }

    public static Map<String, String> getAllResponses() {
        refreshCacheIfNeeded();
        return Map.copyOf(cache);
    }
}
