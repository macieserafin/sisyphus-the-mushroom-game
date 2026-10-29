package main;

public class DebugSettings {
    private static final double[] DAMAGE_MULTIPLIERS = {0.25, 0.5, 1, 2, 5, 10, 25, 100};
    private boolean overlayEnabled = false;
    private boolean godMode = false;
    private boolean enemiesFrozen = false;
    private boolean bytExecutionDisabled = false;
    private int damageIndex = 2;

    public boolean isGodMode() { return godMode; }
    public void toggleGodMode() { godMode = !godMode; }
    public boolean areEnemiesFrozen() { return enemiesFrozen; }
    public void toggleEnemiesFrozen() { enemiesFrozen = !enemiesFrozen; }
    public boolean isBytExecutionEnabled() { return !godMode && !bytExecutionDisabled; }
    public boolean isBytExecutionDisabled() { return bytExecutionDisabled; }
    public void toggleBytExecution() { bytExecutionDisabled = !bytExecutionDisabled; }
    public double playerDamageMultiplier() { return DAMAGE_MULTIPLIERS[damageIndex]; }
    public void cycleDamageMultiplier(int direction) {
        damageIndex = Math.floorMod(damageIndex + direction, DAMAGE_MULTIPLIERS.length);
    }
    public int scalePlayerDamage(int damage) {
        return damage <= 0 ? 0 : Math.max(1, (int) Math.min(Integer.MAX_VALUE,
                Math.round(damage * playerDamageMultiplier())));
    }
    public void resetCheats() {
        godMode = false;
        enemiesFrozen = false;
        bytExecutionDisabled = false;
        damageIndex = 2;
    }

    public boolean isOverlayEnabled() {
        return overlayEnabled;
    }

    public void toggleOverlay() {
        overlayEnabled = !overlayEnabled;
        System.out.println("Debug overlay: " + (overlayEnabled ? "ON" : "OFF"));
    }
}
