package com.highcore.bot.commands;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.highcore.bot.config.Config;
import com.highcore.bot.database.SupabaseClient;
import com.highcore.bot.services.LevelService;
import com.highcore.bot.services.PointsService;
import com.highcore.bot.utils.EmbedUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.RichCustomEmoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class GeneralCommands extends ListenerAdapter {
    private static final Logger log = LoggerFactory.getLogger(GeneralCommands.class);

    private static final String[][] DEFAULT_COLORS = {
            {"Sunset Orange", "FF6B35"}, {"Ocean Blue", "0077B6"}, {"Emerald Green", "2DC653"},
            {"Royal Purple", "7B2D8E"}, {"Rose Pink", "FF477E"}, {"Golden Yellow", "FFD166"}
    };

    private static final String TITLE_ROLE = "1488795130008961041";
    private static final String COLOR_ROLE_1 = "1488795130008961043";
    private static final String COLOR_ROLE_2 = "1488795130008961041";

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        switch (event.getName()) {
            case "ping" -> handlePing(event);
            case "colors" -> handleColors(event);
            case "color-set" -> handleColorSet(event);
            case "rep" -> handleRep(event);
            case "vito" -> handleVito(event);
            case "translate" -> handleTranslate(event);
            case "roll" -> handleRoll(event);
            case "get-emojis" -> handleGetEmojis(event);
            case "points-list" -> handlePointsList(event);
            case "title" -> handleTitle(event);
            case "suggest" -> handleSuggest(event);
            case "suggestion" -> handleSuggestion(event);
            case "profile" -> handleProfile(event);
            case "user" -> handleUser(event);
            case "avatar" -> handleAvatar(event);
            case "server" -> handleServer(event);
            case "roles" -> handleRoles(event);
            case "banner" -> handleBanner(event);
            case "invites" -> handleInvites(event);
            case "server-avatar" -> handleServerAvatar(event);
            case "server-banner" -> handleServerBanner(event);
        }
    }

    // ==================== PING ====================
    private void handlePing(SlashCommandInteractionEvent event) {
        if (!isAdmin(event.getMember())) { event.replyEmbeds(EmbedUtil.error("Unauthorized", "Admin only.")).setEphemeral(true).queue(); return; }
        long ping = event.getJDA().getGatewayPing();
        event.replyEmbeds(EmbedUtil.base().setColor(EmbedUtil.INFO)
                .setDescription("### \uD83C\uDFD3 Pong!\n> Gateway: **" + ping + "ms**\n> API: Calculating...")
                .build()).setEphemeral(true).queue(hook -> {
            long apiPing = System.currentTimeMillis();
            hook.editOriginalEmbeds(EmbedUtil.base().setColor(EmbedUtil.INFO)
                    .setDescription("### \uD83C\uDFD3 Pong!\n> Gateway: **" + ping + "ms**\n> API: **" + (System.currentTimeMillis() - apiPing) + "ms**")
                    .build()).queue();
        });
    }

    // ==================== COLORS ====================
    public static void displayColors(net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent event) {
        ensureColorRoles(event.getGuild());
        JsonArray colors = SupabaseClient.getColorRoles(event.getGuild().getId());
        if (colors == null || colors.size() == 0) {
            event.replyEmbeds(EmbedUtil.info("Colors", "No color roles available.")).setEphemeral(true).queue();
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (var el : colors) {
            JsonObject c = el.getAsJsonObject();
            int pos = c.get("position").getAsInt();
            sb.append("`").append(pos).append(".` **").append(c.get("color_name").getAsString())
                    .append("** \u2014 `#").append(c.get("color_hex").getAsString()).append("`\n");
        }
        event.replyEmbeds(EmbedUtil.base().setColor(EmbedUtil.ACCENT_PURPLE)
                .setDescription("## \uD83C\uDFA8 Available Colors\n\n" + sb + "\n> Use `/color-set <number>` to set your color!")
                .build()).queue();
    }

    private void handleColors(SlashCommandInteractionEvent event) {
        displayColors(event);
    }

    private void handleColorSet(SlashCommandInteractionEvent event) {
        Member m = event.getMember();
        if (!canUseColors(m)) { event.replyEmbeds(EmbedUtil.error("Unauthorized", "You don't have permission to use color roles.")).setEphemeral(true).queue(); return; }
        int number = event.getOption("number", 1, OptionMapping::getAsInt);
        ensureColorRoles(event.getGuild());
        JsonObject colorData = SupabaseClient.getColorRoleByPosition(event.getGuild().getId(), number);
        if (colorData == null) { event.replyEmbeds(EmbedUtil.error("Error", "Color #" + number + " not found. Use `/colors` to see available.")).setEphemeral(true).queue(); return; }

        String roleId = colorData.get("role_id").getAsString();
        Role newRole = event.getGuild().getRoleById(roleId);
        if (newRole == null) { event.replyEmbeds(EmbedUtil.error("Error", "Color role not found.")).setEphemeral(true).queue(); return; }

        // Remove existing color roles
        JsonArray allColors = SupabaseClient.getColorRoles(event.getGuild().getId());
        if (allColors != null) {
            for (var el : allColors) {
                String rid = el.getAsJsonObject().get("role_id").getAsString();
                Role r = event.getGuild().getRoleById(rid);
                if (r != null && m.getRoles().contains(r)) {
                    event.getGuild().removeRoleFromMember(m, r).queue(null, e -> {});
                }
            }
        }

        // Add new color role
        event.getGuild().addRoleToMember(m, newRole).queue(
                v -> event.replyEmbeds(EmbedUtil.success("Color Set", "\uD83C\uDFA8 Your color is now **" + colorData.get("color_name").getAsString() + "**!")).setEphemeral(true).queue(),
                e -> event.replyEmbeds(EmbedUtil.error("Error", e.getMessage())).setEphemeral(true).queue());
    }

    private static void ensureColorRoles(Guild guild) {
        JsonArray existing = SupabaseClient.getColorRoles(guild.getId());
        if (existing != null && existing.size() >= 6) return;

        for (int i = 0; i < DEFAULT_COLORS.length; i++) {
            String name = DEFAULT_COLORS[i][0];
            String hex = DEFAULT_COLORS[i][1];
            // Check if already exists by name
            boolean found = false;
            if (existing != null) {
                for (var el : existing) {
                    if (el.getAsJsonObject().get("color_name").getAsString().equals(name)) { found = true; break; }
                }
            }
            if (found) continue;

            // Check if role exists in guild
            final String colorName = name;
            Role role = guild.getRoles().stream().filter(r -> r.getName().equals(colorName)).findFirst().orElse(null);
            if (role == null) {
                try {
                    role = guild.createRole().setName(name).setColor(new Color(Integer.parseInt(hex, 16))).setPermissions(0L).complete();
                } catch (Exception e) { log.error("Failed to create color role {}: {}", name, e.getMessage()); continue; }
            }
            SupabaseClient.saveColorRole(guild.getId(), role.getId(), name, hex, i + 1);
        }
    }

    private boolean canUseColors(Member m) {
        if (isAdmin(m)) return true;
        return m.getRoles().stream().anyMatch(r -> r.getId().equals(COLOR_ROLE_1) || r.getId().equals(COLOR_ROLE_2));
    }

    // ==================== REP ====================
    private void handleRep(SlashCommandInteractionEvent event) {
        if (!isAdmin(event.getMember())) { event.replyEmbeds(EmbedUtil.error("Unauthorized", "Admin only.")).setEphemeral(true).queue(); return; }
        Member target = event.getOption("member", OptionMapping::getAsMember);
        if (target == null) { event.replyEmbeds(EmbedUtil.error("Error", "Specify a member.")).setEphemeral(true).queue(); return; }
        if (target.getId().equals(event.getUser().getId())) { event.replyEmbeds(EmbedUtil.error("Error", "You can't rep yourself.")).setEphemeral(true).queue(); return; }
        if (!SupabaseClient.canGiveRep(event.getUser().getId(), event.getGuild().getId())) {
            event.replyEmbeds(EmbedUtil.error("Cooldown", "You can only give rep once every 24 hours.")).setEphemeral(true).queue(); return;
        }
        SupabaseClient.addReputation(target.getId(), event.getGuild().getId());
        SupabaseClient.setRepCooldown(event.getUser().getId(), event.getGuild().getId());
        int total = SupabaseClient.getReputation(target.getId(), event.getGuild().getId());
        event.replyEmbeds(EmbedUtil.success("Rep Given", "\u2B50 " + target.getAsMention() + " received a reputation point! (Total: **" + total + "**)")).queue();
    }

    // ==================== VITO ====================
    private void handleVito(SlashCommandInteractionEvent event) {
        Member target = event.getOption("member", OptionMapping::getAsMember);
        if (target == null) target = event.getMember();
        int vitos = SupabaseClient.getVitos(target.getId(), event.getGuild().getId());
        event.replyEmbeds(EmbedUtil.base().setColor(EmbedUtil.GOLD)
                .setAuthor(target.getUser().getName(), null, target.getEffectiveAvatarUrl())
                .setDescription("### \uD83C\uDFC5 Vitos\n**" + target.getUser().getName() + "** has **" + vitos + "** vitos.").build())
                .setEphemeral(true).queue();
    }

    // ==================== TRANSLATE ====================
    private void handleTranslate(SlashCommandInteractionEvent event) {
        String text = event.getOption("text", OptionMapping::getAsString);
        String lang = event.getOption("language", "en", OptionMapping::getAsString);
        if (text == null) { event.replyEmbeds(EmbedUtil.error("Error", "Provide text.")).setEphemeral(true).queue(); return; }
        // Use a free translation approach - just show the request info
        event.replyEmbeds(EmbedUtil.base().setColor(EmbedUtil.INFO)
                .setDescription("### \uD83C\uDF10 Translation\n> **Target:** `" + lang + "`\n> **Text:** " + text +
                        "\n\n\u26A0\uFE0F Full translation requires an external API. Use Google Translate or DeepL for now.")
                .build()).setEphemeral(true).queue();
    }

    // ==================== ROLL ====================
    private void handleRoll(SlashCommandInteractionEvent event) {
        int result = new Random().nextInt(6) + 1;
        String emoji = switch (result) {
            case 1 -> "\u2680"; case 2 -> "\u2681"; case 3 -> "\u2682";
            case 4 -> "\u2683"; case 5 -> "\u2684"; case 6 -> "\u2685"; default -> "\uD83C\uDFB2";
        };
        event.replyEmbeds(EmbedUtil.base().setColor(EmbedUtil.INFO)
                .setDescription("### \uD83C\uDFB2 Dice Roll\n" + emoji + " You rolled a **" + result + "**!").build()).queue();
    }

    // ==================== GET EMOJIS ====================
    private void handleGetEmojis(SlashCommandInteractionEvent event) {
        String text = event.getOption("text", "", OptionMapping::getAsString);
        if (text.isEmpty()) {
            List<RichCustomEmoji> emojis = event.getGuild().getEmojis();
            if (emojis.isEmpty()) { event.replyEmbeds(EmbedUtil.info("Emojis", "No custom emojis.")).setEphemeral(true).queue(); return; }
            StringBuilder sb = new StringBuilder();
            for (var e : emojis) sb.append(e.getAsMention()).append(" `").append(e.getName()).append("` ");
            event.replyEmbeds(EmbedUtil.base().setColor(EmbedUtil.INFO)
                    .setDescription("### \uD83D\uDE00 Server Emojis (" + emojis.size() + ")\n" + sb).build()).setEphemeral(true).queue();
        } else {
            // Extract emoji codes from the text
            event.replyEmbeds(EmbedUtil.base().setColor(EmbedUtil.INFO)
                    .setDescription("### \uD83D\uDE00 Emojis in Text\n" + text).build()).setEphemeral(true).queue();
        }
    }

    // ==================== POINTS LIST ====================
    private void handlePointsList(SlashCommandInteractionEvent event) {
        if (!isAdmin(event.getMember())) { event.replyEmbeds(EmbedUtil.error("Unauthorized", "Admin only.")).setEphemeral(true).queue(); return; }
        event.deferReply(true).queue();
        JsonArray all = SupabaseClient.getTopPoints(event.getGuild().getId(), 50);
        if (all == null || all.size() == 0) { event.getHook().editOriginalEmbeds(EmbedUtil.info("Points", "No data.")).queue(); return; }
        StringBuilder sb = new StringBuilder();
        int i = 1;
        for (var el : all) {
            JsonObject p = el.getAsJsonObject();
            sb.append("`").append(i++).append(".` <@").append(p.get("user_id").getAsString()).append("> \u2014 **")
                    .append(p.get("points").getAsInt()).append("** pts\n");
        }
        event.getHook().editOriginalEmbeds(EmbedUtil.base().setColor(EmbedUtil.GOLD)
                .setDescription("## \u2B50 All Points\n\n" + sb).build()).queue();
    }

    // ==================== TITLE ====================
    private void handleTitle(SlashCommandInteractionEvent event) {
        if (!event.getMember().getRoles().stream().anyMatch(r -> r.getId().equals(TITLE_ROLE)) && !isAdmin(event.getMember())) {
            event.replyEmbeds(EmbedUtil.error("Unauthorized", "You need the required role.")).setEphemeral(true).queue(); return;
        }
        String sub = event.getSubcommandName();
        if (sub == null) return;
        switch (sub) {
            case "view" -> {
                String title = SupabaseClient.getTitle(event.getUser().getId(), event.getGuild().getId());
                event.replyEmbeds(EmbedUtil.base().setColor(EmbedUtil.INFO)
                        .setDescription("### \uD83C\uDFF7\uFE0F Your Title\n" + (title.isEmpty() ? "> No title set." : "> **" + title + "**"))
                        .build()).setEphemeral(true).queue();
            }
            case "set" -> {
                String title = event.getOption("text", "", OptionMapping::getAsString);
                if (title.length() > 50) { event.replyEmbeds(EmbedUtil.error("Error", "Title max 50 characters.")).setEphemeral(true).queue(); return; }
                SupabaseClient.setTitle(event.getUser().getId(), event.getGuild().getId(), title);
                event.replyEmbeds(EmbedUtil.success("Title Set", "\uD83C\uDFF7\uFE0F Your title: **" + title + "**")).setEphemeral(true).queue();
            }
        }
    }

    // ==================== SUGGEST ====================
    private void handleSuggest(SlashCommandInteractionEvent event) {
        String content = event.getOption("content", OptionMapping::getAsString);
        if (content == null || content.length() < 10) { event.replyEmbeds(EmbedUtil.error("Error", "Suggestion must be at least 10 characters.")).setEphemeral(true).queue(); return; }
        JsonObject suggestion = SupabaseClient.createSuggestion(event.getGuild().getId(), event.getUser().getId(), event.getUser().getName(), content);
        long sugId = suggestion != null ? suggestion.get("id").getAsLong() : 0;
        event.replyEmbeds(EmbedUtil.success("Suggestion Submitted",
                "\uD83D\uDCA1 Your suggestion **#" + sugId + "** has been submitted!\n> " + content)).setEphemeral(true).queue();
    }

    private void handleSuggestion(SlashCommandInteractionEvent event) {
        if (!isAdmin(event.getMember())) { event.replyEmbeds(EmbedUtil.error("Unauthorized", "Admin only.")).setEphemeral(true).queue(); return; }
        String sub = event.getSubcommandName();
        if (sub == null) return;
        long id = event.getOption("id", 0L, OptionMapping::getAsLong);
        JsonObject sug = SupabaseClient.getSuggestion(id);
        if (sug == null) { event.replyEmbeds(EmbedUtil.error("Error", "Suggestion #" + id + " not found.")).setEphemeral(true).queue(); return; }
        String note = event.getOption("note", "", OptionMapping::getAsString);
        String status = switch (sub) { case "approve" -> "approved"; case "deny" -> "denied"; case "implement" -> "implemented"; default -> "pending"; };
        String emoji = switch (sub) { case "approve" -> "\u2705"; case "deny" -> "\u274C"; case "implement" -> "\uD83D\uDE80"; default -> "\u2753"; };
        SupabaseClient.updateSuggestion(id, status, event.getUser().getName(), note, null, null);
        event.replyEmbeds(EmbedUtil.base().setColor(sub.equals("deny") ? EmbedUtil.DANGER : EmbedUtil.SUCCESS)
                .setDescription("### " + emoji + " Suggestion #" + id + " " + status.toUpperCase() +
                        "\n> **By:** " + (sug.has("user_name") ? sug.get("user_name").getAsString() : "?") +
                        "\n> **Content:** " + sug.get("content").getAsString() +
                        (note.isEmpty() ? "" : "\n> **Note:** " + note)).build()).queue();
    }

    // ==================== PROFILE ====================
    private void handleProfile(SlashCommandInteractionEvent event) {
        Member target = event.getOption("member", OptionMapping::getAsMember);
        if (target == null) target = event.getMember();
        String guildId = event.getGuild().getId();

        // Gather all data
        JsonObject levelData = SupabaseClient.getLevelData(target.getId(), guildId);
        int xp = levelData != null && levelData.has("xp") ? levelData.get("xp").getAsInt() : 0;
        int level = levelData != null && levelData.has("level") ? levelData.get("level").getAsInt() : 0;
        int messages = levelData != null && levelData.has("messages") ? levelData.get("messages").getAsInt() : 0;
        int points = SupabaseClient.getPoints(target.getId(), guildId);
        int rep = SupabaseClient.getReputation(target.getId(), guildId);
        int vitos = SupabaseClient.getVitos(target.getId(), guildId);
        String title = SupabaseClient.getTitle(target.getId(), guildId);
        int warns = SupabaseClient.getWarningCount(target.getId(), guildId);

        String joined = target.getTimeJoined() != null ?
                DateTimeFormatter.ofPattern("MMM dd, yyyy").withZone(ZoneId.of("UTC")).format(target.getTimeJoined().toInstant()) : "?";
        String created = DateTimeFormatter.ofPattern("MMM dd, yyyy").withZone(ZoneId.of("UTC")).format(target.getTimeCreated().toInstant());

        EmbedBuilder eb = EmbedUtil.base().setColor(EmbedUtil.PRIMARY)
                .setAuthor(target.getUser().getName(), null, target.getEffectiveAvatarUrl())
                .setThumbnail(target.getEffectiveAvatarUrl())
                .setDescription("## \uD83D\uDCCB Profile" + (title.isEmpty() ? "" : "\n> \uD83C\uDFF7\uFE0F *" + title + "*"));

        eb.addField("\uD83C\uDFC5 Level", "**" + level + "**", true)
                .addField("\u2728 XP", "**" + xp + "**", true)
                .addField("\uD83D\uDCAC Messages", "**" + messages + "**", true)
                .addField("\u2B50 Points", "**" + points + "**", true)
                .addField("\uD83C\uDFC5 Rep", "**" + rep + "**", true)
                .addField("\uD83C\uDFC5 Vitos", "**" + vitos + "**", true)
                .addField("\u26A0\uFE0F Warnings", "**" + warns + "**", true)
                .addField("\uD83D\uDCC5 Joined", joined, true)
                .addField("\uD83D\uDCC5 Created", created, true);

        if (!target.getRoles().isEmpty()) {
            String roles = target.getRoles().stream().limit(10).map(Role::getAsMention).collect(Collectors.joining(", "));
            eb.addField("\uD83C\uDFAD Roles (" + target.getRoles().size() + ")", roles, false);
        }

        event.replyEmbeds(eb.build()).queue();
    }

    // ==================== USER INFO ====================
    private void handleUser(SlashCommandInteractionEvent event) {
        if (!isAdmin(event.getMember())) { event.replyEmbeds(EmbedUtil.error("Unauthorized", "Admin only.")).setEphemeral(true).queue(); return; }
        Member target = event.getOption("member", OptionMapping::getAsMember);
        if (target == null) target = event.getMember();

        String joined = target.getTimeJoined() != null ?
                DateTimeFormatter.ofPattern("EEE, MMM dd yyyy HH:mm").withZone(ZoneId.of("Asia/Riyadh")).format(target.getTimeJoined().toInstant()) : "?";
        String created = DateTimeFormatter.ofPattern("EEE, MMM dd yyyy HH:mm").withZone(ZoneId.of("Asia/Riyadh")).format(target.getTimeCreated().toInstant());
        long daysInServer = target.getTimeJoined() != null ?
                (Instant.now().getEpochSecond() - target.getTimeJoined().toEpochSecond()) / 86400 : 0;

        EmbedBuilder eb = EmbedUtil.base().setColor(EmbedUtil.INFO)
                .setAuthor(target.getUser().getName(), null, target.getEffectiveAvatarUrl())
                .setThumbnail(target.getEffectiveAvatarUrl())
                .setDescription("### \uD83D\uDC64 User Information")
                .addField("\uD83D\uDC64 Username", target.getUser().getName(), true)
                .addField("\uD83D\uDCDB Nickname", target.getNickname() != null ? target.getNickname() : "None", true)
                .addField("\uD83C\uDD94 ID", "`" + target.getId() + "`", true)
                .addField("\uD83D\uDCC5 Joined", joined + "\n(" + daysInServer + " days ago)", true)
                .addField("\uD83D\uDCC5 Created", created, true)
                .addField("\uD83D\uDD34 Status", target.getOnlineStatus().name(), true)
                .addField("\uD83E\uDD16 Bot", target.getUser().isBot() ? "Yes" : "No", true)
                .addField("\u2B06\uFE0F Top Role", !target.getRoles().isEmpty() ? target.getRoles().get(0).getAsMention() : "None", true)
                .addField("\uD83D\uDD10 Permissions", String.valueOf(target.getPermissions().size()), true);

        if (!target.getRoles().isEmpty()) {
            String roles = target.getRoles().stream().map(Role::getAsMention).collect(Collectors.joining(", "));
            if (roles.length() > 900) roles = roles.substring(0, 900) + "...";
            eb.addField("\uD83C\uDFAD Roles (" + target.getRoles().size() + ")", roles, false);
        }

        event.replyEmbeds(eb.build()).setEphemeral(true).queue();
    }

    // ==================== AVATAR ====================
    private void handleAvatar(SlashCommandInteractionEvent event) {
        Member target = event.getOption("member", OptionMapping::getAsMember);
        if (target == null) target = event.getMember();
        String url = target.getEffectiveAvatarUrl() + "?size=1024";
        event.replyEmbeds(EmbedUtil.base().setColor(EmbedUtil.INFO)
                .setAuthor(target.getUser().getName(), null, target.getEffectiveAvatarUrl())
                .setDescription("### \uD83D\uDDBC\uFE0F Avatar")
                .setImage(url).build()).queue();
    }

    // ==================== SERVER INFO ====================
    private void handleServer(SlashCommandInteractionEvent event) {
        Guild g = event.getGuild();
        String created = DateTimeFormatter.ofPattern("MMM dd, yyyy").withZone(ZoneId.of("UTC")).format(g.getTimeCreated().toInstant());
        event.replyEmbeds(EmbedUtil.base().setColor(EmbedUtil.PRIMARY)
                .setAuthor(g.getName(), null, g.getIconUrl())
                .setThumbnail(g.getIconUrl())
                .setDescription("### \uD83C\uDFE0 Server Information")
                .addField("\uD83C\uDD94 ID", "`" + g.getId() + "`", true)
                .addField("\uD83D\uDC51 Owner", g.getOwner() != null ? g.getOwner().getAsMention() : "?", true)
                .addField("\uD83D\uDCC5 Created", created, true)
                .addField("\uD83D\uDC65 Members", "**" + g.getMemberCount() + "**", true)
                .addField("\uD83D\uDCAC Channels", "**" + g.getChannels().size() + "**", true)
                .addField("\uD83C\uDFAD Roles", "**" + g.getRoles().size() + "**", true)
                .addField("\uD83D\uDE00 Emojis", "**" + g.getEmojis().size() + "**", true)
                .addField("\uD83D\uDD10 Verification", g.getVerificationLevel().name(), true)
                .addField("\u2B06\uFE0F Boost", "Level **" + g.getBoostTier().getKey() + "** (" + g.getBoostCount() + " boosts)", true)
                .build()).queue();
    }

    // ==================== ROLES LIST ====================
    private void handleRoles(SlashCommandInteractionEvent event) {
        if (!isAdmin(event.getMember())) { event.replyEmbeds(EmbedUtil.error("Unauthorized", "Admin only.")).setEphemeral(true).queue(); return; }
        List<Role> roles = event.getGuild().getRoles();
        StringBuilder sb = new StringBuilder();
        for (Role r : roles) {
            if (r.isPublicRole()) continue;
            sb.append(r.getAsMention()).append(" \u2014 **").append(event.getGuild().getMembersWithRoles(r).size()).append("** members\n");
        }
        if (sb.length() > 3500) sb = new StringBuilder(sb.substring(0, 3500) + "\n...");
        event.replyEmbeds(EmbedUtil.base().setColor(EmbedUtil.INFO)
                .setDescription("### \uD83C\uDFAD Server Roles (" + (roles.size() - 1) + ")\n\n" + sb).build()).setEphemeral(true).queue();
    }

    // ==================== BANNER ====================
    private void handleBanner(SlashCommandInteractionEvent event) {
        Member target = event.getOption("member", OptionMapping::getAsMember);
        if (target == null) target = event.getMember();
        Member finalTarget = target;
        target.getUser().retrieveProfile().queue(profile -> {
            String bannerUrl = profile.getBannerUrl();
            if (bannerUrl == null) {
                event.replyEmbeds(EmbedUtil.info("Banner", finalTarget.getUser().getName() + " has no banner.")).setEphemeral(true).queue();
            } else {
                event.replyEmbeds(EmbedUtil.base().setColor(EmbedUtil.INFO)
                        .setAuthor(finalTarget.getUser().getName(), null, finalTarget.getEffectiveAvatarUrl())
                        .setDescription("### \uD83C\uDFF3\uFE0F Banner").setImage(bannerUrl + "?size=1024").build()).queue();
            }
        });
    }

    // ==================== INVITES ====================
    private void handleInvites(SlashCommandInteractionEvent event) {
        if (!isAdmin(event.getMember())) { event.replyEmbeds(EmbedUtil.error("Unauthorized", "Admin only.")).setEphemeral(true).queue(); return; }
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
            event.getHook().editOriginalEmbeds(EmbedUtil.base().setColor(EmbedUtil.INFO)
                    .setAuthor(finalTarget.getUser().getName(), null, finalTarget.getEffectiveAvatarUrl())
                    .setDescription("### \uD83D\uDCE8 Invites\n" + finalTarget.getAsMention() + " has **" + total + "** invites.").build()).queue();
        });
    }

    // ==================== SERVER AVATAR / BANNER ====================
    private void handleServerAvatar(SlashCommandInteractionEvent event) {
        String url = event.getGuild().getIconUrl();
        if (url == null) { event.replyEmbeds(EmbedUtil.info("Server", "No server icon.")).setEphemeral(true).queue(); return; }
        event.replyEmbeds(EmbedUtil.base().setColor(EmbedUtil.INFO)
                .setDescription("### \uD83C\uDFE0 Server Icon").setImage(url + "?size=1024").build()).queue();
    }

    private void handleServerBanner(SlashCommandInteractionEvent event) {
        String url = event.getGuild().getBannerUrl();
        if (url == null) { event.replyEmbeds(EmbedUtil.info("Server", "No server banner.")).setEphemeral(true).queue(); return; }
        event.replyEmbeds(EmbedUtil.base().setColor(EmbedUtil.INFO)
                .setDescription("### \uD83C\uDFF3\uFE0F Server Banner").setImage(url + "?size=1024").build()).queue();
    }

    private boolean isAdmin(Member m) { return m != null && m.getRoles().stream().anyMatch(r -> Config.getAdminRoles().contains(r.getId())); }
}
