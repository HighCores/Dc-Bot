package com.highcore.bot.listeners;

import com.highcore.bot.services.AutoReplyService;
import com.highcore.bot.services.WordFilterService;
import com.highcore.bot.services.LogManager;
import com.highcore.bot.database.SupabaseClient;
import com.highcore.bot.utils.EmbedUtil;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public class SafetyListener extends ListenerAdapter {

    private static final String BANNED_LOG_CHANNEL = "1490834724518760448";

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (!event.isFromGuild() || event.getAuthor().isBot() || 
            (event.getMember() != null && event.getMember().getRoles().stream().anyMatch(r -> r.getId().equals("1488795130034000036")))) return;

        String content = event.getMessage().getContentRaw();

        String triggeredWord = WordFilterService.findForbiddenWord(content);
        if (triggeredWord != null) {
            handleViolation(event, triggeredWord);
            return;
        }

        String response = AutoReplyService.getResponse(content);
        if (response != null) {
            event.getChannel().sendMessage(response).queue();
        }
    }

    private void handleViolation(MessageReceivedEvent event, String word) {
        event.getMessage().delete().queue(v -> {
            String logMsg = "### 🚨 FIREWALL BREACH DETECTED\n" +
                    "\u25AB\uFE0F **Subject:** " + event.getAuthor().getAsMention() + "\n" +
                    "\u25AB\uFE0F **Channel:** " + event.getChannel().getAsMention() + "\n" +
                    "\u25AB\uFE0F **Detected Term:** `" + word + "`\n" +
                    "\u25AB\uFE0F **Action:** `Message Purged & Violation Recorded`";
            
            var logEmbed = EmbedUtil.createOldLogEmbed("security-breach", logMsg, event.getMember(), event.getAuthor(), null, EmbedUtil.DANGER);
            LogManager.logEmbed(event.getGuild(), BANNED_LOG_CHANNEL, logEmbed);

            SupabaseClient.addWarning(event.getAuthor().getId(), event.getAuthor().getName(), "SYSTEM", "FIREWALL", "Filter Violation: " + word, event.getGuild().getId());

            event.getChannel().sendMessage("⚠️ " + event.getAuthor().getAsMention() + ", your message contained restricted terminology and has been purged.").queue(m -> {
                m.delete().queueAfter(5, java.util.concurrent.TimeUnit.SECONDS);
            });
        });
    }
}
