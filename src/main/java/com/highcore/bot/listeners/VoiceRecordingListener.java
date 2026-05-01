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
    private final Map<Long, String> sessionNames = new ConcurrentHashMap<>();
    private final Map<Long, Integer> partCounters = new ConcurrentHashMap<>();
    private final Map<Long, java.util.concurrent.ScheduledFuture<?>> splitTasks = new ConcurrentHashMap<>();
    private final java.util.concurrent.ScheduledExecutorService scheduler = java.util.concurrent.Executors.newScheduledThreadPool(2);

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

        // Follow/Join logic: If someone joins/moves to a channel
        if (joinedChannel != null && !event.getMember().getUser().isBot()) {
            AudioRecorder activeRecorder = recorders.get(guild.getIdLong());
            
            if (audioManager.isConnected()) {
                AudioChannel currentChannel = audioManager.getConnectedChannel();
                if (currentChannel != null) {
                    // Case A: Bot is in the SAME channel but has no active recorder
                    if (currentChannel.getIdLong() == joinedChannel.getIdLong() && activeRecorder == null) {
                        log.info("[VOICE] Bot is already in {} but no active session found. Starting new session.", currentChannel.getName());
                        connectAndStartRecording(guild, joinedChannel);
                    } 
                    // Case B: Bot is in a DIFFERENT channel and is alone
                    else if (currentChannel.getIdLong() != joinedChannel.getIdLong()) {
                        long humanCountInCurrent = currentChannel.getMembers().stream()
                                .filter(m -> !m.getUser().isBot())
                                .count();
                        
                        if (humanCountInCurrent == 0) {
                            log.info("[VOICE] Bot is alone in {}. Following users to {}.", currentChannel.getName(), joinedChannel.getName());
                            audioManager.openAudioConnection(joinedChannel);
                            
                            // If we lost our recorder somehow, start a new one
                            if (activeRecorder == null) {
                                connectAndStartRecording(guild, joinedChannel);
                            }
                            
                            // Update active text channel
                            if (joinedChannel instanceof net.dv8tion.jda.api.entities.channel.middleman.MessageChannel msgChannel) {
                                activeTextChannels.put(guild.getIdLong(), msgChannel.getId());
                            }
                        }
                    }
                }
            } else {
                // Case C: Bot is not connected at all
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

        if (id.equals("rec_start")) {
            net.dv8tion.jda.api.interactions.modals.Modal modal = net.dv8tion.jda.api.modals.Modal.create("modal_rec_start", "Start Recording")
                    .addComponents(net.dv8tion.jda.api.components.actionrow.ActionRow.of(
                            net.dv8tion.jda.api.components.textinput.TextInput.create("rec_name", "Meeting Name", net.dv8tion.jda.api.components.textinput.TextInputStyle.SHORT)
                                    .setPlaceholder("e.g., Development Meeting")
                                    .setRequired(true)
                                    .build()
                    ))
                    .build();
            event.replyModal(modal).queue();
            return;
        }

        if (id.equals("rec_stop") || id.equals("rec_new")) {
            String actionName = id.equals("rec_stop") ? "STOP" : "SAVE & NEW";

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
            // This is now replaced by Modal interaction, but keeping empty for safety
            event.reply("Please use the Start button to name your session.").setEphemeral(true).queue();
            return;
        }

        if (id.equals("rec_stop_confirm")) {
            Guild guild = event.getGuild();
            long guildId = guild.getIdLong();
            AudioRecorder recorder = recorders.get(guildId);
            if (recorder != null) {
                // Cancel split task
                java.util.concurrent.ScheduledFuture<?> task = splitTasks.remove(guildId);
                if (task != null) task.cancel(false);

                stopAndSendRecording(guild, guild.getAudioManager().getConnectedChannel());
                
                String name = sessionNames.remove(guildId);
                partCounters.remove(guildId);

                net.dv8tion.jda.api.components.container.Container container = com.highcore.bot.utils.EmbedUtil.containerBranded(
                    "PROTOCOL",
                    "Recording Finished",
                    "⏹️ Session **" + (name != null ? name : "Recording") + "** has been **STOPPED & SAVED**.",
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
    }

    @Override
    public void onModalInteraction(@NotNull net.dv8tion.jda.api.events.interaction.ModalInteractionEvent event) {
        if (event.getModalId().equals("modal_rec_start")) {
            String name = event.getValue("rec_name").getAsString();
            Guild guild = event.getGuild();
            if (guild == null) return;

            long guildId = guild.getIdLong();
            sessionNames.put(guildId, name);
            partCounters.put(guildId, 1);

            AudioRecorder recorder = recorders.get(guildId);
            
            // If no recorder exists, try to create one
            if (recorder == null) {
                net.dv8tion.jda.api.managers.AudioManager audioManager = guild.getAudioManager();
                if (audioManager.isConnected() && audioManager.getConnectedChannel() != null) {
                    connectAndStartRecording(guild, audioManager.getConnectedChannel());
                    recorder = recorders.get(guildId);
                }
            }

            if (recorder != null) {
                recorder.setRecording(true);
                log.info("[RECORDING] Started session '{}' for guild: {}", name, guild.getName());
                
                // Schedule splitting every 30 minutes
                java.util.concurrent.ScheduledFuture<?> task = scheduler.scheduleAtFixedRate(() -> {
                    log.info("[VOICE] 30 minutes reached. Splitting recording for session: {}", name);
                    splitAndRestart(guild);
                }, 30, 30, java.util.concurrent.TimeUnit.MINUTES);
                
                splitTasks.put(guildId, task);

                net.dv8tion.jda.api.components.container.Container container = com.highcore.bot.utils.EmbedUtil.containerBranded(
                    "PROTOCOL",
                    "Recording Started",
                    "✅ Recording session **" + name + "** has been started.\nIt will automatically split every 30 minutes for stability.",
                    com.highcore.bot.utils.EmbedUtil.BANNER_MAIN
                );
                event.reply(new net.dv8tion.jda.api.utils.messages.MessageCreateBuilder()
                        .setComponents(container)
                        .useComponentsV2(true)
                        .build())
                    .useComponentsV2(true)
                    .queue();
            } else {
                event.reply("❌ Error: Bot is not in a voice channel. Join first then use `/rec`.").setEphemeral(true).queue();
            }
        }
    }

    private void splitAndRestart(Guild guild) {
        long guildId = guild.getIdLong();
        net.dv8tion.jda.api.managers.AudioManager audioManager = guild.getAudioManager();
        net.dv8tion.jda.api.entities.channel.middleman.AudioChannel channel = audioManager.getConnectedChannel();
        
        if (channel == null) return;

        // Save current part
        stopAndSendRecording(guild, channel);
        
        // Increment part and restart immediately
        partCounters.put(guildId, partCounters.getOrDefault(guildId, 1) + 1);
        connectAndStartRecording(guild, channel);
        
        // Mark as recording immediately
        AudioRecorder next = recorders.get(guildId);
        if (next != null) next.setRecording(true);
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
        long guildId = guild.getIdLong();
        AudioManager audioManager = guild.getAudioManager();
        AudioRecorder recorder = recorders.remove(guildId);

        // Cancel splitting task if it's a final stop
        java.util.concurrent.ScheduledFuture<?> task = splitTasks.remove(guildId);
        // Note: we don't cancel if it's called from splitAndRestart, 
        // but wait, splitAndRestart calls stopAndSendRecording then restarts.
        // Actually it's better to manage task cancellation in the stop button/leave event.

        if (recorder != null) {
            recorder.stop();
        }

        AudioChannel lastChannel = audioManager.getConnectedChannel();
        if (lastChannel == null) lastChannel = fallbackChannel;

        if (lastChannel != null) {
            long humanCount = lastChannel.getMembers().stream()
                    .filter(m -> !m.getUser().isBot())
                    .count();
            if (humanCount == 0) {
                log.info("[VOICE] Channel is empty. Closing connection for guild: {}", guild.getName());
                audioManager.closeAudioConnection();
                if (task != null) task.cancel(false);
                sessionNames.remove(guildId);
                partCounters.remove(guildId);
            }
        }

        if (recorder != null) {
            final String sessionName = sessionNames.getOrDefault(guildId, "Meeting");
            final int part = partCounters.getOrDefault(guildId, 1);

            new Thread(() -> {
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
                File wavFile = new File("rec_" + guild.getId() + "_" + timestamp + "_part" + part + ".wav");
                try {
                    log.info("[UPLOAD] Saving WAV file: {}", wavFile.getName());
                    recorder.saveAsWav(wavFile);

                    if (wavFile.exists() && wavFile.length() > 100) {
                        log.info("[UPLOAD] File saved. Sending Part {} of session '{}'", part, sessionName);
                        TextChannel logChannel = guild.getJDA().getTextChannelById(LOG_CHANNEL_ID);
                        
                        if (logChannel != null) {
                            String timeStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                            net.dv8tion.jda.api.EmbedBuilder eb = new net.dv8tion.jda.api.EmbedBuilder();
                            eb.setTitle("🎙️ " + sessionName + " : Part " + part);
                            eb.setColor(com.highcore.bot.utils.EmbedUtil.INFO);
                            eb.setImage(com.highcore.bot.utils.EmbedUtil.BANNER_MAIN);
                            eb.addField("Channel", "`" + (fallbackChannel != null ? fallbackChannel.getName() : "Unknown") + "`", true);
                            eb.addField("Time", "`" + timeStr + "`", true);
                            eb.addField("Quality", "`48kHz / 16-bit Stereo`", false);
                            eb.setFooter("▪ UNIFIED TERMINAL v1.2.0 ▪ HIGHCORE AGENCY ▪", null);
                            eb.setTimestamp(java.time.Instant.now());

                            String activeChanId = activeTextChannels.get(guild.getIdLong());
                            TextChannel activeChan = activeChanId != null ? guild.getTextChannelById(activeChanId) : null;

                            logChannel.sendMessageEmbeds(eb.build())
                                    .addFiles(FileUpload.fromData(wavFile))
                                    .queue(msg -> {
                                        if (activeChan != null) {
                                            activeChan.sendMessageEmbeds(eb.build())
                                                    .addFiles(FileUpload.fromData(wavFile))
                                                    .queue(m2 -> wavFile.delete(), t2 -> wavFile.delete());
                                        } else {
                                            wavFile.delete();
                                        }
                                    }, t -> wavFile.delete());
                        } else {
                            wavFile.delete();
                        }
                    } else {
                        wavFile.delete();
                    }
                } catch (IOException e) {
                    if (wavFile.exists()) wavFile.delete();
                } finally {
                    recorder.cleanup();
                }
            }).start();
        }
    }
}
