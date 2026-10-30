package main;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

// Pause menu. With an active save slot: Resume / Options / Save / Save and quit to title / Quit without saving.
// Without one (the demo): Resume / Options / Back to title.
public class PauseMenu {
    private enum Item {
        RESUME("Resume"),
        OPTIONS("Options"),
        SAVE("Save game"),
        SAVE_QUIT("Save and quit to title"),
        QUIT("Quit without saving"),
        TITLE("Back to title");

        final String label;

        Item(String label) {
            this.label = label;
        }
    }

    private static final int FEEDBACK_TICKS = 150;

    private final GamePanel gp;
    private final ConcurrentLinkedQueue<Integer> pressedKeys = new ConcurrentLinkedQueue<>();
    private final Font itemFont = UiFonts.of(Font.BOLD, 17);
    private final Font hintFont = UiFonts.of(Font.PLAIN, 13);
    private List<Item> items = List.of(Item.RESUME, Item.OPTIONS, Item.TITLE);
    private int selected = 0;
    private boolean inOptions = false;
    private boolean confirmingQuit = false;
    private String feedback = "";
    private int feedbackTicks = 0;

    public PauseMenu(GamePanel gp) {
        this.gp = gp;
    }

    public void open() {
        pressedKeys.clear();
        items = gp.saves.isActive()
                ? List.of(Item.RESUME, Item.OPTIONS, Item.SAVE, Item.SAVE_QUIT, Item.QUIT)
                : List.of(Item.RESUME, Item.OPTIONS, Item.TITLE);
        selected = 0;
        inOptions = false;
        confirmingQuit = false;
        feedback = "";
        feedbackTicks = 0;
    }

    public void keyPressed(int code) {
        pressedKeys.add(code);
    }

    public void update() {
        if (feedbackTicks > 0) {
            feedbackTicks--;
        }
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
        if (confirmingQuit) {
            if (code == KeyEvent.VK_ENTER) {
                confirmingQuit = false;
                gp.quitWithoutSaving();
            } else if (code == KeyEvent.VK_ESCAPE) {
                confirmingQuit = false;
                gp.playSoundEffect(Sound.UI_BACK);
            }
            return;
        }

        switch (code) {
            case KeyEvent.VK_ESCAPE -> gp.resumeGame();
            case KeyEvent.VK_UP, KeyEvent.VK_W -> {
                selected = (selected + items.size() - 1) % items.size();
                gp.playSoundEffect(Sound.UI_MOVE);
            }
            case KeyEvent.VK_DOWN, KeyEvent.VK_S -> {
                selected = (selected + 1) % items.size();
                gp.playSoundEffect(Sound.UI_MOVE);
            }
            case KeyEvent.VK_ENTER -> choose();
            default -> {
            }
        }
    }

    private void choose() {
        switch (items.get(selected)) {
            case RESUME -> {
                gp.playSoundEffect(Sound.UI_CONFIRM);
                gp.resumeGame();
            }
            case OPTIONS -> {
                gp.playSoundEffect(Sound.UI_CONFIRM);
                inOptions = true;
                gp.optionsMenu.open();
            }
            case SAVE -> {
                boolean saved = gp.saves.saveManually();
                gp.playSoundEffect(saved ? Sound.UI_CONFIRM : Sound.UI_ERROR);
                feedback = saved ? "Game saved" : "Could not save, see the console";
                feedbackTicks = FEEDBACK_TICKS;
            }
            case SAVE_QUIT, TITLE -> {
                gp.playSoundEffect(Sound.UI_CONFIRM);
                gp.returnToTitle();
            }
            case QUIT -> {
                gp.playSoundEffect(Sound.UI_ERROR);
                confirmingQuit = true;
            }
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
        } else if (confirmingQuit) {
            drawItems(g2);
            drawHints(g2, "ENTER  QUIT", "ESC  CANCEL");
        } else {
            drawItems(g2);
            drawHints(g2, "W/S  MOVE", "ESC  RESUME");
        }

        g2.setFont(oldFont);
        g2.setColor(oldColor);
    }

    private void drawItems(Graphics2D g2) {
        int width = 340;
        int rowH = 36;
        int height = 30 + items.size() * rowH + 40;
        int x = gp.screenWidth / 2 - width / 2;
        int y = gp.screenHeight / 2 - height / 2;
        UiSkin.frame(g2, x, y, width, height);
        UiSkin.header(g2, gp.screenWidth / 2, y - 14, "PAUSED");

        g2.setFont(itemFont);
        for (int i = 0; i < items.size(); i++) {
            int rowY = y + 24 + i * rowH;
            boolean active = i == selected;
            if (active) {
                UiSkin.plaque(g2, x + 18, rowY, width - 36, 30, null, UiSkin.GLOW);
                UiSkin.PixelFont.draw(g2, ">", x + 30, rowY + 8, 2, UiSkin.CREAM);
            }
            g2.setColor(active ? UiSkin.TEXT : UiSkin.TEXT_DIM);
            g2.drawString(items.get(i).label, x + 54, rowY + 21);
        }

        g2.setFont(hintFont);
        int noteY = y + height - 16;
        if (confirmingQuit) {
            g2.setColor(UiSkin.TEXT_RED);
            g2.drawString("Everything since your last save is lost.", x + 27, noteY);
        } else if (feedbackTicks > 0) {
            g2.setColor(new Color(0x9CC890));
            g2.drawString(feedback, x + 27, noteY);
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
