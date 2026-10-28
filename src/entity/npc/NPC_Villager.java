package entity.npc;

import main.GamePanel;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Random;

public class NPC_Villager extends NPC {
    private final List<String[]> conversations = new ArrayList<>();
    private final boolean wanders;
    private final boolean silent;
    private final Random random = new Random();
    private int conversationIndex = 0;
    private int walkPixelsLeft = 0;

    public NPC_Villager(GamePanel gp, String name, String sprite, Color accent, boolean wanders) {
        this(gp, name, sprite, accent, wanders, false);
    }

    private NPC_Villager(GamePanel gp, String name, String sprite, Color accent, boolean wanders, boolean silent) {
        super(gp);
        this.wanders = wanders;
        this.silent = silent;
        setupNPCStats(name, wanders ? 1 : 0, "idle");
        setupCollisionAreas(
                new Rectangle(6, 16, 36, 32),
                new Rectangle(0, 0, 48, 48)
        );
        loadSprites(sprite);
        setDialoguePortrait(down1);
        setDialogueAccentColor(accent);
    }

    public static NPC_Villager silent(GamePanel gp, String name, String sprite) {
        return new NPC_Villager(gp, name, sprite, Color.WHITE, false, true);
    }

    public NPC_Villager conversation(String... lines) {
        conversations.add(lines);
        return this;
    }

    public NPC_Villager talkRange(double tiles) {
        talkRangeTiles = tiles;
        return this;
    }

    public NPC_Villager facing(String facing) {
        idleFacing = facing;
        return this;
    }

    private void loadSprites(String sprite) {
        up1 = load(sprite, "up_1");
        up2 = load(sprite, "up_2");
        down1 = load(sprite, "down_1");
        down2 = load(sprite, "down_2");
        left1 = load(sprite, "left_1");
        left2 = load(sprite, "left_2");
        right1 = load(sprite, "right_1");
        right2 = load(sprite, "right_2");
    }

    private BufferedImage load(String sprite, String frame) {
        String path = "npc/" + sprite + "_" + frame + ".png";
        try {
            return ImageIO.read(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream(path), "Missing " + path));
        } catch (IOException e) {
            throw new RuntimeException("Could not load " + path, e);
        }
    }

    @Override
    public void update() {
        if (!wanders) {
            spriteNum = 1;
            return;
        }
        super.update();
    }

    @Override
    public void setAction() {
        if (walkPixelsLeft > 0) {
            walkPixelsLeft -= speed;
            if (walkPixelsLeft <= 0 || collisionOn || !canStep()) {
                walkPixelsLeft = 0;
                idleFacing = direction;
                direction = "idle";
                actionLockCounter = 0;
            }
            return;
        }

        actionLockCounter++;
        if (actionLockCounter < 150) {
            return;
        }
        actionLockCounter = 0;

        if (random.nextInt(100) < 45) {
            direction = "idle";
            return;
        }
        String[] directions = {"up", "down", "left", "right"};
        direction = directions[random.nextInt(directions.length)];
        walkPixelsLeft = (random.nextInt(2) + 1) * gp.tileSize;
    }

    @Override
    public void speak() {
        if (silent) {
            return;
        }
        facePlayer();
        if (!"idle".equals(direction)) {
            idleFacing = direction;
        }
        direction = "idle";
        if (conversations.isEmpty()) {
            return;
        }
        String[] lines = conversations.get(conversationIndex);
        conversationIndex = (conversationIndex + 1) % conversations.size();
        walkPixelsLeft = 0;
        gp.dialogueManager.startBlocking(dialogueSpeaker(), Arrays.asList(lines));
    }
}
