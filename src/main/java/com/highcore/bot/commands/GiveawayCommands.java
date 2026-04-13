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
import net.dv8tion.jda.api.components.label.Label;
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
        if (!event.getName().equals("giveaway")) return;
        if (!event.getMember().hasPermission(net.dv8tion.jda.api.Permission.MANAGE_SERVER)) {
            PanelService.replyEphemeral(event, EmbedUtil.accessDenied());
            return;
        }

        String desc = "Welcome to the **Giveaway Management Panel**.\n\n" +
                "You can manage, create, and deploy automated sweepstakes for the community.\n" +
                "• **Create Giveaway:** Launches a setup wizard.\n" +
                "• **Drop Giveaway:** Drops an instant winner sweepstake.\n" +
                "• **History:** View active and past giveaways.";

        ActionRow row = ActionRow.of(
                Button.primary("btn_gw_create", "Create Giveaway").withEmoji(net.dv8tion.jda.api.entities.emoji.Emoji.fromUnicode("\uD83C\uDF81")),
                Button.secondary("btn_gw_drop", "Quick Drop").withEmoji(net.dv8tion.jda.api.entities.emoji.Emoji.fromUnicode("\uD83D\uDCA8")),
                Button.secondary("btn_gw_history", "History").withEmoji(net.dv8tion.jda.api.entities.emoji.Emoji.fromUnicode("\uD83D\uDDD2"))
        );

        var c = EmbedUtil.containerBranded("GIVEAWAY CONTROL", "Reward System", desc, EmbedUtil.BANNER_GIVEAWAY, row);

        PanelService.reply(event, c);
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String id = event.getComponentId();
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
                        .addOption("Voucher (\u0642\u0633\u064A\u0645\u0629 \u0634\u0631\u0627\u0621)", "Voucher", "Prize of a specific amount")
                        .addOption("Discount (\u0642\u0633\u064A\u0645\u0629 \u062A\u062E\u0641\u0641\u064A\u0636)", "Discount", "Percentage-based discount")
                        .addOption("Custom (\u062C\u0627\u0626\u0632\u0629 \u0645\u062E\u0635\u0635\u0629)", "Custom", "Anything else")
                        .build();

                PanelService.replyEphemeral(event, EmbedUtil.containerBranded("GIVEAWAY CONFIG", "Step 1: Selection", 
                        "Please select the **type of reward** you wish to distribute.", EmbedUtil.BANNER_GIVEAWAY, ActionRow.of(menu)));
            }
        } else if (id.equals("btn_gw_history")) {
            if (!event.getMember().hasPermission(net.dv8tion.jda.api.Permission.MANAGE_SERVER)) return;
            JsonArray active = SupabaseClient.getActiveGiveaways();
            if (active == null || active.size() == 0) {
                event.reply("There are no active giveaways at the moment.").setEphemeral(true).queue();
                return;
            }

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < active.size(); i++) {
                JsonObject g = active.get(i).getAsJsonObject();
                String prize = g.has("prize_details") ? g.get("prize_details").getAsString() : "Unknown";
                String host = g.has("host_name") ? g.get("host_name").getAsString() : "Admin";
                String ends = g.has("ends_at") ? g.get("ends_at").getAsString() : "";
                
                sb.append("• **").append(prize).append("** by ").append(host);
                if (!ends.isEmpty()) {
                    try {
                        long ts = Instant.parse(ends).getEpochSecond();
                        sb.append(" (Ends <t:").append(ts).append(":R>)");
                    } catch (Exception e) {}
                }
                sb.append("\n");
            }

            var c = EmbedUtil.containerBranded("GIVEAWAY REGISTRY", "Active Deployments", sb.toString(), EmbedUtil.BANNER_GIVEAWAY);
            event.replyComponents(c).setEphemeral(true).queue();
        } else if (id.startsWith("gw_end_early_")) {
            if (!event.getMember().hasPermission(net.dv8tion.jda.api.Permission.MANAGE_SERVER)) return;
            long gwId = Long.parseLong(id.replace("gw_end_early_", ""));
            com.highcore.bot.services.GiveawayService.endGiveaway(event.getJDA(), gwId, 1); // 1 is default, will pick winners
            event.reply("Giveaway ended early!").setEphemeral(true).queue();
        } else if (id.startsWith("gw_reroll_adm_")) {
            if (!event.getMember().hasPermission(net.dv8tion.jda.api.Permission.MANAGE_SERVER)) return;
            long gwId = Long.parseLong(id.replace("gw_reroll_adm_", ""));
            com.highcore.bot.services.GiveawayService.rerollGiveaway(event.getJDA(), gwId);
            event.reply("Rerolled giveaway winner(s)!").setEphemeral(true).queue();
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
                .setPlaceholder(isDrop ? "e.g., $10 Store Credit" : type.equals("Voucher") ? "e.g., $50 Account Credit" : type.equals("Discount") ? "e.g., 20% Discount" : "e.g., VIP Rank")
                .setRequired(true).build();
        
        TextInput winnersInput = TextInput.create("winners", TextInputStyle.SHORT)
                .setRequired(true).setValue("1").build();
        
        TextInput timeInput = TextInput.create("duration", TextInputStyle.SHORT)
                .setPlaceholder("Duration in minutes (e.g. 60)")
                .setRequired(!isDrop).setValue(isDrop ? "0" : "60").build();

        Modal.Builder mb = Modal.create(modalId, isDrop ? "QUICK DROP SETUP" : "GIVEAWAY: " + type.toUpperCase())
                .addComponents(Label.of(type.equals("Voucher") ? "Voucher Amount/Detail" : type.equals("Discount") ? "Discount Percentage" : "Reward Details", prizeInput))
                .addComponents(Label.of("Winners Count", winnersInput))
                .addComponents(Label.of("Duration (Minutes)", timeInput));

        event.replyModal(mb.build()).queue();
    }


    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        if (!event.getModalId().startsWith("modal_gw_")) return;
        
        boolean isDrop = event.getModalId().equals("modal_gw_drop");
        String typeStr = event.getModalId().replace("modal_gw_", "");
        typeStr = typeStr.substring(0, 1).toUpperCase() + typeStr.substring(1); // Capitalize
        
        String prizeStr = event.getValue("prize").getAsString();
        int winCount = 1;
        int duration = 0;
        
        try { winCount = Integer.parseInt(event.getValue("winners").getAsString()); } catch (Exception e) {}
        try { duration = Integer.parseInt(event.getValue("duration").getAsString()); } catch (Exception e) {}

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
        if (!idStr.startsWith("sel_gw_chan_")) return;
        
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
                endsAtIso
        );

        if (gw == null) {
            event.reply("Failed to create giveaway in database.").setEphemeral(true).queue();
            return;
        }

        long giveawayId = gw.get("id").getAsLong();
        
        String body;
        if (isDrop) {
            body = "### \uD83D\uDCA8 QUICK DROP: " + type.toUpperCase() + "\n**Prize:** " + prize + "\n**Winners:** " + winCount + "\n\nFirst to claim wins!";
        } else {
            body = "### \uD83C\uDF81 ACTIVE GIVEAWAY\n**Type:** " + type.toUpperCase() + "\n**Prize:** " + prize + "\n**Winners:** **" + winCount + "**\n**Ends:** <t:" + endsAt.getEpochSecond() + ":R>";
        }

        Button joinBtn = Button.primary("gw_enter_" + giveawayId, isDrop ? "Claim Drop" : "Enter Giveaway")
                .withEmoji(net.dv8tion.jda.api.entities.emoji.Emoji.fromUnicode(isDrop ? "\uD83D\uDCA8" : "\uD83C\uDF89"));
        Button countBtn = Button.secondary("gw_count_" + giveawayId, "0 entries");

        target.sendMessageComponents(EmbedUtil.containerBranded(isDrop ? "QUICK DROP" : "SWEEPSTAKES", "Rewards Distribution", body, EmbedUtil.BANNER_GIVEAWAY))
                .setComponents(ActionRow.of(joinBtn, isDrop ? countBtn.asDisabled() : countBtn))
                .queue(msg -> {
                    SupabaseClient.setGiveawayMessageId(giveawayId, msg.getId());
                });

        LogManager.log(event.getGuild(), isDrop ? "DROP LAUNCHED" : "GIVEAWAY STARTED",
                "Prize: " + prize + "\nAdmin: " + event.getUser().getAsMention() + "\nChannel: " + target.getAsMention(), EmbedUtil.INFO);

        // Edit the interaction with a Live Dashboard
        String dashDesc = "### Dashboard: " + prize + "\n**Status:** Active\n**Participants:** 0";
        var dashRow = ActionRow.of(
                Button.danger("gw_end_early_" + giveawayId, "End Early"),
                Button.success("gw_reroll_adm_" + giveawayId, "Reroll Winners")
        );
        var dashC = EmbedUtil.containerBranded("GIVEAWAY MONITOR", "Real-time Tracking", dashDesc, EmbedUtil.BANNER_GIVEAWAY, dashRow);
        
        event.reply("Giveaway sequence fully deployed. You can monitor it below!").setEphemeral(true).queue();

        event.getChannel().sendMessageComponents(dashC).useComponentsV2(true).queue(dashMsg -> {
            dashboardMessages.put(giveawayId, dashMsg.getId());
            dashboardChannels.put(giveawayId, dashMsg.getChannel().getId());
        });
    }
}
