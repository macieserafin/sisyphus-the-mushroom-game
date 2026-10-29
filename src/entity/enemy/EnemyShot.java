package entity.enemy;

import java.awt.Color;
import java.awt.image.BufferedImage;

// One boss projectile or hazard. Kinds:
//  SMALL - fast goo ball, the sword cuts it
//  HEAVY - big slow drop, cannot be cut: dodge it or roll through it; may burst into small balls
//  ZONE  - a marked spot on the ground that erupts after its telegraph (rain, slams)
//  WAVE  - a ring that expands from a point (shockwaves); only a roll gets through it
public final class EnemyShot {
    public enum Kind { SMALL, HEAVY, ZONE, WAVE }

    public final Kind kind;
    public final Enemy owner;
    public double x;
    public double y;
    public double vx;
    public double vy;
    public double radius;
    public int damage = 1;
    public double knockback = 6;

    public int age = 0;
    public int maxAge = 600;
    // ticks before the shot becomes dangerous (it is drawn as a warning until then)
    public int delay = 0;
    public double accel = 1.0;
    public double curve = 0;
    public int burstInto = 0;
    // ZONE: ticks it stays dangerous after erupting; WAVE: growth per tick and ring thickness
    public int activeTicks = 10;
    public double growth = 0;
    public double thickness = 10;

    public Color body = new Color(0x24AE21);
    public Color core = new Color(0xCDEFB9);
    public Color edge = new Color(0x08270A);

    // Optional animated art; collision and sword cutting still follow the shot kind.
    public BufferedImage[] spriteFrames;
    public int spriteFrameTicks = 5;
    // Square display size in world pixels; zero keeps the sprite's native art scale.
    public int spriteWorldSize;
    public Color[] impactColors;

    public boolean alive = true;
    public boolean hitPlayer = false;

    public EnemyShot(Kind kind, Enemy owner, double x, double y) {
        this.kind = kind;
        this.owner = owner;
        this.x = x;
        this.y = y;
    }

    public static EnemyShot small(Enemy owner, double x, double y, double angle, double speed) {
        EnemyShot s = new EnemyShot(Kind.SMALL, owner, x, y);
        s.vx = Math.cos(angle) * speed;
        s.vy = Math.sin(angle) * speed;
        s.radius = 6;
        return s;
    }

    public static EnemyShot heavy(Enemy owner, double x, double y, double angle, double speed) {
        EnemyShot s = new EnemyShot(Kind.HEAVY, owner, x, y);
        s.vx = Math.cos(angle) * speed;
        s.vy = Math.sin(angle) * speed;
        s.radius = 14;
        s.damage = 1;
        s.knockback = 9;
        s.body = new Color(0x572675);
        s.core = new Color(0xCF9FE9);
        s.edge = new Color(0x190922);
        return s;
    }

    public static EnemyShot zone(Enemy owner, double x, double y, double radius, int telegraphTicks) {
        EnemyShot s = new EnemyShot(Kind.ZONE, owner, x, y);
        s.radius = radius;
        s.delay = telegraphTicks;
        s.activeTicks = 10;
        s.maxAge = telegraphTicks + s.activeTicks;
        s.body = new Color(0xC23228);
        s.core = new Color(0xF8D196);
        return s;
    }

    public static EnemyShot wave(Enemy owner, double x, double y, double growth, double maxRadius) {
        EnemyShot s = new EnemyShot(Kind.WAVE, owner, x, y);
        s.radius = 10;
        s.growth = growth;
        s.maxAge = (int) Math.ceil(maxRadius / growth);
        s.thickness = 12;
        s.knockback = 10;
        s.body = new Color(0x50C73B);
        s.core = new Color(0xECE9C7);
        return s;
    }

    public boolean armed() {
        return age >= delay;
    }

    public double speed() {
        return Math.hypot(vx, vy);
    }
}
