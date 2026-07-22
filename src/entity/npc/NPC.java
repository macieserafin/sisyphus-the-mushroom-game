package entity.npc;

import entity.Entity;
import main.GamePanel;

import java.awt.*;

public class NPC extends Entity {

    public String name;

    public NPC(GamePanel gp) {
        super(gp);
    }

    protected void setupNPCStats(String name, int speed, String direction) {
        this.name = name;
        this.speed = speed;
        this.direction = direction;
    }

    protected void setupCollisionAreas(Rectangle solidArea, Rectangle hitBox) {
        this.solidArea = solidArea;
        this.solidAreaDefaultX = solidArea.x;
        this.solidAreaDefaultY = solidArea.y;
        this.hitBox = hitBox;
    }

    protected void setDialogueLine(int index, String text) {
        if (index >= 0 && index < dialogues.length) {
            dialogues[index] = text;
        }
    }
}
