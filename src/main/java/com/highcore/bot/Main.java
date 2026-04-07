// FORCE REBUILD TRIGGER: 2026-04-07-05-55
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
                "            HIGHCORE AGENCY - UNIFIED TERMINAL\n" +
                "\u001B[0m");

        log.info("[\u2699\uFE0F] Initializing Highcore Protocol v1.0.1...");
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
                        new GeneralCommands(),
                        new ServerLogListener(), 
                        new WelcomeListener(), 
                        new CentralInteractionListener(),
                        new OrderCommands(),
                        new ModerationCommands()
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
        log.info("Highcore Agency Bot fully operational!");
    }

    private static void registerCommands(JDA jda) {
        // Clear Global Commands (Forces Cache Refresh)
        jda.updateCommands().queue();

        Guild guild = jda.getGuildById(Config.GUILD_ID);
        if (guild == null) { log.error("[\u274C] Guild not detected. Deployment aborted."); return; }

        guild.updateCommands().addCommands(
                // ===== CORE SECTORS =====
                Commands.slash("menu", "Unified highcore control center"),
                Commands.slash("startup", "Initialize guidance & panels (Admin)"),
                Commands.slash("ticket", "Open service request session"),
                Commands.slash("ticket-panel", "Deploy ticket control node (Admin)"),
                Commands.slash("services", "Agency capability directory"),
                Commands.slash("stats", "Agency health analytics"),
                
                // ===== OPERATIONS =====
                Commands.slash("bc", "Initialize broadcast sequence (Admin)")
                        .addOption(OptionType.ROLE, "role", "Target role", false)
                        .addOption(OptionType.ATTACHMENT, "attachment", "Media payload", false),
                Commands.slash("boter", "Relay message via terminal (Admin)")
                        .addOption(OptionType.CHANNEL, "channel", "Output node", false),

                // ===== MERIT & SOCIAL =====
                Commands.slash("points", "Identity merit standing"),
                Commands.slash("points-manage", "Administrative merit override (Admin)").addSubcommands(
                        new SubcommandData("add","Allocate merit").addOption(OptionType.USER,"member","Subject",true).addOption(OptionType.INTEGER,"amount","Value",true),
                        new SubcommandData("remove","Deallocate merit").addOption(OptionType.USER,"member","Subject",true).addOption(OptionType.INTEGER,"amount","Value",true),
                        new SubcommandData("check","Audit subject merit").addOption(OptionType.USER,"member","Subject",false)),
                Commands.slash("points-leaderboard", "Top operative rankings"),
                
                // ===== MODERATION =====
                Commands.slash("clear", "Clear channel records (Admin)")
                        .addOption(OptionType.INTEGER, "amount", "Number of records", true),

                // ===== UTILITY =====
                Commands.slash("ping", "Latency diagnostic"),
                Commands.slash("profile", "Examine subject profile").addOption(OptionType.USER,"member","Subject",false)
        ).queue(c -> log.info("[\u2705] Synchronized {} verified protocol commands", c.size()));
    }
}
