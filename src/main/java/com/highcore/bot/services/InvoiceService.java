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

            // 1. Invoice ID - Precise landing in the gold box
            g.setFont(new Font("Segoe UI", Font.BOLD, (int)(W * 0.024)));
            g.setColor(COL_GOLD);
            String idTag = invoiceId; 
            g.drawString(idTag, (int)(W * 0.77 - g.getFontMetrics().stringWidth(idTag)/2), (int)(H * 0.142));

            // 2. Avatar & Name - Vertical drop to clear headers
            if (avatarUrl != null && !avatarUrl.isEmpty()) {
                try {
                    java.net.URLConnection conn = new URL(avatarUrl).openConnection();
                    try (InputStream is = conn.getInputStream()) {
                        BufferedImage avatar = ImageIO.read(is);
                        if (avatar != null) {
                            int r = (int)(W * 0.06); 
                            int x = (int)(W * 0.680); 
                            int y = (int)(H * 0.180); 
                            Shape oldClip = g.getClip();
                            g.setClip(new Ellipse2D.Double(x, y, r, r));
                            g.drawImage(avatar, x, y, r, r, null);
                            g.setClip(oldClip);
                        }
                    }
                } catch (Exception ignored) {}
            }
            
            g.setFont(new Font("Segoe UI", Font.BOLD, (int)(W * 0.016)));
            g.setColor(COL_WHITE);
            String uName = truncate(displayName != null ? displayName : clientName, 18);
            g.drawString(uName, (int)(W * 0.710 - g.getFontMetrics().stringWidth(uName)/2), (int)(H * 0.250));

            // 3. Status - Clear of 'CLINET DETAILS' header
            g.setFont(new Font("Segoe UI", Font.BOLD, (int)(W * 0.016)));
            g.setColor(isPaid ? Color.GREEN : COL_GOLD);
            String statusText = isPaid ? "AUTHORIZED" : "PENDING";
            g.drawString(statusText, (int)(W * 0.710 - g.getFontMetrics().stringWidth(statusText)/2), (int)(H * 0.332));

            // 4. Client Details - Lowered alignment
            g.setFont(new Font("Segoe UI", Font.BOLD, (int)(W * 0.018)));
            g.setColor(COL_WHITE);
            int clientBoxX = (int)(W * 0.678);
            g.drawString(truncate(displayName != null ? displayName : clientName, 25), clientBoxX, (int)(H * 0.362));
            g.setFont(new Font("Segoe UI", Font.PLAIN, (int)(W * 0.014)));
            String contactInfo = (contact != null ? contact : "") + (phone != null && !phone.isEmpty() ? " (" + phone + ")" : "");
            g.drawString(truncate(contactInfo.isEmpty() ? "No Contact" : contactInfo, 40), clientBoxX, (int)(H * 0.386));
            g.drawString(truncate(clientName, 30), clientBoxX, (int)(H * 0.410));

            // 5. Project Info - Better alignment with left headers
            g.setFont(new Font("Segoe UI", Font.BOLD, (int)(W * 0.020)));
            g.setColor(COL_WHITE);
            g.drawString(truncate(projectName, 25), (int)(W * 0.44), (int)(H * 0.231));
            
            g.setFont(new Font("Segoe UI", Font.PLAIN, (int)(W * 0.017)));
            g.setColor(COL_CREAM);
            String catLabel = category != null ? category : "Software Services";
            if (catLabel.length() > 2) catLabel = catLabel.substring(0,1).toUpperCase() + catLabel.substring(1).toLowerCase();
            g.drawString(catLabel, (int)(W * 0.44), (int)(H * 0.261));

            // 6. Add-ons
            g.setFont(new Font("Segoe UI", Font.PLAIN, (int)(H * 0.018)));
            g.setColor(COL_CREAM);
            int addOnStartY = (int)(H * 0.358);
            for (int i = 0; i < Math.min(items.size(), 6); i++) {
                g.drawString("\u2022 " + truncate(items.get(i).name, 45), (int)(W * 0.142), addOnStartY + (i * (int)(H * 0.027)));
            }

            // 7. Table - Heightened to avoid bottom overlap
            int col_ServicesX = (int)(W * 0.142);
            int col_PriceX    = (int)(W * 0.672);
            int col_QtyX      = (int)(W * 0.758);
            int col_TotalX    = (int)(W * 0.84);
            int tableStartY = (int)(H * 0.648);
            int tableRowGap = (int)(H * 0.048); 

            double subtotalVal = 0;
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

            // 8. Financials - Major Y-shift and X-expansion
            double taxVal = (subtotalVal - discount) * 0.05;
            double finalTotal = Math.max(0, (subtotalVal - discount) + taxVal);

            g.setFont(new Font("Segoe UI", Font.BOLD, (int)(W * 0.022)));
            g.setColor(COL_GOLD);
            g.drawString("$" + fmt(subtotalVal), (int)(W * 0.38), (int)(H * 0.860)); 
            g.drawString("-$" + fmt(discount),    (int)(W * 0.38), (int)(H * 0.885)); 

            g.drawString("$" + fmt(taxVal),      (int)(W * 0.915), (int)(H * 0.845)); 
            g.setFont(new Font("Segoe UI", Font.BOLD, (int)(W * 0.026)));
            g.drawString("$" + fmt(finalTotal),  (int)(W * 0.915), (int)(H * 0.892));

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
