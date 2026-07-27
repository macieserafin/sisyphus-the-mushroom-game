package main;

public class LevelManager {

    private static final String HUB_MAP_PATH = "maps/world01.txt";
    private static final String RUN_FLOOR_MAP_PATH = "maps/run_forest_test.txt";
    private static final int HUB_PLAYER_SPAWN_COL = 23;
    private static final int HUB_PLAYER_SPAWN_ROW = 21;
    private static final int RUN_PLAYER_SPAWN_COL = 100;
    private static final int RUN_PLAYER_SPAWN_ROW = 182;

    private final GamePanel gp;
    private boolean runActive = false;
    private int floorNumber = 0;

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
        clearLevel();
        gp.tileM.loadMap(RUN_FLOOR_MAP_PATH);
        gp.player.setWorldPosition(
                RUN_PLAYER_SPAWN_COL * gp.tileSize,
                RUN_PLAYER_SPAWN_ROW * gp.tileSize
        );
        gp.aSetter.setRunFloorObject();
        gp.aSetter.setRunFloorEnemy();
        gp.ui.showMessage("Forest run started. Floor " + floorNumber);
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
