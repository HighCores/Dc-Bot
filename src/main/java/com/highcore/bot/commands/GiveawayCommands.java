package com.highcore.bot.commands;

import com.highcore.bot.services.LogManager;
import com.highcore.bot.services.PanelService;
import com.highcore.bot.utils.EmbedUtil;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.modals.Modal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class GiveawayCommands extends ListenerAdapter {

    private static final Logger log = LoggerFactory.getLogger(GiveawayCommands.class);
    private static final Map<String, List<String>> entries = new ConcurrentHashMap<>();

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("giveaway")) return;
        if (!event.getMember().hasPermission(net.dv8tion.jda.api.Permission.MANAGE_SERVER)) {
            PanelService.replyEphemeral(event, EmbedUtil.accessDenied());
            return;
        }

        TextInput prize = TextInput.create("prize", "Prize", TextInputStyle.SHORT)
                .setPlaceholder("e.g. Nitro Basic")
                .setRequired(true)
                .build();
        TextInput winners = TextInput.create("winners", "Number of Winners", TextInputStyle.SHORT)
                .setPlaceholder("e.g. 1")
                .setRequired(true)
                .build();
        TextInput time = TextInput.create("duration", "Duration (minutes)", TextInputStyle.SHORT)
                .setPlaceholder("e.g. 60")
                .setRequired(true)
                .build();

        Modal modal = Modal.create("modal_giveaway", "GIVEAWAY CONFIG")
                .addActionRow(prize)
                .addActionRow(winners)
                .addActionRow(time)
                .build();

        event.replyModal(modal).queue();
    }

    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        if (!event.getModalId().equals("modal_giveaway")) return;
        
        String prize = event.getValue("prize").getAsString();
        int winCount = Integer.parseInt(event.getValue("winners").getAsString());
        int duration = Integer.parseInt(event.getValue("duration").getAsString());

        String giveawayId = "gw_" + System.currentTimeMillis();
        entries.put(giveawayId, new ArrayList<>());

        net.dv8tion.jda.api.components.buttons.Button joinBtn = net.dv8tion.jda.api.components.buttons.Button.primary("gw_join_" + giveawayId, "Join Giveaway")
                .withEmoji(net.dv8tion.jda.api.entities.emoji.Emoji.fromUnicode("\uD83C\uDF89"));

        event.replyEmbeds(EmbedUtil.giveaway(prize, winCount, duration).build())
                .addActionRow(joinBtn)
                .queue(hook -> {
                    com.highcore.bot.services.LogManager.log(event.getGuild(), "GIVEAWAY STARTED", 
                            "Prize: " + prize + "\nAdmin: " + event.getUser().getAsMention(), EmbedUtil.INFO);
                    
                    event.getGuild().getJDA().getExecutorService().schedule(() -> {
                        List<String> participants = entries.get(giveawayId);
                        if (participants == null || participants.isEmpty()) {
                            event.getChannel().sendMessage("Giveaway for **" + prize + "** ended with no participants!").queue();
                            return;
                        }

                        Random r = new Random();
                        List<String> winners = new ArrayList<>();
                        for (int i = 0; i < winCount && !participants.isEmpty(); i++) {
                            int idx = r.nextInt(participants.size());
                            winners.add("<@" + participants.remove(idx) + ">");
                        }

                        String msg = "### \uD83C\uDF89 GIVEAWAY ENDED\n**Prize:** " + prize + "\n**Winners:** " + String.join(", ", winners);
                        event.getChannel().sendMessage(msg).queue();
                        entries.remove(giveawayId);
                    }, duration, TimeUnit.MINUTES);
                });
    }

    @Override
    public void onButtonInteraction(net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent event) {
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
