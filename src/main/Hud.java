package main;

import entity.CombatState;
import entity.enemy.Enemy;
import entity.npc.NPC;
import story.QuestLog;
import tile.MapFile;
import tile.Tile;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

public class Hud {
    private static final int MARGIN = 12;
    private static final int BADGE_SIZE = 60;
    private static final int ABILITY_SIZE = 34;
    private static final int MINIMAP_SIZE = 120;
    private static final int RIGHT_PANEL_WIDTH = 156;
    private static final int QUESTS_SHOWN = 4;
    private static final int QUEST_PANEL_WIDTH = 184;
    private static final Color SIDE_QUEST_COLOR = new Color(0x8FD0C8);

    private final GamePanel gp;
    private final Font smallFont = UiFonts.of(Font.PLAIN, 12);
    private final Font boldFont = UiFonts.of(Font.BOLD, 15);
    private final Font questTitleFont = UiFonts.of(Font.BOLD, 12);
    private final Font questTextFont = UiFonts.of(Font.PLAIN, 11);
    private final Color[] tileColors;

    private int[][] minimapGround;
    private int[][] minimapObjects;
    private BufferedImage minimapImage;

    public Hud(GamePanel gp) {
        this.gp = gp;
        tileColors = new Color[gp.tileM.tile.length];
    }

    public void draw(Graphics2D g2) {
        Object oldTextAa = g2.getRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING);
        Stroke oldStroke = g2.getStroke();
        Font oldFont = g2.getFont();
        Color oldColor = g2.getColor();
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        drawStatus(g2, MARGIN, MARGIN);

        int rightX = gp.screenWidth - MARGIN - RIGHT_PANEL_WIDTH;
        int rightBottom = drawMinimap(g2, rightX, MARGIN + 14);
        drawQuests(g2, rightX + RIGHT_PANEL_WIDTH - QUEST_PANEL_WIDTH, rightBottom + 26);

        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, oldTextAa);
        g2.setStroke(oldStroke);
        g2.setFont(oldFont);
        g2.setColor(oldColor);
    }

    private void drawStatus(Graphics2D g2, int x, int y) {
        boolean melee = gp.player.combatState == CombatState.MELEE;
        boolean locked = gp.player.isWeaponsLocked();

        UiSkin.badge(g2, x, y, BADGE_SIZE, locked ? UiSkin.DIM : UiSkin.RED);
        UiSkin.icon(g2, iconFor(melee, melee ? meleeSlot() : rangedSlot()),
                x + BADGE_SIZE / 2, y + BADGE_SIZE / 2, 3, !locked);
        if (locked) {
            UiSkin.icon(g2, UiSkin.Icon.LOCK, x + BADGE_SIZE - 10, y + BADGE_SIZE - 10, 2, true);
        }

        int heartsX = x + BADGE_SIZE + 8;
        int max = Math.max(0, gp.player.maxHealth);
        int hp = Math.max(0, Math.min(gp.player.health, max));
        for (int i = 0; i < max; i++) {
            UiSkin.heart(g2, heartsX + i * (UiSkin.HEART_W + 3), y + 2, i < hp);
        }
        int shieldX = heartsX + max * (UiSkin.HEART_W + 3) + 6;
        int shield = gp.player.shield();
        for (int i = 0; i < gp.player.maxShield(); i++) {
            double fill = i < shield ? 1 : i == shield ? gp.player.shieldRegenProgress() : 0;
            UiSkin.shield(g2, shieldX + i * (UiSkin.SHIELD_W + 3), y + 1, fill);
        }

        int tabY = y + 36;
        UiSkin.plaque(g2, heartsX, tabY, 84, 27, "MELEE", melee ? UiSkin.GLOW : UiSkin.DIM);
        UiSkin.plaque(g2, heartsX + 87, tabY, 84, 27, "RANGE", melee ? UiSkin.DIM : UiSkin.GLOW);
        int slotY = tabY + 33;
        drawAbility(g2, heartsX, slotY, UiSkin.Icon.ROLL, gp.player.rollReadiness(), melee, gp.progression.hasSkill(Skill.ROLL));
        drawAbility(g2, heartsX + ABILITY_SIZE + 6, slotY, UiSkin.Icon.DASH, gp.player.dashReadiness(), !melee, gp.progression.hasSkill(Skill.DASH));
        drawMushrooms(g2, heartsX + 2 * ABILITY_SIZE + 16, slotY);

        if (gp.player.hasKey > 0) {
            int keyX = heartsX + 177;
            g2.drawImage(gp.ui.keyImage, keyX, tabY + 4, 18, 18, null);
            g2.setFont(boldFont);
            g2.setColor(UiSkin.GOLD);
            g2.drawString("" + gp.player.hasKey, keyX + 20, tabY + 19);
        }
    }

    // Mushroom counter: in a run the mushrooms carried (not safe yet) above the bank, in the hub only the bank.
    private void drawMushrooms(Graphics2D g2, int x, int y) {
        boolean inRun = gp.levelManager.isRunActive();
        UiSkin.icon(g2, UiSkin.Icon.MUSHROOM, x + 12, y + ABILITY_SIZE / 2, 2, true);
        int textX = x + 30;
        shadowText(g2, String.valueOf(inRun ? gp.wallet.run() : gp.wallet.banked()), textX, y + 17, boldFont, UiSkin.GOLD);
        shadowText(g2, inRun ? "bank " + gp.wallet.banked() : "banked", textX, y + 31, smallFont, UiSkin.TEXT_DIM);
    }

    private static void shadowText(Graphics2D g2, String text, int x, int y, Font font, Color color) {
        g2.setFont(font);
        g2.setColor(new Color(0, 0, 0, 170));
        g2.drawString(text, x + 1, y + 1);
        g2.setColor(color);
        g2.drawString(text, x, y);
    }

    // Ability square (roll for melee, dash for ranged): it fills from the bottom while recharging and the icon lights up
    // once it is full and usable in the current mode. Not learned yet: dark, with a lock.
    private void drawAbility(Graphics2D g2, int x, int y, UiSkin.Icon icon, double readiness, boolean active, boolean learned) {
        if (!learned) {
            UiSkin.inset(g2, x, y, ABILITY_SIZE, ABILITY_SIZE);
            UiSkin.icon(g2, icon, x + ABILITY_SIZE / 2, y + ABILITY_SIZE / 2, 2, false);
            UiSkin.icon(g2, UiSkin.Icon.LOCK, x + ABILITY_SIZE - 8, y + ABILITY_SIZE - 8, 1, true);
            return;
        }
        double fill = Math.max(0, Math.min(1, readiness));
        boolean ready = fill >= 1;
        UiSkin.inset(g2, x, y, ABILITY_SIZE, ABILITY_SIZE);
        int inner = ABILITY_SIZE - 8;
        int filled = (int) Math.round(inner * fill);
        g2.setColor(!ready ? new Color(0x6A5C52) : active ? new Color(0x8E2422) : new Color(0x47373A));
        g2.fillRect(x + 4, y + 4 + inner - filled, inner, filled);
        UiSkin.icon(g2, icon, x + ABILITY_SIZE / 2, y + ABILITY_SIZE / 2, 2, ready && active);
    }

    private int meleeSlot() {
        return gp.player.currentMeleeSlot();
    }

    private int rangedSlot() {
        return gp.player.currentRangedSlot();
    }

    private static UiSkin.Icon iconFor(boolean melee, int slot) {
        if (melee) {
            return slot == 0 ? UiSkin.Icon.SWORD : UiSkin.Icon.HEAVY_SWORD;
        }
        return slot == 0 ? UiSkin.Icon.PISTOL : UiSkin.Icon.SHOTGUN;
    }

    private int drawMinimap(Graphics2D g2, int x, int y) {
        int panelH = RIGHT_PANEL_WIDTH;
        UiSkin.frame(g2, x, y, RIGHT_PANEL_WIDTH, panelH);
        int wellX = x + 15;
        int wellY = y + 15;
        UiSkin.inset(g2, wellX, wellY, MINIMAP_SIZE + 6, MINIMAP_SIZE + 6);
        UiSkin.header(g2, x + RIGHT_PANEL_WIDTH / 2, y - 14, "MAP");

        int cols = gp.tileM.getWorldCols();
        int rows = gp.tileM.getWorldRows();
        if (cols <= 0 || rows <= 0) {
            return y + panelH;
        }
        refreshMinimapImage();

        double scale = Math.min((double) MINIMAP_SIZE / cols, (double) MINIMAP_SIZE / rows);
        int drawW = (int) Math.round(cols * scale);
        int drawH = (int) Math.round(rows * scale);
        int mapX = wellX + 3 + (MINIMAP_SIZE - drawW) / 2;
        int mapY = wellY + 3 + (MINIMAP_SIZE - drawH) / 2;

        Object oldInterp = g2.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g2.drawImage(minimapImage, mapX, mapY, drawW, drawH, null);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, oldInterp);

        double worldToMap = scale / gp.tileSize;
        List<NPC> npcs = gp.entities.npcs();
        g2.setColor(UiSkin.GOLD);
        for (NPC npc : npcs) {
            drawDot(g2, mapX, mapY, npc.worldX + gp.tileSize / 2, npc.worldY + gp.tileSize / 2, worldToMap, 3);
        }
        g2.setColor(new Color(235, 60, 60));
        for (Enemy enemy : gp.entities.enemies()) {
            if (enemy.alive) {
                drawDot(g2, mapX, mapY, enemy.worldX + gp.tileSize / 2, enemy.worldY + gp.tileSize / 2, worldToMap, 3);
            }
        }

        int px = gp.player.worldX + gp.tileSize / 2;
        int py = gp.player.worldY + gp.tileSize / 2;
        g2.setColor(Color.BLACK);
        drawDot(g2, mapX, mapY, px, py, worldToMap, 7);
        g2.setColor(UiSkin.CREAM);
        drawDot(g2, mapX, mapY, px, py, worldToMap, 5);
        return y + panelH;
    }

    private void drawDot(Graphics2D g2, int mapX, int mapY, int worldX, int worldY, double worldToMap, int size) {
        int dx = mapX + (int) Math.round(worldX * worldToMap) - size / 2;
        int dy = mapY + (int) Math.round(worldY * worldToMap) - size / 2;
        g2.fillRect(dx, dy, size, size);
    }

    private void refreshMinimapImage() {
        int[][] ground = gp.tileM.mapTileNum;
        int[][] objects = gp.tileM.objectTileNum;
        if (minimapImage != null && ground == minimapGround && objects == minimapObjects) {
            return;
        }
        int cols = gp.tileM.getWorldCols();
        int rows = gp.tileM.getWorldRows();
        BufferedImage image = new BufferedImage(cols, rows, BufferedImage.TYPE_INT_ARGB);
        for (int col = 0; col < cols; col++) {
            for (int row = 0; row < rows; row++) {
                int id = ground[col][row];
                int object = objects[col][row];
                if (object != MapFile.EMPTY && gp.tileM.tile[object].collision) {
                    id = object;
                }
                image.setRGB(col, row, tileColor(id).getRGB());
            }
        }
        minimapImage = image;
        minimapGround = ground;
        minimapObjects = objects;
    }

    private Color tileColor(int id) {
        if (tileColors[id] == null) {
            Tile tile = gp.tileM.tile[id];
            BufferedImage img = tile.image;
            long r = 0, g = 0, b = 0, n = 0;
            if (img != null) {
                for (int py = 0; py < img.getHeight(); py++) {
                    for (int px = 0; px < img.getWidth(); px++) {
                        int argb = img.getRGB(px, py);
                        if ((argb >>> 24) < 128) {
                            continue;
                        }
                        r += (argb >> 16) & 0xFF;
                        g += (argb >> 8) & 0xFF;
                        b += argb & 0xFF;
                        n++;
                    }
                }
            }
            tileColors[id] = n == 0 ? new Color(40, 40, 48) : new Color((int) (r / n), (int) (g / n), (int) (b / n));
        }
        return tileColors[id];
    }

    // Tracker under the minimap: only the names of the active quests (story first); objectives and progress
    // live in the journal (J).
    private void drawQuests(Graphics2D g2, int x, int y) {
        List<QuestLog.Quest> active = gp.questLog.active();
        int shown = Math.min(QUESTS_SHOWN, active.size());
        int lineH = 18;
        int height = 30 + Math.max(1, shown) * lineH + 20;
        UiSkin.frame(g2, x, y, QUEST_PANEL_WIDTH, height);
        UiSkin.header(g2, x + QUEST_PANEL_WIDTH / 2, y - 14, "QUESTS");

        int textX = x + 16;
        int textW = QUEST_PANEL_WIDTH - 32;
        int ty = y + 34;
        g2.setFont(questTitleFont);
        if (shown == 0) {
            g2.setColor(UiSkin.TEXT_DIM);
            g2.drawString("No active quests", textX, ty);
        }
        for (int i = 0; i < shown; i++) {
            QuestLog.Quest quest = active.get(i);
            g2.setColor(quest.main ? UiSkin.GOLD : SIDE_QUEST_COLOR);
            g2.fillRect(textX, ty - 7, 4, 4);
            g2.setColor(UiSkin.TEXT);
            g2.drawString(trim(g2.getFontMetrics(), quest.title, textW - 9), textX + 9, ty);
            ty += lineH;
        }
        g2.setFont(questTextFont);
        g2.setColor(UiSkin.TEXT_DIM);
        String more = active.size() > shown ? "+" + (active.size() - shown) + " more   " : "";
        g2.drawString(more + "[J] Journal", textX, y + height - 12);
    }

    private String trim(FontMetrics fm, String text, int width) {
        if (fm.stringWidth(text) <= width) {
            return text;
        }
        String result = text;
        while (!result.isEmpty() && fm.stringWidth(result + "...") > width) {
            result = result.substring(0, result.length() - 1);
        }
        return result + "...";
    }
}
