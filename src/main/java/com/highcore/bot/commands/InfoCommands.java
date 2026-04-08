package com.highcore.bot.commands;

import com.highcore.bot.utils.EmbedUtil;
import com.highcore.bot.services.PanelService;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.jetbrains.annotations.NotNull;
import java.util.stream.Collectors;

public class InfoCommands extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        String name = event.getName();
        switch (name) {
            case "profile", "user" -> handleUser(event);
            case "avatar" -> handleAvatar(event, false);
            case "server-avatar" -> handleAvatar(event, true);
            case "server" -> handleServer(event);
            case "roles" -> handleRoles(event);
            case "banner" -> handleBanner(event, false);
            case "server-banner" -> handleBanner(event, true);
            case "invites" -> handleInvites(event);
        }
    }

    private void handleUser(SlashCommandInteractionEvent event) {
        Member m = event.getOption("user") != null ? event.getOption("user").getAsMember() : event.getMember();
        if (m == null) return;
        String body = "### \uD83D\uDCC1 Member Profile\n" +
                "**Name:** " + m.getUser().getName() + "\n" +
                "**ID:** `" + m.getId() + "`\n" +
                "**Joined:** <t:" + m.getTimeJoined().toEpochSecond() + ":R>\n" +
                "**Status:** " + m.getOnlineStatus().name();
        PanelService.reply(event, EmbedUtil.containerBranded("Member", "Record Retrieval", body, m.getUser().getEffectiveAvatarUrl()));
    }

    private void handleAvatar(SlashCommandInteractionEvent event, boolean server) {
        String url = server ? event.getGuild().getIconUrl() : 
            (event.getOption("user") != null ? event.getOption("user").getAsUser().getEffectiveAvatarUrl() : event.getUser().getEffectiveAvatarUrl());
        PanelService.reply(event, EmbedUtil.containerBranded("Visuals", "Avatar Display", "### \uD83D\uDDBC High-Resolution Image", url));
    }

    private void handleServer(SlashCommandInteractionEvent event) {
        var g = event.getGuild();
        String body = "### \uD83C\uDF10 Server Information\n" +
                "**Name:** **" + g.getName() + "**\n" +
                "**Members:** **" + g.getMemberCount() + "** users\n" +
                "**Created:** <t:" + g.getTimeCreated().toEpochSecond() + ":R>";
        PanelService.reply(event, EmbedUtil.containerBranded("Server", "System Stats", body, g.getIconUrl()));
    }

    private void handleRoles(SlashCommandInteractionEvent event) {
        String roles = event.getGuild().getRoles().stream().limit(30).map(net.dv8tion.jda.api.entities.Role::getName).collect(Collectors.joining(", "));
        PanelService.reply(event, EmbedUtil.containerBranded("Hierarchy", "Role Directory", "### \uD83D\uDD11 Server Roles\n" + (roles.isEmpty() ? "None" : roles), EmbedUtil.BANNER_MAIN));
    }

    private void handleBanner(SlashCommandInteractionEvent event, boolean server) {
        if (server) {
            String url = event.getGuild().getBannerUrl();
            PanelService.reply(event, EmbedUtil.containerBranded("Visuals", "Server Banner", "### \uD83D\uDDBC High-Resolution Banner", url != null ? url : EmbedUtil.BANNER_MAIN));
        } else {
            User u = event.getOption("user") != null ? event.getOption("user").getAsUser() : event.getUser();
            u.retrieveProfile().queue(profile -> {
                String banner = profile.getBannerUrl();
                PanelService.reply(event, EmbedUtil.containerBranded("Visuals", "User Banner", "### \uD83D\uDDBC High-Resolution Banner", banner != null ? banner : u.getEffectiveAvatarUrl()));
            });
        }
    }

    private void handleInvites(SlashCommandInteractionEvent event) {
        Member m = event.getOption("user") != null ? event.getOption("user").getAsMember() : event.getMember();
        if (m == null) return;
        event.getGuild().retrieveInvites().queue(invs -> {
            long count = invs.stream().filter(i -> i.getInviter() != null && i.getInviter().getId().equals(m.getId())).mapToLong(net.dv8tion.jda.api.entities.Invite::getUses).sum();
            PanelService.reply(event, EmbedUtil.containerBranded("Marketing", "Invite Records", "### \uD83D\uDCE4 Personal Invitation Stats\nUser: " + m.getUser().getName() + "\nTotal Successful Invites: **" + count + "**", EmbedUtil.BANNER_MAIN));
        });
    }
}
