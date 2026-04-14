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
            case "suggest" -> handleSuggest(event);
            case "suggestion" -> handleSuggestionManage(event);
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

                *Number generated successfully within standard industrial parameters.*
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

    private void handleSuggest(SlashCommandInteractionEvent event) {
        String sug = event.getOption("suggestion", OptionMapping::getAsString);
        com.google.gson.JsonObject result = com.highcore.bot.database.SupabaseClient
                .createSuggestion(event.getGuild().getId(), event.getUser().getId(), event.getUser().getName(), sug);

        long id = result != null && result.has("id") ? result.get("id").getAsLong() : 0;

        net.dv8tion.jda.api.entities.channel.concrete.TextChannel sugChan = event.getGuild().getTextChannels().stream()
                .filter(ch -> ch.getName().toLowerCase().contains("suggest") || ch.getName().contains("اقتراحات"))
                .findFirst().orElse(null);

        if (sugChan != null) {
            String body = String.format("### 💡 NEW SUGGESTION #%d\n**From:** %s\n\n%s", id,
                    event.getUser().getAsMention(), sug);
            net.dv8tion.jda.api.components.container.Container container = EmbedUtil.containerBranded("DEVELOPMENT",
                    "Community Feedback", body, EmbedUtil.BANNER_MAIN);
            net.dv8tion.jda.api.components.actionrow.ActionRow buttons = net.dv8tion.jda.api.components.actionrow.ActionRow
                    .of(
                            net.dv8tion.jda.api.components.buttons.Button.primary("suggest_vote_up_" + id, "👍"),
                            net.dv8tion.jda.api.components.buttons.Button.primary("suggest_vote_down_" + id, "👎"));

            sugChan.sendMessageComponents(container, buttons).useComponentsV2(true).queue(m -> {
                com.highcore.bot.database.SupabaseClient.updateSuggestion(id, "pending", null, null, null, m.getId());
            });
            PanelService.reply(event, EmbedUtil.success("Success",
                    "Your suggestion has been logged as **#" + id + "** and posted in " + sugChan.getAsMention()));
        } else {
            String body = String.format("""
                    ### 📝 SUGGESTION LOGGED
                    **Author:** %s
                    **Status:** `Recorded`

                    **Content:**
                    %s
                    """, event.getUser().getAsMention(), sug);
            PanelService.reply(event,
                    EmbedUtil.containerBranded("SUGGESTION", "Submit Idea", body, EmbedUtil.BANNER_MAIN));
        }
    }

    private void handleSuggestionManage(SlashCommandInteractionEvent event) {
        if (!event.getMember().hasPermission(net.dv8tion.jda.api.Permission.ADMINISTRATOR)) {
            PanelService.replyEphemeral(event, EmbedUtil.accessDenied());
            return;
        }

        OptionMapping idOpt = event.getOption("id");
        if (idOpt == null) {
            PanelService.sendSuggestionList(event);
        } else {
            // If they manually put an ID and optional action
            long id = idOpt.getAsLong();
            OptionMapping actionOpt = event.getOption("action");
            String action = actionOpt != null ? actionOpt.getAsString() : "Accepted";
            com.highcore.bot.database.SupabaseClient.updateSuggestion(id, action, "Quick Process",
                    event.getUser().getId(), event.getUser().getName(), null);
            PanelService.reply(event, EmbedUtil.success("Suggestions",
                    "Suggestion ID `" + id + "` quick-updated to: **" + action + "**"));
        }
    }

    private void handleTitle(SlashCommandInteractionEvent event) {
        String title = event.getOption("title", OptionMapping::getAsString);
        boolean isReset = title == null || title.equalsIgnoreCase("none") || title.equalsIgnoreCase("reset") || title.trim().isEmpty();

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
                                isReset ? "Identity registry cleared. Your name has been reset." :
                                "Identity synchronized. Your title is now set to: **" + title + "**")),
                error -> PanelService.reply(event, EmbedUtil.success("Identity Update",
                        "Registry update finalized. *Note: Nickname synchronization failed due to hierarchy restrictions.*")));
    }
}
