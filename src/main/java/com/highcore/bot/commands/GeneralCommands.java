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
            case "get-emojis" -> handleGetEmojis(event);
            case "translate" -> handleTranslate(event);
            case "suggest" -> handleSuggest(event);
            case "suggestion" -> handleSuggestionManage(event);
            case "help" -> handleHelp(event);
            case "colors" -> handleColors(event);
            case "color-set" -> handleColorSet(event);
            case "rep" -> handleRep(event);
            case "title" -> handleTitle(event);
        }
    }

    private void handlePing(SlashCommandInteractionEvent event) {
        long ping = event.getJDA().getGatewayPing();
        PanelService.reply(event, EmbedUtil.containerBranded("التشخيص", "سرعة الاتصال", "### \uD83D\uDCE1 تقرير الاستجابة\nحالة الخدمة: `نشط`\nتأخير الإشارة: **" + ping + "ms**", EmbedUtil.BANNER_MAIN));
    }

    private void handleRoll(SlashCommandInteractionEvent event) {
        int res = new Random().nextInt(6) + 1;
        PanelService.reply(event, EmbedUtil.containerBranded("الترفيه", "حجر النرد", "### \uD83C\uDFB2 النتيجة النهائية\nالرقم الظاهر: **" + res + "**", EmbedUtil.BANNER_MAIN));
    }

    private void handleGetEmojis(SlashCommandInteractionEvent event) {
        String input = event.getOption("emoji", OptionMapping::getAsString);
        PanelService.reply(event, EmbedUtil.containerBranded("الموارد", "سحب الرموز", "### \uD83D\uDDBC بيانات الإيموجي\nالمرجع: " + input, EmbedUtil.BANNER_MAIN));
    }

    private void handleTranslate(SlashCommandInteractionEvent event) {
        String text = event.getOption("text", OptionMapping::getAsString);
        String lang = event.getOption("language", OptionMapping::getAsString);
        PanelService.reply(event, EmbedUtil.containerBranded("اللغويات", "محرك الترجمة", "### \uD83C\uDF10 نتيجة الترجمة\nاللغة المستهدفة: **" + lang + "**\nالمحتوى: `" + text + "`", EmbedUtil.BANNER_MAIN));
    }

    private void handleSuggest(SlashCommandInteractionEvent event) {
        String sug = event.getOption("suggestion", OptionMapping::getAsString);
        com.highcore.bot.database.SupabaseClient.createSuggestion(event.getGuild().getId(), event.getUser().getId(), event.getUser().getName(), sug);
        PanelService.reply(event, EmbedUtil.containerBranded("التطوير", "تقديم اقتراح", "### \uD83D\uDCDD فكرة جديدة\nبواسطة: " + event.getUser().getName() + "\n\n" + sug, EmbedUtil.BANNER_MAIN));
    }

    private void handleSuggestionManage(SlashCommandInteractionEvent event) {
        if (!event.getMember().hasPermission(net.dv8tion.jda.api.Permission.ADMINISTRATOR)) return;
        long id = event.getOption("id", OptionMapping::getAsLong);
        String action = event.getOption("action", OptionMapping::getAsString);
        com.highcore.bot.database.SupabaseClient.updateSuggestion(id, action, "تمت المعالجة من الإدارة", event.getUser().getId(), event.getUser().getName(), null);
        PanelService.reply(event, EmbedUtil.success("إدارة الاقتراحات", "تم تحديث حالة الاقتراح رقم `" + id + "` إلى: **" + action + "**"));
    }

    private void handleHelp(SlashCommandInteractionEvent event) {
        PanelService.reply(event, EmbedUtil.containerBranded("المساعدة", "دليل التشغيل", "### \u25C8 بروتوكول النظام\nيمكنك الوصول إلى دليل الأوامر الكامل عبر لوحة التحكم الخاصة بالوكالة أو استخدام قائمة الأوامر التفاعلية.", EmbedUtil.BANNER_MAIN));
    }

    private void handleColors(SlashCommandInteractionEvent event) {
        PanelService.reply(event, EmbedUtil.containerBranded("التميز", "ألوان الهوية", "### \uD83C\uDFA8 قائمة الألوان\nاستخدم أمر `/color-set` مع رقم اللون المختار لتغيير لون اسمك.", EmbedUtil.BANNER_MAIN));
    }

    private void handleColorSet(SlashCommandInteractionEvent event) {
        String code = event.getOption("code", OptionMapping::getAsString);
        PanelService.reply(event, EmbedUtil.success("تحديث اللون", "تم تطبيق اللون الجديد الخاص بك: `" + code + "`"));
    }

    private void handleRep(SlashCommandInteractionEvent event) {
        net.dv8tion.jda.api.entities.User target = event.getOption("user", OptionMapping::getAsUser);
        if (target.getId().equals(event.getUser().getId())) { PanelService.replyEphemeral(event, EmbedUtil.error("خطأ", "لا يمكنك منح سمعة لنفسك!")); return; }
        
        if (com.highcore.bot.database.SupabaseClient.canGiveRep(event.getUser().getId(), event.getGuild().getId())) {
            com.highcore.bot.database.SupabaseClient.addReputation(target.getId(), event.getGuild().getId(), 1);
            com.highcore.bot.database.SupabaseClient.setRepCooldown(event.getUser().getId(), event.getGuild().getId());
            PanelService.reply(event, EmbedUtil.success("نظام السمعة", "تم منح نقطة سمعة لـ " + target.getName() + " بنجاح."));
        } else {
            PanelService.replyEphemeral(event, EmbedUtil.error("فترة انتظار", "يمكنك منح السمعة مرة واحدة كل 24 ساعة."));
        }
    }

    private void handleTitle(SlashCommandInteractionEvent event) {
        String title = event.getOption("title", OptionMapping::getAsString);
        com.highcore.bot.database.SupabaseClient.setTitle(event.getUser().getId(), event.getGuild().getId(), title);
        PanelService.reply(event, EmbedUtil.success("اللقب الشخصي", "تم تحديث لقبك الشخصي إلى: **" + title + "**"));
    }
}
