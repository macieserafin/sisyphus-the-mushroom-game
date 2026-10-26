package entity.npc;

import main.GamePanel;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.IOException;
import java.util.Objects;
import java.util.Random;

public class NPC_OldMan extends NPC {
    private int walkPixelsLeft = 0;

    public NPC_OldMan(GamePanel gp) {
        super(gp);

        setDefaultValues();
        getNPCImage();
        setDialogue();
    }

    public void setDefaultValues(){
        setupNPCStats("Old Man", 1, "down");
        setupCollisionAreas(
                new Rectangle(0, 0, 48, 48),
                new Rectangle(0, 0, 48, 48)
        );
    }

    public void getNPCImage() {
        try {
            up1 = ImageIO.read(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("npc/oldman_up_1.png")));
            up2 = ImageIO.read(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("npc/oldman_up_2.png")));
            down1 = ImageIO.read(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("npc/oldman_down_1.png")));
            down2 = ImageIO.read(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("npc/oldman_down_2.png")));
            left1 = ImageIO.read(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("npc/oldman_left_1.png")));
            left2 = ImageIO.read(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("npc/oldman_left_2.png")));
            right1 = ImageIO.read(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("npc/oldman_right_1.png")));
            right2 = ImageIO.read(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("npc/oldman_right_2.png")));
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    public void setAction(){

        if (walkPixelsLeft > 0) {
            walkPixelsLeft -= speed;

            if (walkPixelsLeft <= 0) {
                walkPixelsLeft = 0;
                direction = "idle";
                actionLockCounter = 0;
            }
            return;
        }

        actionLockCounter++;

        if(actionLockCounter >= 120){

            Random rand = new Random();
            int i = rand.nextInt(100) + 1;

            if (i <= 50) {
                direction = "idle";
            }
            else {
                int dir = rand.nextInt(4);
                if(dir == 0) direction = "up";
                if(dir == 1) direction = "down";
                if(dir == 2) direction = "left";
                if(dir == 3) direction = "right";

                int tilesToWalk = rand.nextInt(2) + 2;
                walkPixelsLeft = tilesToWalk * gp.tileSize;
            }

            actionLockCounter = 0;
        }
    }

    public void setDialogue(){
        setDialogueAccentColor(new Color(210, 230, 180));
        setDialogueLine(0, "Get the fuck out of my house.");
        setDialogueLine(1, "Get lost.");
        setDialogueLine(2, "U smell like shit.");
        setDialogueLine(3, "bgwoagubaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\naaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
    }

    public void speak(){
        super.speak();
    }
}
