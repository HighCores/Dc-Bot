package com.highcore.bot.commands;

import com.highcore.bot.config.Config;
import com.highcore.bot.services.PanelService;
import com.highcore.bot.utils.EmbedUtil;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.modals.Modal;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

public class SlashCommands extends ListenerAdapter {

    public static class BcSession {
        public String roleId;
        public String attUrl;
    }
    public static final Map<String, BcSession> BC_SESSIONS = new HashMap<>();

    public static class BoterSession {
        public String channelId;
        public List<String> fileUrls = new ArrayList<>();
    }
    public static final Map<String, BoterSession> BOTER_SESSIONS = new HashMap<>();

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        String name = event.getName();

        if (name.equals("startup")) {
            if (!Config.isAdmin(event.getMember())) {
                PanelService.replyEphemeral(event, EmbedUtil.accessDenied());
                return;
            }
            PanelService.sendStartupHub(event);
        } else if (name.equals("bc")) {
            if (!Config.isAdmin(event.getMember())) {
                PanelService.replyEphemeral(event, EmbedUtil.accessDenied());
                return;
            }
            BcSession session = new BcSession();
            session.roleId = event.getOption("role") != null ? event.getOption("role").getAsRole().getId() : null;
            session.attUrl = event.getOption("attachment") != null ? event.getOption("attachment").getAsAttachment().getUrl() : null;
            BC_SESSIONS.put("bc_" + event.getUser().getId(), session);

            TextInput msg = TextInput.create("message", TextInputStyle.PARAGRAPH).setPlaceholder("Broadcast Message").setRequired(true).build();
            event.replyModal(Modal.create("modal_bc", "Send Broadcast")
                .addComponents(net.dv8tion.jda.api.components.label.Label.of("Message", msg))
                .build()).queue();
        } else if (name.equals("boter")) {
            if (!Config.isAdmin(event.getMember())) {
                PanelService.replyEphemeral(event, EmbedUtil.accessDenied());
                return;
            }
            BoterSession session = new BoterSession();
            session.channelId = event.getOption("channel").getAsChannel().getId();
            BOTER_SESSIONS.put("boter_" + event.getUser().getId(), session);

            TextInput msg = TextInput.create("message", TextInputStyle.PARAGRAPH).setPlaceholder("Message to send").setRequired(true).build();
            event.replyModal(Modal.create("modal_boter", "Send via Bot")
                .addComponents(net.dv8tion.jda.api.components.label.Label.of("Message", msg))
                .build()).queue();
        }
    }

    public static List<net.dv8tion.jda.api.interactions.commands.build.CommandData> getCommands() {
        return List.of(
            Commands.slash("startup", "Deploy the main startup control panel"),
            Commands.slash("bc", "Broadcast a message to a specific role")
                .addOptions(new OptionData(OptionType.ROLE, "role", "Target role").setRequired(false),
                            new OptionData(OptionType.ATTACHMENT, "attachment", "Optional image").setRequired(false))
                .setDefaultPermissions(DefaultMemberPermissions.DISABLED),
            Commands.slash("boter", "Send a message to a specific channel as the bot")
                .addOptions(new OptionData(OptionType.CHANNEL, "channel", "Target channel").setRequired(true))
                .setDefaultPermissions(DefaultMemberPermissions.DISABLED)
        );
    }
}
