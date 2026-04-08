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
        logActivity(event.getGuild(), "حدث انضمام", "عضو جديد انضم إلينا: **" + event.getMember().getUser().getName() + "** (`" + event.getMember().getId() + "`)", com.highcore.bot.utils.EmbedUtil.SUCCESS);
    }

    @Override
    public void onGuildMemberRemove(net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent event) {
        logActivity(event.getGuild(), "حدث مغادرة", "غادر أحد الأعضاء الوكالة: **" + event.getUser().getName() + "** (`" + event.getUser().getId() + "`)", com.highcore.bot.utils.EmbedUtil.DANGER);
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

        String body = String.format("""
                ## 🎉 مرحباً بك في وكالة هايكور!
                
                **العضو الجديد:** %s
                **الاسم:** %s
                
                > نحن سعداء جداً بانضمامك إلينا. 
                > خذ جولة في أقسامنا واطلع على خدماتنا الاحترافية.
                """, member.getAsMention(), member.getEffectiveName());

        Container c = EmbedUtil.containerBranded("ترحيب", "انضمام عضو جديد", body, null); // Banner URL null as we send file
        
        var message = ch.sendMessageComponents(c).useComponentsV2(true);
        if (image != null) {
            message.addFiles(FileUpload.fromData(image, "welcome.png"));
        }
        message.queue();
    }

    private void sendStartupDM(Member member) {
        String dp = Config.CH_DEV_PRICES, dsp = Config.CH_DESIGN_PRICES, mp = Config.CH_MINECRAFT_PRICES;
        String o = Config.CH_ORDER, t = Config.CH_TICKET;

        String body = String.format("""
                ## 📖 دليل البداية — وكالة هايكور
                
                أهلاً بك في الوكالة. إليك أهم الروابط التي قد تحتاجها:
                
                📒 **الأسعار والقوانين:**
                - <#%s> | <#%s> | <#%s>
                
                🛒 **طلب خدمة جديدة:**
                - <#%s>
                
                ✉️ **الدعم الفني:**
                - <#%s>
                
                *فريقنا مستعد دائماً لمساعدتك في أي وقت.*
                """, dp, dsp, mp, o, t);

        Container c = EmbedUtil.containerBranded("دليل", "دليل البداية", body, EmbedUtil.BANNER_MAIN);
        member.getUser().openPrivateChannel().queue(
                dm -> dm.sendMessageComponents(c).useComponentsV2(true).queue(null, err -> {}),
                err -> {});
    }
}
