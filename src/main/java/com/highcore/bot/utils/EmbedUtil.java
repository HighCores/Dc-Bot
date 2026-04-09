package com.highcore.bot.utils;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
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
    public static final Color SUCCESS = Color.decode("#D4AF37");
    public static final Color DANGER = Color.decode("#8B0000");
    public static final Color INFO = Color.decode("#C0C0C0");
    public static final Color WARNING = Color.decode("#FFD700");
    public static final Color GOLD = Color.decode("#D4AF37");
    public static final Color ACCENT_GOLD = Color.decode("#FFD700");

    public static MessageEmbed eliteContainer(String title, String description, String imageUrl, ActionRow... rows) {
        EmbedBuilder eb = new EmbedBuilder().setColor(ACCENT);
        if (title != null && !title.isEmpty()) eb.setTitle(title);
        if (description != null && !description.isEmpty()) eb.setDescription(description);
        if (imageUrl != null && !imageUrl.isEmpty()) eb.setImage(imageUrl);
        return eb.build();
    }

    // --- Core Legacy Suite ---

    public static MessageEmbed giveaway(String prize, int winners, int duration) {
        String body = "Prize: **" + prize + "**\nWinners: **" + winners + "**\nDuration: **" + duration + "m**";
        return eliteContainer("New Giveaway", body, BANNER_GIVEAWAY);
    }

    public static MessageEmbed containerBranded(String title, String subtitle, String body, String imageUrl) {
        return containerBranded(title, subtitle, body, imageUrl, null);
    }

    public static MessageEmbed containerBranded(String title, String subtitle, String body, String imageUrl, Emoji emoji, ActionRow... rows) {
        String fullTitle = (subtitle == null || subtitle.isEmpty()) ? title : title + " | " + subtitle;
        return eliteContainer(fullTitle, body, imageUrl, rows);
    }

    public static MessageEmbed success(String title, String description) { return eliteContainer("Success: " + title, description, null); }
    public static MessageEmbed error(String title, String description) { return eliteContainer("Error: " + title, description, null); }
    public static MessageEmbed info(String title, String description) { return eliteContainer("Info: " + title, description, null); }
    public static MessageEmbed accessDenied() { return eliteContainer("Access Denied", "Unauthorized attempt.", null); }
    public static MessageEmbed activityLog(String type, String details, Color color) { return eliteContainer("Log: " + type, details, null); }

    public static MessageEmbed rulesPanel(ActionRow... rows) { return eliteContainer("Rules", "Professional guidelines.", null, rows); }
    public static MessageEmbed termsPanel(ActionRow... rows) { return eliteContainer("Terms", "Engagement protocols.", null, rows); }
    public static MessageEmbed startupPanel(ActionRow... rows) { return eliteContainer("High Core", "Ready.", null, rows); }

    public static MessageEmbed rulesEmbed() { return new EmbedBuilder().setTitle("Guidelines").setColor(ACCENT).build(); }
}
