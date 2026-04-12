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
    public static final Color SUCCESS     = Color.decode("#10b981");
    public static final Color DANGER      = Color.decode("#f43f5e");
    public static final Color INFO        = Color.decode("#3b82f6");
    public static final Color WARNING     = Color.decode("#f59e0b");
    public static final Color GOLD        = Color.decode("#fbbf24");
    public static final Color ACCENT_GOLD = Color.decode("#FFD700");
    public static final Color ACCENT_TEAL = Color.decode("#14b8a6");
    public static final Color PRIMARY     = Color.decode("#10b981");

    public static final String BANNER_MAIN           = "https://i.ibb.co/3ykpY60W/Untitled-1.png";
    public static final String BANNER_SUPPORT        = "https://i.ibb.co/v4mK9Wf1/Untitled-1.png";
    public static final String BANNER_COMPLAINT      = "https://cdn.discordapp.com/attachments/1488900668042510568/1492313318193627156/468b060c-ce67-46c1-b0db-db1f476bebed.png";
    public static final String BANNER_GIVEAWAY       = "https://i.ibb.co/6RTPXvS2/Untitled-1.png";
    public static final String BANNER_INVOICE        = "https://media.discordapp.net/attachments/1488900668042510568/1492648280926978148/invoice_discord_.jpg?ex=69dc188a&is=69dac70a&hm=ce79513d28baf3f7572a0c473107c30930365356dbc52fbe84534ab3bce9ce25&format=png";
    public static final String BANNER_INVOICE_PAID   = "https://media.discordapp.net/attachments/1491360207568113787/1492627055974678610/invoice_site_.png?ex=69dc04c5&is=69dab345&hm=a5f31dd32ae1ecc06f3c87fdde8008018c32ad89da7e28642b62c38f278f4a5d&format=png";
    public static final String BANNER_ORDER_TICKET   = "https://cdn.discordapp.com/attachments/1488900668042510568/1491808487104057455/ORDER-.jpg?ex=69db04ab&is=69d9b32b&hm=4f001336ef9b5ae1c96a2e5083e53e726197782920ce8b7af5f0c22d2ac8ddbf&";

    public static Container eliteContainer(String title, String description, String imageUrl, ActionRow... rows) {
        return containerBranded("", title, description, imageUrl, rows);
    }

    public static Container containerBranded(String sector, String title, String body, String imageUrl, ActionRow... rows) {
        return containerBranded(sector, title, body, imageUrl, null, rows);
    }

    public static Container containerBranded(String sector, String title, String body, String imageUrl, Emoji iconEmoji, ActionRow... rows) {
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
        layout.add(MediaGallery.of(MediaGalleryItem.fromUrl(BANNER_MAIN)));
        
        layout.add(TextDisplay.of("**Highcore Agency**"));
        layout.add(Separator.createDivider(Spacing.SMALL));
        
        layout.add(TextDisplay.of("**PROFESSIONAL MULTI-SECTOR AGENCY OPERATIONS**\n\nEstablishing the definitive global standard for advanced digital infrastructure and elite creative operations. Highcore provides high-fidelity solutions across system development, visual architecture, and strategic media management.\n\nOur internal systems are engineered for absolute brand dominance and superior technological precision. We merge artistic vision with technical mastery to deliver the unlimited potential of the agency directly to our global partners."));
        
        layout.add(Separator.createDivider(Spacing.SMALL));
        
        layout.add(TextDisplay.of("By establishing a connection with our operational modules, you gain access to a spectrum of specialized digital assets designed for performance and reliability at scale.\n\n**SYSTEM NAVIGATION PROTOCOLS**\nExamine our operational modules and establish a secure connection using the authorized protocols below."));
        
        layout.add(Separator.createDivider(Spacing.SMALL));

        if (rows != null && rows.length > 0) {
            for (ActionRow row : rows) layout.add(row);
        }
        return Container.of(layout);
    }

    public static Container rulePanel() {
        String body = """
                ### :shield: I. GENERAL PROTOCOLS
                
                1. **Mutual Respect** — Harassment, bullying, or offensive language is strictly prohibited. We are a professional community built on support.
                2. **Professional Identity** — Operatives should use clear, respectful names and appropriate avatars. 
                3. **Privacy** — Any form of Doxxing or sharing personal information of members or clients is grounds for immediate termination.
                
                ### :briefcase: II. BUSINESS CONDUCT
                
                1. **Order Integrity** — Service corridors for professional work only. Pranks or fake requests result in restricted access.
                2. **Intellectual Property** — All code, designs, and assets remain the property of "Highcore Agency" unless specified otherwise.
                3. **Official Communication** — All agreements must be localized within "Tickets" to ensure transparency.
                
                ### :no_entry_sign: III. PROHIBITIONS
                
                1. **No Advertisements** — Posting external server links without prior authorization is prohibited.
                2. **No Spam** — Avoid message repetition or random pings to the Command staff.
                3. **Sensitive Content** — Political, religious extremism, or NSFW content is strictly forbidden.
                
                ### :warning: IV. ADMINISTRATIVE AUTHORITY
                
                1. **Final Decision** — Administration reserves the right to take appropriate action (Warning/Kick/Ban) to protect the agency's integrity.
                """;
        return containerBranded("clipboard", "Highcore Agency | Official Rules", body, BANNER_MAIN);
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
                "Initialize project session via selection modules below.", BANNER_SUPPORT,
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
        return containerBranded("SYSTEM", "Command Directory", "Access all operative commands via standard slash interface (`/`).", BANNER_MAIN);
    }

    public static Container containerBrandedRows(String title, String subtitle, String body, String imageUrl, ActionRow... rows) {
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
        if (colorStr == null) return ACCENT_TEAL;
        try {
            return Color.decode(colorStr.startsWith("#") ? colorStr : "#" + colorStr);
        } catch (Exception e) {
            return ACCENT_TEAL;
        }
    }
}
