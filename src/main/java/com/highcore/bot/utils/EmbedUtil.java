package com.highcore.bot.utils;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.mediagallery.MediaGallery;
import net.dv8tion.jda.api.components.mediagallery.MediaGalleryItem;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class EmbedUtil {

    public static final String BANNER_MAIN = "https://cdn.discordapp.com/attachments/1488795131296354460/1491194673048457399/banner.png";
    public static final String BANNER_SUPPORT = "https://cdn.discordapp.com/attachments/1491791586479177768/1491810902243148027/IMG_20260409_174341.png";
    public static final String BANNER_INVOICE = "https://cdn.discordapp.com/attachments/1488900668042510568/1491799713391837376/IMG_20260409_165917.png";
    public static final String BANNER_ORDER_TIK = "https://cdn.discordapp.com/attachments/1488900668042510568/1491808487104057455/ORDER-.jpg";
    public static final String BANNER_GIVEAWAY = BANNER_MAIN;
    public static final String BANNER_RULES = BANNER_MAIN;

    public static final Color ACCENT = Color.decode("#D4AF37");

    /**
     * eliteContainer: The premium interface for High Core Agency.
     * Uses Title Case (Example) and clean typography.
     */
    public static Container eliteContainer(String title, String description, String imageUrl, ActionRow... rows) {
        List<ContainerChildComponent> layout = new ArrayList<>();

        if (imageUrl != null && !imageUrl.isEmpty()) {
            layout.add(MediaGallery.of(MediaGalleryItem.fromUrl(imageUrl)));
        }

        if (title != null && !title.isEmpty()) {
            layout.add(TextDisplay.of("### " + title));
        }

        if (description != null && !description.isEmpty()) {
            layout.add(TextDisplay.of(description));
        }

        if (rows != null) {
            for (ActionRow row : rows) {
                if (row != null) layout.add(row);
            }
        }

        return Container.of(layout);
    }

    public static Container simpleContainer(String title, String body, String imageUrl) {
        return eliteContainer(title, body, imageUrl);
    }

    public static MessageEmbed rulesEmbed() {
        return new EmbedBuilder()
            .setTitle("Community Guidelines")
            .setDescription("1. Mutual Respect\n2. Professionalism\n3. Business through Tickets only.\n4. No advertisements.")
            .setColor(ACCENT)
            .setImage(BANNER_RULES)
            .build();
    }
}
