package com.highcore.bot.listeners;

import com.highcore.bot.config.Config;
import com.highcore.bot.services.LogManager;
import com.highcore.bot.utils.EmbedUtil;
import net.dv8tion.jda.api.events.user.update.UserUpdateAvatarEvent;
import net.dv8tion.jda.api.events.user.update.UserUpdateFlagsEvent;
import net.dv8tion.jda.api.events.user.update.UserUpdateGlobalNameEvent;
import net.dv8tion.jda.api.events.user.update.UserUpdateNameEvent;
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateNicknameEvent;
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateAvatarEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.stream.Collectors;

public class UserLogListener extends ListenerAdapter {

    @Override
    public void onUserUpdateName(@NotNull UserUpdateNameEvent event) {
        String details = "### 🏷️ Account Name Migration\n" +
                "▫️ **Old Identity:** `" + event.getOldName() + "`\n" +
                "▫️ **New Identity:** `" + event.getNewName() + "`";
        
        broadcastLog("user-name-update", details, event.getUser(), null);
    }

    @Override
    public void onUserUpdateGlobalName(@NotNull UserUpdateGlobalNameEvent event) {
        String old = event.getOldGlobalName() != null ? event.getOldGlobalName() : "No Display Name";
        String curr = event.getNewGlobalName() != null ? event.getNewGlobalName() : "No Display Name";
        String details = "### 🌎 Global Identity Update\n" +
                "▫️ **Previous:** `" + old + "`\n" +
                "▫️ **Current:** `" + curr + "`";

        broadcastLog("user-globalname-update", details, event.getUser(), null);
    }

    @Override
    public void onUserUpdateAvatar(@NotNull UserUpdateAvatarEvent event) {
        String details = "### 🖼️ Profile Visual Update\n" +
                "Global profile picture has been modified.\n\n" +
                "▫️ **Image Data:** [View Source](" + event.getNewAvatarUrl() + ")";

        broadcastLog("user-avatar-update", details, event.getUser(), null);
    }

    @Override
    public void onUserUpdateFlags(@NotNull UserUpdateFlagsEvent event) {
        String old = event.getOldFlags().stream().map(Enum::name).collect(Collectors.joining(", "));
        String curr = event.getNewFlags().stream().map(Enum::name).collect(Collectors.joining(", "));
        String details = "### 🎖️ Badge Protocol Update\n" +
                "▫️ **Revoked:** `" + (old.isEmpty() ? "None" : old) + "`\n" +
                "▫️ **Granted:** `" + (curr.isEmpty() ? "None" : curr) + "`";

        broadcastLog("user-badges-update", details, event.getUser(), null);
    }

    @Override
    public void onGuildMemberUpdateNickname(@NotNull GuildMemberUpdateNicknameEvent event) {
        String old = event.getOldNickname() != null ? event.getOldNickname() : "Original Name";
        String curr = event.getNewNickname() != null ? event.getNewNickname() : "Reverted to Original";
        String details = "### 🏷️ Local Alias Update\n" +
                "▫️ **Old Alias:** `" + old + "`\n" +
                "▫️ **New Alias:** `" + curr + "`";

        LogManager.logEmbed(event.getGuild(), Config.LOG_USERS, 
            EmbedUtil.createOldLogEmbed("user-nickname-update", details, null, event.getUser(), event.getMember(), EmbedUtil.GOLD));
    }

    @Override
    public void onGuildMemberUpdateAvatar(@NotNull GuildMemberUpdateAvatarEvent event) {
        String details = "### 🎭 Server-Specific Avatar Update\n" +
                "The user has deployed a custom avatar for this station.\n\n" +
                "▫️ **Local File:** [View Image](" + event.getNewAvatarUrl() + ")";

        LogManager.logEmbed(event.getGuild(), Config.LOG_USERS, 
            EmbedUtil.createOldLogEmbed("user-member-avatar-update", details, null, event.getUser(), event.getMember(), EmbedUtil.GOLD));
    }

    private void broadcastLog(String type, String details, net.dv8tion.jda.api.entities.User user, net.dv8tion.jda.api.entities.Member member) {
        user.getJDA().getGuilds().forEach(guild -> {
            if (guild.getMember(user) != null) {
                LogManager.logEmbed(guild, Config.LOG_USERS, 
                    EmbedUtil.createOldLogEmbed(type, details, null, user, member, EmbedUtil.GOLD));
            }
        });
    }
}
