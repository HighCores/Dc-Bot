package com.highcore.bot.commands;

import com.highcore.bot.config.Config;
import com.highcore.bot.services.*;
import com.highcore.bot.utils.EmbedUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.modals.Modal;
import org.jetbrains.annotations.NotNull;
import java.util.HashMap;
import java.util.Map;

public class SlashCommands extends ListenerAdapter {

    public static class BcSession { public String roleId, attUrl; }
    public static final Map<String, BcSession> BC_SESSIONS = new HashMap<>();

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        String name = event.getName().toLowerCase();
        
        // OWNERSHIP-BASED DEFERRAL: Handle Core and Dynamic commands
        java.util.List<String> coreCmds = java.util.Arrays.asList("startup", "tickets", "services", "stats", "bc");
        
        // If it's a core command OR a moderation command (handled elsewhere), we decide deferral
        java.util.List<String> modCmds = java.util.Arrays.asList(
            "setnick", "ban", "unban", "unban-all", "kick", "vkick", "mute-text", "unmute-text",
            "mute-check", "mute-voice", "unmute-voice", "timeout", "untimeout", "clear", "move",
            "role", "role-multiple", "temprole", "rar", "inrole", "warn-add", "warn-remove",
            "warnings", "violations", "violations-clear", "lock", "unlock", "hide", "show", "slowmode", "add-emoji"
        );

        if (modCmds.contains(name)) return; // Let ModerationCommands handle it entirely

        // Defer EVERYTHING else (Core + Dynamic)
        if (!event.isAcknowledged()) {
            boolean ephemeral = !name.equals("startup") && !name.equals("bc");
            event.deferReply(ephemeral).queue();
        }

        switch (name) {
            case "startup" -> { if (isAdmin(event.getMember())) PanelService.sendStartupHub(event); else PanelService.replyEphemeral(event, "Unauthorized Access Detected."); }
            case "tickets" -> PanelService.sendTicketPanel(event);
            case "terms" -> PanelService.replyEphemeral(event, EmbedUtil.termsPanel());
            case "services" -> PanelService.sendServicesCategory(event);
            case "stats" -> PanelService.sendStatsPanel(event);
            case "bc" -> { if (isAdmin(event.getMember())) handleBroadcast(event); else PanelService.replyEphemeral(event, "Unauthorized Access Detected."); }
            default -> CommandService.executeSlash(event);
        }
    }

    private void handleBroadcast(SlashCommandInteractionEvent event) {
        BcSession s = new BcSession();
        if (event.getOption("role") != null) s.roleId = event.getOption("role").getAsRole().getId();
        if (event.getOption("attachment") != null) s.attUrl = event.getOption("attachment").getAsAttachment().getUrl();
        BC_SESSIONS.put("bc_" + event.getUser().getId(), s);
        
        TextInput input = TextInput.create("message", TextInputStyle.PARAGRAPH).build();
                
        event.replyModal(Modal.create("modal_bc", "BROADCAST TRANSMISSION")
                .addComponents(Label.of("Message Content", input)).build()).queue();
    }

    private boolean isAdmin(Member m) { return Config.isAdmin(m); }
}
