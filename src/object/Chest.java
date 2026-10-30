package object;

import entity.Player;
import main.GamePanel;
import main.Sound;
import story.QuestLog;
import world.EffectsManager;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.util.Objects;
import java.util.Random;

// Bumping into a chest opens it: either a power-up with a few mushrooms, or a big handful of mushrooms.
public class Chest extends SuperObject {
    private static final int SMALL_LOOT = 4;
    private static final int BIG_LOOT = 15;
    private static final Random RANDOM = new Random();

    public Chest() {
        name = "Chest";

        try{
            image = ImageIO.read(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("objects/chest.png")));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        collision = true;

    }

    @Override
    public void onPlayerTouch(GamePanel gp, Player player, int objectIndex) {
        double cx = worldX + gp.tileSize / 2.0;
        double groundY = worldY + gp.tileSize;
        gp.questLog.advance(QuestLog.CHESTS, 1);
        gp.entities.removeObject(this);
        gp.effects.burst(cx, groundY - 20, 20, 3, EffectsManager.GOLD);
        gp.effects.ring(cx, groundY - 10, 50, 14, EffectsManager.GOLD[1], true);
        gp.playSoundEffect(Sound.CHEST);

        if (RANDOM.nextBoolean()) {
            gp.entities.addFreeObject(PowerUp.random(cx, groundY));
            MushroomPickup.scatter(gp, cx, groundY, SMALL_LOOT);
        } else {
            MushroomPickup.scatter(gp, cx, groundY, BIG_LOOT);
        }
    }
}
