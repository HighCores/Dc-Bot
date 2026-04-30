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

        // Join and start recording if someone joins a channel and bot is not connected
        if (joinedChannel != null && !event.getMember().getUser().isBot()) {
            if (!audioManager.isConnected()) {
                connectAndStartRecording(guild, joinedChannel);
            }
        }

        // Stop and send recording if the last human leaves the channel the bot is in
        if (leftChannel != null) {
            if (audioManager.isConnected() && leftChannel.equals(audioManager.getConnectedChannel())) {
                long humanCount = leftChannel.getMembers().stream()
                        .filter(m -> !m.getUser().isBot())
                        .count();
                
                if (humanCount == 0) {
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
            
            if (lastChannel == null) return;

            new Thread(() -> {
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
                File wavFile = new File("rec_" + guild.getId() + "_" + timestamp + ".wav");
                try {
                    recorder.saveAsWav(wavFile);
                    
                    TextChannel logChannel = guild.getJDA().getTextChannelById(LOG_CHANNEL_ID);
                    if (logChannel != null) {
                        String timeStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                        logChannel.sendMessage("\uD83C\uDF99\uFE0F **تـــــم انـــــتـــــهـــــاء الـــــتـــــســـــجـــــيـــــل**\n" +
                                "\u25AB\uFE0F **الـــــروم:** `" + lastChannel.getName() + "`\n" +
                                "\u25AB\uFE0F **الـــــوقـــــت:** `" + timeStr + "`\n" +
                                "\u25AB\uFE0F **الـــــجـــــودة:** `48kHz / 16-bit Stereo`")
                                .addFiles(FileUpload.fromData(wavFile))
                                .queue(m -> wavFile.delete(), t -> wavFile.delete());
                    } else {
                        wavFile.delete();
                    }
                } catch (IOException e) {
                    wavFile.delete();
                } finally {
                    recorder.cleanup();
                }
            }).start();
        }
    }
}
