package entity.anim;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public final class WeaponSprite {

    private static final int ANGLE_STEPS = 32;
    private static final Map<String, WeaponSprite> CACHE = new HashMap<>();

    private final BufferedImage image;
    private final int gripX;
    private final int gripY;
    private final int tipX;
    private final int tipY;
    private final Rotated[][] rotations = new Rotated[2][ANGLE_STEPS];

    private WeaponSprite(BufferedImage image, Properties props) {
        this.image = image;
        gripX = Integer.parseInt(props.getProperty("gripX").trim());
        gripY = Integer.parseInt(props.getProperty("gripY").trim());
        tipX = Integer.parseInt(props.getProperty("tipX").trim());
        tipY = Integer.parseInt(props.getProperty("tipY").trim());
    }

    public static synchronized WeaponSprite load(String name) {
        WeaponSprite sprite = CACHE.get(name);
        if (sprite == null) {
            String path = "weapons/" + name;
            sprite = new WeaponSprite(readImage(path + ".png"), readProperties(path + ".properties"));
            CACHE.put(name, sprite);
        }
        return sprite;
    }

    public double length() {
        return Math.hypot(tipX - gripX, tipY - gripY);
    }

    public synchronized Rotated rotated(double angle, boolean flipped) {
        int step = Math.floorMod((int) Math.round(angle / (2 * Math.PI) * ANGLE_STEPS), ANGLE_STEPS);
        int flipIndex = flipped ? 1 : 0;
        if (rotations[flipIndex][step] == null) {
            rotations[flipIndex][step] = build(step * 2 * Math.PI / ANGLE_STEPS, flipped);
        }
        return rotations[flipIndex][step];
    }

    private Rotated build(double angle, boolean flipped) {
        int w = image.getWidth();
        int h = image.getHeight();
        double gx = gripX + 0.5;
        double gy = (flipped ? h - 1 - gripY : gripY) + 0.5;

        AffineTransform aroundGrip = new AffineTransform();
        aroundGrip.rotate(angle);
        aroundGrip.translate(-gx, -gy);
        if (flipped) {
            aroundGrip.translate(0, h);
            aroundGrip.scale(1, -1);
        }

        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        for (double[] corner : new double[][]{{0, 0}, {w, 0}, {0, h}, {w, h}}) {
            Point2D p = aroundGrip.transform(new Point2D.Double(corner[0], corner[1]), null);
            minX = Math.min(minX, p.getX());
            minY = Math.min(minY, p.getY());
            maxX = Math.max(maxX, p.getX());
            maxY = Math.max(maxY, p.getY());
        }
        int gripPixelX = (int) Math.ceil(-minX) + 1;
        int gripPixelY = (int) Math.ceil(-minY) + 1;
        int outW = gripPixelX + (int) Math.ceil(maxX) + 2;
        int outH = gripPixelY + (int) Math.ceil(maxY) + 2;

        AffineTransform place = new AffineTransform();
        place.translate(gripPixelX + 0.5, gripPixelY + 0.5);
        place.concatenate(aroundGrip);

        BufferedImage out = new BufferedImage(outW, outH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.drawImage(image, place, null);
        g.dispose();

        Point2D tip = place.transform(new Point2D.Double(tipX + 0.5, tipY + 0.5), null);
        return new Rotated(out, gripPixelX, gripPixelY, (int) Math.floor(tip.getX()), (int) Math.floor(tip.getY()));
    }

    public static final class Rotated {
        public final BufferedImage image;
        public final int gripX;
        public final int gripY;
        public final int tipX;
        public final int tipY;

        Rotated(BufferedImage image, int gripX, int gripY, int tipX, int tipY) {
            this.image = image;
            this.gripX = gripX;
            this.gripY = gripY;
            this.tipX = tipX;
            this.tipY = tipY;
        }
    }

    private static Properties readProperties(String path) {
        try (InputStream stream = WeaponSprite.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalStateException("Missing weapon sprite description: " + path);
            }
            Properties props = new Properties();
            props.load(stream);
            return props;
        } catch (IOException e) {
            throw new IllegalStateException("Could not read weapon sprite description: " + path, e);
        }
    }

    private static BufferedImage readImage(String path) {
        try (InputStream stream = WeaponSprite.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalStateException("Missing weapon sprite image: " + path);
            }
            return ImageIO.read(stream);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read weapon sprite image: " + path, e);
        }
    }
}
