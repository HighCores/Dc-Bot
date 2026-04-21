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
import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.time.Instant;
import java.util.Collections;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GiveawayService {
    private static final Logger log = LoggerFactory.getLogger(GiveawayService.class);
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private static final String BANNER_WINNER = "https://i.imgur.com/HtHQ1vP.png";

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
        String bannerUrl = getGiveawayBanner(g);
        Container resultC;
        ActionRow endedRow = ActionRow.of(Button.secondary("giveaway_ended", "Deployment Concluded").asDisabled());
        if (userIds.isEmpty()) {
            resultC = EmbedUtil.containerBranded("GIVEAWAY ENDED", "No Winners", 
                    "Selection process finished.\n> **Item:** " + prizeDetails + "\n\n\u274C Not enough participants to pick a winner.", bannerUrl, endedRow);
        } else {
            StringBuilder wb = new StringBuilder();
            for (String w : winners) wb.append("<@").append(w).append("> ");
            resultC = EmbedUtil.containerBranded("GIVEAWAY ENDED", "Winners Picked", 
                    "The selection is complete!\n> **Item:** " + prizeDetails + "\n\n**Winner(s):** " + wb + "\n\nCongratulations! \uD83C\uDF8A", bannerUrl, endedRow);
        }

        if (messageId != null) {
            ch.editMessageComponentsById(messageId, resultC)
              .useComponentsV2(true)
              .queue(null, e -> {
                  // Fallback if edit fails
                  ch.sendMessageComponents(resultC).useComponentsV2(true).queue();
              });
        } else {
            ch.sendMessageComponents(resultC).useComponentsV2(true).queue();
        }

        LogManager.logEmbed(guild, Config.LOG_COMMANDS, EmbedUtil.createOldLogEmbed("giveaway-end", "Operation: Asset Distribution Finalized\nID: #" + giveawayId + "\nPrize: " + prizeDetails + "\nWinners Picked: " + (winners.isEmpty() ? "None" : winners.size()), null, null, null, EmbedUtil.SUCCESS));

        // 🎟️ Send Vouchers in DMs and send final message with IMAGE
        int value = extractValue(prizeDetails);
        String type = g.has("prize_type") && g.get("prize_type").getAsString().equalsIgnoreCase("Discount") ? "PERCENT" : "AMOUNT";
        int expiryDays = g.has("reward_expiry_days") ? g.get("reward_expiry_days").getAsInt() : 7;
        String expiresAt = Instant.now().plus(java.time.Duration.ofDays(expiryDays)).toString();

        if (!winners.isEmpty()) {
            final String firstWinnerId = winners.get(0);
            jda.retrieveUserById(firstWinnerId).queue(user -> {
                VoucherService.issueVoucher(user, value, type, expiresAt, prizeDetails);
                
                // Construct final container with WINNER IMAGE
                byte[] winnerImg = generateWinnerImage(user);
                StringBuilder wb = new StringBuilder();
                for (String w : winners) wb.append("<@").append(w).append("> ");
                
                if (winnerImg != null) {
                    net.dv8tion.jda.api.EmbedBuilder eb = new net.dv8tion.jda.api.EmbedBuilder()
                        .setTitle("\uD83C\uDF8A CONGRATULATIONS \u2014 Winner Identified")
                        .setDescription(wb.toString() + " won **" + prizeDetails + "**!\n\nEstablishing agency dominance through precision selection. Highcore operations are now finalized.")
                        .setImage("attachment://winner.png")
                        .setColor(com.highcore.bot.utils.EmbedUtil.ACCENT);
                    
                    ch.sendFiles(net.dv8tion.jda.api.utils.FileUpload.fromData(winnerImg, "winner.png"))
                      .setEmbeds(eb.build())
                      .queue(null, err -> log.error("Failed to send winner announcement: {}", err.getMessage()));
                } else {
                    ch.sendMessage("### \uD83C\uDF8A CONGRATULATIONS\n" + wb.toString() + " won **" + prizeDetails + "**!").queue();
                }
            }, e -> {
                log.error("Could not retrieve winner user: {}", e.getMessage());
            });
            
            // Issue vouchers to remaining winners
            for (int i = 1; i < winners.size(); i++) {
                jda.retrieveUserById(winners.get(i)).queue(user -> {
                    VoucherService.issueVoucher(user, value, type, expiresAt, prizeDetails);
                }, e -> {});
            }
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
                        ActionRow.of(Button.secondary("gw_reroll_adm_" + giveawayId, "Reroll New Winner")));
                
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

            String prizeDetails = g.has("prize_details") ? g.get("prize_details").getAsString() : "Gift";
            int value = extractValue(prizeDetails);
            String type = g.has("prize_type") && g.get("prize_type").getAsString().equalsIgnoreCase("Discount") ? "PERCENT" : "AMOUNT";
            int expiryDays = g.has("reward_expiry_days") ? g.get("reward_expiry_days").getAsInt() : 7;
            String expiresAt = Instant.now().plus(java.time.Duration.ofDays(expiryDays)).toString();

            for (String userId : winners) {
                jda.retrieveUserById(userId).queue(user -> {
                    VoucherService.issueVoucher(user, value, type, expiresAt, prizeDetails);
                }, e -> {});
            }
        }
    }

    private static int extractValue(String text) {
        if (text == null) return 15;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+)").matcher(text);
        if (m.find()) return Integer.parseInt(m.group(1));
        return 15;
    }

    private static List<String> pickWinners(List<String> pool, int count) {
        if (pool.isEmpty() || count <= 0) return List.of();
        List<String> shuffled = new ArrayList<>(pool);
        Collections.shuffle(shuffled);
        return shuffled.subList(0, Math.min(count, shuffled.size()));
    }

    public static byte[] generateWinnerImage(net.dv8tion.jda.api.entities.User user) {
        try {
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) new URL(BANNER_WINNER).openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            BufferedImage template;
            try (InputStream is = conn.getInputStream()) {
                byte[] bytes = is.readAllBytes();
                template = ImageIO.read(new java.io.ByteArrayInputStream(bytes));
            }
            if (template == null) {
                log.error("Failed to decode winner banner from: {}", BANNER_WINNER);
                return null;
            }

            int W = template.getWidth();
            int H = template.getHeight();
            log.info("Winner Banner Template loaded: {}x{}", W, H);

            BufferedImage image = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = image.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.drawImage(template, 0, 0, null);

            // 1. Draw Avatar in the box
            String avatarUrl = user.getEffectiveAvatarUrl();
            try {
                java.net.HttpURLConnection avConn = (java.net.HttpURLConnection) new URL(avatarUrl).openConnection();
                avConn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36");
                avConn.setConnectTimeout(8000);
                try (InputStream is = avConn.getInputStream()) {
                    byte[] avBytes = is.readAllBytes();
                    BufferedImage avatar = ImageIO.read(new java.io.ByteArrayInputStream(avBytes));
                    if (avatar != null) {
                        // Coordinates for the glass box (left-aligned square)
                        int boxX = 175;
                        int boxY = 175;
                        int boxSize = 385;
                        g.drawImage(avatar, boxX, boxY, boxSize, boxSize, null);
                    }
                }
            } catch (Exception ignored) {}

            // 2. Draw Name in the bar
            g.setFont(new Font("Source Code Pro", Font.BOLD, (int)(H * 0.08)));
            g.setColor(Color.WHITE);
            String name = user.getName().toUpperCase();
            int nameBarX = (int)(W * 0.49);
            int nameBarY = (int)(H * 0.675);
            int barWidth = (int)(W * 0.35);
            
            // Center the name in the bar area
            int textW = g.getFontMetrics().stringWidth(name);
            g.drawString(name, nameBarX + (barWidth - textW)/2, nameBarY);

            g.dispose();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Winner image generation failed: {}", e.getMessage());
            return null;
        }
    }

    private static String getGiveawayBanner(JsonObject g) {
        if (g == null || !g.has("prize_type")) return EmbedUtil.BANNER_GIVEAWAY;
        String type = g.get("prize_type").getAsString().toLowerCase();
        if (type.startsWith("discount-")) {
            return switch(type) {
                case "discount-10" -> "https://i.imgur.com/QpboYHV.png";
                case "discount-20" -> "https://i.imgur.com/FnsAuqW.png";
                case "discount-30" -> "https://i.imgur.com/n503P4n.png";
                case "discount-40" -> "https://i.imgur.com/4swCqaO.png";
                case "discount-50" -> "https://i.imgur.com/p1W4MGn.png";
                case "discount-60" -> "https://i.imgur.com/ujRHuoi.png";
                default -> EmbedUtil.BANNER_GIVEAWAY;
            };
        } else if (type.startsWith("voucher-")) {
            return switch(type) {
                case "voucher-50" -> "https://i.imgur.com/gqEoG4z.png";
                case "voucher-100" -> "https://i.imgur.com/DdlMSHd.png";
                default -> EmbedUtil.BANNER_GIVEAWAY;
            };
        }
        return EmbedUtil.BANNER_GIVEAWAY;
    }
}
