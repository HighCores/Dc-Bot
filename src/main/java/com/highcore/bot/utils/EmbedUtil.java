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

    public static final String BANNER_MAIN = "https://raw.githubusercontent.com/OmarAmr20/Img/main/741852963.png";
    public static final String BANNER_SUPPORT = "https://i.imgur.com/vH97Z9P.png";
    public static final String BANNER_SERVICES = "https://raw.githubusercontent.com/OmarAmr20/Img/main/741852963.png";
    public static final String BANNER_GIVEAWAY = "https://i.imgur.com/XwS1Yj8.png";

    public static final String DIVIDER = "\u25AC\u25AC\u25AC\u25AC\u25AC\u25AC\u25AC\u25AC\u25AC\u25AC\u25AC\u25AC\u25AC\u25AC\u25AC";

    public static final Color PRIMARY = Color.decode("#ffffff");
    public static final Color SUCCESS = Color.decode("#2ecc71");
    public static final Color DANGER = Color.decode("#e74c3c");
    public static final Color WARNING = Color.decode("#f1c40f");
    public static final Color INFO = Color.decode("#3498db");
    public static final Color GOLD = Color.decode("#f1c40f");
    public static final Color ACCENT_PURPLE = Color.decode("#9b59b6");
    public static final Color ACCENT_TEAL = Color.decode("#1abc9c");

    public static Color parseColor(String hex) {
        if (hex == null) return PRIMARY;
        try {
            if (hex.startsWith("#")) return Color.decode(hex);
            return switch (hex.toLowerCase()) {
                case "success" -> SUCCESS;
                case "danger" -> DANGER;
                case "gold" -> GOLD;
                case "info" -> INFO;
                default -> PRIMARY;
            };
        } catch (Exception e) { return PRIMARY; }
    }

    public static Container containerBranded(String category, String title, String description, String bannerUrl) {
        List<ContainerChildComponent> layout = new ArrayList<>();
        
        if (bannerUrl != null && !bannerUrl.isEmpty()) {
            layout.add(MediaGallery.of(MediaGalleryItem.fromUrl(bannerUrl)));
        }

        layout.add(v2Header(category, title));
        layout.add(Separator.createDivider(Separator.Spacing.SMALL));
        layout.add(TextDisplay.of(description));
        layout.add(v2Footer());
        
        return Container.of(layout).withAccentColor(PRIMARY.getRGB() & 0xFFFFFF);
    }

    public static TextDisplay v2Header(String category, String title) {
        return TextDisplay.of("### \u039B " + category.toUpperCase() + " \u2022 " + title);
    }

    public static TextDisplay v2Footer() {
        return TextDisplay.of("*Highcore Agency Registry \u2022 Operational Node*");
    }

    public static Container mainMenu() {
        return containerBranded("HUB", "Main Terminal", 
            "Connect to the Highcore infrastructure. Select a node to begin.\n\n" +
            "\uD83C\uDFAB **Tickets** - Open a secure communication line\n" +
            "\uD83D\uDED2 **Services** - View active agency offerings\n" +
            "\u2B50 **Merit** - Audit your server contribution points\n" +
            "\uD83D\uDCCA **Network** - Operational statistics and uptime", 
            BANNER_MAIN);
    }

    public static Container ticketPanel() {
        return containerBranded("SUPPORT", "Communication Hub", 
            "Encryption established. System ready for input.\n\n" +
            "Please select a secure category to initialize your request. " +
            "An available operative will be assigned to your node shortly.", 
            BANNER_SUPPORT);
    }

    public static Container pointsPanel() {
        return containerBranded("SECURITY", "Merit Hub", 
            "Audit your contribution metrics and agency standing.", 
            BANNER_MAIN);
    }

    public static Container neuralNode(String response) {
        return containerBranded("NEURAL", "AI Logic Response", response, BANNER_MAIN);
    }

    public static Container services() {
        return containerBranded("CATALOG", "Service Directory", 
            "Highcore Agency Service Allocation Matrix:\n\n" +
            "| Service | Price | Status |\n" +
            "| :--- | :--- | :--- |\n" +
            "| **DBA** | $40 | \u2705 ONLINE |\n" +
            "| **Web** | $60 | \u2705 ONLINE |\n" +
            "| **Bot** | $50 | \u2705 ONLINE |\n" +
            "| **AI** | $100 | \u26A0\uFE0F HIGH LOAD |\n\n" +
            "*Prices are indicative base rates. Subject to node complexity.*", 
            BANNER_SERVICES);
    }

    public static Container team() {
        return containerBranded("PERSONNEL", "Agency Operatives", 
            "Operational hierarchy of the Highcore Agency.\n\n" +
            "**Command Node:**\n> OmarAmr (Strategic Director)\n\n" +
            "**Neural Ops:**\n> HC-019 (AI Logic)\n> HC-Bot (Interface)", 
            BANNER_MAIN);
    }

    public static Container ticketNode(String ticketId, String userName, String type, String body) {
        String name = (userName != null) ? userName : "Unknown Operative";
        return containerBranded("SUPPORT", "Sector " + ticketId, 
            "User: **" + name + "**\nType: **" + type + "**\n\n" + body, 
            BANNER_SUPPORT);
    }

    public static Container levelPanel(User user) {
        String name = (user != null) ? user.getName() : "Unknown Operative";
        return containerBranded("IDENTITY", "Profile Audit", 
            "User: **" + name + "**\nStanding: **Verified Operative**\nMerit Multiplier: **1.2x**", 
            BANNER_MAIN);
    }

    public static Container logNode(String title, String body, Color color) {
        return containerBranded("REGISTRY", title, body, BANNER_SUPPORT)
                .withAccentColor(color.getRGB() & 0xFFFFFF);
    }

    public static Container staffAssigned(String name) {
        return containerBranded("NOTIFICATION", "Operator Linked", 
            "This sector is now under the authority of operative **" + name + "**.", 
            BANNER_SUPPORT);
    }

    public static Container orderRegistry(JsonObject data) {
        StringBuilder sb = new StringBuilder();
        sb.append("### \uD83D\uDCC3 TERMINAL LOG: FINANCIAL_ALLOCATION\n");
        sb.append(DIVIDER).append("\n");
        String cat = data.has("category") && !data.get("category").isJsonNull() ? data.get("category").getAsString().toUpperCase() : "GENERAL";
        sb.append("**Sector:** ").append(cat).append("\n");
        if (data.has("total")) sb.append("**Projected Cost:** `$").append(data.get("total").getAsInt()).append("`\n");
        
        return Container.of(
            v2Header("REGISTRY", "Financial Node"),
            Separator.createDivider(Separator.Spacing.SMALL),
            TextDisplay.of(sb.toString()),
            v2Footer()
        ).withAccentColor(SUCCESS.getRGB() & 0xFFFFFF);
    }

    public static Container paymentGateway(String type, String info) {
        return containerBranded("GATEWAY", type + " Information", 
            info + "\n\n*Transaction telemetry sent to Highcore Financial.*", 
            BANNER_MAIN).withAccentColor(INFO.getRGB() & 0xFFFFFF);
    }

    public static Container meritAudit(int points) {
        return containerBranded("SECURITY", "Identity Assessment", 
            "Your current merit balance: **" + points + "**", 
            BANNER_MAIN).withAccentColor(GOLD.getRGB() & 0xFFFFFF);
    }

    public static Container accessDenied() {
        return containerBranded("SECURITY", "Access Denied", 
            "This terminal is restricted to authorized Highcore personnel.", 
            BANNER_MAIN).withAccentColor(DANGER.getRGB() & 0xFFFFFF);
    }

    public static Container stats(int total, int open, int closed, int claimed, String topStaff) {
        return Container.of(
            MediaGallery.of(MediaGalleryItem.fromUrl(BANNER_SUPPORT)),
            v2Header("ANALYTICS", "Agency Performance Matrix"),
            Separator.createDivider(Separator.Spacing.SMALL),
            TextDisplay.of("**Operational Metrics**\n> Total Tickets: **" + total + "**\n> Active Nodes: **" + open + "**\n> Resolved: **" + closed + "**"),
            TextDisplay.of("**Personnel Metrics**\n> Claimed: **" + claimed + "**\n> Top Operative: **" + topStaff + "**"),
            v2Footer()
        ).withAccentColor(INFO.getRGB() & 0xFFFFFF);
    }

    public static Container giveawayPanel() {
        return containerBranded("GIVEAWAY", "Reward Distribution", "Access the central giveaway node to manage community rewards and distributions.", BANNER_GIVEAWAY)
                .withAccentColor(GOLD.getRGB() & 0xFFFFFF);
    }

    public static Container help() {
        return containerBranded("HELP", "Documentation Sync", 
            "### \uD83D\uDCDC Operational Manual\n" +
            "> **Prefix:** `/` (Slash Commands)\n\n" +
            "**\uD83D\uDCCA Modules:**\n" +
            "- `/hub` - Central operational node\n" +
            "- `/tickets` - Support registry\n" +
            "- `/services` - Marketplace overview\n" +
            "- `/level` - Neural progression stats\n" +
            "- `/stats` - Agency telemetry", BANNER_MAIN);
    }

    public static Container startupPanel() {
        return containerBranded("STARTUP", "Agency Initializer", 
            "### \uD83D\uDE80 Welcome to Highcore Agency\n" +
            "Your professional development node is now active. Use the buttons below to navigate our services or initialize a project request.\n\n" +
            "**\u039E Modules:**\n" +
            "> \u2022 **Services & Prices:** View our development catalog\n" +
            "> \u2022 **Project Request:** Start a new venture with us\n" +
            "> \u2022 **Rules & Protocols:** Read the sector guidelines", 
            BANNER_MAIN);
    }

    public static Container ticketClosed(String tid, String closer) {
        return containerBranded("REGISTRY", "Sector Decomm", 
            "Ticket: **#" + tid + "**\nDecommissioned by: **" + closer + "**\n\n*Logs archived to Highcore Vault.*", BANNER_SUPPORT)
            .withAccentColor(DANGER.getRGB() & 0xFFFFFF);
    }

    public static Container withBanner(String banner) {
        return containerBranded("LINK", "Neural Transmission", "Synchronizing data stream with the current node...", banner);
    }

    public static Container custom(String title, String desc, String color, String image, String thumb, String aName, String aIcon, String fText, String fIcon, 
                                   String f1n, String f1v, Boolean f1i, String f2n, String f2v, Boolean f2i, String f3n, String f3v, Boolean f3i) {
        
        StringBuilder sb = new StringBuilder();
        if (desc != null) sb.append(desc).append("\n\n");
        if (f1n != null && f1v != null) sb.append("**").append(f1n).append(":** ").append(f1v).append("\n");
        if (f2n != null && f2v != null) sb.append("**").append(f2n).append(":** ").append(f2v).append("\n");
        if (f3n != null && f3v != null) sb.append("**").append(f3n).append(":** ").append(f3v).append("\n");

        return containerBranded(title != null ? title.toUpperCase() : "TRANSMISSION", "Custom Feed", sb.toString(), image != null ? image : BANNER_MAIN)
                .withAccentColor(parseColor(color).getRGB() & 0xFFFFFF);
    }

    public static Container simple(String title, String description) {
        return containerBranded("INFO", title, description, BANNER_MAIN);
    }

    public static Container success(String title, String description) {
        return containerBranded("SUCCESS", title, "\u2705 " + description, BANNER_MAIN)
                .withAccentColor(SUCCESS.getRGB() & 0xFFFFFF);
    }

    public static Container error(String title, String description) {
        return containerBranded("ERROR", title, "\u274C " + description, BANNER_MAIN)
                .withAccentColor(DANGER.getRGB() & 0xFFFFFF);
    }

    public static Container info(String title, String description) {
        return containerBranded("INFO", title, "\u2139\uFE0F " + description, BANNER_MAIN)
                .withAccentColor(INFO.getRGB() & 0xFFFFFF);
    }

    public static Container warning(String title, String description) {
        return containerBranded("WARNING", title, "\u26A0\uFE0F " + description, BANNER_MAIN)
                .withAccentColor(WARNING.getRGB() & 0xFFFFFF);
    }

    public static Container ratingThanks(int stars) {
        String s = "\u2B50".repeat(stars);
        return success("Feedback Locked", "Thank you for the " + s + " rating! Transmission received.");
    }
}
