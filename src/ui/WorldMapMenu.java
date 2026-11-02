package ui;

import main.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import settings.ControlSettings.Action;
import java.awt.image.BufferedImage;

/** Discovery and objectives are separate layers: a known destination does not reveal its route. */
public final class WorldMapMenu {
    private final GamePanel gp;
    private double zoom=1,panX,panY;
    private boolean all;
    private BufferedImage terrain;
    public WorldMapMenu(GamePanel gp) {
        this.gp=gp;
        gp.addMouseWheelListener(event -> {
            synchronized (gp.frameLock) {
                if (gp.gameState == GameState.MAP) changeZoom(-event.getPreciseWheelRotation());
            }
        });
    }
    public void open() {
        if(!gp.player.alive||gp.campaign.rebirthing())return;
        if(gp.storySequenceManager.isActive()||gp.narrative.busy()||gp.narrativeView.modal())return;
        gp.player.updateCamera();
        gp.narrative.discoverViewport();
        zoom=2;panX=panY=0; rebuild();centerPlayer();gp.keyH.clearGameplayInput();gp.gameState=GameState.MAP;
    }
    private void rebuild() {
        int cols=gp.tileM.getWorldCols(), rows=gp.tileM.getWorldRows();
        if(cols==0||rows==0)return;
        terrain=new BufferedImage(cols*4,rows*4,BufferedImage.TYPE_INT_RGB);
        Graphics2D g=terrain.createGraphics();
        for(int x=0;x<cols;x++)for(int y=0;y<rows;y++) {
            g.drawImage(gp.tileM.tile[gp.tileM.mapTileNum[x][y]].image,x*4,y*4,4,4,null);
            int id=gp.tileM.objectTileNum[x][y];
            if(id>=0)g.drawImage(gp.tileM.tile[id].image,x*4,y*4,4,4,null);
        }
        g.dispose();
    }
    public void key(int code) {
        if(code==KeyEvent.VK_ESCAPE||code==gp.controls.key(settings.ControlSettings.Action.MAP)) {gp.gameState=GameState.PLAYING;gp.keyH.clearGameplayInput();return;}
        if(code==gp.controls.key(Action.JOURNAL)) {gp.questMenu.open();return;}
        if(code==gp.controls.key(Action.NOTES)) {gp.questMenu.openNotes();return;}
        if(code==gp.controls.key(Action.INVENTORY)) {gp.gameState=GameState.PLAYING;gp.relics.open();return;}
        switch(code) {
            case KeyEvent.VK_ADD,KeyEvent.VK_EQUALS -> changeZoom(1);
            case KeyEvent.VK_SUBTRACT,KeyEvent.VK_MINUS -> changeZoom(-1);
            case KeyEvent.VK_W,KeyEvent.VK_UP -> panY+=35;
            case KeyEvent.VK_S,KeyEvent.VK_DOWN -> panY-=35;
            case KeyEvent.VK_A,KeyEvent.VK_LEFT -> panX+=35;
            case KeyEvent.VK_D,KeyEvent.VK_RIGHT -> panX-=35;
            case KeyEvent.VK_SPACE -> centerPlayer();
            case KeyEvent.VK_HOME -> {zoom=1;panX=panY=0;}
            default -> { }
        }
        clampPan();
    }

    private double tileScale() {
        return Math.min((gp.screenWidth-60.0)/Math.max(1,gp.tileM.getWorldCols()),
                (gp.screenHeight-155.0)/Math.max(1,gp.tileM.getWorldRows()))*zoom;
    }
    private void changeZoom(double steps) {
        double previous=zoom;
        zoom=Math.max(1,Math.min(5,zoom*Math.pow(1.25,steps)));
        panX*=zoom/previous;panY*=zoom/previous;clampPan();
    }
    private void centerPlayer() {
        double size=tileScale();
        panX=(gp.tileM.getWorldCols()/2.0-(gp.player.worldX/(double)gp.tileSize+.5))*size;
        panY=(gp.tileM.getWorldRows()/2.0-(gp.player.worldY/(double)gp.tileSize+.5))*size;
        clampPan();
    }
    private void clampPan() {
        double size=tileScale();
        double maxX=Math.max(0,(gp.tileM.getWorldCols()*size-(gp.screenWidth-40))/2);
        double maxY=Math.max(0,(gp.tileM.getWorldRows()*size-(gp.screenHeight-145))/2);
        panX=Math.max(-maxX,Math.min(maxX,panX));
        panY=Math.max(-maxY,Math.min(maxY,panY));
    }
    public void debugAll() {gp.forest.beginPreview();all=!all;open();}
    public boolean all() {return all;}
    public void resetPreview() {all=false;}
    public void draw(Graphics2D target) {
        Graphics2D g=(Graphics2D)target.create();
        g.setColor(new Color(8,14,20));g.fillRect(0,0,gp.screenWidth,gp.screenHeight);
        g.setFont(UiFonts.of(Font.BOLD,21));g.setColor(UiSkin.GOLD);g.drawString("MAP  " + Math.round(zoom*100) + "%",26,35);
        g.setFont(UiFonts.of(Font.PLAIN,12));g.setColor(UiSkin.TEXT_DIM);
        g.drawString("Gold: main objective   Blue: resident   White: you   Dark: unexplored",26,58);
        if(terrain==null){g.dispose();return;}
        int cols=gp.tileM.getWorldCols(),rows=gp.tileM.getWorldRows();
        double size=tileScale();
        double ox=(gp.screenWidth-cols*size)/2+panX,oy=(gp.screenHeight-5-rows*size)/2+panY;
        g.clipRect(20,70,gp.screenWidth-40,gp.screenHeight-145);
        String map=gp.tileM.currentMapPath();
        for(int x=0;x<cols;x++)for(int y=0;y<rows;y++) {
            int sx=(int)Math.floor(ox+x*size),sy=(int)Math.floor(oy+y*size),ex=(int)Math.ceil(ox+(x+1)*size),ey=(int)Math.ceil(oy+(y+1)*size);
            if(all||gp.narrative.mapKnown(map,x,y,cols))g.drawImage(terrain,sx,sy,ex,ey,x*4,y*4,x*4+4,y*4+4,null);
            else {g.setColor(new Color(19,25,33));g.fillRect(sx,sy,ex-sx,ey-sy);}
        }
        java.util.Map<String,story.NarrativeDirector.Marker> grouped=new java.util.LinkedHashMap<>();
        for(var marker:gp.narrative.mainQuestMarkers()) {
            String key=marker.map()+":"+marker.x()+":"+marker.y();
            grouped.merge(key,marker,(a,b)->new story.NarrativeDirector.Marker(
                    a.label().equals(b.label())?a.label():a.label()+" / "+b.label(),a.map(),a.x(),a.y(),a.npc()||b.npc()));
        }
        var markers=grouped.values();
        for(var marker:markers)if(marker.map().equals(map))point(g,ox,oy,size,marker.x(),marker.y(),UiSkin.GOLD,"! "+marker.label());
        for(var npc:gp.entities.npcs()) {
            int x=npc.worldX/gp.tileSize,y=npc.worldY/gp.tileSize;
            if(markers.stream().anyMatch(m->m.npc()&&m.map().equals(map)&&m.x()==x&&m.y()==y))continue;
            if(all||gp.narrative.mapKnown(map,x,y,cols))point(g,ox,oy,size,x,y,new Color(115,204,228),npc.name);
        }
        // Interior residents are represented at the entrance while viewing the settlement.
        for(var room:level.Interior.values())if(!room.mapPath.equals(map)&&map.equals(gp.narrative.get("settlement.map"))) {
            int x=room.building.doorCol(),y=room.building.doorRow();
            if(markers.stream().anyMatch(m->m.map().equals(map)&&m.x()==x&&m.y()==y))continue;
            if(all||gp.narrative.state.has("visit."+room.name()))point(g,ox,oy,size,x,y,new Color(115,204,228),gp.narrative.get("room."+room.name()+".label"));
        }
        point(g,ox,oy,size,gp.player.worldX/gp.tileSize,gp.player.worldY/gp.tileSize,Color.WHITE,"YOU");
        g.setClip(null);g.setFont(UiFonts.of(Font.PLAIN,12));g.setColor(UiSkin.TEXT);
        g.drawString("WASD / arrows: pan   Wheel / +/-: zoom   SPACE: find me   HOME: full map",26,gp.screenHeight-40);
        g.drawString(gp.controls.keyName(Action.MAP)+" / ESC: close   "+gp.controls.keyName(Action.JOURNAL)+": journal   "+gp.controls.keyName(Action.NOTES)+": notes   "+gp.narrative.state.time()+ (all?"  DEVELOPER PREVIEW":""),26,gp.screenHeight-20);
        g.dispose();
    }
    private void point(Graphics2D g,double ox,double oy,double size,int x,int y,Color color,String text) {
        int px=(int)(ox+(x+.5)*size),py=(int)(oy+(y+.5)*size);
        g.setColor(Color.BLACK);g.fillOval(px-5,py-5,10,10);g.setColor(color);g.fillOval(px-3,py-3,6,6);
        g.setFont(UiFonts.of(Font.BOLD,11));g.setColor(Color.BLACK);g.drawString(text,px+8,py+1);g.setColor(color);g.drawString(text,px+7,py);
    }
}
