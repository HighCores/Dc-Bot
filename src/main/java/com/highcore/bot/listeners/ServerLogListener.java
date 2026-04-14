package com.highcore.bot.listeners;

import com.highcore.bot.config.Config;
import com.highcore.bot.services.LogManager;
import com.highcore.bot.utils.EmbedUtil;
import net.dv8tion.jda.api.audit.ActionType;
import net.dv8tion.jda.api.audit.AuditLogEntry;
import net.dv8tion.jda.api.entities.*;
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
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

public class ServerLogListener extends ListenerAdapter {

    @Override
    public void onGuildMemberJoin(@NotNull GuildMemberJoinEvent event) {
        long age = (Instant.now().getEpochSecond() - event.getUser().getTimeCreated().toEpochSecond()) / 86400;
        String details = "### \uD83D\uDFE2 Access Granted: New Unit Arrival\n" +
                "\u25AB\uFE0F **Account Age:** `" + age + " Days`\n" +
                "\u25AB\uFE0F **Registry Date:** `" + DateTimeFormatter.ISO_INSTANT.format(event.getUser().getTimeCreated().toInstant()) + "`\n" +
                "\u25AB\uFE0F **Current Population:** `" + event.getGuild().getMemberCount() + "`";
        
        LogManager.logEmbed(event.getGuild(), Config.LOG_JOIN_LEFT, 
            EmbedUtil.createOldLogEmbed("member-join", details, null, event.getUser(), event.getMember(), EmbedUtil.SUCCESS));
    }

    @Override
    public void onGuildMemberRemove(@NotNull GuildMemberRemoveEvent event) {
        String roles = event.getMember() != null ? event.getMember().getRoles().stream().map(Role::getName).collect(Collectors.joining(", ")) : "Unknown";
        String details = "### \uD83D\uDD34 Access Revoked: Unit Departure\n" +
                "\u25AB\uFE0F **Last Roles:** `" + (roles.isEmpty() ? "None" : roles) + "`\n" +
                "\u25AB\uFE0F **Current Population:** `" + event.getGuild().getMemberCount() + "`";
        
        LogManager.logEmbed(event.getGuild(), Config.LOG_JOIN_LEFT, 
            EmbedUtil.createOldLogEmbed("member-leave", details, null, event.getUser(), null, EmbedUtil.DANGER));
    }

    @Override
    public void onGuildBan(@NotNull GuildBanEvent event) {
        String details = "### \uD83D\uDD28 Entity Blacklisted\n" +
                "\u25AB\uFE0F **Target:** " + event.getUser().getAsMention() + " (`" + event.getUser().getId() + "`)\n" +
                "\u25AB\uFE0F **Status:** `TERMINATED`";
        LogManager.logEmbed(event.getGuild(), Config.LOG_JOIN_LEFT, 
            EmbedUtil.createOldLogEmbed("guild-ban", details, null, event.getUser(), null, java.awt.Color.BLACK));
    }

    @Override
    public void onGuildUnban(@NotNull GuildUnbanEvent event) {
        String details = "### \u2705 Blacklist Revoked\n" +
                "\u25AB\uFE0F **Target:** " + event.getUser().getAsMention() + " (`" + event.getUser().getId() + "`)\n" +
                "\u25AB\uFE0F **Status:** `REINSTATED`";
        LogManager.logEmbed(event.getGuild(), Config.LOG_JOIN_LEFT, 
            EmbedUtil.createOldLogEmbed("guild-unban", details, null, event.getUser(), null, EmbedUtil.SUCCESS));
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (!event.isFromGuild() || event.getAuthor().isBot()) return;
        String content = event.getMessage().getContentRaw();
        if (content.isEmpty()) return;
        if (content.length() > 500) content = content.substring(0, 500) + "...";

        String details = "### \uD83D\uDCAC Transmission Intercepted\n" +
                "\u25AB\uFE0F **Channel:** " + event.getChannel().getAsMention() + "\n" +
                "\u25AB\uFE0F **Content:** ```" + content + "```";

        LogManager.logEmbed(event.getGuild(), Config.LOG_MESSAGE, 
            EmbedUtil.createOldLogEmbed("message-sent", details, event.getMember(), null, null, EmbedUtil.INFO));
    }

    @Override
    public void onMessageUpdate(@NotNull MessageUpdateEvent event) {
        if (!event.isFromGuild() || event.getAuthor().isBot()) return;
        String content = event.getMessage().getContentRaw();
        if (content.length() > 500) content = content.substring(0, 500) + "...";

        String details = "### \u270F\uFE0F Transmission Modified\n" +
                "\u25AB\uFE0F **Channel:** " + event.getChannel().getAsMention() + "\n" +
                "\u25AB\uFE0F **New Data:** ```" + content + "```";

        LogManager.logEmbed(event.getGuild(), Config.LOG_MESSAGE, 
            EmbedUtil.createOldLogEmbed("message-edit", details, event.getMember(), null, null, EmbedUtil.WARNING));
    }

    @Override
    public void onMessageDelete(@NotNull MessageDeleteEvent event) {
        String details = "### \uD83D\uDDD1\uFE0F Transmission Terminated\n" +
                "\u25AB\uFE0F **Channel:** " + event.getChannel().getAsMention() + "\n" +
                "\u25AB\uFE0F **Message ID:** `" + event.getMessageId() + "`";

        LogManager.logEmbed(event.getGuild(), Config.LOG_MESSAGE, 
            EmbedUtil.createOldLogEmbed("message-delete", details, null, null, null, EmbedUtil.DANGER));
    }

    @Override
    public void onGuildVoiceUpdate(@NotNull GuildVoiceUpdateEvent event) {
        AudioChannel joined = event.getChannelJoined();
        AudioChannel left = event.getChannelLeft();
        String details = "";
        net.dv8tion.jda.api.entities.Member m = event.getMember();

        if (left == null && joined != null) {
            details = "### \uD83D\uDD0A Voice Link Established\n" +
                    "\u25AB\uFE0F **Protocol:** `CONNECTION_JOIN`\n" +
                    "\u25AB\uFE0F **Target Frequency:** `" + joined.getName() + "`\n" +
                    "\u25AB\uFE0F **Frequency Occupancy:** `" + joined.getMembers().size() + "`";
            LogManager.logEmbed(event.getGuild(), Config.LOG_VOICE, EmbedUtil.createOldLogEmbed("voice-join", details, m, null, null, EmbedUtil.SUCCESS));
        } else if (left != null && joined == null) {
            details = "### \uD83D\uDD07 Voice Link Severed\n" +
                    "\u25AB\uFE0F **Protocol:** `CONNECTION_DISCONNECT`\n" +
                    "\u25AB\uFE0F **Last Frequency:** `" + left.getName() + "`";
            LogManager.logEmbed(event.getGuild(), Config.LOG_VOICE, EmbedUtil.createOldLogEmbed("voice-leave", details, m, null, null, EmbedUtil.DANGER));
        } else if (left != null && joined != null) {
            details = "### \uD83D\uDD00 Voice Link Rerouted\n" +
                    "\u25AB\uFE0F **Protocol:** `CONNECTION_SWITCH`\n" +
                    "\u25AB\uFE0F **Source:** `" + left.getName() + "`\n" +
                    "\u25AB\uFE0F **Destination:** `" + joined.getName() + "`";
            LogManager.logEmbed(event.getGuild(), Config.LOG_VOICE, EmbedUtil.createOldLogEmbed("voice-switch", details, m, null, null, EmbedUtil.WARNING));
        }
    }

    @Override
    public void onChannelCreate(@NotNull ChannelCreateEvent event) {
        String details = "### \u2795 Structural Node Created\n" +
                "\u25AB\uFE0F **Type:** `" + event.getChannel().getType().name() + "`\n" +
                "\u25AB\uFE0F **Identifier:** " + event.getChannel().getAsMention();

        LogManager.logEmbed(event.getGuild(), Config.LOG_CHANNELS, 
            EmbedUtil.createOldLogEmbed("channel-init", details, null, null, null, EmbedUtil.SUCCESS));
    }

    @Override
    public void onChannelDelete(@NotNull ChannelDeleteEvent event) {
        String details = "### \u2796 Structural Node Decommissioned\n" +
                "\u25AB\uFE0F **Type:** `" + event.getChannel().getType().name() + "`\n" +
                "\u25AB\uFE0F **Name:** `" + event.getChannel().getName() + "`";

        LogManager.logEmbed(event.getGuild(), Config.LOG_CHANNELS, 
            EmbedUtil.createOldLogEmbed("channel-purge", details, null, null, null, EmbedUtil.DANGER));
    }

    @Override
    public void onChannelUpdateName(@NotNull ChannelUpdateNameEvent event) {
        String details = "### \u270F\uFE0F Structural Node Renamed\n" +
                "\u25AB\uFE0F **Old Designation:** `" + event.getOldValue() + "`\n" +
                "\u25AB\uFE0F **New Designation:** `" + event.getNewValue() + "`";

        LogManager.logEmbed(event.getGuild(), Config.LOG_CHANNELS, 
            EmbedUtil.createOldLogEmbed("channel-update", details, null, null, null, EmbedUtil.WARNING));
    }

    @Override
    public void onGuildMemberRoleAdd(@NotNull GuildMemberRoleAddEvent event) {
        String roles = event.getRoles().stream().map(Role::getName).collect(Collectors.joining(", "));
        String details = "### \u2795 Clearance Level Granted\n" +
                "\u25AB\uFE0F **Authority:** `" + roles + "`";

        LogManager.logEmbed(event.getGuild(), Config.LOG_ROLES, 
            EmbedUtil.createOldLogEmbed("clearance-add", details, null, event.getUser(), event.getMember(), EmbedUtil.SUCCESS));
    }

    @Override
    public void onGuildMemberRoleRemove(@NotNull GuildMemberRoleRemoveEvent event) {
        String roles = event.getRoles().stream().map(Role::getName).collect(Collectors.joining(", "));
        String details = "### \u2796 Clearance Level Revoked\n" +
                "\u25AB\uFE0F **Authority:** `" + roles + "`";

        LogManager.logEmbed(event.getGuild(), Config.LOG_ROLES, 
            EmbedUtil.createOldLogEmbed("clearance-remove", details, null, event.getUser(), event.getMember(), EmbedUtil.DANGER));
    }

    @Override
    public void onRoleCreate(@NotNull RoleCreateEvent event) {
        String details = "### \u2795 New Administrative Role Issued\n" +
                "\u25AB\uFE0F **Role Name:** `" + event.getRole().getName() + "`\n" +
                "\u25AB\uFE0F **Identifier:** `" + event.getRole().getId() + "`";

        LogManager.logEmbed(event.getGuild(), Config.LOG_ROLES, 
            EmbedUtil.createOldLogEmbed("role-init", details, null, null, null, EmbedUtil.SUCCESS));
    }

    @Override
    public void onRoleDelete(@NotNull RoleDeleteEvent event) {
        String details = "### \u2796 Administrative Role Purged\n" +
                "\u25AB\uFE0F **Role Name:** `" + event.getRole().getName() + "`";

        LogManager.logEmbed(event.getGuild(), Config.LOG_ROLES, 
            EmbedUtil.createOldLogEmbed("role-purge", details, null, null, null, EmbedUtil.DANGER));
    }

    @Override
    public void onRoleUpdateName(@NotNull RoleUpdateNameEvent event) {
        String details = "### \u270F\uFE0F Administrative Role Modified\n" +
                "\u25AB\uFE0F **Old Identity:** `" + event.getOldName() + "`\n" +
                "\u25AB\uFE0F **New Identity:** `" + event.getNewName() + "`";

        LogManager.logEmbed(event.getGuild(), Config.LOG_ROLES, 
            EmbedUtil.createOldLogEmbed("role-update", details, null, null, null, EmbedUtil.WARNING));
    }

    @Override
    public void onRoleUpdatePermissions(@NotNull RoleUpdatePermissionsEvent event) {
        String details = "### \uD83D\uDD12 Authority Protocols Updated\n" +
                "\u25AB\uFE0F **Target Role:** `" + event.getRole().getName() + "`\n" +
                "\u25AB\uFE0F **Protocol Update:** Permissions have been synchronized.";

        LogManager.logEmbed(event.getGuild(), Config.LOG_ROLES, 
            EmbedUtil.createOldLogEmbed("role-perms", details, null, null, null, EmbedUtil.WARNING));
    }
}
