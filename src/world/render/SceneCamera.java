package world.render;

import main.GamePanel;
import java.awt.Rectangle;

/** Overrides the origin only; all existing world layers retain their common transform. */
public final class SceneCamera {
    private final GamePanel gp;
    private double x, y, fromX, fromY, toX, toY;
    private int elapsed, duration;
    private boolean active;
    private String map = "";
    public SceneCamera(GamePanel gp) { this.gp = gp; }
    public boolean active() { return active; }
    public boolean moving() { return active && elapsed < duration; }
    public boolean fitsScreen() {
        int w=gp.tileM.getWorldCols()*gp.tileSize, h=gp.tileM.getWorldRows()*gp.tileSize;
        return w>0 && h>0 && w<=gp.screenWidth && h<=gp.screenHeight;
    }
    public double roomScale() {
        if(!fitsScreen())return 1;
        return Math.min(1,Math.min((gp.screenWidth-24.0)/(gp.tileM.getWorldCols()*gp.tileSize),
                (gp.screenHeight-roomTop()-roomBottom()-24.0)/(gp.tileM.getWorldRows()*gp.tileSize)));
    }
    private int roomTop() { return Math.max(0,60-gp.player.cameraDialogueInset()); }
    private int roomBottom() { return Math.max(48,gp.player.cameraDialogueInset()); }
    public void frameRoom(java.awt.Graphics2D g) {
        if(!fitsScreen())return;
        double scale=roomScale();
        g.translate(gp.screenWidth*(1-scale)/2,(gp.screenHeight-roomBottom()+roomTop()-gp.screenHeight*scale)/2);
        g.scale(scale,scale);
    }
    public void move(int centerX, int centerY, int frames) {
        if (!active) { x = gp.player.worldX - gp.player.screenX; y = gp.player.worldY - gp.player.screenY; }
        map = gp.tileM.currentMapPath(); active = true;
        fromX = x; fromY = y;
        toX = clamp(centerX - gp.screenWidth / 2.0, gp.tileM.getWorldCols() * gp.tileSize, gp.screenWidth);
        int visibleHeight=gp.screenHeight-gp.ui.dialogueReserve();
        toY = clamp(centerY - visibleHeight / 2.0, gp.tileM.getWorldRows() * gp.tileSize, visibleHeight);
        duration = gp.visuals.reduceMotion() ? 1 : Math.max(1, frames); elapsed = 0;
    }
    private double clamp(double n, int world, int view) { return world < view ? -(view - world) / 2.0 : Math.max(0, Math.min(n, world - view)); }
    public void tick() {
        if (!active) return;
        if (!map.equals(gp.tileM.currentMapPath())) { reset(); return; }
        if (elapsed < duration) {
            double t = ++elapsed / (double)duration; t = t * t * (3 - 2 * t);
            x = fromX + (toX - fromX) * t; y = fromY + (toY - fromY) * t;
        }
    }
    public void apply() {
        if (!active || fitsScreen()) return;
        int px = (int) Math.round(x), py = (int) Math.round(y);
        if (moving()) { int grid = Math.max(1, gp.scale); px = Math.round(px / (float) grid) * grid; py = Math.round(py / (float) grid) * grid; }
        gp.player.screenX = gp.player.worldX - px; gp.player.screenY = gp.player.worldY - py;
    }
    public void reset() { active = false; }
    public void returnToPlayer(int frames) {
        if(!active)return;
        double oldX=x,oldY=y;active=false;gp.player.updateCamera();
        double normalX=gp.player.worldX-gp.player.screenX,normalY=gp.player.worldY-gp.player.screenY;
        active=true;map=gp.tileM.currentMapPath();x=oldX;y=oldY;fromX=x;fromY=y;toX=normalX;toY=normalY;
        elapsed=0;duration=gp.visuals.reduceMotion()?1:Math.max(1,frames);
    }
    public Rectangle viewport() {
        return new Rectangle(gp.player.worldX - gp.player.screenX, gp.player.worldY - gp.player.screenY, gp.screenWidth, gp.screenHeight);
    }
}
