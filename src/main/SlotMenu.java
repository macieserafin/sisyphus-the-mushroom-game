package main;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class SlotMenu {
    public enum Mode { CONTINUE, NEW_GAME }

    private enum Confirm { NONE, DELETE, OVERWRITE }

    private static final int CARD_W = 480;
    private static final int CARD_H = 60;
    private static final int CARD_STEP = 70;
    private static final int TOP = 262;
    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("d MMM, HH:mm").withZone(ZoneId.systemDefault());

    private final GamePanel gp;
    private final Font titleFont = UiFonts.of(Font.BOLD, 17);
    private final Font infoFont = UiFonts.of(Font.PLAIN, 13);
    private Mode mode = Mode.CONTINUE;
    private int selected;
    private Confirm confirm = Confirm.NONE;

    public SlotMenu(GamePanel gp) {
        this.gp = gp;
    }

    public void open(Mode mode) {
        this.mode = mode;
        confirm = Confirm.NONE;
        selected = 0;
        if (mode == Mode.CONTINUE) {
            int last = gp.saves.continueSlot();
            selected = last > 0 ? last - 1 : 0;
        } else {
            for (int slot = GameSaves.SLOTS; slot >= 1; slot--) {
                if (!gp.saves.hasSave(slot)) {
                    selected = slot - 1;
                }
            }
        }
    }

    public Mode mode() {
        return mode;
    }

    // Returns true when the player leaves the slot list.
    public boolean handle(int code) {
        int slot = selected + 1;
        if (confirm != Confirm.NONE) {
            if (code == KeyEvent.VK_ENTER) {
                Confirm done = confirm;
                confirm = Confirm.NONE;
                if (done == Confirm.OVERWRITE) {
                    gp.playSoundEffect(Sound.UI_CONFIRM);
                    gp.startSlot(slot, true);
                } else {
                    gp.saves.delete(slot);
                    gp.playSoundEffect(Sound.UI_BACK);
                    return mode == Mode.CONTINUE && gp.saves.continueSlot() == 0;
                }
            } else if (code == KeyEvent.VK_ESCAPE) {
                confirm = Confirm.NONE;
                gp.playSoundEffect(Sound.UI_BACK);
            }
            return false;
        }
        switch (code) {
            case KeyEvent.VK_ESCAPE -> {
                return true;
            }
            case KeyEvent.VK_UP, KeyEvent.VK_W -> {
                selected = (selected + GameSaves.SLOTS - 1) % GameSaves.SLOTS;
                gp.playSoundEffect(Sound.UI_MOVE);
            }
            case KeyEvent.VK_DOWN, KeyEvent.VK_S -> {
                selected = (selected + 1) % GameSaves.SLOTS;
                gp.playSoundEffect(Sound.UI_MOVE);
            }
            case KeyEvent.VK_ENTER -> {
                boolean filled = gp.saves.hasSave(slot);
                if (mode == Mode.CONTINUE) {
                    if (filled) {
                        gp.playSoundEffect(Sound.UI_CONFIRM);
                        gp.startSlot(slot, false);
                    } else {
                        gp.playSoundEffect(Sound.UI_ERROR);
                    }
                } else if (filled) {
                    confirm = Confirm.OVERWRITE;
                    gp.playSoundEffect(Sound.UI_ERROR);
                } else {
                    gp.playSoundEffect(Sound.UI_CONFIRM);
                    gp.startSlot(slot, true);
                }
            }
            case KeyEvent.VK_X, KeyEvent.VK_DELETE, KeyEvent.VK_BACK_SPACE -> {
                if (gp.saves.hasSave(slot)) {
                    confirm = Confirm.DELETE;
                    gp.playSoundEffect(Sound.UI_ERROR);
                }
            }
            default -> {
            }
        }
        return false;
    }

    public void draw(Graphics2D g2) {
        int x = (gp.screenWidth - CARD_W) / 2;
        for (int slot = 1; slot <= GameSaves.SLOTS; slot++) {
            int y = TOP + (slot - 1) * CARD_STEP;
            boolean active = slot - 1 == selected;
            GameSaves.Summary summary = gp.saves.summary(slot);
            UiSkin.plaque(g2, x + (active ? 8 : 0), y, CARD_W, CARD_H, null, active ? UiSkin.GLOW : UiSkin.DIM);
            int tx = x + (active ? 8 : 0) + 24;

            g2.setFont(titleFont);
            g2.setColor(active ? UiSkin.TEXT : UiSkin.TEXT_DIM);
            String title = "Slot " + slot + "  -  " + (summary.exists() ? summary.stage() : "Empty");
            g2.drawString(title, tx, y + 25);

            g2.setFont(infoFont);
            if (active && confirm == Confirm.DELETE) {
                g2.setColor(UiSkin.TEXT_RED);
                g2.drawString("Delete this save for good?   ENTER yes    ESC no", tx, y + 46);
            } else if (active && confirm == Confirm.OVERWRITE) {
                g2.setColor(UiSkin.TEXT_RED);
                g2.drawString("Overwrite this save with a new game?   ENTER yes    ESC no", tx, y + 46);
            } else if (summary.exists()) {
                g2.setColor(active ? UiSkin.GOLD : UiSkin.TEXT_DIM);
                g2.drawString(describe(summary), tx, y + 46);
            } else {
                g2.setColor(UiSkin.TEXT_DIM);
                g2.drawString(mode == Mode.NEW_GAME ? "Press ENTER to start a new game here" : "Nothing saved here",
                        tx, y + 46);
            }
        }
    }

    private static String describe(GameSaves.Summary summary) {
        long minutes = summary.playSeconds() / 60;
        String time = minutes >= 60 ? (minutes / 60) + "h " + String.format("%02d", minutes % 60) + "m" : minutes + " min";
        String when = summary.savedAt() > 0 ? "   -   " + WHEN.format(Instant.ofEpochMilli(summary.savedAt())) : "";
        return time + "   -   " + summary.banked() + " mushrooms   -   " + summary.upgrades()
                + (summary.upgrades() == 1 ? " upgrade" : " upgrades") + "   -   " + summary.skills()
                + (summary.skills() == 1 ? " skill" : " skills") + when;
    }
}
