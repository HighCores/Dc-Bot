package com.highcore.bot.services;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.highcore.bot.config.Config;
import com.highcore.bot.database.SupabaseClient;
import com.highcore.bot.utils.EmbedUtil;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

public class GiveawayService {
    private static final Logger log = LoggerFactory.getLogger(GiveawayService.class);
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private static final Map<Long, ScheduledFuture<?>> activeTasks = new ConcurrentHashMap<>();

    public static void start(JDA jda) {
        // Check for ending giveaways every 30 seconds
        scheduler.scheduleAtFixedRate(() -> {
            try { checkEndingGiveaways(jda); } catch (Exception e) { log.error("Giveaway check error: {}", e.getMessage()); }
        }, 30, 30, TimeUnit.SECONDS);
        log.info("Giveaway service started");
    }

    private static void checkEndingGiveaways(JDA jda) {
        JsonArray active = SupabaseClient.getActiveGiveaways();
        if (active == null) return;
        Instant now = Instant.now();
        for (var el : active) {
            JsonObject g = el.getAsJsonObject();
            String endsAt = g.has("ends_at") ? g.get("ends_at").getAsString() : null;
            if (endsAt == null) continue;
            try {
                if (Instant.parse(endsAt).isBefore(now)) {
                    endGiveaway(jda, g.get("id").getAsLong(), 1);
                }
            } catch (Exception e) { /* skip */ }
        }
    }

    public static void endGiveaway(JDA jda, long giveawayId, int winnerCount) {
        JsonObject g = SupabaseClient.getGiveawayById(giveawayId);
        if (g == null || g.get("ended").getAsBoolean()) return;

        Guild guild = jda.getGuildById(Config.GUILD_ID);
        if (guild == null) return;
        String channelId = g.get("channel_id").getAsString();
        TextChannel ch = guild.getTextChannelById(channelId);
        if (ch == null) return;

        // Get entries
        JsonArray entries = SupabaseClient.getGiveawayEntries(giveawayId);
        List<String> userIds = new ArrayList<>();
        if (entries != null) {
            for (var e : entries) userIds.add(e.getAsJsonObject().get("user_id").getAsString());
        }

        // Pick winners
        List<String> winners = pickWinners(userIds, Math.min(winnerCount, userIds.size()));
        String[] winnersArr = winners.toArray(new String[0]);

        // Mark as ended
        SupabaseClient.endGiveaway(giveawayId, winnersArr);

        // Send winner announcement
        String prizeDetails = g.has("prize_details") ? g.get("prize_details").getAsString() : "Prize";
        String prizeType = g.has("prize_type") ? g.get("prize_type").getAsString() : "prize";

        if (winners.isEmpty()) {
            ch.sendMessageEmbeds(EmbedUtil.base().setColor(EmbedUtil.WARNING)
                    .setDescription("### \uD83C\uDF89 Giveaway Ended!\n> **" + prizeDetails + "**\n\n\u274C No valid entries. No winner.").build()).queue();
        } else {
            StringBuilder wb = new StringBuilder();
            for (String w : winners) wb.append("<@").append(w).append("> ");
            ch.sendMessageEmbeds(EmbedUtil.base().setColor(EmbedUtil.GOLD)
                    .setDescription("### \uD83C\uDF89 Giveaway Ended!\n> **" + prizeDetails + "**\n\n\uD83C\uDFC6 Winner(s): " + wb + "\n\nCongratulations! \uD83C\uDF8A").build()).queue();
        }

        // Update original message if exists
        String messageId = g.has("message_id") && !g.get("message_id").isJsonNull() ? g.get("message_id").getAsString() : null;
        if (messageId != null) {
            ch.editMessageEmbedsById(messageId, EmbedUtil.base().setColor(EmbedUtil.BRAND)
                    .setDescription("### \uD83C\uDF89 Giveaway Ended\n> **" + prizeDetails + "**\n\n" +
                            (winners.isEmpty() ? "\u274C No winners" : "\uD83C\uDFC6 Winner(s): " + String.join(", ", winners.stream().map(w -> "<@" + w + ">").toList())) +
                            "\n> Entries: **" + userIds.size() + "**").build())
                    .setComponents(ActionRow.of(Button.secondary("giveaway_ended", "\uD83C\uDF89 Ended").asDisabled())).queue(null, e -> {});
        }
    }

    public static void rerollGiveaway(JDA jda, long giveawayId) {
        JsonObject g = SupabaseClient.getGiveawayById(giveawayId);
        if (g == null) return;
        Guild guild = jda.getGuildById(Config.GUILD_ID);
        if (guild == null) return;
        TextChannel ch = guild.getTextChannelById(g.get("channel_id").getAsString());
        if (ch == null) return;

        JsonArray entries = SupabaseClient.getGiveawayEntries(giveawayId);
        List<String> userIds = new ArrayList<>();
        if (entries != null) for (var e : entries) userIds.add(e.getAsJsonObject().get("user_id").getAsString());

        List<String> winners = pickWinners(userIds, 1);
        if (winners.isEmpty()) {
            ch.sendMessageEmbeds(EmbedUtil.warning("Reroll", "No valid entries to reroll.")).queue();
        } else {
            ch.sendMessageEmbeds(EmbedUtil.base().setColor(EmbedUtil.GOLD)
                    .setDescription("### \uD83C\uDF00 Giveaway Rerolled!\n\uD83C\uDFC6 New winner: <@" + winners.get(0) + ">\nCongratulations! \uD83C\uDF8A").build()).queue();
        }
    }

    private static List<String> pickWinners(List<String> pool, int count) {
        if (pool.isEmpty() || count <= 0) return List.of();
        List<String> shuffled = new ArrayList<>(pool);
        Collections.shuffle(shuffled);
        return shuffled.subList(0, Math.min(count, shuffled.size()));
    }
}
