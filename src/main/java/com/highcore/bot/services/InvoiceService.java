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
                log.info("Attempting to load highcore template from: {}", templateUrl);
                java.net.URLConnection conn = new URL(templateUrl).openConnection();
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                try (InputStream is = conn.getInputStream()) {
                    template = ImageIO.read(is);
                }
            } catch (Exception e) {
                log.error("CRITICAL: Failed to load user template ({}): {}", templateUrl, e.getMessage());
            }

            if (template == null) return null;

            int W = template.getWidth();
            int H = template.getHeight();

            BufferedImage image = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = image.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(template, 0, 0, null);

            String dateStr = new SimpleDateFormat("yyyy/MM/dd HH:mm").format(new Date());

            // 1. TOP RIGHT: Invoice ID
            g.setFont(new Font("Segoe UI", Font.BOLD, (int)(W * 0.04)));
            g.setColor(COL_GOLD);
            g.drawString(invoiceId, (int)(W * 0.73), (int)(H * 0.082));

            // 2. TOP GRAY BOX: AVATAR & DISPLAY NAME
            if (avatarUrl != null && !avatarUrl.isEmpty()) {
                try {
                    java.net.URLConnection conn = new URL(avatarUrl).openConnection();
                    conn.setRequestProperty("User-Agent", "Mozilla/5.0");
                    try (InputStream is = conn.getInputStream()) {
                        BufferedImage avatar = ImageIO.read(is);
                        if (avatar != null) {
                            int r = (int)(W * 0.07); 
                            int x = (int)(W * 0.49); 
                            int y = (int)(H * 0.198); 
                            Shape oldClip = g.getClip();
                            g.setClip(new Ellipse2D.Double(x, y, r, r));
                            g.drawImage(avatar, x, y, r, r, null);
                            g.setClip(oldClip);
                        }
                    }
                } catch (Exception ignored) {}
            }
            
            g.setFont(new Font("Segoe UI", Font.BOLD, (int)(W * 0.025)));
            g.setColor(new Color(60, 60, 60));
            String nameToDraw = displayName != null ? displayName : clientName;
            g.drawString(truncate(nameToDraw, 18), (int)(W * 0.58), (int)(H * 0.23));

            // 3. PROJECT DETAILS (LEFT)
            g.setFont(new Font("Segoe UI", Font.BOLD, (int)(W * 0.024)));
            g.setColor(COL_DARK);
            g.drawString(truncate(projectName, 22), (int)(W * 0.26), (int)(H * 0.245)); 
            
            g.setFont(new Font("Segoe UI", Font.PLAIN, (int)(W * 0.02)));
            g.setColor(new Color(100, 100, 100));
            g.drawString(category != null ? category : "Agency Projects", (int)(W * 0.30), (int)(H * 0.273)); 

            // More Details block
            g.setFont(new Font("Segoe UI", Font.PLAIN, (int)(W * 0.02)));
            g.setColor(COL_DARK);
            g.drawString(truncate(projectName, 45), (int)(W * 0.17), (int)(H * 0.355));

            // 4. ADD-ONS GRID (FULL INTEGRITY)
            g.setFont(new Font("Segoe UI", Font.PLAIN, (int)(W * 0.018)));
            g.setColor(new Color(80, 80, 80));
            double[] colXs = {0.16, 0.40, 0.65}; 
            int startY = (int)(H * 0.42);
            int rowGap = (int)(H * 0.03);
            for (int i = 0; i < Math.min(items.size(), 6); i++) {
                int col = i % 3;
                int row = i / 3;
                int x = (int)(W * colXs[col]);
                int y = startY + (row * rowGap);
                g.drawString("- " + truncate(items.get(i).name, 20), x, y);
            }

            // 5. STATUS / CONTACT / DATE (MIDDLE)
            g.setFont(new Font("Segoe UI", Font.BOLD, (int)(W * 0.018)));
            g.setColor(new Color(50, 50, 50));
            int middleX = (int)(W * 0.76); 
            g.drawString(isPaid ? "PAID" : "UNPAID", middleX, (int)(H * 0.283)); 
            g.drawString(truncate(contact != null ? contact : "Discord Client", 18), middleX, (int)(H * 0.323)); 
            g.drawString(new SimpleDateFormat("MM.dd.yyyy").format(new Date()), middleX, (int)(H * 0.363)); 

            // 6. MAIN TABLE ROWS (START AFTER GOLD HEADER)
            int col_ServicesX = (int)(W * 0.16);
            int col_PriceX    = (int)(W * 0.54);
            int col_QtyX      = (int)(W * 0.74);
            int col_TotalX    = (int)(W * 0.88);

            double subtotalVal = 0;
            int tableStartY = (int)(H * 0.685);
            int tableRowGap = (int)(H * 0.0632); 
            
            for (int i = 0; i < Math.min(items.size(), 4); i++) {
                OrderItem item = items.get(i);
                int y = tableStartY + (i * tableRowGap);
                
                g.setFont(new Font("Segoe UI", Font.BOLD, (int)(W * 0.022)));
                g.setColor(new Color(40, 40, 40));
                g.drawString(truncate(item.name, 35), col_ServicesX, y);
                
                g.setFont(new Font("Segoe UI", Font.PLAIN, (int)(W * 0.022)));
                g.setColor(new Color(80, 80, 80));
                String pStr = "$" + fmt(item.price);
                g.drawString(pStr, col_PriceX - g.getFontMetrics().stringWidth(pStr)/2, y);
                g.drawString("1", col_QtyX - g.getFontMetrics().stringWidth("1")/2, y);
                g.drawString(pStr, col_TotalX - g.getFontMetrics().stringWidth(pStr), y);
                
                subtotalVal += item.price;
            }

            // 7. FOOTER FINANTE
            double taxVal = subtotalVal * 0.05;
            double finalTotal = subtotalVal + taxVal;
            int footerValuesX = (int)(W * 0.32); 

            g.setFont(new Font("Segoe UI", Font.BOLD, (int)(W * 0.022)));
            g.setColor(COL_DARK);
            g.drawString("$" + fmt(subtotalVal), footerValuesX, (int)(H * 0.835)); 
            g.drawString("$0",               footerValuesX, (int)(H * 0.858)); 
            g.drawString("$" + fmt(taxVal),      footerValuesX, (int)(H * 0.882)); 

            g.setFont(new Font("Segoe UI", Font.BOLD, (int)(W * 0.035)));
            g.setColor(Color.BLACK);
            g.drawString("$" + fmt(finalTotal), (int)(W * 0.32), (int)(H * 0.925));

            g.dispose();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("CRITICAL: User template injection failure: {}", e.getMessage());
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
