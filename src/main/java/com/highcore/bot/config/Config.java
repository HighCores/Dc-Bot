package com.highcore.bot.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

public class Config {
    private static final Properties properties = new Properties();
    private static final Map<String, String> runtimeOverrides = new ConcurrentHashMap<>();

    static {
        File envFile = new File(".env");
        if (!envFile.exists()) envFile = new File("../.env");
        if (envFile.exists()) {
            try (FileInputStream fis = new FileInputStream(envFile)) {
                properties.load(fis);
            } catch (IOException e) { System.err.println("Error reading .env: " + e.getMessage()); }
        }
    }

    // Discord
    public static String DISCORD_TOKEN = get("DISCORD_TOKEN");
    public static String GUILD_ID = get("GUILD_ID");

    // Roles
    public static String ROLE_HIGH = get("ROLE_HIGH");
    public static String ROLE_FOUNDER = get("ROLE_FOUNDER");
    public static String ROLE_MODERATOR = get("ROLE_MODERATOR");
    public static String ROLE_STAFF = get("ROLE_STAFF");
    public static String ROLE_MEMBER = get("ROLE_MEMBER");

    // Supabase
    public static String SUPABASE_URL = get("SUPABASE_URL");
    public static String SUPABASE_KEY = get("SUPABASE_KEY");

    // Groq AI
    public static String GROQ_API_KEY = get("GROQ_API_KEY");
    public static String GROQ_MODEL = getOrDefault("GROQ_MODEL", "openai/gpt-oss-120b");

    // REST API
    public static int API_PORT = Integer.parseInt(getOrDefault("API_PORT", "8080"));
    public static String API_KEY = get("API_KEY");

    // Channels
    public static String TICKET_CATEGORY_ID = get("TICKET_CATEGORY_ID");
    public static String TRANSCRIPT_CHANNEL_ID = get("TRANSCRIPT_CHANNEL_ID");
    public static String LOG_CATEGORY_ID = get("LOG_CATEGORY_ID");
    public static String WELCOME_CHANNEL_ID = get("WELCOME_CHANNEL_ID");
    public static String CH_STARTUP = get("CH_STARTUP");
    public static String CH_ORDER = get("CH_ORDER");
    public static String CH_HIGHCORE = get("CH_HIGHCORE");
    public static String CH_SERVICE_TERMS = get("CH_SERVICE_TERMS");
    public static String CH_UPDATES = get("CH_UPDATES");
    public static String CH_DEV_PRICES = get("CH_DEV_PRICES");
    public static String CH_DESIGN_PRICES = get("CH_DESIGN_PRICES");
    public static String CH_MINECRAFT_PRICES = get("CH_MINECRAFT_PRICES");
    public static String CH_TICKET = get("CH_TICKET");
    public static String BOT_PREFIX = getOrDefault("BOT_PREFIX", "!");

    // Log channel names (created under LOG_CATEGORY_ID)
    public static final String LOG_JOIN_LEFT = "\u26D4\uFE0Fjoin\u30FBleft\u30FBlogs";
    public static final String LOG_MESSAGE = "\u26D4\uFE0Fmessage\u30FBlogs";
    public static final String LOG_VOICE = "\u26D4\uFE0Fvoice\u30FBlogs";
    public static final String LOG_TICKETS = "\u26D4\uFE0Ftickets\u30FBlogs";
    public static final String LOG_CHANNELS = "\u26D4\uFE0Fchannels\u30FBlogs";
    public static final String LOG_UPDATES = "\u26D4\uFE0Fupdates\u30FBlogs";
    public static final String LOG_COMMANDS = "\u26D4\uFE0Fcommands\u30FBlogs";
    public static final String LOG_MODS_CMD = "\u26D4\uFE0Fmods-cmd\u30FBlogs";
    public static final String LOG_ROLES = "\u26D4\uFE0Froles\u30FBlogs";

    public static void updateRuntime(String key, String value) {
        runtimeOverrides.put(key, value);
        switch (key) {
            case "WELCOME_CHANNEL_ID" -> WELCOME_CHANNEL_ID = value;
            case "TRANSCRIPT_CHANNEL_ID" -> TRANSCRIPT_CHANNEL_ID = value;
            case "TICKET_CATEGORY_ID" -> TICKET_CATEGORY_ID = value;
            case "LOG_CATEGORY_ID" -> LOG_CATEGORY_ID = value;
            case "CH_STARTUP" -> CH_STARTUP = value;
            case "CH_ORDER" -> CH_ORDER = value;
            case "CH_HIGHCORE" -> CH_HIGHCORE = value;
            case "CH_SERVICE_TERMS" -> CH_SERVICE_TERMS = value;
            case "CH_UPDATES" -> CH_UPDATES = value;
            case "CH_DEV_PRICES" -> CH_DEV_PRICES = value;
            case "CH_DESIGN_PRICES" -> CH_DESIGN_PRICES = value;
            case "CH_MINECRAFT_PRICES" -> CH_MINECRAFT_PRICES = value;
            case "CH_TICKET" -> CH_TICKET = value;
            case "LOG_JOIN_LEFT" -> runtimeOverrides.put("LOG_JOIN_LEFT", value);
            case "LOG_MESSAGE" -> runtimeOverrides.put("LOG_MESSAGE", value);
            case "LOG_TICKETS" -> runtimeOverrides.put("LOG_TICKETS", value);
            case "LOG_COMMANDS" -> runtimeOverrides.put("LOG_COMMANDS", value);
            case "LOG_MODS_CMD" -> runtimeOverrides.put("LOG_MODS_CMD", value);
            case "LOG_WARNING" -> runtimeOverrides.put("LOG_WARNING", value);
        }
    }

    public static String get(String key) {
        String o = runtimeOverrides.get(key);
        if (o != null && !o.isEmpty()) return o;
        String v = System.getenv(key);
        if (v != null && !v.isEmpty()) return v;
        return properties.getProperty(key);
    }

    private static String getOrDefault(String key, String def) {
        String v = get(key);
        return (v != null && !v.isEmpty()) ? v : def;
    }

    public static List<String> getStaffRoles() {
        if (com.highcore.bot.services.SettingSyncService.modConfig.has("moderator_roles")) {
            com.google.gson.JsonArray modArr = com.highcore.bot.services.SettingSyncService.modConfig.getAsJsonArray("moderator_roles");
            List<String> list = new java.util.ArrayList<>();
            modArr.forEach(e -> list.add(e.getAsString()));
            if (!list.isEmpty()) return list; 
        }
        return Arrays.asList(ROLE_HIGH, ROLE_FOUNDER, ROLE_MODERATOR, ROLE_STAFF); 
    }
    
    public static List<String> getAdminRoles() {
        if (com.highcore.bot.services.SettingSyncService.modConfig.has("admin_roles")) {
            com.google.gson.JsonArray adminArr = com.highcore.bot.services.SettingSyncService.modConfig.getAsJsonArray("admin_roles");
            List<String> list = new java.util.ArrayList<>();
            adminArr.forEach(e -> list.add(e.getAsString()));
            if (!list.isEmpty()) return list; 
        }
        return Arrays.asList(ROLE_HIGH, ROLE_FOUNDER); 
    }

    public static boolean isStaff(net.dv8tion.jda.api.entities.Member m) {
        return m != null && m.getRoles().stream().anyMatch(r -> getStaffRoles().contains(r.getId()));
    }

    public static boolean isAdmin(net.dv8tion.jda.api.entities.Member m) {
        return m != null && m.getRoles().stream().anyMatch(r -> getAdminRoles().contains(r.getId()));
    }

    public static boolean isGroqConfigured() { return GROQ_API_KEY != null && !GROQ_API_KEY.isEmpty() && !GROQ_API_KEY.equals("your_groq_api_key"); }
}
