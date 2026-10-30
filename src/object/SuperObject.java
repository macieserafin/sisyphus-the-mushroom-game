package object;

import entity.Player;
import main.GamePanel;

import java.awt.*;
import java.awt.image.BufferedImage;

public class SuperObject {
    public String persistenceId = "";
    public java.util.Properties snapshot() { return null; }
    public void restoreSnapshot(java.util.Properties state) {}
    public boolean isInteractable() { return false; }
    public BufferedImage image;
    public String name;
    public boolean collision = false;
    public int worldX,worldY;
    public Rectangle solidArea = new Rectangle(0 , 0, 48, 48);
    public int solidAreaDefaultX, solidAreaDefaultY = 0;

    public void draw(Graphics g2, GamePanel gp) {
        BufferedImage frame = currentImage();
        if (frame == null) {
            return;
        }

        int screenX = worldX - gp.player.worldX + gp.player.screenX;
        int screenY = worldY - gp.player.worldY + gp.player.screenY;

        if (screenX + gp.tileSize > 0 && screenX < gp.screenWidth &&
                screenY + gp.tileSize > 0 && screenY < gp.screenHeight) {
            g2.drawImage(frame, screenX, screenY, gp.tileSize, gp.tileSize, null);
        }
    }

    protected BufferedImage currentImage() {
        return image;
    }

    public void update(GamePanel gp) {
    }

    public void addLights(world.Lighting lighting) {
    }

    public void onPlayerTouch(GamePanel gp, Player player, int objectIndex) {
    }

    public boolean onPlayerInteract(GamePanel gp, Player player) {
        return false;
    }
}
