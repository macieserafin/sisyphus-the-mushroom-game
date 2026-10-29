package main;

import story.QuestLog;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

// Quest journal (J): active and completed quests on two tabs, the selected one described on the right with
// its objective and progress. The world stands still while it is open.
public class QuestMenu {
    private static final int WIDTH = 660;
    private static final int HEIGHT = 410;
    private static final int LIST_W = 236;
    private static final int ROW_H = 34;
    private static final int VISIBLE_ROWS = 8;
    private static final Color MAIN_COLOR = UiSkin.GOLD;
    private static final Color SIDE_COLOR = new Color(0x8FD0C8);
    private static final Color DONE_COLOR = new Color(0x9CC870);

    private final GamePanel gp;
    private final ConcurrentLinkedQueue<Integer> pressedKeys = new ConcurrentLinkedQueue<>();
    private final Font titleFont = new Font("SansSerif", Font.BOLD, 18);
    private final Font rowFont = new Font("SansSerif", Font.BOLD, 14);
    private final Font textFont = new Font("SansSerif", Font.PLAIN, 14);
    private final Font smallFont = new Font("SansSerif", Font.PLAIN, 12);

    private boolean completedTab = false;
    private int selected = 0;
    private int scroll = 0;

    public QuestMenu(GamePanel gp) {
        this.gp = gp;
    }

    public void open() {
        pressedKeys.clear();
        completedTab = false;
        selected = 0;
        scroll = 0;
        gp.keyH.clearGameplayInput();
        gp.gameState = GameState.QUESTS;
    }

    public boolean isCompletedTab() {
        return completedTab;
    }

    public void keyPressed(int code) {
        pressedKeys.add(code);
    }

    public void update() {
        Integer code;
        while (gp.gameState == GameState.QUESTS && (code = pressedKeys.poll()) != null) {
            handle(code);
        }
    }

    private List<QuestLog.Quest> shown() {
        return completedTab ? gp.questLog.completed() : gp.questLog.active();
    }

    private void handle(int code) {
        int count = shown().size();
        switch (code) {
            case KeyEvent.VK_ESCAPE, KeyEvent.VK_J -> close();
            case KeyEvent.VK_A, KeyEvent.VK_D, KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT -> {
                completedTab = !completedTab;
                selected = 0;
                scroll = 0;
                gp.playSoundEffect(Sound.COIN);
            }
            case KeyEvent.VK_W, KeyEvent.VK_UP -> move(-1, count);
            case KeyEvent.VK_S, KeyEvent.VK_DOWN -> move(1, count);
            default -> {
            }
        }
    }

    private void move(int delta, int count) {
        if (count == 0) {
            return;
        }
        selected = Math.floorMod(selected + delta, count);
        if (selected < scroll) scroll = selected;
        if (selected >= scroll + VISIBLE_ROWS) scroll = selected - VISIBLE_ROWS + 1;
        gp.playSoundEffect(Sound.COIN);
    }

    private void close() {
        pressedKeys.clear();
        gp.keyH.clearGameplayInput();
        gp.gameState = GameState.PLAYING;
    }

    // ---------------------------------------------------------------- drawing

    public void draw(Graphics2D g2) {
        Font oldFont = g2.getFont();
        Color oldColor = g2.getColor();
        Object oldAa = g2.getRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g2.setColor(new Color(10, 4, 6, 160));
        g2.fillRect(0, 0, gp.screenWidth, gp.screenHeight);
        int x = (gp.screenWidth - WIDTH) / 2;
        int y = (gp.screenHeight - HEIGHT) / 2 - 10;
        UiSkin.frame(g2, x, y, WIDTH, HEIGHT);
        UiSkin.header(g2, gp.screenWidth / 2, y - 14, "JOURNAL");

        int activeCount = gp.questLog.active().size();
        int doneCount = gp.questLog.completed().size();
        UiSkin.plaque(g2, x + 20, y + 18, 150, 27, "ACTIVE " + activeCount, completedTab ? UiSkin.DIM : UiSkin.GLOW);
        UiSkin.plaque(g2, x + 176, y + 18, 174, 27, "COMPLETED " + doneCount, completedTab ? UiSkin.GLOW : UiSkin.DIM);

        List<QuestLog.Quest> quests = shown();
        if (selected >= quests.size()) {
            selected = Math.max(0, quests.size() - 1);
        }
        int listX = x + 20;
        int listY = y + 58;
        UiSkin.inset(g2, listX, listY, LIST_W, HEIGHT - 78);
        if (quests.isEmpty()) {
            g2.setFont(textFont);
            g2.setColor(UiSkin.TEXT_DIM);
            g2.drawString(completedTab ? "Nothing finished yet." : "No active quests.", listX + 16, listY + 28);
        }
        for (int i = scroll; i < quests.size() && i < scroll + VISIBLE_ROWS; i++) {
            drawRow(g2, quests.get(i), listX + 6, listY + 6 + (i - scroll) * ROW_H, LIST_W - 12, i == selected);
        }
        if (scroll + VISIBLE_ROWS < quests.size()) {
            UiSkin.PixelFont.draw(g2, "...", listX + LIST_W / 2 - 8, listY + HEIGHT - 96, 2, UiSkin.TEXT_DIM);
        }

        if (!quests.isEmpty()) {
            drawDetails(g2, quests.get(selected), listX + LIST_W + 22, listY + 4, WIDTH - LIST_W - 64);
        }

        drawHints(g2, "A/D  TAB", "W/S  MOVE", "J  CLOSE");
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, oldAa);
        g2.setFont(oldFont);
        g2.setColor(oldColor);
    }

    private void drawRow(Graphics2D g2, QuestLog.Quest quest, int x, int y, int w, boolean active) {
        if (active) {
            UiSkin.plaque(g2, x, y, w, 30, null, UiSkin.GLOW);
        }
        boolean done = quest.state() == QuestLog.State.DONE;
        g2.setColor(done ? DONE_COLOR : quest.main ? MAIN_COLOR : SIDE_COLOR);
        g2.fillRect(x + 10, y + 12, 6, 6);
        g2.setFont(rowFont);
        String progress = done ? "" : quest.counted() ? quest.progress() + "/" + quest.target : "";
        int progressW = g2.getFontMetrics().stringWidth(progress);
        g2.setColor(active ? UiSkin.TEXT : UiSkin.TEXT_DIM);
        g2.drawString(trim(g2.getFontMetrics(), quest.title, w - 34 - progressW - 8), x + 24, y + 20);
        if (!progress.isEmpty()) {
            g2.setColor(UiSkin.GOLD);
            g2.drawString(progress, x + w - 10 - progressW, y + 20);
        }
    }

    private void drawDetails(Graphics2D g2, QuestLog.Quest quest, int x, int y, int w) {
        boolean done = quest.state() == QuestLog.State.DONE;
        g2.setFont(smallFont);
        g2.setColor(quest.main ? MAIN_COLOR : SIDE_COLOR);
        g2.drawString(quest.main ? "MAIN QUEST" : "SIDE QUEST", x, y + 12);

        g2.setFont(titleFont);
        g2.setColor(UiSkin.TEXT);
        int ty = wrap(g2, quest.title, x, y + 36, w, 22);

        g2.setFont(textFont);
        g2.setColor(UiSkin.TEXT_DIM);
        ty = wrap(g2, quest.description, x, ty + 24, w, 19);

        g2.setFont(smallFont);
        g2.setColor(UiSkin.GOLD);
        g2.drawString("OBJECTIVE", x, ty + 28);
        g2.setFont(textFont);
        g2.setColor(UiSkin.TEXT);
        ty = wrap(g2, quest.objective, x, ty + 48, w, 19);

        if (quest.counted()) {
            int barY = ty + 16;
            double fill = quest.progress() / (double) quest.target;
            UiSkin.inset(g2, x, barY, w - 70, 16);
            g2.setColor(done ? DONE_COLOR : new Color(0xB8862A));
            g2.fillRect(x + 4, barY + 4, (int) Math.round((w - 78) * fill), 8);
            g2.setFont(rowFont);
            g2.setColor(UiSkin.GOLD);
            g2.drawString(quest.progress() + " / " + quest.target, x + w - 60, barY + 13);
            ty = barY + 16;
        }
        if (done) {
            g2.setFont(rowFont);
            g2.setColor(DONE_COLOR);
            g2.drawString("Completed", x, ty + 30);
        }
    }

    // Draws word-wrapped text and returns the baseline of the last line.
    static int wrap(Graphics2D g2, String text, int x, int y, int width, int lineH) {
        FontMetrics fm = g2.getFontMetrics();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            String next = line.length() == 0 ? word : line + " " + word;
            if (fm.stringWidth(next) > width && line.length() > 0) {
                g2.drawString(line.toString(), x, y);
                y += lineH;
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(next);
            }
        }
        g2.drawString(line.toString(), x, y);
        return y;
    }

    private static String trim(FontMetrics fm, String text, int width) {
        if (fm.stringWidth(text) <= width) {
            return text;
        }
        String dots = "...";
        int end = text.length();
        while (end > 0 && fm.stringWidth(text.substring(0, end) + dots) > width) {
            end--;
        }
        return text.substring(0, end) + dots;
    }

    private void drawHints(Graphics2D g2, String... hints) {
        int gap = 12;
        int total = -gap;
        for (String hint : hints) {
            total += hintWidth(hint) + gap;
        }
        int hx = (gp.screenWidth - total) / 2;
        for (String hint : hints) {
            UiSkin.plaque(g2, hx, gp.screenHeight - 42, hintWidth(hint), 27, hint, UiSkin.DIM);
            hx += hintWidth(hint) + gap;
        }
    }

    private static int hintWidth(String hint) {
        int width = UiSkin.PixelFont.width(hint, 2) + 30;
        return width + (UiSkin.P - width % UiSkin.P) % UiSkin.P;
    }
}
