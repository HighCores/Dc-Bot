package com.highcore.bot.commands;

import com.highcore.bot.config.Config;
import com.highcore.bot.services.PanelService;
import com.highcore.bot.utils.EmbedUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.requests.restaction.PermissionOverrideAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class ModerationCommands extends ListenerAdapter {
    private static final Logger log = LoggerFactory.getLogger(ModerationCommands.class);

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!isAdmin(event.getMember())) return;

        try {
            switch (event.getName()) {
                case "ban" -> handleBan(event);
                case "unban" -> handleUnban(event);
                case "kick" -> handleKick(event);
                case "mute" -> handleMute(event);
                case "unmute" -> handleUnmute(event);
                case "clear" -> handleClear(event);
                case "lock" -> handleLock(event, true);
                case "unlock" -> handleLock(event, false);
            }
        } catch (Exception e) {
            log.error("Error executing ModerationCommands: ", e);
            PanelService.replyEphemeral(event, EmbedUtil.error("TERMINAL ERROR", "Execution failure: " + e.getMessage()));
        }
    }

    private void handleBan(SlashCommandInteractionEvent event) {
        Member target = event.getOption("member", OptionMapping::getAsMember);
        String reason = event.getOption("reason", "No reason provided", OptionMapping::getAsString);
        if (target == null) { PanelService.replyEphemeral(event, EmbedUtil.error("MODERATION ERROR", "Target subject not located.")); return; }

        event.getGuild().ban(target, 0, TimeUnit.DAYS).reason(reason).queue(
                v -> PanelService.reply(event, EmbedUtil.success("SUBJECT TERMINATED", "**" + target.getUser().getName() + "** access revoked.")),
                e -> PanelService.replyEphemeral(event, EmbedUtil.error("TERMINATION FAILED", e.getMessage())));
    }

    private void handleUnban(SlashCommandInteractionEvent event) {
        String userId = event.getOption("user_id", OptionMapping::getAsString);
        if (userId == null) { PanelService.replyEphemeral(event, EmbedUtil.error("DATA ERROR", "Subject ID required.")); return; }
        event.getGuild().unban(UserSnowflake.fromId(userId)).queue(
                v -> PanelService.reply(event, EmbedUtil.success("SUBJECT REHABILITATED", "Access permits restored for `" + userId + "`.")),
                e -> PanelService.replyEphemeral(event, EmbedUtil.error("REHABILITATION FAILED", e.getMessage())));
    }

    private void handleKick(SlashCommandInteractionEvent event) {
        Member target = event.getOption("member", OptionMapping::getAsMember);
        String reason = event.getOption("reason", "No reason provided", OptionMapping::getAsString);
        if (target == null) { PanelService.replyEphemeral(event, EmbedUtil.error("MODERATION ERROR", "Target subject not located.")); return; }
        
        event.getGuild().kick(target).reason(reason).queue(
                v -> PanelService.reply(event, EmbedUtil.success("SUBJECT EJECTED", "**" + target.getUser().getName() + "** removed from sector.")),
                e -> PanelService.replyEphemeral(event, EmbedUtil.error("EJECTION FAILED", e.getMessage())));
    }

    private void handleMute(SlashCommandInteractionEvent event) {
        Member target = event.getOption("member", OptionMapping::getAsMember);
        int mins = event.getOption("duration", 10, OptionMapping::getAsInt);
        if (target == null) { PanelService.replyEphemeral(event, EmbedUtil.error("MODERATION ERROR", "Subject not located.")); return; }
        target.timeoutFor(Duration.ofMinutes(mins)).queue(
                v -> PanelService.reply(event, EmbedUtil.success("TRANSISSION SILENCED", "**" + target.getUser().getName() + "** placed in timeout for " + mins + "m.")),
                e -> PanelService.replyEphemeral(event, EmbedUtil.error("SILENCING FAILED", e.getMessage())));
    }

    private void handleUnmute(SlashCommandInteractionEvent event) {
        Member target = event.getOption("member", OptionMapping::getAsMember);
        if (target == null) { PanelService.replyEphemeral(event, EmbedUtil.error("DATA ERROR", "Subject not located.")); return; }
        target.removeTimeout().queue(
                v -> PanelService.reply(event, EmbedUtil.success("TRANSMISSION RESTORED", "**" + target.getUser().getName() + "** protocols re-enabled.")),
                e -> PanelService.replyEphemeral(event, EmbedUtil.error("RESTORATION FAILED", e.getMessage())));
    }

    private void handleClear(SlashCommandInteractionEvent event) {
        int amount = event.getOption("amount", 10, OptionMapping::getAsInt);
        if (amount < 1 || amount > 100) { PanelService.replyEphemeral(event, EmbedUtil.error("PROTOCOL WARNING", "Purge density 1-100.")); return; }
        event.deferReply(true).queue();
        event.getChannel().asTextChannel().getHistory().retrievePast(amount).queue(messages -> {
            event.getChannel().asTextChannel().purgeMessages(messages);
            PanelService.reply(event, EmbedUtil.success("NODE PURGED", "**" + messages.size() + "** data blocks deleted."));
        });
    }

    private void handleLock(SlashCommandInteractionEvent event, boolean lock) {
        GuildChannel ch = event.getOption("channel") != null ? event.getOption("channel", OptionMapping::getAsChannel) : (GuildChannel)event.getChannel();
        PermissionOverrideAction action = ch.getPermissionContainer().upsertPermissionOverride(event.getGuild().getPublicRole());
        if (lock) action.deny(Permission.MESSAGE_SEND); else action.clear(Permission.MESSAGE_SEND);
        action.queue(
                v -> PanelService.replyEphemeral(event, EmbedUtil.success(lock ? "NODE SECURED" : "NODE ACCESSIBLE", ch.getAsMention() + (lock ? " locked." : " unlocked."))),
                e -> PanelService.replyEphemeral(event, EmbedUtil.error("SECURITY FAILED", e.getMessage())));
    }

    private boolean isAdmin(Member m) { return m != null && m.getRoles().stream().anyMatch(r -> Config.getAdminRoles().contains(r.getId())); }
}
