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
        mushroomValue = 2;
        // steel-blue jelly: it throws bullets back, so it has to be cut
        useVariant("reflector", java.util.Map.of(
                0xFF08270A, 0xFF081C28,
                0xFF0B4A10, 0xFF0C364B,
                0xFF107416, 0xFF115375,
                0xFF17931A, 0xFF187294,
                0xFF24AE21, 0xFF2591AE,
                0xFF50C73B, 0xFF50B8C7,
                0xFFCDEFB9, 0xFFCDEAEF,
                0xFF08400C, 0xFF092B41,
                0xFF07360A, 0xFF082737
        ), false);
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
