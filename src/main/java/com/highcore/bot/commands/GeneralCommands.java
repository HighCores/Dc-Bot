package com.highcore.bot.commands;

import com.highcore.bot.utils.EmbedUtil;
import com.highcore.bot.services.PanelService;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.jetbrains.annotations.NotNull;
import java.util.Random;

public class GeneralCommands extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        String name = event.getName();

        switch (name) {
            case "ping" -> handlePing(event);
            case "roll" -> handleRoll(event);
            case "translate" -> handleTranslate(event);
            case "title" -> handleTitle(event);
            case "line" -> handleLine(event);
            case "order" -> PanelService.handleOrderFlow(event);
            case "terms" -> {
                String[] imgs = {
                    "https://i.imgur.com/KTPxBfL.png",
                    "https://i.imgur.com/1454z6W.png",
                    "https://i.imgur.com/SGcSGsl.png",
                    "https://i.imgur.com/2lSKtYH.png",
                    "https://i.imgur.com/jL2SV1F.png",
                    "https://i.imgur.com/Z8Whznm.png",
                    "https://i.imgur.com/pVu4NGX.png",
                    "https://i.imgur.com/KTPxBfL.png"
                };
                
                // 2. Banners
                java.util.List<net.dv8tion.jda.api.components.container.ContainerChildComponent> layout = new java.util.ArrayList<>();
                for (int i = 0; i < imgs.length; i++) {
                    layout.add(net.dv8tion.jda.api.components.mediagallery.MediaGallery.of(net.dv8tion.jda.api.components.mediagallery.MediaGalleryItem.fromUrl(imgs[i])));
                    if (i < imgs.length - 1) {
                        layout.add(net.dv8tion.jda.api.components.separator.Separator.createDivider(net.dv8tion.jda.api.components.separator.Separator.Spacing.SMALL));
                    }
                }
                PanelService.reply(event, net.dv8tion.jda.api.components.container.Container.of(layout));
            }
            case "prices" -> {
                String[] imgs = {
                    "https://i.imgur.com/2qAETE2.png",
                    "https://i.imgur.com/2qAETE2.png",
                    "https://i.imgur.com/2qAETE2.png",
                    "https://i.imgur.com/2qAETE2.png",
                    "https://i.imgur.com/2qAETE2.png"
                };
                
                java.util.List<net.dv8tion.jda.api.components.container.ContainerChildComponent> layout = new java.util.ArrayList<>();
                for (int i = 0; i < imgs.length; i++) {
                    layout.add(net.dv8tion.jda.api.components.mediagallery.MediaGallery.of(net.dv8tion.jda.api.components.mediagallery.MediaGalleryItem.fromUrl(imgs[i])));
                    if (i < imgs.length - 1) {
                        layout.add(net.dv8tion.jda.api.components.separator.Separator.createDivider(net.dv8tion.jda.api.components.separator.Separator.Spacing.SMALL));
                    }
                }
                PanelService.reply(event, net.dv8tion.jda.api.components.container.Container.of(layout));
            }
        }
    }

    private void handlePing(SlashCommandInteractionEvent event) {
        long ping = event.getJDA().getGatewayPing();
        String body = String.format("### 📡 Latency Status\n**%d**ms", ping);
        PanelService.reply(event, EmbedUtil.containerBranded("SYSTEM", "Bot Status", body, EmbedUtil.BANNER_MAIN));
    }

    private void handleRoll(SlashCommandInteractionEvent event) {
        int res = new Random().nextInt(6) + 1;
        String body = String.format("""
                ### 🎲 Dice Roll Result
                **Number:** **%d**
                
                *Generated successfully*
                """, res);
        PanelService.reply(event,
                EmbedUtil.containerBranded("GAME", "Roll result", body, EmbedUtil.BANNER_MAIN));
    }

    private void handleTranslate(SlashCommandInteractionEvent event) {
        String text = event.getOption("text", OptionMapping::getAsString);
        String lang = event.getOption("language", OptionMapping::getAsString);
        String result = com.highcore.bot.services.AIService.translate(text, lang);
        PanelService.reply(event, EmbedUtil.containerBranded("Language", "Translation Engine",
                "### \uD83C\uDF10 Result\nTarget Language: **" + lang + "**\n\n" + result, EmbedUtil.BANNER_MAIN));
    }

    private void handleTitle(SlashCommandInteractionEvent event) {
        String title = event.getOption("title", OptionMapping::getAsString);
        boolean isReset = title == null || title.equalsIgnoreCase("none") || title.equalsIgnoreCase("reset")
                || title.trim().isEmpty();

        if (isReset) {
            com.highcore.bot.database.SupabaseClient.setTitle(event.getUser().getId(), event.getGuild().getId(), null);
        } else {
            com.highcore.bot.database.SupabaseClient.setTitle(event.getUser().getId(), event.getGuild().getId(), title);
        }

        String currentName = event.getMember().getEffectiveName();
        String baseName = currentName.replaceAll("^\\[.*?\\]\\s*", "");

        final String finalNick;
        if (isReset) {
            finalNick = baseName;
        } else {
            String candidate = "[" + title + "] " + baseName;
            if (candidate.length() > 32) {
                candidate = candidate.substring(0, 32);
            }
            finalNick = candidate;
        }

        event.getMember().modifyNickname(finalNick).queue(
                success -> PanelService.reply(event,
                        EmbedUtil.success("Identity Update",
                                isReset ? "Identity registry cleared. Your name has been reset."
                                        : "Identity synchronized. Your title is now set to: **" + title + "**")),
                error -> PanelService.reply(event, EmbedUtil.success("Identity Update",
                        "Registry update finalized. *Note: Nickname synchronization failed due to hierarchy restrictions.*")));
    }

    private void handleLine(SlashCommandInteractionEvent event) {
        PanelService.reply(event, net.dv8tion.jda.api.components.container.Container.of(
            net.dv8tion.jda.api.components.mediagallery.MediaGallery.of(
                net.dv8tion.jda.api.components.mediagallery.MediaGalleryItem.fromUrl("https://i.imgur.com/KTPxBfL.png")
            )
        ));
    }
}
