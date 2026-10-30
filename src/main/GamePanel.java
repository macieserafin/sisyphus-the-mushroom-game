package main;

import dialogue.DialogueLine;
import dialogue.DialogueManager;
import dialogue.DialogueSpeaker;
import entity.Entity;
import entity.Player;
import entity.anim.SpriteSheet;
import entity.anim.WeaponSprite;
import entity.combat.Hit;
import entity.combat.HitType;
import entity.enemy.Enemy;
import entity.npc.NPC;
import level.AssetSetter;
import level.LevelManager;
import object.SuperObject;
import save.SaveStore;
import save.Settings;
import story.PrologueTutorialManager;
import story.QuestLog;
import story.StoryFlags;
import story.StorySequenceManager;
import story.StorySequences;
import tile.TileManager;
import world.BulletManager;
import world.Ambience;
import world.ScreenFx;
import world.CollisionChecker;
import world.EffectsManager;
import world.EnemyShotManager;
import world.Lighting;
import world.WorldEntities;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.VolatileImage;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
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

    // where the settings and the three save slots live; -Dsisyphus.saveDir=memory keeps them in memory (tests)
    public final SaveStore store = SaveStore.createDefault();
    public final Settings settings = Settings.load(store);
    public final ComfortSettings comfort = new ComfortSettings(settings);
    public final VisualSettings visuals = new VisualSettings(settings);
    public GameSaves saves;

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
    private static final String[] BOSS_RETRY_TAUNTS = {
            "Again? The King remembers your face.",
            "Stubborn. I like stubborn.",
            "Go on. Feed him another one.",
    };
    private final Random random = new Random();
    private boolean bossRetryAvailable = false;
    private int deathChoice = 0;
    Sound se = new Sound();

    public CollisionChecker cChecker = new CollisionChecker(this);
    public AssetSetter aSetter = new AssetSetter(this);
    public UI ui = new UI(this);
    public TitleMenu titleMenu = new TitleMenu(this);
    public DialogueManager dialogueManager = new DialogueManager(this);
    public StoryFlags storyFlags = new StoryFlags();
    public QuestLog questLog = new QuestLog();
    private final ArrayDeque<String> questNotices = new ArrayDeque<>();
    public QuestMenu questMenu = new QuestMenu(this);
    public SkillTreeMenu skillTreeMenu = new SkillTreeMenu(this);
    public ModMenu modMenu = new ModMenu(this);
    public DisplayManager display = new DisplayManager(this);
    public AudioSettings audio = new AudioSettings(this);
    public ControlSettings controls = new ControlSettings(settings);
    public OptionsMenu optionsMenu = new OptionsMenu(this);
    public PauseMenu pauseMenu = new PauseMenu(this);
    public StorySequenceManager storySequenceManager = new StorySequenceManager(this);
    public PrologueTutorialManager prologueTutorialManager = new PrologueTutorialManager(this);
    Thread gameThread;
    public TileManager tileM = new TileManager(this);
    public WorldEntities entities = new WorldEntities(this);
    public BulletManager bulletManager = new BulletManager(this);
    public EffectsManager effects = new EffectsManager(this);
    public final ScreenFx screenFx = new ScreenFx(this);
    private static final int TRANSITION_FRAMES = 26;
    private String transitionMap = "";
    private int transitionFrames;
    private double viewX, viewY, viewScale;
    private int bufferPixelSize = 1;
    private VolatileImage volatileBuffer;
    private BufferedImage softBuffer;
    private final List<Entity> drawOrder = new ArrayList<>();
    private static final Comparator<Entity> FEET_ORDER = Comparator.comparingInt(e -> e.worldY + e.solidArea.y + e.solidArea.height);
    public final Ambience ambience = new Ambience(this);
    public final Lighting lighting = new Lighting(this);
    public EnemyShotManager enemyShots = new EnemyShotManager(this);
    public LevelManager levelManager = new LevelManager(this);
    public DebugSettings debugSettings = new DebugSettings();
    public MushroomWallet wallet = new MushroomWallet();
    public Progression progression = new Progression();
    public ShopMenu shopMenu = new ShopMenu(this);
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
        saves = new GameSaves(this);
    }

    public void setupGame(){
        gameState = GameState.MENU;
        applyAudioSettings();
        titleMenu.refresh();
        // closing the window writes the active slot once more
        Runtime.getRuntime().addShutdownHook(new Thread(() -> saves.saveNow(), "save-on-exit"));
    }

    // Starts playing a save slot: an empty slot is a new game, a filled one resumes in the village
    // (or restarts the prologue when it was not finished).
    public void startSlot(int slot, boolean fresh) {
        boolean resume = !fresh && saves.load(slot);
        if (!resume) {
            saves.startNew(slot);
        }
        storySequenceManager.cancel();
        dialogueManager.clear();
        questNotices.clear();
        villageAwakeningPending = false;
        restartAfterDeathPending = false;
        tutorialRespawnPending = false;
        prologueSkipRequested = false;
        hitStopFrames = 0;
        keyH.clearGameplayInput();
        ui.clearBlink();
        prologueTutorialManager = new PrologueTutorialManager(this);
        player.setDefaultValues();
        player.resetLoadout();
        if (resume && storyFlags.isPrologueBytEncounterCompleted()) {
            gameState = GameState.PLAYING;
            levelManager.loadHub();
            playAreaMusic();
            ui.showMessage("Welcome back");
        } else {
            storyFlags = new StoryFlags();
            startNewGame();
        }
    }

    public void applyAudioSettings() {
        music.setVolume(audio.musicGain());
        cutsceneMusic.setVolume(audio.musicGain());
        se.setVolume(audio.effectsGain());
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
//                    System.out.println("UPS: " + ticks + "  FPS: " + frames);
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
        saves.tick();
        if (gameState == GameState.MOD_MENU) return;
        if (gameState == GameState.MENU) {
            titleMenu.update();
            return;
        }

        if (gameState == GameState.PAUSED) {
            pauseMenu.update();
            return;
        }
        if (gameState == GameState.SHOP) {
            shopMenu.update();
            return;
        }
        if (gameState == GameState.QUESTS) {
            questMenu.update();
            return;
        }
        if (gameState == GameState.SKILLS) {
            skillTreeMenu.update();
            return;
        }
        questLog.sync(this);
        questNotices.addAll(questLog.drainNotices());
        if (!ui.messageOn && !questNotices.isEmpty() && gameState == GameState.PLAYING) {
            String notice = questNotices.poll();
            ui.showMessage(notice);
            if (notice.startsWith("Quest complete")) {
                playSoundEffect(Sound.QUEST_DONE);
            } else if (notice.startsWith("New quest")) {
                playSoundEffect(Sound.QUEST_START);
            }
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

        ui.update();
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
            enemyShots.update();
            effects.update();
            lighting.update();
            ambience.update();
            screenFx.update();
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
                if (enemy.alive && !debugSettings.areEnemiesFrozen()) {
                    enemy.update();
                } else if (!enemy.alive) {
                    enemy.handleDeathOnce();
                    enemy.updateDeathAnimation();
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
        if (!computeView()) {
            return;
        }
        boolean sharp = visuals.scaling() == VisualSettings.Scaling.SHARP
                && Math.abs(bufferPixelSize / deviceScale() - viewScale * scale) > 1e-6;
        if (sharp) {
            paintThroughBuffer(g2, true);
        } else if (screenFx.active() && !visuals.reduceMotion()) {
            paintThroughBuffer(g2, false);
        } else {
            applyUiTransform(g2);
            renderScene(g2);
        }
        g2.dispose();
    }

    private void paintThroughBuffer(Graphics2D g2, boolean smooth) {
        int width = nativeWidth() * bufferPixelSize;
        int height = nativeHeight() * bufferPixelSize;
        Image buffer = frameBuffer(width, height, smooth);
        Graphics2D bg = (Graphics2D) buffer.getGraphics();
        bg.setColor(Color.BLACK);
        bg.fillRect(0, 0, width, height);
        bg.scale(bufferPixelSize / (double) scale, bufferPixelSize / (double) scale);
        bg.clipRect(0, 0, screenWidth, screenHeight);
        renderScene(bg);
        bg.dispose();
        if (buffer instanceof BufferedImage image && screenFx.active() && !visuals.reduceMotion()) {
            screenFx.apply(image, bufferPixelSize / (double) scale);
        }
        g2.translate(viewX, viewY);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, smooth
                ? RenderingHints.VALUE_INTERPOLATION_BILINEAR : RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g2.drawImage(buffer, 0, 0, (int) Math.round(screenWidth * viewScale), (int) Math.round(screenHeight * viewScale), null);
    }

    private Image frameBuffer(int width, int height, boolean accelerated) {
        GraphicsConfiguration config = getGraphicsConfiguration();
        if (accelerated && config != null) {
            if (volatileBuffer == null || volatileBuffer.getWidth() != width || volatileBuffer.getHeight() != height
                    || volatileBuffer.validate(config) == VolatileImage.IMAGE_INCOMPATIBLE) {
                volatileBuffer = config.createCompatibleVolatileImage(width, height);
            }
            return volatileBuffer;
        }
        if (softBuffer == null || softBuffer.getWidth() != width || softBuffer.getHeight() != height) {
            softBuffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        }
        return softBuffer;
    }

    private void renderScene(Graphics2D g2) {
        g2.setRenderingHint(
                RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR
        );
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        if (gameState == GameState.MENU || gameState == GameState.CUTSCENE
                || storySequenceManager.isVisualNovelSequenceActive()) {
            if (gameState == GameState.MENU) {
                titleMenu.draw(g2);
            } else {
                ui.draw(g2);
            }
            return;
        }

        player.updateCamera();
        int shakeX = effects.shakeX();
        int shakeY = effects.shakeY();

        g2.translate(shakeX, shakeY);
        tileM.draw(g2);
        effects.drawGround(g2);

        for(SuperObject object : entities.objects()) {
            object.draw(g2, this);
        }
        drawOrder.clear();
        drawOrder.addAll(entities.npcs());
        drawOrder.addAll(entities.enemies());
        drawOrder.add(player);
        drawOrder.sort(FEET_ORDER);
        for(Entity entity : drawOrder) {
            if (entity == player) {
                player.draw(g2);
            } else {
                entity.draw(g2);
            }
        }

        tileM.drawOverhead(g2);
        bulletManager.draw(g2);
        enemyShots.draw(g2);
        player.drawProjectileCore(g2);
        ambience.drawLow(g2);
        lighting.draw(g2);
        ambience.drawGlow(g2);

        for(Enemy enemy : entities.enemies()) {
            enemy.drawOverlay(g2);
        }
        effects.drawOver(g2);
        g2.translate(-shakeX, -shakeY);
        effects.drawScreen(g2);
        drawMapTransition(g2);

        ui.draw(g2);
    }

    private void drawMapTransition(Graphics2D g2) {
        String path = tileM.currentMapPath();
        if (!path.equals(transitionMap)) {
            transitionMap = path;
            transitionFrames = TRANSITION_FRAMES;
        }
        if (transitionFrames <= 0) {
            return;
        }
        float t = transitionFrames / (float) TRANSITION_FRAMES;
        Composite old = g2.getComposite();
        g2.setComposite(AlphaComposite.SrcOver.derive(t * t));
        g2.setColor(Color.BLACK);
        g2.fillRect(0, 0, screenWidth, screenHeight);
        g2.setComposite(old);
        transitionFrames--;
    }

    private int nativeWidth() {
        return screenWidth / scale;
    }

    private int nativeHeight() {
        return screenHeight / scale;
    }

    private double deviceScale() {
        GraphicsConfiguration config = getGraphicsConfiguration();
        return config == null ? 1.0 : Math.max(1.0, config.getDefaultTransform().getScaleX());
    }

    private boolean computeView() {
        double device = deviceScale();
        double width = getWidth();
        double height = getHeight();
        double fit = Math.min(width / nativeWidth(), height / nativeHeight());
        if (fit <= 0) {
            return false;
        }
        int whole = (int) Math.min(width * device / nativeWidth(), height * device / nativeHeight());
        bufferPixelSize = Math.max(1, whole);
        double artPixel = visuals.scaling() == VisualSettings.Scaling.PIXEL_PERFECT && whole >= 1 ? whole / device : fit;
        viewScale = artPixel / scale;
        viewX = Math.floor((width - nativeWidth() * artPixel) / 2 * device) / device;
        viewY = Math.floor((height - nativeHeight() * artPixel) / 2 * device) / device;
        return true;
    }

    public boolean applyUiTransform(Graphics2D g2) {
        if (viewScale <= 0 && !computeView()) {
            return false;
        }
        g2.translate(viewX, viewY);
        g2.scale(viewScale, viewScale);
        g2.clipRect(0, 0, screenWidth, screenHeight);
        return true;
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

    // Pause menu "Quit without saving": the slot goes back to its last manual save (or to how it was loaded).
    public void quitWithoutSaving() {
        saves.abandon();
        returnToTitle();
    }

    public void returnToTitle() {
        saves.deactivate();
        storySequenceManager.cancel();
        dialogueManager.clear();
        music.stop();
        cutsceneMusic.stop();
        se.stopAllEffects();
        currentMusic = -1;

        storyFlags = new StoryFlags();
        prologueTutorialManager = new PrologueTutorialManager(this);
        questLog.reset();
        questNotices.clear();
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
        titleMenu.refresh();
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

    // Plays one of several variants of an effect so repeated sounds (hits, pickups) do not wear on the ear.
    public void playSoundEffectOf(int... variants) {
        playSoundEffect(variants[random.nextInt(variants.length)]);
    }

    public void enterCutsceneMode() {
        ui.resetVisualNovelScene();
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
        ui.clearBlink();
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
        bossRetryAvailable = levelManager.getActiveBoss() != null && levelManager.getActiveBoss().alive;
        deathChoice = 0;
        gameState = GameState.DEAD;
    }

    // Death screen choice in a boss fight: 0 = fight again, 1 = back to the village.
    public boolean canRetryBoss() {
        return bossRetryAvailable;
    }

    public int deathChoice() {
        return deathChoice;
    }

    public void moveDeathChoice(int delta) {
        if (!bossRetryAvailable) {
            return;
        }
        deathChoice = Math.floorMod(deathChoice + delta, 2);
        playSoundEffect(Sound.UI_MOVE);
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

        if (bossRetryAvailable && deathChoice == 0) {
            bossRetryAvailable = false;
            levelManager.retryBossArena();
            gameState = GameState.PLAYING;
            dialogueManager.showAmbient(BYT_VOICE, BOSS_RETRY_TAUNTS[random.nextInt(BOSS_RETRY_TAUNTS.length)], DialogueLine.Priority.HIGH, 60 * 3);
            return;
        }
        bossRetryAvailable = false;

        boolean diedInRun = levelManager.isRunActive();
        int kept = diedInRun ? wallet.loseRun() : 0;
        levelManager.loadHub();
        gameState = GameState.PLAYING;
        if (diedInRun) {
            ui.showMessage("Kept " + kept + " mushrooms");
        }
        playAreaMusic();
        dialogueManager.showAmbient(BYT_VOICE, WAKE_UP_TAUNTS[random.nextInt(WAKE_UP_TAUNTS.length)], DialogueLine.Priority.HIGH, 60 * 4);
    }

    public void debugDamageActiveBoss(int damage) {
        Enemy boss = levelManager.getActiveBoss();
        if (boss == null) {
            return;
        }
        boss.receiveHit(new Hit(damage, HitType.MELEE, null, player.worldX, player.worldY, 0, null));
    }

    public enum DebugDestination { BYT, TUTORIAL, LOBBY, SLIME_KING }

    public void debugTeleport(DebugDestination destination) {
        storySequenceManager.cancel();
        dialogueManager.clear();
        cutsceneMusic.stop();
        villageAwakeningPending = false;
        restartAfterDeathPending = false;
        tutorialRespawnPending = false;
        prologueSkipRequested = false;
        bossRetryAvailable = false;
        hitStopFrames = 0;
        keyH.clearGameplayInput();
        ui.clearBlink();
        ui.messageOn = false;
        player.setDefaultValues();
        storyFlags = new StoryFlags();
        storyFlags.completeIntroCutscene();
        prologueTutorialManager = new PrologueTutorialManager(this);
        gameState = GameState.PLAYING;
        if (destination != DebugDestination.TUTORIAL) {
            storyFlags.completePrologueTutorial();
            storyFlags.startPrologueBytEncounter();
            if (destination != DebugDestination.BYT) storyFlags.completePrologueBytEncounter();
        }
        switch (destination) {
            case BYT -> levelManager.enterPrologueBytArena();
            case TUTORIAL -> { levelManager.loadPrologue(); playAreaMusic(); }
            case LOBBY -> levelManager.loadHub();
            case SLIME_KING -> levelManager.debugEnterSlimeKingArena();
        }
        player.updateCamera();
    }

    public void debugClearProjectiles() {
        bulletManager.clearBullets();
        enemyShots.clear();
        for (Enemy enemy : entities.enemies()) {
            if (enemy instanceof entity.enemy.Enemy_BYT byt) byt.debugClearAttacks();
        }
    }

    public void debugToggleGodMode() {
        debugSettings.toggleGodMode();
        if (debugSettings.isGodMode()) {
            player.health = player.maxHealth;
            if (levelManager.getActiveBoss() instanceof entity.enemy.Enemy_BYT byt) byt.debugCancelExecution();
        }
    }

    public void debugReloadArt() {
        try {
            int sheets = SpriteSheet.reloadAll();
            int weapons = WeaponSprite.reloadAll();
            ui.showMessage("Art reloaded: " + sheets + " sheets, " + weapons + " weapons");
        } catch (RuntimeException e) {
            ui.showMessage("Art reload failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void startHitStop(int frames) {
        hitStopFrames = Math.max(hitStopFrames, frames);
    }
}
