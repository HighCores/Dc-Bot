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
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SlashCommands extends ListenerAdapter {
    private static final Logger log = LoggerFactory.getLogger(SlashCommands.class);

    public static class BcSession {
        public String roleId;
        public String attUrl;
    }

    public static class BoterSession {
        public String channelId;
        public List<String> fileUrls = new ArrayList<>();
    }

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

            cmdLog.sendMessageComponents(EmbedUtil.activityLog("COMMAND LOG", body, EmbedUtil.INFO))
                    .useComponentsV2(true).queue();
        }

        if (isAdmin(event.getMember())) {
            TextChannel modLog = LogManager.get(event.getGuild(), Config.LOG_MODS_CMD);
            if (modLog != null) {
                String body = "### \uD83D\uDC6E Administrative Activity\n" +
                        "**Command:** `/" + event.getFullCommandName() + "`\n" +
                        "**Admin:** " + event.getUser().getAsMention() + "\n" +
                        "**Channel:** " + event.getChannel().getAsMention();

                modLog.sendMessageComponents(EmbedUtil.activityLog("SECURITY ALERT", body, EmbedUtil.DANGER))
                        .useComponentsV2(true).queue();
            }
        }

        try {
            switch (event.getName()) {
                case "startup" -> handleStartup(event);
                case "menu" -> handleMenu(event);
                case "services" -> PanelService.sendServicesPanel(event);
                case "giveaway" -> PanelService.sendGiveawayPanel(event);
                case "points" -> handlePointsCheck(event);
                case "tickets", "ticket-panel" -> PanelService.sendTicketPanel(event);
                case "stats" -> PanelService.sendStatsPanel(event);
                case "help" -> handleHelp(event);
                case "autoreply" -> handleAutoReply(event);
                case "embed" -> handleEmbed(event);
                case "bc" -> handleBroadcast(event);
                case "boter" -> handleBoter(event);
                case "rename" -> handleRename(event);
                case "setchannel" -> handleSetChannel(event);
            }
        } catch (Exception e) {
            log.error("Error executing SlashCommands: ", e);
            if (!event.isAcknowledged()) {
                PanelService.replyEphemeral(event, EmbedUtil.error("SYSTEM ERROR", "Execution failed: " + e.getMessage()));
            }
        }
    }

    private void handleMenu(SlashCommandInteractionEvent event) {
        if (!isAdmin(event.getMember())) {
            PanelService.replyEphemeral(event, EmbedUtil.accessDenied());
            return;
        }
        PanelService.sendMainMenu(event);
    }

    private void handleStartup(SlashCommandInteractionEvent event) {
        if (!isAdmin(event.getMember())) {
            PanelService.replyEphemeral(event, EmbedUtil.accessDenied());
            return;
        }

        ActionRow row1 = ActionRow.of(
                net.dv8tion.jda.api.components.buttons.Button.primary("menu_main", "Main Terminal")
                        .withEmoji(net.dv8tion.jda.api.entities.emoji.Emoji.fromUnicode("\u23EA")),
                net.dv8tion.jda.api.components.buttons.Button.secondary("startup_map", "Server Map")
                        .withEmoji(net.dv8tion.jda.api.entities.emoji.Emoji.fromUnicode("\uD83D\uDDFA\uFE0F")),
                net.dv8tion.jda.api.components.buttons.Button.primary("menu_services", "Our Services")
                        .withEmoji(net.dv8tion.jda.api.entities.emoji.Emoji.fromUnicode("\uD83D\uDED2")));

        ActionRow row2 = ActionRow.of(
                net.dv8tion.jda.api.components.buttons.Button.success("startup_colors", "Colors")
                        .withEmoji(net.dv8tion.jda.api.entities.emoji.Emoji.fromUnicode("\uD83C\uDFA8")),
                net.dv8tion.jda.api.components.buttons.Button.danger("startup_rules", "Guidelines")
                        .withEmoji(net.dv8tion.jda.api.entities.emoji.Emoji.fromUnicode("\uD83D\uDCDC")),
                net.dv8tion.jda.api.components.buttons.Button.secondary("startup_social", "Social nodes")
                        .withEmoji(net.dv8tion.jda.api.entities.emoji.Emoji.fromUnicode("\uD83C\uDF10")));

        PanelService.reply(event, EmbedUtil.startupPanel(), row1, row2);
    }

    private void handlePointsCheck(SlashCommandInteractionEvent event) {
        event.deferReply(true).queue();
        Member m = event.getOption("member", OptionMapping::getAsMember);
        if (m == null) m = event.getMember();
        int pts = SupabaseClient.getPoints(m.getId(), event.getGuild().getId());

        Container c = EmbedUtil.containerBranded("POINTS", "Points Report",
                "### \u2B50 Your Points\n> Member: " + m.getAsMention() + "\n> Total: **" + pts + "** points.",
                EmbedUtil.BANNER_MAIN);
        c.withAccentColor(EmbedUtil.GOLD.getRGB() & 0xFFFFFF);

        PanelService.reply(event, c);
    }

    private void handleHelp(SlashCommandInteractionEvent event) {
        PanelService.reply(event, EmbedUtil.help());
    }

    private void handleAutoReply(SlashCommandInteractionEvent event) {
        if (!isStaff(event.getMember())) {
            PanelService.reply(event, EmbedUtil.accessDenied());
            return;
        }
        String sub = event.getSubcommandName();
        if (sub == null)
            return;
        switch (sub) {
            case "add" -> {
                String k = event.getOption("keyword", OptionMapping::getAsString);
                String r = event.getOption("response", OptionMapping::getAsString);
                AutoReplyService.addResponse(k, r, event.getUser().getName());
                PanelService.replyEphemeral(event,
                        EmbedUtil.success("SYSTEM UPDATED", "Auto-reply added for keyword: **" + k + "**"));
            }
            case "remove" -> {
                String k = event.getOption("keyword", OptionMapping::getAsString);
                AutoReplyService.removeResponse(k);
                PanelService.replyEphemeral(event,
                        EmbedUtil.success("SYSTEM UPDATED", "Auto-reply removed for keyword: **" + k + "**"));
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
            PanelService.reply(event, EmbedUtil.accessDenied());
            return;
        }
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

        PanelService.reply(event, EmbedUtil.custom("AGENCY", title, desc, image, thumb, aName, aIcon, fText, fIcon,
                f1n, f1v, f1i, f2n, f2v, f2i, f3n, f3v, f3i));
    }

    private void handleRename(SlashCommandInteractionEvent event) {
        if (!isAdmin(event.getMember())) {
            PanelService.reply(event, EmbedUtil.accessDenied());
            return;
        }
        GuildChannel ch = event.getOption("channel") != null ? event.getOption("channel", OptionMapping::getAsChannel)
                : (GuildChannel) event.getChannel();
        String n = event.getOption("name", OptionMapping::getAsString);
        if (ch == null || n == null) {
            PanelService.replyEphemeral(event, EmbedUtil.error("DATA ERROR", "Please specify the new name."));
            return;
        }
        String old = ch.getName();
        ch.getManager().setName(n).queue(v -> PanelService.replyEphemeral(event,
                EmbedUtil.success("SYSTEM UPDATED", "Channel renamed: `" + old + "` \u2192 `" + n + "`")));
    }

    private void handleSetChannel(SlashCommandInteractionEvent event) {
        if (!isAdmin(event.getMember())) {
            PanelService.reply(event, EmbedUtil.accessDenied());
            return;
        }
        String p = event.getOption("purpose", OptionMapping::getAsString);
        GuildChannel ch = event.getOption("channel") != null ? event.getOption("channel", OptionMapping::getAsChannel)
                : (GuildChannel) event.getChannel();
        if (p == null || ch == null) {
            PanelService.replyEphemeral(event, EmbedUtil.error("DATA ERROR", "Specify settings type and target channel."));
            return;
        }
        SupabaseClient.setSetting(p.toUpperCase(), ch.getId());
        Config.updateRuntime(p.toUpperCase(), ch.getId());
        PanelService.replyEphemeral(event, EmbedUtil.success("SETTINGS UPDATED",
                "Setting `" + p.toUpperCase() + "` now set to: " + ch.getAsMention()));
    }

    private void handleBroadcast(SlashCommandInteractionEvent event) {
        if (event.getMember() != null && event.getMember().getRoles().stream()
                .anyMatch(r -> r.getId().equals(BroadcastService.BROADCAST_ROLE_ID))) {
            BcSession session = new BcSession();
            OptionMapping roleOpt = event.getOption("role");
            if (roleOpt != null)
                session.roleId = roleOpt.getAsRole().getId();
            OptionMapping attOpt = event.getOption("attachment");
            if (attOpt != null)
                session.attUrl = attOpt.getAsAttachment().getUrl();

            String sessionId = "bc_" + event.getUser().getId();
            BC_SESSIONS.put(sessionId, session);

            TextInput bodyInput = TextInput.create("message", "Message Content", TextInputStyle.PARAGRAPH)
                    .setPlaceholder("Enter your transmission content here...")
                    .setRequired(true)
                    .build();

            Modal modal = Modal.create("modal_bc", "BROADCAST")
                    .addActionRow(bodyInput)
                    .build();

            event.replyModal(modal).queue();
        } else {
            PanelService.reply(event, EmbedUtil.accessDenied());
        }
    }

    private void handleBoter(SlashCommandInteractionEvent event) {
        if (!isAdmin(event.getMember())) {
            PanelService.reply(event, EmbedUtil.accessDenied());
            return;
        }

        BoterSession session = new BoterSession();
        GuildChannel channel = event.getOption("channel") != null
                ? event.getOption("channel", OptionMapping::getAsChannel)
                : (GuildChannel) event.getChannel();
        session.channelId = channel.getId();

        for (int i = 1; i <= 3; i++) {
            OptionMapping att = event.getOption("file" + i);
            if (att != null)
                session.fileUrls.add(att.getAsAttachment().getUrl());
        }

        String sessionId = "boter_" + event.getUser().getId();
        BOTER_SESSIONS.put(sessionId, session);

        TextInput bodyInput = TextInput.create("message", "Message Content", TextInputStyle.PARAGRAPH)
                .setPlaceholder("Enter the message content here...")
                .setRequired(true)
                .build();

        Modal modal = Modal.create("modal_boter", "EMULATE USER")
                .addActionRow(bodyInput)
                .build();

        event.replyModal(modal).queue();
    }

    private boolean isStaff(Member m) {
        return Config.isStaff(m);
    }

    private boolean isAdmin(Member m) {
        return Config.isAdmin(m);
    }
}
