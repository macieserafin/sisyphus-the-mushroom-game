package entity.weapon;

import entity.Direction8;
import entity.Entity;
import entity.MovementImpulse;
import entity.anim.WeaponSprite;
import world.BulletManager;

public interface RangedWeapon {
    String displayName();
    PlayerCombatAnimations animations();
    WeaponSprite sprite();
    double movementMultiplier();
    double recoilMaxSpeed();
    int shotCooldownFrames();
    MovementImpulse fire(int tileSize, BulletManager bulletManager, Runnable shotSound, Entity owner, int shooterWorldX, int shooterWorldY, Direction8 direction);
    void increaseProjectileSpeed(double amount);
    void reduceCooldown(int frames);
}
