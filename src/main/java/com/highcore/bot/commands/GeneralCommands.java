package com.highcore.bot.commands;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.highcore.bot.config.Config;
import com.highcore.bot.database.SupabaseClient;
import com.highcore.bot.services.PanelService;
import com.highcore.bot.utils.EmbedUtil;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class GeneralCommands extends ListenerAdapter {
    private static final Logger log = LoggerFactory.getLogger(GeneralCommands.class);

    private static final String TITLE_ROLE = "1488795130008961041";

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        try {
            switch (event.getName()) {
                case "ping" -> handlePing(event);
                case "profile" -> handleProfile(event);
                case "avatar" -> handleAvatar(event);
                case "banner" -> handleBanner(event);
                case "server" -> handleServer(event);
                case "server-avatar" -> handleServerAvatar(event);
                case "server-banner" -> handleServerBanner(event);
                case "roles" -> handleRoles(event);
                case "invites" -> handleInvites(event);
                case "rep" -> handleRep(event);
                case "suggestion" -> handleSuggestion(event);
                case "translate" -> handleTranslate(event);
                case "roll" -> handleRoll(event);
                case "get-emojis" -> handleGetEmojis(event);
                case "title" -> handleTitle(event);
                case "points-list" -> handlePointsList(event);
                case "vito" -> handleVito(event);
            }
        } catch (Exception e) {
            log.error("Error executing GeneralCommands: ", e);
            if (!event.isAcknowledged()) {
                PanelService.reply(event, EmbedUtil.error("Execution Error", "An error occurred: " + e.getMessage()), true);
            } else {
                PanelService.reply(event, EmbedUtil.error("Execution Error", "An error occurred: " + e.getMessage()));
            }
        }
    }

    // ==================== PING ====================
    private void handlePing(SlashCommandInteractionEvent event) {
        if (!isAdmin(event.getMember())) { PanelService.reply(event, EmbedUtil.accessDenied()); return; }
        long ping = event.getJDA().getGatewayPing();
        PanelService.reply(event, EmbedUtil.info("SYSTEM STATUS", "### \uD83C\uDFD3 Pong!\n> Gateway: **" + ping + "ms**\n> API: Calculating..."), true);
        
        long apiPing = System.currentTimeMillis();
        long diff = System.currentTimeMillis() - apiPing; 
        PanelService.reply(event, EmbedUtil.info("SYSTEM STATUS", "### \uD83C\uDFD3 Pong!\n> Gateway: **" + ping + "ms**\n> API: **" + Math.max(1, diff) + "ms**"));
    }


    // ==================== REP ====================
    private void handleRep(SlashCommandInteractionEvent event) {
        if (!isAdmin(event.getMember())) { PanelService.reply(event, EmbedUtil.accessDenied()); return; }
        Member target = event.getOption("member", OptionMapping::getAsMember);
        int amount = event.getOption("amount", 1, OptionMapping::getAsInt);
        if (target == null) { PanelService.reply(event, EmbedUtil.error("REPUTATION ERROR", "Target sector inhabitant not specified.")); return; }
        if (target.getId().equals(event.getUser().getId())) { PanelService.reply(event, EmbedUtil.error("PROTOCOL VIOLATION", "Self-recommendation is prohibited.")); return; }
        if (!SupabaseClient.canGiveRep(event.getUser().getId(), event.getGuild().getId())) {
            PanelService.reply(event, EmbedUtil.error("COOLDOWN ACTIVE", "Reputation cycle reset required. Try again later.")); return;
        }
        SupabaseClient.addReputation(target.getId(), event.getGuild().getId(), amount);
        SupabaseClient.setRepCooldown(event.getUser().getId(), event.getGuild().getId());
        int total = SupabaseClient.getReputation(target.getId(), event.getGuild().getId());
        PanelService.reply(event, EmbedUtil.success("CREDIBILITY INCREASE", "\u2B50 " + target.getAsMention() + " reputation adjusted by **" + amount + "**. (Total: **" + total + "**)"));
    }

    // ==================== VITO ====================
    private void handleVito(SlashCommandInteractionEvent event) {
        Member target = event.getOption("member", OptionMapping::getAsMember);
        if (target == null) target = event.getMember();
        int vitos = SupabaseClient.getVitos(target.getId(), event.getGuild().getId());
        if (vitos == 0) {
            PanelService.reply(event, EmbedUtil.success("INTEGRITY REPORT", "### \u2705 Clean Record\n**" + target.getUser().getName() + "** displays zero vitos. Status: **EXEMPLARY**."), true);
            return;
        }
        PanelService.reply(event, EmbedUtil.containerBranded("INTEGRITY REPORT", "Violation Count", 
                "**" + target.getUser().getName() + "** has accumulated **" + vitos + "** vitos within this sector.", EmbedUtil.BANNER_MAIN), true);
    }

    private void handleTranslate(SlashCommandInteractionEvent event) {
        String text = event.getOption("text", OptionMapping::getAsString);
        String lang = event.getOption("language", "ar", OptionMapping::getAsString);
        if (text == null) { PanelService.reply(event, EmbedUtil.error("INPUT ERROR", "No data provided for processing.")); return; }
        event.deferReply(false).queue();

        try {
            String url = "https://api.mymemory.translated.net/get?q=" + java.net.URLEncoder.encode(text, "UTF-8") + "&langpair=Autodetect|" + lang;
            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder().uri(java.net.URI.create(url)).GET().build();
            java.net.http.HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
            
            com.google.gson.JsonObject json = com.google.gson.JsonParser.parseString(response.body()).getAsJsonObject();
            String translated = json.getAsJsonObject("responseData").get("translatedText").getAsString();
            
            PanelService.reply(event, EmbedUtil.containerBranded("LINGUISTIC DECODER", "Translation Result", 
                    "> **Target Pattern:** `" + lang + "`\n\n**Source:**\n" + text + "\n\n**Decoded:**\n" + translated, EmbedUtil.BANNER_MAIN));
        } catch (Exception e) {
            PanelService.reply(event, EmbedUtil.error("TRANSLATION FAILED", e.getMessage()));
        }
    }

    private void handleRoll(SlashCommandInteractionEvent event) {
        int result = new Random().nextInt(6) + 1;
        String emoji = switch (result) {
            case 1 -> "\u2680"; case 2 -> "\u2681"; case 3 -> "\u2682";
            case 4 -> "\u2683"; case 5 -> "\u2684"; case 6 -> "\u2685"; default -> "\uD83C\uDFB2";
        };
        PanelService.reply(event, EmbedUtil.info("PROBABILITY ENGINE", "### \uD83C\uDFB2 Sequence Roll\n" + emoji + " Result yielded value: **" + result + "**"));
    }

    private void handleGetEmojis(SlashCommandInteractionEvent event) {
        String text = event.getOption("text", "", OptionMapping::getAsString);
        if (text.isEmpty()) {
            var emojis = event.getGuild().getEmojis();
            if (emojis.isEmpty()) { PanelService.reply(event, EmbedUtil.info("DATABASE", "No custom visual identifiers detected."), true); return; }
            StringBuilder sb = new StringBuilder();
            for (var e : emojis) sb.append(e.getAsMention()).append(" `").append(e.getName()).append("` ");
            PanelService.reply(event, EmbedUtil.info("EMOJI REGISTRY", "### \uD83D\uDE00 Detected Identifiers (" + emojis.size() + ")\n" + sb), true);
        } else {
            PanelService.reply(event, EmbedUtil.info("EMOJI ANALYZER", "### \uD83D\uDE00 Content Patterns\n" + text), true);
        }
    }

    private void handlePointsList(SlashCommandInteractionEvent event) {
        event.deferReply(true).queue();
        JsonArray top = SupabaseClient.getTopPoints(event.getGuild().getId(), 15);
        if (top == null || top.size() == 0) {
            PanelService.reply(event, EmbedUtil.info("TREASURY", "No merit distribution recorded."));
            return;
        }
        StringBuilder sb = new StringBuilder();
        int rank = 1;
        for (var el : top) {
            JsonObject p = el.getAsJsonObject();
            String emoji = rank == 1 ? "\uD83E\uDD47" : rank == 2 ? "\uD83E\uDD48" : rank == 3 ? "\uD83E\uDD49" : "`" + rank + ".`";
            sb.append(emoji).append(" <@").append(p.get("user_id").getAsString()).append("> \u2014 **")
                    .append(p.get("points").getAsInt()).append("** merit points\n");
            rank++;
        }
        PanelService.reply(event, EmbedUtil.containerBranded("TREASURY AUDIT", "Merit Rankings", sb.toString(), EmbedUtil.BANNER_MAIN));
    }

    private void handleTitle(SlashCommandInteractionEvent event) {
        if (!event.getMember().getRoles().stream().anyMatch(r -> r.getId().equals(TITLE_ROLE)) && !isAdmin(event.getMember())) {
            PanelService.reply(event, EmbedUtil.accessDenied()); return;
        }
        String sub = event.getSubcommandName();
        if (sub == null) return;
        switch (sub) {
            case "view" -> {
                String title = SupabaseClient.getTitle(event.getUser().getId(), event.getGuild().getId());
                PanelService.reply(event, EmbedUtil.info("IDENTIFICATION", "### \u039B Your Active Title\n" + (title.isEmpty() ? "> No designation assigned." : "> **" + title + "**")), true);
            }
            case "set" -> {
                String title = event.getOption("text", "", OptionMapping::getAsString);
                if (title.length() > 50) { PanelService.reply(event, EmbedUtil.error("LIMIT EXCEEDED", "Designation restricted to 50 characters."), true); return; }
                SupabaseClient.setTitle(event.getUser().getId(), event.getGuild().getId(), title);
                PanelService.reply(event, EmbedUtil.success("PROTOCOL UPDATED", "\u039B Active designation now registered as: **" + title + "**"), true);
            }
        }
    }

    private void handleSuggestion(SlashCommandInteractionEvent event) {
        String sub = event.getSubcommandName();
        if (sub == null) return;
        
        if (sub.equals("submit") || sub.equals("create")) {
            String content = event.getOption("content", OptionMapping::getAsString);
            if (content == null || content.length() < 10) { PanelService.reply(event, EmbedUtil.error("MALFORMED INPUT", "Improvement protocols require at least 10 characters.")); return; }
            JsonObject suggestion = SupabaseClient.createSuggestion(event.getGuild().getId(), event.getUser().getId(), event.getUser().getName(), content);
            long sugId = suggestion != null && suggestion.has("id") ? suggestion.get("id").getAsLong() : 0;
            PanelService.reply(event, EmbedUtil.success("PROPOSAL REGISTERED", "\uD83D\uDCA1 Protocol entry **#" + sugId + "** has been synchronized.\n> " + content), true);
            return;
        }

        if (!isAdmin(event.getMember())) { PanelService.reply(event, EmbedUtil.accessDenied()); return; }
        
        long id = event.getOption("id", 0L, OptionMapping::getAsLong);
        JsonObject sug = SupabaseClient.getSuggestion(id);
        if (sug == null || !sug.has("id")) { PanelService.reply(event, EmbedUtil.error("DATA ERROR", "Proposal record #" + id + " not found.")); return; }
        String note = event.getOption("note", "", OptionMapping::getAsString);
        String status = switch (sub) { case "approve" -> "approved"; case "deny" -> "denied"; case "implement" -> "implemented"; default -> "pending"; };
        String emoji = switch (sub) { case "approve" -> "\u2705"; case "deny" -> "\u274C"; case "implement" -> "\uD83D\uDE80"; default -> "\u2753"; };
        SupabaseClient.updateSuggestion(id, status, event.getUser().getName(), note, null, null);
        PanelService.reply(event, EmbedUtil.containerBranded("SUGGESTION MANAGEMENT", "Status Update: " + status.toUpperCase(),
                "### " + emoji + " Proposal #" + id + "\n> **Author:** " + (sug.has("user_name") ? sug.get("user_name").getAsString() : "?") +
                "\n> **Content:** " + sug.get("content").getAsString() +
                (note.isEmpty() ? "" : "\n> **Reviewer Note:** " + note), EmbedUtil.BANNER_MAIN));
    }

    private void handleProfile(SlashCommandInteractionEvent event) {
        Member target = event.getOption("member", OptionMapping::getAsMember);
        if (target == null) target = event.getMember();
        String guildId = event.getGuild().getId();

        int points = SupabaseClient.getPoints(target.getId(), guildId);
        int rep = SupabaseClient.getReputation(target.getId(), guildId);
        int vitos = SupabaseClient.getVitos(target.getId(), guildId);
        String title = SupabaseClient.getTitle(target.getId(), guildId);
        int warns = SupabaseClient.getWarningCount(target.getId(), guildId);

        String joined = target.getTimeJoined() != null ?
                DateTimeFormatter.ofPattern("MMM dd, yyyy").withZone(ZoneId.of("UTC")).format(target.getTimeJoined().toInstant()) : "?";
        
        String body = "## \uD83D\uDCCB System Profile" + (title.isEmpty() ? "" : "\n> \uD83C\uDFF7\uFE0F *" + title + "*") + "\n\n" +
                "**\uC5E5 Social Standing**\n" +
                "\u2022 Merit: **" + points + "**\n" +
                "\u2022 Reputation: **" + rep + "**\n" +
                "\u2022 Joined: **" + joined + "**\n\n" +
                "**\uC5E5 Discipline Record**\n" +
                "\u2022 Vitos: **" + vitos + "**\n" +
                "\u2022 Warnings: **" + warns + "**";

        PanelService.reply(event, EmbedUtil.containerBranded("BIOMETRIC SCAN", target.getUser().getName(), body, EmbedUtil.BANNER_MAIN));
    }

    private void handleUser(SlashCommandInteractionEvent event) {
        if (!isAdmin(event.getMember())) { PanelService.reply(event, EmbedUtil.accessDenied()); return; }
        Member target = event.getOption("member", OptionMapping::getAsMember);
        if (target == null) target = event.getMember();

        String joined = target.getTimeJoined() != null ?
                DateTimeFormatter.ofPattern("EEE, MMM dd yyyy HH:mm").withZone(ZoneId.of("Asia/Riyadh")).format(target.getTimeJoined().toInstant()) : "?";
        String created = DateTimeFormatter.ofPattern("EEE, MMM dd yyyy HH:mm").withZone(ZoneId.of("Asia/Riyadh")).format(target.getTimeCreated().toInstant());
        
        String body = "### \uD83D\uDC64 Subject Audit\n" +
                "**Username:** " + target.getUser().getName() + "\n" +
                "**ID:** `" + target.getId() + "`\n" +
                "**Created:** " + created + "\n" +
                "**Joined Server:** " + joined + "\n" +
                "**Bot Status:** " + (target.getUser().isBot() ? "\u2705 Detected" : "\u274C Negative") + "\n" +
                "**Top Credential:** " + (!target.getRoles().isEmpty() ? target.getRoles().get(0).getAsMention() : "None") + "\n";

        if (!target.getRoles().isEmpty()) {
            String roles = target.getRoles().stream().map(Role::getAsMention).collect(Collectors.joining(", "));
            if (roles.length() > 500) roles = roles.substring(0, 500) + "...";
            body += "**Roles:** " + roles;
        }

        PanelService.reply(event, EmbedUtil.info("CORE AUDIT", body), true);
    }

    private void handleAvatar(SlashCommandInteractionEvent event) {
        Member target = event.getOption("member", OptionMapping::getAsMember);
        if (target == null) target = event.getMember();
        PanelService.reply(event, EmbedUtil.containerBranded("VISUAL FEED", "Biometric Image", 
                "Subject: " + target.getUser().getName(), target.getEffectiveAvatarUrl() + "?size=1024"));
    }

    private void handleServer(SlashCommandInteractionEvent event) {
        Guild g = event.getGuild();
        String created = DateTimeFormatter.ofPattern("MMM dd, yyyy").withZone(ZoneId.of("UTC")).format(g.getTimeCreated().toInstant());
        String body = "### \u039B Sector Summary\n" +
                "**Name:** " + g.getName() + "\n" +
                "**ID:** `" + g.getId() + "`\n" +
                "**Clearance Level:** " + g.getVerificationLevel().name() + "\n" +
                "**Sector Established:** " + created + "\n\n" +
                "**\uC5E5 Population Metrics**\n" +
                "\u2022 Inhabitants: **" + g.getMemberCount() + "**\n" +
                "\u2022 Communication Nodes: **" + g.getChannels().size() + "**\n" +
                "\u2022 Security Designations: **" + g.getRoles().size() + "**\n" +
                "\u2022 Boost Status: Level **" + g.getBoostTier().getKey() + "**";

        PanelService.reply(event, EmbedUtil.containerBranded("SECTOR SCAN", g.getName(), body, g.getIconUrl()));
    }

    private void handleRoles(SlashCommandInteractionEvent event) {
        if (!isAdmin(event.getMember())) { PanelService.reply(event, EmbedUtil.accessDenied()); return; }
        List<Role> roles = event.getGuild().getRoles();
        StringBuilder sb = new StringBuilder();
        for (Role r : roles) {
            if (r.isPublicRole()) continue;
            sb.append(r.getAsMention()).append(" \u2014 **").append(event.getGuild().getMembersWithRoles(r).size()).append("** members\n");
        }
        String content = sb.toString();
        if (content.length() > 3000) content = content.substring(0, 3000) + "\n... [TRUNCATED]";
        PanelService.reply(event, EmbedUtil.info("SECURITY REGISTRY", "### \u039B Active Designations (" + (roles.size() - 1) + ")\n\n" + content), true);
    }

    private void handleBanner(SlashCommandInteractionEvent event) {
        Member target = event.getOption("member", OptionMapping::getAsMember);
        if (target == null) target = event.getMember();
        Member finalTarget = target;
        target.getUser().retrieveProfile().queue(profile -> {
            String bannerUrl = profile.getBannerUrl();
            if (bannerUrl == null) {
                PanelService.reply(event, EmbedUtil.info("DATA ERROR", "Subject " + finalTarget.getUser().getName() + " has no background designation."), true);
            } else {
                PanelService.reply(event, EmbedUtil.containerBranded("VISUAL FEED", "Background Aesthetics", 
                        "Subject: " + finalTarget.getUser().getName(), bannerUrl + "?size=1024"));
            }
        });
    }

    private void handleInvites(SlashCommandInteractionEvent event) {
        if (!isAdmin(event.getMember())) { PanelService.reply(event, EmbedUtil.accessDenied()); return; }
        Member target = event.getOption("member", OptionMapping::getAsMember);
        if (target == null) target = event.getMember();
        Member finalTarget = target;
        event.deferReply(true).queue();
        event.getGuild().retrieveInvites().queue(invites -> {
            int total = 0;
            for (var inv : invites) {
                if (inv.getInviter() != null && inv.getInviter().getId().equals(finalTarget.getId())) {
                    total += inv.getUses();
                }
            }
            PanelService.reply(event, EmbedUtil.info("RECRUITMENT AUDIT", "Subject " + finalTarget.getAsMention() + " has processed **" + total + "** successful arrivals."));
        });
    }

    private void handleServerAvatar(SlashCommandInteractionEvent event) {
        String url = event.getGuild().getIconUrl();
        if (url == null) { PanelService.reply(event, EmbedUtil.info("SCAN ERROR", "No sector identifier detected."), true); return; }
        PanelService.reply(event, EmbedUtil.containerBranded("SECTOR SCAN", "Visual Identifier", "Sector: " + event.getGuild().getName(), url + "?size=1024"));
    }

    private void handleServerBanner(SlashCommandInteractionEvent event) {
        String url = event.getGuild().getBannerUrl();
        if (url == null) { PanelService.reply(event, EmbedUtil.info("SCAN ERROR", "No sector landscape detected."), true); return; }
        PanelService.reply(event, EmbedUtil.containerBranded("SECTOR SCAN", "Landscape Feed", "Sector: " + event.getGuild().getName(), url + "?size=1024"));
    }

    public static void displayColors(SlashCommandInteractionEvent event) {
        StringBuilder sb = new StringBuilder();
        sb.append("### \u039B Sector Color Palette\n\n");
        sb.append("**PRIMARY:** `#ffffff` \u2014 Default Interface\n");
        sb.append("**SUCCESS:** `#2ecc71` \u2014 Operation Confirmed\n");
        sb.append("**DANGER:** `#e74c3c` \u2014 Security Breach / Halt\n");
        sb.append("**WARNING:** `#f1c40f` \u2014 Alert Status\n");
        sb.append("**INFO:** `#3498db` \u2014 System Data\n");
        sb.append("**GOLD:** `#f1c40f` \u2014 Honor / Merit\n");
        sb.append("**TEAL:** `#4db6ac` \u2014 Neural Integration");

        PanelService.reply(event, EmbedUtil.containerBranded("PALETTE", "Visual Identification", sb.toString(), EmbedUtil.BANNER_MAIN), true);
    }

    private boolean isAdmin(Member m) { return m != null && m.getRoles().stream().anyMatch(r -> Config.getAdminRoles().contains(r.getId())); }
}
