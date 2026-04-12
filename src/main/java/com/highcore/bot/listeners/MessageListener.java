package com.highcore.bot.listeners;

import com.google.gson.JsonObject;
import com.highcore.bot.database.SupabaseClient;
import com.highcore.bot.services.AIService;
import com.highcore.bot.services.AutoReplyService;
import com.highcore.bot.services.PanelService;
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
        if (!event.isFromGuild()) return;
        
        saveTicketMessage(event);
        
        if (event.getAuthor().isBot()) return;
        
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
                    String logBody = "### ⚠️ Security Alert: Restricted Content\n" +
                            "**User:** **" + event.getAuthor().getName() + "** (`" + event.getAuthor().getId() + "`)\n" +
                            "**Channel:** #" + event.getChannel().getName() + "\n" +
                            "**Detected Text:**\n> " + content;
                    
                    PanelService.reply(logChannel, EmbedUtil.activityLog("SECURITY LOG", logBody, EmbedUtil.DANGER));
                    PanelService.reply(event.getChannel(), EmbedUtil.error("PROTECTION VIOLATION", "Restricted content detected. Security sequence initiated."));
                }
                return;
            }
        }

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
        String name = channel.getName().toLowerCase();
        // Support all prefixes: ticket, order, case, complaint, support, or any channel ending in decimals
        boolean isTicket = name.contains("ticket") || name.contains("order") || name.contains("case") || 
                           name.contains("complaint") || name.contains("support") || name.matches(".*\\d{3,}");
        
        if (!isTicket) return;
        
        JsonObject ticket = SupabaseClient.getTicketByChannel(channel.getId());
        String ticketId = null;
        
        if (ticket != null) {
            ticketId = ticket.get("ticket_id").getAsString();
        } else {
            // Fallback: extract from name (support-0078 -> 0078)
            String[] parts = name.split("-");
            if (parts.length >= 2) ticketId = parts[1];
        }
        
        if (ticketId == null) return;
        
        StringBuilder contentBuilder = new StringBuilder(event.getMessage().getContentRaw());
        for (net.dv8tion.jda.api.entities.Message.Attachment att : event.getMessage().getAttachments()) {
            contentBuilder.append("\n[ATTACHMENT: ").append(att.getUrl()).append("]");
        }
        
        String role = event.getAuthor().isBot() ? "BOT" : "USER";
        try {
            SupabaseClient.saveTicketMessage(ticketId, event.getAuthor().getId(), event.getAuthor().getName(), contentBuilder.toString(), role, event.getMessageId());
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(MessageListener.class).error("Failed to save message: {}", e.getMessage());
        }
    }
}
