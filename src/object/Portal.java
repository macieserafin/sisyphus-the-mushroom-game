package object;

import entity.Player;
import level.TransitionTarget;
import main.GamePanel;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.util.Objects;

public class Portal extends SuperObject {
    private final TransitionTarget target;

    public Portal(TransitionTarget target) {
        if (target == null) {
            throw new IllegalArgumentException("Portal target is required");
        }

        this.target = target;
        name = "Portal";
        collision = true;

        try {
            image = ImageIO.read(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("objects/door.png")));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onPlayerTouch(GamePanel gp, Player player, int objectIndex) {
        gp.levelManager.transitionTo(target);
    }
}
