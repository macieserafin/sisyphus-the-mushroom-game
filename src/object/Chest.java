package object;

import entity.Player;
import main.GamePanel;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.util.Objects;

public class Chest extends SuperObject {
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
        if(player.hasKey > 0){
            gp.entities.removeObject(this);
            player.hasKey--;
            gp.playSoundEffect(2);
        }
    }
}
