package main;

import entity.Player;
import entity.enemy.Enemy;
import entity.enemy.Enemy_BYT;
import entity.enemy.Enemy_Slime;
import entity.enemy.Enemy_SlimeKing;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

public class ModMenu {
    private enum Page { MAIN, TELEPORT, COMBAT, EQUIPMENT, TOOLS }
    private record Item(Supplier<String> label, Runnable action, IntConsumer adjust) {
        Item(Supplier<String> label, Runnable action) { this(label, action, null); }
    }

    private final GamePanel gp;
    private final Map<Page, List<Item>> pages = new EnumMap<>(Page.class);
    private Page page = Page.MAIN;
    private GameState returnState = GameState.PLAYING;
    private int selected;
    private int rootSelection;
    private String feedback = "";
    private final Font itemFont = new Font("SansSerif", Font.BOLD, 16);
    private final Font hintFont = new Font("SansSerif", Font.PLAIN, 12);

    public ModMenu(GamePanel gp) {
        this.gp = gp;
        pages.put(Page.MAIN, List.of(
                item("Przenieś  >", () -> enter(Page.TELEPORT)),
                item("Walka i obrażenia  >", () -> enter(Page.COMBAT)),
                item("Ekwipunek  >", () -> enter(Page.EQUIPMENT)),
                item("Narzędzia  >", () -> enter(Page.TOOLS)),
                item("Zamknij", this::close)));
        pages.put(Page.TELEPORT, List.of(
                item("Przenieś do walki z BYT-em", () -> teleport(GamePanel.DebugDestination.BYT)),
                item("Przenieś do tutoriala", () -> teleport(GamePanel.DebugDestination.TUTORIAL)),
                item("Przenieś do lobby", () -> teleport(GamePanel.DebugDestination.LOBBY)),
                item("Przenieś do walki z King Slime", () -> teleport(GamePanel.DebugDestination.SLIME_KING)),
                item("Wstecz", this::back)));
        pages.put(Page.COMBAT, List.of(
                new Item(() -> "Nieśmiertelność: " + on(gp.debugSettings.isGodMode()), this::toggleGodMode),
                new Item(() -> "Mnożnik obrażeń gracza: ×" + damageLabel(),
                        () -> adjustDamage(1), this::adjustDamage),
                new Item(() -> "Zamrożenie wrogów: " + on(gp.debugSettings.areEnemiesFrozen()),
                        () -> gp.debugSettings.toggleEnemiesFrozen()),
                new Item(() -> "Egzekucja BYT przy 50%: " + (gp.debugSettings.isGodMode()
                        ? "WYŁ. (nieśmiertelność)" : on(!gp.debugSettings.isBytExecutionDisabled())),
                        this::toggleExecution),
                item("HP i osłona do pełna / ożyw", this::heal),
                item("Zadaj bossowi 25% maks. HP", this::damageBoss),
                item("Restartuj walkę z bossem", this::restartBoss),
                item("Przywróć domyślne ustawienia walki", () -> {
                    gp.debugSettings.resetCheats();
                    feedback = "Nieśmiertelność WYŁ. / obrażenia ×1 / AI włączone";
                }),
                item("Wstecz", this::back)));
        pages.put(Page.EQUIPMENT, List.of(
                item("Dodaj +1 maks. HP", () -> {
                    gp.player.maxHealth++; gp.player.health = gp.player.maxHealth;
                    feedback = "Maks. HP: " + gp.player.maxHealth;
                }),
                item("Dodaj szybkość (+1)", () -> {gp.player.addMaxSpeed(1); feedback = "Szybkość zwiększona";}),
                new Item(() -> "Dodaj klucz (masz: " + gp.player.hasKey + ")",
                        () -> {gp.player.hasKey++; feedback = "Klucze: " + gp.player.hasKey;}),
                new Item(() -> toggleLabel("Broń biała", gp.player.ownsMeleeSlot(1), gp.player.meleeSlotLabel(1)),
                        () -> toggleMelee(1)),
                new Item(() -> toggleLabel("Broń dystansowa", gp.player.ownsRangedSlot(1), gp.player.rangedSlotLabel(1)),
                        () -> toggleRanged(1)),
                new Item(() -> "Broń w lobby: " + (gp.player.isWeaponLockOverridden() ? "ODBLOKOWANA" : "zablokowana"),
                        () -> {gp.player.toggleWeaponLockOverride(); feedback = "Zmieniono blokadę broni w lobby";}),
                item("Reset ekwipunku (miecz + pistolet)", () -> {
                    gp.player.resetLoadout(); feedback = "Ekwipunek zresetowany";
                }),
                item("Wstecz", this::back)));
        pages.put(Page.TOOLS, List.of(
                item("Dodaj slime obok gracza", this::spawnSlime),
                item("Usuń pociski i aktywne ataki BYT-a", () -> {
                    gp.debugClearProjectiles(); feedback = "Pociski i ataki obszarowe usunięte";
                }),
                new Item(() -> "Nakładka debugowania: " + on(gp.debugSettings.isOverlayEnabled()),
                        () -> gp.debugSettings.toggleOverlay()),
                item("Przeładuj grafikę (F5)", () -> {gp.debugReloadArt(); feedback = "Wywołano przeładowanie grafiki";}),
                item("Wstecz", this::back)));
    }

    private Item item(String label, Runnable action) { return new Item(() -> label, action); }
    private String on(boolean enabled) { return enabled ? "WŁ." : "WYŁ."; }
    private String damageLabel() { return String.format(Locale.ROOT, "%s", gp.debugSettings.playerDamageMultiplier()).replaceAll("\\.0$", ""); }
    private List<Item> items() { return pages.get(page); }

    public void open() {
        returnState = gp.gameState;
        gp.keyH.clearGameplayInput();
        page = Page.MAIN;
        selected = rootSelection = 0;
        feedback = "";
        gp.gameState = GameState.MOD_MENU;
    }

    public void close() {
        gp.keyH.clearGameplayInput();
        gp.gameState = returnState;
    }

    private void enter(Page next) {
        rootSelection = selected;
        page = next;
        selected = 0;
        feedback = "";
    }

    private void back() {
        if (page == Page.MAIN) { close(); return; }
        page = Page.MAIN;
        selected = rootSelection;
        feedback = "";
    }

    public void keyPressed(int code) {
        switch (code) {
            case KeyEvent.VK_UP, KeyEvent.VK_W -> selected = Math.floorMod(selected - 1, items().size());
            case KeyEvent.VK_DOWN, KeyEvent.VK_S -> selected = (selected + 1) % items().size();
            case KeyEvent.VK_ENTER, KeyEvent.VK_SPACE -> items().get(selected).action().run();
            case KeyEvent.VK_LEFT, KeyEvent.VK_A -> {
                if (items().get(selected).adjust() != null) items().get(selected).adjust().accept(-1);
                else back();
            }
            case KeyEvent.VK_RIGHT, KeyEvent.VK_D -> {
                if (items().get(selected).adjust() != null) items().get(selected).adjust().accept(1);
                else if (page == Page.MAIN) items().get(selected).action().run();
            }
            case KeyEvent.VK_ESCAPE -> back();
            case KeyEvent.VK_F1 -> close();
            default -> {}
        }
    }

    private void teleport(GamePanel.DebugDestination destination) {
        gp.debugTeleport(destination);
        returnState = GameState.PLAYING;
    }

    private void adjustDamage(int direction) { gp.debugSettings.cycleDamageMultiplier(direction); }

    private void toggleGodMode() {
        boolean wasActive = gp.storySequenceManager.isActive();
        gp.debugToggleGodMode();
        if (wasActive && !gp.storySequenceManager.isActive()) returnState = GameState.PLAYING;
        feedback = gp.debugSettings.isGodMode() ? "Odporność na obrażenia i egzekucję BYT-a" : "Nieśmiertelność wyłączona";
    }

    private void toggleExecution() {
        gp.debugSettings.toggleBytExecution();
        if (gp.debugSettings.isBytExecutionDisabled() && gp.levelManager.getActiveBoss() instanceof Enemy_BYT byt) {
            boolean wasActive = gp.storySequenceManager.isActive();
            byt.debugCancelExecution();
            if (wasActive && !gp.storySequenceManager.isActive()) returnState = GameState.PLAYING;
        }
        feedback = "Przy wyłączonej egzekucji BYT zatrzymuje się na 1 HP";
    }

    private void heal() {
        gp.player.debugRestoreVitals();
        if (returnState == GameState.DEAD) returnState = GameState.PLAYING;
        feedback = "HP i osłona odnowione";
    }

    private void damageBoss() {
        Enemy boss = gp.levelManager.getActiveBoss();
        if (boss == null) {feedback = "Brak aktywnego bossa"; return;}
        int before = boss.health;
        gp.debugDamageActiveBoss(Math.max(1, boss.maxHealth / 4));
        feedback = boss.health < before ? "Zadano obrażenia bossowi" : "Boss jest odporny w bieżącej fazie";
    }

    private void restartBoss() {
        Enemy boss = gp.levelManager.getActiveBoss();
        if (boss instanceof Enemy_BYT) teleport(GamePanel.DebugDestination.BYT);
        else if (boss instanceof Enemy_SlimeKing) teleport(GamePanel.DebugDestination.SLIME_KING);
        else feedback = "Brak aktywnej walki do restartu";
    }

    private void spawnSlime() {
        Enemy_Slime slime = new Enemy_Slime(gp);
        int col = gp.player.worldX / gp.tileSize, row = gp.player.worldY / gp.tileSize;
        int[][] offsets = {{2,0},{-2,0},{0,2},{0,-2},{2,2},{-2,2},{2,-2},{-2,-2}};
        for (int[] offset : offsets) {
            int c = col + offset[0], r = row + offset[1];
            if (c < 0 || r < 0 || c >= gp.tileM.getWorldCols() || r >= gp.tileM.getWorldRows()) continue;
            int x = c * gp.tileSize, y = r * gp.tileSize;
            if (!gp.cChecker.isPlayerPositionBlocked(slime,x,y) && !gp.cChecker.checkPlayerAt(slime,x,y)) {
                gp.entities.addEnemy(slime,c,r);
                feedback = "Dodano slime na wolnym polu"; return;
            }
        }
        feedback = "Brak wolnego miejsca w pobliżu";
    }

    private void toggleMelee(int slot) {
        Player p = gp.player;
        p.setMeleeSlotOwned(slot,!p.ownsMeleeSlot(slot));
        feedback = "Zmieniono broń białą";
    }

    private void toggleRanged(int slot) {
        Player p = gp.player;
        p.setRangedSlotOwned(slot,!p.ownsRangedSlot(slot));
        feedback = "Zmieniono broń dystansową";
    }

    private String toggleLabel(String kind, boolean owned, String slotLabel) {
        String name = slotLabel.substring(slotLabel.indexOf(' ') + 1);
        return (owned ? "[x] " : "[ ] ") + kind + ": " + name;
    }

    public void draw(Graphics2D original) {
        Graphics2D g = (Graphics2D) original.create();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(new Color(10,4,6,185));
        g.fillRect(0,0,gp.screenWidth,gp.screenHeight);
        List<Item> entries = items();
        int rowH = 30, width = 570, height = 126 + entries.size()*rowH;
        int x = (gp.screenWidth-width)/2, y = (gp.screenHeight-height)/2+6;
        UiSkin.frame(g,x,y,width,height);
        UiSkin.header(g,gp.screenWidth/2,y-14,"DEVELOPER");
        g.setFont(hintFont);g.setColor(UiSkin.TEXT_DIM);
        String title = switch(page) {
            case MAIN -> "Menu deweloperskie";
            case TELEPORT -> "Przenieś / wybór sceny";
            case COMBAT -> "Walka i obrażenia";
            case EQUIPMENT -> "Ekwipunek";
            case TOOLS -> "Narzędzia testowe";
        };
        g.drawString(title,x+27,y+25);
        g.setFont(itemFont);
        for(int i=0;i<entries.size();i++){
            int rowY = y+43+i*rowH;
            if(i==selected){
                UiSkin.plaque(g,x+18,rowY-3,width-36,30,null,UiSkin.GLOW);
                UiSkin.PixelFont.draw(g,">",x+30,rowY+5,2,UiSkin.CREAM);
                g.setColor(UiSkin.TEXT);
            }else g.setColor(UiSkin.TEXT_DIM);
            g.drawString(entries.get(i).label().get(),x+54,rowY+18);
        }
        int footer = y+height-65;
        UiSkin.divider(g,x+24,footer,width-48);
        g.setFont(hintFont);g.setColor(new Color(0x9CC890));
        g.drawString(feedback,x+27,footer+21);
        g.setColor(UiSkin.TEXT_DIM);
        g.drawString("W/S: wybór   ENTER: użyj   A/D: mnożnik   ESC: wstecz   F1: zamknij",x+27,footer+43);
        g.dispose();
    }
}
