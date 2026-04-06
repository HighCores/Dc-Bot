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
        log.info("Initializing Highcore Bot...");
        if (Config.DISCORD_TOKEN == null || Config.DISCORD_TOKEN.isEmpty()) { log.error("DISCORD_TOKEN not set!"); System.exit(1); }

        JDA jda = JDABuilder.createDefault(Config.DISCORD_TOKEN)
                .setStatus(OnlineStatus.ONLINE)
                .setActivity(Activity.watching("Highcore Agency"))
                .enableIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_MEMBERS, GatewayIntent.MESSAGE_CONTENT,
                        GatewayIntent.GUILD_MESSAGE_REACTIONS, GatewayIntent.GUILD_MODERATION, GatewayIntent.GUILD_VOICE_STATES)
                .setMemberCachePolicy(MemberCachePolicy.ALL)
                .addEventListeners(
                        new MessageListener(), 
                        new SlashCommands(),
                        new ModerationCommands(), 
                        new GiveawayCommands(), 
                        new PointsCommands(), 
                        new GeneralCommands(),
                        new ServerLogListener(), 
                        new WelcomeListener(), 
                        new CentralInteractionListener(),
                        new OrderCommands()
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
        log.info("Highcore Agency Bot fully ready!");
    }

    private static void registerCommands(JDA jda) {
        Guild guild = jda.getGuildById(Config.GUILD_ID);
        if (guild == null) return;

        guild.updateCommands().addCommands(
                // ===== CORE AGENCY TOOLS =====
                Commands.slash("menu", "Control panel \u2014 all panels & features here")
                        .setDefaultPermissions(net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions.enabledFor(net.dv8tion.jda.api.Permission.ADMINISTRATOR)),
                Commands.slash("startup", "Send startup guide & welcome panel (Admin)")
                        .setDefaultPermissions(net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions.enabledFor(net.dv8tion.jda.api.Permission.ADMINISTRATOR)),
                Commands.slash("ticket", "Open a new support ticket"),
                Commands.slash("tickets", "Send ticket control panel (Admin)")
                        .setDefaultPermissions(net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions.enabledFor(net.dv8tion.jda.api.Permission.ADMINISTRATOR)),
                Commands.slash("giveaway", "Giveaway management panel (Admin)")
                        .setDefaultPermissions(net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions.enabledFor(net.dv8tion.jda.api.Permission.ADMINISTRATOR)),
                Commands.slash("services", "Send services & prices panel (Admin)")
                        .setDefaultPermissions(net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions.enabledFor(net.dv8tion.jda.api.Permission.ADMINISTRATOR)),
                Commands.slash("stats", "Send agency operational stats (Admin)")
                        .setDefaultPermissions(net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions.enabledFor(net.dv8tion.jda.api.Permission.ADMINISTRATOR)),
                OrderCommands.getCommandData(),
                
                // ===== BROADCAST & MESSAGING =====
                Commands.slash("bc", "Send a broadcast message to members (Admin)")
                        .addOption(OptionType.ROLE, "role", "Target specific role", false)
                        .addOption(OptionType.ATTACHMENT, "attachment", "Attach media", false),
                Commands.slash("boter", "Send message on behalf of the terminal (Admin)")
                        .addOption(OptionType.CHANNEL, "channel", "Target node", false)
                        .addOption(OptionType.ATTACHMENT, "file1", "Payload A", false),

                // ===== MODERATION (MANUAL OVERRIDE) =====
                Commands.slash("ban", "Revoke member access (Admin)").addOption(OptionType.USER,"member","Subject",true).addOption(OptionType.STRING,"reason","Log reason",false),
                Commands.slash("unban", "Restore member access (Admin)").addOption(OptionType.STRING,"user_id","Subject ID",true),
                Commands.slash("kick", "Eject member from sector (Admin)").addOption(OptionType.USER,"member","Subject",true).addOption(OptionType.STRING,"reason","Log reason",false),
                Commands.slash("mute", "Silence member transmission (Admin)").addOption(OptionType.USER,"member","Subject",true).addOption(OptionType.INTEGER,"duration","Minutes",false),
                Commands.slash("unmute", "Restore member transmission (Admin)").addOption(OptionType.USER,"member","Subject",true),
                Commands.slash("clear", "Purge local data blocks (Admin)").addOption(OptionType.INTEGER,"amount","Purge density (1-100)",true),
                Commands.slash("lock", "Secure transmission node (Admin)").addOption(OptionType.CHANNEL,"channel","Target node",false),
                Commands.slash("unlock", "Enable transmission node (Admin)").addOption(OptionType.CHANNEL,"channel","Target node",false),

                // ===== ADMINISTRATIVE =====
                Commands.slash("points", "View merit points (Admin)")
                        .setDefaultPermissions(net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions.enabledFor(net.dv8tion.jda.api.Permission.ADMINISTRATOR)),
                Commands.slash("points-manage", "Manage merit points (Admin)").addSubcommands(
                        new SubcommandData("add","Add merit").addOption(OptionType.USER,"member","Subject",true).addOption(OptionType.INTEGER,"amount","Value",true),
                        new SubcommandData("remove","Remove merit").addOption(OptionType.USER,"member","Subject",true).addOption(OptionType.INTEGER,"amount","Value",true),
                        new SubcommandData("check","Audit merit").addOption(OptionType.USER,"member","Subject",false)),
                Commands.slash("points-leaderboard", "Merit ranking"),
                
                // ===== UTILITY =====
                Commands.slash("ping", "Check terminal latency (Admin)"),
                Commands.slash("profile", "View subject identity").addOption(OptionType.USER,"member","Subject",false),
                Commands.slash("server", "Display sector information"),
                Commands.slash("autoreply", "Manage auto-responses (Admin)")
                        .addSubcommands(
                                new SubcommandData("add", "Add response").addOption(OptionType.STRING, "keyword", "Trigger", true).addOption(OptionType.STRING, "response", "Output", true),
                                new SubcommandData("remove", "Remove response").addOption(OptionType.STRING, "keyword", "Trigger", true),
                                new SubcommandData("list", "List responses"))
        ).queue(c -> log.info("Synchronized {} protocol commands", c.size()), e -> log.error("Command sync failed: {}", e.getMessage()));

    }
}
