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
        // Highcore Agency Branding Banner (Image 1 from user)
        BufferedImage background = ImageIO.read(new URL("https://i.ibb.co/3ykfX5K/media-1775551414274.png"));
        int width = background.getWidth();
        int height = background.getHeight();

        BufferedImage combined = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = combined.createGraphics();
        
        // Quality rendering hints
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        // 1. Draw Background
        g.drawImage(background, 0, 0, null);

        // 2. Prepare Avatar
        String avatarUrl = member.getUser().getEffectiveAvatarUrl() + "?size=256";
        BufferedImage avatar = ImageIO.read(new URL(avatarUrl));

        // Placement math: Left-aligned, vertically centered
        int avatarSize = (int) (height * 0.65);
        int avatarX = (int) (width * 0.06);
        int avatarY = (height - avatarSize) / 2;

        // Circular Clip for Avatar
        g.setClip(new Ellipse2D.Float(avatarX, avatarY, avatarSize, avatarSize));
        g.drawImage(avatar, avatarX, avatarY, avatarSize, avatarSize, null);
        g.setClip(null);

        // Golden Outer Border for Avatar
        g.setColor(new Color(212, 175, 55)); // Metallic Gold #D4AF37
        g.setStroke(new BasicStroke(6));
        g.drawOval(avatarX, avatarY, avatarSize, avatarSize);

        // 3. Welcome Subtext (Small font above name)
        g.setFont(new Font("Arial", Font.PLAIN, 18));
        g.setColor(new Color(200, 200, 200));
        int textX = avatarX + avatarSize + 40;
        int textY = avatarY + 35;
        g.drawString("Welcome to the Future", textX, textY);

        // 4. Name Box (Modern transparency)
        int boxW = 450;
        int boxH = 75;
        int boxX = textX;
        int boxY = textY + 15;
        
        // Shadow/Glow (Subtle)
        g.setColor(new Color(212, 175, 55, 30));
        g.fillRect(boxX - 2, boxY - 2, boxW + 4, boxH + 4);

        // Box Body
        g.setColor(new Color(0, 0, 0, 180));
        g.fillRect(boxX, boxY, boxW, boxH);
        
        // Box Border (Gold)
        g.setColor(new Color(212, 175, 55));
        g.setStroke(new BasicStroke(2));
        g.drawRect(boxX, boxY, boxW, boxH);

        // 5. User Name
        g.setFont(new Font("Arial", Font.BOLD, 42));
        g.setColor(Color.WHITE);
        String name = member.getEffectiveName();
        // Simple truncation if too long for the box
        if (name.length() > 18) name = name.substring(0, 15) + "...";
        g.drawString(name, boxX + 25, boxY + 52);

        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(combined, "png", baos);
        return baos.toByteArray();
    }
}
