package com.highcore.bot.services;

import com.highcore.bot.utils.EmbedUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class InvoiceService {
    private static final Logger log = LoggerFactory.getLogger(InvoiceService.class);

    private static final Color COL_DARK  = Color.decode("#212121");
    private static final Color COL_GRAY  = new Color(160, 160, 160);
    private static final Color COL_GOLD  = new Color(197, 160, 89);
    private static final Color COL_WHITE = new Color(255, 255, 255);
    private static final Color COL_LIGHT = new Color(220, 220, 220);

    public static byte[] generateInvoice(String invoiceId, String clientName, String projectName, List<OrderItem> items) {
        return generateInvoice(invoiceId, clientName, projectName, items, false, null, clientName, "Service Delivery", "Highcore Member");
    }

    public static byte[] generateInvoice(String invoiceId, String clientName, String projectName, List<OrderItem> items, boolean isPaid, String avatarUrl, String displayName, String category, String contact) {
        try {
            int width = 700;
            int height = 800;
            BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();

            // High-Fidelity Rendering
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            // NEBULA DARK GRADIENT BACKGROUND
            GradientPaint bgGradient = new GradientPaint(0, 0, new Color(12, 12, 14), 0, height, new Color(20, 20, 24));
            g.setPaint(bgGradient);
            g.fillRect(0, 0, width, height);

            // GLASS-FRAME BORDER
            g.setColor(new Color(91, 110, 245, 100));
            g.setStroke(new BasicStroke(2));
            g.drawRoundRect(15, 15, width - 30, height - 30, 25, 25);

            // NEON TOP ACCENT
            Color ACCENT_BLUE = new Color(91, 110, 245);
            g.setColor(ACCENT_BLUE);
            g.fillRoundRect(width/2 - 50, 0, 100, 8, 4, 4);

            // HEADER SECTION
            g.setFont(new Font("Segoe UI", Font.BOLD, 32));
            g.setColor(COL_WHITE);
            g.drawString("INVOICE", 45, 75);
            
            g.setFont(new Font("Segoe UI", Font.PLAIN, 15));
            g.setColor(COL_GRAY);
            g.drawString("AGENCY REGISTRY: " + invoiceId, 45, 105);
            g.drawString("ISSUED: " + java.time.LocalDate.now().toString(), 45, 125);

            // DRAW AVARAR IF PROVIDED
            if (avatarUrl != null && !avatarUrl.isEmpty()) {
                try {
                    java.net.URLConnection conn = new URL(avatarUrl).openConnection();
                    conn.setRequestProperty("User-Agent", "Mozilla/5.0");
                    try (InputStream is = conn.getInputStream()) {
                        BufferedImage avatar = ImageIO.read(is);
                        if (avatar != null) {
                            int r = 60;
                            int x = width - 105;
                            int y = 45;
                            Shape oldClip = g.getClip();
                            g.setClip(new Ellipse2D.Double(x, y, r, r));
                            g.drawImage(avatar, x, y, r, r, null);
                            g.setClip(oldClip);
                            g.setColor(ACCENT_BLUE);
                            g.setStroke(new BasicStroke(2));
                            g.drawOval(x, y, r, r);
                        }
                    }
                } catch (Exception ignored) {}
            }

            // DIVIDER
            g.setColor(new Color(50, 50, 60));
            g.drawLine(45, 155, width - 45, 155);

            // TABLE HEADERS
            g.setFont(new Font("Segoe UI", Font.BOLD, 13));
            g.setColor(ACCENT_BLUE);
            g.drawString("SERVICE DESCRIPTION", 45, 185);
            g.drawString("PRICE", width - 120, 185);

            // ITEMS LISTING
            g.setFont(new Font("Segoe UI", Font.PLAIN, 15));
            g.setColor(COL_WHITE);
            int y = 220;
            double total = 0;
            for (OrderItem item : items) {
                if (y > height - 200) break; // Prevent overflow
                g.drawString("\u25B8 " + truncate(item.name, 45), 45, y);
                String p = "$" + fmt(item.price);
                int pW = g.getFontMetrics().stringWidth(p);
                g.drawString(p, width - 65 - pW, y);
                total += item.price;
                y += 35;
            }

            // FINANCIAL TOTAL BLOCK
            int blockY = height - 160;
            g.setColor(new Color(25, 25, 30));
            g.fillRoundRect(45, blockY, width - 90, 90, 20, 20);
            g.setColor(new Color(91, 110, 245, 40));
            g.drawRoundRect(45, blockY, width - 90, 90, 20, 20);

            g.setFont(new Font("Segoe UI", Font.BOLD, 15));
            g.setColor(COL_GRAY);
            g.drawString("TOTAL PRICE", 65, blockY + 35);

            g.setFont(new Font("Segoe UI", Font.BOLD, 38));
            g.setColor(ACCENT_BLUE);
            String totalStr = "$" + fmt(total);
            int totalWidth = g.getFontMetrics().stringWidth(totalStr);
            g.drawString(totalStr, width - 75 - totalWidth, blockY + 60);

            // STATUS BADGE
            g.setFont(new Font("Segoe UI", Font.BOLD, 12));
            if (isPaid) {
                g.setColor(new Color(0, 208, 148));
                g.drawString("\u2705 PAYMENT_VERIFIED", 45, height - 40);
            } else {
                g.setColor(new Color(244, 63, 94));
                g.drawString("\u26A0\uFE0F AWAITING_PAYMENT", 45, height - 40);
            }

            g.setFont(new Font("Segoe UI", Font.ITALIC, 11));
            g.setColor(COL_GRAY);
            g.drawString("Data synchronized with Highcore Financial Control Hub", 45, height - 23);

            g.dispose();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(img, "png", baos);
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("CRITICAL: Invoice generation failure: {}", e.getMessage());
            return null;
        }
    }

    private static String fmt(double d) {
        if (d == (long) d) return String.format("%d", (long) d);
        return String.format("%.2f", d);
    }

    private static String truncate(String s, int n) {
        if (s == null) return "";
        return s.length() > n ? s.substring(0, n - 3) + "..." : s;
    }

    private static Font boldFont(int w, float sizePct) {
        return new Font("SansSerif", Font.BOLD, (int)(w * sizePct));
    }

    private static Font plainFont(int w, float sizePct) {
        return new Font("SansSerif", Font.PLAIN, (int)(w * sizePct));
    }

    public static class OrderItem {
        public String name;
        public double price;
        public OrderItem(String name, double price) { this.name = name; this.price = price; }
    }
}
