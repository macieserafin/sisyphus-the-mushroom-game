package object;

import dialogue.DialogueSpeaker;
import entity.Player;
import main.GamePanel;

import java.awt.*;
import java.util.Arrays;

public class Inspectable extends SuperObject {
    private static final DialogueSpeaker NARRATION = new DialogueSpeaker("", null, new Color(200, 190, 170));

    private final String[] lines;
    private String story = "";

    public Inspectable story(String id) { story = id; return this; }

    public Inspectable(String name, String... lines) {
        this.name = name;
        this.lines = lines;
        collision = false;
    }

    @Override
    public boolean isInteractable() { return true; }

    @Override
    public boolean onPlayerInteract(GamePanel gp, Player player) {
        if (!story.isEmpty() && gp.narrative.enabled() && !gp.narrative.conversations(story, "talk").isEmpty()) { gp.narrative.play(gp.narrative.conversations(story, "talk").getFirst()); return true; }
        gp.playSoundEffect(audio.Sound.PAPER);
        gp.dialogueManager.startBlocking(NARRATION, Arrays.asList(lines));
        return true;
    }
}
