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
                // ===== Core & Business =====
                Commands.slash("startup", "Show the main dashboard"),
                Commands.slash("tickets", "Open the ticket system"),
                Commands.slash("services", "List our services and categories"),
                Commands.slash("stats", "Show agency and system statistics"),
                Commands.slash("help", "Display the help manual and commands"),
                Commands.slash("ping", "Check the bot connectivity status"),
                Commands.slash("bc", "Broadcast a message").addOption(OptionType.STRING, "message", "The message content", true).addOption(OptionType.ROLE, "role", "Target role", false).addOption(OptionType.ATTACHMENT, "attachment", "Media attachment", false),

                // ===== Moderation =====
                Commands.slash("setnick", "Change a member nickname").addOption(OptionType.USER, "user", "Target member", true).addOption(OptionType.STRING, "nick", "New nickname", true),
                Commands.slash("ban", "\u062D\u0638\u0631 \u0639\u0636\u0648 \u0645\u0646 \u0627\u0644\u0633\u0641\u0631").addOption(OptionType.USER, "user", "\u0627\u0644\u0639\u0636\u0648", true).addOption(OptionType.STRING, "reason", "\u0627\u0644\u0633\u0628\u0628", false),
                Commands.slash("unban", "Unban a member").addOption(OptionType.STRING, "user_id", "Member identifier ID", true),
                Commands.slash("unban-all", "Clear the entire ban list"),
                Commands.slash("kick", "Kick a member from the server").addOption(OptionType.USER, "user", "Target member", true).addOption(OptionType.STRING, "reason", "Reason", false),
                Commands.slash("vkick", "Kick a member from a voice channel").addOption(OptionType.USER, "user", "Target member", true),
                Commands.slash("mute-text", "Mute a member from text channels").addOption(OptionType.USER, "user", "Target member", true),
                Commands.slash("unmute-text", "Unmute a member from text channels").addOption(OptionType.USER, "user", "Target member", true),
                Commands.slash("mute-check", "Check the mute status of a member").addOption(OptionType.USER, "user", "Target member", true),
                Commands.slash("mute-voice", "Mute a member from voice channels").addOption(OptionType.USER, "user", "Target member", true),
                Commands.slash("unmute-voice", "Unmute a member from voice channels").addOption(OptionType.USER, "user", "Target member", true),
                Commands.slash("timeout", "Timeout a member").addOption(OptionType.USER, "user", "Target member", true).addOption(OptionType.INTEGER, "duration", "Duration in minutes", true),
                Commands.slash("untimeout", "Remove a member timeout").addOption(OptionType.USER, "user", "Target member", true),
                Commands.slash("clear", "Delete messages from the channel").addOption(OptionType.INTEGER, "amount", "Number of messages", true),
                Commands.slash("move", "Move a member to a voice channel").addOption(OptionType.USER, "user", "Target member", true).addOption(OptionType.CHANNEL, "channel", "Destination channel", true),
                Commands.slash("role", "Add or remove a role from a member").addOption(OptionType.USER, "user", "Target member", true).addOption(OptionType.ROLE, "role", "Target role", true),
                Commands.slash("temprole", "Give a temporary role to a member").addOption(OptionType.USER, "user", "Target member", true).addOption(OptionType.ROLE, "role", "Target role", true).addOption(OptionType.INTEGER, "duration", "Duration in hours", true),
                Commands.slash("rar", "Strip all roles from a member").addOption(OptionType.USER, "user", "Target member", true),
                Commands.slash("inrole", "Display members with a specific role").addOption(OptionType.ROLE, "role", "Target role", true),
                Commands.slash("warn-add", "Add a warning to a member").addOption(OptionType.USER, "user", "Target member", true).addOption(OptionType.STRING, "reason", "Reason", false),
                Commands.slash("warn-remove", "Remove warnings from a member").addOption(OptionType.USER, "user", "Target member", true).addOption(OptionType.INTEGER, "id", "Warning ID", false),
                Commands.slash("warnings", "View member warnings").addOption(OptionType.USER, "user", "Target member", true),
                Commands.slash("violations", "View filter violations").addOption(OptionType.USER, "user", "Target member", true),
                Commands.slash("violations-clear", "Clear filter violations").addOption(OptionType.USER, "user", "Target member", true),
                Commands.slash("lock", "Lock the current channel"),
                Commands.slash("unlock", "Unlock the current channel"),
                Commands.slash("hide", "Hide the current channel"),
                Commands.slash("show", "Show the current channel"),
                Commands.slash("slowmode", "Enable channel slowmode").addOption(OptionType.INTEGER, "seconds", "Seconds", true),
                Commands.slash("add-emoji", "\u0625\u0636\u0627\u0641\u0629 \u0625\u064A\u0645\u0648\u062C\u064A \u0644\u0644\u0633\u064A\u0631\u0641\u0631").addOption(OptionType.ATTACHMENT, "image", "\u0627\u0644\u0635\u0648\u0631\u0629", true).addOption(OptionType.STRING, "name", "\u0627\u0633\u0645 \u0627\u0644\u0625\u064A\u0645\u0648\u062C\u064A", true),
                Commands.slash("role-multiple", "Bulk role management").addOption(OptionType.ROLE, "role", "Target role", true).addOption(OptionType.STRING, "action", "Action (Add/Remove)", true),

                // ===== Giveaways =====
                Commands.slash("giveaway-start", "Start a new giveaway"),
                Commands.slash("giveaway-end", "End an active giveaway"),
                Commands.slash("giveaway-reroll", "Pick a new winner"),
                Commands.slash("giveaway-list", "List active giveaways"),
                Commands.slash("giveaway-drop", "Create a quick drop giveaway"),

                // ===== Information =====
                Commands.slash("profile", "View member profile").addOption(OptionType.USER, "user", "Target member", false),
                Commands.slash("user", "View member information").addOption(OptionType.USER, "user", "Target member", false),
                Commands.slash("avatar", "\u0639\u0631\u0636 \u0635\u0648\u0631\u0629 \u0627\u0644\u062D\u0633\u0627\u0628").addOption(OptionType.USER, "user", "\u0627\u0644\u0639\u0636\u0648", false),
                Commands.slash("server", "View server information"),
                Commands.slash("roles", "List server roles"),
                Commands.slash("banner", "View member banner").addOption(OptionType.USER, "user", "Target member", false),
                Commands.slash("invites", "View your invite statistics").addOption(OptionType.USER, "user", "Target member", false),
                Commands.slash("server-avatar", "View server icon"),
                Commands.slash("server-banner", "View server banner"),

                // ===== General & Fun =====
                Commands.slash("colors", "View available color roles"),
                Commands.slash("color-set", "Apply a specific color role").addOption(OptionType.STRING, "code", "Color code", true),
                Commands.slash("rep", "Give a reputation point to a member").addOption(OptionType.USER, "user", "Target member", true),
                Commands.slash("translate", "Translate text").addOption(OptionType.STRING, "text", "The content", true).addOption(OptionType.STRING, "language", "Target language", true),
                Commands.slash("roll", "Roll a dice"),
                Commands.slash("get-emojis", "Export an emoji").addOption(OptionType.STRING, "emoji", "The emoji", true),
                Commands.slash("title", "Change your personal title").addOption(OptionType.STRING, "title", "New title", true),
                Commands.slash("suggest", "Submit a new suggestion").addOption(OptionType.STRING, "suggestion", "The content", true),
                Commands.slash("suggestion", "Manage suggestions").addOption(OptionType.INTEGER, "id", "Suggestion ID", true).addOption(OptionType.STRING, "action", "Action (Approve/Decline)", true),

                // ===== Merit System =====
                Commands.slash("leaderboard", "View the Merit Leaderboard")
        ).queue();
    }
}
