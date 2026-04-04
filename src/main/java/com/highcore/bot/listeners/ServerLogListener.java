package com.highcore.bot.listeners;

import com.highcore.bot.config.Config;
import com.highcore.bot.services.LogManager;
import com.highcore.bot.utils.EmbedUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.audit.ActionType;
import net.dv8tion.jda.api.audit.AuditLogEntry;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
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
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
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

    // ==================== JOIN / LEFT ====================
    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        TextChannel ch = LogManager.get(event.getGuild(), Config.LOG_JOIN_LEFT);
        if (ch == null) return;
        Member m = event.getMember();
        long age = (Instant.now().getEpochSecond() - m.getTimeCreated().toEpochSecond()) / 86400;
        String created = DateTimeFormatter.ofPattern("MMM dd, yyyy").withZone(ZoneId.of("UTC")).format(m.getTimeCreated().toInstant());

        // Try to get invite info
        EmbedBuilder eb = EmbedUtil.base().setColor(EmbedUtil.SUCCESS)
                .setAuthor("Member Joined", null, m.getEffectiveAvatarUrl())
                .setThumbnail(m.getEffectiveAvatarUrl())
                .setDescription("### \uD83D\uDFE2 " + m.getUser().getName() + " joined the server")
                .addField("\uD83D\uDC64 Member", m.getAsMention(), true)
                .addField("\uD83C\uDD94 ID", "`" + m.getId() + "`", true)
                .addField("\uD83D\uDCC5 Created", created + " (" + age + "d ago)", true)
                .addField("\uD83D\uDC65 Count", "**" + event.getGuild().getMemberCount() + "**", true)
                .addField("\uD83D\uDD52 Time", now(), false);
        if (age < 7) eb.addField("\u26A0\uFE0F", "New account (< 7 days)", false);
        ch.sendMessageEmbeds(eb.build()).queue();
    }

    @Override
    public void onGuildMemberRemove(GuildMemberRemoveEvent event) {
        TextChannel ch = LogManager.get(event.getGuild(), Config.LOG_JOIN_LEFT);
        if (ch == null) return;
        User u = event.getUser();
        Member m = event.getMember();
        EmbedBuilder eb = EmbedUtil.base().setColor(EmbedUtil.DANGER)
                .setAuthor("Member Left", null, u.getEffectiveAvatarUrl())
                .setThumbnail(u.getEffectiveAvatarUrl())
                .setDescription("### \uD83D\uDD34 " + u.getName() + " left the server")
                .addField("\uD83D\uDC64 Member", "`" + u.getName() + "`", true)
                .addField("\uD83C\uDD94 ID", "`" + u.getId() + "`", true)
                .addField("\uD83D\uDC65 Count", "**" + event.getGuild().getMemberCount() + "**", true);
        if (m != null && !m.getRoles().isEmpty())
            eb.addField("\uD83C\uDFAD Roles", m.getRoles().stream().map(Role::getAsMention).collect(Collectors.joining(", ")), false);
        eb.addField("\uD83D\uDD52 Time", now(), false);
        ch.sendMessageEmbeds(eb.build()).queue();
    }

    @Override
    public void onGuildBan(GuildBanEvent event) {
        TextChannel ch = LogManager.get(event.getGuild(), Config.LOG_JOIN_LEFT);
        if (ch == null) return;
        User u = event.getUser();
        EmbedBuilder eb = EmbedUtil.base().setColor(new java.awt.Color(0x000000))
                .setAuthor("Member Banned", null, u.getEffectiveAvatarUrl())
                .setThumbnail(u.getEffectiveAvatarUrl())
                .setDescription("### \uD83D\uDD28 " + u.getName() + " was banned")
                .addField("\uD83D\uDC64 Member", "`" + u.getName() + "`", true)
                .addField("\uD83C\uDD94 ID", "`" + u.getId() + "`", true);
        event.getGuild().retrieveAuditLogs().type(ActionType.BAN).limit(1).queue(entries -> {
            if (!entries.isEmpty()) { AuditLogEntry e = entries.get(0);
                if (e.getTargetId().equals(u.getId())) {
                    if (e.getUser() != null) eb.addField("\uD83D\uDC6E By", e.getUser().getAsMention(), true);
                    if (e.getReason() != null) eb.addField("\uD83D\uDCDD Reason", e.getReason(), false);
                }
            }
            eb.addField("\uD83D\uDD52 Time", now(), false);
            ch.sendMessageEmbeds(eb.build()).queue();
        }, err -> { eb.addField("\uD83D\uDD52 Time", now(), false); ch.sendMessageEmbeds(eb.build()).queue(); });
    }

    @Override
    public void onGuildUnban(GuildUnbanEvent event) {
        TextChannel ch = LogManager.get(event.getGuild(), Config.LOG_JOIN_LEFT);
        if (ch == null) return;
        User u = event.getUser();
        EmbedBuilder eb = EmbedUtil.base().setColor(EmbedUtil.SUCCESS)
                .setAuthor("Member Unbanned", null, u.getEffectiveAvatarUrl())
                .setDescription("### \u2705 " + u.getName() + " was unbanned")
                .addField("\uD83D\uDC64 Member", "`" + u.getName() + "`", true)
                .addField("\uD83C\uDD94 ID", "`" + u.getId() + "`", true)
                .addField("\uD83D\uDD52 Time", now(), false);
        ch.sendMessageEmbeds(eb.build()).queue();
    }

    // ==================== MESSAGE LOGS ====================
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;
        TextChannel ch = LogManager.get(event.getGuild(), Config.LOG_MESSAGE);
        if (ch == null) return;
        String content = event.getMessage().getContentRaw();
        if (content.isEmpty()) return;
        if (content.length() > 1000) content = content.substring(0, 1000) + "...";
        ch.sendMessageEmbeds(EmbedUtil.base().setColor(EmbedUtil.INFO)
                .setAuthor(event.getAuthor().getName(), null, event.getAuthor().getEffectiveAvatarUrl())
                .setDescription("### \uD83D\uDCAC Message Sent")
                .addField("\uD83D\uDC64 Author", event.getAuthor().getAsMention(), true)
                .addField("\uD83D\uDCCD Channel", event.getChannel().getAsMention(), true)
                .addField("\uD83C\uDD94 ID", "`" + event.getAuthor().getId() + "`", true)
                .addField("\uD83D\uDCDD Content", "```" + content + "```", false)
                .addField("\uD83D\uDD52 Time", now(), false).build()).queue();
    }

    @Override
    public void onMessageUpdate(MessageUpdateEvent event) {
        if (event.getAuthor().isBot()) return;
        TextChannel ch = LogManager.get(event.getGuild(), Config.LOG_MESSAGE);
        if (ch == null) return;
        String content = event.getMessage().getContentRaw();
        if (content.length() > 1000) content = content.substring(0, 1000) + "...";
        ch.sendMessageEmbeds(EmbedUtil.base().setColor(EmbedUtil.WARNING)
                .setAuthor(event.getAuthor().getName(), null, event.getAuthor().getEffectiveAvatarUrl())
                .setDescription("### \u270F\uFE0F Message Edited")
                .addField("\uD83D\uDC64 Author", event.getAuthor().getAsMention(), true)
                .addField("\uD83D\uDCCD Channel", event.getChannel().getAsMention(), true)
                .addField("\uD83D\uDCDD New Content", "```" + content + "```", false)
                .addField("\uD83D\uDD52 Time", now(), false).build()).queue();
    }

    @Override
    public void onMessageDelete(MessageDeleteEvent event) {
        TextChannel ch = LogManager.get(event.getGuild(), Config.LOG_MESSAGE);
        if (ch == null) return;
        ch.sendMessageEmbeds(EmbedUtil.base().setColor(EmbedUtil.DANGER)
                .setDescription("### \uD83D\uDDD1\uFE0F Message Deleted")
                .addField("\uD83D\uDCCD Channel", event.getChannel().getAsMention(), true)
                .addField("\uD83C\uDD94 Msg ID", "`" + event.getMessageId() + "`", true)
                .addField("\uD83D\uDD52 Time", now(), false).build()).queue();
    }

    // ==================== VOICE LOGS ====================
    @Override
    public void onGuildVoiceUpdate(GuildVoiceUpdateEvent event) {
        TextChannel ch = LogManager.get(event.getGuild(), Config.LOG_VOICE);
        if (ch == null) return;
        Member m = event.getMember();
        AudioChannel joined = event.getChannelJoined();
        AudioChannel left = event.getChannelLeft();

        EmbedBuilder eb = EmbedUtil.base()
                .setAuthor(m.getUser().getName(), null, m.getEffectiveAvatarUrl())
                .addField("\uD83D\uDC64 Member", m.getAsMention(), true)
                .addField("\uD83C\uDD94 ID", "`" + m.getId() + "`", true);

        if (left == null && joined != null) {
            eb.setColor(EmbedUtil.SUCCESS).setDescription("### \uD83D\uDD0A Joined Voice").addField("\uD83D\uDCCD Channel", joined.getAsMention(), true);
        } else if (left != null && joined == null) {
            eb.setColor(EmbedUtil.DANGER).setDescription("### \uD83D\uDD07 Left Voice").addField("\uD83D\uDCCD Channel", left.getAsMention(), true);
        } else if (left != null && joined != null) {
            eb.setColor(EmbedUtil.WARNING).setDescription("### \uD83D\uDD00 Switched Voice")
                    .addField("From", left.getAsMention(), true).addField("To", joined.getAsMention(), true);
        }
        eb.addField("\uD83D\uDD52 Time", now(), false);
        ch.sendMessageEmbeds(eb.build()).queue();
    }

    // ==================== CHANNEL LOGS ====================
    @Override
    public void onChannelCreate(ChannelCreateEvent event) {
        TextChannel ch = LogManager.get(event.getGuild(), Config.LOG_CHANNELS);
        if (ch == null) return;
        EmbedBuilder eb = EmbedUtil.base().setColor(EmbedUtil.SUCCESS)
                .setDescription("### \u2795 Channel Created")
                .addField("\uD83D\uDCCD Channel", event.getChannel().getAsMention(), true)
                .addField("Name", "`" + event.getChannel().getName() + "`", true)
                .addField("Type", event.getChannel().getType().name(), true);
        event.getGuild().retrieveAuditLogs().type(ActionType.CHANNEL_CREATE).limit(1).queue(entries -> {
            if (!entries.isEmpty() && entries.get(0).getUser() != null)
                eb.addField("\uD83D\uDC6E By", entries.get(0).getUser().getAsMention(), true);
            eb.addField("\uD83D\uDD52 Time", now(), false);
            ch.sendMessageEmbeds(eb.build()).queue();
        }, err -> { eb.addField("\uD83D\uDD52 Time", now(), false); ch.sendMessageEmbeds(eb.build()).queue(); });
    }

    @Override
    public void onChannelDelete(ChannelDeleteEvent event) {
        TextChannel ch = LogManager.get(event.getGuild(), Config.LOG_CHANNELS);
        if (ch == null) return;
        EmbedBuilder eb = EmbedUtil.base().setColor(EmbedUtil.DANGER)
                .setDescription("### \u2796 Channel Deleted")
                .addField("Name", "`" + event.getChannel().getName() + "`", true)
                .addField("Type", event.getChannel().getType().name(), true);
        event.getGuild().retrieveAuditLogs().type(ActionType.CHANNEL_DELETE).limit(1).queue(entries -> {
            if (!entries.isEmpty() && entries.get(0).getUser() != null)
                eb.addField("\uD83D\uDC6E By", entries.get(0).getUser().getAsMention(), true);
            eb.addField("\uD83D\uDD52 Time", now(), false);
            ch.sendMessageEmbeds(eb.build()).queue();
        }, err -> { eb.addField("\uD83D\uDD52 Time", now(), false); ch.sendMessageEmbeds(eb.build()).queue(); });
    }

    @Override
    public void onChannelUpdateName(ChannelUpdateNameEvent event) {
        TextChannel ch = LogManager.get(event.getGuild(), Config.LOG_CHANNELS);
        if (ch == null) return;
        EmbedBuilder eb = EmbedUtil.base().setColor(EmbedUtil.WARNING)
                .setDescription("### \u270F\uFE0F Channel Renamed")
                .addField("Before", "`" + event.getOldValue() + "`", true)
                .addField("After", "`" + event.getNewValue() + "`", true);
        event.getGuild().retrieveAuditLogs().type(ActionType.CHANNEL_UPDATE).limit(1).queue(entries -> {
            if (!entries.isEmpty() && entries.get(0).getUser() != null)
                eb.addField("\uD83D\uDC6E By", entries.get(0).getUser().getAsMention(), true);
            eb.addField("\uD83D\uDD52 Time", now(), false);
            ch.sendMessageEmbeds(eb.build()).queue();
        }, err -> { eb.addField("\uD83D\uDD52 Time", now(), false); ch.sendMessageEmbeds(eb.build()).queue(); });
    }

    // ==================== ROLE LOGS ====================
    @Override
    public void onGuildMemberRoleAdd(GuildMemberRoleAddEvent event) {
        TextChannel ch = LogManager.get(event.getGuild(), Config.LOG_ROLES);
        if (ch == null) return;
        String roles = event.getRoles().stream().map(Role::getAsMention).collect(Collectors.joining(", "));
        EmbedBuilder eb = EmbedUtil.base().setColor(EmbedUtil.INFO)
                .setAuthor(event.getMember().getUser().getName(), null, event.getMember().getEffectiveAvatarUrl())
                .setDescription("### \u2795 Role Added")
                .addField("\uD83D\uDC64 Member", event.getMember().getAsMention(), true)
                .addField("\uD83C\uDFAD Role", roles, true);
        event.getGuild().retrieveAuditLogs().type(ActionType.MEMBER_ROLE_UPDATE).limit(1).queue(entries -> {
            if (!entries.isEmpty() && entries.get(0).getTargetId().equals(event.getMember().getId()) && entries.get(0).getUser() != null)
                eb.addField("\uD83D\uDC6E By", entries.get(0).getUser().getAsMention(), true);
            eb.addField("\uD83D\uDD52 Time", now(), false);
            ch.sendMessageEmbeds(eb.build()).queue();
        }, err -> { eb.addField("\uD83D\uDD52 Time", now(), false); ch.sendMessageEmbeds(eb.build()).queue(); });
    }

    @Override
    public void onGuildMemberRoleRemove(GuildMemberRoleRemoveEvent event) {
        TextChannel ch = LogManager.get(event.getGuild(), Config.LOG_ROLES);
        if (ch == null) return;
        String roles = event.getRoles().stream().map(Role::getAsMention).collect(Collectors.joining(", "));
        EmbedBuilder eb = EmbedUtil.base().setColor(EmbedUtil.WARNING)
                .setAuthor(event.getMember().getUser().getName(), null, event.getMember().getEffectiveAvatarUrl())
                .setDescription("### \u2796 Role Removed")
                .addField("\uD83D\uDC64 Member", event.getMember().getAsMention(), true)
                .addField("\uD83C\uDFAD Role", roles, true);
        event.getGuild().retrieveAuditLogs().type(ActionType.MEMBER_ROLE_UPDATE).limit(1).queue(entries -> {
            if (!entries.isEmpty() && entries.get(0).getTargetId().equals(event.getMember().getId()) && entries.get(0).getUser() != null)
                eb.addField("\uD83D\uDC6E By", entries.get(0).getUser().getAsMention(), true);
            eb.addField("\uD83D\uDD52 Time", now(), false);
            ch.sendMessageEmbeds(eb.build()).queue();
        }, err -> { eb.addField("\uD83D\uDD52 Time", now(), false); ch.sendMessageEmbeds(eb.build()).queue(); });
    }

    @Override
    public void onRoleCreate(RoleCreateEvent event) {
        TextChannel ch = LogManager.get(event.getGuild(), Config.LOG_ROLES);
        if (ch == null) return;
        ch.sendMessageEmbeds(EmbedUtil.base().setColor(EmbedUtil.SUCCESS)
                .setDescription("### \u2795 Role Created")
                .addField("Role", event.getRole().getAsMention(), true)
                .addField("Name", "`" + event.getRole().getName() + "`", true)
                .addField("\uD83D\uDD52 Time", now(), false).build()).queue();
    }

    @Override
    public void onRoleDelete(RoleDeleteEvent event) {
        TextChannel ch = LogManager.get(event.getGuild(), Config.LOG_ROLES);
        if (ch == null) return;
        ch.sendMessageEmbeds(EmbedUtil.base().setColor(EmbedUtil.DANGER)
                .setDescription("### \u2796 Role Deleted")
                .addField("Name", "`" + event.getRole().getName() + "`", true)
                .addField("\uD83D\uDD52 Time", now(), false).build()).queue();
    }

    @Override
    public void onRoleUpdateName(RoleUpdateNameEvent event) {
        TextChannel ch = LogManager.get(event.getGuild(), Config.LOG_ROLES);
        if (ch == null) return;
        ch.sendMessageEmbeds(EmbedUtil.base().setColor(EmbedUtil.WARNING)
                .setDescription("### \u270F\uFE0F Role Renamed")
                .addField("Before", "`" + event.getOldName() + "`", true)
                .addField("After", "`" + event.getNewName() + "`", true)
                .addField("\uD83D\uDD52 Time", now(), false).build()).queue();
    }

    @Override
    public void onRoleUpdatePermissions(RoleUpdatePermissionsEvent event) {
        TextChannel ch = LogManager.get(event.getGuild(), Config.LOG_ROLES);
        if (ch == null) return;
        ch.sendMessageEmbeds(EmbedUtil.base().setColor(EmbedUtil.WARNING)
                .setDescription("### \uD83D\uDD12 Role Permissions Changed")
                .addField("Role", event.getRole().getAsMention(), true)
                .addField("\uD83D\uDD52 Time", now(), false).build()).queue();
    }
}
