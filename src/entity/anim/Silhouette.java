package entity.anim;

import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.WeakHashMap;

public final class Silhouette {

    private static final Map<BufferedImage, BufferedImage> WHITE = new WeakHashMap<>();

    private Silhouette() {
    }

    public static synchronized BufferedImage white(BufferedImage source) {
        BufferedImage white = WHITE.get(source);
        if (white == null) {
            white = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < source.getHeight(); y++) {
                for (int x = 0; x < source.getWidth(); x++) {
                    int alpha = source.getRGB(x, y) >>> 24;
                    if (alpha > 0) {
                        white.setRGB(x, y, (alpha << 24) | 0xFFFFFF);
                    }
                }
            }
            WHITE.put(source, white);
        }
        return white;
    }
}
