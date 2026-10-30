package main;

import java.awt.Font;
import java.io.InputStream;

public final class UiFonts {
    private static final Font PIXEL = load();
    private static final float PIXEL_SCALE = 0.9f;
    private static boolean pixel = true;

    private UiFonts() {
    }

    public static void refresh(boolean usePixel) {
        pixel = usePixel;
    }

    public static boolean pixelAvailable() {
        return PIXEL != null;
    }

    public static Font of(int style, int size) {
        if (pixel && PIXEL != null) {
            return PIXEL.deriveFont((float) Math.round(size * PIXEL_SCALE));
        }
        return new Font("SansSerif", style, size);
    }

    private static Font load() {
        try (InputStream in = UiFonts.class.getClassLoader().getResourceAsStream("fonts/sisyphus_pixel.ttf")) {
            return in == null ? null : Font.createFont(Font.TRUETYPE_FONT, in);
        } catch (Exception e) {
            return null;
        }
    }
}
