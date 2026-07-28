package entity.enemy;

import main.GamePanel;

public class Enemy_SlimeKing extends Enemy_Slime {

    public Enemy_SlimeKing(GamePanel gp) {
        super(gp);
        setupEnemyStats("Slime King", 100, 1, 2);
    }

    @Override
    protected void onDeath() {
        super.onDeath();
        gp.levelManager.onBossDefeated();
    }
}
