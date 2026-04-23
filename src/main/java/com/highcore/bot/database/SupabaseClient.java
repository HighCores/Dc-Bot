package com.highcore.bot.database;

import com.google.gson.*;
import com.highcore.bot.config.Config;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.ArrayList;

public class SupabaseClient {

    private static final Logger log = LoggerFactory.getLogger(SupabaseClient.class);
    private static final OkHttpClient http = new OkHttpClient();
    private static final MediaType JSON = MediaType.get("application/json");

    private static String url(String table) {
        return Config.SUPABASE_URL + "/rest/v1/" + table;
    }

    private static Request.Builder auth(Request.Builder rb) {
        String key = (Config.SUPABASE_KEY != null) ? Config.SUPABASE_KEY.trim() : "";
        return rb.header("apikey", key)
                 .header("Authorization", "Bearer " + key)
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

    public static int getNextTicketNumber(String type) {
        log.info("[DB DEBUG] Fetching next ticket number for: {}", type);
        String key = "ticket_next_id_" + type.toUpperCase();
        String nextStr = getSettingValue(key);
        int next = 1;
        if (nextStr != null) {
            try { next = Integer.parseInt(nextStr); } catch (Exception e) {}
        }
        
        // Prevent 409 collisions by checking if the ID already exists
        while (getTicketById(String.format("%04d", next)) != null) {
            next++;
        }

        setSetting(key, String.valueOf(next + 1));
        log.info("[DB DEBUG] Resulting ticket number: {}", next);
        return next;
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
            if (id.matches("\\d+")) {
                int nid = Integer.parseInt(id);
                arr = get("dc_tickets", "id=eq." + nid + "&limit=1");
            }
        }
        return arr != null && arr.size() > 0 ? arr.get(0).getAsJsonObject() : null;
    }

    public static void clearTicketHistory() {
        delete("dc_tickets", "id=gt.0");
        delete("dc_ticket_meta", "id=gt.0");
    }

    public static void createTicket(String ticketId, String userId, String userName, String channelId, String type, String subject, String priority) {
        log.info("[TRACE] createTicket called for ID: {}", ticketId);
        JsonObject body = new JsonObject();
        body.addProperty("ticket_id", ticketId);
        body.addProperty("user_id", userId);
        body.addProperty("user_name", userName);
        body.addProperty("status", "open");
        body.addProperty("priority", priority);
        body.addProperty("channel_id", channelId);
        body.addProperty("subject", subject);
        body.addProperty("platform", "Discord");
        
        log.info("[TRACE] Attempting POST to dc_tickets: {}", body.toString());
        post("dc_tickets", body);
        log.info("[TRACE] createTicket finished for ID: {}", ticketId);
    }

    public static void saveTicketMeta(String ticketId, JsonObject meta) {
        if (meta == null) return;
        setSetting("ticket_meta_" + ticketId, meta.toString());
    }

    public static JsonObject getTicketMeta(String ticketId) {
        String val = getSettingValue("ticket_meta_" + ticketId);
        if (val == null) return null;
        try { return JsonParser.parseString(val).getAsJsonObject(); } catch (Exception e) { return null; }
    }
    
    public static JsonObject getTicketAndMetaByChannel(String channelId) {
        JsonObject ticket = getTicketByChannel(channelId);
        if (ticket == null) return null;
        String tid = ticket.get("ticket_id").getAsString();
        JsonObject meta = getTicketMeta(tid);
        if (meta != null) ticket.add("metadata", meta);
        return ticket;
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
        return get("dc_ticket_messages", "ticket_id=eq." + ticketId + "&order=created_at.asc");
    }

    public static void saveTicketMessage(String ticketId, String userId, String userName, String content, String role, String messageId) {
        new Thread(() -> {
            try {
                Thread.sleep(1500);
                JsonObject body = new JsonObject();
                body.addProperty("ticket_id", ticketId); 
                body.addProperty("user_id", userId);
                body.addProperty("user_name", userName);
                body.addProperty("content", content);
                body.addProperty("message_id", messageId);
                post("dc_ticket_messages", body);
            } catch (Exception e) {
                log.error("Save error: ", e);
            }
        }).start();
    }

    // ========== MENUS & COMMANDS ==========

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

    public static JsonObject createGiveaway(String channelId, String guildId, String hostId, String hostName, String type, int prizeValue, String details, String currency, String expiry, String service, String discount, int winners, String endsAt, int rewardExpiryDays) {
        JsonObject body = new JsonObject();
        body.addProperty("channel_id", channelId);
        body.addProperty("guild_id", guildId);
        body.addProperty("host_id", hostId);
        body.addProperty("host_name", hostName);
        body.addProperty("prize_type", type);
        body.addProperty("prize_details", details);
        body.addProperty("currency", currency != null ? currency : "Points");
        body.addProperty("coupon_expiry", String.valueOf(rewardExpiryDays));
        body.addProperty("service_name", service != null ? service : "General");
        body.addProperty("discount_info", String.valueOf(prizeValue));
        body.addProperty("winner_count", winners);
        body.addProperty("ends_at", endsAt);
        body.addProperty("ended", false);
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

    public static JsonArray getAllGiveaways() {
        return get("dc_giveaways", "order=id.desc&limit=50");
    }

    public static void endGiveaway(long id, String[] winners) {
        JsonObject body = new JsonObject();
        body.addProperty("ended", true);
        if (winners != null) {
            JsonArray winArr = new JsonArray();
            for (String w : winners) winArr.add(w);
            body.add("winners", winArr);
        }
        patch("dc_giveaways", "id=eq." + id, body);
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

    // ========== TITLES & COLORS ==========

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

    public static List<String> getBannedWords() {
        JsonArray arr = get("dc_banned_words", null);
        java.util.List<String> words = new java.util.ArrayList<>();
        if (arr != null) for (var e : arr) words.add(e.getAsJsonObject().get("word").getAsString());
        return words;
    }

    public static void addBannedWord(String word) {
        JsonObject body = new JsonObject();
        body.addProperty("word", word);
        upsert("dc_banned_words", body, "word");
    }

    public static void removeBannedWord(String word) {
        delete("dc_banned_words", "word=eq." + word);
    }

    // ========== MODERATION & WARNINGS ==========

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
        return get("dc_warnings", "user_id=eq." + userId + "&guild_id=eq." + guildId + "&order=created_at.desc");
    }

    public static void clearUserWarnings(String userId, String guildId) {
        delete("dc_warnings", "user_id=eq." + userId + "&guild_id=eq." + guildId);
    }

    public static JsonArray getServerWarnings(String guildId, int limit) {
        return get("dc_warnings", "guild_id=eq." + guildId + "&order=created_at.desc&limit=" + limit);
    }

    public static void deleteWarningById(int id) {
        delete("dc_warnings", "id=eq." + id);
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
        Request request = auth(new Request.Builder()).url(url(table)).post(RequestBody.create(body.toString(), JSON)).build();
        try (Response response = http.newCall(request).execute()) {
            int code = response.code();
            String b = response.body() != null ? response.body().string() : "{}";
            if (!response.isSuccessful()) {
                log.error("POST {} failed: {} - {} | Body: {}", table, code, b, body.toString());
                return null;
            }
            JsonElement el = JsonParser.parseString(b);
            if (el.isJsonArray() && el.getAsJsonArray().size() > 0) return el.getAsJsonArray().get(0).getAsJsonObject();
            return el.isJsonObject() ? el.getAsJsonObject() : new JsonObject();
        } catch (IOException e) { log.error("POST {} error: {}", table, e.getMessage()); return null; }
    }

    public static void patch(String table, String filter, JsonObject body) {
        String fullUrl = url(table) + "?" + filter;
        Request request = auth(new Request.Builder()).url(fullUrl).patch(RequestBody.create(body.toString(), JSON)).build();
        try (Response response = http.newCall(request).execute()) { } catch (IOException e) { }
    }

    public static void upsert(String table, JsonObject body, String onConflict) {
        Request.Builder rb = auth(new Request.Builder()).url(url(table) + (onConflict != null ? "?on_conflict=" + onConflict : ""));
        rb.header("Prefer", "resolution=merge-duplicates,return=representation");
        Request request = rb.post(RequestBody.create(body.toString(), JSON)).build();
        try (Response response = http.newCall(request).execute()) { } catch (IOException e) { }
    }

    public static void delete(String table, String filter) {
        String fullUrl = url(table) + "?" + filter;
        Request request = auth(new Request.Builder()).url(fullUrl).delete().header("Prefer", "return=representation").build();
        try (Response response = http.newCall(request).execute()) { } catch (IOException e) { }
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

    // ========== DISCOUNTS & SCHEDULING ==========

    public static JsonArray getAllDiscounts() {
        return get("dc_discounts", "order=schedule_date.asc");
    }

    public static JsonArray getDiscountsByMonth(String monthStr) {
        return get("dc_discounts", "schedule_date=like." + monthStr + "*");
    }

    public static int getGlobalDiscountPercentage() {
        JsonArray all = getAllDiscounts();
        if (all == null) return 0;
        java.time.LocalDate now = java.time.LocalDate.now();
        for (var el : all) {
            JsonObject d = el.getAsJsonObject();
            String dateStr = d.get("schedule_date").getAsString();
            String type = d.get("type").getAsString();
            String repeat = d.has("repeat_interval") ? d.get("repeat_interval").getAsString().toUpperCase() : "NONE";
            int pct = d.has("percentage") ? d.get("percentage").getAsInt() : 15;
            
            java.time.LocalDate startDate = java.time.LocalDate.parse(dateStr);
            java.time.LocalDate endDate = d.has("end_date") && !d.get("end_date").isJsonNull() ? 
                java.time.LocalDate.parse(d.get("end_date").getAsString()) : null;

            boolean active = false;
            if (repeat.equals("NONE") || type.equals("MANUAL")) {
                if (endDate != null) {
                    if (!now.isBefore(startDate) && !now.isAfter(endDate)) active = true;
                } else {
                    if (startDate.equals(now)) active = true;
                }
            } else if (repeat.equals("MONTHLY")) {
                if (startDate.getDayOfMonth() == now.getDayOfMonth() && !now.isBefore(startDate)) active = true;
            } else if (repeat.equals("YEARLY")) {
                if (startDate.getMonthValue() == now.getMonthValue() && startDate.getDayOfMonth() == now.getDayOfMonth() && !now.isBefore(startDate)) active = true;
            } else if (repeat.equals("WEEKLY")) {
                if (startDate.getDayOfWeek() == now.getDayOfWeek() && !now.isBefore(startDate)) active = true;
            }
            if (active) return pct;
        }
        return 0;
    }

    public static void createDiscount(String type, String date, String repeat, int percentage, String name, String endDate) {
        JsonObject body = new JsonObject();
        body.addProperty("type", type);
        body.addProperty("schedule_date", date);
        body.addProperty("repeat_interval", repeat);
        body.addProperty("percentage", percentage);
        body.addProperty("name", name != null ? name : "Discount Event");
        if (endDate != null) body.addProperty("end_date", endDate);
        post("dc_discounts", body);
    }

    public static void deleteDiscount(long id) {
        delete("dc_discounts", "id=eq." + id);
    }

    // ===== Vouchers =====
    public static void createVoucher(String userId, String code, int value, String type, String expiresAt) {
        JsonObject json = new JsonObject();
        json.addProperty("user_id", userId);
        json.addProperty("code", code);
        json.addProperty("percentage", value); 
        json.addProperty("type", type);
        json.addProperty("expires_at", expiresAt);
        json.addProperty("is_used", false);
        post("dc_vouchers", json);
    }

    public static JsonObject getVoucherByCode(String code) {
        JsonArray arr = get("dc_vouchers", "code=ilike." + code);
        if (arr != null && !arr.isEmpty()) return arr.get(0).getAsJsonObject();
        return null;
    }

    public static void markVoucherAsUsed(String code) {
        JsonObject json = new JsonObject();
        json.addProperty("is_used", true);
        patch("dc_vouchers", "code=eq." + code, json);
    }
}
