package audio;

import main.GamePanel;
import main.GameState;

import entity.enemy.Enemy_BYT;
import entity.enemy.Enemy_SlimeKing;
import java.util.Set;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

/** Selects the score from live game state; two voices preserve tails during transitions. */
public final class AudioDirector implements AutoCloseable {
    public static final int SILENCE = -2;
    public static final String[] TRACKS = {"title", "void", "tutorial", "byt_dialogue", "byt_battle",
            "lobby", "interior", "sage", "forest", "forest_combat", "anchor", "king_1", "king_2",
            "king_3", "victory", "rebirth", "descent", "vn_street", "vn_house", "vn_room", "vn_stairs",
            "vn_basement", "vn_mushrooms", "vn_closeup", "vn_tunnel", "vn_forest"};
    private static final ExecutorService LOADER = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "music-loader"); thread.setDaemon(true); return thread;
    });
    private final Properties sceneMusic = new Properties();
    private final GamePanel gp;
    private Sound active = new Sound(), outgoing;
    private final Sound wind = new Sound(), water = new Sound(), fire = new Sound();
    private int current = -1, scan, candidate = -1, loadingTrack = -1;
    private double stableFor, fadeProgress = 1, fadeSeconds = 1.6, playSeconds;
    private float mixGain = .85f;
    private CompletableFuture<Sound> loading;
    private long lastUpdate;
    private int failedTrack = -1;
    private double retryAfter;
    private boolean closed;
    private int preview = -1, sceneTrack = -1;
    private float waterNear, fireNear, windGain, waterGain, fireGain;
    private long lastText;
    private final boolean[] ambientAttempted = new boolean[3];
    public AudioDirector(GamePanel gp) {
        this.gp = gp;
        try (var in = AudioDirector.class.getResourceAsStream("/sounds/music-scenes.properties")) {
            if (in != null) sceneMusic.load(in);
        } catch (java.io.IOException e) { System.err.println("Cannot read scene music: " + e.getMessage()); }
    }

    public int contextTrack() {
        if (gp.gameState == GameState.MENU) return 0;
        String map = gp.tileM.currentMapPath();
        if (gp.levelManager.getActiveBoss() instanceof Enemy_BYT
                || map != null && (map.contains("prologue") || map.contains("byt_arena"))
                || gp.gameState == GameState.CUTSCENE && !gp.storyFlags.isPrologueBytEncounterCompleted())
            return SILENCE;
        if (gp.gameState == GameState.DEAD || gp.campaign.rebirthing()) return 15;
        if (gp.gameState == GameState.CUTSCENE) {
            return 5;
        }
        if (gp.levelManager.getActiveBoss() instanceof Enemy_SlimeKing king) return 10 + king.audioPhase();
        if (map == null) return 0;
        if (gp.campaign.inArena() && gp.campaign.bossCleared()) return 14;
        if (map.contains("interior_sage")) return 7;
        if (map.contains("interior")) return 6;
        if (map.contains("village")) return 5;
        if (gp.campaign.inForest()) {
            if (gp.campaign.inCombat()) return 9;
            if (gp.campaign.anchorOpen() && gp.player.worldX / gp.tileSize > 140
                    && gp.player.worldY / gp.tileSize < 15) return 10;
            return 8;
        }
        return 16;
    }

    public String status() {
        return (current == SILENCE ? "Cisza — rozdział 0" : current < 0 ? "—" : TRACKS[current])
                + (candidate >= 0 && candidate != current ? " → " + TRACKS[candidate] : "")
                + (preview >= 0 ? " (odsłuch)" : "");
    }
    public int currentTrack() { return current; }
    public boolean isTransitioning() { return outgoing != null; }
    public static boolean isChapterZeroTrack(int track) { return track >= 1 && track <= 4 || track >= 17; }
    public void preview(int track) {
        int selected = Math.floorMod(track, TRACKS.length);
        if (!isChapterZeroTrack(selected)) preview = selected;
    }
    public void automatic() { preview = -1; }

    public void update() {
        long now = System.nanoTime();
        double seconds = lastUpdate == 0 ? 1.0 / 60 : (now - lastUpdate) / 1_000_000_000.0;
        lastUpdate = now;
        update(seconds);
    }

    /** Explicit elapsed time also permits deterministic transition validation without an audio device. */
    public void update(double seconds) {
        if (closed) return;
        double dt = Math.max(0, Math.min(.25, seconds));
        retryAfter = Math.max(0, retryAfter - dt);
        // Keep the scene when a modal overlay is opened. F1 previews end on closing F1.
        if (gp.gameState != GameState.MOD_MENU) preview = -1;
        boolean overlay = gp.gameState == GameState.PAUSED || gp.gameState == GameState.MOD_MENU
                || gp.gameState == GameState.SHOP || gp.gameState == GameState.QUESTS || gp.gameState == GameState.SKILLS;
        if (!overlay || sceneTrack < 0) sceneTrack = contextTrack();
        int wanted = preview >= 0 ? preview : sceneTrack;
        if (wanted == SILENCE) {
            active.close();
            if (outgoing != null) { outgoing.close(); outgoing = null; }
            if (loading != null) { loading.thenAccept(Sound::close); loading = null; }
            current = candidate = SILENCE; fadeProgress = 1;
            updateAmbience(overlay);
            return;
        }
        if (candidate != wanted) { candidate = wanted; stableFor = 0; }
        stableFor += dt; playSeconds += dt;
        if (loading != null && loading.isDone() && loadingTrack != wanted) {
            Sound obsolete = loading.join(); obsolete.close(); loading = null;
        }
        if (wanted != current && loading == null && (wanted != failedTrack || retryAfter == 0)
                && (current < 0 || stableFor >= .2)) {
            loadingTrack = wanted;
            final int track = wanted;
            if (java.awt.GraphicsEnvironment.isHeadless()) loading = CompletableFuture.completedFuture(new Sound());
            else loading = CompletableFuture.supplyAsync(() -> {
                Sound prepared = new Sound(); prepared.setVolume(0); prepared.setFile(Sound.SCORE_START + track);
                return prepared;
            }, LOADER);
        }
        // Finish the previous fade before committing another: rapid dialogue skips cannot chop a tail.
        if (loading != null && loading.isDone() && outgoing == null && (current < 0 || fadeProgress >= 1)
                && stableFor >= .2 && onBeat(dt)) {
            Sound prepared = loading.join(); loading = null;
            if (prepared.hasLoadedClip() || java.awt.GraphicsEnvironment.isHeadless()) {
                outgoing = current < 0 ? null : active;
                if (current < 0) active.close();
                active = prepared; current = wanted; playSeconds = 0; fadeProgress = 0;
                failedTrack = -1;
                fadeSeconds = current >= 17 ? 2.2 : current >= 11 && current <= 13 ? 1.0 : 1.6;
                active.loop();
            } else {
                prepared.close(); // Retain the playing score when the device cannot open the replacement.
                failedTrack = wanted; retryAfter = 5;
            }
        }
        fadeProgress = Math.min(1, fadeProgress + dt / fadeSeconds);
        // Smoothstep has zero slope at both ends; equal-power gains avoid a hole halfway through.
        double x = fadeProgress * fadeProgress * (3 - 2 * fadeProgress);
        boolean talking = gp.dialogueManager.hasActiveLine();
        float target = talking ? .72f : overlay ? .5f : .85f;
        mixGain += (target - mixGain) * (float)(1 - Math.exp(-dt / .28));
        float gain = gp.audio.musicGain() * mixGain;
        active.setVolume(gain * (float)Math.sin(x * Math.PI / 2));
        if (outgoing != null) {
            outgoing.setVolume(gain * (float)Math.cos(x * Math.PI / 2));
            if (fadeProgress >= 1) { outgoing.close(); outgoing = null; }
        }
        updateAmbience(overlay);
    }

    private boolean onBeat(double dt) {
        if (current < 0 || preview >= 0 || gp.gameState == GameState.DEAD) return true;
        String tempo = sceneMusic.getProperty("bpm." + TRACKS[current]);
        if (tempo == null) return true;
        double beat = 60.0 / Double.parseDouble(tempo);
        double position = java.awt.GraphicsEnvironment.isHeadless() ? playSeconds : active.playbackSeconds();
        return position % beat <= dt;
    }

    private void updateAmbience(boolean overlay) {
        if (java.awt.GraphicsEnvironment.isHeadless()) return;
        String map = gp.tileM.currentMapPath();
        boolean outside = map != null && (map.contains("village") || gp.campaign.inForest());
        boolean audible = outside && !overlay && gp.gameState != GameState.CUTSCENE && gp.gameState != GameState.DEAD;
        if (++scan % 30 == 0) {
            waterNear = fireNear = 0;
            if (audible) {
                int px = gp.player.worldX / gp.tileSize, py = gp.player.worldY / gp.tileSize;
                for (int c=Math.max(0,px-7);c<Math.min(gp.tileM.getWorldCols(),px+8);c++)
                    for(int r=Math.max(0,py-7);r<Math.min(gp.tileM.getWorldRows(),py+8);r++) {
                        float near = (float)Math.max(0, 1-Math.hypot(c-px,r-py)/8);
                        String ground=gp.tileM.tile[gp.tileM.mapTileNum[c][r]].name;
                        if (Set.of("Water","Deep water","Lily pad").contains(ground)) waterNear=Math.max(waterNear,near);
                        int obj=gp.tileM.objectTileNum[c][r];
                        if(obj>=0 && Set.of("Campfire","Cave wall (torch)","Void brazier").contains(gp.tileM.tile[obj].name)) fireNear=Math.max(fireNear,near);
                    }
            }
        }
        float gain=gp.audio.effectsGain();
        windGain += ((audible?.22f:0)-windGain)*.04f;
        waterGain += ((audible?waterNear*.42f:0)-waterGain)*.04f;
        fireGain += ((audible?fireNear*.36f:0)-fireGain)*.04f;
        ambient(wind, Sound.AMBIENT_START, windGain*gain);
        ambient(water, Sound.AMBIENT_START+1, waterGain*gain);
        ambient(fire, Sound.AMBIENT_START+2, fireGain*gain);
    }
    private void ambient(Sound voice,int id,float gain) {
        int index=id-Sound.AMBIENT_START;
        if(gain>.001f && !ambientAttempted[index]){ambientAttempted[index]=true;voice.setVolume(0);voice.setFile(id);voice.loop();}
        voice.setVolume(gain);
    }
    public void text(char character, String speaker) {
        if (!gp.audio.textSounds() || !Character.isLetterOrDigit(character)) return;
        long now=System.nanoTime(); if(now-lastText<75_000_000L)return; lastText=now;
        int variant="BYT".equalsIgnoreCase(speaker)?3:Math.floorMod(speaker.hashCode(),3);
        gp.soundEffects().playEffect(Sound.TEXT_SOFT+variant);
    }
    public void dialogueEffect(int id) { gp.soundEffects().playEffect(id); }

    @Override public void close() {
        closed = true;
        active.close();
        if (outgoing != null) outgoing.close();
        wind.close(); water.close(); fire.close();
        if (loading != null) loading.thenAccept(Sound::close);
    }
}
