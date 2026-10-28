package entity.weapon;

import entity.anim.SpriteSheet;

public class PlayerCombatAnimations {
    private final String shoot;
    private final String attack;

    private PlayerCombatAnimations(String shoot, String attack) {
        this.shoot = shoot;
        this.attack = attack;
    }

    public static PlayerCombatAnimations loadDefaultRanged() {
        return new PlayerCombatAnimations("player/anim/player_shoot", null);
    }

    public static PlayerCombatAnimations loadDefaultMelee() {
        return new PlayerCombatAnimations(null, "player/anim/player_attack");
    }

    public SpriteSheet shoot() {
        return shoot == null ? null : SpriteSheet.load(shoot);
    }

    public SpriteSheet attack() {
        return attack == null ? null : SpriteSheet.load(attack);
    }
}
