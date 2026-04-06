package com.highcore.bot.listeners;

import com.highcore.bot.config.Config;
import com.highcore.bot.utils.EmbedUtil;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.components.container.Container;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WelcomeListener extends ListenerAdapter {
    private static final Logger log = LoggerFactory.getLogger(WelcomeListener.class);

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        log.info("Member joined: {} in {}", event.getMember().getUser().getName(), event.getGuild().getName());
        sendWelcomeMessage(event.getMember(), event.getGuild());
        sendStartupDM(event.getMember());
    }

    private void sendWelcomeMessage(Member member, Guild guild) {
        TextChannel ch = guild.getTextChannelById(Config.WELCOME_CHANNEL_ID);
        if (ch == null) return;

        String body = String.format("""
                ## \uD83C\uDF89 Welcome to Highcore Agency!
                
                **Operative Identified:** %s
                **Designation:** %s
                
                > You have been successfully integrated into our neural network. 
                > Please initialize your workstation and proceed to the designated sector.
                """, member.getAsMention(), member.getEffectiveName());

        Container c = EmbedUtil.containerBranded("WELCOME PROTOCOL", "New Integration", body, EmbedUtil.BANNER_MAIN);
        c.withAccentColor(EmbedUtil.ACCENT_TEAL.getRGB() & 0xFFFFFF);

        ch.sendMessageComponents(c).useComponentsV2(true).queue();
    }

    private void sendStartupDM(Member member) {
        String dp = Config.CH_DEV_PRICES, dsp = Config.CH_DESIGN_PRICES, mp = Config.CH_MINECRAFT_PRICES;
        String o = Config.CH_ORDER, t = Config.CH_TICKET;

        String body = String.format("""
                ## \uD83D\uDCD6 Highcore Agency \u2014 Startup Guide
                
                Welcome to the agency. Here are your primary operational links:
                
                \uD83D\uDCD1 **Protocols & Prices:**
                - <#%s> | <#%s> | <#%s>
                
                \uD83D\uDED2 **Resource Procurement:**
                - <#%s>
                
                \u2709\uFE0F **Operational Support:**
                - <#%s>
                
                *Synchronize your neural link before proceeding.*
                """, dp, dsp, mp, o, t);

        Container c = EmbedUtil.containerBranded("SYSTEM ENTRY", "Startup Guide", body, EmbedUtil.BANNER_MAIN);
        c.withAccentColor(EmbedUtil.PRIMARY.getRGB() & 0xFFFFFF);

        member.getUser().openPrivateChannel().queue(
                dm -> dm.sendMessageComponents(c).useComponentsV2(true).queue(null, err -> {}),
                err -> {});
    }
}
