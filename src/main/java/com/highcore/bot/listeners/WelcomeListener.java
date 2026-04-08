package com.highcore.bot.listeners;

import com.highcore.bot.config.Config;
import com.highcore.bot.services.WelcomeCardService;
import com.highcore.bot.utils.EmbedUtil;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.utils.FileUpload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WelcomeListener extends ListenerAdapter {
    private static final Logger log = LoggerFactory.getLogger(WelcomeListener.class);

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        log.info("Member joined: {} in {}", event.getMember().getUser().getName(), event.getGuild().getName());
        
        try {
            byte[] welcomeImage = WelcomeCardService.generateWelcomeCard(event.getMember());
            sendWelcomeMessage(event.getMember(), event.getGuild(), welcomeImage);
        } catch (Exception e) {
            log.error("Failed to generate welcome image", e);
            sendWelcomeMessage(event.getMember(), event.getGuild(), null);
        }

        sendStartupDM(event.getMember());
        logActivity(event.getGuild(), "Member Joined", "A new member has joined: **" + event.getMember().getUser().getName() + "** (" + event.getMember().getId() + ")", com.highcore.bot.utils.EmbedUtil.SUCCESS);
    }

    @Override
    public void onGuildMemberRemove(net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent event) {
        logActivity(event.getGuild(), "Member Left", "A member has left the server: **" + event.getUser().getName() + "** (" + event.getUser().getId() + ")", com.highcore.bot.utils.EmbedUtil.DANGER);
    }

    private void logActivity(Guild guild, String title, String body, java.awt.Color color) {
        TextChannel ch = com.highcore.bot.services.LogManager.get(guild, Config.LOG_JOIN_LEFT);
        if (ch != null) {
            ch.sendMessageComponents(EmbedUtil.activityLog(title, body, color)).useComponentsV2(true).queue();
        }
    }

    private void sendWelcomeMessage(Member member, Guild guild, byte[] image) {
        TextChannel ch = guild.getTextChannelById(Config.WELCOME_CHANNEL_ID);
        if (ch == null) return;

        // 1. Precise 3-line message
        String body = String.format("""
                Welcome : %s, 
                You are the %d'th member .
                Dont forget to visit the line : <#1488795130470072321>
                """, member.getAsMention(), guild.getMemberCount());

        // 2. Build Container using the attached image as a Banner (attachment://welcome.png)
        // This ensures the image is part of the primary UI structure.
        Container c = EmbedUtil.containerBranded("Welcome", "New Member", body, "attachment://welcome.png");
        
        var message = ch.sendMessageComponents(c).useComponentsV2(true);
        if (image != null) {
            message.addFiles(FileUpload.fromData(image, "welcome.png"));
        }
        message.queue();
    }

    private void sendStartupDM(Member member) {
        String hub = "1488795130470072321";

        String body = String.format("""
                Welcome : %s, 
                You are the %d'th member .
                Dont forget to visit the line : <#%s>
                """, member.getUser().getAsMention(), member.getGuild().getMemberCount(), hub);

        Container c = EmbedUtil.containerBranded("Guide", "Startup Information", body, EmbedUtil.BANNER_MAIN);
        member.getUser().openPrivateChannel().queue(
                dm -> dm.sendMessageComponents(c).useComponentsV2(true).queue(null, err -> {}),
                err -> {});
    }
}
