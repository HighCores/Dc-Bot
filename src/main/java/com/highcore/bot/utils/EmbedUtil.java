package com.highcore.bot.utils;

import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.components.mediagallery.MediaGallery;
import net.dv8tion.jda.api.components.mediagallery.MediaGalleryItem;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.separator.Separator.Spacing;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class EmbedUtil {
    public static final Color ACCENT      = Color.decode("#C5A059");
    public static final Color SUCCESS     = Color.decode("#2ECC71");
    public static final Color DANGER      = Color.decode("#E74C3C");
    public static final Color INFO        = Color.decode("#3498DB");
    public static final Color WARNING     = Color.decode("#F1C40F");
    public static final Color GOLD        = Color.decode("#D4AF37");
    public static final Color ACCENT_GOLD = Color.decode("#FFD700");

    public static final String BANNER_MAIN        = "https://i.ibb.co/3ykpY60W/Untitled-1.png";
    public static final String BANNER_SUPPORT     = "https://i.ibb.co/v4mK9Wf1/Untitled-1.png";
    public static final String BANNER_GIVEAWAY    = "https://i.ibb.co/6RTPXvS2/Untitled-1.png";
    public static final String BANNER_INVOICE       = "https://cdn.discordapp.com/attachments/1488900668042510568/1491799713391837376/IMG_20260409_165917.png?ex=69dafc7f&is=69d9aaff&hm=b344d8ceae8572e0ab8a972b9ec4ca7b60be6ad87314bac903c8a2a9a643629a&";
    public static final String BANNER_ORDER_TICKET  = "https://cdn.discordapp.com/attachments/1488900668042510568/1491808487104057455/ORDER-.jpg?ex=69db04ab&is=69d9b32b&hm=4f001336ef9b5ae1c96a2e5083e53e726197782920ce8b7af5f0c22d2ac8ddbf&";
    public static final String BANNER_TICKET_PANEL = BANNER_MAIN;
    public static final String BANNER_ORDER_TIK   = BANNER_INVOICE;

    // ── Core builder: image + text + separator + buttons ─────────────────────
    public static Container eliteContainer(String title, String description, String imageUrl, ActionRow... rows) {
        List<ContainerChildComponent> children = new ArrayList<>();

        if (imageUrl == null || imageUrl.isBlank()) imageUrl = BANNER_MAIN;
        children.add(MediaGallery.of(MediaGalleryItem.fromUrl(imageUrl)));

        String text = (description != null && !description.isEmpty())
                ? "## " + title + "\n" + description
                : "## " + title;
        children.add(TextDisplay.of(text));

        if (rows.length > 0) {
            children.add(Separator.createDivider(Spacing.SMALL));
            for (ActionRow row : rows) children.add(row);
        }

        // NO withAccentColor — pure Component V2 dark container
        return Container.of(children);
    }

    // ── Branded overloads ─────────────────────────────────────────────────────
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

    // ── Status containers (no image) ──────────────────────────────────────────
    private static Container coloredContainer(String title, String description) {
        List<ContainerChildComponent> children = new ArrayList<>();
        String text = (description != null && !description.isEmpty())
                ? "## " + title + "\n" + description
                : "## " + title;
        children.add(TextDisplay.of(text));
        return Container.of(children);
    }

    public static Container success(String title, String description)  { return coloredContainer(title, description); }
    public static Container error(String title, String description)    { return coloredContainer(title, description); }
    public static Container info(String title, String description)     { return coloredContainer(title, description); }
    public static Container accessDenied()                             { return coloredContainer("Access Denied", "You are not authorized to perform this action."); }

    // ── Activity log ──────────────────────────────────────────────────────────
    public static Container activityLog(String type, String details, Color color) {
        return Container.of(TextDisplay.of(details));
    }

    public static Container rulesPanel(ActionRow... rows)  { return coloredContainer("Rules", "Professional guidelines."); }
    public static Container termsPanel(ActionRow... rows)  { return coloredContainer("Terms", "Engagement protocols."); }
    public static Container rulesEmbed()                   { return coloredContainer("Guidelines", null); }

    // ── Giveaway ──────────────────────────────────────────────────────────────
    public static Container giveaway(String prize, int winners, int duration) {
        String body = "### \uD83C\uDF81 Active Giveaway\n" +
                "**Prize:** `" + prize + "`\n" +
                "**Winners:** `" + winners + "`\n" +
                "**Duration:** `" + duration + "m`";
        return eliteContainer("SWEEPSTAKES", body, BANNER_GIVEAWAY);
    }

    public static Container eliteContainerWithImage(String title, String description, String imageUrl, ActionRow... rows) {
        return eliteContainer(title, description, imageUrl, rows);
    }
}
