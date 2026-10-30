package object;

import entity.Player;
import main.GamePanel;
import main.Sound;
import main.UiSkin;

import java.awt.*;
import java.util.Random;

public class PowerUp extends SuperObject {
    public enum Kind {
        HEART("Max health +1", new Color(0xF08C80)),
        SPEED("Speed up", new Color(0x8FD0E0)),
        DAMAGE("Damage up", new Color(0xEBBC5F));

        final String label;
        final Color glow;

        Kind(String label, Color glow) {
            this.label = label;
            this.glow = glow;
        }
    }

    private static final double SPEED_BONUS = 0.6;
    private static final double DAMAGE_BONUS = 0.25;
    private static final Random RANDOM = new Random();

    private final Kind kind;
    private final double x;
    private final double y;
    private int age = 0;

    public PowerUp(Kind kind, double centerX, double groundY) {
        this.kind = kind;
        name = kind.label;
        x = centerX;
        y = groundY;
        worldX = (int) Math.round(x) - 24;
        worldY = (int) Math.round(y) - 40;
        solidArea = new Rectangle(8, 8, 32, 32);
        solidAreaDefaultX = solidArea.x;
        solidAreaDefaultY = solidArea.y;
    }

    public static PowerUp random(double centerX, double groundY) {
        Kind[] kinds = Kind.values();
        return new PowerUp(kinds[RANDOM.nextInt(kinds.length)], centerX, groundY);
    }

    public Kind kind() {
        return kind;
    }

    @Override public java.util.Properties snapshot() {
        var p = new java.util.Properties();
        p.setProperty("x", "" + x);
        p.setProperty("y", "" + y);
        p.setProperty("age", "" + age);
        p.setProperty("kind", kind.name());
        return p;
    }
    @Override public void restoreSnapshot(java.util.Properties p) {
        age = Math.max(0, save.Values.integer(p,"age",0));
    }

    @Override
    public void update(GamePanel gp) {
        age++;
        if (age % 24 == 0) {
            gp.effects.sparks(x + (RANDOM.nextDouble() - 0.5) * 24, y - 34, 0, -1, 1,
                    new Color[]{kind.glow.brighter(), kind.glow, kind.glow.darker()});
        }
    }

    @Override
    public void addLights(world.Lighting lighting) {
        lighting.add(x, y - 24, 80, kind.glow, 0.8f, 0.08f);
    }

    @Override
    public void onPlayerTouch(GamePanel gp, Player player, int objectIndex) {
        if (age < 20 || !player.alive) {
            return;
        }
        switch (kind) {
            case HEART: player.addMaxHealth(1); break;
            case SPEED: player.addMaxSpeed(SPEED_BONUS); break;
            default: player.addRunDamage(DAMAGE_BONUS);
        }
        gp.ui.showMessage(kind.label);
        gp.effects.ring(x, y - 20, 56, 16, kind.glow, false);
        gp.effects.burst(x, y - 20, 18, 3, new Color[]{Color.WHITE, kind.glow, kind.glow.darker()});
        gp.playSoundEffect(Sound.POWERUP);
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
        int s = UiSkin.P;
        int cy = sy - 9 * s - (int) Math.round(Math.sin(age * 0.08) * 4);

        Composite old = g2.getComposite();
        g2.setComposite(AlphaComposite.SrcOver.derive(0.3f));
        g2.setColor(Color.BLACK);
        g2.fillOval(sx - 5 * s, sy - s, 10 * s, 3 * s);
        g2.setComposite(AlphaComposite.SrcOver.derive(0.22f + 0.1f * (float) Math.sin(age * 0.12)));
        g2.setColor(kind.glow);
        g2.fillOval(sx - 11 * s, cy - 10 * s, 22 * s, 20 * s);
        g2.setComposite(AlphaComposite.SrcOver.derive(0.35f));
        g2.fillOval(sx - 7 * s, cy - 6 * s, 14 * s, 12 * s);
        g2.setComposite(old);

        switch (kind) {
            case HEART:
                UiSkin.heart(g2, (sx - UiSkin.HEART_W / 2) / s * s, (cy - UiSkin.HEART_H / 2) / s * s, true);
                break;
            case SPEED:
                UiSkin.icon(g2, UiSkin.Icon.DASH, sx, cy, 2, true);
                break;
            default:
                UiSkin.icon(g2, UiSkin.Icon.SWORD, sx, cy, 2, true);
        }
    }
}
