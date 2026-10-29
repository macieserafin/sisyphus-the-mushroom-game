package main;

import java.awt.*;
import java.awt.event.KeyEvent;

public class OptionsMenu {
    private static final int DISPLAY = 0, MUSIC = 1, EFFECTS = 2, MUTE = 3, CONTROLS = 4, BACK = 5, ITEM_COUNT = 6;
    private static final int BAR_SEGMENTS = 10;

    private final GamePanel gp;
    private final Font itemFont = new Font("SansSerif", Font.BOLD, 16);
    private int selected = DISPLAY;

    public OptionsMenu(GamePanel gp) {
        this.gp = gp;
    }

    public void open() {
        selected = DISPLAY;
    }

    public boolean handle(int code) {
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
            case MUSIC:
                gp.audio.changeMusic(direction);
                break;
            case EFFECTS:
                gp.audio.changeEffects(direction);
                gp.playSoundEffect(Sound.COIN);
                break;
            case MUTE:
                gp.audio.toggleMute();
                break;
            case CONTROLS:
                gp.controls.toggleModeKeys();
                gp.playSoundEffect(Sound.COIN);
                break;
            default:
                break;
        }
    }

    public void draw(Graphics2D g2, int y) {
        int width = 402;
        int rowH = 32;
        int height = 30 + ITEM_COUNT * rowH + 18;
        int x = gp.screenWidth / 2 - width / 2;
        UiSkin.frame(g2, x, y, width, height);
        UiSkin.header(g2, gp.screenWidth / 2, y - 14, "OPTIONS");

        g2.setFont(itemFont);
        for (int i = 0; i < ITEM_COUNT; i++) {
            int rowY = y + 24 + i * rowH;
            boolean active = i == selected;
            if (active) {
                UiSkin.plaque(g2, x + 18, rowY, width - 36, 28, null, UiSkin.GLOW);
            }
            g2.setColor(active ? UiSkin.TEXT : UiSkin.TEXT_DIM);
            int textY = rowY + 20;
            int right = x + width - 33;
            switch (i) {
                case DISPLAY:
                    g2.drawString("Display mode", x + 33, textY);
                    drawValue(g2, "< " + gp.display.mode().label + " >", right, textY, active);
                    break;
                case MUSIC:
                    g2.drawString("Music volume", x + 33, textY);
                    drawVolume(g2, gp.audio.musicPercent(), right, rowY, active);
                    break;
                case EFFECTS:
                    g2.drawString("Effects volume", x + 33, textY);
                    drawVolume(g2, gp.audio.effectsPercent(), right, rowY, active);
                    break;
                case MUTE:
                    g2.drawString("Mute", x + 33, textY);
                    drawValue(g2, "< " + (gp.audio.isMuted() ? "On" : "Off") + " >", right, textY, active);
                    break;
                case CONTROLS:
                    g2.drawString("Weapon modes", x + 33, textY);
                    drawValue(g2, "< " + gp.controls.modeKeys().label + " >", right, textY, active);
                    break;
                default:
                    g2.drawString("Back", x + 33, textY);
                    break;
            }
        }
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
