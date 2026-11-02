package level;

import main.GamePanel;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import object.*;

public final class PlacedEntities {
    private PlacedEntities() {}
    public static String persistenceId(String map, String id) { return "placed:" + map + ":" + id; }
    public static List<EntityPlacement> load(String map) {
        try (InputStream in=PlacedEntities.class.getClassLoader().getResourceAsStream(EntityPlacement.path(map))) {
            return in==null?List.of():EntityPlacement.read(new InputStreamReader(in,StandardCharsets.UTF_8));
        } catch(IOException ex) { throw new IllegalStateException("Cannot read entities for " + map,ex); }
    }
    public static void apply(GamePanel gp, List<EntityPlacement> entries) {
        var errors=EntityPlacement.validate(entries,gp.tileM.getWorldCols(),gp.tileM.getWorldRows());
        if(!errors.isEmpty())throw new IllegalArgumentException(String.join("\n",errors));
        String map=gp.tileM.currentMapPath();
        for(var e:entries) {
            String id=persistenceId(map,e.id());
            if(!gp.narrative.state.matches(e.when())||gp.narrative.state.has("collected."+id))continue;
            if(gp.entities.objects().stream().anyMatch(o->o.persistenceId.equals(id))||gp.entities.enemies().stream().anyMatch(o->o.persistenceId.equals(id)))continue;
            switch(e.type()) {
                case KEY -> { var o=gp.entities.addObject(new Key(),e.col(),e.row());o.persistenceId=id;o.worldX+=e.dx();o.worldY+=e.dy(); }
                case CHEST, DOCUMENT -> {
                    var o=gp.entities.addObject(new PlacedObject(e),e.col(),e.row());o.persistenceId=id;o.worldX+=e.dx();o.worldY+=e.dy();
                }
                case PORTAL -> {var o=gp.entities.addObject(new Portal(target(e)),e.col(),e.row());o.persistenceId=id;o.worldX+=e.dx();o.worldY+=e.dy();}
                case NPC -> {
                    String storyId=e.value("storyId",e.id());
                    if(gp.entities.npcs().stream().anyMatch(n->n.storyId.equals(storyId)))continue;
                    var npc=new entity.npc.NPC_Villager(gp,e.value("name",e.id()),e.value("sprite",""),new java.awt.Color(200,190,170),false);
                    npc.storyId=storyId; npc.conversation(e.value("lines","Hello.").split("\\|"));
                    gp.entities.addNPC(npc,e.col(),e.row());npc.worldX+=e.dx();npc.worldY+=e.dy();
                }
                case SLIME, SKELETON -> {
                    entity.enemy.Enemy enemy=e.type()==EntityPlacement.Type.SLIME?new entity.enemy.Enemy_Slime(gp):new entity.enemy.Enemy_Skeleton(gp);
                    gp.entities.addEnemy(enemy,e.col(),e.row());enemy.persistenceId=id;
                    enemy.worldX+=e.dx();enemy.worldY+=e.dy();enemy.spawnWorldX=enemy.worldX;enemy.spawnWorldY=enemy.worldY;
                }
            }
        }
    }
    private static TransitionTarget target(EntityPlacement e) {
        return switch(TransitionTarget.Type.valueOf(e.value("target","HUB"))) {
            case HUB -> TransitionTarget.hub();
            case HUB_AT -> TransitionTarget.hubAt(Integer.parseInt(e.value("targetCol","0")),Integer.parseInt(e.value("targetRow","0")));
            case INTERIOR -> TransitionTarget.interior(Interior.valueOf(e.value("interior","")));
            case RUN_FLOOR -> TransitionTarget.runFloor();case BOSS_ARENA -> TransitionTarget.bossArena();
            case RETURN_FROM_BOSS -> TransitionTarget.returnFromBoss();case NEXT_FLOOR -> TransitionTarget.nextFloor();case FINISH_RUN -> TransitionTarget.finishRun();
        };
    }
    private static final class PlacedObject extends SuperObject {
        private final EntityPlacement entry;
        PlacedObject(EntityPlacement entry) {
            this.entry=entry; name=entry.value("name",entry.type()==EntityPlacement.Type.CHEST?"Chest":"Note");
            String imagePath=entry.value("image",entry.type()==EntityPlacement.Type.CHEST?"objects/chest.png":"");
            if(!imagePath.isBlank())try(var in=getClass().getClassLoader().getResourceAsStream(imagePath)) {
                if(in==null)throw new IllegalArgumentException("Missing image: "+imagePath);image=javax.imageio.ImageIO.read(in);
            }catch(IOException ex){throw new IllegalArgumentException("Cannot load "+imagePath,ex);}
        }
        @Override public boolean isInteractable(){return true;}
        @Override public boolean onPlayerInteract(GamePanel gp,entity.Player player) {
            if(entry.type()==EntityPlacement.Type.DOCUMENT) {
                String document=entry.value("document","");
                gp.narrative.state.set("doc."+document);gp.narrative.reread(document);gp.saves.requestProgressSave();
            } else {
                int amount=Integer.parseInt(entry.value("mushrooms","10"));
                gp.entities.removeObject(this);MushroomPickup.scatter(gp,worldX+24,worldY+24,amount);gp.playSoundEffect(audio.Sound.CHEST);
            }
            return true;
        }
        @Override public void draw(java.awt.Graphics g,GamePanel gp) {
            if(image!=null){super.draw(g,gp);return;}
            int x=worldX-gp.player.worldX+gp.player.screenX,y=worldY-gp.player.worldY+gp.player.screenY;
            g.setColor(new java.awt.Color(232,217,166));g.fillRect(x+13,y+15,22,24);g.setColor(java.awt.Color.DARK_GRAY);g.drawRect(x+13,y+15,22,24);
        }
    }
}
