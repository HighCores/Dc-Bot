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
        
        long created = m.getUser().getTimeCreated().toEpochSecond();
        long joined = m.getTimeJoined().toEpochSecond();
        
        String body = String.format("""
                ### 👤 بـيـانـات الـهـويـة الـمـسـجـلـة
                **الـإسـم الــكــامــل:** %s
                **الـمـعـرّف الـرقـمـي:** `%s`
                
                ▫️ **الـتـسـجـيـلات الـزمـنـيـة:**
                • **إنـشـاء الـحـسـاب:** <t:%d:D> (<t:%d:R>)
                • **الـإنـضـمـام لـلـوكـالـة:** <t:%d:D> (<t:%d:R>)
                
                ▫️ **الـتـصـنـيـف والـأذونـات:**
                • **الـرتـبـة الـعـلـيـا:** %s
                • **الـأذونـات الـأسـاسـيـة:** %s
                • **حـالـة الـإتـصـال:** `%s`
                """, 
                m.getEffectiveName(), 
                m.getId(), 
                created, created,
                joined, joined,
                m.getRoles().isEmpty() ? "None" : m.getRoles().get(0).getAsMention(),
                m.hasPermission(net.dv8tion.jda.api.Permission.ADMINISTRATOR) ? "إدارة عــلــيــا" : "عــضــو مــصــرح",
                m.getOnlineStatus().name().toLowerCase());
        
        PanelService.reply(event, EmbedUtil.containerBranded("IDENTITY", "Profile Data", body, m.getUser().getEffectiveAvatarUrl()));
    }

    private void handleAvatar(SlashCommandInteractionEvent event, boolean server) {
        String url = server ? event.getGuild().getIconUrl() : 
            (event.getOption("user") != null ? event.getOption("user").getAsUser().getEffectiveAvatarUrl() : event.getUser().getEffectiveAvatarUrl());
        PanelService.reply(event, EmbedUtil.containerBranded("VISUALS", "Visual Identity", "### 🖼️ Operational Visual Retrieval", url));
    }

    private void handleServer(SlashCommandInteractionEvent event) {
        var g = event.getGuild();
        
        long total = g.getMemberCount();
        long humans = g.getMembers().stream().filter(m -> !m.getUser().isBot()).count();
        long bots = total - humans;
        
        int tc = g.getTextChannels().size();
        int vc = g.getVoiceChannels().size();
        int cat = g.getCategories().size();
        int rc = g.getRoles().size();
        int ec = g.getEmojis().size();
        int sc = g.getStickers().size();
        int bc = g.getBoostCount();
        
        String body = String.format("""
                ### 🌐 تـقـريـر الـكـيـان الـرئـيـسـي
                **إسـم الـمـنـظـمـة:** **%s**
                **الـمـديـر الـمـسـؤول:** <@%s>
                
                ▫️ **الـقـوى الـبـشـريـة والـتـقـنـيـة:**
                • **إجـمـالـي الـأعـضـاء:** **%d** (بـشـر: %d | أنـظـمـة: %d)
                • **تـاريـخ الـتـأسـيـس:** <t:%d:D> (<t:%d:R>)
                
                ▫️ **الـبـنـيـة الـتـحـتـيـة:**
                • **الـقـنـوات:** %d كـتـابـيـة / %d صـوتـيـة / %d فـئـات
                • **الـرتـب:** %d رتـبـة مـفـعـلـة
                • **الـأصـول:** %d إيـمـوجـي / %d مـلـصـق
                
                ▫️ **مـسـتـوى الـتـمـيـز والـأمـان:**
                • **مـسـتـوى الـتـعـزيـز:** %s (%d بـوسـت)
                • **مـسـتـوى الـتـحـقـق:** `%s`
                """, 
                g.getName(), 
                g.getOwnerId(),
                total, humans, bots,
                g.getTimeCreated().toEpochSecond(), g.getTimeCreated().toEpochSecond(),
                tc, vc, cat, rc, ec, sc,
                g.getBoostTier().name(), bc,
                g.getVerificationLevel().name());
        
        PanelService.reply(event, EmbedUtil.containerBranded("SYSTEM", "Operational Stats", body, g.getIconUrl()));
    }

    private void handleRoles(SlashCommandInteractionEvent event) {
        java.util.List<net.dv8tion.jda.api.entities.Role> allRoles = event.getGuild().getRoles();
        StringBuilder sb = new StringBuilder();
        sb.append("### 🔑 دلـيـل الـرتـب الـتـشـغـيـلـيـة\n\n");
        
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
                sb.append("\n*... والـمـزيـد مـن الـرتب*");
                break;
            }
        }
        
        sb.append("\n\n*تـم اسـتـخـراج هـذه الـبـيـانـات مـن الـنـظـام الـأسـاسـي لـلـوكـالـة.*");
        PanelService.reply(event, EmbedUtil.containerBranded("HIERARCHY", "Role Directory", sb.toString(), EmbedUtil.BANNER_MAIN));
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
            String body = String.format("""
                    ### \u1312 سـجـل الـدعـوات الـتـسـويـقـيـة
                    **الـعـضـو:** %s
                    **إجـمـالـي الـدعـوات نـاجـحـة:** **%d** دعـوة
                    """, m.getUser().getName(), count);
            PanelService.reply(event, EmbedUtil.containerBranded("\u0627\u0644\u062A\u0633\u0648\u064A\u0642", "\u0625\u062D\u0635\u0627\u0624\u064A\u0627\u062A \u0627\u0644\u0646\u0645\u0648", body, EmbedUtil.BANNER_MAIN));
        });
    }
}
