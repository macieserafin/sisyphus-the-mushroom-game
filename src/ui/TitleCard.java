package ui;

import main.GamePanel;
import java.awt.*;

public final class TitleCard {
    private static final int FADE = 36, TEXT = 30, HOLD = 120;
    private final GamePanel gp;
    private String text = "";
    private int tick = -1, start;
    private boolean keepBlack, black;
    private int release = -1, releaseFrames;

    public TitleCard(GamePanel gp) { this.gp = gp; }

    public boolean active() { return tick >= 0 || release >= 0; }
    public boolean black() { return black; }

    public void show(String text, boolean fromBlack, boolean keepBlack) {
        this.text = text;
        this.keepBlack = keepBlack;
        start = fromBlack || black ? FADE : 0;
        tick = start;
        release = -1;
    }

    public void blackout() { black = true; tick = -1; release = -1; text = ""; }

    public void release(int frames) {
        if (!black && tick < 0) return;
        tick = -1; text = ""; black = true;
        releaseFrames = gp.visuals.reduceMotion() ? 1 : Math.max(1, frames);
        release = 0;
    }

    public void clear() { tick = -1; release = -1; black = false; text = ""; }

    private int end() { return FADE + TEXT + HOLD + TEXT; }

    public void update() {
        if (release >= 0 && ++release >= releaseFrames) { release = -1; black = false; }
        if (tick < 0) return;
        tick += gp.visuals.reduceMotion() && tick < FADE ? FADE - tick : 1;
        if (tick >= end()) {
            if (keepBlack) { black = true; tick = -1; text = ""; }
            else release(FADE);
        }
    }

    public void draw(Graphics2D g) {
        float shade;
        if (release >= 0) shade = 1 - release / (float) releaseFrames;
        else if (tick >= 0) shade = tick < FADE ? tick / (float) FADE : 1;
        else shade = black ? 1 : 0;
        if (shade <= 0) return;
        g.setColor(new Color(0, 0, 0, Math.round(255 * Math.min(1, shade))));
        g.fillRect(0, 0, gp.screenWidth, gp.screenHeight);
        if (tick < FADE || text.isBlank()) return;
        int t = tick - FADE;
        float alpha = gp.visuals.reduceMotion() ? 1 : t < TEXT ? t / (float) TEXT : t < TEXT + HOLD ? 1 : 1 - (t - TEXT - HOLD) / (float) TEXT;
        String[] lines = text.split("\\|");
        int scale = 4;
        int y = gp.screenHeight / 2 - lines.length * 12 * scale / 2;
        Composite old = g.getComposite();
        g.setComposite(AlphaComposite.SrcOver.derive(Math.max(0, Math.min(1, alpha))));
        for (String line : lines) {
            int width = UiSkin.PixelFont.width(line, scale);
            UiSkin.PixelFont.draw(g, line, (gp.screenWidth - width) / 2, y, scale, UiSkin.GOLD);
            y += 12 * scale;
            scale = 2;
        }
        g.setComposite(old);
    }
}
