import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import javax.imageio.ImageIO;
import java.io.File;

public class AsciiArt {
    public static void main(String[] args) throws Exception {
        BufferedImage img = ImageIO.read(new File("Winner_.jpg"));
        int width = 80;
        int height = 30;
        BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = scaled.createGraphics();
        g.drawImage(img, 0, 0, width, height, null);
        g.dispose();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = scaled.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g_ = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                int gray = (r + g_ + b) / 3;
                char c = gray < 50 ? '#' : (gray < 100 ? '*' : (gray < 150 ? '+' : (gray < 200 ? '-' : ' ')));
                System.out.print(c);
            }
            System.out.println();
        }
    }
}
