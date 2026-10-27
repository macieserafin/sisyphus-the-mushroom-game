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

    // How far away the player can be and still start a conversation with ENTER.
    public double talkRangeTiles = 1.2;
    // World-pixel rectangle the NPC never leaves; null means it may walk anywhere.
    private Rectangle homeArea;

    public NPC(GamePanel gp) {
        super(gp);
    }

    /** Keeps the NPC inside the given tiles (inclusive), e.g. the room it lives in. */
    public void setHomeArea(int leftCol, int topRow, int rightCol, int bottomRow) {
        homeArea = new Rectangle(
                leftCol * gp.tileSize,
                topRow * gp.tileSize,
                (rightCol - leftCol + 1) * gp.tileSize,
                (bottomRow - topRow + 1) * gp.tileSize
        );
    }

    @Override
    protected boolean canStep() {
        if (homeArea == null) {
            return true;
        }
        int nextX = worldX;
        int nextY = worldY;
        switch (direction) {
            case "up": nextY -= speed; break;
            case "down": nextY += speed; break;
            case "left": nextX -= speed; break;
            case "right": nextX += speed; break;
            default: return true;
        }
        Rectangle body = new Rectangle(nextX + solidArea.x, nextY + solidArea.y, solidArea.width, solidArea.height);
        return homeArea.contains(body);
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
