package com.highcore.bot.commands;

import com.highcore.bot.services.PanelService;
import com.highcore.bot.utils.EmbedUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.modals.Modal;
import org.jetbrains.annotations.NotNull;
import java.util.stream.Collectors;

public class InfoCommands extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        String name = event.getName();
        if (name.equals("profile")) {
            handleUser(event);
        } else if (name.equals("ping")) {
            event.reply("Pong! `" + event.getJDA().getGatewayPing() + "ms`").setEphemeral(true).queue();
        }
    }

    private void handleUser(SlashCommandInteractionEvent event) {
        Member m = event.getOption("user") != null ? event.getOption("user").getAsMember() : event.getMember();
        if (m == null) return;
        
        // Use PanelService for interaction safety
        m.getUser().retrieveProfile().queue(profile -> {
            long created = m.getUser().getTimeCreated().toEpochSecond();
            long joined = m.getTimeJoined().toEpochSecond();

            String roles = m.getRoles().stream()
                    .map(net.dv8tion.jda.api.entities.Role::getAsMention)
                    .collect(Collectors.joining(" "));
            if (roles.isEmpty()) roles = "None";

            EmbedBuilder eb = new EmbedBuilder()
                    .setAuthor(m.getUser().getName(), null, m.getUser().getEffectiveAvatarUrl())
                    .setThumbnail(m.getUser().getEffectiveAvatarUrl())
                    .setTitle("Operative Profile")
                    .addField("Identity", m.getUser().getAsMention() + " (`" + m.getUser().getId() + "`)", true)
                    .addField("Status", m.getOnlineStatus().name().toLowerCase(), true)
                    .addField("Joined Agency", "<t:" + joined + ":R>", true)
                    .addField("Registration", "<t:" + created + ":R>", true)
                    .addField("Clearance Roles", roles, false)
                    .setColor(EmbedUtil.ACCENT)
                    .setFooter("UNIFIED TERMINAL ACCESS", null);

            PanelService.reply(event, eb.build());
        });
    }
}
