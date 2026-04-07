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
                case "rep" -> handleRep(event);
                case "suggestion" -> handleSuggestion(event);
                case "title" -> handleTitle(event);
                case "leaderboard" -> handlePointsList(event);
                case "vito" -> handleVito(event);
            }
        } catch (Exception e) {
            log.error("Error executing GeneralCommands: ", e);
            if (!event.isAcknowledged()) {
                PanelService.replyEphemeral(event, EmbedUtil.error("Execution Error", "An error occurred: " + e.getMessage()));
            } else {
                PanelService.reply(event, EmbedUtil.error("Execution Error", "An error occurred: " + e.getMessage()));
            }
        }
    }

    private void handlePing(SlashCommandInteractionEvent event) {
        if (!isAdmin(event.getMember())) { PanelService.replyEphemeral(event, EmbedUtil.accessDenied()); return; }
        long ping = event.getJDA().getGatewayPing();
        PanelService.replyEphemeral(event, EmbedUtil.info("SYSTEM STATUS", "### \uD83C\uDFD3 Pong!\n> Gateway: **" + ping + "ms**\n> API: Calculating..."));
    }

    private void handleRep(SlashCommandInteractionEvent event) {
        Member target = event.getOption("member", OptionMapping::getAsMember);
        int amount = event.getOption("amount", 1, OptionMapping::getAsInt);
        if (target == null) { PanelService.replyEphemeral(event, EmbedUtil.error("REPUTATION ERROR", "Target user not specified.")); return; }
        if (target.getId().equals(event.getUser().getId())) { PanelService.replyEphemeral(event, EmbedUtil.error("PROTOCOL VIOLATION", "Self-recommendation is prohibited.")); return; }
        if (!SupabaseClient.canGiveRep(event.getUser().getId(), event.getGuild().getId())) {
            PanelService.replyEphemeral(event, EmbedUtil.error("COOLDOWN ACTIVE", "Reputation cycle reset required. Try again later.")); return;
        }
        SupabaseClient.addReputation(target.getId(), event.getGuild().getId(), amount);
        SupabaseClient.setRepCooldown(event.getUser().getId(), event.getGuild().getId());
        int total = SupabaseClient.getReputation(target.getId(), event.getGuild().getId());
        PanelService.reply(event, EmbedUtil.success("CREDIBILITY INCREASE", "\u2B50 " + target.getAsMention() + " reputation adjusted by **" + amount + "**. (Total: **" + total + "**)"));
    }

    private void handleVito(SlashCommandInteractionEvent event) {
        Member target = event.getOption("member", OptionMapping::getAsMember);
        if (target == null) target = event.getMember();
        int vitos = SupabaseClient.getVitos(target.getId(), event.getGuild().getId());
        if (vitos == 0) {
            PanelService.replyEphemeral(event, EmbedUtil.success("RECORD STATUS", "### ✅ Clear Status\n**" + target.getUser().getName() + "** has a clean record with zero violations."));
            return;
        }
        PanelService.replyEphemeral(event, EmbedUtil.containerBranded("STATUS AUDIT", "Active Violations", 
                "**" + target.getUser().getName() + "** has accumulated **" + vitos + "** warnings in the agency.", EmbedUtil.BANNER_MAIN));
    }

    private void handlePointsList(SlashCommandInteractionEvent event) {
        event.deferReply(true).queue();
        JsonArray top = SupabaseClient.getTopPoints(event.getGuild().getId(), 15);
        if (top == null || top.size() == 0) {
            PanelService.replyEphemeral(event, EmbedUtil.info("TREASURY", "No merit distribution recorded."));
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
            PanelService.replyEphemeral(event, EmbedUtil.accessDenied()); return;
        }
        String sub = event.getSubcommandName();
        if (sub == null) return;
        switch (sub) {
            case "view" -> {
                String title = SupabaseClient.getTitle(event.getUser().getId(), event.getGuild().getId());
                PanelService.replyEphemeral(event, EmbedUtil.info("IDENTIFICATION", "### \u039B Your Active Title\n" + (title.isEmpty() ? "> No designation assigned." : "> **" + title + "**")));
            }
            case "set" -> {
                String title = event.getOption("text", "", OptionMapping::getAsString);
                if (title.length() > 50) { PanelService.replyEphemeral(event, EmbedUtil.error("LIMIT EXCEEDED", "Designation restricted to 50 characters.")); return; }
                SupabaseClient.setTitle(event.getUser().getId(), event.getGuild().getId(), title);
                PanelService.replyEphemeral(event, EmbedUtil.success("PROTOCOL UPDATED", "\u039B Active designation now registered as: **" + title + "**"));
            }
        }
    }

    private void handleSuggestion(SlashCommandInteractionEvent event) {
        String sub = event.getSubcommandName();
        if (sub == null) return;
        
        if (sub.equals("submit") || sub.equals("create")) {
            String content = event.getOption("content", OptionMapping::getAsString);
            if (content == null || content.length() < 10) { PanelService.replyEphemeral(event, EmbedUtil.error("MALFORMED INPUT", "Improvement protocols require at least 10 characters.")); return; }
            JsonObject suggestion = SupabaseClient.createSuggestion(event.getGuild().getId(), event.getUser().getId(), event.getUser().getName(), content);
            long sugId = suggestion != null && suggestion.has("id") ? suggestion.get("id").getAsLong() : 0;
            PanelService.replyEphemeral(event, EmbedUtil.success("PROPOSAL REGISTERED", "\uD83D\uDCA1 Protocol entry **#" + sugId + "** has been synchronized.\n> " + content));
            return;
        }

        if (!isAdmin(event.getMember())) { PanelService.replyEphemeral(event, EmbedUtil.accessDenied()); return; }
        
        long id = event.getOption("id", 0L, OptionMapping::getAsLong);
        JsonObject sug = SupabaseClient.getSuggestion(id);
        if (sug == null || !sug.has("id")) { PanelService.replyEphemeral(event, EmbedUtil.error("DATA ERROR", "Proposal record #" + id + " not found.")); return; }
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
        
        String body = "## 👤 User Profile" + (title.isEmpty() ? "" : "\n> 🏷️ *" + title + "*") + "\n\n" +
                "**✨ Agency Standing**\n" +
                "\u2022 Merit: **" + points + "**\n" +
                "\u2022 Reputation: **" + rep + "**\n" +
                "\u2022 Joined: **" + joined + "**\n\n" +
                "**📄 Record Summary**\n" +
                "\u2022 Warnings: **" + warns + "**\n" +
                "\u2022 Admin Notes: **" + vitos + "**";

        PanelService.reply(event, EmbedUtil.containerBranded("PROFILE SUMMARY", target.getUser().getName(), body, EmbedUtil.BANNER_MAIN));
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

    private void handleBanner(SlashCommandInteractionEvent event) {
        Member target = event.getOption("member", OptionMapping::getAsMember);
        if (target == null) target = event.getMember();
        Member finalTarget = target;
        target.getUser().retrieveProfile().queue(profile -> {
            String bannerUrl = profile.getBannerUrl();
            if (bannerUrl == null) {
                PanelService.replyEphemeral(event, EmbedUtil.info("DATA ERROR", "Subject " + finalTarget.getUser().getName() + " has no background designation."));
            } else {
                PanelService.reply(event, EmbedUtil.containerBranded("VISUAL FEED", "Background Aesthetics", 
                        "Subject: " + finalTarget.getUser().getName(), bannerUrl + "?size=1024"));
            }
        });
    }

    public static void displayColors(SlashCommandInteractionEvent event) {
        StringBuilder sb = new StringBuilder();
        sb.append("### \u039B Sector Color Palette\n\n");
        sb.append("**PRIMARY:** `#ffffff` \u2014 Default Interface\n");
        sb.append("**SUCCESS:** `#10b981` \u2014 Operation Confirmed\n");
        sb.append("**DANGER:** `#ef4444` \u2014 Security Breach / Halt\n");
        sb.append("**WARNING:** `#f59e0b` \u2014 Alert Status\n");
        sb.append("**INFO:** `#3b82f6` \u2014 System Data\n");
        sb.append("**GOLD:** `#f1c40f` \u2014 Honor / Merit\n");
        sb.append("**TEAL:** `#14b8a6` \u2014 Modern Agency Accent");

        PanelService.replyEphemeral(event, EmbedUtil.containerBranded("PALETTE", "Visual Identification", sb.toString(), EmbedUtil.BANNER_MAIN));
    }

    private boolean isAdmin(Member m) { return m != null && m.getRoles().stream().anyMatch(r -> Config.getAdminRoles().contains(r.getId())); }
}
