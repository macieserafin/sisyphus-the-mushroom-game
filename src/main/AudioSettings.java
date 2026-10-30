package main;

public class AudioSettings {
    public static final int STEP = 10;

    private static final String MUSIC_KEY = "musicVolume";
    private static final String EFFECTS_KEY = "effectsVolume";
    private static final String MUTED_KEY = "muted";

    private final GamePanel gp;

    private int musicPercent;
    private int effectsPercent;
    private boolean muted;

    public AudioSettings(GamePanel gp) {
        this.gp = gp;
        musicPercent = clampPercent(gp.settings.getInt(MUSIC_KEY, 100));
        effectsPercent = clampPercent(gp.settings.getInt(EFFECTS_KEY, 100));
        muted = gp.settings.getBoolean(MUTED_KEY, false);
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

    public void reset() {
        musicPercent = 100;
        effectsPercent = 100;
        muted = false;
        save();
    }

    private void save() {
        gp.settings.putInt(MUSIC_KEY, musicPercent);
        gp.settings.putInt(EFFECTS_KEY, effectsPercent);
        gp.settings.putBoolean(MUTED_KEY, muted);
        gp.applyAudioSettings();
    }

    private static int clampPercent(int value) {
        return Math.max(0, Math.min(100, value));
    }
}
