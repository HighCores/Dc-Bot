package com.highcore.bot.listeners;

import com.highcore.bot.config.Config;
import com.highcore.bot.utils.EmbedUtil;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WelcomeListener extends ListenerAdapter {
    private static final Logger log = LoggerFactory.getLogger(WelcomeListener.class);

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        Guild guild = event.getGuild();
        Member member = event.getMember();
        assignMemberRole(guild, member);
        sendWelcomeEmbed(guild, member);
        sendStartupDM(member);
    }

    private void assignMemberRole(Guild guild, Member member) {
        if (Config.ROLE_MEMBER == null || Config.ROLE_MEMBER.isEmpty()) return;
        Role role = guild.getRoleById(Config.ROLE_MEMBER);
        if (role == null) return;
        guild.addRoleToMember(member, role).queue(null, err -> log.warn("Could not assign role: {}", err.getMessage()));
    }

    private void sendWelcomeEmbed(Guild guild, Member member) {
        if (Config.WELCOME_CHANNEL_ID == null || Config.WELCOME_CHANNEL_ID.isEmpty()) return;
        TextChannel ch = guild.getTextChannelById(Config.WELCOME_CHANNEL_ID);
        if (ch == null) return;

        String s = Config.CH_STARTUP;
        var eb = EmbedUtil.withBanner().setColor(EmbedUtil.ACCENT_TEAL)
                .setThumbnail(member.getEffectiveAvatarUrl())
                .setDescription(String.format("""
                        ## \uD83C\uDF89 Welcome to Highcore Agency!

                        > Hey **%s**! Welcome to the server \uD83D\uDC9C

                        You are member **#%d** \uD83C\uDF1F

                        > \u25B8 Check out <#%s> to learn everything about us
                        > \u25B8 Check your DMs \u2014 we sent you a startup guide \uD83D\uDCD6
                        > \u25B8 Respect the rules and enjoy your stay!
                        """, member.getEffectiveName(), guild.getMemberCount(),
                        s != null ? s : "start-up"));

        ch.sendMessage(member.getAsMention()).setEmbeds(eb.build()).queue();
    }

    private void sendStartupDM(Member member) {
        String h = Config.CH_HIGHCORE, st = Config.CH_SERVICE_TERMS, u = Config.CH_UPDATES;
        String dp = Config.CH_DEV_PRICES, dsp = Config.CH_DESIGN_PRICES, mp = Config.CH_MINECRAFT_PRICES;
        String o = Config.CH_ORDER, t = Config.CH_TICKET;

        var eb = EmbedUtil.branded().setColor(EmbedUtil.PRIMARY)
                .setDescription(String.format("""
                        ## \uD83D\uDCD6 Highcore Agency \u2014 Startup Guide

                        > Welcome **%s**! Here's your quick guide \uD83D\uDE80

                        ### \uD83C\uDFE0 About Us
                        > <#%s> \u2014 Learn about Highcore Agency
                        > <#%s> \u2014 Service terms & conditions
                        > <#%s> \u2014 Latest updates & offers

                        ### \uD83D\uDCB0 Pricing
                        > <#%s> \u2014 Development prices
                        > <#%s> \u2014 Design prices
                        > <#%s> \u2014 Minecraft prices

                        ### \uD83D\uDECD\uFE0F Order Now
                        > <#%s> \u2014 Place an order directly
                        > <#%s> \u2014 Open a support ticket

                        ### \u2753 Need Help?
                        > Open a ticket and our team will assist you ASAP! \uD83D\uDE0A
                        """,
                        member.getEffectiveName(),
                        h != null ? h : "highcore", st != null ? st : "service-terms", u != null ? u : "updates",
                        dp != null ? dp : "dev-prices", dsp != null ? dsp : "design-prices", mp != null ? mp : "minecraft-prices",
                        o != null ? o : "order", t != null ? t : "ticket"));

        member.getUser().openPrivateChannel().queue(
                dm -> dm.sendMessageEmbeds(eb.build()).queue(null, err -> {}),
                err -> {});
    }
}
