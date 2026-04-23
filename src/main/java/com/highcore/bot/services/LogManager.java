package com.highcore.bot.services;

import com.highcore.bot.config.Config;
import com.highcore.bot.database.SupabaseClient;
import net.dv8tion.jda.api.Permission;
import com.highcore.bot.services.PanelService;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LogManager {
    private static final Logger log = LoggerFactory.getLogger(LogManager.class);
    private static final Map<String, TextChannel> channelCache = new ConcurrentHashMap<>();

    // All log channel names
    private static final String[] LOG_CHANNELS = {
            Config.LOG_JOIN_LEFT,
            Config.LOG_MESSAGE,
            Config.LOG_VOICE,
            Config.LOG_TICKETS,
            Config.LOG_CHANNELS,
            Config.LOG_UPDATES,
            Config.LOG_COMMANDS,
            Config.LOG_MODS_CMD,
            Config.LOG_ROLES,
            Config.LOG_USERS
    };

    /**
     * Called on bot startup. Finds existing log channels or creates missing ones.
     * Uses contains-based matching to handle Discord name normalization.
     */
    public static void initialize(Guild guild) {
        if (Config.LOG_CATEGORY_ID == null || Config.LOG_CATEGORY_ID.isEmpty()) {
            log.warn("LOG_CATEGORY_ID not set, skipping log channel init");
            return;
        }

        // Status: Log channels active. Resets disabled to preserve DB integrity.
        // SupabaseClient.clearTicketHistory();
        // SupabaseClient.setSetting("ticket_next_id_ORDER", "1");
        // SupabaseClient.setSetting("ticket_next_id_SUPPORT", "1");
        // SupabaseClient.setSetting("ticket_next_id_COMPLAINT", "1");

        Category category = guild.getCategoryById(Config.LOG_CATEGORY_ID);
        if (category == null) {
            log.error("Log category not found: {}", Config.LOG_CATEGORY_ID);
            return;
        }

        // Get ALL text channels in the category once
        List<TextChannel> existingChannels = category.getTextChannels();

        int found = 0;
        int created = 0;

        for (String name : LOG_CHANNELS) {
            // Priority: Try ID lookup if the name string is numeric
            if (name.matches("\\d+")) {
                TextChannel ch = guild.getTextChannelById(name);
                if (ch != null) {
                    channelCache.put(name, ch);
                    found++;
                    log.debug("Attached to existing ID-based log channel: {} (id: {})", ch.getName(), ch.getId());
                    continue; // Skip creation
                }
            }

            // Try exact match first, then normalized match from the category
            TextChannel existing = findChannel(existingChannels, name);
            if (existing != null) {
                channelCache.put(name, existing);
                found++;
                log.debug("Found existing log channel by name: {} (id: {})", existing.getName(), existing.getId());
            } else {
                try {
                    TextChannel ch = category.createTextChannel(name)
                            .addPermissionOverride(guild.getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL))
                            .complete();
                    channelCache.put(name, ch);
                    created++;
                    log.info("Created log channel: {}", name);
                } catch (Exception e) {
                    log.error("Failed to create log channel {}: {}", name, e.getMessage());
                }
            }
        }

        log.info("Log channels initialized: {} found existing, {} newly created", found, created);
    }

    /**
     * Find a channel by name with fallback to normalized matching.
     * Discord may normalize channel names (lowercase, replace special chars).
     */
    private static TextChannel findChannel(List<TextChannel> channels, String targetName) {
        // 1. Exact match
        for (TextChannel ch : channels) {
            if (ch.getName().equals(targetName)) {
                return ch;
            }
        }

        // 2. Normalized match — Discord converts names to lowercase and may alter unicode
        String normalized = targetName.toLowerCase().replaceAll("[^a-z0-9\\-_]", "");
        for (TextChannel ch : channels) {
            String chNormalized = ch.getName().toLowerCase().replaceAll("[^a-z0-9\\-_]", "");
            if (chNormalized.equals(normalized)) {
                return ch;
            }
        }

        // 3. Contains match — if the channel name contains the key part
        // Extract the meaningful part (e.g., "join-left-logs" from "⛔️join・left・logs")
        String keyPart = targetName.replaceAll("[^a-zA-Z]", "").toLowerCase();
        if (!keyPart.isEmpty()) {
            for (TextChannel ch : channels) {
                String chKey = ch.getName().replaceAll("[^a-zA-Z]", "").toLowerCase();
                if (chKey.equals(keyPart)) {
                    return ch;
                }
            }
        }

        return null;
    }

    /**
     * Get a log channel by name. Returns null if not found.
     */
    public static TextChannel get(String channelName) {
        return channelCache.get(channelName);
    }

    /**
     * Get a log channel, falling back to guild lookup if not cached.
     */
    public static TextChannel get(Guild guild, String channelName) {
        TextChannel cached = channelCache.get(channelName);
        if (cached != null) return cached;

        if (Config.LOG_CATEGORY_ID == null) return null;
        Category cat = guild.getCategoryById(Config.LOG_CATEGORY_ID);
        if (cat == null) return null;

        TextChannel found = findChannel(cat.getTextChannels(), channelName);
        if (found != null) {
            channelCache.put(channelName, found);
        }
        return found;
    }

    /**
     * Get a log channel. Prioritizes the Dashboard configuration, 
     * falls back to category-based auto-creation.
     */
    public static TextChannel getDashboardLogChannel(Guild guild, String channelKey) {
        // Try ID first if it looks like one
        if (channelKey.matches("\\d+")) {
            TextChannel ch = guild.getTextChannelById(channelKey);
            if (ch != null) return ch;
        }

        // Only use the global dashboard 'Moderation Log Channel' override FOR core moderation actions
        // (Bans, Kicks, Mutes). Other logs (Commands, Tickets) should stay in their specific channels.
        if (channelKey.equals(Config.LOG_MODS_CMD)) {
            String dashboardId = SettingSyncService.getModerationLogChannel();
            if (dashboardId != null && !dashboardId.isEmpty()) {
                TextChannel dashChan = guild.getTextChannelById(dashboardId);
                if (dashChan != null) return dashChan;
            }
        }
        return get(guild, channelKey);
    }

    public static void log(Guild guild, String title, String description, java.awt.Color color) {
        TextChannel ch = getDashboardLogChannel(guild, Config.LOG_MODS_CMD);
        if (ch != null) {
            PanelService.reply(ch, com.highcore.bot.utils.EmbedUtil.activityLog(title, description, color));
        }
    }

    public static void logEmbed(Guild guild, String channelKey, MessageEmbed embed) {
        if (guild == null || embed == null) return;
        TextChannel ch = getDashboardLogChannel(guild, channelKey);
        if (ch != null) {
            ch.sendMessageEmbeds(embed)
                .setAllowedMentions(java.util.Collections.emptyList())
                .queue();
        }
    }

    public static void logCommandInteraction(net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent event, String details) {
        if (event.getGuild() == null) return;
        logEmbed(event.getGuild(), Config.LOG_COMMANDS, 
            com.highcore.bot.utils.EmbedUtil.createOldLogEmbed(event.getName(), details + "\nChannel: " + event.getChannel().getAsMention(), event.getMember(), null, null, com.highcore.bot.utils.EmbedUtil.GOLD));
    }
}
