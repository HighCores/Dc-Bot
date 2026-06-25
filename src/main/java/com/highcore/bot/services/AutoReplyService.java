package com.highcore.bot.services;

import com.google.gson.JsonArray;
import com.highcore.bot.database.SupabaseClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AutoReplyService {
    private static final Logger log = LoggerFactory.getLogger(AutoReplyService.class);
    private static final Map<String, String> responses = new HashMap<>();
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public static void init() {
        refreshCache();
        // Background sync
        scheduler.scheduleAtFixedRate(AutoReplyService::refreshCache, 10, 10, TimeUnit.SECONDS);
    }

    public static void refreshCache() {
        try {
            JsonArray arr = SupabaseClient.getAutoResponses();
            synchronized (responses) {
                responses.clear();
                if (arr != null) {
                    arr.forEach(el -> {
                        var obj = el.getAsJsonObject();
                        // Support both trigger/response (new DB schema) and keyword/response_text (legacy schema)
                        String key = obj.has("trigger") ? obj.get("trigger").getAsString() : (obj.has("keyword") ? obj.get("keyword").getAsString() : null);
                        String val = obj.has("response") ? obj.get("response").getAsString() : (obj.has("response_text") ? obj.get("response_text").getAsString() : null);
                        
                        if (key != null && val != null) {
                            responses.put(key.toLowerCase(), val);
                        }
                    });
                }
            }
            log.info("Auto-replies reloaded. Total pairs: {}", responses.size());
        } catch (Exception e) {
            log.error("Failed to reload auto-replies: {}", e.getMessage());
        }
    }

    public static void addResponse(String trigger, String response, String author) {
        SupabaseClient.createAutoResponse(trigger, response, author);
        refreshCache();
    }

    public static void removeResponse(String trigger) {
        SupabaseClient.deleteAutoResponse(trigger);
        refreshCache();
    }

    public static Map<String, String> getAllResponses() {
        synchronized (responses) {
            return new HashMap<>(responses);
        }
    }

    public static String getResponse(String content) {
        String lower = content.toLowerCase();
        synchronized (responses) {
            for (Map.Entry<String, String> entry : responses.entrySet()) {
                if (lower.contains(entry.getKey())) return entry.getValue();
            }
        }
        return null;
    }
}
