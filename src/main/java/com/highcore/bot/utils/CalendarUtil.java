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
        LocalDate now = LocalDate.now();

        StringBuilder sb = new StringBuilder();
        String monthName = ym.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH);

        sb.append("**").append(monthName).append(" ").append(year).append("**\n");
        sb.append("` Su  Mo  Tu  We  Th  Fr  Sa `\n");

        int currentDay = 1;
        sb.append("` ");
        
        // Initial padding
        for (int i = 0; i < startOffset; i++) {
            sb.append("    ");
        }

        for (int i = startOffset; i < 7; i++) {
            boolean isToday = now.getYear() == year && now.getMonthValue() == month && now.getDayOfMonth() == currentDay;
            appendDay(sb, currentDay++, scheduledDates, isToday);
        }
        sb.append("` \n");

        while (currentDay <= length) {
            sb.append("` ");
            for (int i = 0; i < 7; i++) {
                if (currentDay <= length) {
                    boolean isToday = now.getYear() == year && now.getMonthValue() == month && now.getDayOfMonth() == currentDay;
                    appendDay(sb, currentDay++, scheduledDates, isToday);
                } else {
                    sb.append("    ");
                }
            }
            sb.append("` \n");
        }
        
        return sb.toString();
    }

    private static void appendDay(StringBuilder sb, int day, Set<Integer> scheduledDates, boolean isToday) {
        String dayStr = String.format("%02d", day);
        if (isToday) {
            if (scheduledDates.contains(day)) {
                sb.append("!!").append(dayStr).append(" "); // Today + Scheduled High Priority
            } else {
                sb.append(">>").append(dayStr).append(" "); // Today Only
            }
        } else if (scheduledDates.contains(day)) {
            sb.append("[").append(dayStr).append("] "); // Scheduled Only
        } else {
            sb.append(" ").append(dayStr).append("  "); // Regular
        }
    }
}
