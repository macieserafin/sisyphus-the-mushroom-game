package main;

import entity.enemy.Enemy;
import entity.enemy.Enemy_PhaseTest;
import entity.enemy.Enemy_Reflector;
import entity.enemy.Enemy_Slime;
import entity.enemy.Enemy_SlimeKing;
import entity.npc.NPC;
import entity.npc.NPC_OldMan;
import object.Boots;
import object.Chest;
import object.Door;
import object.Key;
import object.Portal;
import object.SuperObject;

public class AssetSetter {

    GamePanel gp;
    public AssetSetter(GamePanel gp) {
        this.gp = gp;
    }

        public void setObject(){

            spawnObject(new Key(), 23, 7);
            spawnObject(new Key(), 34, 23);
            spawnObject(new Chest(), 25, 10);
            spawnObject(new Door(false), 25, 11);
            spawnObject(new Door(false), 30, 23);
            spawnObject(new Door(false), 34, 34);
            spawnObject(new Door(true), 20, 32);
            spawnObject(new Door(false), 12, 34);
            spawnObject(new Door(false), 16, 17);
            spawnObject(new Boots(), 15, 29);
            spawnObject(new Portal(TransitionTarget.runFloor()), 12, 40);

        }

        public void setNPC(){

            spawnNPC(new NPC_OldMan(gp), 16, 31);
        }

        public void setEnemy(){

            int[][] hubSlimeSpawns = {
                    {14, 10}, {34, 11}, {18, 18}, {28, 17}, {15, 24},
                    {36, 26}, {18, 36}, {27, 34}, {41, 36}, {10, 41}
            };

            for (int i = 0; i < hubSlimeSpawns.length; i++) {
                spawnEnemy(new Enemy_Slime(gp), hubSlimeSpawns[i][0], hubSlimeSpawns[i][1]);
            }

            // spawnEnemy(new Enemy_Reflector(gp), 23, 23);
            spawnEnemy(new Enemy_PhaseTest(gp), 23, 25);
        }

        public void setRunFloorObject() {
            spawnObject(new Boots(), 95, 181);
            spawnObject(new Key(), 106, 178);
            spawnObject(new Chest(), 100, 24);
            spawnObject(new Portal(TransitionTarget.bossArena()), 100, 179);
        }

        public void setBossArenaRewardObject() {
            spawnObject(new Portal(TransitionTarget.finishRun()), 12, 15);
        }

        public void setBossArenaEnemy() {
            spawnEnemy(new Enemy_SlimeKing(gp), 12, 8);
        }

        public void setRunFloorEnemy() {
            int[][] slimeSpawns = {
                    {122, 22}, {97, 25}, {110, 31}, {149, 37}, {85, 38},
                    {118, 39}, {74, 42}, {110, 42}, {154, 43}, {83, 45},
                    {96, 56}, {145, 57}, {67, 59}, {108, 59}, {59, 62},
                    {82, 62}, {51, 64}, {89, 64}, {113, 66}, {56, 69},
                    {99, 73}, {126, 75}, {93, 78}, {105, 79}, {133, 81},
                    {90, 87}, {122, 88}, {99, 93}, {107, 95}, {81, 96},
                    {161, 99}, {128, 100}, {102, 101}, {137, 103}, {32, 104},
                    {158, 106}, {147, 113}, {126, 115}, {43, 122}, {104, 124},
                    {116, 127}, {37, 130}, {101, 131}, {114, 137}, {40, 139},
                    {102, 143}, {82, 144}, {38, 146}, {145, 150}, {110, 153},
                    {158, 153}, {36, 156}, {78, 160}, {71, 165}, {130, 166},
                    {120, 175}, {86, 178}, {123, 183}, {86, 185}, {114, 185}
            };

            for (int i = 0; i < slimeSpawns.length; i++) {
                spawnEnemy(new Enemy_Slime(gp), slimeSpawns[i][0], slimeSpawns[i][1]);
            }
        }

        private SuperObject spawnObject(SuperObject object, int col, int row) {
            return gp.entities.addObject(object, col, row);
        }

        private Enemy spawnEnemy(Enemy enemy, int col, int row) {
            return gp.entities.addEnemy(enemy, col, row);
        }

        private NPC spawnNPC(NPC npc, int col, int row) {
            return gp.entities.addNPC(npc, col, row);
        }
}
