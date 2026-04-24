package com.highcore.bot.listeners;

import com.highcore.bot.services.AIService;
import com.highcore.bot.services.TranslationService;
import com.highcore.bot.utils.EmbedUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.mediagallery.MediaGallery;
import net.dv8tion.jda.api.components.mediagallery.MediaGalleryItem;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TranslationListener extends ListenerAdapter {

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        String id = event.getComponentId();
        if (id.startsWith("translate_init_")) {
            String type = id.replace("translate_init_", "");
            
            StringSelectMenu menu = StringSelectMenu.create("translate_lang_" + type)
                    .setPlaceholder("Select Language / اختر اللغة")
                    .addOption("English", "en", "Translate to English", Emoji.fromUnicode("🇺🇸"))
                    .addOption("Turkish", "tr", "Translate to Turkish", Emoji.fromUnicode("🇹🇷"))
                    .addOption("Russian", "ru", "Translate to Russian", Emoji.fromUnicode("🇷🇺"))
                    .addOption("French", "fr", "Translate to French", Emoji.fromUnicode("🇫🇷"))
                    .build();

            event.reply("Choose your language / اختر لغتك:")
                    .setEphemeral(true)
                    .setComponents(ActionRow.of(menu))
                    .queue();
        }
    }

    @Override
    public void onStringSelectInteraction(@NotNull StringSelectInteractionEvent event) {
        String id = event.getComponentId();
        if (id.startsWith("translate_lang_")) {
            String type = id.replace("translate_lang_", "");
            String lang = event.getValues().get(0);
            
            event.deferReply(true).queue();
            
            // Logic to get the original embed content and image
            // Since we're replying to a select menu, the original message is the one the button was on
            event.getMessageChannel().retrieveMessageById(event.getMessageId()).queue(originalMsg -> {
                // Actually, in Discord, you can't easily read the "original message" content if it's a container in an ephemeral reply chain?
                // But here, the original message is the one with the 🌐 button.
            });
            
            // We'll use a mapping for the 5 main embeds
            handleTranslation(event, type, lang);
        }
    }

    private void handleTranslation(StringSelectInteractionEvent event, String type, String lang) {
        String title = "Translated Content";
        String body = "";
        String imageUrl = null;

        // Simplified mapping for the requested embeds
        switch (type) {
            case "startup" -> {
                title = "Startup Hub";
                body = "Welcome to Highcore Agency. Access all departments via the control modules below.";
                imageUrl = EmbedUtil.BANNER_MAIN;
            }
            case "order" -> {
                title = "Highcore Orders";
                body = "Ready to bring your idea to life? Choose a category below to see our services.";
                imageUrl = EmbedUtil.BANNER_ORDER;
            }
            case "tickets" -> {
                title = "Ticket Support";
                body = "Initialize a project session or report an issue via the modules below. Please follow agency rules.";
                imageUrl = EmbedUtil.BANNER_TICKETS_MENU;
            }
            case "giveaway" -> {
                title = "Active Rewards";
                body = "A new giveaway session is active. Participate to acquire agency merits.";
                imageUrl = EmbedUtil.BANNER_GIVEAWAY;
            }
            case "terms" -> {
                title = "Agency Protocols";
                body = "By engaging with Highcore Agency, you agree to the following terms and conditions.";
                imageUrl = EmbedUtil.BANNER_MAIN;
            }
        }

        // Translate text
        String translatedTitle = TranslationService.translateText(title, lang);
        String translatedBody = TranslationService.translateText(body, lang);
        
        // Translate Image
        byte[] translatedImage = TranslationService.translateImage(imageUrl, lang);

        List<ContainerChildComponent> layout = new ArrayList<>();
        if (translatedImage != null) {
            // Send with translated image
            event.getHook().sendFiles(FileUpload.fromData(translatedImage, "translated.png")).setEphemeral(true).queue(sentMsg -> {
                layout.add(MediaGallery.of(MediaGalleryItem.fromUrl(sentMsg.getAttachments().get(0).getUrl())));
                event.getHook().editOriginalComponents(Collections.emptyList()).queue(); // Clear the select menu
                event.getHook().editOriginal(sentMsg.getAttachments().get(0).getUrl() + "\n\n" + translatedBody).queue();
            });
        } else {
            // Fallback to original image or just text
            EmbedBuilder eb = new EmbedBuilder()
                    .setTitle(translatedTitle)
                    .setDescription(translatedBody)
                    .setImage(imageUrl)
                    .setColor(EmbedUtil.ACCENT);
            event.getHook().sendMessageEmbeds(eb.build()).setEphemeral(true).queue();
        }
    }
}
