package object;

import dialogue.DialogueSpeaker;
import entity.Player;
import main.GamePanel;
import main.Sound;

import java.awt.*;

/** The dojo weapon rack and gun cabinet: ENTER swaps the weapon the player starts every run with. */
public class LoadoutRack extends SuperObject {
    private static final DialogueSpeaker NARRATION = new DialogueSpeaker("", null, new Color(230, 200, 150));

    private final boolean melee;

    public LoadoutRack(boolean melee) {
        this.melee = melee;
        name = melee ? "Weapon rack" : "Gun cabinet";
        collision = false;
    }

    @Override
    public boolean onPlayerInteract(GamePanel gp, Player player) {
        String weapon = melee ? player.cycleStartingMeleeWeapon() : player.cycleStartingRangedWeapon();
        gp.playSoundEffect(Sound.SWORD);
        gp.dialogueManager.startBlocking(NARRATION, (melee ? "You take the " : "You load the ") + weapon
                + ". It will be in your hands every time you wake.");
        return true;
    }
}
