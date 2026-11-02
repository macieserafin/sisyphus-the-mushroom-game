package level;


import main.GamePanel;
import entity.enemy.*;
import object.ForestObject;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.awt.*;

public final class ForestScenes {
    private ForestScenes(){}
    public static void enemies(GamePanel gp){
        try(var in=ForestScenes.class.getResourceAsStream("/maps/forest1_spawns.tsv")){
            if(in==null)throw new IllegalStateException("Missing forest population");
            for(String line:new String(in.readAllBytes(),StandardCharsets.UTF_8).split("\\R")){
                if(line.isBlank()||line.startsWith("#"))continue;String[] v=line.split("\\s+");
                Enemy enemy=switch(v[0]){case "S"->new Enemy_Slime(gp);case "Z"->new Enemy_SpikySlime(gp);case "R"->new Enemy_Reflector(gp);case "K"->new Enemy_Skeleton(gp);default->throw new IllegalStateException("Invalid enemy type");};
                gp.entities.addEnemy(enemy,Integer.parseInt(v[1]),Integer.parseInt(v[2]));enemy.persistenceId=v[3];
                String[] old=v[3].split(":");enemy.relocatedSnapshot=old.length==3&&(!old[1].equals(v[1])||!old[2].equals(v[2]));
                enemy.newInWorldVersion=Boolean.parseBoolean(v[4]);
                enemy.territoryRadius=gp.tileSize*(enemy.persistenceId.startsWith("forest1.dojo.")?6:10);
            }
        }catch(IOException e){throw new IllegalStateException(e);}
    }
    public static void objects(GamePanel gp){
        int[][] ores={{46,90},{36,54},{53,19},{88,89},{119,88},{139,21}};
        for(int i=0;i<ores.length;i++)add(gp,new ForestObject(ForestObject.Kind.ORE,"forest1.iron."+(i+1)),ores[i][0],ores[i][1],"forest1.iron."+(i+1));
        add(gp,new ForestObject(ForestObject.Kind.DOCUMENTS,""),60,66,"forest1.documents");
        add(gp,new ForestObject(ForestObject.Kind.SUPPLIES,""),63,69,"forest1.supplies");
        var log=new ForestObject(ForestObject.Kind.RIDGE,"");add(gp,log,101,24,"forest1.shortcut.ridge");log.update(gp);
        for(Enemy e:gp.entities.enemies())if(gp.cChecker.checkTileAt(e,e.worldX,e.worldY)||gp.cChecker.checkObjectAt(e,e.worldX,e.worldY))e.restoreSnapshot(e.health,e.spawnWorldX,e.spawnWorldY);
    }
    private static void add(GamePanel gp,ForestObject o,int x,int y,String id){gp.entities.addObject(o,x,y);o.persistenceId=id;}
    public static int legacyResidue(java.util.Properties data){
        String prefix="world."+Campaign.FOREST+"#";
        if(!Boolean.parseBoolean(data.getProperty(prefix+"captured","false")))return 0;
        int total=0;
        try(var in=ForestScenes.class.getResourceAsStream("/maps/forest1_spawns.tsv")){
            if(in==null)throw new IllegalStateException("Missing forest population");
            for(String line:new String(in.readAllBytes(),StandardCharsets.UTF_8).split("\\R")){
                if(line.isBlank()||line.startsWith("#"))continue;String[] v=line.split("\\s+");
                if(!v[0].equals("K")&&!Boolean.parseBoolean(v[4])&&!data.containsKey(prefix+"enemy."+v[3]))total++;
            }
        }catch(IOException e){throw new IllegalStateException(e);}return total;
    }
    public static void tick(GamePanel gp){
        if(gp.narrative.enabled()||!gp.campaign.inForest()||!gp.campaign.started()||gp.forest.isPreview()||gp.campaign.anchorOpen()||gp.campaign.inCombat())return;
        if(gp.player.worldY<=9*gp.tileSize&&gp.player.worldX>=143*gp.tileSize)gp.campaign.bindAnchor();
    }
    public static void drawTargets(Graphics2D g,GamePanel gp){
        for(Enemy e:gp.entities.enemies())if(e.alive&&gp.forest.dojoTarget(e.persistenceId))ui.UiSkin.PixelFont.draw(g,"!",e.worldX-gp.player.worldX+gp.player.screenX+18,e.worldY-gp.player.worldY+gp.player.screenY-16,2,ui.UiSkin.GOLD);
    }
    public static void safePosition(GamePanel gp){
        if(!gp.campaign.inForest()||!gp.cChecker.checkTileAt(gp.player,gp.player.worldX,gp.player.worldY)&&!gp.cChecker.checkObjectAt(gp.player,gp.player.worldX,gp.player.worldY))return;
        int cx=gp.player.worldX/gp.tileSize,cy=gp.player.worldY/gp.tileSize;
        for(int r=1;r<20;r++)for(int y=cy-r;y<=cy+r;y++)for(int x=cx-r;x<=cx+r;x++)if(x>=0&&y>=0&&x<160&&y<120&&!gp.cChecker.checkTileAt(gp.player,x*48,y*48-4)&&!gp.cChecker.checkObjectAt(gp.player,x*48,y*48-4)){gp.player.setWorldPosition(x*48,y*48-4);return;}
        gp.player.setWorldPosition(22*48,108*48);
    }
}
