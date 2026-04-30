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
                                Commands.slash("startup", "Display the main control panel")
                                                .setDefaultPermissions(adminPerms),
                                Commands.slash("tickets", "Open the ticket system")
                                                .setDefaultPermissions(adminPerms),
                                Commands.slash("bc", "Broadcast a mass message")
                                                .addOption(OptionType.STRING, "message", "Message content",
                                                                true)
                                                .addOption(OptionType.ROLE, "role",
                                                                "Target role", false)
                                                .addOption(OptionType.ATTACHMENT, "attachment",
                                                                "Media attachment", false)
                                                .setDefaultPermissions(adminPerms),
                                Commands.slash("discounts", "Manage discounts and scheduling system")
                                                .setDefaultPermissions(adminPerms),

                                // ===== Moderation (Administrative) =====
                                Commands.slash("setnick", "Change a member's nickname")
                                                .addOption(OptionType.USER, "user", "Target member",
                                                                true)
                                                .addOption(OptionType.STRING, "nick", "New nickname", true)
                                                .setDefaultPermissions(adminPerms),
                                Commands.slash("ban", "Ban a member from the server")
                                                .addOption(OptionType.USER, "user", "The member", true)
                                                .addOption(OptionType.STRING, "reason", "The reason", false)
                                                .setDefaultPermissions(adminPerms),
                                Commands.slash("unban", "Unban a member")
                                                .addOption(OptionType.STRING, "user_id", "Member ID", true)
                                                .setDefaultPermissions(adminPerms),
                                Commands.slash("unban-all", "Clear the entire ban list")
                                                .setDefaultPermissions(adminPerms),
                                Commands.slash("kick", "Kick a member from the server")
                                                .addOption(OptionType.USER, "user", "Target member",
                                                                true)
                                                .addOption(OptionType.STRING, "reason", "The reason", false)
                                                .setDefaultPermissions(adminPerms),
                                Commands.slash("vkick", "Kick a member from the voice channel")
                                                .addOption(OptionType.USER, "user", "Target member",
                                                                true)
                                                .setDefaultPermissions(adminPerms),
                                Commands.slash("mute-text", "Mute a member from chat")
                                                .addOption(OptionType.USER, "user", "Target member",
                                                                true)
                                                .setDefaultPermissions(adminPerms),
                                Commands.slash("unmute-text", "Unmute a member from chat")
                                                .addOption(OptionType.USER, "user", "Target member",
                                                                true)
                                                .setDefaultPermissions(adminPerms),
                                Commands.slash("mute-check", "Check a member's mute status")
                                                .addOption(OptionType.USER, "user", "Target member",
                                                                true)
                                                .setDefaultPermissions(adminPerms),
                                Commands.slash("mute-voice", "Mute a member from voice")
                                                .addOption(OptionType.USER, "user", "Target member",
                                                                true)
                                                .setDefaultPermissions(adminPerms),
                                Commands.slash("unmute-voice", "Unmute a member from voice")
                                                .addOption(OptionType.USER, "user", "Target member",
                                                                true)
                                                .setDefaultPermissions(adminPerms),
                                Commands.slash("timeout", "Timeout a member for a duration")
                                                .addOption(OptionType.USER, "user", "Target member",
                                                                true)
                                                .addOption(OptionType.INTEGER, "duration", "Duration in minutes",
                                                                true)
                                                .setDefaultPermissions(adminPerms),
                                Commands.slash("untimeout", "Remove member timeout")
                                                .addOption(OptionType.USER, "user", "Target member",
                                                                true)
                                                .setDefaultPermissions(adminPerms),
                                Commands.slash("clear", "Clear channel messages")
                                                .addOption(OptionType.INTEGER, "amount", "Number of messages", true)
                                                .setDefaultPermissions(adminPerms),
                                Commands.slash("move", "Move a member to a voice channel")
                                                .addOption(OptionType.USER, "user", "Target member",
                                                                true)
                                                .addOption(OptionType.CHANNEL, "channel", "The destination", true)
                                                .setDefaultPermissions(adminPerms),
                                Commands.slash("role", "Manage member roles")
                                                .addOption(OptionType.USER, "user", "Target member",
                                                                true)
                                                .addOption(OptionType.ROLE, "role",
                                                                "Target role", true)
                                                .setDefaultPermissions(adminPerms),
                                Commands.slash("temprole", "Give a temporary role")
                                                .addOption(OptionType.USER, "user", "Target member",
                                                                true)
                                                .addOption(OptionType.ROLE, "role",
                                                                "Target role", true)
                                                .addOption(OptionType.INTEGER, "duration", "Duration in hours",
                                                                true)
                                                .setDefaultPermissions(adminPerms),
                                Commands.slash("rar", "Remove all roles from a member")
                                                .addOption(OptionType.USER, "user", "Target member",
                                                                true)
                                                .setDefaultPermissions(adminPerms),
                                Commands.slash("inrole", "Display members in a role")
                                                .addOption(OptionType.ROLE, "role",
                                                                "Target role", true)
                                .setDefaultPermissions(adminPerms),
                                Commands.slash("warn-add", "Add a warning to a member")
                                                .addOption(OptionType.USER, "user", "Target member",
                                                                true)
                                                .addOption(OptionType.STRING, "reason", "The reason", false)
                                                .setDefaultPermissions(adminPerms),
                                Commands.slash("warn-remove", "Remove a warning from a member")
                                                .addOption(OptionType.USER, "user", "Target member",
                                                                true)
                                                .addOption(OptionType.INTEGER, "id", "Warning ID", false)
                                                .setDefaultPermissions(adminPerms),
                                Commands.slash("warnings", "View member warnings")
                                                .addOption(OptionType.USER, "user", "Target member",
                                                                true)
                                                .setDefaultPermissions(adminPerms),
                                Commands.slash("violations", "View filter violations")
                                                .addOption(OptionType.USER, "user", "Target member",
                                                                true)
                                                .setDefaultPermissions(adminPerms),
                                Commands.slash("violations-clear", "Clear filter violations")
                                                .addOption(OptionType.USER, "user", "Target member",
                                                                true)
                                                .setDefaultPermissions(adminPerms),
                                Commands.slash("lock", "Lock the current channel")
                                                .setDefaultPermissions(adminPerms),
                                Commands.slash("unlock", "Unlock the current channel")
                                                .setDefaultPermissions(adminPerms),
                                Commands.slash("hide", "Hide the current channel")
                                                .setDefaultPermissions(adminPerms),
                                Commands.slash("show", "Show the current channel")
                                                .setDefaultPermissions(adminPerms),
                                Commands.slash("slowmode", "Enable slowmode")
                                                .addOption(OptionType.INTEGER, "seconds", "Seconds", true)
                                                .setDefaultPermissions(adminPerms),
                                Commands.slash("add-emoji", "Add a new emoji")
                                                .addOption(OptionType.ATTACHMENT, "image", "The image", true)
                                                .addOption(OptionType.STRING, "name", "Emoji name", true)
                                                .setDefaultPermissions(adminPerms),
                                Commands.slash("role-multiple", "Manage roles for everyone")
                                                .addOption(OptionType.ROLE, "role",
                                                                "Target role", true)
                                                .addOption(OptionType.STRING, "action", "The action", true)
                                                .setDefaultPermissions(adminPerms),

                                // ===== Giveaways (Administrative) =====
                                Commands.slash("giveaway", "Open giveaways control panel")
                                                .setDefaultPermissions(adminPerms),
                                Commands.slash("replay", "Manage auto-reply system")
                                                .setDefaultPermissions(adminPerms),
                                Commands.slash("banned-words", "Manage banned words system")
                                                .setDefaultPermissions(adminPerms),

                                // ===== Emulation (Administrative) =====
                                Commands.slash("boter", "Send a message as the bot")
                                                .addOption(OptionType.CHANNEL, "channel",
                                                                "Target channel", false)
                                                .addOption(OptionType.ATTACHMENT, "file1", "First attachment", false)
                                                .addOption(OptionType.ATTACHMENT, "file2", "Second attachment",
                                                                false)
                                                .addOption(OptionType.ATTACHMENT, "file3", "Third attachment",
                                                                false)
                                                .setDefaultPermissions(adminPerms),

                                // ===== Information (Administrative & Public) =====
                                Commands.slash("roles", "List of server roles")
                                                .setDefaultPermissions(adminPerms), // Restricted per user request

                                // ===== Vouchers (Administrative) =====
                                Commands.slash("voucher", "Manage and generate vouchers")
                                                .addSubcommands(
                                                                new SubcommandData("create", "Generate a new voucher")
                                                                                .addOption(OptionType.USER, "user", "Member", true)
                                                                                .addOptions(new OptionData(OptionType.STRING, "type", "Type", true)
                                                                                        .addChoice("Discount", "DISCOUNT")
                                                                                        .addChoice("Voucher", "VOUCHER"))
                                                                                .addOptions(new OptionData(OptionType.INTEGER, "amount", "Value", true)
                                                                                        .addChoice("10", 10)
                                                                                        .addChoice("20", 20)
                                                                                        .addChoice("30", 30)
                                                                                        .addChoice("40", 40)
                                                                                        .addChoice("50", 50)
                                                                                        .addChoice("60", 60)
                                                                                        .addChoice("100", 100))
                                                                                .addOption(OptionType.INTEGER, "expiry", "Validity period in days (e.g., 7)", true),
                                                                new SubcommandData("view", "View active vouchers")
                                                                                .addOption(OptionType.USER, "user", "Member", false)
                                                )
                                                .setDefaultPermissions(net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions.enabledFor(net.dv8tion.jda.api.Permission.MESSAGE_MANAGE)),

                                // ===== Information (Public) =====
                                Commands.slash("ping", "Check bot latency"),
                                Commands.slash("profile", "View member profile")
                                                .addOption(OptionType.USER, "user", "Member", false),
                                Commands.slash("avatar", "View member avatar")
                                                .addOption(OptionType.USER, "user", "Member", false),
                                Commands.slash("server", "View server information"),
                                Commands.slash("banner", "View member banner")
                                                .addOption(OptionType.USER, "user", "Member", false),
                                Commands.slash("invites", "View invitation statistics")
                                                .addOption(OptionType.USER, "user", "Member", false),
                                Commands.slash("server-avatar", "View server icon"),
                                Commands.slash("server-banner", "View server banner"),

                                // ===== General & Fun (Public) =====
                                Commands.slash("translate", "Translate text")
                                                .addOption(OptionType.STRING, "text", "The content", true)
                                                .addOption(OptionType.STRING, "language", "The language", true),
                                Commands.slash("roll", "Roll the dice"),
                                Commands.slash("order", "Start a new project order"),
                                Commands.slash("terms", "View agency terms and rules"),
                                Commands.slash("prices", "View agency services prices"),
                                Commands.slash("line", "Send the global line separator")).queue();
        }
}
