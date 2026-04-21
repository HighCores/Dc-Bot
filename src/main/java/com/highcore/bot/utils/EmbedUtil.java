package com.highcore.bot.utils;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.UserSnowflake;
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
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class EmbedUtil {
    public static final Color ACCENT = Color.decode("#C5A059");
    public static final Color SUCCESS = Color.decode("#10b981");
    public static final Color DANGER = Color.decode("#f43f5e");
    public static final Color INFO = Color.decode("#3b82f6");
    public static final Color WARNING = Color.decode("#f59e0b");
    public static final Color GOLD = Color.decode("#fbbf24");
    public static final Color ACCENT_GOLD = Color.decode("#FFD700");
    public static final Color ACCENT_TEAL = Color.decode("#14b8a6");
    public static final Color PRIMARY = Color.decode("#35423E");

    // User requested to migrate to Imgur to avoid Discord CDN expiry links.
    // Replace the "PLACEHOLDER" parts below with real Imgur IDs.
    public static final String BANNER_MAIN = "https://i.imgur.com/lOQt0gJ.png";
    public static final String BANNER_WELCOME = "https://i.imgur.com/Lzun3rb.png";
    public static final String BANNER_GIVEAWAY = BANNER_MAIN;
    
    // Support category banners
    public static final String BANNER_SUPPORT = "https://i.imgur.com/MBU5wvl.png";
    public static final String BANNER_COMPLAINT = "https://i.imgur.com/t7Prrsr.png";
    public static final String BANNER_TICKETS_MENU = "https://i.imgur.com/wllO63d.png";
    public static final String BANNER_ORDER_TICKET = "https://i.imgur.com/OHF6qJB.png";
    public static final String BANNER_INVOICE = "https://i.imgur.com/OHF6qJB.png";
    public static final String BANNER_INVOICE_PAID = "https://i.imgur.com/OHF6qJB.png";
    
    // Order category banners
    public static final String BANNER_DESIGN = "https://i.imgur.com/sHZzmVi.png";
    public static final String BANNER_DEVELOPER = "https://i.imgur.com/rX2oXzt.png";
    public static final String BANNER_MINECRAFT = "https://i.imgur.com/1TKfy9i.png";
    public static final String BANNER_EDITOR = "https://i.imgur.com/R4126YU.png";

    public static String getCategoryBanner(String cat) {
        if (cat == null) return BANNER_MAIN;
        return switch (cat.toLowerCase()) {
            case "designer"  -> BANNER_DESIGN;
            case "developer" -> BANNER_DEVELOPER;
            case "minecraft" -> BANNER_MINECRAFT;
            case "editor"    -> BANNER_EDITOR;
            default -> BANNER_MAIN;
        };
    }

    public static Container eliteContainer(String title, String description, String imageUrl, ActionRow... rows) {
        return containerBranded("", title, description, imageUrl, rows);
    }

    public static Container containerBranded(String sector, String title, String body, String imageUrl,
            ActionRow... rows) {
        return containerBranded(sector, title, body, imageUrl, null, rows);
    }

    public static Container containerBranded(String sector, String title, String body, String imageUrl, Emoji iconEmoji,
            ActionRow... rows) {
        List<ContainerChildComponent> layout = new ArrayList<>();

        if (imageUrl != null && !imageUrl.isEmpty()) {
            layout.add(MediaGallery.of(MediaGalleryItem.fromUrl(imageUrl)));
        }

        StringBuilder text = new StringBuilder();
        if (sector != null && !sector.isEmpty()) {
            text.append("### ► ").append(sector.toUpperCase()).append(" ・ ").append(title).append("\n");
        } else {
            text.append("## ").append(title).append("\n");
        }

        if (body != null && !body.isEmpty()) {
            text.append(body);
        }
        layout.add(TextDisplay.of(text.toString()));

        if (rows != null && rows.length > 0) {
            layout.add(Separator.createDivider(Spacing.SMALL));
            for (ActionRow row : rows) {
                layout.add(row);
            }
        }

        return Container.of(layout);
    }

    public static MessageEmbed brandedEmbed(String title, String description) {
        return new EmbedBuilder()
                .setTitle(title)
                .setDescription(description)
                .setColor(ACCENT)
                .build();
    }

    public static Container brandedNotice(String title, String description) {
        List<ContainerChildComponent> layout = new ArrayList<>();
        layout.add(TextDisplay.of("### " + title + "\n" + description));
        return Container.of(layout);
    }

    public static Container mainMenu(ActionRow... rows) {
        String body = """
                Welcome Operative. Access restricted agency sectors via control modules.

                ● **Tickets** — Secure Support Channels
                ● **Services** — Agency Projects
                ● **Merit Hub** — Identity Audit
                """;
        return containerBranded("CENTER", "Main Control Node", body, BANNER_MAIN, Emoji.fromUnicode("📡"), rows);
    }

    public static Container startupPanel(ActionRow... rows) {
        List<ContainerChildComponent> layout = new ArrayList<>();
        layout.add(MediaGallery.of(MediaGalleryItem.fromUrl(BANNER_WELCOME)));

        layout.add(TextDisplay.of("**Highcore Agency**"));
        layout.add(Separator.createDivider(Spacing.SMALL));

        layout.add(TextDisplay.of(
                "**PROFESSIONAL MULTI-SECTOR AGENCY OPERATIONS**\n\nEstablishing the definitive global standard for advanced digital infrastructure and elite creative operations. Highcore provides high-fidelity solutions across system development, visual architecture, and strategic media management.\n\nOur internal systems are engineered for absolute brand dominance and superior technological precision. We merge artistic vision with technical mastery to deliver the unlimited potential of the agency directly to our global partners."));

        layout.add(Separator.createDivider(Spacing.SMALL));

        layout.add(TextDisplay.of(
                "By establishing a connection with our operational modules, you gain access to a spectrum of specialized digital assets designed for performance and reliability at scale.\n\n**SYSTEM NAVIGATION PROTOCOLS**\nExamine our operational modules and establish a secure connection using the authorized protocols below."));

        layout.add(Separator.createDivider(Spacing.SMALL));

        if (rows != null && rows.length > 0) {
            for (ActionRow row : rows)
                layout.add(row);
        }
        return Container.of(layout);
    }

    public static Container rulesArabicPanel() {
        String body = """
                ## \uD83D\uDCCB Highcore Agency | Official Rules

                ### \uD83D\uDEE1\uFE0F I. General Rules
                1- **Mutual Respect:** Harassment, bullying, or offensive language is strictly prohibited. We are a professional community built on support and respect.

                2- **Professional Identity:** Please use clear, respectful names and appropriate avatars.

                3- **Privacy:** Any form of Doxxing or sharing personal information of members or clients is strictly prohibited and grounds for immediate termination.

                ### \uD83D\uDCBC II. Business Rules
                1- **Order Integrity:** Order channels are for professional work only. Pranks or fake requests will result in restricted access.

                2- **Intellectual Property:** All code, designs, and assets remain the property of "Highcore Agency" unless agreed otherwise with the client.

                3- **Official Communication:** All financial or technical agreements must be localized within Tickets to ensure transparency and proper documentation.

                ### \uD83D\uDEAB III. Prohibitions
                1- **No Advertisements:** Posting external server links or promoting external services without administration approval is prohibited.

                2- **No Spam:** Avoid repetitive messages or random pings to the administration. We are here to serve you efficiently.

                3- **Sensitive Content:** Posting political, extreme religious, or NSFW content is strictly forbidden.

                ### \u26A0\uFE0F IV. Administrative Actions
                1- **Final Decision:** The administration reserves the right to take appropriate action (Warning/Kick/Ban) in case of any violation against the agency's general spirit, even if not explicitly stated here.
                """;
        return containerBranded("PROTOCOL", "Terminal Rules", body, BANNER_MAIN);
    }

    public static Container rulePanel() {
        return rulesArabicPanel();
    }

    public static Container success(String title, String description) {
        return containerBranded("SUCCESS", title, "✅ " + description, BANNER_MAIN);
    }

    public static Container error(String title, String description) {
        return containerBranded("ERROR", title, "❌ " + description, BANNER_SUPPORT);
    }

    public static Container info(String title, String description) {
        return containerBranded("INFO", title, "ℹ️ " + description, BANNER_MAIN);
    }

    public static Container ticketHeader(String tid, String u, String ty, String b) {
        return containerBranded("SESSION", "Case #" + tid, "**Client:** " + u + "\n" + b, BANNER_SUPPORT);
    }

    public static Container staffAssigned(String n) {
        return containerBranded("NOTICE", "Agent Assigned", "Operative **" + n + "** claimed.", BANNER_SUPPORT);
    }

    public static Container accessDenied() {
        return error("ACCESS RESTRICTED", "Credentials insufficient for this terminal node.");
    }

    public static Container activityLog(String type, String details, Color color) {
        return containerBranded("activity", type, details, BANNER_MAIN).withAccentColor(color.getRGB() & 0xFFFFFF);
    }

    public static MessageEmbed createOldLogEmbed(String command, String details, Member moderator, UserSnowflake targetUser, Member targetMember, Color color) {
        EmbedBuilder eb = new EmbedBuilder();
        
        // Match the screenshot: Banner + Activity Sector Header
        eb.setImage(BANNER_MAIN);
        eb.setTitle("► Highcore Agency ・ Activity Log");
        eb.setAuthor("Action Executed", null, null);
        eb.setColor(color);
        eb.setTimestamp(Instant.now());

        // Body Fields with the exact style
        eb.addField("Action:", "`/" + command + "`", false);

        if (moderator != null) {
            eb.addField("User:", moderator.getAsMention() + " (`" + moderator.getId() + "`)", true);
        } else {
            eb.addField("User:", "Automated System", true);
        }

        // Add context for the channel if available
        if (moderator != null && moderator.getGuild() != null) {
             // We'll try to guess if we can get the interaction channel in a better way later, 
             // but for now we rely on the command handling pass-through or generic tag.
             // Usually, logs provide channel info in 'details'.
        }

        if (targetUser != null || targetMember != null) {
            String targetId = targetMember != null ? targetMember.getId() : targetUser.getId();
            eb.addField("Target:", "<@" + targetId + "> (`" + targetId + "`)", true);
            
            if (targetMember != null) {
                String roles = targetMember.getRoles().stream()
                        .map(Role::getAsMention)
                        .collect(Collectors.joining(" "));
                if (roles.isEmpty()) roles = "None";
                eb.addField("Roles:", roles, false);
            }
        }

        if (details != null && !details.isEmpty()) {
            eb.addField("Intelligence/Data:", details, false);
        }

        // Exact Footer from screenshot
        eb.setFooter("\u25AA UNIFIED TERMINAL v1.2.0 \u30FB HIGHCORE AGENCY \u25AA");

        return eb.build();
    }

    public static Container ticketClosed(String id, String user) {
        return containerBranded("session", "Closed", "Case #" + id + " archived by " + user, BANNER_SUPPORT);
    }

    public static Container ratingThanks(int r) {
        return containerBranded("LOG", "Review Saved", "Rating: " + r + "/5 ★. Thank you.", BANNER_MAIN);
    }

    public static Container warning(String t, String d) {
        return containerBranded("WARN", t, "[!] " + d, BANNER_SUPPORT).withAccentColor(WARNING.getRGB() & 0xFFFFFF);
    }

    public static Container assistantResponse(String r) {
        return containerBranded("ASSIST", "AI Transmission", r, BANNER_MAIN);
    }

    public static Container ticketPanel(ActionRow... rows) {
        return containerBranded("LOGISTICS", "Terminal Access",
                "Initialize project session via selection modules below.", BANNER_TICKETS_MENU,
                Emoji.fromUnicode("📄"), rows);
    }

    public static Container rulesPanel() {
        return rulePanel();
    }

    public static Container rulesPanel(ActionRow... rows) {
        return rulePanel();
    }

    public static Container termsPanel(ActionRow... rows) {
        return containerBranded("PROTOCOL", "Terms", "Engagement protocols.", BANNER_MAIN);
    }

    public static Container help() {
        return containerBranded("SYSTEM", "Command Directory",
                "Access all operative commands via standard slash interface (`/`).", BANNER_MAIN);
    }

    public static Container containerBrandedRows(String title, String subtitle, String body, String imageUrl,
            ActionRow... rows) {
        return containerBranded(title, subtitle, body, imageUrl, rows);
    }

    public static Container giveaway(String prize, int winners, int duration) {
        String body = "### 🎁 Active Giveaway\n" +
                "**Prize:** `" + prize + "`\n" +
                "**Winners:** `" + winners + "`\n" +
                "**Duration:** `" + duration + "m`";
        return containerBranded("SWEEPSTAKES", "Active", body, BANNER_GIVEAWAY);
    }

    public static Container custom(String category, String title, String body, String imageUrl, String thumbnail,
            String author, String authorUrl, String footer, String footerIcon, String field1Title, String field1Value,
            Boolean field1Inline, String field2Title, String field2Value, Boolean field2Inline, String field3Title,
            String field3Value, Boolean field3Inline) {
        return containerBranded(category, title, body, imageUrl);
    }

    public static Color parseColor(String colorStr) {
        if (colorStr == null)
            return ACCENT_TEAL;
        try {
            return Color.decode(colorStr.startsWith("#") ? colorStr : "#" + colorStr);
        } catch (Exception e) {
            return ACCENT_TEAL;
        }
    }
}
