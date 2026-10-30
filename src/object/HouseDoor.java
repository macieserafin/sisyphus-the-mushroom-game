package object;

import entity.Player;
import level.TransitionTarget;
import main.GamePanel;
import main.Sound;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Objects;

/**
 * A building door. Walking into it swings it open and, a moment later, takes the player through.
 * A door without a target is locked: it only shows a short message.
 * NPCs never pass: the door is always solid for them.
 */
public class HouseDoor extends SuperObject {
    private static final int OPEN_FRAMES = 14;
    private static final int LOCKED_MESSAGE_COOLDOWN = 90;

    private final TransitionTarget target;
    private final String lockedMessage;
    private final BufferedImage closedImage;
    private final BufferedImage openImage;
    private int openTicks = -1;
    private int lockedCooldown = 0;

    public HouseDoor(TransitionTarget target, String style) {
        this(target, style, null);
    }

    public HouseDoor(String style, String lockedMessage) {
        this(null, style, lockedMessage);
    }

    private HouseDoor(TransitionTarget target, String style, String lockedMessage) {
        this.target = target;
        this.lockedMessage = lockedMessage;
        name = "Door";
        collision = true;
        closedImage = load("objects/door_" + style + ".png");
        openImage = load("objects/door_" + style + "_open.png");
        image = closedImage;
    }

    @Override
    public void onPlayerTouch(GamePanel gp, Player player, int objectIndex) {
        if (target == null) {
            if (lockedCooldown == 0) {
                gp.ui.showMessage(lockedMessage);
                lockedCooldown = LOCKED_MESSAGE_COOLDOWN;
            }
            return;
        }
        if (openTicks < 0) {
            openTicks = 0;
            gp.playSoundEffect(Sound.DOOR);
        }
    }

    @Override
    public void update(GamePanel gp) {
        if (lockedCooldown > 0) {
            lockedCooldown--;
        }
        if (openTicks < 0) {
            return;
        }
        openTicks++;
        if (openTicks == OPEN_FRAMES) {
            gp.levelManager.transitionTo(target);
        }
    }

    @Override
    protected BufferedImage currentImage() {
        return openTicks >= 0 ? openImage : closedImage;
    }

    private static BufferedImage load(String path) {
        try {
            return ImageIO.read(Objects.requireNonNull(HouseDoor.class.getClassLoader().getResourceAsStream(path), "Missing " + path));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
