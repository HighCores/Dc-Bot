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

    public static final String BANNER_MAIN = "https://images.unsplash.com/photo-1614850523296-d8c1af93d400?q=80&w=2070&auto=format&fit=crop";
    public static final String BANNER_SUPPORT = "https://images.unsplash.com/photo-1518770660439-4636190af475?q=80&w=2070&auto=format&fit=crop";
    public static final String BANNER_GIVEAWAY = "https://images.unsplash.com/photo-1513151233558-d860c5398176?q=80&w=2070&auto=format&fit=crop";
    public static final String BANNER_INVOICE    = "https://cdn.discordapp.com/attachments/1488900668042510568/1491799713391837376/IMG_20260409_165917.png?ex=69dafc7f&is=69d9aaff&hm=b344d8ceae8572e0ab8a972b9ec4ca7b60be6ad87314bac903c8a2a9a643629a&";
    public static final String BANNER_ORDER_TICKET = "https://cdn.discordapp.com/attachments/1488900668042510568/1491808487104057455/ORDER-.jpg?ex=69db04ab&is=69d9b32b&hm=4f001336ef9b5ae1c96a2e5083e53e726197782920ce8b7af5f0c22d2ac8ddbf&";

    public static final Color ACCENT_TEAL = Color.decode("#14b8a6");

    // ── Core builder: image + text + separator + buttons ─────────────────────
    public static Container eliteContainer(String title, String description, String imageUrl, ActionRow... rows) {
        List<ContainerChildComponent> children = new ArrayList<>();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            children.add(MediaGallery.of(MediaGalleryItem.fromUrl(imageUrl)));
        }
        children.add(TextDisplay.of("## " + title + "\n" + (description != null ? description : "")));
        if (rows != null && rows.length > 0) {
            children.add(Separator.createDivider(Spacing.SMALL));
            for (ActionRow row : rows) children.add(row);
        }
        return Container.of(children);
    }

    public static Container containerBranded(String sector, String title, String body, String imageUrl) {
        return containerBranded(sector, title, body, imageUrl, null);
    }

    public static Container containerBranded(String sector, String title, String body, String imageUrl, Emoji iconEmoji,
            ActionRow... rows) {
        List<ContainerChildComponent> layout = new ArrayList<>();

        if (imageUrl != null && !imageUrl.isEmpty()) {
            layout.add(MediaGallery.of(MediaGalleryItem.fromUrl(imageUrl)));
        }

        layout.add(TextDisplay.of("### \u25C8 " + sector.toUpperCase() + " SECTOR \u30FB " + title.toUpperCase()));
        layout.add(Separator.createDivider(Separator.Spacing.SMALL));

        String content = (iconEmoji != null ? iconEmoji.getFormatted() + " " : "") + body;
        layout.add(TextDisplay.of(content));

        if (rows != null && rows.length > 0) {
            layout.add(Separator.createDivider(Separator.Spacing.SMALL));
            for (ActionRow row : rows) if (row != null) layout.add(row);
        }

        return Container.of(layout);
    }

    public static Container mainMenu(ActionRow... rows) {
        String body = """
                Welcome Operative. Access restricted agency sectors via control modules.

                \u25CF **Tickets** \u2014 Secure Support Channels
                \u25CF **Services** \u2014 Agency Projects
                \u25CF **Merit Hub** \u2014 Identity Audit
                """;
        return containerBranded("CENTER", "Main Control Node", body, BANNER_MAIN, Emoji.fromUnicode("\uD83D\uDCE1"),
                rows);
    }

    public static Container accessDenied() {
        return error("ACCESS RESTRICTED", "Credentials insufficient for this terminal node.");
    }

    public static Container giveawayPanel() {
        return containerBranded("OPS", "Active Sweepstakes", "Initialize participation in agency giveaways below.",
                BANNER_GIVEAWAY, Emoji.fromUnicode("\uD83C\uDF81"));
    }

    public static Container help() {
        return containerBranded("SYSTEM", "Terminal Intel", "Comprehensive command listing and protocol documentation.",
                BANNER_MAIN, Emoji.fromUnicode("\u2705"));
    }

    public static Container activityLog(String type, String details, Color color) {
        return containerBranded("activity", type, details, BANNER_MAIN).withAccentColor(color.getRGB() & 0xFFFFFF);
    }

    public static Container ticketClosed(String id, String user) {
        return containerBranded("session", "Closed", "Case #" + id + " archived by " + user, BANNER_SUPPORT);
    }

    public static Container ratingThanks(int r) {
        return containerBranded("LOG", "Review Saved", "Rating: " + r + "/5 \u2605. Thank you.", BANNER_MAIN);
    }

    public static Container paymentGateway(String m, String a) {
        return containerBranded("PAYS", "Invoice Init", "Method: " + m + " | Amount: $" + a, BANNER_MAIN);
    }

    public static Container serverMap() {
        return containerBranded("INFO", "Node Map", "Agency Geographic Nodes & Connectivity.", BANNER_MAIN);
    }

    public static Container socialMedia() {
        return containerBranded("INFO", "Agency Feeds", "Follow Highcore on digital channels.", BANNER_MAIN);
    }

    public static Container warning(String t, String d) {
        return containerBranded("WARN", t, "[!] " + d, BANNER_SUPPORT).withAccentColor(WARNING.getRGB() & 0xFFFFFF);
    }

    public static Container assistantResponse(String r) {
        return containerBranded("ASSIST", "AI Transmission", r, BANNER_MAIN);
    }

    public static Container ticketPanel(ActionRow... rows) {
        return containerBranded("LOGISTICS", "Terminal Access",
                "Initialize project session via selection modules below.", BANNER_SUPPORT,
                Emoji.fromUnicode("\uD83D\uDCC4"), rows);
    }

    public static Container startupPanel(ActionRow... rows) {
        return containerBranded("INIT", "Onboarding Sequence",
                "Highcore Agency delivers high-fidelity digital solutions.\nDeploy modules to begin.", BANNER_MAIN,
                Emoji.fromUnicode("\uD83D\uDE80"), rows);
    }

    public static Container error(String title, String description) {
        return containerBranded("ERROR", title, "[\u274C] " + description, BANNER_SUPPORT)
                .withAccentColor(DANGER.getRGB() & 0xFFFFFF);
    }

    public static Container success(String title, String description) {
        return containerBranded("SUCCESS", title, "[\u2705] " + description, BANNER_MAIN)
                .withAccentColor(SUCCESS.getRGB() & 0xFFFFFF);
    }

    public static Container info(String title, String description) {
        return containerBranded("INFO", title, "[\u2139\uFE0F] " + description, BANNER_MAIN);
    }

    public static Container services() {
        return containerBranded("OPS", "Capability Directory", "Explore Design & Development.", BANNER_MAIN);
    }

    public static Container rulePanel() {
        return containerBranded("PROTOCOL", "Compliance", "Professionalism mandatory.", BANNER_MAIN);
    }

    public static Container pointsPanel() {
        return containerBranded("SYSTEM", "Merit Registry", "Examine standing.", BANNER_MAIN);
    }

    public static Container stats(int t, int o, int c, int cl, String s) {
        return containerBranded("STATS", "Network", "Active Nodes: " + o, BANNER_MAIN);
    }

    public static Container meritAudit(int points) {
        return containerBranded("AUDIT", "Identity Summary", "Merit: **" + points + "**", BANNER_MAIN);
    }

    public static Container ticketHeader(String tid, String u, String ty, String b) {
        return containerBranded("SESSION", "Case #" + tid, "**Client:** " + u + "\n" + b, BANNER_SUPPORT);
    }

    public static Container staffAssigned(String n) {
        return containerBranded("NOTICE", "Agent Assigned", "Operative **" + n + "** claimed.", BANNER_SUPPORT);
    }

    public static TextDisplay v2Header(String category, String title) {
        return TextDisplay.of("### \u25BA " + category.toUpperCase() + " \u30FB " + title);
    }

    public static TextDisplay v2Footer() {
        return TextDisplay.of("` \u2022 UNIFIED TERMINAL v2.2 \u2022 `");
    }

    public static final Color PRIMARY = Color.decode("#10b981");

    public static Color parseColor(String colorStr) {
        if (colorStr == null)
            return ACCENT_TEAL;
        try {
            return Color.decode(colorStr.startsWith("#") ? colorStr : "#" + colorStr);
        } catch (Exception e) {
            return ACCENT_TEAL;
        }
    }

    public static Container containerBrandedRows(String title, String subtitle, String body, String imageUrl, ActionRow... rows) {
        return containerBranded(title, subtitle, body, imageUrl, (Emoji) null, rows);
    }

    public static Container custom(String category, String title, String body, String imageUrl, String thumbnail,
            String author, String authorUrl, String footer, String footerIcon, String field1Title, String field1Value,
            Boolean field1Inline, String field2Title, String field2Value, Boolean field2Inline, String field3Title,
            String field3Value, Boolean field3Inline) {
        return containerBranded(category, title, body, imageUrl);
    }
}
