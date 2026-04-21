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
import java.util.Random;
import java.util.List;
import java.util.ArrayList;

public class VoucherService {
    private static final Logger log = LoggerFactory.getLogger(VoucherService.class);

    private static final String BACKGROUND_URL = "https://i.imgur.com/OXI22JW.png";

    public static String generateRandomCode(int value, boolean isPercent) {
        Random r = new Random();
        char c1 = (char) ('A' + r.nextInt(26));
        char c2 = (char) ('A' + r.nextInt(26));
        int n1 = r.nextInt(10);
        int n2 = r.nextInt(10);
        
        return "HC" + value + "-" + c1 + c2 + n1 + n2;
    }

    public static byte[] drawVoucher(String code) {
        log.info("Attempting to generate voucher image for code: {}", code);
        try {
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) new java.net.URL(BACKGROUND_URL).openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.8 Safari/537.36");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);
            
            BufferedImage base;
            try (InputStream is = conn.getInputStream()) {
                byte[] bytes = is.readAllBytes();
                if (bytes.length < 500) {
                     log.error("Possible error response from Imgur: {} bytes", bytes.length);
                     log.debug("Imgur Response: {}", new String(bytes));
                }
                base = ImageIO.read(new java.io.ByteArrayInputStream(bytes));
            }
            
            if (base == null) {
                log.error("ImageIO failed to decode background from URL: {}.", BACKGROUND_URL);
                return null;
            }

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
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Voucher drawing failed: {}", e.getMessage());
            return null;
        }
    }

    public static void issueVoucher(User user, int value, String type, String expiresAt, String prizeDetails, byte[] winnerImg) {
        boolean isPercent = type.equalsIgnoreCase("PERCENT");
        String code = generateRandomCode(value, isPercent);
        
        log.info("Issuing voucher for {}: {}", user.getName(), code);
        com.highcore.bot.database.SupabaseClient.createVoucher(user.getId(), code, value, type, expiresAt);
        
        byte[] voucherImg = drawVoucher(code);

        user.openPrivateChannel().queue(pc -> {
            long ts = 0;
            try {
                ts = java.time.Instant.parse(expiresAt).getEpochSecond();
            } catch (Exception ignored) {
                ts = java.time.Instant.now().plus(java.time.Duration.ofDays(7)).getEpochSecond();
            }

            String body = "### Congratulations! \uD83C\uDF8A\n" +
                    "You have successfully secured an elite reward from **Highcore Agency**.\n\n" +
                    "● **Reward:** `" + prizeDetails + "`\n" +
                    "● **Voucher Code:** `" + code + "`\n" +
                    "● **Expiry:** <t:" + ts + ":D> (<t:" + ts + ":R>)";

            // If we have both, we use winnerImg as the primary banner in the container
            String bannerRef = (winnerImg != null) ? "attachment://winner.png" : (voucherImg != null ? "attachment://voucher.png" : null);
            net.dv8tion.jda.api.components.container.Container container = EmbedUtil.containerBranded("CONGRATULATIONS", "Voucher Awarded", body, bannerRef);
            
            var action = pc.sendMessageComponents(container).useComponentsV2(true);
            
            List<net.dv8tion.jda.api.utils.FileUpload> files = new ArrayList<>();
            if (winnerImg != null) files.add(net.dv8tion.jda.api.utils.FileUpload.fromData(winnerImg, "winner.png"));
            if (voucherImg != null) files.add(net.dv8tion.jda.api.utils.FileUpload.fromData(voucherImg, "voucher.png"));
            
            if (!files.isEmpty()) {
                action.setFiles(files).queue(null, err -> log.error("Failed to DM voucher: {}", err.getMessage()));
            } else {
                action.queue(null, err -> log.error("Failed to DM voucher fallback: {}", err.getMessage()));
            }
        }, err -> log.warn("Closed DMs for {}: {}", user.getName(), err.getMessage()));
    }
}
