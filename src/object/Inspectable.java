package object;

import dialogue.DialogueSpeaker;
import entity.Player;
import main.GamePanel;

import java.awt.*;
import java.util.Arrays;

public class Inspectable extends SuperObject {
    private static final DialogueSpeaker NARRATION = new DialogueSpeaker("", null, new Color(200, 190, 170));

    private final String[] lines;

    public Inspectable(String name, String... lines) {
        this.name = name;
        this.lines = lines;
        collision = false;
    }

    @Override
    public boolean isInteractable() { return true; }

    @Override
    public boolean onPlayerInteract(GamePanel gp, Player player) {
        gp.dialogueManager.startBlocking(NARRATION, Arrays.asList(lines));
        return true;
    }
}
