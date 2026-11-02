package level;

import audio.Sound;
import main.GamePanel;
import main.GameState;
import progression.Relics;
import ui.ChoiceMenu;

import dialogue.DialogueSpeaker;
import entity.enemy.Enemy;
import object.Portal;
import object.ChapterObject;
import save.Values;
import story.ChapterText;
import story.QuestLog;
import java.awt.Color;
import java.util.*;

public final class Campaign {
    public static final String FOREST = "maps/level1_forest.txt";
    public static final String ARENA = "maps/boss_slime_arena.txt";
    public static final String SECOND = "maps/floor2_entrance.txt";
    public static final int ANCHOR_COL = 147, ANCHOR_ROW = 7;
    public static final int WORKSHOP_COL = VillageLayout.LEGACY_WORKSHOP_COL, WORKSHOP_ROW = VillageLayout.LEGACY_WORKSHOP_ROW;
    private final GamePanel gp;
    private final Map<String, Properties> maps = new LinkedHashMap<>();
    private boolean started, bound, anchorOpen, bossCleared, rewardClaimed, sageMet;
    private boolean warriorMet, recruited, built, signFound, signExplained;
    private int floor = 1, combatTicks, rebirthTicks;
    private boolean restoring;
    private int rewardStage, rewardTicks;
    private boolean rewardSaved;
    private static final int REBIRTH_TICKS=60;
    public record ReturnPoint(String map,int x,int y) {}
    private ReturnPoint lastReturnPoint;
    public ReturnPoint returnPoint() {
        if(SECOND.equals(gp.tileM.currentMapPath()))return new ReturnPoint(SECOND,12*gp.tileSize,13*gp.tileSize);
        if(inForest()||inArena()||lastReturnPoint==null) {
            return new ReturnPoint(FOREST,(anchorOpen?146:LevelManager.RUN_PLAYER_SPAWN_COL)*gp.tileSize,
                    (anchorOpen?8:LevelManager.RUN_PLAYER_SPAWN_ROW)*gp.tileSize);
        }
        return lastReturnPoint;
    }
    public void debugLifeCycle(boolean atAnchor) {
        gp.narrative.cancel();gp.storyFlags.completePrologueBytEncounter();
        started=sageMet=true;anchorOpen=atAnchor;floor=1;rebirthTicks=rewardStage=rewardTicks=0;
        gp.forest.debugPrepareChapter(false);gp.player.debugRestoreVitals();clearCombat();
        gp.levelManager.loadManualRunFloor();
        ReturnPoint point=returnPoint();gp.player.setWorldPosition(point.x(),point.y());
    }

    public boolean victoryInProgress() { return rewardStage > 0; }
    public String victoryBanner() { return rewardStage == 2 ? ChapterText.get("boss.victory") : ""; }

    public Campaign(GamePanel gp) { this.gp = gp; }
    public void debugPrepareChapter(boolean afterBoss) {
        gp.forest.debugPrepareChapter(afterBoss);
        started = bound = sageMet = anchorOpen = true;
        floor = 1;
        bossCleared = rewardClaimed = signFound = signExplained = afterBoss;
        warriorMet = recruited = true;
        built = false;
        rewardStage = rewardTicks = rebirthTicks = 0;
        maps.clear();
        clearCombat();
        gp.player.endScriptedHold();
        gp.questLog.complete(QuestLog.GROTTO);
        if (afterBoss) gp.relics.grant(Relics.Item.CROWN);
        gp.levelManager.restoreCampaignMap(FOREST);
        gp.player.setWorldPosition(146 * gp.tileSize, 8 * gp.tileSize);
        ensureReturnPortal();
        gp.player.updateCamera();
    }
    public boolean started() { return started; }
    public void debugStart() { started = true; sageMet = true; floor = 1; }
    public int floor() { return floor; }
    public boolean anchorOpen() { return anchorOpen; }
    public boolean bossCleared() { return bossCleared; }
    public boolean rewardClaimed() { return rewardClaimed; }
    public boolean recruited() { return recruited; }
    public boolean built() { return built; }
    public boolean restoring() { return restoring; }
    public boolean rebirthing() { return rebirthTicks > 0; }
    public boolean inForest() { return FOREST.equals(gp.tileM.currentMapPath()); }
    public boolean inArena() { return ARENA.equals(gp.tileM.currentMapPath()); }
    public boolean inCombat() {
        if (!gp.player.alive) return false;
        return combatTicks > 0 || gp.entities.enemies().stream().anyMatch(e -> e.alive && e.isEngaged());
    }
    public void engage() { combatTicks = 300; }
    public void clearCombat() { combatTicks = 0; }

    public void tick() {
        gp.forest.flushPending();
        if (rebirthTicks > 0) {
            gp.player.updateRebirth(1-(--rebirthTicks/(double)REBIRTH_TICKS));
            if (rebirthTicks == 0) { gp.player.endScriptedHold();gp.saves.requestProgressSave(); }
            return;
        }
        if (gp.entities.enemies().stream().anyMatch(e -> e.alive && e.isEngaged()) || gp.enemyShots.count() > 0) combatTicks = 300;
        else if (combatTicks > 0) combatTicks--;
        if (gp.storyFlags.isPrologueBytEncounterCompleted() && !bound && !started
                && !gp.storySequenceManager.isActive() && gp.gameState == GameState.PLAYING) {
            bound = true;
            if (!gp.saves.checkpoint()) bound = false;
        }
        if (started && inArena() && gp.player.alive && rewardStage > 0) {
            if (rewardTicks > 0) rewardTicks--;
            if (rewardTicks == 0) {
                if (rewardStage == 1) {
                    if (gp.narrative.enabled() && !gp.narrative.state.has("scene.kingFall")) { if (!gp.narrative.play("kingFall")) gp.narrative.state.set("scene.kingFall"); return; }
                    gp.player.beginScriptedHold();
                    rewardStage = 2; rewardTicks = 90;
                }
                else if (rewardStage == 2) {
                    rewardStage = 3;
                    gp.relics.grant(Relics.Item.CROWN);
                    gp.choiceMenu.open(ChapterText.get("boss.item.received"), List.of(new ChoiceMenu.Option(
                            Relics.Item.CROWN.title(), Relics.Item.CROWN.description(), this::openReward)), false);
                    gp.choiceMenu.protectConfirmation(45);
                } else if (rewardStage == 5) {
                    rewardStage = 6;
                    gp.blinkTransition(() -> {
                        gp.levelManager.returnFromBossArena();
                        gp.player.setWorldPosition(146 * gp.tileSize, 8 * gp.tileSize);
                        rewardStage = 0;
                        gp.ui.showMessage(ChapterText.get(rewardSaved ? "boss.return" : "boss.return.unsaved"));
                    });
                }
            }
        }
    }

    public void prepareDeparture() { sageMet=true; }
    public boolean beginFloor() {
        if(gp.forest.isPreview()) { gp.ui.showMessage("Developer preview active: end preview in F1 before entering the forest"); return false; }
        if(gp.narrative.enabled() && !gp.narrative.state.has("departure.ready")) { say("forest.gate", "You"); return false; }
        if (started) return false;
        if (!sageMet) { say("sage.gate", "Aldous"); return false; }
        int departureX = gp.player.worldX, departureY = gp.player.worldY;
        gp.player.setWorldPosition(VillageLayout.SPAWN_COL * gp.tileSize, VillageLayout.SPAWN_ROW * gp.tileSize);
        boolean saved = gp.saves.checkpoint();
        gp.player.setWorldPosition(departureX, departureY);
        if (!saved) return false;
        started = true; floor = 1;
        return true;
    }

    public void captureCurrent() {
        if (restoring || !started || gp.forest.isPreview()) return;
        String map = gp.tileM.currentMapPath();
        if (map.equals(FOREST) || map.equals(ARENA) || map.equals(SECOND)) maps.put(map, MapMemory.capture(gp));
    }

    public void restoreMap() {
        if(!gp.forest.isPreview()||restoring)MapMemory.restore(gp, maps.get(gp.tileM.currentMapPath()),!restoring);
        clearCombat();
    }

    public int requiredRemaining() { return 0; }

    public boolean bindAnchor() {
        if(gp.forest.isPreview()){gp.ui.showMessage(ChapterText.get("forest.preview"));return false;}
        if (inCombat()) { gp.ui.showMessage("In combat: anchor and portals locked"); return false; }
        boolean first=!anchorOpen;
        Properties previous = gp.saves.snapshot();
        anchorOpen = true;
        ensureReturnPortal();
        gp.questLog.complete(QuestLog.FOREST);
        boolean saved=first&&gp.narrative.enabled() ? gp.saves.checkpointAt("maps/village.txt",VillageLayout.SPAWN_COL,VillageLayout.SPAWN_ROW) : gp.saves.checkpoint();
        if (!saved) {
            gp.saves.restoreSnapshot(previous);
            gp.ui.showMessage("Pattern could not be updated. Your previous checkpoint is safe");
            return false;
        }
        gp.playSoundEffect(Sound.ANCHOR_BIND);
        gp.effects.ring(gp.player.worldX + 24, gp.player.worldY + 36, 75, 25, new Color(0x91C6AD), false);
        if(gp.narrative.enabled()) { if(first&&started)gp.blinkTransition(()->gp.levelManager.loadHub()); return true; }
        gp.ui.showMessage("Return point bound. Progress saved. Village connection open");
        if(first&&started)gp.blinkTransition(()->{gp.levelManager.loadHub();say("forest.anchor.arrival","");});
        return true;
    }

    public void ensureReturnPortal() {
        if (gp.narrative.enabled()) return;
        if (inForest() && anchorOpen() && gp.entities.objects().stream().noneMatch(o -> o.persistenceId.equals("Portal:145:7")))
            gp.entities.addObject(new Portal(TransitionTarget.hub()), 145, 7);
    }

    public void bossDefeated() {
        bossCleared = true; clearCombat(); gp.enemyShots.clear(); gp.bulletManager.clearBullets();
        gp.forest.bossSample();
        if (started && gp.narrative.enabled()) gp.chapterOne.placeCoreRemains();
        if (started) {
            rewardStage = 1; rewardTicks = 120;
            gp.keyH.clearGameplayInput(); gp.player.beginScriptedHold();
        }
        for (var object : new ArrayList<>(gp.entities.objects())) if (object instanceof object.MushroomPickup pickup) {
            gp.wallet.collect(pickup.value()); gp.entities.removeObject(object);
        }
        if(started)gp.saves.checkpointAt(FOREST,146,8);
    }

    public void resumeReward(){rewardStage=3;gp.relics.grant(Relics.Item.CROWN);openReward();}

    public void openReward() {
        if (rewardStage != 3 || rewardClaimed) return;
        rewardStage = 4;
        List<ChoiceMenu.Option> rows = new ArrayList<>();
        for (Relics.Item item : List.of(Relics.Item.RESIN, Relics.Item.EMBER, Relics.Item.THREAD))
            rows.add(new ChoiceMenu.Option(item.title(), item.description(), () -> claimReward(item)));
        gp.choiceMenu.open("The King's legacy - choose one", rows, false);
        gp.choiceMenu.protectConfirmation(20);
    }

    public void claimReward(Relics.Item selected) {
        if (rewardClaimed || rewardStage != 4 || !bossCleared || selected == Relics.Item.CROWN) return;
        gp.relics.grant(Relics.Item.CROWN); gp.relics.grant(selected);
        gp.playSoundEffect(Sound.REWARD);
        rewardClaimed = true;
        rewardStage = 5; rewardTicks = 60;
        rewardSaved = gp.saves.checkpointAt(FOREST, 146, 8);
        if (rewardSaved) gp.ui.showMessage(ChapterText.get("boss.return.pending"));
    }

    public void requestDescent() {
        if(gp.forest.isPreview()){gp.ui.showMessage(ChapterText.get("forest.preview"));return;}
        if (!rewardClaimed || !gp.forest.reported()) { say("forest.report.required", gp.narrative.enabled() ? "You" : ""); return; }
        if (inCombat()) { gp.ui.showMessage("In combat: passage locked"); return; }
        gp.choiceMenu.open(ChapterText.get("forest.descent.title"), List.of(
                new ChoiceMenu.Option("Stay on floor 1", ChapterText.get("descent.warning"), () -> {}),
                new ChoiceMenu.Option("Descend to floor 2", ChapterText.get("descent.warning"), this::descend)), true);
    }

    private void descend() {
        gp.blinkTransition(this::performDescent);
    }

    private void performDescent() {
        Properties previous = gp.saves.snapshot();
        floor = 2;
        int hp = gp.player.health, shield = gp.player.shield();
        restoring = true;
        try { gp.levelManager.loadSecondFloor(); } finally { restoring = false; }
        if (!gp.saves.checkpoint()) { gp.saves.restoreSnapshot(previous); return; }
        say("floor2.arrival", "");
    }

    public void talkSage() {
        sageMet = true;
        if(bossCleared&&gp.forest.sample()&&!gp.forest.reported()){
            if(gp.forest.report()){signExplained=true;gp.questLog.complete(QuestLog.HOME);say("forest.research.report","Aldous");gp.saves.saveProgress();}return;
        }
        if(anchorOpen&&!gp.forest.anchorTalk()){
            if(gp.forest.talkAnchor())say("forest.anchor.explain","Aldous");return;
        }
        if (signFound) { signExplained = true; gp.questLog.complete(QuestLog.HOME); }
        gp.questLog.complete(QuestLog.VILLAGE);
        say(signFound ? "sage.sign" : "sage.mission", "Aldous");
    }

    public void talkWarrior() {
        if (!warriorMet) {
            warriorMet = true; gp.questLog.start("wanderer"); say("warrior.meet", ChapterText.get("warrior.name"));
        } else if (!recruited && gp.questLog.get(QuestLog.GROTTO)!=null && gp.questLog.get(QuestLog.GROTTO).state() == QuestLog.State.DONE) {
            recruited = true; gp.questLog.complete("wanderer"); say("warrior.join", ChapterText.get("warrior.name"));
        } else if (recruited && anchorOpen() && "maps/village.txt".equals(gp.tileM.currentMapPath())) {
            if (!built) gp.choiceMenu.open("A place in the village", List.of(
                    new ChoiceMenu.Option("Help build the workshop - 30 mushrooms", ChapterText.get("warrior.build"), () -> {
                        if (gp.wallet.spend(30)) {
                            built = true;
                            VillageScenes.prepareLegacyWorkshop(gp);
                            gp.entities.addObject(new ChapterObject(ChapterObject.Kind.WORKSHOP),WORKSHOP_COL,WORKSHOP_ROW);
                            say("warrior.built", ChapterText.get("warrior.name"));
                        }
                        else gp.ui.showMessage("Not enough mushrooms");
                    }), new ChoiceMenu.Option("Later", ChapterText.get("warrior.wait"), () -> {})), true);
            else say("warrior.built", ChapterText.get("warrior.name"));
        } else say("warrior.wait", ChapterText.get("warrior.name"));
    }

    public void foundSign() {
        if (!rewardClaimed) { say("sign.before", ""); return; }
        signFound = true; say("sign.found", "");
    }
    public boolean signFound() { return signFound; }

    public void say(String key, String speaker) {
        gp.dialogueManager.startBlocking(new DialogueSpeaker(speaker, null, new Color(0x91C6AD)), ChapterText.lines(key));
    }

    public void save(Properties out) {
        captureCurrent();
        if(inForest()||inArena()||SECOND.equals(gp.tileM.currentMapPath()))lastReturnPoint=returnPoint();
        ReturnPoint point=returnPoint();
        out.setProperty("campaign.returnMap",point.map());
        out.setProperty("campaign.returnX",Integer.toString(point.x()));
        out.setProperty("campaign.returnY",Integer.toString(point.y()));
        out.setProperty("campaign.version", "1");
        out.setProperty("campaign.map", gp.tileM.currentMapPath());
        out.setProperty("campaign.floor", "" + floor);
        boolean[] flags = {started,bound,anchorOpen,bossCleared,rewardClaimed,sageMet,warriorMet,recruited,built,signFound};
        String[] keys = {"started","bound","anchor","boss","reward","sage","warrior","recruited","built","sign"};
        for (int i = 0; i < keys.length; i++) out.setProperty("campaign." + keys[i], "" + flags[i]);
        out.setProperty("campaign.signExplained", "" + signExplained);
        for (var entry : maps.entrySet()) entry.getValue().forEach((k,v) -> out.setProperty("world." + entry.getKey() + "#" + k, v.toString()));
        if(gp.forest.isPreview()&&started&&(inForest()||inArena()||SECOND.equals(gp.tileM.currentMapPath()))) {
            String prefix="world."+gp.tileM.currentMapPath()+"#";
            out.stringPropertyNames().stream().filter(k->k.startsWith(prefix)).toList().forEach(out::remove);
            MapMemory.capture(gp).forEach((k,v)->out.setProperty(prefix+k,v.toString()));
        }
    }

    public void load(Properties in) {
        started=Values.bool(in,"campaign.started"); bound=Values.bool(in,"campaign.bound");
        anchorOpen=Values.bool(in,"campaign.anchor"); bossCleared=Values.bool(in,"campaign.boss");
        rewardClaimed=Values.bool(in,"campaign.reward"); sageMet=Values.bool(in,"campaign.sage");
        warriorMet=Values.bool(in,"campaign.warrior"); recruited=Values.bool(in,"campaign.recruited");
        built=Values.bool(in,"campaign.built"); signFound=Values.bool(in,"campaign.sign");
        signExplained=Values.bool(in,"campaign.signExplained");
        floor=Math.max(1,Math.min(2,Values.integer(in,"campaign.floor",1)));
        String returnMap=in.getProperty("campaign.returnMap",floor==2?SECOND:FOREST);
        if(!returnMap.equals(FOREST)&&!returnMap.equals(SECOND))returnMap=FOREST;
        int fallbackX=(returnMap.equals(SECOND)?12:anchorOpen?146:LevelManager.RUN_PLAYER_SPAWN_COL)*gp.tileSize;
        int fallbackY=(returnMap.equals(SECOND)?13:anchorOpen?8:LevelManager.RUN_PLAYER_SPAWN_ROW)*gp.tileSize;
        lastReturnPoint=new ReturnPoint(returnMap,Values.integer(in,"campaign.returnX",fallbackX),Values.integer(in,"campaign.returnY",fallbackY));
        combatTicks=0; rebirthTicks=0; rewardStage=0; rewardTicks=0; maps.clear();
        for (String key : in.stringPropertyNames()) if (key.startsWith("world.")) {
            int split=key.indexOf('#');
            if (split>6) maps.computeIfAbsent(key.substring(6,split), k -> new Properties()).setProperty(key.substring(split+1),in.getProperty(key));
        }
    }

    public void restoreLocation(Properties in, boolean rebirth) {
        restoring = true;
        try {
            gp.levelManager.restoreCampaignMap(in.getProperty("campaign.map", "maps/village.txt"));
            gp.player.loadCheckpoint(in);
            if ("maps/village.txt".equals(gp.tileM.currentMapPath())) VillageScenes.ensureSafePlayerPosition(gp);
            ForestScenes.safePosition(gp);
            gp.gameState = GameState.PLAYING;
            gp.keyH.clearGameplayInput();
            if (rebirth) {
                if (gp.narrative.enabled()) gp.narrative.event("defeated.once");
                rebirthTicks = REBIRTH_TICKS;
                gp.playSoundEffect(Sound.AWAKEN);
                gp.player.beginRebirth();
                if(!gp.isBlinkTransition())gp.ui.blink(0, 12, 24);
                gp.effects.burst(gp.player.worldX + 24,gp.player.worldY + 36,36,2,world.render.EffectsManager.SPORES);
                gp.ui.showMessage(ChapterText.get("rebirth.arrival"));
            }
        } finally { restoring = false; }
    }
}
