package com.highcore.bot.commands;

import com.google.gson.JsonObject;
import com.highcore.bot.database.SupabaseClient;
import com.highcore.bot.services.PanelService;
import com.highcore.bot.utils.EmbedUtil;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.jetbrains.annotations.NotNull;

public class OrderCommands extends ListenerAdapter {

    public static SlashCommandData getCommandData() {
        return Commands.slash("order-status", "الاستعلام عن حالة مشروعك المسجل في الوكالة")
                .addOption(OptionType.STRING, "number", "رقم المشروع الخاص بك (مثال: HC-007)", true)
                .setDefaultPermissions(DefaultMemberPermissions.ENABLED);
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!event.getName().equals("order-status")) return;

        String num = event.getOption("number").getAsString().toUpperCase();
        if (!num.startsWith("HC-")) num = "HC-" + num;

        JsonObject order = SupabaseClient.getOrder(num);
        if (order == null) {
            PanelService.replyEphemeral(event, EmbedUtil.error("خطأ في البيانات", "### \u274C لم يتم العثور على الطلب\n" + 
                    "رقم الطلب `" + num + "` غير موجود في سجلات وكالة هايكور."));
            return;
        }

        String statusRaw = order.get("status").getAsString();
        String cat = order.has("category") && !order.get("category").isJsonNull() ? order.get("category").getAsString() : "عام";
        String name = order.has("specs") && order.get("specs").isJsonObject() && order.getAsJsonObject("specs").has("name") 
                        ? order.getAsJsonObject("specs").get("name").getAsString() : "مشروع خاص";

        String statusAr = switch (statusRaw) {
            case "COMPLETED" -> "مكتمل بنجاح";
            case "IN_PROGRESS" -> "قيد التنفيذ حالياً";
            case "CANCELLED" -> "ملغى";
            case "PENDING" -> "في انتظار المراجعة";
            default -> statusRaw;
        };

        String emoji = switch (statusRaw) {
            case "COMPLETED" -> "\u2705";
            case "IN_PROGRESS" -> "\u2699\uFE0F";
            case "CANCELLED" -> "\u274C";
            default -> "\u23F3";
        };

        String body = "## " + emoji + " الحالة: " + statusAr + "\n\n"
                + "**رقم المشروع:** `" + num + "`\n"
                + "**اسم الخدمة:** " + name + "\n"
                + "**القسم:** " + cat.toUpperCase() + "\n"
                + "**ملاحظات العمل:** " + (order.has("status_notes") && !order.get("status_notes").isJsonNull() ? order.get("status_notes").getAsString() : "لا توجد ملاحظات عامة متاحة حالياً.");

        PanelService.reply(event, EmbedUtil.containerBranded("السجلات", "بيان المشروع", body, EmbedUtil.BANNER_MAIN));
    }
}
