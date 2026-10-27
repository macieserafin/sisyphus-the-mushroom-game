package main;

import java.awt.*;
import java.awt.event.KeyEvent;

/** Settings panel shared by the title screen and the pause menu. */
public class OptionsMenu {
    private static final int DISPLAY = 0, BACK = 1, ITEM_COUNT = 2;

    private final GamePanel gp;
    private final Font itemFont = new Font("SansSerif", Font.BOLD, 16);
    private int selected = DISPLAY;

    public OptionsMenu(GamePanel gp) {
        this.gp = gp;
    }

    public void open() {
        selected = DISPLAY;
    }

    /** Handles one key; returns true when the player leaves the options. */
    public boolean handle(int code) {
        switch (code) {
            case KeyEvent.VK_ESCAPE:
                return true;
            case KeyEvent.VK_UP:
            case KeyEvent.VK_W:
                selected = (selected + ITEM_COUNT - 1) % ITEM_COUNT;
                break;
            case KeyEvent.VK_DOWN:
            case KeyEvent.VK_S:
                selected = (selected + 1) % ITEM_COUNT;
                break;
            case KeyEvent.VK_LEFT:
            case KeyEvent.VK_A:
                if (selected == DISPLAY) {
                    gp.display.setMode(gp.display.mode().previous());
                }
                break;
            case KeyEvent.VK_RIGHT:
            case KeyEvent.VK_D:
                if (selected == DISPLAY) {
                    gp.display.setMode(gp.display.mode().next());
                }
                break;
            case KeyEvent.VK_ENTER:
                if (selected == BACK) {
                    return true;
                }
                gp.display.setMode(gp.display.mode().next());
                break;
            default:
                break;
        }
        return false;
    }

    /** Draws the panel with its top edge at y. */
    public void draw(Graphics2D g2, int y) {
        int width = 402;
        int rowH = 36;
        int height = 30 + ITEM_COUNT * rowH + 18;
        int x = gp.screenWidth / 2 - width / 2;
        UiSkin.frame(g2, x, y, width, height);
        UiSkin.header(g2, gp.screenWidth / 2, y - 14, "OPTIONS");

        g2.setFont(itemFont);
        for (int i = 0; i < ITEM_COUNT; i++) {
            int rowY = y + 24 + i * rowH;
            boolean active = i == selected;
            if (active) {
                UiSkin.plaque(g2, x + 18, rowY, width - 36, 30, null, UiSkin.GLOW);
            }
            g2.setColor(active ? UiSkin.TEXT : UiSkin.TEXT_DIM);
            if (i == DISPLAY) {
                g2.drawString("Display mode", x + 33, rowY + 21);
                String value = "< " + gp.display.mode().label + " >";
                g2.setColor(active ? UiSkin.GOLD : UiSkin.TEXT_DIM);
                g2.drawString(value, x + width - 33 - g2.getFontMetrics().stringWidth(value), rowY + 21);
            } else {
                g2.drawString("Back", x + 33, rowY + 21);
            }
        }
    }
}
