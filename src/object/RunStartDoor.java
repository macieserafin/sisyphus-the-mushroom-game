package object;

import entity.Player;
import main.GamePanel;

public class RunStartDoor extends Door {

    public RunStartDoor() {
        super(false);
        name = "RunStartDoor";
    }

    @Override
    public void onPlayerTouch(GamePanel gp, Player player, int objectIndex) {
        gp.levelManager.startRun();
    }
}
