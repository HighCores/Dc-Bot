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
        // Highcore Agency Branding Background (The premium golden background provided by user)
        BufferedImage background = ImageIO.read(new URL("https://i.ibb.co/L5T8S0j/IMG-8438.jpg"));
        int width = background.getWidth();
        int height = background.getHeight();

        BufferedImage combined = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = combined.createGraphics();
        
        // High-Quality Rendering
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

        // 1. Draw Background
        g.drawImage(background, 0, 0, width, height, null);

        // 2. Prepare Avatar
        String avatarUrl = member.getUser().getEffectiveAvatarUrl() + "?size=512";
        BufferedImage avatar = ImageIO.read(new URL(avatarUrl));

        // Placement math: Left-aligned, vertically offset for balance
        int avatarSize = (int) (height * 0.50);
        int avatarX = (int) (width * 0.08);
        int avatarY = (int) (height * 0.25);

        // Circular Clip
        g.setClip(new Ellipse2D.Float(avatarX, avatarY, avatarSize, avatarSize));
        g.drawImage(avatar, avatarX, avatarY, avatarSize, avatarSize, null);
        g.setClip(null);

        // Premium Gold Outer Border
        g.setColor(new Color(212, 175, 55)); // Metallic Gold
        g.setStroke(new BasicStroke(8));
        g.drawOval(avatarX, avatarY, avatarSize, avatarSize);

        // 3. Welcome Text (Small above the name box)
        g.setFont(new Font("SansSerif", Font.PLAIN, 22));
        g.setColor(new Color(230, 230, 230));
        int textX = avatarX + avatarSize + 50;
        int textY = avatarY + 45;
        g.drawString("Welcome to the Future", textX + 5, textY);

        // 4. Modern Name Box
        int boxW = 500;
        int boxH = 90;
        int boxX = textX;
        int boxY = textY + 15;
        
        // Semi-transparent deep background
        g.setColor(new Color(0, 0, 0, 160));
        g.fillRoundRect(boxX, boxY, boxW, boxH, 20, 20);
        
        // Gold Border for Box
        g.setColor(new Color(212, 175, 55, 200));
        g.setStroke(new BasicStroke(3));
        g.drawRoundRect(boxX, boxY, boxW, boxH, 20, 20);

        // 5. Member Name
        g.setFont(new Font("SansSerif", Font.BOLD, 48));
        g.setColor(Color.WHITE);
        String name = member.getUser().getName().toUpperCase();
        if (name.length() > 16) name = name.substring(0, 14) + "..";
        g.drawString(name, boxX + 40, boxY + 62);

        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(combined, "png", baos);
        return baos.toByteArray();
    }
}
