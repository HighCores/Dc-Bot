package com.highcore.bot.commands;

import com.highcore.bot.config.Config;
import com.highcore.bot.database.SupabaseClient;
import com.highcore.bot.services.*;
import com.highcore.bot.utils.EmbedUtil;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.modals.Modal;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.HashMap;
import java.util.Map;

public class SlashCommands extends ListenerAdapter {
    private static final Logger log = LoggerFactory.getLogger(SlashCommands.class);

    public static class BcSession { public String roleId, attUrl; }
    public static final Map<String, BcSession> BC_SESSIONS = new HashMap<>();

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        String name = event.getName().toLowerCase();
        
        switch (name) {
            case "startup" -> { if (isAdmin(event.getMember())) PanelService.sendStartupHub(event); else event.reply("Unauthorized.").setEphemeral(true).queue(); return; }
            case "tickets" -> { PanelService.sendTicketPanel(event); return; }
            case "services" -> { PanelService.sendServicesCategory(event); return; }
            case "giveaway" -> { if (isAdmin(event.getMember())) PanelService.sendGiveawayPanel(event); else event.reply("Unauthorized.").setEphemeral(true).queue(); return; }
            case "stats" -> { PanelService.sendStatsPanel(event); return; }
            case "points" -> { handlePoints(event); return; }
            case "leaderboard" -> { handleLeaderboard(event); return; }
            case "bc" -> { if (isAdmin(event.getMember())) handleBroadcast(event); else event.reply("Unauthorized.").setEphemeral(true).queue(); return; }
            case "filter" -> { if (isAdmin(event.getMember())) handleFilter(event); else event.reply("Unauthorized.").setEphemeral(true).queue(); return; }
            case "auto" -> { if (isAdmin(event.getMember())) handleAutoReply(event); else event.reply("Unauthorized.").setEphemeral(true).queue(); return; }
            case "clear" -> { if (isAdmin(event.getMember())) handleClear(event); else event.reply("Unauthorized.").setEphemeral(true).queue(); return; }
            case "ping" -> { event.reply("Latency: " + event.getJDA().getGatewayPing() + "ms").setEphemeral(true).queue(); return; }
        }

        CommandService.executeSlash(event);
    }

    private void handlePoints(SlashCommandInteractionEvent event) {
        String sub = event.getSubcommandName();
        if (sub == null) return;
        Member m = event.getOption("member", OptionMapping::getAsMember);
        if (m == null) m = event.getMember();

        if (sub.equals("check")) {
            int pts = SupabaseClient.getPoints(m.getId(), event.getGuild().getId());
            PanelService.reply(event, EmbedUtil.containerBranded("MERIT", "Operational Standing",
                    "### \u2B50 Identity Summary\n> Operative: " + m.getAsMention() + "\n> Total Merit: **" + pts + "** units.", EmbedUtil.BANNER_MAIN));
        } else if (isAdmin(event.getMember())) {
            int amt = event.getOption("amount") != null ? event.getOption("amount").getAsInt() : 0;
            switch (sub) {
                case "add" -> { SupabaseClient.addPoints(m.getId(), event.getGuild().getId(), amt, "Admin Allocation", event.getUser().getName()); event.reply("Merit allocated.").setEphemeral(true).queue(); }
                case "remove" -> { SupabaseClient.addPoints(m.getId(), event.getGuild().getId(), -amt, "Admin Deallocation", event.getUser().getName()); event.reply("Merit deallocated.").setEphemeral(true).queue(); }
                case "reset" -> { SupabaseClient.setPoints(m.getId(), event.getGuild().getId(), 0, "Admin Reset", event.getUser().getName()); event.reply("Merit reset.").setEphemeral(true).queue(); }
            }
        } else event.reply("Unauthorized.").setEphemeral(true).queue();
    }

    private void handleLeaderboard(SlashCommandInteractionEvent event) {
        com.google.gson.JsonArray top = SupabaseClient.getLeaderboard(event.getGuild().getId());
        StringBuilder sb = new StringBuilder("### \uD83C\uDFC6 Top Operative Ranking\n");
        for (int i = 0; i < top.size(); i++) {
            com.google.gson.JsonObject op = top.get(i).getAsJsonObject();
            sb.append(i + 1).append(". <@").append(op.get("user_id").getAsString()).append("> \u2192 **").append(op.get("points").getAsInt()).append("**\n");
        }
        PanelService.reply(event, EmbedUtil.containerBranded("RANKING", "Performance Ledger", sb.toString(), EmbedUtil.BANNER_MAIN));
    }

    private void handleFilter(SlashCommandInteractionEvent event) {
        String sub = event.getSubcommandName();
        if (sub == null) return;
        String word = event.getOption("word") != null ? event.getOption("word").getAsString() : "";
        if (sub.equals("add")) { SupabaseClient.addForbiddenWord(word); WordFilterService.init(); event.reply("Added: " + word).setEphemeral(true).queue(); }
        else if (sub.equals("remove")) { SupabaseClient.removeForbiddenWord(word); WordFilterService.init(); event.reply("Removed: " + word).setEphemeral(true).queue(); }
        else if (sub.equals("list")) { event.reply("Matrix: " + SupabaseClient.getWordFilter().toString()).setEphemeral(true).queue(); }
    }

    private void handleAutoReply(SlashCommandInteractionEvent event) {
        String sub = event.getSubcommandName();
        if (sub == null) return;
        if (sub.equals("add")) { AutoReplyService.addResponse(event.getOption("keyword").getAsString(), event.getOption("response").getAsString(), event.getUser().getName()); event.reply("Response added.").setEphemeral(true).queue(); }
        else if (sub.equals("remove")) { AutoReplyService.removeResponse(event.getOption("keyword").getAsString()); event.reply("Response purged.").setEphemeral(true).queue(); }
        else if (sub.equals("list")) { event.reply("Matrix active.").setEphemeral(true).queue(); }
    }

    private void handleBroadcast(SlashCommandInteractionEvent event) {
        BcSession s = new BcSession();
        if (event.getOption("role") != null) s.roleId = event.getOption("role").getAsRole().getId();
        if (event.getOption("attachment") != null) s.attUrl = event.getOption("attachment").getAsAttachment().getUrl();
        BC_SESSIONS.put("bc_" + event.getUser().getId(), s);
        TextInput input = TextInput.create("message", TextInputStyle.PARAGRAPH).setRequired(true).build();
        event.replyModal(Modal.create("modal_bc", "BROADCAST TRANSMISSION").addComponents(Label.of("Message Content", input)).build()).queue();
    }

    private void handleClear(SlashCommandInteractionEvent event) {
        int count = (event.getOption("amount") != null) ? event.getOption("amount").getAsInt() : 0;
        event.getChannel().getIterableHistory().takeAsync(count).thenAccept(msgs -> {
            event.getGuildChannel().deleteMessages(msgs).queue(v -> event.reply("Purged " + count + " records.").setEphemeral(true).queue());
        });
    }

    private boolean isAdmin(Member m) { return Config.isAdmin(m); }
}
