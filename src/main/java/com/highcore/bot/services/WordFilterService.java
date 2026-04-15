package com.highcore.bot.services;

import com.google.gson.JsonArray;
import com.highcore.bot.database.SupabaseClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class WordFilterService {
    private static final Logger log = LoggerFactory.getLogger(WordFilterService.class);
    private static final Set<String> forbiddenWords = new HashSet<>();
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public static void init() {
        reload();
        // Reload every 5 minutes
        scheduler.scheduleAtFixedRate(WordFilterService::reload, 5, 5, TimeUnit.MINUTES);
    }

    public static void reload() {
        try {
            JsonArray arr = SupabaseClient.getWordFilter();
            synchronized (forbiddenWords) {
                forbiddenWords.clear();
                if (arr != null) {
                    arr.forEach(el -> {
                        if (el.getAsJsonObject().has("word")) {
                            forbiddenWords.add(el.getAsJsonObject().get("word").getAsString().toLowerCase());
                        }
                    });
                }
            }
            log.info("Word filter reloaded. Total words: {}", forbiddenWords.size());
        } catch (Exception e) {
            log.error("Failed to reload word filter: {}", e.getMessage());
        }
    }

    public static void addWord(String word) {
        SupabaseClient.addForbiddenWord(word);
        reload();
    }

    public static void removeWord(String word) {
        SupabaseClient.removeForbiddenWord(word);
        reload();
    }

    public static boolean isForbidden(String content) {
        return findForbiddenWord(content) != null;
    }

    public static String findForbiddenWord(String content) {
        String lower = content.toLowerCase();
        synchronized (forbiddenWords) {
            for (String word : forbiddenWords) {
                if (lower.contains(word)) return word;
            }
        }
        return null;
    }
}
