package main;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.List;

public final class ChoiceMenu {
    public record Option(String label, String description, Runnable action) {}
    private final GamePanel gp;
    private String title = "";
    private List<Option> options = List.of();
    private int selected;
    private boolean cancelable;
    private int lockTicks;

    public void update() { if (lockTicks > 0) lockTicks--; }
    public void protectConfirmation(int ticks) { lockTicks = ticks; }
    public String title() { return title; }
    public int optionCount() { return options.size(); }

    public ChoiceMenu(GamePanel gp) { this.gp = gp; }

    public void open(String title, List<Option> options, boolean cancelable) {
        this.title = title;
        this.options = List.copyOf(options);
        this.cancelable = cancelable;
        selected = 0;
        lockTicks = 0;
        gp.keyH.clearGameplayInput();
        gp.gameState = GameState.CHOICE;
    }

    public void handle(int code) {
        if (options.isEmpty() || lockTicks > 0) return;
        if (code == KeyEvent.VK_UP || code == KeyEvent.VK_W) selected = Math.floorMod(selected - 1, options.size());
        if (code == KeyEvent.VK_DOWN || code == KeyEvent.VK_S) selected = (selected + 1) % options.size();
        if (code == KeyEvent.VK_ESCAPE && cancelable) gp.resumeGame();
        if (code == KeyEvent.VK_ENTER) {
            Option option = options.get(selected);
            gp.resumeGame();
            option.action().run();
        }
    }

    public void draw(Graphics2D g) {
        g.setColor(new Color(8, 5, 14, 220));
        g.fillRect(0, 0, gp.screenWidth, gp.screenHeight);
        int x = 54, y = 60, width = gp.screenWidth - 108;
        UiSkin.frame(g, x, y, width, gp.screenHeight - 120);
        g.setFont(UiFonts.of(Font.BOLD, 20));
        g.setColor(UiSkin.GOLD);
        g.drawString(title, x + 24, y + 34);
        for (int i = 0; i < options.size(); i++) {
            int row = y + 62 + i * 35;
            g.setColor(i == selected ? new Color(0x493C4F) : new Color(0x211C28));
            g.fillRect(x + 14, row - 20, width - 28, 31);
            g.setColor(UiSkin.TEXT);
            g.setFont(UiFonts.of(Font.BOLD, 16));
            g.drawString((i == selected ? "> " : "  ") + options.get(i).label(), x + 24, row);
        }
        g.setColor(UiSkin.TEXT_DIM);
        g.setFont(UiFonts.of(Font.PLAIN, 14));
        int row = Math.max(y + 110, y + 80 + options.size() * 35);
        String line = "";
        for (String word : options.get(selected).description().split(" ")) {
            if (g.getFontMetrics().stringWidth(line + word) > width - 54) {
                g.drawString(line, x + 24, row); row += 21; line = "";
            }
            line += word + " ";
        }
        g.drawString(line, x + 24, row);
        g.drawString("W/S select   ENTER confirm" + (cancelable ? "   ESC back" : ""), x + 24, gp.screenHeight - 78);
    }
}
