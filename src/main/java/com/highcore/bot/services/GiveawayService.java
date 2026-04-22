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
import net.dv8tion.jda.api.utils.FileUpload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.net.URL;
import java.time.Instant;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;

public class GiveawayService {
    private static final Logger log = LoggerFactory.getLogger(GiveawayService.class);
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public static final String BANNER_WINNER = "https://imgur.com/lpsAlRe.png";

    static {
        ImageIO.setUseCache(false);
    }

    public static void start(JDA jda) {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                checkEndingGiveaways(jda);
            } catch (Exception ignored) {
            }
        }, 30, 30, TimeUnit.SECONDS);
    }

    private static void checkEndingGiveaways(JDA jda) {
        JsonArray active = SupabaseClient.getActiveGiveaways();
        if (active == null)
            return;
        Instant now = Instant.now();
        for (var el : active) {
            JsonObject g = el.getAsJsonObject();
            String endsAt = g.has("ends_at") ? g.get("ends_at").getAsString() : null;
            if (endsAt == null)
                continue;
            try {
                if (Instant.parse(endsAt).isBefore(now)) {
                    int winners = g.has("winner_count") ? g.get("winner_count").getAsInt() : 1;
                    endGiveaway(jda, g.get("id").getAsLong(), winners);
                }
            } catch (Exception ignored) {
            }
        }
    }

    public static void endGiveaway(JDA jda, long giveawayId, int winnerCount) {
        JsonObject g = SupabaseClient.getGiveawayById(giveawayId);
        if (g == null || g.get("ended").getAsBoolean())
            return;

        Guild guild = jda.getGuildById(Config.GUILD_ID);
        if (guild == null)
            return;
        TextChannel ch = guild.getTextChannelById(g.get("channel_id").getAsString());
        if (ch == null)
            return;

        JsonArray entries = SupabaseClient.getGiveawayEntries(giveawayId);
        List<String> userIds = new ArrayList<>();
        if (entries != null) {
            for (var e : entries)
                userIds.add(e.getAsJsonObject().get("user_id").getAsString());
        }

        List<String> winners = pickWinners(userIds, Math.min(winnerCount, userIds.size()));
        SupabaseClient.endGiveaway(giveawayId, winners.toArray(new String[0]));

        String prizeDetails = g.has("prize_details") ? g.get("prize_details").getAsString() : "Classified Item";
        int value = 0;
        if (g.has("prize_value") && !g.get("prize_value").isJsonNull()) {
            try {
                value = g.get("prize_value").getAsInt();
            } catch (Exception e) {
            }
        } else if (g.has("discount_info") && !g.get("discount_info").isJsonNull()
                && !g.get("discount_info").getAsString().isEmpty()) {
            try {
                value = Integer.parseInt(g.get("discount_info").getAsString());
            } catch (Exception e) {
            }
        }
        String type = g.has("prize_type") ? g.get("prize_type").getAsString() : "PERCENT";
        String endsStr = g.has("ends_at") ? g.get("ends_at").getAsString() : Instant.now().toString();
        int expiryDays = 7;
        if (g.has("coupon_expiry") && !g.get("coupon_expiry").isJsonNull()
                && !g.get("coupon_expiry").getAsString().isEmpty()) {
            try {
                expiryDays = Integer.parseInt(g.get("coupon_expiry").getAsString());
            } catch (Exception e) {
            }
        } else if (g.has("reward_expiry_days") && !g.get("reward_expiry_days").isJsonNull()) {
            try {
                expiryDays = g.get("reward_expiry_days").getAsInt();
            } catch (Exception e) {
            }
        }
        String expiresAt = Instant.parse(endsStr).plus(java.time.Duration.ofDays(expiryDays)).toString();

        final int finalValue = value;

        if (winners.isEmpty()) {
            ch.sendMessageComponents(EmbedUtil.containerBranded("GIVEAWAY ENDED", "No Winners",
                    "Selection process finished.\n> **Item:** " + prizeDetails + "\n\n\u274C Not enough participants.",
                    EmbedUtil.BANNER_GIVEAWAY)).useComponentsV2(true).queue();
        } else {
            final String firstId = winners.get(0);
            jda.retrieveUserById(firstId).queue(user -> {
                byte[] winnerImg = generateWinnerImage(user, prizeDetails);
                VoucherService.issueVoucher(user, finalValue, type, expiresAt, prizeDetails, ch);

                StringBuilder wb = new StringBuilder();
                for (String w : winners)
                    wb.append("<@").append(w).append("> ");

                if (winnerImg != null) {
                    var eb = EmbedUtil.containerBranded("GIVEAWAY", "Winner Identified",
                            wb + " won **" + prizeDetails
                                    + "**!\n\n> Voucher Sent In Dm",
                            "attachment://winner.png");

                    ch.sendFiles(FileUpload.fromData(winnerImg, "winner.png")).setContent("").setComponents(eb)
                            .useComponentsV2(true).queue();
                } else {
                    ch.sendMessage("### \uD83C\uDF8A CONGRATULATIONS\n" + wb + " won **" + prizeDetails + "**!\n> Voucher Sent In Dm")
                            .queue();
                }

                // Others
                for (int i = 1; i < winners.size(); i++) {
                    jda.retrieveUserById(winners.get(i)).queue(u -> {
                        byte[] wImg = generateWinnerImage(u, prizeDetails);
                        VoucherService.issueVoucher(u, finalValue, type, expiresAt, prizeDetails, ch);
                    }, e -> {
                    });
                }
            }, e -> log.error("Failed to retrieve winner: {}", e.getMessage()));
        }

        // Sync Dashboard
        String dashMsgId = com.highcore.bot.commands.GiveawayCommands.dashboardMessages.get(giveawayId);
        String dashChId = com.highcore.bot.commands.GiveawayCommands.dashboardChannels.get(giveawayId);
        if (dashMsgId != null && dashChId != null) {
            TextChannel dashCh = guild.getTextChannelById(dashChId);
            if (dashCh != null) {
                String dashDesc = "### " + prizeDetails
                        + " | Final Report\n▫️ **Status:** Deployment Concluded\n▫️ **Outcome:** " + winners.size()
                        + " winners identified";
                var dashC = EmbedUtil.containerBranded("GIVEAWAY DASHBOARD", "Operation Complete", dashDesc,
                        EmbedUtil.BANNER_GIVEAWAY,
                        ActionRow.of(Button.success("gw_reroll_adm_" + giveawayId, "Reroll New Winner")));
                dashCh.editMessageComponentsById(dashMsgId, dashC).useComponentsV2(true).queue(null, ex -> {
                });
            }
        }
    }

    public static byte[] generateWinnerImage(net.dv8tion.jda.api.entities.User user, String prize) {
        log.info("Winner Image: Processing for {} with prize {}", user.getName(), prize);
        String bannerUrl = BANNER_WINNER;
        try {
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) new URL(bannerUrl).openConnection();
            conn.setRequestProperty("User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36");
            conn.setRequestProperty("Accept", "image/png,image/jpeg,image/*;q=0.8");
            conn.setConnectTimeout(20000);
            conn.setReadTimeout(20000);
            conn.setInstanceFollowRedirects(true);

            BufferedImage template;
            try (InputStream is = conn.getInputStream()) {
                byte[] bytes = is.readAllBytes();
                if (bytes == null || bytes.length < 1000)
                    return null;
                template = ImageIO.read(new ByteArrayInputStream(bytes));
            }
            if (template == null)
                return null;

            int W = template.getWidth();
            int H = template.getHeight();
            BufferedImage image = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = image.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.drawImage(template, 0, 0, null);

            String avatarUrl = user.getEffectiveAvatarUrl();
            try {
                java.net.HttpURLConnection avConn = (java.net.HttpURLConnection) new URL(avatarUrl).openConnection();
                avConn.setRequestProperty("User-Agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36");
                avConn.setConnectTimeout(10000);
                try (InputStream is = avConn.getInputStream()) {
                    byte[] avBytes = is.readAllBytes();
                    BufferedImage avatar = ImageIO.read(new ByteArrayInputStream(avBytes));
                    if (avatar != null) {
                        g.drawImage(avatar, 465, 253, 547, 547, null);
                    }
                }
            } catch (Exception ignored) {
            }

            g.dispose();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Winner Image Error: {}", e.getMessage());
            return null;
        }
    }

    public static void rerollGiveaway(JDA jda, long giveawayId) {
        JsonObject g = SupabaseClient.getGiveawayById(giveawayId);
        if (g == null)
            return;

        JsonArray entries = SupabaseClient.getGiveawayEntries(giveawayId);
        List<String> userIds = new ArrayList<>();
        if (entries != null) {
            for (var e : entries)
                userIds.add(e.getAsJsonObject().get("user_id").getAsString());
        }

        if (userIds.isEmpty())
            return;
        List<String> winners = pickWinners(userIds, 1);

        String prizeDetails = g.has("prize_details") ? g.get("prize_details").getAsString() : "Classified Item";
        int value = 0;
        if (g.has("prize_value") && !g.get("prize_value").isJsonNull()) {
            try {
                value = g.get("prize_value").getAsInt();
            } catch (Exception e) {
            }
        } else if (g.has("discount_info") && !g.get("discount_info").isJsonNull()
                && !g.get("discount_info").getAsString().isEmpty()) {
            try {
                value = Integer.parseInt(g.get("discount_info").getAsString());
            } catch (Exception e) {
            }
        }
        String type = g.has("prize_type") ? g.get("prize_type").getAsString() : "PERCENT";
        String endsStr = g.has("ends_at") ? g.get("ends_at").getAsString() : Instant.now().toString();
        int expiryDays = 7;
        if (g.has("coupon_expiry") && !g.get("coupon_expiry").isJsonNull()
                && !g.get("coupon_expiry").getAsString().isEmpty()) {
            try {
                expiryDays = Integer.parseInt(g.get("coupon_expiry").getAsString());
            } catch (Exception e) {
            }
        } else if (g.has("reward_expiry_days") && !g.get("reward_expiry_days").isJsonNull()) {
            try {
                expiryDays = g.get("reward_expiry_days").getAsInt();
            } catch (Exception e) {
            }
        }
        String expiresAt = Instant.parse(endsStr).plus(java.time.Duration.ofDays(expiryDays)).toString();

        final int finalValue = value;

        Guild guild = jda.getGuildById(Config.GUILD_ID);
        if (guild == null)
            return;
        TextChannel ch = guild.getTextChannelById(g.get("channel_id").getAsString());
        if (ch == null)
            return;

        jda.retrieveUserById(winners.get(0)).queue(user -> {
            byte[] winnerImg = generateWinnerImage(user, prizeDetails);
            VoucherService.issueVoucher(user, finalValue, type, expiresAt, prizeDetails, ch);

            if (winnerImg != null) {
                var eb = EmbedUtil.containerBranded("REROLL", "New Winner Identified",
                        "<@" + user.getId() + "> won the reroll for **" + prizeDetails + "**!",
                        "attachment://winner.png");
                ch.sendFiles(FileUpload.fromData(winnerImg, "winner.png")).setContent("").setComponents(eb)
                        .useComponentsV2(true).queue();
            } else {
                ch.sendMessage("### \uD83C\uDF8A REROLL SUCCESSFUL\n<@" + user.getId() + "> won the reroll for **"
                        + prizeDetails + "**!").queue();
            }
        });
    }

    private static List<String> pickWinners(List<String> users, int count) {
        if (users == null || users.isEmpty())
            return new ArrayList<>();
        Collections.shuffle(users);
        return users.subList(0, Math.min(count, users.size()));
    }
}
