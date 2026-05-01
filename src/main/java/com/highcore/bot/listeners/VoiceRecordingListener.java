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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Section: Voice Recording Listener
 * Human-generated listener for recording voice conversations in Highcore Bot
 */
public class VoiceRecordingListener extends ListenerAdapter {
    private static final Logger log = LoggerFactory.getLogger(VoiceRecordingListener.class);
    private static final String LOG_CHANNEL_ID = "1499416739597914122";
    private final Map<Long, AudioRecorder> recorders = new ConcurrentHashMap<>();
    private final Map<Long, String> activeTextChannels = new ConcurrentHashMap<>();

    @Override
    public void onSlashCommandInteraction(@NotNull net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent event) {
        if (event.getName().equals("rec")) {
            Guild guild = event.getGuild();
            if (guild == null) return;
            
            AudioChannel channel = guild.getSelfMember().getVoiceState().getChannel();
            if (channel == null) {
                event.reply("❌ The bot is not currently in a voice channel. Join a channel first to start recording.").setEphemeral(true).queue();
                return;
            }
            
            activeTextChannels.put(guild.getIdLong(), event.getChannel().getId());
            
            net.dv8tion.jda.api.components.container.Container container = com.highcore.bot.utils.EmbedUtil.containerBranded(
                "PROTOCOL", 
                "Recording System",
                "Control the audio recording for this channel.\nRecording is currently **PAUSED**.\nClick **Start** to begin.",
                com.highcore.bot.utils.EmbedUtil.BANNER_MAIN,
                net.dv8tion.jda.api.components.actionrow.ActionRow.of(
                    net.dv8tion.jda.api.components.buttons.Button.secondary("rec_start", "Start"),
                    net.dv8tion.jda.api.components.buttons.Button.secondary("rec_stop", "Stop"),
                    net.dv8tion.jda.api.components.buttons.Button.secondary("rec_new", "New Record")
                )
            );
            
            event.reply(new net.dv8tion.jda.api.utils.messages.MessageCreateBuilder()
                    .setComponents(container)
                    .useComponentsV2(true)
                    .build())
                .useComponentsV2(true)
                .queue();
        }
    }

    @Override
    public void onGuildVoiceUpdate(@NotNull GuildVoiceUpdateEvent event) {
        Guild guild = event.getGuild();
        AudioManager audioManager = guild.getAudioManager();
        AudioChannel joinedChannel = event.getChannelJoined();
        AudioChannel leftChannel = event.getChannelLeft();


        // Stop and send recording if the last human leaves the channel the bot is in
        if (leftChannel != null) {
            AudioChannel connectedChannel = audioManager.getConnectedChannel();
            if (connectedChannel != null && leftChannel.getIdLong() == connectedChannel.getIdLong()) {
                long humanCount = connectedChannel.getMembers().stream()
                        .filter(m -> !m.getUser().isBot())
                        .count();
                
                if (humanCount == 0) {
                    log.info("[VOICE] Last human left channel {}. Stopping and sending recording.", leftChannel.getName());
                    stopAndSendRecording(guild, connectedChannel);
                }
            }
        }

        // Follow logic: If someone joins/moves to a channel and the bot is ALONE in its current channel
        if (joinedChannel != null && !event.getMember().getUser().isBot()) {
            if (audioManager.isConnected()) {
                AudioChannel currentChannel = audioManager.getConnectedChannel();
                if (currentChannel != null && currentChannel.getIdLong() != joinedChannel.getIdLong()) {
                    long humanCountInCurrent = currentChannel.getMembers().stream()
                            .filter(m -> !m.getUser().isBot())
                            .count();
                    
                    if (humanCountInCurrent == 0) {
                        log.info("[VOICE] Bot is alone in {}. Following users to {}.", currentChannel.getName(), joinedChannel.getName());
                        audioManager.openAudioConnection(joinedChannel);
                        
                        // Update active text channel if possible
                        if (joinedChannel instanceof net.dv8tion.jda.api.entities.channel.middleman.MessageChannel msgChannel) {
                            activeTextChannels.put(guild.getIdLong(), msgChannel.getId());
                        }
                    }
                }
            } else {
                // Join and start recording if bot is not connected at all
                connectAndStartRecording(guild, joinedChannel);

                // Send control panel
                if (joinedChannel instanceof net.dv8tion.jda.api.entities.channel.middleman.MessageChannel msgChannel) {
                    activeTextChannels.put(guild.getIdLong(), msgChannel.getId());
                    net.dv8tion.jda.api.components.container.Container container = com.highcore.bot.utils.EmbedUtil.containerBranded(
                        "PROTOCOL", 
                        "Recording System",
                        "Control the audio recording for this channel.\nRecording is currently **PAUSED**.\nClick **Start** to begin.",
                        com.highcore.bot.utils.EmbedUtil.BANNER_MAIN,
                        net.dv8tion.jda.api.components.actionrow.ActionRow.of(
                            net.dv8tion.jda.api.components.buttons.Button.secondary("rec_start", "Start"),
                            net.dv8tion.jda.api.components.buttons.Button.secondary("rec_stop", "Stop"),
                            net.dv8tion.jda.api.components.buttons.Button.secondary("rec_new", "New Record")
                        )
                    );
                    
                    msgChannel.sendMessage(new net.dv8tion.jda.api.utils.messages.MessageCreateBuilder()
                            .setComponents(container)
                                .useComponentsV2(true)
                                .build())
                            .useComponentsV2(true)
                            .queue();
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

            event.reply(new net.dv8tion.jda.api.utils.messages.MessageCreateBuilder()
                            .setComponents(container)
                            .useComponentsV2(true)
                            .build())
                    .setEphemeral(false)
                    .useComponentsV2(true)
                    .queue();
            return;
        }

        if (id.equals("rec_cancel")) {
            net.dv8tion.jda.api.components.container.Container container = com.highcore.bot.utils.EmbedUtil.containerBranded(
                "VERIFY",
                "Action Cancelled",
                "The recording action was cancelled.",
                com.highcore.bot.utils.EmbedUtil.BANNER_MAIN
            );
            event.editMessage(new net.dv8tion.jda.api.utils.messages.MessageEditBuilder()
                    .setComponents(container)
                    .useComponentsV2(true)
                    .build()).queue();
            return;
        }

        if (id.equals("rec_start_confirm")) {
            Guild guild = event.getGuild();
            AudioRecorder recorder = recorders.get(guild.getIdLong());
            if (recorder != null) {
                recorder.setRecording(true);
                log.info("[RECORDING] Started session for guild: {}", guild.getName());
                net.dv8tion.jda.api.components.container.Container container = com.highcore.bot.utils.EmbedUtil.containerBranded(
                    "PROTOCOL",
                    "Recording Started",
                    "✅ The recording session has been **STARTED**.",
                    com.highcore.bot.utils.EmbedUtil.BANNER_MAIN
                );
                event.editMessage(new net.dv8tion.jda.api.utils.messages.MessageEditBuilder()
                        .setComponents(container)
                        .useComponentsV2(true)
                        .build()).queue();
            } else {
                net.dv8tion.jda.api.components.container.Container container = com.highcore.bot.utils.EmbedUtil.containerBranded(
                    "ERROR",
                    "Recording Error",
                    "❌ Bot is not currently recording in a voice channel.",
                    com.highcore.bot.utils.EmbedUtil.BANNER_MAIN
                );
                event.editMessage(new net.dv8tion.jda.api.utils.messages.MessageEditBuilder()
                        .setComponents(container)
                        .useComponentsV2(true)
                        .build()).queue();
            }
            return;
        }

        if (id.equals("rec_stop_confirm")) {
            Guild guild = event.getGuild();
            AudioRecorder recorder = recorders.get(guild.getIdLong());
            if (recorder != null) {
                stopAndSendRecording(guild, guild.getAudioManager().getConnectedChannel());
                net.dv8tion.jda.api.components.container.Container container = com.highcore.bot.utils.EmbedUtil.containerBranded(
                    "PROTOCOL",
                    "Recording Finished",
                    "⏹️ The recording session has been **STOPPED & SAVED**.",
                    com.highcore.bot.utils.EmbedUtil.BANNER_MAIN
                );
                event.editMessage(new net.dv8tion.jda.api.utils.messages.MessageEditBuilder()
                        .setComponents(container)
                        .useComponentsV2(true)
                        .build()).queue();
            } else {
                net.dv8tion.jda.api.components.container.Container container = com.highcore.bot.utils.EmbedUtil.containerBranded(
                    "ERROR",
                    "Recording Error",
                    "❌ Bot is not currently recording in a voice channel.",
                    com.highcore.bot.utils.EmbedUtil.BANNER_MAIN
                );
                event.editMessage(new net.dv8tion.jda.api.utils.messages.MessageEditBuilder()
                        .setComponents(container)
                        .useComponentsV2(true)
                        .build()).queue();
            }
            return;
        }

        if (id.equals("rec_new_confirm")) {
            Guild guild = event.getGuild();
            AudioRecorder recorder = recorders.get(guild.getIdLong());
            if (recorder != null) {
                net.dv8tion.jda.api.managers.AudioManager audioManager = guild.getAudioManager();
                AudioChannel connectedChannel = audioManager.getConnectedChannel();
                stopAndSendRecording(guild, connectedChannel);
                
                if (audioManager.isConnected() && connectedChannel != null) {
                    connectAndStartRecording(guild, audioManager.getConnectedChannel());
                    net.dv8tion.jda.api.components.container.Container container = com.highcore.bot.utils.EmbedUtil.containerBranded(
                        "PROTOCOL",
                        "New Session",
                        "🔄 Recording saved. A new session is ready. Click **Start** to begin recording.",
                        com.highcore.bot.utils.EmbedUtil.BANNER_MAIN
                    );
                    event.editMessage(new net.dv8tion.jda.api.utils.messages.MessageEditBuilder()
                            .setComponents(container)
                            .useComponentsV2(true)
                            .build()).queue();
                } else {
                    net.dv8tion.jda.api.components.container.Container container = com.highcore.bot.utils.EmbedUtil.containerBranded(
                        "ERROR",
                        "Connection Error",
                        "❌ Bot is not connected anymore.",
                        com.highcore.bot.utils.EmbedUtil.BANNER_MAIN
                    );
                    event.editMessage(new net.dv8tion.jda.api.utils.messages.MessageEditBuilder()
                            .setComponents(container)
                            .useComponentsV2(true)
                            .build()).queue();
                }
            } else {
                net.dv8tion.jda.api.components.container.Container container = com.highcore.bot.utils.EmbedUtil.containerBranded(
                    "ERROR",
                    "Recording Error",
                    "❌ No active recording found.",
                    com.highcore.bot.utils.EmbedUtil.BANNER_MAIN
                );
                event.editMessage(new net.dv8tion.jda.api.utils.messages.MessageEditBuilder()
                        .setComponents(container)
                        .useComponentsV2(true)
                        .build()).queue();
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
            log.info("[VOICE] Bot connected and ready to record in guild: {}", guild.getName());
        } catch (IOException e) {
            log.error("[VOICE] Failed to initialize recorder: {}", e.getMessage());
        }
    }

    private void stopAndSendRecording(Guild guild, AudioChannel fallbackChannel) {
        AudioManager audioManager = guild.getAudioManager();
        AudioRecorder recorder = recorders.remove(guild.getIdLong());

        if (recorder != null) {
            recorder.stop();
            AudioChannel lastChannel = audioManager.getConnectedChannel();
            if (lastChannel == null) lastChannel = fallbackChannel;

            // Note: Don't close connection here, so the bot can stay if it's "New Record"
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
                    log.info("[UPLOAD] Saving WAV file: {}", wavFile.getName());
                    recorder.saveAsWav(wavFile);

                    if (wavFile.exists() && wavFile.length() > 100) {
                        log.info("[UPLOAD] File saved (Size: {} bytes). Searching for log channel...", wavFile.length());
                        TextChannel logChannel = guild.getJDA().getTextChannelById(LOG_CHANNEL_ID);
                        
                        if (logChannel != null) {
                            String timeStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                            net.dv8tion.jda.api.EmbedBuilder eb = new net.dv8tion.jda.api.EmbedBuilder();
                            eb.setTitle("🎙️ Voice Recording Finished");
                            eb.setColor(com.highcore.bot.utils.EmbedUtil.INFO);
                            eb.setImage(com.highcore.bot.utils.EmbedUtil.BANNER_MAIN);
                            eb.addField("Channel", "`" + (fallbackChannel != null ? fallbackChannel.getName() : "Unknown") + "`", true);
                            eb.addField("Time", "`" + timeStr + "`", true);
                            eb.addField("Quality", "`48kHz / 16-bit Stereo`", false);
                            eb.setFooter("▪ UNIFIED TERMINAL v1.2.0 ▪ HIGHCORE AGENCY ▪", null);
                            eb.setTimestamp(java.time.Instant.now());

                            String activeChanId = activeTextChannels.get(guild.getIdLong());
                            TextChannel activeChan = activeChanId != null ? guild.getTextChannelById(activeChanId) : null;

                            log.info("[UPLOAD] Sending to LOG channel: {}", logChannel.getName());
                            
                            logChannel.sendMessageEmbeds(eb.build())
                                    .addFiles(FileUpload.fromData(wavFile))
                                    .queue(msg -> {
                                        log.info("[UPLOAD] Successfully sent to LOG channel.");
                                        // After log channel, try sending to active channel if exists
                                        if (activeChan != null) {
                                            log.info("[UPLOAD] Sending to active channel: {}", activeChan.getName());
                                            activeChan.sendMessageEmbeds(eb.build())
                                                    .addFiles(FileUpload.fromData(wavFile))
                                                    .queue(m2 -> {
                                                        log.info("[UPLOAD] Successfully sent to active channel. Deleting local file.");
                                                        wavFile.delete();
                                                    }, t2 -> {
                                                        log.error("[UPLOAD] Failed to send to active channel: {}", t2.getMessage());
                                                        wavFile.delete();
                                                    });
                                        } else {
                                            log.info("[UPLOAD] No active text channel found. Deleting local file.");
                                            wavFile.delete();
                                        }
                                    }, t -> {
                                        log.error("[UPLOAD] Failed to send to LOG channel: {}", t.getMessage());
                                        wavFile.delete();
                                    });
                        } else {
                            log.error("[UPLOAD] Log channel with ID {} not found!", LOG_CHANNEL_ID);
                            wavFile.delete();
                        }
                    } else {
                        log.warn("[UPLOAD] Recording too short or empty. Deleting.");
                        wavFile.delete();
                    }
                } catch (IOException e) {
                    log.error("[UPLOAD] IO Error during WAV conversion: {}", e.getMessage());
                    if (wavFile.exists()) wavFile.delete();
                } finally {
                    recorder.cleanup();
                }
            }).start();
        }
    }
}
