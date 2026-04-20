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
        return generateInvoice(invoiceId, clientName, projectName, items, false, null, clientName, "Service Delivery", "Highcore Member", 0.0);
    }

    public static byte[] generateInvoice(String invoiceId, String clientName, String projectName, List<OrderItem> items, boolean isPaid, String avatarUrl, String displayName, String category, String contact, double discount) {
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

            // 1. TOP RIGHT: Invoice ID
            g.setFont(new Font("Segoe UI", Font.BOLD, (int)(W * 0.035)));
            g.setColor(COL_GOLD);
            g.drawString("#" + invoiceId, (int)(W * 0.66), (int)(H * 0.145));

            // 2. DATE (In Capsule area)
            g.setFont(new Font("Segoe UI", Font.BOLD, (int)(W * 0.02)));
            g.setColor(COL_WHITE);
            g.drawString(dateStr, (int)(W * 0.685), (int)(H * 0.235));
            
            // 3. STATUS
            g.setFont(new Font("Segoe UI", Font.BOLD, (int)(W * 0.022)));
            g.setColor(isPaid ? Color.GREEN : COL_GOLD);
            g.drawString(isPaid ? "AUTHORIZED" : "PENDING", (int)(W * 0.665), (int)(H * 0.282));

            // 4. CLIENT DETAILS (Right side box)
            g.setFont(new Font("Segoe UI", Font.BOLD, (int)(W * 0.02)));
            g.setColor(COL_WHITE);
            String clientInfo = "CLIENT: " + truncate(displayName != null ? displayName : clientName, 20);
            g.drawString(clientInfo, (int)(W * 0.675), (int)(H * 0.355));
            g.setFont(new Font("Segoe UI", Font.PLAIN, (int)(W * 0.016)));
            g.drawString("CONTACT: " + truncate(contact != null ? contact : "Discord Hub", 18), (int)(W * 0.675), (int)(H * 0.385));
            g.drawString("ID: " + truncate(clientName, 18), (int)(W * 0.675), (int)(H * 0.405));

            // 5. PROJECT INFO (Left side)
            g.setFont(new Font("Segoe UI", Font.BOLD, (int)(W * 0.022)));
            g.setColor(COL_WHITE);
            g.drawString(truncate(projectName, 25), (int)(W * 0.13), (int)(H * 0.245));
            
            g.setFont(new Font("Segoe UI", Font.PLAIN, (int)(W * 0.018)));
            g.setColor(COL_CREAM);
            g.drawString(category != null ? category : "Software Services", (int)(W * 0.13), (int)(H * 0.275));

            // 6. ADD-ONS (The box below)
            g.setFont(new Font("Segoe UI", Font.PLAIN, (int)(H * 0.018)));
            g.setColor(COL_CREAM);
            int addOnStartY = (int)(H * 0.365);
            for (int i = 0; i < Math.min(items.size(), 4); i++) {
                g.drawString("‣ " + truncate(items.get(i).name, 35), (int)(W * 0.135), addOnStartY + (i * (int)(H * 0.025)));
            }

            // 7. MAIN SERVICES TABLE
            int col_ServicesX = (int)(W * 0.14);
            int col_PriceX    = (int)(W * 0.68);
            int col_QtyX      = (int)(W * 0.76);
            int col_TotalX    = (int)(W * 0.85);

            double subtotalVal = 0;
            int tableStartY = (int)(H * 0.655);
            int tableRowGap = (int)(H * 0.045); 
            
            g.setFont(new Font("Segoe UI", Font.BOLD, (int)(W * 0.018)));
            g.setColor(COL_WHITE);
            for (int i = 0; i < Math.min(items.size(), 5); i++) {
                OrderItem item = items.get(i);
                int y = tableStartY + (i * tableRowGap);
                
                g.drawString(truncate(item.name, 45), col_ServicesX, y);
                
                String pStr = "$" + fmt(item.price);
                g.drawString(pStr, col_PriceX - g.getFontMetrics().stringWidth(pStr)/2, y);
                g.drawString("1", col_QtyX - g.getFontMetrics().stringWidth("1")/2, y);
                g.drawString(pStr, col_TotalX - g.getFontMetrics().stringWidth(pStr)/2, y);
                
                subtotalVal += item.price;
            }

            // 8. FOOTER FINANCIALS
            double taxVal = (subtotalVal - discount) * 0.05;
            if (taxVal < 0) taxVal = 0;
            double finalTotal = (subtotalVal - discount) + taxVal;

            g.setFont(new Font("Segoe UI", Font.BOLD, (int)(W * 0.02)));
            g.setColor(COL_GOLD);
            // Left Bottom
            g.drawString("$" + fmt(subtotalVal), (int)(W * 0.25), (int)(H * 0.855)); 
            g.drawString("-$" + fmt(discount),    (int)(W * 0.25), (int)(H * 0.885)); 

            // Right Bottom
            g.drawString("$" + fmt(taxVal),      (int)(W * 0.75), (int)(H * 0.840)); 
            g.setFont(new Font("Segoe UI", Font.BOLD, (int)(W * 0.025)));
            g.drawString("$" + fmt(finalTotal),  (int)(W * 0.75), (int)(H * 0.880));

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
