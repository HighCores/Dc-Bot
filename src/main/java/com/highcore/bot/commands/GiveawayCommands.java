package com.highcore.bot.commands;

import com.highcore.bot.config.Config;
import com.highcore.bot.services.LogManager;
import com.highcore.bot.services.PanelService;
import com.highcore.bot.utils.EmbedUtil;
import com.highcore.bot.database.SupabaseClient;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.components.selections.EntitySelectMenu;
import net.dv8tion.jda.api.modals.Modal;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.dv8tion.jda.api.entities.emoji.Emoji;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GiveawayCommands extends ListenerAdapter {
    private static final Logger log = LoggerFactory.getLogger(GiveawayCommands.class);

    // Map to hold pending giveaway settings before channel selection
    private static final Map<String, JsonObject> pendingGiveaways = new ConcurrentHashMap<>();

    // Map to hold dashboard message IDs for live updates: giveawayId -> messageId
    public static final Map<Long, String> dashboardMessages = new ConcurrentHashMap<>();
    public static final Map<Long, String> dashboardChannels = new ConcurrentHashMap<>();

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equalsIgnoreCase("giveaway"))
            return;

        if (!Config.isAdmin(event.getMember())) {
            PanelService.reply(event, EmbedUtil.accessDenied());
            return;
        }

        ActionRow row = ActionRow.of(
                Button.secondary("btn_gw_create", "Create Giveaway")
                        .withEmoji(Emoji.fromCustom("CreatGiveaway", 1496974773098516510L, false)),
                Button.secondary("btn_gw_history", "View History")
                        .withEmoji(Emoji.fromCustom("GiveawayRecords", 1496974526557327530L, false)));

        String desc = "Welcome to the **Giveaway Management Hub**.\n" +
                "Easily create rewards, launch instant drops, or review history.\n\n" +
                "\uD83D\uDCDD **Create Giveaway** \u2014 Step-by-step setup.\n" +
                "\uD83D\uDCA8 **Instant Drop** \u2014 Fast 'first-to-claim' prize.\n" +
                "\uD83D\uDDD2 **View History** \u2014 Check active tasks.";

        PanelService.reply(event, EmbedUtil.containerBranded("GIVEAWAY MANAGER", "Setup & History", desc,
                EmbedUtil.BANNER_GIVEAWAY, row));
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String id = event.getComponentId();

        // Fix: DO NOT defer if we plan to show a modal (drop/create)
        // Only defer if we are doing standard replies like history or management
        if (id.equals("btn_gw_history") || id.startsWith("gw_end_early_") || id.startsWith("gw_reroll_adm_")) {
            if (!event.isAcknowledged())
                event.deferReply(true).queue();
        }

        if (id.equals("btn_gw_create") || id.equals("btn_gw_drop")) {
            if (!Config.isAdmin(event.getMember())) {
                PanelService.replyEphemeral(event, EmbedUtil.accessDenied());
                return;
            }

            if (id.equals("btn_gw_drop")) {
                showGiveawayModal(event, "Drop");
            } else {
                StringSelectMenu menu = StringSelectMenu.create("sel_gw_type")
                        .setPlaceholder("Select Reward Category...")
                        .addOption("Digital Voucher", "Voucher")
                        .addOption("Discount Coupon", "Discount")
                        .addOption("Custom Reward", "Custom")
                        .build();

                PanelService.replyEphemeral(event, EmbedUtil.containerBranded("GIVEAWAY CONFIG", "Step 1: Selection",
                        "Please select the **reward type** you wish to distribute.", EmbedUtil.BANNER_GIVEAWAY,
                        ActionRow.of(menu)));
            }
        } else if (id.equals("btn_gw_history")) {
            if (!Config.isAdmin(event.getMember()))
                return;
            JsonArray active = SupabaseClient.getAllGiveaways();

            if (active == null || active.size() == 0) {
                var emptyC = EmbedUtil.containerBranded("REWARDS HISTORY", "Logs Empty",
                        "\u26A0\uFE0F **Status:** No previous reward sessions were found in our records.",
                        EmbedUtil.BANNER_GIVEAWAY);
                PanelService.replyEphemeral(event, emptyC);
                return;
            }

            StringBuilder sb = new StringBuilder();
            sb.append(
                    "### \uD83D\uDCC3 Giveaway History\nListing the most recent reward sessions found in the registry:\n\n");

            for (int i = 0; i < active.size(); i++) {
                JsonObject g = active.get(i).getAsJsonObject();
                long gwIdLong = g.get("id").getAsLong();
                String prize = g.has("prize_details") ? g.get("prize_details").getAsString() : "Unknown";
                String hostId = g.has("host_id") ? g.get("host_id").getAsString() : "0";
                String chanId = g.has("channel_id") ? g.get("channel_id").getAsString() : "0";
                String ends = g.has("ends_at") ? g.get("ends_at").getAsString() : "";

                // Get entry count
                JsonArray entries = SupabaseClient.getGiveawayEntries(gwIdLong);
                int pCount = (entries != null) ? entries.size() : 0;

                sb.append("\u25AB\uFE0F **Prize:** ").append(prize)
                        .append("\n\u001F \u001F **Host:** <@").append(hostId).append(">")
                        .append("\n\u001F \u001F **Room:** <#").append(chanId).append(">")
                        .append("\n\u001F \u001F **Stats:** ").append(pCount).append(" members joined");

                if (!ends.isEmpty()) {
                    try {
                        long ts = java.time.Instant.parse(ends).getEpochSecond();
                        sb.append("\n\u001F \u001F **Limit:** <t:").append(ts).append(":R>");
                    } catch (Exception e) {
                    }
                }
                sb.append("\n\n");
            }

            var c = EmbedUtil.containerBranded("REWARDS", "Live Giveaways", sb.toString(), EmbedUtil.BANNER_GIVEAWAY);
            PanelService.replyEphemeral(event, c);
        } else if (id.startsWith("gw_count_")) {
            event.reply("This button shows the current entry count. Click **Join** to be part of it!")
                    .setEphemeral(true).queue();
        }
    }

    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent event) {
        String id = event.getComponentId();
        String value = event.getValues().get(0);

        if (id.equals("sel_gw_type")) {
            if (value.equals("Discount")) {
                StringSelectMenu menu = StringSelectMenu.create("sel_gw_sub_discount")
                        .setPlaceholder("Select Percentage...")
                        .addOption("10% Off", "10")
                        .addOption("20% Off", "20")
                        .addOption("30% Off", "30")
                        .addOption("40% Off", "40")
                        .addOption("50% Off", "50")
                        .addOption("60% Off", "60")
                        .build();
                PanelService.replyEphemeral(event, EmbedUtil.containerBranded("DISCOUNT SETUP", "Step 2: Percentage",
                        "Select the fixed **percentage** for this schedule.", EmbedUtil.BANNER_GIVEAWAY,
                        ActionRow.of(menu)));
            } else if (value.equals("Voucher")) {
                StringSelectMenu menu = StringSelectMenu.create("sel_gw_sub_voucher")
                        .setPlaceholder("Select Amount...")
                        .addOption("50 Credits", "50")
                        .addOption("100 Credits", "100")
                        .build();
                PanelService.replyEphemeral(event, EmbedUtil.containerBranded("VOUCHER SETUP", "Step 2: Value",
                        "Select the **credit amount** for this voucher.", EmbedUtil.BANNER_GIVEAWAY,
                        ActionRow.of(menu)));
            } else {
                showGiveawayModal(event, value);
            }
        } else if (id.equals("sel_gw_sub_discount")) {
            showGiveawayModal(event, "Discount-" + value);
        } else if (id.equals("sel_gw_sub_voucher")) {
            showGiveawayModal(event, "Voucher-" + value);
        }
    }

    private void showGiveawayModal(net.dv8tion.jda.api.interactions.callbacks.IModalCallback event, String type) {
        boolean isDrop = type.equals("Drop");
        String cleanType = type.contains("-") ? type.split("-")[0] : type;
        String modalId = "modal_gw_" + type.toLowerCase();

        String defaultPrize = null;
        if (type.startsWith("Discount-")) {
            defaultPrize = type.split("-")[1] + "% Discount";
        } else if (type.startsWith("Voucher-")) {
            defaultPrize = "$" + type.split("-")[1] + " Voucher";
        }

        TextInput prizeInput = TextInput.create("prize", TextInputStyle.SHORT)
                .setPlaceholder(isDrop ? "e.g., $10 Store Credit"
                        : cleanType.equals("Voucher") ? "e.g., $50 Account Credit"
                                : cleanType.equals("Discount") ? "e.g., 20% Discount" : "e.g., VIP Rank")
                .setValue(defaultPrize)
                .setRequired(true).build();

        TextInput winnersInput = TextInput.create("winners", TextInputStyle.SHORT)
                .setRequired(true).setValue("1").build();

        TextInput timeInput = TextInput.create("duration", TextInputStyle.SHORT)
                .setPlaceholder("Duration in minutes (e.g. 60)")
                .setRequired(!isDrop).setValue(isDrop ? "0" : "60").build();

        TextInput expireInput = TextInput.create("expire", TextInputStyle.SHORT)
                .setPlaceholder("Expiry in days (e.g. 7)")
                .setRequired(true).setValue("7").build();

        String prizeLabelText = (type.equals("Voucher") ? "Voucher Amount"
                : type.equals("Discount") ? "Discount Percentage" : "Reward Details");

        Modal modal = Modal.create(modalId, isDrop ? "QUICK DROP SETUP" : "REWARD CONFIG")
                .addComponents(
                        net.dv8tion.jda.api.components.label.Label.of(prizeLabelText, prizeInput),
                        net.dv8tion.jda.api.components.label.Label.of("Winner Count", winnersInput),
                        net.dv8tion.jda.api.components.label.Label.of("Duration (Min)", timeInput),
                        net.dv8tion.jda.api.components.label.Label.of("Expiry (Days)", expireInput))
                .build();

        event.replyModal(modal).queue();
    }

    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        if (!event.getModalId().startsWith("modal_gw_"))
            return;

        boolean isDrop = event.getModalId().equals("modal_gw_drop");
        String typeStr = event.getModalId().replace("modal_gw_", "");
        typeStr = typeStr.substring(0, 1).toUpperCase() + typeStr.substring(1); // Capitalize

        String prizeStr = event.getValue("prize").getAsString();

        // Symbol Validation
        if (typeStr.equalsIgnoreCase("Voucher")) {
            if (!prizeStr.contains("$")) {
                event.reply("\u26A0\uFE0F **Format Error:** Voucher prizes must include the `$` symbol (e.g., $50).")
                        .setEphemeral(true).queue();
                return;
            }
            if (prizeStr.contains("%")) {
                event.reply("\u26A0\uFE0F **Format Error:** Vouchers cannot use the `%` symbol. Use `$` for amounts.")
                        .setEphemeral(true).queue();
                return;
            }
        }
        if (typeStr.equalsIgnoreCase("Discount")) {
            if (!prizeStr.contains("%")) {
                event.reply("\u26A0\uFE0F **Format Error:** Discount prizes must include the `%` symbol (e.g., 20%).")
                        .setEphemeral(true).queue();
                return;
            }
            if (prizeStr.contains("$")) {
                event.reply(
                        "\u26A0\uFE0F **Format Error:** Discounts cannot use the `$` symbol. Use `%` for percentages.")
                        .setEphemeral(true).queue();
                return;
            }
        }

        int winCount = 1;
        int duration = 0;
        int expireDays = 7;

        try {
            winCount = Integer.parseInt(event.getValue("winners").getAsString());
        } catch (Exception e) {
        }
        try {
            duration = Integer.parseInt(event.getValue("duration").getAsString());
        } catch (Exception e) {
        }
        try {
            if (event.getValue("expire") != null) {
                expireDays = Integer.parseInt(event.getValue("expire").getAsString());
            }
        } catch (Exception e) {
        }

        String tempId = "setup_" + System.currentTimeMillis() + "_" + event.getUser().getId();

        JsonObject setupObj = new JsonObject();
        setupObj.addProperty("prize", prizeStr);
        setupObj.addProperty("type", typeStr);
        setupObj.addProperty("winCount", winCount);
        setupObj.addProperty("duration", duration);
        setupObj.addProperty("expire", expireDays);
        setupObj.addProperty("isDrop", isDrop);

        pendingGiveaways.put(tempId, setupObj);

        EntitySelectMenu menu = EntitySelectMenu.create("sel_gw_chan_" + tempId, EntitySelectMenu.SelectTarget.CHANNEL)
                .setPlaceholder("Select target channel...")
                .setChannelTypes(net.dv8tion.jda.api.entities.channel.ChannelType.TEXT)
                .setMinValues(1).setMaxValues(1).build();

        event.reply("Great! Now **select the text channel** where the giveaway should be posted:")
                .setComponents(ActionRow.of(menu))
                .setEphemeral(true)
                .queue();
    }

    @Override
    public void onEntitySelectInteraction(EntitySelectInteractionEvent event) {
        event.deferReply(true).queue();

        String idStr = event.getComponentId();
        if (!idStr.startsWith("sel_gw_chan_"))
            return;

        String tempId = idStr.replace("sel_gw_chan_", "");
        JsonObject setupObj = pendingGiveaways.remove(tempId);

        if (setupObj == null) {
            event.reply("Setup session expired. Please try again.").setEphemeral(true).queue();
            return;
        }

        TextChannel target = event.getMentions().getChannels(TextChannel.class).stream().findFirst().orElse(null);
        if (target == null && !event.getMentions().getChannels().isEmpty()) {
            target = (TextChannel) event.getMentions().getChannels().get(0);
        }

        if (target == null) {
            event.getHook().sendMessage("Selection Error: Unrecognized or invalid channel type selected.")
                    .setEphemeral(true).queue();
            return;
        }

        String prize = setupObj.get("prize").getAsString();
        String type = setupObj.get("type").getAsString();
        int winCount = setupObj.get("winCount").getAsInt();
        int duration = setupObj.get("duration").getAsInt();
        int expireDays = setupObj.has("expire") ? setupObj.get("expire").getAsInt() : 7;
        boolean isDrop = setupObj.get("isDrop").getAsBoolean();

        int prizeValue = 0;
        if (type.contains("-")) {
            try {
                prizeValue = Integer.parseInt(type.split("-")[1]);
            } catch (Exception e) {
            }
        }
        String cleanType = type.contains("-") ? type.split("-")[0] : type;

        Instant endsAt = Instant.now().plusSeconds(duration * 60L);
        String endsAtIso = isDrop ? Instant.now().plusSeconds(60).toString() : endsAt.toString();

        JsonObject gw = SupabaseClient.createGiveaway(
                target.getId(),
                target.getGuild().getId(),
                event.getUser().getId(),
                event.getUser().getName(),
                cleanType,
                prizeValue,
                prize,
                "Points",
                "",
                "General",
                "",
                winCount,
                endsAtIso,
                expireDays);

        if (gw == null) {
            event.getHook().sendMessage("Failed to create giveaway in database.").setEphemeral(true).queue();
            return;
        }

        long giveawayId = gw.get("id").getAsLong();
        dashboardMessages.put(giveawayId, ""); // Placeholder to be updated by msg queue
        dashboardChannels.put(giveawayId, target.getId());

        LogManager.logEmbed(event.getGuild(), Config.LOG_COMMANDS, EmbedUtil.createOldLogEmbed("giveaway-start",
                "Operation: Giveaway Started\nTarget Channel: " + target.getAsMention()
                        + "\nPrize: `" + prize + "`\nWinner Slots: " + winCount + "\nDuration: "
                        + (isDrop ? "Instant" : duration + "m"),
                event.getMember(), null, null, EmbedUtil.SUCCESS));

        String body;
        if (isDrop) {
            body = "### \uD83D\uDCA8 Instant Priority Drop\nA high-priority prize is available for the fastest member to claim.\n\n\u25AB\uFE0F **Prize:** "
                    + prize + "\n\u25AB\uFE0F **Winners:** " + winCount + "\n\nClick claim below to win!";
        } else {
            body = "### \uD83C\uDF81 Active Sweepstakes\nA new reward opportunity is now available for all members.\n\n\u25AB\uFE0F **Prize:** "
                    + prize + "\n\u25AB\uFE0F **Winners:** **" + winCount + "**\n\u25AB\uFE0F **Ends In:** <t:"
                    + endsAt.getEpochSecond() + ":R>";
        }

        Button joinBtn = Button.primary("gw_enter_" + giveawayId, isDrop ? "Claim Instant Prize" : "Join Sweepstakes")
                .withEmoji(
                        net.dv8tion.jda.api.entities.emoji.Emoji.fromUnicode(isDrop ? "\uD83D\uDCA8" : "\uD83C\uDF89"));
        Button countBtn = Button.secondary("gw_count_" + giveawayId, "0 entries");

        ActionRow gwRow = ActionRow.of(joinBtn, isDrop ? countBtn.asDisabled() : countBtn);
        var gwC = EmbedUtil.containerBranded("GIVEAWAY", isDrop ? "Instant Prize" : "Active Rewards", body,
                EmbedUtil.getDynamicBanner(prize), gwRow);

        // Use a list of components including the mention as TextDisplay
        List<net.dv8tion.jda.api.components.MessageTopLevelComponent> gwComps = new ArrayList<>();
        gwComps.add(net.dv8tion.jda.api.components.textdisplay.TextDisplay.of("<@&1488916921687736421>"));
        gwComps.add(gwC); // The branded container

        target.sendMessageComponents(gwComps)
                .useComponentsV2(true)
                .queue(msg -> {
                    SupabaseClient.setGiveawayMessageId(giveawayId, msg.getId());
                });

        LogManager.log(event.getGuild(), isDrop ? "DROP LAUNCHED" : "GIVEAWAY STARTED",
                "Prize: " + prize + "\nAdmin: **" + event.getUser().getAsTag() + "**\nChannel: "
                        + target.getAsMention(),
                EmbedUtil.INFO);

        // Edit the interaction with a Live Dashboard
        String dashDesc = "### " + prize + " | Live Status\n" +
                "\u25AB\uFE0F **Status:** Currently Active\n" +
                "\u25AB\uFE0F **Users Joined:** 0 members";

        var dashRow = ActionRow.of(
                Button.danger("gw_end_early_" + giveawayId, "End Early"),
                Button.success("gw_reroll_adm_" + giveawayId, "Reroll Winners"));
        var dashC = EmbedUtil.containerBranded("GIVEAWAY DASHBOARD", "Live Tracking", dashDesc,
                EmbedUtil.BANNER_GIVEAWAY, dashRow);

        PanelService.reply(event, dashC);

        event.getChannel().sendMessageComponents(dashC).useComponentsV2(true).queue(dashMsg -> {
            dashboardMessages.put(giveawayId, dashMsg.getId());
            dashboardChannels.put(giveawayId, dashMsg.getChannel().getId());
        });
    }

    private void updateDashboard(net.dv8tion.jda.api.entities.Guild guild, long giveawayId) {
        String msgId = dashboardMessages.get(giveawayId);
        String chanId = dashboardChannels.get(giveawayId);
        if (msgId == null || chanId == null)
            return;

        TextChannel ch = guild.getTextChannelById(chanId);
        if (ch == null)
            return;

        JsonObject g = SupabaseClient.getGiveawayById(giveawayId);
        if (g == null)
            return;

        JsonArray entries = SupabaseClient.getGiveawayEntries(giveawayId);
        int count = (entries != null) ? entries.size() : 0;
        String prize = g.get("prize_details").getAsString();

        String dashDesc = "### " + prize + " | Live Status\n" +
                "\u25AB\uFE0F **Status:** Currently Active\n" +
                "\u25AB\uFE0F **Users Joined:** " + count + " members";

        var dashRow = ActionRow.of(
                Button.danger("gw_end_early_" + giveawayId, "End Early"),
                Button.success("gw_reroll_adm_" + giveawayId, "Reroll Winners"));
        var dashC = EmbedUtil.containerBranded("GIVEAWAY DASHBOARD", "Live Tracking", dashDesc,
                EmbedUtil.BANNER_GIVEAWAY, dashRow);

        ch.editMessageComponentsById(msgId, dashC).useComponentsV2(true).queue(null, ex -> {
        });

        // Also update the main giveaway message count button
        String mainMsgId = g.has("message_id") && !g.get("message_id").isJsonNull() ? g.get("message_id").getAsString()
                : null;
        String mainChanId = g.has("channel_id") ? g.get("channel_id").getAsString() : null;

        if (mainMsgId != null && mainChanId != null) {
            TextChannel mainCh = guild.getTextChannelById(mainChanId);
            if (mainCh != null) {
                mainCh.retrieveMessageById(mainMsgId).queue(msg -> {
                    boolean isDrop = g.has("prize_type") && g.get("prize_type").getAsString().equalsIgnoreCase("Drop");
                    Button joinBtn = Button
                            .primary("gw_enter_" + giveawayId, isDrop ? "Claim Instant Prize" : "Join Sweepstakes")
                            .withEmoji(net.dv8tion.jda.api.entities.emoji.Emoji
                                    .fromUnicode(isDrop ? "\uD83D\uDCA8" : "\uD83C\uDF89"));
                    Button countBtn = Button.secondary("gw_count_" + giveawayId,
                            count + (count == 1 ? " entry" : " entries"));

                    ActionRow newGwRow = ActionRow.of(joinBtn, isDrop ? countBtn.asDisabled() : countBtn);

                    java.util.List<net.dv8tion.jda.api.components.MessageTopLevelComponent> currentComps = new java.util.ArrayList<>(
                            msg.getComponents());
                    for (int i = 0; i < currentComps.size(); i++) {
                        if (currentComps.get(i) instanceof net.dv8tion.jda.api.components.container.Container) {
                            net.dv8tion.jda.api.components.container.Container currentContainer = (net.dv8tion.jda.api.components.container.Container) currentComps
                                    .get(i);
                            java.util.List<net.dv8tion.jda.api.components.container.ContainerChildComponent> children = new java.util.ArrayList<>(
                                    currentContainer.getComponents());
                            for (int j = 0; j < children.size(); j++) {
                                if (children.get(j) instanceof ActionRow) {
                                    children.set(j, newGwRow);
                                }
                            }
                            currentComps.set(i, net.dv8tion.jda.api.components.container.Container.of(children));
                        }
                    }
                    mainCh.editMessageComponentsById(mainMsgId, currentComps).useComponentsV2(true).queue(null, ex -> {
                    });
                }, ex -> {
                });
            }
        }
    }
}
