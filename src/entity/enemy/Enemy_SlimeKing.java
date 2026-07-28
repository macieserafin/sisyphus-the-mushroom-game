package entity.enemy;

import entity.combat.Hit;
import entity.combat.HitResult;
import main.GamePanel;

import java.awt.*;

public class Enemy_SlimeKing extends Enemy_Slime {
    private static final int ENRAGED_SPEED = 4;
    private static final int ENRAGED_ATTACK_DAMAGE = 3;

    private boolean enraged = false;

    public Enemy_SlimeKing(GamePanel gp) {
        super(gp);
        setupEnemyStats("Slime King", 100, 1, 2);
        setupDrawArea(gp.tileSize * 2, gp.tileSize * 2, -gp.tileSize / 2, -gp.tileSize);
        setupCollisionAreas(
                new Rectangle(-10, 0, 68, 42),
                new Rectangle(-12, -27, 72, 63)
        );
    }

    @Override
    public HitResult receiveHit(Hit hit) {
        HitResult result = super.receiveHit(hit);

        if (result == HitResult.APPLIED && !enraged && health > 0 && health <= maxHealth / 2) {
            becomeEnraged();
        }

        return result;
    }

    @Override
    public void setAction() {
        if (!enraged) {
            super.setAction();
            return;
        }

        speed = ENRAGED_SPEED;
        Rectangle playerHitBox = gp.player.getWorldHitBox();
        int playerCenterX = playerHitBox.x + playerHitBox.width / 2;
        int playerCenterY = playerHitBox.y + playerHitBox.height / 2;
        moveToward(playerCenterX, playerCenterY, ENRAGED_SPEED);
    }

    private void becomeEnraged() {
        enraged = true;
        attackDamage = ENRAGED_ATTACK_DAMAGE;
        gp.ui.showMessage("Slime King enraged!");
    }

    @Override
    protected void onDeath() {
        super.onDeath();
        gp.levelManager.onBossDefeated();
    }
}
