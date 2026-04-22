package com.highcore.bot.listeners;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.highcore.bot.commands.GiveawayCommands;
import com.highcore.bot.database.SupabaseClient;
import com.highcore.bot.services.GiveawayService;
import com.highcore.bot.utils.EmbedUtil;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
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
        }
    }

    private void handleEntry(ButtonInteractionEvent event) {
        String idStr = event.getComponentId().replace("gw_enter_", "");
        long giveawayId;
        try { giveawayId = Long.parseLong(idStr); } catch (Exception e) { return; }

        event.deferEdit().queue(); // Immediate acknowledgment

        JsonObject g = SupabaseClient.getGiveawayById(giveawayId);
        if (g == null || g.get("ended").getAsBoolean()) {
            event.getHook().sendMessage("This giveaway operation has concluded.").setEphemeral(true).queue();
            return;
        }

        if (SupabaseClient.hasEnteredGiveaway(giveawayId, event.getUser().getId())) {
            event.getHook().sendMessage("Your entry is already synchronized.").setEphemeral(true).queue();
            return;
        }

        String prizeType = g.has("prize_type") ? g.get("prize_type").getAsString() : "";
        boolean isDrop = prizeType.equalsIgnoreCase("drop");
        
        SupabaseClient.addGiveawayEntry(giveawayId, event.getUser().getId());

        if (isDrop) {
            GiveawayService.endGiveaway(event.getJDA(), giveawayId, 1);
            event.getHook().sendMessage("Target Optimized: You have successfully claimed the priority reward!").setEphemeral(true).queue();
            return;
        }

        JsonArray entries = SupabaseClient.getGiveawayEntries(giveawayId);
        int count = (entries != null) ? entries.size() : 1;

        String prize = g.has("prize_details") ? g.get("prize_details").getAsString() : "Classified Item";
        int winCount = g.has("winner_count") ? g.get("winner_count").getAsInt() : 1;
        String endsStr = g.has("ends_at") ? g.get("ends_at").getAsString() : "";
        long endsTs = !endsStr.isEmpty() ? java.time.Instant.parse(endsStr).getEpochSecond() : 0;

        Button joinBtn = Button.primary("gw_enter_" + giveawayId, isDrop ? "Claim Instant Prize" : "Join Sweepstakes")
            .withEmoji(net.dv8tion.jda.api.entities.emoji.Emoji.fromUnicode(isDrop ? "\uD83D\uDCA8" : "\uD83C\uDF89"));
        Button countBtn = Button.secondary("gw_count_" + giveawayId, count + (count == 1 ? " entry" : " entries")).asDisabled();

        String body;
        if (isDrop) {
            body = "### \uD83D\uDCA8 Instant Priority Drop\nA high-priority prize is available for the fastest member to claim.\n\n\u25AB\uFE0F **Prize:** "
                    + prize + "\n\u25AB\uFE0F **Winners:** " + winCount + "\n\nClick claim below to win!";
        } else {
            body = "### \uD83C\uDF81 Active Sweepstakes\nA new reward opportunity is now available for all members.\n\n\u25AB\uFE0F **Prize:** "
                    + prize + "\n\u25AB\uFE0F **Winners:** **" + winCount + "**\n\u25AB\uFE0F **Ends In:** <t:"
                    + endsTs + ":R>";
        }

        var gwC = EmbedUtil.containerBranded("GIVEAWAY", isDrop ? "Instant Prize" : "Active Rewards", body, EmbedUtil.getDynamicBanner(prize), ActionRow.of(joinBtn, countBtn));
        java.util.List<net.dv8tion.jda.api.components.MessageTopLevelComponent> gwComps = new java.util.ArrayList<>();
        gwComps.add(net.dv8tion.jda.api.components.textdisplay.TextDisplay.of("<@&1488916921687736421>"));
        gwComps.add(gwC);

        event.getMessage().editMessageComponents(gwComps).useComponentsV2(true).queue(null, err -> {});
        event.getHook().sendMessage("Registry updated. You have successfully entered the giveaway!").setEphemeral(true).queue();
        
        // Sync Dashboard
        String dashMsgId = GiveawayCommands.dashboardMessages.get(giveawayId);
        String dashChId = GiveawayCommands.dashboardChannels.get(giveawayId);
        if (dashMsgId != null && dashChId != null) {
            TextChannel dashCh = event.getGuild().getTextChannelById(dashChId);
            if (dashCh != null) {
                String dashDesc = "### " + prize + " | Live Status\n▫️ **Status:** Currently Active\n▫️ **Users Joined:** " + count + " members";
                var dashC = EmbedUtil.containerBranded("GIVEAWAY DASHBOARD", "Live Status", dashDesc, EmbedUtil.BANNER_GIVEAWAY, 
                        ActionRow.of(Button.secondary("gw_end_early_" + giveawayId, "End Early"), Button.secondary("gw_reroll_adm_" + giveawayId, "Reroll Winners")));
                dashCh.editMessageComponentsById(dashMsgId, dashC).useComponentsV2(true).queue(null, ex -> {});
            }
        }
    }

    private void handleEndEarly(ButtonInteractionEvent event) {
        String idStr = event.getComponentId().replace("gw_end_early_", "");
        long giveawayId;
        try { giveawayId = Long.parseLong(idStr); } catch (Exception e) { return; }

        if (!com.highcore.bot.config.Config.isAdmin(event.getMember())) {
            event.reply("Authorization required for this operation.").setEphemeral(true).queue();
            return;
        }

        event.deferEdit().queue();
        GiveawayService.endGiveaway(event.getJDA(), giveawayId, 1);
        event.getHook().sendMessage("Manual termination successful. Winners are being processed.").setEphemeral(true).queue();
    }
}
