package entity.weapon;

import entity.Direction8;
import entity.MovementImpulse;

import java.awt.Shape;

public class Sword implements MeleeWeapon {
    private static final int WINDUP_FRAMES = 3;
    private static final int STRIKE_FRAMES = 7;
    private static final int RECOVER_FRAMES = 6;
    private static final int DAMAGE = 5;
    private static final int INPUT_BUFFER_FRAMES = 7;
    private static final int COOLDOWN_FRAMES = 4;
    private static final int REACH = 52;
    private static final int SWING_WIDTH = 56;
    private static final int OVERLAP = 14;
    private static final double IMPULSE = 4.65;
    private static final double IMPULSE_MAX_SPEED = 7.15;
    private static final double STRIKE_MAX_SPEED = 5.45;
    private static final double CONTROL_ACCEL_MULTIPLIER = 0.62;
    private static final double WINDUP_DAMPING = 0.94;
    private static final double STRIKE_DAMPING = 0.88;
    private static final double RECOVERY_DAMPING = 0.76;
    private static final double ENEMY_KNOCKBACK = 8.0;
    private static final int HIT_STOP_FRAMES = 2;
    private final PlayerCombatAnimations animations = PlayerCombatAnimations.loadDefaultMelee();

    @Override
    public String displayName() {
        return "Short Sword";
    }

    @Override
    public PlayerCombatAnimations animations() {
        return animations;
    }

    public int inputBufferFrames() {
        return INPUT_BUFFER_FRAMES;
    }

    public int cooldownFrames() {
        return COOLDOWN_FRAMES;
    }

    public int damage() {
        return DAMAGE;
    }

    public double enemyKnockback() {
        return ENEMY_KNOCKBACK;
    }

    public int hitStopFrames() {
        return HIT_STOP_FRAMES;
    }

    public double controlAccelMultiplier() {
        return CONTROL_ACCEL_MULTIPLIER;
    }

    public int totalFrames() {
        return WINDUP_FRAMES + STRIKE_FRAMES + RECOVER_FRAMES;
    }

    public boolean isStrikeFrame(int frame) {
        return frame >= WINDUP_FRAMES && frame < WINDUP_FRAMES + STRIKE_FRAMES;
    }

    public boolean useStrikeSprite(int frame) {
        return isStrikeFrame(frame);
    }

    public double maxSpeedForFrame(int frame, double baseMaxSpeed) {
        if (frame <= WINDUP_FRAMES + 1) {
            return IMPULSE_MAX_SPEED;
        }
        if (isStrikeFrame(frame)) {
            return STRIKE_MAX_SPEED;
        }
        return baseMaxSpeed;
    }

    public double dampingForFrame(int frame) {
        if (frame <= WINDUP_FRAMES + 1) {
            return WINDUP_DAMPING;
        }
        if (isStrikeFrame(frame)) {
            return STRIKE_DAMPING;
        }
        return RECOVERY_DAMPING;
    }

    public MovementImpulse impulseFor(Direction8 direction) {
        return new MovementImpulse(
                direction.unitX * IMPULSE,
                direction.unitY * IMPULSE,
                IMPULSE_MAX_SPEED
        );
    }

    public Shape attackArea(Direction8 direction, int frame, int centerX, int centerY) {
        int strikeFrame = Math.max(0, frame - WINDUP_FRAMES);
        int reach = REACH;
        int swingWidth = SWING_WIDTH;

        if (isStrikeFrame(frame)) {
            if (strikeFrame <= 1) {
                reach -= 8;
                swingWidth -= 8;
            } else if (strikeFrame >= STRIKE_FRAMES - 2) {
                reach -= 4;
                swingWidth += 4;
            }
        }

        return MeleeWeapon.swingArea(direction, centerX, centerY, OVERLAP, reach, swingWidth);
    }
}
