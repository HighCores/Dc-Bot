package com.highcore.bot.listeners;

import com.highcore.bot.config.Config;
import com.highcore.bot.services.LogManager;
import com.highcore.bot.utils.EmbedUtil;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import org.jetbrains.annotations.NotNull;

import java.util.stream.Collectors;

public class CommandLogListener extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        String cmdName = event.getName();
        String options = event.getOptions().stream()
                .map(opt -> opt.getName() + ": " + getOptionValue(opt))
                .collect(Collectors.joining("\n"));

        String details = "### 📡 Command Execution Detected\n" +
                "\u25AB\uFE0F **Command:** `/" + cmdName + "`\n" +
                "\u25AB\uFE0F **Channel:** " + event.getChannel().getAsMention() + "\n" +
                "\u25AB\uFE0F **Parameters:**\n" + (options.isEmpty() ? "`None`" : "```\n" + options + "\n```");

        MessageEmbed logEmbed = EmbedUtil.createOldLogEmbed("command-intercept", details, event.getMember(), null, null, EmbedUtil.INFO);
        LogManager.logEmbed(event.getGuild(), Config.LOG_COMMANDS, logEmbed);
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        String details = "### 🔘 Interface Interaction: Button\n" +
                "\u25AB\uFE0F **Identifier:** `" + event.getComponentId() + "`\n" +
                "\u25AB\uFE0F **Label:** `" + event.getComponent().getLabel() + "`\n" +
                "\u25AB\uFE0F **Channel:** " + event.getChannel().getAsMention() + "\n" +
                "\u25AB\uFE0F **Status:** `ACTION_EXECUTED`";

        MessageEmbed logEmbed = EmbedUtil.createOldLogEmbed("button-intercept", details, event.getMember(), null, null, EmbedUtil.INFO);
        LogManager.logEmbed(event.getGuild(), Config.LOG_COMMANDS, logEmbed);
    }

    @Override
    public void onStringSelectInteraction(@NotNull StringSelectInteractionEvent event) {
        String values = String.join(", ", event.getValues());
        String details = "### 📑 Interface Interaction: Menu\n" +
                "\u25AB\uFE0F **Identifier:** `" + event.getComponentId() + "`\n" +
                "\u25AB\uFE0F **Selection:** `" + values + "`\n" +
                "\u25AB\uFE0F **Channel:** " + event.getChannel().getAsMention() + "\n" +
                "\u25AB\uFE0F **Status:** `DATA_SUBMITTED`";

        MessageEmbed logEmbed = EmbedUtil.createOldLogEmbed("menu-intercept", details, event.getMember(), null, null, EmbedUtil.INFO);
        LogManager.logEmbed(event.getGuild(), Config.LOG_COMMANDS, logEmbed);
    }

    private String getOptionValue(OptionMapping opt) {
        if (opt.getType() == OptionType.USER) return opt.getAsUser().getName();
        if (opt.getType() == OptionType.ROLE) return opt.getAsRole().getName();
        if (opt.getType() == OptionType.CHANNEL) return opt.getAsChannel().getName();
        return opt.getAsString();
    }
}
