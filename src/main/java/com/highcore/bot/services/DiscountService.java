package com.highcore.bot.services;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.highcore.bot.database.SupabaseClient;
import com.highcore.bot.utils.CalendarUtil;
import com.highcore.bot.utils.EmbedUtil;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

public class DiscountService {

    public static void sendDiscountPanel(SlashCommandInteractionEvent event) {
        LocalDate now = LocalDate.now();
        sendDiscountPanel(event, now.getYear(), now.getMonthValue());
    }

    public static void sendDiscountPanel(SlashCommandInteractionEvent event, int year, int month) {
        JsonArray allDiscounts = SupabaseClient.getAllDiscounts();
        Set<Integer> days = calculateActiveDays(allDiscounts, year, month);
        
        String calendar = CalendarUtil.generateSymbolicCalendar(year, month, days);
        String desc = "### Discount Management\n" +
                "View and manage active discount events.\n\n" +
                calendar + "\n" +
                "*Symbols: `>>` Today, `[]` Scheduled, `!!` Both.*";

        ActionRow navRow = ActionRow.of(
            Button.secondary("disc_prev_" + year + "_" + month, "Previous"),
            Button.secondary("disc_next_" + year + "_" + month, "Next")
        );
        ActionRow actionRow = ActionRow.of(
            Button.secondary("disc_deploy_auto", "New Auto Discount"),
            Button.secondary("disc_deploy_manual", "New Manual Discount"),
            Button.primary("disc_view_all", "View Full Schedule")
        );

        PanelService.reply(event, EmbedUtil.containerBrandedRows("DISCOUNT MANAGER", "Discount Schedule", desc, null, navRow, actionRow));
    }

    public static void updateDiscountPanel(net.dv8tion.jda.api.interactions.callbacks.IReplyCallback event, int year, int month) {
        JsonArray allDiscounts = SupabaseClient.getAllDiscounts();
        Set<Integer> days = calculateActiveDays(allDiscounts, year, month);

        String calendar = CalendarUtil.generateSymbolicCalendar(year, month, days);
        String desc = "### Discount Management\n" +
                "View and manage active discount events.\n\n" +
                calendar + "\n" +
                "*Symbols: `>>` Today, `[]` Scheduled, `!!` Both.*";

        ActionRow navRow = ActionRow.of(
            Button.secondary("disc_prev_" + year + "_" + month, "Previous"),
            Button.secondary("disc_next_" + year + "_" + month, "Next")
        );
        ActionRow actionRow = ActionRow.of(
            Button.secondary("disc_deploy_auto", "New Auto Discount"),
            Button.secondary("disc_deploy_manual", "New Manual Discount"),
            Button.primary("disc_view_all", "View Full Schedule")
        );

        PanelService.reply(event, EmbedUtil.containerBrandedRows("DISCOUNT MANAGER", "Discount Schedule", desc, null, navRow, actionRow));
    }

    private static Set<Integer> calculateActiveDays(JsonArray allDiscounts, int year, int month) {
        Set<Integer> days = new HashSet<>();
        if (allDiscounts == null) return days;
        
        LocalDate viewMonthStart = LocalDate.of(year, month, 1);
        LocalDate viewMonthEnd = viewMonthStart.plusMonths(1).minusDays(1);

        for (var dev : allDiscounts) {
            JsonObject d = dev.getAsJsonObject();
            String dateStr = d.get("schedule_date").getAsString();
            String repeat = d.has("repeat_interval") ? d.get("repeat_interval").getAsString().toUpperCase() : "NONE";
            
            LocalDate startDate = LocalDate.parse(dateStr);
            LocalDate endDate = d.has("end_date") && !d.get("end_date").isJsonNull() ? 
                LocalDate.parse(d.get("end_date").getAsString()) : startDate;

            if (repeat.equals("NONE")) {
                LocalDate current = startDate;
                while (!current.isAfter(endDate)) {
                    if (current.getYear() == year && current.getMonthValue() == month) {
                        days.add(current.getDayOfMonth());
                    }
                    current = current.plusDays(1);
                }
            } else if (repeat.equals("MONTHLY")) {
                int startD = startDate.getDayOfMonth();
                int endD = endDate.getDayOfMonth();
                if (endD < startD) { // Wrap
                    for (int i = 1; i <= viewMonthEnd.getDayOfMonth(); i++) {
                        if (i >= startD || i <= endD) days.add(i);
                    }
                } else {
                    for (int i = startD; i <= endD && i <= viewMonthEnd.getDayOfMonth(); i++) {
                        days.add(i);
                    }
                }
            } else if (repeat.equals("YEARLY")) {
                if (startDate.getMonthValue() == month || endDate.getMonthValue() == month) {
                    java.time.MonthDay startMD = java.time.MonthDay.from(startDate);
                    java.time.MonthDay endMD = java.time.MonthDay.from(endDate);
                    for (int i = 1; i <= viewMonthEnd.getDayOfMonth(); i++) {
                        try {
                            java.time.MonthDay currentMD = java.time.MonthDay.of(month, i);
                            if (endMD.isBefore(startMD)) {
                                if (!currentMD.isBefore(startMD) || !currentMD.isAfter(endMD)) days.add(i);
                            } else {
                                if (!currentMD.isBefore(startMD) && !currentMD.isAfter(endMD)) days.add(i);
                            }
                        } catch (Exception ignored) {}
                    }
                }
            } else if (repeat.equals("WEEKLY")) {
                long duration = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate);
                LocalDate currentBase = startDate;
                while (!currentBase.isAfter(viewMonthEnd)) {
                    for (int i = 0; i <= duration; i++) {
                        LocalDate activeDay = currentBase.plusDays(i);
                        if (activeDay.getYear() == year && activeDay.getMonthValue() == month) {
                            days.add(activeDay.getDayOfMonth());
                        }
                    }
                    currentBase = currentBase.plusWeeks(1);
                }
            }
        }
        return days;
    }
}
