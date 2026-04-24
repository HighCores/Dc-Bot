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
import net.dv8tion.jda.api.components.MessageTopLevelComponent;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.separator.Separator.Spacing;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class TranslationListener extends ListenerAdapter {
    private static final Logger log = LoggerFactory.getLogger(TranslationListener.class);

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
                layout.add(MediaGallery.of(MediaGalleryItem.fromUrl(EmbedUtil.BANNER_STARTUP_HEADER)));
                layout.add(Separator.createDivider(Spacing.SMALL));
                layout.add(MediaGallery.of(MediaGalleryItem.fromUrl(EmbedUtil.BANNER_WELCOME)));

                CompletableFuture<String> b1 = CompletableFuture.supplyAsync(() -> TranslationService.translateText("HighCore", lang));
                CompletableFuture<String> b2 = CompletableFuture.supplyAsync(() -> TranslationService.translateText("About Us", lang));
                CompletableFuture<String> b3 = CompletableFuture.supplyAsync(() -> TranslationService.translateText("Partners", lang));
                CompletableFuture<String> b4 = CompletableFuture.supplyAsync(() -> TranslationService.translateText("Support", lang));

                actionRows.add(ActionRow.of(
                    Button.secondary("btn_highcore", b1.join()).withEmoji(Emoji.fromCustom("Highcore", 1496974488099885177L, false)),
                    Button.secondary("btn_about", b2.join()).withEmoji(Emoji.fromCustom("Aboutus", 1496974985875427358L, false)),
                    Button.secondary("btn_partners", b3.join()).withEmoji(Emoji.fromCustom("Partners", 1496974406369415178L, false)),
                    Button.link("https://discord.com/channels/1488798547947159612", b4.join()).withEmoji(Emoji.fromCustom("Support", 1496974182217547816L, false))
                ));
            }
            case "order" -> {
                CompletableFuture<String> fHeader = CompletableFuture.supplyAsync(() -> TranslationService.translateText("HIGHCORE ORDERS", lang));
                CompletableFuture<String> fBody = CompletableFuture.supplyAsync(() -> TranslationService.translateText("◗ Ready to bring your idea to life ?\n\nChoose a category below to see our services.\nAfter that, you can fill in your project details.", lang));
                CompletableFuture<String> b1 = CompletableFuture.supplyAsync(() -> TranslationService.translateText("Designer", lang));
                CompletableFuture<String> b2 = CompletableFuture.supplyAsync(() -> TranslationService.translateText("Developer", lang));
                CompletableFuture<String> b3 = CompletableFuture.supplyAsync(() -> TranslationService.translateText("Editor", lang));
                CompletableFuture<String> b4 = CompletableFuture.supplyAsync(() -> TranslationService.translateText("Minecraft", lang));

                layout.add(MediaGallery.of(MediaGalleryItem.fromUrl(EmbedUtil.BANNER_ORDER)));
                layout.add(TextDisplay.of("## " + fHeader.join()));
                layout.add(Separator.createDivider(Spacing.SMALL));
                layout.add(TextDisplay.of(fBody.join()));

                actionRows.add(ActionRow.of(
                    Button.secondary("order_cat_designer", b1.join()).withEmoji(Emoji.fromCustom("Design", 1496974725258285157L, false)),
                    Button.secondary("order_cat_developer", b2.join()).withEmoji(Emoji.fromCustom("Developer", 1496974704005611633L, false)),
                    Button.secondary("order_cat_editor", b3.join()).withEmoji(Emoji.fromCustom("Editor", 1496974685030715503L, false)),
                    Button.secondary("order_cat_minecraft", b4.join()).withEmoji(Emoji.fromCustom("Minecraft", 1496974445905051719L, false))
                ));
            }
            case "tickets" -> {
                CompletableFuture<String> fHeader = CompletableFuture.supplyAsync(() -> TranslationService.translateText("TICKET SUPPORT | High Core Agency", lang));
                CompletableFuture<String> fRules = CompletableFuture.supplyAsync(() -> TranslationService.translateText("📜 **RULES & GUIDELINES**\n\n" +
                        "**Mutual Respect** — Please respect all staff members. Any form of offensive behavior or harassment will not be tolerated.\n\n" +
                        "**One Ticket** — Open only one ticket per issue. Do not open multiple tickets for the same problem.\n\n" +
                        "**Clarity** — Please fully describe your issue or request before a staff member responds.\n\n" +
                        "**Content** — Spam and external links are strictly prohibited without staff authorization.\n\n" +
                        "**Mentions** — Pinging or mentioning the staff member inside the ticket is strictly forbidden.", lang));
                CompletableFuture<String> b1 = CompletableFuture.supplyAsync(() -> TranslationService.translateText("Support", lang));
                CompletableFuture<String> b2 = CompletableFuture.supplyAsync(() -> TranslationService.translateText("Complaint", lang));

                layout.add(MediaGallery.of(MediaGalleryItem.fromUrl(EmbedUtil.BANNER_TICKETS_MENU)));
                layout.add(TextDisplay.of("### " + fHeader.join()));
                layout.add(Separator.createDivider(Spacing.SMALL));
                layout.add(TextDisplay.of(fRules.join()));
                layout.add(Separator.createDivider(Spacing.SMALL));

                actionRows.add(ActionRow.of(
                    Button.secondary("ticket_init_support", b1.join()).withEmoji(Emoji.fromCustom("TechnicalSupport", 1496974160621207673L, false)),
                    Button.secondary("ticket_init_complaint", b2.join()).withEmoji(Emoji.fromCustom("FileComplaint", 1496974577576968272L, false))
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

        if (!actionRows.isEmpty()) {
            layout.add(Separator.createDivider(Spacing.SMALL));
            layout.addAll(actionRows);
        }

        try {
            List<MessageTopLevelComponent> components = new ArrayList<>();
            components.add(Container.of(layout));
            
            MessageEditBuilder meb = new MessageEditBuilder()
                    .setContent("")
                    .setComponents(components)
                    .useComponentsV2(true);

            event.getHook().editOriginal(meb.build()).queue();
        } catch (Exception e) {
            log.error("CRITICAL: Failed to render translation UI for type: {} in lang: {}", type, lang, e);
            event.getHook().editOriginal("⚠️ Rendering Error: " + e.getMessage()).queue();
        }
    }
}
