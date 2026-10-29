package main;

import entity.npc.NPC;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.concurrent.ConcurrentLinkedQueue;

// Talking to an NPC who sells something first asks "Talk or Shop?". The shop lists the offers with their prices;
// buying spends banked mushrooms. The world stands still while the window is open.
public class ShopMenu {
    private static final String[] CHOICES = {"Talk", "Shop", "Leave"};
    private static final int TALK = 0, SHOP = 1, LEAVE = 2;
    private static final int WIDTH = 460;
    private static final int ROW_H = 38;
    private static final Color RED_TEXT = UiSkin.TEXT_RED;

    private final GamePanel gp;
    private final ConcurrentLinkedQueue<Integer> pressedKeys = new ConcurrentLinkedQueue<>();
    private final Font titleFont = new Font("SansSerif", Font.BOLD, 17);
    private final Font itemFont = new Font("SansSerif", Font.BOLD, 16);
    private final Font smallFont = new Font("SansSerif", Font.PLAIN, 13);

    private NPC npc;
    private Shop shop;
    private boolean trainer;
    private boolean browsing;
    private int selected;
    private String feedback = "";
    private boolean feedbackBad;

    public ShopMenu(GamePanel gp) {
        this.gp = gp;
    }

    public void open(NPC npc, Shop shop) {
        this.npc = npc;
        this.shop = shop;
        trainer = false;
        browsing = false;
        selected = 0;
        feedback = "";
        pressedKeys.clear();
        gp.keyH.clearGameplayInput();
        gp.gameState = GameState.SHOP;
    }

    // A teacher (Master Kuro) offers "Skills" instead of "Shop"; it opens the skill tree.
    public void openTrainer(NPC npc, boolean onSkills) {
        open(npc, null);
        trainer = true;
        selected = onSkills ? SHOP : 0;
    }

    public boolean isBrowsing() {
        return browsing;
    }

    public void keyPressed(int code) {
        pressedKeys.add(code);
    }

    public void update() {
        Integer code;
        while (gp.gameState == GameState.SHOP && (code = pressedKeys.poll()) != null) {
            handle(code);
        }
    }

    private void handle(int code) {
        int rows = browsing ? shop.offers.size() + 1 : CHOICES.length;
        switch (code) {
            case KeyEvent.VK_UP, KeyEvent.VK_W -> { selected = (selected + rows - 1) % rows; gp.playSoundEffect(Sound.COIN); }
            case KeyEvent.VK_DOWN, KeyEvent.VK_S -> { selected = (selected + 1) % rows; gp.playSoundEffect(Sound.COIN); }
            case KeyEvent.VK_ENTER, KeyEvent.VK_SPACE -> choose();
            case KeyEvent.VK_ESCAPE -> back();
            default -> {
            }
        }
    }

    private void choose() {
        if (!browsing) {
            switch (selected) {
                case TALK -> {
                    close();
                    npc.talk();
                }
                case SHOP -> {
                    if (trainer) {
                        gp.skillTreeMenu.open(npc);
                        return;
                    }
                    browsing = true;
                    selected = 0;
                    feedback = "";
                }
                default -> close();
            }
            return;
        }
        if (selected == shop.offers.size()) {
            back();
            return;
        }
        buy(shop.offers.get(selected));
    }

    // Buys one offer if it is not sold out and the bank can pay for it.
    void buy(Shop.Offer offer) {
        if (offer.soldOut()) {
            say(offer.limit() > 1 ? "Sold out." : "You already own it.", true);
            return;
        }
        int price = offer.price();
        if (!gp.wallet.spend(price)) {
            say("Not enough mushrooms. You need " + price + ".", true);
            return;
        }
        offer.buy();
        say("Bought " + offer.name + ".", false);
        gp.playSoundEffect(Sound.POWERUP);
    }

    private void say(String text, boolean bad) {
        feedback = text;
        feedbackBad = bad;
    }

    private void back() {
        if (browsing) {
            browsing = false;
            selected = SHOP;
            feedback = "";
        } else {
            close();
        }
    }

    private void close() {
        pressedKeys.clear();
        gp.keyH.clearGameplayInput();
        gp.gameState = GameState.PLAYING;
    }

    // ---------------------------------------------------------------- drawing

    public void draw(Graphics2D g2) {
        if (npc == null) {
            return;
        }
        Font oldFont = g2.getFont();
        Color oldColor = g2.getColor();
        Object oldAa = g2.getRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g2.setColor(new Color(10, 4, 6, 150));
        g2.fillRect(0, 0, gp.screenWidth, gp.screenHeight);
        if (browsing) {
            drawShop(g2);
            drawHints(g2, "W/S  MOVE", "ENTER  BUY", "ESC  BACK");
        } else {
            drawChoice(g2);
            drawHints(g2, "W/S  MOVE", "ENTER  CHOOSE", "ESC  LEAVE");
        }

        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, oldAa);
        g2.setFont(oldFont);
        g2.setColor(oldColor);
    }

    private void drawChoice(Graphics2D g2) {
        int width = 320;
        int height = 92 + CHOICES.length * ROW_H;
        int x = (gp.screenWidth - width) / 2;
        int y = (gp.screenHeight - height) / 2;
        UiSkin.frame(g2, x, y, width, height);
        UiSkin.header(g2, gp.screenWidth / 2, y - 14, npc.name.toUpperCase());

        drawPortrait(g2, x + 22, y + 22);
        g2.setFont(titleFont);
        g2.setColor(UiSkin.TEXT);
        g2.drawString("What do you need?", x + 84, y + 50);

        for (int i = 0; i < CHOICES.length; i++) {
            String label = i == SHOP && trainer ? "Skills" : CHOICES[i];
            drawRow(g2, x + 18, y + 80 + i * ROW_H, width - 36, i == selected, label, null, false);
        }
    }

    private void drawShop(Graphics2D g2) {
        int rows = shop.offers.size() + 1;
        int height = 128 + rows * ROW_H + 64;
        int x = (gp.screenWidth - WIDTH) / 2;
        int y = (gp.screenHeight - height) / 2;
        UiSkin.frame(g2, x, y, WIDTH, height);
        UiSkin.header(g2, gp.screenWidth / 2, y - 14, "SHOP");

        drawPortrait(g2, x + 22, y + 22);
        g2.setFont(titleFont);
        g2.setColor(npc.dialogueSpeaker().accentColor());
        g2.drawString(npc.name, x + 84, y + 40);
        g2.setFont(smallFont);
        g2.setColor(UiSkin.TEXT);
        drawWrapped(g2, "\"" + shop.greeting + "\"", x + 84, y + 60, WIDTH - 110, 16);

        int listY = y + 104;
        for (int i = 0; i < shop.offers.size(); i++) {
            Shop.Offer offer = shop.offers.get(i);
            String label = offer.limit() > 1 ? offer.name + "  " + offer.bought() + "/" + offer.limit() : offer.name;
            String price = offer.soldOut() ? (offer.limit() > 1 ? "SOLD OUT" : "OWNED") : String.valueOf(offer.price());
            boolean tooExpensive = !offer.soldOut() && offer.price() > gp.wallet.banked();
            drawRow(g2, x + 18, listY + i * ROW_H, WIDTH - 36, i == selected, label, price, tooExpensive);
        }
        drawRow(g2, x + 18, listY + shop.offers.size() * ROW_H, WIDTH - 36, selected == shop.offers.size(), "Back", null, false);

        int infoY = listY + rows * ROW_H + 10;
        g2.setFont(smallFont);
        if (!feedback.isEmpty()) {
            g2.setColor(feedbackBad ? RED_TEXT : UiSkin.GOLD);
            g2.drawString(feedback, x + 26, infoY + 14);
        } else if (selected < shop.offers.size()) {
            g2.setColor(UiSkin.TEXT_DIM);
            g2.drawString(shop.offers.get(selected).description, x + 26, infoY + 14);
        }
        UiSkin.icon(g2, UiSkin.Icon.MUSHROOM, x + 36, infoY + 38, 2, true);
        g2.setFont(itemFont);
        g2.setColor(UiSkin.GOLD);
        g2.drawString(gp.wallet.banked() + " banked", x + 56, infoY + 44);
    }

    private void drawRow(Graphics2D g2, int x, int y, int w, boolean active, String label, String price, boolean tooExpensive) {
        if (active) {
            UiSkin.plaque(g2, x, y, w, 32, null, UiSkin.GLOW);
            UiSkin.PixelFont.draw(g2, ">", x + 12, y + 9, 2, UiSkin.CREAM);
        }
        g2.setFont(itemFont);
        g2.setColor(active ? UiSkin.TEXT : UiSkin.TEXT_DIM);
        g2.drawString(label, x + 36, y + 22);
        if (price == null) {
            return;
        }
        boolean number = Character.isDigit(price.charAt(0));
        int textW = g2.getFontMetrics().stringWidth(price);
        int textX = x + w - 16 - textW;
        g2.setColor(!number ? UiSkin.TEXT_DIM : tooExpensive ? RED_TEXT : UiSkin.GOLD);
        g2.drawString(price, textX, y + 22);
        if (number) {
            UiSkin.icon(g2, UiSkin.Icon.MUSHROOM, textX - 14, y + 16, 1, true);
        }
    }

    private void drawPortrait(Graphics2D g2, int x, int y) {
        UiSkin.inset(g2, x, y, 52, 52);
        BufferedImage portrait = npc.dialogueSpeaker().portrait();
        if (portrait != null) {
            g2.drawImage(portrait, x + 4, y + 4, 44, 44, null);
        }
    }

    private static void drawWrapped(Graphics2D g2, String text, int x, int y, int width, int lineH) {
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
        if (line.length() > 0) {
            g2.drawString(line.toString(), x, y);
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
