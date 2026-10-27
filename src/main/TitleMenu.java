package main;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Title screen: Start Game / Options / Exit. Arrows or W/S move, ENTER picks, ESC goes back.
 * Keys arrive on the AWT thread and are queued, so the menu only acts inside the game loop.
 */
public class TitleMenu {
    private static final String[] ITEMS = {"Start Game", "Options", "Exit"};
    private static final int START = 0, OPTIONS = 1, EXIT = 2;
    private static final String BACKGROUND = "backgrounds/vn_mushroom_forest.png";

    private final GamePanel gp;
    private final ConcurrentLinkedQueue<Integer> pressedKeys = new ConcurrentLinkedQueue<>();
    private final BufferedImage background;
    private final Font titleFont = new Font("Serif", Font.BOLD, 54);
    private final Font subtitleFont = new Font("Serif", Font.ITALIC, 20);
    private final Font itemFont = new Font("Monospaced", Font.BOLD, 26);
    private final Font hintFont = new Font("Monospaced", Font.PLAIN, 14);

    private int selected = START;
    private boolean inOptions = false;
    private int tick = 0;

    public TitleMenu(GamePanel gp) {
        this.gp = gp;
        this.background = loadBackground();
    }

    public void keyPressed(int code) {
        pressedKeys.add(code);
    }

    public void update() {
        tick++;
        Integer code;
        while ((code = pressedKeys.poll()) != null) {
            handle(code);
        }
    }

    private void handle(int code) {
        if (inOptions) {
            if (code == KeyEvent.VK_ESCAPE || code == KeyEvent.VK_ENTER) {
                inOptions = false;
            }
            return;
        }

        if (code == KeyEvent.VK_UP || code == KeyEvent.VK_W) {
            selected = (selected + ITEMS.length - 1) % ITEMS.length;
        }
        else if (code == KeyEvent.VK_DOWN || code == KeyEvent.VK_S) {
            selected = (selected + 1) % ITEMS.length;
        }
        else if (code == KeyEvent.VK_ENTER) {
            choose();
        }
    }

    private void choose() {
        switch (selected) {
            case START:
                pressedKeys.clear();
                gp.startNewGame();
                break;
            case OPTIONS:
                inOptions = true;
                break;
            case EXIT:
                System.exit(0);
                break;
            default:
                break;
        }
    }

    public void draw(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int w = gp.screenWidth, h = gp.screenHeight;

        if (background != null) {
            g2.drawImage(background, 0, 0, w, h, null);
        } else {
            g2.setColor(new Color(18, 22, 16));
            g2.fillRect(0, 0, w, h);
        }
        g2.setPaint(new GradientPaint(0, 0, new Color(8, 6, 10, 170), 0, h, new Color(8, 6, 10, 235)));
        g2.fillRect(0, 0, w, h);

        if (inOptions) {
            drawOptions(g2);
        } else {
            drawTitle(g2);
            drawItems(g2);
        }

        g2.setFont(hintFont);
        g2.setColor(new Color(170, 160, 140));
        String hint = inOptions ? "ESC - back" : "W/S or arrows - move     ENTER - choose";
        g2.drawString(hint, centered(g2, hint), h - 24);
    }

    private void drawTitle(Graphics2D g2) {
        String title = "Sisyphus the Mushroom";
        g2.setFont(titleFont);
        int x = centered(g2, title);
        int y = 150;
        g2.setColor(new Color(0, 0, 0, 200));
        g2.drawString(title, x + 3, y + 3);
        g2.setColor(new Color(200, 80, 60));
        g2.drawString(title, x, y);

        String subtitle = "you will wake up again";
        g2.setFont(subtitleFont);
        float pulse = 0.55f + 0.25f * (float) Math.sin(tick * 0.05);
        g2.setColor(new Color(190, 170, 220, (int) (255 * pulse)));
        g2.drawString(subtitle, centered(g2, subtitle), y + 36);
    }

    private void drawItems(Graphics2D g2) {
        g2.setFont(itemFont);
        int y = 300;
        for (int i = 0; i < ITEMS.length; i++) {
            boolean active = i == selected;
            String text = active ? "> " + ITEMS[i] + " <" : ITEMS[i];
            int x = centered(g2, text);
            g2.setColor(new Color(0, 0, 0, 180));
            g2.drawString(text, x + 2, y + 2);
            g2.setColor(active ? new Color(240, 210, 140) : new Color(150, 140, 125));
            g2.drawString(text, x, y);
            y += 52;
        }
    }

    private void drawOptions(Graphics2D g2) {
        g2.setFont(itemFont);
        g2.setColor(new Color(240, 210, 140));
        String title = "Options";
        g2.drawString(title, centered(g2, title), 200);

        g2.setFont(subtitleFont);
        g2.setColor(new Color(170, 160, 140));
        String empty = "Nothing here yet.";
        g2.drawString(empty, centered(g2, empty), 260);
    }

    private int centered(Graphics2D g2, String text) {
        return (gp.screenWidth - g2.getFontMetrics().stringWidth(text)) / 2;
    }

    private static BufferedImage loadBackground() {
        try (InputStream stream = TitleMenu.class.getClassLoader().getResourceAsStream(BACKGROUND)) {
            return stream == null ? null : ImageIO.read(stream);
        } catch (IOException e) {
            return null;
        }
    }
}
