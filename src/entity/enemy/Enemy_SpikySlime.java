package entity.enemy;

import entity.combat.HitType;
import main.GamePanel;

import java.util.Map;

// A red slime covered in bone spikes: blades barely scratch it, so it has to be shot.
public class Enemy_SpikySlime extends Enemy_Slime {
    private static final Map<Integer, Integer> PALETTE = Map.of(
            0xFF08270A, 0xFF28090B,
            0xFF0B4A10, 0xFF4C0D18,
            0xFF107416, 0xFF761325,
            0xFF17931A, 0xFF951B2C,
            0xFF24AE21, 0xFFAF2835,
            0xFF50C73B, 0xFFC9534E,
            0xFFCDEFB9, 0xFFF0CABD,
            0xFF08400C, 0xFF410E0A,
            0xFF07360A, 0xFF38090B
    );

    public Enemy_SpikySlime(GamePanel gp) {
        super(gp);
        name = "Spiky Slime";
        setupEnemyStats("Spiky Slime", 14, 1, 1);
        useVariant("spiky", PALETTE, true);
    }

    @Override
    protected double resistance(HitType type) {
        return type == HitType.MELEE ? 0.15 : 1.0;
    }
}
