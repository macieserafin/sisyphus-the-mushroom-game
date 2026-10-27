package entity.weapon;

import entity.anim.SpriteSheet;

public class PlayerCombatAnimations {
    private final SpriteSheet shoot;
    private final SpriteSheet attack;

    private PlayerCombatAnimations(SpriteSheet shoot, SpriteSheet attack) {
        this.shoot = shoot;
        this.attack = attack;
    }

    public static PlayerCombatAnimations loadDefaultRanged() {
        return new PlayerCombatAnimations(SpriteSheet.load("player/anim/player_shoot"), null);
    }

    public static PlayerCombatAnimations loadDefaultMelee() {
        return new PlayerCombatAnimations(null, SpriteSheet.load("player/anim/player_melee"));
    }

    public SpriteSheet shoot() {
        return shoot;
    }

    public SpriteSheet attack() {
        return attack;
    }
}
