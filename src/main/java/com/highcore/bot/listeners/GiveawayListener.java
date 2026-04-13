package com.highcore.bot.listeners;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.highcore.bot.database.SupabaseClient;
import com.highcore.bot.services.GiveawayService;
import com.highcore.bot.utils.EmbedUtil;
import com.highcore.bot.commands.GiveawayCommands;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GiveawayListener extends ListenerAdapter {
    private static final Logger log = LoggerFactory.getLogger(GiveawayListener.class);

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String id = event.getComponentId();

        if (id.startsWith("gw_enter_")) {
            handleEntry(event);
        } else if (id.startsWith("gw_count_")) {
            handleCount(event);
        }
    }

    private void handleEntry(ButtonInteractionEvent event) {
        String idStr = event.getComponentId().replace("gw_enter_", "");
        long giveawayId;
        try { giveawayId = Long.parseLong(idStr); } catch (Exception e) { return; }

        JsonObject g = SupabaseClient.getGiveawayById(giveawayId);
        if (g == null || g.get("ended").getAsBoolean()) {
            event.reply("\u274C This giveaway has ended!").setEphemeral(true).queue();
            return;
        }

        if (SupabaseClient.hasEnteredGiveaway(giveawayId, event.getUser().getId())) {
            event.reply("\u2705 You've already entered! Good luck! \uD83C\uDF40").setEphemeral(true).queue();
            return;
        }

        String prizeType = g.has("prize_type") ? g.get("prize_type").getAsString() : "";
        boolean isDrop = prizeType.equalsIgnoreCase("drop");
        
        SupabaseClient.addGiveawayEntry(giveawayId, event.getUser().getId());

        if (isDrop) {
            event.reply("\uD83D\uDCA8 You claimed it! You won!").setEphemeral(true).queue();
            GiveawayService.endGiveaway(event.getJDA(), giveawayId, 1);
            return;
        }

        JsonArray entries = SupabaseClient.getGiveawayEntries(giveawayId);
        int count = entries != null ? entries.size() : 1;

        try {
            event.editComponents(ActionRow.of(
                    Button.success("gw_enter_" + giveawayId, "\uD83C\uDF89 Enter Giveaway"),
                    Button.secondary("gw_count_" + giveawayId, count + " entries")
            )).queue();
        } catch (Exception e) { /* ignore */ }

        event.reply("\u2705 You've entered the giveaway! Good luck! \uD83C\uDF40").setEphemeral(true).queue();
        
        // Update the live dashboard!
        String dashMsgId = GiveawayCommands.dashboardMessages.get(giveawayId);
        String dashChId = GiveawayCommands.dashboardChannels.get(giveawayId);
        
        if (dashMsgId != null && dashChId != null) {
            TextChannel dashCh = event.getGuild().getTextChannelById(dashChId);
            if (dashCh != null) {
                String prizeDetail = g.has("prize_details") ? g.get("prize_details").getAsString() : "Unknown Prize";
                String dashDesc = "### Dashboard: " + prizeDetail + "\n**Status:** Active\n**Participants:** " + count;
                var dashC = EmbedUtil.containerBranded("GIVEAWAY MONITOR", "Real-time Tracking", dashDesc, EmbedUtil.BANNER_GIVEAWAY);
                
                dashCh.editMessageComponentsById(dashMsgId, dashC).queue(null, ex -> {});
            }
        }
    }

    private void handleCount(ButtonInteractionEvent event) {
        String idStr = event.getComponentId().replace("gw_count_", "");
        long giveawayId;
        try { giveawayId = Long.parseLong(idStr); } catch (Exception e) { return; }

        JsonArray entries = SupabaseClient.getGiveawayEntries(giveawayId);
        int count = entries != null ? entries.size() : 0;
        event.reply("\uD83D\uDCCA This giveaway has **" + count + "** entries so far!").setEphemeral(true).queue();
    }
}
