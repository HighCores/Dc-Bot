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
    private static final long ERROR_RETRY_DELAY = 10 * 60 * 1000; // 10 minutes
    
    private static Translate getTranslateService() {
        return TranslateOptions.newBuilder().setApiKey(Config.GOOGLE_API_KEY).build().getService();
    }

    public static String translateText(String text, String targetLang) {
        if (Config.GOOGLE_API_KEY == null || Config.GOOGLE_API_KEY.isEmpty()) {
            return AIService.translate(text, targetLang);
        }

        // Fast fallback if Google was recently detected as blocked/disabled
        if (System.currentTimeMillis() - lastGoogleErrorTime < ERROR_RETRY_DELAY) {
            return AIService.translate(text, targetLang);
        }

        try {
            Translate translate = getTranslateService();
            Translation translation = translate.translate(text, Translate.TranslateOption.targetLanguage(targetLang));
            return translation.getTranslatedText();
        } catch (com.google.cloud.translate.TranslateException e) {
            String msg = e.getMessage();
            if (msg.contains("SERVICE_DISABLED") || msg.contains("API_KEY_SERVICE_BLOCKED") || msg.contains("403")) {
                log.warn("Google Translate API error: {}. Switching to AI fallback for 10 minutes.", msg);
                lastGoogleErrorTime = System.currentTimeMillis();
                return AIService.translate(text, targetLang);
            }
            log.error("Google Translate error", e);
            return AIService.translate(text, targetLang);
        } catch (Exception e) {
            log.error("General Translate error", e);
            return AIService.translate(text, targetLang);
        }
    }

    public static byte[] translateImage(String imageUrl, String targetLang) {
        // Disabled as per user request to focus on text and buttons only.
        return null;
    }
}
