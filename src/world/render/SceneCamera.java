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
    public void move(int centerX, int centerY, int frames) {
        if (!active) { x = gp.player.worldX - gp.player.screenX; y = gp.player.worldY - gp.player.screenY; }
        map = gp.tileM.currentMapPath(); active = true;
        fromX = x; fromY = y;
        toX = clamp(centerX - gp.screenWidth / 2.0, gp.tileM.getWorldCols() * gp.tileSize, gp.screenWidth);
        toY = clamp(centerY - gp.screenHeight / 2.0, gp.tileM.getWorldRows() * gp.tileSize, gp.screenHeight);
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
        if (active) { gp.player.screenX = gp.player.worldX - (int)Math.round(x); gp.player.screenY = gp.player.worldY - (int)Math.round(y); }
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
