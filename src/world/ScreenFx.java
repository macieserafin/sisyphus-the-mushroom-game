package world;

import main.GamePanel;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

public class ScreenFx {
    private static final int WAVE_TICKS = 28;

    private final GamePanel gp;
    private double waveX;
    private double waveY;
    private int waveAge = -1;
    private int aberrationTicks;
    private int aberrationTotal;
    private int[] copy = new int[0];

    public ScreenFx(GamePanel gp) {
        this.gp = gp;
    }

    public void shockwave(double worldX, double worldY) {
        waveX = worldX;
        waveY = worldY;
        waveAge = 0;
    }

    public void aberration(int ticks) {
        if (!gp.comfort.fullFlashes()) {
            return;
        }
        aberrationTicks = Math.max(aberrationTicks, ticks);
        aberrationTotal = Math.max(aberrationTicks, 1);
    }

    public boolean active() {
        return waveAge >= 0 || aberrationTicks > 0;
    }

    public void clear() {
        waveAge = -1;
        aberrationTicks = 0;
    }

    public void update() {
        if (waveAge >= 0 && ++waveAge > WAVE_TICKS) {
            waveAge = -1;
        }
        if (aberrationTicks > 0) {
            aberrationTicks--;
        }
    }

    public void apply(BufferedImage image, double pixelsPerUnit) {
        if (!(image.getRaster().getDataBuffer() instanceof DataBufferInt buffer)) {
            return;
        }
        int[] pixels = buffer.getData();
        int width = image.getWidth();
        int height = image.getHeight();
        if (copy.length != pixels.length) {
            copy = new int[pixels.length];
        }
        if (waveAge >= 0) {
            System.arraycopy(pixels, 0, copy, 0, pixels.length);
            applyWave(pixels, width, height, pixelsPerUnit);
        }
        if (aberrationTicks > 0) {
            System.arraycopy(pixels, 0, copy, 0, pixels.length);
            applyAberration(pixels, width, height, pixelsPerUnit);
        }
    }

    private void applyWave(int[] pixels, int width, int height, double pixelsPerUnit) {
        double t = waveAge / (double) WAVE_TICKS;
        int cameraX = gp.player.worldX - gp.player.screenX;
        int cameraY = gp.player.worldY - gp.player.screenY;
        double cx = (waveX - cameraX) * pixelsPerUnit;
        double cy = (waveY - cameraY) * pixelsPerUnit;
        double radius = (30 + 280 * t) * pixelsPerUnit;
        double band = 28 * pixelsPerUnit;
        double amplitude = 8 * pixelsPerUnit * (1 - t);
        int x0 = Math.max(0, (int) (cx - radius - band));
        int x1 = Math.min(width - 1, (int) (cx + radius + band));
        int y0 = Math.max(0, (int) (cy - radius - band));
        int y1 = Math.min(height - 1, (int) (cy + radius + band));
        for (int y = y0; y <= y1; y++) {
            for (int x = x0; x <= x1; x++) {
                double dx = x - cx;
                double dy = y - cy;
                double distance = Math.sqrt(dx * dx + dy * dy);
                double offset = distance - radius;
                if (distance < 1 || Math.abs(offset) >= band) {
                    continue;
                }
                double shift = amplitude * Math.sin(offset / band * Math.PI);
                int sx = (int) Math.round(x - dx / distance * shift);
                int sy = (int) Math.round(y - dy / distance * shift);
                if (sx >= 0 && sy >= 0 && sx < width && sy < height) {
                    pixels[y * width + x] = copy[sy * width + sx];
                }
            }
        }
    }

    private void applyAberration(int[] pixels, int width, int height, double pixelsPerUnit) {
        int shift = Math.max(1, (int) Math.round(3 * pixelsPerUnit * aberrationTicks / (double) aberrationTotal));
        for (int y = 0; y < height; y++) {
            int row = y * width;
            for (int x = 0; x < width; x++) {
                int red = copy[row + Math.min(width - 1, x + shift)] & 0xFF0000;
                int blue = copy[row + Math.max(0, x - shift)] & 0x0000FF;
                pixels[row + x] = (copy[row + x] & 0xFF00FF00) | red | blue;
            }
        }
    }
}
