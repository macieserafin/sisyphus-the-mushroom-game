package object;

import entity.Player;
import main.GamePanel;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.util.Objects;

public class Key extends SuperObject {

    public Key() {
        name = "Key";

        try{
            image = ImageIO.read(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("objects/key.png")));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public void onPlayerTouch(GamePanel gp, Player player, int objectIndex) {
        player.hasKey++;
        gp.obj[objectIndex] = null;
        gp.playSoundEffect(3);
        gp.ui.showMessage("You picked up key");
    }
}
