package object;

import entity.Player;
import main.GamePanel;
import audio.Sound;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.util.Objects;

public class Boots extends SuperObject {
    public Boots() {
        name = "Boots";

        try{
            image = ImageIO.read(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("objects/boots.png")));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public void onPlayerTouch(GamePanel gp, Player player, int objectIndex) {
        player.addMaxSpeed(3);
        gp.entities.removeObject(this);
        gp.saves.requestProgressSave();
        gp.playSoundEffect(Sound.POWERUP_BOOTS);
    }

}
