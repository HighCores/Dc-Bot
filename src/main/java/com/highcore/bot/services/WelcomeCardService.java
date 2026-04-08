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
            String urlStr = com.highcore.bot.config.Config.WELCOME_BG_URL;
            log.debug("Attempting to load welcome background from: [{}]", urlStr);
            
            java.net.URL url = new java.net.URL(urlStr);
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
            background = ImageIO.read(connection.getInputStream());
        } catch (Exception e) {
            log.error("Error loading background image: {}", e.getMessage());
            throw new Exception("Source image unreachable or invalid format: " + e.getMessage());
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

        // 2. Overlay Avatar (Covering placeholder cowboy)
        String avatarUrl = member.getUser().getEffectiveAvatarUrl() + "?size=256";
        BufferedImage avatar = ImageIO.read(new URL(avatarUrl));

        int avatarSize = 216;
        int avatarX = 187; // Precision calibrated for the template circle
        int avatarY = 88;

        g.setClip(new Ellipse2D.Float(avatarX, avatarY, avatarSize, avatarSize));
        g.drawImage(avatar, avatarX, avatarY, avatarSize, avatarSize, null);
        g.setClip(null);

        // 3. Overlay Name (Covering placeholder "2mw_x")
        g.setFont(new Font("SansSerif", Font.BOLD, 68));
        g.setColor(new Color(212, 175, 55)); // Gold matching the template theme
        
        String name = member.getUser().getName();
        if (name.length() > 12) name = name.substring(0, 10) + "..";
        
        // Horizontal centering logic for the name box
        FontMetrics metrics = g.getFontMetrics();
        int boxCenterX = 560; // Approximate center of the name rectangle
        int nameX = boxCenterX - (metrics.stringWidth(name) / 2);
        int nameY = 245;

        g.drawString(name, nameX, nameY);

        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(combined, "png", baos);
        return baos.toByteArray();
    }
}
