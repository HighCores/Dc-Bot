package com.highcore.bot.services;

import com.highcore.bot.utils.EmbedUtil;
import net.dv8tion.jda.api.entities.User;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Random;

public class VoucherService {

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
        try {
            java.net.URLConnection conn = new java.net.URL(BACKGROUND_URL).openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            conn.setConnectTimeout(8000);
            BufferedImage base = ImageIO.read(conn.getInputStream());
            if (base == null) throw new Exception("Failed to load background image");

            int w = base.getWidth();
            int h = base.getHeight();
            BufferedImage combined = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = combined.createGraphics();

            // Quality
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            // Draw background
            g.drawImage(base, 0, 0, null);

            // Draw Code
            // User requested "Source Code Pro". Fallback to Monospaced if not found.
            Font font = new Font("Source Code Pro", Font.BOLD, 60);
            // Check if font exists, if not use Monospaced
            if (font.getFamily().equals("Dialog") || font.getFamily().equals("SansSerif")) {
                font = new Font("Monospaced", Font.BOLD, 60);
            }
            
            g.setFont(font);
            g.setColor(Color.WHITE); // User requested white

            FontMetrics fm = g.getFontMetrics();
            int textW = fm.stringWidth(code);
            int textH = fm.getAscent();

            // Golden Rectangle Coordinates (Natural Pixels from inspection)
            // X: 974, Y: 302, Width: 612, Height: 120
            int rectX = 974;
            int rectY = 302;
            int rectW = 612;
            int rectH = 120;

            int x = rectX + (rectW - textW) / 2;
            int y = rectY + (rectH + textH) / 2 - 5;

            g.drawString(code, x, y);

            g.dispose();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(combined, "jpg", baos);
            return baos.toByteArray();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void issueVoucher(User user, int value, String type, String expiresAt, String prizeDetails) {
        boolean isPercent = type.equalsIgnoreCase("PERCENT");
        String code = generateRandomCode(value, isPercent);
        com.highcore.bot.database.SupabaseClient.createVoucher(user.getId(), code, value, type, expiresAt);
        
        byte[] img = drawVoucher(code);
        if (img == null) return;

        user.openPrivateChannel().queue(pc -> {
            long ts = 0;
            try {
                ts = java.time.Instant.parse(expiresAt).getEpochSecond();
            } catch (Exception ignored) {}

            pc.sendMessage("### Congratulations! \uD83C\uDF8A\n" +
                    "You won a reward from **Highcore Agency**.\n" +
                    "> **Reward:** `" + prizeDetails + "`\n" +
                    "> **Your Code:** `" + code + "`\n" +
                    "> **Valid Until:** <t:" + ts + ":D> (<t:" + ts + ":R>)")
              .setFiles(net.dv8tion.jda.api.utils.FileUpload.fromData(img, "voucher.jpg"))
              .queue();
        });
    }
}
