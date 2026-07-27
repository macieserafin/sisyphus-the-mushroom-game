package entity.weapon;

public class RangedWeaponUpgrade {
    private final double projectileSpeedIncrease;
    private final int cooldownReductionFrames;

    public RangedWeaponUpgrade(double projectileSpeedIncrease, int cooldownReductionFrames) {
        if (projectileSpeedIncrease <= 0) {
            throw new IllegalArgumentException("Projectile speed increase must be positive: " + projectileSpeedIncrease);
        }
        if (cooldownReductionFrames <= 0) {
            throw new IllegalArgumentException("Cooldown reduction must be positive: " + cooldownReductionFrames);
        }
        this.projectileSpeedIncrease = projectileSpeedIncrease;
        this.cooldownReductionFrames = cooldownReductionFrames;
    }

    public void applyTo(RangedWeapon weapon) {
        weapon.increaseProjectileSpeed(projectileSpeedIncrease);
        weapon.reduceCooldown(cooldownReductionFrames);
    }
}
