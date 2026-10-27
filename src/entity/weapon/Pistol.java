package entity.weapon;

import entity.Bullet;
import entity.Entity;
import entity.MovementImpulse;
import entity.combat.Faction;
import world.BulletManager;

public class Pistol implements RangedWeapon {
    private static final double MOVEMENT_MULTIPLIER = 0.9;
    private static final double RECOIL = 0.42;
    private static final double RECOIL_MAX_SPEED = 5.0;
    private static final int DEFAULT_SHOT_COOLDOWN_FRAMES = 24;
    private static final double DEFAULT_BULLET_SPEED = 10;
    private static final int BULLET_RANGE_TILES = 5;
    private static final int BULLET_DAMAGE = 3;
    private static final double BULLET_SPAWN_OFFSET = 25.0;
    private int shotCooldownFrames = DEFAULT_SHOT_COOLDOWN_FRAMES;
    private double bulletSpeed = DEFAULT_BULLET_SPEED;
    private final PlayerCombatAnimations animations = PlayerCombatAnimations.loadDefaultRanged();

    @Override
    public String displayName() {
        return "Pistol";
    }

    @Override
    public PlayerCombatAnimations animations() {
        return animations;
    }

    public double movementMultiplier() {
        return MOVEMENT_MULTIPLIER;
    }

    public double recoilMaxSpeed() {
        return RECOIL_MAX_SPEED;
    }

    public int shotCooldownFrames() {
        return shotCooldownFrames;
    }

    @Override
    public MovementImpulse fire(
            int tileSize,
            BulletManager bulletManager,
            Runnable shotSound,
            Entity owner,
            int shooterWorldX,
            int shooterWorldY,
            String direction
    ) {
        double[] shotVector = directionVector(direction);
        double bulletHalfSize = Bullet.DEFAULT_SIZE / 2.0;
        double startX = shooterWorldX + (tileSize / 2.0) - bulletHalfSize + shotVector[0] * BULLET_SPAWN_OFFSET;
        double startY = shooterWorldY + (tileSize / 2.0) - bulletHalfSize + shotVector[1] * BULLET_SPAWN_OFFSET;

        Bullet bullet = new Bullet.Builder(tileSize, owner.gp())
                .startPosition(startX, startY)
                .velocity(shotVector[0] * bulletSpeed, shotVector[1] * bulletSpeed)
                .rangeTiles(BULLET_RANGE_TILES)
                .damage(BULLET_DAMAGE)
                .owner(owner)
                .faction(Faction.PLAYER)
                .build();

        bulletManager.addBullet(bullet);
        shotSound.run();

        return new MovementImpulse(
                -shotVector[0] * RECOIL,
                -shotVector[1] * RECOIL,
                RECOIL_MAX_SPEED
        );
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

    private double[] directionVector(String direction) {
        switch (direction) {
            case "up":
                return new double[]{0, -1};
            case "down":
                return new double[]{0, 1};
            case "left":
                return new double[]{-1, 0};
            case "right":
                return new double[]{1, 0};
            default:
                return new double[]{0, 0};
        }
    }
}
