package com.highcore.bot.commands;

import com.highcore.bot.config.Config;
import com.highcore.bot.services.LogManager;
import com.highcore.bot.utils.EmbedUtil;
import com.highcore.bot.services.PanelService;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.jetbrains.annotations.NotNull;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.stream.Collectors;

public class ModerationCommands extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        String name = event.getName().toLowerCase();
        
        // OWNERSHIP-BASED DEFERRAL for Moderation: Only acknowledge if it's a mod tool
        java.util.List<String> modCmds = java.util.Arrays.asList(
            "setnick", "ban", "unban", "unban-all", "kick", "vkick", "mute-text", "unmute-text",
            "mute-check", "mute-voice", "unmute-voice", "timeout", "untimeout", "clear", "move",
            "role", "role-multiple", "temprole", "rar", "inrole", "warn-add", "warn-remove",
            "warnings", "violations", "violations-clear", "lock", "unlock", "hide", "show", "slowmode", "add-emoji"
        );
        
        if (!modCmds.contains(name)) return; // Pass to other listeners

        java.util.List<String> publicCmds = java.util.Arrays.asList("hide", "show", "lock", "unlock", "slowmode", "inrole", "add-emoji");
        boolean ephemeral = !publicCmds.contains(name);
        
        if (!event.isAcknowledged()) event.deferReply(ephemeral).queue();

        try {
            switch (name) {
                case "setnick" -> handleSetNick(event);
                case "ban" -> handleBan(event);
                case "unban" -> handleUnban(event);
                case "unban-all" -> handleUnbanAll(event);
                case "kick" -> handleKick(event);
                case "vkick" -> handleVoiceKick(event);
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
                case "role-multiple" -> handleRoleMultiple(event);
                case "temprole" -> handleTempRole(event);
                case "rar" -> handleRemoveAllRoles(event);
                case "inrole" -> handleInRole(event);
                case "warn-add" -> handleWarnAdd(event);
                case "warn-remove" -> handleWarnRemove(event);
                case "warnings" -> handleWarnings(event);
                case "violations" -> handleViolations(event);
                case "violations-clear" -> handleViolationsClear(event);
                case "lock" -> handleLock(event, false);
                case "unlock" -> handleLock(event, true);
                case "hide" -> handleVisibility(event, false);
                case "show" -> handleVisibility(event, true);
                case "slowmode" -> handleSlowmode(event);
                case "add-emoji" -> handleAddEmoji(event);
            }
        } catch (Exception e) {
            PanelService.replyEphemeral(event, EmbedUtil.error("TERMINAL ERROR", "Internal execution failure: " + e.getMessage()));
        }
    }

    private void handleSetNick(SlashCommandInteractionEvent event) {
        if (!hasPerm(event, Permission.NICKNAME_MANAGE)) return;
        Member m = event.getOption("user", OptionMapping::getAsMember);
        String nick = event.getOption("nick", OptionMapping::getAsString);
        if (!canManage(event, m)) return;
        m.modifyNickname(nick).queue(v -> {
            PanelService.reply(event, EmbedUtil.success("Update Nickname", "Nickname for " + m.getUser().getName() + " has been changed to: `" + (nick == null ? "Original" : nick) + "`"));
            LogManager.logEmbed(event.getGuild(), Config.LOG_MODS_CMD, EmbedUtil.createOldLogEmbed("setnick", "Update Summary: Nickname Synchronization\nOriginal Result: " + (nick == null ? "Reset" : nick) + "\nChannel: " + event.getChannel().getAsMention(), event.getMember(), m.getUser(), m, EmbedUtil.INFO));
        });
    }

    private void handleBan(SlashCommandInteractionEvent event) {
        if (!hasPerm(event, Permission.BAN_MEMBERS)) return;
        User u = event.getOption("user", OptionMapping::getAsUser);
        String reason = getReason(event);
        if (u == null) return;
        
        Member targetMember = event.getGuild().getMember(u);
        if (targetMember != null && !canManage(event, targetMember)) return;

        event.getGuild().ban(u, 7, TimeUnit.DAYS).reason(reason).queue(v -> {
            PanelService.reply(event, EmbedUtil.success("Ban Enforcement", u.getName() + " has been banned from the agency.\nReason: " + reason));
            LogManager.logEmbed(event.getGuild(), Config.LOG_MODS_CMD, EmbedUtil.createOldLogEmbed("ban", "Action: Global Blacklist\nReason: " + reason + "\nChannel: " + event.getChannel().getAsMention(), event.getMember(), u, targetMember, EmbedUtil.DANGER));
        });
    }

    private void handleUnban(SlashCommandInteractionEvent event) {
        if (!hasPerm(event, Permission.BAN_MEMBERS)) return;
        String userId = event.getOption("user_id", OptionMapping::getAsString);
        event.getGuild().unban(User.fromId(userId)).queue(v -> {
            PanelService.reply(event, EmbedUtil.success("Unban System", "Restrictions removed for user ID: `" + userId + "`"));
            LogManager.logEmbed(event.getGuild(), Config.LOG_MODS_CMD, EmbedUtil.createOldLogEmbed("unban", "Action: Exile Revocation\nTarget ID: `" + userId + "`\nChannel: " + event.getChannel().getAsMention(), event.getMember(), User.fromId(userId), null, EmbedUtil.SUCCESS));
        });
    }

    private void handleUnbanAll(SlashCommandInteractionEvent event) {
        if (!hasPerm(event, Permission.ADMINISTRATOR)) return;
        event.getGuild().retrieveBanList().queue(bans -> {
            bans.forEach(ban -> event.getGuild().unban(ban.getUser()).queue());
            PanelService.reply(event, EmbedUtil.success("Clear Ban List", "**" + bans.size() + "** members have been unbanned successfully."));
            LogManager.logEmbed(event.getGuild(), Config.LOG_MODS_CMD, EmbedUtil.createOldLogEmbed("unban-all", "Operation: Database Wipe\nAffected: **" + bans.size() + "** subjects unbanned.\nChannel: " + event.getChannel().getAsMention(), event.getMember(), null, null, EmbedUtil.WARNING));
        });
    }

    private void handleKick(SlashCommandInteractionEvent event) {
        if (!hasPerm(event, Permission.KICK_MEMBERS)) return;
        Member m = event.getOption("user", OptionMapping::getAsMember);
        String reason = getReason(event);
        if (m == null) return;
        m.kick().reason(reason).queue(v -> {
            PanelService.reply(event, EmbedUtil.success("Kick System", m.getUser().getName() + " has been kicked from the server.\nReason: " + reason));
            LogManager.logEmbed(event.getGuild(), Config.LOG_MODS_CMD, EmbedUtil.createOldLogEmbed("kick", "Action: Manual Disconnect\nReason: " + reason + "\nChannel: " + event.getChannel().getAsMention(), event.getMember(), m.getUser(), m, EmbedUtil.WARNING));
        });
    }

    private void handleVoiceKick(SlashCommandInteractionEvent event) {
        if (!hasPerm(event, Permission.KICK_MEMBERS)) return;
        Member m = event.getOption("user", OptionMapping::getAsMember);
        if (m != null && m.getVoiceState().inAudioChannel()) {
            event.getGuild().kickVoiceMember(m).queue(v -> {
                PanelService.reply(event, EmbedUtil.success("Voice Kick", m.getUser().getName() + " has been disconnected from the voice channel."));
                LogManager.logEmbed(event.getGuild(), Config.LOG_MODS_CMD, EmbedUtil.createOldLogEmbed("vkick", "Action: Voice Channel Eviction\nChannel: " + event.getChannel().getAsMention(), event.getMember(), m.getUser(), m, EmbedUtil.WARNING));
            });
        }
    }

    private void handleMuteText(SlashCommandInteractionEvent event) {
        if (!hasPerm(event, Permission.MODERATE_MEMBERS)) return;
        Member m = event.getOption("user", OptionMapping::getAsMember);
        if (m == null) return;
        m.timeoutFor(24, TimeUnit.HOURS).reason("Text Mute").queue(v -> {
            PanelService.reply(event, EmbedUtil.success("Mute System", m.getUser().getName() + " has been muted for 24 hours."));
            LogManager.logEmbed(event.getGuild(), Config.LOG_MODS_CMD, EmbedUtil.createOldLogEmbed("mute-text", "Action: Text Isolation\nDuration: 24h\nChannel: " + event.getChannel().getAsMention(), event.getMember(), m.getUser(), m, EmbedUtil.WARNING));
        });
    }

    private void handleUnmuteText(SlashCommandInteractionEvent event) {
        if (!hasPerm(event, Permission.MODERATE_MEMBERS)) return;
        Member m = event.getOption("user", OptionMapping::getAsMember);
        if (m == null) return;
        m.removeTimeout().queue(v -> {
            PanelService.reply(event, EmbedUtil.success("Unmute System", "Writing permissions restored for " + m.getUser().getName()));
            LogManager.logEmbed(event.getGuild(), Config.LOG_MODS_CMD, EmbedUtil.createOldLogEmbed("unmute-text", "Action: Isolation Revocation\nChannel: " + event.getChannel().getAsMention(), event.getMember(), m.getUser(), m, EmbedUtil.SUCCESS));
        });
    }

    private void handleMuteCheck(SlashCommandInteractionEvent event) {
        Member m = event.getOption("user", OptionMapping::getAsMember);
        if (m == null) return;
        boolean muted = m.isTimedOut();
        PanelService.reply(event, EmbedUtil.containerBranded("Status", "Restrictions", "Member: " + m.getUser().getName() + "\nStatus: " + (muted ? "`Timed Out`" : "`Clear`"), EmbedUtil.BANNER_MAIN));
    }

    private void handleMuteVoice(SlashCommandInteractionEvent event) {
        if (!hasPerm(event, Permission.VOICE_MUTE_OTHERS)) return;
        Member m = event.getOption("user", OptionMapping::getAsMember);
        if (m == null) return;
        m.mute(true).queue(v -> {
            PanelService.reply(event, EmbedUtil.success("Voice Mute", "Microphone muted for " + m.getUser().getName()));
            LogManager.logEmbed(event.getGuild(), Config.LOG_MODS_CMD, EmbedUtil.createOldLogEmbed("mute-voice", "Action: Microphone Restriction\nChannel: " + event.getChannel().getAsMention(), event.getMember(), m.getUser(), m, EmbedUtil.WARNING));
        });
    }

    private void handleUnmuteVoice(SlashCommandInteractionEvent event) {
        if (!hasPerm(event, Permission.VOICE_MUTE_OTHERS)) return;
        Member m = event.getOption("user", OptionMapping::getAsMember);
        if (m == null) return;
        m.mute(false).queue(v -> {
            PanelService.reply(event, EmbedUtil.success("Voice Unmute", "Microphone enabled for " + m.getUser().getName()));
            LogManager.logEmbed(event.getGuild(), Config.LOG_MODS_CMD, EmbedUtil.createOldLogEmbed("unmute-voice", "Action: Voice Restoration\nChannel: " + event.getChannel().getAsMention(), event.getMember(), m.getUser(), m, EmbedUtil.SUCCESS));
        });
    }

    private void handleTimeout(SlashCommandInteractionEvent event) {
        if (!hasPerm(event, Permission.MODERATE_MEMBERS)) return;
        Member m = event.getOption("user", OptionMapping::getAsMember);
        int dur = event.getOption("duration", OptionMapping::getAsInt);
        if (m == null) return;
        m.timeoutFor(dur, TimeUnit.MINUTES).queue(v -> {
            PanelService.reply(event, EmbedUtil.success("Timeout System", m.getUser().getName() + " has been timed out for **" + dur + "** minutes."));
            LogManager.logEmbed(event.getGuild(), Config.LOG_MODS_CMD, EmbedUtil.createOldLogEmbed("timeout", "Action: Temporal Access Restriction\nDuration: " + dur + "m\nChannel: " + event.getChannel().getAsMention(), event.getMember(), m.getUser(), m, EmbedUtil.WARNING));
        });
    }

    private void handleUntimeout(SlashCommandInteractionEvent event) {
        handleUnmuteText(event);
    }

    private void handleClear(SlashCommandInteractionEvent event) {
        if (!hasPerm(event, Permission.MESSAGE_MANAGE)) return;
        int amt = event.getOption("amount", OptionMapping::getAsInt);
        event.getChannel().getIterableHistory().takeAsync(amt).thenAccept(msgs -> {
            event.getGuildChannel().deleteMessages(msgs).queue(v -> {
                PanelService.replyEphemeral(event, EmbedUtil.success("Channel Purge", "**" + msgs.size() + "** messages deleted."));
                LogManager.logEmbed(event.getGuild(), Config.LOG_MODS_CMD, EmbedUtil.createOldLogEmbed("clear", "Action: Intelligence Wipe\nAmount: " + msgs.size() + "\nChannel: " + event.getChannel().getAsMention(), event.getMember(), null, null, EmbedUtil.DANGER));
            });
        });
    }

    private void handleMove(SlashCommandInteractionEvent event) {
        if (!hasPerm(event, Permission.VOICE_MOVE_OTHERS)) return;
        Member m = event.getOption("user", OptionMapping::getAsMember);
        var chMapping = event.getOption("channel");
        if (m != null && chMapping != null) {
            net.dv8tion.jda.api.entities.channel.middleman.AudioChannel ch = chMapping.getAsChannel().asAudioChannel();
            event.getGuild().moveVoiceMember(m, ch).queue(v -> {
                PanelService.reply(event, EmbedUtil.success("Member Move", m.getUser().getName() + " moved to channel " + ch.getName()));
                LogManager.logEmbed(event.getGuild(), Config.LOG_MODS_CMD, EmbedUtil.createOldLogEmbed("move", "Action: Station Relocation\nTarget: " + ch.getAsMention() + "\nChannel: " + event.getChannel().getAsMention(), event.getMember(), m.getUser(), m, EmbedUtil.INFO));
            });
        }
    }

    private void handleRole(SlashCommandInteractionEvent event) {
        if (!hasPerm(event, Permission.MANAGE_ROLES)) return;
        Member m = event.getOption("user", OptionMapping::getAsMember);
        Role r = event.getOption("role", OptionMapping::getAsRole);
        if (m == null || r == null) return;
        if (m.getRoles().contains(r)) {
            event.getGuild().removeRoleFromMember(m, r).queue(v -> {
                PanelService.reply(event, EmbedUtil.success("Role System", "Role " + r.getName() + " removed from " + m.getUser().getName()));
                LogManager.logEmbed(event.getGuild(), Config.LOG_MODS_CMD, EmbedUtil.createOldLogEmbed("Role Removed", "Role: " + r.getAsMention(), event.getMember(), m.getUser(), m, EmbedUtil.INFO));
            });
        } else {
            event.getGuild().addRoleToMember(m, r).queue(v -> {
                PanelService.reply(event, EmbedUtil.success("Role System", "Role " + r.getName() + " added to " + m.getUser().getName()));
                LogManager.logEmbed(event.getGuild(), Config.LOG_MODS_CMD, EmbedUtil.createOldLogEmbed("role", "Update Summary: Role Allocation\nRole: " + r.getAsMention() + "\nChannel: " + event.getChannel().getAsMention(), event.getMember(), m.getUser(), m, EmbedUtil.SUCCESS));
            });
        }
    }

    private void handleTempRole(SlashCommandInteractionEvent event) {
        if (!hasPerm(event, Permission.MANAGE_ROLES)) return;
        Member m = event.getOption("user", OptionMapping::getAsMember);
        Role r = event.getOption("role", OptionMapping::getAsRole);
        int hours = event.getOption("duration", OptionMapping::getAsInt);
        if (m == null || r == null) return;
        
        String expiry = Instant.now().plus(hours, ChronoUnit.HOURS).toString();
        com.highcore.bot.database.SupabaseClient.saveTempRole(m.getId(), event.getGuild().getId(), r.getId(), expiry);
        event.getGuild().addRoleToMember(m, r).queue(v -> {
            PanelService.reply(event, EmbedUtil.success("Temporary Role", "Role " + r.getName() + " added to " + m.getUser().getName() + " for " + hours + " hours."));
            LogManager.logEmbed(event.getGuild(), Config.LOG_MODS_CMD, EmbedUtil.createOldLogEmbed("temprole", "Action: Temporal Role Assignment\nRole: " + r.getAsMention() + "\nDuration: " + hours + "h\nChannel: " + event.getChannel().getAsMention(), event.getMember(), m.getUser(), m, EmbedUtil.SUCCESS));
        });
    }

    private void handleInRole(SlashCommandInteractionEvent event) {
        Role r = event.getOption("role", OptionMapping::getAsRole);
        if (r == null) return;
        event.getGuild().loadMembers().onSuccess(members -> {
            java.util.List<Member> list = members.stream().filter(m -> m.getRoles().contains(r)).collect(java.util.stream.Collectors.toList());
            String names = list.stream().map(Member::getEffectiveName).limit(100).collect(java.util.stream.Collectors.joining(", "));
            if (list.size() > 100) names += "... and " + (list.size() - 100) + " more";
            
            String body = String.format("**Role:** %s\n**Members:** %s", r.getAsMention(), names.isEmpty() ? "None" : names);
            PanelService.reply(event, EmbedUtil.containerBranded("QUERY", "Role Members", body, EmbedUtil.BANNER_MAIN));
        });
    }

    private void handleWarnAdd(SlashCommandInteractionEvent event) {
        if (!hasPerm(event, Permission.MODERATE_MEMBERS)) return;
        User u = event.getOption("user", OptionMapping::getAsUser);
        String reason = getReason(event);
        if (u == null) return;
        com.highcore.bot.database.SupabaseClient.addWarning(u.getId(), u.getName(), event.getUser().getId(), event.getUser().getName(), reason, event.getGuild().getId());
        PanelService.reply(event, EmbedUtil.success("New Warning", "Warning recorded for " + u.getName() + ".\nReason: " + reason));
        LogManager.logEmbed(event.getGuild(), Config.LOG_MODS_CMD, EmbedUtil.createOldLogEmbed("warn-add", "Action: Official Warning Issuance\nReason: " + reason + "\nChannel: " + event.getChannel().getAsMention(), event.getMember(), u, event.getGuild().getMember(u), EmbedUtil.WARNING));
    }

    private void handleWarnRemove(SlashCommandInteractionEvent event) {
        if (!hasPerm(event, Permission.MODERATE_MEMBERS)) return;
        User u = event.getOption("user", OptionMapping::getAsUser);
        OptionMapping idOpt = event.getOption("id");
        
        if (u == null) return;
        
        event.deferReply(false).queue();
        
        if (idOpt != null) {
            int id = idOpt.getAsInt();
            com.highcore.bot.database.SupabaseClient.deleteWarningById(id);
            PanelService.reply(event, EmbedUtil.success("Infraction Removed", "Warning entry #`" + id + "` has been purged for " + u.getName()));
            LogManager.logEmbed(event.getGuild(), Config.LOG_MODS_CMD, EmbedUtil.createOldLogEmbed("warn-remove", "Action: Specific Record Purge\nID: #`" + id + "`\nChannel: " + event.getChannel().getAsMention(), event.getMember(), u, event.getGuild().getMember(u), EmbedUtil.SUCCESS));
        } else {
            com.highcore.bot.database.SupabaseClient.clearUserWarnings(u.getId(), event.getGuild().getId());
            PanelService.reply(event, EmbedUtil.success("Clear History", "All verified warnings removed for " + u.getName()));
            LogManager.logEmbed(event.getGuild(), Config.LOG_MODS_CMD, EmbedUtil.createOldLogEmbed("warn-remove", "Action: Full Disciplinary Purge\nChannel: " + event.getChannel().getAsMention(), event.getMember(), u, event.getGuild().getMember(u), EmbedUtil.SUCCESS));
        }
    }

    private void handleWarnings(SlashCommandInteractionEvent event) {
        User u = event.getOption("user", OptionMapping::getAsUser);
        if (u == null) return;
        
        event.deferReply().queue();
        
        // Minor delay to ensure database sync if command was just run
        try { Thread.sleep(600); } catch (InterruptedException ignored) {}

        com.google.gson.JsonArray warns = com.highcore.bot.database.SupabaseClient.getUserWarnings(u.getId(), event.getGuild().getId());
        int count = warns != null ? warns.size() : 0;
        
        StringBuilder sb = new StringBuilder();
        sb.append("### 👤 SUBJECT IDENTIFICATION\n");
        sb.append("**Name:** ").append(u.getAsMention()).append("\n");
        sb.append("**Registry ID:** `").append(u.getId()).append("`\n\n");
        
        sb.append("### 📋 WARNING REGISTRY LOGS\n");
        sb.append("**Total Infractions Detected:** `").append(count).append("`\n\n");
        
        if (count > 0) {
            sb.append("▫️ **Documented Violations:**\n");
            for (int i = 0; i < Math.min(warns.size(), 8); i++) {
                com.google.gson.JsonElement el = warns.get(i);
                if (el == null || !el.isJsonObject()) continue;
                com.google.gson.JsonObject w = el.getAsJsonObject();
                String reason = w.has("reason") ? w.get("reason").getAsString() : "Internal Error: Reason Missing";
                String date = w.has("created_at") ? w.get("created_at").getAsString().split("T")[0] : "Archive Date Missing";
                String mod = w.has("warned_by_name") ? w.get("warned_by_name").getAsString() : "System Automation";
                
                sb.append("`").append(date).append("` \u2014 **").append(reason).append("** (By: `").append(mod).append("`)\n");
            }
            if (count > 8) sb.append("\n*+ ").append(count - 8).append(" additional encrypted records in archive.*");
        } else {
            sb.append("*No documented infractions discovered within the Highcore Security Network.*");
        }
        
        PanelService.reply(event, EmbedUtil.containerBranded("HISTORY", "Infraction Database", sb.toString(), EmbedUtil.BANNER_MAIN));
    }

    private void handleViolations(SlashCommandInteractionEvent event) {
        User u = event.getOption("user", OptionMapping::getAsUser);
        if (u == null) return;
        com.google.gson.JsonArray vits = com.highcore.bot.database.SupabaseClient.getUserViolations(u.getId(), event.getGuild().getId());
        int count = vits != null ? vits.size() : 0;
        PanelService.reply(event, EmbedUtil.containerBranded("Safety", "Violations", "User: " + u.getName() + "\nFilter Violations: **" + count + "**", EmbedUtil.BANNER_MAIN));
    }

    private void handleViolationsClear(SlashCommandInteractionEvent event) {
        if (!hasPerm(event, Permission.ADMINISTRATOR)) return;
        User u = event.getOption("user", OptionMapping::getAsUser);
        if (u == null) return;
        com.highcore.bot.database.SupabaseClient.clearUserViolations(u.getId(), event.getGuild().getId());
        PanelService.reply(event, EmbedUtil.success("Clear Violations", "All filter violations removed for " + u.getName()));
        LogManager.logEmbed(event.getGuild(), Config.LOG_MODS_CMD, EmbedUtil.createOldLogEmbed("violations-clear", "Operation: Filter Interaction Wipe\nChannel: " + event.getChannel().getAsMention(), event.getMember(), u, event.getGuild().getMember(u), EmbedUtil.SUCCESS));
    }

    private void handleRoleMultiple(SlashCommandInteractionEvent event) {
        if (!hasPerm(event, Permission.ADMINISTRATOR)) return;
        Role r = event.getOption("role", OptionMapping::getAsRole);
        String action = event.getOption("action", OptionMapping::getAsString);
        if (r == null || action == null) return;
        
        event.getGuild().loadMembers().onSuccess(members -> {
            int count = 0;
            for (Member m : members) {
                if (action.equalsIgnoreCase("Add") && !m.getRoles().contains(r)) {
                    event.getGuild().addRoleToMember(m, r).queue();
                    count++;
                } else if (action.equalsIgnoreCase("Remove") && m.getRoles().contains(r)) {
                    event.getGuild().removeRoleFromMember(m, r).queue();
                    count++;
                }
            }
            PanelService.reply(event, EmbedUtil.success("Bulk Management", "Role: " + r.getName() + "\nAction: " + action + "\nAffected Members: **" + count + "**"));
            LogManager.logEmbed(event.getGuild(), Config.LOG_MODS_CMD, EmbedUtil.createOldLogEmbed("role-multiple", "Operation: Bulk Intelligence Modification\nAction: " + action + "\nRole: " + r.getAsMention() + "\nAffected: " + count + " Members\nChannel: " + event.getChannel().getAsMention(), event.getMember(), null, null, EmbedUtil.GOLD));
        });
    }

    private void handleRemoveAllRoles(SlashCommandInteractionEvent event) {
        if (!hasPerm(event, Permission.MANAGE_ROLES)) return;
        Member m = event.getOption("user", OptionMapping::getAsMember);
        if (m == null) return;
        m.getRoles().forEach(r -> {
            if (!r.isManaged()) event.getGuild().removeRoleFromMember(m, r).queue();
        });
        PanelService.reply(event, EmbedUtil.success("Strip Roles", "All removable roles stripped from " + m.getUser().getName()));
        LogManager.logEmbed(event.getGuild(), Config.LOG_MODS_CMD, EmbedUtil.createOldLogEmbed("Member Roles Stripped", "All removable roles removed.", event.getMember(), m.getUser(), m, EmbedUtil.DANGER));
    }

    private void handleLock(SlashCommandInteractionEvent event, boolean unlock) {
        if (!hasPerm(event, Permission.MANAGE_CHANNEL)) return;
        TextChannel tc = event.getChannel().asTextChannel();
        EnumSet<Permission> allow = unlock ? EnumSet.of(Permission.MESSAGE_SEND) : EnumSet.noneOf(Permission.class);
        EnumSet<Permission> deny = unlock ? EnumSet.noneOf(Permission.class) : EnumSet.of(Permission.MESSAGE_SEND);
        
        tc.upsertPermissionOverride(event.getGuild().getPublicRole()).setAllowed(allow).setDenied(deny).queue(
            v -> {
                PanelService.reply(event, EmbedUtil.success("Management", "Channel successfully " + (unlock ? "Unlocked" : "Locked") + "."));
                LogManager.logEmbed(event.getGuild(), Config.LOG_MODS_CMD, EmbedUtil.createOldLogEmbed("lock/unlock", "Action: Global Security Protocol Override\nStatus: " + (unlock ? "🔓 System Unlocked" : "🔒 System Locked") + "\nChannel: " + event.getChannel().getAsMention(), event.getMember(), null, null, unlock ? EmbedUtil.SUCCESS : EmbedUtil.DANGER));
            },
            e -> PanelService.replyEphemeral(event, EmbedUtil.error("Failed", "Permission update failed: " + e.getMessage()))
        );
    }

    private void handleVisibility(SlashCommandInteractionEvent event, boolean show) {
        if (!hasPerm(event, Permission.MANAGE_CHANNEL)) return;
        TextChannel tc = event.getChannel().asTextChannel();
        EnumSet<Permission> allow = show ? EnumSet.of(Permission.VIEW_CHANNEL) : EnumSet.noneOf(Permission.class);
        EnumSet<Permission> deny = show ? EnumSet.noneOf(Permission.class) : EnumSet.of(Permission.VIEW_CHANNEL);

        tc.upsertPermissionOverride(event.getGuild().getPublicRole()).setAllowed(allow).setDenied(deny).queue(
            v -> {
                PanelService.reply(event, EmbedUtil.success("Visibility", "Channel is now " + (show ? "Visible" : "Hidden") + " to everyone."));
                LogManager.logEmbed(event.getGuild(), Config.LOG_MODS_CMD, EmbedUtil.createOldLogEmbed("show/hide", "Action: Optical Intelligence Toggle\nStatus: " + (show ? "👁 Visible" : "🌫 Hidden") + "\nChannel: " + event.getChannel().getAsMention(), event.getMember(), null, null, show ? EmbedUtil.SUCCESS : EmbedUtil.WARNING));
            },
            e -> PanelService.replyEphemeral(event, EmbedUtil.error("Failed", "Visibility update failed: " + e.getMessage()))
        );
    }

    private void handleSlowmode(SlashCommandInteractionEvent event) {
        if (!hasPerm(event, Permission.MANAGE_CHANNEL)) return;
        int sec = event.getOption("seconds", OptionMapping::getAsInt);
        event.getChannel().asTextChannel().getManager().setSlowmode(sec).queue(
            v -> {
                PanelService.reply(event, EmbedUtil.success("Slowmode", "Slowmode delay set to `" + sec + " seconds`"));
                LogManager.logEmbed(event.getGuild(), Config.LOG_MODS_CMD, EmbedUtil.createOldLogEmbed("slowmode", "Action: Operational Pace Adjustment\nDelay: " + sec + "s\nChannel: " + event.getChannel().getAsMention(), event.getMember(), null, null, EmbedUtil.INFO));
            },
            e -> PanelService.replyEphemeral(event, EmbedUtil.error("Failed", "Slowmode update failed: " + e.getMessage()))
        );
    }

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ModerationCommands.class);

    private void handleAddEmoji(SlashCommandInteractionEvent event) {
        if (!hasPerm(event, net.dv8tion.jda.api.Permission.MANAGE_GUILD_EXPRESSIONS)) return;
        String name = event.getOption("name", OptionMapping::getAsString);
        OptionMapping imgMapping = event.getOption("image");
        if (imgMapping == null || name == null) return;

        // Sanitize emoji name: alphanumeric and underscores only, 2-32 chars
        String sanitized = name.replaceAll("[^a-zA-Z0-9_]", "");
        if (sanitized.length() < 2) sanitized = "emoji_" + sanitized;
        if (sanitized.length() > 32) sanitized = sanitized.substring(0, 32);
        final String finalName = sanitized;
        
        log.info("[MOD] Initiating emoji deployment: {}", finalName);
        event.getHook().editOriginal("📥 **Fetching asset data...**").queue();

        imgMapping.getAsAttachment().getProxy().download().thenAccept(stream -> {
            try (stream) {
                log.info("[MOD] Stream received, creating icon for {}", finalName);
                net.dv8tion.jda.api.entities.Icon icon = net.dv8tion.jda.api.entities.Icon.from(stream);
                event.getGuild().createEmoji(finalName, icon).queue(
                    v -> {
                        log.info("[MOD] Emoji {} deployed successfully.", finalName);
                        PanelService.reply(event, EmbedUtil.containerBranded("Emoji Protocol", "Emoji Deployed", "### ⚡ Data Asset Initialized\nThe emoji `" + finalName + "` has been successfully deployed.", EmbedUtil.BANNER_MAIN));
                        LogManager.logEmbed(event.getGuild(), Config.LOG_MODS_CMD, EmbedUtil.createOldLogEmbed("add-emoji", "Action: Creative Asset Deployment\nEmoji Name: `:" + finalName + ":`" + "\nChannel: " + event.getChannel().getAsMention(), event.getMember(), null, null, EmbedUtil.SUCCESS));
                    },
                    e -> {
                        log.error("[MOD] Emoji deployment failed: {}", e.getMessage());
                        PanelService.reply(event, EmbedUtil.error("Deployment Failure", "Registry rejected asset: " + e.getMessage()));
                    }
                );
            } catch (Exception e) { 
                log.error("[MOD] Stream processing error for emoji: {}", e.getMessage());
                PanelService.reply(event, EmbedUtil.error("Procedure Failed", "Stream processing error: " + e.getMessage())); 
            }
        }).exceptionally(ex -> {
            log.error("[MOD] Retrieval failure for emoji: {}", ex.getMessage());
            PanelService.reply(event, EmbedUtil.error("Procedure Failed", "Asset transmission failure: " + ex.getMessage()));
            return null;
        });
    }

    private boolean canManage(SlashCommandInteractionEvent event, Member target) {
        if (target == null) return false;
        if (!event.getGuild().getSelfMember().canInteract(target)) {
            PanelService.replyEphemeral(event, EmbedUtil.error("Hierarchy Error", "I cannot manage this member because their highest role is equal to or higher than mine."));
            return false;
        }
        return true;
    }

    private boolean hasPerm(SlashCommandInteractionEvent event, Permission perm) {
        if (!event.getMember().hasPermission(perm)) {
            PanelService.replyEphemeral(event, EmbedUtil.accessDenied());
            return false;
        }
        return true;
    }

    private String getReason(SlashCommandInteractionEvent e) {
        return e.getOption("reason") != null ? e.getOption("reason").getAsString() : "No reason specified";
    }
}
