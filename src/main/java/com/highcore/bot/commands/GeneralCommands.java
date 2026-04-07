package com.highcore.bot.commands;

import com.highcore.bot.utils.EmbedUtil;
import com.highcore.bot.services.PanelService;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import org.jetbrains.annotations.NotNull;

public class GeneralCommands extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (event.getName().equals("ping")) handlePing(event);
        else if (event.getName().equals("profile")) handleProfile(event);
        else if (event.getName().equals("server")) handleServer(event);
    }

    private void handlePing(SlashCommandInteractionEvent event) {
        long time = System.currentTimeMillis();
        event.replyComponents(EmbedUtil.info("DIAGNOSTIC", "Terminal Latency: `calculating...`")).setEphemeral(true).queue(v -> {
            long ping = System.currentTimeMillis() - time;
            event.getHook().editOriginalComponents(EmbedUtil.info("DIAGNOSTIC", "Terminal Latency: `" + ping + "ms`")).queue();
        });
    }

    private void handleProfile(SlashCommandInteractionEvent event) {
        var user = event.getOption("member") != null ? event.getOption("member").getAsUser() : event.getUser();
        String body = "### \uD83D\uDCC1 Subject Identity\n" +
                "**Operator:** " + user.getAsMention() + "\n" +
                "**Identifier:** `" + user.getId() + "`\n" +
                "**Registered:** <t:" + (user.getTimeCreated().toEpochSecond()) + ":R>";
        
        PanelService.reply(event, EmbedUtil.containerBranded("IDENTITY", "Accessing Data Node", body, user.getAvatarUrl()));
    }

    private void handleServer(SlashCommandInteractionEvent event) {
        var guild = event.getGuild();
        if (guild == null) return;
        
        String body = "### \uD83C\uDF10 Sector Infrastructure\n" +
                "**Sector Name:** **" + guild.getName() + "**\n" +
                "**Assets Found:** **" + guild.getMemberCount() + "** operatives\n" +
                "**Operational Status:** ACTIVE";
        
        PanelService.reply(event, EmbedUtil.containerBranded("SECTOR", "Infrastructure Report", body, guild.getIconUrl()));
    }

    public static void displayColors(SlashCommandInteractionEvent event) {
        ActionRow row1 = ActionRow.of(
            Button.success("color_emerald", "Emerald Green"),
            Button.primary("color_ocean", "Ocean Blue"),
            Button.secondary("color_royal", "Royal Purple")
        );
        ActionRow row2 = ActionRow.of(
            Button.primary("color_golden", "Golden Yellow"),
            Button.danger("color_rose", "Rose Pink"),
            Button.secondary("color_sunset", "Sunset Orange")
        );
        PanelService.reply(event, EmbedUtil.containerBranded("VISUALS", "Identity Selection", 
            "Select a color to update your appearance. This will toggle existing roles.", EmbedUtil.BANNER_MAIN, null, row1, row2));
    }
}
