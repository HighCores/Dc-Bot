package com.highcore.bot.utils;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.Set;

public class CalendarUtil {

    public static String generateSymbolicCalendar(int year, int month, Set<Integer> scheduledDates) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDate firstDay = ym.atDay(1);
        int length = ym.lengthOfMonth();
        int startOffset = firstDay.getDayOfWeek().getValue() % 7; // Sunday = 0

        StringBuilder sb = new StringBuilder();
        String monthName = ym.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH);

        sb.append("`|-------------------------------------------|`\n");
        sb.append("`|` **").append(String.format("%-39s", monthName + " " + year)).append("** `|`\n");
        sb.append("`|-------------------------------------------|`\n");
        sb.append("`|  Su |  Mo |  Tu |  We |  Th |  Fr |  Sa  |`\n");
        sb.append("`|-------------------------------------------|`\n");

        int currentDay = 1;
        sb.append("`|` ");
        
        // Initial padding
        for (int i = 0; i < startOffset; i++) {
            sb.append("    | ");
        }

        for (int i = startOffset; i < 7; i++) {
            appendDay(sb, currentDay++, scheduledDates);
        }
        sb.append("`\n");

        while (currentDay <= length) {
            sb.append("`|` ");
            for (int i = 0; i < 7; i++) {
                if (currentDay <= length) {
                    appendDay(sb, currentDay++, scheduledDates);
                } else {
                    sb.append("    | ");
                }
            }
            sb.append("`\n");
        }
        
        sb.append("`|-------------------------------------------|`\n");

        return sb.toString();
    }

    private static void appendDay(StringBuilder sb, int day, Set<Integer> scheduledDates) {
        String dayStr = String.format("%02d", day);
        if (scheduledDates.contains(day)) {
            sb.append(" [").append(dayStr).append("] |"); // Higher fidelity symbol
        } else {
            sb.append("  ").append(dayStr).append("  |");
        }
    }
}
