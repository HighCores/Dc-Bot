package com.highcore.bot;

import com.highcore.bot.api.RestApiServer;
import com.highcore.bot.commands.SlashCommands;
import com.highcore.bot.commands.ModerationCommands;
import com.highcore.bot.commands.GiveawayCommands;
import com.highcore.bot.commands.PointsCommands;
import com.highcore.bot.commands.LevelCommands;
import com.highcore.bot.commands.GeneralCommands;
import com.highcore.bot.config.Config;
import com.highcore.bot.listeners.ButtonListener;
import com.highcore.bot.listeners.MessageListener;
import com.highcore.bot.listeners.ServerLogListener;
import com.highcore.bot.listeners.WelcomeListener;
import com.highcore.bot.listeners.GiveawayListener;
import com.highcore.bot.services.AutoReplyService;
import com.highcore.bot.services.LogManager;
import com.highcore.bot.services.TicketReminderService;
import com.highcore.bot.services.GiveawayService;
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
        log.info("Starting Highcore Agency Discord Bot...");
        if (Config.DISCORD_TOKEN == null || Config.DISCORD_TOKEN.isEmpty()) { log.error("DISCORD_TOKEN not set!"); System.exit(1); }

        JDA jda = JDABuilder.createDefault(Config.DISCORD_TOKEN)
                .setStatus(OnlineStatus.ONLINE)
                .setActivity(Activity.watching("Highcore Agency"))
                .enableIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_MEMBERS, GatewayIntent.MESSAGE_CONTENT,
                        GatewayIntent.GUILD_MESSAGE_REACTIONS, GatewayIntent.GUILD_MODERATION, GatewayIntent.GUILD_VOICE_STATES)
                .setMemberCachePolicy(MemberCachePolicy.ALL)
                .addEventListeners(
                        new ButtonListener(), new MessageListener(), new SlashCommands(),
                        new ModerationCommands(), new GiveawayCommands(), new PointsCommands(), new LevelCommands(),
                        new GeneralCommands(),
                        new ServerLogListener(), new WelcomeListener(), new GiveawayListener())
                .build().awaitReady();

        log.info("Bot online: {}", jda.getSelfUser().getName());

        Guild guild = jda.getGuildById(Config.GUILD_ID);
        if (guild != null) LogManager.initialize(guild);

        registerCommands(jda, Config.GUILD_ID);
        AutoReplyService.refreshCache();
        TicketReminderService.start(jda);
        GiveawayService.start(jda);
        RestApiServer.start(jda);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> { RestApiServer.stop(); jda.shutdown(); }));
        log.info("Highcore Agency Bot fully ready!");
    }

    public static int registerCommands(JDA jda, String guildId) {
        net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction updateAction;
        
        if (guildId != null && !guildId.isEmpty() && jda.getGuildById(guildId) != null) {
            updateAction = jda.getGuildById(guildId).updateCommands();
        } else {
            updateAction = jda.updateCommands();
        }

        java.util.List<net.dv8tion.jda.api.interactions.commands.build.CommandData> commandList = new java.util.ArrayList<>();

        // ===== 1. CORE STATIC COMMANDS =====
        commandList.addAll(java.util.Arrays.asList(
                // ===== CORE =====
                Commands.slash("menu", "Control panel — all panels & features here"),
                Commands.slash("ticket", "Open a new support ticket"),
                Commands.slash("help", "Show all bot commands"),
                Commands.slash("autoreply", "Manage auto-replies (Staff)")
                        .addSubcommands(
                                new SubcommandData("add", "Add auto-reply").addOption(OptionType.STRING, "keyword", "Keyword", true).addOption(OptionType.STRING, "response", "Response", true),
                                new SubcommandData("remove", "Remove auto-reply").addOption(OptionType.STRING, "keyword", "Keyword", true),
                                new SubcommandData("list", "List all auto-replies")),
                Commands.slash("panel", "Send custom panel (Admin)").addOption(OptionType.STRING, "title", "Title", true).addOption(OptionType.STRING, "description", "Description", true).addOption(OptionType.STRING, "color", "Color", false),
                Commands.slash("embed", "Create custom embed (Admin)")
                        .addOption(OptionType.STRING,"title","Title",false).addOption(OptionType.STRING,"description","Text (\\n for newline)",false)
                        .addOption(OptionType.STRING,"color","Color",false).addOption(OptionType.STRING,"image","Banner URL",false)
                        .addOption(OptionType.STRING,"thumbnail","Thumbnail URL",false).addOption(OptionType.STRING,"author_name","Author",false)
                        .addOption(OptionType.STRING,"author_icon","Author icon URL",false).addOption(OptionType.STRING,"footer_text","Footer",false)
                        .addOption(OptionType.STRING,"footer_icon","Footer icon URL",false)
                        .addOption(OptionType.STRING,"field1_name","Field 1 name",false).addOption(OptionType.STRING,"field1_value","Field 1 value",false).addOption(OptionType.BOOLEAN,"field1_inline","Field 1 inline?",false)
                        .addOption(OptionType.STRING,"field2_name","Field 2 name",false).addOption(OptionType.STRING,"field2_value","Field 2 value",false).addOption(OptionType.BOOLEAN,"field2_inline","Field 2 inline?",false)
                        .addOption(OptionType.STRING,"field3_name","Field 3 name",false).addOption(OptionType.STRING,"field3_value","Field 3 value",false).addOption(OptionType.BOOLEAN,"field3_inline","Field 3 inline?",false),
                Commands.slash("startup", "Display startup guide"),
                Commands.slash("sync", "Sync dynamic commands from dashboard (Admin only)"),
                Commands.slash("rename", "Rename a channel (Admin)").addOption(OptionType.STRING,"name","New name",true).addOption(OptionType.CHANNEL,"channel","Channel",false),
                Commands.slash("setchannel", "Set channel purpose (Admin)").addOption(OptionType.STRING,"purpose","Purpose",true).addOption(OptionType.CHANNEL,"channel","Channel",false),

                // ===== MODERATION =====
                Commands.slash("setnick", "Change nickname (Admin)").addOption(OptionType.USER,"member","Member",true).addOption(OptionType.STRING,"nickname","New nickname",false),
                Commands.slash("ban", "Ban a member (Admin)").addOption(OptionType.USER,"member","Member",true).addOption(OptionType.STRING,"reason","Reason",false).addOption(OptionType.INTEGER,"delete_days","Delete msg days",false),
                Commands.slash("unban", "Unban a member (Admin)").addOption(OptionType.STRING,"user_id","User ID",true),
                Commands.slash("unban-all", "Unban all members (Admin)"),
                Commands.slash("kick", "Kick a member (Admin)").addOption(OptionType.USER,"member","Member",true).addOption(OptionType.STRING,"reason","Reason",false),
                Commands.slash("vkick", "Voice kick (Admin)").addOption(OptionType.USER,"member","Member",true),
                Commands.slash("mute-text", "Text mute (Admin)").addOption(OptionType.USER,"member","Member",true).addOption(OptionType.STRING,"reason","Reason",false),
                Commands.slash("unmute-text", "Text unmute (Admin)").addOption(OptionType.USER,"member","Member",true),
                Commands.slash("mute-check", "Check mute status (Admin)").addOption(OptionType.USER,"member","Member",true),
                Commands.slash("mute-voice", "Voice mute (Admin)").addOption(OptionType.USER,"member","Member",true).addOption(OptionType.STRING,"reason","Reason",false),
                Commands.slash("unmute-voice", "Voice unmute (Admin)").addOption(OptionType.USER,"member","Member",true),
                Commands.slash("timeout", "Timeout a member (Admin)").addOption(OptionType.USER,"member","Member",true).addOption(OptionType.INTEGER,"duration","Minutes",true).addOption(OptionType.STRING,"reason","Reason",false),
                Commands.slash("untimeout", "Remove timeout (Admin)").addOption(OptionType.USER,"member","Member",true),
                Commands.slash("clear", "Delete messages (Admin)").addOption(OptionType.INTEGER,"amount","Amount (1-100)",true),
                Commands.slash("move", "Move to voice channel (Admin)").addOption(OptionType.USER,"member","Member",true).addOption(OptionType.CHANNEL,"channel","Voice channel",true),
                Commands.slash("role", "Manage roles (Admin)").addSubcommands(
                        new SubcommandData("add","Add role").addOption(OptionType.USER,"member","Member",true).addOption(OptionType.ROLE,"role","Role",true),
                        new SubcommandData("remove","Remove role").addOption(OptionType.USER,"member","Member",true).addOption(OptionType.ROLE,"role","Role",true)),
                Commands.slash("temprole", "Temporary role (Admin)").addOption(OptionType.USER,"member","Member",true).addOption(OptionType.ROLE,"role","Role",true).addOption(OptionType.INTEGER,"duration","Minutes",true),
                Commands.slash("rar", "Remove all roles (Admin)").addOption(OptionType.USER,"member","Member",true),
                Commands.slash("inrole", "List members with role (Admin)").addOption(OptionType.ROLE,"role","Role",true),
                Commands.slash("warn-add", "Warn a member (Admin)").addOption(OptionType.USER,"member","Member",true).addOption(OptionType.STRING,"reason","Reason",true),
                Commands.slash("warn-remove", "Remove warnings (Admin)").addOption(OptionType.USER,"member","Member",false),
                Commands.slash("warnings", "View warnings (Admin)").addOption(OptionType.USER,"member","Member",false),
                Commands.slash("violations", "View violations (Admin)").addOption(OptionType.USER,"member","Member",true),
                Commands.slash("violations-clear", "Clear violations (Admin)").addOption(OptionType.USER,"member","Member",true),
                Commands.slash("lock", "Lock channel (Admin)").addOption(OptionType.CHANNEL,"channel","Channel",false),
                Commands.slash("unlock", "Unlock channel (Admin)").addOption(OptionType.CHANNEL,"channel","Channel",false),
                Commands.slash("hide", "Hide channel (Admin)").addOption(OptionType.CHANNEL,"channel","Channel",false),
                Commands.slash("show", "Show channel (Admin)").addOption(OptionType.CHANNEL,"channel","Channel",false),
                Commands.slash("slowmode", "Set slowmode (Admin)").addOption(OptionType.INTEGER,"seconds","Seconds (0=off)",true).addOption(OptionType.CHANNEL,"channel","Channel",false),
                Commands.slash("add-emoji", "Add custom emoji (Admin)").addOption(OptionType.STRING,"emoji_name","Name",true).addOption(OptionType.STRING,"image_url","Image URL",true),
                Commands.slash("role-multiple", "Mass role manage (Admin)").addSubcommands(
                        new SubcommandData("add","Mass add").addOption(OptionType.ROLE,"role","Role",true),
                        new SubcommandData("remove","Mass remove").addOption(OptionType.ROLE,"role","Role",true)),

                // ===== GIVEAWAY =====
                Commands.slash("giveaway-start", "Start giveaway (Admin)").addOption(OptionType.STRING,"prize","Prize",true).addOption(OptionType.INTEGER,"duration","Minutes",true).addOption(OptionType.INTEGER,"winners","Winners",true).addOption(OptionType.CHANNEL,"channel","Channel",false),
                Commands.slash("giveaway-end", "End giveaway (Admin)").addOption(OptionType.STRING,"id","Giveaway ID",true),
                Commands.slash("giveaway-reroll", "Reroll winners (Admin)").addOption(OptionType.STRING,"id","Giveaway ID",true),
                Commands.slash("giveaway-drop", "Drop giveaway (Admin)").addOption(OptionType.STRING,"prize","Prize",true).addOption(OptionType.CHANNEL,"channel","Channel",false),

                // ===== POINTS =====
                Commands.slash("points", "Manage points").addSubcommands(
                        new SubcommandData("add","Add points (Admin)").addOption(OptionType.USER,"member","Member",true).addOption(OptionType.INTEGER,"amount","Points",true),
                        new SubcommandData("remove","Remove points (Admin)").addOption(OptionType.USER,"member","Member",true).addOption(OptionType.INTEGER,"amount","Points",true),
                        new SubcommandData("set","Set points (Admin)").addOption(OptionType.USER,"member","Member",true).addOption(OptionType.INTEGER,"amount","Points",true),
                        new SubcommandData("check","Check points").addOption(OptionType.USER,"member","Member",false)),
                Commands.slash("points-reset", "Reset points (Admin)").addSubcommands(
                        new SubcommandData("user","Reset member").addOption(OptionType.USER,"member","Member",true),
                        new SubcommandData("all","Reset all")),
                Commands.slash("points-leaderboard", "Points ranking"),

                // ===== LEVELING =====
                Commands.slash("level", "Check level").addOption(OptionType.USER,"member","Member",false),
                Commands.slash("rank", "View rank card").addOption(OptionType.USER,"member","Member",false),
                Commands.slash("top", "Leaderboard").addOption(OptionType.STRING,"type","text or voice",false),
                Commands.slash("setxp", "Set XP (High Admin)").addOption(OptionType.USER,"member","Member",true).addOption(OptionType.INTEGER,"amount","XP",true),
                Commands.slash("setlevel", "Set level (High Admin)").addOption(OptionType.USER,"member","Member",true).addOption(OptionType.INTEGER,"level","Level",true),
                Commands.slash("reset", "Reset XP (High Admin)").addSubcommands(
                        new SubcommandData("user","Reset member").addOption(OptionType.USER,"member","Member",true),
                        new SubcommandData("all","Reset everyone")),

                // ===== GENERAL =====
                Commands.slash("ping", "Bot latency (Admin)"),
                Commands.slash("colors", "View color roles"),
                Commands.slash("color-set", "Set your color").addOption(OptionType.INTEGER,"number","Color number",true),
                Commands.slash("rep", "Give reputation (Admin)").addOption(OptionType.USER,"member","Member",true),
                Commands.slash("vito", "View vitos").addOption(OptionType.USER,"member","Member",false),
                Commands.slash("translate", "Translate text").addOption(OptionType.STRING,"text","Text",true).addOption(OptionType.STRING,"language","Target language",true),
                Commands.slash("roll", "Roll a dice"),
                Commands.slash("get-emojis", "Server emojis").addOption(OptionType.STRING,"text","Text with emojis",false),
                Commands.slash("title", "Profile title").addSubcommands(
                        new SubcommandData("view","View title"),
                        new SubcommandData("set","Set title").addOption(OptionType.STRING,"text","Title",true)),
                Commands.slash("suggest", "Submit suggestion").addOption(OptionType.STRING,"content","Your suggestion",true),
                Commands.slash("suggestion", "Manage suggestions (Admin)").addSubcommands(
                        new SubcommandData("approve","Approve").addOption(OptionType.INTEGER,"id","ID",true).addOption(OptionType.STRING,"note","Note",false),
                        new SubcommandData("deny","Deny").addOption(OptionType.INTEGER,"id","ID",true).addOption(OptionType.STRING,"note","Note",false),
                        new SubcommandData("implement","Implemented").addOption(OptionType.INTEGER,"id","ID",true).addOption(OptionType.STRING,"note","Note",false)),

                // ===== INFO =====
                Commands.slash("profile", "View profile card").addOption(OptionType.USER,"member","Member",false),
                Commands.slash("user", "User info (Admin)").addOption(OptionType.USER,"member","Member",false),
                Commands.slash("avatar", "Get avatar").addOption(OptionType.USER,"member","Member",false),
                Commands.slash("server", "Server info"),
                Commands.slash("roles", "List roles (Admin)"),
                Commands.slash("banner", "Get banner").addOption(OptionType.USER,"member","Member",false),
                Commands.slash("invites", "View invites (Admin)").addOption(OptionType.USER,"member","Member",false),
                Commands.slash("server-avatar", "Server icon"),
                Commands.slash("server-banner", "Server banner")
        ));
        
        // ===== 2. DYNAMIC DATABASE SLASH COMMANDS =====
        int newCommandCount = 0;
        try {
            // 1. Register Standard Commands
            com.google.gson.JsonArray dbCmds = com.highcore.bot.database.SupabaseClient.getAllCommands();
            if (dbCmds != null && dbCmds.size() > 0) {
                for (com.google.gson.JsonElement el : dbCmds) {
                    com.google.gson.JsonObject obj = el.getAsJsonObject();
                    String name = obj.get("name").getAsString().toLowerCase().replaceAll("[^a-z0-9_-]", "");
                    if (name.isEmpty()) continue;
                    
                    String desc = obj.has("description") && !obj.get("description").isJsonNull() ? obj.get("description").getAsString() : "Custom neural command";
                    if (desc.isEmpty()) desc = "Custom neural command";
                    
                    commandList.add(Commands.slash(name, desc.substring(0, Math.min(desc.length(), 100))));
                    newCommandCount++;
                    log.info("Loaded dynamic command: /{}", name);
                }
            }
            
            // 2. Register Panel/Menu Triggers
            com.google.gson.JsonArray menus = com.highcore.bot.database.SupabaseClient.getAllMenus();
            if (menus != null && menus.size() > 0) {
                for (com.google.gson.JsonElement el : menus) {
                    com.google.gson.JsonObject obj = el.getAsJsonObject();
                    if (obj.has("trigger_command") && !obj.get("trigger_command").isJsonNull()) {
                        String name = obj.get("trigger_command").getAsString().toLowerCase().replaceAll("[^a-z0-9_-]", "");
                        if (!name.isEmpty()) {
                            String title = obj.has("title") ? obj.get("title").getAsString() : "Custom Panel";
                            commandList.add(Commands.slash(name, "Open panel: " + title));
                            newCommandCount++;
                            log.info("Loaded panel trigger: /{}", name);
                        }
                    }
                }
            }
        } catch (Exception ex) {
            log.error("Failed to load DB commands: {}", ex.getMessage());
        }

        // ===== 3. FINAL REGISTRATION =====
        updateAction.addCommands(commandList).queue(
                c -> log.info("Registered {} complete commands (Static + DB) to Discord API.", c.size()), 
                e -> log.error("Command registration failed: {}", e.getMessage())
        );

        return newCommandCount;
    }
}
