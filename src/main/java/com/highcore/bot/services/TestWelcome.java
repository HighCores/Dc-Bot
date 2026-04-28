package com.highcore.bot.services;

import java.io.File;
import java.io.FileInputStream;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;

public class TestWelcome {
    public static void main(String[] args) throws Exception {
        System.out.println("Testing Welcome Card Image Loading...");
        
        File f = new File("src/main/resources/welcome.png");
        System.out.println("Exists: " + f.exists() + ", Size: " + f.length());
        
        try (FileInputStream fis = new FileInputStream(f)) {
            BufferedImage img = ImageIO.read(fis);
            System.out.println("Image read successfully: " + img.getWidth() + "x" + img.getHeight());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        System.out.println("Testing Imgur fetch...");
        try {
            String urlStr = "https://i.imgur.com/Lzun3rb.png";
            java.net.URL url = new java.net.URL(urlStr);
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(8000);
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36");
            
            BufferedImage bg = ImageIO.read(connection.getInputStream());
            if (bg == null) {
                System.out.println("Imgur fetch returned null!");
            } else {
                System.out.println("Imgur fetch successful: " + bg.getWidth() + "x" + bg.getHeight());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
