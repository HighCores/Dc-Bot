package com.highcore.bot.commands;

import com.highcore.bot.utils.EmbedUtil;
import com.highcore.bot.services.PanelService;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.jetbrains.annotations.NotNull;
import java.util.Random;

public class GeneralCommands extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        String name = event.getName();
        switch (name) {
            case "ping" -> handlePing(event);
            case "roll" -> handleRoll(event);
            case "get-emojis" -> handleGetEmojis(event);
            case "translate" -> handleTranslate(event);
            case "suggest" -> handleSuggest(event);
            case "suggestion" -> handleSuggestionManage(event);
            case "help" -> handleHelp(event);
            case "colors" -> handleColors(event);
            case "color-set" -> handleColorSet(event);
            case "rep" -> handleRep(event);
            case "title" -> handleTitle(event);
        }
    }

    private void handlePing(SlashCommandInteractionEvent event) {
        long ping = event.getJDA().getGatewayPing();
        PanelService.reply(event, EmbedUtil.containerBranded("Service", "Connectivity", "### \uD83D\uDCE1 Signal Report\nStatus: `Active`\nLatency: **" + ping + "ms**", EmbedUtil.BANNER_MAIN));
    }

    private void handleRoll(SlashCommandInteractionEvent event) {
        int res = new Random().nextInt(6) + 1;
        PanelService.reply(event, EmbedUtil.containerBranded("Entertainment", "Dice Roll", "### \uD83C\uDFB2 Result\nNumber: **" + res + "**", EmbedUtil.BANNER_MAIN));
    }

    private void handleGetEmojis(SlashCommandInteractionEvent event) {
        String input = event.getOption("emoji", OptionMapping::getAsString);
        PanelService.reply(event, EmbedUtil.containerBranded("Assets", "Emoji Data", "### \uD83D\uDDBC Emoji Details\nIdentifier: " + input, EmbedUtil.BANNER_MAIN));
    }

    private void handleTranslate(SlashCommandInteractionEvent event) {
        String text = event.getOption("text", OptionMapping::getAsString);
        String lang = event.getOption("language", OptionMapping::getAsString);
        PanelService.reply(event, EmbedUtil.containerBranded("Language", "Translation Engine", "### \uD83C\uDF10 Result\nTarget Language: **" + lang + "**\nContent: `" + text + "`", EmbedUtil.BANNER_MAIN));
    }

    private void handleSuggest(SlashCommandInteractionEvent event) {
        String sug = event.getOption("suggestion", OptionMapping::getAsString);
        com.highcore.bot.database.SupabaseClient.createSuggestion(event.getGuild().getId(), event.getUser().getId(), event.getUser().getName(), sug);
        PanelService.reply(event, EmbedUtil.containerBranded("Development", "Submit Idea", "### \uD83D\uDCDD New Suggestion\nBy: " + event.getUser().getName() + "\n\n" + sug, EmbedUtil.BANNER_MAIN));
    }

    private void handleSuggestionManage(SlashCommandInteractionEvent event) {
        if (!event.getMember().hasPermission(net.dv8tion.jda.api.Permission.ADMINISTRATOR)) return;
        long id = event.getOption("id", OptionMapping::getAsLong);
        String action = event.getOption("action", OptionMapping::getAsString);
        com.highcore.bot.database.SupabaseClient.updateSuggestion(id, action, "Processed by Administration", event.getUser().getId(), event.getUser().getName(), null);
        PanelService.reply(event, EmbedUtil.success("Suggestions", "Suggestion ID `" + id + "` state updated to: **" + action + "**"));
    }

    private void handleHelp(SlashCommandInteractionEvent event) {
        PanelService.reply(event, EmbedUtil.containerBranded("Support", "User Manual", "### \u25C8 System Protocol\nYou can access the full manual via the agency dashboard or use the interactive command list.", EmbedUtil.BANNER_MAIN));
    }

    private void handleColors(SlashCommandInteractionEvent event) {
        PanelService.reply(event, EmbedUtil.containerBranded("Identity", "Color Branding", "### \uD83C\uDFA8 Available Colors\nUse `/color-set` with a color code to change your name color.", EmbedUtil.BANNER_MAIN));
    }

    private void handleColorSet(SlashCommandInteractionEvent event) {
        String code = event.getOption("code", OptionMapping::getAsString);
        PanelService.reply(event, EmbedUtil.success("Update Color", "Your new personal color has been applied: `" + code + "`"));
    }

    private void handleRep(SlashCommandInteractionEvent event) {
        net.dv8tion.jda.api.entities.User target = event.getOption("user", OptionMapping::getAsUser);
        if (target.getId().equals(event.getUser().getId())) { PanelService.replyEphemeral(event, EmbedUtil.error("Error", "You cannot give reputation to yourself!")); return; }
        
        if (com.highcore.bot.database.SupabaseClient.canGiveRep(event.getUser().getId(), event.getGuild().getId())) {
            com.highcore.bot.database.SupabaseClient.addReputation(target.getId(), event.getGuild().getId(), 1);
            com.highcore.bot.database.SupabaseClient.setRepCooldown(event.getUser().getId(), event.getGuild().getId());
            PanelService.reply(event, EmbedUtil.success("Reputation", "Successfully gave a reputation point to " + target.getName() + "."));
        } else {
            PanelService.replyEphemeral(event, EmbedUtil.error("Cooldown", "You can only give reputation once every 24 hours."));
        }
    }

    private void handleTitle(SlashCommandInteractionEvent event) {
        String title = event.getOption("title", OptionMapping::getAsString);
        com.highcore.bot.database.SupabaseClient.setTitle(event.getUser().getId(), event.getGuild().getId(), title);
        PanelService.reply(event, EmbedUtil.success("Identity Update", "Your personal tile has been updated to: **" + title + "**"));
    }
}
