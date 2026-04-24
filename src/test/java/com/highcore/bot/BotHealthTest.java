package com.highcore.bot;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class BotHealthTest {

    @Test
    void testBasicAssertions() {
        // Basic test to verify JUnit 5 is working
        assertTrue(true, "The world is still sane");
    }

    @Test
    void testConfigMock() {
        // Verify we can access classes from main source
        assertNotNull(com.highcore.bot.config.Config.GUILD_ID, "Guild ID should be defined");
    }

    @Test
    void testVoucherCodeGeneration() {
        String code = com.highcore.bot.services.VoucherService.generateRandomCode(50, true);
        assertNotNull(code);
        assertTrue(code.startsWith("HC50-"), "Code should start with prefix HC and value");
        assertEquals(9, code.length(), "Code should be exactly 9 characters long");
    }

    @Test
    void testGiveawayWinnerSelection() {
        java.util.List<String> users = new java.util.ArrayList<>();
        for(int i=0; i<10; i++) users.add("user"+i);
        
        java.util.List<String> winners = com.highcore.bot.services.GiveawayService.pickWinners(users, 3);
        assertEquals(3, winners.size(), "Should pick exactly 3 winners");
        assertTrue(users.containsAll(winners), "Winners must be from the original list");
    }
}
