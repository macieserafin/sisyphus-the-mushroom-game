package main;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.IOException;
import java.net.URL;

public class Sound {
    public static final int BACKGROUND = 0;
    public static final int GUNSHOT = 1;
    public static final int UNLOCK = 2;
    public static final int COIN = 3;
    public static final int POWERUP = 4;
    public static final int SWORD = 5;
    public static final int PLAYER_HAPPY = 6;
    public static final int PLAYER_HURT_1 = 7;
    public static final int PLAYER_HURT_2 = 8;
    public static final int POWERUP_BOOTS = 9;
    public static final int POWERUP_KEY = 10;
    public static final int SLIME_DEATH = 11;
    public static final int PLAYER_DEATH = 12;
    public static final int CUTSCENE_MUSIC = 13;
    public static final int BOSS_MUSIC = 14;

    private static final int EFFECT_VOICES = 12;

    Clip clip;
    URL[] soundURL = new URL[30];
    private final Clip[][] effectClips = new Clip[30][EFFECT_VOICES];
    private final int[] nextEffectVoice = new int[30];
    private final boolean[] effectLoadFailed = new boolean[30];
    private float volume = 1f;

    public Sound() {
        soundURL[BACKGROUND] = getClass().getResource("/sounds/background.wav");
        soundURL[GUNSHOT] = getClass().getResource("/sounds/gunshot.wav");
        soundURL[UNLOCK] = getClass().getResource("/sounds/unlock.wav");
        soundURL[COIN] = getClass().getResource("/sounds/coin.wav");
        soundURL[POWERUP] = getClass().getResource("/sounds/powerup.wav");
        soundURL[SWORD] = getClass().getResource("/sounds/sword.wav");
        soundURL[PLAYER_HAPPY] = getClass().getResource("/sounds/playerhappy.wav");
        soundURL[PLAYER_HURT_1] = getClass().getResource("/sounds/playerhurt1.wav");
        soundURL[PLAYER_HURT_2] = getClass().getResource("/sounds/playerhurt2.wav");
        soundURL[POWERUP_BOOTS] = getClass().getResource("/sounds/powerup2.wav");
        soundURL[POWERUP_KEY] = getClass().getResource("/sounds/powerup1.wav");
        soundURL[SLIME_DEATH] = getClass().getResource("/sounds/slimedeath.wav");
        soundURL[PLAYER_DEATH] = getClass().getResource("/sounds/playerdeath.wav");
        soundURL[CUTSCENE_MUSIC] = getClass().getResource("/sounds/cutscene.wav");
        soundURL[BOSS_MUSIC] = getClass().getResource("/sounds/babycore.wav");

    }

    public boolean hasResource(int i) {
        return i >= 0 && i < soundURL.length && soundURL[i] != null;
    }

    public synchronized void setFile(int i) {
        closeClip();

        if (i < 0 || i >= soundURL.length) {
            System.err.println("Audio resource index out of range: " + i);
            return;
        }

        URL resource = soundURL[i];
        if (resource == null) {
            System.err.println("Missing audio resource at index " + i);
            return;
        }

        try (AudioInputStream ais = AudioSystem.getAudioInputStream(resource)) {
            clip = AudioSystem.getClip();
            clip.open(ais);
            applyVolume(clip);
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            System.err.println("Could not load audio resource at index " + i + " from " + resource + ": " + e.getMessage());
            closeClip();
        }
    }

    public synchronized void play() {
        if (clip == null) {
            System.err.println("Cannot play audio: no clip loaded.");
            return;
        }
        clip.setFramePosition(0);
        clip.start();

    }

    public synchronized void loop() {
        if (clip == null) {
            System.err.println("Cannot loop audio: no clip loaded.");
            return;
        }
        clip.loop(Clip.LOOP_CONTINUOUSLY);

    }
    public synchronized void stop() {
        if (clip == null) {
            return;
        }
        clip.stop();

    }

    public void playEffect(int i) {
        Clip effect = getEffectClipForPlayback(i);
        if (effect == null) {
            return;
        }

        effect.stop();
        effect.setFramePosition(0);
        effect.start();
    }

    public synchronized void setVolume(float volume) {
        this.volume = Math.max(0f, Math.min(1f, volume));
        applyVolume(clip);
        for (Clip[] voices : effectClips) {
            for (Clip effect : voices) {
                applyVolume(effect);
            }
        }
    }

    private void applyVolume(Clip target) {
        if (target == null || !target.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            return;
        }
        FloatControl gain = (FloatControl) target.getControl(FloatControl.Type.MASTER_GAIN);
        float decibels = volume <= 0.001f ? gain.getMinimum() : (float) (20.0 * Math.log10(volume));
        gain.setValue(Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), decibels)));
    }

    public void stopAllEffects() {
        for (int soundIndex = 0; soundIndex < effectClips.length; soundIndex++) {
            for (int voice = 0; voice < effectClips[soundIndex].length; voice++) {
                Clip effect = effectClips[soundIndex][voice];
                if (effect != null) {
                    effect.stop();
                    effect.setFramePosition(0);
                }
            }
        }
    }

    private Clip getEffectClipForPlayback(int i) {
        if (i < 0 || i >= soundURL.length) {
            System.err.println("Audio effect index out of range: " + i);
            return null;
        }
        if (effectLoadFailed[i]) {
            return null;
        }

        for (int offset = 0; offset < EFFECT_VOICES; offset++) {
            int voice = (nextEffectVoice[i] + offset) % EFFECT_VOICES;
            Clip effect = getEffectClip(i, voice);
            if (effect != null && !effect.isRunning()) {
                nextEffectVoice[i] = (voice + 1) % EFFECT_VOICES;
                return effect;
            }
        }

        return null;
    }

    private Clip getEffectClip(int i, int voice) {
        if (effectClips[i][voice] != null) {
            return effectClips[i][voice];
        }

        URL resource = soundURL[i];
        if (resource == null) {
            System.err.println("Missing audio effect at index " + i);
            effectLoadFailed[i] = true;
            return null;
        }

        try (AudioInputStream ais = AudioSystem.getAudioInputStream(resource)) {
            Clip effect = AudioSystem.getClip();
            effect.open(ais);
            applyVolume(effect);
            effectClips[i][voice] = effect;
            return effect;
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            System.err.println("Could not load audio effect at index " + i + ", voice " + voice + " from " + resource + ": " + e.getMessage());
            effectLoadFailed[i] = true;
            return null;
        }
    }

    private synchronized void closeClip() {
        if (clip != null) {
            clip.stop();
            clip.close();
            clip = null;
        }
    }
}
