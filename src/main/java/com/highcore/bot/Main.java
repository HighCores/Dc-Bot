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
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.audio.AudioModuleConfig;
import moe.kyokobot.libdave.NativeDaveFactory;
import moe.kyokobot.libdave.jda.LDJDADaveSessionFactory;
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
                                .setAudioModuleConfig(new AudioModuleConfig()
                                                .withDaveSessionFactory(new LDJDADaveSessionFactory(new NativeDaveFactory())))
                                .setStatus(OnlineStatus.ONLINE)
                                .setActivity(Activity.watching("High Core [v1.2.0]"))
                                .enableIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_MEMBERS,
                                                GatewayIntent.MESSAGE_CONTENT,
                                                GatewayIntent.GUILD_MESSAGE_REACTIONS, GatewayIntent.GUILD_MODERATION,
                                                GatewayIntent.GUILD_VOICE_STATES, GatewayIntent.GUILD_PRESENCES)
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
                                                new TranslationListener(),
                                                new DiscountListener(),
                                                new VoucherCommands(),
                                                new VoiceRecordingListener(),
                                                new CentralInteractionListener())
                                .build().awaitReady();

                Guild guild = jda.getGuildById(Config.GUILD_ID);
                if (guild != null)
                        LogManager.initialize(guild);

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
                if (guild == null)
                        return;

                var adminPerms = net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
                                .enabledFor(net.dv8tion.jda.api.Permission.ADMINISTRATOR);

                guild.updateCommands().addCommands(
                                // ===== Core & Business (Administrative) =====
                                Commands.slash("startup", "فـــتــــح بـــنـــل الـــتـــحـــكـــم")
                                                .setDefaultPermissions(adminPerms),
                                Commands.slash("tickets", "فـــتــــح بـــنـــل الــتـــكـــتــات")
                                                .setDefaultPermissions(adminPerms),
                                Commands.slash("bc", "إرســـــال بـــــروكــــاســــت لــــلــــجــــمــــيــــع")
                                                .addOption(OptionType.STRING, "message", "مـــحـــتـــوى الـــرســـالـــة",
                                                                true)
                                                .addOption(OptionType.ROLE, "role",
                                                                "الـــرتـــبـــة الـــمـــســـتـــهـــدفـــة", false)
                                                .addOption(OptionType.ATTACHMENT, "attachment",
                                                                "الـــمـــلـــف الـــمـــرفـــق", false)
                                                .setDefaultPermissions(adminPerms),
                                Commands.slash("discounts", "إدارة نــــظــــام الــــتـــخـــفـــيـــضــــات")
                                                .setDefaultPermissions(adminPerms),

                                // ===== Moderation (Administrative) =====
                                Commands.slash("setnick", "تــــغــــيــــيــــر لــــقــــب الـــعـــضـــو")
                                                .addOption(OptionType.USER, "user", "الـــعـــضـــو الـــمـــســـتـــهـــدف",
                                                                true)
                                                .addOption(OptionType.STRING, "nick", "الـــلـــقـــب الـــجـــديـــد", true)
                                                .setDefaultPermissions(adminPerms),
                                Commands.slash("ban", "بـــــان لــــلــــعــــضـــــو")
                                                .addOption(OptionType.USER, "user", "The member", true)
                                                .addOption(OptionType.STRING, "reason", "الـــســـبـــب", false)
                                                .setDefaultPermissions(adminPerms),
                                Commands.slash("unban", "فـــــك الــــبــــان عــــن الـــعـــضــــو")
                                                .addOption(OptionType.STRING, "user_id", "إي دي الـــعـــضـــو", true)
                                                .setDefaultPermissions(adminPerms),
                                Commands.slash("unban-all", "فـــــك الــــبــــان عــــن الـــجـــمـــيــــع")
                                                .setDefaultPermissions(adminPerms),
                                Commands.slash("kick", "طـــــرد الــــعــــضــــو مــــن الـــســـيـــرفــــر")
                                                .addOption(OptionType.USER, "user", "الـــعـــضـــو الـــمـــســـتـــهـــدف",
                                                                true)
                                                .addOption(OptionType.STRING, "reason", "الـــســـبـــب", false)
                                                .setDefaultPermissions(adminPerms),
                                Commands.slash("vkick", "طـــــرد الــــعــــضــــو مــــن الـــروم الـــصـــوتـــي")
                                                .addOption(OptionType.USER, "user", "الـــعـــضـــو الـــمـــســـتـــهـــدف",
                                                                true)
                                                .setDefaultPermissions(adminPerms),
                                Commands.slash("rec", "إدارة نــــظــــام الــــتـــســـجـــيـــل الـــصـــوتـــي")
                                                .setDefaultPermissions(adminPerms),
                                Commands.slash("mute-text", "إعـــــطــــاء مـــيـــوت كـــتـــابـــي")
                                                .addOption(OptionType.USER, "user", "الـــعـــضـــو الـــمـــســـتـــهـــدف",
                                                                true)
                                                .setDefaultPermissions(adminPerms),
                                Commands.slash("unmute-text", "فـــــك الـــمـــيـــوت الـــكـــتـــابـــي")
                                                .addOption(OptionType.USER, "user", "الـــعـــضـــو الـــمـــســـتـــهـــدف",
                                                                true)
                                                .setDefaultPermissions(adminPerms),
                                Commands.slash("mute-check", "الـــتـــحـــقـــق مـــن حـــالـــة الـــمـــيـــوت")
                                                .addOption(OptionType.USER, "user", "الـــعـــضـــو الـــمـــســـتـــهـــدف",
                                                                true)
                                                .setDefaultPermissions(adminPerms),
                                Commands.slash("mute-voice", "إعـــــطــــاء مـــيـــوت صـــوتـــي")
                                                .addOption(OptionType.USER, "user", "الـــعـــضـــو الـــمـــســـتـــهـــدف",
                                                                true)
                                                .setDefaultPermissions(adminPerms),
                                Commands.slash("unmute-voice", "فـــــك الـــمـــيـــوت الـــصـــوتـــي")
                                                .addOption(OptionType.USER, "user", "الـــعـــضـــو الـــمـــســـتـــهـــدف",
                                                                true)
                                                .setDefaultPermissions(adminPerms),
                                Commands.slash("timeout", "إعـــــطــــاء تـــايـــم أوت لـــلـــعـــضـــو")
                                                .addOption(OptionType.USER, "user", "الـــعـــضـــو الـــمـــســـتـــهـــدف",
                                                                true)
                                                .addOption(OptionType.INTEGER, "duration", "الـــمـــدة بـــالـــدقـــائـــق",
                                                                true)
                                                .setDefaultPermissions(adminPerms),
                                Commands.slash("untimeout", "فـــــك الـــتـــايـــم أوت عـــن الـــعـــضـــو")
                                                .addOption(OptionType.USER, "user", "الـــعـــضـــو الـــمـــســـتـــهـــدف",
                                                                true)
                                                .setDefaultPermissions(adminPerms),
                                Commands.slash("clear", "تـــنـــظـــيـــف الـــشـــات")
                                                .addOption(OptionType.INTEGER, "amount", "عـــدد الـــرســـائـــل", true)
                                                .setDefaultPermissions(adminPerms),
                                Commands.slash("move", "ســـحـــب الـــعـــضـــو إلـــى روم صـــوتـــي")
                                                .addOption(OptionType.USER, "user", "الـــعـــضـــو الـــمـــســـتـــهـــدف",
                                                                true)
                                                .addOption(OptionType.CHANNEL, "channel", "الـــوجـــهـــة", true)
                                                .setDefaultPermissions(adminPerms),
                                Commands.slash("role", "إدارة رتـــــب الـــعـــضــــو")
                                                .addOption(OptionType.USER, "user", "الـــعـــضـــو الـــمـــســـتـــهـــدف",
                                                                true)
                                                .addOption(OptionType.ROLE, "role",
                                                                "الـــرتـــبـــة الـــمـــســـتـــهـــدفـــة", true)
                                                .setDefaultPermissions(adminPerms),
                                Commands.slash("temprole", "إعـــــطــــاء رتـــبـــة مـــؤقـــتــــة")
                                                .addOption(OptionType.USER, "user", "الـــعـــضـــو الـــمـــســـتـــهـــدف",
                                                                true)
                                                .addOption(OptionType.ROLE, "role",
                                                                "الـــرتـــبـــة الـــمـــســـتـــهـــدفـــة", true)
                                                .addOption(OptionType.INTEGER, "duration", "الـــمـــدة بـــالـــســـاعـــات",
                                                                true)
                                                .setDefaultPermissions(adminPerms),
                                Commands.slash("rar", "ســـحـــب جـــمـــيـــع الـــرتـــب")
                                                .addOption(OptionType.USER, "user", "الـــعـــضـــو الـــمـــســـتـــهـــدف",
                                                                true)
                                                .setDefaultPermissions(adminPerms),
                                Commands.slash("inrole", "عـــعرض الأعـــضـــاء فـــي رتـــبـــة مـــعـــيـــنـــة")
                                                .addOption(OptionType.ROLE, "role",
                                                                "الـــرتـــبـــة الـــمـــســـتـــهـــدفـــة", true)
                                .setDefaultPermissions(adminPerms),
                                Commands.slash("warn-add", "إعـــــطــــاء تـــحـــذيـــر لـــلـــعـــضـــو")
                                                .addOption(OptionType.USER, "user", "الـــعـــضـــو الـــمـــســـتـــهـــدف",
                                                                true)
                                                .addOption(OptionType.STRING, "reason", "الـــســـبـــب", false)
                                                .setDefaultPermissions(adminPerms),
                                Commands.slash("warn-remove", "حــــذف تـــحـــذيـــر مـــن الـــعـــضـــو")
                                                .addOption(OptionType.USER, "user", "الـــعـــضـــو الـــمـــســـتـــهـــدف",
                                                                true)
                                                .addOption(OptionType.INTEGER, "id", "إي دي الـــتـــحـــذيـــر", false)
                                                .setDefaultPermissions(adminPerms),
                                Commands.slash("warnings", "عـــعرض تـــحـــذيـــرات الـــعـــضـــو")
                                                .addOption(OptionType.USER, "user", "الـــعـــضـــو الـــمـــســـتـــهـــدف",
                                                                true)
                                                .setDefaultPermissions(adminPerms),
                                Commands.slash("violations", "عـــعرض مـــخـــالـــفـــات الـــتـــصـــفـــيـــة")
                                                .addOption(OptionType.USER, "user", "الـــعـــضـــو الـــمـــســـتـــهـــدف",
                                                                true)
                                                .setDefaultPermissions(adminPerms),
                                Commands.slash("violations-clear", "مـــســـح مـــخـــالـــفـــات الـــتـــصـــفـــيـــة")
                                                .addOption(OptionType.USER, "user", "الـــعـــضـــو الـــمـــســـتـــهـــدف",
                                                                true)
                                                .setDefaultPermissions(adminPerms),
                                Commands.slash("lock", "قـــفـــل الـــروم الـــحـــالـــي")
                                                .setDefaultPermissions(adminPerms),
                                Commands.slash("unlock", "فــــتـــح الـــروم الـــحـــالـــي")
                                                .setDefaultPermissions(adminPerms),
                                Commands.slash("hide", "إخـــفـــاء الـــروم الـــحـــالـــي")
                                                .setDefaultPermissions(adminPerms),
                                Commands.slash("show", "إظـــهـــار الـــروم الـــحـــالـــي")
                                                .setDefaultPermissions(adminPerms),
                                Commands.slash("slowmode", "تـــفـــعـــيـــل الـــوضـــع الـــبـــطـــيء")
                                                .addOption(OptionType.INTEGER, "seconds", "الـــثـــوانـــي", true)
                                                .setDefaultPermissions(adminPerms),
                                Commands.slash("add-emoji", "إضـــافـــة إيـــمـــوجـــي جـــديـــد")
                                                .addOption(OptionType.ATTACHMENT, "image", "الـــصـــورة", true)
                                                .addOption(OptionType.STRING, "name", "اســـم الإيـــمـــوجـــي", true)
                                                .setDefaultPermissions(adminPerms),
                                Commands.slash("role-multiple", "إدارة الـــرتـــب لـــلـــجـــمـــيـــع")
                                                .addOption(OptionType.ROLE, "role",
                                                                "الـــرتـــبـــة الـــمـــســـتـــهـــدفـــة", true)
                                                .addOption(OptionType.STRING, "action", "الإجـــراء", true)
                                                .setDefaultPermissions(adminPerms),

                                // ===== Giveaways (Administrative) =====
                                Commands.slash("giveaway", "فـــتـــح بـــنـــل الـــقـــيـــف اواي")
                                                .setDefaultPermissions(adminPerms),
                                Commands.slash("replay", "إدارة نــــظــــام الـــرد الـــتـــلـــقـــائـــي")
                                                .setDefaultPermissions(adminPerms),
                                Commands.slash("banned-words", "إدارة نــــظــــام الـــكـــلـــمـــات الـــمـــمـــنـــوعـــة")
                                                .setDefaultPermissions(adminPerms),

                                // ===== Emulation (Administrative) =====
                                Commands.slash("boter", "إرســـــال رســـــالـــة عـــن طـــريـــق الـــبـــوت")
                                                .addOption(OptionType.CHANNEL, "channel",
                                                                "الـــروم الـــمـــســـتـــهـــدف", false)
                                                .addOption(OptionType.ATTACHMENT, "file1", "الـــمـــرفـــق الأول", false)
                                                .addOption(OptionType.ATTACHMENT, "file2", "الـــمـــرفـــق الـــثـــانـــي",
                                                                false)
                                                .addOption(OptionType.ATTACHMENT, "file3", "الـــمـــرفـــق الـــثـــالـــث",
                                                                false)
                                                .setDefaultPermissions(adminPerms),

                                // ===== Information (Administrative & Public) =====
                                Commands.slash("roles", "قـــائـــمـــة رتـــب الـــســـيـــرفـــر")
                                                .setDefaultPermissions(adminPerms), // Restricted per user request

                                // ===== Vouchers (Administrative) =====
                                Commands.slash("voucher", "إدارة وإنـــشـــاء الـــقـــســـائـــم")
                                                .addSubcommands(
                                                                new SubcommandData("create", "إنـــشـــاء قـــســـيـــمـــة جـــديـــدة")
                                                                                .addOption(OptionType.USER, "user", "الـــعـــضـــو", true)
                                                                                .addOptions(new OptionData(OptionType.STRING, "type", "الـــنـــوع", true)
                                                                                        .addChoice("Discount", "DISCOUNT")
                                                                                        .addChoice("Voucher", "VOUCHER"))
                                                                                .addOptions(new OptionData(OptionType.INTEGER, "amount", "الـــقـــيـــمـــة", true)
                                                                                        .addChoice("10", 10)
                                                                                        .addChoice("20", 20)
                                                                                        .addChoice("30", 30)
                                                                                        .addChoice("40", 40)
                                                                                        .addChoice("50", 50)
                                                                                        .addChoice("60", 60)
                                                                                        .addChoice("100", 100))
                                                                                .addOption(OptionType.INTEGER, "expiry", "مـــدة الـــصـــلاحـــيـــة بـــالأيـــام", true),
                                                                new SubcommandData("view", "عـــعرض الـــقـــســـائـــم الـــمـــفـــعـــلـــة")
                                                                                .addOption(OptionType.USER, "user", "الـــعـــضـــو", false)
                                                )
                                                .setDefaultPermissions(net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions.enabledFor(net.dv8tion.jda.api.Permission.MESSAGE_MANAGE)),

                                // ===== Information (Public) =====
                                Commands.slash("ping", "فـــحـــص ســـرعـــة اتـــصـــال الـــبـــوت"),
                                Commands.slash("profile", "عـــعرض مـــلـــف الـــعـــضـــو")
                                                .addOption(OptionType.USER, "user", "الـــعـــضـــو", false),
                                Commands.slash("avatar", "عـــعرض صـــورة الـــعـــضـــو")
                                                .addOption(OptionType.USER, "user", "الـــعـــضـــو", false),
                                Commands.slash("server", "عـــعرض مـــعـــلـــومـــات الـــســـيـــرفـــر"),
                                Commands.slash("banner", "عـــعرض بـــانـــر الـــعـــضـــو")
                                                .addOption(OptionType.USER, "user", "الـــعـــضـــو", false),
                                Commands.slash("invites", "عـــعرض إحـــصـــائـــيـــات الـــدعـــوات")
                                                .addOption(OptionType.USER, "user", "الـــعـــضـــو", false),
                                Commands.slash("server-avatar", "عـــعرض صـــورة الـــســـيـــرفـــر"),
                                Commands.slash("server-banner", "عـــعرض بـــانـــر الـــســـيـــرفـــر"),

                                // ===== General & Fun (Public) =====
                                Commands.slash("translate", "تـــرجـــمـــة الـــنـــصـــوص")
                                                .addOption(OptionType.STRING, "text", "الـــمـــحـــتـــوى", true)
                                                .addOption(OptionType.STRING, "language", "الـــلـــغـــة", true),
                                Commands.slash("roll", "رمـــي الـــنـــرد"),
                                Commands.slash("order", "بـــدء طـــلـــب مـــشـــروع جـــديـــد"),
                                Commands.slash("terms", "عـــعرض شـــروط وقـــوانـــيـــن الـــوكـــالـــة"),
                                Commands.slash("prices", "عـــعرض أســـعـــار خـــدمـــات الـــوكـــالـــة"),
                                Commands.slash("line", "إرســـــال الـــخـــط الـــفـــاصـــل")).queue();
        }
}
