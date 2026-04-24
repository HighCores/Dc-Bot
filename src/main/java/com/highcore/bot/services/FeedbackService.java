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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FeedbackService {
    private static final Logger log = LoggerFactory.getLogger(FeedbackService.class);

    public static final Map<String, Integer> ratingCache = new ConcurrentHashMap<>();
    private static final Map<String, BufferedImage> emojiCache = new HashMap<>();
    private static final Pattern CUSTOM_EMOJI_PATTERN = Pattern.compile("<a?:(\\w+):(\\d+)>");

    private static final String TEMPLATE_1 = "https://i.imgur.com/H5h3H4b.jpg";
    private static final String TEMPLATE_2 = "https://i.imgur.com/eyypAlB.jpg";
    private static final String TEMPLATE_3 = "https://i.imgur.com/kdWPO3f.jpg";
    private static final String TEMPLATE_4 = "https://i.imgur.com/mXYKYnv.jpg";
    private static final String TEMPLATE_5 = "https://i.imgur.com/RqCWzzv.jpg";

    public static final String FEEDBACK_CHANNEL_ID = "1491423672202952806";

    public static void submitFeedback(User user, int stars, String feedback, GuildChannel channel) {
        String expandedFeedback = expandShortcodes(feedback, user.getJDA());
        log.info("[FEEDBACK] Submitting feedback for user: {} ({} stars). Channel: {}", user.getName(), stars,
                (channel != null ? channel.getName() : "NULL"));
        try {
            byte[] image = generateFeedbackImage(user, stars, expandedFeedback);
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
        String fontName = "Zain-Bold.ttf";
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

    private static String expandShortcodes(String text, net.dv8tion.jda.api.JDA jda) {
        if (text == null || jda == null) return text;
        Pattern p = Pattern.compile(":(\\w+):");
        Matcher m = p.matcher(text);
        StringBuilder sb = new StringBuilder();
        int last = 0;
        while (m.find()) {
            sb.append(text, last, m.start());
            String name = m.group(1);
            // Check if it's already part of <:name:id>
            boolean alreadyFormatted = (m.start() > 1 && text.charAt(m.start()-1) == '<' && text.charAt(m.start()-2) == ':');
            if (alreadyFormatted) {
                sb.append(":").append(name).append(":");
            } else {
                String id = null;
                for (net.dv8tion.jda.api.entities.Guild g : jda.getGuilds()) {
                    java.util.List<net.dv8tion.jda.api.entities.emoji.RichCustomEmoji> emojis = g.getEmojisByName(name, true);
                    if (!emojis.isEmpty()) {
                        id = emojis.get(0).getId();
                        break;
                    }
                }
                if (id != null) sb.append("<:").append(name).append(":").append(id).append(">");
                else sb.append(":").append(name).append(":");
            }
            last = m.end();
        }
        sb.append(text.substring(last));
        return sb.toString();
    }

    private static void drawWrappedText(Graphics2D g, String text, int x, int y, int width, int height) {
        Font zain = g.getFont();
        FontMetrics fm = g.getFontMetrics(zain);
        int lineHeight = fm.getHeight();
        int curY = y + fm.getAscent();

        List<String> wrappedLines = wrapText(text, fm, width);
        for (String rawLine : wrappedLines) {
            List<Object> parts = parseLineParts(rawLine);
            int lineWidth = calculatePartsWidth(g, parts, zain);
            
            int curX = x + (width - lineWidth) / 2;

            for (Object part : parts) {
                if (part instanceof String s) {
                    g.drawString(s, curX, curY);
                    curX += g.getFontMetrics().stringWidth(s);
                } else if (part instanceof BufferedImage img) {
                    int size = (int) (lineHeight * 0.85);
                    int offset = (lineHeight - size) / 2;
                    g.drawImage(img, curX, curY - fm.getAscent() + offset, size, size, null);
                    curX += size + 2;
                }
            }
            
            curY += lineHeight;
            if (curY > y + height) return;
        }
    }

    private static List<String> wrapText(String text, FontMetrics fm, int maxWidth) {
        List<String> lines = new ArrayList<>();
        String[] paragraphs = text.split("\n");
        for (String p : paragraphs) {
            String[] words = p.split(" ");
            StringBuilder line = new StringBuilder();
            for (String word : words) {
                if (fm.stringWidth(word) > maxWidth) {
                    // Word itself is too long, break it
                    if (line.length() > 0) {
                        lines.add(line.toString().trim());
                        line = new StringBuilder();
                    }
                    for (int i = 0; i < word.length(); i++) {
                        if (fm.stringWidth(line.toString() + word.charAt(i)) < maxWidth) {
                            line.append(word.charAt(i));
                        } else {
                            lines.add(line.toString().trim());
                            line = new StringBuilder().append(word.charAt(i));
                        }
                    }
                    line.append(" ");
                } else if (fm.stringWidth(line + word) < maxWidth) {
                    line.append(word).append(" ");
                } else {
                    lines.add(line.toString().trim());
                    line = new StringBuilder(word).append(" ");
                }
            }
            if (line.length() > 0) lines.add(line.toString().trim());
        }
        return lines;
    }

    private static List<Object> parseLineParts(String line) {
        List<Object> parts = new ArrayList<>();
        int i = 0;
        while (i < line.length()) {
            // Check for Custom Discord Emoji
            Matcher m = CUSTOM_EMOJI_PATTERN.matcher(line.substring(i));
            if (m.find() && m.start() == 0) {
                String id = m.group(2);
                BufferedImage img = getEmojiImage(id, false);
                if (img != null) parts.add(img);
                else parts.add(m.group(0));
                i += m.end();
                continue;
            }

            int codePoint = line.codePointAt(i);
            int charCount = Character.charCount(codePoint);

            if (isEmoji(codePoint)) {
                StringBuilder hex = new StringBuilder(Integer.toHexString(codePoint));
                // Handle variation selectors and ZWJ sequences if needed (simplified here)
                BufferedImage img = getEmojiImage(hex.toString(), true);
                if (img != null) parts.add(img);
                else parts.add(line.substring(i, i + charCount));
                i += charCount;
            } else {
                StringBuilder textPart = new StringBuilder();
                while (i < line.length() && !isEmoji(line.codePointAt(i)) && !CUSTOM_EMOJI_PATTERN.matcher(line.substring(i)).find()) {
                    int cp = line.codePointAt(i);
                    int cc = Character.charCount(cp);
                    textPart.append(line.substring(i, i + cc));
                    i += cc;
                    if (i < line.length() && CUSTOM_EMOJI_PATTERN.matcher(line.substring(i)).find()) break;
                }
                parts.add(textPart.toString());
            }
        }
        return parts;
    }

    private static boolean isEmoji(int codePoint) {
        return (codePoint >= 0x1F300 && codePoint <= 0x1F9FF) || 
               (codePoint >= 0x2600 && codePoint <= 0x27BF) ||
               (codePoint >= 0x1F1E6 && codePoint <= 0x1F1FF);
    }

    private static BufferedImage getEmojiImage(String id, boolean isUnicode) {
        String cacheKey = (isUnicode ? "U_" : "C_") + id;
        if (emojiCache.containsKey(cacheKey)) return emojiCache.get(cacheKey);
        try {
            String urlStr = isUnicode 
                ? "https://cdnjs.cloudflare.com/ajax/libs/twemoji/14.0.2/72x72/" + id + ".png"
                : "https://cdn.discordapp.com/emojis/" + id + ".png";
            URL url = new URL(urlStr);
            BufferedImage img = ImageIO.read(url);
            if (img != null) {
                emojiCache.put(cacheKey, img);
                return img;
            }
        } catch (Exception e) {
            if (!isUnicode) log.error("[FEEDBACK] Failed to load emoji: " + id);
        }
        return null;
    }

    private static int calculatePartsWidth(Graphics2D g, List<Object> parts, Font zain) {
        int w = 0;
        int emojiSize = (int) (g.getFontMetrics(zain).getHeight() * 0.85);
        for (Object p : parts) {
            if (p instanceof String s) {
                w += g.getFontMetrics(zain).stringWidth(s);
            } else if (p instanceof BufferedImage) {
                w += emojiSize + 2;
            }
        }
        return w;
    }
}
