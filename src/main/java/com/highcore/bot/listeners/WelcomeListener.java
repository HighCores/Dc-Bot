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
        logActivity(event.getGuild(), "JOIN EVENT", "New Member joined: " + event.getMember().getAsMention(), com.highcore.bot.utils.EmbedUtil.SUCCESS);
    }

    @Override
    public void onGuildMemberRemove(net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent event) {
        logActivity(event.getGuild(), "LEAVE EVENT", "Member left the agency: **" + event.getUser().getName() + "** (" + event.getUser().getAsMention() + ")", com.highcore.bot.utils.EmbedUtil.DANGER);
    }

    private void logActivity(Guild guild, String title, String body, java.awt.Color color) {
        TextChannel ch = com.highcore.bot.services.LogManager.get(guild, Config.LOG_JOIN_LEFT);
        if (ch != null) {
            ch.sendMessageComponents(EmbedUtil.activityLog(title, body, color)).useComponentsV2(true).queue();
        }
    }

    private void sendWelcomeMessage(Member member, Guild guild) {
        TextChannel ch = guild.getTextChannelById(Config.WELCOME_CHANNEL_ID);
        if (ch == null) return;

        String body = String.format("""
                ## 🎉 Welcome to Highcore Agency!
                
                **New Member:** %s
                **Display Name:** %s
                
                > We are glad to have you with us at Highcore Agency. 
                > Please feel free to explore our professional services and active projects.
                """, member.getAsMention(), member.getEffectiveName());

        Container c = EmbedUtil.containerBranded("AGENCY WELCOME", "New Member joined", body, EmbedUtil.BANNER_MAIN);
        c.withAccentColor(EmbedUtil.ACCENT_TEAL.getRGB() & 0xFFFFFF);

        ch.sendMessageComponents(c).useComponentsV2(true).queue();
    }

    private void sendStartupDM(Member member) {
        String dp = Config.CH_DEV_PRICES, dsp = Config.CH_DESIGN_PRICES, mp = Config.CH_MINECRAFT_PRICES;
        String o = Config.CH_ORDER, t = Config.CH_TICKET;

        String body = String.format("""
                ## 📖 Highcore Agency — Startup Guide
                
                Welcome to the agency. Here are your primary operational links:
                
                📒 **Protocols & Prices:**
                - <#%s> | <#%s> | <#%s>
                
                🛒 **Resource Procurement:**
                - <#%s>
                
                ✉️ **Operational Support:**
                - <#%s>
                
                *Our team is ready to assist you in any department.*
                """, dp, dsp, mp, o, t);

        Container c = EmbedUtil.containerBranded("AGENCY GUIDE", "Startup Guide", body, EmbedUtil.BANNER_MAIN);
        c.withAccentColor(EmbedUtil.PRIMARY.getRGB() & 0xFFFFFF);

        member.getUser().openPrivateChannel().queue(
                dm -> dm.sendMessageComponents(c).useComponentsV2(true).queue(null, err -> {}),
                err -> {});
    }
}
