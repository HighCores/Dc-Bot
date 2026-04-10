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
    public static final Color ACCENT = Color.decode("#C5A059");
    public static final Color SUCCESS = Color.decode("#2ECC71");
    public static final Color DANGER = Color.decode("#E74C3C");
    public static final Color INFO = Color.decode("#3498DB");
    public static final Color WARNING = Color.decode("#F1C40F");
    public static final Color GOLD = Color.decode("#D4AF37");
    public static final Color ACCENT_GOLD = Color.decode("#FFD700");

    // ── Upload these to a permanent host (e.g. imgur, cdn.discordapp) ──────────
    // ibb.co blocks Discord hotlinking — set to null to disable banner until
    // re-hosted
    public static final String BANNER_MAIN = "https://media.discordapp.net/attachments/1488900668042510568/1491409986277085224/banner.png?ex=69da3a49&is=69d8e8c9&hm=0a9d640c8668b0f682e0ba27f2d9db7e35e46b34ddc50570dc7364d8ee0aca22&=&format=webp&quality=lossless&width=1572&height=884";
    public static final String BANNER_SUPPORT = "https://cdn.discordapp.com/attachments/1488900668042510568/1491803731312906261/7101585d-b014-4163-9016-a8eaffb0d27c.png?ex=69d9aebd&is=69d85d3d&hm=c2387d4dcc5123476d2a423cb6678755d7681532efff0da73f6adb0a0bd56de6&";
    public static final String BANNER_GIVEAWAY = null;
    public static final String BANNER_INVOICE = "https://cdn.discordapp.com/attachments/1488900668042510568/1491799713391837376/IMG_20260409_165917.png?ex=69d9aaff&is=69d8597f&hm=78cb60f4ec51bdc2baae65a16debbcee47bd108827815c2854a446bada209200&";
    public static final String BANNER_TICKET_PANEL = "https://cdn.discordapp.com/attachments/1488900668042510568/1491873673357824121/70b9423fa5bc68a7.png?ex=69d9efe1&is=69d89e61&hm=a3118a8daea8b3d1f354d539a59a2178c731ba9f73bb9672521009319a8eea33&";
    public static final String BANNER_ORDER_TIK = "https://cdn.discordapp.com/attachments/1488900668042510568/1491808487104057455/ORDER-.jpg?ex=69d9b32b&is=69d861ab&hm=c183c88688cafd019b038435193973560837cdb71fcfdbb5dd62d90d26d2e62e&";

    // ── Core builder — adds MediaGallery ONLY when imageUrl is non-null ───────
    public static Container eliteContainer(String title, String description, String imageUrl, ActionRow... rows) {
        List<ContainerChildComponent> children = new ArrayList<>();

        // Banner image (only if URL is provided and non-empty)
        if (imageUrl != null && !imageUrl.isBlank()) {
            children.add(MediaGallery.of(MediaGalleryItem.fromUrl(imageUrl)));
        }

        // Title + body
        String text = (description != null && !description.isEmpty())
                ? "## " + title + "\n" + description
                : "## " + title;
        children.add(TextDisplay.of(text));

        // Separator + buttons
        if (rows.length > 0) {
            children.add(Separator.createDivider(Spacing.SMALL));
            for (ActionRow row : rows)
                children.add(row);
        }

        return Container.of(children).withAccentColor(ACCENT.getRGB() & 0xFFFFFF);
    }

    // ── Branded overloads ─────────────────────────────────────────────────────
    public static Container containerBranded(String title, String subtitle, String body, String imageUrl) {
        return containerBranded(title, subtitle, body, imageUrl, (Emoji) null);
    }

    public static Container containerBranded(String title, String subtitle, String body, String imageUrl, Emoji emoji,
            ActionRow... rows) {
        String fullTitle = (subtitle == null || subtitle.isEmpty()) ? title : title + " | " + subtitle;
        return eliteContainer(fullTitle, body, imageUrl, rows);
    }

    public static Container containerBrandedRows(String title, String subtitle, String body, String imageUrl,
            ActionRow... rows) {
        return containerBranded(title, subtitle, body, imageUrl, (Emoji) null, rows);
    }

    // ── Colored containers (no banner, used for status messages) ─────────────
    private static Container coloredContainer(String title, String description, Color color) {
        List<ContainerChildComponent> children = new ArrayList<>();
        String text = (description != null && !description.isEmpty())
                ? "## " + title + "\n" + description
                : "## " + title;
        children.add(TextDisplay.of(text));
        return Container.of(children).withAccentColor(color.getRGB() & 0xFFFFFF);
    }

    public static Container success(String title, String description) {
        return coloredContainer(title, description, SUCCESS);
    }

    public static Container error(String title, String description) {
        return coloredContainer(title, description, DANGER);
    }

    public static Container info(String title, String description) {
        return coloredContainer(title, description, INFO);
    }

    public static Container accessDenied() {
        return coloredContainer("Access Denied", "Unauthorized.", DANGER);
    }

    // ── Activity log ──────────────────────────────────────────────────────────
    public static Container activityLog(String type, String details, Color color) {
        return Container.of(TextDisplay.of(details)).withAccentColor(color.getRGB() & 0xFFFFFF);
    }

    public static Container rulesPanel(ActionRow... rows) {
        return coloredContainer("Rules", "Professional guidelines.", ACCENT);
    }

    public static Container termsPanel(ActionRow... rows) {
        return coloredContainer("Terms", "Engagement protocols.", ACCENT);
    }

    // ── Giveaway ──────────────────────────────────────────────────────────────
    public static Container giveaway(String prize, int winners, int duration) {
        String body = "### \uD83C\uDF81 Active Giveaway\n" +
                "**Prize:** `" + prize + "`\n" +
                "**Winners:** `" + winners + "`\n" +
                "**Duration:** `" + duration + "m`";
        return eliteContainer("SWEEPSTAKES", body, BANNER_GIVEAWAY);
    }

    public static Container rulesEmbed() {
        return coloredContainer("Guidelines", null, ACCENT);
    }

    // ── Helper: set image after the object is built (for re-upload flows) ─────
    /**
     * Use this when you want a banner from a Discord attachment URL,
     * e.g. EmbedUtil.withImage(EmbedUtil.success(...), "attachment://banner.png")
     * This is the recommended approach — attach the file and reference it here.
     */
    public static Container eliteContainerWithImage(String title, String description, String imageUrl,
            ActionRow... rows) {
        return eliteContainer(title, description, imageUrl, rows);
    }
}
