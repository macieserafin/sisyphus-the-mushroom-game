package world;

import entity.enemy.Enemy;
import entity.npc.NPC;
import main.GamePanel;
import object.SuperObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WorldEntities {
    private final GamePanel gp;
    private final List<SuperObject> objects = new ArrayList<>();
    private final List<NPC> npcs = new ArrayList<>();
    private final List<Enemy> enemies = new ArrayList<>();
    private final List<SuperObject> objectView = Collections.unmodifiableList(objects);
    private final List<NPC> npcView = Collections.unmodifiableList(npcs);
    private final List<Enemy> enemyView = Collections.unmodifiableList(enemies);
    private boolean deadEnemyCleanupNeeded = false;

    public WorldEntities(GamePanel gp) {
        this.gp = gp;
    }

    public List<SuperObject> objects() {
        return objectView;
    }

    public List<NPC> npcs() {
        return npcView;
    }

    public List<Enemy> enemies() {
        return enemyView;
    }

    public SuperObject addObject(SuperObject object, int col, int row) {
        if (object == null) {
            throw new IllegalArgumentException("Cannot spawn null object at col " + col + ", row " + row);
        }
        object.worldX = col * gp.tileSize;
        object.worldY = row * gp.tileSize;
        objects.add(object);
        return object;
    }

    public NPC addNPC(NPC npc, int col, int row) {
        if (npc == null) {
            throw new IllegalArgumentException("Cannot spawn null NPC at col " + col + ", row " + row);
        }
        npc.worldX = col * gp.tileSize;
        npc.worldY = row * gp.tileSize;
        npcs.add(npc);
        return npc;
    }

    public Enemy addEnemy(Enemy enemy, int col, int row) {
        if (enemy == null) {
            throw new IllegalArgumentException("Cannot spawn null enemy at col " + col + ", row " + row);
        }
        enemy.worldX = col * gp.tileSize;
        enemy.worldY = row * gp.tileSize;
        enemies.add(enemy);
        return enemy;
    }

    public void removeObject(SuperObject object) {
        if (!objects.remove(object)) {
            System.err.println("Object removal requested for object not in level: " + object);
        }
    }

    public void clearObjects() {
        objects.clear();
    }

    public void removeNPC(NPC npc) {
        npcs.remove(npc);
    }

    public void clearNPCs() {
        npcs.clear();
    }

    public void clearEnemies() {
        enemies.clear();
    }

    public void cleanupDeadEnemies() {
        if (!deadEnemyCleanupNeeded) {
            return;
        }
        enemies.removeIf(Enemy::isReadyForRemoval);
        deadEnemyCleanupNeeded = false;
    }

    public void markDeadEnemyCleanupNeeded() {
        deadEnemyCleanupNeeded = true;
    }
}
