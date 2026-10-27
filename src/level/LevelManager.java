package level;

import entity.enemy.Enemy;
import main.GamePanel;
import main.GameState;

public class LevelManager {

    private static final String HUB_MAP_PATH = "maps/village.txt";
    private static final String PROLOGUE_MAP_PATH = "maps/prologue_void.txt";
    private static final String RUN_FLOOR_MAP_PATH = "maps/run_forest_test.txt";
    private static final String BOSS_ARENA_MAP_PATH = "maps/boss_slime_arena.txt";
    private static final int PROLOGUE_PLAYER_SPAWN_COL = 12;
    private static final int PROLOGUE_PLAYER_SPAWN_ROW = 13;
    private static final int RUN_PLAYER_SPAWN_COL = 100;
    private static final int RUN_PLAYER_SPAWN_ROW = 182;
    private static final int BOSS_ARENA_PLAYER_SPAWN_COL = 12;
    private static final int BOSS_ARENA_PLAYER_SPAWN_ROW = 13;

    private final GamePanel gp;
    private boolean runActive = false;
    private int floorNumber = 0;
    private int bossReturnWorldX = 0;
    private int bossReturnWorldY = 0;
    private boolean bossReturnPositionSaved = false;
    private boolean bossDefeated = false;
    private Enemy activeBoss;

    public LevelManager(GamePanel gp) {
        this.gp = gp;
    }

    public void loadPrologue() {
        runActive = false;
        floorNumber = 0;
        bossReturnPositionSaved = false;
        bossDefeated = false;
        clearLevel();
        activeBoss = null;
        gp.keyH.clearGameplayInput();
        gp.tileM.loadMap(PROLOGUE_MAP_PATH);
        placePlayerAtPrologueSpawn();
    }

    public void placePlayerAtPrologueSpawn() {
        gp.player.setWorldPosition(
                PROLOGUE_PLAYER_SPAWN_COL * gp.tileSize,
                PROLOGUE_PLAYER_SPAWN_ROW * gp.tileSize
        );
    }

    /** The village, waking up on the plaza. */
    public void loadHub() {
        loadHubAt(VillageLayout.SPAWN_COL, VillageLayout.SPAWN_ROW);
    }

    public void loadHubAt(int col, int row) {
        runActive = false;
        floorNumber = 0;
        clearLevel();
        activeBoss = null;
        gp.keyH.clearGameplayInput();
        gp.tileM.loadMap(HUB_MAP_PATH);
        gp.player.setWorldPosition(col * gp.tileSize, row * gp.tileSize);
        gp.aSetter.setVillageObjects();
        gp.aSetter.setVillageNPCs();
        gp.playAreaMusic();
    }

    public void enterInterior(Interior interior) {
        clearLevel();
        activeBoss = null;
        gp.keyH.clearGameplayInput();
        gp.tileM.loadMap(interior.mapPath);
        // A few pixels up, so the player does not start out touching the exit door below.
        gp.player.setWorldPosition(interior.spawnCol() * gp.tileSize, interior.spawnRow() * gp.tileSize - 4);
        gp.aSetter.setInterior(interior);
    }

    public void loadStoryLobby() {
        runActive = false;
        floorNumber = 0;
        bossReturnPositionSaved = false;
        bossDefeated = false;
        clearLevel();
        activeBoss = null;
        gp.keyH.clearGameplayInput();
        gp.tileM.loadMap(HUB_MAP_PATH);
        gp.player.setDefaultValues();
        gp.player.setWorldPosition(
                VillageLayout.SPAWN_COL * gp.tileSize,
                VillageLayout.SPAWN_ROW * gp.tileSize
        );
        gp.aSetter.setVillageObjects();
        gp.aSetter.setVillageNPCs();
        gp.ui.showMessage("You wake up somewhere almost safe.");
    }

    public void finishPrologueBytEncounter() {
        gp.storyFlags.completePrologueBytEncounter();
        gp.keyH.clearGameplayInput();
        gp.player.setDefaultValues();
        loadHub();
        gp.requestVillageAwakening();
    }

    public void startRun() {
        if (runActive) {
            return;
        }

        runActive = true;
        floorNumber = 1;
        loadManualRunFloor();
    }

    public void loadManualRunFloor() {
        loadManualRunFloor(true);
    }

    private void loadManualRunFloor(boolean showMessage) {
        clearLevel();
        activeBoss = null;
        gp.tileM.loadMap(RUN_FLOOR_MAP_PATH);
        gp.player.setWorldPosition(
                RUN_PLAYER_SPAWN_COL * gp.tileSize,
                RUN_PLAYER_SPAWN_ROW * gp.tileSize
        );
        gp.aSetter.setRunFloorObject();
        gp.aSetter.setRunFloorEnemy();
        if (showMessage) {
            gp.ui.showMessage("Forest run started. Floor " + floorNumber);
        }
    }

    public void enterBossArena() {
        bossReturnWorldX = gp.player.worldX;
        bossReturnWorldY = gp.player.worldY + gp.tileSize;
        bossReturnPositionSaved = true;
        bossDefeated = false;

        clearLevel();
        gp.tileM.loadMap(BOSS_ARENA_MAP_PATH);
        gp.player.setWorldPosition(
                BOSS_ARENA_PLAYER_SPAWN_COL * gp.tileSize,
                BOSS_ARENA_PLAYER_SPAWN_ROW * gp.tileSize
        );
        activeBoss = gp.aSetter.setBossArenaEnemy();
        gp.ui.showMessage("Boss arena");
        gp.playAreaMusic();
    }

    public void enterPrologueBytArena() {
        runActive = false;
        floorNumber = 0;
        bossReturnPositionSaved = false;
        bossDefeated = false;

        clearLevel();
        gp.keyH.clearGameplayInput();
        gp.tileM.loadMap(BOSS_ARENA_MAP_PATH);
        gp.player.setWorldPosition(
                BOSS_ARENA_PLAYER_SPAWN_COL * gp.tileSize,
                BOSS_ARENA_PLAYER_SPAWN_ROW * gp.tileSize
        );
        activeBoss = gp.aSetter.setPrologueBytArenaEnemy();
        gp.ui.showMessage("BYT");
        gp.playAreaMusic();
    }

    public void onBossDefeated() {
        if (bossDefeated) {
            return;
        }

        bossDefeated = true;
        activeBoss = null;
        gp.aSetter.setBossArenaRewardObject();
        gp.ui.showMessage("Boss defeated");
        gp.playAreaMusic();
    }

    public void returnFromBossArena() {
        loadManualRunFloor(false);

        if (bossReturnPositionSaved) {
            gp.player.setWorldPosition(bossReturnWorldX, bossReturnWorldY);
        }

        gp.ui.showMessage("Returned to forest floor");
    }

    public void finishRun() {
        runActive = false;
        floorNumber = 0;
        bossReturnPositionSaved = false;
        bossDefeated = false;
        activeBoss = null;

        gp.keyH.clearGameplayInput();
        gp.player.setDefaultValues();
        loadHub();
        gp.gameState = GameState.PLAYING;
        gp.ui.showMessage("Run finished");
    }

    public void goToNextFloor() {
        runActive = true;
        floorNumber++;
        bossReturnPositionSaved = false;
        bossDefeated = false;
        activeBoss = null;

        gp.keyH.clearGameplayInput();
        loadManualRunFloor();
    }

    public void transitionTo(TransitionTarget target) {
        if (target == null) {
            throw new IllegalArgumentException("Transition target is required");
        }

        switch (target.type()) {
            case HUB:
                loadHub();
                break;
            case HUB_AT:
                loadHubAt(target.col(), target.row());
                break;
            case INTERIOR:
                enterInterior(target.interior());
                break;
            case RUN_FLOOR:
                startRun();
                break;
            case BOSS_ARENA:
                enterBossArena();
                break;
            case RETURN_FROM_BOSS:
                returnFromBossArena();
                break;
            case NEXT_FLOOR:
                goToNextFloor();
                break;
            case FINISH_RUN:
                finishRun();
                break;
            default:
                throw new IllegalArgumentException("Unsupported transition type: " + target.type());
        }
    }

    public Enemy getActiveBoss() {
        if (activeBoss == null || !activeBoss.alive) {
            return null;
        }

        return activeBoss;
    }

    public void clearLevel() {
        clearObjects();
        clearNPCs();
        clearEnemies();
        clearBullets();
    }

    public void clearObjects() {
        gp.entities.clearObjects();
    }

    public void clearNPCs() {
        gp.entities.clearNPCs();
    }

    public void clearEnemies() {
        gp.entities.clearEnemies();
    }

    public void clearBullets() {
        gp.bulletManager.clearBullets();
    }
}
