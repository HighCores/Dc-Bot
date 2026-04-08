package com.highcore.bot.utils;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.mediagallery.MediaGallery;
import net.dv8tion.jda.api.components.mediagallery.MediaGalleryItem;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

public class EmbedUtil {

    public static final String BANNER_MAIN = "https://media.discordapp.net/attachments/1488795131296354460/1491194673048457399/banner.png?ex=69d6cec3&is=69d57d43&hm=fb7fa5371021b0967abe71019d9cf5b2ed5f7ce1efc3b75cdd6cd8d8bc67b50d&format=webp&quality=lossless&width=1572&height=884";
    public static final String BANNER_SUPPORT = BANNER_MAIN;
    public static final String BANNER_GIVEAWAY = BANNER_MAIN;
    
    public static final Color SUCCESS = Color.decode("#D4AF37");
    public static final Color DANGER = Color.decode("#8B0000");
    public static final Color WARNING = Color.decode("#FFD700");
    public static final Color INFO = Color.decode("#C0C0C0");
    public static final Color GOLD = Color.decode("#D4AF37");
    public static final Color ACCENT_GOLD = Color.decode("#FFD700");
    
    public static final String RULES_TEXT = """
            ### 🛡️ 1. General Rules
            1. **Mutual Respect**: Harassment, bullying, or offensive language is strictly prohibited.
            2. **Professionalism**: Use clear nicknames and avoid inappropriate profile pictures.
            3. **Privacy**: Never share personal information (Doxxing) of any member or client.
            
            ### 💼 2. Business & Service Rules
            1. **Serious Projects**: Order channels are for work only. Mockery or fake orders may lead to restrictions.
            2. **Copyright**: All development and design rights belong to High Core unless agreed otherwise.
            3. **Official Channels**: Financial or technical agreements must go through Tickets.
            
            ### 🚫 3. Prohibitions
            1. **No Ads**: No promotion of external servers or services without permission.
            2. **No Spam**: Avoid excessive mentions or message repetition.
            3. **Content**: No political, religious extremism, or NSFW content.
            """;

    public static Container containerBranded(String category, String topic, String body, String imageUrl) {
        return containerBranded(category, topic, body, imageUrl, null);
    }

    public static Container containerBranded(String title, String subtitle, String body, String imageUrl, Emoji emoji, ActionRow... rows) {
        List<ContainerChildComponent> layout = new ArrayList<>();
        
        // 1. ELITE HEADER
        layout.add(TextDisplay.of("**\u25C8 " + title.toUpperCase() + " \u30FB " + subtitle.toUpperCase() + "**"));
        layout.add(Separator.createDivider(Separator.Spacing.SMALL));

        // 2. TECHNICAL BODY
        if (body != null) {
            layout.add(TextDisplay.of(body));
        }

        // 3. ACTION INTERFACE
        if (rows != null && rows.length > 0) {
            for (ActionRow row : rows) {
                layout.add(row);
            }
        }

        // 4. INFRASTRUCTURE FOOTER
        layout.add(Separator.createDivider(Separator.Spacing.SMALL));
        layout.add(TextDisplay.of("` \u2022 High Core Unified System \u2022 v1.7.0 \u2022 `"));

        return Container.of(layout);
    }

    public static Container startupPanel(ActionRow... rows) {
        String body = "◈ High Core Unified Terminal\n" +
                "Global infrastructure at your fingertips. Use the protocols below to navigate our sectors.";
        return containerBranded("HUB", "Main Dashboard", body, BANNER_MAIN, null, rows);
    }

    public static Container rulesPanel(ActionRow... rows) {
        return containerBranded("Rules", "Compliance", RULES_TEXT, BANNER_MAIN, Emoji.fromUnicode("\uD83D\uDCDC"), rows);
    }

    public static Container accessDenied() { return error("Unauthorized", "You do not have the required permissions to access this section."); }
    public static Container error(String title, String description) { return containerBranded("Error", title, "[\u274C] " + description, BANNER_SUPPORT); }
    public static Container success(String title, String description) { return containerBranded("Success", title, "[\u2705] " + description, BANNER_MAIN); }
    public static Container info(String title, String description) { return containerBranded("Information", title, "[\u2139\uFE0F] " + description, BANNER_MAIN); }

    public static Container activityLog(String type, String details, Color color) { return containerBranded("System", type, details, null); }
    public static Container giveaway(String prize, int winners, int duration) {
        String body = "### \uD83C\uDF81 **New Giveaway!**\n" +
                "**Prize:** `" + prize + "`\n" +
                "**Winners:** `" + winners + "`\n" +
                "**Duration:** `" + duration + " minutes`\n\n" +
                "Click the button below to participate.";
        return containerBranded("Event", "Giveaway", body, BANNER_GIVEAWAY, Emoji.fromUnicode("\uD83C\uDF81"));
    }
}
