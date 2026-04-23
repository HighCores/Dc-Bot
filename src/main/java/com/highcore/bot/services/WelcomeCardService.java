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
        BufferedImage background = null;
        try {
            // Priority 1: Remote URL (User's specific preference)
            String urlStr = com.highcore.bot.config.Config.WELCOME_BG_URL;
            if (urlStr != null && !urlStr.isEmpty()) {
                log.info("Attempting to load background from URL: [{}]", urlStr);
                java.net.URL url = new java.net.URL(urlStr);
                java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(8000);
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36");
                background = ImageIO.read(connection.getInputStream());
            }

            // Priority 2: Classpath Resource (Fallback)
            if (background == null) {
                log.info("Falling back to classpath resources...");
                java.io.InputStream is = WelcomeCardService.class.getResourceAsStream("/welcome.png");
                if (is == null) is = WelcomeCardService.class.getResourceAsStream("/IMG_20260408_171922.png");
                if (is != null) background = ImageIO.read(is);
            }
        } catch (Exception e) {
            log.error("Resource pipeline failure: {}", e.getMessage());
            // Final fallback: try local resource if URL fails
            if (background == null) {
                try {
                    java.io.InputStream is = WelcomeCardService.class.getResourceAsStream("/welcome.png");
                    if (is != null) background = ImageIO.read(is);
                } catch (Exception ignored) {}
            }
            if (background == null) throw new Exception("Branding pipeline failure: " + e.getMessage());
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

        // --- THE DESIGNER'S BLUEPRINT (PIXEL-PERFECT ARCHITECTURE) ---
        
        // 1. Digital Avatar Container (Surgically Calibrated)
        int avatarSize = 640; 
        int avatarX = 410; 
        int avatarY = 198;

        g.setClip(new Ellipse2D.Float(avatarX, avatarY, avatarSize, avatarSize));
        g.drawImage(avatar, avatarX, avatarY, avatarSize, avatarSize, null);
        g.setClip(null);

        // 2. Welcome Headline Layer (Fixed)
        int headlineY = 145;

        // 3. Member Identity Engine (Sleek Font)
        String name = member.getEffectiveName().toUpperCase(); 
        if (name.length() > 25) name = name.substring(0, 23) + "..";

        int fontSize = 60; 
        g.setFont(new Font("SansSerif", Font.BOLD, fontSize));
        
        java.util.Map<java.awt.font.TextAttribute, Object> attributes = new java.util.HashMap<>();
        attributes.put(java.awt.font.TextAttribute.TRACKING, 0.1); // Tech spacing
        g.setFont(g.getFont().deriveFont(attributes));

        FontMetrics metrics = g.getFontMetrics();
        int nameX = 1204; 
        int nameY = 652 + ((725 - 652) / 2) + (metrics.getAscent() / 2) - 5;

        // A. Drop Shadow Case
        g.setColor(new Color(0, 0, 0, 180));
        g.drawString(name, nameX + 3, nameY + 3);

        // B. Master Golden Gradient
        GradientPaint gp = new GradientPaint(
            nameX, nameY - fontSize, new Color(197, 160, 89), 
            nameX, nameY, new Color(142, 115, 65) 
        );
        g.setPaint(gp);
        g.drawString(name, nameX, nameY);

        // C. Specular Highlight
        g.setColor(new Color(255, 255, 255, 60));
        g.drawString(name, nameX, nameY - 1);

        // C. Subtle Highlight (Overlaying slightly shifted for gloss)
        g.setColor(new Color(240, 230, 140, 100)); // #F0E68C with alpha
        g.drawString(name, nameX, nameY - 1); 

        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(combined, "png", baos);
        return baos.toByteArray();
    }
}
