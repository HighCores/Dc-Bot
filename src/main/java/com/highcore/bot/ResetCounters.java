package com.highcore.bot;

import com.highcore.bot.database.SupabaseClient;
import com.highcore.bot.config.Config;

public class ResetCounters {
    public static void main(String[] args) {
        System.out.println("Resetting ticket counters...");
        SupabaseClient.setSetting("ticket_next_id_ORDER", "1");
        SupabaseClient.setSetting("ticket_next_id_SUPPORT", "1");
        SupabaseClient.setSetting("ticket_next_id_COMPLAINT", "1");
        System.out.println("Counters reset to 1 (Next ticket will be 0001).");
    }
}
