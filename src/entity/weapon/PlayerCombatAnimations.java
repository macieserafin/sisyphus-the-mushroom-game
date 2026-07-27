package entity.weapon;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

public class PlayerCombatAnimations {
    private final BufferedImage upShoot;
    private final BufferedImage downShoot;
    private final BufferedImage leftShoot;
    private final BufferedImage rightShoot;
    private final BufferedImage upAttack1;
    private final BufferedImage downAttack1;
    private final BufferedImage leftAttack1;
    private final BufferedImage rightAttack1;
    private final BufferedImage upAttack2;
    private final BufferedImage downAttack2;
    private final BufferedImage leftAttack2;
    private final BufferedImage rightAttack2;

    private PlayerCombatAnimations(
            BufferedImage upShoot,
            BufferedImage downShoot,
            BufferedImage leftShoot,
            BufferedImage rightShoot,
            BufferedImage upAttack1,
            BufferedImage downAttack1,
            BufferedImage leftAttack1,
            BufferedImage rightAttack1,
            BufferedImage upAttack2,
            BufferedImage downAttack2,
            BufferedImage leftAttack2,
            BufferedImage rightAttack2
    ) {
        this.upShoot = upShoot;
        this.downShoot = downShoot;
        this.leftShoot = leftShoot;
        this.rightShoot = rightShoot;
        this.upAttack1 = upAttack1;
        this.downAttack1 = downAttack1;
        this.leftAttack1 = leftAttack1;
        this.rightAttack1 = rightAttack1;
        this.upAttack2 = upAttack2;
        this.downAttack2 = downAttack2;
        this.leftAttack2 = leftAttack2;
        this.rightAttack2 = rightAttack2;
    }

    public static PlayerCombatAnimations loadDefaultRanged() {
        return new PlayerCombatAnimations(
                load("player/combat/player_up_shoot.png"),
                load("player/combat/player_down_shoot.png"),
                load("player/combat/player_left_shoot.png"),
                load("player/combat/player_right_shoot.png"),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    public static PlayerCombatAnimations loadDefaultMelee() {
        return new PlayerCombatAnimations(
                null,
                null,
                null,
                null,
                load("player/combat/player_up_attack_1.png"),
                load("player/combat/player_down_attack_1.png"),
                load("player/combat/player_left_attack_1.png"),
                load("player/combat/player_right_attack_1.png"),
                load("player/combat/player_up_attack_2.png"),
                load("player/combat/player_down_attack_2.png"),
                load("player/combat/player_left_attack_2.png"),
                load("player/combat/player_right_attack_2.png")
        );
    }

    public BufferedImage shootImage(String direction) {
        switch (direction) {
            case "up": return upShoot;
            case "down": return downShoot;
            case "left": return leftShoot;
            case "right": return rightShoot;
            default: return null;
        }
    }

    public BufferedImage attackImage(String direction, boolean useSecondFrame) {
        switch (direction) {
            case "up": return useSecondFrame ? upAttack2 : upAttack1;
            case "down": return useSecondFrame ? downAttack2 : downAttack1;
            case "left": return useSecondFrame ? leftAttack2 : leftAttack1;
            case "right": return useSecondFrame ? rightAttack2 : rightAttack1;
            default: return null;
        }
    }

    private static BufferedImage load(String path) {
        try (InputStream stream = Objects.requireNonNull(
                PlayerCombatAnimations.class.getClassLoader().getResourceAsStream(path),
                "Missing player combat animation: " + path
        )) {
            return ImageIO.read(stream);
        } catch (IOException e) {
            throw new IllegalStateException("Could not load player combat animation: " + path, e);
        }
    }
}
