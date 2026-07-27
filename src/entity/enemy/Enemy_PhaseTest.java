package entity.enemy;

import entity.combat.Hit;
import entity.combat.HitResult;
import main.GamePanel;

public class Enemy_PhaseTest extends Enemy_Slime {
    private boolean enraged = false;

    public Enemy_PhaseTest(GamePanel gp) {
        super(gp);
        name = "PhaseTest";
    }

    @Override
    public HitResult receiveHit(Hit hit) {
        HitResult result = super.receiveHit(hit);
        if (!enraged && health > 0 && health <= maxHealth / 2) {
            enraged = true;
        }
        return result;
    }

    @Override
    public void setAction() {
        if (!enraged) {
            super.setAction();
            return;
        }

        speed = 5;
        int playerCenterX = gp.player.worldX + gp.tileSize / 2;
        int playerCenterY = gp.player.worldY + gp.tileSize / 2;
        moveToward(playerCenterX, playerCenterY, speed);
    }

    public boolean isEnraged() {
        return enraged;
    }
}
