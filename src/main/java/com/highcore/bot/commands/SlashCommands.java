package com.highcore.bot.commands;

import com.highcore.bot.config.Config;
import com.highcore.bot.database.SupabaseClient;
import com.highcore.bot.services.*;
import com.highcore.bot.utils.EmbedUtil;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.modals.Modal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SlashCommands extends ListenerAdapter {
    private static final Logger log = LoggerFactory.getLogger(SlashCommands.class);

    public static class BcSession { public String roleId; public String attUrl; }
    public static class BoterSession { public String channelId; public List<String> fileUrls = new ArrayList<>(); }
    public static final Map<String, BcSession> BC_SESSIONS = new HashMap<>();
    public static final Map<String, BoterSession> BOTER_SESSIONS = new HashMap<>();

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        TextChannel cmdLog = LogManager.get(event.getGuild(), Config.LOG_COMMANDS);
        if (cmdLog != null) {
            String body = "### \u2699\uFE0F Command Executed\n" +
                    "**Command:** `/" + event.getFullCommandName() + "`\n" +
                    "**User:** " + event.getUser().getAsMention() + "\n" +
                    "**Channel:** " + event.getChannel().getAsMention() + "\n" +
                    "**Time:** " + java.time.format.DateTimeFormatter.ofPattern("EEE, MMM dd yyyy \u2022 hh:mm:ss a")
                    .withZone(java.time.ZoneId.of("Asia/Riyadh")).format(java.time.Instant.now());
            
            cmdLog.sendMessageComponents(EmbedUtil.logNode("COMMAND AUDIT", body, EmbedUtil.INFO))
                    .useComponentsV2(true).queue();
        }
        
        if (isAdmin(event.getMember())) {
            TextChannel modLog = LogManager.get(event.getGuild(), Config.LOG_MODS_CMD);
            if (modLog != null) {
                String body = "### \uD83D\uDC6E Administrative Override\n" +
                        "**Command:** `/" + event.getFullCommandName() + "`\n" +
                        "**Admin:** " + event.getUser().getAsMention() + "\n" +
                        "**Node:** " + event.getChannel().getAsMention();
                
                modLog.sendMessageComponents(EmbedUtil.logNode("SECURITY ALERT", body, EmbedUtil.DANGER))
                        .useComponentsV2(true).queue();
            }
        }

        try {
            switch (event.getName()) {
                case "hub", "panel" -> handleMenu(event);
                case "services" -> PanelService.sendServicesPanel(event);
                case "giveaway" -> PanelService.sendGiveawayPanel(event);
                case "points" -> handlePointsCheck(event);
                case "tickets" -> PanelService.sendTicketPanel(event);
                case "stats" -> PanelService.sendStatsPanel(event);
                case "help" -> handleHelp(event);
                case "autoreply" -> handleAutoReply(event);
                case "embed" -> handleEmbed(event);
                case "startup" -> PanelService.sendStartupPanel(event);
                case "rename" -> handleRename(event);
                case "setchannel" -> handleSetChannel(event);
                case "bc" -> handleBroadcast(event);
                case "boter" -> handleBoter(event);
            }
        } catch (Exception e) {
            log.error("Error executing SlashCommands: ", e);
            if (!event.isAcknowledged()) {
                PanelService.reply(event, EmbedUtil.error("SYSTEM ERROR", "Execution failed: " + e.getMessage()), true);
            }
        }
    }

    private void handleMenu(SlashCommandInteractionEvent event) {
        boolean admin = isAdmin(event.getMember());
        StringSelectMenu.Builder mb = StringSelectMenu.create("menu_select").setPlaceholder("Select a module...");
        
        mb.addOption("\uD83C\uDFAB Open Ticket", "open_ticket", "Create a support request")
          .addOption("\uD83D\uDCCA Statistics", "stats", "View agency stats");

        if (admin) {
            mb.addOption("\uD83C\uDF89 Giveaway Panel", "giveaway_panel", "Manage takeaways")
              .addOption("\u2B50 Points Panel", "points_panel", "Manage reward points");
        }

        PanelService.reply(event, EmbedUtil.mainMenu(), ActionRow.of(mb.build()));
    }

    private void handlePointsCheck(SlashCommandInteractionEvent event) {
        Member m = event.getOption("member", OptionMapping::getAsMember);
        if (m == null) m = event.getMember();
        int pts = SupabaseClient.getPoints(m.getId(), event.getGuild().getId());
        
        Container c = EmbedUtil.containerBranded("SECURITY AUDIT", "Merit Ledger", 
            "### \u2B50 Merit Report\n> Subject: " + m.getAsMention() + "\n> Status: **" + pts + "** reward points.", EmbedUtil.BANNER_MAIN);
        c.withAccentColor(EmbedUtil.GOLD.getRGB() & 0xFFFFFF);
        
        PanelService.reply(event, c);
    }

    private void handleHelp(SlashCommandInteractionEvent event) {
        PanelService.reply(event, EmbedUtil.help());
    }

    private void handleAutoReply(SlashCommandInteractionEvent event) {
        if(!isStaff(event.getMember())){ PanelService.reply(event, EmbedUtil.accessDenied()); return; }
        String sub=event.getSubcommandName(); if(sub==null) return;
        switch(sub){
            case "add"->{
                String k=event.getOption("keyword",OptionMapping::getAsString);
                String r=event.getOption("response",OptionMapping::getAsString);
                AutoReplyService.addResponse(k,r,event.getUser().getName());
                PanelService.reply(event, EmbedUtil.success("PROTOCOL ADDED", "Auto-reply designated for keyword: **"+k+"**"), true);
            }
            case "remove"->{
                String k=event.getOption("keyword",OptionMapping::getAsString);
                AutoReplyService.removeResponse(k);
                PanelService.reply(event, EmbedUtil.success("PROTOCOL REMOVED", "Auto-reply designation purged for: **"+k+"**"), true);
            }
            case "list"->{
                Map<String,String>all=AutoReplyService.getAllResponses();
                if(all.isEmpty()){ PanelService.reply(event, EmbedUtil.info("REGISTRY EMPTY", "No auto-reply protocols configured."), true); return; }
                StringBuilder s=new StringBuilder();
                all.forEach((k,v)->s.append("**").append(k).append("** \u2192 ").append(v).append("\n"));
                PanelService.reply(event, EmbedUtil.info("PROTOCOL LIST", s.toString()), true);
            }
        }
    }

    private void handleEmbed(SlashCommandInteractionEvent event) {
        if (!isAdmin(event.getMember())) { PanelService.reply(event, EmbedUtil.accessDenied()); return; }
        // For custom embeds, we still use the complex helper but ensure it's V2-wrapped where possible or just keep it as is if it uses Container
        // PanelService.reply handles the V2 wrap.
        String title = event.getOption("title") != null ? event.getOption("title").getAsString() : null;
        String desc = event.getOption("description") != null ? event.getOption("description").getAsString() : null;
        String color = event.getOption("color") != null ? event.getOption("color").getAsString() : null;
        String image = event.getOption("image") != null ? event.getOption("image").getAsString() : null;
        String thumb = event.getOption("thumbnail") != null ? event.getOption("thumbnail").getAsString() : null;
        String aName = event.getOption("author_name") != null ? event.getOption("author_name").getAsString() : null;
        String aIcon = event.getOption("author_icon") != null ? event.getOption("author_icon").getAsString() : null;
        String fText = event.getOption("footer_text") != null ? event.getOption("footer_text").getAsString() : null;
        String fIcon = event.getOption("footer_icon") != null ? event.getOption("footer_icon").getAsString() : null;

        String f1n = event.getOption("field1_name") != null ? event.getOption("field1_name").getAsString() : null;
        String f1v = event.getOption("field1_value") != null ? event.getOption("field1_value").getAsString() : null;
        Boolean f1i = event.getOption("field1_inline") != null ? event.getOption("field1_inline").getAsBoolean() : null;

        String f2n = event.getOption("field2_name") != null ? event.getOption("field2_name").getAsString() : null;
        String f2v = event.getOption("field2_value") != null ? event.getOption("field2_value").getAsString() : null;
        Boolean f2i = event.getOption("field2_inline") != null ? event.getOption("field2_inline").getAsBoolean() : null;

        String f3n = event.getOption("field3_name") != null ? event.getOption("field3_name").getAsString() : null;
        String f3v = event.getOption("field3_value") != null ? event.getOption("field3_value").getAsString() : null;
        Boolean f3i = event.getOption("field3_inline") != null ? event.getOption("field3_inline").getAsBoolean() : null;

        PanelService.reply(event, EmbedUtil.custom(title, desc, color, image, thumb, aName, aIcon, fText, fIcon, 
                f1n, f1v, f1i, f2n, f2v, f2i, f3n, f3v, f3i));
    }


    private void handleRename(SlashCommandInteractionEvent event) {
        if (!isAdmin(event.getMember())) { PanelService.reply(event, EmbedUtil.accessDenied()); return; }
        GuildChannel ch = event.getOption("channel") != null ? event.getOption("channel", OptionMapping::getAsChannel) : (GuildChannel)event.getChannel();
        String n = event.getOption("name", OptionMapping::getAsString);
        if (ch == null || n == null) { PanelService.reply(event, EmbedUtil.error("DATA ERROR", "Specify target designation name."), true); return; }
        String old = ch.getName();
        ch.getManager().setName(n).queue(v -> PanelService.reply(event, EmbedUtil.success("PROTOCOL UPDATED", "Node renamed: `" + old + "` \u2192 `" + n + "`"), true));
    }

    private void handleSetChannel(SlashCommandInteractionEvent event){
        if(!isAdmin(event.getMember())){ PanelService.reply(event, EmbedUtil.accessDenied()); return; }
        String p=event.getOption("purpose",OptionMapping::getAsString);
        GuildChannel ch=event.getOption("channel")!=null?event.getOption("channel",OptionMapping::getAsChannel):(GuildChannel)event.getChannel();
        if(p==null||ch==null){ PanelService.reply(event, EmbedUtil.error("DATA ERROR", "Specify protocol purpose and target node."), true); return; }
        SupabaseClient.setSetting(p.toUpperCase(),ch.getId());
        Config.updateRuntime(p.toUpperCase(),ch.getId());
        PanelService.reply(event, EmbedUtil.success("REGISTRY UPDATED", "Protocol `" + p.toUpperCase() + "` now bound to: " + ch.getAsMention()), true);
    }

    private void handleBroadcast(SlashCommandInteractionEvent event) {
        if (event.getMember() != null && event.getMember().getRoles().stream().anyMatch(r -> r.getId().equals(BroadcastService.BROADCAST_ROLE_ID))) {
            BcSession session = new BcSession();
            OptionMapping roleOpt = event.getOption("role");
            if (roleOpt != null) session.roleId = roleOpt.getAsRole().getId();
            OptionMapping attOpt = event.getOption("attachment");
            if (attOpt != null) session.attUrl = attOpt.getAsAttachment().getUrl();
            
            String sessionId = "bc_" + event.getUser().getId();
            BC_SESSIONS.put(sessionId, session);

            TextInput bodyInput = TextInput.create("message", TextInputStyle.PARAGRAPH)
                    .setPlaceholder("Enter your transmission content here...")
                    .setRequired(true)
                    .build();

            Modal modal = Modal.create("modal_bc", "BROADCAST INTERFACE")
                    .addComponents(Label.of("Message Content", bodyInput))
                    .build();

            event.replyModal(modal).queue();
        } else {
            PanelService.reply(event, EmbedUtil.accessDenied());
        }
    }

    private void handleBoter(SlashCommandInteractionEvent event) {
        if (!isAdmin(event.getMember())) { PanelService.reply(event, EmbedUtil.accessDenied()); return; }
        
        BoterSession session = new BoterSession();
        GuildChannel channel = event.getOption("channel") != null ? event.getOption("channel", OptionMapping::getAsChannel) : (GuildChannel)event.getChannel();
        session.channelId = channel.getId();
        
        for (int i = 1; i <= 3; i++) {
            OptionMapping att = event.getOption("file" + i);
            if (att != null) session.fileUrls.add(att.getAsAttachment().getUrl());
        }

        String sessionId = "boter_" + event.getUser().getId();
        BOTER_SESSIONS.put(sessionId, session);

        TextInput bodyInput = TextInput.create("message", TextInputStyle.PARAGRAPH)
                .setPlaceholder("\u0627\u0643\u062a\u0628 \u0627\u0644\u0631\u0633\u0627\u0644\u062a\u0642 \u0627\u0644\u062a\u064a \u062a\u0631\u064A\u062F...")
                .setRequired(true)
                .build();

        Modal modal = Modal.create("modal_boter", "BOT EMULATION")
                .addComponents(Label.of("Transmission Content", bodyInput))
                .build();

        event.replyModal(modal).queue();
    }

    private boolean isStaff(Member m){return Config.isStaff(m);}
    private boolean isAdmin(Member m){return Config.isAdmin(m);}
}
