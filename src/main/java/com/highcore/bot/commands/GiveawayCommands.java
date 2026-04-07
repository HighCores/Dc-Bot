package com.highcore.bot.commands;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.highcore.bot.config.Config;
import com.highcore.bot.database.SupabaseClient;
import com.highcore.bot.services.GiveawayService;
import com.highcore.bot.services.PanelService;
import com.highcore.bot.utils.EmbedUtil;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.modals.Modal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class GiveawayCommands extends ListenerAdapter {
    private static final Logger log = LoggerFactory.getLogger(GiveawayCommands.class);

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!isAdmin(event.getMember())) { PanelService.replyEphemeral(event, EmbedUtil.accessDenied()); return; }
        try {
            switch (event.getName()) {
                case "giveaway-start" -> handleStart(event);
                case "giveaway-end" -> handleEnd(event);
                case "giveaway-reroll" -> handleReroll(event);
                case "giveaway-list" -> handleList(event);
                case "giveaway-drop" -> handleDrop(event);
                case "giveaway-panel" -> handlePanel(event);
                case "giveaway" -> handlePanel(event);
            }
        } catch (Exception e) {
            log.error("Error executing GiveawayCommands: ", e);
            if (!event.isAcknowledged()) {
                PanelService.replyEphemeral(event, EmbedUtil.error("SYSTEM ERROR", "Execution failed: " + e.getMessage()));
            }
        }
    }

    private void handlePanel(SlashCommandInteractionEvent event) {
        PanelService.reply(event, EmbedUtil.giveawayPanel(), ActionRow.of(
                        Button.success("gw_create", "\u2795 Create Giveaway"),
                        Button.secondary("gw_list", "\uD83D\uDCCB List Active")));
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        if (!event.getComponentId().startsWith("gw_")) return;
        if (!isAdmin(event.getMember())) { PanelService.replyEphemeral(event, EmbedUtil.accessDenied()); return; }

        switch (event.getComponentId()) {
            case "gw_create" -> {
                PanelService.replyEphemeral(event, EmbedUtil.info("GIVEAWAY PROTOCOL", "Select prize classification type:"), ActionRow.of(
                                StringSelectMenu.create("gw_type_select")
                                        .setPlaceholder("Choose prize category...")
                                        .addOption("\uD83D\uDCB0 Discount", "discount", "Percentage or fixed discount")
                                        .addOption("\uD83C\uDF9F\uFE0F Coupon", "coupon", "Gift card or coupon code")
                                        .addOption("\uD83C\uDD93 Free Service", "free_service", "A free service offering")
                                        .addOption("\uD83C\uDF81 Custom Prize", "custom", "Any other prize")
                                        .build()));
            }
            case "gw_list" -> {
                JsonArray active = SupabaseClient.getActiveGiveaways();
                if (active == null || active.size() == 0) {
                    PanelService.replyEphemeral(event, EmbedUtil.info("REGISTRY", "No active giveaway sequences detected."));
                    return;
                }
                StringBuilder sb = new StringBuilder();
                for (var el : active) {
                    JsonObject g = el.getAsJsonObject();
                    sb.append("`#").append(g.get("id").getAsLong()).append("` \u2014 ")
                            .append(g.has("prize_details") ? g.get("prize_details").getAsString() : "Classified")
                            .append(" \u2014 <#").append(g.get("channel_id").getAsString()).append(">\n");
                }
                PanelService.replyEphemeral(event, EmbedUtil.containerBranded("GIVEAWAY AUDIT", "Active Sequences", "### \uD83C\uDF89 Operational Nodes\n" + sb, EmbedUtil.BANNER_GIVEAWAY));
            }
        }
    }

    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent event) {
        if (!event.getComponentId().equals("gw_type_select")) return;
        String type = event.getValues().get(0);

        TextInput channel = TextInput.create("gw_channel", TextInputStyle.SHORT)
                .setLabel("Target Node ID")
                .setPlaceholder("Channel ID (Node Designation)").setRequired(true).build();
        TextInput duration = TextInput.create("gw_duration", TextInputStyle.SHORT)
                .setLabel("Duration (Minutes)")
                .setPlaceholder("Duration in minutes (e.g. 1440 = 1 day)").setRequired(true).build();
        TextInput winners = TextInput.create("gw_winners", TextInputStyle.SHORT)
                .setLabel("Winner Quantity")
                .setPlaceholder("Winner quantity").setRequired(true).setValue("1").build();

        switch (type) {
            case "discount" -> {
                TextInput details = TextInput.create("gw_details", TextInputStyle.PARAGRAPH)
                        .setLabel("Prize Telemetry")
                        .setPlaceholder("e.g. 30% off Web Development, up to $100").setRequired(true).build();
                event.replyModal(Modal.create("gw_modal_discount", "DISCOUNT SEQUENCE CONFIG")
                        .addComponents(ActionRow.of(details), ActionRow.of(channel), 
                                ActionRow.of(duration), ActionRow.of(winners)).build()).queue();
            }
            case "coupon" -> {
                TextInput details = TextInput.create("gw_details", TextInputStyle.PARAGRAPH)
                        .setLabel("Details")
                        .setPlaceholder("e.g. $50 USD coupon, expires Dec 2025").setRequired(true).build();
                event.replyModal(Modal.create("gw_modal_coupon", "COUPON SEQUENCE CONFIG")
                        .addComponents(ActionRow.of(details), ActionRow.of(channel), 
                                ActionRow.of(duration), ActionRow.of(winners)).build()).queue();
            }
            case "free_service" -> {
                TextInput details = TextInput.create("gw_details", TextInputStyle.PARAGRAPH)
                        .setLabel("Service Telemetry")
                        .setPlaceholder("e.g. Free Discord Bot setup (worth $200)").setRequired(true).build();
                event.replyModal(Modal.create("gw_modal_free_service", "SERVICE SEQUENCE CONFIG")
                        .addComponents(ActionRow.of(details), ActionRow.of(channel), 
                                ActionRow.of(duration), ActionRow.of(winners)).build()).queue();
            }
            case "custom" -> {
                TextInput details = TextInput.create("gw_details", TextInputStyle.PARAGRAPH)
                        .setLabel("Prize Telemetry")
                        .setPlaceholder("Describe the prize...").setRequired(true).build();
                event.replyModal(Modal.create("gw_modal_custom", "CUSTOM SEQUENCE CONFIG")
                        .addComponents(ActionRow.of(details), ActionRow.of(channel), 
                                ActionRow.of(duration), ActionRow.of(winners)).build()).queue();
            }
        }
    }

    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        if (!event.getModalId().startsWith("gw_modal_")) return;
        event.deferReply(true).queue();

        String type = event.getModalId().replace("gw_modal_", "");
        String details = event.getValue("gw_details").getAsString();
        String channelId = event.getValue("gw_channel").getAsString().trim();
        int durationMin, winnerCount;
        try {
            durationMin = Integer.parseInt(event.getValue("gw_duration").getAsString().trim());
            winnerCount = Integer.parseInt(event.getValue("gw_winners").getAsString().trim());
        } catch (NumberFormatException e) {
            PanelService.replyEphemeral(event, EmbedUtil.error("DATA ERROR", "Duration and winners must be numerical values."));
            return;
        }

        TextChannel targetCh = event.getGuild().getTextChannelById(channelId);
        if (targetCh == null) {
            PanelService.replyEphemeral(event, EmbedUtil.error("DATA ERROR", "Target node ID not detected in sector."));
            return;
        }

        String endsAt = Instant.now().plus(durationMin, ChronoUnit.MINUTES).toString();
        String typeEmoji = switch (type) {
            case "discount" -> "\uD83D\uDCB0"; case "coupon" -> "\uD83C\uDF9F\uFE0F";
            case "free_service" -> "\uD83C\uDD93"; default -> "\uD83C\uDF81";
        };
        String typeName = switch (type) {
            case "discount" -> "Discount Allocation"; case "coupon" -> "Coupon Allocation";
            case "free_service" -> "Service Allocation"; default -> "Custom Allocation";
        };

        JsonObject giveaway = SupabaseClient.createGiveaway(channelId, event.getGuild().getId(),
                event.getUser().getId(), event.getUser().getName(), type, details, null, null, null, null, winnerCount, endsAt);

        if (giveaway == null) {
            PanelService.replyEphemeral(event, EmbedUtil.error("DATABASE ERROR", "Failed to initialize giveaway registry entry."));
            return;
        }

        long giveawayId = giveaway.get("id").getAsLong();
        String timeStr = durationMin >= 1440 ? (durationMin / 1440) + " day(s)" : durationMin >= 60 ? (durationMin / 60) + " hour(s)" : durationMin + " min";

        Container c = EmbedUtil.containerBranded("GIVEAWAY INITIALIZED", typeName.toUpperCase(), 
                "## \uD83C\uDF89 GIVEAWAY " + typeEmoji + "\n\n" +
                        "> **" + details + "**\n\n" +
                        "**Winners:** " + winnerCount + "\n" +
                        "**Duration:** " + timeStr + "\n" +
                        "**Hosted by:** " + event.getUser().getAsMention() + "\n\n" +
                        "Click the button below to initialize entry! \uD83C\uDF89", EmbedUtil.BANNER_GIVEAWAY);
        c.withAccentColor(EmbedUtil.GOLD.getRGB() & 0xFFFFFF);

        targetCh.sendMessageComponents(c).useComponentsV2(true).addComponents(ActionRow.of(
                        Button.success("gw_enter_" + giveawayId, "\uD83C\uDF89 Enter Giveaway"),
                        Button.secondary("gw_count_" + giveawayId, "0 entries recorded")))
                .queue(msg -> SupabaseClient.setGiveawayMessageId(giveawayId, msg.getId()));

        PanelService.replyEphemeral(event, EmbedUtil.success("SEQUENCE ACTIVE", "Giveaway **#" + giveawayId + "** deployed to " + targetCh.getAsMention() + "!"));
    }

    private void handleStart(SlashCommandInteractionEvent event) {
        String prize = event.getOption("prize", OptionMapping::getAsString);
        int duration = event.getOption("duration", 60, OptionMapping::getAsInt);
        int winners = event.getOption("winners", 1, OptionMapping::getAsInt);
        TextChannel ch = event.getOption("channel") != null ? event.getOption("channel", OptionMapping::getAsChannel).asTextChannel() : (TextChannel)event.getChannel();

        String endsAt = Instant.now().plus(duration, ChronoUnit.MINUTES).toString();
        JsonObject g = SupabaseClient.createGiveaway(ch.getId(), event.getGuild().getId(),
                event.getUser().getId(), event.getUser().getName(), "custom", prize, null, null, null, null, winners, endsAt);
        if (g == null) { PanelService.replyEphemeral(event, EmbedUtil.error("DATABASE ERROR", "Failed to create registry entry.")); return; }

        long gid = g.get("id").getAsLong();
        String timeStr = duration >= 60 ? (duration / 60) + "h" : duration + "min";
        
        Container c = EmbedUtil.containerBranded("GIVEAWAY DEPLOYED", "Custom Priority", 
                "## \uD83C\uDF89 GIVEAWAY\n\n> **" + prize + "**\n\n**Winners:** " + winners +
                        "\n**Duration:** " + timeStr + "\n**Hosted by:** " + event.getUser().getAsMention() +
                        "\n\nClick to enter! \uD83C\uDF89", EmbedUtil.BANNER_GIVEAWAY);
        c.withAccentColor(EmbedUtil.GOLD.getRGB() & 0xFFFFFF);

        ch.sendMessageComponents(c).useComponentsV2(true).addComponents(ActionRow.of(
                        Button.success("gw_enter_" + gid, "\uD83C\uDF89 Enter"),
                        Button.secondary("gw_count_" + gid, "0 entries recorded")))
                .queue(msg -> SupabaseClient.setGiveawayMessageId(gid, msg.getId()));
        
        PanelService.replyEphemeral(event, EmbedUtil.success("SEQUENCE DEPLOYED", "Action active in " + ch.getAsMention()));
    }

    private void handleEnd(SlashCommandInteractionEvent event) {
        String idStr = event.getOption("id", OptionMapping::getAsString);
        try {
            long id = Long.parseLong(idStr);
            GiveawayService.endGiveaway(event.getJDA(), id, 1);
            PanelService.replyEphemeral(event, EmbedUtil.success("PROTOCOL FINALIZED", "Giveaway **#" + id + "** decommissioning initiated."));
        } catch (Exception e) { PanelService.replyEphemeral(event, EmbedUtil.error("DATA ERROR", "Malformed sequence ID.")); }
    }

    private void handleReroll(SlashCommandInteractionEvent event) {
        String idStr = event.getOption("id", OptionMapping::getAsString);
        try {
            long id = Long.parseLong(idStr);
            GiveawayService.rerollGiveaway(event.getJDA(), id);
            PanelService.replyEphemeral(event, EmbedUtil.success("PROTOCOL REROLLED", "Re-calibrating winners for sequence **#" + id + "**."));
        } catch (Exception e) { PanelService.replyEphemeral(event, EmbedUtil.error("DATA ERROR", "Malformed sequence ID.")); }
    }

    private void handleList(SlashCommandInteractionEvent event) {
        JsonArray active = SupabaseClient.getActiveGiveaways();
        if (active == null || active.size() == 0) { PanelService.replyEphemeral(event, EmbedUtil.info("REGISTRY", "No active giveaway sequences detected.")); return; }
        StringBuilder sb = new StringBuilder();
        for (var el : active) {
            JsonObject g = el.getAsJsonObject();
            sb.append("`#").append(g.get("id").getAsLong()).append("` \u2014 ")
                    .append(g.has("prize_details") ? g.get("prize_details").getAsString() : "Classified")
                    .append(" \u2014 <#").append(g.get("channel_id").getAsString()).append(">\n");
        }
        PanelService.replyEphemeral(event, EmbedUtil.containerBranded("GIVEAWAY AUDIT", "Active Sequences", "### \uD83C\uDF89 Operational Nodes\n" + sb, EmbedUtil.BANNER_GIVEAWAY));
    }

    private void handleDrop(SlashCommandInteractionEvent event) {
        String prize = event.getOption("prize", OptionMapping::getAsString);
        TextChannel ch = event.getOption("channel") != null ? event.getOption("channel", OptionMapping::getAsChannel).asTextChannel() : (TextChannel)event.getChannel();
        String endsAt = Instant.now().plus(1, ChronoUnit.MINUTES).toString();
        JsonObject g = SupabaseClient.createGiveaway(ch.getId(), event.getGuild().getId(),
                event.getUser().getId(), event.getUser().getName(), "drop", prize, null, null, null, null, 1, endsAt);
        if (g == null) { PanelService.replyEphemeral(event, EmbedUtil.error("DATABASE ERROR", "Failed to initialize drop registry.")); return; }
        long gid = g.get("id").getAsLong();
        
        Container c = EmbedUtil.containerBranded("NEURAL DROP", "First Priority Acquisition", 
                "## \uD83D\uDCA8 DROP GIVEAWAY!\n\n> **" + prize + "**\n\n\u26A1 First to click wins! Be fast! Transmission unstable.", EmbedUtil.BANNER_GIVEAWAY);
        c.withAccentColor(EmbedUtil.GOLD.getRGB() & 0xFFFFFF);

        ch.sendMessageComponents(c).useComponentsV2(true).addComponents(ActionRow.of(Button.success("gw_enter_" + gid, "\uD83D\uDCA8 CLAIM!")))
                .queue(msg -> SupabaseClient.setGiveawayMessageId(gid, msg.getId()));
        
        PanelService.replyEphemeral(event, EmbedUtil.success("DROP DEPLOYED", "Acquisition node active in " + ch.getAsMention()));
    }

    private boolean isAdmin(Member m) { return m != null && m.getRoles().stream().anyMatch(r -> Config.getAdminRoles().contains(r.getId())); }
}
