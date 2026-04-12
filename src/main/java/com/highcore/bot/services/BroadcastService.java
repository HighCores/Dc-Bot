package com.highcore.bot.services;

import com.highcore.bot.utils.EmbedUtil;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class BroadcastService {
    private static final Logger log = LoggerFactory.getLogger(BroadcastService.class);
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private static boolean isRunning = false;

    public static String BROADCAST_ROLE_ID = "1488795130034000036";

    public static synchronized boolean startBroadcast(Guild guild, String message, String targetRoleId, String attachmentUrl) {
        if (isRunning) return false;
        isRunning = true;

        log.info("Starting broadcast for guild: {} (Role: {}) (Media: {})", guild.getName(), targetRoleId != null ? targetRoleId : "ALL", attachmentUrl != null ? "YES" : "NO");
        
        guild.loadMembers().onSuccess(members -> {
            List<Member> targetMembers = new ArrayList<>();
            for (Member m : members) {
                if (m.getUser().isBot()) continue;
                if (targetRoleId != null && !targetRoleId.isEmpty()) {
                    if (m.getRoles().stream().noneMatch(r -> r.getId().equals(targetRoleId))) continue;
                }
                targetMembers.add(m);
            }

            log.info("Broadcast queue established: {} users. Beginning delivery...", targetMembers.size());
            executeBroadcastStep(targetMembers, com.highcore.bot.utils.EmojiUtil.parse(message), attachmentUrl, 0);
        }).onError(e -> {
            log.error("Failed to load members for broadcast: {}", e.getMessage());
            isRunning = false;
        });

        return true;
    }

    private static void executeBroadcastStep(List<Member> members, String message, String media, int index) {
        if (index >= members.size()) {
            log.info("Broadcast sequence complete. Total: {}", members.size());
            isRunning = false;
            return;
        }

        Member m = members.get(index);
        String basePersonalized = message.replace("{user}", m.getAsMention()).replace("{name}", m.getEffectiveName());
        if (!basePersonalized.contains(m.getAsMention())) basePersonalized = m.getAsMention() + "\n\n" + basePersonalized;
        final String personalized = basePersonalized;

        m.getUser().openPrivateChannel().queue(pc -> {
            try {
                Container c = EmbedUtil.containerBranded("Broadcast", "Urgent Update", personalized, media != null ? media : EmbedUtil.BANNER_MAIN, null);
                pc.sendMessageComponents(c).useComponentsV2(true).queue(
                    s -> log.debug("Broadcast delivered to {}", m.getUser().getName()),
                    e -> log.warn("Broadcast failed for {}: {}", m.getUser().getName(), e.getMessage())
                );
            } catch (Exception ex) { log.error("Service error for member {}: {}", m.getUser().getName(), ex.getMessage()); }
        }, error -> log.warn("Could not open DM for {}: {}", m.getUser().getName(), error.getMessage()));

        scheduler.schedule(() -> executeBroadcastStep(members, message, media, index + 1), 7, TimeUnit.SECONDS);
    }

    public static boolean isBroadcasting() {
        return isRunning;
    }
}
