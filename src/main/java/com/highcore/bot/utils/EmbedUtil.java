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

    public static final String BANNER_MAIN = "https://cdn.discordapp.com/attachments/1488795131296354460/1491194673048457399/banner.png";
    public static final String BANNER_MAP = BANNER_MAIN;
    public static final String BANNER_PINGS = BANNER_MAIN;
    public static final String BANNER_COLORS = BANNER_MAIN;
    public static final String BANNER_RULES = BANNER_MAIN;
    public static final String BANNER_SOCIAL = BANNER_MAIN;
    public static final String BANNER_SUPPORT = "https://cdn.discordapp.com/attachments/1491791586479177768/1491810902243148027/IMG_20260409_174341.png";
    public static final String BANNER_GIVEAWAY = BANNER_MAIN;
    public static final String BANNER_INVOICE = "https://cdn.discordapp.com/attachments/1488900668042510568/1491799713391837376/IMG_20260409_165917.png";
    public static final String BANNER_ORDER_TIK = "https://cdn.discordapp.com/attachments/1488900668042510568/1491808487104057455/ORDER-.jpg";

    public static final Color SUCCESS = Color.decode("#D4AF37");
    public static final Color DANGER = Color.decode("#8B0000");
    public static final Color WARNING = Color.decode("#FFD700");
    public static final Color INFO = Color.decode("#C0C0C0");
    public static final Color GOLD = Color.decode("#D4AF37");
    public static final Color ACCENT_GOLD = Color.decode("#FFD700");

    public static final String RULES_TEXT = """
            1. General Rules
            - Mutual Respect: No harassment or offensive language.
            - Professionalism: Clear nicknames, appropriate profiles.
            - Privacy: No personal information sharing.

            2. Business Rules
            - Orders are for work only.
            - Copyright remains with High Core unless specified.
            - All business must go through Tickets.

            3. Prohibitions
            - No advertisements or external promotions.
            - No spamming or excessive mentions.
            - No NSFW, political, or religious content.
            """;

    public static final String TICKET_RULES = """
            ### 📜 PROTOCOLS & COMPLIANCE
            
            - **MUTUAL RESPECT:** Respect all staff members. Harassment or offensive language will result in an immediate ban.
            - **SINGLE SESSION:** Open only one ticket per issue. Do not duplicate requests to speed up response times.
            - **PRECISION:** Explain your issue fully before staff arrives to ensure a fast and effective resolution.
            - **CONTENT CONTROL:** External links and spam are strictly prohibited without staff authorization.
            - **MENTION BAN:** Strictly NO pinging/mentioning staff members within the ticket.
            """;

    public static final String TERMS_TEXT = """
            ### 🛡️ 1. OUR PROMISE
            We are High Core Agency. We provide top-quality digital work. By using our services, you agree to these simple rules. Your work and safety are our priority.

            ### 📹 2. CLIPS & MEDIA POLICY
            - **Your Rights:** You own any clip or file you give us. We only keep rights to the special edits we add.
            - **Privacy:** We save your clips in a private place. We never share your files with anyone else.
            - **Cleanup:** We delete your clips from our system **30 days** after we finish the job.

            ### 🔒 3. SECURITY & PRIVACY
            - **Simple Data:** We don't track you. We only use your Discord ID to manage your orders.
            - **Safe System:** Our bot uses professional security to keep your information secret.
            """;

    public static Container containerBranded(String category, String topic, String body, String imageUrl) {
        return containerBranded(category, topic, body, imageUrl, null);
    }

    public static Container containerBranded(String title, String subtitle, String body, String imageUrl, Emoji emoji,
            ActionRow... rows) {
        List<ContainerChildComponent> layout = new ArrayList<>();

        // 1. BRANDING BANNER - High Priority Rendering
        if (imageUrl != null && !imageUrl.isEmpty()) {
            layout.add(MediaGallery.of(MediaGalleryItem.fromUrl(imageUrl)));
        }

        // 2. ELITE HEADER
        String headerText = (subtitle == null || subtitle.isEmpty()) ? "### " + title.toUpperCase() : "### " + title.toUpperCase() + " : " + subtitle.toUpperCase();
        layout.add(TextDisplay.of(headerText));

        // 3. TECHNICAL BODY
        if (body != null && !body.isEmpty()) {
            layout.add(TextDisplay.of(body));
        }

        // 4. ACTION INTERFACE
        if (rows != null && rows.length > 0) {
            for (ActionRow row : rows) {
                if (row != null) layout.add(row);
            }
        }

        return Container.of(layout);
    }

    public static Container startupPanel(ActionRow... rows) {
        String body = "High Core Agency\nGlobal infrastructure at your fingertips. Use the protocols below to navigate our sectors.";
        return containerBranded("HUB", "Main Dashboard", body, BANNER_MAIN, null, rows);
    }

    public static Container rulesPanel(ActionRow... rows) {
        return containerBranded("Rules", "Compliance", RULES_TEXT, BANNER_RULES, Emoji.fromUnicode("\uD83D\uDCDC"),
                rows);
    }

    public static Container termsPanel(ActionRow... rows) {
        return containerBranded("Terms", "Transparency", TERMS_TEXT, BANNER_MAIN, Emoji.fromUnicode("\u2696\uFE0F"),
                rows);
    }

    public static Container accessDenied() {
        return error("Unauthorized", "You do not have the required permissions to access this section.");
    }

    public static Container error(String title, String description) {
        return containerBranded("Error", title, description, BANNER_SUPPORT);
    }

    public static Container success(String title, String description) {
        return containerBranded("Success", title, description, BANNER_MAIN);
    }

    public static Container info(String title, String description) {
        return containerBranded("Information", title, description, BANNER_MAIN);
    }

    public static Container activityLog(String type, String details, Color color) {
        return containerBranded("System", type, details, null);
    }

    public static java.util.List<net.dv8tion.jda.api.components.container.Container> helpPanel() {
        String body = "TERMINAL OPERATIONAL GUIDE\n\nOperational interface for High Core Agency. Access to multi-sector modules and system telemetry is restricted to authorized personnel.\n\n### COMMAND REGISTER\n- `/startup`: Initialize Hub\n- `/tickets`: Access Logistics\n- `/services`: Capability Matrix\n- `/stats`: Telemetry Status";
        return List.of(containerBranded("HELP", "Manual", body, BANNER_MAIN));
    }

    public static Container giveaway(String prize, int winners, int duration) {
        String body = "New Giveaway\n" +
                "Prize: " + prize + "\n" +
                "Winners: " + winners + "\n" +
                "Duration: " + duration + " minutes\n\n" +
                "Click the button below to participate.";
        return containerBranded("Event", "Giveaway", body, BANNER_GIVEAWAY, null);
    }
}
