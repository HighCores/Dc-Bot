package com.highcore.bot.commands;

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

import java.time.Instant;
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
        if (!event.getName().equals("giveaway"))
            return;

        if (!event.getMember().hasPermission(net.dv8tion.jda.api.Permission.MANAGE_SERVER)) {
            PanelService.reply(event, EmbedUtil.accessDenied());
            return;
        }

        String desc = "Welcome to the **Registry Hub**.\n\n" +
                "Easily create rewards, launch instant drops, or review history.\n\n" +
                "\uD83D\uDCDD **Create Reward** — Step-by-step setup.\n" +
                "\uD83D\uDCA8 **Instant Drop** — Fast 'first-to-claim' prize.\n" +
                "\uD83D\uDDD2 **View History** — Review all active deployments.";

        ActionRow row = ActionRow.of(
                Button.link("https://discord.com/channels/" + com.highcore.bot.config.Config.GUILD_ID
                        + "/1490334823565365308", "Rewards Feed"),
                Button.secondary("btn_gw_create", "Create Deployment"),
                Button.secondary("btn_gw_drop", "Instant Drop"),
                Button.secondary("btn_gw_history", "View Records"));

        PanelService.reply(event,
                EmbedUtil.containerBranded("LOGISTICS", "Registry Node", desc, EmbedUtil.BANNER_GIVEAWAY, row));
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String id = event.getComponentId();

        // Defer giveaway interactions early to handle potential DB latency
        if (id.startsWith("gw_") || id.startsWith("btn_gw_") || id.startsWith("sel_gw_")) {
            if (!event.isAcknowledged())
                event.deferReply(true).queue();
        }

        if (id.equals("btn_gw_create") || id.equals("btn_gw_drop")) {
            if (!event.getMember().hasPermission(net.dv8tion.jda.api.Permission.MANAGE_SERVER)) {
                PanelService.replyEphemeral(event, EmbedUtil.accessDenied());
                return;
            }

            boolean isDrop = id.equals("btn_gw_drop");

            if (isDrop) {
                // Drop skips type selection
                showGiveawayModal(event, "Drop");
            } else {
                // Regular giveaway shows type selection first
                StringSelectMenu menu = StringSelectMenu.create("sel_gw_type")
                        .setPlaceholder("Select the type of giveaway...")
                        .addOption("Voucher", "Voucher", "Prize of a specific amount")
                        .addOption("Discount", "Discount", "Percentage-based discount")
                        .addOption("Custom", "Custom", "Anything else")
                        .build();

                PanelService.replyEphemeral(event, EmbedUtil.containerBranded("GIVEAWAY CONFIG", "Step 1: Selection",
                        "Please select the **reward type** you wish to distribute.", EmbedUtil.BANNER_GIVEAWAY,
                        ActionRow.of(menu)));
            }
        } else if (id.equals("btn_gw_history")) {
            if (!event.getMember().hasPermission(net.dv8tion.jda.api.Permission.MANAGE_SERVER))
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
                    "### \uD83D\uDCC3 Reward Deployment Log\nListing the most recent reward sessions found in the registry:\n\n");

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
        } else if (id.startsWith("gw_end_early_")) {
            if (!event.getMember().hasPermission(net.dv8tion.jda.api.Permission.MANAGE_SERVER))
                return;
            long gwId = Long.parseLong(id.replace("gw_end_early_", ""));
            com.highcore.bot.services.GiveawayService.endGiveaway(event.getJDA(), gwId, 1);
            PanelService.replyEphemeral(event, EmbedUtil.success("SYSTEM", "Giveaway ended early!"));
        } else if (id.startsWith("gw_reroll_adm_")) {
            if (!event.getMember().hasPermission(net.dv8tion.jda.api.Permission.MANAGE_SERVER))
                return;
            long gwId = Long.parseLong(id.replace("gw_reroll_adm_", ""));
            com.highcore.bot.services.GiveawayService.rerollGiveaway(event.getJDA(), gwId);
            PanelService.replyEphemeral(event, EmbedUtil.success("SYSTEM", "Rerolled giveaway winner(s)!"));
        }
    }

    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent event) {
        if (event.getComponentId().equals("sel_gw_type")) {
            String type = event.getValues().get(0);
            showGiveawayModal(event, type);
        }
    }

    private void showGiveawayModal(net.dv8tion.jda.api.interactions.callbacks.IModalCallback event, String type) {
        boolean isDrop = type.equals("Drop");
        String modalId = "modal_gw_" + type.toLowerCase();

        TextInput prizeInput = TextInput.create("prize", TextInputStyle.SHORT)
                .setPlaceholder(isDrop ? "e.g., $10 Store Credit"
                        : type.equals("Voucher") ? "e.g., $50 Account Credit"
                                : type.equals("Discount") ? "e.g., 20% Discount" : "e.g., VIP Rank")
                .setRequired(true).build();

        TextInput winnersInput = TextInput.create("winners", TextInputStyle.SHORT)
                .setRequired(true).setValue("1").build();

        TextInput timeInput = TextInput.create("duration", TextInputStyle.SHORT)
                .setPlaceholder("Duration in minutes (e.g. 60)")
                .setRequired(!isDrop).setValue(isDrop ? "0" : "60").build();

        String prizeLabelText = (type.equals("Voucher") ? "Voucher Amount"
                : type.equals("Discount") ? "Discount Percentage" : "Reward Details");

        Modal modal = Modal.create(modalId, isDrop ? "QUICK DROP SETUP" : "GIVEAWAY: " + type.toUpperCase())
                .addComponents(
                        net.dv8tion.jda.api.components.label.Label.of(prizeLabelText, prizeInput),
                        net.dv8tion.jda.api.components.label.Label.of("Number of Winners", winnersInput),
                        net.dv8tion.jda.api.components.label.Label.of("Duration (Minutes)", timeInput))
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

        try {
            winCount = Integer.parseInt(event.getValue("winners").getAsString());
        } catch (Exception e) {
        }
        try {
            duration = Integer.parseInt(event.getValue("duration").getAsString());
        } catch (Exception e) {
        }

        String tempId = "setup_" + System.currentTimeMillis() + "_" + event.getUser().getId();

        JsonObject setupObj = new JsonObject();
        setupObj.addProperty("prize", prizeStr);
        setupObj.addProperty("type", typeStr);
        setupObj.addProperty("winCount", winCount);
        setupObj.addProperty("duration", duration);
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
        if (target == null) {
            event.reply("Invalid channel selected.").setEphemeral(true).queue();
            return;
        }

        String prize = setupObj.get("prize").getAsString();
        String type = setupObj.get("type").getAsString();
        int winCount = setupObj.get("winCount").getAsInt();
        int duration = setupObj.get("duration").getAsInt();
        boolean isDrop = setupObj.get("isDrop").getAsBoolean();

        Instant endsAt = Instant.now().plusSeconds(duration * 60L);
        String endsAtIso = isDrop ? Instant.now().plusSeconds(60).toString() : endsAt.toString();

        JsonObject gw = SupabaseClient.createGiveaway(
                target.getId(),
                target.getGuild().getId(),
                event.getUser().getId(),
                event.getUser().getName(),
                type,
                prize,
                "Points",
                "",
                "General",
                "",
                winCount,
                endsAtIso);

        if (gw == null) {
            event.reply("Failed to create giveaway in database.").setEphemeral(true).queue();
            return;
        }

        long giveawayId = gw.get("id").getAsLong();

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
                EmbedUtil.BANNER_GIVEAWAY, gwRow);

        target.sendMessageComponents(gwC).useComponentsV2(true).queue(msg -> {
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

        event.reply("Giveaway sequence fully deployed. You can monitor it below!").setEphemeral(false).queue();

        event.getChannel().sendMessageComponents(dashC).useComponentsV2(true).queue(dashMsg -> {
            dashboardMessages.put(giveawayId, dashMsg.getId());
            dashboardChannels.put(giveawayId, dashMsg.getChannel().getId());
        });
    }
}
