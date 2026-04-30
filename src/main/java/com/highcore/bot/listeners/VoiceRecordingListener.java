package com.highcore.bot.listeners;

import com.highcore.bot.audio.AudioRecorder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.managers.AudioManager;
import net.dv8tion.jda.api.utils.FileUpload;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Section: Voice Recording Listener
 * Human-generated listener for recording voice conversations in Highcore Bot
 */
public class VoiceRecordingListener extends ListenerAdapter {
    private static final String LOG_CHANNEL_ID = "1499416739597914122";
    private final Map<Long, AudioRecorder> recorders = new ConcurrentHashMap<>();

    @Override
    public void onGuildVoiceUpdate(@NotNull GuildVoiceUpdateEvent event) {
        Guild guild = event.getGuild();
        AudioManager audioManager = guild.getAudioManager();
        AudioChannel joinedChannel = event.getChannelJoined();
        AudioChannel leftChannel = event.getChannelLeft();

        // Join and start recording if someone joins/switches to a channel and bot is
        // not connected
        if (joinedChannel != null && (leftChannel == null || joinedChannel.getIdLong() != leftChannel.getIdLong())
                && !event.getMember().getUser().isBot()) {
            if (!audioManager.isConnected()) {
                connectAndStartRecording(guild, joinedChannel);

                // Send control panel
                if (joinedChannel instanceof net.dv8tion.jda.api.entities.channel.middleman.MessageChannel msgChannel) {
                    net.dv8tion.jda.api.components.container.Container container = com.highcore.bot.utils.EmbedUtil.containerBranded(
                        "PROTOCOL", 
                        "Recording System",
                        "Control the audio recording for this channel.\nRecording is currently **PAUSED**.\nClick **Start** to begin.",
                        com.highcore.bot.utils.EmbedUtil.BANNER_MAIN,
                        net.dv8tion.jda.api.components.actionrow.ActionRow.of(
                            net.dv8tion.jda.api.components.buttons.Button.success("rec_start", "Start"),
                            net.dv8tion.jda.api.components.buttons.Button.danger("rec_stop", "Stop"),
                            net.dv8tion.jda.api.components.buttons.Button.primary("rec_new", "New Record")
                        )
                    );
                    
                    msgChannel.sendMessage(new net.dv8tion.jda.api.utils.messages.MessageCreateBuilder().setComponents(container).build())
                        .useComponentsV2(true)
                        .queue();
                }
            }
        }

        // Stop and send recording if the last human leaves the channel the bot is in
        if (leftChannel != null && (joinedChannel == null || leftChannel.getIdLong() != joinedChannel.getIdLong())) {
            AudioChannel connectedChannel = audioManager.getConnectedChannel();
            if (audioManager.isConnected() && connectedChannel != null && leftChannel.getIdLong() == connectedChannel.getIdLong()) {
                long humanCount = leftChannel.getMembers().stream()
                        .filter(m -> !m.getUser().isBot())
                        .count();
                
                if (humanCount == 0) {
                    stopAndSendRecording(guild, connectedChannel);
                }
            }
        }
        // Detect if the bot itself is disconnected unexpectedly
        if (event.getMember().equals(guild.getSelfMember())) {
            if (leftChannel != null && joinedChannel == null) {
                if (recorders.containsKey(guild.getIdLong())) {
                    stopAndSendRecording(guild, leftChannel);
                }
            }
        }
    }

    @Override
    public void onButtonInteraction(
            @NotNull net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent event) {
        String id = event.getComponentId();

        if (id.equals("rec_start") || id.equals("rec_stop") || id.equals("rec_new")) {
            String actionName = id.equals("rec_start") ? "START" : id.equals("rec_stop") ? "STOP" : "SAVE & NEW";

            net.dv8tion.jda.api.components.container.Container container = com.highcore.bot.utils.EmbedUtil.containerBranded(
                "VERIFY",
                "Action Confirmation",
                "Are you sure you want to **" + actionName + "** the recording?",
                com.highcore.bot.utils.EmbedUtil.BANNER_MAIN,
                net.dv8tion.jda.api.components.actionrow.ActionRow.of(
                        net.dv8tion.jda.api.components.buttons.Button.success(id + "_confirm", "Confirm"),
                        net.dv8tion.jda.api.components.buttons.Button.danger("rec_cancel", "Cancel")
                )
            );

            event.reply(new net.dv8tion.jda.api.utils.messages.MessageCreateBuilder().setComponents(container).build())
                    .setEphemeral(true)
                    .useComponentsV2(true)
                    .queue();
            return;
        }

        if (id.equals("rec_cancel")) {
            event.editMessage("Action cancelled.").setComponents().queue();
            return;
        }

        if (id.equals("rec_start_confirm")) {
            Guild guild = event.getGuild();
            AudioRecorder recorder = recorders.get(guild.getIdLong());
            if (recorder != null) {
                recorder.setRecording(true);
                event.editMessage("✅ Recording **STARTED**.").setComponents().queue();
            } else {
                event.editMessage("❌ Bot is not currently recording in a voice channel.").setComponents().queue();
            }
            return;
        }

        if (id.equals("rec_stop_confirm")) {
            Guild guild = event.getGuild();
            AudioRecorder recorder = recorders.get(guild.getIdLong());
            if (recorder != null) {
                recorder.setRecording(false);
                event.editMessage("⏸️ Recording **STOPPED**.").setComponents().queue();
            } else {
                event.editMessage("❌ Bot is not currently recording in a voice channel.").setComponents().queue();
            }
            return;
        }

        if (id.equals("rec_new_confirm")) {
            Guild guild = event.getGuild();
            AudioRecorder recorder = recorders.get(guild.getIdLong());
            if (recorder != null) {
                stopAndSendRecording(guild);

                net.dv8tion.jda.api.managers.AudioManager audioManager = guild.getAudioManager();
                if (audioManager.isConnected() && audioManager.getConnectedChannel() != null) {
                    connectAndStartRecording(guild, audioManager.getConnectedChannel());
                    event.editMessage("🔄 Recording saved. A new session is ready. Click **Start** to begin recording.")
                            .setComponents().queue();
                } else {
                    event.editMessage("❌ Bot is not connected anymore.").setComponents().queue();
                }
            } else {
                event.editMessage("❌ No active recording found.").setComponents().queue();
            }
            return;
        }
    }

    private void connectAndStartRecording(Guild guild, AudioChannel channel) {
        try {
            AudioRecorder recorder = new AudioRecorder();
            AudioManager audioManager = guild.getAudioManager();
            audioManager.setReceivingHandler(recorder);
            audioManager.openAudioConnection(channel);
            recorders.put(guild.getIdLong(), recorder);
        } catch (IOException e) {
            // Error handled silently
        }
    }

    private void stopAndSendRecording(Guild guild) {
        AudioManager audioManager = guild.getAudioManager();
        AudioRecorder recorder = recorders.remove(guild.getIdLong());

        if (recorder != null) {
            recorder.stop();
            AudioChannel lastChannel = audioManager.getConnectedChannel();
            // Note: Don't close connection here, so the bot can stay if it's "New Record"
            // Wait! The auto-leave logic calls stopAndSendRecording and we DO want it to
            // leave.
            // If it's "New Record", we reconnect immediately. But wait, closing and
            // reopening might cause a blip.
            // Let's modify stopAndSendRecording to close connection only if humanCount ==
            // 0?
            // Actually, we can check if humans are still there.
            boolean shouldLeave = true;
            if (lastChannel != null) {
                long humanCount = lastChannel.getMembers().stream()
                        .filter(m -> !m.getUser().isBot())
                        .count();
                if (humanCount > 0)
                    shouldLeave = false;
            }

            if (shouldLeave) {
                audioManager.closeAudioConnection();
            }

            new Thread(() -> {
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
                File wavFile = new File("rec_" + guild.getId() + "_" + timestamp + ".wav");
                try {
                    recorder.saveAsWav(wavFile);

                    // Only send if the file has actual audio data (WAV header is 44 bytes)
                    if (wavFile.exists() && wavFile.length() > 100) {
                        TextChannel logChannel = guild.getJDA().getTextChannelById(LOG_CHANNEL_ID);
                        if (logChannel != null) {
                            String timeStr = LocalDateTime.now()
                                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

                            net.dv8tion.jda.api.EmbedBuilder eb = new net.dv8tion.jda.api.EmbedBuilder();
                            eb.setTitle("🎙️ Voice Recording Finished");
                            eb.setColor(com.highcore.bot.utils.EmbedUtil.INFO);
                            eb.setImage(com.highcore.bot.utils.EmbedUtil.BANNER_MAIN);
                            eb.addField("Channel",
                                    "`" + (lastChannel != null ? lastChannel.getName() : "Unknown") + "`", true);
                            eb.addField("Time", "`" + timeStr + "`", true);
                            eb.addField("Quality", "`48kHz / 16-bit Stereo`", false);
                            eb.setFooter("▪ UNIFIED TERMINAL v1.2.0 ▪ HIGHCORE AGENCY ▪", null);
                            eb.setTimestamp(java.time.Instant.now());

                            logChannel.sendMessageEmbeds(eb.build())
                                    .addFiles(FileUpload.fromData(wavFile))
                                    .queue(m -> wavFile.delete(), t -> wavFile.delete());
                        } else {
                            wavFile.delete();
                        }
                    } else {
                        wavFile.delete();
                    }
                } catch (IOException e) {
                    if (wavFile.exists())
                        wavFile.delete();
                } finally {
                    recorder.cleanup();
                }
            }).start();
        }
    }
}
