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
            case "translate" -> handleTranslate(event);
            case "title" -> handleTitle(event);
        }
    }

    private void handlePing(SlashCommandInteractionEvent event) {
        long ping = event.getJDA().getGatewayPing();
        String body = String.format("""
                ### 📡 CONNECTIVITY REPORT
                **Status:** `Operational`
                **Latency:** **%d**ms
                """, ping);
        PanelService.reply(event, EmbedUtil.containerBranded("SYSTEM", "Network Latency", body, EmbedUtil.BANNER_MAIN));
    }

    private void handleRoll(SlashCommandInteractionEvent event) {
        int res = new Random().nextInt(6) + 1;
        String body = String.format("""
                ### 🎲 RANDOM ENTROPY GENERATION
                **Extracted Result:** **%d**

                *Number generated successfully within standard industrial parameters*
                """, res);
        PanelService.reply(event,
                EmbedUtil.containerBranded("ENTERTAINMENT", "Dice Module", body, EmbedUtil.BANNER_MAIN));
    }

    private void handleTranslate(SlashCommandInteractionEvent event) {
        String text = event.getOption("text", OptionMapping::getAsString);
        String lang = event.getOption("language", OptionMapping::getAsString);
        String result = com.highcore.bot.services.AIService.translate(text, lang);
        PanelService.reply(event, EmbedUtil.containerBranded("Language", "Translation Engine",
                "### \uD83C\uDF10 Result\nTarget Language: **" + lang + "**\n\n" + result, EmbedUtil.BANNER_MAIN));
    }

    private void handleTitle(SlashCommandInteractionEvent event) {
        String title = event.getOption("title", OptionMapping::getAsString);
        boolean isReset = title == null || title.equalsIgnoreCase("none") || title.equalsIgnoreCase("reset")
                || title.trim().isEmpty();

        if (isReset) {
            com.highcore.bot.database.SupabaseClient.setTitle(event.getUser().getId(), event.getGuild().getId(), null);
        } else {
            com.highcore.bot.database.SupabaseClient.setTitle(event.getUser().getId(), event.getGuild().getId(), title);
        }

        // Refined Nickname Sync: Clean old [Title] tags if they exist
        String currentName = event.getMember().getEffectiveName();
        // Regex matches anything inside starting brackets: e.g. [ADMIN] Name -> Name
        String baseName = currentName.replaceAll("^\\[.*?\\]\\s*", "");

        final String finalNick;
        if (isReset) {
            finalNick = baseName;
        } else {
            String candidate = "[" + title + "] " + baseName;
            if (candidate.length() > 32) {
                // If too long, try to truncate title or just use base
                candidate = candidate.substring(0, 32);
            }
            finalNick = candidate;
        }

        event.getMember().modifyNickname(finalNick).queue(
                success -> PanelService.reply(event,
                        EmbedUtil.success("Identity Update",
                                isReset ? "Identity registry cleared. Your name has been reset."
                                        : "Identity synchronized. Your title is now set to: **" + title + "**")),
                error -> PanelService.reply(event, EmbedUtil.success("Identity Update",
                        "Registry update finalized. *Note: Nickname synchronization failed due to hierarchy restrictions.*")));
        
        com.highcore.bot.services.LogManager.logEmbed(event.getGuild(), com.highcore.bot.config.Config.LOG_COMMANDS, 
            EmbedUtil.createOldLogEmbed("title", "Action: " + (isReset ? "Identity Cleared" : "Title Synchronized") + "\nNew Title: " + (title == null ? "None" : title) + "\nChannel: " + event.getChannel().getAsMention(), event.getMember(), null, null, EmbedUtil.INFO));
    }
}
