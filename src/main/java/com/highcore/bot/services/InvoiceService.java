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

    private static final Color COL_GOLD  = Color.decode("#C5A059");
    private static final Color COL_WHITE = Color.decode("#FFFFFF");
    private static final Color COL_CREAM = Color.decode("#EAEAEA");
    private static final String BANNER_NEW = "https://cdn.discordapp.com/attachments/1488900668042510568/1495893318217764884/Invoice.jpg?ex=69e7e6b7&is=69e69537&hm=83960d6e0495f3dc32222dee7309429ed39097fa09b0008683b85e42a58cccd3&";

    public static byte[] generateInvoice(String invoiceId, String clientName, String projectName, List<OrderItem> items) {
        return generateInvoice(invoiceId, clientName, projectName, items, false, null, clientName, "Service Delivery", "Highcore Member", 0.0, null);
    }

    public static byte[] generateInvoice(String invoiceId, String clientName, String projectName, List<OrderItem> items, boolean isPaid, String avatarUrl, String displayName, String category, String contact, double discount, String phone) {
        try {
            BufferedImage template = null;
            String templateUrl = BANNER_NEW;
            
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

            String dateStr = new SimpleDateFormat("yyyy/MM/dd").format(new Date());

            // 1. TOP RIGHT: Invoice ID (Smaller and centered under INVOICE)
            g.setFont(new Font("Segoe UI", Font.BOLD, (int)(W * 0.02)));
            g.setColor(COL_GOLD);
            String idTag = "#" + invoiceId;
            g.drawString(idTag, (int)(W * 0.706 - g.getFontMetrics().stringWidth(idTag)/2), (int)(H * 0.155));

            // 2. AVATAR & NAME (Top Right Profile)
            if (avatarUrl != null && !avatarUrl.isEmpty()) {
                try {
                    java.net.URLConnection conn = new URL(avatarUrl).openConnection();
                    conn.setRequestProperty("User-Agent", "Mozilla/5.0");
                    try (InputStream is = conn.getInputStream()) {
                        BufferedImage avatar = ImageIO.read(is);
                        if (avatar != null) {
                            int r = (int)(W * 0.05); 
                            int x = (int)(W * 0.686); 
                            int y = (int)(H * 0.188); 
                            Shape oldClip = g.getClip();
                            g.setClip(new Ellipse2D.Double(x, y, r, r));
                            g.drawImage(avatar, x, y, r, r, null);
                            g.setClip(oldClip);
                        }
                    }
                } catch (Exception ignored) {}
            }
            
            // Name inside the small capsule below avatar
            g.setFont(new Font("Segoe UI", Font.BOLD, (int)(W * 0.015)));
            g.setColor(COL_WHITE);
            String uName = truncate(displayName != null ? displayName : clientName, 15);
            g.drawString(uName, (int)(W * 0.711 - g.getFontMetrics().stringWidth(uName)/2), (int)(H * 0.260));

            // 3. STATUS BAR (Center Right)
            g.setFont(new Font("Segoe UI", Font.BOLD, (int)(W * 0.018)));
            g.setColor(isPaid ? Color.GREEN : COL_GOLD);
            String statusText = isPaid ? "AUTHORIZED" : "PENDING";
            g.drawString(statusText, (int)(W * 0.710 - g.getFontMetrics().stringWidth(statusText)/2), (int)(H * 0.288));

            // 4. CLIENT DETAILS BOX (Bottom Right Box)
            g.setFont(new Font("Segoe UI", Font.BOLD, (int)(W * 0.018)));
            g.setColor(COL_WHITE);
            int clientBoxX = (int)(W * 0.675);
            g.drawString(truncate(displayName != null ? displayName : clientName, 25), clientBoxX, (int)(H * 0.375));
            g.setFont(new Font("Segoe UI", Font.PLAIN, (int)(W * 0.014)));
            String contactInfo = (contact != null ? contact : "") + (phone != null && !phone.isEmpty() ? " (" + phone + ")" : "");
            g.drawString(truncate(contactInfo.isEmpty() ? "No Contact Info" : contactInfo, 40), clientBoxX, (int)(H * 0.395));
            g.drawString(clientName, clientBoxX, (int)(H * 0.415)); // Discord ID

            // 5. PROJECT INFO (Left Side) - Adjusted to avoid overlaps
            g.setFont(new Font("Segoe UI", Font.BOLD, (int)(W * 0.018)));
            g.setColor(COL_WHITE);
            g.drawString(truncate(projectName, 30), (int)(W * 0.29), (int)(H * 0.245));
            
            g.setFont(new Font("Segoe UI", Font.PLAIN, (int)(W * 0.016)));
            g.setColor(COL_CREAM);
            g.drawString(category != null ? category : "Software Services", (int)(W * 0.26), (int)(H * 0.274));

            // 6. ADD-ONS LIST (Large Box Below)
            g.setFont(new Font("Segoe UI", Font.PLAIN, (int)(H * 0.017)));
            g.setColor(COL_CREAM);
            int addOnStartY = (int)(H * 0.380);
            for (int i = 0; i < Math.min(items.size(), 6); i++) {
                g.drawString("\u2022 " + truncate(items.get(i).name, 45), (int)(W * 0.14), addOnStartY + (i * (int)(H * 0.026)));
            }

            // 7. MAIN SERVICES TABLE (Centered vertically in rows)
            int col_ServicesX = (int)(W * 0.14);
            int col_PriceX    = (int)(W * 0.672);
            int col_QtyX      = (int)(W * 0.758);
            int col_TotalX    = (int)(W * 0.84);

            double subtotalVal = 0;
            int tableStartY = (int)(H * 0.655);
            int tableRowGap = (int)(H * 0.0454); 
            
            for (int i = 0; i < Math.min(items.size(), 5); i++) {
                OrderItem item = items.get(i);
                int y = tableStartY + (i * tableRowGap);
                
                g.setFont(new Font("Segoe UI", Font.BOLD, (int)(W * 0.018)));
                g.setColor(COL_WHITE);
                g.drawString(truncate(item.name, 40), col_ServicesX, y);
                
                g.setFont(new Font("Segoe UI", Font.PLAIN, (int)(W * 0.018)));
                String pStr = "$" + fmt(item.price);
                g.drawString(pStr, col_PriceX - g.getFontMetrics().stringWidth(pStr)/2, y);
                g.drawString("1", col_QtyX - g.getFontMetrics().stringWidth("1")/2, y);
                g.drawString(pStr, col_TotalX - g.getFontMetrics().stringWidth(pStr)/2, y);
                
                subtotalVal += item.price;
            }

            // 8. FOOTER FINANCIALS (Precision Alignment with Baked-in Labels)
            double taxVal = (subtotalVal - discount) * 0.05;
            if (taxVal < 0) taxVal = 0;
            double finalTotal = (subtotalVal - discount) + taxVal;

            g.setFont(new Font("Segoe UI", Font.BOLD, (int)(W * 0.019)));
            g.setColor(COL_GOLD);
            g.drawString("$" + fmt(subtotalVal), (int)(W * 0.28), (int)(H * 0.854)); 
            g.drawString("-$" + fmt(discount),    (int)(W * 0.28), (int)(H * 0.884)); 

            g.drawString("$" + fmt(taxVal),      (int)(W * 0.78), (int)(H * 0.841)); 
            g.setFont(new Font("Segoe UI", Font.BOLD, (int)(W * 0.024)));
            g.drawString("$" + fmt(finalTotal),  (int)(W * 0.78), (int)(H * 0.881));

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
