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
                "            HIGHCORE AGENCY - GOLDEN TERMINAL v1.2.0\n" +
                "\u001B[0m");

        if (Config.DISCORD_TOKEN == null || Config.DISCORD_TOKEN.isEmpty()) { 
            log.error("CRITICAL: Token missing."); 
            System.exit(1); 
        }

        JDA jda = JDABuilder.createDefault(Config.DISCORD_TOKEN)
                .setStatus(OnlineStatus.ONLINE)
                .setActivity(Activity.watching("Highcore Agency [v1.2.0]"))
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
                        new PointsCommands(),
                        new ServerLogListener(),
                        new UserLogListener(),
                        new WelcomeListener(), 
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
                // ===== المركز والأعمال =====
                Commands.slash("startup", "عرض لوحة التحكم الرئيسية"),
                Commands.slash("tickets", "فتح نظام التذاكر"),
                Commands.slash("services", "قائمة الخدمات والقطاعات"),
                Commands.slash("stats", "إحصائيات النظام والوكالة"),
                Commands.slash("help", "دليل المساعدة والأوامر"),
                Commands.slash("ping", "فحص سرعة استجابة البوت"),
                Commands.slash("bc", "بث رسالة - برودكاست").addOption(OptionType.STRING, "message", "نص الرسالة", true).addOption(OptionType.ROLE, "role", "الفئة المستهدفة", false).addOption(OptionType.ATTACHMENT, "attachment", "المرفقات", false),

                // ===== أوامر الإدارة والرقابة =====
                Commands.slash("setnick", "تغيير لقب عضو").addOption(OptionType.USER, "user", "العضو", true).addOption(OptionType.STRING, "nick", "اللقب الجديد", true),
                Commands.slash("ban", "حظر عضو من السيرفر").addOption(OptionType.USER, "user", "العضو", true).addOption(OptionType.STRING, "reason", "السبب", false),
                Commands.slash("unban", "فك الحظر عن عضو").addOption(OptionType.STRING, "user_id", "معرف العضو (ID)", true),
                Commands.slash("unban-all", "فك الحظر عن جميع المحظورين"),
                Commands.slash("kick", "طرد عضو من السيرفر").addOption(OptionType.USER, "user", "العضو", true).addOption(OptionType.STRING, "reason", "السبب", false),
                Commands.slash("vkick", "طرد عضو من القناة الصوتية").addOption(OptionType.USER, "user", "العضو", true),
                Commands.slash("mute-text", "كتم عضو كتابياً").addOption(OptionType.USER, "user", "العضو", true),
                Commands.slash("unmute-text", "فك الكتم الكتابي").addOption(OptionType.USER, "user", "العضو", true),
                Commands.slash("mute-check", "فحص حالة كتم العضو").addOption(OptionType.USER, "user", "العضو", true),
                Commands.slash("mute-voice", "كتم عضو صوتياً").addOption(OptionType.USER, "user", "العضو", true),
                Commands.slash("unmute-voice", "فك الكتم الصوتي").addOption(OptionType.USER, "user", "العضو", true),
                Commands.slash("timeout", "تقييد عضو مؤقتاً").addOption(OptionType.USER, "user", "العضو", true).addOption(OptionType.INTEGER, "duration", "المدة بالدقائق", true),
                Commands.slash("untimeout", "فك التقييد المؤقت").addOption(OptionType.USER, "user", "العضو", true),
                Commands.slash("clear", "مسح رسائل القناة").addOption(OptionType.INTEGER, "amount", "عدد الرسائل", true),
                Commands.slash("move", "نقل عضو لقناة صوتية").addOption(OptionType.USER, "user", "العضو", true).addOption(OptionType.CHANNEL, "channel", "القناة المستهدفة", true),
                Commands.slash("role", "إضافة أو إزالة رتبة").addOption(OptionType.USER, "user", "العضو", true).addOption(OptionType.ROLE, "role", "الرتبة", true),
                Commands.slash("temprole", "إعطاء رتبة مؤقتة").addOption(OptionType.USER, "user", "العضو", true).addOption(OptionType.ROLE, "role", "الرتبة", true).addOption(OptionType.INTEGER, "duration", "المدة بالساعات", true),
                Commands.slash("rar", "سحب جميع الرتب من عضو").addOption(OptionType.USER, "user", "العضو", true),
                Commands.slash("inrole", "عرض الأعضاء برتبة معينة").addOption(OptionType.ROLE, "role", "الرتبة", true),
                Commands.slash("warn-add", "إضافة تحذير لعضو").addOption(OptionType.USER, "user", "العضو", true).addOption(OptionType.STRING, "reason", "السبب", false),
                Commands.slash("warn-remove", "إزالة تحذيرات عضو").addOption(OptionType.USER, "user", "العضو", true).addOption(OptionType.INTEGER, "id", "معرف التحذير", false),
                Commands.slash("warnings", "عرض تحذيرات عضو").addOption(OptionType.USER, "user", "العضو", true),
                Commands.slash("violations", "عرض مخالفات الفلتر").addOption(OptionType.USER, "user", "العضو", true),
                Commands.slash("violations-clear", "مسح مخالفات الفلتر").addOption(OptionType.USER, "user", "العضو", true),
                Commands.slash("lock", "قفل القناة الحالية"),
                Commands.slash("unlock", "فتح القناة الحالية"),
                Commands.slash("hide", "إخفاء القناة عن الجميع"),
                Commands.slash("show", "إظهار القناة للجميع"),
                Commands.slash("slowmode", "تفعيل وضع التباطؤ").addOption(OptionType.INTEGER, "seconds", "الثواني", true),
                Commands.slash("add-emoji", "إضافة إيموجي للسيرفر").addOption(OptionType.STRING, "name", "الاسم", true).addOption(OptionType.STRING, "url", "رابط الصورة", true),
                Commands.slash("role-multiple", "إدارة رتب لمجموعة أعضاء").addOption(OptionType.ROLE, "role", "الرتبة", true).addOption(OptionType.STRING, "action", "إضافة/سحب", true),

                // ===== أوامر المسابقات =====
                Commands.slash("giveaway-start", "بدء مسابقة جديدة"),
                Commands.slash("giveaway-end", "إنهاء مسابقة نشطة"),
                Commands.slash("giveaway-reroll", "إعادة اختيار فائز"),
                Commands.slash("giveaway-list", "قائمة المسابقات النشطة"),
                Commands.slash("giveaway-drop", "إنشاء مسابقة سريعة (Drop)"),

                // ===== أوامر المعلومات والبيانات =====
                Commands.slash("profile", "عرض الملف الشخصي").addOption(OptionType.USER, "user", "العضو", false),
                Commands.slash("user", "عرض معلومات العضو").addOption(OptionType.USER, "user", "العضو", false),
                Commands.slash("avatar", "عرض صورة العضو").addOption(OptionType.USER, "user", "العضو", false),
                Commands.slash("server", "عرض معلومات السيرفر"),
                Commands.slash("roles", "قائمة رتب السيرفر"),
                Commands.slash("banner", "عرض بنر العضو").addOption(OptionType.USER, "user", "العضو", false),
                Commands.slash("invites", "عرض دعوتك للسيرفر").addOption(OptionType.USER, "user", "العضو", false),
                Commands.slash("server-avatar", "عرض أيقونة السيرفر"),
                Commands.slash("server-banner", "عرض بنر السيرفر"),

                // ===== الأوامر العامة والترفيه =====
                Commands.slash("colors", "عرض ألوان التميز المتاحة"),
                Commands.slash("color-set", "تعيين لونك الخاص").addOption(OptionType.STRING, "code", "رمز اللون", true),
                Commands.slash("rep", "إعطاء نقطة سمعة لعضو").addOption(OptionType.USER, "user", "العضو", true),
                Commands.slash("translate", "مترجم وكالة هايكور").addOption(OptionType.STRING, "text", "النص", true).addOption(OptionType.STRING, "language", "اللغة", true),
                Commands.slash("roll", "رمي حجر النرد"),
                Commands.slash("get-emojis", "سحب إيموجي لإضافته").addOption(OptionType.STRING, "emoji", "الإيموجي", true),
                Commands.slash("title", "تغيير لقبك الشخصي").addOption(OptionType.STRING, "title", "اللقب الجديد", true),
                Commands.slash("suggest", "تقديم اقتراح جديد").addOption(OptionType.STRING, "suggestion", "الاقتراح", true),
                Commands.slash("suggestion", "إدارة الاقتراحات").addOption(OptionType.INTEGER, "id", "رقم الاقتراح", true).addOption(OptionType.STRING, "action", "الموافقة/الرفض", true),

                // ===== نظام التميز (Merit) =====
                Commands.slash("leaderboard", "قائمة التميز (Merit Leaderboard)")
        ).queue();
    }
}
