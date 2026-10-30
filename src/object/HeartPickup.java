package object;

import entity.Player;
import main.GamePanel;
import main.Sound;
import main.UiSkin;
import world.EffectsManager;

import java.awt.*;
import java.util.Random;

public class HeartPickup extends SuperObject {
    private static final int LIFETIME = 900;
    private static final int BLINK_FROM = 180;
    private static final double MAGNET_RANGE = 96;
    private static final Color[] PINK = {new Color(0xFFD6D0), new Color(0xF08C80), new Color(0xC8282E)};

    private final Random random = new Random();
    private double x;
    private double y;
    private double vx;
    private double height = 0;
    private double vHeight;
    private int age = 0;

    public HeartPickup(double centerX, double groundY) {
        name = "Heart";
        x = centerX;
        y = groundY;
        vx = (random.nextDouble() - 0.5) * 3;
        vHeight = 5.5;
        syncPosition();
        solidArea = new Rectangle(8, 8, 32, 32);
        solidAreaDefaultX = solidArea.x;
        solidAreaDefaultY = solidArea.y;
    }

    private void syncPosition() {
        worldX = (int) Math.round(x) - 24;
        worldY = (int) Math.round(y) - 40;
    }

    @Override public java.util.Properties snapshot() {
        var p = new java.util.Properties();
        p.setProperty("x", "" + x);
        p.setProperty("y", "" + y);
        p.setProperty("vx", "" + vx);
        p.setProperty("height", "" + height);
        p.setProperty("vHeight", "" + vHeight);
        p.setProperty("age", "" + age);
        return p;
    }
    @Override public void restoreSnapshot(java.util.Properties p) {
        x = save.Values.decimal(p,"x",x);
        y = save.Values.decimal(p,"y",y);
        vx = save.Values.decimal(p,"vx",vx);
        height = save.Values.decimal(p,"height",height);
        vHeight = save.Values.decimal(p,"vHeight",vHeight);
        age = Math.max(0, save.Values.integer(p,"age",0));
        syncPosition();
    }

    @Override
    public void update(GamePanel gp) {
        age++;
        if (age > LIFETIME) {
            gp.effects.burst(x, y - 20, 6, 1.2, PINK);
            gp.entities.removeObject(this);
            return;
        }
        if (height > 0 || vHeight > 0) {
            height = Math.max(0, height + vHeight);
            vHeight -= 0.45;
            x += vx;
            vx *= 0.93;
            if (height == 0 && vHeight < 0) {
                vHeight = 0;
                gp.effects.dust(x, y, 0, 0, 3);
            }
        }
        Player p = gp.player;
        double px = p.worldX + gp.tileSize / 2.0, py = p.worldY + gp.tileSize * 0.8;
        double d = Math.hypot(px - x, py - y);
        if (p.health < p.maxHealth && d < MAGNET_RANGE && d > 1) {
            double pull = 1.5 + (MAGNET_RANGE - d) * 0.06;
            x += (px - x) / d * pull;
            y += (py - y) / d * pull;
        }
        if (age % 20 == 0) {
            gp.effects.sparks(x + (random.nextDouble() - 0.5) * 20, y - 30, 0, -1, 1, PINK);
        }
        syncPosition();
    }

    @Override
    public void onPlayerTouch(GamePanel gp, Player player, int objectIndex) {
        if (age < 15 || !player.heal(1)) {
            return;
        }
        gp.effects.text(x, y - 50, "+1", new Color(0xF08C80), 2);
        gp.effects.burst(x, y - 20, 14, 2.6, PINK);
        gp.effects.ring(x, y - 20, 40, 12, new Color(0xF08C80), false);
        gp.playSoundEffect(Sound.HEART);
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
        int s = UiSkin.P;
        double bob = height > 0 ? height : Math.sin(age * 0.09) * 3 + 3;
        Composite old = g2.getComposite();
        g2.setComposite(AlphaComposite.SrcOver.derive(0.3f));
        g2.setColor(Color.BLACK);
        g2.fillOval(sx - 5 * s, sy - s, 10 * s, 3 * s);
        float glow = 0.18f + 0.1f * (float) Math.sin(age * 0.15);
        g2.setComposite(AlphaComposite.SrcOver.derive(glow));
        g2.setColor(new Color(0xF08C80));
        int gy = sy - (int) Math.round(bob) - 7 * s;
        g2.fillOval(sx - 9 * s, gy - 8 * s, 18 * s, 16 * s);
        g2.setComposite(old);
        int hx = (sx - UiSkin.HEART_W / 2) / s * s;
        int hy = (gy - UiSkin.HEART_H / 2) / s * s;
        UiSkin.heart(g2, hx, hy, true);
    }
}
