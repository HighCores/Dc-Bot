
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.io.InputStream;
import java.net.HttpURLConnection;

public class ImageInfo {
    public static void main(String[] args) {
        try {
            URL url = new URL("https://i.imgur.com/HtHQ1vP.png");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36");
            conn.setRequestProperty("Accept", "image/png,image/jpeg,image/*;q=0.8");
            conn.setConnectTimeout(20000);
            conn.setReadTimeout(20000);
            conn.setInstanceFollowRedirects(true);
            
            try (InputStream is = conn.getInputStream()) {
                BufferedImage img = ImageIO.read(is);
                if (img == null) {
                    System.out.println("Failed to read image (null)");
                } else {
                    System.out.println("Width: " + img.getWidth());
                    System.out.println("Height: " + img.getHeight());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
