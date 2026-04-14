package com.highcore.bot;

import com.highcore.bot.api.RestApiServer;
import com.highcore.bot.commands.*;
import com.highcore.bot.config.Config;
import com.highcore.bot.listeners.*;
import com.highcore.bot.services.*;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws Exception {
        System.out.println("\u001B[33m" +
                "  _    _  _____  _____  _    _  _____  ____   _____  ______ \n" +
                " | |  | ||_   _||  ___|| |  | ||  ___||  _ \\ |  ___||  ____|\n" +
                " | |__| |  | |  | | __ | |__| || |___ | |_) || |___ | |__   \n" +
                " |  __  |  | |  | | \u2514\u2510| |  __  ||  ___||  _  /|  ___||  __|  \n" +
                " | |  | | _| |_ | |__| || |  | || |___ | | \\ \\| |___ | |____ \n" +
                " |_|  |_||_____| \\____/ |_|  |_||_____||_|  \\_\\_____||______|\n" +
                "               HIGH CORE - GOLDEN TERMINAL v1.2.0\n" +
                "\u001B[0m");

        if (Config.DISCORD_TOKEN == null || Config.DISCORD_TOKEN.isEmpty()) { 
            log.error("CRITICAL: Token missing."); 
            System.exit(1); 
        }

        JDA jda = JDABuilder.createDefault(Config.DISCORD_TOKEN)
                .setStatus(OnlineStatus.ONLINE)
                .setActivity(Activity.watching("High Core [v1.2.0]"))
                .enableIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_MEMBERS, GatewayIntent.MESSAGE_CONTENT,
                        GatewayIntent.GUILD_MESSAGE_REACTIONS, GatewayIntent.GUILD_MODERATION, GatewayIntent.GUILD_VOICE_STATES, GatewayIntent.GUILD_PRESENCES)
                .setMemberCachePolicy(MemberCachePolicy.ALL)
                .addEventListeners(
                        new MessageListener(), 
                        new SlashCommands(),
                        new GiveawayCommands(),
                        new ModerationCommands(),
                        new InfoCommands(),
                        new GeneralCommands(),
                        new ServerLogListener(),
                        new UserLogListener(),
                        new WelcomeListener(), 
                        new GiveawayListener(),
                        new CentralInteractionListener()
                )
        .build().awaitReady();

        Guild guild = jda.getGuildById(Config.GUILD_ID);
        if (guild != null) LogManager.initialize(guild);

        registerCommands(jda);
        AutoReplyService.refreshCache();
        TicketReminderService.start(jda);
        GiveawayService.start(jda);
        SettingSyncService.start();
        WordFilterService.init();
        TelemetryService.start(jda);
        RestApiServer.start(jda);

        log.info("Golden Terminal v1.2.0 fully operational!");
    }

    private static void registerCommands(JDA jda) {
        Guild guild = jda.getGuildById(Config.GUILD_ID);
        if (guild == null) return;

        guild.updateCommands().addCommands(
                // ===== Core & Business =====
                Commands.slash("startup", "عــرض لــوحــة الــتــحــكــم الــرئــيــســيــة"),
                Commands.slash("tickets", "فــتــح نــظــام الــتــذاكــر"),
                Commands.slash("ping", "فــحــص ســرعــة اتــصــال الــبــوت"),
                Commands.slash("bc", "بــث رســالــة جــمــاعــيــة")
                        .addOption(OptionType.STRING, "message", "مــحــتــوى الــرســالــة", true)
                        .addOption(OptionType.ROLE, "role", "الــرتــبــة الــمــســتــهــدفــة", false)
                        .addOption(OptionType.ATTACHMENT, "attachment", "مــرفــق الــمــيــديــا", false),

                // ===== Moderation =====
                Commands.slash("setnick", "تــغــيــيــر لــقــب عــضــو")
                        .addOption(OptionType.USER, "user", "الــعــضــو الــمــســتــهــدف", true)
                        .addOption(OptionType.STRING, "nick", "الــلــقــب الــجــديــد", true),
                Commands.slash("ban", "حــظــر عــضــو مــن الــســيــرفــر")
                        .addOption(OptionType.USER, "user", "الــعــضــو", true)
                        .addOption(OptionType.STRING, "reason", "الــســبــب", false),
                Commands.slash("unban", "إلــغــاء حــظــر عــضــو")
                        .addOption(OptionType.STRING, "user_id", "مــعــرف الــعــضــو", true),
                Commands.slash("unban-all", "مــســح قــائمة الــحــظــر بــالــكــامــل"),
                Commands.slash("kick", "طــرد عــضــو مــن الــســيــرفــر")
                        .addOption(OptionType.USER, "user", "الــعــضــو الــمــســتــهــدف", true)
                        .addOption(OptionType.STRING, "reason", "الــســبــب", false),
                Commands.slash("vkick", "طــرد عــضــو مــن الــقـنــاة الــصــوتــيــة")
                        .addOption(OptionType.USER, "user", "الــعــضــو الــمــســتــهــدف", true),
                Commands.slash("mute-text", "كــتــم عــضــو كــتــابــيــاً")
                        .addOption(OptionType.USER, "user", "الــعــضــو الــمــســتــهــدف", true),
                Commands.slash("unmute-text", "إلــغــاء كــتــم عــضــو كــتــابــيــاً")
                        .addOption(OptionType.USER, "user", "الــعــضــو الــمــســتــهــدف", true),
                Commands.slash("mute-check", "فــحــص حــالــة الــكــتــم لــعــضــو")
                        .addOption(OptionType.USER, "user", "الــعــضــو الــمــســتــهــدف", true),
                Commands.slash("mute-voice", "كــتــم عــضــو صــوتــيــاً")
                        .addOption(OptionType.USER, "user", "الــعــضــو الــمــســتــهــدف", true),
                Commands.slash("unmute-voice", "إلــغــاء كــتــم عــضــو صــوتــيــاً")
                        .addOption(OptionType.USER, "user", "الــعــضــو الــمــســتــهــدف", true),
                Commands.slash("timeout", "إســكــات عــضــو لــفــتـرة")
                        .addOption(OptionType.USER, "user", "الــعــضــو الــمــســتــهــدف", true)
                        .addOption(OptionType.INTEGER, "duration", "الــمــدة بــالــدقــائــق", true),
                Commands.slash("untimeout", "إلــغــاء إســكــات الــعــضــو")
                        .addOption(OptionType.USER, "user", "الــعــضــو الــمــســتــهــدف", true),
                Commands.slash("clear", "مــســح رســائل الــقــنــاة")
                        .addOption(OptionType.INTEGER, "amount", "عــدد الــرســائــل", true),
                Commands.slash("move", "نــقــل عــضــو لــقــنــاة صــوتــيــة")
                        .addOption(OptionType.USER, "user", "الــعــضــو الــمــســتــهــدف", true)
                        .addOption(OptionType.CHANNEL, "channel", "الــوجــهــة", true),
                Commands.slash("role", "إدارة رتــب الــعــضــو")
                        .addOption(OptionType.USER, "user", "الــعــضــو الــمــســتــهــدف", true)
                        .addOption(OptionType.ROLE, "role", "الــرتــبــة الــمــســتــهــدفــة", true),
                Commands.slash("temprole", "إعــطــاء رتــبــة مــؤقــتــة")
                        .addOption(OptionType.USER, "user", "الــعــضــو الــمــســتــهــدف", true)
                        .addOption(OptionType.ROLE, "role", "الــرتــبــة الــمــســتــهــدفــة", true)
                        .addOption(OptionType.INTEGER, "duration", "الــمــدة بــالــســاعــات", true),
                Commands.slash("rar", "ســحــب جــمــيــع الــرتــب")
                        .addOption(OptionType.USER, "user", "الــعــضــو الــمــســتــهــدف", true),
                Commands.slash("inrole", "عــرض الــمــتــواجــديــن فــي رتــبــة")
                        .addOption(OptionType.ROLE, "role", "الــرتــبــة الــمــســتــهــدفــة", true),
                Commands.slash("warn-add", "إضــافــة تــحــذير لــعــضــو")
                        .addOption(OptionType.USER, "user", "الــعــضــو الــمــســتــهــدف", true)
                        .addOption(OptionType.STRING, "reason", "الــســبــب", false),
                Commands.slash("warn-remove", "إزالــة تــحــذير لــعــضــو")
                        .addOption(OptionType.USER, "user", "الــعــضــو الــمــســتــهــدف", true)
                        .addOption(OptionType.INTEGER, "id", "مــعــرف الــتــحــذير", false),
                Commands.slash("warnings", "عــرض تــحــذيرات الــعــضــو")
                        .addOption(OptionType.USER, "user", "الــعــضــو الــمــســتــهــدف", true),
                Commands.slash("violations", "عــرض مــخــالــفــات الــفــلــتــر")
                        .addOption(OptionType.USER, "user", "الــعــضــو الــمــســتــهــدف", true),
                Commands.slash("violations-clear", "مــســح مــخــالــفــات الــفــلــتــر")
                        .addOption(OptionType.USER, "user", "الــعــضــو الــمــســتــهــدف", true),
                Commands.slash("lock", "إغــلاق الــقــنــاة الــحــالــيــة"),
                Commands.slash("unlock", "فــتــح الــقــنــاة الــحــالــيــة"),
                Commands.slash("hide", "إخــفــاء الــقــنــاة الــحــالــيــة"),
                Commands.slash("show", "إظــهــار الــقــنــاة الــحــالــيــة"),
                Commands.slash("slowmode", "تــفــعــيــل الــوضــع الــبــطــيء")
                        .addOption(OptionType.INTEGER, "seconds", "الــثــوانــي", true),
                Commands.slash("add-emoji", "إضــافــة إيــمــوجــي جــديــد")
                        .addOption(OptionType.ATTACHMENT, "image", "الــصــورَة", true)
                        .addOption(OptionType.STRING, "name", "اســم الــإيــمــوجــي", true),
                Commands.slash("role-multiple", "إدارة الــرتــب لــلــجــمــيــع")
                        .addOption(OptionType.ROLE, "role", "الــرتــبــة الــمــســتــهــدفــة", true)
                        .addOption(OptionType.STRING, "action", "الــإجــراء", true),

                // ===== Giveaways =====
                Commands.slash("giveaway", "فــتــح لــوحــة تــحــكــم الــمــســابــقــات"),

                // ===== Information =====
                Commands.slash("profile", "عــرض مــلــف الــعــضــو")
                        .addOption(OptionType.USER, "user", "الــعــضــو", false),
                Commands.slash("user", "عــرض مــعــلــومــات الــعــضــو")
                        .addOption(OptionType.USER, "user", "الــعــضــو", false),
                Commands.slash("avatar", "عــرض صــورة حــســاب الــعــضــو")
                        .addOption(OptionType.USER, "user", "الــعــضــو", false),
                Commands.slash("server", "عــرض مــعــلــومــات الــســيــرفــر"),
                Commands.slash("roles", "قــائمة رتــب الــســيــرفــر"),
                Commands.slash("banner", "عــرض بــانــر الــعــضــو")
                        .addOption(OptionType.USER, "user", "الــعــضــو", false),
                Commands.slash("invites", "عــرض إحــصــائــيــات الــدعــوات")
                        .addOption(OptionType.USER, "user", "الــعــضــو", false),
                Commands.slash("server-avatar", "عــرض أيــقــونــة الــســيــرفــر"),
                Commands.slash("server-banner", "عــرض بــانــر الــســيــرفــر"),

                // ===== General & Fun =====
                Commands.slash("translate", "تــرجــمــة الــنــصــوص")
                        .addOption(OptionType.STRING, "text", "الــمــحــتــوى", true)
                        .addOption(OptionType.STRING, "language", "الــلــغــة", true),
                Commands.slash("roll", "رمــي الــنــرد"),
                Commands.slash("title", "تــغــيــيــر لــقــبــك الــشــخــصــي")
                        .addOption(OptionType.STRING, "title", "الــلــقــب الــجــديــد", true),
                Commands.slash("suggest", "تــقــديــم اقــتــراح جــديــد")
                        .addOption(OptionType.STRING, "suggestion", "الــمــحــتــوى", true),
                Commands.slash("suggestion", "إدارة الاقــتــراحــات")
                        .addOption(OptionType.INTEGER, "id", "مــعــرف الاقــتــراح", false)
                        .addOption(OptionType.STRING, "action", "الــإجــراء", false)
        ).queue();
    }
}
