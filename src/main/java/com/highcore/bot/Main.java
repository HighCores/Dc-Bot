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
                .enableCache(net.dv8tion.jda.api.utils.cache.CacheFlag.ONLINE_STATUS)
                .setMemberCachePolicy(MemberCachePolicy.ALL)
                .addEventListeners(
                        new MessageListener(), 
                        new CommandLogListener(),
                        new SlashCommands(),
                        new GiveawayCommands(),
                        new AutoReplyCommands(),
                        new BannedWordCommands(),
                        new ModerationCommands(),
                        new InfoCommands(),
                        new GeneralCommands(),
                        new ServerLogListener(),
                        new UserLogListener(),
                        new WelcomeListener(), 
                        new GiveawayListener(),
                        new DiscountListener(),
                        new CentralInteractionListener()
                )
        .build().awaitReady();

        Guild guild = jda.getGuildById(Config.GUILD_ID);
        if (guild != null) LogManager.initialize(guild);

        registerCommands(jda);
        AutoReplyService.init();
        TicketReminderService.start(jda);
        GiveawayService.start(jda);
        SettingSyncService.start();
        WordFilterService.init();
        TelemetryService.start(jda);
        RestApiServer.start(jda);

        log.info("=================================================");
        log.info("   HIGHCORE BOT - DEBUG VERSION 2.0 (TRACE ON)   ");
        log.info("=================================================");
    }

    private static void registerCommands(JDA jda) {
        Guild guild = jda.getGuildById(Config.GUILD_ID);
        if (guild == null) return;

        var adminPerms = net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions.enabledFor(net.dv8tion.jda.api.Permission.ADMINISTRATOR);

        guild.updateCommands().addCommands(
                // ===== Core & Business (Administrative) =====
                Commands.slash("startup", "عــرض لــوحــة الــتــحــكــم الــرئــيــســيــة").setDefaultPermissions(adminPerms),
                Commands.slash("tickets", "فــتــح نــظــام الــتــذاكــر").setDefaultPermissions(adminPerms),
                Commands.slash("bc", "بــث رســالــة جــمــاعــيــة")
                        .addOption(OptionType.STRING, "message", "مــحــتــوى الــرســالــة", true)
                        .addOption(OptionType.ROLE, "role", "الــرتــبــة الــمــســتــهــدفــة", false)
                        .addOption(OptionType.ATTACHMENT, "attachment", "مــرفــق الــمــيــديــا", false)
                        .setDefaultPermissions(adminPerms),
                Commands.slash("discounts", "إدارة نــظــام الــخــصــومــات والــمــجــدول").setDefaultPermissions(adminPerms),

                // ===== Moderation (Administrative) =====
                Commands.slash("setnick", "تــغــيــيــر لــقــب عــضــو")
                        .addOption(OptionType.USER, "user", "الــعــضــو الــمــســتــهــدف", true)
                        .addOption(OptionType.STRING, "nick", "الــلــقــب الــجــديــد", true)
                        .setDefaultPermissions(adminPerms),
                Commands.slash("ban", "حــظــر عــضــو مــن الــســيــرفــر")
                        .addOption(OptionType.USER, "user", "الــعــضــو", true)
                        .addOption(OptionType.STRING, "reason", "الــســبــب", false)
                        .setDefaultPermissions(adminPerms),
                Commands.slash("unban", "إلــغــاء حــظــر عــضــو")
                        .addOption(OptionType.STRING, "user_id", "مــعــرف الــعــضــو", true)
                        .setDefaultPermissions(adminPerms),
                Commands.slash("unban-all", "مــســح قــائمة الــحــظــر بــالــكــامــل").setDefaultPermissions(adminPerms),
                Commands.slash("kick", "طــرد عــضــو مــن الــســيــرفــر")
                        .addOption(OptionType.USER, "user", "الــعــضــو الــمــســتــهــدف", true)
                        .addOption(OptionType.STRING, "reason", "الــســبــب", false)
                        .setDefaultPermissions(adminPerms),
                Commands.slash("vkick", "طــرد عــضــو مــن الــقـنــاة الــصــوتــيــة")
                        .addOption(OptionType.USER, "user", "الــعــضــو الــمــســتــهــدف", true)
                        .setDefaultPermissions(adminPerms),
                Commands.slash("mute-text", "كــتــم عــضــو كــتــابــيــاً")
                        .addOption(OptionType.USER, "user", "الــعــضــو الــمــســتــهــدف", true)
                        .setDefaultPermissions(adminPerms),
                Commands.slash("unmute-text", "إلــغــاء كــتــم عــضــو كــتــابــيــاً")
                        .addOption(OptionType.USER, "user", "الــعــضــو الــمــســتــهــدف", true)
                        .setDefaultPermissions(adminPerms),
                Commands.slash("mute-check", "فــحــص حــالــة الــكــتــم لــعــضــو")
                        .addOption(OptionType.USER, "user", "الــعــضــو الــمــســتــهــدف", true)
                        .setDefaultPermissions(adminPerms),
                Commands.slash("mute-voice", "كــتــم عــضــو صــوتــيــاً")
                        .addOption(OptionType.USER, "user", "الــعــضــو الــمــســتــهــدف", true)
                        .setDefaultPermissions(adminPerms),
                Commands.slash("unmute-voice", "إلــغــاء كــتــم عــضــو صــوتــيــاً")
                        .addOption(OptionType.USER, "user", "الــعــضــو الــمــســتــهــدف", true)
                        .setDefaultPermissions(adminPerms),
                Commands.slash("timeout", "إســكــات عــضــو لــفــتـرة")
                        .addOption(OptionType.USER, "user", "الــعــضــو الــمــســتــهــدف", true)
                        .addOption(OptionType.INTEGER, "duration", "الــمــدة بــالــدقــائــق", true)
                        .setDefaultPermissions(adminPerms),
                Commands.slash("untimeout", "إلــغــاء إســكــات الــعــضــو")
                        .addOption(OptionType.USER, "user", "الــعــضــو الــمــســتــهــدف", true)
                        .setDefaultPermissions(adminPerms),
                Commands.slash("clear", "مــســح رســائل الــقــنــاة")
                        .addOption(OptionType.INTEGER, "amount", "عــدد الــرســائــل", true)
                        .setDefaultPermissions(adminPerms),
                Commands.slash("move", "نــقــل عــضــو لــقــنــاة صــوتــيــة")
                        .addOption(OptionType.USER, "user", "الــعــضــو الــمــســتــهــدف", true)
                        .addOption(OptionType.CHANNEL, "channel", "الــوجــهــة", true)
                        .setDefaultPermissions(adminPerms),
                Commands.slash("role", "إدارة رتــب الــعــضــو")
                        .addOption(OptionType.USER, "user", "الــعــضــو الــمــســتــهــدف", true)
                        .addOption(OptionType.ROLE, "role", "الــرتــبــة الــمــســتــهــدفــة", true)
                        .setDefaultPermissions(adminPerms),
                Commands.slash("temprole", "إعــطــاء رتــبــة مــؤقــتــة")
                        .addOption(OptionType.USER, "user", "الــعــضــو الــمــســتــهــدف", true)
                        .addOption(OptionType.ROLE, "role", "الــرتــبــة الــمــســتــهــدفــة", true)
                        .addOption(OptionType.INTEGER, "duration", "الــمــدة بــالــســاعــات", true)
                        .setDefaultPermissions(adminPerms),
                Commands.slash("rar", "ســحــب جــمــيــع الــرتــب")
                        .addOption(OptionType.USER, "user", "الــعــضــو الــمــســتــهــدف", true)
                        .setDefaultPermissions(adminPerms),
                Commands.slash("inrole", "عــرض الــمــتــواجــديــن فــي رتــبــة")
                        .addOption(OptionType.ROLE, "role", "الــرتــبــة الــمــســتــهــدفــة", true)
                        .setDefaultPermissions(adminPerms),
                Commands.slash("warn-add", "إضــافــة تــحــذير لــعــضــو")
                        .addOption(OptionType.USER, "user", "الــعــضــو الــمــســتــهــدف", true)
                        .addOption(OptionType.STRING, "reason", "الــســبــب", false)
                        .setDefaultPermissions(adminPerms),
                Commands.slash("warn-remove", "إزالــة تــحــذير لــعــضــو")
                        .addOption(OptionType.USER, "user", "الــعــضــو الــمــســتــهــدف", true)
                        .addOption(OptionType.INTEGER, "id", "مــعــرف الــتــحــذير", false)
                        .setDefaultPermissions(adminPerms),
                Commands.slash("warnings", "عــرض تــحــذيرات الــعــضــو")
                        .addOption(OptionType.USER, "user", "الــعــضــو الــمــســتــهــدف", true)
                        .setDefaultPermissions(adminPerms),
                Commands.slash("violations", "عــرض مــخــالــفــات الــفــلــتــر")
                        .addOption(OptionType.USER, "user", "الــعــضــو الــمــســتــهــدف", true)
                        .setDefaultPermissions(adminPerms),
                Commands.slash("violations-clear", "مــســح مــخــالــفــات الــفــلــتــر")
                        .addOption(OptionType.USER, "user", "الــعــضــو الــمــســتــهــدف", true)
                        .setDefaultPermissions(adminPerms),
                Commands.slash("lock", "إغــلاق الــقــنــاة الــحــالــيــة").setDefaultPermissions(adminPerms),
                Commands.slash("unlock", "فــتــح الــقــنــاة الــحــالــيــة").setDefaultPermissions(adminPerms),
                Commands.slash("hide", "إخــفــاء الــقــنــاة الــحــالــيــة").setDefaultPermissions(adminPerms),
                Commands.slash("show", "إظــهــار الــقــنــاة الــحــالــيــة").setDefaultPermissions(adminPerms),
                Commands.slash("slowmode", "تــفــعــيــل الــوضــع الــبــطــيء")
                        .addOption(OptionType.INTEGER, "seconds", "الــثــوانــي", true)
                        .setDefaultPermissions(adminPerms),
                Commands.slash("add-emoji", "إضــافــة إيــمــوجــي جــديــد")
                        .addOption(OptionType.ATTACHMENT, "image", "الــصــورَة", true)
                        .addOption(OptionType.STRING, "name", "اســم الــإيــمــوجــي", true)
                        .setDefaultPermissions(adminPerms),
                Commands.slash("role-multiple", "إدارة الــرتــب لــلــجــمــيــع")
                        .addOption(OptionType.ROLE, "role", "الــرتــبــة الــمــســتــهــدفــة", true)
                        .addOption(OptionType.STRING, "action", "الــإجــراء", true)
                        .setDefaultPermissions(adminPerms),

                // ===== Giveaways (Administrative) =====
                Commands.slash("giveaway", "فــتــح لــوحــة تــحــكــم الــمــســابــقــات").setDefaultPermissions(adminPerms),
                Commands.slash("replay", "إدارة نــظــام الــرد الــتــلــقــائــي").setDefaultPermissions(adminPerms),
                Commands.slash("banned-words", "إدارة نــظــام الــكــلــمــات الــمــحــظــورة").setDefaultPermissions(adminPerms),

                // ===== Emulation (Administrative) =====
                Commands.slash("boter", "إرســال رســالــة بــاســم الــبــوت")
                        .addOption(OptionType.CHANNEL, "channel", "الــقــناة الــمــســتــهــدفــة", false)
                        .addOption(OptionType.ATTACHMENT, "file1", "الــمــرفــق الأوّل", false)
                        .addOption(OptionType.ATTACHMENT, "file2", "الــمــرفــق الــثــانــي", false)
                        .addOption(OptionType.ATTACHMENT, "file3", "الــمــرفــق الــثــالــث", false)
                        .setDefaultPermissions(adminPerms),

                // ===== Information (Administrative & Public) =====
                Commands.slash("roles", "قــائمة رتــب الــســيــرفــر").setDefaultPermissions(adminPerms), // Restricted per user request

                // ===== Information (Public) =====
                Commands.slash("ping", "فــحــص ســرعــة اتــصــال الــبــوت"),
                Commands.slash("profile", "عــرض مــلــف الــعــضــو")
                        .addOption(OptionType.USER, "user", "الــعــضــو", false),
                Commands.slash("avatar", "عــرض صــورة حــســاب الــعــضــو")
                        .addOption(OptionType.USER, "user", "الــعــضــو", false),
                Commands.slash("server", "عــرض مــعــلــومــات الــســيــرفــر"),
                Commands.slash("banner", "عــرض بــانــر الــعــضــو")
                        .addOption(OptionType.USER, "user", "الــعــضــو", false),
                Commands.slash("invites", "عــرض إحــصــائــيــات الــدعــوات")
                        .addOption(OptionType.USER, "user", "الــعــضــو", false),
                Commands.slash("server-avatar", "عــرض أيــقــونــة الــســيــرفــر"),
                Commands.slash("server-banner", "عــرض بــانــر الــســيــرفــر"),

                // ===== General & Fun (Public) =====
                Commands.slash("translate", "تــرجــمــة الــنــصــوص")
                        .addOption(OptionType.STRING, "text", "الــمــحــتــوى", true)
                        .addOption(OptionType.STRING, "language", "الــلــغــة", true),
                Commands.slash("roll", "رمــي الــنــرد")
        ).queue();
    }
}
