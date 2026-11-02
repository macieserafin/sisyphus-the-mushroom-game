package object;

import entity.Player;
import main.GamePanel;
import story.ChapterText;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;

public final class ForestObject extends SuperObject {
    public enum Kind { ORE, DOCUMENTS, SUPPLIES, RIDGE }
    private final Kind kind;
    private final String id;
    private final BufferedImage normal,changed;
    public ForestObject(Kind kind,String id){
        this.kind=kind;this.id=id;name=ChapterText.get("forest.object."+kind.name().toLowerCase());
        String file=switch(kind){case ORE->"tiles/big/iron_deposit";case DOCUMENTS->"tiles/papers_floor";case SUPPLIES->"tiles/crate_obj";case RIDGE->"objects/ridge_log";};
        normal=read(file+".png");changed=kind==Kind.ORE?read("tiles/big/iron_deposit_depleted.png"):kind==Kind.RIDGE?read("objects/ridge_log_open.png"):normal;
        collision=kind==Kind.ORE||kind==Kind.RIDGE;
        solidArea=new Rectangle(0,0,kind==Kind.ORE?96:kind==Kind.RIDGE?240:48,kind==Kind.ORE?96:48);
    }
    private static BufferedImage read(String path){try(var in=ForestObject.class.getResourceAsStream("/"+path)){if(in==null)throw new IllegalStateException("Missing "+path);return ImageIO.read(in);}catch(java.io.IOException e){throw new IllegalStateException(e);}}
    private BufferedImage taken;
    private BufferedImage taken(){if(taken==null)taken=read("objects/papers_taken.png");return taken;}
    @Override public boolean isInteractable(){return true;}
    @Override public double interactionDistance(double x,double y,int tileSize){
        if(kind!=Kind.ORE&&kind!=Kind.RIDGE)return super.interactionDistance(x,y,tileSize);
        double xx=Math.max(worldX,Math.min(worldX+solidArea.width,x)),yy=Math.max(worldY,Math.min(worldY+solidArea.height,y));return Math.hypot(xx-x,yy-y);
    }
    @Override public void update(GamePanel gp){if(kind==Kind.RIDGE)collision=!gp.forest.shortcut();}
    public String interactionLabel(GamePanel gp){return "ENTER: "+name;}
    @Override public boolean onPlayerInteract(GamePanel gp,Player player){
        if(gp.campaign.inCombat()){gp.ui.showMessage(ChapterText.get("forest.combat"));return true;}
        switch(kind){case ORE->gp.forest.mine(id,worldX+solidArea.width/2,worldY+solidArea.height/2);case DOCUMENTS->gp.forest.readDocuments();case SUPPLIES->gp.forest.takeSupplies();case RIDGE->{gp.forest.openShortcut();collision=!gp.forest.shortcut();}}
        return true;
    }
    @Override public void draw(Graphics g,GamePanel gp){
        boolean used=kind==Kind.ORE?gp.forest.mined(id):kind==Kind.RIDGE&&gp.forest.shortcut();
        if(kind==Kind.SUPPLIES&&gp.forest.supplies()&&!gp.narrative.enabled())return;
        BufferedImage frame=used?changed:normal;
        if(kind==Kind.DOCUMENTS&&gp.forest.documents()&&gp.narrative.enabled())frame=taken();
        g.drawImage(frame,worldX-gp.player.worldX+gp.player.screenX,worldY-gp.player.worldY+gp.player.screenY,frame.getWidth()*3,frame.getHeight()*3,null);
    }
}
