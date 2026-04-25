package com.highcore.bot.services;

import com.highcore.bot.utils.EmbedUtil;
import net.dv8tion.jda.api.entities.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.util.Random;
import java.util.List;
import java.util.ArrayList;

public class VoucherService {
    private static final Logger log = LoggerFactory.getLogger(VoucherService.class);

    private static final String BACKGROUND_URL = "https://i.imgur.com/OXI22JW.png";

    static {
        ImageIO.setUseCache(false);
    }

    public static String generateRandomCode(int value, boolean isPercent) {
        Random r = new Random();
        char c1 = (char) ('A' + r.nextInt(26));
        char c2 = (char) ('A' + r.nextInt(26));
        int n1 = r.nextInt(10);
        int n2 = r.nextInt(10);
        return "HC" + value + "-" + c1 + c2 + n1 + n2;
    }

    public static byte[] drawVoucher(String code) {
        log.info("Voucher Generation: Starting for code {}", code);
        try {
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) new java.net.URL(BACKGROUND_URL).openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36");
            conn.setRequestProperty("Accept", "image/png,image/jpeg,image/*;q=0.8");
            conn.setConnectTimeout(20000);
            conn.setReadTimeout(20000);
            conn.setInstanceFollowRedirects(true);
            
            byte[] bytes;
            try (InputStream is = conn.getInputStream()) {
                bytes = is.readAllBytes();
            }

            if (bytes == null || bytes.length < 1000) {
                log.error("Voucher Background: Empty or invalid stream (Size: {} bytes) from {}. Response: {}", 
                    (bytes != null ? bytes.length : 0), BACKGROUND_URL, conn.getResponseCode());
                return null;
            }

            BufferedImage base = ImageIO.read(new ByteArrayInputStream(bytes));
            if (base == null) return null;

            int w = base.getWidth();
            int h = base.getHeight();
            BufferedImage combined = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = combined.createGraphics();

            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

            g.drawImage(base, 0, 0, null);

            Font font = new Font("Source Code Pro", Font.BOLD, 60);
            if (font.getFamily().equals("Dialog") || font.getFamily().equals("SansSerif")) {
                font = new Font("Monospaced", Font.BOLD, 60);
            }
            
            g.setFont(font);
            g.setColor(Color.WHITE); 

            FontMetrics fm = g.getFontMetrics();
            int textW = fm.stringWidth(code);
            int textH = fm.getAscent();

            int rectX = 974;
            int rectY = 302;
            int rectW = 612;
            int rectH = 120;

            int x = rectX + (rectW - textW) / 2;
            int y = rectY + (rectH + textH) / 2 - 5;

            g.drawString(code, x, y);
            g.dispose();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(combined, "png", baos);
            log.info("Voucher Generation: Success for code {}", code);
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Voucher Generation: Error - {}", e.getMessage());
            return null;
        }
    }

    public static void issueVoucher(User user, int value, String type, String expiresAt, String prizeDetails, net.dv8tion.jda.api.entities.channel.concrete.TextChannel ch, net.dv8tion.jda.api.entities.channel.concrete.TextChannel adminLogs) {
        boolean isPercent = type.equalsIgnoreCase("PERCENT") || type.toLowerCase().contains("discount");
        String code = generateRandomCode(value, isPercent);
        
        log.info("Voucher Issuance: {} for {}", code, user.getName());
        com.highcore.bot.database.SupabaseClient.createVoucher(user.getId(), code, value, type, expiresAt);
        
        byte[] voucherImg = drawVoucher(code);

        long ts = 0;
        try {
            ts = java.time.Instant.parse(expiresAt).getEpochSecond();
        } catch (Exception ignored) {
            ts = java.time.Instant.now().plus(java.time.Duration.ofDays(7)).getEpochSecond();
        }

        String body2 = "### \uD83C\uDFAB VOUCHER ISSUED\n" +
                "**Congratulations <@" + user.getId() + ">!**\n" +
                "● **Item:** `" + prizeDetails + "`\n" +
                "● **Code:** `" + code + "`\n" +
                "● **Expiry:** <t:" + ts + ":D> (<t:" + ts + ":R>)\n\n" +
                "Claim this voucher through the agency terminal or support desk.";
        
        var eb2 = EmbedUtil.containerBranded("PRIZE ACQUIRED", "Voucher Deployment", body2,
                (voucherImg != null ? "attachment://voucher.png" : null));

        if (voucherImg != null) {
            user.openPrivateChannel().queue(dm -> {
                dm.sendMessageComponents(eb2)
                  .setFiles(net.dv8tion.jda.api.utils.FileUpload.fromData(voucherImg, "voucher.png"))
                  .useComponentsV2(true).queue(null, ex -> {
                      log.error("Failed to DM voucher to {}: {}", user.getName(), ex.getMessage());
                      ch.sendMessage("### \u26A0\uFE0F ENCRYPTION ERROR\n<@" + user.getId() + ">, I couldn't DM your voucher. Please enable DMs and contact staff.").queue();
                      if (adminLogs != null) {
                          adminLogs.sendMessage("### \u26A0\uFE0F DM FAILURE\nWinner <@" + user.getId() + "> (" + user.getName() + ") did not receive their voucher for **" + prizeDetails + "** because their DMs are closed.").queue();
                      }
                  });
            });
        } else {
            user.openPrivateChannel().queue(dm -> {
                dm.sendMessageComponents(eb2).useComponentsV2(true).queue(null, ex -> {
                    log.error("Failed to DM voucher to {}: {}", user.getName(), ex.getMessage());
                    ch.sendMessage("### \u26A0\uFE0F ENCRYPTION ERROR\n<@" + user.getId() + ">, I couldn't DM your voucher. Please enable DMs and contact staff.").queue();
                    if (adminLogs != null) {
                        adminLogs.sendMessage("### \u26A0\uFE0F DM FAILURE\nWinner <@" + user.getId() + "> (" + user.getName() + ") did not receive their voucher for **" + prizeDetails + "** because their DMs are closed.").queue();
                    }
                });
            });
        }
    }

    public static void issueVoucherToChannel(User user, int value, String type, String expiresAt, String prizeDetails, net.dv8tion.jda.api.entities.channel.concrete.TextChannel ch) {
        boolean isPercent = type.equalsIgnoreCase("PERCENT") || type.toLowerCase().contains("discount");
        String code = generateRandomCode(value, isPercent);
        
        com.highcore.bot.database.SupabaseClient.createVoucher(user.getId(), code, value, type, expiresAt);
        
        byte[] voucherImg = drawVoucher(code);

        long ts = 0;
        try {
            ts = java.time.Instant.parse(expiresAt).getEpochSecond();
        } catch (Exception ignored) {
            ts = java.time.Instant.now().plus(java.time.Duration.ofDays(7)).getEpochSecond();
        }

        String body = "### \uD83C\uDFAB VOUCHER ISSUED\n" +
                "**Congratulations <@" + user.getId() + ">!**\n" +
                "● **Item:** `" + prizeDetails + "`\n" +
                "● **Code:** `" + code + "`\n" +
                "● **Expiry:** <t:" + ts + ":D> (<t:" + ts + ":R>)\n\n" +
                "Claim this voucher through the agency terminal or support desk.";
        
        var eb = EmbedUtil.containerBranded("PRIZE ACQUIRED", "Voucher Deployment", body,
                (voucherImg != null ? "attachment://voucher.png" : null));

        if (voucherImg != null) {
            ch.sendFiles(net.dv8tion.jda.api.utils.FileUpload.fromData(voucherImg, "voucher.png"))
              .setComponents(eb)
              .useComponentsV2(true).queue();
        } else {
            ch.sendMessageComponents(eb).useComponentsV2(true).queue();
        }
    }
}
