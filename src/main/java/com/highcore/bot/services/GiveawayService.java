package com.highcore.bot.services;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.highcore.bot.config.Config;
import com.highcore.bot.database.SupabaseClient;
import com.highcore.bot.utils.EmbedUtil;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.EmbedBuilder;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

public class GiveawayService {
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public static void start(JDA jda) {
        scheduler.scheduleAtFixedRate(() -> {
            try { checkEndingGiveaways(jda); } catch (Exception e) { }
        }, 30, 30, TimeUnit.SECONDS);
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
                    int winners = g.has("winner_count") ? g.get("winner_count").getAsInt() : 1;
                    endGiveaway(jda, g.get("id").getAsLong(), winners);
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

        JsonArray entries = SupabaseClient.getGiveawayEntries(giveawayId);
        List<String> userIds = new ArrayList<>();
        if (entries != null) {
            for (var e : entries) userIds.add(e.getAsJsonObject().get("user_id").getAsString());
        }

        List<String> winners = pickWinners(userIds, Math.min(winnerCount, userIds.size()));
        String[] winnersArr = winners.toArray(new String[0]);
        SupabaseClient.endGiveaway(giveawayId, winnersArr);

        String prizeDetails = g.has("prize_details") ? g.get("prize_details").getAsString() : "Classified Item";

        if (winners.isEmpty()) {
            MessageEmbed me = EmbedUtil.containerBranded("GIVEAWAY TERMINATED", "No Deployment Result", 
                    "Sequence Finished\n> **Item:** " + prizeDetails + "\n\n\u274C Insufficient data points detected. No winner assigned.", EmbedUtil.BANNER_GIVEAWAY);
            ch.sendMessageEmbeds(me).queue();
        } else {
            StringBuilder wb = new StringBuilder();
            for (String w : winners) wb.append("<@").append(w).append("> ");
            MessageEmbed me = EmbedUtil.containerBranded("GIVEAWAY CONCLUDED", "Successful Allocation", 
                    "Neural Selection Complete\n> **Item:** " + prizeDetails + "\n\n**Winner(s):** " + wb + "\n\nCongratulations! Synchronization complete. \uD83C\uDF8A", EmbedUtil.BANNER_GIVEAWAY);
            ch.sendMessageEmbeds(me).queue();
        }

        String messageId = g.has("message_id") && !g.get("message_id").isJsonNull() ? g.get("message_id").getAsString() : null;
        if (messageId != null) {
            String statusBody = "Sequence Deactivated\n" +
                    "> **Item:** " + prizeDetails + "\n" +
                    (winners.isEmpty() ? "\u274C No valid winners" : "\uD83C\uDFC6 Winner(s): " + String.join(", ", winners.stream().map(w -> "<@" + w + ">").toList())) +
                    "\n> Entries: **" + userIds.size() + "** subjects recorded.";
            
            MessageEmbed editC = EmbedUtil.containerBranded("GIVEAWAY ARCHIVE", "Sequence Finalized", statusBody, EmbedUtil.BANNER_GIVEAWAY);
            ch.editMessageEmbedsById(messageId, editC).setComponents(ActionRow.of(Button.secondary("giveaway_ended", "ARCHIVED").asDisabled())).queue(null, e -> {});
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

        int winnersNeeded = g.has("winner_count") ? g.get("winner_count").getAsInt() : 1;
        List<String> winners = pickWinners(userIds, winnersNeeded);
        if (winners.isEmpty()) {
            PanelService.reply(ch, EmbedUtil.error("REROLL FAILED", "No valid population data detected for re-allocation."));
        } else {
            StringBuilder wb = new StringBuilder();
            for (String w : winners) wb.append("<@").append(w).append("> ");
            MessageEmbed me = EmbedUtil.containerBranded("GIVEAWAY RE-CALIBRATED", "New Allocation", 
                    "New Selection Complete\n\n**Winner(s):** " + wb + "\n\nCongratulations! Structural integrity restored. \uD83C\uDF8A", EmbedUtil.BANNER_GIVEAWAY);
            ch.sendMessageEmbeds(me).queue();
            
            SupabaseClient.endGiveaway(giveawayId, winners.toArray(new String[0]));
        }
    }

    private static List<String> pickWinners(List<String> pool, int count) {
        if (pool.isEmpty() || count <= 0) return List.of();
        List<String> shuffled = new ArrayList<>(pool);
        Collections.shuffle(shuffled);
        return shuffled.subList(0, Math.min(count, shuffled.size()));
    }
}
