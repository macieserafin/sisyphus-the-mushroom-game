package entity;

import main.GamePanel;
import entity.combat.Hit;
import entity.combat.HitResult;

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
    protected int drawWidth;
    protected int drawHeight;
    protected int drawOffsetX = 0;
    protected int drawOffsetY = 0;

    public int solidAreaDefaultX, solidAreaDefaultY;
    public boolean collisionOn = false;

    public Rectangle hitBox;

    public int maxHealth = 1;
    public int health = 1;
    public boolean alive = true;

    public int actionLockCounter = 0;

    public Entity(GamePanel gp) {
        this.gp = gp;
        drawWidth = gp.tileSize;
        drawHeight = gp.tileSize;
    }

    public GamePanel gp() {
        return gp;
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
        int worldWidth = gp.tileM.getWorldCols() * gp.tileSize;
        int worldHeight = gp.tileM.getWorldRows() * gp.tileSize;

        return bounds.x + bounds.width < 0 ||
                bounds.x >= worldWidth ||
                bounds.y + bounds.height < 0 ||
                bounds.y >= worldHeight;
    }

    protected void setupDrawArea(int drawWidth, int drawHeight, int drawOffsetX, int drawOffsetY) {
        if (drawWidth <= 0 || drawHeight <= 0) {
            throw new IllegalArgumentException("Entity draw size must be positive");
        }

        this.drawWidth = drawWidth;
        this.drawHeight = drawHeight;
        this.drawOffsetX = drawOffsetX;
        this.drawOffsetY = drawOffsetY;
    }

    protected int getDrawScreenX() {
        return worldX - gp.player.worldX + gp.player.screenX + drawOffsetX;
    }

    protected int getDrawScreenY() {
        return worldY - gp.player.worldY + gp.player.screenY + drawOffsetY;
    }

    public void takeDamage(int damage) {
        health -= damage;

        if (health <= 0) {
            health = 0;
            alive = false;
        }
    }

    public HitResult receiveHit(Hit hit) {
        if (!alive) {
            return HitResult.IGNORED;
        }
        takeDamage(hit.damage());
        return HitResult.APPLIED;
    }

    public void draw(Graphics g2) {
        if (direction == null) direction = "down";

        BufferedImage image = null;

        int screenX = worldX - gp.player.worldX + gp.player.screenX;
        int screenY = worldY - gp.player.worldY + gp.player.screenY;
        int drawScreenX = getDrawScreenX();
        int drawScreenY = getDrawScreenY();

        if (drawScreenX + drawWidth > 0 &&
                drawScreenX < gp.screenWidth &&
                drawScreenY + drawHeight > 0 &&
                drawScreenY < gp.screenHeight) {

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
                g2.drawImage(image, drawScreenX, drawScreenY, drawWidth, drawHeight, null);
            }

            if (gp.debugSettings.isOverlayEnabled()) {
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
    }

    public void setAction(){}
    public void speak(){
        facePlayer();
    }

    protected void facePlayer() {
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
