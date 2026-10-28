package main;

import dialogue.DialogueLine;
import dialogue.DialogueManager;
import dialogue.DialogueSpeaker;
import entity.Player;
import entity.combat.Hit;
import entity.combat.HitType;
import entity.enemy.Enemy;
import entity.npc.NPC;
import level.AssetSetter;
import level.LevelManager;
import object.SuperObject;
import story.PrologueTutorialManager;
import story.QuestLog;
import story.StoryFlags;
import story.StorySequenceManager;
import story.StorySequences;
import tile.TileManager;
import world.BulletManager;
import world.CollisionChecker;
import world.EffectsManager;
import world.WorldEntities;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GamePanel extends JPanel implements Runnable {
    public final int originalTitleSize = 16;
    public final int scale = 3;

    public final int tileSize = originalTitleSize * scale;
    public final int maxScreenCol = 16;
    public final int maxScreenRow = 12;
    public final int screenWidth = tileSize * maxScreenCol;
    public final int screenHeight = tileSize * maxScreenRow;

    public final int maxWorldCol = 50;
    public final int maxWorldRow = 50;

    int FPS = 60;

    public final Object frameLock = new Object();

    public KeyHandler keyH = new KeyHandler(this);
    Sound music = new Sound();
    Sound cutsceneMusic = new Sound();
    private int currentMusic = -1;
    private boolean restartAfterDeathPending = false;
    private boolean tutorialRespawnPending = false;
    private volatile boolean prologueSkipRequested = false;
    private boolean villageAwakeningPending = false;

    private static final DialogueSpeaker BYT_VOICE = new DialogueSpeaker("BYT", null, new Color(190, 80, 230));
    private static final String[] WAKE_UP_TAUNTS = {
            "Back already? The mushroom spat you out again.",
            "Again. You are very good at that.",
            "Every time you wake up, you are a little less you.",
            "The village loves a regular.",
            "I counted. That was not your best death.",
    };
    private final Random random = new Random();
    Sound se = new Sound();

    public CollisionChecker cChecker = new CollisionChecker(this);
    public AssetSetter aSetter = new AssetSetter(this);
    public UI ui = new UI(this);
    public TitleMenu titleMenu = new TitleMenu(this);
    public DialogueManager dialogueManager = new DialogueManager(this);
    public StoryFlags storyFlags = new StoryFlags();
    public QuestLog questLog = new QuestLog();
    public ModMenu modMenu = new ModMenu(this);
    public DisplayManager display = new DisplayManager(this);
    public OptionsMenu optionsMenu = new OptionsMenu(this);
    public PauseMenu pauseMenu = new PauseMenu(this);
    public StorySequenceManager storySequenceManager = new StorySequenceManager(this);
    public PrologueTutorialManager prologueTutorialManager = new PrologueTutorialManager(this);
    Thread gameThread;
    public TileManager tileM = new TileManager(this);
    public WorldEntities entities = new WorldEntities(this);
    public BulletManager bulletManager = new BulletManager(this);
    public EffectsManager effects = new EffectsManager(this);
    public LevelManager levelManager = new LevelManager(this);
    public DebugSettings debugSettings = new DebugSettings();
    private int hitStopFrames = 0;

    public Player player = new Player(this, keyH);

    public GameState gameState;

    public GamePanel() {
        this.setPreferredSize(new Dimension(screenWidth, screenHeight));
        this.setBackground(Color.black);
        this.setDoubleBuffered(true);
        this.addKeyListener(keyH);
        this.setFocusable(true);
        // Let TAB reach the key handler instead of moving keyboard focus.
        this.setFocusTraversalKeysEnabled(false);
    }

    public void setupGame(){
        gameState = GameState.MENU;
    }

    public void startNewGame() {
        levelManager.loadPrologue();

        gameState = GameState.PLAYING;

        if (!storyFlags.isIntroCutsceneCompleted()) {
            storySequenceManager.start(StorySequences.openingIntro(this));
        } else {
            playMusic(Sound.BACKGROUND);
        }
    }

    public void startGameThread() {
        gameThread = new Thread(this);
        gameThread.start();
    }

    @Override
    public void run() {
        final long tickNanos = 1_000_000_000L / FPS;
        final int maxCatchUpTicks = 5;
        long nextTick = System.nanoTime();
        long statsStart = nextTick;
        int ticks = 0;
        int frames = 0;

        while (gameThread != null) {
            int updates = 0;
            while (System.nanoTime() - nextTick >= 0 && updates < maxCatchUpTicks) {
                synchronized (frameLock) {
                    update();
                }
                nextTick += tickNanos;
                updates++;
            }
            if (updates == maxCatchUpTicks && System.nanoTime() - nextTick >= 0) {
                nextTick = System.nanoTime();
            }

            if (updates > 0) {
                repaint();
                ticks += updates;
                frames++;
            }

            long now = System.nanoTime();
            if (now - statsStart >= 1_000_000_000L) {
                if (debugSettings.isOverlayEnabled()) {
                    System.out.println("UPS: " + ticks + "  FPS: " + frames);
                }
                ticks = 0;
                frames = 0;
                statsStart = now;
            }

            long sleepNanos = nextTick - System.nanoTime();
            if (sleepNanos > 2_000_000L) {
                try {
                    Thread.sleep((sleepNanos - 1_000_000L) / 1_000_000L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            } else if (sleepNanos > 0) {
                Thread.yield();
            }
        }
    }

    public void update() {
        if (gameState == GameState.MENU) {
            titleMenu.update();
            return;
        }

        if (gameState == GameState.PAUSED) {
            pauseMenu.update();
            return;
        }

        if (prologueSkipRequested) {
            prologueSkipRequested = false;
            skipPrologue();
        }
        if (villageAwakeningPending && !storySequenceManager.isActive()) {
            villageAwakeningPending = false;
            storySequenceManager.start(StorySequences.villageAwakening(this));
        }
        if (restartAfterDeathPending) {
            restartAfterDeathPending = false;
            restartAfterDeath();
        }
        if (tutorialRespawnPending) {
            tutorialRespawnPending = false;
            keyH.clearGameplayInput();
            prologueTutorialManager.respawnPlayerForPractice();
        }

        dialogueManager.update();
        storySequenceManager.update();

        if (storySequenceManager.isActive()) {
            return;
        }

        if (gameState == GameState.PLAYING) {
            if (hitStopFrames > 0) {
                hitStopFrames--;
                return;
            }

            prologueTutorialManager.update();
            if (storySequenceManager.isActive()) {
                return;
            }
            player.update();
            if (storySequenceManager.isActive()) {
                return;
            }
            bulletManager.update();
            effects.update();
            if (storySequenceManager.isActive()) {
                return;
            }

            // A copy, because a door that finishes opening loads another map and replaces the list.
            for (SuperObject object : new ArrayList<>(entities.objects())) {
                object.update(this);
            }

            List<NPC> npcs = entities.npcs();
            for(int i = 0; i < npcs.size(); i++) {
                NPC npc = npcs.get(i);
                npc.update();
            }

            List<Enemy> enemies = entities.enemies();
            int enemyCount = enemies.size();
            for(int i = 0; i < enemyCount; i++) {
                Enemy enemy = enemies.get(i);
                if(enemy.alive) {
                    enemy.update();
                }
                else {
                    enemy.handleDeathOnce();
                    entities.markDeadEnemyCleanupNeeded();
                }
            }
            entities.cleanupDeadEnemies();
        }
    }

    public void paintComponent(Graphics g) {
        synchronized (frameLock) {
            paintFrame(g);
        }
        Toolkit.getDefaultToolkit().sync();
    }

    private void paintFrame(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;
        double viewScale = Math.min(getWidth() / (double) screenWidth, getHeight() / (double) screenHeight);
        if (viewScale <= 0) {
            return;
        }
        g2.translate((getWidth() - screenWidth * viewScale) / 2, (getHeight() - screenHeight * viewScale) / 2);
        g2.scale(viewScale, viewScale);
        g2.clipRect(0, 0, screenWidth, screenHeight);
        g2.setRenderingHint(
                RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR
        );

        if (gameState == GameState.MENU) {
            titleMenu.draw(g2);
            g2.dispose();
            return;
        }

        if (gameState == GameState.CUTSCENE || storySequenceManager.isVisualNovelSequenceActive()) {
            ui.draw(g2);
            g2.dispose();
            return;
        }

        player.updateCamera();
        int shakeX = effects.shakeX();
        int shakeY = effects.shakeY();
        g2.translate(shakeX, shakeY);
        tileM.draw(g2);
        effects.drawGround(g2);

        bulletManager.draw(g2);

        for(SuperObject object : entities.objects()) {
            object.draw(g2, this);
        }
        for(NPC npc : entities.npcs()) {
            npc.draw(g2);
        }
        for(Enemy enemy : entities.enemies()) {
            enemy.draw(g2);
        }

        player.draw(g2);

        for(Enemy enemy : entities.enemies()) {
            enemy.drawOverlay(g2);
        }
        effects.drawOver(g2);
        g2.translate(-shakeX, -shakeY);

        ui.draw(g2);

        g2.dispose();
    }

    public void playMusic(int i) {
        music.setFile(i);
        music.play();
        music.loop();
        currentMusic = i;
    }

    public void pauseGame() {
        keyH.clearGameplayInput();
        pauseMenu.open();
        gameState = GameState.PAUSED;
        pauseMusic(0);
    }

    public void resumeGame() {
        keyH.clearGameplayInput();
        gameState = GameState.PLAYING;
        playAreaMusic();
    }

    public void returnToTitle() {
        storySequenceManager.cancel();
        dialogueManager.clear();
        music.stop();
        cutsceneMusic.stop();
        se.stopAllEffects();
        currentMusic = -1;

        storyFlags = new StoryFlags();
        prologueTutorialManager = new PrologueTutorialManager(this);
        questLog.clear();
        villageAwakeningPending = false;
        restartAfterDeathPending = false;
        tutorialRespawnPending = false;
        prologueSkipRequested = false;
        hitStopFrames = 0;

        levelManager.clearLevel();
        keyH.clearGameplayInput();
        player.setDefaultValues();
        player.resetLoadout();
        gameState = GameState.MENU;
    }

    public void pauseMusic(int i) {
        music.stop();
        currentMusic = -1;
    }

    public void playAreaMusic() {
        if (gameState == GameState.CUTSCENE) {
            return;
        }

        int track = levelManager.getActiveBoss() != null ? Sound.BOSS_MUSIC : Sound.BACKGROUND;
        if (track != currentMusic) {
            playMusic(track);
        }
    }

    public void playSoundEffect(int i) {
        if (gameState == GameState.CUTSCENE) {
            return;
        }
        se.playEffect(i);
    }

    public void enterCutsceneMode() {
        keyH.clearGameplayInput();
        music.stop();
        currentMusic = -1;
        se.stopAllEffects();
        gameState = GameState.CUTSCENE;

        if (cutsceneMusic.hasResource(Sound.CUTSCENE_MUSIC)) {
            cutsceneMusic.setFile(Sound.CUTSCENE_MUSIC);
            cutsceneMusic.play();
            cutsceneMusic.loop();
        }
    }

    public void exitCutsceneMode(GameState nextState) {
        keyH.clearGameplayInput();
        cutsceneMusic.stop();
        gameState = nextState == null || nextState == GameState.CUTSCENE
                ? GameState.PLAYING
                : nextState;

        if (gameState == GameState.PLAYING) {
            playAreaMusic();
        }
    }

    public void requestVillageAwakening() {
        villageAwakeningPending = true;
    }

    // Called from the key thread; the skip itself runs at the start of the next update.
    public void requestPrologueSkip() {
        prologueSkipRequested = true;
    }

    private void skipPrologue() {
        if (storyFlags.isPrologueBytEncounterCompleted()) {
            return;
        }
        storySequenceManager.cancel();
        dialogueManager.clear();
        cutsceneMusic.stop();
        storyFlags.completeIntroCutscene();
        storyFlags.completePrologueTutorial();
        storyFlags.startPrologueBytEncounter();
        storyFlags.completePrologueBytEncounter();
        villageAwakeningPending = false;
        restartAfterDeathPending = false;
        tutorialRespawnPending = false;
        hitStopFrames = 0;
        keyH.clearGameplayInput();
        player.setDefaultValues();
        player.resetLoadout();
        gameState = GameState.PLAYING;
        levelManager.loadHub();
        ui.showMessage("Prologue skipped");
    }

    // Dying to BYT in the prologue skips the death screen and goes straight to his lines.
    // The restart waits for the next update so it doesn't run in the middle of the hit that killed the player.
    public void onPlayerDied() {
        if (isPrologueBytEncounterActive()) {
            restartAfterDeathPending = true;
            return;
        }
        if (prologueTutorialManager.isFightingPracticeSlimes()) {
            tutorialRespawnPending = true;
            return;
        }
        gameState = GameState.DEAD;
    }

    private boolean isPrologueBytEncounterActive() {
        return storyFlags.isPrologueBytEncounterStarted() && !storyFlags.isPrologueBytEncounterCompleted();
    }

    public void restartAfterDeath() {
        keyH.clearGameplayInput();
        hitStopFrames = 0;
        player.setDefaultValues();

        if (isPrologueBytEncounterActive()) {
            gameState = GameState.PLAYING;
            storySequenceManager.start(StorySequences.prologueBytPlayerFell(this));
            return;
        }

        levelManager.loadHub();
        gameState = GameState.PLAYING;
        playAreaMusic();
        dialogueManager.showAmbient(BYT_VOICE, WAKE_UP_TAUNTS[random.nextInt(WAKE_UP_TAUNTS.length)], DialogueLine.Priority.HIGH, 60 * 4);
    }

    public void debugDamageActiveBoss(int damage) {
        Enemy boss = levelManager.getActiveBoss();
        if (boss == null) {
            return;
        }
        boss.receiveHit(new Hit(damage, HitType.MELEE, player, player.worldX, player.worldY, 0, null));
    }

    public void startHitStop(int frames) {
        hitStopFrames = Math.max(hitStopFrames, frames);
    }
}
