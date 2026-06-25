package com.highcore.bot.commands;

import com.highcore.bot.services.PanelService;
import com.highcore.bot.utils.EmbedUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.modals.Modal;
import org.jetbrains.annotations.NotNull;
import java.util.stream.Collectors;

public class InfoCommands extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        String name = event.getName();
        switch (name) {
            case "profile" -> handleUser(event);
            case "ping" -> event.reply("Pong! `" + event.getJDA().getGatewayPing() + "ms`").setEphemeral(true).queue();
            case "avatar" -> handleAvatar(event);
            case "server" -> handleServer(event);
            case "banner" -> handleBanner(event);
            case "invites" -> handleInvites(event);
            case "server-avatar" -> handleServerAvatar(event);
            case "server-banner" -> handleServerBanner(event);
            case "roles" -> handleRoles(event);
        }
    }

    private void handleRoles(SlashCommandInteractionEvent event) {
        if (!com.highcore.bot.config.Config.isAdmin(event.getMember())) {
            PanelService.replyEphemeral(event, EmbedUtil.accessDenied());
            return;
        }
        
        String roles = event.getGuild().getRoles().stream()
                .map(r -> r.getAsMention() + " (`" + r.getId() + "`) \u2014 " + event.getGuild().getMembersWithRoles(r).size() + " members")
                .collect(Collectors.joining("\n"));
        
        if (roles.length() > 4096) roles = roles.substring(0, 4090) + "...";
        
        PanelService.reply(event, EmbedUtil.containerBranded("SECURITY", "Clearance Registry", 
            "### 📑 Authority Roles\n" + roles, null));
    }

    private void handleAvatar(SlashCommandInteractionEvent event) {
        User u = event.getOption("user") != null ? event.getOption("user").getAsUser() : event.getUser();
        String url = u.getEffectiveAvatarUrl() + "?size=1024";
        PanelService.reply(event, EmbedUtil.containerBranded("IDENTITY", "Visual Data", 
            "### 🖼️ Profile Avatar\nUser: " + u.getAsMention(), url));
    }

    private void handleBanner(SlashCommandInteractionEvent event) {
        User u = event.getOption("user") != null ? event.getOption("user").getAsUser() : event.getUser();
        u.retrieveProfile().queue(profile -> {
            String url = profile.getBannerUrl();
            if (url == null) {
                PanelService.replyEphemeral(event, EmbedUtil.error("DATA ERROR", "This user has no banner deployed."));
                return;
            }
            PanelService.reply(event, EmbedUtil.containerBranded("IDENTITY", "Banner Data", 
                "### 🎨 Profile Banner\nUser: " + u.getAsMention(), url + "?size=1024"));
        });
    }

    private void handleServer(SlashCommandInteractionEvent event) {
        net.dv8tion.jda.api.entities.Guild g = event.getGuild();
        if (g == null) return;
        String desc = "### 🏛️ Station Information\n" +
                "▫️ **Identifier:** `" + g.getId() + "`\n" +
                "▫️ **Owner:** <@" + g.getOwnerId() + ">\n" +
                "▫️ **Established:** <t:" + g.getTimeCreated().toEpochSecond() + ":D>\n" +
                "▫️ **Personnel:** " + g.getMemberCount() + " total\n" +
                "▫️ **Boost Level:** " + g.getBoostTier().name() + " (" + g.getBoostCount() + " boosts)";
        PanelService.reply(event, EmbedUtil.containerBranded("REGISTRY", "Server Metrics", desc, g.getIconUrl()));
    }

    private void handleServerAvatar(SlashCommandInteractionEvent event) {
        net.dv8tion.jda.api.entities.Guild g = event.getGuild();
        if (g == null || g.getIconUrl() == null) {
            PanelService.replyEphemeral(event, EmbedUtil.error("DATA ERROR", "Server has no icon deployed."));
            return;
        }
        PanelService.reply(event, EmbedUtil.containerBranded("REGISTRY", "Icon Data", 
            "### 🏛️ Server Icon", g.getIconUrl() + "?size=1024"));
    }

    private void handleServerBanner(SlashCommandInteractionEvent event) {
        net.dv8tion.jda.api.entities.Guild g = event.getGuild();
        if (g == null || g.getBannerUrl() == null) {
            PanelService.replyEphemeral(event, EmbedUtil.error("DATA ERROR", "Server has no banner deployed."));
            return;
        }
        PanelService.reply(event, EmbedUtil.containerBranded("REGISTRY", "Banner Data", 
            "### 🏛️ Server Banner", g.getBannerUrl() + "?size=1024"));
    }

    private void handleInvites(SlashCommandInteractionEvent event) {
        User u = event.getOption("user") != null ? event.getOption("user").getAsUser() : event.getUser();
        event.getGuild().retrieveInvites().queue(invites -> {
            long count = invites.stream()
                    .filter(i -> i.getInviter() != null && i.getInviter().getId().equals(u.getId()))
                    .mapToLong(net.dv8tion.jda.api.entities.Invite::getUses)
                    .sum();
            PanelService.reply(event, EmbedUtil.containerBranded("NETWORK", "Recruitment Metrics", 
                "### 📈 Invitation Status\nUser: " + u.getAsMention() + "\n▫️ **Total Uses:** `" + count + "`", null));
        });
    }

    private void handleUser(SlashCommandInteractionEvent event) {
        Member m = event.getOption("user") != null ? event.getOption("user").getAsMember() : event.getMember();
        if (m == null) return;
        
        m.getUser().retrieveProfile().queue(profile -> {
            long created = m.getUser().getTimeCreated().toEpochSecond();
            long joined = m.getTimeJoined().toEpochSecond();

            String roles = m.getRoles().stream()
                    .map(net.dv8tion.jda.api.entities.Role::getAsMention)
                    .collect(Collectors.joining(" "));
            if (roles.isEmpty()) roles = "None";

            EmbedBuilder eb = new EmbedBuilder()
                    .setAuthor(m.getUser().getName(), null, m.getUser().getEffectiveAvatarUrl())
                    .setThumbnail(m.getUser().getEffectiveAvatarUrl())
                    .setTitle("Operative Profile")
                    .addField("Identity", m.getUser().getAsMention() + " (`" + m.getUser().getId() + "`)", true)
                    .addField("Status", m.getOnlineStatus().name().toLowerCase(), true)
                    .addField("Joined Agency", "<t:" + joined + ":R>", true)
                    .addField("Registration", "<t:" + created + ":R>", true)
                    .addField("Clearance Roles", roles, false)
                    .setColor(EmbedUtil.ACCENT)
                    .setFooter("UNIFIED TERMINAL ACCESS", null);

            PanelService.reply(event, eb.build());
        });
    }
}
