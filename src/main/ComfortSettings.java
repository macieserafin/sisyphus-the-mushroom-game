package main;

import save.Settings;

public class ComfortSettings {
    public static final int[] SHAKE_LEVELS = {100, 50, 0};

    private static final String SHAKE_KEY = "shake";
    private static final String FLASHES_KEY = "flashes";
    private static final float REDUCED_FLASH = 0.3f;

    private final Settings settings;
    private int shakePercent;
    private boolean fullFlashes;

    public ComfortSettings(Settings settings) {
        this.settings = settings;
        shakePercent = nearestLevel(settings.getInt(SHAKE_KEY, 100));
        fullFlashes = settings.getBoolean(FLASHES_KEY, true);
    }

    public int shakePercent() {
        return shakePercent;
    }

    public double shakeScale() {
        return shakePercent / 100.0;
    }

    public boolean fullFlashes() {
        return fullFlashes;
    }

    public float flashScale() {
        return fullFlashes ? 1f : REDUCED_FLASH;
    }

    // direction +1 goes towards less shake, -1 towards more; wraps around.
    public void cycleShake(int direction) {
        int index = 0;
        for (int i = 0; i < SHAKE_LEVELS.length; i++) {
            if (SHAKE_LEVELS[i] == shakePercent) {
                index = i;
            }
        }
        shakePercent = SHAKE_LEVELS[Math.floorMod(index + direction, SHAKE_LEVELS.length)];
        settings.putInt(SHAKE_KEY, shakePercent);
    }

    public void toggleFlashes() {
        fullFlashes = !fullFlashes;
        settings.putBoolean(FLASHES_KEY, fullFlashes);
    }

    public void reset() {
        shakePercent = 100;
        fullFlashes = true;
        settings.putInt(SHAKE_KEY, shakePercent);
        settings.putBoolean(FLASHES_KEY, fullFlashes);
    }

    private static int nearestLevel(int value) {
        int best = SHAKE_LEVELS[0];
        for (int level : SHAKE_LEVELS) {
            if (Math.abs(level - value) < Math.abs(best - value)) {
                best = level;
            }
        }
        return best;
    }
}
