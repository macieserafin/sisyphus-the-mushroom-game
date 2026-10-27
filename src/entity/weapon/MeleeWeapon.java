package entity.weapon;

import entity.Direction8;
import entity.MovementImpulse;
import entity.anim.WeaponSprite;

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

public interface MeleeWeapon {
    String displayName();
    PlayerCombatAnimations animations();
    WeaponSprite sprite();
    double swingArcRadians();
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
    MovementImpulse impulseFor(Direction8 direction);
    Shape attackArea(Direction8 direction, int frame, int centerX, int centerY);

    static Shape swingArea(Direction8 direction, int centerX, int centerY, int behind, int reach, int width) {
        Rectangle2D eastward = new Rectangle2D.Double(-behind, -width / 2.0, reach + behind, width);
        AffineTransform toWorld = new AffineTransform();
        toWorld.translate(centerX, centerY);
        toWorld.rotate(direction.angle());
        return toWorld.createTransformedShape(eastward);
    }
}
