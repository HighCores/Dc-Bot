package com.highcore.bot.utils;

import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.mediagallery.MediaGallery;
import net.dv8tion.jda.api.components.mediagallery.MediaGalleryItem;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.components.section.Section;
import net.dv8tion.jda.api.components.section.SectionIcon;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import com.google.gson.JsonObject;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

public class EmbedUtil {

    public static final String BANNER_MAIN = "https://images.unsplash.com/photo-1614850523296-d8c1af93d400?q=80&w=2070&auto=format&fit=crop";
    public static final String BANNER_SUPPORT = "https://images.unsplash.com/photo-1518770660439-4636190af475?q=80&w=2070&auto=format&fit=crop";
    
    public static final Color SUCCESS = Color.decode("#10b981"); 
    public static final Color DANGER = Color.decode("#f43f5e");  
    public static final Color WARNING = Color.decode("#f59e0b"); 
    public static final Color GOLD = Color.decode("#fbbf24");
    public static final Color ACCENT_TEAL = Color.decode("#14b8a6");

    /**
     * UNIFIED TERMINAL ENGINE V2
     * Integrated ActionRows INSIDE the Container for a single premium card view.
     */
    public static Container containerBranded(String sector, String title, String body, String imageUrl, Emoji iconEmoji, ActionRow... rows) {
        List<ContainerChildComponent> layout = new ArrayList<>();
        
        if (imageUrl != null && !imageUrl.isEmpty()) {
            layout.add(MediaGallery.of(MediaGalleryItem.fromUrl(imageUrl)));
        }

        layout.add(TextDisplay.of("### \u25C8 " + sector.toUpperCase() + " SECTOR \u30FB " + title.toUpperCase()));
        layout.add(Separator.createDivider(Separator.Spacing.SMALL));

        if (iconEmoji != null) {
            layout.add(Section.of(SectionIcon.of(iconEmoji), TextDisplay.of(body)));
        } else {
            layout.add(TextDisplay.of(body));
        }

        layout.add(Separator.createDivider(Separator.Spacing.SMALL));

        // CRITICAL: Adding ActionRows INSIDE the Container for V2 Card Style
        if (rows != null) {
            layout.addAll(Arrays.asList(rows));
        }

        layout.add(Separator.createDivider(Separator.Spacing.SMALL));
        layout.add(TextDisplay.of("` \u2022 UNIFIED TERMINAL v2.1 \u2022 HIGHCORE AGENCY \u2022 `"));

        return Container.of(layout).withAccentColor(ACCENT_TEAL.getRGB() & 0xFFFFFF);
    }

    public static Container mainMenu(ActionRow... rows) {
        String body = """
            Welcome Operative. Access restricted agency sectors via control modules.
            
            \u25CF **Tickets** \u2014 Secure Support Channels
            \u25CF **Services** \u2014 Agency Projects
            \u25CF **Merit Hub** \u2014 Identity Audit
            """;
        return containerBranded("CENTER", "Main Control Node", body, BANNER_MAIN, Emoji.fromUnicode("\uD83D\uDCE1"), rows);
    }

    public static Container ticketPanel(ActionRow... rows) {
        return containerBranded("LOGISTICS", "Terminal Access", "Initialize project session via selection modules below.", BANNER_SUPPORT, Emoji.fromUnicode("\uD83D\uDCC4"), rows);
    }

    public static Container startupPanel(ActionRow... rows) {
        return containerBranded("INIT", "Onboarding Sequence", "Highcore Agency delivers high-fidelity digital solutions.\nDeploy modules to begin.", BANNER_MAIN, Emoji.fromUnicode("\uD83D\uDE80"), rows);
    }

    public static Container error(String title, String description) { return containerBranded("ERROR", title, "[\u274C] " + description, BANNER_SUPPORT, Emoji.fromUnicode("\u274C")).withAccentColor(DANGER.getRGB() & 0xFFFFFF); }
    public static Container success(String title, String description) { return containerBranded("SUCCESS", title, "[\u2705] " + description, BANNER_MAIN, Emoji.fromUnicode("\u2705")).withAccentColor(SUCCESS.getRGB() & 0xFFFFFF); }
    public static Container info(String title, String description) { return containerBranded("INFO", title, "[\u2139\uFE0F] " + description, BANNER_MAIN, Emoji.fromUnicode("\u2139\uFE0F")); }
    
    // BACKWARDS COMPATIBILITY WRAPPERS
    public static Container services() { return containerBranded("OPS", "Capability Directory", "Explore Design & Development.", BANNER_MAIN, Emoji.fromUnicode("\uD83D\uDED2")); }
    public static Container rulePanel() { return containerBranded("PROTOCOL", "Compliance", "Professionalism mandatory.", BANNER_MAIN, Emoji.fromUnicode("\uD83D\uDCDC")); }
    public static Container pointsPanel() { return containerBranded("SYSTEM", "Merit Registry", "Examine standing.", BANNER_MAIN, Emoji.fromUnicode("\uD83D\uDCCA")); }
    public static Container stats(int t, int o, int c, int cl, String s) { return containerBranded("STATS", "Network", "Active Nodes: " + o, BANNER_MAIN, Emoji.fromUnicode("\uD83D\uDCCA")); }
    public static Container meritAudit(int points) { return containerBranded("AUDIT", "Identity Summary", "Merit: **" + points + "**", BANNER_MAIN, Emoji.fromUnicode("\u2B50")); }
    public static Container ticketHeader(String tid, String u, String ty, String b) { return containerBranded("SESSION", "Case #" + tid, "**Client:** " + u + "\n" + b, BANNER_SUPPORT, Emoji.fromUnicode("\uD83D\uDCC1")); }
    public static Container staffAssigned(String n) { return containerBranded("NOTICE", "Agent Assigned", "Operative **" + n + "** claimed.", BANNER_SUPPORT, Emoji.fromUnicode("\u2705")); }
    
    public static TextDisplay v2Header(String category, String title) { return TextDisplay.of("### \u25BA " + category.toUpperCase() + " \u30FB " + title); }
    public static TextDisplay v2Footer() { return TextDisplay.of("` \u2022 UNIFIED TERMINAL v2.1 \u2022 `"); }
    public static Container custom(String category, String title, String body, String imageUrl, String thumbnail, String author, String authorUrl, String footer, String footerIcon, String field1Title, String field1Value, Boolean field1Inline, String field2Title, String field2Value, Boolean field2Inline, String field3Title, String field3Value, Boolean field3Inline) {
        return containerBranded(category, title, body, imageUrl, null);
    }
}
