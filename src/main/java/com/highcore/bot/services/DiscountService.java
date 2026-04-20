package com.highcore.bot.services;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.highcore.bot.database.SupabaseClient;
import com.highcore.bot.utils.CalendarUtil;
import com.highcore.bot.utils.EmbedUtil;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.container.Container;

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
        
        Set<Integer> days = new HashSet<>();
        if (allDiscounts != null) {
            LocalDate viewMonthStart = LocalDate.of(year, month, 1);
            LocalDate viewMonthEnd = viewMonthStart.plusMonths(1).minusDays(1);

            for (var dev : allDiscounts) {
                JsonObject d = dev.getAsJsonObject();
                String dateStr = d.get("schedule_date").getAsString();
                String type = d.get("type").getAsString();
                String repeat = d.has("repeat_interval") ? d.get("repeat_interval").getAsString().toUpperCase() : "NONE";
                
                LocalDate startDate = LocalDate.parse(dateStr);
                
                if (repeat.equals("NONE") || type.equals("MANUAL")) {
                    if (startDate.getYear() == year && startDate.getMonthValue() == month) {
                        days.add(startDate.getDayOfMonth());
                    }
                } else if (repeat.equals("MONTHLY")) {
                    if (!startDate.isAfter(viewMonthEnd)) {
                        days.add(startDate.getDayOfMonth());
                    }
                } else if (repeat.equals("YEARLY")) {
                    if (!startDate.isAfter(viewMonthEnd) && startDate.getMonthValue() == month) {
                        days.add(startDate.getDayOfMonth());
                    }
                } else if (repeat.equals("WEEKLY")) {
                    LocalDate current = startDate;
                    while (!current.isAfter(viewMonthEnd)) {
                        if (current.getYear() == year && current.getMonthValue() == month) {
                            days.add(current.getDayOfMonth());
                        }
                        current = current.plusWeeks(1);
                    }
                }
            }
        }

        String calendar = CalendarUtil.generateSymbolicCalendar(year, month, days);
        
        String desc = "### \uD83C\uDFAB Discount Infrastructure\n" +
                "Manage scheduled discount events (Auto/Manual).\n\n" +
                calendar + "\n" +
                "*Dates marked with `*` indicate scheduled events.*";

        ActionRow navRow = ActionRow.of(
            Button.secondary("disc_prev_" + year + "_" + month, Emoji.fromFormatted("\u25C0\uFE0F")),
            Button.secondary("disc_next_" + year + "_" + month, Emoji.fromFormatted("\u25B6\uFE0F"))
        );
        
        ActionRow actionRow = ActionRow.of(
            Button.primary("disc_deploy_auto", "New Auto Discount"),
            Button.success("disc_deploy_manual", "New Manual Discount")
        );

        PanelService.reply(event, EmbedUtil.containerBrandedRows("DISCOUNT MANAGER", "Operational Schedule", desc, null, navRow, actionRow));
    }

    public static void updateDiscountPanel(ButtonInteractionEvent event, int year, int month) {
        JsonArray allDiscounts = SupabaseClient.getAllDiscounts();
        
        Set<Integer> days = new HashSet<>();
        if (allDiscounts != null) {
            LocalDate viewMonthStart = LocalDate.of(year, month, 1);
            LocalDate viewMonthEnd = viewMonthStart.plusMonths(1).minusDays(1);

            for (var dev : allDiscounts) {
                JsonObject d = dev.getAsJsonObject();
                String dateStr = d.get("schedule_date").getAsString();
                String type = d.get("type").getAsString();
                String repeat = d.has("repeat_interval") ? d.get("repeat_interval").getAsString().toUpperCase() : "NONE";
                
                LocalDate startDate = LocalDate.parse(dateStr);
                
                if (repeat.equals("NONE") || type.equals("MANUAL")) {
                    if (startDate.getYear() == year && startDate.getMonthValue() == month) {
                        days.add(startDate.getDayOfMonth());
                    }
                } else if (repeat.equals("MONTHLY")) {
                    if (!startDate.isAfter(viewMonthEnd)) {
                        days.add(startDate.getDayOfMonth());
                    }
                } else if (repeat.equals("YEARLY")) {
                    if (!startDate.isAfter(viewMonthEnd) && startDate.getMonthValue() == month) {
                        days.add(startDate.getDayOfMonth());
                    }
                } else if (repeat.equals("WEEKLY")) {
                    LocalDate current = startDate;
                    while (!current.isAfter(viewMonthEnd)) {
                        if (current.getYear() == year && current.getMonthValue() == month) {
                            days.add(current.getDayOfMonth());
                        }
                        current = current.plusWeeks(1);
                    }
                }
            }
        }

        String calendar = CalendarUtil.generateSymbolicCalendar(year, month, days);
        String desc = "### \uD83C\uDFAB Discount Infrastructure\n" +
                "Manage scheduled discount events (Auto/Manual).\n\n" +
                calendar + "\n" +
                "*Dates marked with `*` indicate scheduled events.*";

        ActionRow navRow = ActionRow.of(
            Button.secondary("disc_prev_" + year + "_" + month, Emoji.fromFormatted("\u25C0\uFE0F")),
            Button.secondary("disc_next_" + year + "_" + month, Emoji.fromFormatted("\u25B6\uFE0F"))
        );
        
        ActionRow actionRow = ActionRow.of(
            Button.primary("disc_deploy_auto", "New Auto Discount"),
            Button.success("disc_deploy_manual", "New Manual Discount")
        );

        event.editMessageComponents(EmbedUtil.containerBrandedRows("DISCOUNT MANAGER", "Operational Schedule", desc, null, navRow, actionRow)).queue();
    }
}
