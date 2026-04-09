package com.highcore.bot.utils;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import java.awt.Color;

public class EmbedUtil {
    public static final Color ACCENT = Color.decode("#C5A059");
    public static final Color SUCCESS = Color.decode("#2ECC71");
    public static final Color DANGER = Color.decode("#E74C3C");
    public static final Color INFO = Color.decode("#3498DB");
    public static final Color WARNING = Color.decode("#F1C40F");
    
    public static final String BANNER_MAIN = "https://i.ibb.co/3ykpY60W/Untitled-1.png";
    public static final String BANNER_SUPPORT = "https://i.ibb.co/v4mK9Wf1/Untitled-1.png";
    public static final String BANNER_GIVEAWAY = "https://i.ibb.co/6RTPXvS2/Untitled-1.png";
    public static final String BANNER_ORDER_TIK = "https://i.ibb.co/C5XBr7mY/Untitled-1.png";
    public static final String BANNER_INVOICE = "https://i.ibb.co/5Xm8M89V/Untitled-1.png";

    public static Container eliteContainer(String title, String description, String imageUrl, ActionRow... rows) {
        if (imageUrl == null) imageUrl = BANNER_MAIN;
        return Container.create()
                .withTitle(title)
                .withAccentColor(ACCENT.getRGB() & 0xFFFFFF)
                .withMedia(imageUrl)
                .addChildren(TextDisplay.of(description))
                .addChildren(rows)
                .build();
    }

    public static Container containerBranded(String title, String subtitle, String body, String imageUrl) {
        return containerBranded(title, subtitle, body, imageUrl, (Emoji) null);
    }

    public static Container containerBranded(String title, String subtitle, String body, String imageUrl, Emoji emoji, ActionRow... rows) {
        String fullTitle = (subtitle == null || subtitle.isEmpty()) ? title : title + " | " + subtitle;
        return eliteContainer(fullTitle, body, imageUrl, rows);
    }
    
    public static Container containerBrandedRows(String title, String subtitle, String body, String imageUrl, ActionRow... rows) {
        return containerBranded(title, subtitle, body, imageUrl, (Emoji) null, rows);
    }

    public static MessageEmbed success(String title, String description) { return new EmbedBuilder().setTitle(title).setDescription(description).setColor(SUCCESS).build(); }
    public static MessageEmbed error(String title, String description) { return new EmbedBuilder().setTitle(title).setDescription(description).setColor(DANGER).build(); }
    public static MessageEmbed info(String title, String description) { return new EmbedBuilder().setTitle(title).setDescription(description).setColor(INFO).build(); }
    public static MessageEmbed accessDenied() { return new EmbedBuilder().setTitle("Access Denied").setDescription("Unauthorized attempt.").setColor(DANGER).build(); }
    public static MessageEmbed activityLog(String type, String details, Color color) { return new EmbedBuilder().setTitle("Log: " + type).setDescription(details).setColor(color).build(); }

    public static MessageEmbed rulesPanel(ActionRow... rows) { return new EmbedBuilder().setTitle("Rules").setDescription("Professional guidelines.").setColor(ACCENT).build(); }
    public static MessageEmbed termsPanel(ActionRow... rows) { return new EmbedBuilder().setTitle("Terms").setDescription("Engagement protocols.").setColor(ACCENT).build(); }

    public static Container giveaway(String prize, int winners, int duration) {
        String body = "### \uD83C\uDF81 Active Distribution\n" +
                "**Prize:** `" + prize + "`\n" +
                "**Winners:** `" + winners + "`\n" +
                "**Duration:** `" + duration + "m`";
        return eliteContainer("SWEEPSTAKES", body, BANNER_GIVEAWAY);
    }

    public static MessageEmbed rulesEmbed() { return new EmbedBuilder().setTitle("Guidelines").setColor(ACCENT).build(); }
}
