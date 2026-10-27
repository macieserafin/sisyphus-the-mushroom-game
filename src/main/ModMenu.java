package main;

import entity.Player;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.function.Supplier;

/** Prototype mod menu (F1): grants weapons, healing, power-ups and keys for testing. */
public class ModMenu {

    private record Item(Supplier<String> label, Runnable action) {
    }

    private final GamePanel gp;
    private final List<Item> items;
    private int selected = 0;
    private String feedback = "";

    private final Font itemFont = new Font("SansSerif", Font.BOLD, 16);
    private final Font hintFont = new Font("SansSerif", Font.PLAIN, 12);

    public ModMenu(GamePanel gp) {
        this.gp = gp;
        items = List.of(
                new Item(() -> "Heal to full", this::heal),
                new Item(() -> "Power-up: +1 max HP", this::addMaxHealth),
                new Item(() -> "Power-up: boots (speed)", this::addBoots),
                new Item(() -> "Add key  (have: " + gp.player.hasKey + ")", this::addKey),
                new Item(() -> toggleLabel("Melee", gp.player.ownsMeleeSlot(1), gp.player.meleeSlotLabel(1)),
                        () -> toggleMelee(1)),
                new Item(() -> toggleLabel("Ranged", gp.player.ownsRangedSlot(1), gp.player.rangedSlotLabel(1)),
                        () -> toggleRanged(1)),
                new Item(() -> "Weapons in village: " + (gp.player.isWeaponLockOverridden() ? "UNLOCKED" : "locked"),
                        this::toggleVillageLock),
                new Item(() -> "Reset loadout (sword + pistol)", this::resetLoadout),
                new Item(() -> "Close", this::close)
        );
    }

    public void open() {
        gp.keyH.clearGameplayInput();
        selected = 0;
        feedback = "";
        gp.gameState = GameState.MOD_MENU;
    }

    public void close() {
        gp.keyH.clearGameplayInput();
        gp.gameState = GameState.PLAYING;
    }

    public void keyPressed(int code) {
        switch (code) {
            case KeyEvent.VK_UP, KeyEvent.VK_W -> selected = (selected + items.size() - 1) % items.size();
            case KeyEvent.VK_DOWN, KeyEvent.VK_S -> selected = (selected + 1) % items.size();
            case KeyEvent.VK_ENTER, KeyEvent.VK_SPACE -> items.get(selected).action().run();
            case KeyEvent.VK_ESCAPE, KeyEvent.VK_F1 -> close();
            default -> {
            }
        }
    }

    // ---- actions ------------------------------------------------------------------------------

    private void heal() {
        gp.player.health = gp.player.maxHealth;
        feedback = "Healed";
    }

    private void addMaxHealth() {
        gp.player.maxHealth++;
        gp.player.health = gp.player.maxHealth;
        feedback = "Max HP: " + gp.player.maxHealth;
    }

    private void addBoots() {
        gp.player.addMaxSpeed(1);
        feedback = "Faster boots";
    }

    private void addKey() {
        gp.player.hasKey++;
        feedback = "Keys: " + gp.player.hasKey;
    }

    private void toggleMelee(int slot) {
        Player player = gp.player;
        player.setMeleeSlotOwned(slot, !player.ownsMeleeSlot(slot));
        feedback = player.ownsMeleeSlot(slot) ? "Added to loadout" : "Removed from loadout";
    }

    private void toggleRanged(int slot) {
        Player player = gp.player;
        player.setRangedSlotOwned(slot, !player.ownsRangedSlot(slot));
        feedback = player.ownsRangedSlot(slot) ? "Added to loadout" : "Removed from loadout";
    }

    private void toggleVillageLock() {
        gp.player.toggleWeaponLockOverride();
        feedback = gp.player.isWeaponLockOverridden() ? "Weapons work in the village" : "Weapons locked in the village";
    }

    private void resetLoadout() {
        gp.player.resetLoadout();
        feedback = "Ekwipunek zresetowany";
    }

    private String toggleLabel(String kind, boolean owned, String slotLabel) {
        String name = slotLabel.substring(slotLabel.indexOf(' ') + 1);
        return (owned ? "[x] " : "[ ] ") + kind + ": " + (owned ? name : "(none)");
    }

    // ---- drawing ------------------------------------------------------------------------------

    public void draw(Graphics2D g2) {
        Object oldAa = g2.getRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING);
        Font oldFont = g2.getFont();
        Color oldColor = g2.getColor();
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g2.setColor(new Color(10, 4, 6, 160));
        g2.fillRect(0, 0, gp.screenWidth, gp.screenHeight);

        int rowH = 30;
        int width = 468;
        int height = 48 + items.size() * rowH + 54;
        int x = gp.screenWidth / 2 - width / 2;
        int y = gp.screenHeight / 2 - height / 2 + 6;

        UiSkin.frame(g2, x, y, width, height);
        UiSkin.header(g2, gp.screenWidth / 2, y - 14, "MOD MENU");

        g2.setFont(itemFont);
        for (int i = 0; i < items.size(); i++) {
            int rowY = y + 27 + i * rowH;
            if (i == selected) {
                UiSkin.plaque(g2, x + 18, rowY - 3, width - 36, 30, null, UiSkin.GLOW);
                UiSkin.PixelFont.draw(g2, ">", x + 30, rowY + 5, 2, UiSkin.CREAM);
                g2.setColor(UiSkin.TEXT);
            } else {
                g2.setColor(UiSkin.TEXT_DIM);
            }
            g2.drawString(items.get(i).label().get(), x + 54, rowY + 18);
        }

        int footerY = y + height - 48;
        UiSkin.divider(g2, x + 24, footerY, width - 48);
        g2.setFont(hintFont);
        g2.setColor(new Color(0x9CC890));
        g2.drawString(feedback, x + 27, footerY + 21);
        g2.setColor(UiSkin.TEXT_DIM);
        String hint = "W/S: move   ENTER: use   ESC/F1: close";
        g2.drawString(hint, x + width - 27 - g2.getFontMetrics().stringWidth(hint), footerY + 21);

        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, oldAa);
        g2.setFont(oldFont);
        g2.setColor(oldColor);
    }
}
