package com.highcore.bot.utils;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.mediagallery.MediaGallery;
import net.dv8tion.jda.api.components.mediagallery.MediaGalleryItem;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import com.google.gson.JsonObject;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class EmbedUtil {

    public static final String BANNER_MAIN = "https://i.imgur.com/B94Uf6O.png";
    public static final String BANNER_SUPPORT = "https://i.imgur.com/vH97Z9P.png";
    public static final String BANNER_SERVICES = "https://i.imgur.com/B94Uf6O.png";
    public static final String BANNER_GIVEAWAY = "https://i.imgur.com/vH97Z9P.png";

    public static final String DIVIDER = "▬" + "▬" + "▬" + "▬" + "▬" + "▬" + "▬" + "▬" + "▬" + "▬" + "▬" + "▬" + "▬" + "▬" + "▬";

    public static final Color PRIMARY = Color.decode("#ffffff");
    public static final Color SUCCESS = Color.decode("#10b981");
    public static final Color ACCENT_TEAL = Color.decode("#14b8a6");
    public static final Color DANGER = Color.decode("#ef4444");
    public static final Color WARNING = Color.decode("#f59e0b");
    public static final Color INFO = Color.decode("#3b82f6");
    public static final Color GOLD = Color.decode("#f59e0b");

    public static Color parseColor(String hex) {
        if (hex == null || hex.isEmpty() || hex.equalsIgnoreCase("brand")) return SUCCESS;
        try {
            if (hex.startsWith("#")) return Color.decode(hex);
            return switch (hex.toLowerCase()) {
                case "success" -> SUCCESS;
                case "danger" -> DANGER;
                case "gold" -> GOLD;
                case "info" -> INFO;
                case "teal" -> ACCENT_TEAL;
                default -> SUCCESS;
            };
        } catch (Exception e) { return SUCCESS; }
    }

    public static TextDisplay v2Header(String category, String title) {
        return TextDisplay.of("### ✨ " + category.toUpperCase() + " \u2022 " + title);
    }

    public static TextDisplay v2Footer() {
        return TextDisplay.of("*Highcore Agency \u2022 Professional Service Excellence*");
    }

    public static Container containerBranded(String category, String title, String description, String bannerUrl) {
        List<ContainerChildComponent> layout = new ArrayList<>();
        if (bannerUrl != null && !bannerUrl.isEmpty()) layout.add(MediaGallery.of(MediaGalleryItem.fromUrl(bannerUrl)));
        layout.add(TextDisplay.of("### \u2728 " + category.toUpperCase() + " \u2022 " + title));
        layout.add(Separator.createDivider(Separator.Spacing.SMALL));
        layout.add(TextDisplay.of(description));
        layout.add(TextDisplay.of("*Highcore Agency \u2022 Professional Service Excellence*"));
        return Container.of(layout).withAccentColor(SUCCESS.getRGB() & 0xFFFFFF);
    }

    public static Container sectionedBranded(String category, String title, String body, String topBanner, String middleImage) {
        List<ContainerChildComponent> layout = new ArrayList<>();
        if (topBanner != null) layout.add(MediaGallery.of(MediaGalleryItem.fromUrl(topBanner)));
        layout.add(TextDisplay.of("### \u2728 " + category.toUpperCase() + " \u2022 " + title));
        layout.add(Separator.createDivider(Separator.Spacing.SMALL));
        if (body != null) layout.add(TextDisplay.of(body));
        if (middleImage != null) layout.add(MediaGallery.of(MediaGalleryItem.fromUrl(middleImage)));
        layout.add(TextDisplay.of("*Highcore Agency \u2022 Established Excellence*"));
        return Container.of(layout).withAccentColor(SUCCESS.getRGB() & 0xFFFFFF);
    }

    public static Container mainMenu() {
        return containerBranded("CENTER", "Main Menu", 
            "Welcome to the Highcore Agency. Choose a department to begin.\n\n" +
            "🎫 **Tickets** - Open a support request\n" +
            "🛒 **Services** - View our professional offerings\n" +
            "📊 **Activity** - Server statistics and data", 
            BANNER_MAIN);
    }

    public static Container ticketPanel() {
        return containerBranded("SUPPORT", "Support Center", 
            "Professional assistance is ready. How can we help you today?\n\n" +
            "Please select a category to start your request. " +
            "A team member will be assigned to assist you shortly.", 
            BANNER_SUPPORT);
    }

    public static Container pointsPanel() {
        return containerBranded("SECURITY", "Merit Hub", 
            "Audit your contribution metrics and administrative standing.", 
            BANNER_MAIN);
    }

    public static Container assistantResponse(String response) {
        return containerBranded("ASSISTANT", "Smart Assistant Response", response, BANNER_MAIN);
    }

    public static Container services() {
        String body = """
            ### 🛒 AGENCY SERVICE CATALOG
            ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            **🎨 DESIGN DEP.**
            `[Vector Logo ]` \u2022 $25
            `[Abstract Logo]` \u2022 $35
            `[Full Branding]` \u2022 $60+
            `[Motion Intro ]` \u2022 $30
            
            **⚙️ DEVELOPMENT DEP.**
            `[Discord Bot  ]` \u2022 $20+
            `[Custom Plugin]` \u2022 $35+
            `[Web Landing  ]` \u2022 $45+
            `[Full Systems ]` \u2022 $120+
            
            **🎬 MEDIA & MINECRAFT**
            `[Video Edit   ]` \u2022 $15+
            `[Server Setup ]` \u2022 $30+
            `[Architectural]` \u2022 $25+
            ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            *Select a department below to start your project request.*
            """;
        return sectionedBranded("SERVICES", "Professional Directory", body, BANNER_SERVICES, "https://i.imgur.com/vH97Z9P.png");
    }

    public static Container rulePanel() {
        return containerBranded("RULES", "Agency Guidelines", 
            "## 📋 Highcore Agency Rules\n\n" +
            "### 🛡️ I. General Guidelines\n" +
            "1. **Respect**: Mutual respect is mandatory. No abuse, bullying, or slurs.\n" +
            "2. **Identity**: Use professional names. Avoid inappropriate visuals.\n" +
            "3. **Privacy**: Sharing private or sensitive data is strictly prohibited.\n\n" +
            "### 💼 II. Business Rules\n" +
            "1. **Seriousness**: Project requests are for serious business. Fake requests are not allowed.\n" +
            "2. **IP Rights**: All code and designs belong to Highcore unless agreed otherwise.\n" +
            "3. **Official Chat**: Financial or job agreements MUST happen in Tickets.\n\n" +
            "### 🚫 III. Prohibited Content\n" +
            "1. **No Ads**: Promoting other services is not allowed.\n" +
            "2. **No Spam**: Do not spam or ping staff unnecessarily.\n" +
            "3. **Sensitive Content**: No politics, religion, or NSFW content.\n\n" +
            "### ⚠️ IV. Final Authority\n" +
            "1. **Final Decision**: Staff decisions are final. Rules may be updated as needed.", 
            BANNER_MAIN);
    }

    public static Container socialMedia() {
        return containerBranded("SOCIAL", "Connect with Us", 
            "Follow our social media pages. Select a platform to visit.", 
            BANNER_MAIN);
    }

    public static Container team() {
        return containerBranded("MANAGEMENT", "Executive Team", 
            "Professional leadership of Highcore Agency.\n\n" +
            "**Agency Director:**\n> OmarAmr (Senior Director)\n\n" +
            "**Operational Staff:**\n> Our team is ready to assist you in tickets.", 
            BANNER_MAIN);
    }

    public static Container ticketHeader(String ticketId, String userName, String type, String body) {
        String name = (userName != null) ? userName : "User";
        return containerBranded("TICKET", "Order #" + ticketId, 
            "Client: **" + name + "**\nCategory: **" + type + "**\n\n**Details:**\n" + body, 
            BANNER_SUPPORT);
    }

    public static Container activityLog(String title, String body, Color color) {
        return containerBranded("LOG", title, body, BANNER_SUPPORT)
                .withAccentColor(color.getRGB() & 0xFFFFFF);
    }

    public static Container staffAssigned(String name) {
        return containerBranded("NOTICE", "Staff Assigned", 
            "A staff member (**" + name + "**) is now helping you.", 
            BANNER_SUPPORT);
    }

    public static Container orderLog(JsonObject data) {
        StringBuilder sb = new StringBuilder();
        sb.append("### 📜 TERMINAL LOG: FINANCIAL_ALLOCATION\n");
        sb.append(DIVIDER).append("\n");
        String cat = data.has("category") && !data.get("category").isJsonNull() ? data.get("category").getAsString().toUpperCase() : "GENERAL";
        sb.append("**Sector:** ").append(cat).append("\n");
        if (data.has("total")) sb.append("**Projected Cost:** `$").append(data.get("total").getAsInt()).append("`\n");
        
        return Container.of(
            v2Header("PAYMENT", "Financial Record"),
            Separator.createDivider(Separator.Spacing.SMALL),
            TextDisplay.of(sb.toString()),
            v2Footer()
        ).withAccentColor(SUCCESS.getRGB() & 0xFFFFFF);
    }

    public static Container paymentGateway(String type, String info) {
        return containerBranded("GATEWAY", type + " Information", 
            info + "\n\n*Transaction details sent to Highcore Finance.*", 
            BANNER_MAIN).withAccentColor(INFO.getRGB() & 0xFFFFFF);
    }

    public static Container meritAudit(int points) {
        return containerBranded("SYSTEM", "Points Check", 
            "Your current balance: **" + points + "** points.", 
            BANNER_MAIN).withAccentColor(GOLD.getRGB() & 0xFFFFFF);
    }

    public static Container accessDenied() {
        return containerBranded("SECURITY", "Access Denied", 
            "You do not have permission to access this area. This attempt has been logged.", 
            BANNER_MAIN).withAccentColor(DANGER.getRGB() & 0xFFFFFF);
    }

    public static Container stats(int total, int open, int closed, int claimed, String topStaff) {
        return Container.of(
            MediaGallery.of(MediaGalleryItem.fromUrl(BANNER_SUPPORT)),
            v2Header("STATS", "Agency Growth & Data"),
            Separator.createDivider(Separator.Spacing.SMALL),
            TextDisplay.of("**Summary**\n> Total Tickets: **" + total + "**\n> Active: **" + open + "**\n> Resolved: **" + closed + "**"),
            TextDisplay.of("**Team**\n> Claimed Cases: **" + claimed + "**\n> Top Staff Member: **" + topStaff + "**"),
            v2Footer()
        ).withAccentColor(INFO.getRGB() & 0xFFFFFF);
    }

    public static Container giveawayPanel() {
        return containerBranded("GIVEAWAY", "Community Giveaway", "Manage active community giveaways and prize distributions here.", BANNER_GIVEAWAY)
                .withAccentColor(GOLD.getRGB() & 0xFFFFFF);
    }

    public static Container help() {
        return containerBranded("HELP", "Documentation", 
            "### 📜 Command Guide\n" +
            "> **Prefix:** `/` (Slash Commands)\n\n" +
            "**📊 Available Commands:**\n" +
            "- `/menu` - Open the main menu\n" +
            "- `/tickets` - Manage or open tickets\n" +
            "- `/services` - View what we offer\n" +
            "- `/stats` - View group statistics", BANNER_MAIN);
    }

    public static Container startupPanel() {
        return containerBranded("WELCOME", "Highcore Agency", 
            "### 🚀 System Online\n" +
            "Welcome to Highcore Agency. We provide professional solutions for your needs.\n\n" +
            "Use the buttons below to browse our services, check prices, or start a project. We specialize in design, development, and media production.", 
            BANNER_MAIN);
    }

    public static Container serverMap() {
        return containerBranded("MAP", "Server Layout", 
            "### 🗺️ Channel Guide\n" +
            "**ℹ️ Information:** Rules, Info, News\n" +
            "**🛒 Store:** Services, Pricing, Orders\n" +
            "**💬 Chat:** General chat, media share\n" +
            "**🎫 Support:** Private project tickets\n" +
            "**🏆 Showcase:** Designs, Bots, Portfolio", BANNER_MAIN);
    }

    public static Container ticketClosed(String tid, String closer) {
        return containerBranded("LOGS", "Ticket Closed", 
            "Ticket: **#" + tid + "**\nClosed by: **" + closer + "**\n\n*Logs have been saved.*", BANNER_SUPPORT)
            .withAccentColor(DANGER.getRGB() & 0xFFFFFF);
    }

    public static Container error(String title, String description) {
        return containerBranded("ERROR", title, "❌ " + description, BANNER_MAIN)
                .withAccentColor(DANGER.getRGB() & 0xFFFFFF);
    }

    public static Container success(String title, String description) {
        return containerBranded("SUCCESS", title, "✅ " + description, BANNER_MAIN)
                .withAccentColor(SUCCESS.getRGB() & 0xFFFFFF);
    }

    public static Container info(String title, String description) {
        return containerBranded("INFO", title, "ℹ️ " + description, BANNER_MAIN)
                .withAccentColor(INFO.getRGB() & 0xFFFFFF);
    }

    public static Container warning(String title, String description) {
        return containerBranded("WARNING", title, "\u26A0\uFE0F " + description, BANNER_MAIN)
                .withAccentColor(WARNING.getRGB() & 0xFFFFFF);
    }

    public static Container ratingThanks(int stars) {
        return containerBranded("FEEDBACK", "Thank You", 
            "We appreciate your **" + stars + "-star** rating! Your feedback helps us maintain professional excellence.", 
            BANNER_MAIN).withAccentColor(SUCCESS.getRGB() & 0xFFFFFF);
    }

    public static Container custom(String category, String title, String body, String imageUrl, String thumbnail, String author, String authorUrl, String footer, String footerIcon, String field1Title, String field1Value, Boolean field1Inline, String field2Title, String field2Value, Boolean field2Inline, String field3Title, String field3Value, Boolean field3Inline) {
        List<ContainerChildComponent> layout = new ArrayList<>();
        if (imageUrl != null && !imageUrl.isEmpty()) layout.add(MediaGallery.of(MediaGalleryItem.fromUrl(imageUrl)));
        layout.add(v2Header(category != null ? category : "AGENCY", title != null ? title : "Information"));
        layout.add(Separator.createDivider(Separator.Spacing.SMALL));
        if (body != null) layout.add(TextDisplay.of(body));
        
        StringBuilder sb = new StringBuilder();
        if (field1Title != null) sb.append("**").append(field1Title).append(":** ").append(field1Value).append("\n");
        if (field2Title != null) sb.append("**").append(field2Title).append(":** ").append(field2Value).append("\n");
        if (field3Title != null) sb.append("**").append(field3Title).append(":** ").append(field3Value).append("\n");
        
        if (sb.length() > 0) layout.add(TextDisplay.of(sb.toString()));
        layout.add(v2Footer());
        
        return Container.of(layout).withAccentColor(SUCCESS.getRGB() & 0xFFFFFF);
    }
}
