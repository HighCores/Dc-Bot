package com.highcore.bot.services;

import net.dv8tion.jda.api.entities.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Random;

public class VoucherService {
    private static final Logger log = LoggerFactory.getLogger(VoucherService.class);

    private static final String BACKGROUND_URL = "https://i.imgur.com/OXI22JW.png";

    public static String generateRandomCode(int value, boolean isPercent) {
        Random r = new Random();
        char c1 = (char) ('A' + r.nextInt(26));
        char c2 = (char) ('A' + r.nextInt(26));
        int n1 = r.nextInt(10);
        int n2 = r.nextInt(10);
        
        // Format: HC[Value]-[A-Z][A-Z][0-9][0-9]
        return "HC" + value + "-" + c1 + c2 + n1 + n2;
    }

    public static byte[] drawVoucher(String code) {
        log.info("Generating voucher image for code: {}", code);
        try {
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) new java.net.URL(BACKGROUND_URL).openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            
            BufferedImage base;
            try (java.io.InputStream is = conn.getInputStream()) {
                base = ImageIO.read(is);
            }
            
            if (base == null) {
                log.error("ImageIO failed to decode background from URL: {}. Response Code: {}", BACKGROUND_URL, conn.getResponseCode());
                throw new Exception("Failed to load background image (decode failure)");
            }

            int w = base.getWidth();
            int h = base.getHeight();
            // Use ARGB to preserve quality
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

            // Golden Rectangle Coordinates
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
            log.info("Voucher image generated successfully for code: {}", code);
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Critical failure during voucher image generation: {}", e.getMessage());
            return null;
        }
    }

    public static void issueVoucher(User user, int value, String type, String expiresAt, String prizeDetails) {
        boolean isPercent = type.equalsIgnoreCase("PERCENT");
        String code = generateRandomCode(value, isPercent);
        
        log.info("Issuing voucher for {}: Code={}, Value={}, Type={}", user.getName(), code, value, type);
        
        com.highcore.bot.database.SupabaseClient.createVoucher(user.getId(), code, value, type, expiresAt);
        
        byte[] img = drawVoucher(code);
        if (img == null) {
            log.warn("Voucher image generation failed for {}, sending text fallback.", user.getName());
        }

        user.openPrivateChannel().queue(pc -> {
            long ts = 0;
            try {
                ts = java.time.Instant.parse(expiresAt).getEpochSecond();
            } catch (Exception ignored) {
                ts = java.time.Instant.now().plus(java.time.Duration.ofDays(7)).getEpochSecond();
            }

            net.dv8tion.jda.api.EmbedBuilder eb = new net.dv8tion.jda.api.EmbedBuilder()
                .setTitle("\uD83C\uDF8A Congratulations! \u2014 Voucher Awarded")
                .setDescription("You have successfully secured an elite reward from **Highcore Agency**.\n\n" +
                        "● **Reward:** `" + prizeDetails + "`\n" +
                        "● **Voucher Code:** `" + code + "`\n" +
                        "● **Expiry:** <t:" + ts + ":D> (<t:" + ts + ":R>)")
                .setColor(com.highcore.bot.utils.EmbedUtil.ACCENT)
                .setFooter("Highcore Agency \u30FB Authorized Deployment", null);
            
            if (img != null) {
                eb.setImage("attachment://voucher.png");
                pc.sendFiles(net.dv8tion.jda.api.utils.FileUpload.fromData(img, "voucher.png"))
                  .setEmbeds(eb.build())
                  .queue(null, err -> log.error("Failed to deliver voucher DM to {}: {}", user.getName(), err.getMessage()));
            } else {
                pc.sendMessageEmbeds(eb.build()).queue(null, err -> log.error("Failed to deliver voucher DM (fallback) to {}: {}", user.getName(), err.getMessage()));
            }
        }, err -> {
            log.warn("Cannot open DM for {}: {}", user.getName(), err.getMessage());
        });
    }
}
