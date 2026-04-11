package com.highcore.bot.services;

import com.highcore.bot.utils.EmbedUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Random;

public class InvoiceService {
    private static final Logger log = LoggerFactory.getLogger(InvoiceService.class);

    // ── Palette ───────────────────────────────────────────────────────────────
    private static final Color COL_WHITE  = new Color(255, 255, 255);
    private static final Color COL_LIGHT  = new Color(220, 220, 220);
    private static final Color COL_GOLD   = new Color(197, 160, 89);
    private static final Color COL_GOLD_B = new Color(215, 180, 100);
    private static final Color COL_GRAY   = new Color(160, 160, 160);

    public static byte[] generateInvoice(String clientName, String projectName, List<OrderItem> items) {
        try {
            // ── Load template ─────────────────────────────────────────────────
            BufferedImage invoice;
            try (InputStream is = InvoiceService.class.getResourceAsStream("/invoice.png")) {
                if (is != null) {
                    invoice = ImageIO.read(is);
                } else {
                    invoice = ImageIO.read(new URL(EmbedUtil.BANNER_INVOICE));
                }
            }

            int W = invoice.getWidth();
            int H = invoice.getHeight();

            Graphics2D g = invoice.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,  RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
            g.setRenderingHint(RenderingHints.KEY_RENDERING,          RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,  RenderingHints.VALUE_FRACTIONALMETRICS_ON);

            // ─────────────────────────────────────────────────────────────────
            // Layout is based on the invoice template proportions.
            // The template is roughly 1080-wide dark themed invoice card.
            // All positions use percentages of W/H so they adapt to any size.
            // ─────────────────────────────────────────────────────────────────

            int col1X   = (int)(W * 0.07);   // description column start
            int col2X   = (int)(W * 0.58);   // qty column
            int col3X   = (int)(W * 0.73);   // unit price column
            int col4X   = (int)(W * 0.86);   // total column

            // ── Invoice number & date (top-right area) ────────────────────────
            String invoiceId = "HC-" + (1000 + new Random().nextInt(9000));
            String date      = new SimpleDateFormat("dd/MM/yyyy").format(new Date());

            g.setFont(boldFont(W, 0.018f));
            g.setColor(COL_LIGHT);
            // Invoice # label area — roughly 72% from left, 9% from top
            drawRight(g, invoiceId, (int)(W * 0.93), (int)(H * 0.092));
            drawRight(g, date,      (int)(W * 0.93), (int)(H * 0.124));

            // ── Bill To / Project (two columns ~28% and ~54% from left) ───────
            g.setFont(boldFont(W, 0.020f));
            g.setColor(COL_WHITE);
            g.drawString(truncate(clientName.toUpperCase(), 22),  (int)(W * 0.07), (int)(H * 0.268));
            g.drawString(truncate(projectName.toUpperCase(), 22), (int)(W * 0.54), (int)(H * 0.268));

            // ── Column headers (repeat template) ─────────────────────────────
            // (template already has them as graphics; skip or draw subtle)

            // ── Line items ────────────────────────────────────────────────────
            double startYpct = 0.335;   // first item row ~ 33.5% from top
            double rowPct    = 0.052;   // each row is ~5.2% of height
            double subtotal  = 0;
            int maxRows      = Math.min(items.size(), 8);

            g.setFont(plainFont(W, 0.017f));

            for (int i = 0; i < maxRows; i++) {
                OrderItem item = items.get(i);
                int rowY = (int)(H * (startYpct + i * rowPct));

                // Description
                g.setColor(COL_LIGHT);
                g.drawString(truncate(item.name, 34), col1X, rowY);

                // Qty
                g.setColor(COL_GRAY);
                g.drawString("1", col2X, rowY);

                // Unit price
                String priceStr = item.price == 0 ? "Quote" : "$" + fmt(item.price);
                g.setColor(COL_GOLD);
                drawRight(g, priceStr, col3X + (int)(W * 0.085), rowY);

                // Total
                g.setColor(COL_WHITE);
                drawRight(g, item.price == 0 ? "—" : "$" + fmt(item.price), col4X + (int)(W * 0.07), rowY);

                subtotal += item.price;
            }

            // ── Totals section ────────────────────────────────────────────────
            // Subtotal row at ~84% height
            double subtotalY = 0.840;
            double taxY      = 0.873;
            double totalY    = 0.922;

            int totalsX = (int)(W * 0.935); // right-align to ~93.5%

            g.setFont(boldFont(W, 0.019f));
            g.setColor(COL_GOLD);
            drawRight(g, "$" + fmt(subtotal),              totalsX, (int)(H * subtotalY));

            double tax   = subtotal * 0.05;
            double total = subtotal + tax;

            g.setColor(COL_LIGHT);
            drawRight(g, "$" + fmt(tax),                   totalsX, (int)(H * taxY));

            g.setFont(boldFont(W, 0.024f));
            g.setColor(COL_WHITE);
            drawRight(g, "$" + fmt(total),                 totalsX, (int)(H * totalY));

            g.dispose();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(invoice, "png", baos);
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Failed to generate invoice: {}", e.getMessage());
            return null;
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Draw text right-aligned to the given X. */
    private static void drawRight(Graphics2D g, String text, int rightX, int y) {
        int tw = g.getFontMetrics().stringWidth(text);
        g.drawString(text, rightX - tw, y);
    }

    private static Font boldFont(int imgWidth, float pct) {
        return new Font("SansSerif", Font.BOLD,  Math.max(10, (int)(imgWidth * pct)));
    }

    private static Font plainFont(int imgWidth, float pct) {
        return new Font("SansSerif", Font.PLAIN, Math.max(9,  (int)(imgWidth * pct)));
    }

    private static String fmt(double v) {
        if (v == (long) v) return String.valueOf((long) v);
        return String.format("%.2f", v);
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max - 1) + "\u2026" : s;
    }

    // ── Model ─────────────────────────────────────────────────────────────────
    public static class OrderItem {
        public String name;
        public double price;
        public OrderItem(String name, double price) { this.name = name; this.price = price; }
    }
}
