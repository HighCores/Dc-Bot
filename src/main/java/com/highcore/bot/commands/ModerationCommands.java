package com.highcore.bot.commands;

import com.highcore.bot.utils.EmbedUtil;
import com.highcore.bot.services.PanelService;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.jetbrains.annotations.NotNull;

public class ModerationCommands extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (event.getName().equals("kick")) handleKick(event);
        else if (event.getName().equals("ban")) handleBan(event);
        else if (event.getName().equals("clear")) handleClear(event);
    }

    private void handleKick(SlashCommandInteractionEvent event) {
        if (!event.getMember().hasPermission(Permission.KICK_MEMBERS)) {
            PanelService.replyEphemeral(event, EmbedUtil.accessDenied());
            return;
        }
        var user = event.getOption("user", OptionMapping::getAsUser);
        var reason = event.getOption("reason") != null ? event.getOption("reason").getAsString() : "No reason provided";
        if (user == null) return;

        event.getGuild().kick(user).reason(reason).queue(v -> 
            PanelService.reply(event, EmbedUtil.success("USER EJECTED", "Target: " + user.getAsMention() + "\nReason: " + reason)));
    }

    private void handleBan(SlashCommandInteractionEvent event) {
        if (!event.getMember().hasPermission(Permission.BAN_MEMBERS)) {
            PanelService.replyEphemeral(event, EmbedUtil.accessDenied());
            return;
        }
        var user = event.getOption("user", OptionMapping::getAsUser);
        var reason = event.getOption("reason") != null ? event.getOption("reason").getAsString() : "No reason provided";
        if (user == null) return;

        event.getGuild().ban(user, 7, java.util.concurrent.TimeUnit.DAYS).reason(reason).queue(v -> 
            PanelService.reply(event, EmbedUtil.success("USER BANNED", "Target: " + user.getAsMention() + "\nReason: " + reason)));
    }

    private void handleClear(SlashCommandInteractionEvent event) {
        if (!event.getMember().hasPermission(Permission.MESSAGE_MANAGE)) {
            PanelService.replyEphemeral(event, EmbedUtil.accessDenied());
            return;
        }
        int amount = event.getOption("amount", OptionMapping::getAsInt);
        if (amount < 1 || amount > 100) {
            PanelService.replyEphemeral(event, EmbedUtil.error("INVALID INPUT", "Specify a range between 1 and 100."));
            return;
        }

        event.getChannel().getIterableHistory().takeAsync(amount).thenAccept(messages -> {
            event.getGuildChannel().deleteMessages(messages).queue(v -> 
                PanelService.replyEphemeral(event, EmbedUtil.success("PURGE COMPLETE", "Sector cleared of **" + amount + "** records.")));
        });
    }
}
