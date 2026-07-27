package entity.enemy;

import entity.combat.Faction;
import entity.combat.Hit;
import entity.combat.HitResult;
import entity.combat.HitType;
import main.GamePanel;

import java.awt.Rectangle;

public class Enemy_Reflector extends Enemy_Slime {
    public Enemy_Reflector(GamePanel gp) {
        super(gp);
        name = "Reflector";
        setupCollisionAreas(
                new Rectangle(9, 29, 28, 20),
                new Rectangle(4, 22, 40, 30)
        );
    }

    @Override
    public HitResult receiveHit(Hit hit) {
        if (hit.type() == HitType.RANGED && hit.projectile() != null) {
            hit.projectile().reflectFrom(this, Faction.ENEMY);
            return HitResult.REFLECTED;
        }
        return super.receiveHit(hit);
    }
}
