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

    public static void reply(Object interaction, Object content) { handleReply(interaction, content, false); }
    public static void replyEphemeral(Object interaction, Object content) { handleReply(interaction, content, true); }

    private static void handleReply(Object interaction, Object content, boolean ephemeral) {
        List<MessageTopLevelComponent> components = new ArrayList<>();
        if (content instanceof Container c) components.add(c);
        else if (content instanceof List<?> list) {
            for (Object obj : list) if (obj instanceof MessageTopLevelComponent mtc) components.add(mtc);
        }

        if (interaction instanceof IReplyCallback replyCallback) {
            InteractionHook hook = replyCallback.getHook();
            
            // Absolute Image Stability via separate Embed
            net.dv8tion.jda.api.entities.MessageEmbed banner = new EmbedBuilder()
                .setImage(EmbedUtil.BANNER_MAIN)
                .setColor(EmbedUtil.ACCENT)
                .build();
            
            net.dv8tion.jda.api.utils.messages.MessageCreateBuilder mcb = new net.dv8tion.jda.api.utils.messages.MessageCreateBuilder();
            net.dv8tion.jda.api.utils.messages.MessageEditBuilder meb = new net.dv8tion.jda.api.utils.messages.MessageEditBuilder();
            
            if (ephemeral) {
                mcb.setComponents(components).useComponentsV2(true).addEmbeds(banner);
                hook.sendMessage(mcb.build()).setEphemeral(true).queue();
            } else {
                meb.setComponents(components).useComponentsV2(true).setEmbeds(banner);
                hook.editOriginal(meb.build()).queue();
            }
        }
    }

    public static void sendStartupHub(Object target) {
        String body = "Welcome to High Core Agency. We provide elite digital solutions, from advanced development to high-end creative design. Use the options below to explore our services and interact with our team.";
        ActionRow row = ActionRow.of(
            Button.secondary("hub_highcore", "Map").withEmoji(Emoji.fromUnicode("\uD83D\uDDFA\uFE0F")),
            Button.secondary("hub_about", "About").withEmoji(Emoji.fromUnicode("\u2139\uFE0F")),
            Button.secondary("hub_partners", "Partners").withEmoji(Emoji.fromUnicode("\uD83E\uDDE1")),
            Button.link("https://discord.com/channels/1488795129996116212/1488798547947159612", "Support").withEmoji(Emoji.fromUnicode("\uD83D\uDEE1\uFE0F"))
        );
        reply(target, EmbedUtil.eliteContainer("High Core Agency", body, null, row));
    }

    public static void sendServerMap(Object target) {
        String body = "Main Rooms:\n- Startup: <#1488795130470072321>\n- Terms: <#1489158831916454070>\n- Updates: <#1488797040732278814>\n\nSupport:\n- Tickets: <#1488798547947159612>";
        ActionRow row = ActionRow.of(
            Button.success("hub_pings", "Notifications").withEmoji(Emoji.fromUnicode("\uD83D\uDD14")),
            Button.secondary("hub_rules", "Rules").withEmoji(Emoji.fromUnicode("\uD83D\uDCDC"))
        );
        replyEphemeral(target, EmbedUtil.eliteContainer("Server Map", body, null, row));
    }

    public static void sendAboutUs(Object target) {
        String body = "High Core is a premium digital agency specialized in creative identity and professional automation. We turn your vision into reality.";
        ActionRow row = ActionRow.of(
            Button.link("https://x.com/CoreHigh70331", "X"),
            Button.link("https://t.me/Beta_Team1/1", "Telegram").withEmoji(Emoji.fromUnicode("\u2708\uFE0F"))
        );
        replyEphemeral(target, EmbedUtil.eliteContainer("About Us", body, null, row));
    }

    public static void sendTicketPanel(Object target) {
        reply(target, EmbedUtil.eliteContainer("Help Center", "Please select the type of request you wish to open.", null,
                ActionRow.of(
                    Button.primary("ticket_init_support", "Support").withEmoji(Emoji.fromUnicode("\uD83D\uDEE1\uFE0F")),
                    Button.success("ticket_init_order", "Order").withEmoji(Emoji.fromUnicode("\uD83D\uDED2")),
                    Button.danger("ticket_init_complaint", "Complaint").withEmoji(Emoji.fromUnicode("\u26A0\uFE0F"))
                )
        ));
    }
}
