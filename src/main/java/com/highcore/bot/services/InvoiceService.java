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
            BufferedImage template = null;
            String templateUrl = isPaid ? EmbedUtil.BANNER_INVOICE_PAID : EmbedUtil.BANNER_INVOICE;
            
            try {
                log.info("Attempting to load template from: {}", templateUrl);
                java.net.URLConnection conn = new URL(templateUrl).openConnection();
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                try (InputStream is = conn.getInputStream()) {
                    template = ImageIO.read(is);
                }
            } catch (Exception e) {
                log.error("CRITICAL: Failed to load invoice template ({}): {}", templateUrl, e.getMessage());
            }

            if (template == null) return null;

            int W = template.getWidth();
            int H = template.getHeight();

            BufferedImage image = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = image.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING,          RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(template, 0, 0, null);

            String dateStr = new SimpleDateFormat("yyyy/MM/dd").format(new Date());

            // 1. Top Right Box: Invoice ID & Date
            g.setFont(boldFont(W, 0.028f));
            g.setColor(COL_GOLD);
            g.drawString(invoiceId, (int)(W * 0.742), (int)(H * 0.076));

            // 2. Avatar & Name
            if (avatarUrl != null && !avatarUrl.isEmpty()) {
                try {
                    java.net.URLConnection conn = new URL(avatarUrl).openConnection();
                    conn.setRequestProperty("User-Agent", "Mozilla/5.0");
                    try (InputStream is = conn.getInputStream()) {
                        BufferedImage avatar = ImageIO.read(is);
                        if (avatar != null) {
                            int r = (int)(W * 0.0532); 
                            int x = (int)(W * 0.4901); 
                            int y = (int)(H * 0.1794); 
                            Shape oldClip = g.getClip();
                            g.setClip(new Ellipse2D.Double(x, y, r, r));
                            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                            g.drawImage(avatar, x, y, r, r, null);
                            g.setClip(oldClip);
                        }
                    }
                } catch (Exception e) {
                    log.warn("Failed to draw avatar: {}", e.getMessage());
                }
            }
            
            g.setFont(boldFont(W, 0.019f));
            g.setColor(Color.decode("#5b6471"));
            String nameToDraw = displayName != null ? displayName : clientName;
            g.drawString(truncate(nameToDraw, 20), (int)(W * 0.5656), (int)(H * 0.2078));

            // 3. PROJECT DETAILS
            g.setFont(boldFont(W, 0.016f));
            g.setColor(COL_DARK);
            g.drawString(truncate(projectName, 22).toUpperCase(), (int)(W * 0.300), (int)(H * 0.222)); 
            
            g.setFont(plainFont(W, 0.016f));
            g.setColor(COL_GRAY);
            g.drawString(category != null ? category : "Premium Delivery", (int)(W * 0.300), (int)(H * 0.246)); 

            // More Details text (bottom left of header)
            g.setFont(plainFont(W, 0.014f));
            g.setColor(COL_DARK);
            g.drawString(truncate(projectName, 40), (int)(W * 0.11), (int)(H * 0.320));

            // 4. Grid of all items (Full Integrity)
            g.setFont(plainFont(W, 0.011f));
            g.setColor(COL_GRAY);
            double[] colXs = {0.11, 0.30, 0.49, 0.68}; 
            int startY = (int)(H * 0.408);
            int rowGap = (int)(H * 0.025);
            for (int i = 0; i < Math.min(items.size(), 8); i++) {
                int col = i % 4;
                int row = i / 4;
                int x = (int)(W * colXs[col]);
                int y = startY + (row * rowGap);
                g.drawString(truncate("- " + items.get(i).name, 30), x, y);
            }

            // 5. Identity Column
            g.setFont(plainFont(W, 0.015f));
            g.setColor(COL_GRAY);
            int columnX = (int)(W * 0.680); 
            g.drawString(isPaid ? "PAID" : "UNPAID", columnX, (int)(H * 0.264)); 
            g.drawString(truncate(contact != null ? contact : "Discord Member", 20), columnX, (int)(H * 0.298)); 
            g.drawString(dateStr,                   columnX, (int)(H * 0.332)); 

            // 6. Main Table Rows
            int col_ServicesX = (int)(W * 0.1467);
            int col_PriceX    = (int)(W * 0.5105);
            int col_QtyX      = (int)(W * 0.6556);
            int col_TotalX    = (int)(W * 0.7963);

            double subtotalVal = 0;
            double[] rowYs = {0.5712, 0.6237, 0.6718, 0.7243}; 
            int tableMax = Math.min(items.size(), 4);
            for (int i = 0; i < tableMax; i++) {
                OrderItem item = items.get(i);
                int y = (int)(H * rowYs[i]);
                
                g.setFont(boldFont(W, 0.017f));
                g.setColor(COL_DARK);
                g.drawString(truncate(item.name, 35), col_ServicesX, y);
                
                g.setFont(plainFont(W, 0.016f));
                g.setColor(COL_GRAY);
                g.drawString("$" + fmt(item.price), col_PriceX, y);
                g.drawString("1", col_QtyX, y);
                g.drawString("$" + fmt(item.price), col_TotalX, y);
                
                subtotalVal += item.price;
            }

            // 7. Footer Totals
            double taxVal = subtotalVal * 0.05;
            double finalTotal = subtotalVal + taxVal;
            int valuesX = (int)(W * 0.28); 

            g.setFont(boldFont(W, 0.019f));
            g.setColor(COL_DARK);
            g.drawString("$" + fmt(subtotalVal), valuesX, (int)(H * 0.842)); 
            g.drawString("$0",               valuesX, (int)(H * 0.867)); 
            g.drawString("$" + fmt(taxVal),      valuesX, (int)(H * 0.892)); 

            g.setFont(boldFont(W, 0.024f));
            g.setColor(COL_DARK);
            g.drawString("$" + fmt(finalTotal), (int)(W * 0.28), (int)(H * 0.932));

            g.dispose();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
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
