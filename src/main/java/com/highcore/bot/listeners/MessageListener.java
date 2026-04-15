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
                
                String logId = Config.LOG_BANNED_WORDS;
                TextChannel logChannel = event.getGuild().getTextChannelById(logId);
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
        if (autoReply != null) {
            event.getMessage().reply(autoReply).queue();
            return;
        }

        // 🛠️ Manual Security Overrides (+ commands)
        if (content.startsWith("+")) {
            handleManualCommand(event, content);
        }
    }

    private void handleManualCommand(MessageReceivedEvent event, String input) {
        if (event.getMember() != null && !event.getMember().hasPermission(Permission.MANAGE_SERVER)) return;

        String[] args = input.substring(1).split("\\s+");
        String cmd = args[0].toLowerCase();

        switch (cmd) {
            case "warn-remove" -> {
                if (event.getMessage().getMentions().getUsers().isEmpty()) {
                    PanelService.reply(event.getChannel(), EmbedUtil.error("Input Error", "Subject identification required: `+warn-remove @user [id]`"));
                    return;
                }
                net.dv8tion.jda.api.entities.User target = event.getMessage().getMentions().getUsers().get(0);
                if (args.length > 2) {
                    try {
                        int id = Integer.parseInt(args[2]);
                        SupabaseClient.deleteWarningById(id);
                        PanelService.reply(event.getChannel(), EmbedUtil.success("Infraction Purged", "Specific warning record ID: `" + id + "` has been wiped for " + target.getName()));
                    } catch (NumberFormatException e) {
                        PanelService.reply(event.getChannel(), EmbedUtil.error("Data Type Error", "Invalid record identifier."));
                    }
                } else {
                    SupabaseClient.clearUserWarnings(target.getId(), event.getGuild().getId());
                    PanelService.reply(event.getChannel(), EmbedUtil.success("Archive Sanitized", "Full disciplinary history has been cleaned for " + target.getName()));
                }
                
                // Log the manual command usage
                String details = "### 📡 Manual Override Executed\n" +
                        "\u25AB\uFE0F **Operation:** `+warn-remove`\n" +
                        "\u25AB\uFE0F **Target:** " + target.getAsMention() + "\n" +
                        "\u25AB\uFE0F **Operator:** " + event.getAuthor().getAsMention();
                
                com.highcore.bot.services.LogManager.logEmbed(event.getGuild(), Config.get("LOG_MOD_CMD"), 
                    EmbedUtil.createOldLogEmbed("manual-security-override", details, event.getMember(), target, event.getGuild().getMember(target), EmbedUtil.WARNING));
            }
            case "warnings" -> {
                if (event.getMessage().getMentions().getUsers().isEmpty()) return;
                net.dv8tion.jda.api.entities.User target = event.getMessage().getMentions().getUsers().get(0);
                com.google.gson.JsonArray warns = SupabaseClient.getUserWarnings(target.getId(), event.getGuild().getId());
                int count = warns != null ? warns.size() : 0;

                StringBuilder sb = new StringBuilder();
                sb.append("### \uD83D\uDC64 SUBJECT IDENTIFICATION\n");
                sb.append("**Name:** ").append(target.getAsMention()).append("\n");
                sb.append("**Registry ID:** `").append(target.getId()).append("`\n\n");
                sb.append("### \uD83D\uDCCB WARNING REGISTRY LOGS\n");
                sb.append("**Total Infractions Detected:** `").append(count).append("`\n\n");

                if (count > 0) {
                    sb.append("\u25AB\uFE0F **Documented Violations:**\n");
                    for (int i = 0; i < Math.min(warns.size(), 8); i++) {
                        com.google.gson.JsonObject w = warns.get(i).getAsJsonObject();
                        String reason = w.get("reason").getAsString();
                        String date = w.get("created_at").getAsString().split("T")[0];
                        sb.append("`").append(date).append("` \u2014 **").append(reason).append("**\n");
                    }
                } else {
                    sb.append("*No documented infractions discovered.*");
                }
                PanelService.reply(event.getChannel(), EmbedUtil.containerBranded("HISTORY", "Infraction Database", sb.toString(), EmbedUtil.BANNER_MAIN));
            }
        }
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
        // Deep UI V2 Extraction (Modern Highcore Panels: Containers, TextDisplays, MediaGalleries)
        for (var layout : event.getMessage().getComponents()) {
            if (layout instanceof net.dv8tion.jda.api.components.actionrow.ActionRow row) {
                for (var component : row.getComponents()) {
                    if (component instanceof net.dv8tion.jda.api.components.container.Container container) {
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
