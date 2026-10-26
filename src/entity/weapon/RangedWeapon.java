package entity.weapon;

import entity.Entity;
import entity.MovementImpulse;
import world.BulletManager;

public interface RangedWeapon {
    String displayName();
    PlayerCombatAnimations animations();
    double movementMultiplier();
    double recoilMaxSpeed();
    int shotCooldownFrames();
    MovementImpulse fire(int tileSize, BulletManager bulletManager, Runnable shotSound, Entity owner, int shooterWorldX, int shooterWorldY, String direction);
    void increaseProjectileSpeed(double amount);
    void reduceCooldown(int frames);
}
