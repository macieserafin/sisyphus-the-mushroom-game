package entity.weapon;

import entity.MovementImpulse;

import java.awt.Rectangle;

public class HeavySword implements MeleeWeapon {
    private static final int WINDUP_FRAMES = 6;
    private static final int STRIKE_FRAMES = 10;
    private static final int RECOVER_FRAMES = 12;
    private static final int DAMAGE = 9;
    private static final int INPUT_BUFFER_FRAMES = 7;
    private static final int COOLDOWN_FRAMES = 16;
    private static final int REACH = 58;
    private static final int SWING_WIDTH = 62;
    private static final int OVERLAP = 14;
    private static final double IMPULSE = 3.4;
    private static final double IMPULSE_MAX_SPEED = 5.8;
    private static final double STRIKE_MAX_SPEED = 4.2;
    private static final double CONTROL_ACCEL_MULTIPLIER = 0.45;
    private static final double WINDUP_DAMPING = 0.90;
    private static final double STRIKE_DAMPING = 0.82;
    private static final double RECOVERY_DAMPING = 0.70;
    private static final double ENEMY_KNOCKBACK = 11.0;
    private static final int HIT_STOP_FRAMES = 3;
    private final PlayerCombatAnimations animations = PlayerCombatAnimations.loadDefaultMelee();

    @Override
    public String displayName() {
        return "Heavy Sword";
    }

    @Override
    public PlayerCombatAnimations animations() {
        return animations;
    }

    @Override
    public int inputBufferFrames() {
        return INPUT_BUFFER_FRAMES;
    }

    @Override
    public int cooldownFrames() {
        return COOLDOWN_FRAMES;
    }

    @Override
    public int damage() {
        return DAMAGE;
    }

    @Override
    public double enemyKnockback() {
        return ENEMY_KNOCKBACK;
    }

    @Override
    public int hitStopFrames() {
        return HIT_STOP_FRAMES;
    }

    @Override
    public double controlAccelMultiplier() {
        return CONTROL_ACCEL_MULTIPLIER;
    }

    @Override
    public int totalFrames() {
        return WINDUP_FRAMES + STRIKE_FRAMES + RECOVER_FRAMES;
    }

    @Override
    public boolean isStrikeFrame(int frame) {
        return frame >= WINDUP_FRAMES && frame < WINDUP_FRAMES + STRIKE_FRAMES;
    }

    @Override
    public boolean useStrikeSprite(int frame) {
        return isStrikeFrame(frame);
    }

    @Override
    public double maxSpeedForFrame(int frame, double baseMaxSpeed) {
        if (frame <= WINDUP_FRAMES + 1) {
            return IMPULSE_MAX_SPEED;
        }
        if (isStrikeFrame(frame)) {
            return STRIKE_MAX_SPEED;
        }
        return baseMaxSpeed;
    }

    @Override
    public double dampingForFrame(int frame) {
        if (frame <= WINDUP_FRAMES + 1) {
            return WINDUP_DAMPING;
        }
        if (isStrikeFrame(frame)) {
            return STRIKE_DAMPING;
        }
        return RECOVERY_DAMPING;
    }

    @Override
    public MovementImpulse impulseFor(String direction) {
        double[] vector = directionVector(direction);
        return new MovementImpulse(vector[0] * IMPULSE, vector[1] * IMPULSE, IMPULSE_MAX_SPEED);
    }

    @Override
    public Rectangle attackBox(String direction, int frame, int centerX, int centerY) {
        switch (direction) {
            case "up":
                return new Rectangle(centerX - SWING_WIDTH / 2, centerY - REACH, SWING_WIDTH, REACH + OVERLAP);
            case "down":
                return new Rectangle(centerX - SWING_WIDTH / 2, centerY - OVERLAP, SWING_WIDTH, REACH + OVERLAP);
            case "left":
                return new Rectangle(centerX - REACH, centerY - SWING_WIDTH / 2, REACH + OVERLAP, SWING_WIDTH);
            case "right":
            default:
                return new Rectangle(centerX - OVERLAP, centerY - SWING_WIDTH / 2, REACH + OVERLAP, SWING_WIDTH);
        }
    }

    private double[] directionVector(String direction) {
        switch (direction) {
            case "up": return new double[]{0, -1};
            case "down": return new double[]{0, 1};
            case "left": return new double[]{-1, 0};
            case "right": return new double[]{1, 0};
            default: return new double[]{0, 0};
        }
    }
}
