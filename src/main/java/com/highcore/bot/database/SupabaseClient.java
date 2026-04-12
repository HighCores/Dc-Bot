package com.highcore.bot.database;

import com.google.gson.*;
import com.highcore.bot.config.Config;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Instant;

public class SupabaseClient {

    private static final Logger log = LoggerFactory.getLogger(SupabaseClient.class);
    private static final OkHttpClient http = new OkHttpClient();
    private static final MediaType JSON = MediaType.get("application/json");

    private static String url(String table) {
        return Config.SUPABASE_URL + "/rest/v1/" + table;
    }

    private static Request.Builder auth(Request.Builder builder) {
        return builder
                .header("apikey", Config.SUPABASE_KEY)
                .header("Authorization", "Bearer " + Config.SUPABASE_KEY)
                .header("Content-Type", "application/json")
                .header("Prefer", "return=representation");
    }

    // ========== GLOBAL SETTINGS & CONFIG ==========

    public static JsonObject getSetting(String key) {
        JsonArray arr = get("dc_settings", "key=eq." + key + "&limit=1");
        return arr != null && arr.size() > 0 ? arr.get(0).getAsJsonObject() : null;
    }

    public static String getSettingValue(String key) {
        JsonObject s = getSetting(key);
        return (s != null && s.has("value")) ? s.get("value").getAsString() : null;
    }

    public static void setSetting(String key, String value) {
        JsonObject body = new JsonObject();
        body.addProperty("key", key);
        body.addProperty("value", value);
        body.addProperty("guild_id", "global");
        body.addProperty("updated_at", Instant.now().toString());
        upsert("dc_settings", body, "key,guild_id");
    }

    public static JsonObject getModerationConfig(String guildId) {
        JsonArray arr = get("dc_moderation_config", "guild_id=eq." + guildId + "&limit=1");
        return arr != null && arr.size() > 0 ? arr.get(0).getAsJsonObject() : new JsonObject();
    }

    public static JsonObject getLogConfig(String guildId) {
        JsonArray arr = get("dc_log_config", "guild_id=eq." + guildId + "&limit=1");
        return arr != null && arr.size() > 0 ? arr.get(0).getAsJsonObject() : new JsonObject();
    }

    // ========== TICKETS ==========

    public static int getNextTicketNumber() {
        JsonArray arr = get("dc_tickets", "order=id.desc&limit=1");
        return arr != null && arr.size() > 0 ? arr.get(0).getAsJsonObject().get("id").getAsInt() + 1 : 1;
    }

    public static JsonObject getTicketByChannel(String channelId) {
        JsonArray arr = get("dc_tickets", "channel_id=eq." + channelId + "&limit=1");
        return arr != null && arr.size() > 0 ? arr.get(0).getAsJsonObject() : null;
    }

    public static String getTicketOwner(String channelId) {
        JsonObject ticket = getTicketByChannel(channelId);
        return (ticket != null && ticket.has("user_id")) ? ticket.get("user_id").getAsString() : null;
    }

    public static JsonObject getTicketById(String id) {
        JsonArray arr = get("dc_tickets", "ticket_id=eq." + id + "&limit=1");
        if (arr == null || arr.size() == 0) {
            // Try numeric fallback if id is all digits
            if (id.matches("\\d+")) {
                int nid = Integer.parseInt(id);
                arr = get("dc_tickets", "id=eq." + nid + "&limit=1");
            }
        }
        return arr != null && arr.size() > 0 ? arr.get(0).getAsJsonObject() : null;
    }

    public static void createTicket(String ticketId, String userId, String userName, String channelId, String type, String subject, String priority) {
        JsonObject body = new JsonObject();
        // Minimal set to ensure success
        body.addProperty("ticket_id", ticketId);
        body.addProperty("user_id", userId);
        body.addProperty("user_name", userName);
        body.addProperty("status", "open");
        
        // Add headers as extras
        body.addProperty("channel_id", channelId);
        body.addProperty("subject", subject);
        
        post("dc_tickets", body);
    }

    public static void updateTicketStatus(String id, String status, String closedBy) {
        JsonObject body = new JsonObject();
        body.addProperty("status", status);
        if (closedBy != null) {
            body.addProperty("closed_by", closedBy);
            body.addProperty("closed_at", Instant.now().toString());
        }
        patch("dc_tickets", "ticket_id=eq." + id, body);
    }

    public static void claimTicket(String id, String staffName) {
        JsonObject body = new JsonObject();
        body.addProperty("status", "claimed");
        body.addProperty("claimed_by", staffName);
        body.addProperty("claimed_at", Instant.now().toString());
        patch("dc_tickets", "ticket_id=eq." + id, body);
    }

    public static void unclaimTicket(String id) {
        JsonObject body = new JsonObject();
        body.addProperty("status", "open");
        body.addProperty("claimed_by", (String)null);
        body.addProperty("claimed_at", (String)null);
        patch("dc_tickets", "ticket_id=eq." + id, body);
    }

    public static JsonArray getTicketMessages(String ticketId) {
        JsonArray arr = get("dc_ticket_messages", "ticket_id=eq." + ticketId + "&order=created_at.asc");
        if ((arr == null || arr.size() == 0) && ticketId.matches("\\d+")) {
            int nid = Integer.parseInt(ticketId);
            arr = get("dc_ticket_messages", "ticket_id=eq." + nid + "&order=created_at.asc");
        }
        return arr;
    }

    public static void saveTicketMessage(String ticketId, String userId, String userName, String content, String role, String messageId) {
        JsonObject body = new JsonObject();
        body.addProperty("ticket_id", ticketId);
        body.addProperty("ticketId", ticketId);
        body.addProperty("id_ticket", ticketId);
        
        body.addProperty("user_id", userId);
        body.addProperty("user_name", userName);
        body.addProperty("content", content);
        body.addProperty("message_id", messageId);
        
        // Attempt saving
        JsonObject resp = post("dc_ticket_messages", body);
        
        // If it failed due to potentially slow Ticket creation (Race Condition), retry once
        if (resp == null) {
            try { 
                Thread.sleep(800); 
                post("dc_ticket_messages", body);
                org.slf4j.LoggerFactory.getLogger(SupabaseClient.class).info("Message retry executed for ID: {}", ticketId);
            } catch (Exception ignored) {} 
        }
    }

    // ========== MENUS & COMMANDS (Dynamic) ==========

    public static JsonObject getMenuByTrigger(String trigger) {
        JsonArray arr = get("dc_menus", "trigger_command=eq." + trigger + "&limit=1");
        return arr != null && arr.size() > 0 ? arr.get(0).getAsJsonObject() : null;
    }

    public static JsonObject getMenu(String menuId) {
        JsonArray arr = get("dc_menus", "menu_id=eq." + menuId + "&limit=1");
        return arr != null && arr.size() > 0 ? arr.get(0).getAsJsonObject() : null;
    }

    public static JsonArray getAllMenus() {
        return get("dc_menus", null);
    }

    public static JsonObject getCommand(String trigger) {
        JsonArray arr = get("dc_commands", "name=eq." + trigger + "&limit=1");
        return arr != null && arr.size() > 0 ? arr.get(0).getAsJsonObject() : null;
    }

    public static JsonArray getAllCommands() {
        return get("dc_commands", null);
    }

    public static JsonArray getButtons(String menuId) {
        return get("dc_buttons", "menu_id=eq." + menuId + "&order=position.asc");
    }

    // ========== ORDERS & SESSIONS ==========

    public static JsonObject createOrder(JsonObject body) {
        return post("dc_orders", body);
    }

    public static void updateOrderStatus(String orderNum, String status) {
        JsonObject body = new JsonObject();
        body.addProperty("status", status);
        body.addProperty("updated_at", Instant.now().toString());
        patch("dc_orders", "order_num=eq." + orderNum, body);
    }

    public static JsonObject getOrder(String orderNum) {
        JsonArray arr = get("dc_orders", "order_num=eq." + orderNum + "&limit=1");
        return arr != null && arr.size() > 0 ? arr.get(0).getAsJsonObject() : null;
    }

    public static JsonObject getOrderSession(String userId) {
        JsonArray arr = get("dc_order_sessions", "user_id=eq." + userId + "&limit=1");
        return arr != null && arr.size() > 0 ? arr.get(0).getAsJsonObject() : null;
    }

    public static void updateOrderSession(String userId, JsonObject data) {
        data.addProperty("user_id", userId);
        upsert("dc_order_sessions", data, "user_id");
    }

    public static void deleteOrderSession(String userId) {
        delete("dc_order_sessions", "user_id=eq." + userId);
    }

    public static JsonObject createGiveaway(String channelId, String guildId, String hostId, String hostName, String type, String details, String currency, String expiry, String service, String discount, int winners, String endsAt) {
        JsonObject body = new JsonObject();
        body.addProperty("channel_id", channelId);
        body.addProperty("guild_id", guildId);
        body.addProperty("host_id", hostId);
        body.addProperty("host_name", hostName);
        body.addProperty("prize_type", type);
        body.addProperty("prize_details", details);
        body.addProperty("winner_count", winners);
        body.addProperty("ends_at", endsAt);
        return post("dc_giveaways", body);
    }

    public static void setGiveawayMessageId(long id, String msgId) {
        JsonObject body = new JsonObject();
        body.addProperty("message_id", msgId);
        patch("dc_giveaways", "id=eq." + id, body);
    }

    public static JsonObject getGiveawayById(long id) {
        JsonArray arr = get("dc_giveaways", "id=eq." + id + "&limit=1");
        return arr != null && arr.size() > 0 ? arr.get(0).getAsJsonObject() : null;
    }

    public static JsonArray getActiveGiveaways() {
        return get("dc_giveaways", "ended=eq.false");
    }

    public static void setGiveawayEnded(long id, String[] winners) {
        JsonObject body = new JsonObject();
        body.addProperty("ended", true);
        if (winners != null) {
            JsonArray winArr = new JsonArray();
            for (String w : winners) winArr.add(w);
            body.add("winners", winArr);
        }
        patch("dc_giveaways", "id=eq." + id, body);
    }

    public static void endGiveaway(long id, String[] winners) {
        setGiveawayEnded(id, winners);
    }

    public static void addGiveawayEntry(long giveawayId, String userId) {
        JsonObject body = new JsonObject();
        body.addProperty("giveaway_id", giveawayId);
        body.addProperty("user_id", userId);
        post("dc_giveaway_entries", body);
    }

    public static boolean hasEnteredGiveaway(long giveawayId, String userId) {
        JsonArray arr = get("dc_giveaway_entries", "giveaway_id=eq." + giveawayId + "&user_id=eq." + userId + "&limit=1");
        return arr != null && arr.size() > 0;
    }

    public static JsonArray getGiveawayEntries(long giveawayId) {
        return get("dc_giveaway_entries", "giveaway_id=eq." + giveawayId);
    }

    // ========== POINTS & REPUTATION & VITOS ==========

    public static int getPoints(String userId, String guildId) {
        JsonArray arr = get("dc_points", "user_id=eq." + userId + "&guild_id=eq." + guildId + "&limit=1");
        return arr != null && arr.size() > 0 ? arr.get(0).getAsJsonObject().get("points").getAsInt() : 0;
    }

    public static void setPoints(String userId, String guildId, int amount, String reason, String givenBy) {
        JsonObject body = new JsonObject();
        body.addProperty("user_id", userId);
        body.addProperty("guild_id", guildId);
        body.addProperty("points", amount);
        body.addProperty("updated_at", Instant.now().toString());
        upsert("dc_points", body, "user_id,guild_id");
        logPointAction(userId, guildId, amount, reason, givenBy);
    }

    public static void addPoints(String userId, String guildId, int amount, String reason, String givenBy) {
        setPoints(userId, guildId, getPoints(userId, guildId) + amount, reason, givenBy);
    }

    public static void resetAllPoints(String guildId) {
        delete("dc_points", "guild_id=eq." + guildId);
    }

    private static void logPointAction(String userId, String guildId, int amount, String reason, String givenBy) {
        JsonObject log = new JsonObject();
        log.addProperty("user_id", userId);
        log.addProperty("guild_id", guildId);
        log.addProperty("amount", amount);
        log.addProperty("reason", reason);
        log.addProperty("given_by", givenBy);
        post("dc_points_log", log);
    }

    public static JsonArray getTopPoints(String guildId, int limit) {
        return get("dc_points", "guild_id=eq." + guildId + "&order=points.desc&limit=" + limit);
    }

    public static JsonArray getLeaderboard(String guildId) {
        return getTopPoints(guildId, 10);
    }

    public static int getReputation(String userId, String guildId) {
        JsonArray arr = get("dc_reputation", "user_id=eq." + userId + "&guild_id=eq." + guildId + "&limit=1");
        return arr != null && arr.size() > 0 ? arr.get(0).getAsJsonObject().get("rep").getAsInt() : 0;
    }

    public static void addReputation(String userId, String guildId, int amount) {
        JsonObject body = new JsonObject();
        body.addProperty("user_id", userId);
        body.addProperty("guild_id", guildId);
        body.addProperty("rep", getReputation(userId, guildId) + amount);
        upsert("dc_reputation", body, "user_id,guild_id");
    }

    public static boolean canGiveRep(String userId, String guildId) {
        JsonArray arr = get("dc_rep_cooldowns", "user_id=eq." + userId + "&guild_id=eq." + guildId + "&limit=1");
        if (arr == null || arr.size() == 0) return true;
        Instant expiry = Instant.parse(arr.get(0).getAsJsonObject().get("expires_at").getAsString());
        return Instant.now().isAfter(expiry);
    }

    public static void setRepCooldown(String userId, String guildId) {
        JsonObject body = new JsonObject();
        body.addProperty("user_id", userId);
        body.addProperty("guild_id", guildId);
        body.addProperty("expires_at", Instant.now().plusSeconds(86400).toString()); // 24h
        upsert("dc_rep_cooldowns", body, "user_id,guild_id");
    }

    public static int getVitos(String userId, String guildId) {
        JsonArray arr = get("dc_vitos", "user_id=eq." + userId + "&guild_id=eq." + guildId + "&limit=1");
        return arr != null && arr.size() > 0 ? arr.get(0).getAsJsonObject().get("amount").getAsInt() : 0;
    }

    // ========== LEVELS ==========

    public static JsonObject getLevelData(String userId, String guildId) {
        JsonArray arr = get("dc_levels", "user_id=eq." + userId + "&guild_id=eq." + guildId + "&limit=1");
        return arr != null && arr.size() > 0 ? arr.get(0).getAsJsonObject() : null;
    }

    public static void incrementMessages(String userId, String guildId) {
        JsonObject data = getLevelData(userId, guildId);
        if (data == null) {
            data = new JsonObject();
            data.addProperty("user_id", userId);
            data.addProperty("guild_id", guildId);
            data.addProperty("messages", 1);
        } else {
            data.addProperty("messages", data.get("messages").getAsInt() + 1);
        }
        upsert("dc_levels", data, "user_id,guild_id");
    }

    public static void updateLevelData(String userId, String guildId, int xp, int level) {
        JsonObject data = new JsonObject();
        data.addProperty("user_id", userId);
        data.addProperty("guild_id", guildId);
        data.addProperty("xp", xp);
        data.addProperty("level", level);
        data.addProperty("last_xp_at", Instant.now().toString());
        data.addProperty("updated_at", Instant.now().toString());
        upsert("dc_levels", data, "user_id,guild_id");
    }

    public static void setUserXp(String userId, String guildId, int xp) {
        updateLevelData(userId, guildId, xp, (int)Math.sqrt(xp/100.0));
    }

    public static void setUserLevel(String userId, String guildId, int level) {
        updateLevelData(userId, guildId, level*level*100, level);
    }

    public static void resetUserLevel(String userId, String guildId) {
        delete("dc_levels", "user_id=eq." + userId + "&guild_id=eq." + guildId);
    }

    public static void resetAllLevels(String guildId) {
        delete("dc_levels", "guild_id=eq." + guildId);
    }

    public static JsonObject getLevelReward(String guildId, int level) {
        JsonArray arr = get("dc_level_rewards", "guild_id=eq." + guildId + "&level=eq." + level + "&limit=1");
        return arr != null && arr.size() > 0 ? arr.get(0).getAsJsonObject() : null;
    }

    public static JsonArray getLevelRewards(String guildId) {
        return get("dc_level_rewards", "guild_id=eq." + guildId);
    }

    public static void setLevelReward(String guildId, int level, String roleId) {
        JsonObject body = new JsonObject();
        body.addProperty("guild_id", guildId);
        body.addProperty("level", level);
        body.addProperty("role_id", roleId);
        upsert("dc_level_rewards", body, "guild_id,level");
    }

    public static void removeLevelReward(String guildId, int level) {
        delete("dc_level_rewards", "guild_id=eq." + guildId + "&level=eq." + level);
    }

    public static int getXP(String userId, String guildId) {
        JsonObject data = getLevelData(userId, guildId);
        return data != null && data.has("xp") ? data.get("xp").getAsInt() : 0;
    }

    public static JsonArray getTopLevels(String guildId, int limit) {
        return get("dc_levels", "guild_id=eq." + guildId + "&order=xp.desc&limit=" + limit);
    }

    // ========== TITLES & COLORS & SUGGESTIONS ==========

    public static String getTitle(String userId, String guildId) {
        JsonArray arr = get("dc_titles", "user_id=eq." + userId + "&guild_id=eq." + guildId + "&limit=1");
        return (arr != null && arr.size() > 0) ? arr.get(0).getAsJsonObject().get("title").getAsString() : "No Title";
    }

    public static void setTitle(String userId, String guildId, String title) {
        JsonObject body = new JsonObject();
        body.addProperty("user_id", userId);
        body.addProperty("guild_id", guildId);
        body.addProperty("title", title);
        upsert("dc_titles", body, "user_id,guild_id");
    }

    public static JsonArray getColorRoles(String guildId) {
        return get("dc_color_roles", "guild_id=eq." + guildId);
    }

    public static JsonObject getColorRoleByPosition(String guildId, int pos) {
        JsonArray arr = get("dc_color_roles", "guild_id=eq." + guildId + "&position=eq." + pos + "&limit=1");
        return arr != null && arr.size() > 0 ? arr.get(0).getAsJsonObject() : null;
    }

    public static void saveColorRole(String guildId, String roleId, String name, String hex, int pos) {
        JsonObject body = new JsonObject();
        body.addProperty("guild_id", guildId);
        body.addProperty("role_id", roleId);
        body.addProperty("color_name", name);
        body.addProperty("color_hex", hex);
        body.addProperty("position", pos);
        upsert("dc_color_roles", body, "role_id");
    }

    public static JsonObject createSuggestion(String guildId, String userId, String userName, String content) {
        JsonObject body = new JsonObject();
        body.addProperty("guild_id", guildId);
        body.addProperty("user_id", userId);
        body.addProperty("user_name", userName);
        body.addProperty("content", content);
        body.addProperty("status", "pending");
        return post("dc_suggestions", body);
    }

    public static JsonObject getSuggestion(long id) {
        JsonArray arr = get("dc_suggestions", "id=eq." + id + "&limit=1");
        return arr != null && arr.size() > 0 ? arr.get(0).getAsJsonObject() : null;
    }

    public static void updateSuggestion(long id, String status, String reason, String modId, String modName, String msgId) {
        JsonObject body = new JsonObject();
        if (status != null) body.addProperty("status", status);
        if (reason != null) body.addProperty("reason", reason);
        if (modId != null) {
            body.addProperty("mod_id", modId);
            if (modName != null) body.addProperty("mod_name", modName);
        }
        if (msgId != null) body.addProperty("message_id", msgId);
        patch("dc_suggestions", "id=eq." + id, body);
    }

    // ========== AUTO-REPLIES ==========

    public static JsonArray getAutoResponses() {
        return get("dc_auto_responses", null);
    }

    public static void createAutoResponse(String keyword, String response, String createdBy) {
        JsonObject body = new JsonObject();
        body.addProperty("keyword", keyword);
        body.addProperty("response_text", response);
        body.addProperty("created_by", createdBy);
        upsert("dc_auto_responses", body, "keyword");
    }

    public static void deleteAutoResponse(String keyword) {
        delete("dc_auto_responses", "keyword=eq." + keyword);
    }

    // ========== MODERATION & WARNINGS & VIOLATIONS ==========

    public static void addWarning(String userId, String userName, String modId, String modName, String reason, String guildId) {
        JsonObject body = new JsonObject();
        body.addProperty("user_id", userId);
        body.addProperty("user_name", userName);
        body.addProperty("warned_by", modId);
        body.addProperty("warned_by_name", modName);
        body.addProperty("reason", reason);
        body.addProperty("guild_id", guildId);
        post("dc_warnings", body);
    }

    public static int getWarningCount(String userId, String guildId) {
        JsonArray arr = get("dc_warnings", "user_id=eq." + userId + "&guild_id=eq." + guildId);
        return arr != null ? arr.size() : 0;
    }

    public static JsonArray getUserWarnings(String userId, String guildId) {
        return get("dc_warnings", "user_id=eq." + userId + "&guild_id=eq." + guildId);
    }

    public static JsonArray getServerWarnings(String guildId, int limit) {
        return get("dc_warnings", "guild_id=eq." + guildId + "&order=created_at.desc&limit=" + limit);
    }

    public static void clearUserWarnings(String userId, String guildId) {
        delete("dc_warnings", "user_id=eq." + userId + "&guild_id=eq." + guildId);
    }

    public static void clearAllWarnings(String guildId) {
        delete("dc_warnings", "guild_id=eq." + guildId);
    }

    public static JsonArray getUserViolations(String userId, String guildId) {
        return get("dc_violations", "user_id=eq." + userId + "&guild_id=eq." + guildId);
    }

    public static void clearUserViolations(String userId, String guildId) {
        delete("dc_violations", "user_id=eq." + userId + "&guild_id=eq." + guildId);
    }

    public static void saveTempRole(String userId, String guildId, String roleId, String expiresAt) {
        JsonObject body = new JsonObject();
        body.addProperty("user_id", userId);
        body.addProperty("guild_id", guildId);
        body.addProperty("role_id", roleId);
        body.addProperty("expires_at", expiresAt);
        upsert("dc_temp_roles", body, "user_id,role_id");
    }

    public static void deleteTempRole(String userId, String roleId) {
        delete("dc_temp_roles", "user_id=eq." + userId + "&role_id=eq." + roleId);
    }

    // ========== STATS & TELEMETRY ==========

    public static JsonArray getTicketsByStatus(String status) {
        return get("dc_tickets", "status=eq." + status);
    }

    public static JsonArray getStaffStats(String since) {
        return get("dc_staff_stats", "timestamp=gte." + since);
    }

    public static void logStat(String type, String userId, String details) {
        JsonObject body = new JsonObject();
        body.addProperty("event_type", type);
        body.addProperty("user_id", userId);
        body.addProperty("details", details);
        post("dc_stats", body);
    }

    public static void logRating(String ticketId, int stars, String userId) {
        JsonObject body = new JsonObject();
        body.addProperty("ticket_id", ticketId);
        body.addProperty("stars", stars);
        body.addProperty("user_id", userId);
        post("dc_ratings", body);
    }

    // ========== CORE HTTP METHODS ==========

    public static JsonArray get(String table, String query) {
        Request request = auth(new Request.Builder()).url(url(table) + (query != null ? "?" + query : "")).get().build();
        try (Response response = http.newCall(request).execute()) {
            String b = response.body() != null ? response.body().string() : "[]";
            if (!response.isSuccessful()) { log.error("GET {} failed: {} - {}", table, response.code(), b); return new JsonArray(); }
            return JsonParser.parseString(b).getAsJsonArray();
        } catch (IOException e) { log.error("GET {} error: {}", table, e.getMessage()); return new JsonArray(); }
    }

    public static JsonObject post(String table, JsonObject body) {
        // LOUD LOGGING for diagnosis
        org.slf4j.LoggerFactory.getLogger(SupabaseClient.class).info(">>> POSTing to {}: {}", table, body.toString());
        Request request = auth(new Request.Builder()).url(url(table)).post(RequestBody.create(body.toString(), JSON)).build();
        try (Response response = http.newCall(request).execute()) {
            String b = response.body() != null ? response.body().string() : "{}";
            if (!response.isSuccessful()) { 
                org.slf4j.LoggerFactory.getLogger(SupabaseClient.class).error("POST {} failed: Status {} - Response: {}", table, response.code(), b); 
                return null; 
            }
            JsonElement el = JsonParser.parseString(b);
            if (el.isJsonArray() && el.getAsJsonArray().size() > 0) return el.getAsJsonArray().get(0).getAsJsonObject();
            return el.isJsonObject() ? el.getAsJsonObject() : new JsonObject();
        } catch (IOException e) { 
            org.slf4j.LoggerFactory.getLogger(SupabaseClient.class).error("POST {} IO error: {}", table, e.getMessage()); 
            return null; 
        }
    }

    public static void patch(String table, String filter, JsonObject body) {
        Request request = auth(new Request.Builder()).url(url(table) + "?" + filter).patch(RequestBody.create(body.toString(), JSON)).build();
        try (Response response = http.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String b = response.body() != null ? response.body().string() : "No body";
                log.error("PATCH {} failed: {} - {}", table, response.code(), b);
            }
        } catch (IOException e) { log.error("PATCH {} error: {}", table, e.getMessage()); }
    }

    public static void upsert(String table, JsonObject body, String onConflict) {
        Request.Builder rb = auth(new Request.Builder()).url(url(table) + (onConflict != null ? "?on_conflict=" + onConflict : ""));
        rb.header("Prefer", "resolution=merge-duplicates,return=representation");
        Request request = rb.post(RequestBody.create(body.toString(), JSON)).build();
        try (Response response = http.newCall(request).execute()) {
            if (!response.isSuccessful()) log.error("UPSERT {} failed: {}", table, response.code());
        } catch (IOException e) { log.error("UPSERT {} error: {}", table, e.getMessage()); }
    }

    public static void delete(String table, String filter) {
        Request request = auth(new Request.Builder()).url(url(table) + "?" + filter).delete().build();
        try (Response response = http.newCall(request).execute()) {
            if (!response.isSuccessful()) log.error("DELETE {} failed: {}", table, response.code());
        } catch (IOException e) { log.error("DELETE {} error: {}", table, e.getMessage()); }
    }

    // ========== WORD FILTER ==========

    public static JsonArray getWordFilter() {
        return get("dc_word_filter", null);
    }

    public static void addForbiddenWord(String word) {
        JsonObject body = new JsonObject();
        body.addProperty("word", word.toLowerCase());
        upsert("dc_word_filter", body, "word");
    }

    public static void removeForbiddenWord(String word) {
        delete("dc_word_filter", "word=eq." + word.toLowerCase());
    }
}
