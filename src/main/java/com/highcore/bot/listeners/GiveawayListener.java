package com.highcore.bot.listeners;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.highcore.bot.database.SupabaseClient;
import com.highcore.bot.services.GiveawayService;
import com.highcore.bot.services.PanelService;
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

        // Correctly re-render the Giveaway Container to avoid visual repetition and update count
        String prize = g.has("prize_details") ? g.get("prize_details").getAsString() : "Unknown Prize";
        int winCount = g.has("winner_count") ? g.get("winner_count").getAsInt() : 1;
        String endsStr = g.has("ends_at") ? g.get("ends_at").getAsString() : "";
        long endsTs = !endsStr.isEmpty() ? java.time.Instant.parse(endsStr).getEpochSecond() : 0;

        String body = isDrop ? 
            "### \uD83D\uDCA8 Instant Priority Drop\nA high-priority prize is available for the fastest member to claim.\n\n\u25AB\uFE0F **Prize:** " + prize + "\n\u25AB\uFE0F **Winners:** " + winCount + "\n\nClick claim below to win!" :
            "### \uD83C\uDF81 Active Sweepstakes\nA new reward opportunity is now available for all members.\n\n\u25AB\uFE0F **Prize:** " + prize + "\n\u25AB\uFE0F **Winners:** **" + winCount + "**\n\u25AB\uFE0F **Ends In:** <t:" + endsTs + ":R>";

        Button joinBtn = Button.primary("gw_enter_" + giveawayId, isDrop ? "Claim Instant Prize" : "Join Sweepstakes")
                .withEmoji(net.dv8tion.jda.api.entities.emoji.Emoji.fromUnicode(isDrop ? "\uD83D\uDCA8" : "\uD83C\uDF89"));
        Button countBtn = Button.secondary("gw_count_" + giveawayId, count + " entries");

        var gwC = EmbedUtil.containerBranded("GIVEAWAY", isDrop ? "Instant Prize" : "Active Rewards", body, EmbedUtil.BANNER_GIVEAWAY, ActionRow.of(joinBtn, isDrop ? countBtn.asDisabled() : countBtn));

        PanelService.reply(event, gwC);

        event.getHook().sendMessage("\u2705 You've entered the giveaway! Good luck! \uD83C\uDF40").setEphemeral(true).queue();
        
        // Update the live dashboard!
        String dashMsgId = GiveawayCommands.dashboardMessages.get(giveawayId);
        String dashChId = GiveawayCommands.dashboardChannels.get(giveawayId);
        
        if (dashMsgId != null && dashChId != null) {
            TextChannel dashCh = event.getGuild().getTextChannelById(dashChId);
            if (dashCh != null) {
                String prizeDetail = g.has("prize_details") ? g.get("prize_details").getAsString() : "Unknown Prize";
                String dashDesc = "### " + prizeDetail + " | Live Status\n" +
                        "\u25AB\uFE0F **Status:** Currently Active\n" +
                        "\u25AB\uFE0F **Users Joined:** " + count + " members";
                
                var dashRow = ActionRow.of(
                        Button.danger("gw_end_early_" + giveawayId, "End Early"),
                        Button.success("gw_reroll_adm_" + giveawayId, "Reroll Winners")
                );
                var dashC = EmbedUtil.containerBranded("GIVEAWAY DASHBOARD", "Live Status", dashDesc, EmbedUtil.BANNER_GIVEAWAY, dashRow);
                
                dashCh.editMessageComponentsById(dashMsgId, dashC).useComponentsV2(true).queue(null, ex -> {});
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
