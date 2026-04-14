package com.highcore.bot.listeners;

import com.highcore.bot.config.Config;
import com.highcore.bot.services.LogManager;
import com.highcore.bot.utils.EmbedUtil;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import org.jetbrains.annotations.NotNull;

import java.util.stream.Collectors;

public class CommandLogListener extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (event.getGuild() == null) return;

        // Skip commands that have their own HIGH-FIDELITY moderation logging
        // to avoid double logging or less detailed logs for critical actions
        java.util.List<String> modCmds = java.util.Arrays.asList(
            "setnick", "ban", "unban", "unban-all", "kick", "vkick", "mute-text", "unmute-text",
            "mute-voice", "unmute-voice", "timeout", "untimeout", "clear", "move",
            "role", "role-multiple", "temprole", "rar", "warn-add", "warn-remove",
            "violations-clear", "lock", "unlock", "hide", "show", "slowmode", "add-emoji"
        );

        // However, we still want to log that the command was ATTEMPTED/RUN in the general logs
        // but we'll use a simplified format if it's a mod command, or a full one if it's general/query.
        
        String options = event.getOptions().stream()
                .map(opt -> opt.getName() + ": `" + getOptionValue(opt) + "`")
                .collect(Collectors.joining("\n"));

        String details = "### 📥 COMMAND EXECUTION LOG\n" +
                "**Command:** `/" + event.getName() + (event.getSubcommandName() != null ? " " + event.getSubcommandName() : "") + "`\n" +
                "**Operator:** " + event.getUser().getAsMention() + " (" + event.getUser().getName() + ")\n" +
                "**Target Channel:** " + event.getChannel().getAsMention() + "\n";
        
        if (!options.isEmpty()) {
            details += "\n▫️ **Parameters Detected:**\n" + options;
        }

        // Always log to the general COMMANDS log
        LogManager.logEmbed(event.getGuild(), Config.LOG_COMMANDS, 
            EmbedUtil.createOldLogEmbed("command-intercept", details, event.getMember(), null, null, EmbedUtil.INFO));

        // If it's a mod command but NOT one of the high-fidelity ones (like a query), 
        // OR if it's an IMPORTANT query like 'warnings', maybe log to MODS log too?
        // Let's stick to the user's request: "literally everything".
    }

    private String getOptionValue(OptionMapping opt) {
        OptionType type = opt.getType();
        if (type == OptionType.USER) return opt.getAsUser().getName();
        if (type == OptionType.CHANNEL) return opt.getAsChannel().getName();
        if (type == OptionType.ROLE) return opt.getAsRole().getName();
        return opt.getAsString();
    }
}
