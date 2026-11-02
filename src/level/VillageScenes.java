package level;

import entity.npc.NPC_Villager;
import entity.npc.NPC_Ellis;
import main.GamePanel;
import object.EllisNotebook;
import object.Inspectable;
import story.ChapterText;
import tile.MapPatch;
import tile.TileManager;
import java.awt.Color;

public final class VillageScenes {
    public static final int FOREST_CAMP_COL=20, FOREST_CAMP_ROW=51;
    public static final int NOTEBOOK_COL=31, NOTEBOOK_ROW=40;
    private VillageScenes() {}

    public static void applyLobby(TileManager map, boolean settled) {
        if (settled) MapPatch.apply(map,"maps/village_ellis.patch");
    }

    public static void addLobby(GamePanel gp) {
        if (gp.village.ellisSettled()) {
            boolean working = gp.narrative.enabled() && !gp.narrative.state.has("ellis.bench");
            var ellis = gp.entities.addNPC(new NPC_Ellis(gp),working ? 36 : VillageLayout.ELLIS_COL,working ? 27 : VillageLayout.ELLIS_ROW);
            if (working) ellis.scriptedFace(ellis.worldX - 1, ellis.worldY);
        }
        for (int x : new int[]{6,9,12}) {
            gp.entities.addObject(new Inspectable(ChapterText.get("care.grave.name"),
                    ChapterText.lines("care.grave").toArray(String[]::new)).story("cemetery"),x,14);
        }
    }

    private static NPC_Villager resident(GamePanel gp,String key,String sprite,Color accent) {
        return new NPC_Villager(gp,ChapterText.get(key+".name"),sprite,accent,false)
                .conversation(ChapterText.lines(key+".talk").toArray(String[]::new));
    }

    public static void addCareHome(GamePanel gp) {
        gp.entities.addNPC(resident(gp,"care.keeper","caregiver",new Color(0x91ad94)),7,5);
        gp.entities.addNPC(resident(gp,"care.elder.a","care_elder_a",new Color(0xa497ae)),3,6);
        gp.entities.addNPC(resident(gp,"care.elder.b","care_elder_b",new Color(0x99abb8)),11,6);
    }

    public static void prepareLegacyWorkshop(GamePanel gp) {
        for(int y=34;y<=36;y++)for(int x=39;x<=42;x++) {
            gp.tileM.mapTileNum[x][y]=tile.TileSet.id("earth");
            gp.tileM.objectTileNum[x][y]=tile.MapFile.EMPTY;
        }
    }

    public static void addForest(GamePanel gp) {
        MapPatch.apply(gp.tileM,gp.village.ellisSettled()?"maps/ellis_forest_empty.patch":"maps/ellis_forest_camp.patch");
        if (gp.village.ellisSettled()) return;
        gp.entities.addNPC(new NPC_Ellis(gp),21,54);
        if (!gp.village.notebookFound()) gp.entities.addObject(new EllisNotebook(),NOTEBOOK_COL,NOTEBOOK_ROW);
    }

    public static void ensureSafePlayerPosition(GamePanel gp) {
        if (!gp.cChecker.checkTileAt(gp.player,gp.player.worldX,gp.player.worldY)) return;
        int cx=Math.floorDiv(gp.player.worldX,gp.tileSize), cy=Math.floorDiv(gp.player.worldY,gp.tileSize);
        for(int radius=1;radius<=8;radius++) for(int dy=-radius;dy<=radius;dy++) for(int dx=-radius;dx<=radius;dx++) {
            if(Math.max(Math.abs(dx),Math.abs(dy))!=radius)continue;
            int x=(cx+dx)*gp.tileSize,y=(cy+dy)*gp.tileSize;
            if(!gp.cChecker.checkTileAt(gp.player,x,y)){gp.player.setWorldPosition(x,y);return;}
        }
        gp.player.setWorldPosition(VillageLayout.SPAWN_COL*gp.tileSize,VillageLayout.SPAWN_ROW*gp.tileSize);
    }
}
