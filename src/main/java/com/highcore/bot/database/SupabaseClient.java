package com.highcore.bot.database;

import com.google.gson.*;
import com.highcore.bot.config.Config;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class SupabaseClient {

    private static final Logger log = LoggerFactory.getLogger(SupabaseClient.class);
    private static final OkHttpClient http = new OkHttpClient();
    private static final Gson gson = new Gson();
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

    // ========== TICKETS ==========

    public static JsonObject createTicket(String orderId, String userId, String userName,
                                          String channelId, String subject, String priority) {
        JsonObject body = new JsonObject();
        body.addProperty("ticket_id", orderId);
        body.addProperty("user_id", userId);
        body.addProperty("user_name", userName);
        body.addProperty("channel_id", channelId);
        body.addProperty("subject", subject);
        body.addProperty("priority", priority);
        body.addProperty("status", "open");
        return post("dc_tickets", body);
    }

    public static JsonObject updateTicketStatus(String ticketId, String status, String closedBy) {
        JsonObject body = new JsonObject();
        body.addProperty("status", status);
        if (closedBy != null) body.addProperty("closed_by", closedBy);
        if (status.equals("closed")) body.addProperty("closed_at", java.time.Instant.now().toString());
        return patch("dc_tickets", "ticket_id=eq." + ticketId, body);
    }

    public static JsonObject claimTicket(String ticketId, String claimedBy) {
        JsonObject body = new JsonObject();
        body.addProperty("status", "claimed");
        body.addProperty("claimed_by", claimedBy);
        body.addProperty("claimed_at", java.time.Instant.now().toString());
        return patch("dc_tickets", "ticket_id=eq." + ticketId, body);
    }

    public static JsonArray getTicketsByStatus(String status) {
        return get("dc_tickets", "status=eq." + status + "&order=created_at.desc");
    }

    public static JsonObject getTicketByChannel(String channelId) {
        JsonArray arr = get("dc_tickets", "channel_id=eq." + channelId + "&limit=1");
        return arr != null && arr.size() > 0 ? arr.get(0).getAsJsonObject() : null;
    }

    public static JsonObject getTicketById(String ticketId) {
        JsonArray arr = get("dc_tickets", "ticket_id=eq." + ticketId + "&limit=1");
        return arr != null && arr.size() > 0 ? arr.get(0).getAsJsonObject() : null;
    }

    public static int getNextTicketNumber() {
        JsonArray arr = get("dc_tickets", "select=ticket_id&order=created_at.desc&limit=1");
        if (arr != null && arr.size() > 0) {
            String lastId = arr.get(0).getAsJsonObject().get("ticket_id").getAsString();
            try {
                return Integer.parseInt(lastId.replace("HC-", "")) + 1;
            } catch (NumberFormatException e) {
                return 1;
            }
        }
        return 1;
    }

    // ========== TICKET MESSAGES ==========

    public static void saveTicketMessage(String ticketId, String userId, String userName,
                                         String content, String messageId) {
        JsonObject body = new JsonObject();
        body.addProperty("ticket_id", ticketId);
        body.addProperty("user_id", userId);
        body.addProperty("user_name", userName);
        body.addProperty("content", content);
        body.addProperty("message_id", messageId);
        post("dc_ticket_messages", body);
    }

    public static JsonArray getTicketMessages(String ticketId) {
        return get("dc_ticket_messages", "ticket_id=eq." + ticketId + "&order=created_at.asc");
    }

    // ========== AUTO RESPONSES ==========

    public static JsonArray getAutoResponses() {
        return get("dc_auto_responses", "is_active=eq.true");
    }

    public static JsonObject getAutoResponse(String keyword) {
        JsonArray arr = get("dc_auto_responses",
                "keyword=eq." + keyword + "&is_active=eq.true&limit=1");
        return arr != null && arr.size() > 0 ? arr.get(0).getAsJsonObject() : null;
    }

    public static JsonObject createAutoResponse(String keyword, String response, String createdBy) {
        JsonObject body = new JsonObject();
        body.addProperty("keyword", keyword);
        body.addProperty("response_text", response);
        body.addProperty("created_by", createdBy);
        body.addProperty("is_active", true);
        return post("dc_auto_responses", body);
    }

    public static JsonObject deleteAutoResponse(String keyword) {
        return delete("dc_auto_responses", "keyword=eq." + keyword);
    }

    // ========== BUTTONS & MENUS ==========

    public static JsonArray getButtons(String menuId) {
        return get("dc_buttons", "menu_id=eq." + menuId + "&is_active=eq.true&order=position.asc");
    }

    public static JsonArray getAllMenus() {
        return get("dc_menus", "is_active=eq.true&order=position.asc");
    }

    public static JsonObject getMenu(String menuId) {
        JsonArray arr = get("dc_menus", "menu_id=eq." + menuId + "&limit=1");
        return arr != null && arr.size() > 0 ? arr.get(0).getAsJsonObject() : null;
    }

    public static JsonObject getMenuByTrigger(String trigger) {
        JsonArray arr = get("dc_menus", "trigger_command=eq." + trigger + "&is_active=eq.true&limit=1");
        return arr != null && arr.size() > 0 ? arr.get(0).getAsJsonObject() : null;
    }

    // ========== COMMANDS ==========

    public static JsonObject getCommand(String name) {
        JsonArray arr = get("dc_commands", "name=eq." + name + "&is_active=eq.true&limit=1");
        return arr != null && arr.size() > 0 ? arr.get(0).getAsJsonObject() : null;
    }

    public static JsonArray getAllCommands() {
        return get("dc_commands", "is_active=eq.true&type=eq.slash");
    }

    // ========== STATS ==========

    public static void logStat(String eventType, String userId, String details) {
        JsonObject body = new JsonObject();
        body.addProperty("event_type", eventType);
        body.addProperty("user_id", userId);
        body.addProperty("details", details);
        post("dc_stats", body);
    }

    public static JsonArray getStats(String eventType, String since) {
        String query = "event_type=eq." + eventType;
        if (since != null) query += "&created_at=gte." + since;
        query += "&order=created_at.desc";
        return get("dc_stats", query);
    }

    public static JsonArray getStaffStats(String since) {
        return get("dc_stats",
                "event_type=in.(ticket_claimed,ticket_closed)&created_at=gte." + since + "&order=created_at.desc");
    }

    // ========== BOT SETTINGS ==========

    public static JsonObject getSetting(String key) {
        JsonArray arr = get("dc_settings", "key=eq." + key + "&limit=1");
        return arr != null && arr.size() > 0 ? arr.get(0).getAsJsonObject() : null;
    }

    public static void setSetting(String key, String value) {
        JsonObject existing = getSetting(key);
        JsonObject body = new JsonObject();
        body.addProperty("key", key);
        body.addProperty("value", value);
        if (existing != null) {
            patch("dc_settings", "key=eq." + key, body);
        } else {
            post("dc_settings", body);
        }
    }

    // ========== HTTP HELPERS ==========

    private static JsonArray get(String table, String query) {
        Request request = auth(new Request.Builder())
                .url(url(table) + "?" + query)
                .get()
                .build();
        try (Response response = http.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "[]";
            if (!response.isSuccessful()) {
                log.error("GET {} failed: {} - {}", table, response.code(), responseBody);
                return new JsonArray();
            }
            return JsonParser.parseString(responseBody).getAsJsonArray();
        } catch (IOException e) {
            log.error("GET {} error: {}", table, e.getMessage());
            return new JsonArray();
        }
    }

    private static JsonObject post(String table, JsonObject body) {
        Request request = auth(new Request.Builder())
                .url(url(table))
                .post(RequestBody.create(body.toString(), JSON))
                .build();
        try (Response response = http.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "{}";
            if (!response.isSuccessful()) {
                log.error("POST {} failed: {} - {}", table, response.code(), responseBody);
                return null;
            }
            JsonElement el = JsonParser.parseString(responseBody);
            if (el.isJsonArray() && el.getAsJsonArray().size() > 0) {
                return el.getAsJsonArray().get(0).getAsJsonObject();
            }
            return el.isJsonObject() ? el.getAsJsonObject() : null;
        } catch (IOException e) {
            log.error("POST {} error: {}", table, e.getMessage());
            return null;
        }
    }

    public static JsonObject patch(String table, String filter, JsonObject body) {
        Request request = auth(new Request.Builder())
                .url(url(table) + "?" + filter)
                .patch(RequestBody.create(body.toString(), JSON))
                .build();
        try (Response response = http.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "{}";
            if (!response.isSuccessful()) {
                log.error("PATCH {} failed: {} - {}", table, response.code(), responseBody);
                return null;
            }
            JsonElement el = JsonParser.parseString(responseBody);
            if (el.isJsonArray() && el.getAsJsonArray().size() > 0) {
                return el.getAsJsonArray().get(0).getAsJsonObject();
            }
            return null;
        } catch (IOException e) {
            log.error("PATCH {} error: {}", table, e.getMessage());
            return null;
        }
    }

    // ========== GENERIC HELPERS (for new systems) ==========

    /** Generic insert — used by warnings, giveaways, points, levels, violations */
    public static JsonObject insert(String table, JsonObject body) {
        return post(table, body);
    }

    /** Generic query — used by warnings, giveaways, points, levels, violations */
    public static JsonArray query(String table, String filter) {
        return get(table, filter);
    }

    /** Generic delete with filter */
    public static void deleteWhere(String table, String filter) {
        Request request = auth(new Request.Builder())
                .url(url(table) + "?" + filter)
                .delete()
                .build();
        try (Response response = http.newCall(request).execute()) {
            if (!response.isSuccessful()) log.error("DELETE {} failed: {}", table, response.code());
        } catch (IOException e) { log.error("DELETE {} error: {}", table, e.getMessage()); }
    }

    /** Generic upsert (insert or update) */
    public static JsonObject upsert(String table, JsonObject body) {
        return upsert(table, body, null);
    }

    /** Upsert with specific conflict columns */
    public static JsonObject upsert(String table, JsonObject body, String onConflict) {
        Request.Builder rb = auth(new Request.Builder());
        if (onConflict != null) {
            rb.header("Prefer", "resolution=merge-duplicates,return=representation");
            rb.url(url(table) + "?on_conflict=" + onConflict);
        } else {
            rb.header("Prefer", "resolution=merge-duplicates,return=representation");
            rb.url(url(table));
        }
        Request request = rb.post(RequestBody.create(body.toString(), JSON)).build();
        try (Response response = http.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "{}";
            if (!response.isSuccessful()) { log.error("UPSERT {} failed: {} - {}", table, response.code(), responseBody); return null; }
            JsonElement el = JsonParser.parseString(responseBody);
            if (el.isJsonArray() && el.getAsJsonArray().size() > 0) return el.getAsJsonArray().get(0).getAsJsonObject();
            return el.isJsonObject() ? el.getAsJsonObject() : null;
        } catch (IOException e) { log.error("UPSERT {} error: {}", table, e.getMessage()); return null; }
    }

    // ========== WARNINGS ==========

    public static void addWarning(String userId, String userName, String warnedBy, String warnedByName, String reason, String guildId) {
        JsonObject body = new JsonObject();
        body.addProperty("user_id", userId); body.addProperty("user_name", userName);
        body.addProperty("warned_by", warnedBy); body.addProperty("warned_by_name", warnedByName);
        body.addProperty("reason", reason); body.addProperty("guild_id", guildId);
        post("dc_warnings", body);
    }

    public static int getWarningCount(String userId, String guildId) {
        JsonArray arr = get("dc_warnings", "user_id=eq." + userId + "&guild_id=eq." + guildId);
        return arr != null ? arr.size() : 0;
    }

    public static JsonArray getUserWarnings(String userId, String guildId) {
        return get("dc_warnings", "user_id=eq." + userId + "&guild_id=eq." + guildId + "&order=created_at.desc");
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

    // ========== VIOLATIONS ==========

    public static JsonArray getUserViolations(String userId, String guildId) {
        return get("dc_violations", "user_id=eq." + userId + "&guild_id=eq." + guildId + "&order=created_at.desc");
    }

    public static void clearUserViolations(String userId, String guildId) {
        delete("dc_violations", "user_id=eq." + userId + "&guild_id=eq." + guildId);
    }

    // ========== GIVEAWAYS ==========

    public static JsonObject createGiveaway(String channelId, String guildId, String hostId, String hostName,
                                             String prizeType, String prizeDetails, String currency, String couponExpiry,
                                             String serviceName, String discountInfo, int winnerCount, String endsAt) {
        JsonObject body = new JsonObject();
        body.addProperty("channel_id", channelId); body.addProperty("guild_id", guildId);
        body.addProperty("host_id", hostId); body.addProperty("host_name", hostName);
        body.addProperty("prize_type", prizeType); body.addProperty("prize_details", prizeDetails);
        if (currency != null) body.addProperty("currency", currency);
        if (couponExpiry != null) body.addProperty("coupon_expiry", couponExpiry);
        if (serviceName != null) body.addProperty("service_name", serviceName);
        if (discountInfo != null) body.addProperty("discount_info", discountInfo);
        body.addProperty("winner_count", winnerCount); body.addProperty("ends_at", endsAt);
        body.addProperty("ended", false);
        return post("dc_giveaways", body);
    }

    public static void setGiveawayMessageId(long giveawayId, String messageId) {
        JsonObject body = new JsonObject();
        body.addProperty("message_id", messageId);
        patch("dc_giveaways", "id=eq." + giveawayId, body);
    }

    public static JsonArray getActiveGiveaways() {
        return get("dc_giveaways", "ended=eq.false&order=ends_at.asc");
    }

    public static JsonObject getGiveawayById(long id) {
        JsonArray arr = get("dc_giveaways", "id=eq." + id + "&limit=1");
        return arr != null && arr.size() > 0 ? arr.get(0).getAsJsonObject() : null;
    }

    public static void endGiveaway(long id, String[] winners) {
        JsonObject body = new JsonObject();
        body.addProperty("ended", true);
        com.google.gson.JsonArray winnersArr = new com.google.gson.JsonArray();
        for (String w : winners) winnersArr.add(w);
        body.add("winners", winnersArr);
        patch("dc_giveaways", "id=eq." + id, body);
    }

    public static void addGiveawayEntry(long giveawayId, String userId) {
        JsonObject body = new JsonObject();
        body.addProperty("giveaway_id", giveawayId);
        body.addProperty("user_id", userId);
        post("dc_giveaway_entries", body);
    }

    public static JsonArray getGiveawayEntries(long giveawayId) {
        return get("dc_giveaway_entries", "giveaway_id=eq." + giveawayId);
    }

    public static boolean hasEnteredGiveaway(long giveawayId, String userId) {
        JsonArray arr = get("dc_giveaway_entries", "giveaway_id=eq." + giveawayId + "&user_id=eq." + userId + "&limit=1");
        return arr != null && arr.size() > 0;
    }

    // ========== POINTS ==========

    public static int getPoints(String userId, String guildId) {
        JsonArray arr = get("dc_points", "user_id=eq." + userId + "&guild_id=eq." + guildId + "&limit=1");
        if (arr != null && arr.size() > 0) return arr.get(0).getAsJsonObject().get("points").getAsInt();
        return 0;
    }

    public static void setPoints(String userId, String guildId, int amount, String reason, String givenBy) {
        JsonObject body = new JsonObject();
        body.addProperty("user_id", userId); body.addProperty("guild_id", guildId);
        body.addProperty("points", amount); body.addProperty("updated_at", java.time.Instant.now().toString());
        upsert("dc_points", body, "user_id,guild_id");
        // Log
        JsonObject logBody = new JsonObject();
        logBody.addProperty("user_id", userId); logBody.addProperty("guild_id", guildId);
        logBody.addProperty("amount", amount); logBody.addProperty("reason", reason);
        logBody.addProperty("given_by", givenBy);
        post("dc_points_log", logBody);
    }

    public static void addPoints(String userId, String guildId, int amount, String reason, String givenBy) {
        int current = getPoints(userId, guildId);
        setPoints(userId, guildId, Math.max(0, current + amount), reason, givenBy);
    }

    public static void resetAllPoints(String guildId) {
        // Get all points entries for this guild and set to 0
        JsonArray all = get("dc_points", "guild_id=eq." + guildId);
        if (all != null) {
            for (var el : all) {
                String uid = el.getAsJsonObject().get("user_id").getAsString();
                setPoints(uid, guildId, 0, "Global reset", "system");
            }
        }
    }

    public static JsonArray getTopPoints(String guildId, int limit) {
        return get("dc_points", "guild_id=eq." + guildId + "&order=points.desc&limit=" + limit);
    }

    // ========== LEVELS ==========

    public static JsonObject getLevelData(String userId, String guildId) {
        JsonArray arr = get("dc_levels", "user_id=eq." + userId + "&guild_id=eq." + guildId + "&limit=1");
        return arr != null && arr.size() > 0 ? arr.get(0).getAsJsonObject() : null;
    }

    public static void updateLevelData(String userId, String guildId, int xp, int level) {
        JsonObject body = new JsonObject();
        body.addProperty("user_id", userId); body.addProperty("guild_id", guildId);
        body.addProperty("xp", xp); body.addProperty("level", level);
        body.addProperty("last_xp_at", java.time.Instant.now().toString());
        body.addProperty("updated_at", java.time.Instant.now().toString());
        upsert("dc_levels", body, "user_id,guild_id");
    }

    public static void incrementMessages(String userId, String guildId) {
        JsonObject data = getLevelData(userId, guildId);
        int msgs = data != null && data.has("messages") ? data.get("messages").getAsInt() : 0;
        JsonObject body = new JsonObject();
        body.addProperty("user_id", userId); body.addProperty("guild_id", guildId);
        body.addProperty("messages", msgs + 1);
        body.addProperty("xp", data != null && data.has("xp") ? data.get("xp").getAsInt() : 0);
        body.addProperty("level", data != null && data.has("level") ? data.get("level").getAsInt() : 0);
        body.addProperty("updated_at", java.time.Instant.now().toString());
        upsert("dc_levels", body, "user_id,guild_id");
    }

    public static JsonArray getTopLevels(String guildId, int limit) {
        return get("dc_levels", "guild_id=eq." + guildId + "&order=xp.desc&limit=" + limit);
    }

    public static JsonObject getLevelReward(String guildId, int level) {
        JsonArray arr = get("dc_level_rewards", "guild_id=eq." + guildId + "&level=eq." + level + "&limit=1");
        return arr != null && arr.size() > 0 ? arr.get(0).getAsJsonObject() : null;
    }

    public static void setLevelReward(String guildId, int level, String roleId) {
        JsonObject body = new JsonObject();
        body.addProperty("guild_id", guildId); body.addProperty("level", level); body.addProperty("role_id", roleId);
        upsert("dc_level_rewards", body, "guild_id,level");
    }

    public static void removeLevelReward(String guildId, int level) {
        delete("dc_level_rewards", "guild_id=eq." + guildId + "&level=eq." + level);
    }

    public static JsonArray getLevelRewards(String guildId) {
        return get("dc_level_rewards", "guild_id=eq." + guildId + "&order=level.asc");
    }

    // ========== TEMP ROLES ==========

    public static void saveTempRole(String userId, String guildId, String roleId, String expiresAt) {
        JsonObject body = new JsonObject();
        body.addProperty("user_id", userId); body.addProperty("guild_id", guildId);
        body.addProperty("role_id", roleId); body.addProperty("expires_at", expiresAt);
        post("dc_temp_roles", body);
    }

    public static void deleteTempRole(String userId, String roleId) {
        delete("dc_temp_roles", "user_id=eq." + userId + "&role_id=eq." + roleId);
    }

    public static JsonArray getExpiredTempRoles() {
        return get("dc_temp_roles", "expires_at=lte." + java.time.Instant.now().toString());
    }

    // ========== COLOR ROLES ==========

    public static void saveColorRole(String guildId, String roleId, String colorName, String colorHex, int position) {
        JsonObject body = new JsonObject();
        body.addProperty("guild_id", guildId); body.addProperty("role_id", roleId);
        body.addProperty("color_name", colorName); body.addProperty("color_hex", colorHex);
        body.addProperty("position", position);
        upsert("dc_color_roles", body, "guild_id,color_name");
    }

    public static JsonArray getColorRoles(String guildId) {
        return get("dc_color_roles", "guild_id=eq." + guildId + "&order=position.asc");
    }

    public static JsonObject getColorRoleByPosition(String guildId, int position) {
        JsonArray arr = get("dc_color_roles", "guild_id=eq." + guildId + "&position=eq." + position + "&limit=1");
        return arr != null && arr.size() > 0 ? arr.get(0).getAsJsonObject() : null;
    }

    // ========== REPUTATION ==========

    public static int getReputation(String userId, String guildId) {
        JsonArray arr = get("dc_reputation", "user_id=eq." + userId + "&guild_id=eq." + guildId + "&limit=1");
        if (arr != null && arr.size() > 0) return arr.get(0).getAsJsonObject().get("rep_points").getAsInt();
        return 0;
    }

    public static void addReputation(String userId, String guildId) {
        int current = getReputation(userId, guildId);
        JsonObject body = new JsonObject();
        body.addProperty("user_id", userId); body.addProperty("guild_id", guildId);
        body.addProperty("rep_points", current + 1);
        upsert("dc_reputation", body, "user_id,guild_id");
    }

    public static boolean canGiveRep(String giverId, String guildId) {
        JsonArray arr = get("dc_rep_cooldown", "giver_id=eq." + giverId + "&guild_id=eq." + guildId + "&limit=1");
        if (arr == null || arr.size() == 0) return true;
        String lastGiven = arr.get(0).getAsJsonObject().get("last_given_at").getAsString();
        try {
            return java.time.Instant.parse(lastGiven).plus(24, java.time.temporal.ChronoUnit.HOURS).isBefore(java.time.Instant.now());
        } catch (Exception e) { return true; }
    }

    public static void setRepCooldown(String giverId, String guildId) {
        JsonObject body = new JsonObject();
        body.addProperty("giver_id", giverId); body.addProperty("guild_id", guildId);
        body.addProperty("last_given_at", java.time.Instant.now().toString());
        upsert("dc_rep_cooldown", body, "giver_id,guild_id");
    }

    // ========== VITOS ==========

    public static int getVitos(String userId, String guildId) {
        JsonArray arr = get("dc_vitos", "user_id=eq." + userId + "&guild_id=eq." + guildId + "&limit=1");
        if (arr != null && arr.size() > 0) return arr.get(0).getAsJsonObject().get("vitos").getAsInt();
        return 0;
    }

    public static void addVito(String userId, String guildId, int amount) {
        int current = getVitos(userId, guildId);
        JsonObject body = new JsonObject();
        body.addProperty("user_id", userId); body.addProperty("guild_id", guildId);
        body.addProperty("vitos", current + amount);
        upsert("dc_vitos", body, "user_id,guild_id");
    }

    // ========== SUGGESTIONS ==========

    public static JsonObject createSuggestion(String guildId, String userId, String userName, String content) {
        JsonObject body = new JsonObject();
        body.addProperty("guild_id", guildId); body.addProperty("user_id", userId);
        body.addProperty("user_name", userName); body.addProperty("content", content);
        body.addProperty("status", "pending");
        return post("dc_suggestions", body);
    }

    public static JsonObject getSuggestion(long id) {
        JsonArray arr = get("dc_suggestions", "id=eq." + id + "&limit=1");
        return arr != null && arr.size() > 0 ? arr.get(0).getAsJsonObject() : null;
    }

    public static void updateSuggestion(long id, String status, String reviewedBy, String note, String messageId, String channelId) {
        JsonObject body = new JsonObject();
        body.addProperty("status", status);
        if (reviewedBy != null) body.addProperty("reviewed_by", reviewedBy);
        if (note != null) body.addProperty("review_note", note);
        if (messageId != null) body.addProperty("message_id", messageId);
        if (channelId != null) body.addProperty("channel_id", channelId);
        patch("dc_suggestions", "id=eq." + id, body);
    }

    // ========== TITLES ==========

    public static String getTitle(String userId, String guildId) {
        JsonArray arr = get("dc_titles", "user_id=eq." + userId + "&guild_id=eq." + guildId + "&limit=1");
        if (arr != null && arr.size() > 0) return arr.get(0).getAsJsonObject().get("title").getAsString();
        return "";
    }

    public static void setTitle(String userId, String guildId, String title) {
        JsonObject body = new JsonObject();
        body.addProperty("user_id", userId); body.addProperty("guild_id", guildId);
        body.addProperty("title", title);
        upsert("dc_titles", body, "user_id,guild_id");
    }

    // ========== LEVELS (extended) ==========

    public static void setUserXp(String userId, String guildId, int xp) {
        int level = (int) Math.floor(Math.sqrt(xp / 100.0));
        JsonObject body = new JsonObject();
        body.addProperty("user_id", userId); body.addProperty("guild_id", guildId);
        body.addProperty("xp", xp); body.addProperty("level", level);
        body.addProperty("updated_at", java.time.Instant.now().toString());
        upsert("dc_levels", body, "user_id,guild_id");
    }

    public static void setUserLevel(String userId, String guildId, int level) {
        int xp = level * level * 100;
        setUserXp(userId, guildId, xp);
    }

    public static void resetUserLevel(String userId, String guildId) {
        setUserXp(userId, guildId, 0);
    }

    public static void resetAllLevels(String guildId) {
        JsonArray all = get("dc_levels", "guild_id=eq." + guildId);
        if (all != null) for (var el : all) resetUserLevel(el.getAsJsonObject().get("user_id").getAsString(), guildId);
    }

    // ========== HTTP HELPERS ==========

    private static JsonObject delete(String table, String filter) {
        Request request = auth(new Request.Builder())
                .url(url(table) + "?" + filter)
                .delete()
                .build();
        try (Response response = http.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.error("DELETE {} failed: {}", table, response.code());
            }
            return null;
        } catch (IOException e) {
            log.error("DELETE {} error: {}", table, e.getMessage());
            return null;
        }
    }
}
