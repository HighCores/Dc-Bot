package com.highcore.bot.commands;

import com.highcore.bot.config.Config;
import com.highcore.bot.database.SupabaseClient;
import com.highcore.bot.services.*;
import com.highcore.bot.utils.EmbedUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;

public class SlashCommands extends ListenerAdapter {
    private static final Logger log = LoggerFactory.getLogger(SlashCommands.class);

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        TextChannel cmdLog = LogManager.get(event.getGuild(), Config.LOG_COMMANDS);
        if (cmdLog != null) {
            cmdLog.sendMessageEmbeds(EmbedUtil.base().setColor(EmbedUtil.INFO)
                    .setAuthor(event.getUser().getName(), null, event.getUser().getEffectiveAvatarUrl())
                    .setDescription("### \u2699\uFE0F Command Used")
                    .addField("Command", "`/" + event.getFullCommandName() + "`", true)
                    .addField("User", event.getUser().getAsMention(), true)
                    .addField("Channel", event.getChannel().getAsMention(), true)
                    .addField("\uD83D\uDD52 Time", java.time.format.DateTimeFormatter.ofPattern("EEE, MMM dd yyyy \u2022 hh:mm:ss a")
                            .withZone(java.time.ZoneId.of("Asia/Riyadh")).format(java.time.Instant.now()), false).build()).queue();
        }
        if (isAdmin(event.getMember()) && !event.getName().equals("menu") && !event.getName().equals("help")) {
            TextChannel modLog = LogManager.get(event.getGuild(), Config.LOG_MODS_CMD);
            if (modLog != null) {
                modLog.sendMessageEmbeds(EmbedUtil.base().setColor(EmbedUtil.DANGER)
                        .setAuthor(event.getUser().getName(), null, event.getUser().getEffectiveAvatarUrl())
                        .setDescription("### \uD83D\uDC6E Admin Command")
                        .addField("Command", "`/" + event.getFullCommandName() + "`", true)
                        .addField("Admin", event.getUser().getAsMention(), true)
                        .addField("Channel", event.getChannel().getAsMention(), true).build()).queue();
            }
        }
        switch (event.getName()) {
            case "sync" -> handleSync(event);
            case "help" -> handleHelp(event);
            case "autoreply" -> handleAutoReply(event);
            case "panel" -> handlePanel(event);
            case "embed" -> handleEmbed(event);
            case "startup" -> handleStartup(event);
            case "rename" -> handleRename(event);
            case "setchannel" -> handleSetChannel(event);
            default -> com.highcore.bot.services.CommandService.executeSlash(event);
        }
    }

    private void handleSync(SlashCommandInteractionEvent event) {
        if (!isAdmin(event.getMember()) && !event.getMember().hasPermission(net.dv8tion.jda.api.Permission.ADMINISTRATOR)) {
            event.replyEmbeds(EmbedUtil.error("Unauthorized", "High Command authority required.")).setEphemeral(true).queue();
            return;
        }
        event.deferReply().queue(hook -> {
            int count = com.highcore.bot.Main.registerCommands(event.getJDA());
            hook.editOriginalEmbeds(EmbedUtil.success("Sync Complete", "Successfully pushed **" + count + "** dynamic commands to the Discord API.")).queue();
        });
    }

    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent event) {
        if (!event.getComponentId().equals("menu_select")) return;
        String choice = event.getValues().get(0);
        switch (choice) {
            case "main_menu" -> event.replyEmbeds(EmbedUtil.mainMenu()).setEphemeral(true).queue();
            case "open_ticket" -> event.replyEmbeds(EmbedUtil.ticketPanel()).addComponents(ActionRow.of(
                    StringSelectMenu.create("ticket_type_select").setPlaceholder("Select ticket type...")
                            .addOption("\uD83D\uDD27 Technical Support","tech_support","Get help with a technical issue")
                            .addOption("\u2753 General Inquiry","inquiry","Ask a general question")
                            .addOption("\uD83D\uDED2 Purchase Service","purchase","Buy a service from us")
                            .addOption("\uD83D\uDCE6 Order Status","order_status","Check your order status")
                            .addOption("\uD83D\uDC54 Apply for Management","apply_management","Apply for management")
                            .addOption("\uD83D\uDC65 Apply for Highcore Team","apply_team","Join the Highcore team")
                            .build())).setEphemeral(true).queue();
            case "services" -> event.replyEmbeds(EmbedUtil.services()).setEphemeral(true).queue();
            case "team" -> event.replyEmbeds(EmbedUtil.team()).setEphemeral(true).queue();
            case "ai_query" -> { AIService.enableAI(event.getChannel().getId()); event.reply("\uD83D\uDCAC AI enabled! Type your question. `!stop` to disable.").setEphemeral(true).queue(); }
            case "stats" -> { event.deferReply(true).queue(); Map<String,Integer> s=StatsService.getTicketStats(); event.getHook().editOriginalEmbeds(EmbedUtil.stats(s.get("total"),s.get("open"),s.get("closed"),s.get("claimed"),StatsService.getTopStaff(30))).queue(); }
            case "giveaway_panel" -> {
                if(!isAdmin(event.getMember())){event.reply("Admin only.").setEphemeral(true).queue();return;}
                event.replyEmbeds(EmbedUtil.branded().setColor(EmbedUtil.GOLD).setDescription("""
                        ## \uD83C\uDF89 Giveaway Management
                        > Create and manage giveaways for your community.
                        **How it works:**
                        > \u25B8 Click **Create** to start a new giveaway
                        > \u25B8 Choose type: discount, coupon, free service, or custom
                        > \u25B8 Members join by clicking the \uD83C\uDF89 button
                        > \u25B8 Winners are auto-picked when time runs out
                        **Commands:**
                        > `/giveaway-start` — Quick create \u2022 `/giveaway-end` — End early
                        > `/giveaway-reroll` — New winner \u2022 `/giveaway-drop` — First-click-wins
                        """).build()).addComponents(ActionRow.of(Button.success("gw_create","\u2795 Create Giveaway"),Button.secondary("gw_list","\uD83D\uDCCB List Active"))).setEphemeral(true).queue();
            }
            case "points_panel" -> {
                if(!isAdmin(event.getMember())){event.reply("Admin only.").setEphemeral(true).queue();return;}
                event.replyEmbeds(EmbedUtil.branded().setColor(EmbedUtil.ACCENT_PURPLE).setDescription("""
                        ## \u2B50 Points Management
                        > Reward team members for their work.
                        **Auto-Points (Team role only):**
                        > \u25B8 Claim ticket → +5 pts \u2022 Close ticket → +10 pts
                        **Commands:**
                        > `/points add/remove/set/check` \u2022 `/points-reset user/all` \u2022 `/points-leaderboard`
                        """).build()).addComponents(ActionRow.of(Button.primary("pts_leaderboard","\uD83C\uDFC6 Leaderboard"))).setEphemeral(true).queue();
            }
            case "level_panel" -> {
                if(!isAdmin(event.getMember())){event.reply("Admin only.").setEphemeral(true).queue();return;}
                JsonArray rewards=SupabaseClient.getLevelRewards(event.getGuild().getId());
                StringBuilder rl=new StringBuilder();
                if(rewards!=null&&rewards.size()>0){for(var el:rewards){JsonObject r=el.getAsJsonObject();rl.append("> Lv.**").append(r.get("level").getAsInt()).append("** → <@&").append(r.get("role_id").getAsString()).append(">\n");}}else rl.append("> None configured.\n");
                event.replyEmbeds(EmbedUtil.branded().setColor(EmbedUtil.ACCENT_TEAL).setDescription("## \uD83D\uDCCA Level System\n> 15-25 XP/msg (1 min cooldown) \u2022 Level = \u221A(XP/100)\n\n### \uD83C\uDF81 Role Rewards\n"+rl+"\n**Commands:** `/rank` `/top` `/setxp` `/setlevel` `/reset`").build())
                        .addComponents(ActionRow.of(Button.success("lvl_add_reward","\u2795 Add Reward"),Button.danger("lvl_remove_reward","\u2796 Remove Reward"),Button.primary("lvl_leaderboard","\uD83C\uDFC6 Leaderboard"))).setEphemeral(true).queue();
            }
            case "settings_panel" -> {
                if(!isAdmin(event.getMember())){event.reply("Admin only.").setEphemeral(true).queue();return;}
                event.replyEmbeds(EmbedUtil.branded().setColor(EmbedUtil.INFO).setDescription("""
                        ## \u2699\uFE0F Bot Settings
                        **Channel Config:** `/setchannel <purpose> [channel]`
                        > welcome, transcript, ticket, order, updates, startup, log, dev-prices, design-prices, minecraft-prices, highcore, service-terms
                        **Admin Tools:**
                        > `/panel` — Custom embed panel \u2022 `/embed` — Full embed builder
                        > `/startup` — Send startup guide \u2022 `/rename` — Rename channel
                        > `/autoreply add/remove/list` — Auto-replies
                        """).build()).setEphemeral(true).queue();
            }
        }
    }

    private void handleHelp(SlashCommandInteractionEvent event) {
        boolean admin=isAdmin(event.getMember()); boolean staff=isStaff(event.getMember());
        StringBuilder sb=new StringBuilder("## \uD83D\uDCD6 Highcore Bot — Commands\n\n");
        sb.append("### \uD83D\uDC64 General\n");
        sb.append("> `/menu` — Control panel (all panels here)\n> `/help` — This message\n> `/ticket` — Open a ticket\n");
        sb.append("> `/profile` `/avatar` `/banner` — User info\n> `/server` `/server-avatar` `/server-banner` — Server info\n");
        sb.append("> `/colors` `/color-set` — Color roles\n> `/roll` `/translate` `/get-emojis` `/vito` `/suggest`\n\n");
        sb.append("### \uD83D\uDCCA Leveling & Points\n");
        sb.append("> `/rank` — Rank card \u2022 `/top` — Leaderboard \u2022 `/level` — Quick check\n");
        sb.append("> `/points check` — Points \u2022 `/points-leaderboard` — Ranking\n");
        sb.append("> `/title view/set` — Profile title\n\n");
        if(staff) sb.append("### \uD83D\uDCAC Staff\n> `/autoreply add/remove/list`\n\n");
        if(admin){
            sb.append("### \uD83D\uDC6E Moderation\n");
            sb.append("> `/ban` `/unban` `/unban-all` `/kick` `/vkick` `/setnick`\n");
            sb.append("> `/mute-text` `/unmute-text` `/mute-voice` `/unmute-voice` `/mute-check`\n");
            sb.append("> `/timeout` `/untimeout` `/clear` `/lock` `/unlock` `/hide` `/show` `/slowmode`\n");
            sb.append("> `/move` `/role` `/temprole` `/rar` `/inrole` `/role-multiple` `/add-emoji`\n");
            sb.append("> `/warn-add` `/warn-remove` `/warnings` `/violations` `/violations-clear`\n\n");
            sb.append("### \u2699\uFE0F Management\n");
            sb.append("> `/menu` → All panels \u2022 `/ping` `/rep` `/suggestion`\n");
            sb.append("> `/points add/remove/set` `/points-reset` `/user` `/roles` `/invites`\n");
            sb.append("> `/giveaway-start` `/giveaway-end` `/giveaway-reroll` `/giveaway-drop`\n\n");
            sb.append("### \uD83D\uDD12 High Admin\n> `/setxp` `/setlevel` `/reset`\n\n");
        }
        sb.append("### \uD83E\uDD16 Text\n> `!ai` — Enable AI \u2022 `!stop` — Disable AI");
        event.replyEmbeds(EmbedUtil.branded().setColor(EmbedUtil.PRIMARY).setDescription(sb.toString()).build()).setEphemeral(true).queue();
    }

    private void handleAutoReply(SlashCommandInteractionEvent event) {
        if(!isStaff(event.getMember())){event.replyEmbeds(EmbedUtil.error("Unauthorized","Staff only.")).setEphemeral(true).queue();return;}
        String sub=event.getSubcommandName();if(sub==null)return;
        switch(sub){
            case "add"->{String k=event.getOption("keyword",OptionMapping::getAsString),r=event.getOption("response",OptionMapping::getAsString);AutoReplyService.addResponse(k,r,event.getUser().getName());event.replyEmbeds(EmbedUtil.success("Added","Auto-reply for: **"+k+"**")).setEphemeral(true).queue();}
            case "remove"->{String k=event.getOption("keyword",OptionMapping::getAsString);AutoReplyService.removeResponse(k);event.replyEmbeds(EmbedUtil.success("Removed","Removed: **"+k+"**")).setEphemeral(true).queue();}
            case "list"->{Map<String,String>all=AutoReplyService.getAllResponses();if(all.isEmpty()){event.replyEmbeds(EmbedUtil.info("Auto-Replies","None configured.")).setEphemeral(true).queue();return;}StringBuilder s=new StringBuilder();all.forEach((k,v)->s.append("**").append(k).append("** → ").append(v).append("\n"));event.replyEmbeds(EmbedUtil.info("Auto-Replies",s.toString())).setEphemeral(true).queue();}
        }
    }

    private void handlePanel(SlashCommandInteractionEvent event){if(!isAdmin(event.getMember())){event.replyEmbeds(EmbedUtil.error("Unauthorized","Admin only.")).setEphemeral(true).queue();return;}event.replyEmbeds(EmbedUtil.base().setColor(EmbedUtil.parseColor(event.getOption("color","primary",OptionMapping::getAsString))).setTitle(event.getOption("title",OptionMapping::getAsString)).setDescription(event.getOption("description",OptionMapping::getAsString)).build()).queue();}

    private void handleEmbed(SlashCommandInteractionEvent event){if(!isAdmin(event.getMember())){event.replyEmbeds(EmbedUtil.error("Unauthorized","Admin only.")).setEphemeral(true).queue();return;}event.replyEmbeds(EmbedUtil.custom(event.getOption("title",OptionMapping::getAsString),event.getOption("description",OptionMapping::getAsString),event.getOption("color",OptionMapping::getAsString),event.getOption("image",OptionMapping::getAsString),event.getOption("thumbnail",OptionMapping::getAsString),event.getOption("author_name",OptionMapping::getAsString),event.getOption("author_icon",OptionMapping::getAsString),event.getOption("footer_text",OptionMapping::getAsString),event.getOption("footer_icon",OptionMapping::getAsString),event.getOption("field1_name",OptionMapping::getAsString),event.getOption("field1_value",OptionMapping::getAsString),event.getOption("field1_inline")!=null?event.getOption("field1_inline").getAsBoolean():null,event.getOption("field2_name",OptionMapping::getAsString),event.getOption("field2_value",OptionMapping::getAsString),event.getOption("field2_inline")!=null?event.getOption("field2_inline").getAsBoolean():null,event.getOption("field3_name",OptionMapping::getAsString),event.getOption("field3_value",OptionMapping::getAsString),event.getOption("field3_inline")!=null?event.getOption("field3_inline").getAsBoolean():null)).queue();}

    private void handleStartup(SlashCommandInteractionEvent event){if(!isAdmin(event.getMember())){event.replyEmbeds(EmbedUtil.error("Unauthorized","Admin only.")).setEphemeral(true).queue();return;}String h=Config.CH_HIGHCORE,st=Config.CH_SERVICE_TERMS,u=Config.CH_UPDATES,dp=Config.CH_DEV_PRICES,dsp=Config.CH_DESIGN_PRICES,mp=Config.CH_MINECRAFT_PRICES,o=Config.CH_ORDER,t=Config.CH_TICKET;event.replyEmbeds(EmbedUtil.branded().setColor(EmbedUtil.PRIMARY).setDescription(String.format("## \uD83D\uDCD6 Highcore Agency — Startup Guide\n> Welcome! Here's your quick guide \uD83D\uDE80\n### \uD83C\uDFE0 About Us\n> <#%s> — About Highcore\n> <#%s> — Service Terms\n> <#%s> — Updates & Offers\n### \uD83D\uDCB0 Pricing\n> <#%s> — Dev Prices\n> <#%s> — Design Prices\n> <#%s> — MC Prices\n### \uD83D\uDECD\uFE0F Order\n> <#%s> — Place an order\n> <#%s> — Open a ticket\n### \u2753 Need Help?\n> Open a ticket! \uD83D\uDE0A",h!=null?h:"0",st!=null?st:"0",u!=null?u:"0",dp!=null?dp:"0",dsp!=null?dsp:"0",mp!=null?mp:"0",o!=null?o:"0",t!=null?t:"0")).build()).queue();}

    private void handleRename(SlashCommandInteractionEvent event){if(!isAdmin(event.getMember())){event.replyEmbeds(EmbedUtil.error("Unauthorized","Admin only.")).setEphemeral(true).queue();return;}GuildChannel ch=event.getOption("channel")!=null?event.getOption("channel",OptionMapping::getAsChannel):event.getGuildChannel();String n=event.getOption("name",OptionMapping::getAsString);if(ch==null||n==null){event.replyEmbeds(EmbedUtil.error("Error","Specify a name.")).setEphemeral(true).queue();return;}String old=ch.getName();ch.getManager().setName(n).queue(v->event.replyEmbeds(EmbedUtil.success("Renamed","`"+old+"` → `"+n+"`")).setEphemeral(true).queue(),e->event.replyEmbeds(EmbedUtil.error("Error",e.getMessage())).setEphemeral(true).queue());}

    private void handleSetChannel(SlashCommandInteractionEvent event){if(!isAdmin(event.getMember())){event.replyEmbeds(EmbedUtil.error("Unauthorized","Admin only.")).setEphemeral(true).queue();return;}String p=event.getOption("purpose",OptionMapping::getAsString);GuildChannel ch=event.getOption("channel")!=null?event.getOption("channel",OptionMapping::getAsChannel):event.getGuildChannel();if(p==null||ch==null){event.replyEmbeds(EmbedUtil.error("Error","Specify purpose.")).setEphemeral(true).queue();return;}String key,label;switch(p.toLowerCase()){case "welcome"->{key="WELCOME_CHANNEL_ID";label="\uD83C\uDF89 Welcome";}case "transcript"->{key="TRANSCRIPT_CHANNEL_ID";label="\uD83D\uDCDC Transcript";}case "ticket"->{key="CH_TICKET";label="\uD83C\uDFAB Tickets";}case "order"->{key="CH_ORDER";label="\uD83D\uDED2 Orders";}case "updates"->{key="CH_UPDATES";label="\uD83D\uDCEF Updates";}case "startup"->{key="CH_STARTUP";label="\uD83D\uDCD6 Startup";}case "log"->{key="LOG_CATEGORY_ID";label="\uD83D\uDD14 Logs";}case "dev-prices"->{key="CH_DEV_PRICES";label="\uD83D\uDCB0 Dev Prices";}case "design-prices"->{key="CH_DESIGN_PRICES";label="\uD83C\uDFA8 Design Prices";}case "minecraft-prices"->{key="CH_MINECRAFT_PRICES";label="\u26CF\uFE0F MC Prices";}case "highcore"->{key="CH_HIGHCORE";label="\uD83C\uDFAF Highcore";}case "service-terms"->{key="CH_SERVICE_TERMS";label="\uD83D\uDCC3 Terms";}default->{event.replyEmbeds(EmbedUtil.error("Unknown","Options: welcome/transcript/ticket/order/updates/startup/log/dev-prices/design-prices/minecraft-prices/highcore/service-terms")).setEphemeral(true).queue();return;}}SupabaseClient.setSetting(key,ch.getId());Config.updateRuntime(key,ch.getId());event.replyEmbeds(EmbedUtil.success("Channel Set",label+" → "+ch.getAsMention()+"\n\n\u2705 Saved!")).setEphemeral(true).queue();TextChannel uc=LogManager.get(event.getGuild(),Config.LOG_UPDATES);if(uc!=null)uc.sendMessageEmbeds(EmbedUtil.base().setColor(EmbedUtil.INFO).setDescription("### \u2699\uFE0F Setting Changed\n> **"+label+"** → "+ch.getAsMention()+"\n> By: "+event.getUser().getAsMention()).build()).queue();}

    private boolean isStaff(Member m){return m!=null&&m.getRoles().stream().anyMatch(r->Config.getStaffRoles().contains(r.getId()));}
    private boolean isAdmin(Member m){return m!=null&&m.getRoles().stream().anyMatch(r->Config.getAdminRoles().contains(r.getId()));}
}
