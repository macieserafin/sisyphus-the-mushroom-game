package main;

import object.Key;

import java.awt.*;
import java.awt.image.BufferedImage;

public class UI {

    GamePanel gp;
    Graphics2D g2;
    Font arial_40;
    Font consolas_18;
    BufferedImage keyImage;

    public boolean messageOn = false;
    public String message = "";
    int messageCounter = 0;

    public String pauseMessage = "";

    public String currentDialogue = "";


    public UI(GamePanel gp) {
        this.gp = gp;

        arial_40 = new Font("Arial", Font.PLAIN, 20);
        consolas_18 = new Font("Consolas", Font.PLAIN, 18);

        Key key = new Key();
        keyImage = key.image;
    }

    public void showMessage(String s) {
        message = s;
        messageOn = true;
    }

    public void draw(Graphics g2) {
        this.g2 = (Graphics2D) g2;

        g2.setFont(consolas_18);
        g2.setColor(Color.WHITE);
        g2.drawImage(keyImage, 115, 33, 20, 20, null);
        g2.drawString("Keys = " + gp.player.hasKey, 30, 50);
        g2.drawString("Health = " + gp.player.health + "/" + gp.player.maxHealth, 30, 70);
        g2.drawString("Combat mode (Q): " + gp.player.combatState, 30, 30);
        g2.drawString("World position = x: " + gp.player.worldX + " y:" + gp.player.worldY, 400, 30);



        if (messageOn) {
            g2.drawString(message, 300, 250);

            messageCounter++;

            if (messageCounter > 60) {
                messageCounter = 0;
                messageOn = false;
            }
        }

        if (gp.gameState == GameState.PAUSED) {
            pauseMessage = "Paused";
            g2.drawString(pauseMessage, 300, 250);
        }
        if (gp.gameState == GameState.PLAYING) {
            pauseMessage = "";

        }

        // DIALOGUE

        if (gp.gameState == GameState.DIALOGUE) {
            drawDialogueScreen();
        }

        if (gp.gameState == GameState.DEAD) {
            drawDeathScreen();
        }


    }

    public void drawDeathScreen() {
        String text = "You died.";

        g2.setFont(arial_40);
        g2.setColor(Color.RED);

        int x = getXForCenteredText(text);
        int y = gp.screenHeight / 2;

        g2.drawString(text, x, y);
    }

    public void drawDialogueScreen() {
        //window

        int x, y, width, height;
        x = gp.tileSize * 2;
        y = gp.tileSize / 2;
        width = gp.screenWidth - (gp.tileSize*4);
        height = gp.tileSize * 4;

        drawSubWindow(x, y, width, height);

        x += gp.tileSize;
        y += gp.tileSize;
        g2.setFont(consolas_18);
        for(String line : currentDialogue.split("\n")) {
            g2.drawString(line, x, y);
            y+= 40;
        }


    }

    public void drawSubWindow(int x, int y, int width, int height) {
        Color c = new Color(0, 0, 0, 200);
        g2.setColor(c);
        g2.fillRoundRect(x, y, width, height, 35, 35);

        c = new Color(255, 255, 255);
        g2.setColor(c);
        g2.setStroke(new BasicStroke(5));
        g2.drawRoundRect(x+5, y+5, width-10, height-10, 25, 25);
    }

    public int getXForCenteredText(String text) {
        int length = (int)g2.getFontMetrics().getStringBounds(text, g2).getWidth();
        return gp.screenWidth / 2 - length / 2;
    }
}
