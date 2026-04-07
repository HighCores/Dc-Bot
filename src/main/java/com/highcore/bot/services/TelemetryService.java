package com.highcore.bot.services;

import com.highcore.bot.database.SupabaseClient;
import net.dv8tion.jda.api.JDA;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TelemetryService {
    private static final Logger log = LoggerFactory.getLogger(TelemetryService.class);
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public static void start(JDA jda) {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                long ping = jda.getGatewayPing();
                long uptime = java.lang.management.ManagementFactory.getRuntimeMXBean().getUptime();
                
                // Mirroring data to Supabase for the Dashboard (The "Mirrors")
                SupabaseClient.setSetting("bot_status", "ONLINE");
                SupabaseClient.setSetting("bot_latency", ping + "ms");
                SupabaseClient.setSetting("bot_uptime", formatUptime(uptime));
                
                log.debug("System performance check: ping={}ms, uptime={}", ping, formatUptime(uptime));
            } catch (Exception e) {
                log.error("Performance check failed: {}", e.getMessage());
            }
        }, 30, 60, TimeUnit.SECONDS);
    }

    private static String formatUptime(long duration) {
        long hours = TimeUnit.MILLISECONDS.toHours(duration);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(duration) % 60;
        return hours + "h " + minutes + "m";
    }

    public static void stop() {
        scheduler.shutdown();
    }
}
