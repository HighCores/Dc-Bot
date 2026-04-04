package com.highcore.bot.services;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.highcore.bot.config.Config;
import com.highcore.bot.database.SupabaseClient;
import com.highcore.bot.utils.EmbedUtil;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.*;

public class TicketReminderService {
    private static final Logger log = LoggerFactory.getLogger(TicketReminderService.class);
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public static void start(JDA jda) {
        // Run cleanup once on startup after a short delay (let JDA cache populate)
        scheduler.schedule(() -> {
            try { cleanupOrphanedTickets(jda); } catch (Exception e) { log.error("Startup cleanup error: {}", e.getMessage()); }
        }, 30, TimeUnit.SECONDS);

        // Then run both cleanup and reminders every hour
        scheduler.scheduleAtFixedRate(() -> {
            try {
                cleanupOrphanedTickets(jda);
                checkUnansweredTickets(jda);
            } catch (Exception e) { log.error("Reminder/cleanup error: {}", e.getMessage()); }
        }, 1, 1, TimeUnit.HOURS);
        log.info("Ticket reminder & cleanup service started");
    }

    /**
     * Find tickets that are open/claimed/reopened but their Discord channel no longer exists.
     * Auto-close them so they don't pollute stats or reminders.
     */
    private static void cleanupOrphanedTickets(JDA jda) {
        Guild guild = jda.getGuildById(Config.GUILD_ID);
        if (guild == null) return;

        int cleaned = 0;
        for (String status : new String[]{"open", "claimed", "reopened"}) {
            JsonArray tickets = SupabaseClient.getTicketsByStatus(status);
            if (tickets == null || tickets.size() == 0) continue;

            for (var el : tickets) {
                JsonObject ticket = el.getAsJsonObject();
                String channelId = ticket.has("channel_id") ? ticket.get("channel_id").getAsString() : null;
                String ticketId = ticket.has("ticket_id") ? ticket.get("ticket_id").getAsString() : null;

                if (channelId == null || ticketId == null) continue;

                // Check if the channel still exists in Discord
                TextChannel channel = guild.getTextChannelById(channelId);
                if (channel == null) {
                    // Channel was deleted manually — auto-close the ticket in database
                    SupabaseClient.updateTicketStatus(ticketId, "closed", "System (channel deleted)");
                    SupabaseClient.logStat("ticket_auto_closed", "system", ticketId + " (orphaned)");
                    cleaned++;
                    log.info("Auto-closed orphaned ticket {} (channel {} deleted)", ticketId, channelId);
                }
            }
        }

        if (cleaned > 0) {
            log.info("Cleaned up {} orphaned tickets", cleaned);
        }
    }

    private static void checkUnansweredTickets(JDA jda) {
        Guild guild = jda.getGuildById(Config.GUILD_ID);
        if (guild == null) return;
        JsonArray tickets = SupabaseClient.getTicketsByStatus("open");
        if (tickets == null || tickets.size() == 0) return;
        Instant cutoff = Instant.now().minus(24, ChronoUnit.HOURS);

        for (var el : tickets) {
            JsonObject ticket = el.getAsJsonObject();
            String createdAt = ticket.has("created_at") ? ticket.get("created_at").getAsString() : null;
            if (createdAt == null) continue;
            try {
                if (Instant.parse(createdAt).isBefore(cutoff)) {
                    String chId = ticket.get("channel_id").getAsString();
                    String tId = ticket.get("ticket_id").getAsString();
                    TextChannel ch = guild.getTextChannelById(chId);
                    if (ch == null) continue; // Skip — cleanup will handle this
                    ch.sendMessageEmbeds(EmbedUtil.warning("Reminder",
                            "\u23F0 Ticket **#" + tId + "** has been open for over **24 hours** without being claimed!\n\n" +
                                    "<@&" + Config.ROLE_STAFF + "> Please respond to the client.")).queue();
                }
            } catch (Exception e) { /* skip */ }
        }
    }

    public static void stop() { scheduler.shutdown(); }
}
