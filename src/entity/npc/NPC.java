package entity.npc;

import entity.Entity;
import dialogue.DialogueSpeaker;
import main.GamePanel;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NPC extends Entity {

    public String name;
    private final List<String> dialogueLines = new ArrayList<>();
    private BufferedImage portrait;
    private Color dialogueAccentColor = Color.WHITE;

    public NPC(GamePanel gp) {
        super(gp);
    }

    protected void setupNPCStats(String name, int speed, String direction) {
        this.name = name;
        this.speed = speed;
        this.direction = direction;
    }

    protected void setupCollisionAreas(Rectangle solidArea, Rectangle hitBox) {
        this.solidArea = solidArea;
        this.solidAreaDefaultX = solidArea.x;
        this.solidAreaDefaultY = solidArea.y;
        this.hitBox = hitBox;
    }

    protected void setDialogueLine(int index, String text) {
        if (index < 0) {
            return;
        }
        while (dialogueLines.size() <= index) {
            dialogueLines.add(null);
        }
        dialogueLines.set(index, text);
    }

    protected void addDialogueLine(String text) {
        dialogueLines.add(text);
    }

    protected void setDialoguePortrait(BufferedImage portrait) {
        this.portrait = portrait;
    }

    protected void setDialogueAccentColor(Color dialogueAccentColor) {
        this.dialogueAccentColor = dialogueAccentColor == null ? Color.WHITE : dialogueAccentColor;
    }

    public List<String> dialogueLines() {
        List<String> visibleLines = new ArrayList<>();
        for (String line : dialogueLines) {
            if (line != null && !line.trim().isEmpty()) {
                visibleLines.add(line);
            }
        }
        return Collections.unmodifiableList(visibleLines);
    }

    public DialogueSpeaker dialogueSpeaker() {
        return new DialogueSpeaker(name, portrait, dialogueAccentColor);
    }

    @Override
    public void speak() {
        gp.dialogueManager.startBlocking(dialogueSpeaker(), dialogueLines());
        facePlayer();
    }
}
