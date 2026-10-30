package main;

import main.ControlSettings.Action;

import java.awt.*;
import java.awt.event.KeyEvent;

// Options screen (title and pause menu). Two pages: the options themselves and the key bindings.
// handle() returns true when the player leaves the options.
public class OptionsMenu {
    private static final int DISPLAY = 0, SCALING = 1, MUSIC = 2, EFFECTS = 3, MUTE = 4, CONTROLS = 5,
            KEYS = 6, LIGHTING = 7, DETAIL = 8, MOTION = 9, SHAKE = 10, FLASH = 11, FONT = 12, RESET = 13, BACK = 14,
            ITEM_COUNT = 15;
    private static final int BAR_SEGMENTS = 10;
    private static final int KEY_ROWS = Action.values().length + 2; // the actions, then "Reset keys" and "Back"

    private final GamePanel gp;
    private final Font itemFont = UiFonts.of(Font.BOLD, 16);
    private final Font keyFont = UiFonts.of(Font.BOLD, 14);
    private final Font hintFont = UiFonts.of(Font.PLAIN, 12);
    private int selected = DISPLAY;
    private boolean keysPage;
    private int keySelected;
    private Action capturing;
    private String feedback = "";

    public OptionsMenu(GamePanel gp) {
        this.gp = gp;
    }

    public void open() {
        selected = DISPLAY;
        keysPage = false;
        capturing = null;
        feedback = "";
    }

    public boolean handle(int code) {
        if (keysPage) {
            handleKeys(code);
            return false;
        }
        switch (code) {
            case KeyEvent.VK_ESCAPE:
                return true;
            case KeyEvent.VK_UP:
            case KeyEvent.VK_W:
                selected = (selected + ITEM_COUNT - 1) % ITEM_COUNT;
                break;
            case KeyEvent.VK_DOWN:
            case KeyEvent.VK_S:
                selected = (selected + 1) % ITEM_COUNT;
                break;
            case KeyEvent.VK_LEFT:
            case KeyEvent.VK_A:
                adjust(-1);
                break;
            case KeyEvent.VK_RIGHT:
            case KeyEvent.VK_D:
                adjust(1);
                break;
            case KeyEvent.VK_ENTER:
                if (selected == BACK) {
                    return true;
                }
                adjust(1);
                break;
            default:
                break;
        }
        return false;
    }

    private void adjust(int direction) {
        switch (selected) {
            case DISPLAY:
                gp.display.setMode(direction < 0 ? gp.display.mode().previous() : gp.display.mode().next());
                break;
            case SCALING:
                gp.visuals.cycleScaling(direction);
                gp.playSoundEffect(Sound.UI_MOVE);
                break;
            case LIGHTING:
                gp.lighting.toggle();
                gp.playSoundEffect(Sound.UI_MOVE);
                break;
            case DETAIL:
                gp.visuals.cycleDetail(direction);
                gp.playSoundEffect(Sound.UI_MOVE);
                break;
            case MOTION:
                gp.visuals.toggleReduceMotion();
                gp.playSoundEffect(Sound.UI_MOVE);
                break;
            case FONT:
                gp.visuals.togglePixelFont();
                UiFonts.refresh(gp.visuals.pixelFont());
                gp.playSoundEffect(Sound.UI_MOVE);
                break;
            case MUSIC:
                gp.audio.changeMusic(direction);
                break;
            case EFFECTS:
                gp.audio.changeEffects(direction);
                gp.playSoundEffect(Sound.UI_MOVE);
                break;
            case MUTE:
                gp.audio.toggleMute();
                break;
            case CONTROLS:
                gp.controls.toggleModeKeys();
                gp.playSoundEffect(Sound.UI_MOVE);
                break;
            case KEYS:
                keysPage = true;
                keySelected = 0;
                capturing = null;
                feedback = "";
                gp.playSoundEffect(Sound.UI_CONFIRM);
                break;
            case SHAKE:
                gp.comfort.cycleShake(direction);
                gp.playSoundEffect(Sound.UI_MOVE);
                break;
            case FLASH:
                gp.comfort.toggleFlashes();
                gp.playSoundEffect(Sound.UI_MOVE);
                break;
            case RESET:
                gp.display.setMode(DisplayManager.Mode.WINDOWED);
                gp.audio.reset();
                gp.controls.resetAll();
                gp.comfort.reset();
                gp.visuals.reset();
                UiFonts.refresh(gp.visuals.pixelFont());
                if (!gp.lighting.enabled()) {
                    gp.lighting.toggle();
                }
                gp.playSoundEffect(Sound.UI_CONFIRM);
                break;
            default:
                break;
        }
    }

    // ------------------------------------------------------------------ key bindings page

    private void handleKeys(int code) {
        if (capturing != null) {
            if (code == KeyEvent.VK_ESCAPE) {
                capturing = null;
                feedback = "";
            } else if (gp.controls.rebind(capturing, code)) {
                feedback = capturing.label + " is now " + gp.controls.keyName(capturing);
                capturing = null;
                gp.playSoundEffect(Sound.UI_CONFIRM);
            } else {
                feedback = "That key is reserved, pick another";
                gp.playSoundEffect(Sound.UI_ERROR);
            }
            return;
        }
        switch (code) {
            case KeyEvent.VK_ESCAPE -> leaveKeys();
            case KeyEvent.VK_UP, KeyEvent.VK_W -> keySelected = (keySelected + KEY_ROWS - 1) % KEY_ROWS;
            case KeyEvent.VK_DOWN, KeyEvent.VK_S -> keySelected = (keySelected + 1) % KEY_ROWS;
            case KeyEvent.VK_ENTER -> {
                Action[] actions = Action.values();
                if (keySelected < actions.length) {
                    capturing = actions[keySelected];
                    feedback = "Press the new key for \"" + capturing.label + "\"";
                } else if (keySelected == actions.length) {
                    gp.controls.resetBindings();
                    feedback = "Keys back to the defaults";
                    gp.playSoundEffect(Sound.UI_CONFIRM);
                } else {
                    leaveKeys();
                }
            }
            default -> {
            }
        }
    }

    private void leaveKeys() {
        keysPage = false;
        feedback = "";
        gp.playSoundEffect(Sound.UI_BACK);
    }

    // ------------------------------------------------------------------ drawing

    public void draw(Graphics2D g2, int y) {
        if (keysPage) {
            drawKeys(g2, y);
            return;
        }
        int width = 402;
        int rowH = 26;
        int height = 30 + ITEM_COUNT * rowH + 18;
        int x = gp.screenWidth / 2 - width / 2;
        y = Math.min(y, gp.screenHeight - height - 56);
        UiSkin.frame(g2, x, y, width, height);
        UiSkin.header(g2, gp.screenWidth / 2, y - 14, "OPTIONS");

        g2.setFont(itemFont);
        for (int i = 0; i < ITEM_COUNT; i++) {
            int rowY = y + 24 + i * rowH;
            boolean active = i == selected;
            if (active) {
                UiSkin.plaque(g2, x + 18, rowY, width - 36, 24, null, UiSkin.GLOW);
            }
            g2.setColor(active ? UiSkin.TEXT : UiSkin.TEXT_DIM);
            int textY = rowY + 18;
            int right = x + width - 33;
            switch (i) {
                case DISPLAY:
                    g2.drawString("Display mode", x + 33, textY);
                    drawValue(g2, "< " + gp.display.mode().label + " >", right, textY, active);
                    break;
                case SCALING:
                    g2.drawString("Scaling", x + 33, textY);
                    drawValue(g2, "< " + gp.visuals.scaling().label + " >", right, textY, active);
                    break;
                case LIGHTING:
                    g2.drawString("Lighting", x + 33, textY);
                    drawValue(g2, "< " + (gp.lighting.enabled() ? "On" : "Off") + " >", right, textY, active);
                    break;
                case DETAIL:
                    g2.drawString("World detail", x + 33, textY);
                    drawValue(g2, "< " + gp.visuals.detail().label + " >", right, textY, active);
                    break;
                case MOTION:
                    g2.drawString("Reduce motion", x + 33, textY);
                    drawValue(g2, "< " + (gp.visuals.reduceMotion() ? "On" : "Off") + " >", right, textY, active);
                    break;
                case FONT:
                    g2.drawString("Text font (restart)", x + 33, textY);
                    drawValue(g2, "< " + (gp.visuals.pixelFont() ? "Pixel" : "System") + " >", right, textY, active);
                    break;
                case MUSIC:
                    g2.drawString("Music volume", x + 33, textY);
                    drawVolume(g2, gp.audio.musicPercent(), right, rowY - 1, active);
                    break;
                case EFFECTS:
                    g2.drawString("Effects volume", x + 33, textY);
                    drawVolume(g2, gp.audio.effectsPercent(), right, rowY - 1, active);
                    break;
                case MUTE:
                    g2.drawString("Mute", x + 33, textY);
                    drawValue(g2, "< " + (gp.audio.isMuted() ? "On" : "Off") + " >", right, textY, active);
                    break;
                case CONTROLS:
                    g2.drawString("Weapon modes", x + 33, textY);
                    drawValue(g2, "< " + gp.controls.modeKeys().label + " >", right, textY, active);
                    break;
                case KEYS:
                    g2.drawString("Key bindings", x + 33, textY);
                    drawValue(g2, ">", right, textY, active);
                    break;
                case SHAKE:
                    g2.drawString("Screen shake", x + 33, textY);
                    drawValue(g2, "< " + (gp.comfort.shakePercent() == 0 ? "Off" : gp.comfort.shakePercent() + "%")
                            + " >", right, textY, active);
                    break;
                case FLASH:
                    g2.drawString("Screen flashes", x + 33, textY);
                    drawValue(g2, "< " + (gp.comfort.fullFlashes() ? "Full" : "Reduced") + " >", right, textY, active);
                    break;
                case RESET:
                    g2.drawString("Reset options to defaults", x + 33, textY);
                    break;
                default:
                    g2.drawString("Back", x + 33, textY);
                    break;
            }
        }
    }

    private void drawKeys(Graphics2D g2, int y) {
        int width = 402;
        int rowH = 23;
        int height = 30 + KEY_ROWS * rowH + 40;
        int x = gp.screenWidth / 2 - width / 2;
        y = Math.min(y, gp.screenHeight - height - 56);
        UiSkin.frame(g2, x, y, width, height);
        UiSkin.header(g2, gp.screenWidth / 2, y - 14, "KEY BINDINGS");

        Action[] actions = Action.values();
        g2.setFont(keyFont);
        for (int i = 0; i < KEY_ROWS; i++) {
            int rowY = y + 22 + i * rowH;
            boolean active = i == keySelected;
            if (active) {
                UiSkin.plaque(g2, x + 18, rowY, width - 36, rowH - 2, null, UiSkin.GLOW);
            }
            g2.setColor(active ? UiSkin.TEXT : UiSkin.TEXT_DIM);
            int textY = rowY + 16;
            if (i < actions.length) {
                g2.drawString(actions[i].label, x + 33, textY);
                boolean waiting = capturing == actions[i];
                drawValue(g2, waiting ? "press a key..." : gp.controls.keyName(actions[i]), x + width - 33, textY,
                        active);
            } else {
                g2.drawString(i == actions.length ? "Reset keys to defaults" : "Back", x + 33, textY);
            }
        }
        g2.setFont(hintFont);
        g2.setColor(new Color(0x9CC890));
        g2.drawString(feedback, x + 27, y + height - 14);
    }

    private void drawValue(Graphics2D g2, String value, int right, int textY, boolean active) {
        g2.setColor(active ? UiSkin.GOLD : UiSkin.TEXT_DIM);
        g2.drawString(value, right - g2.getFontMetrics().stringWidth(value), textY);
    }

    private void drawVolume(Graphics2D g2, int percent, int right, int rowY, boolean active) {
        String label = percent + "%";
        int labelWidth = g2.getFontMetrics().stringWidth("100%");
        g2.setColor(active ? UiSkin.GOLD : UiSkin.TEXT_DIM);
        g2.drawString(label, right - g2.getFontMetrics().stringWidth(label), rowY + 20);

        int segment = 9;
        int gap = 2;
        int barWidth = BAR_SEGMENTS * (segment + gap) - gap;
        int barX = right - labelWidth - 12 - barWidth;
        int filled = Math.round(percent / 100f * BAR_SEGMENTS);
        for (int i = 0; i < BAR_SEGMENTS; i++) {
            Color on = gp.audio.isMuted() ? UiSkin.TEXT_DIM : (active ? UiSkin.GOLD : UiSkin.TEXT);
            g2.setColor(i < filled ? on : new Color(0x4A3A32));
            g2.fillRect(barX + i * (segment + gap), rowY + 8, segment, 12);
        }
    }
}
