package com.highcore.bot.listeners;

import com.highcore.bot.utils.EmbedUtil;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.user.update.UserUpdateAvatarEvent;
import net.dv8tion.jda.api.events.user.update.UserUpdateNameEvent;
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateNicknameEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class UserLogListener extends ListenerAdapter {
    private static final String LOG_CHANNEL_ID = "1491201543960137758";
    private static final DateTimeFormatter TF = DateTimeFormatter.ofPattern("EEE, MMM dd yyyy \u2022 hh:mm:ss a")
            .withZone(ZoneId.of("Asia/Riyadh"));

    private String now() { return TF.format(Instant.now()); }

    @Override
    public void onUserUpdateName(@NotNull UserUpdateNameEvent event) {
        TextChannel ch = event.getJDA().getTextChannelById(LOG_CHANNEL_ID);
        if (ch == null) return;
        String body = "### \uD83D\uDCDD Account Name Updated\n" +
                "**User:** `" + event.getUser().getName() + "`\n" +
                "**ID:** `" + event.getUser().getId() + "`\n" +
                "**Old Name:** `" + event.getOldName() + "`\n" +
                "**New Name:** `" + event.getNewName() + "`\n" +
                "**Time:** " + now();
        ch.sendMessageComponents(EmbedUtil.activityLog("Identity Update", body, EmbedUtil.GOLD)).queue();
    }

    @Override
    public void onUserUpdateAvatar(@NotNull UserUpdateAvatarEvent event) {
        TextChannel ch = event.getJDA().getTextChannelById(LOG_CHANNEL_ID);
        if (ch == null) return;
        String body = "### \uD83D\uDDBC Profile Picture Updated\n" +
                "**User:** `" + event.getUser().getName() + "`\n" +
                "**ID:** `" + event.getUser().getId() + "`\n" +
                "**New Avatar:** [Click to view](" + event.getNewAvatarUrl() + ")\n" +
                "**Time:** " + now();
        ch.sendMessageComponents(EmbedUtil.activityLog("Visual Update", body, EmbedUtil.GOLD)).queue();
    }

    @Override
    public void onGuildMemberUpdateNickname(@NotNull GuildMemberUpdateNicknameEvent event) {
        TextChannel ch = event.getJDA().getTextChannelById(LOG_CHANNEL_ID);
        if (ch == null) return;
        String old = event.getOldNickname() != null ? event.getOldNickname() : "None";
        String curr = event.getNewNickname() != null ? event.getNewNickname() : "Reset";
        String body = "### \uD83C\uDFF7\uFE0F Nickname Updated\n" +
                "**User:** `" + event.getUser().getName() + "`\n" +
                "**ID:** `" + event.getUser().getId() + "`\n" +
                "**Old Nickname:** `" + old + "`\n" +
                "**New Nickname:** `" + curr + "`\n" +
                "**Time:** " + now();
        ch.sendMessageComponents(EmbedUtil.activityLog("Nickname Update", body, EmbedUtil.GOLD)).queue();
    }
}
