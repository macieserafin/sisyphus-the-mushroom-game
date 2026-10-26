package entity.enemy;

import entity.combat.Hit;
import entity.combat.HitResult;
import main.GamePanel;
import story.StorySequences;

import java.awt.*;

public class Enemy_BYT extends Enemy_Slime {
    private static final int MAX_HEALTH = 1000;
    private static final int MOVE_SPEED = 2;
    private static final int ATTACK_DAMAGE = 2;
    private boolean scriptedDefeatStarted = false;

    public Enemy_BYT(GamePanel gp) {
        super(gp);
        setupEnemyStats("BYT", MAX_HEALTH, MOVE_SPEED, ATTACK_DAMAGE);
        setupDrawArea(gp.tileSize * 2, gp.tileSize * 2, -gp.tileSize / 2, -gp.tileSize);
        setupCollisionAreas(
                new Rectangle(-10, 0, 68, 42),
                new Rectangle(-12, -27, 72, 63)
        );
    }

    @Override
    public HitResult receiveHit(Hit hit) {
        HitResult result = super.receiveHit(hit);
        if (result == HitResult.APPLIED && !scriptedDefeatStarted && health > 0 && health <= maxHealth * 9 / 10) {
            scriptedDefeatStarted = true;
            gp.storySequenceManager.start(StorySequences.prologueBytScriptedDefeat(gp));
        }
        return result;
    }

    @Override
    public void setAction() {
        Rectangle playerHitBox = gp.player.getWorldHitBox();
        int playerCenterX = playerHitBox.x + playerHitBox.width / 2;
        int playerCenterY = playerHitBox.y + playerHitBox.height / 2;
        moveToward(playerCenterX, playerCenterY, MOVE_SPEED);
    }
}
