package world;

import main.GamePanel;

import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class EffectsManager {

    public static final Color[] DUST = {new Color(0xC1AF91), new Color(0xA08F76), new Color(0x776A58)};
    public static final Color[] SPARK = {new Color(0xF7EEC7), new Color(0xEBBC5F), new Color(0xC25D27)};
    public static final Color[] SPORES = {new Color(0xEADEBB), new Color(0xD34134), new Color(0x89201E)};
    public static final Color[] HURT = {new Color(0xF7F7F7), new Color(0xDA5341), new Color(0x89201E)};

    public static final Color[] GOO = {new Color(0xCDEFB9), new Color(0x50C73B), new Color(0x17931A)};
    public static final Color[] GOLD = {new Color(0xF7EEC7), new Color(0xEBBC5F), new Color(0xC17E25)};

    private final GamePanel gp;
    private final List<Particle> particles = new ArrayList<>();
    private final List<FloatText> texts = new ArrayList<>();
    private final List<Ring> rings = new ArrayList<>();
    private int flashTicks;
    private int flashTotalTicks;
    private Color flashColor = Color.WHITE;
    private float flashAlpha;
    private final Random random = new Random();
    private int shakeTicks;
    private int shakeTotalTicks;
    private double shakeStrength;
    private int shakeX;
    private int shakeY;

    public EffectsManager(GamePanel gp) {
        this.gp = gp;
    }

    public void clear() {
        particles.clear();
        texts.clear();
        rings.clear();
        flashTicks = 0;
        shakeTicks = 0;
        shakeX = 0;
        shakeY = 0;
    }

    public void update() {
        Iterator<Particle> iterator = particles.iterator();
        while (iterator.hasNext()) {
            Particle p = iterator.next();
            p.x += p.vx;
            p.y += p.vy;
            p.vx *= p.drag;
            p.vy = p.vy * p.drag + p.gravity;
            p.life--;
            if (p.life <= 0) {
                iterator.remove();
            }
        }

        texts.removeIf(t -> --t.life <= 0);
        for (FloatText t : texts) {
            t.y += t.vy;
            t.vy *= 0.9;
        }
        rings.removeIf(r -> --r.life <= 0);
        if (flashTicks > 0) {
            flashTicks--;
        }

        if (shakeTicks > 0) {
            shakeTicks--;
            double strength = shakeStrength * shakeTicks / (double) shakeTotalTicks;
            int step = gp.scale;
            shakeX = (int) Math.round((random.nextDouble() * 2 - 1) * strength / step) * step;
            shakeY = (int) Math.round((random.nextDouble() * 2 - 1) * strength / step) * step;
        } else {
            shakeX = 0;
            shakeY = 0;
        }
    }

    public void shake(int ticks, double strengthPixels) {
        if (strengthPixels >= shakeStrength * shakeTicks / Math.max(1.0, shakeTotalTicks)) {
            shakeTicks = ticks;
            shakeTotalTicks = ticks;
            shakeStrength = strengthPixels;
        }
    }

    public int shakeX() {
        return shakeX;
    }

    public int shakeY() {
        return shakeY;
    }

    public void dust(double worldX, double worldY, double dirX, double dirY, int count) {
        for (int i = 0; i < count; i++) {
            double spread = (random.nextDouble() - 0.5) * 1.4;
            double speed = 0.6 + random.nextDouble() * 1.2;
            Particle p = new Particle(worldX + (random.nextDouble() - 0.5) * 12, worldY + (random.nextDouble() - 0.5) * 4,
                    (dirX + spread) * speed, (dirY + spread * 0.4) * speed - 0.3, 20 + random.nextInt(12), DUST, false);
            p.drag = 0.86;
            p.gravity = -0.02;
            p.size = random.nextInt(2) == 0 ? 2 : 1;
            particles.add(p);
        }
    }

    public void sparks(double worldX, double worldY, double dirX, double dirY, int count, Color[] colors) {
        double length = Math.hypot(dirX, dirY);
        double ux = length > 0 ? dirX / length : 0;
        double uy = length > 0 ? dirY / length : -1;
        for (int i = 0; i < count; i++) {
            double angle = Math.atan2(uy, ux) + (random.nextDouble() - 0.5) * 1.8;
            double speed = 2.5 + random.nextDouble() * 3.5;
            Particle p = new Particle(worldX, worldY, Math.cos(angle) * speed, Math.sin(angle) * speed,
                    8 + random.nextInt(8), colors, true);
            p.drag = 0.8;
            p.gravity = 0.12;
            particles.add(p);
        }
    }

    public void burst(double worldX, double worldY, int count, double maxSpeed, Color[] colors) {
        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double speed = 0.5 + random.nextDouble() * maxSpeed;
            Particle p = new Particle(worldX, worldY, Math.cos(angle) * speed, Math.sin(angle) * speed - 0.8,
                    26 + random.nextInt(24), colors, true);
            p.drag = 0.9;
            p.gravity = -0.03;
            p.size = random.nextInt(4) == 0 ? 2 : 1;
            particles.add(p);
        }
    }

    // A word or number that pops up, rises and fades (damage numbers, DETONATE, SWAP!).
    public void text(double worldX, double worldY, String text, Color color, int scale) {
        FloatText t = new FloatText();
        t.x = worldX + (random.nextDouble() - 0.5) * 10;
        t.y = worldY;
        t.vy = -2.2;
        t.text = text;
        t.color = color;
        t.scale = scale;
        t.life = t.maxLife = 46;
        texts.add(t);
    }

    // An expanding ring on the ground or in the air (shockwaves, detonations, cut projectiles).
    public void ring(double worldX, double worldY, double maxRadius, int ticks, Color color, boolean ground) {
        Ring r = new Ring();
        r.x = worldX;
        r.y = worldY;
        r.maxRadius = maxRadius;
        r.life = r.maxLife = ticks;
        r.color = color;
        r.ground = ground;
        rings.add(r);
    }

    // A full-screen flash, drawn over the world (phase changes, detonations).
    public void flash(int ticks, Color color, float alpha) {
        flashTicks = flashTotalTicks = ticks;
        flashColor = color;
        flashAlpha = alpha;
    }

    public void drawGround(Graphics2D g2) {
        drawRings(g2, true);
        draw(g2, false);
    }

    public void drawOver(Graphics2D g2) {
        draw(g2, true);
        drawRings(g2, false);
        drawTexts(g2);
    }

    public void drawScreen(Graphics2D g2) {
        if (flashTicks <= 0) {
            return;
        }
        Composite old = g2.getComposite();
        g2.setComposite(AlphaComposite.SrcOver.derive(flashAlpha * flashTicks / (float) flashTotalTicks));
        g2.setColor(flashColor);
        g2.fillRect(0, 0, gp.screenWidth, gp.screenHeight);
        g2.setComposite(old);
    }

    private void drawRings(Graphics2D g2, boolean ground) {
        Stroke oldStroke = g2.getStroke();
        Composite oldComposite = g2.getComposite();
        for (Ring r : rings) {
            if (r.ground != ground) {
                continue;
            }
            double t = 1 - r.life / (double) r.maxLife;
            double ease = 1 - (1 - t) * (1 - t);
            int radius = (int) Math.round(r.maxRadius * ease);
            int sx = (int) Math.round(r.x - gp.player.worldX + gp.player.screenX);
            int sy = (int) Math.round(r.y - gp.player.worldY + gp.player.screenY);
            float width = (float) Math.max(gp.scale, (1 - t) * gp.scale * 3);
            g2.setComposite(AlphaComposite.SrcOver.derive((float) Math.max(0, 1 - t) * 0.9f));
            g2.setStroke(new BasicStroke(width));
            g2.setColor(r.color);
            int ry = r.ground ? (int) (radius * 0.6) : radius;
            g2.drawOval(sx - radius, sy - ry, radius * 2, ry * 2);
        }
        g2.setStroke(oldStroke);
        g2.setComposite(oldComposite);
    }

    private void drawTexts(Graphics2D g2) {
        Composite oldComposite = g2.getComposite();
        for (FloatText t : texts) {
            int age = t.maxLife - t.life;
            // pop in big, settle, fade at the end
            int scale = age < 4 ? t.scale + 1 : t.scale;
            float alpha = t.life < 12 ? t.life / 12f : 1f;
            int width = main.UiSkin.PixelFont.width(t.text, scale);
            int sx = (int) Math.round(t.x - gp.player.worldX + gp.player.screenX) - width / 2;
            int sy = (int) Math.round(t.y - gp.player.worldY + gp.player.screenY);
            g2.setComposite(AlphaComposite.SrcOver.derive(alpha));
            main.UiSkin.PixelFont.drawOutlined(g2, t.text, sx, sy, scale, t.color, new Color(0, 0, 0, 120));
        }
        g2.setComposite(oldComposite);
    }

    private void draw(Graphics2D g2, boolean over) {
        Color old = g2.getColor();
        int scale = gp.scale;
        for (Particle p : particles) {
            if (p.over != over) {
                continue;
            }
            double age = 1 - p.life / (double) p.maxLife;
            Color color = p.colors[Math.min(p.colors.length - 1, (int) (age * p.colors.length))];
            int size = p.size * scale;
            int sx = (int) Math.round(p.x - gp.player.worldX + gp.player.screenX) - size / 2;
            int sy = (int) Math.round(p.y - gp.player.worldY + gp.player.screenY) - size / 2;
            g2.setColor(color);
            g2.fillRect(sx, sy, size, size);
        }
        g2.setColor(old);
    }

    private static final class FloatText {
        double x;
        double y;
        double vy;
        String text;
        Color color;
        int scale;
        int life;
        int maxLife;
    }

    private static final class Ring {
        double x;
        double y;
        double maxRadius;
        int life;
        int maxLife;
        Color color;
        boolean ground;
    }

    private static final class Particle {
        double x;
        double y;
        double vx;
        double vy;
        double drag = 0.9;
        double gravity;
        int life;
        final int maxLife;
        final Color[] colors;
        final boolean over;
        int size = 1;

        Particle(double x, double y, double vx, double vy, int life, Color[] colors, boolean over) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.life = life;
            this.maxLife = life;
            this.colors = colors;
            this.over = over;
        }
    }
}
