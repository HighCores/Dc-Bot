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
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.EmbedBuilder;
import com.highcore.bot.services.VoucherService;
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

        String messageId = g.has("message_id") && !g.get("message_id").isJsonNull() ? g.get("message_id").getAsString() : null;
        Container resultC;
        if (userIds.isEmpty()) {
            resultC = EmbedUtil.containerBranded("GIVEAWAY ENDED", "No Winners", 
                    "Selection process finished.\n> **Item:** " + prizeDetails + "\n\n\u274C Not enough participants to pick a winner.", EmbedUtil.BANNER_GIVEAWAY);
        } else {
            StringBuilder wb = new StringBuilder();
            for (String w : winners) wb.append("<@").append(w).append("> ");
            resultC = EmbedUtil.containerBranded("GIVEAWAY ENDED", "Winners Picked", 
                    "The selection is complete!\n> **Item:** " + prizeDetails + "\n\n**Winner(s):** " + wb + "\n\nCongratulations! \uD83C\uDF8A", EmbedUtil.BANNER_GIVEAWAY);
        }

        if (messageId != null) {
            ch.editMessageComponentsById(messageId, resultC)
              .setComponents(ActionRow.of(Button.secondary("giveaway_ended", "GIVEAWAY ENDED").asDisabled()))
              .useComponentsV2(true)
              .queue(null, e -> {
                  // Fallback if edit fails
                  ch.sendMessageComponents(resultC).useComponentsV2(true).queue();
              });
        } else {
            ch.sendMessageComponents(resultC).useComponentsV2(true).queue();
        }

        LogManager.logEmbed(guild, Config.LOG_COMMANDS, EmbedUtil.createOldLogEmbed("giveaway-end", "Operation: Asset Distribution Finalized\nID: #" + giveawayId + "\nPrize: " + prizeDetails + "\nWinners Picked: " + (winners.isEmpty() ? "None" : winners.size()), null, null, null, EmbedUtil.SUCCESS));

        // 🎟️ Send Vouchers in DMs
        for (String userId : winners) {
            jda.retrieveUserById(userId).queue(user -> {
                VoucherService.issueVoucher(user, 15); // Default 15% Giveaway Discount
            }, e -> {});
        }

        // 🔗 Sync Dashboard (if exists)
        String dashMsgId = com.highcore.bot.commands.GiveawayCommands.dashboardMessages.get(giveawayId);
        String dashChId = com.highcore.bot.commands.GiveawayCommands.dashboardChannels.get(giveawayId);
        
        if (dashMsgId != null && dashChId != null) {
            TextChannel dashCh = guild.getTextChannelById(dashChId);
            if (dashCh != null) {
                String dashDesc = "### " + prizeDetails + " | Final Report\n" +
                        "\u25AB\uFE0F **Status:** Deployment Concluded\n" +
                        "\u25AB\uFE0F **Outcome:** " + (userIds.isEmpty() ? "No participants" : winners.size() + " winners identified") + "\n" +
                        "\u25AB\uFE0F **Total Joined:** " + userIds.size() + " members";
                
                var dashC = EmbedUtil.containerBranded("GIVEAWAY DASHBOARD", "Operation Complete", dashDesc, EmbedUtil.BANNER_GIVEAWAY, 
                        ActionRow.of(Button.success("gw_reroll_adm_" + giveawayId, "Reroll New Winner")));
                
                dashCh.editMessageComponentsById(dashMsgId, dashC).useComponentsV2(true).queue(null, ex -> {});
            }
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
            PanelService.reply(ch, EmbedUtil.error("REROLL FAILED", "No users found to pick a new winner from."));
        } else {
            StringBuilder wb = new StringBuilder();
            for (String w : winners) wb.append("<@").append(w).append("> ");
            Container c = EmbedUtil.containerBranded("GIVEAWAY REROLLED", "New Winners", 
                    "New winners have been selected!\n\n**Winner(s):** " + wb + "\n\nCongratulations! \uD83C\uDF8A", EmbedUtil.BANNER_GIVEAWAY);
            ch.sendMessageComponents(c).useComponentsV2(true).queue();
            
            SupabaseClient.endGiveaway(giveawayId, winners.toArray(new String[0]));
            LogManager.logEmbed(guild, Config.LOG_COMMANDS, EmbedUtil.createOldLogEmbed("giveaway-reroll", "Action: Operational Backup Triggered\nID: #" + giveawayId + "\nNew Winners Identified: " + winners.size(), null, null, null, EmbedUtil.WARNING));

            // 🎟️ Send Vouchers in DMs
            for (String userId : winners) {
                jda.retrieveUserById(userId).queue(user -> {
                    VoucherService.issueVoucher(user, 15);
                }, e -> {});
            }
        }
    }

    private static List<String> pickWinners(List<String> pool, int count) {
        if (pool.isEmpty() || count <= 0) return List.of();
        List<String> shuffled = new ArrayList<>(pool);
        Collections.shuffle(shuffled);
        return shuffled.subList(0, Math.min(count, shuffled.size()));
    }
}
