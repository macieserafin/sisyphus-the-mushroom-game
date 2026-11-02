package level;

import entity.npc.NPC;
import main.GamePanel;
import object.ChapterObject;
import tile.MapPatch;
import ui.ChoiceMenu;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

public final class ChapterOne {
    private final GamePanel gp;
    private BufferedImage[] slimeFrames;
    private double slimeX, slimeY, slimeTargetX, slimeTargetY;
    private int slimeFrame, slimeTick, slimeMode = -1;
    private boolean benchRepaired;

    public ChapterOne(GamePanel gp) {
        this.gp = gp;
        var narrative = gp.narrative;
        for (String service : new String[]{"smith", "witch", "dojo"}) {
            narrative.registerEffect("accept." + service, () -> gp.forest.acceptWithoutSave(service));
            narrative.registerEffect("turnin." + service, () -> gp.forest.turnInWithoutSave(service));
            narrative.registerFact("materials." + service, () -> gp.forest.materialsReady(service));
        }
        narrative.registerFact("residue.active", () -> gp.forest.residueActive());
        narrative.registerEffect("accept.ellis", () -> gp.village.askEllisWithoutSave());
        narrative.registerEffect("settle.ellis", () -> gp.village.settleEllisWithoutSave());
        narrative.registerEffect("find.notebook", () -> gp.village.findNotebookWithoutSave());
        narrative.registerEffect("station", () -> gp.forest.studyWithoutSave());
        narrative.registerAfter("ellis.leaveCamp", () -> {
            gp.entities.npcs().stream().filter(npc -> npc instanceof entity.npc.NPC_Ellis).toList().forEach(gp.entities::removeNPC);
            if (Campaign.FOREST.equals(gp.tileM.currentMapPath())) MapPatch.apply(gp.tileM, "maps/ellis_forest_empty.patch");
        });
        narrative.registerAfter("satchel.taken", () -> {
            gp.entities.objects().stream().filter(o -> o instanceof object.EllisNotebook).toList().forEach(gp.entities::removeObject);
            gp.choiceMenu.open(gp.narrative.get("ui.notebook.title"), List.of(
                    new ChoiceMenu.Option(gp.narrative.get("ui.notebook.read"), "", () -> gp.narrative.play("satchelRead")),
                    new ChoiceMenu.Option(gp.narrative.get("ui.notebook.keep"), "", () -> {})), true);
        });
        narrative.registerAfter("station.note", () -> gp.ui.showMessage(gp.narrative.get("ui.research.updated")));
        narrative.registerAction("healing_slime", a -> switch (a[0]) {
            case "show" -> () -> showSlime(a[1]);
            case "heal" -> () -> { slimeMode = 1; slimeTick = 0; };
            case "leave" -> () -> { var at = gp.narrative.stage.point(a[1]); slimeTargetX = at.x; slimeTargetY = at.y; slimeMode = 2; slimeTick = 0; };
            default -> throw new IllegalStateException("Unknown healing_slime step " + a[0]);
        });
        narrative.registerWait("healing_slime", a -> () -> slimeMode != 1 && slimeMode != 2);
        narrative.registerAction("stone", a -> () -> stone(a[0]));
        narrative.registerAction("hammer", a -> () -> hammer(a[0], a[1].equals("on")));
        narrative.registerAction("bench", a -> () -> benchRepaired = true);
        narrative.registerAction("sleep_commit", a -> this::commitSleep);
        narrative.registerAction("dawn_commit", a -> () -> commitSleep(true));
        narrative.registerAction("toast", a -> () -> gp.ui.showMessage(gp.narrative.get("toast." + a[0])));
    }

    public void placeCoreRemains() {
        for (var enemy : gp.entities.enemies()) if (enemy instanceof entity.enemy.Enemy_SlimeKing) {
            var box = enemy.getWorldHitBox();
            var remains = new ChapterObject(ChapterObject.Kind.REMAINS);
            gp.entities.addObject(remains, 0, 0);
            remains.worldX = (int) box.getCenterX() - gp.tileSize / 2;
            remains.worldY = (int) box.getMaxY() - gp.tileSize / 2;
            return;
        }
    }

    public boolean benchRepaired() { return benchRepaired || gp.narrative.state.has("ellis.bench"); }

    public void resetPresentation() { benchRepaired = false; slimeMode = -1; }

    private BufferedImage[] slimeFrames() {
        if (slimeFrames == null) {
            try (var in = getClass().getResourceAsStream("/enemy/slime_heal.png")) {
                BufferedImage atlas = ImageIO.read(in);
                slimeFrames = new BufferedImage[atlas.getWidth() / 16];
                for (int i = 0; i < slimeFrames.length; i++) slimeFrames[i] = atlas.getSubimage(i * 16, 0, 16, 16);
            } catch (Exception e) { throw new IllegalStateException(e); }
        }
        return slimeFrames;
    }

    private void showSlime(String point) {
        var at = gp.narrative.stage.point(point);
        slimeX = at.x; slimeY = at.y; slimeFrame = 0; slimeMode = 0; slimeTick = 0;
        gp.narrative.stage.ticker("slime", this::tickSlime);
        gp.narrative.stage.drawer("slime", this::drawSlime);
    }

    private static final int[][] HEAL = {{0, 30}, {1, 16}, {0, 18}, {1, 16}, {2, 24}, {3, 16}, {2, 14}, {4, 1}};

    private void tickSlime() {
        slimeTick++;
        if (slimeMode == 1) {
            int t = slimeTick;
            for (int[] step : HEAL) {
                if (t <= step[1]) { slimeFrame = step[0]; return; }
                t -= step[1];
            }
            slimeFrame = 4; slimeMode = 0;
            if (gp.narrative.stage != null) gp.playSoundEffect(audio.Sound.SHIELD_RESTORE);
        } else if (slimeMode == 2) {
            double dx = slimeTargetX - slimeX, dy = slimeTargetY - slimeY, d = Math.hypot(dx, dy);
            if (d < 2) { slimeMode = 3; return; }
            slimeX += dx / d * 1.6; slimeY += dy / d * 1.6;
        }
    }

    private void drawSlime(Graphics2D g) {
        if (slimeMode < 0 || slimeMode == 3) return;
        int bob = slimeMode == 2 ? (int) Math.round(-Math.abs(Math.sin(slimeTick * 0.25)) * 9) : 0;
        int x = (int) Math.round(slimeX) - gp.player.worldX + gp.player.screenX;
        int y = (int) Math.round(slimeY) - gp.player.worldY + gp.player.screenY + bob;
        g.setColor(new Color(0, 0, 0, 70));
        g.fillOval(x + 9, (int) Math.round(slimeY) - gp.player.worldY + gp.player.screenY + 40, 30, 7);
        g.drawImage(slimeFrames()[slimeFrame], x, y, gp.tileSize, gp.tileSize, null);
        if (slimeFrame == 1 || slimeFrame == 3) {
            Composite old = g.getComposite();
            g.setComposite(AlphaComposite.SrcOver.derive(0.45f));
            g.setColor(new Color(0xF2D27A));
            g.fillOval(x + 15, y + 30, 18, 14);
            g.setComposite(old);
        }
    }

    private void stone(String step) {
        for (var object : gp.entities.objects()) if (object instanceof ChapterObject anchor && anchor.isForestAnchor()) {
            if (step.equals("reveal")) { anchor.reveal(); gp.effects.burst(anchor.worldX + 24, anchor.worldY + 30, 10, 1.4, world.render.EffectsManager.SPORES); gp.playSoundEffect(audio.Sound.PAPER); }
            else { anchor.reveal(); gp.playSoundEffect(audio.Sound.ANCHOR_BIND); gp.effects.ring(anchor.worldX + 24, anchor.worldY + 30, 70, 24, new Color(0x91C6AD), false); }
        }
    }

    private void hammer(String actor, boolean on) {
        if (!on) { gp.narrative.stage.ticker("hammer", null); gp.narrative.stage.pose(actor, "clear", 0); return; }
        int[] tick = {0};
        gp.narrative.stage.ticker("hammer", () -> {
            int beat = tick[0]++ / 12 % 2;
            gp.narrative.stage.pose(actor, "ellis_hammer_{dir}", beat + 1);
            if (tick[0] % 24 == 13) gp.playSoundEffect(audio.Sound.MINE);
        });
    }

    public void openBed() {
        if (gp.narrative.busy() || gp.storySequenceManager.isActive()) return;
        Runnable choice = () -> {
            gp.choiceMenu.open(gp.narrative.get("ui.rest.title"), List.of(
                    new ChoiceMenu.Option(gp.narrative.get("ui.rest.sleep"), gp.narrative.get("ui.rest.text"), () -> gp.narrative.play("sleep")),
                    new ChoiceMenu.Option(gp.narrative.get("ui.rest.dawn"), gp.narrative.get("ui.rest.dawnText"), () -> gp.narrative.play("sleepDawn")),
                    new ChoiceMenu.Option(gp.narrative.get("ui.rest.awake"), "", () -> {})), true);
        };
        if (!gp.narrative.state.has("scene.bedLook")) { gp.narrative.play("bedLook"); pendingBedChoice = true; }
        else choice.run();
        bedChoice = choice;
    }

    private boolean pendingBedChoice;
    private Runnable bedChoice;

    public void tick() {
        if (pendingBedChoice && !gp.narrative.busy() && !gp.storySequenceManager.isActive()) {
            pendingBedChoice = false;
            if (gp.narrative.state.has("scene.bedLook") && bedChoice != null) bedChoice.run();
        }
    }

    private void commitSleep() {
        commitSleep(false);
    }

    private void commitSleep(boolean untilSunrise) {
        Point bedside=gp.narrative.stage.point("bedSide");
        boolean saved=gp.narrative.commit(() -> {
            if(untilSunrise) gp.narrative.state.advanceToSunrise();
            else for (int i = 0; i < 8; i++) gp.narrative.state.advanceHour();
            gp.player.debugRestoreVitals();
            gp.narrative.state.set("sleep.first");
            if(bedside!=null)gp.player.setWorldPosition(bedside.x,bedside.y);
        });
        if(!saved) {
            if(bedside!=null)gp.player.setWorldPosition(bedside.x,bedside.y);
            gp.narrative.cancel();gp.titleCard.clear();
        }
    }

    public void anchorInteract(boolean village) {
        if (gp.campaign.inCombat()) { gp.ui.showMessage(gp.narrative.get("ui.anchor.combat")); return; }
        if (!gp.narrative.state.has("world.explained")) {
            if (village) gp.narrative.play(gp.narrative.state.has("world.anchor") ? "anchorWait" : "anchorOld");
            return;
        }
        var known = gp.narrative.knownAnchors();
        List<ChoiceMenu.Option> options = new java.util.ArrayList<>();
        if (!village && known.contains("village")) options.add(new ChoiceMenu.Option(gp.narrative.get("anchor.village.label"), "", () -> travel(true)));
        if (village && known.contains("forest")) options.add(new ChoiceMenu.Option(gp.narrative.get("anchor.forest.label"), "", () -> travel(false)));
        options.add(new ChoiceMenu.Option(gp.narrative.get("ui.back"), "", () -> {}));
        gp.choiceMenu.open(gp.narrative.get("ui.anchor.title"), options, true);
    }

    private void travel(boolean village) {
        if (gp.campaign.inCombat()) { gp.ui.showMessage(gp.narrative.get("ui.anchor.combat")); return; }
        Runnable go = () -> gp.blinkTransition(() -> {
            if (village) gp.levelManager.loadHubAt(VillageLayout.SPAWN_COL, VillageLayout.SPAWN_ROW);
            else gp.levelManager.travelToForestAnchor();
            gp.saves.requestProgressSave();
        });
        if (!gp.narrative.state.has("anchor.firstUse")) {
            if(!gp.narrative.commit(() -> gp.narrative.state.set("anchor.firstUse"))) return;
            gp.player.beginScriptedHold();
            gp.player.scriptedPose("player/anim/player_reach", 1);
            gp.playSoundEffect(audio.Sound.ANCHOR_BIND);
            gp.effects.ring(gp.player.worldX + 24, gp.player.worldY + 24, 70, 24, new Color(0x91C6AD), false);
        }
        go.run();
    }

    public static boolean isEllis(NPC npc) { return npc instanceof entity.npc.NPC_Ellis; }

    public String stage() {
        var s = gp.narrative.state;
        if (s.has("chapter.1.complete")) return "rozdział 1 ukończony";
        if (s.has("world.boss")) return "raport u Aldousa";
        if (s.has("world.explained")) return "przygotowania do Króla";
        if (s.has("world.anchor")) return "wyjaśnienie kotwicy";
        if (s.has("world.documents")) return "kamień przed jaskinią";
        if (s.has("departure.ready")) return "las i stanowisko";
        if (s.has("scene.awakening")) return "obchód wioski";
        return "przed przebudzeniem";
    }

    private void facts(String... ids) { for (String id : ids) gp.narrative.state.set(id); }

    private void opened() {
        gp.narrative.cancel();
        gp.narrative.state.set("narrative.active");
        facts("scene.awakening", "chapter.1.started", "talk.sage", "doc.friendPage", "popup.breathe");
        gp.questLog.configureStages(gp.narrative.dataView());
        gp.storyFlags.completePrologueBytEncounter();
    }

    private void departed() {
        opened();
        for (String room : gp.narrative.list("requiredRooms")) gp.narrative.state.set("visit." + room);
        facts("scene.prepare", "departure.ready", "visits.done");
        gp.campaign.prepareDeparture();
    }

    private void inForest(int col, int row) {
        departed();
        facts("scene.threshold", "popup.findingWay");
        if (!gp.campaign.started()) gp.campaign.debugStart();
        gp.levelManager.restoreCampaignMap(Campaign.FOREST);
        gp.player.setWorldPosition(col * gp.tileSize, row * gp.tileSize);
        ForestScenes.safePosition(gp);
        gp.player.updateCamera();
    }

    public void debug(String action) {
        switch (action) {
            case "visits" -> { opened(); for (String room : gp.narrative.list("requiredRooms")) gp.narrative.state.set("visit." + room); }
            case "prepare" -> { opened(); for (String room : gp.narrative.list("requiredRooms")) gp.narrative.state.set("visit." + room); gp.levelManager.enterInterior(Interior.SAGE); }
            case "intros" -> { opened(); gp.forest.debugResetServices(); for (String id : new String[]{"smith.intro", "witch.intro", "dojo.intro"}) gp.narrative.state.remove("scene." + id); gp.levelManager.enterInterior(Interior.SMITHY); }
            case "rearm" -> gp.narrative.rearmAll();
            case "bed" -> { departed(); gp.levelManager.enterInterior(Interior.SAGE); openBed(); }
            case "sleep" -> commitSleep();
            case "dawn" -> commitSleep(true);
            case "threshold" -> { departed(); gp.narrative.state.remove("scene.threshold"); if (!gp.campaign.started()) gp.campaign.debugStart(); gp.levelManager.restoreCampaignMap(Campaign.FOREST); }
            case "station" -> { inForest(59, 68); gp.forest.debugState("documentsOff"); gp.narrative.play("station"); }
            case "ore" -> { inForest(46, 92); gp.forest.acceptWithoutSave("smith"); gp.forest.debugState("ore"); }
            case "dojo" -> { inForest(11, 88); gp.narrative.state.remove("scene.dojoGround"); }
            case "stone" -> { inForest(146, 13); gp.forest.studyWithoutSave(); gp.narrative.play("anchor"); }
            case "arrival" -> { departed(); facts("world.documents", "scene.anchor"); gp.levelManager.loadHub(); gp.narrative.state.set("world.anchor"); gp.narrative.play("arrival"); }
            case "travel" -> { departed(); facts("world.documents", "scene.anchor", "scene.arrival", "world.explained", "scene.explain", "popup.anchors", "anchor.village", "anchor.forest"); gp.levelManager.loadHub(); gp.player.setWorldPosition(31 * gp.tileSize, 23 * gp.tileSize); anchorInteract(true); }
            case "ellis" -> { inForest(21, 56); gp.village.debugEllisState(false, false, false); }
            case "ellisNotebook" -> { inForest(21, 56); gp.village.debugEllisState(true, true, false); }
            case "bench" -> { departed(); gp.village.debugEllisState(true, true, true); gp.narrative.state.remove("ellis.bench"); gp.narrative.state.remove("scene.bench"); benchRepaired = false; gp.levelManager.loadHub(); gp.player.setWorldPosition(32 * gp.tileSize, 24 * gp.tileSize); }
            case "king" -> { departed(); facts("world.documents", "world.anchor", "world.explained", "scene.station", "scene.anchor", "scene.arrival", "scene.explain"); gp.narrative.state.remove("scene.kingIntro"); gp.campaign.debugPrepareChapter(false); gp.levelManager.enterBossArena(); }
            case "report" -> { departed(); facts("world.documents", "world.anchor", "world.explained", "world.boss", "scene.station", "observe.regeneration"); gp.campaign.debugPrepareChapter(true); gp.forest.debugState("unreported"); gp.levelManager.enterInterior(Interior.SAGE); }
            case "titles" -> { gp.narrative.play("chapterEnd"); }
            case "defeat" -> { gp.narrative.state.remove("scene.firstDefeat"); gp.narrative.play("firstDefeat"); }
            default -> throw new IllegalArgumentException(action);
        }
        gp.narrative.refresh();
    }
}
