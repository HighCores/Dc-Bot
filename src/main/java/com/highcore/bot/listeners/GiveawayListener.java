package com.highcore.bot.listeners;

import com.google.gson.JsonObject;
import com.highcore.bot.database.SupabaseClient;
import com.highcore.bot.services.GiveawayService;
import com.highcore.bot.services.PanelService;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import org.jetbrains.annotations.NotNull;

public class GiveawayListener extends ListenerAdapter {

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        String id = event.getComponentId();

        if (id.startsWith("gw_enter_")) {
            handleEntry(event);
        } else if (id.startsWith("gw_end_early_")) {
             handleEndEarly(event);
        } else if (id.startsWith("gw_reroll_adm_")) {
             handleReroll(event);
        }
    }

    private void handleEntry(ButtonInteractionEvent event) {
        String idStr = event.getComponentId().replace("gw_enter_", "");
        long giveawayId;
        try { giveawayId = Long.parseLong(idStr); } catch (Exception e) { return; }

        event.deferEdit().queue();

        JsonObject g = SupabaseClient.getGiveawayById(giveawayId);
        if (g == null || g.get("ended").getAsBoolean()) {
            PanelService.replyEphemeral(event, "This giveaway has already ended.");
            return;
        }

        String userId = event.getUser().getId();
        if (SupabaseClient.hasEnteredGiveaway(giveawayId, userId)) {
            PanelService.replyEphemeral(event, "You are already registered for this giveaway.");
            return;
        }

        SupabaseClient.addGiveawayEntry(giveawayId, userId);
        PanelService.replyEphemeral(event, "✅ Entry Registered! Good luck.");
    }

    private void handleEndEarly(ButtonInteractionEvent event) {
        String idStr = event.getComponentId().replace("gw_end_early_", "");
        long giveawayId;
        try { giveawayId = Long.parseLong(idStr); } catch (Exception e) { return; }

        if (!com.highcore.bot.config.Config.isAdmin(event.getMember())) {
            PanelService.replyEphemeral(event, "Authorization required for this operation.");
            return;
        }

        JsonObject g = SupabaseClient.getGiveawayById(giveawayId);
        if (g == null) return;
        int winCount = g.has("winner_count") ? g.get("winner_count").getAsInt() : 1;

        GiveawayService.endGiveaway(event.getJDA(), giveawayId, winCount);
        PanelService.replyEphemeral(event, "Success! Giveaway ended manually with " + winCount + " winner slots.");
    }

    private void handleReroll(ButtonInteractionEvent event) {
        String idStr = event.getComponentId().replace("gw_reroll_adm_", "");
        long giveawayId;
        try { giveawayId = Long.parseLong(idStr); } catch (Exception e) { return; }

        if (!com.highcore.bot.config.Config.isAdmin(event.getMember())) {
            PanelService.replyEphemeral(event, "Authorization required for this operation.");
            return;
        }

        GiveawayService.rerollGiveaway(event.getJDA(), giveawayId);
        PanelService.replyEphemeral(event, "🔄 Reroll initiated for the selected giveaway.");
    }
}
