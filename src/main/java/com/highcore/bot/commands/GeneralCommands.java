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
                ### \uD83D\uDCE1 تـقـريـر الـإتـصـال
                **الـحـالـة:** `مـتـصـل`
                **الـتـأخـيـر:** **%d** مـلـي ثـانـيـة
                """, ping);
        PanelService.reply(event, EmbedUtil.containerBranded("\u0627\u0644\u062E\u062F\u0645\u0629", "\u0633\u0631\u0639\u0629 \u0627\u0644\u0627\u0633\u062A\u062C\u0627\u0628\u0629", body, EmbedUtil.BANNER_MAIN));
    }

    private void handleRoll(SlashCommandInteractionEvent event) {
        int res = new Random().nextInt(6) + 1;
        String body = String.format("""
                ### 🎲 تـولـيـد رتـم عـشـوائـي
                **الـنـتـيـجـة الـمـسـتـخـرجـة:** **%d**
                
                *تـم تـولـيـد الـرقـم بـنـجـاح ضـمـن الـنـطـاق الـقـيـاسـي.*
                """, res);
        PanelService.reply(event, EmbedUtil.containerBranded("ENTERTAINMENT", "Dice Module", body, EmbedUtil.BANNER_MAIN));
    }


    private void handleTranslate(SlashCommandInteractionEvent event) {
        String text = event.getOption("text", OptionMapping::getAsString);
        String lang = event.getOption("language", OptionMapping::getAsString);
        String result = com.highcore.bot.services.AIService.translate(text, lang);
        PanelService.reply(event, EmbedUtil.containerBranded("Language", "Translation Engine", "### \uD83C\uDF10 Result\nTarget Language: **" + lang + "**\n\n" + result, EmbedUtil.BANNER_MAIN));
    }

    private void handleSuggest(SlashCommandInteractionEvent event) {
        String sug = event.getOption("suggestion", OptionMapping::getAsString);
        com.highcore.bot.database.SupabaseClient.createSuggestion(event.getGuild().getId(), event.getUser().getId(), event.getUser().getName(), sug);
        PanelService.reply(event, EmbedUtil.containerBranded("Development", "Submit Idea", "### \uD83D\uDCDD New Suggestion\nBy: " + event.getUser().getName() + "\n\n" + sug, EmbedUtil.BANNER_MAIN));
    }

    private void handleSuggestionManage(SlashCommandInteractionEvent event) {
        if (!event.getMember().hasPermission(net.dv8tion.jda.api.Permission.ADMINISTRATOR)) return;
        
        OptionMapping idOpt = event.getOption("id");
        OptionMapping actionOpt = event.getOption("action");

        if (idOpt == null) {
            PanelService.sendSuggestionList(event);
            return;
        }

        long id = idOpt.getAsLong();
        String action = actionOpt != null ? actionOpt.getAsString() : "Accepted";
        com.highcore.bot.database.SupabaseClient.updateSuggestion(id, action, "Processed by Administration", event.getUser().getId(), event.getUser().getName(), null);
        PanelService.reply(event, EmbedUtil.success("Suggestions", "Suggestion ID `" + id + "` state updated to: **" + action + "**"));
    }




    private void handleTitle(SlashCommandInteractionEvent event) {
        String title = event.getOption("title", OptionMapping::getAsString);
        com.highcore.bot.database.SupabaseClient.setTitle(event.getUser().getId(), event.getGuild().getId(), title);
        PanelService.reply(event, EmbedUtil.success("Identity Update", "Your personal tile has been updated to: **" + title + "**"));
    }
}
