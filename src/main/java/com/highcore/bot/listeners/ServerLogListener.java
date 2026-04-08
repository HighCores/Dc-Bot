package com.highcore.bot.listeners;

import com.highcore.bot.config.Config;
import com.highcore.bot.services.LogManager;
import com.highcore.bot.utils.EmbedUtil;
import net.dv8tion.jda.api.audit.ActionType;
import net.dv8tion.jda.api.audit.AuditLogEntry;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.events.channel.ChannelCreateEvent;
import net.dv8tion.jda.api.events.channel.ChannelDeleteEvent;
import net.dv8tion.jda.api.events.channel.update.ChannelUpdateNameEvent;
import net.dv8tion.jda.api.events.guild.GuildBanEvent;
import net.dv8tion.jda.api.events.guild.GuildUnbanEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleRemoveEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.events.role.RoleCreateEvent;
import net.dv8tion.jda.api.events.role.RoleDeleteEvent;
import net.dv8tion.jda.api.events.role.update.RoleUpdateNameEvent;
import net.dv8tion.jda.api.events.role.update.RoleUpdatePermissionsEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

public class ServerLogListener extends ListenerAdapter {
    private static final Logger log = LoggerFactory.getLogger(ServerLogListener.class);
    private static final DateTimeFormatter TF = DateTimeFormatter.ofPattern("EEE, MMM dd yyyy \u2022 hh:mm:ss a")
            .withZone(ZoneId.of("Asia/Riyadh"));
    
    private String now() { return TF.format(Instant.now()); }

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        TextChannel ch = LogManager.getDashboardLogChannel(event.getGuild(), Config.LOG_JOIN_LEFT);
        if (ch == null) return;
        Member m = event.getMember();
        if (m == null) return;
        long age = (Instant.now().getEpochSecond() - m.getTimeCreated().toEpochSecond()) / 86400;
        String created = DateTimeFormatter.ofPattern("MMM dd, yyyy").withZone(ZoneId.of("UTC")).format(m.getTimeCreated().toInstant());

        StringBuilder sb = new StringBuilder();
        sb.append("### \uD83D\uDFE2 ").append(m.getUser().getName()).append(" joined the server\n");
        sb.append("**User:** **").append(m.getUser().getName()).append("**\n");
        sb.append("**ID:** `").append(m.getId()).append("`\n");
        sb.append("**Created:** ").append(created).append(" (").append(age).append("d ago)\n");
        sb.append("**Count:** **").append(event.getGuild().getMemberCount()).append("**\n");
        if (age < 7) sb.append("\u26A0\uFE0F **New account (< 7 days)**\n");
        sb.append("**Time:** ").append(now());

        ch.sendMessageComponents(EmbedUtil.activityLog("Member Joined", sb.toString(), EmbedUtil.SUCCESS))
                .useComponentsV2(true).queue();
    }

    @Override
    public void onGuildMemberRemove(GuildMemberRemoveEvent event) {
        TextChannel ch = LogManager.getDashboardLogChannel(event.getGuild(), Config.LOG_JOIN_LEFT);
        if (ch == null) return;
        User u = event.getUser();
        Member m = event.getMember();
        StringBuilder sb = new StringBuilder();
        sb.append("### \uD83D\uDD34 ").append(u.getName()).append(" left the server\n");
        sb.append("**User:** **").append(u.getName()).append("**\n");
        sb.append("**ID:** `").append(u.getId()).append("`\n");
        sb.append("**Count:** **").append(event.getGuild().getMemberCount()).append("**\n");
        if (m != null && !m.getRoles().isEmpty())
            sb.append("**Roles:** ").append(m.getRoles().stream().map(Role::getName).collect(Collectors.joining(", "))).append("\n");
        sb.append("**Time:** ").append(now());

        ch.sendMessageComponents(EmbedUtil.activityLog("Member Left", sb.toString(), EmbedUtil.DANGER))
                .useComponentsV2(true).queue();
    }

    @Override
    public void onGuildBan(GuildBanEvent event) {
        TextChannel ch = LogManager.getDashboardLogChannel(event.getGuild(), Config.LOG_JOIN_LEFT);
        if (ch == null) return;
        User u = event.getUser();
        
        event.getGuild().retrieveAuditLogs().type(ActionType.BAN).limit(1).queue(entries -> {
            StringBuilder sb = new StringBuilder();
            sb.append("### \uD83D\uDD28 ").append(u.getName()).append(" was banned\n");
            sb.append("**User:** **").append(u.getName()).append("**\n");
            sb.append("**ID:** `").append(u.getId()).append("`\n");
            
            if (!entries.isEmpty() && entries.get(0).getTargetId().equals(u.getId())) {
                AuditLogEntry e = entries.get(0);
                if (e.getUser() != null) sb.append("**By:** **").append(e.getUser().getName()).append("**\n");
                if (e.getReason() != null) sb.append("**Reason:** ").append(e.getReason()).append("\n");
            }
            sb.append("**Time:** ").append(now());
            ch.sendMessageComponents(EmbedUtil.activityLog("Member Banned", sb.toString(), java.awt.Color.BLACK))
                    .useComponentsV2(true).queue();
        }, err -> { 
            String fallback = "### \uD83D\uDD28 " + u.getName() + " was banned\n" +
                    "**ID:** `" + u.getId() + "`\n" +
                    "**Time:** " + now();
            ch.sendMessageComponents(EmbedUtil.activityLog("Member Banned", fallback, java.awt.Color.BLACK))
                    .useComponentsV2(true).queue();
        });
    }

    @Override
    public void onGuildUnban(GuildUnbanEvent event) {
        TextChannel ch = LogManager.getDashboardLogChannel(event.getGuild(), Config.LOG_JOIN_LEFT);
        if (ch == null) return;
        User u = event.getUser();
        String body = "### \u2705 " + u.getName() + " was unbanned\n" +
                "**User:** **" + u.getName() + "**\n" +
                "**ID:** `" + u.getId() + "`\n" +
                "**Time:** " + now();
        ch.sendMessageComponents(EmbedUtil.activityLog("Member Unbanned", body, EmbedUtil.SUCCESS))
                .useComponentsV2(true).queue();
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (!event.isFromGuild() || event.getAuthor().isBot()) return;
        TextChannel ch = LogManager.getDashboardLogChannel(event.getGuild(), Config.LOG_MESSAGE);
        if (ch == null) return;
        String content = event.getMessage().getContentRaw();
        if (content.isEmpty()) return;
        if (content.length() > 800) content = content.substring(0, 800) + "...";
        
        String body = "### \uD83D\uDCAC Message Sent\n" +
                "**Author:** **" + event.getAuthor().getName() + "**\n" +
                "**Channel:** #" + event.getChannel().getName() + "\n" +
                "**ID:** `" + event.getAuthor().getId() + "`\n" +
                "**Content:** ```" + content + "```\n" +
                "**Time:** " + now();
        ch.sendMessageComponents(EmbedUtil.activityLog("Message Log", body, EmbedUtil.INFO))
                .useComponentsV2(true).queue();
    }

    @Override
    public void onMessageUpdate(MessageUpdateEvent event) {
        if (!event.isFromGuild() || event.getAuthor().isBot()) return;
        TextChannel ch = LogManager.getDashboardLogChannel(event.getGuild(), Config.LOG_MESSAGE);
        if (ch == null) return;
        String content = event.getMessage().getContentRaw();
        if (content.length() > 800) content = content.substring(0, 800) + "...";

        String body = "### \u270F\uFE0F Message Edited\n" +
                "**Author:** **" + event.getAuthor().getName() + "**\n" +
                "**Channel:** #" + event.getChannel().getName() + "\n" +
                "**New Content:** ```" + content + "```\n" +
                "**Time:** " + now();
        ch.sendMessageComponents(EmbedUtil.activityLog("Message Edit", body, EmbedUtil.WARNING))
                .useComponentsV2(true).queue();
    }

    @Override
    public void onMessageDelete(MessageDeleteEvent event) {
        if (!event.isFromGuild()) return;
        TextChannel ch = LogManager.getDashboardLogChannel(event.getGuild(), Config.LOG_MESSAGE);
        if (ch == null) return;
        String body = "### \uD83D\uDDD1\uFE0F Message Deleted\n" +
                "**Channel:** #" + event.getChannel().getName() + "\n" +
                "**Msg ID:** `" + event.getMessageId() + "`\n" +
                "**Time:** " + now();
        ch.sendMessageComponents(EmbedUtil.activityLog("Message Deletion", body, EmbedUtil.DANGER))
                .useComponentsV2(true).queue();
    }

    @Override
    public void onGuildVoiceUpdate(GuildVoiceUpdateEvent event) {
        TextChannel ch = LogManager.getDashboardLogChannel(event.getGuild(), Config.LOG_VOICE);
        if (ch == null) return;
        Member m = event.getMember();
        AudioChannel joined = event.getChannelJoined();
        AudioChannel left = event.getChannelLeft();
        StringBuilder sb = new StringBuilder();
        sb.append("**User:** **").append(m.getUser().getName()).append("**\n");
        sb.append("**ID:** `").append(m.getId()).append("`\n");

        java.awt.Color color = EmbedUtil.INFO;
        if (left == null && joined != null) {
            color = EmbedUtil.SUCCESS;
            sb.append("### \uD83D\uDD0A Joined Voice\n**Channel:** ").append(joined.getName()).append("\n");
        } else if (left != null && joined == null) {
            color = EmbedUtil.DANGER;
            sb.append("### \uD83D\uDD07 Left Voice\n**Channel:** ").append(left.getName()).append("\n");
        } else if (left != null && joined != null) {
            color = EmbedUtil.WARNING;
            sb.append("### \uD83D\uDD00 Switched Voice\n**From:** ").append(left.getName()).append("\n**To:** ").append(joined.getName()).append("\n");
        }
        sb.append("**Time:** ").append(now());
        ch.sendMessageComponents(EmbedUtil.activityLog("Voice Activity", sb.toString(), color))
                .useComponentsV2(true).queue();
    }

    @Override
    public void onChannelCreate(ChannelCreateEvent event) {
        TextChannel ch = LogManager.getDashboardLogChannel(event.getGuild(), Config.LOG_CHANNELS);
        if (ch == null) return;
        
        event.getGuild().retrieveAuditLogs().type(ActionType.CHANNEL_CREATE).limit(1).queue(entries -> {
            StringBuilder sb = new StringBuilder();
            sb.append("### \u2795 Channel Created\n");
            sb.append("**Name:** `").append(event.getChannel().getName()).append("`\n");
            sb.append("**Type:** ").append(event.getChannel().getType().name()).append("\n");
            if (!entries.isEmpty() && entries.get(0).getUser() != null)
                sb.append("**By:** **").append(entries.get(0).getUser().getName()).append("**\n");
            sb.append("**Time:** ").append(now());
            ch.sendMessageComponents(EmbedUtil.activityLog("Channel Activity", sb.toString(), EmbedUtil.SUCCESS))
                    .useComponentsV2(true).queue();
        });
    }

    @Override
    public void onChannelDelete(ChannelDeleteEvent event) {
        TextChannel ch = LogManager.getDashboardLogChannel(event.getGuild(), Config.LOG_CHANNELS);
        if (ch == null) return;
        
        event.getGuild().retrieveAuditLogs().type(ActionType.CHANNEL_DELETE).limit(1).queue(entries -> {
            StringBuilder sb = new StringBuilder();
            sb.append("### \u2796 Channel Deleted\n");
            sb.append("**Name:** `").append(event.getChannel().getName()).append("`\n");
            if (!entries.isEmpty() && entries.get(0).getUser() != null)
                sb.append("**By:** **").append(entries.get(0).getUser().getName()).append("**\n");
            sb.append("**Time:** ").append(now());
            ch.sendMessageComponents(EmbedUtil.activityLog("Channel Activity", sb.toString(), EmbedUtil.DANGER))
                    .useComponentsV2(true).queue();
        });
    }

    @Override
    public void onChannelUpdateName(ChannelUpdateNameEvent event) {
        TextChannel ch = LogManager.getDashboardLogChannel(event.getGuild(), Config.LOG_CHANNELS);
        if (ch == null) return;
        String body = "### \u270F\uFE0F Channel Renamed\n" +
                "**Before:** `" + event.getOldValue() + "`\n" +
                "**After:** `" + event.getNewValue() + "`\n" +
                "**Time:** " + now();
        ch.sendMessageComponents(EmbedUtil.activityLog("Channel Activity", body, EmbedUtil.WARNING))
                .useComponentsV2(true).queue();
    }

    @Override
    public void onGuildMemberRoleAdd(GuildMemberRoleAddEvent event) {
        TextChannel ch = LogManager.getDashboardLogChannel(event.getGuild(), Config.LOG_ROLES);
        if (ch == null) return;
        String roles = event.getRoles().stream().map(Role::getName).collect(Collectors.joining(", "));

        event.getGuild().retrieveAuditLogs().type(ActionType.MEMBER_ROLE_UPDATE).limit(1).queue(entries -> {
            StringBuilder sb = new StringBuilder();
            sb.append("### \u2795 Role Added\n");
            sb.append("**Member:** **").append(event.getMember().getUser().getName()).append("**\n");
            sb.append("**Role:** ").append(roles).append("\n");
            if (!entries.isEmpty() && entries.get(0).getTargetId().equals(event.getMember().getId()) && entries.get(0).getUser() != null)
                sb.append("**By:** **").append(entries.get(0).getUser().getName()).append("**\n");
            sb.append("**Time:** ").append(now());
            ch.sendMessageComponents(EmbedUtil.activityLog("Member Role Add", sb.toString(), EmbedUtil.SUCCESS))
                    .useComponentsV2(true).queue();
        }, err -> { 
            String fallback = "### \u2795 Role Added\n**Member:** **" + event.getMember().getUser().getName() + "**\n**Role:** " + roles + "\n**Time:** " + now();
            ch.sendMessageComponents(EmbedUtil.activityLog("Member Role Add", fallback, EmbedUtil.SUCCESS))
                    .useComponentsV2(true).queue();
        });
    }

    @Override
    public void onGuildMemberRoleRemove(GuildMemberRoleRemoveEvent event) {
        TextChannel ch = LogManager.getDashboardLogChannel(event.getGuild(), Config.LOG_ROLES);
        if (ch == null) return;
        String roles = event.getRoles().stream().map(Role::getName).collect(Collectors.joining(", "));

        event.getGuild().retrieveAuditLogs().type(ActionType.MEMBER_ROLE_UPDATE).limit(1).queue(entries -> {
            StringBuilder sb = new StringBuilder();
            sb.append("### \u2796 Role Removed\n");
            sb.append("**Member:** **").append(event.getMember().getUser().getName()).append("**\n");
            sb.append("**Role:** ").append(roles).append("\n");
            if (!entries.isEmpty() && entries.get(0).getTargetId().equals(event.getMember().getId()) && entries.get(0).getUser() != null)
                sb.append("**By:** **").append(entries.get(0).getUser().getName()).append("**\n");
            sb.append("**Time:** ").append(now());
            ch.sendMessageComponents(EmbedUtil.activityLog("Member Role Remove", sb.toString(), EmbedUtil.WARNING))
                    .useComponentsV2(true).queue();
        }, err -> { 
            String fallback = "### \u2796 Role Removed\n**Member:** **" + event.getMember().getUser().getName() + "**\n**Role:** " + roles + "\n**Time:** " + now();
            ch.sendMessageComponents(EmbedUtil.activityLog("Member Role Remove", fallback, EmbedUtil.WARNING))
                    .useComponentsV2(true).queue();
        });
    }

    @Override
    public void onRoleCreate(RoleCreateEvent event) {
        TextChannel ch = LogManager.getDashboardLogChannel(event.getGuild(), Config.LOG_ROLES);
        if (ch == null) return;
        String body = "### \u2795 Role Created\n" +
                "**Name:** `" + event.getRole().getName() + "`\n" +
                "**Time:** " + now();
        ch.sendMessageComponents(EmbedUtil.activityLog("Role Activity", body, EmbedUtil.SUCCESS))
                .useComponentsV2(true).queue();
    }

    @Override
    public void onRoleDelete(RoleDeleteEvent event) {
        TextChannel ch = LogManager.getDashboardLogChannel(event.getGuild(), Config.LOG_ROLES);
        if (ch == null) return;
        String body = "### \u2796 Role Deleted\n" +
                "**Name:** `" + event.getRole().getName() + "`\n" +
                "**Time:** " + now();
        ch.sendMessageComponents(EmbedUtil.activityLog("Role Activity", body, EmbedUtil.DANGER))
                .useComponentsV2(true).queue();
    }

    @Override
    public void onRoleUpdateName(RoleUpdateNameEvent event) {
        TextChannel ch = LogManager.getDashboardLogChannel(event.getGuild(), Config.LOG_ROLES);
        if (ch == null) return;
        String body = "### \u270F\uFE0F Role Renamed\n" +
                "**Before:** `" + event.getOldName() + "`\n" +
                "**After:** `" + event.getNewName() + "`\n" +
                "**Time:** " + now();
        ch.sendMessageComponents(EmbedUtil.activityLog("Role Activity", body, EmbedUtil.WARNING))
                .useComponentsV2(true).queue();
    }

    @Override
    public void onRoleUpdatePermissions(RoleUpdatePermissionsEvent event) {
        TextChannel ch = LogManager.getDashboardLogChannel(event.getGuild(), Config.LOG_ROLES);
        if (ch == null) return;
        String body = "### \uD83D\uDD12 Role Permissions Changed\n" +
                "**Role Name:** `" + event.getRole().getName() + "`\n" +
                "**Time:** " + now();
        ch.sendMessageComponents(EmbedUtil.activityLog("Role Activity", body, EmbedUtil.WARNING))
                .useComponentsV2(true).queue();
    }
}
