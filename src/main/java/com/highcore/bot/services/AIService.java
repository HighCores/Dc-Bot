package com.highcore.bot.services;

import com.google.gson.*;
import com.highcore.bot.config.Config;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class AIService {

    private static final Logger log = LoggerFactory.getLogger(AIService.class);
    private static final Gson gson = new Gson();
    private static final MediaType JSON = MediaType.get("application/json");
    private static final OkHttpClient http = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS).readTimeout(60, TimeUnit.SECONDS).build();
    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";

    private static final String SYSTEM_PROMPT = """
            You are the AI assistant for Highcore Agency, a digital agency specializing in development and design.

            Strict rules:
            1. Reply in English, be friendly, concise, and professional.
            2. NEVER make up any information — no prices, no order statuses, no emails, no links.
            3. If someone asks about an order status or ticket, say: "Please open a ticket and our team will help you!"
            4. If someone asks a general question (e.g. what services?), answer briefly and direct them to open a ticket for details.
            5. You have NO access to orders or tickets — always redirect to tickets.
            6. Never provide any email or link on your own.
            7. Keep responses to 2-3 sentences max.
            """;

    private static final Map<String, List<JsonObject>> sessions = new ConcurrentHashMap<>();
    private static final Map<String, Boolean> aiStatus = new ConcurrentHashMap<>();

    public static String chat(String userId, String message) {
        if (!Config.isGroqConfigured()) return "AI service is not configured. Please contact management.";

        List<JsonObject> history = sessions.computeIfAbsent(userId, k -> new ArrayList<>());
        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", message);
        history.add(userMsg);
        while (history.size() > 10) history.remove(0);

        JsonArray messages = new JsonArray();
        JsonObject systemMsg = new JsonObject();
        systemMsg.addProperty("role", "system");
        systemMsg.addProperty("content", SYSTEM_PROMPT);
        messages.add(systemMsg);
        for (JsonObject msg : history) messages.add(msg);

        JsonObject body = new JsonObject();
        body.addProperty("model", Config.GROQ_MODEL);
        body.add("messages", messages);
        body.addProperty("temperature", 0.2);
        body.addProperty("max_tokens", 1024);

        Request request = new Request.Builder()
                .url(GROQ_URL)
                .header("Authorization", "Bearer " + Config.GROQ_API_KEY)
                .header("Content-Type", "application/json")
                .post(RequestBody.create(body.toString(), JSON)).build();

        try (Response response = http.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) return "An error occurred with the AI service. Please try again.";

            JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
            String reply = json.getAsJsonArray("choices").get(0).getAsJsonObject()
                    .getAsJsonObject("message").get("content").getAsString();
            if (reply.length() > 1900) reply = reply.substring(0, 1900) + "...";

            JsonObject assistantMsg = new JsonObject();
            assistantMsg.addProperty("role", "assistant");
            assistantMsg.addProperty("content", reply);
            history.add(assistantMsg);
            return reply;
        } catch (IOException e) {
            return "Could not connect to the AI service.";
        }
    }

    public static void enableAI(String channelKey) { aiStatus.put(channelKey, true); }
    public static void disableAI(String channelKey) { aiStatus.remove(channelKey); }
    public static boolean isAIEnabled(String channelKey) { return aiStatus.getOrDefault(channelKey, false); }
    public static void clearSession(String userId) { sessions.remove(userId); }
}
