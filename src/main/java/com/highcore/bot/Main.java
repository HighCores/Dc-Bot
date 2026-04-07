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
        System.out.println("\u001B[36m" +
                "  _    _  _____  _____  _    _  _____  ____   _____  ______ \n" +
                " | |  | ||_   _||  ___|| |  | ||  ___||  _ \\ |  ___||  ____|\n" +
                " | |__| |  | |  | | __ | |__| || |___ | |_) || |___ | |__   \n" +
                " |  __  |  | |  | | \u2514\u2510| |  __  ||  ___||  _  /|  ___||  __|  \n" +
                " | |  | | _| |_ | |__| || |  | || |___ | | \\ \\| |___ | |____ \n" +
                " |_|  |_||_____| \\____/ |_|  |_||_____||_|  \\_\\_____||______|\n" +
                "            HIGHCORE AGENCY - UNIFIED TERMINAL v2.5\n" +
                "\u001B[0m");

        log.info("[\u2699\uFE0F] Initializing Highcore Protocol v1.0.4...");
        if (Config.DISCORD_TOKEN == null || Config.DISCORD_TOKEN.isEmpty()) { 
            log.error("[\u274C] CRITICAL: DISCORD_TOKEN is absent from configuration."); 
            System.exit(1); 
        }

        JDA jda = JDABuilder.createDefault(Config.DISCORD_TOKEN)
                .setStatus(OnlineStatus.ONLINE)
                .setActivity(Activity.watching("Highcore Agency"))
                .enableIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_MEMBERS, GatewayIntent.MESSAGE_CONTENT,
                        GatewayIntent.GUILD_MESSAGE_REACTIONS, GatewayIntent.GUILD_MODERATION, GatewayIntent.GUILD_VOICE_STATES)
                .setMemberCachePolicy(MemberCachePolicy.ALL)
                .addEventListeners(
                        new MessageListener(), 
                        new SlashCommands(),
                        new GiveawayCommands(), 
                        new PointsCommands(), 
                        new ServerLogListener(), 
                        new WelcomeListener(), 
                        new CentralInteractionListener()
                )
                .build().awaitReady();

        log.info("Bot online: {}", jda.getSelfUser().getName());

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

        Runtime.getRuntime().addShutdownHook(new Thread(() -> { 
            RestApiServer.stop(); 
            TelemetryService.stop(); 
            jda.shutdown(); 
        }));
        log.info("Highcore Agency Bot (Terminal v2.5) fully operational!");
    }

    private static void registerCommands(JDA jda) {
        jda.updateCommands().queue();
        Guild guild = jda.getGuildById(Config.GUILD_ID);
        if (guild == null) return;

        guild.updateCommands().addCommands(
                // ===== CORE HUB & OPERATIONS =====
                Commands.slash("startup", "Initialize agency guidance & hub panels (Admin)"),
                Commands.slash("tickets", "Initialize support logistics ticket module"),
                Commands.slash("services", "Open agency capability directory"),
                Commands.slash("giveaway", "Sweepstakes configuration module (Admin)"),
                Commands.slash("stats", "Agency health analytics"),
                Commands.slash("bc", "Initialize broadcast sequence (Admin)")
                        .addOption(OptionType.ROLE, "role", "Target role", false)
                        .addOption(OptionType.ATTACHMENT, "attachment", "Media payload", false),
                
                // ===== MERIT SYSTEM (ADMIN POINTS) =====
                Commands.slash("points", "Identity merit standing").addSubcommands(
                        new SubcommandData("check", "Audit operative merit").addOption(OptionType.USER, "member", "Subject", false),
                        new SubcommandData("add", "Allocate merit (Admin)").addOption(OptionType.USER, "member", "Subject", true).addOption(OptionType.INTEGER, "amount", "Value", true),
                        new SubcommandData("remove", "Deallocate merit (Admin)").addOption(OptionType.USER, "member", "Subject", true).addOption(OptionType.INTEGER, "amount", "Value", true),
                        new SubcommandData("reset", "Reset merit standing (Admin)").addOption(OptionType.USER, "member", "Subject", true)),
                Commands.slash("leaderboard", "Top operative rankings"),
                
                // ===== CONFIGURATION (ADMIN) =====
                Commands.slash("filter", "Configure forbidden word matrix (Admin)").addSubcommands(
                        new SubcommandData("add", "Add word").addOption(OptionType.STRING, "word", "Word", true),
                        new SubcommandData("remove", "Remove word").addOption(OptionType.STRING, "word", "Word", true),
                        new SubcommandData("list", "Display matrix")),
                Commands.slash("auto", "Configure keyword response matrix (Admin)").addSubcommands(
                        new SubcommandData("add", "Add keyword").addOption(OptionType.STRING, "keyword", "Key", true).addOption(OptionType.STRING, "response", "Value", true),
                        new SubcommandData("remove", "Remove keyword").addOption(OptionType.STRING, "keyword", "Key", true),
                        new SubcommandData("list", "Display list")),

                // ===== UTILITY & MAINTENANCE =====
                Commands.slash("clear", "Clear channel records (Admin)").addOption(OptionType.INTEGER, "amount", "Count", true),
                Commands.slash("ping", "Latency diagnostic")
        ).queue(c -> log.info("[\u2705] Synchronized {} verified protocol commands (Terminal v2.5)", c.size()));
    }
}
