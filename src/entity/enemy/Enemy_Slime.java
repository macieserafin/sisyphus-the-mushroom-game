package entity.enemy;

import main.GamePanel;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Objects;

public class Enemy_Slime extends Enemy {

    public Enemy_Slime(GamePanel gp) {
        super(gp);

        setDefaultValues();
        getEnemyImage();
    }

    public void setDefaultValues() {
        direction = "idle";
        setupEnemyStats("Slime", 3, 1, 1);
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
        patrolRandomly();
    }
}
