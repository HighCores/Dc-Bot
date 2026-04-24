package com.highcore.bot.services;

import com.google.cloud.translate.Translate;
import com.google.cloud.translate.TranslateOptions;
import com.google.cloud.translate.Translation;
import com.google.cloud.vision.v1.*;
import com.google.protobuf.ByteString;
import com.highcore.bot.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class TranslationService {
    private static final Logger log = LoggerFactory.getLogger(TranslationService.class);
    private static long lastGoogleErrorTime = 0;
    private static final long ERROR_RETRY_DELAY = 10 * 60 * 1000;
    
    // Simple cache to make translations instant for repeated calls
    private static final java.util.Map<String, String> cache = new java.util.concurrent.ConcurrentHashMap<>();

    
    private static Translate getTranslateService() {
        return TranslateOptions.newBuilder().setApiKey(Config.GOOGLE_API_KEY).build().getService();
    }

    public static String translateText(String text, String targetLang) {
        String cacheKey = targetLang + ":" + text;
        if (cache.containsKey(cacheKey)) return cache.get(cacheKey);

        if (Config.GOOGLE_API_KEY == null || Config.GOOGLE_API_KEY.isEmpty() ||
            (System.currentTimeMillis() - lastGoogleErrorTime < ERROR_RETRY_DELAY)) {
            String result = AIService.translate(text, targetLang);
            cache.put(cacheKey, result);
            return result;
        }

        try {
            Translate translate = getTranslateService();
            Translation translation = translate.translate(text, Translate.TranslateOption.targetLanguage(targetLang));
            String result = translation.getTranslatedText();
            cache.put(cacheKey, result);
            return result;
        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg != null && (msg.contains("SERVICE_DISABLED") || msg.contains("403"))) {
                lastGoogleErrorTime = System.currentTimeMillis();
            }
            String result = AIService.translate(text, targetLang);
            cache.put(cacheKey, result);
            return result;
        }
    }

    public static byte[] translateImage(String imageUrl, String targetLang) {
        // Disabled as per user request to focus on text and buttons only.
        return null;
    }
}
