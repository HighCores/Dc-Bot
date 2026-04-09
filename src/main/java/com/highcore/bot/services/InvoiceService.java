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

    public static byte[] generateInvoice(String clientName, String projectName, List<OrderItem> items) {
        try {
            // 1. Load Template
            BufferedImage invoice;
            try (InputStream is = InvoiceService.class.getResourceAsStream("/invoice.png")) {
                if (is != null) {
                    invoice = ImageIO.read(is);
                } else {
                    // Fallback to URL if resource not found in classpath yet
                    invoice = ImageIO.read(new URL(EmbedUtil.BANNER_INVOICE));
                }
            }

            Graphics2D g = invoice.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            // 2. Metadata (Top Right Boxes)
            g.setFont(new Font("SansSerif", Font.BOLD, 18));
            g.setColor(new Color(200, 200, 200));
            
            String invoiceId = "HC-" + (1000 + new Random().nextInt(9000));
            String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
            
            g.drawString(invoiceId, 770, 107);
            g.drawString(date, 705, 150);

            // 3. Client Header
            g.setFont(new Font("SansSerif", Font.BOLD, 22));
            g.setColor(Color.WHITE);
            g.drawString(clientName.toUpperCase(), 120, 295);
            g.drawString(projectName.toUpperCase(), 585, 295);

            // 4. Line Items
            int startY = 375;
            int rowHeight = 40;
            double subtotal = 0;

            g.setFont(new Font("SansSerif", Font.PLAIN, 18));
            g.setColor(new Color(220, 220, 220));

            for (int i = 0; i < items.size() && i < 10; i++) {
                OrderItem item = items.get(i);
                int currentY = startY + (i * rowHeight);
                
                g.drawString(item.name, 120, currentY); // Description
                g.drawString("1", 640, currentY);       // Qty
                g.drawString("$" + item.price, 775, currentY); // Price
                
                subtotal += item.price;
            }

            // 5. Totals
            g.setFont(new Font("SansSerif", Font.BOLD, 22));
            g.setColor(new Color(197, 160, 89)); // Gold
            
            double tax = subtotal * 0.05; // 5% Service Tax
            double total = subtotal + tax;

            g.drawString(String.format("$%.2f", subtotal), 750, 715);
            g.drawString(String.format("$%.2f", tax), 750, 750);
            
            g.setFont(new Font("SansSerif", Font.BOLD, 28));
            g.setColor(Color.WHITE);
            g.drawString(String.format("$%.2f", total), 750, 804);

            g.dispose();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(invoice, "png", baos);
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Failed to generate digital invoice.", e);
            return null;
        }
    }

    public static class OrderItem {
        public String name;
        public double price;
        public OrderItem(String name, double price) { this.name = name; this.price = price; }
    }
}
