package object;

import entity.Player;
import main.GamePanel;
import story.QuestLog;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.util.Objects;

public class Door extends SuperObject {
    private final boolean keyNeeded;

    public Door() {
        this(true);
    }

    public Door(boolean keyNeeded) {
        this.keyNeeded = keyNeeded;
        name = "Door";

        try{
            image = ImageIO.read(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("objects/door.png")));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        collision = true;
    }

    @Override
    public void onPlayerTouch(GamePanel gp, Player player, int objectIndex) {
        if (keyNeeded && player.hasKey == 0) {
            gp.ui.showMessage("You need a key!");
            return;
        }

        if (keyNeeded) {
            player.hasKey--;
            gp.questLog.complete(QuestLog.GROTTO);
        }

        gp.entities.removeObject(this);
        gp.playSoundEffect(2);
        gp.playSoundEffect(main.Sound.DOOR);
    }

    public boolean isKeyNeeded() {
        return keyNeeded;
    }
}
