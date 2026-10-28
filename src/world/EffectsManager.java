package world;

import main.GamePanel;

import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class EffectsManager {

    public static final Color[] DUST = {new Color(0xC9B79A), new Color(0xA8977E), new Color(0x7E715F)};
    public static final Color[] SPARK = {new Color(0xFFF6D0), new Color(0xF2C46A), new Color(0xC8642E)};
    public static final Color[] SPORES = {new Color(0xF2E6C4), new Color(0xD9473A), new Color(0x8E2422)};
    public static final Color[] HURT = {new Color(0xFFFFFF), new Color(0xE05A48), new Color(0x8E2422)};

    private final GamePanel gp;
    private final List<Particle> particles = new ArrayList<>();
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

    public void drawGround(Graphics2D g2) {
        draw(g2, false);
    }

    public void drawOver(Graphics2D g2) {
        draw(g2, true);
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
