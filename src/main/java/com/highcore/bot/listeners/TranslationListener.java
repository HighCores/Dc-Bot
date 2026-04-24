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
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.mediagallery.MediaGallery;
import net.dv8tion.jda.api.components.mediagallery.MediaGalleryItem;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;
import net.dv8tion.jda.api.utils.messages.MessageEditData;
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
            handleTranslation(event, type, lang);
        }
    }

    private void handleTranslation(StringSelectInteractionEvent event, String type, String lang) {
        List<ContainerChildComponent> layout = new ArrayList<>();
        List<ActionRow> actionRows = new ArrayList<>();

        switch (type) {
            case "startup" -> {
                String title = TranslationService.translateText("PROTOCOL", lang);
                String header = TranslationService.translateText("Startup Hub", lang);
                String body = TranslationService.translateText("Welcome to Highcore Agency. Access all departments via the control modules below.", lang);
                
                layout.add(MediaGallery.of(MediaGalleryItem.fromUrl(EmbedUtil.BANNER_MAIN)));
                layout.add(TextDisplay.of("### " + title + "\n## " + header + "\n" + body));
                layout.add(MediaGallery.of(MediaGalleryItem.fromUrl("https://i.imgur.com/KTPxBfL.png"))); // The second banner from the screenshot

                actionRows.add(ActionRow.of(
                    Button.secondary("btn_highcore", TranslationService.translateText("HighCore", lang)).withEmoji(Emoji.fromCustom("Highcore", 1496974136931778641L, false)),
                    Button.secondary("btn_about", TranslationService.translateText("About Us", lang)).withEmoji(Emoji.fromCustom("Aboutus", 1496974985875427358L, false)),
                    Button.secondary("btn_partners", TranslationService.translateText("Partners", lang)).withEmoji(Emoji.fromCustom("Partners", 1496974406369415178L, false)),
                    Button.link("https://discord.com/channels/1488798547947159612", TranslationService.translateText("Support", lang)).withEmoji(Emoji.fromCustom("Support", 1496974182217547816L, false))
                ));
            }
            case "order" -> {
                String header = TranslationService.translateText("Highcore Orders", lang);
                String body = TranslationService.translateText("Ready to bring your idea to life? Choose a category below to see our services.", lang);
                layout.add(MediaGallery.of(MediaGalleryItem.fromUrl(EmbedUtil.BANNER_ORDER)));
                layout.add(TextDisplay.of("## " + header + "\n" + body));
                actionRows.add(ActionRow.of(
                    Button.secondary("order_cat_creative", TranslationService.translateText("Creative", lang)).withEmoji(Emoji.fromCustom("Creative", 1496974426573373461L, false)),
                    Button.secondary("order_cat_developer", TranslationService.translateText("Developer", lang)).withEmoji(Emoji.fromCustom("Developer", 1496974704005611633L, false)),
                    Button.secondary("order_cat_editor", TranslationService.translateText("Editor", lang)).withEmoji(Emoji.fromCustom("Editor", 1496974685030715503L, false)),
                    Button.secondary("order_cat_minecraft", TranslationService.translateText("Minecraft", lang)).withEmoji(Emoji.fromCustom("Minecraft", 1496974445905051719L, false))
                ));
            }
            case "tickets" -> {
                String header = TranslationService.translateText("Ticket Support", lang);
                String body = TranslationService.translateText("Initialize a project session or report an issue via the modules below. Please follow agency rules.", lang);
                layout.add(MediaGallery.of(MediaGalleryItem.fromUrl(EmbedUtil.BANNER_TICKETS_MENU)));
                layout.add(TextDisplay.of("## " + header + "\n" + body));
                actionRows.add(ActionRow.of(
                    Button.secondary("ticket_init_order", TranslationService.translateText("Order", lang)).withEmoji(Emoji.fromCustom("Order", 1496974488561123380L, false)),
                    Button.secondary("ticket_init_support", TranslationService.translateText("Support", lang)).withEmoji(Emoji.fromCustom("TechnicalSupport", 1496974160621207673L, false)),
                    Button.secondary("ticket_init_complaint", TranslationService.translateText("Complaint", lang)).withEmoji(Emoji.fromCustom("FileComplaint", 1496974577576968272L, false))
                ));
            }
            case "giveaway" -> {
                String header = TranslationService.translateText("Active Rewards", lang);
                String body = TranslationService.translateText("A new giveaway session is active. Participate to acquire agency merits.", lang);
                layout.add(MediaGallery.of(MediaGalleryItem.fromUrl(EmbedUtil.BANNER_GIVEAWAY)));
                layout.add(TextDisplay.of("## " + header + "\n" + body));
                actionRows.add(ActionRow.of(
                    Button.primary("gw_enter_translated", TranslationService.translateText("Join Giveaway", lang)).withEmoji(Emoji.fromUnicode("\uD83C\uDF89"))
                ));
            }
            case "terms" -> {
                String header = TranslationService.translateText("Agency Protocols", lang);
                String body = TranslationService.translateText("By engaging with Highcore Agency, you agree to the following terms and conditions.", lang);
                layout.add(MediaGallery.of(MediaGalleryItem.fromUrl(EmbedUtil.BANNER_MAIN)));
                layout.add(TextDisplay.of("## " + header + "\n" + body));
                // Add images from handleTerms logic
                String[] imgs = {"https://i.imgur.com/KTPxBfL.png", "https://i.imgur.com/1454z6W.png"};
                for (String img : imgs) {
                    layout.add(MediaGallery.of(MediaGalleryItem.fromUrl(img)));
                }
            }
        }

        // Final message construction
        event.getHook().editOriginalComponents(Collections.emptyList()).queue();
        
        MessageCreateBuilder mcb = new MessageCreateBuilder()
                .setContent("")
                .addComponents(Container.of(layout))
                .addComponents(actionRows);

        event.getHook().sendMessage(mcb.build()).setEphemeral(true).queue();
    }
}
