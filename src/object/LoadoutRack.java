package object;

import dialogue.DialogueSpeaker;
import entity.Player;
import main.GamePanel;
import main.Sound;

import java.awt.*;

public class LoadoutRack extends SuperObject {
    private static final DialogueSpeaker NARRATION = new DialogueSpeaker("", null, new Color(230, 200, 150));

    private final boolean melee;

    public LoadoutRack(boolean melee) {
        this.melee = melee;
        name = melee ? "Weapon rack" : "Gun cabinet";
        collision = false;
    }

    @Override
    public boolean isInteractable() { return true; }

    @Override
    public boolean onPlayerInteract(GamePanel gp, Player player) {
        if (gp.campaign.inCombat()) { gp.ui.showMessage("In combat: equipment locked"); return true; }
        String weapon = melee ? player.cycleStartingMeleeWeapon() : player.cycleStartingRangedWeapon();
        gp.playSoundEffect(Sound.SWORD);
        gp.dialogueManager.startBlocking(NARRATION, (melee ? "You take the " : "You load the ") + weapon
                + ". It will be in your hands every time you wake.");
        return true;
    }
}
