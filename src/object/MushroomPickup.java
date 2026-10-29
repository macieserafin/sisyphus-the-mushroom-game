package object;

import entity.Player;
import main.GamePanel;
import main.Sound;
import main.UiSkin;
import world.EffectsManager;

import java.awt.*;
import java.util.Random;

// Currency dropped by enemies and chests. It pops out with a hop, bobs, is pulled toward a nearby player
// and goes into the run's wallet on touch. A golden one is worth five. It blinks before it fades away.
public class MushroomPickup extends SuperObject {
    public static final int BIG_VALUE = 5;
    private static final int MAX_PIECES = 12;
    private static final int LIFETIME = 60 * 30;
    private static final int BLINK_FROM = 180;
    private static final double MAGNET_RANGE = 120;
    private static final Color GLOW = new Color(0xEBBC5F);

    private static final Random RANDOM = new Random();

    private final int value;
    private double x;
    private double y;
    private double vx;
    private double height = 0;
    private double vHeight;
    private int age = 0;

    public MushroomPickup(double centerX, double groundY, int value) {
        name = "Mushroom";
        this.value = value;
        x = centerX;
        y = groundY;
        vx = (RANDOM.nextDouble() - 0.5) * 4.5;
        vHeight = 4.5 + RANDOM.nextDouble() * 2;
        syncPosition();
        solidArea = new Rectangle(8, 8, 32, 32);
        solidAreaDefaultX = solidArea.x;
        solidAreaDefaultY = solidArea.y;
    }

    // Splits a total into golden (5) and plain (1) mushrooms and throws them around a point.
    public static void scatter(GamePanel gp, double centerX, double groundY, int total) {
        int pieces = 0;
        while (total > 0 && pieces < MAX_PIECES - 1) {
            int value = total >= BIG_VALUE ? BIG_VALUE : 1;
            gp.entities.addFreeObject(new MushroomPickup(centerX, groundY, value));
            total -= value;
            pieces++;
        }
        if (total > 0) {
            gp.entities.addFreeObject(new MushroomPickup(centerX, groundY, total));
        }
    }

    public int value() {
        return value;
    }

    private void syncPosition() {
        worldX = (int) Math.round(x) - 24;
        worldY = (int) Math.round(y) - 40;
    }

    @Override
    public void update(GamePanel gp) {
        age++;
        if (age > LIFETIME) {
            gp.entities.removeObject(this);
            return;
        }
        if (height > 0 || vHeight > 0) {
            height = Math.max(0, height + vHeight);
            vHeight -= 0.45;
            x += vx;
            vx *= 0.92;
            if (height == 0 && vHeight < 0) {
                vHeight = 0;
            }
        }
        Player p = gp.player;
        double px = p.worldX + gp.tileSize / 2.0, py = p.worldY + gp.tileSize * 0.8;
        double d = Math.hypot(px - x, py - y);
        if (p.alive && age > 20 && d < MAGNET_RANGE && d > 1) {
            double pull = 2 + (MAGNET_RANGE - d) * 0.07;
            x += (px - x) / d * pull;
            y += (py - y) / d * pull;
        }
        syncPosition();
    }

    @Override
    public void onPlayerTouch(GamePanel gp, Player player, int objectIndex) {
        if (age < 15 || !player.alive) {
            return;
        }
        gp.wallet.collect(value);
        gp.effects.text(x, y - 46, "+" + value, GLOW, value >= BIG_VALUE ? 2 : 1);
        gp.effects.sparks(x, y - 20, 0, -1, value >= BIG_VALUE ? 8 : 4, EffectsManager.GOLD);
        gp.playSoundEffect(Sound.COIN);
        gp.entities.removeObject(this);
    }

    @Override
    public void draw(Graphics g, GamePanel gp) {
        Graphics2D g2 = (Graphics2D) g;
        int sx = (int) Math.round(x - gp.player.worldX + gp.player.screenX);
        int sy = (int) Math.round(y - gp.player.worldY + gp.player.screenY);
        if (sx < -60 || sy < -80 || sx > gp.screenWidth + 60 || sy > gp.screenHeight + 60) {
            return;
        }
        if (age > LIFETIME - BLINK_FROM && (age / 6) % 2 == 0) {
            return;
        }
        boolean big = value >= BIG_VALUE;
        int s = UiSkin.P;
        double bob = height > 0 ? height : Math.sin(age * 0.1 + value) * 2 + 2;
        int iy = sy - (int) Math.round(bob) - (big ? 7 : 5) * s;

        Composite old = g2.getComposite();
        g2.setComposite(AlphaComposite.SrcOver.derive(0.3f));
        g2.setColor(Color.BLACK);
        g2.fillOval(sx - 4 * s, sy - s, 8 * s, 3 * s);
        if (big) {
            g2.setComposite(AlphaComposite.SrcOver.derive(0.2f + 0.08f * (float) Math.sin(age * 0.15)));
            g2.setColor(GLOW);
            g2.fillOval(sx - 9 * s, iy - 8 * s, 18 * s, 16 * s);
        }
        g2.setComposite(old);
        UiSkin.icon(g2, UiSkin.Icon.MUSHROOM, sx, iy, big ? 3 : 2, true);
    }
}
