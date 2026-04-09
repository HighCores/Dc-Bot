package com.highcore.bot.services;

import com.highcore.bot.utils.EmbedUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
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

        net.dv8tion.jda.api.entities.MessageEmbed banner = new EmbedBuilder().setImage(EmbedUtil.BANNER_MAIN).setColor(EmbedUtil.ACCENT).build();
        
        if (target instanceof IReplyCallback event) {
            if (event.isAcknowledged()) {
                InteractionHook hook = event.getHook();
                if (ephemeral) hook.sendMessageEmbeds(banner).setComponents(components).setEphemeral(true).queue();
                else hook.editOriginalEmbeds(banner).setComponents(components).queue();
            } else {
                event.replyEmbeds(banner).setComponents(components).setEphemeral(ephemeral).queue();
            }
        } else if (target instanceof MessageChannel channel) {
            net.dv8tion.jda.api.utils.messages.MessageCreateBuilder mcb = new net.dv8tion.jda.api.utils.messages.MessageCreateBuilder().addEmbeds(banner).setComponents(components).useComponentsV2(true);
            channel.sendMessage(mcb.build()).queue();
        }
    }

    public static void sendStartupHub(IReplyCallback event) {
        ActionRow row = ActionRow.of(Button.secondary("hub_highcore", "Map"), Button.secondary("hub_about", "About"));
        reply(event, EmbedUtil.eliteContainer("High Core Agency", "Global operations hub.", null, row));
    }

    public static void sendServerMap(IReplyCallback event) {
        ActionRow row = ActionRow.of(Button.success("hub_pings", "Alerts"), Button.secondary("hub_rules", "Rules"));
        replyEphemeral(event, EmbedUtil.eliteContainer("Map", "Internal navigation active.", null, row));
    }

    public static void sendAboutUs(IReplyCallback event) {
        replyEphemeral(event, EmbedUtil.eliteContainer("About Us", "Elite creative identity.", null, ActionRow.of(Button.link("https://x.com/CoreHigh70331", "X"))));
    }

    public static void sendPartnersPanel(IReplyCallback event) {
        replyEphemeral(event, EmbedUtil.eliteContainer("Partners", "Strategic collaborations.", null));
    }

    public static void sendPingsPanel(IReplyCallback event) {
        replyEphemeral(event, EmbedUtil.eliteContainer("Pings", "Select notification layers.", null));
    }

    public static void sendStatsPanel(IReplyCallback event) {
        replyEphemeral(event, EmbedUtil.eliteContainer("Telemetry", "Systems Operational.", null));
    }

    public static void sendPricesCategory(IReplyCallback event) {
        replyEphemeral(event, EmbedUtil.eliteContainer("Pricing", "Service modules processing.", null));
    }

    public static void sendServicesCategory(IReplyCallback event) {
        replyEphemeral(event, EmbedUtil.eliteContainer("Services", "Agency assets online.", null));
    }

    public static void sendTicketPanel(IReplyCallback event) {
        ActionRow row = ActionRow.of(Button.primary("ticket_init_support", "Support"), Button.success("ticket_init_order", "Order"));
        reply(event, EmbedUtil.eliteContainer("Help Center", "Select request type.", null, row));
    }

    public static void handleSupportFlow(IReplyCallback event) {
        if (event instanceof net.dv8tion.jda.api.interactions.callbacks.IModalCallback modal) {
            modal.replyModal(Modal.create("modal_support_init", "Support Request")
                .addComponents(Label.of("Message", TextInput.create("issue_desc", TextInputStyle.PARAGRAPH).build())).build()).queue();
        }
    }

    public static void handleComplaintFlow(IReplyCallback event) {
        if (event instanceof net.dv8tion.jda.api.interactions.callbacks.IModalCallback modal) {
            modal.replyModal(Modal.create("modal_complaint_init", "Complaint")
                .addComponents(Label.of("Reason", TextInput.create("comp_reason", TextInputStyle.PARAGRAPH).build())).build()).queue();
        }
    }

    public static void handleOrderFlow(IReplyCallback event) {
        StringSelectMenu menu = StringSelectMenu.create("order_sector_select").setPlaceholder("Select Sector")
            .addOption("Design", "sect_designer").addOption("Code", "sect_developer").build();
        replyEphemeral(event, EmbedUtil.eliteContainer("New Order", "Identify project sector.", null, ActionRow.of(menu)));
    }

    public static void handleSectorSelection(net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent event, String val) {
        reply(event, EmbedUtil.eliteContainer("Services", "Select requirements.", null));
    }

    public static void handleServiceSelection(net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent event, List<String> vals) {
        reply(event, EmbedUtil.eliteContainer("Finalizing", "Click to proceed.", null, ActionRow.of(Button.success("order_final_meta", "Proceed"))));
    }

    public static void handleOrderMetaModal(net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent event) {
        event.replyModal(Modal.create("modal_order_finalize", "Order Details")
            .addComponents(Label.of("Project", TextInput.create("p_name", TextInputStyle.SHORT).build()))
            .build()).queue();
    }
}
