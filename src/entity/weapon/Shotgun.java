package entity.weapon;

import entity.Bullet;
import entity.Direction8;
import entity.Entity;
import entity.MovementImpulse;
import entity.anim.WeaponSprite;
import entity.combat.Faction;
import world.BulletManager;

public class Shotgun implements RangedWeapon {
    private static final double MOVEMENT_MULTIPLIER = 0.82;
    private static final double RECOIL = 6;
    private static final double RECOIL_MAX_SPEED = 6.2;
    private static final int BULLET_RANGE_TILES = 3;
    private static final int BULLET_DAMAGE = 2;
    private static final double BULLET_SPAWN_OFFSET = 25.0;
    private static final double SPREAD_RADIANS = Math.toRadians(12);
    private int shotCooldownFrames = 28;
    private double bulletSpeed = 9.0;
    private final PlayerCombatAnimations animations = PlayerCombatAnimations.loadDefaultRanged();

    @Override
    public String displayName() {
        return "Shotgun";
    }

    @Override
    public PlayerCombatAnimations animations() {
        return animations;
    }

    @Override
    public WeaponSprite sprite() {
        return WeaponSprite.load("shotgun");
    }

    @Override
    public double movementMultiplier() {
        return MOVEMENT_MULTIPLIER;
    }

    @Override
    public double recoilMaxSpeed() {
        return RECOIL_MAX_SPEED;
    }

    @Override
    public int shotCooldownFrames() {
        return shotCooldownFrames;
    }

    @Override
    public MovementImpulse fire(int tileSize, BulletManager bulletManager, Runnable shotSound, Entity owner, int shooterWorldX, int shooterWorldY, Direction8 direction) {
        double[] baseVector = {direction.unitX, direction.unitY};
        for (int i = -1; i <= 1; i++) {
            double[] vector = rotate(baseVector[0], baseVector[1], i * SPREAD_RADIANS);
            double bulletHalfSize = Bullet.DEFAULT_SIZE / 2.0;
            double startX = shooterWorldX + (tileSize / 2.0) - bulletHalfSize + baseVector[0] * BULLET_SPAWN_OFFSET;
            double startY = shooterWorldY + (tileSize / 2.0) - bulletHalfSize + baseVector[1] * BULLET_SPAWN_OFFSET;

            Bullet bullet = new Bullet.Builder(tileSize, owner.gp())
                    .startPosition(startX, startY)
                    .velocity(vector[0] * bulletSpeed, vector[1] * bulletSpeed)
                    .rangeTiles(BULLET_RANGE_TILES)
                    .damage(BULLET_DAMAGE)
                    .appearance(Bullet.Appearance.SHOTGUN_PELLET)
                    .owner(owner)
                    .faction(Faction.PLAYER)
                    .build();
            bulletManager.addBullet(bullet);
        }

        shotSound.run();
        return new MovementImpulse(-baseVector[0] * RECOIL, -baseVector[1] * RECOIL, RECOIL_MAX_SPEED);
    }

    @Override
    public void increaseProjectileSpeed(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Projectile speed increase must be positive: " + amount);
        }
        bulletSpeed += amount;
    }

    @Override
    public void reduceCooldown(int frames) {
        if (frames <= 0) {
            throw new IllegalArgumentException("Cooldown reduction must be positive: " + frames);
        }
        shotCooldownFrames = Math.max(1, shotCooldownFrames - frames);
    }

    private double[] rotate(double x, double y, double radians) {
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        return new double[]{x * cos - y * sin, x * sin + y * cos};
    }
}
