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
}
