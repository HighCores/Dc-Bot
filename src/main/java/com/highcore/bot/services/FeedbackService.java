package com.highcore.bot.services;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.ForumChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.FileUpload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FeedbackService {
    private static final Logger log = LoggerFactory.getLogger(FeedbackService.class);

    public static final Map<String, Integer> ratingCache = new ConcurrentHashMap<>();

    private static final String TEMPLATE_1 = "https://i.imgur.com/H5h3H4b.jpg";
    private static final String TEMPLATE_2 = "https://i.imgur.com/eyypAlB.jpg";
    private static final String TEMPLATE_3 = "https://i.imgur.com/kdWPO3f.jpg";
    private static final String TEMPLATE_4 = "https://i.imgur.com/mXYKYnv.jpg";
    private static final String TEMPLATE_5 = "https://i.imgur.com/RqCWzzv.jpg";

    public static final String FEEDBACK_CHANNEL_ID = "1491423672202952806";

    public static void submitFeedback(User user, int stars, String feedback, GuildChannel channel) {
        log.info("[FEEDBACK] Submitting feedback for user: {} ({} stars). Channel: {}", user.getName(), stars,
                (channel != null ? channel.getName() : "NULL"));
        try {
            byte[] image = generateFeedbackImage(user, stars, feedback);
            if (image == null) {
                log.warn("[FEEDBACK] Failed to generate image for user: {}", user.getName());
                return;
            }
            if (channel == null) {
                log.warn("[FEEDBACK] Feedback channel not found! ID: {}", FEEDBACK_CHANNEL_ID);
                return;
            }

            String title = "Feedback | " + user.getEffectiveName();
            FileUpload file = FileUpload.fromData(image, "feedback.png");
            MessageCreateData messageData = new net.dv8tion.jda.api.utils.messages.MessageCreateBuilder()
                    .addFiles(file)
                    .setContent("### Feedback from " + user.getAsMention())
                    .build();

            if (channel instanceof ForumChannel forum) {
                log.info("[FEEDBACK] Posting to ForumChannel: {}", forum.getName());
                forum.createForumPost(title, messageData).queue(
                        post -> {
                            log.info("[FEEDBACK] Successfully created forum post: {}",
                                    post.getThreadChannel().getName());
                            // Lock the thread
                            post.getThreadChannel().getManager().setLocked(true).queue();
                        },
                        err -> log.error("[FEEDBACK] Failed to create forum post", err));
            } else if (channel instanceof MessageChannel mc) {
                log.info("[FEEDBACK] Posting to MessageChannel: {}", mc.getName());
                mc.sendMessage(messageData)
                        .queue(
                                msg -> log.info("[FEEDBACK] Successfully sent feedback message"),
                                err -> log.error("[FEEDBACK] Failed to send feedback message", err));
            } else {
                log.warn("[FEEDBACK] Channel is not a ForumChannel or MessageChannel! Type: {}", channel.getType());
            }
        } catch (Exception e) {
            log.error("[FEEDBACK] Critical error in submitFeedback", e);
        }
    }

    private static byte[] generateFeedbackImage(User user, int stars, String feedback) throws Exception {
        String templatePath = "/templates/feedback_" + stars + ".jpg";
        log.info("[FEEDBACK] Loading local template: {}", templatePath);

        BufferedImage template = null;
        try (java.io.InputStream is = FeedbackService.class.getResourceAsStream(templatePath)) {
            if (is == null) {
                log.error("[FEEDBACK] Template NOT FOUND in classpath: {}", templatePath);
                return null;
            }
            template = ImageIO.read(is);
        } catch (Exception e) {
            log.error("[FEEDBACK] Failed to read local template: " + templatePath, e);
            return null;
        }

        if (template == null) {
            log.error("[FEEDBACK] ImageIO.read returned null for local template: {}", templatePath);
            return null;
        }

        int w = template.getWidth();
        int h = template.getHeight();

        BufferedImage combined = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = combined.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

        g.drawImage(template, 0, 0, null);

        // Fonts
        Font arabicFont;
        String fontName = "thmanyahsans-Bold.otf";
        log.info("[FEEDBACK] Current Directory: {}", new java.io.File(".").getAbsolutePath());
        
        java.io.InputStream is = FeedbackService.class.getResourceAsStream("/templates/" + fontName);
        if (is == null) is = FeedbackService.class.getClassLoader().getResourceAsStream("templates/" + fontName);
        
        if (is == null) {
            try {
                String[] fallbacks = {
                    "src/main/resources/templates/" + fontName,
                    "highcore-bot/src/main/resources/templates/" + fontName,
                    "templates/" + fontName,
                    fontName
                };
                for (String path : fallbacks) {
                    java.io.File file = new java.io.File(path);
                    if (file.exists()) {
                        is = new java.io.FileInputStream(file);
                        log.info("[FEEDBACK] Found font at fallback path: {}", file.getAbsolutePath());
                        break;
                    }
                }
            } catch (Exception e) {
                log.error("[FEEDBACK] Error in filesystem fallback search", e);
            }
        }

        try (java.io.InputStream fontStream = is) {
            if (fontStream == null) {
                log.error("[FEEDBACK] FONT NOT FOUND in any location: {}", fontName);
                arabicFont = new Font("SansSerif", Font.BOLD, 45);
            } else {
                arabicFont = Font.createFont(Font.TRUETYPE_FONT, fontStream).deriveFont(Font.BOLD, 45f);
                log.info("[FEEDBACK] Successfully loaded font: {}", arabicFont.getFontName());
            }
        } catch (Exception e) {
            log.error("[FEEDBACK] Error creating font from stream", e);
            arabicFont = new Font("SansSerif", Font.BOLD, 45);
        }

        // Feedback (Updated coordinates)
        g.setFont(arabicFont);
        g.setColor(Color.WHITE);
        int commBoxW = 1273 - 545;
        int commBoxH = 592 - 341;
        drawWrappedText(g, feedback, 545, 341, commBoxW, commBoxH);

        g.dispose();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(combined, "png", baos);
        return baos.toByteArray();
    }

    private static void drawWrappedText(Graphics2D g, String text, int x, int y, int width, int height) {
        FontMetrics fm = g.getFontMetrics();
        int lineHeight = fm.getHeight();
        int curY = y + fm.getAscent();

        String[] lines = text.split("\n");
        for (String rawLine : lines) {
            String[] words = rawLine.split(" ");
            StringBuilder currentLine = new StringBuilder();

            for (String word : words) {
                if (fm.stringWidth(currentLine + word) < width) {
                    currentLine.append(word).append(" ");
                } else {
                    String toDraw = currentLine.toString().trim();
                    int lineX = x + (width - fm.stringWidth(toDraw)) / 2;
                    g.drawString(toDraw, lineX, curY);
                    currentLine = new StringBuilder(word).append(" ");
                    curY += lineHeight;
                    if (curY > y + height)
                        return;
                }
            }
            String toDraw = currentLine.toString().trim();
            int lineX = x + (width - fm.stringWidth(toDraw)) / 2;
            g.drawString(toDraw, lineX, curY);
            curY += lineHeight;
            if (curY > y + height)
                return;
        }
    }
}
