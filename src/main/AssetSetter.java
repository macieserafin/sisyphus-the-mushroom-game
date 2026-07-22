package main;

import entity.enemy.Enemy;
import entity.enemy.Enemy_Slime;
import entity.npc.NPC;
import entity.npc.NPC_OldMan;
import object.Boots;
import object.Chest;
import object.Door;
import object.Key;

public class AssetSetter {

    GamePanel gp;
    public AssetSetter(GamePanel gp) {
        this.gp = gp;
    }

        public void setObject(){

            gp.obj[0] = new Key();
            gp.obj[0].worldX = 23 * gp.tileSize;
            gp.obj[0].worldY = 7 * gp.tileSize;

            gp.obj[1] = new Key();
            gp.obj[1].worldX = 35 * gp.tileSize;
            gp.obj[1].worldY = 24 * gp.tileSize;

            gp.obj[2] = new Chest();
            gp.obj[2].worldX = 25 * gp.tileSize;
            gp.obj[2].worldY = 10 * gp.tileSize;

            gp.obj[3] = new Door();
            gp.obj[3].worldX = 32 * gp.tileSize;
            gp.obj[3].worldY = 23 * gp.tileSize;

            gp.obj[4] = new Boots();
            gp.obj[4].worldX = 34 * gp.tileSize;
            gp.obj[4].worldY = 20 * gp.tileSize;

        }

        public void setNPC(){

            spawnNPC(new NPC_OldMan(gp), 32, 20);
        }

        public void setEnemy(){

            spawnEnemy(new Enemy_Slime(gp), 28, 15);
        }

        private void spawnEnemy(Enemy enemy, int col, int row) {
            for (int i = 0; i < gp.enemy.length; i++) {
                if (gp.enemy[i] == null) {
                    gp.enemy[i] = enemy;
                    gp.enemy[i].worldX = col * gp.tileSize;
                    gp.enemy[i].worldY = row * gp.tileSize;
                    return;
                }
            }
        }

        private void spawnNPC(NPC npc, int col, int row) {
            for (int i = 0; i < gp.npc.length; i++) {
                if (gp.npc[i] == null) {
                    gp.npc[i] = npc;
                    gp.npc[i].worldX = col * gp.tileSize;
                    gp.npc[i].worldY = row * gp.tileSize;
                    return;
                }
            }
        }
}
