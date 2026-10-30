package entity.anim;

import entity.Direction8;
import entity.weapon.MeleeWeapon;

import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class WeaponRenderer {

    private static final double WINDUP_PULL_RADIANS = Math.toRadians(22);
    private static final double SETTLE_RADIANS = Math.toRadians(12);
    private static final double ARM_DEPTH = 0.8;
    private static final int SMEAR_FADE_FRAMES = 3;
    private static final double SMEAR_TRAIL_RADIANS = Math.toRadians(75);
    private static final Color[] SMEAR_COLORS = {
            new Color(246, 242, 232, 190),
            new Color(236, 224, 196, 120),
            new Color(220, 205, 175, 60)
    };
    private static final Color FLASH_CORE = new Color(0xFFF6D0);
    private static final Color FLASH_EDGE = new Color(0xF2A33A);

    private WeaponRenderer() {
    }

    public static final class SwingPose {
        public final double bladeAngle;
        public final double smearFrom;
        public final double smearTo;
        public final double smearStrength;

        SwingPose(double bladeAngle, double smearFrom, double smearTo, double smearStrength) {
            this.bladeAngle = bladeAngle;
            this.smearFrom = smearFrom;
            this.smearTo = smearTo;
            this.smearStrength = smearStrength;
        }
    }

    public static SwingPose swingPose(MeleeWeapon weapon, int frame, Direction8 aim) {
        int strikeStart = -1;
        int strikeEnd = -1;
        for (int f = 0; f < weapon.totalFrames(); f++) {
            if (weapon.isStrikeFrame(f)) {
                if (strikeStart < 0) {
                    strikeStart = f;
                }
                strikeEnd = f + 1;
            }
        }
        if (strikeStart < 0) {
            return new SwingPose(aim.angle(), 0, 0, 0);
        }

        double sign = aim.dx < 0 ? -1 : 1;
        double halfArc = weapon.swingArcRadians() / 2;
        double from = aim.angle() - sign * halfArc;
        double to = aim.angle() + sign * halfArc;

        if (frame < strikeStart) {
            double t = (frame + 1) / (double) strikeStart;
            return new SwingPose(from - sign * WINDUP_PULL_RADIANS * easeOut(t), from, from, 0);
        }
        if (frame < strikeEnd) {
            double t = (frame - strikeStart + 1) / (double) (strikeEnd - strikeStart);
            double angle = from + (to - from) * easeOutCubic(t);
            return new SwingPose(angle, from, angle, 1);
        }
        int recoverFrames = Math.max(1, weapon.totalFrames() - strikeEnd);
        double t = (frame - strikeEnd + 1) / (double) recoverFrames;
        double fade = Math.max(0, 1 - (frame - strikeEnd + 1) / (double) SMEAR_FADE_FRAMES);
        return new SwingPose(to - sign * SETTLE_RADIANS * easeOut(t), from, to, fade);
    }

    public static int[] armHand(double angle, double reach, int kickArtPixels) {
        double r = Math.max(1, reach - kickArtPixels);
        return new int[]{(int) Math.round(Math.cos(angle) * r), (int) Math.round(Math.sin(angle) * r * ARM_DEPTH)};
    }

    public static void drawArm(Graphics2D g2, SpriteSheet sheet, int shoulderX, int shoulderY, int[] hand, int scale) {
        List<int[]> line = cellLine(hand[0], hand[1]);
        int[] perp = Math.abs(hand[0]) >= Math.abs(hand[1]) ? new int[]{0, 1} : new int[]{1, 0};
        Map<Long, Color> cells = new LinkedHashMap<>();
        Color sleeve = sheet.armColor("sleeve");
        Color sleeveShade = colorOr(sheet.armColor("sleeveShade"), sleeve);
        Color cuff = colorOr(sheet.armColor("cuff"), sleeveShade);
        Color skin = sheet.armColor("skin");
        Color skinShade = colorOr(sheet.armColor("skinShade"), skin);
        for (int i = 0; i < line.size() - 1; i++) {
            int[] c = line.get(i);
            boolean isCuff = i == line.size() - 2 && line.size() > 2;
            cells.put(key(c[0] + perp[0], c[1] + perp[1]), isCuff ? cuff : sleeveShade);
            cells.put(key(c[0], c[1]), isCuff ? cuff : sleeve);
        }
        cells.put(key(hand[0] + perp[0], hand[1] + perp[1]), skinShade);
        cells.put(key(hand[0], hand[1]), skin);

        Color old = g2.getColor();
        g2.setColor(colorOr(sheet.armColor("outline"), Color.BLACK));
        for (long k : cells.keySet()) {
            int x = (int) (k >> 32), y = (int) k;
            for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                if (!cells.containsKey(key(x + d[0], y + d[1]))) {
                    fillCell(g2, shoulderX + (x + d[0]) * scale, shoulderY + (y + d[1]) * scale, scale);
                }
            }
        }
        for (Map.Entry<Long, Color> e : cells.entrySet()) {
            long k = e.getKey();
            g2.setColor(e.getValue());
            fillCell(g2, shoulderX + (int) (k >> 32) * scale, shoulderY + (int) k * scale, scale);
        }
        g2.setColor(old);
    }

    public static void drawFist(Graphics2D g2, SpriteSheet sheet, int shoulderX, int shoulderY, int[] hand, int scale) {
        Color old = g2.getColor();
        g2.setColor(sheet.armColor("skin"));
        fillCell(g2, shoulderX + hand[0] * scale, shoulderY + hand[1] * scale, scale);
        g2.setColor(old);
    }

    private static List<int[]> cellLine(int x1, int y1) {
        List<int[]> pts = new ArrayList<>();
        int x0 = 0, y0 = 0;
        int dx = Math.abs(x1), dy = -Math.abs(y1);
        int sx = x1 > 0 ? 1 : -1, sy = y1 > 0 ? 1 : -1, err = dx + dy;
        while (true) {
            pts.add(new int[]{x0, y0});
            if (x0 == x1 && y0 == y1) {
                return pts;
            }
            int e2 = 2 * err;
            if (e2 >= dy) {
                err += dy;
                x0 += sx;
            }
            if (e2 <= dx) {
                err += dx;
                y0 += sy;
            }
        }
    }

    private static long key(int x, int y) {
        return ((long) x << 32) | (y & 0xFFFFFFFFL);
    }

    private static Color colorOr(Color color, Color fallback) {
        return color != null ? color : fallback;
    }

    public static void drawHeld(Graphics2D g2, WeaponSprite sprite, double angle, int handX, int handY, int scale, int pushArtPixels) {
        WeaponSprite.Rotated rotated = rotatedFor(sprite, angle);
        int pushX = (int) Math.round(Math.cos(angle) * pushArtPixels);
        int pushY = (int) Math.round(Math.sin(angle) * pushArtPixels);
        int x = handX + (pushX - rotated.gripX) * scale;
        int y = handY + (pushY - rotated.gripY) * scale;
        Object old = g2.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g2.drawImage(rotated.image, x, y, rotated.image.getWidth() * scale, rotated.image.getHeight() * scale, null);
        if (old != null) {
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, old);
        }
    }

    public static void drawMuzzleFlash(Graphics2D g2, WeaponSprite sprite, double angle, int handX, int handY, int scale, int pushArtPixels, int ticksLeft) {
        WeaponSprite.Rotated rotated = rotatedFor(sprite, angle);
        int pushX = (int) Math.round(Math.cos(angle) * pushArtPixels);
        int pushY = (int) Math.round(Math.sin(angle) * pushArtPixels);
        int tipX = handX + (rotated.tipX - rotated.gripX + pushX) * scale;
        int tipY = handY + (rotated.tipY - rotated.gripY + pushY) * scale;
        int cx = tipX + (int) Math.round(Math.cos(angle) * 2) * scale;
        int cy = tipY + (int) Math.round(Math.sin(angle) * 2) * scale;
        Color old = g2.getColor();
        if (ticksLeft >= 3) {
            g2.setColor(FLASH_EDGE);
            fillCell(g2, cx - scale, cy, scale);
            fillCell(g2, cx + scale, cy, scale);
            fillCell(g2, cx, cy - scale, scale);
            fillCell(g2, cx, cy + scale, scale);
            fillCell(g2, cx + (int) Math.round(Math.cos(angle) * 2) * scale, cy + (int) Math.round(Math.sin(angle) * 2) * scale, scale);
        }
        g2.setColor(FLASH_CORE);
        fillCell(g2, cx, cy, scale);
        g2.setColor(old);
    }

    public static void drawSmear(Graphics2D g2, SwingPose pose, double innerRadius, double outerRadius, int handX, int handY, int scale) {
        if (pose.smearStrength <= 0) {
            return;
        }
        double sweep = pose.smearTo - pose.smearFrom;
        if (Math.abs(sweep) < 1e-6) {
            return;
        }
        Color old = g2.getColor();
        int r = (int) Math.ceil(outerRadius);
        for (int dy = -r; dy <= r; dy++) {
            for (int dx = -r; dx <= r; dx++) {
                double cx = dx + 0.5;
                double cy = dy + 0.5;
                double dist = Math.hypot(cx, cy);
                if (dist < innerRadius || dist > outerRadius) {
                    continue;
                }
                double angle = Math.atan2(cy, cx);
                double fromStart = sweep > 0 ? normalize(angle - pose.smearFrom) : normalize(pose.smearFrom - angle);
                if (fromStart > Math.abs(sweep)) {
                    continue;
                }
                double behindBlade = (Math.abs(sweep) - fromStart) / SMEAR_TRAIL_RADIANS;
                double faded = behindBlade + (1 - pose.smearStrength);
                int level = (int) (faded * SMEAR_COLORS.length);
                if (level >= SMEAR_COLORS.length) {
                    continue;
                }
                g2.setColor(SMEAR_COLORS[level]);
                fillCell(g2, handX + dx * scale, handY + dy * scale, scale);
            }
        }
        g2.setColor(old);
    }

    private static WeaponSprite.Rotated rotatedFor(WeaponSprite sprite, double angle) {
        return sprite.rotated(angle, Math.cos(angle) < -1e-6);
    }

    private static void fillCell(Graphics2D g2, int x, int y, int scale) {
        g2.fillRect(x, y, scale, scale);
    }

    private static double normalize(double angle) {
        double a = angle % (2 * Math.PI);
        return a < 0 ? a + 2 * Math.PI : a;
    }

    private static double easeOut(double t) {
        double c = Math.min(1, Math.max(0, t));
        return 1 - (1 - c) * (1 - c);
    }

    private static double easeOutCubic(double t) {
        double c = Math.min(1, Math.max(0, t));
        return 1 - Math.pow(1 - c, 3);
    }
}
