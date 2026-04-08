package com.highcore.bot.utils;

import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.mediagallery.MediaGallery;
import net.dv8tion.jda.api.components.mediagallery.MediaGalleryItem;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import com.google.gson.JsonObject;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

public class EmbedUtil {

    public static final String BANNER_MAIN = "https://i.ibb.co/3ykfX5K/media-1775551414274.png";
    public static final String BANNER_SUPPORT = "https://i.ibb.co/3ykfX5K/media-1775551414274.png";
    public static final String BANNER_GIVEAWAY = "https://i.ibb.co/3ykfX5K/media-1775551414274.png";
    
    public static final Color SUCCESS = Color.decode("#D4AF37"); // Metallic Gold
    public static final Color DANGER = Color.decode("#8B0000");  // Deep Red for Danger
    public static final Color WARNING = Color.decode("#FFD700"); // Bright Gold
    public static final Color INFO = Color.decode("#C0C0C0");    // Silver for Info
    public static final Color GOLD = Color.decode("#D4AF37");
    public static final Color ACCENT_GOLD = Color.decode("#FFD700");

    public static final String RULES_TEXT = """
            ## \uD83D\uDCCB Haikore Agency Rules
            
            ### 🛡️ 1. General Rules
            1. **Mutual Respect**: Harassment, bullying, or offensive language is strictly prohibited.
            2. **Professionalism**: Use clear nicknames and avoid inappropriate profile pictures.
            3. **Privacy**: Never share personal information (Doxxing) of any member or client.
            
            ### \uD83D\uDCBC 2. Business & Service Rules
            1. **Serious Projects**: Order channels are for work only. Mockery or fake orders may lead to restrictions.
            2. **Copyright**: All development and design rights belong to Haikore Agency unless agreed otherwise.
            3. **Official Channels**: Financial or technical agreements must go through Tickets.
            
            ### \uD83D\uDEAB 3. Prohibitions
            1. **No Ads**: No promotion of external servers or services without permission.
            2. **No Spam**: Avoid excessive mentions or message repetition.
            3. **Content**: No political, religious extremism, or NSFW content.
            """;

    public static Container containerBranded(String category, String topic, String body, String imageUrl) {
        return containerBranded(category, topic, body, imageUrl, null);
    }

    public static Container containerBranded(String category, String topic, String body, String imageUrl, Emoji iconEmoji, ActionRow... rows) {
        List<ContainerChildComponent> layout = new ArrayList<>();
        
        if (imageUrl != null && !imageUrl.isEmpty()) {
            layout.add(MediaGallery.of(MediaGalleryItem.fromUrl(imageUrl)));
        }

        layout.add(TextDisplay.of("### \u25C8 " + category.toUpperCase() + " \u30FB " + topic.toUpperCase()));
        layout.add(Separator.createDivider(Separator.Spacing.SMALL));

        String content = (iconEmoji != null ? iconEmoji.getFormatted() + " " : "") + body;
        layout.add(TextDisplay.of(content));

        if (rows != null && rows.length > 0) {
            layout.add(Separator.createDivider(Separator.Spacing.SMALL));
            layout.addAll(Arrays.asList(rows));
            layout.add(Separator.createDivider(Separator.Spacing.SMALL));
        }

        layout.add(TextDisplay.of("` \u2022 Haikore Agency Unified System \u2022 v1.2.0 \u2022 `"));

        return Container.of(layout);
    }

    public static Container startupPanel(ActionRow... rows) {
        String body = "Welcome to Haikore Agency.\n" +
                "We provide professional digital solutions in Design, Coding, and Minecraft services.\n\n" +
                "Please use the menu below to navigate our services.";
        return containerBranded("Main", "Dashboard", body, BANNER_MAIN, Emoji.fromUnicode("\uD83D\uDE80"), rows);
    }

    public static Container rulesPanel() {
        return containerBranded("Rules", "Compliance", RULES_TEXT, BANNER_MAIN, Emoji.fromUnicode("\uD83D\uDCDC"));
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
