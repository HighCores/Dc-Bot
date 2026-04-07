package com.highcore.bot.commands;

import com.highcore.bot.services.LogManager;
import com.highcore.bot.services.PanelService;
import com.highcore.bot.utils.EmbedUtil;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.modals.Modal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.*;

public class GiveawayCommands extends ListenerAdapter {

    private static final Logger log = LoggerFactory.getLogger(GiveawayCommands.class);
    private static final Map<String, List<String>> entries = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("giveaway")) return;
        if (!event.getMember().hasPermission(net.dv8tion.jda.api.Permission.MANAGE_SERVER)) {
            PanelService.replyEphemeral(event, EmbedUtil.accessDenied());
            return;
        }

        // JDA 6.4.1 GOLDEN PATTERN: Label.of("Text", input)
        TextInput prizeInput = TextInput.create("prize", "Prize", TextInputStyle.SHORT)
                .setPlaceholder("e.g. Nitro Basic")
                .setRequired(true)
                .build();

        TextInput winnersInput = TextInput.create("winners", "Number of Winners", TextInputStyle.SHORT)
                .setPlaceholder("e.g. 1")
                .setRequired(true)
                .build();

        TextInput timeInput = TextInput.create("duration", "Duration (minutes)", TextInputStyle.SHORT)
                .setPlaceholder("e.g. 60")
                .setRequired(true)
                .build();

        Modal modal = Modal.create("modal_giveaway", "GIVEAWAY CONFIG")
                .addComponents(Label.of("Prize", prizeInput))
                .addComponents(Label.of("Winners Count", winnersInput))
                .addComponents(Label.of("Duration (Min)", timeInput))
                .build();

        event.replyModal(modal).queue();
    }

    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        if (!event.getModalId().equals("modal_giveaway")) return;
        
        String prizeStr = event.getValue("prize").getAsString();
        int winCount = Integer.parseInt(event.getValue("winners").getAsString());
        int duration = Integer.parseInt(event.getValue("duration").getAsString());

        String giveawayId = "gw_" + System.currentTimeMillis();
        entries.put(giveawayId, new ArrayList<>());

        net.dv8tion.jda.api.components.buttons.Button joinBtn = net.dv8tion.jda.api.components.buttons.Button.primary("gw_join_" + giveawayId, "Join Giveaway")
                .withEmoji(net.dv8tion.jda.api.entities.emoji.Emoji.fromUnicode("\uD83C\uDF89"));

        // JDA 6.4.1: Container.of must be sent via replyComponents (NO .build())
        event.replyComponents(EmbedUtil.giveaway(prizeStr, winCount, duration))
                .addActionRow(joinBtn)
                .queue(hook -> {
                    LogManager.log(event.getGuild(), "GIVEAWAY STARTED", 
                            "Prize: " + prizeStr + "\nAdmin: " + event.getUser().getAsMention(), EmbedUtil.INFO);
                    
                    scheduler.schedule(() -> {
                        List<String> participants = entries.get(giveawayId);
                        if (participants == null || participants.isEmpty()) {
                            event.getChannel().sendMessage("Giveaway for **" + prizeStr + "** ended with no participants!").queue();
                            return;
                        }

                        Random r = new Random();
                        List<String> winnerMentions = new ArrayList<>();
                        for (int i = 0; i < winCount && !participants.isEmpty(); i++) {
                            int idx = r.nextInt(participants.size());
                            winnerMentions.add("<@" + participants.remove(idx) + ">");
                        }

                        String msg = "### \uD83C\uDF89 GIVEAWAY ENDED\n**Prize:** " + prizeStr + "\n**Winners:** " + String.join(", ", winnerMentions);
                        event.getChannel().sendMessage(msg).queue();
                        entries.remove(giveawayId);
                    }, duration, TimeUnit.MINUTES);
                });
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String id = event.getComponentId();
        if (id.startsWith("gw_join_")) {
            String gwId = id.replace("gw_join_", "");
            List<String> participants = entries.get(gwId);
            if (participants == null) {
                event.reply("This giveaway has already ended.").setEphemeral(true).queue();
                return;
            }
            if (participants.contains(event.getUser().getId())) {
                event.reply("You have already joined this giveaway.").setEphemeral(true).queue();
                return;
            }
            participants.add(event.getUser().getId());
            event.reply("You have successfully joined the giveaway!").setEphemeral(true).queue();
        }
    }
}
