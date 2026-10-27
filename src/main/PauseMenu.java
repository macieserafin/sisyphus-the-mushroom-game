package main;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * ESC pause menu: resume, options, back to the title screen.
 * Keys are queued like in {@link TitleMenu}, so the actions run inside the game loop.
 */
public class PauseMenu {
    private static final String[] ITEMS = {"Resume", "Options", "Back to title"};
    private static final int RESUME = 0, OPTIONS = 1, TITLE = 2;

    private final GamePanel gp;
    private final ConcurrentLinkedQueue<Integer> pressedKeys = new ConcurrentLinkedQueue<>();
    private final Font itemFont = new Font("SansSerif", Font.BOLD, 17);
    private int selected = RESUME;
    private boolean inOptions = false;

    public PauseMenu(GamePanel gp) {
        this.gp = gp;
    }

    public void open() {
        pressedKeys.clear();
        selected = RESUME;
        inOptions = false;
    }

    public void keyPressed(int code) {
        pressedKeys.add(code);
    }

    public void update() {
        Integer code;
        while (gp.gameState == GameState.PAUSED && (code = pressedKeys.poll()) != null) {
            handle(code);
        }
    }

    private void handle(int code) {
        if (inOptions) {
            if (gp.optionsMenu.handle(code)) {
                inOptions = false;
            }
            return;
        }

        switch (code) {
            case KeyEvent.VK_ESCAPE -> gp.resumeGame();
            case KeyEvent.VK_UP, KeyEvent.VK_W -> selected = (selected + ITEMS.length - 1) % ITEMS.length;
            case KeyEvent.VK_DOWN, KeyEvent.VK_S -> selected = (selected + 1) % ITEMS.length;
            case KeyEvent.VK_ENTER -> choose();
            default -> {
            }
        }
    }

    private void choose() {
        switch (selected) {
            case RESUME -> gp.resumeGame();
            case OPTIONS -> {
                inOptions = true;
                gp.optionsMenu.open();
            }
            case TITLE -> gp.returnToTitle();
            default -> {
            }
        }
    }

    public void draw(Graphics2D g2) {
        Font oldFont = g2.getFont();
        Color oldColor = g2.getColor();

        g2.setColor(new Color(10, 4, 6, 160));
        g2.fillRect(0, 0, gp.screenWidth, gp.screenHeight);

        if (inOptions) {
            gp.optionsMenu.draw(g2, gp.screenHeight / 2 - 60);
            drawHints(g2, "A/D  CHANGE", "ESC  BACK");
        } else {
            drawItems(g2);
            drawHints(g2, "W/S  MOVE", "ESC  RESUME");
        }

        g2.setFont(oldFont);
        g2.setColor(oldColor);
    }

    private void drawItems(Graphics2D g2) {
        int width = 300;
        int rowH = 36;
        int height = 30 + ITEMS.length * rowH + 18;
        int x = gp.screenWidth / 2 - width / 2;
        int y = gp.screenHeight / 2 - height / 2;
        UiSkin.frame(g2, x, y, width, height);
        UiSkin.header(g2, gp.screenWidth / 2, y - 14, "PAUSED");

        g2.setFont(itemFont);
        for (int i = 0; i < ITEMS.length; i++) {
            int rowY = y + 24 + i * rowH;
            boolean active = i == selected;
            if (active) {
                UiSkin.plaque(g2, x + 18, rowY, width - 36, 30, null, UiSkin.GLOW);
                UiSkin.PixelFont.draw(g2, ">", x + 30, rowY + 8, 2, UiSkin.CREAM);
            }
            g2.setColor(active ? UiSkin.TEXT : UiSkin.TEXT_DIM);
            g2.drawString(ITEMS[i], x + 54, rowY + 21);
        }
    }

    private void drawHints(Graphics2D g2, String... hints) {
        int gap = 12;
        int total = -gap;
        for (String hint : hints) {
            total += hintWidth(hint) + gap;
        }
        int x = (gp.screenWidth - total) / 2;
        for (String hint : hints) {
            UiSkin.plaque(g2, x, gp.screenHeight - 42, hintWidth(hint), 27, hint, UiSkin.DIM);
            x += hintWidth(hint) + gap;
        }
    }

    private static int hintWidth(String hint) {
        int width = UiSkin.PixelFont.width(hint, 2) + 30;
        return width + (UiSkin.P - width % UiSkin.P) % UiSkin.P;
    }
}
