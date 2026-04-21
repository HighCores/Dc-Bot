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

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GiveawayCommands extends ListenerAdapter {
    private static final Logger log = LoggerFactory.getLogger(GiveawayCommands.class);
    private static final Map<String, JsonObject> pendingGiveaways = new ConcurrentHashMap<>();
    public static final Map<Long, String> dashboardMessages = new ConcurrentHashMap<>();
    public static final Map<Long, String> dashboardChannels = new ConcurrentHashMap<>();

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("giveaway")) return;
        if (!Config.isAdmin(event.getMember())) { PanelService.reply(event, EmbedUtil.accessDenied()); return; }

        String desc = """
                Welcome to the **Giveaway Management Hub**.

                Easily create rewards, launch instant drops, or review history.

                📝 **Create Giveaway** — Step-by-step setup.
                💨 **Instant Drop** — Fast 'first-to-claim' prize.
                🗒️ **View History** — Check active tasks.
                """;
        ActionRow row = ActionRow.of(
            Button.secondary("btn_gw_create", "Create Giveaway"),
            Button.secondary("btn_gw_history", "View History")
        );
        PanelService.reply(event, EmbedUtil.containerBranded("GIVEAWAY MANAGER", "Setup & History", desc, EmbedUtil.BANNER_GIVEAWAY, row));
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String id = event.getComponentId();
        if (id.startsWith("gw_enter_")) {
            long gwId = Long.parseLong(id.replace("gw_enter_", ""));
            if (SupabaseClient.hasEnteredGiveaway(gwId, event.getUser().getId())) {
                event.reply("Entry already logged.").setEphemeral(true).queue();
            } else {
                SupabaseClient.addGiveawayEntry(gwId, event.getUser().getId());
                
                JsonArray entries = SupabaseClient.getGiveawayEntries(gwId);
                int count = (entries != null) ? entries.size() : 0;
                
                ActionRow newRow = ActionRow.of(
                    Button.secondary("gw_enter_" + gwId, "Join Sweepstakes"),
                    Button.secondary("gw_count_" + gwId, count + (count == 1 ? " entry" : " entries")).asDisabled()
                );
                
                event.deferReply(true).queue();
                
                JsonObject g = SupabaseClient.getGiveawayById(gwId);
                String prize = g != null ? g.get("prize_details").getAsString() : "Classified Item";
                int win = g != null && g.has("winner_count") ? g.get("winner_count").getAsInt() : 1;
                long endsTs = g != null ? java.time.Instant.parse(g.get("ends_at").getAsString()).getEpochSecond() : java.time.Instant.now().getEpochSecond();
                
                String bannerUrl = EmbedUtil.BANNER_GIVEAWAY;
                if (g != null) {
                    String typeStr = g.get("prize_type").getAsString().toLowerCase();
                    if (typeStr.startsWith("discount-")) {
                        switch(typeStr) {
                            case "discount-10": bannerUrl = "https://i.imgur.com/QpboYHV.png"; break;
                            case "discount-20": bannerUrl = "https://i.imgur.com/FnsAuqW.png"; break;
                            case "discount-30": bannerUrl = "https://i.imgur.com/n503P4n.png"; break;
                            case "discount-40": bannerUrl = "https://i.imgur.com/4swCqaO.png"; break;
                            case "discount-50": bannerUrl = "https://i.imgur.com/p1W4MGn.png"; break;
                            case "discount-60": bannerUrl = "https://i.imgur.com/ujRHuoi.png"; break;
                        }
                    } else if (typeStr.startsWith("voucher-")) {
                        switch(typeStr) {
                            case "voucher-50": bannerUrl = "https://i.imgur.com/gqEoG4z.png"; break;
                            case "voucher-100": bannerUrl = "https://i.imgur.com/DdlMSHd.png"; break;
                        }
                    }
                }
                
                String body = "### Active Sweepstakes\n\n▫️ **Prize:** " + prize + "\n▫️ **Winners:** " + win + "\n▫️ **Ends:** <t:" + endsTs + ":R>\n\nInteract below to enter.";
                
                event.getChannel().editMessageComponentsById(event.getMessageId(), EmbedUtil.containerBranded("GIVEAWAY", "Active Reward", body, bannerUrl, newRow)).useComponentsV2(true).queue(v -> {
                    event.getHook().sendMessage("Success: Entry secured.").queue();
                });
                
                updateDashboard(event.getGuild(), gwId);
            }
            return;
        }

        if (!Config.isAdmin(event.getMember())) {
             if (id.equals("btn_gw_create") || id.equals("btn_gw_history")) {
                 PanelService.replyEphemeral(event, EmbedUtil.accessDenied());
             }
             return;
        }

        try {
            if (id.equals("btn_gw_create")) {
                StringSelectMenu menu = StringSelectMenu.create("sel_gw_type")
                        .setPlaceholder("Deployment Select...")
                        .addOption("Voucher ($50, $100)", "Voucher_Select")
                        .addOption("Discount (10%-60%)", "Discount_Select")
                        .addOption("Custom", "Custom")
                        .build();
                PanelService.replyEphemeral(event, EmbedUtil.containerBranded("GIVEAWAY", "Selection", "Select reward type:", EmbedUtil.BANNER_GIVEAWAY, ActionRow.of(menu)));
            } else if (id.equals("btn_gw_history")) {
                event.deferReply(true).queue();
                JsonArray active = SupabaseClient.getAllGiveaways();
                if (active == null || active.size() == 0) {
                    event.getHook().sendMessage("No active deployments found.").setEphemeral(true).queue();
                    return;
                }
                StringBuilder sb = new StringBuilder("### Tactical History\n\n");
                for (int i = 0; i < Math.min(active.size(), 10); i++) {
                    JsonObject g = active.get(i).getAsJsonObject();
                    sb.append("▫️ **Prize:** ").append(g.get("prize_details").getAsString()).append("\n");
                }
                event.getHook().sendMessageComponents(EmbedUtil.containerBranded("REWARDS", "Records", sb.toString(), EmbedUtil.BANNER_GIVEAWAY))
                     .useComponentsV2(true)
                     .setEphemeral(true)
                     .queue();
            } else if (id.startsWith("gw_end_early_")) {
                long gwId = Long.parseLong(id.replace("gw_end_early_", ""));
                com.highcore.bot.services.GiveawayService.endGiveaway(event.getJDA(), gwId, 1);
                PanelService.replyEphemeral(event, "Archival initiated.");
            }
        } catch (Exception e) {
            log.error("Registry Button Error", e);
        }
    }

    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent event) {
        if (event.getComponentId().equals("sel_gw_type")) {
            String type = event.getValues().get(0);
            if (type.equals("Discount_Select")) {
                StringSelectMenu m = StringSelectMenu.create("sel_gw_perc")
                        .setPlaceholder("Percentage...")
                        .addOption("10% Off", "10").addOption("20% Off", "20").addOption("30% Off", "30").addOption("40% Off", "40").addOption("50% Off", "50").addOption("60% Off", "60")
                        .build();
                PanelService.replyEphemeral(event, EmbedUtil.containerBranded("DISCOUNT", "Configuration", "Select amount:", EmbedUtil.BANNER_GIVEAWAY, ActionRow.of(m)));
            } else if (type.equals("Voucher_Select")) {
                StringSelectMenu m = StringSelectMenu.create("sel_gw_vouch")
                        .setPlaceholder("Voucher Amount...")
                        .addOption("$50 Voucher", "50").addOption("$100 Voucher", "100")
                        .build();
                PanelService.replyEphemeral(event, EmbedUtil.containerBranded("VOUCHER", "Configuration", "Select amount (Only $50 and $100 allowed):", EmbedUtil.BANNER_GIVEAWAY, ActionRow.of(m)));
            } else showGiveawayModal(event, type);
        } else if (event.getComponentId().equals("sel_gw_perc")) {
            showGiveawayModal(event, "Discount-" + event.getValues().get(0));
        } else if (event.getComponentId().equals("sel_gw_vouch")) {
            showGiveawayModal(event, "Voucher-" + event.getValues().get(0));
        }
    }

    private void showGiveawayModal(net.dv8tion.jda.api.interactions.callbacks.IModalCallback event, String type) {
        boolean isDrop = type.equals("Drop");
        String defaultPrize = "";
        if (type.startsWith("Discount-")) defaultPrize = type.split("-")[1] + "% Discount";
        else if (type.startsWith("Voucher-")) defaultPrize = "$" + type.split("-")[1] + " Store Credit";

        TextInput.Builder pb = TextInput.create("prize", TextInputStyle.SHORT).setRequired(true)
                .setPlaceholder(isDrop ? "e.g., $10 Store Credit" : "e.g., VIP Rank");
        if (!defaultPrize.isBlank()) pb.setValue(defaultPrize);
        
        TextInput winners = TextInput.create("winners", TextInputStyle.SHORT).setRequired(true).setValue("1").build();
        TextInput duration = TextInput.create("duration", TextInputStyle.SHORT).setRequired(true).setValue("60").build();
        TextInput expiry = TextInput.create("reward_expiry", TextInputStyle.SHORT).setRequired(true).setValue("7").build();

        Modal modal = Modal.create("modal_gw_" + type.toLowerCase(), "GIVEAWAY CONFIG")
                .addComponents(
                    net.dv8tion.jda.api.components.label.Label.of("Prize", pb.build()),
                    net.dv8tion.jda.api.components.label.Label.of("Winners", winners),
                    net.dv8tion.jda.api.components.label.Label.of("Duration (Min)", duration),
                    net.dv8tion.jda.api.components.label.Label.of("Expiry (Days)", expiry)
                ).build();
        event.replyModal(modal).queue();
    }

    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        if (!event.getModalId().startsWith("modal_gw_")) return;
        event.deferReply(true).queue();
        
        String idStr = event.getModalId().replace("modal_gw_", "");
        String prize = event.getValue("prize").getAsString();
        int win = 1, dur = 0, exp = 7;
        try { win = Integer.parseInt(event.getValue("winners").getAsString()); } catch (Exception e) {}
        try { dur = Integer.parseInt(event.getValue("duration").getAsString()); } catch (Exception e) {}
        try { exp = Integer.parseInt(event.getValue("reward_expiry").getAsString()); } catch (Exception e) {}

        JsonObject setup = new JsonObject();
        setup.addProperty("prize", prize);
        setup.addProperty("type", idStr);
        setup.addProperty("win", win);
        setup.addProperty("dur", dur);
        setup.addProperty("exp", exp);

        String tid = "setup_" + System.currentTimeMillis();
        pendingGiveaways.put(tid, setup);

        EntitySelectMenu menu = EntitySelectMenu.create("sel_chan_" + tid, EntitySelectMenu.SelectTarget.CHANNEL)
                .setPlaceholder("Target sector...").setChannelTypes(net.dv8tion.jda.api.entities.channel.ChannelType.TEXT).setMinValues(1).setMaxValues(1).build();
        event.getHook().sendMessage("Identify the deployment sector:").setComponents(ActionRow.of(menu)).setEphemeral(true).queue();
    }

    @Override
    public void onEntitySelectInteraction(EntitySelectInteractionEvent event) {
        String id = event.getComponentId();
        if (!id.startsWith("sel_chan_")) return;
        event.deferReply(true).queue();
        
        JsonObject s = pendingGiveaways.remove(id.replace("sel_chan_", ""));
        if (s == null) { event.getHook().sendMessage("Session expired.").queue(); return; }

        TextChannel target = event.getMentions().getChannels(TextChannel.class).get(0);
        String prize = s.get("prize").getAsString();
        Instant ends = Instant.now().plusSeconds(s.get("dur").getAsInt() * 60L);

        JsonObject gw = SupabaseClient.createGiveaway(target.getId(), target.getGuild().getId(), event.getUser().getId(), event.getUser().getName(), s.get("type").getAsString(), prize, "Points", "", "General", "", s.get("win").getAsInt(), ends.toString(), s.get("exp").getAsInt());
        if (gw == null) { event.getHook().sendMessage("Database Failure.").queue(); return; }

        long gwId = gw.get("id").getAsLong();
        dashboardMessages.put(gwId, ""); 
        dashboardChannels.put(gwId, target.getId());

        String bannerUrl = EmbedUtil.BANNER_GIVEAWAY;
        String typeStr = s.get("type").getAsString().toLowerCase();
        if (typeStr.startsWith("discount-")) {
            switch(typeStr) {
                case "discount-10": bannerUrl = "https://i.imgur.com/QpboYHV.png"; break;
                case "discount-20": bannerUrl = "https://i.imgur.com/FnsAuqW.png"; break;
                case "discount-30": bannerUrl = "https://i.imgur.com/n503P4n.png"; break;
                case "discount-40": bannerUrl = "https://i.imgur.com/4swCqaO.png"; break;
                case "discount-50": bannerUrl = "https://i.imgur.com/p1W4MGn.png"; break;
                case "discount-60": bannerUrl = "https://i.imgur.com/ujRHuoi.png"; break;
            }
        } else if (typeStr.startsWith("voucher-")) {
            switch(typeStr) {
                case "voucher-50": bannerUrl = "https://i.imgur.com/gqEoG4z.png"; break;
                case "voucher-100": bannerUrl = "https://i.imgur.com/DdlMSHd.png"; break;
            }
        }

        String body = "### Active Sweepstakes\n\n▫️ **Prize:** " + prize + "\n▫️ **Winners:** " + s.get("win").getAsInt() + "\n▫️ **Ends:** <t:" + ends.getEpochSecond() + ":R>\n\nInteract below to enter.";
        ActionRow row = ActionRow.of(Button.secondary("gw_enter_" + gwId, "Join Sweepstakes"), Button.secondary("gw_count_" + gwId, "0 entries").asDisabled());
        
        target.sendMessageComponents(EmbedUtil.containerBranded("GIVEAWAY", "Active Reward", body, bannerUrl, row)).useComponentsV2(true).queue(m -> SupabaseClient.setGiveawayMessageId(gwId, m.getId()));
        
        String dashDesc = "### " + prize + " | Logistics\n▫️ **Joins:** 0 members";
        ActionRow dashRow = ActionRow.of(Button.secondary("gw_end_early_" + gwId, "Close Early"));
        
        event.getChannel().sendMessageComponents(EmbedUtil.containerBranded("DASHBOARD", "Command Monitoring", dashDesc, EmbedUtil.BANNER_GIVEAWAY, dashRow)).useComponentsV2(true).queue(dm -> {
            dashboardMessages.put(gwId, dm.getId());
            dashboardChannels.put(gwId, dm.getChannel().getId());
        });
        
        event.getHook().sendMessage("Giveaway successfully deployed to " + target.getAsMention()).setEphemeral(true).queue();
    }

    private void updateDashboard(net.dv8tion.jda.api.entities.Guild guild, long gwId) {
        String mid = dashboardMessages.get(gwId);
        String cid = dashboardChannels.get(gwId);
        if (mid == null || cid == null) return;
        TextChannel ch = guild.getTextChannelById(cid);
        if (ch == null) return;
        JsonArray entries = SupabaseClient.getGiveawayEntries(gwId);
        int count = (entries != null) ? entries.size() : 0;
        JsonObject g = SupabaseClient.getGiveawayById(gwId);
        if (g == null) return;
        String desc = "### " + g.get("prize_details").getAsString() + " | Logistics\n▫️ **Joins:** " + count + " members";
        ch.editMessageComponentsById(mid, EmbedUtil.containerBranded("DASHBOARD", "Monitoring", desc, EmbedUtil.BANNER_GIVEAWAY, ActionRow.of(Button.secondary("gw_end_early_" + gwId, "Close Early")))).useComponentsV2(true).queue(null, e -> {});
    }
}
