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
        return generateInvoice(invoiceId, clientName, projectName, items, false, null, clientName, "Service", "Member", 0.0, null);
    }

    public static byte[] generateInvoice(String invoiceId, String clientName, String projectName, List<OrderItem> items, boolean isPaid, String avatarUrl, String displayName, String category, String contact, double discount, String phone) {
        try {
            BufferedImage template = null;
            String templateUrl = BANNER_NEW;
            
            try {
                java.net.URLConnection conn = new URL(templateUrl).openConnection();
                conn.setRequestProperty("User-Agent", "Mozilla/5.0");
                try (InputStream is = conn.getInputStream()) {
                    template = ImageIO.read(is);
                }
            } catch (Exception e) {}

            if (template == null) return null;

            int W = template.getWidth();
            int H = template.getHeight();

            BufferedImage image = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = image.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.drawImage(template, 0, 0, null);

            double sX = W / 1000.0;
            double sY = H / 1000.0;

            // 1. Invoice ID (#)
            g.setFont(new Font("Segoe UI", Font.BOLD, (int)(24 * sX)));
            g.setColor(COL_GOLD);
            // Positioned exactly after the # on the right
            g.drawString(invoiceId, (int)(745 * sX), (int)(148 * sY));

            // 2. Avatar
            if (avatarUrl != null && !avatarUrl.isEmpty()) {
                try {
                    java.net.URLConnection conn = new URL(avatarUrl).openConnection();
                    try (InputStream is = conn.getInputStream()) {
                        BufferedImage avatar = ImageIO.read(is);
                        if (avatar != null) {
                            int r = (int)(50 * sX); 
                            int x = (int)((700 - 25) * sX); 
                            int y = (int)((210 - 25) * sY); 
                            Shape oldClip = g.getClip();
                            g.setClip(new Ellipse2D.Double(x, y, r, r));
                            g.drawImage(avatar, x, y, r, r, null);
                            g.setClip(oldClip);
                        }
                    }
                } catch (Exception ignored) {}
            }
            
            // 2b. Username - Centered in the brown rectangle
            g.setFont(new Font("Segoe UI", Font.BOLD, (int)(16 * sX)));
            g.setColor(COL_WHITE);
            String uName = truncate(displayName != null ? displayName : clientName, 20);
            g.drawString(uName, (int)(775 * sX) - g.getFontMetrics().stringWidth(uName)/2, (int)(237 * sY));

            // 3. Status
            g.setFont(new Font("Segoe UI", Font.BOLD, (int)(18 * sX)));
            g.setColor(isPaid ? Color.GREEN : COL_GOLD);
            String statusText = isPaid ? "AUTHORIZED" : "PENDING";
            g.drawString(statusText, (int)(815 * sX) - g.getFontMetrics().stringWidth(statusText)/2, (int)(284 * sY));

            // 4. Client Details - Restoring contact but keeping description removed per request
            g.setFont(new Font("Segoe UI", Font.BOLD, (int)(18 * sX)));
            g.setColor(COL_WHITE);
            g.drawString(truncate(displayName != null ? displayName : clientName, 25), (int)(675 * sX), (int)(355 * sY));
            
            if (contact != null && !contact.isEmpty()) {
                g.setFont(new Font("Segoe UI", Font.PLAIN, (int)(16 * sX)));
                g.setColor(COL_CREAM);
                g.drawString(truncate(contact, 30), (int)(675 * sX), (int)(380 * sY));
            }

            // 5. Project Info
            g.setFont(new Font("Segoe UI", Font.BOLD, (int)(20 * sX)));
            g.setColor(COL_WHITE);
            g.drawString(truncate(projectName, 25), (int)(300 * sX), (int)(252 * sY));
            
            g.setFont(new Font("Segoe UI", Font.PLAIN, (int)(17 * sX)));
            g.setColor(COL_CREAM);
            String catLabel = category != null ? category : "Software Services";
            if (catLabel.length() > 2) catLabel = catLabel.substring(0,1).toUpperCase() + catLabel.substring(1).toLowerCase();
            g.drawString(catLabel, (int)(280 * sX), (int)(282 * sY));

            // 6. Add-ons (Excluding the main service at index 0)
            g.setFont(new Font("Segoe UI", Font.PLAIN, (int)(18 * sX)));
            g.setColor(COL_CREAM);
            int addOnStartY = (int)(385 * sY);
            for (int i = 1; i < Math.min(items.size(), 7); i++) {
                g.drawString("\u2022 " + truncate(items.get(i).name, 45), (int)(145 * sX), addOnStartY + ((i - 1) * (int)(26 * sY)));
            }

            // 7. Table
            int col_ServicesX = (int)(150 * sX);
            int col_PriceX    = (int)(660 * sX);
            int col_QtyX      = (int)(745 * sX);
            int col_TotalX    = (int)(830 * sX);
            int tableStartY   = (int)(635 * sY); 
            int tableRowGap   = (int)(45 * sY); 

            double subtotalVal = 0;
            int drawnCount = 0;
            for (int i = 0; i < items.size() && drawnCount < 5; i++) {
                OrderItem item = items.get(i);
                if (item.price <= 0) {
                    subtotalVal += item.price; // Keep adding to subtotal even if hidden
                    continue;
                }
                int y = tableStartY + (drawnCount * tableRowGap);
                g.setFont(new Font("Segoe UI", Font.BOLD, (int)(16 * sX)));
                g.setColor(COL_WHITE);
                g.drawString(truncate(item.name, 40), col_ServicesX, y);
                g.setFont(new Font("Segoe UI", Font.PLAIN, (int)(16 * sX)));
                String pStr = "$" + fmt(item.price);
                g.drawString(pStr, col_PriceX - g.getFontMetrics().stringWidth(pStr)/2, y);
                g.drawString("1", col_QtyX - g.getFontMetrics().stringWidth("1")/2, y);
                g.drawString(pStr, col_TotalX - g.getFontMetrics().stringWidth(pStr)/2, y);
                subtotalVal += item.price;
                drawnCount++;
            }

            // 8. Financials
            double taxVal = (subtotalVal - discount) * 0.05;
            double finalTotal = Math.max(0, (subtotalVal - discount) + taxVal);

            g.setFont(new Font("Segoe UI", Font.BOLD, (int)(22 * sX)));
            g.setColor(COL_GOLD);
            g.drawString("$" + fmt(subtotalVal), (int)(245 * sX), (int)(862 * sY)); 
            g.drawString("-$" + fmt(discount),    (int)(245 * sX), (int)(888 * sY)); 

            g.drawString("$" + fmt(taxVal),      (int)(725 * sX), (int)(852 * sY)); 
            g.setFont(new Font("Segoe UI", Font.BOLD, (int)(26 * sX)));
            g.drawString("$" + fmt(finalTotal),  (int)(775 * sX), (int)(888 * sY));

            g.dispose();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            return baos.toByteArray();
        } catch (Exception e) { return null; }
    }

    private static String fmt(double d) {
        if (d == (long) d) return String.format("%d", (long) d);
        return String.format("%.2f", d);
    }

    private static String truncate(String s, int n) {
        if (s == null) return "";
        return s.length() > n ? s.substring(0, n - 3) + "..." : s;
    }

    public static class OrderItem {
        public String name;
        public double price;
        public OrderItem(String name, double price) { this.name = name; this.price = price; }
    }
}
