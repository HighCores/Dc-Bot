package com.highcore.bot.commands;

import com.highcore.bot.config.Config;
import com.highcore.bot.database.SupabaseClient;
import com.highcore.bot.services.LogManager;
import com.highcore.bot.utils.EmbedUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class ModerationCommands extends ListenerAdapter {
    private static final Logger log = LoggerFactory.getLogger(ModerationCommands.class);
    private static final ScheduledExecutorService tempRoleScheduler = Executors.newSingleThreadScheduledExecutor();

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!isAdmin(event.getMember())) return; // Only handle if admin, otherwise ignore (let other listeners handle)

        switch (event.getName()) {
            case "setnick" -> handleSetNick(event);
            case "ban" -> handleBan(event);
            case "unban" -> handleUnban(event);
            case "unban-all" -> handleUnbanAll(event);
            case "kick" -> handleKick(event);
            case "vkick" -> handleVKick(event);
            case "mute-text" -> handleMuteText(event);
            case "unmute-text" -> handleUnmuteText(event);
            case "mute-check" -> handleMuteCheck(event);
            case "mute-voice" -> handleMuteVoice(event);
            case "unmute-voice" -> handleUnmuteVoice(event);
            case "timeout" -> handleTimeout(event);
            case "untimeout" -> handleUntimeout(event);
            case "clear" -> handleClear(event);
            case "move" -> handleMove(event);
            case "role" -> handleRole(event);
            case "temprole" -> handleTempRole(event);
            case "rar" -> handleRAR(event);
            case "inrole" -> handleInRole(event);
            case "warn-add" -> handleWarnAdd(event);
            case "warn-remove" -> handleWarnRemove(event);
            case "warnings" -> handleWarnings(event);
            case "violations" -> handleViolations(event);
            case "violations-clear" -> handleViolationsClear(event);
            case "lock" -> handleLock(event);
            case "unlock" -> handleUnlock(event);
            case "hide" -> handleHide(event);
            case "show" -> handleShow(event);
            case "slowmode" -> handleSlowmode(event);
            case "add-emoji" -> handleAddEmoji(event);
            case "role-multiple" -> handleRoleMultiple(event);
        }
    }

    // ==================== NICK ====================
    private void handleSetNick(SlashCommandInteractionEvent event) {
        Member target = event.getOption("member", OptionMapping::getAsMember);
        String nick = event.getOption("nickname", OptionMapping::getAsString);
        if (target == null) { event.replyEmbeds(EmbedUtil.error("Error", "Member not found.")).setEphemeral(true).queue(); return; }
        String old = target.getNickname() != null ? target.getNickname() : target.getUser().getName();
        event.getGuild().modifyNickname(target, nick).queue(
                v -> event.replyEmbeds(EmbedUtil.success("Nickname Changed", "**" + old + "** → **" + (nick != null ? nick : "reset") + "**")).setEphemeral(true).queue(),
                e -> event.replyEmbeds(EmbedUtil.error("Error", e.getMessage())).setEphemeral(true).queue());
        logMod(event, "Nickname Changed", target.getUser(), "Old: " + old + " → New: " + (nick != null ? nick : "reset"));
    }

    // ==================== BAN / UNBAN ====================
    private void handleBan(SlashCommandInteractionEvent event) {
        Member target = event.getOption("member", OptionMapping::getAsMember);
        String reason = event.getOption("reason", "No reason provided", OptionMapping::getAsString);
        int days = event.getOption("delete_days", 0, OptionMapping::getAsInt);
        if (target == null) { event.replyEmbeds(EmbedUtil.error("Error", "Member not found.")).setEphemeral(true).queue(); return; }
        if (!event.getGuild().getSelfMember().canInteract(target)) { event.replyEmbeds(EmbedUtil.error("Error", "Cannot ban this member (higher role).")).setEphemeral(true).queue(); return; }
        target.getUser().openPrivateChannel().queue(dm -> dm.sendMessageEmbeds(EmbedUtil.warning("Banned",
                "You have been banned from **" + event.getGuild().getName() + "**\nReason: " + reason)).queue(null, e -> {}), e -> {});
        event.getGuild().ban(target, days, TimeUnit.DAYS).reason(reason).queue(
                v -> event.replyEmbeds(EmbedUtil.success("Banned", "**" + target.getUser().getName() + "** has been banned.\nReason: " + reason)).queue(),
                e -> event.replyEmbeds(EmbedUtil.error("Error", e.getMessage())).setEphemeral(true).queue());
        logMod(event, "Member Banned", target.getUser(), reason);
    }

    private void handleUnban(SlashCommandInteractionEvent event) {
        String userId = event.getOption("user_id", OptionMapping::getAsString);
        if (userId == null) { event.replyEmbeds(EmbedUtil.error("Error", "Provide a user ID.")).setEphemeral(true).queue(); return; }
        event.getGuild().unban(UserSnowflake.fromId(userId)).queue(
                v -> event.replyEmbeds(EmbedUtil.success("Unbanned", "User `" + userId + "` has been unbanned.")).queue(),
                e -> event.replyEmbeds(EmbedUtil.error("Error", e.getMessage())).setEphemeral(true).queue());
        logMod(event, "Member Unbanned", null, "User ID: " + userId);
    }

    private void handleUnbanAll(SlashCommandInteractionEvent event) {
        event.deferReply().queue();
        event.getGuild().retrieveBanList().queue(bans -> {
            if (bans.isEmpty()) { event.getHook().editOriginalEmbeds(EmbedUtil.info("No Bans", "No banned members found.")).queue(); return; }
            int count = bans.size();
            for (Guild.Ban ban : bans) {
                event.getGuild().unban(ban.getUser()).queue(null, e -> {});
            }
            event.getHook().editOriginalEmbeds(EmbedUtil.success("Unbanned All", "Unbanning **" + count + "** members. This may take a moment.")).queue();
            logMod(event, "All Members Unbanned", null, count + " members");
        });
    }

    // ==================== KICK ====================
    private void handleKick(SlashCommandInteractionEvent event) {
        Member target = event.getOption("member", OptionMapping::getAsMember);
        String reason = event.getOption("reason", "No reason provided", OptionMapping::getAsString);
        if (target == null) { event.replyEmbeds(EmbedUtil.error("Error", "Member not found.")).setEphemeral(true).queue(); return; }
        if (!event.getGuild().getSelfMember().canInteract(target)) { event.replyEmbeds(EmbedUtil.error("Error", "Cannot kick this member.")).setEphemeral(true).queue(); return; }
        target.getUser().openPrivateChannel().queue(dm -> dm.sendMessageEmbeds(EmbedUtil.warning("Kicked",
                "You have been kicked from **" + event.getGuild().getName() + "**\nReason: " + reason)).queue(null, e -> {}), e -> {});
        event.getGuild().kick(target).reason(reason).queue(
                v -> event.replyEmbeds(EmbedUtil.success("Kicked", "**" + target.getUser().getName() + "** has been kicked.\nReason: " + reason)).queue(),
                e -> event.replyEmbeds(EmbedUtil.error("Error", e.getMessage())).setEphemeral(true).queue());
        logMod(event, "Member Kicked", target.getUser(), reason);
    }

    private void handleVKick(SlashCommandInteractionEvent event) {
        Member target = event.getOption("member", OptionMapping::getAsMember);
        if (target == null) { event.replyEmbeds(EmbedUtil.error("Error", "Member not found.")).setEphemeral(true).queue(); return; }
        if (target.getVoiceState() == null || target.getVoiceState().getChannel() == null) {
            event.replyEmbeds(EmbedUtil.error("Error", "Member is not in a voice channel.")).setEphemeral(true).queue(); return;
        }
        event.getGuild().moveVoiceMember(target, null).queue(
                v -> event.replyEmbeds(EmbedUtil.success("Voice Kicked", "**" + target.getUser().getName() + "** disconnected from voice.")).setEphemeral(true).queue(),
                e -> event.replyEmbeds(EmbedUtil.error("Error", e.getMessage())).setEphemeral(true).queue());
        logMod(event, "Voice Kicked", target.getUser(), "Disconnected from voice");
    }

    // ==================== MUTE ====================
    private void handleMuteText(SlashCommandInteractionEvent event) {
        Member target = event.getOption("member", OptionMapping::getAsMember);
        String reason = event.getOption("reason", "No reason provided", OptionMapping::getAsString);
        if (target == null) { event.replyEmbeds(EmbedUtil.error("Error", "Member not found.")).setEphemeral(true).queue(); return; }
        Role muteRole = getOrCreateMuteRole(event.getGuild());
        if (muteRole == null) { event.replyEmbeds(EmbedUtil.error("Error", "Could not create/find mute role.")).setEphemeral(true).queue(); return; }
        event.getGuild().addRoleToMember(target, muteRole).queue(
                v -> event.replyEmbeds(EmbedUtil.success("Muted", "**" + target.getUser().getName() + "** has been text muted.\nReason: " + reason)).queue(),
                e -> event.replyEmbeds(EmbedUtil.error("Error", e.getMessage())).setEphemeral(true).queue());
        logMod(event, "Text Muted", target.getUser(), reason);
    }

    private void handleUnmuteText(SlashCommandInteractionEvent event) {
        Member target = event.getOption("member", OptionMapping::getAsMember);
        if (target == null) { event.replyEmbeds(EmbedUtil.error("Error", "Member not found.")).setEphemeral(true).queue(); return; }
        Role muteRole = getOrCreateMuteRole(event.getGuild());
        if (muteRole == null) { event.replyEmbeds(EmbedUtil.error("Error", "Mute role not found.")).setEphemeral(true).queue(); return; }
        event.getGuild().removeRoleFromMember(target, muteRole).queue(
                v -> event.replyEmbeds(EmbedUtil.success("Unmuted", "**" + target.getUser().getName() + "** has been unmuted.")).queue(),
                e -> event.replyEmbeds(EmbedUtil.error("Error", e.getMessage())).setEphemeral(true).queue());
        logMod(event, "Text Unmuted", target.getUser(), null);
    }

    private void handleMuteCheck(SlashCommandInteractionEvent event) {
        Member target = event.getOption("member", OptionMapping::getAsMember);
        if (target == null) { event.replyEmbeds(EmbedUtil.error("Error", "Member not found.")).setEphemeral(true).queue(); return; }
        Role muteRole = getOrCreateMuteRole(event.getGuild());
        boolean textMuted = muteRole != null && target.getRoles().contains(muteRole);
        boolean voiceMuted = target.getVoiceState() != null && target.getVoiceState().isGuildMuted();
        boolean timedOut = target.isTimedOut();
        event.replyEmbeds(EmbedUtil.base().setColor(EmbedUtil.INFO)
                .setDescription("### \uD83D\uDD0D Mute Status: " + target.getUser().getName())
                .addField("\uD83D\uDCAC Text Muted", textMuted ? "\u274C Yes" : "\u2705 No", true)
                .addField("\uD83D\uDD0A Voice Muted", voiceMuted ? "\u274C Yes" : "\u2705 No", true)
                .addField("\u23F0 Timed Out", timedOut ? "\u274C Yes" : "\u2705 No", true).build()).setEphemeral(true).queue();
    }

    private void handleMuteVoice(SlashCommandInteractionEvent event) {
        Member target = event.getOption("member", OptionMapping::getAsMember);
        String reason = event.getOption("reason", "No reason provided", OptionMapping::getAsString);
        if (target == null) { event.replyEmbeds(EmbedUtil.error("Error", "Member not found.")).setEphemeral(true).queue(); return; }
        target.mute(true).queue(
                v -> event.replyEmbeds(EmbedUtil.success("Voice Muted", "**" + target.getUser().getName() + "** has been voice muted.\nReason: " + reason)).queue(),
                e -> event.replyEmbeds(EmbedUtil.error("Error", e.getMessage())).setEphemeral(true).queue());
        logMod(event, "Voice Muted", target.getUser(), reason);
    }

    private void handleUnmuteVoice(SlashCommandInteractionEvent event) {
        Member target = event.getOption("member", OptionMapping::getAsMember);
        if (target == null) { event.replyEmbeds(EmbedUtil.error("Error", "Member not found.")).setEphemeral(true).queue(); return; }
        target.mute(false).queue(
                v -> event.replyEmbeds(EmbedUtil.success("Voice Unmuted", "**" + target.getUser().getName() + "** has been voice unmuted.")).queue(),
                e -> event.replyEmbeds(EmbedUtil.error("Error", e.getMessage())).setEphemeral(true).queue());
        logMod(event, "Voice Unmuted", target.getUser(), null);
    }

    // ==================== TIMEOUT ====================
    private void handleTimeout(SlashCommandInteractionEvent event) {
        Member target = event.getOption("member", OptionMapping::getAsMember);
        int minutes = event.getOption("duration", 5, OptionMapping::getAsInt);
        String reason = event.getOption("reason", "No reason provided", OptionMapping::getAsString);
        if (target == null) { event.replyEmbeds(EmbedUtil.error("Error", "Member not found.")).setEphemeral(true).queue(); return; }
        target.timeoutFor(Duration.ofMinutes(minutes)).reason(reason).queue(
                v -> event.replyEmbeds(EmbedUtil.success("Timed Out", "**" + target.getUser().getName() + "** timed out for **" + minutes + " minutes**.\nReason: " + reason)).queue(),
                e -> event.replyEmbeds(EmbedUtil.error("Error", e.getMessage())).setEphemeral(true).queue());
        logMod(event, "Timed Out", target.getUser(), minutes + "min — " + reason);
    }

    private void handleUntimeout(SlashCommandInteractionEvent event) {
        Member target = event.getOption("member", OptionMapping::getAsMember);
        if (target == null) { event.replyEmbeds(EmbedUtil.error("Error", "Member not found.")).setEphemeral(true).queue(); return; }
        target.removeTimeout().queue(
                v -> event.replyEmbeds(EmbedUtil.success("Timeout Removed", "**" + target.getUser().getName() + "** timeout removed.")).queue(),
                e -> event.replyEmbeds(EmbedUtil.error("Error", e.getMessage())).setEphemeral(true).queue());
        logMod(event, "Timeout Removed", target.getUser(), null);
    }

    // ==================== CLEAR ====================
    private void handleClear(SlashCommandInteractionEvent event) {
        int amount = event.getOption("amount", 10, OptionMapping::getAsInt);
        if (amount < 1 || amount > 100) { event.replyEmbeds(EmbedUtil.error("Error", "Amount must be 1-100.")).setEphemeral(true).queue(); return; }
        event.deferReply(true).queue();
        event.getChannel().asTextChannel().getHistory().retrievePast(amount).queue(messages -> {
            if (messages.isEmpty()) { event.getHook().editOriginalEmbeds(EmbedUtil.info("Clear", "No messages to delete.")).queue(); return; }
            event.getChannel().asTextChannel().purgeMessages(messages);
            event.getHook().editOriginalEmbeds(EmbedUtil.success("Cleared", "\uD83D\uDDD1\uFE0F Deleted **" + messages.size() + "** messages.")).queue();
            logMod(event, "Messages Cleared", null, amount + " messages in " + event.getChannel().getAsMention());
        });
    }

    // ==================== MOVE ====================
    private void handleMove(SlashCommandInteractionEvent event) {
        Member target = event.getOption("member", OptionMapping::getAsMember);
        GuildChannel channel = event.getOption("channel", OptionMapping::getAsChannel);
        if (target == null || channel == null) { event.replyEmbeds(EmbedUtil.error("Error", "Specify member and voice channel.")).setEphemeral(true).queue(); return; }
        if (!(channel instanceof AudioChannel vc)) { event.replyEmbeds(EmbedUtil.error("Error", "Target must be a voice channel.")).setEphemeral(true).queue(); return; }
        event.getGuild().moveVoiceMember(target, vc).queue(
                v -> event.replyEmbeds(EmbedUtil.success("Moved", "**" + target.getUser().getName() + "** moved to " + channel.getAsMention())).setEphemeral(true).queue(),
                e -> event.replyEmbeds(EmbedUtil.error("Error", e.getMessage())).setEphemeral(true).queue());
    }

    // ==================== ROLE ====================
    private void handleRole(SlashCommandInteractionEvent event) {
        String sub = event.getSubcommandName();
        if (sub == null) return;
        Member target = event.getOption("member", OptionMapping::getAsMember);
        Role role = event.getOption("role", OptionMapping::getAsRole);
        if (target == null || role == null) { event.replyEmbeds(EmbedUtil.error("Error", "Specify member and role.")).setEphemeral(true).queue(); return; }
        if (sub.equals("add")) {
            event.getGuild().addRoleToMember(target, role).queue(
                    v -> event.replyEmbeds(EmbedUtil.success("Role Added", role.getAsMention() + " → " + target.getAsMention())).setEphemeral(true).queue(),
                    e -> event.replyEmbeds(EmbedUtil.error("Error", e.getMessage())).setEphemeral(true).queue());
        } else {
            event.getGuild().removeRoleFromMember(target, role).queue(
                    v -> event.replyEmbeds(EmbedUtil.success("Role Removed", role.getAsMention() + " ✕ " + target.getAsMention())).setEphemeral(true).queue(),
                    e -> event.replyEmbeds(EmbedUtil.error("Error", e.getMessage())).setEphemeral(true).queue());
        }
        logMod(event, "Role " + (sub.equals("add") ? "Added" : "Removed"), target.getUser(), role.getName());
    }

    private void handleTempRole(SlashCommandInteractionEvent event) {
        Member target = event.getOption("member", OptionMapping::getAsMember);
        Role role = event.getOption("role", OptionMapping::getAsRole);
        int minutes = event.getOption("duration", 60, OptionMapping::getAsInt);
        if (target == null || role == null) { event.replyEmbeds(EmbedUtil.error("Error", "Specify member and role.")).setEphemeral(true).queue(); return; }
        event.getGuild().addRoleToMember(target, role).queue(v -> {
            // Save to DB
            SupabaseClient.saveTempRole(target.getId(), event.getGuild().getId(), role.getId(), Instant.now().plus(Duration.ofMinutes(minutes)).toString());
            // Schedule removal
            tempRoleScheduler.schedule(() -> {
                event.getGuild().removeRoleFromMember(UserSnowflake.fromId(target.getId()), role).queue(null, e -> {});
                SupabaseClient.deleteTempRole(target.getId(), role.getId());
            }, minutes, TimeUnit.MINUTES);
            event.replyEmbeds(EmbedUtil.success("Temp Role", role.getAsMention() + " → " + target.getAsMention() + " for **" + minutes + " min**")).setEphemeral(true).queue();
        }, e -> event.replyEmbeds(EmbedUtil.error("Error", e.getMessage())).setEphemeral(true).queue());
        logMod(event, "Temp Role Assigned", target.getUser(), role.getName() + " for " + minutes + "min");
    }

    private void handleRAR(SlashCommandInteractionEvent event) {
        Member target = event.getOption("member", OptionMapping::getAsMember);
        if (target == null) { event.replyEmbeds(EmbedUtil.error("Error", "Member not found.")).setEphemeral(true).queue(); return; }
        event.deferReply(true).queue();
        List<Role> roles = new ArrayList<>(target.getRoles());
        int count = roles.size();
        for (Role r : roles) {
            if (event.getGuild().getSelfMember().canInteract(r)) {
                event.getGuild().removeRoleFromMember(target, r).queue(null, e -> {});
            }
        }
        event.getHook().editOriginalEmbeds(EmbedUtil.success("Roles Removed", "Removed **" + count + "** roles from " + target.getAsMention())).queue();
        logMod(event, "All Roles Removed", target.getUser(), count + " roles");
    }

    private void handleInRole(SlashCommandInteractionEvent event) {
        Role role = event.getOption("role", OptionMapping::getAsRole);
        if (role == null) { event.replyEmbeds(EmbedUtil.error("Error", "Specify a role.")).setEphemeral(true).queue(); return; }
        List<Member> members = event.getGuild().getMembersWithRoles(role);
        if (members.isEmpty()) { event.replyEmbeds(EmbedUtil.info("In Role", "No members have " + role.getAsMention())).setEphemeral(true).queue(); return; }
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (Member m : members) {
            if (count >= 30) { sb.append("\n... and ").append(members.size() - 30).append(" more"); break; }
            sb.append(m.getAsMention()).append(" ");
            count++;
        }
        event.replyEmbeds(EmbedUtil.base().setColor(EmbedUtil.INFO)
                .setDescription("### \uD83C\uDFAD " + role.getName() + " (" + members.size() + " members)\n" + sb).build()).setEphemeral(true).queue();
    }

    // ==================== WARNINGS ====================
    private void handleWarnAdd(SlashCommandInteractionEvent event) {
        Member target = event.getOption("member", OptionMapping::getAsMember);
        String reason = event.getOption("reason", "No reason provided", OptionMapping::getAsString);
        if (target == null) { event.replyEmbeds(EmbedUtil.error("Error", "Member not found.")).setEphemeral(true).queue(); return; }
        SupabaseClient.addWarning(target.getId(), target.getUser().getName(), event.getUser().getId(), event.getUser().getName(), reason, event.getGuild().getId());
        int count = SupabaseClient.getWarningCount(target.getId(), event.getGuild().getId());
        target.getUser().openPrivateChannel().queue(dm -> dm.sendMessageEmbeds(EmbedUtil.warning("Warning",
                "You have been warned in **" + event.getGuild().getName() + "**\nReason: " + reason + "\nTotal warnings: **" + count + "**")).queue(null, e -> {}), e -> {});
        event.replyEmbeds(EmbedUtil.success("Warned", "**" + target.getUser().getName() + "** warned.\nReason: " + reason + "\nTotal: **" + count + "** warnings")).queue();
        logMod(event, "Warning Added", target.getUser(), reason + " (Total: " + count + ")");
    }

    private void handleWarnRemove(SlashCommandInteractionEvent event) {
        Member target = event.getOption("member", OptionMapping::getAsMember);
        if (target == null) {
            // No member specified — clear all warnings
            SupabaseClient.clearAllWarnings(event.getGuild().getId());
            event.replyEmbeds(EmbedUtil.success("Warnings Cleared", "All warnings for this server have been cleared.")).queue();
        } else {
            SupabaseClient.clearUserWarnings(target.getId(), event.getGuild().getId());
            event.replyEmbeds(EmbedUtil.success("Warnings Cleared", "Cleared all warnings for " + target.getAsMention())).queue();
        }
    }

    private void handleWarnings(SlashCommandInteractionEvent event) {
        Member target = event.getOption("member", OptionMapping::getAsMember);
        if (target == null) {
            // Show server warnings
            JsonArray warnings = SupabaseClient.getServerWarnings(event.getGuild().getId(), 20);
            if (warnings == null || warnings.size() == 0) { event.replyEmbeds(EmbedUtil.info("Warnings", "No warnings found.")).setEphemeral(true).queue(); return; }
            StringBuilder sb = new StringBuilder();
            for (var el : warnings) {
                JsonObject w = el.getAsJsonObject();
                sb.append("**").append(w.has("user_name") ? w.get("user_name").getAsString() : "?").append("** — ")
                        .append(w.has("reason") ? w.get("reason").getAsString() : "N/A").append("\n");
            }
            event.replyEmbeds(EmbedUtil.base().setColor(EmbedUtil.WARNING).setDescription("### \u26A0\uFE0F Server Warnings\n" + sb).build()).setEphemeral(true).queue();
        } else {
            JsonArray warnings = SupabaseClient.getUserWarnings(target.getId(), event.getGuild().getId());
            if (warnings == null || warnings.size() == 0) { event.replyEmbeds(EmbedUtil.info("Warnings", target.getUser().getName() + " has no warnings.")).setEphemeral(true).queue(); return; }
            StringBuilder sb = new StringBuilder();
            int i = 1;
            for (var el : warnings) {
                JsonObject w = el.getAsJsonObject();
                String reason = w.has("reason") ? w.get("reason").getAsString() : "N/A";
                String by = w.has("warned_by_name") ? w.get("warned_by_name").getAsString() : "?";
                sb.append("`").append(i++).append(".` ").append(reason).append(" — by **").append(by).append("**\n");
            }
            event.replyEmbeds(EmbedUtil.base().setColor(EmbedUtil.WARNING).setDescription("### \u26A0\uFE0F Warnings: " + target.getUser().getName() + "\n" + sb).build()).setEphemeral(true).queue();
        }
    }

    // ==================== VIOLATIONS ====================
    private void handleViolations(SlashCommandInteractionEvent event) {
        Member target = event.getOption("member", OptionMapping::getAsMember);
        if (target == null) { event.replyEmbeds(EmbedUtil.error("Error", "Specify a member.")).setEphemeral(true).queue(); return; }
        JsonArray violations = SupabaseClient.getUserViolations(target.getId(), event.getGuild().getId());
        if (violations == null || violations.size() == 0) { event.replyEmbeds(EmbedUtil.info("Violations", target.getUser().getName() + " has no violations.")).setEphemeral(true).queue(); return; }
        StringBuilder sb = new StringBuilder();
        int i = 1;
        for (var el : violations) {
            JsonObject v = el.getAsJsonObject();
            sb.append("`").append(i++).append(".` ").append(v.has("violation_type") ? v.get("violation_type").getAsString() : "?")
                    .append(" — ").append(v.has("details") ? v.get("details").getAsString() : "").append("\n");
        }
        event.replyEmbeds(EmbedUtil.base().setColor(EmbedUtil.DANGER).setDescription("### \uD83D\uDEA8 Violations: " + target.getUser().getName() + "\n" + sb).build()).setEphemeral(true).queue();
    }

    private void handleViolationsClear(SlashCommandInteractionEvent event) {
        Member target = event.getOption("member", OptionMapping::getAsMember);
        if (target == null) { event.replyEmbeds(EmbedUtil.error("Error", "Specify a member.")).setEphemeral(true).queue(); return; }
        SupabaseClient.clearUserViolations(target.getId(), event.getGuild().getId());
        event.replyEmbeds(EmbedUtil.success("Cleared", "Violations cleared for " + target.getAsMention())).queue();
    }

    // ==================== CHANNEL PERMS ====================
    private void handleLock(SlashCommandInteractionEvent event) {
        GuildChannel ch = event.getOption("channel") != null ? event.getOption("channel", OptionMapping::getAsChannel) : event.getGuildChannel();
        ch.getPermissionContainer().upsertPermissionOverride(event.getGuild().getPublicRole()).deny(Permission.MESSAGE_SEND).queue(
                v -> event.replyEmbeds(EmbedUtil.success("Locked", "\uD83D\uDD12 " + ch.getAsMention() + " is now locked.")).setEphemeral(true).queue(),
                e -> event.replyEmbeds(EmbedUtil.error("Error", e.getMessage())).setEphemeral(true).queue());
        logMod(event, "Channel Locked", null, ch.getName());
    }

    private void handleUnlock(SlashCommandInteractionEvent event) {
        GuildChannel ch = event.getOption("channel") != null ? event.getOption("channel", OptionMapping::getAsChannel) : event.getGuildChannel();
        ch.getPermissionContainer().upsertPermissionOverride(event.getGuild().getPublicRole()).clear(Permission.MESSAGE_SEND).queue(
                v -> event.replyEmbeds(EmbedUtil.success("Unlocked", "\uD83D\uDD13 " + ch.getAsMention() + " is now unlocked.")).setEphemeral(true).queue(),
                e -> event.replyEmbeds(EmbedUtil.error("Error", e.getMessage())).setEphemeral(true).queue());
        logMod(event, "Channel Unlocked", null, ch.getName());
    }

    private void handleHide(SlashCommandInteractionEvent event) {
        GuildChannel ch = event.getOption("channel") != null ? event.getOption("channel", OptionMapping::getAsChannel) : event.getGuildChannel();
        ch.getPermissionContainer().upsertPermissionOverride(event.getGuild().getPublicRole()).deny(Permission.VIEW_CHANNEL).queue(
                v -> event.replyEmbeds(EmbedUtil.success("Hidden", "\uD83D\uDEAB " + ch.getAsMention() + " is now hidden.")).setEphemeral(true).queue(),
                e -> event.replyEmbeds(EmbedUtil.error("Error", e.getMessage())).setEphemeral(true).queue());
        logMod(event, "Channel Hidden", null, ch.getName());
    }

    private void handleShow(SlashCommandInteractionEvent event) {
        GuildChannel ch = event.getOption("channel") != null ? event.getOption("channel", OptionMapping::getAsChannel) : event.getGuildChannel();
        ch.getPermissionContainer().upsertPermissionOverride(event.getGuild().getPublicRole()).clear(Permission.VIEW_CHANNEL).queue(
                v -> event.replyEmbeds(EmbedUtil.success("Shown", "\uD83D\uDC41\uFE0F " + ch.getAsMention() + " is now visible.")).setEphemeral(true).queue(),
                e -> event.replyEmbeds(EmbedUtil.error("Error", e.getMessage())).setEphemeral(true).queue());
        logMod(event, "Channel Shown", null, ch.getName());
    }

    // ==================== SLOW MODE ====================
    private void handleSlowmode(SlashCommandInteractionEvent event) {
        int seconds = event.getOption("seconds", 0, OptionMapping::getAsInt);
        TextChannel ch = event.getOption("channel") != null ? event.getOption("channel", OptionMapping::getAsChannel).asTextChannel() : event.getChannel().asTextChannel();
        ch.getManager().setSlowmode(seconds).queue(
                v -> event.replyEmbeds(EmbedUtil.success("Slowmode", seconds == 0 ? "Slowmode disabled in " + ch.getAsMention() : "Slowmode set to **" + seconds + "s** in " + ch.getAsMention())).setEphemeral(true).queue(),
                e -> event.replyEmbeds(EmbedUtil.error("Error", e.getMessage())).setEphemeral(true).queue());
    }

    // ==================== EMOJI ====================
    private void handleAddEmoji(SlashCommandInteractionEvent event) {
        String eName = event.getOption("emoji_name", OptionMapping::getAsString);
        String url = event.getOption("image_url", OptionMapping::getAsString);
        if (eName == null || url == null) { event.replyEmbeds(EmbedUtil.error("Error", "Provide name and image URL.")).setEphemeral(true).queue(); return; }
        event.deferReply(true).queue();
        try {
            event.getGuild().createEmoji(eName, Icon.from(new java.io.ByteArrayInputStream(downloadImage(url)), Icon.IconType.PNG)).queue(
                    emoji -> event.getHook().editOriginalEmbeds(EmbedUtil.success("Emoji Added", emoji.getAsMention() + " **:" + eName + ":**")).queue(),
                    e -> event.getHook().editOriginalEmbeds(EmbedUtil.error("Error", e.getMessage())).queue());
        } catch (java.io.IOException ex) {
            event.getHook().editOriginalEmbeds(EmbedUtil.error("Error", "Failed to process image: " + ex.getMessage())).queue();
        }
    }

    private byte[] downloadImage(String url) {
        try {
            okhttp3.OkHttpClient client = new okhttp3.OkHttpClient();
            okhttp3.Request req = new okhttp3.Request.Builder().url(url).build();
            try (okhttp3.Response res = client.newCall(req).execute()) {
                return res.body() != null ? res.body().bytes() : new byte[0];
            }
        } catch (Exception e) { return new byte[0]; }
    }

    // ==================== ROLE MULTIPLE ====================
    private void handleRoleMultiple(SlashCommandInteractionEvent event) {
        String sub = event.getSubcommandName();
        if (sub == null) return;
        Role role = event.getOption("role", OptionMapping::getAsRole);
        if (role == null) { event.replyEmbeds(EmbedUtil.error("Error", "Specify a role.")).setEphemeral(true).queue(); return; }
        event.deferReply().queue();
        List<Member> members = event.getGuild().getMembers();
        int count = 0;
        for (Member m : members) {
            if (m.getUser().isBot()) continue;
            if (sub.equals("add") && !m.getRoles().contains(role)) {
                event.getGuild().addRoleToMember(m, role).queue(null, e -> {});
                count++;
            } else if (sub.equals("remove") && m.getRoles().contains(role)) {
                event.getGuild().removeRoleFromMember(m, role).queue(null, e -> {});
                count++;
            }
        }
        event.getHook().editOriginalEmbeds(EmbedUtil.success("Role Multiple",
                (sub.equals("add") ? "Added" : "Removed") + " " + role.getAsMention() + " " + (sub.equals("add") ? "to" : "from") + " **" + count + "** members.")).queue();
        logMod(event, "Mass Role " + (sub.equals("add") ? "Add" : "Remove"), null, role.getName() + " → " + count + " members");
    }

    // ==================== HELPERS ====================
    private Role getOrCreateMuteRole(Guild guild) {
        Role existing = guild.getRoles().stream().filter(r -> r.getName().equalsIgnoreCase("Muted")).findFirst().orElse(null);
        if (existing != null) return existing;
        try {
            Role role = guild.createRole().setName("Muted").setColor(0x818386).complete();
            for (var ch : guild.getTextChannels()) {
                ch.upsertPermissionOverride(role).deny(Permission.MESSAGE_SEND, Permission.MESSAGE_ADD_REACTION).queue(null, e -> {});
            }
            return role;
        } catch (Exception e) { log.error("Failed to create mute role: {}", e.getMessage()); return null; }
    }

    private void logMod(SlashCommandInteractionEvent event, String action, User target, String details) {
        TextChannel logCh = LogManager.get(event.getGuild(), Config.LOG_MODS_CMD);
        if (logCh == null) return;
        var eb = EmbedUtil.base().setColor(EmbedUtil.DANGER)
                .setAuthor(event.getUser().getName(), null, event.getUser().getEffectiveAvatarUrl())
                .setDescription("### \uD83D\uDC6E " + action)
                .addField("Admin", event.getUser().getAsMention(), true);
        if (target != null) eb.addField("Target", target.getAsMention(), true);
        if (details != null) eb.addField("Details", details, false);
        eb.addField("\uD83D\uDD52 Time", DateTimeFormatter.ofPattern("EEE, MMM dd yyyy \u2022 hh:mm:ss a")
                .withZone(ZoneId.of("Asia/Riyadh")).format(Instant.now()), false);
        logCh.sendMessageEmbeds(eb.build()).queue();
    }

    private boolean isAdmin(Member m) { return m != null && m.getRoles().stream().anyMatch(r -> Config.getAdminRoles().contains(r.getId())); }
}
