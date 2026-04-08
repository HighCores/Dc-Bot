package com.highcore.bot.services;

import net.dv8tion.jda.api.entities.Member;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.URL;

public class WelcomeCardService {

    /**
     * Generates a high-fidelity welcome card using Highcore Agency branding.
     * Background: Highcore Golden Banner
     * Overlay: User Avatar (Left), "Welcome to the Future" text, and Name Box.
     */
    public static byte[] generateWelcomeCard(Member member) throws Exception {
        // Highcore Agency Precision Template (1126x398)
        BufferedImage background = null;
        try {
            background = ImageIO.read(new URL("https://media.discordapp.net/attachments/1488900668042510568/1491413582108430386/IMG_20260408_152445.png?ex=69d79aa3&is=69d64923&hm=3c5b32bb1aaa54ad4267d5a9ef9eba50ac0ac997a111ab515254d5126460aa66&=&format=webp&quality=lossless&width=1126&height=398"));
        } catch (Exception e) {
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
