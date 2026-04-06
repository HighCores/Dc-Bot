package com.highcore.bot.utils;

import java.util.HashMap;
import java.util.Map;

public class EmojiUtil {
    private static final Map<String, String> EMOJI_MAP = new HashMap<>();

    static {
        EMOJI_MAP.put(":heart:", "❤️");
        EMOJI_MAP.put(":check:", "✅");
        EMOJI_MAP.put(":warning:", "⚠️");
        EMOJI_MAP.put(":error:", "❌");
        EMOJI_MAP.put(":fire:", "🔥");
        EMOJI_MAP.put(":sparkles:", "✨");
        EMOJI_MAP.put(":star:", "⭐");
        EMOJI_MAP.put(":rocket:", "🚀");
        EMOJI_MAP.put(":smile:", "😄");
        EMOJI_MAP.put(":thumbsup:", "👍");
        EMOJI_MAP.put(":ok:", "👌");
    }

    public static String parse(String input) {
        if (input == null) return null;
        String output = input;
        for (Map.Entry<String, String> entry : EMOJI_MAP.entrySet()) {
            output = output.replace(entry.getKey(), entry.getValue());
        }
        return output;
    }
}
