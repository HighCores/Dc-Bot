package com.highcore.bot.services;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;

public class ReceiptService {

    private static final Color ACCENT_BLUE = new Color(91, 110, 245);
    private static final Color TEXT_WHITE = new Color(240, 240, 245);
    private static final Color TEXT_GRAY = new Color(160, 160, 175);

    public static byte[] generateReceipt(JsonObject data, String ticketId) throws IOException {
        int width = 700;
        int height = 700;
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        // NEBULA DARK GRADIENT BACKGROUND
        GradientPaint bgGradient = new GradientPaint(0, 0, new Color(12, 12, 14), 0, height, new Color(20, 20, 24));
        g.setPaint(bgGradient);
        g.fillRect(0, 0, width, height);

        g.setColor(new Color(91, 110, 245, 100));
        g.setStroke(new BasicStroke(2));
        g.drawRoundRect(15, 15, width - 30, height - 30, 25, 25);

        // NEON TOP ACCENT
        g.setColor(ACCENT_BLUE);
        g.fillRoundRect(width/2 - 50, 0, 100, 8, 4, 4);

        // HEADER SECTION
        g.setFont(new Font("Dialog", Font.BOLD, 32));
        g.setColor(TEXT_WHITE);
        g.drawString("INVOICE", 45, 75);

        g.setFont(new Font("Dialog", Font.PLAIN, 15));
        g.setColor(TEXT_GRAY);
        g.drawString("AGENCY REGISTRY: " + ticketId, 45, 105);
        g.drawString("ISSUED: " + java.time.LocalDate.now().toString(), 45, 125);

        // DECORATIVE NODE MARKER
        g.setColor(new Color(255, 255, 255, 10));
        g.fillOval(width - 150, -50, 200, 200);

        // DIVIDER
        g.setColor(new Color(50, 50, 60));
        g.drawLine(45, 155, width - 45, 155);

        // TABLE HEADERS
        g.setFont(new Font("Dialog", Font.BOLD, 13));
        g.setColor(ACCENT_BLUE);
        g.drawString("SERVICE DESCRIPTION", 45, 185);
        g.drawString("PRICE", width - 120, 185);

        // ITEMS LISTING
        int y = 220;
        JsonArray srvs = data.getAsJsonArray("services");
        JsonArray adds = data.getAsJsonArray("addons");

        g.setFont(new Font("Dialog", Font.PLAIN, 15));
        g.setColor(TEXT_WHITE);

        if (srvs != null) {
            for (var e : srvs) {
                renderItem(g, e.getAsString(), y, width, true);
                y += 35;
            }
        }
        if (adds != null) {
            g.setColor(new Color(40, 40, 50));
            g.drawLine(45, y - 10, width - 45, y - 10);
            y += 20;
            for (var e : adds) {
                renderItem(g, e.getAsString(), y, width, false);
                y += 35;
            }
        }

        // FINANCIAL TOTAL BLOCK
        int blockY = height - 160;
        g.setColor(new Color(25, 25, 30));
        g.fillRoundRect(45, blockY, width - 90, 90, 20, 20);
        g.setColor(new Color(91, 110, 245, 40));
        g.drawRoundRect(45, blockY, width - 90, 90, 20, 20);

        g.setFont(new Font("Dialog", Font.BOLD, 15));
        g.setColor(TEXT_GRAY);
        g.drawString("TOTAL PRICE", 65, blockY + 35);

        g.setFont(new Font("Dialog", Font.BOLD, 38));
        g.setColor(ACCENT_BLUE);
        String totalStr = "$" + data.get("total").getAsInt();
        int totalWidth = g.getFontMetrics().stringWidth(totalStr);
        g.drawString(totalStr, width - 75 - totalWidth, blockY + 60);

        // VERIFIED BADGE
        g.setFont(new Font("Dialog", Font.BOLD, 12));
        g.setColor(new Color(0, 208, 148));
        g.drawString("\u2705 SECURE_INFRASTRUCTURE_VERIFIED", 45, height - 40);

        g.setFont(new Font("Dialog", Font.ITALIC, 11));
        g.setColor(TEXT_GRAY);
        g.drawString("Data synchronized with Highcore Financial Control Hub", 45, height - 23);

        g.dispose();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        return baos.toByteArray();
    }

    private static void renderItem(Graphics2D g, String id, int y, int width, boolean isMain) {
        String name  = OrderService.ITEM_NAMES.get(id);
        double[] priceArr = OrderService.ITEM_PRICES.get(id);

        if (name != null) {
            g.setColor(isMain ? TEXT_WHITE : TEXT_GRAY);
            g.drawString((isMain ? "\u25B8 " : "\u25AB ") + name, 45, y);

            g.setFont(new Font("Monospaced", Font.BOLD, 15));
            String p = (priceArr == null || priceArr[0] == 0) ? "Quote" : "$" + (int) priceArr[0];
            int pW = g.getFontMetrics().stringWidth(p);
            g.drawString(p, width - 65 - pW, y);
            g.setFont(new Font("Dialog", Font.PLAIN, 15));
        }
    }
}
