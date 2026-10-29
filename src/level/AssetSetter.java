package level;

import entity.enemy.Enemy;
import entity.enemy.Enemy_BYT;
import entity.enemy.Enemy_Reflector;
import entity.enemy.Enemy_Slime;
import entity.enemy.Enemy_Skeleton;
import entity.enemy.Enemy_SlimeKing;
import entity.enemy.Enemy_SpikySlime;
import entity.npc.NPC;
import entity.npc.NPC_Villager;
import main.GamePanel;
import main.Shop;
import object.Boots;
import object.Chest;
import object.Door;
import object.HouseDoor;
import object.Inspectable;
import object.Key;
import object.LoadoutRack;
import object.Portal;
import object.SuperObject;

import java.awt.*;

public class AssetSetter {
    private final GamePanel gp;

    public AssetSetter(GamePanel gp) {
        this.gp = gp;
    }

    private static final String[] LOCKED_HOME_MESSAGES = {
            "Locked. Someone inside is humming.",
            "Locked. It smells like soup.",
            "Locked. A voice: \"Not now, love, I'm sprouting.\"",
            "Locked. Something big is snoring.",
            "Locked. A note on the door: \"back after the fog\".",
    };

    public void setVillageObjects() {
        spawnObject(new HouseDoor(TransitionTarget.interior(Interior.DOJO), "hall_l"), VillageLayout.DOJO.doorCol(), VillageLayout.DOJO.doorRow());
        spawnObject(new HouseDoor(TransitionTarget.interior(Interior.DOJO), "hall_r"), VillageLayout.DOJO.doorCol() + 1, VillageLayout.DOJO.doorRow());
        spawnDoor(VillageLayout.MYCOLOGIST, Interior.MYCOLOGIST, "wood");
        spawnDoor(VillageLayout.SHOP, Interior.SHOP, "wood");
        spawnDoor(VillageLayout.SMITHY, Interior.SMITHY, "wood");
        spawnDoor(VillageLayout.SAGE, Interior.SAGE, "wood");
        for (int i = 0; i < VillageLayout.HOMES.length; i++) {
            VillageLayout.Building home = VillageLayout.HOMES[i];
            spawnObject(new HouseDoor("wood", LOCKED_HOME_MESSAGES[i % LOCKED_HOME_MESSAGES.length]), home.doorCol(), home.doorRow());
        }

        for (VillageLayout.Readable readable : VillageLayout.READABLES) {
            spawnObject(new Inspectable(readable.tile, readable.lines), readable.col, readable.row);
        }

        spawnObject(new Portal(TransitionTarget.runFloor()), VillageLayout.GATE_OPENING_COL, VillageLayout.PORTAL_ROW);
        spawnObject(new Portal(TransitionTarget.runFloor()), VillageLayout.GATE_OPENING_COL + 1, VillageLayout.PORTAL_ROW);
    }

    public void setVillageNPCs() {
        int plazaLeft = VillageLayout.PLAZA_LEFT;
        int plazaRight = VillageLayout.PLAZA_RIGHT;

        NPC fern = new NPC_Villager(gp, "Fern", "villager_fern", new Color(240, 170, 190), true)
                .conversation("Morning! Or evening. We stopped arguing about which.",
                        "You're the new one from the forest, yes? Don't worry, everyone's the new one once.")
                .conversation("Woke up on the plaza again? The old mushroom likes you. It only lets the stubborn ones back.")
                .conversation("If you see Pip, tell him the well is not a slide.");
        fern.setHomeArea(plazaLeft, VillageLayout.PLAZA_TOP + 4, plazaRight, VillageLayout.PLAZA_BOTTOM);
        spawnNPC(fern, 28, 26);

        NPC pip = new NPC_Villager(gp, "Pip", "villager_pip", new Color(150, 220, 120), true)
                .conversation("You died, right? Me too! Loads. You get used to the tingly part.")
                .conversation("The guards at the gate used to be people. Now they're more like... furniture that breathes.")
                .conversation("Bet you can't hit BYT. Nobody can. Ha!");
        pip.setHomeArea(36, 12, 48, 16);
        spawnNPC(pip, 41, 14);

        NPC moss = new NPC_Villager(gp, "Old Moss", "villager_moss", new Color(150, 190, 210), false)
                .facing("down")
                .conversation("Shh. The lake is thinking.",
                        "One day I'll lend you a rod. The fish here bite back, but they taste of rain.")
                .conversation("Another new face from the gate. You'll fit. Everyone does, eventually.");
        spawnNPC(moss, VillageLayout.PIER_COL, VillageLayout.PIER_BOTTOM - 1);

        NPC bram = new NPC_Villager(gp, "Bram", "villager_bram", new Color(210, 180, 120), true)
                .conversation("That empty plot? Reserved. Somebody always turns up to fill it. Usually after the forest spits them out.")
                .conversation("Grew turnips once. They grew me back. We've agreed not to talk about it.");
        bram.setHomeArea(12, 25, 24, 29);
        spawnNPC(bram, 18, 27);

        spawnNPC(NPC_Villager.silent(gp, "Guard", "guard"), VillageLayout.GUARD_LEFT_COL, VillageLayout.GUARD_ROW);
        spawnNPC(NPC_Villager.silent(gp, "Guard", "guard"), VillageLayout.GUARD_RIGHT_COL, VillageLayout.GUARD_ROW);
    }

    public NPC spawnAwakeningSage() {
        NPC sage = NPC_Villager.silent(gp, "Old Aldous", "sage").facing("left");
        return spawnNPC(sage, VillageLayout.SPAWN_COL + 1, VillageLayout.SPAWN_ROW);
    }

    public void setInterior(Interior interior) {
        VillageLayout.Building building = interior.building;
        spawnObject(new HouseDoor(TransitionTarget.hubAt(building.doorCol(), building.frontRow()), "interior"),
                Interior.EXIT_COL, Interior.EXIT_ROW);

        switch (interior) {
            case SMITHY:
                NPC smith = new NPC_Villager(gp, "Blacksmith", "blacksmith", new Color(230, 140, 90), true)
                        .sells(Shop.smith(gp))
                        .conversation("Another one woke up, huh? Mind the sparks.",
                                "Bring me what the forest leaves behind and I'll make your blade remember it.")
                        .conversation("Upgrades? Soon. The forge needs feeding first, and you need pockets.")
                        .conversation("That pistol of yours... spores get in the barrel. Clean it, or it cleans you.");
                smith.setHomeArea(8, 4, 12, 6);
                spawnNPC(smith, 10, 5);
                break;
            case SHOP:
                NPC merchant = new NPC_Villager(gp, "Spore Merchant", "merchant", new Color(200, 160, 240), true)
                        .sells(Shop.merchant(gp))
                        .talkRange(2.4)
                        .conversation("Welcome, welcome! Everything here costs mushrooms. The forest pays in them, if you survive it.")
                        .conversation("I was a man once. Then a better offer came along.")
                        .conversation("Don't touch the jar on the left. It's touching you back.");
                merchant.setHomeArea(3, 3, 11, 3);
                spawnNPC(merchant, 7, 3);
                break;
            case DOJO:
                NPC master = new NPC_Villager(gp, "Master Kuro", "karate_master", new Color(240, 80, 70), false)
                        .teaches()
                        .facing("down")
                        .conversation("You fight like someone who has died before. Good. Fear is a poor teacher, death is a patient one.",
                                "The rack on the left holds blades, the cabinet on the right holds guns. Choose what you carry into the forest.")
                        .conversation("Techniques are not free. Mushrooms, for now. Later, only what you have earned in blood. Ask me about skills.")
                        .conversation("Breathe in through the nose. Out through the spores.");
                spawnNPC(master, 7, 4);
                spawnObject(new LoadoutRack(true), Interior.DOJO_WEAPON_RACK_COL, Interior.DOJO_WEAPON_RACK_ROW);
                spawnObject(new LoadoutRack(false), Interior.DOJO_GUN_CABINET_COL, Interior.DOJO_GUN_CABINET_ROW);
                break;
            case MYCOLOGIST:
                NPC mycologist = new NPC_Villager(gp, "Ilse the Witch", "mycologist", new Color(150, 200, 130), true)
                        .sells(Shop.witch(gp))
                        .conversation("Ah, the newest sprout. Sit. Don't touch the pot. You died, you woke up on the plaza. That's the rule here.",
                                "Go through the gate, fight what you find, and the forest pays you in mushrooms. Bring them back. The village grows with you.")
                        .conversation("Every boss you put down leaves something behind. Watch the empty plots. They fill up.")
                        .conversation("BYT? Yes. It talks to all of us. It just talks to you louder.")
                        .conversation("Melee and ranged are not two styles, dear. They are two hands. Use both.")
                        .conversation("I brew what the forest grows. Bring me caps and spores, and one day I'll brew something that keeps you alive a little longer.");
                mycologist.setHomeArea(2, 3, 12, 8);
                spawnNPC(mycologist, 5, 5);
                break;
            case SAGE:
                NPC sage = new NPC_Villager(gp, "Old Aldous", "sage", new Color(200, 205, 220), true)
                        .sells(Shop.sage(gp))
                        .conversation("Hm? Another one. Come in, mind the hole in the roof. The rain only falls on the books.",
                                "I was here before the village. Before the gate. Before the lead came.")
                        .conversation("Your kind brought iron and powder into a forest that only knew spores. The forest has been learning ever since.")
                        .conversation("BYT is not a monster, child. It is a habit. This world has the habit of eating whoever walks in.",
                                "Habits can be broken. Rarely. Painfully.")
                        .conversation("Every death you suffer, you leave a little of yourself in the soil. Every return, you take a little of the soil with you.",
                                "Be careful which one wins.")
                        .conversation("The first one who woke on the plaza... I buried him under the old mushroom. Or he buried me. I forget.");
                sage.setHomeArea(3, 4, 11, 8);
                spawnNPC(sage, 7, 4);
                for (VillageLayout.Readable note : Interior.SAGE_NOTES) {
                    spawnObject(new Inspectable(note.tile, note.lines), note.col, note.row);
                }
                break;
            default:
                break;
        }
    }

    private void spawnDoor(VillageLayout.Building building, Interior interior, String style) {
        spawnObject(new HouseDoor(TransitionTarget.interior(interior), style), building.doorCol(), building.doorRow());
    }

    // Level 1 (maps/level1_forest.txt): boots and the key wait in maze dead ends,
    // the key opens the grotto off the second valley, the cave mouth leads to the Slime King.
    public void setRunFloorObject() {
        spawnObject(new Boots(), 96, 77);
        spawnObject(new Key(), 104, 46);
        spawnObject(new Door(), 90, 28);
        spawnObject(new Chest(), 80, 26);
        spawnObject(new Chest(), 60, 27);  // forest lake
        spawnObject(new Chest(), 90, 94);  // ruined quarry
        spawnObject(new Portal(TransitionTarget.bossArena()), 146, 5);
        spawnObject(new Portal(TransitionTarget.bossArena()), 147, 5);
    }

    public void setBossArenaRewardObject() {
        spawnObject(new Portal(TransitionTarget.finishRun()), 17, 20);
    }

    public Enemy setBossArenaEnemy() {
        return spawnEnemy(new Enemy_SlimeKing(gp), 17, 8);
    }

    public Enemy setPrologueBytArenaEnemy() {
        return spawnEnemy(new Enemy_BYT(gp), 12, 8);
    }

    // Grouped by area in route order: start glade, ravine, first valley, forest maze, mushroom pocket,
    // second valley, grotto, lake, quarry, rocky pass, cave clearing. Packed groups are the ambushes.
    public void setRunFloorEnemy() {
        int[][] slimes = {
                {22, 99}, {27, 101}, {20, 91}, {21, 93}, {24, 81}, {25, 83},
                {24, 85}, {25, 74}, {24, 76}, {37, 64}, {35, 62}, {39, 62},
                {50, 57}, {48, 55}, {52, 55}, {57, 66}, {57, 64}, {59, 64},
                {44, 69}, {42, 67}, {44, 67}, {60, 59}, {70, 52}, {77, 48},
                {75, 48}, {70, 68}, {70, 70}, {84, 64}, {84, 62}, {88, 56},
                {90, 56}, {92, 48}, {94, 47}, {100, 45}, {96, 64}, {98, 64},
                {100, 56}, {100, 72}, {100, 70}, {82, 42}, {80, 40}, {76, 77},
                {121, 58}, {119, 56}, {123, 56}, {119, 58}, {119, 60}, {123, 60},
                {117, 54}, {119, 54}, {123, 54}, {125, 54}, {110, 40}, {108, 39},
                {106, 39}, {129, 34}, {133, 34}, {106, 30}, {106, 28}, {108, 28},
                {119, 28}, {117, 26}, {121, 26}, {132, 29}, {136, 29}, {81, 28},
                {81, 26}, {69, 36}, {45, 28}, {44, 30}, {47, 26}, {58, 33},
                {60, 31}, {50, 36}, {48, 36}, {87, 88}, {83, 86}, {76, 79},
                {139, 18}, {144, 9}, {148, 9}
        };
        int[][] spikySlimes = {
                {37, 62}, {48, 57}, {86, 64}, {94, 64}, {76, 75}, {121, 56},
                {121, 60}, {121, 54}, {131, 34}, {134, 29}, {79, 26}, {45, 26},
                {85, 90}, {141, 21}, {144, 11}
        };
        int[][] reflectors = {
                {55, 64}, {70, 66}, {84, 66}, {100, 58}, {123, 58}, {117, 56},
                {119, 26}, {58, 31}, {87, 92}
        };
        int[][] skeletons = {
                {23, 83}, {22, 81}, {21, 83}, {22, 85}, {50, 55}, {58, 57},
                {70, 50}, {79, 72}, {77, 72}, {86, 56}, {98, 45}, {96, 46},
                {100, 74}, {112, 39}, {104, 28}, {117, 28}, {134, 31}, {72, 37},
                {87, 90}, {85, 88}, {89, 88}, {89, 90}, {85, 92}, {89, 92},
                {78, 82}, {137, 24}, {135, 26}, {141, 19}, {141, 17}, {143, 17},
                {143, 19}, {139, 20}, {144, 15}, {146, 11}, {146, 9}
        };

        for (int[] p : slimes) spawnEnemy(new Enemy_Slime(gp), p[0], p[1]);
        for (int[] p : spikySlimes) spawnEnemy(new Enemy_SpikySlime(gp), p[0], p[1]);
        for (int[] p : reflectors) spawnEnemy(new Enemy_Reflector(gp), p[0], p[1]);
        for (int[] p : skeletons) spawnEnemy(new Enemy_Skeleton(gp), p[0], p[1]);
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
