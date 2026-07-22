package entity.enemy;

import main.GamePanel;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Objects;

public class Enemy_Slime extends Enemy {
    private final int patrolSpeed = 1;
    private final int rushSpeed = 3;
    private final int detectionRangePixels = 3 * 48;
    private final int loseInterestRangePixels = 5 * 48;
    private boolean rushingPlayer = false;

    public Enemy_Slime(GamePanel gp) {
        super(gp);

        setDefaultValues();
        getEnemyImage();
    }

    public void setDefaultValues() {
        direction = "idle";
        setupEnemyStats("Slime", 3, patrolSpeed, 1);
        setupCollisionAreas(
                new Rectangle(9, 29, 28, 20),
                new Rectangle(6, 26, 34, 26)
        );
    }

    public void getEnemyImage() {
        try {
            BufferedImage enemyImage = ImageIO.read(
                    Objects.requireNonNull(
                            getClass().getClassLoader().getResourceAsStream("enemy/enemy.png")
                    )
            );

            up1 = up2 = down1 = down2 = left1 = left2 = right1 = right2 = enemyImage;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setAction() {
        updateRushState();

        if (rushingPlayer) {
            rushTowardPlayer();
        }
        else {
            speed = patrolSpeed;
            patrolRandomly();
        }
    }

    private void updateRushState() {
        double distanceToPlayer = getDistanceToPlayer();

        if (!rushingPlayer && distanceToPlayer <= detectionRangePixels) {
            rushingPlayer = true;
        }

        if (rushingPlayer && distanceToPlayer > loseInterestRangePixels) {
            rushingPlayer = false;
            direction = "idle";
            actionLockCounter = 0;
        }
    }

    private double getDistanceToPlayer() {
        int slimeCenterX = worldX + gp.tileSize / 2;
        int slimeCenterY = worldY + gp.tileSize / 2;
        int playerCenterX = gp.player.worldX + gp.tileSize / 2;
        int playerCenterY = gp.player.worldY + gp.tileSize / 2;

        int dx = playerCenterX - slimeCenterX;
        int dy = playerCenterY - slimeCenterY;

        return Math.sqrt(dx * dx + dy * dy);
    }

    private void rushTowardPlayer() {
        speed = rushSpeed;

        int playerCenterX = gp.player.worldX + gp.tileSize / 2;
        int playerCenterY = gp.player.worldY + gp.tileSize / 2;

        moveToward(playerCenterX, playerCenterY, rushSpeed);
    }
}
