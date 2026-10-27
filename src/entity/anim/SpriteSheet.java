package entity.anim;

import entity.Direction8;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public final class SpriteSheet {
    private static final Map<String, SpriteSheet> CACHE = new HashMap<>();

    public final String name;
    public final int frameWidth;
    public final int frameHeight;
    public final int anchorX;
    public final int anchorY;
    public final int directions;
    public final int frameCount;
    public final int frameTicks;
    public final boolean loop;

    private final BufferedImage[][] frames;

    private SpriteSheet(String name, Properties props, BufferedImage image) {
        this.name = name;
        frameWidth = intProp(props, "frameWidth");
        frameHeight = intProp(props, "frameHeight");
        anchorX = intProp(props, "anchorX");
        anchorY = intProp(props, "anchorY");
        directions = intProp(props, "directions");
        frameCount = intProp(props, "frames");
        frameTicks = Math.max(1, intProp(props, "frameTicks"));
        loop = Boolean.parseBoolean(props.getProperty("loop", "true").trim());

        if (directions != 1 && directions != 5 && directions != 8) {
            throw new IllegalStateException(name + ": directions must be 1, 5 or 8, was " + directions);
        }
        if (image.getWidth() < frameWidth * frameCount || image.getHeight() < frameHeight * directions) {
            throw new IllegalStateException(name + ": image is " + image.getWidth() + "x" + image.getHeight()
                    + ", expected at least " + frameWidth * frameCount + "x" + frameHeight * directions);
        }

        frames = new BufferedImage[directions][frameCount];
        GraphicsConfiguration config = graphicsConfiguration();
        for (int row = 0; row < directions; row++) {
            for (int col = 0; col < frameCount; col++) {
                BufferedImage cut = image.getSubimage(col * frameWidth, row * frameHeight, frameWidth, frameHeight);
                frames[row][col] = toCompatible(cut, config);
            }
        }
    }

    public static synchronized SpriteSheet load(String path) {
        SpriteSheet sheet = CACHE.get(path);
        if (sheet == null) {
            sheet = new SpriteSheet(path, readProperties(path + ".properties"), readImage(path + ".png"));
            CACHE.put(path, sheet);
        }
        return sheet;
    }

    public Frame frame(Direction8 facing, int index) {
        Direction8 dir = facing == null ? Direction8.S : facing;
        int col = Math.floorMod(index, frameCount);
        switch (directions) {
            case 1:
                return new Frame(this, frames[0][col], false);
            case 5:
                Direction8 east = dir.mirroredToEast();
                return new Frame(this, frames[fiveRow(east)][col], east != dir);
            default:
                return new Frame(this, frames[dir.ordinal()][col], false);
        }
    }

    private static int fiveRow(Direction8 eastOrVertical) {
        switch (eastOrVertical) {
            case S: return 0;
            case SE: return 1;
            case E: return 2;
            case NE: return 3;
            case N: return 4;
            default: throw new IllegalArgumentException("Not an east-side direction: " + eastOrVertical);
        }
    }

    public static final class Frame {
        public final SpriteSheet sheet;
        public final BufferedImage image;
        public final boolean mirrored;

        Frame(SpriteSheet sheet, BufferedImage image, boolean mirrored) {
            this.sheet = sheet;
            this.image = image;
            this.mirrored = mirrored;
        }

        public int anchorX() {
            return mirrored ? sheet.frameWidth - sheet.anchorX : sheet.anchorX;
        }

        public void draw(Graphics2D g2, int footX, int footY, int scale) {
            int w = sheet.frameWidth * scale;
            int h = sheet.frameHeight * scale;
            int x = footX - anchorX() * scale;
            int y = footY - sheet.anchorY * scale;
            if (mirrored) {
                g2.drawImage(image, x + w, y, -w, h, null);
            } else {
                g2.drawImage(image, x, y, w, h, null);
            }
        }
    }

    private static int intProp(Properties props, String key) {
        String value = props.getProperty(key);
        if (value == null) {
            throw new IllegalStateException("Missing sprite sheet property: " + key);
        }
        return Integer.parseInt(value.trim());
    }

    private static Properties readProperties(String path) {
        try (InputStream stream = SpriteSheet.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalStateException("Missing sprite sheet description: " + path);
            }
            Properties props = new Properties();
            props.load(stream);
            return props;
        } catch (IOException e) {
            throw new IllegalStateException("Could not read sprite sheet description: " + path, e);
        }
    }

    private static BufferedImage readImage(String path) {
        try (InputStream stream = SpriteSheet.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalStateException("Missing sprite sheet image: " + path);
            }
            return ImageIO.read(stream);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read sprite sheet image: " + path, e);
        }
    }

    private static GraphicsConfiguration graphicsConfiguration() {
        if (GraphicsEnvironment.isHeadless()) {
            return null;
        }
        return GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
    }

    private static BufferedImage toCompatible(BufferedImage source, GraphicsConfiguration config) {
        BufferedImage copy = config != null
                ? config.createCompatibleImage(source.getWidth(), source.getHeight(), Transparency.TRANSLUCENT)
                : new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = copy.createGraphics();
        g.drawImage(source, 0, 0, null);
        g.dispose();
        return copy;
    }
}
