package com.highcore.bot.listeners;

import com.google.gson.JsonObject;
import com.highcore.bot.database.SupabaseClient;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class WelcomeListener extends ListenerAdapter {
    private static final Logger log = LoggerFactory.getLogger(WelcomeListener.class);

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        
        // 1. Fetch live config from Supabase (Dashboard synced)
        JsonObject config = SupabaseClient.getWelcomeConfig();
        
        // Default High Core logic fallback if dashboard is completely empty
        if (config == null || !config.has("is_active")) {
            sendHighCoreLegacyWelcome(event);
            return;
        }

        // If explicitly disabled in dashboard
        if (!config.get("is_active").getAsBoolean()) return;

        // 2. Fetch Channel
        String channelId = config.has("channel_id") && !config.get("channel_id").isJsonNull() ? config.get("channel_id").getAsString() : null;
        if (channelId == null || channelId.isEmpty()) return;

        TextChannel channel = event.getGuild().getTextChannelById(channelId);
        if (channel == null) return;

        // 3. Assemble Message Payload
        String messageType = config.has("message_type") && !config.get("message_type").isJsonNull() ? config.get("message_type").getAsString() : "embed";
        String messageBody = config.has("message") && !config.get("message").isJsonNull() ? config.get("message").getAsString() : "Welcome {user} to the server!";
        
        // Dynamic Variable Injection (Vetox parity)
        messageBody = messageBody
                .replace("{user}", event.getMember().getAsMention())
                .replace("{user.tag}", event.getUser().getAsTag())
                .replace("{server}", event.getGuild().getName())
                .replace("{member_count}", String.valueOf(event.getGuild().getMemberCount()))
                .replace("\\n", "\n");

        if (messageType.equalsIgnoreCase("text")) {
            channel.sendMessage(messageBody).queue();
            return;
        }

        // Embed Builder (Real-time syncing from Dashboard settings)
        String embedTitle = config.has("embed_title") && !config.get("embed_title").isJsonNull() ? config.get("embed_title").getAsString() : "";
        String colorHex = config.has("color") && !config.get("color").isJsonNull() ? config.get("color").getAsString() : "#5865F2";
        String thumbnailUrl = config.has("thumbnail_url") && !config.get("thumbnail_url").isJsonNull() ? config.get("thumbnail_url").getAsString() : "{user_avatar}";
        String bannerUrl = config.has("image_url") && !config.get("image_url").isJsonNull() ? config.get("image_url").getAsString() : null;

        if (thumbnailUrl.equals("{user_avatar}")) {
            thumbnailUrl = event.getUser().getEffectiveAvatarUrl();
        } else if (thumbnailUrl.equals("{none}") || thumbnailUrl.isEmpty()) {
            thumbnailUrl = null;
        }

        int parsedColor = 0x5865F2;
        try { parsedColor = Integer.parseInt(colorHex.replace("#", ""), 16); } catch (Exception ignored) {}

        List<MessageEmbed> embeds = new ArrayList<>();
        
        EmbedBuilder eb = new EmbedBuilder()
                .setColor(parsedColor)
                .setDescription(messageBody);
        
        if (!embedTitle.isEmpty()) {
            eb.setTitle(embedTitle);
        }
                
        if (thumbnailUrl != null) eb.setThumbnail(thumbnailUrl);
        if (bannerUrl != null && !bannerUrl.isEmpty()) eb.setImage(bannerUrl);

        embeds.add(eb.build());
        channel.sendMessage(event.getMember().getAsMention()).setEmbeds(embeds).queue();
    }
    
    // Legacy fallback to not break current specific High Core workflow if database isn't fully migrated yet
    private void sendHighCoreLegacyWelcome(GuildMemberJoinEvent event) {
        String welcomeChannelId = com.highcore.bot.config.Config.WELCOME_CHANNEL_ID;
        if (welcomeChannelId == null || welcomeChannelId.isEmpty()) return;
        TextChannel ch = event.getGuild().getTextChannelById(welcomeChannelId);
        if (ch == null) return;

        String s = com.highcore.bot.config.Config.CH_STARTUP;
        var eb = com.highcore.bot.utils.EmbedUtil.withBanner().setColor(com.highcore.bot.utils.EmbedUtil.ACCENT_TEAL)
                .setThumbnail(event.getMember().getEffectiveAvatarUrl())
                .setDescription(String.format("""
                        ## 🎉 Welcome to Highcore Agency!

                        > Hey **%s**! Welcome to the server 💜

                        You are member **#%d** 🌟

                        > ▸ Check out <#%s> to learn everything about us
                        > ▸ Check your DMs — we sent you a startup guide 📖
                        > ▸ Respect the rules and enjoy your stay!
                        """, event.getMember().getEffectiveName(), event.getGuild().getMemberCount(),
                        s != null ? s : "start-up"));

        ch.sendMessage(event.getMember().getAsMention()).setEmbeds(eb.build()).queue();
    }
}
