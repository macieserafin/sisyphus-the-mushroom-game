package entity;

import main.GamePanel;

import java.awt.*;
import java.awt.image.BufferedImage;

public class Entity {
    protected GamePanel gp;

    public int worldX,worldY;
    public int speed;

    public BufferedImage up1, up2, down1, down2, left1, left2, right1, right2, idle1, idle2;
    public String direction;


    public int spriteCounter = 0;
    public int spriteNum = 1;
    public Rectangle solidArea = new Rectangle(0 , 0, 48, 48);;

    public int solidAreaDefaultX, solidAreaDefaultY;
    public boolean collisionOn = false;

    public Rectangle hitBox;

    public int maxHealth = 1;
    public int health = 1;
    public boolean alive = true;

    public int actionLockCounter = 0;

    protected String[] dialogues = new String[20];
    protected int dialogueIndex = 0;

    public Entity(GamePanel gp) {
        this.gp = gp;
    }

    public Rectangle getWorldSolidArea() {
        return new Rectangle(
                worldX + solidArea.x,
                worldY + solidArea.y,
                solidArea.width,
                solidArea.height
        );
    }

    public Rectangle getWorldHitBox() {
        Rectangle box = (hitBox != null) ? hitBox : solidArea;

        return new Rectangle(
                worldX + box.x,
                worldY + box.y,
                box.width,
                box.height
        );
    }

    public boolean isOutsideWorld() {
        Rectangle bounds = getWorldSolidArea();
        int worldWidth = gp.maxWorldCol * gp.tileSize;
        int worldHeight = gp.maxWorldRow * gp.tileSize;

        return bounds.x + bounds.width < 0 ||
                bounds.x >= worldWidth ||
                bounds.y + bounds.height < 0 ||
                bounds.y >= worldHeight;
    }

    public void takeDamage(int damage) {
        health -= damage;

        if (health <= 0) {
            health = 0;
            alive = false;
        }
    }

    public void draw(Graphics g2) {
        if (direction == null) direction = "down";

        BufferedImage image = null;

        int screenX = worldX - gp.player.worldX + gp.player.screenX;
        int screenY = worldY - gp.player.worldY + gp.player.screenY;

        if (worldX + gp.tileSize > gp.player.worldX - gp.player.screenX &&
                worldX - gp.tileSize < gp.player.worldX + gp.player.screenX &&
                worldY + gp.tileSize > gp.player.worldY - gp.player.screenY &&
                worldY - gp.tileSize < gp.player.worldY + gp.player.screenY) {

            switch (direction) {
                case "idle":
                    image = down1;
                    break;
                case "up":
                    image = (spriteNum == 1) ? up1 : up2;
                    break;
                case "left":
                    image = (spriteNum == 1) ? left1 : left2;
                    break;
                case "right":
                    image = (spriteNum == 1) ? right1 : right2;
                    break;
                case "down":
                default:
                    image = (spriteNum == 1) ? down1 : down2;
                    break;
            }

            if (image != null) {
                g2.drawImage(image, screenX, screenY, gp.tileSize, gp.tileSize, null);
            }

            Color oldColor = g2.getColor();

            g2.setColor(Color.GREEN);
            g2.drawRect(
                    screenX + solidArea.x,
                    screenY + solidArea.y,
                    solidArea.width,
                    solidArea.height
            );

            Rectangle worldHitBox = getWorldHitBox();
            int hitBoxScreenX = worldHitBox.x - gp.player.worldX + gp.player.screenX;
            int hitBoxScreenY = worldHitBox.y - gp.player.worldY + gp.player.screenY;

            g2.setColor(Color.RED);
            g2.drawRect(hitBoxScreenX, hitBoxScreenY, worldHitBox.width, worldHitBox.height);

            g2.setColor(oldColor);
        }
    }

    public void setAction(){}
    public void speak(){
        gp.ui.currentDialogue = dialogues[dialogueIndex];
        if (gp.ui.currentDialogue == null) {
            gp.ui.currentDialogue = "";
        }
        dialogueIndex++;
        if (dialogueIndex >= dialogues.length) {
            dialogueIndex = 0;
        }

        switch(gp.player.direction) {
            case "up":
                direction = "down";
                break;
            case "down":
                direction = "up";
                break;
            case "left":
                direction = "right";
                break;
            case "right":
                direction = "left";
                break;
        }
    }

    public void update(){
        setAction();

        collisionOn = false;
        gp.cChecker.checkTile(this);
        gp.cChecker.checkObject(this, false);
        gp.cChecker.checkPlayer(this);

        if(!collisionOn){
            switch (direction) {
                case "up":    worldY -= speed; break;
                case "down":  worldY += speed; break;
                case "left":  worldX -= speed; break;
                case "right": worldX += speed; break;
                case "idle":
                default:
                    break;
            }
        }
        spriteCounter++;
        if(spriteCounter > 12){
            if(spriteNum == 1){
                spriteNum = 2;
            }
            else if(spriteNum == 2){
                spriteNum = 1;
            }
            spriteCounter = 0;
        }

    }

}
