package entity.enemy;

import entity.Entity;
import main.GamePanel;

import java.awt.*;
import java.util.Random;

public class Enemy extends Entity {

    public String name;
    public int attackDamage = 1;
    private int walkPixelsLeft = 0;
    private int contactDamageCooldownCounter = 0;
    private final int contactDamageCooldownFrames = 60;
    private final Random random = new Random();

    public Enemy(GamePanel gp) {
        super(gp);
    }

    protected void setupEnemyStats(String name, int maxHealth, int speed, int attackDamage) {
        this.name = name;
        this.maxHealth = maxHealth;
        this.health = maxHealth;
        this.speed = speed;
        this.attackDamage = attackDamage;
        this.alive = true;
    }

    protected void setupCollisionAreas(Rectangle solidArea, Rectangle hitBox) {
        this.solidArea = solidArea;
        this.solidAreaDefaultX = solidArea.x;
        this.solidAreaDefaultY = solidArea.y;
        this.hitBox = hitBox;
    }

    @Override
    public void update() {
        super.update();
        updateContactDamage();
    }

    @Override
    public void setAction() {
        patrolRandomly();
    }

    protected void updateContactDamage() {
        if (contactDamageCooldownCounter > 0) {
            contactDamageCooldownCounter--;
        }

        if (!gp.player.alive || gp.player.isHurtInvincible()) {
            return;
        }

        if (contactDamageCooldownCounter == 0 && getWorldSolidArea().intersects(gp.player.getWorldHitBox())) {
            gp.player.takeDamage(attackDamage);
            gp.player.applyKnockbackFrom(worldX + gp.tileSize / 2, worldY + gp.tileSize / 2, 7.0);
            contactDamageCooldownCounter = contactDamageCooldownFrames;
        }
    }

    protected void patrolRandomly() {
        if (walkPixelsLeft > 0) {
            walkPixelsLeft -= speed;

            if (walkPixelsLeft <= 0) {
                walkPixelsLeft = 0;
                direction = "idle";
                actionLockCounter = 0;
            }
            return;
        }

        actionLockCounter++;

        if (actionLockCounter >= 90) {
            if (random.nextInt(100) < 35) {
                direction = "idle";
            }
            else {
                int dir = random.nextInt(4);

                if (dir == 0) direction = "up";
                if (dir == 1) direction = "down";
                if (dir == 2) direction = "left";
                if (dir == 3) direction = "right";

                int tilesToWalk = random.nextInt(2) + 1;
                walkPixelsLeft = tilesToWalk * gp.tileSize;
            }

            actionLockCounter = 0;
        }
    }

    @Override
    public void draw(Graphics g2) {
        super.draw(g2);
        drawHealthBar(g2);
    }

    protected void drawHealthBar(Graphics g2) {
        if (maxHealth <= 0) {
            return;
        }

        int screenX = worldX - gp.player.worldX + gp.player.screenX;
        int screenY = worldY - gp.player.worldY + gp.player.screenY;
        int barWidth = 32;
        int barHeight = 4;
        int filledWidth = barWidth * health / maxHealth;

        Color oldColor = g2.getColor();

        g2.setColor(Color.RED);
        g2.fillRect(screenX + 8, screenY - 8, barWidth, barHeight);

        g2.setColor(Color.GREEN);
        g2.fillRect(screenX + 8, screenY - 8, filledWidth, barHeight);

        g2.setColor(oldColor);
    }
}
