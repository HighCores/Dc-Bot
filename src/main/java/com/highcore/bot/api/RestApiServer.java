package com.highcore.bot.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.highcore.bot.config.Config;
import com.highcore.bot.database.SupabaseClient;
import com.highcore.bot.services.*;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.UnauthorizedResponse;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class RestApiServer {

    private static final Logger log = LoggerFactory.getLogger(RestApiServer.class);
    private static final Gson gson = new Gson();
    private static Javalin app;
    private static JDA jda;

    public static void start(JDA jdaInstance) {
        jda = jdaInstance;

        app = Javalin.create(config -> {
            config.showJavalinBanner = false;
        }).start(Config.API_PORT);

        // Auth middleware - throws exception to stop unauthorized requests
        app.before("/api/*", ctx -> {
            String apiKey = ctx.header("X-API-Key");
            if (apiKey == null || !apiKey.equals(Config.API_KEY)) {
                throw new UnauthorizedResponse("Invalid API key");
            }
        });

        // ===== HEALTH (no auth needed) =====
        app.get("/health", ctx -> ctx.json(Map.of(
                "status", "ok",
                "bot", jda.getStatus().toString(),
                "uptime", getUptime()
        )));

        // ===== TICKETS API =====
        app.get("/api/tickets", RestApiServer::getTickets);
        app.get("/api/tickets/{id}", RestApiServer::getTicket);
        app.post("/api/tickets/{id}/close", RestApiServer::closeTicket);
        app.post("/api/tickets/{id}/reopen", RestApiServer::reopenTicket);
        app.get("/api/tickets/{id}/transcript", RestApiServer::getTranscript);

        // ===== AUTO-REPLIES API =====
        app.get("/api/auto-replies", RestApiServer::getAutoReplies);
        app.post("/api/auto-replies", RestApiServer::addAutoReply);
        app.delete("/api/auto-replies/{keyword}", RestApiServer::deleteAutoReply);

        // ===== STATS API =====
        app.get("/api/stats", RestApiServer::getStats);
        app.get("/api/stats/staff", RestApiServer::getStaffStats);

        // ===== SETTINGS API =====
        app.get("/api/settings/{key}", RestApiServer::getSetting);
        app.put("/api/settings/{key}", RestApiServer::setSetting);

        // ===== SEND MESSAGE API =====
        app.post("/api/channels/{channelId}/message", RestApiServer::sendMessage);

        // ===== WEBHOOK (for n8n) =====
        app.post("/api/webhook", RestApiServer::handleWebhook);

        log.info("REST API started on port {}", Config.API_PORT);
    }

    // ===== TICKETS =====

    private static void getTickets(Context ctx) {
        String status = ctx.queryParam("status");
        if (status != null) {
            ctx.json(SupabaseClient.getTicketsByStatus(status).toString());
        } else {
            var result = new com.google.gson.JsonArray();
            for (String s : new String[]{"open", "claimed", "closed", "reopened"}) {
                var tickets = SupabaseClient.getTicketsByStatus(s);
                if (tickets != null) tickets.forEach(result::add);
            }
            ctx.json(result.toString());
        }
    }

    private static void getTicket(Context ctx) {
        String id = ctx.pathParam("id");
        var ticket = SupabaseClient.getTicketById(id);
        if (ticket != null) {
            ctx.json(ticket.toString());
        } else {
            ctx.status(404).json(Map.of("error", "Ticket not found"));
        }
    }

    private static void closeTicket(Context ctx) {
        String id = ctx.pathParam("id");
        var ticket = SupabaseClient.getTicketById(id);
        if (ticket == null) {
            ctx.status(404).json(Map.of("error", "Ticket not found"));
            return;
        }

        SupabaseClient.updateTicketStatus(id, "closed", "API");

        String channelId = ticket.get("channel_id").getAsString();
        Guild guild = jda.getGuildById(Config.GUILD_ID);
        if (guild != null) {
            TextChannel channel = guild.getTextChannelById(channelId);
            if (channel != null) {
                channel.sendMessageEmbeds(
                        com.highcore.bot.utils.EmbedUtil.ticketClosed(id, "API/n8n")
                ).queue();
            }
        }

        ctx.json(Map.of("success", true, "message", "Ticket closed"));
    }

    private static void reopenTicket(Context ctx) {
        String id = ctx.pathParam("id");
        var ticket = SupabaseClient.getTicketById(id);
        if (ticket == null) {
            ctx.status(404).json(Map.of("error", "Ticket not found"));
            return;
        }

        SupabaseClient.updateTicketStatus(id, "reopened", null);
        ctx.json(Map.of("success", true, "message", "Ticket reopened"));
    }

    private static void getTranscript(Context ctx) {
        String id = ctx.pathParam("id");
        var messages = com.highcore.bot.database.SupabaseClient.getTicketMessages(id);
        if (messages == null || messages.size() == 0) {
            ctx.json(Map.of("ticket_id", id, "transcript", "No messages found."));
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (var el : messages) {
            var msg = el.getAsJsonObject();
            String user = msg.has("user_name") ? msg.get("user_name").getAsString() : "Unknown";
            String content = msg.has("content") ? msg.get("content").getAsString() : "";
            String time = msg.has("created_at") ? msg.get("created_at").getAsString() : "";
            sb.append("[").append(time).append("] ").append(user).append(": ").append(content).append("\n");
        }
        ctx.json(Map.of("ticket_id", id, "transcript", sb.toString()));
    }

    // ===== AUTO-REPLIES =====

    private static void getAutoReplies(Context ctx) {
        ctx.json(gson.toJson(AutoReplyService.getAllResponses()));
    }

    private static void addAutoReply(Context ctx) {
        JsonObject body = JsonParser.parseString(ctx.body()).getAsJsonObject();
        String keyword = body.get("keyword").getAsString();
        String response = body.get("response").getAsString();
        AutoReplyService.addResponse(keyword, response, "API");
        ctx.json(Map.of("success", true));
    }

    private static void deleteAutoReply(Context ctx) {
        String keyword = ctx.pathParam("keyword");
        AutoReplyService.removeResponse(keyword);
        ctx.json(Map.of("success", true));
    }

    // ===== STATS =====

    private static void getStats(Context ctx) {
        Map<String, Integer> stats = StatsService.getTicketStats();
        ctx.json(gson.toJson(stats));
    }

    private static void getStaffStats(Context ctx) {
        String topStaff = StatsService.getTopStaff(30);
        ctx.json(Map.of("top_staff", topStaff != null ? topStaff : "N/A"));
    }

    // ===== SETTINGS =====

    private static void getSetting(Context ctx) {
        String key = ctx.pathParam("key");
        var setting = SupabaseClient.getSetting(key);
        if (setting != null) {
            ctx.json(setting.toString());
        } else {
            ctx.status(404).json(Map.of("error", "Setting not found"));
        }
    }

    private static void setSetting(Context ctx) {
        String key = ctx.pathParam("key");
        JsonObject body = JsonParser.parseString(ctx.body()).getAsJsonObject();
        String value = body.get("value").getAsString();
        SupabaseClient.setSetting(key, value);
        ctx.json(Map.of("success", true));
    }

    // ===== SEND MESSAGE =====

    private static void sendMessage(Context ctx) {
        String channelId = ctx.pathParam("channelId");
        JsonObject body = JsonParser.parseString(ctx.body()).getAsJsonObject();
        String message = body.get("message").getAsString();

        Guild guild = jda.getGuildById(Config.GUILD_ID);
        if (guild == null) {
            ctx.status(500).json(Map.of("error", "Guild not found"));
            return;
        }

        TextChannel channel = guild.getTextChannelById(channelId);
        if (channel == null) {
            ctx.status(404).json(Map.of("error", "Channel not found"));
            return;
        }

        channel.sendMessage(message).queue();
        ctx.json(Map.of("success", true));
    }

    // ===== WEBHOOK =====

    private static void handleWebhook(Context ctx) {
        JsonObject body = JsonParser.parseString(ctx.body()).getAsJsonObject();
        String action = body.has("action") ? body.get("action").getAsString() : "";

        switch (action) {
            case "send_message" -> {
                String channelId = body.get("channel_id").getAsString();
                String message = body.get("message").getAsString();
                Guild guild = jda.getGuildById(Config.GUILD_ID);
                if (guild != null) {
                    TextChannel channel = guild.getTextChannelById(channelId);
                    if (channel != null) channel.sendMessage(message).queue();
                }
                ctx.json(Map.of("success", true));
            }
            case "close_ticket" -> {
                String ticketId = body.get("ticket_id").getAsString();
                SupabaseClient.updateTicketStatus(ticketId, "closed", "n8n");
                ctx.json(Map.of("success", true));
            }
            default -> ctx.json(Map.of("received", true, "action", action));
        }
    }

    private static String getUptime() {
        long uptime = java.lang.management.ManagementFactory.getRuntimeMXBean().getUptime();
        long hours = uptime / 3600000;
        long minutes = (uptime % 3600000) / 60000;
        return hours + "h " + minutes + "m";
    }

    public static void stop() {
        if (app != null) app.stop();
    }
}
