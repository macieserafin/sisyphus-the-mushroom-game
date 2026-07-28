package main;

public class LevelManager {

    private static final String HUB_MAP_PATH = "maps/world01.txt";
    private static final String RUN_FLOOR_MAP_PATH = "maps/run_forest_test.txt";
    private static final String BOSS_ARENA_MAP_PATH = "maps/boss_slime_arena.txt";
    private static final int HUB_PLAYER_SPAWN_COL = 23;
    private static final int HUB_PLAYER_SPAWN_ROW = 21;
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

    public LevelManager(GamePanel gp) {
        this.gp = gp;
    }

    public void loadHub() {
        runActive = false;
        floorNumber = 0;
        clearLevel();
        gp.tileM.loadMap(HUB_MAP_PATH);
        gp.player.setWorldPosition(
                HUB_PLAYER_SPAWN_COL * gp.tileSize,
                HUB_PLAYER_SPAWN_ROW * gp.tileSize
        );
        gp.aSetter.setObject();
        gp.aSetter.setNPC();
        gp.aSetter.setEnemy();
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
        gp.aSetter.setBossArenaEnemy();
        gp.ui.showMessage("Boss arena");
    }

    public void onBossDefeated() {
        if (bossDefeated) {
            return;
        }

        bossDefeated = true;
        gp.aSetter.setBossArenaRewardObject();
        gp.ui.showMessage("Boss defeated");
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

        gp.keyH.clearGameplayInput();
        gp.player.setDefaultValues();
        loadHub();
        gp.gameState = GameState.PLAYING;
        gp.ui.showMessage("Run finished");
    }

    public void transitionTo(TransitionTarget target) {
        if (target == null) {
            throw new IllegalArgumentException("Transition target is required");
        }

        switch (target.type()) {
            case HUB:
                loadHub();
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
            case FINISH_RUN:
                finishRun();
                break;
            default:
                throw new IllegalArgumentException("Unsupported transition type: " + target.type());
        }
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
