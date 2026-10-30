package main;

import entity.anim.PlayerAnimationGallery;
import dialogue.DialogueLine;
import dialogue.DialogueSpeaker;
import entity.enemy.Enemy;
import object.Key;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UI {
    GamePanel gp;
    Graphics2D g2;
    Font arial_40;
    Font consolas_18;
    BufferedImage keyImage;
    private Hud hud;

    private static final int DIALOGUE_WIDTH = 600;
    private static final int DIALOGUE_HEIGHT = 120;
    private static final int DIALOGUE_BOTTOM_MARGIN = 18;
    private final Font dialogueFont = UiFonts.of(Font.PLAIN, 16);
    private final Map<String, BufferedImage> visualNovelBackgroundCache = new HashMap<>();

    public boolean messageOn = false;
    private final Font toastFont = UiFonts.of(Font.BOLD, 13);
    public String message = "";
    int messageCounter = 0;

    private int blinkClose, blinkHold, blinkOpen;
    private int blinkTick = -1;
    private DialogueLine lastVisualNovelLine;
    private Enemy barBoss;
    private double ghostFraction = 1;
    private double lastBarFraction = 1;
    private int ghostDelay;
    private int barFlash;
    private int uiTick;

    public UI(GamePanel gp) {
        this.gp = gp;

        arial_40 = UiFonts.of(Font.PLAIN, 20);
        consolas_18 = UiFonts.of(Font.PLAIN, 18);

        Key key = new Key();
        keyImage = key.image;
    }

    public void showMessage(String s) {
        message = s;
        messageOn = true;
        messageCounter = 0;
    }

    public void draw(Graphics g2) {
        this.g2 = (Graphics2D) g2;

        if (gp.gameState == GameState.CUTSCENE) {
            drawVisualNovelScene();
            drawBlink();
            return;
        }

        g2.setFont(consolas_18);
        g2.setColor(Color.WHITE);
        if (hud == null) {
            hud = new Hud(gp);
        }
        hud.draw(this.g2);
        g2.setFont(consolas_18);
        g2.setColor(Color.WHITE);
        if (gp.debugSettings.isOverlayEnabled()) {
            Rectangle playerSolidArea = gp.player.getWorldSolidArea();
            int playerTileCol = Math.floorDiv(playerSolidArea.x + playerSolidArea.width / 2, gp.tileSize);
            int playerTileRow = Math.floorDiv(playerSolidArea.y + playerSolidArea.height / 2, gp.tileSize);
            g2.drawString("Tile position = col: " + playerTileCol + " row: " + playerTileRow, 400, 30);
        }

        uiTick++;
        drawBossHealthBar();

        if (messageOn) {
            drawMessageToast();

            messageCounter++;

            if (messageCounter > 60) {
                messageCounter = 0;
                messageOn = false;
            }
        }

        String victory = gp.campaign.victoryBanner();
        if (!victory.isEmpty()) {
            int width = 440, x = (gp.screenWidth - width) / 2, y = gp.screenHeight / 3;
            UiSkin.frame(this.g2, x, y, width, 78);
            this.g2.setFont(UiFonts.of(Font.BOLD, 22));
            this.g2.setColor(UiSkin.GOLD);
            this.g2.drawString(victory, (gp.screenWidth - this.g2.getFontMetrics().stringWidth(victory)) / 2, y + 47);
        }
        if (gp.gameState == GameState.CHOICE) gp.choiceMenu.draw(this.g2);

        if (gp.gameState == GameState.DEAD) {
            drawDeathScreen();
        }

        drawDialogueOverlay();
        if (gp.gameState == GameState.MOD_MENU) {
            gp.modMenu.draw(this.g2);
        }
        if (gp.gameState == GameState.PAUSED) {
            gp.pauseMenu.draw(this.g2);
        }
        if (gp.gameState == GameState.SHOP) {
            gp.shopMenu.draw(this.g2);
        }
        if (gp.gameState == GameState.QUESTS) {
            gp.questMenu.draw(this.g2);
        }
        if (gp.gameState == GameState.SKILLS) {
            gp.skillTreeMenu.draw(this.g2);
        }
        if (gp.debugSettings.playerGallery() >= 0) {
            PlayerAnimationGallery.draw(this.g2, gp.debugSettings.playerGallery(), 2, gp.screenWidth / 2, 8, gp.debugSettings.isGallerySlow());
        }
        if (gp.gameState != GameState.MOD_MENU) drawBlink();
    }

    public void update() {
        if (blinkTick >= 0 && ++blinkTick >= blinkClose + blinkHold + blinkOpen) {
            blinkTick = -1;
        }
    }

    public void clearBlink() {
        blinkTick = -1;
    }

    public void resetVisualNovelScene() {
        lastVisualNovelLine = null;
    }

    public void blink(int closeFrames, int holdFrames, int openFrames) {
        blinkClose = closeFrames;
        blinkHold = holdFrames;
        blinkOpen = openFrames;
        blinkTick = 0;
    }

    private void drawBlink() {
        if (blinkTick < 0) {
            return;
        }

        double shut;
        if (blinkTick < blinkClose) {
            shut = blinkTick / (double) blinkClose;
        } else if (blinkTick < blinkClose + blinkHold) {
            shut = 1;
        } else if (blinkTick < blinkClose + blinkHold + blinkOpen) {
            shut = 1 - (blinkTick - blinkClose - blinkHold) / (double) blinkOpen;
        } else {
            blinkTick = -1;
            return;
        }

        // Ease so the lids snap shut and open slowly, like waking up.
        shut = shut * shut * (3 - 2 * shut);
        int lid = (int) Math.round(shut * (gp.screenHeight / 2.0 + 24));
        Color oldColor = g2.getColor();
        g2.setColor(Color.BLACK);
        for (int x = 0; x < gp.screenWidth; x += 4) {
            double u = (x - gp.screenWidth / 2.0) / (gp.screenWidth / 2.0);
            int bow = (int) Math.round((1 - u * u) * 24 * shut);
            g2.fillRect(x, 0, 4, lid + bow - 24);
            g2.fillRect(x, gp.screenHeight - lid - bow + 24, 4, lid + bow);
        }
        g2.setColor(oldColor);
    }

    private void drawBossHealthBar() {
        Enemy boss = gp.levelManager.getActiveBoss();
        if (boss == null || boss.maxHealth <= 0) {
            return;
        }

        int barWidth = 396;
        int barHeight = 24;
        int x = gp.screenWidth / 2 - barWidth / 2;
        int y = gp.screenHeight - 50;
        int inner = barWidth - 12;
        double fraction = Math.max(0, boss.health / (double) boss.maxHealth);
        int filledWidth = (int) Math.round(inner * fraction);

        // the "ghost" chunk lags behind recent damage, then drains
        if (boss != barBoss) {
            barBoss = boss;
            ghostFraction = fraction;
        }
        if (fraction < ghostFraction - 1e-6) {
            if (fraction < lastBarFraction - 1e-6) {
                ghostDelay = 30;
                barFlash = 6;
            }
            if (ghostDelay > 0) {
                ghostDelay--;
            } else {
                ghostFraction = Math.max(fraction, ghostFraction - 0.004);
            }
        } else {
            ghostFraction = fraction;
        }
        lastBarFraction = fraction;
        int ghostWidth = (int) Math.round(inner * ghostFraction);

        Color oldColor = g2.getColor();

        UiSkin.inset(g2, x, y, barWidth, barHeight);
        g2.setColor(new Color(0x3A1418));
        g2.fillRect(x + 6, y + 6, inner, barHeight - 12);
        g2.setColor(new Color(0xF2E6C4));
        g2.fillRect(x + 6, y + 6, ghostWidth, barHeight - 12);
        g2.setColor(barFlash > 0 ? new Color(0xF2E6C4) : new Color(0xA8302A));
        g2.fillRect(x + 6, y + 6, filledWidth, barHeight - 12);
        g2.setColor(new Color(0xD8584A));
        g2.fillRect(x + 6, y + 6, filledWidth, UiSkin.P);
        if (barFlash > 0) {
            barFlash--;
        }
        for (double notch : boss.phaseThresholds()) {
            int nx = x + 6 + (int) Math.round(inner * notch);
            g2.setColor(new Color(0x120C09));
            g2.fillRect(nx - UiSkin.P / 2, y + 3, UiSkin.P, barHeight - 6);
            g2.setColor(new Color(0xF2C46A));
            g2.fillRect(nx - UiSkin.P / 2, y, UiSkin.P, UiSkin.P);
        }
        UiSkin.header(g2, gp.screenWidth / 2, y - 30, boss.name);

        drawWantedMode(boss, x - 44, y - 5);
        if (boss.adapts()) {
            drawAdaptation(boss, x, y + barHeight + 4, barWidth);
        }

        g2.setColor(oldColor);
    }

    private void drawWantedMode(Enemy boss, int x, int y) {
        entity.CombatState wanted = boss.wantedMode();
        if (wanted == null) {
            return;
        }
        int size = 34;
        UiSkin.inset(g2, x, y, size, size);
        boolean pulse = (uiTick / 10) % 2 == 0;
        g2.setColor(wanted == entity.CombatState.MELEE ? new Color(0x8E2422) : new Color(0x1E4A5E));
        g2.fillRect(x + 4, y + 4, size - 8, size - 8);
        UiSkin.icon(g2, wanted == entity.CombatState.MELEE ? UiSkin.Icon.SWORD : UiSkin.Icon.PISTOL,
                x + size / 2, y + size / 2, 2, true);
        if (pulse) {
            g2.setColor(new Color(255, 246, 208, 70));
            g2.fillRect(x + 4, y + 4, size - 8, size - 8);
        }
    }

    private void drawAdaptation(Enemy boss, int x, int y, int barWidth) {
        int half = barWidth / 2 - 4;
        drawAdaptMeter(boss, entity.combat.HitType.MELEE, UiSkin.Icon.SWORD, x, y, half);
        drawAdaptMeter(boss, entity.combat.HitType.RANGED, UiSkin.Icon.PISTOL, x + barWidth - half, y, half);
    }

    private void drawAdaptMeter(Enemy boss, entity.combat.HitType type, UiSkin.Icon icon, int x, int y, int width) {
        double progress = boss.adaptationProgress(type);
        double level = boss.adaptationLevel(type);
        int s = UiSkin.P;
        int meterX = x + 22;
        int meterW = width - 22;
        UiSkin.icon(g2, icon, x + 8, y + 6, 1, progress > 0);
        g2.setColor(new Color(0x120C09));
        g2.fillRect(meterX, y, meterW, 4 * s);
        g2.setColor(new Color(0x2F2427));
        g2.fillRect(meterX + s, y + s, meterW - 2 * s, 2 * s);
        boolean warn = level > 0;
        boolean blink = warn && (uiTick / 6) % 2 == 0;
        g2.setColor(warn ? (blink ? new Color(0xFFD9A0) : new Color(0xD04A36)) : new Color(0xC8B48C));
        g2.fillRect(meterX + s, y + s, (int) Math.round((meterW - 2 * s) * progress), 2 * s);
        if (warn) {
            String text = "ADAPTING";
            int tw = UiSkin.PixelFont.width(text, 1);
            UiSkin.PixelFont.draw(g2, text, meterX + meterW / 2 - tw / 2, y + 5 * s, 1,
                    blink ? new Color(0xFFD9A0) : new Color(0xD04A36));
        }
    }

    public void drawDeathScreen() {
        String text = "Reawaken";
        String prompt = "Continue? Press ENTER";

        g2.setColor(new Color(10, 4, 6, 170));
        g2.fillRect(0, 0, gp.screenWidth, gp.screenHeight);

        int width = 366;
        int height = 135;
        int x = gp.screenWidth / 2 - width / 2;
        int y = gp.screenHeight / 2 - height / 2;
        UiSkin.frame(g2, x, y, width, height);

        int scale = 4;
        UiSkin.PixelFont.draw(g2, text, gp.screenWidth / 2 - UiSkin.PixelFont.width(text, scale) / 2, y + 30, scale, UiSkin.TEXT_RED);
        UiSkin.divider(g2, x + 30, y + 72, width - 60);

        if (gp.canRetryBoss()) {
            int optionW = 156;
            int optionY = y + 88;
            String[] options = {"RESPAWN", "QUIT GAME"};
            for (int i = 0; i < 2; i++) {
                int ox = gp.screenWidth / 2 - optionW - 6 + i * (optionW + 12);
                boolean selected = gp.deathChoice() == i;
                UiSkin.plaque(g2, ox, optionY, optionW, 30, options[i], selected ? UiSkin.GLOW : UiSkin.DIM);
                if (selected && (uiTick / 15) % 2 == 0) {
                    UiSkin.PixelFont.draw(g2, ">", ox - 14, optionY + 8, 2, UiSkin.TEXT_RED);
                }
            }
            String hint = "A/D choose, ENTER confirm";
            UiSkin.PixelFont.draw(g2, hint, gp.screenWidth / 2 - UiSkin.PixelFont.width(hint, 1) / 2, y + height + 10, 1, UiSkin.TEXT);
            return;
        }

        g2.setFont(consolas_18);
        g2.setColor(UiSkin.TEXT);
        g2.drawString(prompt, getXForCenteredText(prompt), y + 105);
    }

    public int dialogueOverlayHeight() {
        DialogueLine line = gp.dialogueManager.currentLine();
        if (line == null || line.presentation() == DialogueLine.Presentation.VISUAL_NOVEL) {
            return 0;
        }
        return DIALOGUE_HEIGHT + DIALOGUE_BOTTOM_MARGIN + 12;
    }

    public void drawDialogueOverlay() {
        DialogueLine line = gp.dialogueManager.currentLine();
        if (line == null) {
            return;
        }

        int x = gp.screenWidth / 2 - DIALOGUE_WIDTH / 2;
        int y = gp.screenHeight - DIALOGUE_HEIGHT - DIALOGUE_BOTTOM_MARGIN;

        drawDialogueWindow(x, y, DIALOGUE_WIDTH, DIALOGUE_HEIGHT, line);
    }

    private void drawMessageToast() {
        Font oldFont = g2.getFont();
        Object oldAa = g2.getRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setFont(toastFont);
        FontMetrics fm = g2.getFontMetrics();
        int width = fm.stringWidth(message) + 30;
        int x = gp.screenWidth / 2 - width / 2;
        int y = 92;
        UiSkin.plaque(g2, x, y, width, 24, null, UiSkin.RED);
        int tx = gp.screenWidth / 2 - fm.stringWidth(message) / 2;
        int ty = y + 12 + (fm.getAscent() - fm.getDescent()) / 2;
        g2.setColor(new Color(0, 0, 0, 150));
        g2.drawString(message, tx + 1, ty + 1);
        g2.setColor(UiSkin.TEXT);
        g2.drawString(message, tx, ty);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, oldAa);
        g2.setFont(oldFont);
    }

    private void drawVisualNovelScene() {
        DialogueLine line = gp.dialogueManager.currentLine();
        if (line != null && line.presentation() == DialogueLine.Presentation.VISUAL_NOVEL) {
            lastVisualNovelLine = line;
        } else {
            line = lastVisualNovelLine;
        }

        drawVisualNovelBackground(line);
        if (line == null) {
            return;
        }

        Color oldColor = g2.getColor();
        Font oldFont = g2.getFont();
        Stroke oldStroke = g2.getStroke();

        int sideMargin = gp.tileSize;
        int textBoxHeight = gp.tileSize * 3;
        int textBoxX = sideMargin;
        int textBoxY = gp.screenHeight - textBoxHeight - gp.tileSize / 2;
        int textBoxWidth = gp.screenWidth - sideMargin * 2;

        int castY = gp.tileSize / 2;
        int castHeight = gp.screenHeight - textBoxHeight - gp.tileSize;
        drawVisualNovelCast(line, castY, castHeight);

        drawVisualNovelTextBox(textBoxX, textBoxY, textBoxWidth, textBoxHeight, line);

        g2.setStroke(oldStroke);
        g2.setFont(oldFont);
        g2.setColor(oldColor);
    }

    private void drawVisualNovelBackground(DialogueLine line) {
        BufferedImage background = loadVisualNovelBackground(line);
        if (background != null) {
            g2.drawImage(background, 0, 0, gp.screenWidth, gp.screenHeight, null);
            return;
        }

        Paint oldPaint = g2.getPaint();

        GradientPaint sky = new GradientPaint(
                0,
                0,
                new Color(18, 23, 38),
                0,
                gp.screenHeight,
                new Color(5, 8, 14)
        );
        g2.setPaint(sky);
        g2.fillRect(0, 0, gp.screenWidth, gp.screenHeight);
        g2.setPaint(oldPaint);

        g2.setColor(new Color(33, 46, 45, 170));
        g2.fillRect(0, gp.screenHeight / 2, gp.screenWidth, gp.screenHeight / 2);

        g2.setColor(new Color(8, 11, 18, 130));
        for (int i = 0; i < 7; i++) {
            int x = i * gp.tileSize * 2 - gp.tileSize;
            int y = gp.screenHeight / 2 + (i % 3) * 18;
            g2.fillRect(x, y, gp.tileSize * 3, gp.screenHeight / 2);
        }
    }

    private BufferedImage loadVisualNovelBackground(DialogueLine line) {
        if (line == null || line.visualNovelBackgroundPath() == null) {
            return null;
        }

        String path = line.visualNovelBackgroundPath();
        if (visualNovelBackgroundCache.containsKey(path)) {
            return visualNovelBackgroundCache.get(path);
        }

        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(path)) {
            if (stream != null) {
                BufferedImage image = ImageIO.read(stream);
                visualNovelBackgroundCache.put(path, image);
                return image;
            }
        } catch (IOException e) {
            System.err.println("Could not load VN background " + path + ": " + e.getMessage());
            visualNovelBackgroundCache.put(path, null);
            return null;
        }

        File file = new File("res", path);
        if (!file.isFile()) {
            System.err.println("Missing VN background: " + path);
            visualNovelBackgroundCache.put(path, null);
            return null;
        }

        try {
            BufferedImage image = ImageIO.read(file);
            visualNovelBackgroundCache.put(path, image);
            return image;
        } catch (IOException e) {
            System.err.println("Could not load VN background file " + file + ": " + e.getMessage());
            visualNovelBackgroundCache.put(path, null);
            return null;
        }
    }

    private void drawVisualNovelTextBox(int x, int y, int width, int height, DialogueLine line) {
        Color oldColor = g2.getColor();
        Font oldFont = g2.getFont();
        Stroke oldStroke = g2.getStroke();

        UiSkin.frame(g2, x, y, width, height);
        if (!line.speaker().name().isEmpty()) {
            drawNamePlate(x + 24, y - 15, line.speaker().name());
        }

        int textX = x + 30;
        int textY = y + 50;
        int textWidth = width - 60;
        int textHeight = height - 74;

        g2.setFont(consolas_18.deriveFont(Font.PLAIN, 25f));
        g2.setColor(UiSkin.TEXT);
        drawWrappedText(gp.dialogueManager.visibleText(), textX, textY, textWidth, textHeight, 35);

        drawKeyHint(x + width - 24, y + height - 36, "ENTER");
        if (gp.storySequenceManager.canSkipActiveSequence()) {
            drawKeyHint(x + width - 114, y + height - 36, "ESC SKIP");
        }

        g2.setStroke(oldStroke);
        g2.setFont(oldFont);
        g2.setColor(oldColor);
    }

    private void drawNamePlate(int x, int y, String name) {
        int width = UiSkin.PixelFont.width(name, 2) + 42;
        width += (UiSkin.P - width % UiSkin.P) % UiSkin.P;
        UiSkin.plaque(g2, x, y, width, 30, name, UiSkin.GLOW);
    }

    private void drawKeyHint(int rightX, int y, String key) {
        int width = UiSkin.PixelFont.width(key, 1) + 24;
        width += (UiSkin.P - width % UiSkin.P) % UiSkin.P;
        UiSkin.plaque(g2, rightX - width, y, width, 24, key, UiSkin.DIM);
    }

    public void drawSubWindow(int x, int y, int width, int height) {
        Color c = new Color(0, 0, 0, 200);
        g2.setColor(c);
        g2.fillRoundRect(x, y, width, height, 35, 35);

        c = new Color(255, 255, 255);
        g2.setColor(c);
        g2.setStroke(new BasicStroke(5));
        g2.drawRoundRect(x+5, y+5, width-10, height-10, 25, 25);
    }

    public int getXForCenteredText(String text) {
        int length = (int)g2.getFontMetrics().getStringBounds(text, g2).getWidth();
        return gp.screenWidth / 2 - length / 2;
    }

    private void drawDialogueWindow(int x, int y, int width, int height, DialogueLine line) {
        Color oldColor = g2.getColor();
        Font oldFont = g2.getFont();
        Stroke oldStroke = g2.getStroke();

        UiSkin.frame(g2, x, y, width, height);

        int portraitSize = height - 36;
        int portraitX = x + 18;
        int portraitY = y + 18;
        drawPortraitSlot(portraitX, portraitY, portraitSize, line.speaker());

        if (!line.speaker().name().isEmpty()) {
            drawNamePlate(portraitX + portraitSize + 12, y - 15, line.speaker().name());
        }

        int textX = portraitX + portraitSize + 18;
        int textY = y + 38;
        int textWidth = x + width - textX - 24;
        int lineHeight = 22;

        g2.setFont(dialogueFont);
        g2.setColor(UiSkin.TEXT);
        drawWrappedText(gp.dialogueManager.visibleText(), textX, textY, textWidth, lineHeight * 3, lineHeight);

        if (line.mode() == DialogueLine.Mode.BLOCKING) {
            drawKeyHint(x + width - 24, y + height - 12, "ENTER");
        }

        g2.setStroke(oldStroke);
        g2.setFont(oldFont);
        g2.setColor(oldColor);
    }

    private void drawPortraitSlot(int x, int y, int size, DialogueSpeaker speaker) {
        UiSkin.inset(g2, x, y, size, size);

        BufferedImage portrait = speaker.portrait();
        if (portrait != null) {
            g2.drawImage(portrait, x + 6, y + 6, size - 12, size - 12, null);
            return;
        }
        if (speaker.name().isEmpty()) {
            return;
        }

        String initial = speaker.name().substring(0, 1);
        int scale = 6;
        UiSkin.PixelFont.draw(g2, initial, x + size / 2 - UiSkin.PixelFont.width(initial, scale) / 2,
                y + size / 2 - 7 * scale / 2, scale, speaker.accentColor());
    }

    private void drawVisualNovelPortrait(int x, int y, int width, int height, DialogueSpeaker speaker) {
        drawVisualNovelPortrait(x, y, width, height, speaker, true);
    }

    private void drawVisualNovelCast(DialogueLine line, int y, int height) {
        List<DialogueSpeaker> speakers = line.sceneSpeakers();
        int count = Math.min(3, Math.max(1, speakers.size()));
        int sideMargin = gp.tileSize;
        int availableWidth = gp.screenWidth - sideMargin * 2;
        int slotWidth = availableWidth / count;
        int preferredPortraitWidth = 256;
        int portraitWidth = count == 1
                ? preferredPortraitWidth
                : Math.min(preferredPortraitWidth, slotWidth - gp.tileSize / 4);

        for (int i = 0; i < count; i++) {
            DialogueSpeaker sceneSpeaker = speakers.get(i);
            int x = count == 1
                    ? gp.screenWidth / 2 - portraitWidth / 2
                    : sideMargin + i * slotWidth + slotWidth / 2 - portraitWidth / 2;
            boolean active = isSameSpeaker(sceneSpeaker, line.speaker());
            DialogueSpeaker speakerToDraw = active ? line.speaker() : sceneSpeaker;
            drawVisualNovelPortrait(x, y, portraitWidth, height, speakerToDraw, active);
        }
    }

    private void drawVisualNovelPortrait(
            int x,
            int y,
            int width,
            int height,
            DialogueSpeaker speaker,
            boolean active
    ) {
        BufferedImage portrait = speaker.portrait();
        if (portrait != null) {
            drawVisualNovelPortraitImage(portrait, x, y, width, height, active);
            return;
        }

        Color oldColor = g2.getColor();
        Font oldFont = g2.getFont();
        Stroke oldStroke = g2.getStroke();
        Composite oldComposite = g2.getComposite();

        int bodyX = x + width / 4;
        int bodyY = y + height / 3;
        int bodyW = width / 2;
        int bodyH = height * 2 / 3;
        int headSize = Math.min(width / 2, height / 3);
        int headX = x + width / 2 - headSize / 2;
        int headY = y + height / 5;
        Color accent = active ? speaker.accentColor() : new Color(110, 115, 122);
        Color bodyColor = active ? new Color(16, 17, 24, 220) : new Color(45, 48, 54, 175);

        if (!active) {
            g2.setComposite(AlphaComposite.SrcOver.derive(0.74f));
        }

        g2.setColor(bodyColor);
        g2.fillOval(headX, headY, headSize, headSize);
        g2.fillRoundRect(bodyX, bodyY, bodyW, bodyH, 36, 36);

        g2.setColor(accent);
        g2.setStroke(new BasicStroke(active ? 4 : 2));
        g2.drawOval(headX + 2, headY + 2, headSize - 4, headSize - 4);

        String initial = speaker.name().substring(0, 1).toUpperCase();
        g2.setFont(arial_40.deriveFont(Font.BOLD, 76f));
        FontMetrics fm = g2.getFontMetrics();
        int textX = headX + headSize / 2 - fm.stringWidth(initial) / 2;
        int textY = headY + headSize / 2 + fm.getAscent() / 2 - 12;
        g2.drawString(initial, textX, textY);

        g2.setComposite(oldComposite);
        g2.setStroke(oldStroke);
        g2.setFont(oldFont);
        g2.setColor(oldColor);
    }

    private void drawVisualNovelPortraitImage(
            BufferedImage portrait,
            int x,
            int y,
            int width,
            int height,
            boolean active
    ) {
        if (active) {
            g2.drawImage(portrait, x, y, width, height, null);
            return;
        }

        Composite oldComposite = g2.getComposite();
        g2.setComposite(AlphaComposite.SrcOver.derive(0.42f));
        g2.drawImage(portrait, x, y, width, height, null);
        g2.setComposite(oldComposite);
    }

    private boolean isSameSpeaker(DialogueSpeaker first, DialogueSpeaker second) {
        return first == second || first.name().equals(second.name());
    }

    private void drawWrappedText(String text, int x, int y, int width, int height, int lineHeight) {
        FontMetrics fm = g2.getFontMetrics();
        int maxLines = Math.max(1, height / lineHeight);
        List<String> visibleLines = new ArrayList<>();
        for (String manualLine : text.split("\n")) {
            visibleLines.addAll(wrapLine(manualLine, fm, width));
        }

        for (int i = 0; i < visibleLines.size() && i < maxLines; i++) {
            String line = visibleLines.get(i);
            if (i == maxLines - 1 && visibleLines.size() > maxLines) {
                line = trimToWidth(line + "...", fm, width);
            }
            g2.drawString(line, x, y);
            y += lineHeight;
        }
    }

    private List<String> wrapLine(String text, FontMetrics fm, int width) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            lines.add("");
            return lines;
        }

        StringBuilder current = new StringBuilder();
        for (String word : text.split(" ")) {
            if (fm.stringWidth(word) > width) {
                if (current.length() > 0) {
                    lines.add(current.toString());
                    current = new StringBuilder();
                }
                lines.addAll(splitLongWord(word, fm, width));
                continue;
            }

            String candidate = current.length() == 0 ? word : current + " " + word;
            if (fm.stringWidth(candidate) <= width) {
                current = new StringBuilder(candidate);
                continue;
            }

            if (current.length() > 0) {
                lines.add(current.toString());
            }
            current = new StringBuilder(word);
        }

        if (current.length() > 0) {
            lines.add(current.toString());
        }
        return lines;
    }

    private List<String> splitLongWord(String word, FontMetrics fm, int width) {
        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (int i = 0; i < word.length(); i++) {
            String candidate = current.toString() + word.charAt(i);
            if (fm.stringWidth(candidate) <= width) {
                current.append(word.charAt(i));
                continue;
            }

            if (current.length() > 0) {
                lines.add(current.toString());
            }
            current = new StringBuilder(String.valueOf(word.charAt(i)));
        }

        if (current.length() > 0) {
            lines.add(current.toString());
        }
        return lines;
    }

    private String trimToWidth(String text, FontMetrics fm, int width) {
        String result = text;
        while (!result.isEmpty() && fm.stringWidth(result) > width) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }
}
