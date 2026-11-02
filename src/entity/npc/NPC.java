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
    public String storyId = "";
    private final List<String> dialogueLines = new ArrayList<>();
    private BufferedImage portrait;
    private Color dialogueAccentColor = Color.WHITE;

    public double talkRangeTiles = 1.2;
    private Rectangle homeArea;
    private BufferedImage poseImage;
    private static final java.util.Map<String, BufferedImage> POSES = new java.util.HashMap<>();

    public NPC(GamePanel gp) {
        super(gp);
    }

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

    public String facing() { return "idle".equals(direction) ? idleFacing : direction; }

    public void poseFrame(String sheet, int frame) {
        String side = facing().equals("up") ? "down" : facing();
        sheet = sheet.replace("{dir}", side);
        String path = "npc/" + sheet + "_" + frame + ".png";
        poseImage = POSES.computeIfAbsent(path, key -> {
            try (var in = getClass().getClassLoader().getResourceAsStream(key)) {
                if (in == null) throw new IllegalStateException("Missing NPC pose " + key);
                return javax.imageio.ImageIO.read(in);
            } catch (java.io.IOException e) { throw new IllegalStateException(e); }
        });
    }

    public void clearPoseFrame() { poseImage = null; }

    @Override protected BufferedImage currentImage() { return poseImage != null ? poseImage : super.currentImage(); }

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

    @Override public void draw(Graphics graphics) {
        super.draw(graphics);
        if(gp.narrative==null)return;
        int marker=gp.narrative.busy()?0:gp.narrative.marker(this);
        if(marker>0)ui.UiSkin.PixelFont.draw((Graphics2D)graphics,marker==1?"!":"?",worldX-gp.player.worldX+gp.player.screenX+18,worldY-gp.player.worldY+gp.player.screenY-20,2,marker==3?ui.UiSkin.TEXT_DIM:ui.UiSkin.GOLD);
    }
    @Override
    public void speak() {
        talk();
    }

    public void talk() {
        gp.dialogueManager.startBlocking(dialogueSpeaker(), dialogueLines());
        facePlayer();
    }
}
