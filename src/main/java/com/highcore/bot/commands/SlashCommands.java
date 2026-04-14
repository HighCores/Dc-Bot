package com.highcore.bot.commands;

import com.highcore.bot.config.Config;
import com.highcore.bot.database.SupabaseClient;
import com.highcore.bot.services.*;
import com.highcore.bot.utils.EmbedUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.modals.Modal;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SlashCommands extends ListenerAdapter {
    private static final Logger log = LoggerFactory.getLogger(SlashCommands.class);

    public static class BcSession { public String roleId, attUrl; }
    public static final Map<String, BcSession> BC_SESSIONS = new HashMap<>();

    public static class BoterSession { public String channelId; public List<String> fileUrls = new ArrayList<>(); }
    public static final Map<String, BoterSession> BOTER_SESSIONS = new HashMap<>();

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        String name = event.getName().toLowerCase();
        
        // SMART DEFERRAL: Do NOT defer if the command uses a Modal (bc, embed, boter)
        boolean usesModal = name.equals("bc") || name.equals("embed") || name.equals("boter");
        
        if (!event.isAcknowledged() && !usesModal) {
            boolean ephemeral = !name.equals("startup") && !name.equals("tickets") && !name.equals("terms") && !name.equals("ping") && !name.equals("roll") && !name.equals("giveaway") && !name.equals("suggest") && !name.equals("profile");
            event.deferReply(ephemeral).queue();
        }

        // Dedicated commands handled elsewhere
        List<String> dedicatedCmds = java.util.Arrays.asList(
            "setnick", "ban", "unban", "unban-all", "kick", "vkick", "mute-text", "unmute-text",
            "mute-check", "mute-voice", "unmute-voice", "timeout", "untimeout", "clear", "move",
            "role", "role-multiple", "temprole", "rar", "inrole", "warn-add", "warn-remove",
            "warnings", "violations", "violations-clear", "lock", "unlock", "hide", "show", "slowmode", "add-emoji", "giveaway",
            "profile", "avatar", "server-avatar", "server", "roles", "banner", "server-banner", "invites",
            "ping", "roll", "translate", "suggest", "suggestion", "title"
        );
        if (dedicatedCmds.contains(name)) return;

        try {
            switch (name) {
                case "startup" -> { if (isAdmin(event.getMember())) { PanelService.sendStartupHub(event); } else PanelService.replyEphemeral(event, "Unauthorized Access Detected."); }
                case "tickets" -> { if (isAdmin(event.getMember())) { PanelService.sendTicketPanel(event); } else PanelService.replyEphemeral(event, "Unauthorized Access Detected."); }
                case "bc" -> { if (isAdmin(event.getMember())) { handleBroadcast(event); } else PanelService.replyEphemeral(event, "Unauthorized Access Detected."); }
                case "autoreply" -> handleAutoReply(event);
                case "embed" -> { handleEmbed(event); }
                case "boter" -> { handleBoter(event); }
                case "rename" -> { handleRename(event); }
                case "setchannel" -> { handleSetChannel(event); }
                default -> CommandService.executeSlash(event);
            }
        } catch (Exception e) {
            log.error("Error executing SlashCommands: ", e);
            if (!event.isAcknowledged()) {
                PanelService.replyEphemeral(event, EmbedUtil.error("SYSTEM ERROR", "Execution failed: " + e.getMessage()));
            }
        }
    }

    private void handleAutoReply(SlashCommandInteractionEvent event) {
        if (!isStaff(event.getMember())) {
            PanelService.replyEphemeral(event, EmbedUtil.accessDenied());
            return;
        }
        String sub = event.getSubcommandName();
        if (sub == null) return;
        switch (sub) {
            case "add" -> {
                String k = event.getOption("keyword", OptionMapping::getAsString);
                String r = event.getOption("response", OptionMapping::getAsString);
                AutoReplyService.addResponse(k, r, event.getUser().getName());
                PanelService.replyEphemeral(event, EmbedUtil.success("SYSTEM UPDATED", "Auto-reply added for keyword: **" + k + "**"));
                LogManager.logEmbed(event.getGuild(), Config.LOG_COMMANDS, EmbedUtil.createOldLogEmbed("AutoReply Extension", "Action: Add\nKeyword: `" + k + "`\nResponse: " + r, event.getMember(), null, null, EmbedUtil.SUCCESS));
            }
            case "remove" -> {
                String k = event.getOption("keyword", OptionMapping::getAsString);
                AutoReplyService.removeResponse(k);
                PanelService.replyEphemeral(event, EmbedUtil.success("SYSTEM UPDATED", "Auto-reply removed for keyword: **" + k + "**"));
                LogManager.logEmbed(event.getGuild(), Config.LOG_COMMANDS, EmbedUtil.createOldLogEmbed("AutoReply Removals", "Action: Remove\nKeyword: `" + k + "`", event.getMember(), null, null, EmbedUtil.DANGER));
            }
            case "list" -> {
                Map<String, String> all = AutoReplyService.getAllResponses();
                if (all.isEmpty()) {
                    PanelService.replyEphemeral(event, EmbedUtil.info("LIST EMPTY", "No auto-reply messages found."));
                    return;
                }
                StringBuilder s = new StringBuilder();
                all.forEach((k, v) -> s.append("**").append(k).append("** \u2192 ").append(v).append("\n"));
                PanelService.replyEphemeral(event, EmbedUtil.info("MESSAGE LIST", s.toString()));
            }
        }
    }

    private void handleEmbed(SlashCommandInteractionEvent event) {
        if (!isAdmin(event.getMember())) {
            PanelService.replyEphemeral(event, EmbedUtil.accessDenied());
            return;
        }
        String title = event.getOption("title") != null ? event.getOption("title").getAsString() : null;
        String desc = event.getOption("description") != null ? event.getOption("description").getAsString() : null;
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

        PanelService.reply(event, EmbedUtil.custom("AGENCY", title, desc, image, thumb, aName, aIcon, fText, fIcon,
                f1n, f1v, f1i, f2n, f2v, f2i, f3n, f3v, f3i));
        LogManager.logEmbed(event.getGuild(), Config.LOG_COMMANDS, EmbedUtil.createOldLogEmbed("Embed Deployment", "Branded embed deployed in " + event.getChannel().getAsMention(), event.getMember(), null, null, EmbedUtil.GOLD));
    }

    private void handleRename(SlashCommandInteractionEvent event) {
        if (!isAdmin(event.getMember())) {
            PanelService.replyEphemeral(event, EmbedUtil.accessDenied());
            return;
        }
        GuildChannel ch = event.getOption("channel") != null ? (GuildChannel) event.getOption("channel").getAsChannel() : (GuildChannel) event.getChannel();
        String n = event.getOption("name", OptionMapping::getAsString);
        if (ch == null || n == null) {
            PanelService.replyEphemeral(event, EmbedUtil.error("DATA ERROR", "Please specify the new name."));
            return;
        }
        String old = ch.getName();
        ch.getManager().setName(n).queue(v -> {
            PanelService.replyEphemeral(event, EmbedUtil.success("SYSTEM UPDATED", "Channel renamed: `" + old + "` \u2192 `" + n + "`"));
            LogManager.logEmbed(event.getGuild(), Config.LOG_COMMANDS, EmbedUtil.createOldLogEmbed("Channel Renamed", "Target: " + ch.getAsMention() + "\n`" + old + "` \u2192 `" + n + "`", event.getMember(), null, null, EmbedUtil.INFO));
        });
    }

    private void handleSetChannel(SlashCommandInteractionEvent event) {
        if (!isAdmin(event.getMember())) {
            PanelService.replyEphemeral(event, EmbedUtil.accessDenied());
            return;
        }
        String p = event.getOption("purpose", OptionMapping::getAsString);
        GuildChannel ch = event.getOption("channel") != null ? (GuildChannel) event.getOption("channel").getAsChannel() : (GuildChannel) event.getChannel();
        if (p == null || ch == null) {
            PanelService.replyEphemeral(event, EmbedUtil.error("DATA ERROR", "Specify settings type and target channel."));
            return;
        }
        SupabaseClient.setSetting(p.toUpperCase(), ch.getId());
        Config.updateRuntime(p.toUpperCase(), ch.getId());
        PanelService.replyEphemeral(event, EmbedUtil.success("SETTINGS UPDATED", "Setting `" + p.toUpperCase() + "` now set to: " + ch.getAsMention()));
        LogManager.logEmbed(event.getGuild(), Config.LOG_COMMANDS, EmbedUtil.createOldLogEmbed("Configuration Update", "Key: `" + p.toUpperCase() + "`\nValue: " + ch.getAsMention(), event.getMember(), null, null, EmbedUtil.SUCCESS));
    }

    private void handleBroadcast(SlashCommandInteractionEvent event) {
        BcSession session = new BcSession();
        OptionMapping roleOpt = event.getOption("role");
        if (roleOpt != null) session.roleId = roleOpt.getAsRole().getId();
        OptionMapping attOpt = event.getOption("attachment");
        if (attOpt != null) session.attUrl = attOpt.getAsAttachment().getUrl();

        BC_SESSIONS.put("bc_" + event.getUser().getId(), session);

        TextInput bcMsg = TextInput.create("message", TextInputStyle.PARAGRAPH)
                .setPlaceholder("Insert broadcast announcement here...")
                .setRequired(true).build();
 
        event.replyModal(Modal.create("modal_bc", "Broadcast Transmission")
                .addComponents(Label.of("Broadcast Content", bcMsg))
                .build()).queue();
    }

    private void handleBoter(SlashCommandInteractionEvent event) {
        if (!isAdmin(event.getMember())) {
            PanelService.replyEphemeral(event, EmbedUtil.accessDenied());
            return;
        }

        BoterSession session = new BoterSession();
        session.channelId = (event.getOption("channel") != null ? event.getOption("channel").getAsChannel() : event.getChannel()).getId();

        for (int i = 1; i <= 3; i++) {
            OptionMapping att = event.getOption("file" + i);
            if (att != null) session.fileUrls.add(att.getAsAttachment().getUrl());
        }

        BOTER_SESSIONS.put("boter_" + event.getUser().getId(), session);

        TextInput boterMsg = TextInput.create("message", TextInputStyle.PARAGRAPH)
                .setPlaceholder("Enter the transmission content (emojis & separators supported)...")
                .setRequired(true)
                .build();
        event.replyModal(Modal.create("modal_boter", "Highcore Transmission")
                .addComponents(Label.of("Message Content", boterMsg))
                .build()).queue();
    }

    private boolean isStaff(Member m) { return Config.isStaff(m); }
    private boolean isAdmin(Member m) { return Config.isAdmin(m); }
}
