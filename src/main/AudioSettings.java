package main;

import java.util.prefs.Preferences;

public class AudioSettings {
    public static final int STEP = 10;

    private static final String MUSIC_KEY = "musicVolume";
    private static final String EFFECTS_KEY = "effectsVolume";
    private static final String MUTED_KEY = "muted";

    private final GamePanel gp;
    private final Preferences prefs = Preferences.userNodeForPackage(AudioSettings.class);

    private int musicPercent = clampPercent(prefs.getInt(MUSIC_KEY, 100));
    private int effectsPercent = clampPercent(prefs.getInt(EFFECTS_KEY, 100));
    private boolean muted = prefs.getBoolean(MUTED_KEY, false);

    public AudioSettings(GamePanel gp) {
        this.gp = gp;
    }

    public int musicPercent() {
        return musicPercent;
    }

    public int effectsPercent() {
        return effectsPercent;
    }

    public boolean isMuted() {
        return muted;
    }

    public float musicGain() {
        return muted ? 0f : musicPercent / 100f;
    }

    public float effectsGain() {
        return muted ? 0f : effectsPercent / 100f;
    }

    public void changeMusic(int steps) {
        musicPercent = clampPercent(musicPercent + steps * STEP);
        save();
    }

    public void changeEffects(int steps) {
        effectsPercent = clampPercent(effectsPercent + steps * STEP);
        save();
    }

    public void toggleMute() {
        muted = !muted;
        save();
    }

    private void save() {
        try {
            prefs.putInt(MUSIC_KEY, musicPercent);
            prefs.putInt(EFFECTS_KEY, effectsPercent);
            prefs.putBoolean(MUTED_KEY, muted);
            prefs.flush();
        } catch (Exception e) {
            System.err.println("Could not save audio settings: " + e.getMessage());
        }
        gp.applyAudioSettings();
    }

    private static int clampPercent(int value) {
        return Math.max(0, Math.min(100, value));
    }
}
