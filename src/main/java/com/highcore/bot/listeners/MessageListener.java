package com.highcore.bot.listeners;

import com.google.gson.JsonObject;
import com.highcore.bot.database.SupabaseClient;
import com.highcore.bot.services.AIService;
import com.highcore.bot.services.AutoReplyService;
import com.highcore.bot.services.LevelService;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class MessageListener extends ListenerAdapter {
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;
        if (!event.isFromGuild()) return;
        String content = event.getMessage().getContentRaw();
        String channelId = event.getChannel().getId();
        String userId = event.getAuthor().getId();

        if (content.equalsIgnoreCase("!sync")) {
            if (event.getMember() != null && (event.getMember().hasPermission(net.dv8tion.jda.api.Permission.ADMINISTRATOR) || 
                com.highcore.bot.config.Config.getAdminRoles().stream().anyMatch(r -> event.getMember().getRoles().stream().anyMatch(mr -> mr.getId().equals(r))))) {
                
                event.getMessage().reply("\u2699\uFE0F **Syncing Neural Commands...**").queue(msg -> {
                    int count = com.highcore.bot.Main.registerCommands(event.getJDA());
                    msg.editMessage("\u2705 **Sync Complete!** Successfully added **" + count + "** new commands to the Discord API.").queue();
                });
            } else {
                event.getMessage().reply("\u274C **Unauthorized:** High Command authority required.").queue();
            }
            return;
        }

        // Dynamic Commands Sync (Dashboard -> Bot)
        if (content.startsWith(com.highcore.bot.config.Config.BOT_PREFIX)) {
            String cmd = content.substring(com.highcore.bot.config.Config.BOT_PREFIX.length()).trim().toLowerCase();
            if (com.highcore.bot.services.CommandService.execute(event.getMember(), event.getChannel(), cmd)) {
                return;
            }
        }

        saveTicketMessage(event);

        // Process XP for leveling (async, non-blocking)
        if (event.getMember() != null) {
            try { LevelService.processMessage(event.getGuild(), event.getMember()); } catch (Exception e) { /* don't break messaging */ }
        }

        if (content.equalsIgnoreCase("!stop")) {
            if (AIService.isAIEnabled(channelId)) {
                AIService.disableAI(channelId);
                AIService.clearSession(userId);
                event.getMessage().reply("\u2705 AI assistant disabled.").queue();
            }
            return;
        }


        if (content.equalsIgnoreCase("!ai")) {
            AIService.enableAI(channelId);
            event.getMessage().reply("\uD83D\uDCAC AI assistant enabled! Type your question.\nType `!stop` to disable.").queue();
            return;
        }
        if (AIService.isAIEnabled(channelId)) {
            event.getChannel().sendTyping().queue();
            String reply = AIService.chat(userId, content);
            event.getMessage().reply(reply).queue();
            return;
        }
        String autoReply = AutoReplyService.getResponse(content);
        if (autoReply != null) event.getMessage().reply(autoReply).queue();
    }

    private void saveTicketMessage(MessageReceivedEvent event) {
        if (!(event.getChannel() instanceof TextChannel channel)) return;
        if (!channel.getName().startsWith("ticket-")) return;
        JsonObject ticket = SupabaseClient.getTicketByChannel(channel.getId());
        if (ticket == null) return;
        SupabaseClient.saveTicketMessage(ticket.get("ticket_id").getAsString(),
                event.getAuthor().getId(), event.getAuthor().getName(),
                event.getMessage().getContentRaw(), event.getMessageId());
    }
}
