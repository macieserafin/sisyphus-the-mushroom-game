package entity.combat;

import entity.Bullet;
import entity.Entity;

public final class Hit {
    private final int damage;
    private final HitType type;
    private final Entity source;
    private final int sourceWorldX;
    private final int sourceWorldY;
    private final double knockbackForce;
    private final Bullet projectile;

    public Hit(
            int damage,
            HitType type,
            Entity source,
            int sourceWorldX,
            int sourceWorldY,
            double knockbackForce,
            Bullet projectile
    ) {
        if (damage < 0) {
            throw new IllegalArgumentException("Hit damage cannot be negative: " + damage);
        }
        if (type == null) {
            throw new IllegalArgumentException("Hit type is required");
        }
        this.damage = damage;
        this.type = type;
        this.source = source;
        this.sourceWorldX = sourceWorldX;
        this.sourceWorldY = sourceWorldY;
        this.knockbackForce = knockbackForce;
        this.projectile = projectile;
    }

    public int damage() {
        return damage;
    }

    public HitType type() {
        return type;
    }

    public Entity source() {
        return source;
    }

    public int sourceWorldX() {
        return sourceWorldX;
    }

    public int sourceWorldY() {
        return sourceWorldY;
    }

    public double knockbackForce() {
        return knockbackForce;
    }

    public Bullet projectile() {
        return projectile;
    }
}
