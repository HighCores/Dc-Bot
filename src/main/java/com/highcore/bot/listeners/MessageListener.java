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
        
        // Comprehensive Embed Extraction (Legacy Embeds)
        if (!event.getMessage().getEmbeds().isEmpty()) {
            for (net.dv8tion.jda.api.entities.MessageEmbed embed : event.getMessage().getEmbeds()) {
                if (embed.getAuthor() != null && embed.getAuthor().getName() != null) contentBuilder.append("**").append(embed.getAuthor().getName()).append("**\n");
                if (embed.getTitle() != null) contentBuilder.append("**").append(embed.getTitle()).append("**\n");
                if (embed.getDescription() != null) contentBuilder.append(embed.getDescription()).append("\n");
                
                for (net.dv8tion.jda.api.entities.MessageEmbed.Field field : embed.getFields()) {
                    contentBuilder.append("\n**").append(field.getName()).append("**\n").append(field.getValue());
                }
                
                if (embed.getImage() != null && embed.getImage().getUrl() != null) {
                    contentBuilder.append("\n[ATTACHMENT: ").append(embed.getImage().getUrl()).append("]");
                }
                if (embed.getFooter() != null && embed.getFooter().getText() != null) {
                    contentBuilder.append("\n\n_").append(embed.getFooter().getText()).append("_");
                }
            }
        }

        // Deep UI V2 Extraction (Modern Highcore Panels: Containers, TextDisplays, MediaGalleries)
        for (net.dv8tion.jda.api.components.LayoutComponent layout : event.getMessage().getComponents()) {
            if (layout instanceof net.dv8tion.jda.api.components.container.Container container) {
                for (net.dv8tion.jda.api.components.container.ContainerChildComponent child : container.getComponents()) {
                    if (child instanceof net.dv8tion.jda.api.components.textdisplay.TextDisplay textDisplay) {
                        contentBuilder.append("\n").append(textDisplay.getContent());
                    } else if (child instanceof net.dv8tion.jda.api.components.mediagallery.MediaGallery gallery) {
                        for (net.dv8tion.jda.api.components.mediagallery.MediaGalleryItem item : gallery.getItems()) {
                            if (item.getUrl() != null) {
                                contentBuilder.append("\n[ATTACHMENT: ").append(item.getUrl()).append("]");
                            }
                        }
                    }
                }
            }
        }

        for (net.dv8tion.jda.api.entities.Message.Attachment att : event.getMessage().getAttachments()) {
            contentBuilder.append("\n[ATTACHMENT: ").append(att.getUrl()).append("]");
        }
    
        String role = event.getAuthor().isBot() ? "BOT" : "USER";
        org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(MessageListener.class);
        logger.info("[DEBUG] ATTEMPTING TO SAVE MESSAGE - Ticket: {}, User: {}, Content: {}", ticketId, event.getAuthor().getName(), contentBuilder.toString());
        
        try {
            SupabaseClient.saveTicketMessage(ticketId, event.getAuthor().getId(), event.getAuthor().getName(), contentBuilder.toString(), role, event.getMessageId());
            logger.info("[DEBUG] SAVE CALL EXECUTED FOR {}", ticketId);
        } catch (Exception e) {
            logger.error("[DEBUG] CRITICAL ERROR SAVING MESSAGE: {}", e.getMessage(), e);
        }
    }
}
