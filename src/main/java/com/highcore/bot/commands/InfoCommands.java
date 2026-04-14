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
            case "profile" -> handleUser(event);
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
        
        long created = m.getUser().getTimeCreated().toEpochSecond();
        long joined = m.getTimeJoined().toEpochSecond();
        
        String body = String.format("""
                ### 👤 IDENTITY REGISTRY DATA
                **Full Name:** %s
                **Digital ID:** `%s`
                
                ▫️ **Chronological Logs:**
                • **Account Creation:** <t:%d:D> (<t:%d:R>)
                • **Agency Admission:** <t:%d:D> (<t:%d:R>)
                
                ▫️ **Classification & Permissions:**
                • **Primary Designation:** %s
                • **Security Clearance:** %s
                • **Connection State:** `%s`
                """, 
                m.getEffectiveName(), 
                m.getId(), 
                created, created,
                joined, joined,
                m.getRoles().isEmpty() ? "None" : m.getRoles().get(0).getAsMention(),
                m.hasPermission(net.dv8tion.jda.api.Permission.ADMINISTRATOR) ? "Executive Management" : "Authorized Personnel",
                m.getOnlineStatus().name().toLowerCase());
        
        PanelService.reply(event, EmbedUtil.containerBranded("IDENTITY", "Profile Data", body, m.getUser().getEffectiveAvatarUrl()));
    }

    private void handleAvatar(SlashCommandInteractionEvent event, boolean server) {
        String url = server ? event.getGuild().getIconUrl() : 
            (event.getOption("user") != null ? event.getOption("user").getAsUser().getEffectiveAvatarUrl() : event.getUser().getEffectiveAvatarUrl());
        PanelService.reply(event, EmbedUtil.containerBranded("VISUALS", "Visual Asset Retrieval", "### 🖼️ High-Resolution Profile Asset", url));
    }

    private void handleServer(SlashCommandInteractionEvent event) {
        var g = event.getGuild();
        if (g == null) return;
        
        long total = g.getMemberCount();
        
        int tc = g.getTextChannels().size();
        int vc = g.getVoiceChannels().size();
        int cat = g.getCategories().size();
        int rc = g.getRoles().size();
        int ec = g.getEmojis().size();
        int sc = g.getStickers().size();
        int bc = g.getBoostCount();
        
        String body = String.format("""
                ### 🌐 PRIMARY ENTITY REPORT
                **Organization Name:** **%s**
                **Chief Administrator:** <@%s>
                
                ▫️ **Personnel & Intelligence:**
                • **Total Assets:** **%d**
                • **Foundation Date:** <t:%d:D> (<t:%d:R>)
                
                ▫️ **Infrastructure Matrix:**
                • **Channels:** %d Text / %d Voice / %d Categories
                • **Designations:** %d Active Roles
                • **Assets:** %d Emojis / %d Stickers
                
                ▫️ **Performance & Verification:**
                • **Boost Tier:** %s (%d Boosts)
                • **Security Protocol:** `%s`
                """, 
                g.getName(), 
                g.getOwnerId(),
                total,
                g.getTimeCreated().toEpochSecond(), g.getTimeCreated().toEpochSecond(),
                tc, vc, cat, rc, ec, sc,
                g.getBoostTier().name(), bc,
                g.getVerificationLevel().name());
        
        PanelService.reply(event, EmbedUtil.containerBranded("SYSTEM", "Operational Stats", body, g.getIconUrl()));
    }

    private void handleRoles(SlashCommandInteractionEvent event) {
        java.util.List<net.dv8tion.jda.api.entities.Role> allRoles = event.getGuild().getRoles();
        StringBuilder sb = new StringBuilder();
        sb.append("### 🔑 OPERATIONAL ROLE DIRECTORY\n\n");
        
        int count = 0;
        for (net.dv8tion.jda.api.entities.Role role : allRoles) {
            if (role.isPublicRole() || role.isManaged()) continue;
            String name = role.getName();
            
            // Check for separators
            if (name.contains("---") || name.contains("===") || name.contains("━") || name.contains("─")) {
                sb.append("\n▫️ **").append(name.replaceAll("[-=━─]", "").trim()).append("**\n");
            } else {
                sb.append("`").append(name).append("` ");
                count++;
            }
            
            if (count > 80) {
                sb.append("\n*... and more roles*");
                break;
            }
        }
        
        sb.append("\n\n*Data synchronized directly from internal Highcore protocols.*");
        PanelService.reply(event, EmbedUtil.containerBranded("HIERARCHY", "Role Directory", sb.toString(), EmbedUtil.BANNER_MAIN));
    }

    private void handleBanner(SlashCommandInteractionEvent event, boolean server) {
        if (server) {
            String url = event.getGuild().getBannerUrl();
            if (url != null) url += "?size=2048";
            PanelService.reply(event, EmbedUtil.containerBranded("Visuals", "Server Banner", "### \uD83D\uDDBC High-Resolution Banner", url != null ? url : EmbedUtil.BANNER_MAIN));
        } else {
            User u = event.getOption("user") != null ? event.getOption("user").getAsUser() : event.getUser();
            u.retrieveProfile().queue(profile -> {
                String banner = profile.getBannerUrl();
                if (banner != null) {
                    banner += "?size=2048";
                    PanelService.reply(event, EmbedUtil.containerBranded("Visuals", "User Banner", "### \uD83D\uDDBC High-Resolution Banner", banner));
                } else {
                    PanelService.reply(event, EmbedUtil.containerBranded("Visuals", "User Banner", String.format("### \u26A0\uFE0F NO BANNER DETECTED\nUser **%s** has not configured a visual banner asset in their Discord profile settings.", u.getName()), u.getEffectiveAvatarUrl()));
                }
            }, err -> {
                PanelService.reply(event, EmbedUtil.containerBranded("Visuals", "User Banner", "### \u26A0\uFE0F RETRIEVAL ERROR\nFailed to sync with Discord Registry to fetch the user's profile banner.", u.getEffectiveAvatarUrl()));
            });
        }
    }

    private void handleInvites(SlashCommandInteractionEvent event) {
        Member m = event.getOption("user") != null ? event.getOption("user").getAsMember() : event.getMember();
        if (m == null) return;
        event.getGuild().retrieveInvites().queue(invs -> {
            long count = invs.stream().filter(i -> i.getInviter() != null && i.getInviter().getId().equals(m.getId())).mapToLong(net.dv8tion.jda.api.entities.Invite::getUses).sum();
            String body = String.format("""
                    ### 📈 MARKETING AFFILIATION LOG
                    **Personnel:** %s
                    **Total Verified Conversions:** **%d** successful invites
                    """, m.getUser().getName(), count);
            PanelService.reply(event, EmbedUtil.containerBranded("MARKETING", "Growth Statistics", body, EmbedUtil.BANNER_MAIN));
        });
    }
}
