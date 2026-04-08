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
        String body = "### \uD83D\uDCDD تحديث اسم الحساب\n" +
                "**العضو:** `" + event.getUser().getName() + "`\n" +
                "**المعرف:** `" + event.getUser().getId() + "`\n" +
                "**الاسم السابق:** `" + event.getOldName() + "`\n" +
                "**الاسم الجديد:** `" + event.getNewName() + "`\n" +
                "**الوقت:** " + now();
        ch.sendMessageComponents(EmbedUtil.activityLog("تغيير الهوية", body, EmbedUtil.GOLD)).queue();
    }

    @Override
    public void onUserUpdateAvatar(@NotNull UserUpdateAvatarEvent event) {
        TextChannel ch = event.getJDA().getTextChannelById(LOG_CHANNEL_ID);
        if (ch == null) return;
        String body = "### \uD83D\uDDBC تحديث الصورة الشخصية\n" +
                "**العضو:** `" + event.getUser().getName() + "`\n" +
                "**المعرف:** `" + event.getUser().getId() + "`\n" +
                "**الصورة الجديدة:** [اضغط هنا للعرض](" + event.getNewAvatarUrl() + ")\n" +
                "**الوقت:** " + now();
        ch.sendMessageComponents(EmbedUtil.activityLog("تحديث بصري", body, EmbedUtil.GOLD)).queue();
    }

    @Override
    public void onGuildMemberUpdateNickname(@NotNull GuildMemberUpdateNicknameEvent event) {
        TextChannel ch = event.getJDA().getTextChannelById(LOG_CHANNEL_ID);
        if (ch == null) return;
        String old = event.getOldNickname() != null ? event.getOldNickname() : "لا يوجد";
        String curr = event.getNewNickname() != null ? event.getNewNickname() : "إعادة تعيين";
        String body = "### \uD83C\uDFF7\uFE0F تحديث لقب العضو\n" +
                "**العضو:** `" + event.getUser().getName() + "`\n" +
                "**المعرف:** `" + event.getUser().getId() + "`\n" +
                "**اللقب السابق:** `" + old + "`\n" +
                "**اللقب الجديد:** `" + curr + "`\n" +
                "**الوقت:** " + now();
        ch.sendMessageComponents(EmbedUtil.activityLog("تغيير اللقب", body, EmbedUtil.GOLD)).queue();
    }
}
