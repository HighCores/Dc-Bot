import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.net.URL;

public class scratch {
    public static void main(String[] args) throws Exception {
        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) new java.net.URL("https://i.imgur.com/HtHQ1vP.png").openConnection();
        conn.setRequestProperty("User-Agent", "Mozilla/5.0");
        BufferedImage img = ImageIO.read(conn.getInputStream());
        int w = img.getWidth();
        int h = img.getHeight();
        System.out.println("Image size: " + w + "x" + h);
        int minX = -1, maxX = -1, minY = -1, maxY = -1;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = img.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                int a = (rgb >> 24) & 0xFF;
                if (r < 30 && g < 30 && b < 30 && a > 200 && x < w/2) {
                    if (minX == -1 || x < minX) minX = x;
                    if (maxX < x) maxX = x;
                    if (minY == -1 || y < minY) minY = y;
                    if (maxY < y) maxY = y;
                }
            }
        }
        System.out.println(String.format("Box bounded by: x=%d y=%d w=%d h=%d", minX, minY, maxX-minX, maxY-minY));
    }
}
