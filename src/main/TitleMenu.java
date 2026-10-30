package main;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Title screen: Continue (when there is a save) / Slots / Options / Quit. Arrows or W/S move, ENTER picks, ESC goes
 * back. Keys arrive on the AWT thread and are queued, so the menu only acts inside the game loop.
 */
public class TitleMenu {
    private enum Item {
        CONTINUE("CONTINUE", UiSkin.Icon.PLAY),
        NEW_GAME("NEW GAME", UiSkin.Icon.MUSHROOM),
        OPTIONS("OPTIONS", UiSkin.Icon.OPTIONS),
        QUIT("QUIT", UiSkin.Icon.CLOSE);

        final String label;
        final UiSkin.Icon icon;

        Item(String label, UiSkin.Icon icon) {
            this.label = label;
            this.icon = icon;
        }
    }

    private static final String BACKGROUND = "backgrounds/vn_mushroom_forest.png";

    private final GamePanel gp;
    private final ConcurrentLinkedQueue<Integer> pressedKeys = new ConcurrentLinkedQueue<>();
    private final BufferedImage background;
    private final Font subtitleFont = new Font("Serif", Font.ITALIC, 20);
    private final SlotMenu slotMenu;

    private java.util.List<Item> items = java.util.List.of(Item.NEW_GAME, Item.OPTIONS, Item.QUIT);
    private boolean itemsReady = false;
    private int selected = 0;
    private boolean inOptions = false;
    private boolean inSlots = false;
    private int tick = 0;

    public TitleMenu(GamePanel gp) {
        this.gp = gp;
        this.background = loadBackground();
        this.slotMenu = new SlotMenu(gp);
    }

    public void keyPressed(int code) {
        pressedKeys.add(code);
    }

    public void refresh() {
        items = gp.saves.continueSlot() > 0
                ? java.util.List.of(Item.CONTINUE, Item.NEW_GAME, Item.OPTIONS, Item.QUIT)
                : java.util.List.of(Item.NEW_GAME, Item.OPTIONS, Item.QUIT);
        selected = 0;
        itemsReady = true;
    }

    public void update() {
        tick++;
        if (!itemsReady) {
            refresh();
        }
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
        if (inSlots) {
            if (slotMenu.handle(code)) {
                inSlots = false;
                refresh();
            }
            return;
        }

        if (code == KeyEvent.VK_UP || code == KeyEvent.VK_W) {
            selected = (selected + items.size() - 1) % items.size();
            gp.playSoundEffect(Sound.UI_MOVE);
        }
        else if (code == KeyEvent.VK_DOWN || code == KeyEvent.VK_S) {
            selected = (selected + 1) % items.size();
            gp.playSoundEffect(Sound.UI_MOVE);
        }
        else if (code == KeyEvent.VK_ENTER) {
            gp.playSoundEffect(Sound.UI_CONFIRM);
            choose();
        }
    }

    private void choose() {
        switch (items.get(selected)) {
            case CONTINUE:
                inSlots = true;
                slotMenu.open(SlotMenu.Mode.CONTINUE);
                break;
            case NEW_GAME:
                inSlots = true;
                slotMenu.open(SlotMenu.Mode.NEW_GAME);
                break;
            case OPTIONS:
                inOptions = true;
                gp.optionsMenu.open();
                break;
            case QUIT:
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

        if (!inOptions) {
            drawTitle(g2);
        }
        if (inOptions) {
            drawOptions(g2);
        } else if (inSlots) {
            slotMenu.draw(g2);
        } else {
            drawItems(g2);
        }

        String[] hints = inOptions
                ? new String[]{"W/S  MOVE", "A/D  CHANGE", "ENTER  PICK", "ESC  BACK"}
                : inSlots
                ? new String[]{"W/S  MOVE", slotMenu.mode() == SlotMenu.Mode.CONTINUE ? "ENTER  LOAD" : "ENTER  START",
                        "X  DELETE", "ESC  BACK"}
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

        g2.setFont(subtitleFont);
        float pulse = 0.55f + 0.3f * (float) Math.sin(tick * 0.05);
        g2.setColor(new Color(232, 216, 176, (int) (255 * pulse)));
    }

    private void drawTitleLine(Graphics2D g2, String text, int scale, int y, Color color) {
        int x = (gp.screenWidth - UiSkin.PixelFont.width(text, scale)) / 2;
        UiSkin.PixelFont.drawOutlined(g2, text, x, y, scale, color, new Color(0, 0, 0, 150));
    }

    private void drawItems(Graphics2D g2) {
        int badge = 48;
        int plaqueW = 168;
        int left = gp.screenWidth / 2 - (badge + plaqueW - 12) / 2;
        int y = 276;
        for (int i = 0; i < items.size(); i++) {
            Item item = items.get(i);
            boolean active = i == selected;
            int x = left + (active ? 12 : 0);
            UiSkin.plaque(g2, x + badge - 15, y + 8, plaqueW, 33, item.label, active ? UiSkin.GLOW : UiSkin.DIM);
            UiSkin.badge(g2, x, y, badge, active ? UiSkin.RED : UiSkin.DIM);
            UiSkin.icon(g2, item.icon, x + badge / 2 + (item == Item.CONTINUE ? 2 : 0), y + badge / 2, 3, active);
            y += 56;
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
