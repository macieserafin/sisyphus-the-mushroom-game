package entity.anim;

import entity.Direction8;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public final class PlayerAnimationGallery {
    private static final String[] STATES = {"idle", "walk", "sprint", "attack", "shoot", "hurt", "death", "roll", "dash"};
    private static final String[] NPCS = {"wanderer_mara", "guard", "blacksmith", "merchant", "karate_master", "mycologist",
            "sage", "oldman", "villager_bram", "villager_fern", "villager_moss", "villager_pip"};
    private static final String[] NPC_DIRECTIONS = {"down", "right", "up", "left"};
    private static final Map<String, BufferedImage> NPC_CACHE = new HashMap<>();
    private static final Direction8[] ORDER = {Direction8.S, Direction8.SE, Direction8.E, Direction8.NE,
            Direction8.N, Direction8.NW, Direction8.W, Direction8.SW};
    private static final int MIN_TICKS_PER_FRAME = 6;
    private static final long NANOS_PER_TICK = 1_000_000_000L / 60;

    private PlayerAnimationGallery() {
    }

    public static int stateCount() {
        return STATES.length + NPCS.length;
    }

    public static String stateName(int state) {
        int index = Math.floorMod(state, stateCount());
        return index < STATES.length ? STATES[index] : "npc " + NPCS[index - STATES.length];
    }

    public static void draw(Graphics2D g2, int state, int scale, int centerX, int top, boolean slow) {
        int index = Math.floorMod(state, stateCount());
        if (index >= STATES.length) {
            drawNpc(g2, NPCS[index - STATES.length], scale, centerX, top, slow);
            return;
        }
        SpriteSheet sheet = SpriteSheet.loadOptional("player/anim/player_" + STATES[index]);
        int cell = 32 * scale;
        int width = cell * ORDER.length;
        int x = centerX - width / 2;
        Color oldColor = g2.getColor();
        Font oldFont = g2.getFont();
        g2.setColor(new Color(12, 16, 12, 215));
        g2.fillRect(x - 6, top - 4, width + 12, cell + 24);
        g2.setFont(new Font(Font.MONOSPACED, Font.BOLD, 12));
        if (sheet == null) {
            g2.setColor(Color.WHITE);
            g2.drawString(stateName(state) + ": -", x, top + 12);
            g2.setColor(oldColor);
            g2.setFont(oldFont);
            return;
        }
        int ticksPerFrame = Math.max(MIN_TICKS_PER_FRAME, sheet.frameTicks) * (slow ? 4 : 1);
        long tick = System.nanoTime() / NANOS_PER_TICK;
        int frameIndex = (int) ((tick / ticksPerFrame) % sheet.frameCount);
        for (int i = 0; i < ORDER.length; i++) {
            SpriteSheet.Frame frame = sheet.frame(ORDER[i], frameIndex);
            int footX = x + i * cell + cell / 2;
            int footY = top + 14 + sheet.anchorY * scale;
            boolean behind = ORDER[i].dy < 0;
            if (behind) {
                drawArm(g2, frame, ORDER[i], footX, footY, scale);
            }
            frame.draw(g2, footX, footY, scale);
            if (!behind) {
                drawArm(g2, frame, ORDER[i], footX, footY, scale);
            }
            g2.setColor(new Color(220, 220, 200));
            g2.drawString(ORDER[i].name(), x + i * cell + 4, top + 12);
        }
        g2.setColor(new Color(156, 200, 144));
        String label = stateName(state) + "  " + (frameIndex + 1) + "/" + sheet.frameCount + (slow ? "  x0.25" : "");
        g2.drawString(label, x + width - g2.getFontMetrics().stringWidth(label), top + 12);
        g2.setColor(oldColor);
        g2.setFont(oldFont);
    }

    private static void drawNpc(Graphics2D g2, String name, int scale, int centerX, int top, boolean slow) {
        int cell = 32 * scale;
        int width = cell * ORDER.length;
        int step = width / NPC_DIRECTIONS.length;
        int x = centerX - width / 2;
        Color oldColor = g2.getColor();
        Font oldFont = g2.getFont();
        g2.setColor(new Color(12, 16, 12, 215));
        g2.fillRect(x - 6, top - 4, width + 12, cell + 24);
        g2.setFont(new Font(Font.MONOSPACED, Font.BOLD, 12));
        long tick = System.nanoTime() / NANOS_PER_TICK;
        int frame = (int) ((tick / (16L * (slow ? 4 : 1))) % 2) + 1;
        for (int i = 0; i < NPC_DIRECTIONS.length; i++) {
            BufferedImage image = npcImage(name + "_" + NPC_DIRECTIONS[i] + "_" + frame);
            if (image != null) {
                int size = image.getWidth() * scale;
                g2.drawImage(image, x + i * step + (step - size) / 2, top + 14 + cell - size - 2 * scale, size, size, null);
            }
            g2.setColor(new Color(220, 220, 200));
            g2.drawString(NPC_DIRECTIONS[i], x + i * step + 4, top + 12);
        }
        g2.setColor(new Color(156, 200, 144));
        String label = name + "  " + frame + "/2";
        g2.drawString(label, x + width - g2.getFontMetrics().stringWidth(label), top + cell + 14);
        g2.setColor(oldColor);
        g2.setFont(oldFont);
    }

    private static synchronized BufferedImage npcImage(String frame) {
        return NPC_CACHE.computeIfAbsent(frame, key -> {
            try (InputStream stream = ArtFiles.open("npc/" + key + ".png")) {
                return stream == null ? null : ImageIO.read(stream);
            } catch (IOException e) {
                return null;
            }
        });
    }

    private static void drawArm(Graphics2D g2, SpriteSheet.Frame frame, Direction8 dir, int footX, int footY, int scale) {
        if (!frame.hasShoulder()) {
            return;
        }
        int shoulderX = frame.shoulderScreenX(footX, scale, 0);
        int shoulderY = frame.shoulderScreenY(footY, scale, 0);
        int[] hand = WeaponRenderer.armHand(dir.angle(), frame.sheet.armReach, 0);
        WeaponRenderer.drawArm(g2, frame.sheet, shoulderX, shoulderY, hand, scale);
        WeaponRenderer.drawFist(g2, frame.sheet, shoulderX, shoulderY, hand, scale);
    }
}
