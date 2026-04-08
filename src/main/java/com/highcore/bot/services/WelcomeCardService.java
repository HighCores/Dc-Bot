package com.highcore.bot.services;

import net.dv8tion.jda.api.entities.Member;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.URL;

public class WelcomeCardService {
    private static final Logger log = LoggerFactory.getLogger(WelcomeCardService.class);

    /**
     * Generates a high-fidelity welcome card using Highcore Agency branding.
     * Background: Highcore Golden Banner
     * Overlay: User Avatar (Left), "Welcome to the Future" text, and Name Box.
     */
    public static byte[] generateWelcomeCard(Member member) throws Exception {
        // Highcore Agency Precision Template (1126x398) - Loaded from Config
        BufferedImage background = null;
        try {
            // Priority 1: Multi-Path Local Search (Highly Robust for Railway/Docker)
            String[] commonPaths = {"welcome.png", "IMG_20260408_171922.png", "src/main/resources/welcome.png", "/app/welcome.png"};
            for (String path : commonPaths) {
                java.io.File file = new java.io.File(path);
                if (file.exists()) {
                    log.info("Asset detected at: [{}]. Initializing...", path);
                    background = ImageIO.read(file);
                    break;
                }
            }
            
            // Priority 2: Remote URL (Fallback)
            if (background == null) {
                String urlStr = com.highcore.bot.config.Config.WELCOME_BG_URL;
                log.debug("Local asset not found. Requesting remote fallback: [{}]", urlStr);
                
                java.net.URL url = new java.net.URL(urlStr);
                java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36");
                connection.setRequestProperty("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8");
                
                background = ImageIO.read(connection.getInputStream());
            }
        } catch (Exception e) {
            log.error("Asset initialization failed: {}", e.getMessage());
            throw new Exception("Background initialization aborted: " + e.getMessage());
        }

        if (background == null) {
            throw new Exception("Background image is null after read attempt.");
        }

        int width = background.getWidth();
        int height = background.getHeight();

        BufferedImage combined = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = combined.createGraphics();
        
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

        // 1. Draw Template
        g.drawImage(background, 0, 0, width, height, null);

        // 2. Avatar - Calibrated for the new clean circle
        String avatarUrl = member.getUser().getEffectiveAvatarUrl() + "?size=256";
        BufferedImage avatar = null;
        try {
            java.net.URL url = new java.net.URL(avatarUrl);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
            avatar = ImageIO.read(conn.getInputStream());
        } catch (Exception e) {
            log.warn("Failed to load user avatar: {}. Using generic fallback.", e.getMessage());
            // Create a generic colored circle if avatar fails to avoid total failure
            avatar = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
            Graphics2D gAv = avatar.createGraphics();
            gAv.setColor(new Color(212, 175, 55));
            gAv.fillOval(0, 0, 256, 256);
            gAv.dispose();
        }

        int avatarSize = 196; 
        int avatarX = 227;    // Precise horizontal center for the new template circle
        int avatarY = 101;    // Precise vertical center

        g.setClip(new Ellipse2D.Float(avatarX, avatarY, avatarSize, avatarSize));
        g.drawImage(avatar, avatarX, avatarY, avatarSize, avatarSize, null);
        g.setClip(null);

        // 3. Name - Premium Typography (Golden Gradient + Shadow)
        String name = member.getUser().getName().toUpperCase(); // Tech Look
        if (name.length() > 14) name = name.substring(0, 12) + "..";

        int fontSize = 42; 
        g.setFont(new Font("SansSerif", Font.BOLD, fontSize));
        
        // Letter Spacing / Tracking implementation (+2%)
        java.util.Map<java.awt.font.TextAttribute, Object> attributes = new java.util.HashMap<>();
        attributes.put(java.awt.font.TextAttribute.TRACKING, 0.05); // Subtle tech spacing
        g.setFont(g.getFont().deriveFont(attributes));

        FontMetrics metrics = g.getFontMetrics();
        int boxCenterX = 585; 
        int boxCenterY = 245;
        int nameX = boxCenterX - (metrics.stringWidth(name) / 2);
        int nameY = boxCenterY + (metrics.getAscent() / 3); // Better visual centering

        // A. Drop Shadow
        g.setColor(new Color(0, 0, 0, 180));
        g.drawString(name, nameX + 2, nameY + 2);

        // B. Golden Gradient Body
        GradientPaint gp = new GradientPaint(
            nameX, nameY - fontSize, new Color(197, 160, 89), // #C5A059
            nameX, nameY, new Color(142, 115, 65)             // #8E7341
        );
        g.setPaint(gp);
        g.drawString(name, nameX, nameY);

        // C. Subtle Highlight (Overlaying slightly shifted for gloss)
        g.setColor(new Color(240, 230, 140, 100)); // #F0E68C with alpha
        g.drawString(name, nameX, nameY - 1); 

        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(combined, "png", baos);
        return baos.toByteArray();
    }
}
