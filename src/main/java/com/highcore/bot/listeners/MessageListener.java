package com.highcore.bot.listeners;

import com.google.gson.JsonObject;
import com.highcore.bot.database.SupabaseClient;
import com.highcore.bot.services.AIService;
import com.highcore.bot.services.AutoReplyService;
import com.highcore.bot.services.WordFilterService;
import com.highcore.bot.config.Config;
import com.highcore.bot.utils.EmbedUtil;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.Permission;

public class MessageListener extends ListenerAdapter {
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;
        if (!event.isFromGuild()) return;
        String content = event.getMessage().getContentRaw();
        String channelId = event.getChannel().getId();
        String userId = event.getAuthor().getId();

        // 🛡️ Word Filter
        if (WordFilterService.isForbidden(content)) {
            if (event.getMember() != null && !event.getMember().hasPermission(Permission.MANAGE_SERVER)) {
                event.getMessage().delete().queue();
                
                String logId = Config.get("LOG_WARNING");
                TextChannel logChannel = (logId != null && !logId.isEmpty()) ? event.getGuild().getTextChannelById(logId) : null;
                if (logChannel != null) {
                    String logBody = "### \u26A0\uFE0F Security Alert: Restricted Content\n" +
                            "**User:** **" + event.getAuthor().getName() + "** (`" + event.getAuthor().getId() + "`)\n" +
                            "**Channel:** #" + event.getChannel().getName() + "\n" +
                            "**Detected Text:**\n> " + content;
                    
                    logChannel.sendMessageComponents(EmbedUtil.activityLog("SECURITY LOG", logBody, EmbedUtil.DANGER))
                            .useComponentsV2(true).queue();
                }
                return;
            }
        }

        saveTicketMessage(event);

        if (AIService.isAIEnabled(channelId)) {
            event.getChannel().sendTyping().queue();
            event.getMessage().reply(AIService.chat(userId, content)).queue();
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
        SupabaseClient.saveTicketMessage(ticket.get("ticket_id").getAsString(), event.getAuthor().getId(), event.getAuthor().getName(), event.getMessage().getContentRaw(), event.getMessageId());
    }
}
