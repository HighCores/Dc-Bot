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
    
    private static Translate getTranslateService() {
        return TranslateOptions.newBuilder().setApiKey(Config.GOOGLE_API_KEY).build().getService();
    }

    public static String translateText(String text, String targetLang) {
        if (Config.GOOGLE_API_KEY == null || Config.GOOGLE_API_KEY.isEmpty()) {
            return AIService.translate(text, targetLang); // Fallback to Groq if Google not configured
        }
        try {
            Translate translate = getTranslateService();
            Translation translation = translate.translate(text, Translate.TranslateOption.targetLanguage(targetLang));
            return translation.getTranslatedText();
        } catch (Exception e) {
            log.error("Google Translate error", e);
            return AIService.translate(text, targetLang);
        }
    }

    public static byte[] translateImage(String imageUrl, String targetLang) {
        if (Config.GOOGLE_API_KEY == null || Config.GOOGLE_API_KEY.isEmpty()) {
            return null;
        }

        try {
            // 1. Download image
            BufferedImage image = ImageIO.read(new URL(imageUrl));
            if (image == null) return null;

            List<AnnotateImageRequest> requests = new ArrayList<>();
            ByteString imgBytes = ByteString.readFrom(new URL(imageUrl).openStream());
            com.google.cloud.vision.v1.Image img = com.google.cloud.vision.v1.Image.newBuilder().setContent(imgBytes).build();
            Feature feat = Feature.newBuilder().setType(Feature.Type.TEXT_DETECTION).build();
            AnnotateImageRequest request = AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img).build();
            requests.add(request);

            try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
                BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
                List<AnnotateImageResponse> responses = response.getResponsesList();

                Graphics2D g = image.createGraphics();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Translate translate = getTranslateService();

                for (AnnotateImageResponse res : responses) {
                    if (res.hasError()) {
                        log.error("Vision Error: {}", res.getError().getMessage());
                        continue;
                    }

                    for (EntityAnnotation annotation : res.getTextAnnotationsList()) {
                        // The first annotation is the entire text block, skip it and use individual words/lines
                        if (annotation.equals(res.getTextAnnotationsList().get(0))) continue;

                        String originalText = annotation.getDescription();
                        String translatedText = translate.translate(originalText, Translate.TranslateOption.targetLanguage(targetLang)).getTranslatedText();

                        BoundingPoly poly = annotation.getBoundingPoly();
                        if (poly.getVerticesCount() > 0) {
                            Vertex v1 = poly.getVertices(0);
                            Vertex v3 = poly.getVertices(2);
                            
                            int x = v1.getX();
                            int y = v1.getY();
                            int width = v3.getX() - v1.getX();
                            int height = v3.getY() - v1.getY();

                            // Mask old text
                            g.setColor(new Color(0, 0, 0, 180)); // Subtle dark mask
                            g.fillRect(x, y, width, height);

                            // Draw translated text
                            g.setColor(Color.WHITE);
                            g.setFont(new Font("Arial", Font.BOLD, Math.max(12, height - 2)));
                            g.drawString(translatedText, x, y + height - 2);
                        }
                    }
                }
                g.dispose();
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Image translation error", e);
            return null;
        }
    }
}
