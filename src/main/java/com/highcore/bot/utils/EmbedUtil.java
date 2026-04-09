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

    public static net.dv8tion.jda.api.components.container.Container eliteContainer(String title, String description, String imageUrl, ActionRow... rows) {
        List<ContainerChildComponent> layout = new ArrayList<>();
        if (title != null && !title.isEmpty()) layout.add(TextDisplay.of("### " + title));
        if (description != null && !description.isEmpty()) layout.add(TextDisplay.of(description));
        if (rows != null) for (ActionRow row : rows) if (row != null) layout.add(row);
        return Container.of(layout);
    }

    // --- Core Legacy Suite ---

    public static net.dv8tion.jda.api.components.container.Container giveaway(String prize, int winners, int duration) {
        String body = "Prize: **" + prize + "**\nWinners: **" + winners + "**\nDuration: **" + duration + "m**";
        return eliteContainer("New Giveaway", body, BANNER_GIVEAWAY);
    }

    public static net.dv8tion.jda.api.components.container.Container containerBranded(String title, String subtitle, String body, String imageUrl) {
        return containerBranded(title, subtitle, body, imageUrl, null);
    }

    public static net.dv8tion.jda.api.components.container.Container containerBranded(String title, String subtitle, String body, String imageUrl, Emoji emoji, ActionRow... rows) {
        String fullTitle = (subtitle == null || subtitle.isEmpty()) ? title : title + " | " + subtitle;
        return eliteContainer(fullTitle, body, null, rows);
    }

    public static net.dv8tion.jda.api.components.container.Container success(String title, String description) { return eliteContainer("Success: " + title, description, null); }
    public static net.dv8tion.jda.api.components.container.Container error(String title, String description) { return eliteContainer("Error: " + title, description, null); }
    public static net.dv8tion.jda.api.components.container.Container info(String title, String description) { return eliteContainer("Info: " + title, description, null); }
    public static net.dv8tion.jda.api.components.container.Container accessDenied() { return eliteContainer("Access Denied", "Unauthorized attempt.", null); }
    public static net.dv8tion.jda.api.components.container.Container activityLog(String type, String details, Color color) { return eliteContainer("Log: " + type, details, null); }

    public static net.dv8tion.jda.api.components.container.Container rulesPanel(ActionRow... rows) { return eliteContainer("Rules", "Professional guidelines.", null, rows); }
    public static net.dv8tion.jda.api.components.container.Container termsPanel(ActionRow... rows) { return eliteContainer("Terms", "Engagement protocols.", null, rows); }
    public static net.dv8tion.jda.api.components.container.Container startupPanel(ActionRow... rows) { return eliteContainer("High Core", "Ready.", null, rows); }

    public static MessageEmbed rulesEmbed() { return new EmbedBuilder().setTitle("Guidelines").setColor(ACCENT).build(); }
}
