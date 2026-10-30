package main;

import entity.anim.PlayerAnimationGallery;
import entity.Player;
import entity.enemy.Enemy;
import entity.enemy.Enemy_BYT;
import entity.enemy.Enemy_Slime;
import entity.enemy.Enemy_SlimeKing;
import story.QuestLog;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

public class ModMenu {
    private enum Page { MAIN, TELEPORT, COMBAT, EQUIPMENT, PROGRESS, QUESTS, CHAPTER, RELICS, TOOLS }
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
    private final Font itemFont = UiFonts.of(Font.BOLD, 16);
    private final Font hintFont = UiFonts.of(Font.PLAIN, 12);

    public ModMenu(GamePanel gp) {
        this.gp = gp;
        pages.put(Page.MAIN, List.of(
                item("Przenieś  >", () -> enter(Page.TELEPORT)),
                item("Walka i obrażenia  >", () -> enter(Page.COMBAT)),
                item("Ekwipunek  >", () -> enter(Page.EQUIPMENT)),
                item("Grzyby i umiejętności  >", () -> enter(Page.PROGRESS)),
                item("Questy  >", () -> enter(Page.QUESTS)),
                item("Pierwszy rozdział i checkpointy  >", () -> enter(Page.CHAPTER)),
                item("Relikty  >", () -> enter(Page.RELICS)),
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
        List<Item> progress = new ArrayList<>(List.of(
                new Item(() -> "Grzyby: " + gp.wallet.banked() + "  (A/D ±50)",
                        () -> adjustBanked(100), d -> adjustBanked(d * 50)),
                item("Wyzeruj grzyby", () -> {
                    gp.wallet.debugAdjustBanked(-gp.wallet.banked());
                    feedback = "Grzyby wyzerowane";
                })));
        for (Skill skill : Skill.values()) {
            progress.add(new Item(() -> (gp.progression.hasSkill(skill) ? "[x] " : "[ ] ") + "Umiejętność: " + skill.title,
                    () -> toggleSkill(skill)));
        }
        progress.addAll(List.of(
                item("Naucz wszystkich umiejętności", () -> {
                    for (Skill skill : Skill.values()) if (!gp.progression.hasSkill(skill)) gp.progression.learn(skill);
                    feedback = "Wszystkie umiejętności poznane";
                }),
                item("Zapomnij wszystkie umiejętności", () -> {
                    for (Skill skill : Skill.values()) gp.progression.forget(skill);
                    feedback = "Umiejętności zapomniane";
                }),
                item("Ulepszenia sklepu do maksimum", this::maxUpgrades),
                item("Cofnij ulepszenia i bronie ze sklepu", () -> {
                    gp.progression.resetUpgrades(gp.player);
                    gp.player.debugRestoreVitals();
                    feedback = "Ulepszenia sklepu cofnięte";
                }),
                item("Wstecz", this::back)));
        pages.put(Page.PROGRESS, List.copyOf(progress));
        pages.put(Page.QUESTS, List.of(
                new Item(() -> "Ukończ bieżący quest fabularny" + currentMainLabel(), this::completeMainQuest),
                item("Ukończ wszystkie questy poboczne", this::completeSideQuests),
                item("Zresetuj dziennik questów", () -> {
                    gp.questLog.reset();
                    feedback = "Dziennik zresetowany (flagi fabuły zostają)";
                }),
                item("Wstecz", this::back)));
        pages.put(Page.TOOLS, List.of(
                item("Dodaj slime obok gracza", this::spawnSlime),
                item("Usuń pociski i aktywne ataki BYT-a", () -> {
                    gp.debugClearProjectiles(); feedback = "Pociski i ataki obszarowe usunięte";
                }),
                new Item(() -> "Nakładka debugowania: " + on(gp.debugSettings.isOverlayEnabled()),
                        () -> gp.debugSettings.toggleOverlay()),
                new Item(() -> "Galeria animacji gracza: " + galleryLabel(), this::toggleGallery, this::cycleGallery),
                new Item(() -> "Tempo galerii: " + (gp.debugSettings.isGallerySlow() ? "x0.25" : "x1"),
                        () -> gp.debugSettings.toggleGallerySlow()),
                new Item(() -> "Wygładzanie obrotu broni dystansowej: " + on(gp.debugSettings.isAimSmoothing()),
                        () -> gp.debugSettings.toggleAimSmoothing()),
                item("Test czcionki: ĄĆĘŁŃÓŚŹŻ ąćęłńóśźż", () -> feedback = "Zażółć gęślą jaźń"),
                item("Przeładuj grafikę (F5)", () -> {gp.debugReloadArt(); feedback = "Wywołano przeładowanie grafiki";}),
                item("Wstecz", this::back)));
        pages.put(Page.CHAPTER, List.of(
                item("Przygotuj kotwicę, Marę i walkę z bossem", () -> chapterScenario(false)),
                item("Przygotuj stan po bossie i zejście niżej", () -> chapterScenario(true)),
                item("Zwiąż kotwicę (normalne warunki i zapis)", () -> playAction(() -> gp.campaign.bindAnchor())),
                item("Zapisz checkpoint tutaj", () -> {
                    feedback = gp.saves.checkpoint() ? "Checkpoint zapisany" : "Zapis nie powiódł się";
                }),
                item("Odtwórz checkpoint z odrodzeniem", () -> playAction(() -> gp.saves.respawn())),
                item("Rozmowa z Aldousem / rozpoznanie znaku", () -> playAction(() -> gp.campaign.talkSage())),
                item("Rozmowa z Marą / budowa warsztatu", () -> playAction(() -> gp.campaign.talkWarrior())),
                item("Odczytaj znak Erena", () -> playAction(() -> gp.campaign.foundSign())),
                item("Zejście na piętro 2 (potwierdzenie)", () -> playAction(() -> gp.campaign.requestDescent())),
                item("Wstecz", this::back)));
        List<Item> relicRows = new ArrayList<>();
        for (Relics.Item relic : Relics.Item.values()) {
            relicRows.add(new Item(() -> "Dodaj: " + relic.title() + (gp.relics.owns(relic) ? " [posiadany]" : ""),
                    () -> { gp.relics.grant(relic); feedback = "Dodano: " + relic.title(); }));
            relicRows.add(new Item(() -> "Wyposażenie: " + relic.title() + " " + on(gp.relics.active(relic)),
                    () -> feedback = gp.relics.toggle(relic) ? "Zmieniono wyposażenie" : "Wymaga przedmiotu i braku walki"));
        }
        relicRows.add(item("Otwórz ekwipunek (I)", () -> playAction(() -> gp.relics.open())));
        relicRows.add(item("Wstecz", this::back));
        pages.put(Page.RELICS, List.copyOf(relicRows));
    }

    private void playAction(Runnable action) {
        close();
        gp.gameState = GameState.PLAYING;
        action.run();
    }

    private void chapterScenario(boolean afterBoss) {
        teleport(GamePanel.DebugDestination.LOBBY);
        gp.campaign.debugPrepareChapter(afterBoss);
        feedback = "Scenariusz przygotowany; checkpoint nie został zapisany";
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

    private String galleryLabel() {
        int state = gp.debugSettings.playerGallery();
        return state < 0 ? "WYŁ.  (A/D: animacja)" : PlayerAnimationGallery.stateName(state) + "  (A/D: animacja)";
    }

    private void toggleGallery() {
        gp.debugSettings.setPlayerGallery(gp.debugSettings.playerGallery() < 0 ? 0 : -1);
    }

    private void cycleGallery(int direction) {
        int state = gp.debugSettings.playerGallery();
        gp.debugSettings.setPlayerGallery(state < 0 ? 0 : Math.floorMod(state + direction, PlayerAnimationGallery.stateCount()));
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

    private void adjustBanked(int delta) {
        gp.wallet.debugAdjustBanked(delta);
        feedback = "Grzyby: " + gp.wallet.banked();
    }

    private void toggleSkill(Skill skill) {
        if (gp.progression.hasSkill(skill)) {
            gp.progression.forget(skill);
            feedback = "Zapomniano: " + skill.title;
        } else {
            if (skill.requires != null && !gp.progression.hasSkill(skill.requires)) gp.progression.learn(skill.requires);
            gp.progression.learn(skill);
            feedback = "Poznano: " + skill.title;
        }
    }

    private void maxUpgrades() {
        Progression p = gp.progression;
        while (p.healthUpgrades() < Progression.MAX_HEALTH_UPGRADES) p.buyHealth(gp.player);
        while (p.shieldUpgrades() < Progression.MAX_SHIELD_UPGRADES) p.buyShield(gp.player);
        while (p.speedElixirs() < Progression.MAX_SPEED_ELIXIRS) p.buySpeedElixir(gp.player);
        if (!p.ownsShotgun()) p.buyShotgun(gp.player);
        if (!p.ownsHeavySword()) p.buyHeavySword(gp.player);
        gp.player.debugRestoreVitals();
        feedback = "Wszystkie ulepszenia i bronie ze sklepu";
    }

    private QuestLog.Quest currentMainQuest() {
        for (QuestLog.Quest quest : gp.questLog.active()) if (quest.main) return quest;
        return null;
    }

    private String currentMainLabel() {
        QuestLog.Quest quest = currentMainQuest();
        return quest == null ? " (brak)" : ": " + quest.title;
    }

    private void completeMainQuest() {
        QuestLog.Quest quest = currentMainQuest();
        if (quest == null) {feedback = "Brak aktywnego questa fabularnego"; return;}
        gp.questLog.complete(quest.id);
        feedback = "Ukończono: " + quest.title;
    }

    private void completeSideQuests() {
        for (String id : List.of(QuestLog.HOARD, QuestLog.SHOPPING, QuestLog.GROTTO, QuestLog.PESTS, QuestLog.CHESTS)) {
            gp.questLog.complete(id);
        }
        feedback = "Questy poboczne ukończone";
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
        UiSkin.header(g,gp.screenWidth/2,y-14,"DEWELOPER");
        g.setFont(hintFont);g.setColor(UiSkin.TEXT_DIM);
        String title = switch(page) {
            case MAIN -> "Menu deweloperskie";
            case TELEPORT -> "Przenieś / wybór sceny";
            case COMBAT -> "Walka i obrażenia";
            case EQUIPMENT -> "Ekwipunek";
            case PROGRESS -> "Grzyby, umiejętności i sklep (utrwala checkpoint)";
            case QUESTS -> "Questy";
            case CHAPTER -> "Rozdział 1 — scenariusze zmieniają bieżący świat";
            case RELICS -> "Relikty — przyznawanie i wyposażenie";
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
        g.drawString("W/S: wybór   ENTER: użyj   A/D: zmień wartość   ESC: wstecz   F1: zamknij",x+27,footer+43);
        g.dispose();
    }
}
