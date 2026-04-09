package com.highcore.bot.services;

import com.highcore.bot.utils.EmbedUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.components.MessageTopLevelComponent;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.modals.Modal;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import java.util.*;

public class PanelService {

    public static void reply(Object target, Object content) { handleReply(target, content, false); }
    public static void replyEphemeral(Object target, Object content) { handleReply(target, content, true); }

    private static void handleReply(Object target, Object content, boolean ephemeral) {
        List<MessageTopLevelComponent> components = new ArrayList<>();
        if (content instanceof Container c) components.add(c);
        else if (content instanceof List<?> list) {
            for (Object obj : list) if (obj instanceof MessageTopLevelComponent mtc) components.add(mtc);
        }

        net.dv8tion.jda.api.entities.MessageEmbed bannerView = new EmbedBuilder()
            .setImage(EmbedUtil.BANNER_MAIN)
            .setColor(EmbedUtil.ACCENT)
            .build();
        
        if (target instanceof IReplyCallback event) {
            if (event.isAcknowledged()) {
                InteractionHook hook = event.getHook();
                net.dv8tion.jda.api.utils.messages.MessageEditBuilder meb = new net.dv8tion.jda.api.utils.messages.MessageEditBuilder()
                    .setEmbeds(bannerView)
                    .setComponents(components)
                    .useComponentsV2(true);
                hook.editOriginal(meb.build()).queue();
            } else {
                net.dv8tion.jda.api.utils.messages.MessageCreateBuilder mcb = new net.dv8tion.jda.api.utils.messages.MessageCreateBuilder()
                    .addEmbeds(bannerView)
                    .setComponents(components)
                    .useComponentsV2(true);
                event.reply(mcb.build()).setEphemeral(ephemeral).queue();
            }
        } else if (target instanceof net.dv8tion.jda.api.entities.channel.middleman.MessageChannel channel) {
            net.dv8tion.jda.api.utils.messages.MessageCreateBuilder mcb = new net.dv8tion.jda.api.utils.messages.MessageCreateBuilder()
                .addEmbeds(bannerView)
                .setComponents(components)
                .useComponentsV2(true);
            channel.sendMessage(mcb.build()).queue();
        }
    }

    public static void sendStartupHub(IReplyCallback event) {
        ActionRow row1 = ActionRow.of(
            Button.secondary("hub_highcore", "Service Map").withEmoji(Emoji.fromUnicode("\uD83D\uDDFA\uFE0F")),
            Button.secondary("hub_about", "Agency Info").withEmoji(Emoji.fromUnicode("\u2139\uFE0F"))
        );
        ActionRow row2 = ActionRow.of(
            Button.link("https://discord.com/channels/1488795129996116212/1488798547947159612", "Support Center").withEmoji(Emoji.fromUnicode("\uD83D\uDEE1\uFE0F"))
        );
        reply(event, EmbedUtil.eliteContainer("High Core Agency", "Global business operations and elite digital assets hub.", null, row1, row2));
    }

    public static void sendServerMap(IReplyCallback event) {
        ActionRow row = ActionRow.of(
            Button.success("hub_pings", "Alert Notifications").withEmoji(Emoji.fromUnicode("\uD83D\uDD14")),
            Button.secondary("hub_rules", "Operational Rules").withEmoji(Emoji.fromUnicode("\uD83D\uDCDC"))
        );
        replyEphemeral(event, EmbedUtil.eliteContainer("Infrastructure Map", "Connect to authorized agency channels below.", null, row));
    }

    public static void sendAboutUs(IReplyCallback event) {
        replyEphemeral(event, EmbedUtil.eliteContainer("Agency Identity", "High Core is a premium multi-sector agency delivering superior digital infrastructure.", null, ActionRow.of(Button.link("https://x.com/CoreHigh70331", "Agency Twitter"))));
    }

    public static void sendTicketPanel(IReplyCallback event) {
        ActionRow row = ActionRow.of(
            Button.primary("ticket_init_support", "Help Desk").withEmoji(Emoji.fromUnicode("\uD83D\uDEE1\uFE0F")),
            Button.success("ticket_init_order", "New Project").withEmoji(Emoji.fromUnicode("\uD83D\uDED2"))
        );
        reply(event, EmbedUtil.eliteContainer("Communications Hub", "Select a department category to initiate a secure session.", null, row));
    }
    
    public static void sendStatsPanel(IReplyCallback event) { replyEphemeral(event, EmbedUtil.eliteContainer("System Metrics", "All core agency modules are stable and operational.", null)); }
    public static void sendPricesCategory(IReplyCallback event) { replyEphemeral(event, EmbedUtil.eliteContainer("Financial Logistics", "Pricing matrices are synchronized and verified.", null)); }
    public static void sendServicesCategory(IReplyCallback event) { replyEphemeral(event, EmbedUtil.eliteContainer("Agency Assets", "Exploring specialized creative capabilities.", null)); }
    public static void sendPartnersPanel(IReplyCallback event) { replyEphemeral(event, EmbedUtil.eliteContainer("Strategic Partners", "Collaborative agency networks active.", null)); }
    public static void sendPingsPanel(IReplyCallback event) { replyEphemeral(event, EmbedUtil.eliteContainer("Frequency Sync", "Select notification layers for protocol updates.", null)); }

    public static void handleSupportFlow(IReplyCallback event) {
        if (event instanceof net.dv8tion.jda.api.interactions.callbacks.IModalCallback modal) {
            modal.replyModal(Modal.create("modal_support_init", "Help Desk")
                .addComponents(ActionRow.of(TextInput.create("issue_desc", "Subject of Inquiry", TextInputStyle.PARAGRAPH).setRequired(true).build())).build()).queue();
        }
    }

    public static void handleOrderFlow(IReplyCallback event) {
        StringSelectMenu menu = StringSelectMenu.create("order_sector_select").setPlaceholder("Choose specialized department...")
            .addOption("Elite Design Sector", "sect_designer")
            .addOption("Professional Development", "sect_developer")
            .build();
        replyEphemeral(event, EmbedUtil.eliteContainer("Project Initiation", "Identify the specialized agency sector for your project.", null, ActionRow.of(menu)));
    }

    public static void handleSectorSelection(net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent event, String val) {
        StringSelectMenu menu = StringSelectMenu.create("order_service_select_" + val).setPlaceholder("Select services...").addOption("Standard Service", "Mod-S").build();
        reply(event, EmbedUtil.eliteContainer("Services Selection", "Select your creative requirements.", null, ActionRow.of(menu)));
    }

    public static void handleServiceSelection(net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent event, List<String> vals) {
        reply(event, EmbedUtil.eliteContainer("Configuration", "Proceed to final project configuration.", null, ActionRow.of(Button.success("order_final_meta", "Proceed to Details").withEmoji(Emoji.fromUnicode("\u2705")))));
    }

    public static void handleOrderMetaModal(net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent event) {
        event.replyModal(Modal.create("modal_order_finalize", "Project Details")
            .addComponents(ActionRow.of(TextInput.create("p_name", "Project Name", TextInputStyle.SHORT).build()))
            .addComponents(ActionRow.of(TextInput.create("p_client", "Client Identity", TextInputStyle.SHORT).build()))
            .build()).queue();
    }
}
