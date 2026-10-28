package entity.anim;

import entity.Direction8;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public final class SpriteSheet {
    private static final Map<String, SpriteSheet> CACHE = new HashMap<>();
    private static final Set<String> MISSING = new HashSet<>();

    public final String name;
    public final int frameWidth;
    public final int frameHeight;
    public final int anchorX;
    public final int anchorY;
    public final int directions;
    public final int frameCount;
    public final int frameTicks;
    public final boolean loop;
    public final double distancePerFrame;

    private final BufferedImage[][] frames;
    private final Point[][] hands;

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
        distancePerFrame = Double.parseDouble(props.getProperty("distancePerFrame", "0").trim());

        if (directions != 1 && directions != 5 && directions != 8) {
            throw new IllegalStateException(name + ": directions must be 1, 5 or 8, was " + directions);
        }
        if (image.getWidth() < frameWidth * frameCount || image.getHeight() < frameHeight * directions) {
            throw new IllegalStateException(name + ": image is " + image.getWidth() + "x" + image.getHeight()
                    + ", expected at least " + frameWidth * frameCount + "x" + frameHeight * directions);
        }

        hands = readHands(props);

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

    public static synchronized SpriteSheet loadOptional(String path) {
        SpriteSheet cached = CACHE.get(path);
        if (cached != null || MISSING.contains(path)) {
            return cached;
        }
        if (!ArtFiles.exists(path + ".properties") || !ArtFiles.exists(path + ".png")) {
            MISSING.add(path);
            return null;
        }
        return load(path);
    }

    public static synchronized int reloadAll() {
        Map<String, SpriteSheet> reloaded = new HashMap<>();
        for (String path : CACHE.keySet()) {
            reloaded.put(path, reload(path));
        }
        Set<String> stillMissing = new HashSet<>();
        for (String path : MISSING) {
            if (ArtFiles.exists(path + ".properties") && ArtFiles.exists(path + ".png")) {
                reloaded.put(path, reload(path));
            } else {
                stillMissing.add(path);
            }
        }
        CACHE.clear();
        CACHE.putAll(reloaded);
        MISSING.clear();
        MISSING.addAll(stillMissing);
        return reloaded.size();
    }

    private static SpriteSheet reload(String path) {
        try {
            return new SpriteSheet(path, readProperties(path + ".properties"), readImage(path + ".png"));
        } catch (RuntimeException e) {
            throw new IllegalStateException(path + ": " + e.getMessage(), e);
        }
    }

    public Frame frame(Direction8 facing, int index) {
        Direction8 dir = facing == null ? Direction8.S : facing;
        int col = Math.floorMod(index, frameCount);
        switch (directions) {
            case 1:
                return new Frame(this, frames[0][col], hands[0][col], false, col);
            case 5:
                Direction8 east = dir.mirroredToEast();
                int row = fiveRow(east);
                return new Frame(this, frames[row][col], hands[row][col], east != dir, col);
            default:
                return new Frame(this, frames[dir.ordinal()][col], hands[dir.ordinal()][col], false, col);
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
        public final int index;
        private final Point hand;

        Frame(SpriteSheet sheet, BufferedImage image, Point hand, boolean mirrored, int index) {
            this.sheet = sheet;
            this.image = image;
            this.hand = hand;
            this.mirrored = mirrored;
            this.index = index;
        }

        public boolean hasHand() {
            return hand != null;
        }

        public int handX() {
            return mirrored ? sheet.frameWidth - 1 - hand.x : hand.x;
        }

        public int handY() {
            return hand.y;
        }

        public int anchorX() {
            return mirrored ? sheet.frameWidth - sheet.anchorX : sheet.anchorX;
        }

        public void draw(Graphics2D g2, int footX, int footY, int scale) {
            draw(g2, footX, footY, scale, 0, 0, image);
        }

        public void drawWhite(Graphics2D g2, int footX, int footY, int scale, int extraWidth, int extraHeight) {
            draw(g2, footX, footY, scale, extraWidth, extraHeight, Silhouette.white(image));
        }

        public void draw(Graphics2D g2, int footX, int footY, int scale, int extraWidth, int extraHeight) {
            draw(g2, footX, footY, scale, extraWidth, extraHeight, image);
        }

        private void draw(Graphics2D g2, int footX, int footY, int scale, int extraWidth, int extraHeight, BufferedImage source) {
            int w = (sheet.frameWidth + extraWidth) * scale;
            int h = (sheet.frameHeight + extraHeight) * scale;
            int x = footX - (int) Math.round(anchorX() * (double) w / sheet.frameWidth);
            int y = footY - (int) Math.round(sheet.anchorY * (double) h / sheet.frameHeight);
            if (mirrored) {
                g2.drawImage(source, x + w, y, -w, h, null);
            } else {
                g2.drawImage(source, x, y, w, h, null);
            }
        }

        public int handScreenX(int footX, int scale, int extraWidth) {
            double stretch = (sheet.frameWidth + extraWidth) / (double) sheet.frameWidth;
            return footX + (int) Math.round((handX() - anchorX()) * scale * stretch);
        }

        public int handScreenY(int footY, int scale, int extraHeight) {
            double stretch = (sheet.frameHeight + extraHeight) / (double) sheet.frameHeight;
            return footY + (int) Math.round((handY() - sheet.anchorY) * scale * stretch);
        }
    }

    private Point[][] readHands(Properties props) {
        Point[][] result = new Point[directions][frameCount];
        for (int row = 0; row < directions; row++) {
            String rowKey = directions == 1 ? "hand" : "hand." + rowDirection(row).name();
            Point rowHand = parsePoint(props.getProperty(rowKey));
            for (int col = 0; col < frameCount; col++) {
                Point frameHand = parsePoint(props.getProperty(rowKey + "." + col));
                result[row][col] = frameHand != null ? frameHand : rowHand;
            }
        }
        return result;
    }

    private Direction8 rowDirection(int row) {
        if (directions == 5) {
            return new Direction8[]{Direction8.S, Direction8.SE, Direction8.E, Direction8.NE, Direction8.N}[row];
        }
        return Direction8.values()[row];
    }

    private static Point parsePoint(String value) {
        if (value == null) {
            return null;
        }
        String[] parts = value.split(",");
        return new Point(Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim()));
    }

    private static int intProp(Properties props, String key) {
        String value = props.getProperty(key);
        if (value == null) {
            throw new IllegalStateException("Missing sprite sheet property: " + key);
        }
        return Integer.parseInt(value.trim());
    }

    private static Properties readProperties(String path) {
        try (InputStream stream = ArtFiles.open(path)) {
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
        try (InputStream stream = ArtFiles.open(path)) {
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
