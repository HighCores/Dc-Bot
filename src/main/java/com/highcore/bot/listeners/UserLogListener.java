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
        String details = "### \uD83D\uDCDD Account Name Update\n" +
                "\u25AB\uFE0F **Old Name:** `" + event.getOldName() + "`\n" +
                "\u25AB\uFE0F **New Name:** `" + event.getNewName() + "`";
        
        event.getJDA().getGuilds().forEach(guild -> {
            if (guild.getMember(event.getUser()) != null) {
                LogManager.logEmbed(guild, Config.LOG_USERS, 
                    EmbedUtil.createOldLogEmbed("user-name-update", details, null, event.getUser(), null, EmbedUtil.GOLD));
            }
        });
    }

    @Override
    public void onUserUpdateGlobalName(@NotNull UserUpdateGlobalNameEvent event) {
        String old = event.getOldGlobalName() != null ? event.getOldGlobalName() : "None";
        String curr = event.getNewGlobalName() != null ? event.getNewGlobalName() : "None";
        String details = "### \uD83C\uDF10 Global Name Update\n" +
                "\u25AB\uFE0F **Old Name:** `" + old + "`\n" +
                "\u25AB\uFE0F **New Name:** `" + curr + "`";

        event.getJDA().getGuilds().forEach(guild -> {
            if (guild.getMember(event.getUser()) != null) {
                LogManager.logEmbed(guild, Config.LOG_USERS, 
                    EmbedUtil.createOldLogEmbed("user-globalname-update", details, null, event.getUser(), null, EmbedUtil.GOLD));
            }
        });
    }

    @Override
    public void onUserUpdateAvatar(@NotNull UserUpdateAvatarEvent event) {
        String details = "### \uD83D\uDDBC Profile Picture Update\n" +
                "The user has updated their global account avatar.\n\n" +
                "\u25AB\uFE0F **New Image:** [Access File](" + event.getNewAvatarUrl() + ")";

        event.getJDA().getGuilds().forEach(guild -> {
            if (guild.getMember(event.getUser()) != null) {
                LogManager.logEmbed(guild, Config.LOG_USERS, 
                    EmbedUtil.createOldLogEmbed("user-avatar-update", details, null, event.getUser(), null, EmbedUtil.GOLD));
            }
        });
    }

    @Override
    public void onUserUpdateFlags(@NotNull UserUpdateFlagsEvent event) {
        String old = event.getOldFlags().stream().map(Enum::name).collect(Collectors.joining(", "));
        String curr = event.getNewFlags().stream().map(Enum::name).collect(Collectors.joining(", "));
        String details = "### \uD83C\uDFC5 Account Badges Update\n" +
                "\u25AB\uFE0F **Old Flags:** `" + (old.isEmpty() ? "None" : old) + "`\n" +
                "\u25AB\uFE0F **New Flags:** `" + (curr.isEmpty() ? "None" : curr) + "`";

        event.getJDA().getGuilds().forEach(guild -> {
            if (guild.getMember(event.getUser()) != null) {
                LogManager.logEmbed(guild, Config.LOG_USERS, 
                    EmbedUtil.createOldLogEmbed("user-badges-update", details, null, event.getUser(), null, EmbedUtil.GOLD));
            }
        });
    }

    @Override
    public void onGuildMemberUpdateNickname(@NotNull GuildMemberUpdateNicknameEvent event) {
        String old = event.getOldNickname() != null ? event.getOldNickname() : "Default Identity";
        String curr = event.getNewNickname() != null ? event.getNewNickname() : "Identity Reset";
        String details = "### \uD83C\uDFF7\uFE0F Station Nickname Update\n" +
                "\u25AB\uFE0F **Old Nick:** `" + old + "`\n" +
                "\u25AB\uFE0F **New Nick:** `" + curr + "`";

        LogManager.logEmbed(event.getGuild(), Config.LOG_USERS, 
            EmbedUtil.createOldLogEmbed("user-nickname-update", details, null, event.getUser(), event.getMember(), EmbedUtil.GOLD));
    }

    @Override
    public void onGuildMemberUpdateAvatar(@NotNull GuildMemberUpdateAvatarEvent event) {
        String details = "### \uD83D\uDCDA Server Profile Avatar Update\n" +
                "The member has updated their station-specific avatar.\n\n" +
                "\u25AB\uFE0F **New Image:** [Access File](" + event.getNewAvatarUrl() + ")";

        LogManager.logEmbed(event.getGuild(), Config.LOG_USERS, 
            EmbedUtil.createOldLogEmbed("user-member-avatar-update", details, null, event.getUser(), event.getMember(), EmbedUtil.GOLD));
    }
}
