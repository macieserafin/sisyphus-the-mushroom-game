package object;

import entity.Player;
import main.GamePanel;
import main.Sound;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.util.Objects;

public class TutorialSword extends SuperObject {
    public TutorialSword() {
        name = "Sword";

        try {
            image = ImageIO.read(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("objects/sword.png")));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onPlayerTouch(GamePanel gp, Player player, int objectIndex) {
        if (!gp.prologueTutorialManager.canPickUpTutorialSword(this)) {
            return;
        }

        gp.entities.removeObject(this);
        gp.playSoundEffect(Sound.POWERUP);
        gp.prologueTutorialManager.onTutorialSwordPickedUp(this);
    }
}
