package ui;

import audio.Sound;
import main.GamePanel;
import main.GameState;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.List;

public final class ChoiceMenu {
    public record Option(String label, String description, Runnable action) {}
    private final GamePanel gp;
    private String title = "";
    private List<Option> options = List.of();
    public List<String> labels() { return options.stream().map(Option::label).toList(); }
    private int selected;
    private boolean cancelable;
    private int lockTicks;
    private int closeKey = KeyEvent.VK_UNDEFINED;
    private entity.npc.NPC npc;
    private Runnable back;

    public void update() { if (lockTicks > 0) lockTicks--; }
    public void protectConfirmation(int ticks) { lockTicks = ticks; }
    public String title() { return title; }
    public int optionCount() { return options.size(); }

    public ChoiceMenu(GamePanel gp) { this.gp = gp; }

    public void open(String title, List<Option> options, boolean cancelable) {
        open(title, options, cancelable, KeyEvent.VK_UNDEFINED);
    }

    public void open(String title, List<Option> options, boolean cancelable, int closeKey) {
        npc = null;
        back = null;
        this.title = title;
        this.options = List.copyOf(options);
        this.cancelable = cancelable;
        this.closeKey = closeKey;
        selected = 0;
        lockTicks = 0;
        gp.keyH.clearGameplayInput();
        gp.gameState = GameState.CHOICE;
    }

    public void openForNpc(entity.npc.NPC npc, List<Option> options, Runnable back) {
        open(npc.name, options, true);
        this.npc = npc;
        this.back = back;
    }

    public void handle(int code) {
        if (options.isEmpty() || lockTicks > 0) return;
        if(code==KeyEvent.VK_UP||code==KeyEvent.VK_W||code==KeyEvent.VK_DOWN||code==KeyEvent.VK_S)gp.playSoundEffect(Sound.UI_MOVE);
        if (code == KeyEvent.VK_UP || code == KeyEvent.VK_W) selected = Math.floorMod(selected - 1, options.size());
        if (code == KeyEvent.VK_DOWN || code == KeyEvent.VK_S) selected = (selected + 1) % options.size();
        if ((code == KeyEvent.VK_ESCAPE || closeKey != KeyEvent.VK_UNDEFINED && code == closeKey) && cancelable) {
            gp.resumeGame();
            if (back != null) back.run();
        }
        if (code == KeyEvent.VK_ENTER || code == KeyEvent.VK_SPACE) {
            gp.playSoundEffect(Sound.UI_CONFIRM);
            Option option = options.get(selected);
            gp.resumeGame();
            option.action().run();
        }
    }

    public void draw(Graphics2D g) {
        if (options.isEmpty()) return;
        g = (Graphics2D) g.create();
        g.setColor(new Color(10, 4, 6, 150));
        g.fillRect(0, 0, gp.screenWidth, gp.screenHeight);
        int width = Math.min(460, gp.screenWidth - 48);
        boolean description = !options.get(selected).description().isBlank();
        int header = npc == null ? 28 : 80;
        int visible = Math.min(options.size(), Math.max(1, (gp.screenHeight - 140 - header - (description ? 65 : 0)) / 38));
        int height = header + visible * 38 + 24 + (description ? 65 : 0);
        int x = (gp.screenWidth - width) / 2, y = (gp.screenHeight - height) / 2;
        UiSkin.frame(g, x, y, width, height);
        UiSkin.header(g, gp.screenWidth / 2, y - 14, title.toUpperCase(java.util.Locale.ROOT));
        if (npc != null) {
            UiSkin.inset(g, x + 22, y + 18, 52, 52);
            var portrait = npc.dialogueSpeaker().portrait();
            if (portrait != null) g.drawImage(portrait, x + 26, y + 22, 44, 44, null);
            g.setFont(UiFonts.of(Font.BOLD, 16)); g.setColor(UiSkin.TEXT);
            g.drawString("What do you need?", x + 86, y + 48);
        }
        int first = Math.max(0, Math.min(selected - visible + 1, options.size() - visible));
        for (int i = first; i < first + visible; i++) {
            int row = y + header + (i - first) * 38;
            if (i == selected) {
                UiSkin.plaque(g, x + 18, row, width - 36, 32, null, UiSkin.GLOW);
                UiSkin.PixelFont.draw(g, ">", x + 30, row + 9, 2, UiSkin.CREAM);
            }
            g.setFont(UiFonts.of(Font.BOLD, 16));
            g.setColor(i == selected ? UiSkin.TEXT : UiSkin.TEXT_DIM);
            g.drawString(options.get(i).label(), x + 54, row + 22);
        }
        g.setColor(UiSkin.TEXT_DIM);
        g.setFont(UiFonts.of(Font.PLAIN, 14));
        int row = y + header + visible * 38 + 15;
        String line = "";
        for (String word : options.get(selected).description().split(" ")) {
            if (g.getFontMetrics().stringWidth(line + word) > width - 54) {
                if (row <= y + height - 14) g.drawString(line, x + 26, row);
                row += 18; line = "";
            }
            line += word + " ";
        }
        if (description && row <= y + height - 14) g.drawString(line, x + 26, row);
        String hint = "W/S MOVE   ENTER CHOOSE" + (cancelable ? "   ESC BACK" : "");
        int hintWidth = UiSkin.PixelFont.width(hint, 2) + 30;
        UiSkin.plaque(g, (gp.screenWidth - hintWidth) / 2, gp.screenHeight - 42, hintWidth, 27, hint, UiSkin.DIM);
        g.dispose();
    }
}
