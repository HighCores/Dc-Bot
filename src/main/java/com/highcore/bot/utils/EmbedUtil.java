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
    public static final String BANNER_MAP = BANNER_MAIN = "https://cdn.discordapp.com/attachments/1488795131296354460/1491333834577281117/096a0e39-3e6f-4c0c-88a8-16dbe65d67b9.png?ex=69d7505d&is=69d5fedd&hm=623ca264421546f78fc5548d1639739445201523e7ca9e5248ddca05c195260a&";
    public static final String BANNER_PINGS = BANNER_MAIN = "https://cdn.discordapp.com/attachments/1488795131296354460/1491334697438150676/d2dfda42-f703-4949-bb18-130c6a3740cf.png?ex=69d7512b&is=69d5ffab&hm=e98a31b2ee16ef0f1452982b677ce0c6c87ff7dea9d86edafca2ea02715bf1a1&"
    public static final String BANNER_COLORS = BANNER_MAIN = "https://cdn.discordapp.com/attachments/1488795131296354460/1491334863029276722/45269a83-132c-4f4b-a693-ea261ddad457.png?ex=69d75153&is=69d5ffd3&hm=13e655a2b0c1dced8e9a72b311351b32b55088a5bb406317f79e88b2e4f77527&"
    public static final String BANNER_RULES = BANNER_MAIN = "https://cdn.discordapp.com/attachments/1488795131296354460/1491335141874733127/07c22c1b-bb01-4415-8e27-0b96fafdd919.png?ex=69d75195&is=69d60015&hm=174c290a487d8d5485b1c13a38c2d9085978907b89d88baf4368d76f60f73b77&"
    public static final String BANNER_SOCIAL = BANNER_MAIN = "https://cdn.discordapp.com/attachments/1488795131296354460/1491335292504772619/914b0747-24ce-4b42-b257-38b3b8a4bece.png?ex=69d751b9&is=69d60039&hm=f36702400869f38e60a7156ea955bb6358421146bbfd3bfa9c5aa031967e5dd1&"
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

    public static Container containerBranded(String title, String subtitle, String body, String imageUrl, Emoji emoji,
            ActionRow... rows) {
        List<ContainerChildComponent> layout = new ArrayList<>();

        // 1. BRANDING BANNER (V2 COMPLIANT)
        if (imageUrl != null) {
            layout.add(MediaGallery.of(MediaGalleryItem.fromUrl(imageUrl)));
        }

        // 2. ELITE HEADER
        layout.add(TextDisplay.of("**\u25C8 " + title.toUpperCase() + " \u30FB " + subtitle.toUpperCase() + "**"));
        layout.add(Separator.createDivider(Separator.Spacing.SMALL));

        // 3. TECHNICAL BODY
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
        return containerBranded("Rules", "Compliance", RULES_TEXT, BANNER_MAIN, Emoji.fromUnicode("\uD83D\uDCDC"),
                rows);
    }

    public static Container accessDenied() {
        return error("Unauthorized", "You do not have the required permissions to access this section.");
    }

    public static Container error(String title, String description) {
        return containerBranded("Error", title, "[\u274C] " + description, BANNER_SUPPORT);
    }

    public static Container success(String title, String description) {
        return containerBranded("Success", title, "[\u2705] " + description, BANNER_MAIN);
    }

    public static Container info(String title, String description) {
        return containerBranded("Information", title, "[\u2139\uFE0F] " + description, BANNER_MAIN);
    }

    public static Container activityLog(String type, String details, Color color) {
        return containerBranded("System", type, details, null);
    }

    public static Container giveaway(String prize, int winners, int duration) {
        String body = "### \uD83C\uDF81 **New Giveaway!**\n" +
                "**Prize:** `" + prize + "`\n" +
                "**Winners:** `" + winners + "`\n" +
                "**Duration:** `" + duration + " minutes`\n\n" +
                "Click the button below to participate.";
        return containerBranded("Event", "Giveaway", body, BANNER_GIVEAWAY, Emoji.fromUnicode("\uD83C\uDF81"));
    }
}
