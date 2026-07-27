package entity.weapon;

import entity.MovementImpulse;

import java.awt.Rectangle;

public interface MeleeWeapon {
    String displayName();
    PlayerCombatAnimations animations();
    int inputBufferFrames();
    int cooldownFrames();
    int damage();
    double enemyKnockback();
    int hitStopFrames();
    double controlAccelMultiplier();
    int totalFrames();
    boolean isStrikeFrame(int frame);
    boolean useStrikeSprite(int frame);
    double maxSpeedForFrame(int frame, double baseMaxSpeed);
    double dampingForFrame(int frame);
    MovementImpulse impulseFor(String direction);
    Rectangle attackBox(String direction, int frame, int centerX, int centerY);
}
