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

        // Join and start recording if someone joins/switches to a channel and bot is not connected
        if (joinedChannel != null && joinedChannel != leftChannel && !event.getMember().getUser().isBot()) {
            if (!audioManager.isConnected()) {
                connectAndStartRecording(guild, joinedChannel);
            }
        }

        // Stop and send recording if the last human leaves the channel the bot is in
        if (leftChannel != null && leftChannel != joinedChannel) {
            if (audioManager.isConnected() && leftChannel.equals(audioManager.getConnectedChannel())) {
                long humanCount = leftChannel.getMembers().stream()
                        .filter(m -> !m.getUser().isBot())
                        .count();
                
                if (humanCount == 0) {
                    stopAndSendRecording(guild);
                }
            }
        }
        // Detect if the bot itself is disconnected unexpectedly
        if (event.getMember().equals(guild.getSelfMember())) {
            if (leftChannel != null && joinedChannel == null) {
                if (recorders.containsKey(guild.getIdLong())) {
                    stopAndSendRecording(guild);
                }
            }
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
            audioManager.closeAudioConnection();
            
            new Thread(() -> {
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
                File wavFile = new File("rec_" + guild.getId() + "_" + timestamp + ".wav");
                try {
                    recorder.saveAsWav(wavFile);
                    
                    // Only send if the file has actual audio data (WAV header is 44 bytes)
                    if (wavFile.exists() && wavFile.length() > 1000) { 
                        TextChannel logChannel = guild.getJDA().getTextChannelById(LOG_CHANNEL_ID);
                        if (logChannel != null) {
                            String timeStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                            
                            net.dv8tion.jda.api.EmbedBuilder eb = new net.dv8tion.jda.api.EmbedBuilder();
                            eb.setTitle("\uD83C\uDF99\uFE0F Voice Recording Finished");
                            eb.setColor(com.highcore.bot.utils.EmbedUtil.INFO);
                            eb.setImage(com.highcore.bot.utils.EmbedUtil.BANNER_MAIN);
                            eb.addField("Channel", "`" + (lastChannel != null ? lastChannel.getName() : "Unknown") + "`", true);
                            eb.addField("Time", "`" + timeStr + "`", true);
                            eb.addField("Quality", "`48kHz / 16-bit Stereo`", false);
                            eb.setFooter("\u25AA UNIFIED TERMINAL v1.2.0 \u30FB HIGHCORE AGENCY \u25AA", null);
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
                    if (wavFile.exists()) wavFile.delete();
                } finally {
                    recorder.cleanup();
                }
            }).start();
        }
    }
}
