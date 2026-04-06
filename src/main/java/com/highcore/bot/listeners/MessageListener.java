package com.highcore.bot.listeners;

import com.google.gson.JsonObject;
import com.highcore.bot.database.SupabaseClient;
import com.highcore.bot.services.AIService;
import com.highcore.bot.services.AutoReplyService;
import com.highcore.bot.services.BroadcastService;
import com.highcore.bot.services.WordFilterService;
import com.highcore.bot.services.SettingSyncService;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.Permission;

import java.util.concurrent.TimeUnit;

public class MessageListener extends ListenerAdapter {
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot())
            return;
        if (!event.isFromGuild())
            return;
        String content = event.getMessage().getContentRaw();
        String channelId = event.getChannel().getId();
        String userId = event.getAuthor().getId();

        // 🛡️ Word Filter (Simplified Security)
        if (WordFilterService.isForbidden(content)) {
            if (event.getMember() != null && !event.getMember().hasPermission(Permission.MANAGE_SERVER)) {
                event.getMessage().delete().queue();
                event.getChannel()
                        .sendMessage(event.getAuthor().getAsMention()
                                + " \u26A0\uFE0F Your transmission contains forbidden terminology. Access denied.")
                        .queue(m -> m.delete().queueAfter(5, TimeUnit.SECONDS));
                return;
            }
        }

        saveTicketMessage(event);

        // Leveling system removed (Phase 1 Cleanup)

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
            event.getMessage().reply("\uD83D\uDCAC AI assistant enabled! Type your question.\nType `!stop` to disable.")
                    .queue();
            return;
        }
        if (AIService.isAIEnabled(channelId)) {
            event.getChannel().sendTyping().queue();
            String reply = AIService.chat(userId, content);
            event.getMessage().reply(reply).queue();
            return;
        }
        String autoReply = AutoReplyService.getResponse(content);
        if (autoReply != null)
            event.getMessage().reply(autoReply).queue();

        // 📡 NEURAL BROADCAST: !bc <@role> <message>
        if (content.toLowerCase().startsWith("!bc ")) {
            if (event.getMember() != null && event.getMember().getRoles().stream().anyMatch(r -> r.getId().equals(BroadcastService.BROADCAST_ROLE_ID))) {
                String cmdArgs = content.substring(4).trim();
                String targetRoleId = null;
                String bcMsg = cmdArgs;

                if (cmdArgs.startsWith("<@&") && cmdArgs.contains(">")) {
                    int endIdx = cmdArgs.indexOf(">");
                    targetRoleId = cmdArgs.substring(3, endIdx);
                    bcMsg = cmdArgs.substring(endIdx + 1).trim();
                }

                if (bcMsg.isEmpty()) { event.getMessage().reply("\u26A0\uFE0F Transmission content cannot be empty.").queue(); return; }
                bcMsg = bcMsg.replace("\\n", "\n");

                boolean started = BroadcastService.startBroadcast(event.getGuild(), bcMsg, targetRoleId, null);
                if (started) {
                    String targetText = (targetRoleId != null) ? "<@&" + targetRoleId + ">" : "All Authorized Nodes";
                    event.getMessage().replyEmbeds(com.highcore.bot.utils.EmbedUtil.success("Broadcast Protocol", "### 📡 Neural Transmission Initiated\n> Target: " + targetText + "\n> Status: Queueing via 7s sync delay.")).queue();
                } else {
                    event.getMessage().reply("\u26A0\uFE0F A broadcast is already in progress. Please wait for the current sequence to finalize.").queue();
                }
            } else {
                event.getMessage().reply("\u274C Unauthorized Protocol Access. Operational permissions required.").queue();
            }
        }
    }

    private void saveTicketMessage(MessageReceivedEvent event) {
        if (!(event.getChannel() instanceof TextChannel channel))
            return;
        if (!channel.getName().startsWith("ticket-"))
            return;
        JsonObject ticket = SupabaseClient.getTicketByChannel(channel.getId());
        if (ticket == null)
            return;
        SupabaseClient.saveTicketMessage(ticket.get("ticket_id").getAsString(),
                event.getAuthor().getId(), event.getAuthor().getName(),
                event.getMessage().getContentRaw(), event.getMessageId());
    }
}
