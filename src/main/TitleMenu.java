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
    private final Font subtitleFont = new Font("Serif", Font.ITALIC, 20);

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
            if (gp.optionsMenu.handle(code)) {
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
                gp.optionsMenu.open();
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
        g2.setPaint(new GradientPaint(0, 0, new Color(14, 6, 10, 150), 0, h, new Color(10, 4, 8, 235)));
        g2.fillRect(0, 0, w, h);
        g2.setPaint(new RadialGradientPaint(w / 2f, h / 2f, w * 0.75f, new float[]{0.55f, 1f},
                new Color[]{new Color(0, 0, 0, 0), new Color(0, 0, 0, 170)}));
        g2.fillRect(0, 0, w, h);

        drawTitle(g2);
        if (inOptions) {
            drawOptions(g2);
        } else {
            drawItems(g2);
        }

        String[] hints = inOptions
                ? new String[]{"A/D  CHANGE", "ESC  BACK"}
                : new String[]{"W/S  MOVE", "ENTER  SELECT"};
        int totalW = 0;
        for (String hint : hints) {
            totalW += hintWidth(hint) + 12;
        }
        int x = (w - totalW + 12) / 2;
        for (String hint : hints) {
            UiSkin.plaque(g2, x, h - 42, hintWidth(hint), 27, hint, UiSkin.DIM);
            x += hintWidth(hint) + 12;
        }
    }

    private static int hintWidth(String hint) {
        int width = UiSkin.PixelFont.width(hint, 2) + 30;
        return width + (UiSkin.P - width % UiSkin.P) % UiSkin.P;
    }

    private void drawTitle(Graphics2D g2) {
        int cx = gp.screenWidth / 2;
        int bob = (int) Math.round(Math.sin(tick * 0.04) * 3);
        UiSkin.icon(g2, UiSkin.Icon.MUSHROOM, cx, 70 + bob, 5, true);

        drawTitleLine(g2, "SISYPHUS", 7, 126, UiSkin.CREAM);
        drawTitleLine(g2, "THE", 3, 188, UiSkin.TEXT_DIM);
        drawTitleLine(g2, "MUSHROOM", 6, 214, new Color(0xC8382E));

//        String subtitle = "you will wake up again";
        g2.setFont(subtitleFont);
        float pulse = 0.55f + 0.3f * (float) Math.sin(tick * 0.05);
        g2.setColor(new Color(232, 216, 176, (int) (255 * pulse)));
//        g2.drawString(subtitle, centered(g2, subtitle), 286);
    }

    private void drawTitleLine(Graphics2D g2, String text, int scale, int y, Color color) {
        int x = (gp.screenWidth - UiSkin.PixelFont.width(text, scale)) / 2;
        UiSkin.PixelFont.drawOutlined(g2, text, x, y, scale, color, new Color(0, 0, 0, 150));
    }

    private void drawItems(Graphics2D g2) {
        UiSkin.Icon[] icons = {UiSkin.Icon.PLAY, UiSkin.Icon.OPTIONS, UiSkin.Icon.CLOSE};
        String[] labels = {"START", "OPTIONS", "QUIT"};
        int badge = 54;
        int plaqueW = 168;
        int left = gp.screenWidth / 2 - (badge + plaqueW - 12) / 2;
        int y = 312;
        for (int i = 0; i < ITEMS.length; i++) {
            boolean active = i == selected;
            int x = left + (active ? 12 : 0);
            UiSkin.plaque(g2, x + badge - 15, y + 12, plaqueW, 33, labels[i], active ? UiSkin.GLOW : UiSkin.DIM);
            UiSkin.badge(g2, x, y, badge, active ? UiSkin.RED : UiSkin.DIM);
            UiSkin.icon(g2, icons[i], x + badge / 2 + (i == 0 ? 2 : 0), y + badge / 2, 3, active);
            y += 64;
        }
    }

    private void drawOptions(Graphics2D g2) {
        gp.optionsMenu.draw(g2, 300);
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
