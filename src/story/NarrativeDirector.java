package story;

import main.*;
import entity.npc.NPC;
import level.Interior;
import ui.ChoiceMenu;
import java.util.*;
import java.util.function.BooleanSupplier;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.awt.Rectangle;

/** Resource-defined conversations, events and presentation, sharing one slot-local state. */
public final class NarrativeDirector {
    private final GamePanel gp;
    public final NarrativeState state = new NarrativeState();
    private final Properties data = new Properties();
    private String running = "", runningMap = "", pendingInterior = "";
    private int moveX, moveY, moveTimeout;
    private boolean moving;
    private entity.Entity actor;
    private String pendingTalk="";
    private NPC companion;
    private Runnable completion;
    public boolean dialogue(String id, dialogue.DialogueSpeaker speaker, java.util.List<String> lines, Runnable effect) {
        if(busy()||gp.storySequenceManager.isActive())return false;
        running=id;runningMap=gp.tileM.currentMapPath();completion=effect;
        gp.player.beginScriptedHold();
        gp.storySequenceManager.start(StorySequence.of(id,StorySequenceStep.dialogue(speaker,lines.toArray(String[]::new))));
        return true;
    }
    public void clearPendingTalk(){pendingTalk="";}
    public void trackTalk(NPC npc) { pendingTalk=npcId(npc); }
    public void dialogueEnded() { if(!pendingTalk.isEmpty()){String id=pendingTalk;pendingTalk="";commit(()->state.set("talk."+id));} }
    private int clockFrame;
    public NarrativeDirector(GamePanel gp) {
        this.gp = gp;
        try (var in = getClass().getClassLoader().getResourceAsStream("story/narrative.properties")) {
            if (in != null) data.load(new InputStreamReader(in,StandardCharsets.UTF_8));
        } catch (java.io.IOException e) { throw new IllegalStateException(e); }
    }
    public String get(String key) {
        String text=data.getProperty(key, "");
        if(text.contains("{") && gp.controls!=null)for(var action:settings.ControlSettings.Action.values())text=text.replace("{"+action.name()+"}",gp.controls.keyName(action));
        return text;
    }
    public String[] list(String key) { return Arrays.stream(get(key).split(",")).map(String::trim).filter(s->!s.isEmpty()).toArray(String[]::new); }
    public boolean enabled() { return state.has("narrative.active"); }
    public boolean usesExploration(String map) { return Arrays.asList(list("fog.maps")).contains(map); }
    public boolean mapKnown(String map,int x,int y,int width) { return !usesExploration(map)||gp.exploration.known(map,x,y,width); }
    public void discoverViewport() {
        if(gp.gameState!=GameState.PLAYING || !gp.player.alive || gp.forest.isPreview() || gp.sceneCamera.active()
                || gp.storySequenceManager.isActive() || busy() || gp.narrativeView.modal() || gp.isBlinkTransition())return;
        String map=gp.tileM.currentMapPath();if(!usesExploration(map))return;
        gp.player.updateCamera();
        gp.exploration.reveal(map,gp.tileM.getWorldCols(),gp.tileM.getWorldRows(),gp.sceneCamera.viewport(),gp.tileSize);
    }
    public void tickClock() {
        if(gp.gameState!=GameState.PLAYING||!gp.player.alive||gp.campaign.rebirthing()||gp.isBlinkTransition()
                ||busy()||gp.storySequenceManager.isActive()||gp.narrativeView.modal()
                ||!gp.campaign.started()&&!gp.storyFlags.isPrologueBytEncounterCompleted())return;
        if(++clockFrame>=Math.max(1,number("clock.framesPerSecond",60))){clockFrame=0;state.tick();}
    }
    public boolean daylight() { return Arrays.asList(list("daylight.maps")).contains(gp.tileM.currentMapPath()); }
    public boolean busy() { return !running.isEmpty(); }
    public String status() { return state.time()+" | "+running+" | "+state.facts(); }
    public boolean matches(String expression) { return state.matches(expression); }
    public void event(String id) { state.set(id); }
    public void visit(String id) { event("visit."+id); pendingInterior=id; }
    public void refresh() {
        if (!enabled()) return;
        flag("world.departed",gp.campaign.started());
        flag("world.documents",gp.forest.documents());
        flag("world.anchor",gp.campaign.anchorOpen());
        flag("world.explained",gp.forest.anchorTalk());
        flag("world.boss",gp.campaign.bossCleared());
        flag("world.report",gp.forest.reported());
        flag("world.ellis",gp.village.ellisSettled());
        for(String service:new String[]{"smith","witch","dojo"}) {
            int marker=gp.forest.marker(service);
            flag("service."+service+".accepted",marker==2||marker==3||marker==0);
            flag("service."+service+".done",marker==0);
            if(marker==3)state.set("service."+service+".ready");else state.remove("service."+service+".ready");
        }
        for(String id:list("oreIds"))flag("mined."+id,gp.forest.mined(id));
        flag("ellis.asked",gp.village.ellisAsked());flag("ellis.notebook",gp.village.notebookFound());
        for(String id:list("derivedFacts"))if(matches(get("fact."+id+".when")))state.set(id);
        for(String id:list("anchors"))if(matches(get("anchor."+id+".when")))state.set("anchor."+id);
        gp.questLog.syncStages(state);
    }
    private void flag(String id, boolean value) { if(value)state.set(id); }
    public void tick() {
        if (gp.forest.isPreview() && gp.gameState == GameState.MOD_MENU) return;
        if (moving) {
            int dx=moveX-actor.worldX, dy=moveY-actor.worldY;
            int x=actor.worldX+Integer.signum(dx)*Math.min(3,Math.abs(dx));
            int y=actor.worldY+Integer.signum(dy)*Math.min(3,Math.abs(dy));
            if(--moveTimeout<=0 || gp.cChecker.checkTileAt(actor,x,y) || gp.cChecker.checkObjectAt(actor,x,y)) {
                cancel(); gp.ui.showMessage(get("movement.blocked")); return;
            }
            actor.scriptedStep(x,y);
            moving=x!=moveX||y!=moveY;
            if(!moving)actor.scriptedFace(moveX,moveY);
        }
        if (!running.isEmpty()) {
            if (!runningMap.equals(gp.tileM.currentMapPath())) { cancel(); return; }
            if (!gp.storySequenceManager.isActive()) finish();
            return;
        }
        if(!enabled() || gp.gameState!=GameState.PLAYING || gp.isBlinkTransition() || gp.storySequenceManager.isActive()
                || gp.dialogueManager.hasActiveLine() || gp.campaign.rebirthing() || gp.campaign.victoryInProgress() || !gp.player.alive) return;
        refresh();
        for(String id:list("popups")) if(!state.has("popup."+id) && matches(get("popup."+id+".when"))) {
            gp.narrativeView.show(get("popup."+id+".title"),get("popup."+id+".text"),number("popup."+id+".frames",0),()->commit(()->state.set("popup."+id)));
            return;
        }
        if(!pendingInterior.isEmpty()) {
            String id="room."+pendingInterior; pendingInterior="";
            if(!state.has("scene."+id) && !get("scene."+id+".script").isEmpty()) { play(id); return; }
        }
        for(String id:list("triggers")) {
            String p="trigger."+id+".";
            if(!get(p+"map").equals(gp.tileM.currentMapPath()) || !matches(get(p+"when")))continue;
            Rectangle area=new Rectangle(number(p+"x",0)*gp.tileSize,number(p+"y",0)*gp.tileSize,number(p+"w",1)*gp.tileSize,number(p+"h",1)*gp.tileSize);
            if(area.intersects(new Rectangle(gp.player.worldX,gp.player.worldY,gp.tileSize,gp.tileSize)) && !gp.campaign.inCombat()) { play(get(p+"scene"));return; }
        }
    }
    private int number(String key,int fallback) { try{return Integer.parseInt(get(key));}catch(NumberFormatException e){return fallback;} }
    public void begin() {
        state.set("narrative.active");
        gp.questLog.sync(gp);
        gp.questLog.configureStages(data);
        gp.clearQuestNotices();
        companion=gp.aSetter.spawnAwakeningSage();
        play(get("opening"));
        openingPopup();
    }
    private void openingPopup() {
        String id=get("initialPopup");
        if(!id.isBlank()&&!state.has("popup."+id))gp.narrativeView.show(get("popup."+id+".title"),get("popup."+id+".text"),0,()->commit(()->state.set("popup."+id)));
    }
    public boolean play(String id) {
        if(busy()||gp.storySequenceManager.isActive()||id.isBlank()||get("scene."+id+".script").isBlank())return false;
        Map<String,Runnable> actions=new HashMap<>();
        actions.put("camera",()->gp.sceneCamera.move(number("scene."+id+".cameraX",7)*gp.tileSize,number("scene."+id+".cameraY",4)*gp.tileSize,45));
        actions.put("camera_return",()->gp.sceneCamera.returnToPlayer(35));
        actions.put("document",()->gp.narrativeView.document(get("scene."+id+".document")));
        actions.put("document_close",()->gp.narrativeView.document(""));
        actions.put("move",()->{actor=actor(id);moveX=number("scene."+id+".moveX",actor.worldX/gp.tileSize)*gp.tileSize;
            moveY=number("scene."+id+".moveY",actor.worldY/gp.tileSize)*gp.tileSize;moveTimeout=300;moving=true;});
        actions.put("face",()->actor(id).scriptedFace(number("scene."+id+".faceX",gp.player.worldX/gp.tileSize)*gp.tileSize,number("scene."+id+".faceY",gp.player.worldY/gp.tileSize)*gp.tileSize));
        actions.put("pose",()->{var entity=actor(id);int frame=number("scene."+id+".poseFrame",0);
            if(entity==gp.player)gp.player.scriptedPose(get("scene."+id+".poseSheet"),frame);else entity.spriteNum=frame%2+1;});
        Map<String,BooleanSupplier> waits=Map.of("camera",()->!gp.sceneCamera.moving(),"move",()->!moving);
        StorySequence sequence=StoryScriptLoader.load(get("scene."+id+".script"),actions,waits);
        running=id; runningMap=gp.tileM.currentMapPath();
        gp.player.beginScriptedHold();gp.keyH.clearGameplayInput();
        gp.storySequenceManager.start(sequence);
        return true;
    }
    private void finish() {
        String id=running;Runnable effect=completion;completion=null;running="";cleanup();
        commit(()->{
            state.set("scene."+id);
            for(String fact:list("scene."+id+".facts"))state.set(fact);
            String action=get("scene."+id+".effect");
            switch(action) {
                case "prepare" -> gp.campaign.prepareDeparture();
                case "explain" -> gp.forest.explainWithoutSave();
                case "report" -> gp.forest.reportWithoutSave();
                default -> { }
            }
            if(effect!=null)effect.run();
            refresh();
        });
        // Travel commits its own world transition after the conversation has finished.
        if(get("scene."+id+".effect").equals("anchor") && state.has("scene."+id)) gp.campaign.bindAnchor();
    }
    private entity.Entity actor(String scene) {
        String id=get("scene."+scene+".actor");
        if(id.isBlank()||id.equals("player"))return gp.player;
        return gp.entities.npcs().stream().filter(n->npcId(n).equals(id)).findFirst().orElseThrow(()->new IllegalStateException("Missing scene actor: "+id));
    }
    public boolean commit(Runnable action) {
        if(gp.forest.isPreview()) { action.run();refresh();return true; }
        Properties previous=gp.saves.snapshot();action.run();refresh();
        if(gp.saves.saveProgress()) return true;
        gp.saves.restoreSnapshot(previous);gp.ui.showMessage(get("save.failed"));return false;
    }
    private void cleanup() { if(companion!=null){gp.entities.removeNPC(companion);companion=null;} moving=false;if(actor!=null&&actor!=gp.player)actor.scriptedFace(actor.worldX,actor.worldY);actor=null;gp.sceneCamera.reset();gp.narrativeView.reset();gp.player.endScriptedHold();gp.keyH.clearGameplayInput();if(gp.gameState==GameState.DIALOGUE||gp.gameState==GameState.CUTSCENE)gp.gameState=GameState.PLAYING; }
    public void onCancelled() { running="";completion=null;cleanup(); }
    public void requestSkip() {
        if(!busy() || !get("scene."+running+".skippable").equals("true"))return;
        GameState previous=gp.gameState;
        gp.choiceMenu.open("Skip this scene?",java.util.List.of(
                new ChoiceMenu.Option("Continue","",()->gp.gameState=previous),
                new ChoiceMenu.Option("Skip scene","",this::skip)),false);
        gp.choiceMenu.protectConfirmation(20);
    }
    public void skip() {
        if(!busy())return;
        String id=running;Runnable effect=completion;
        gp.storySequenceManager.cancel();
        running=id;completion=effect;gp.gameState=GameState.PLAYING;finish();
    }
    public void cancel() { completion=null;pendingTalk="";running="";gp.storySequenceManager.cancel();cleanup(); }
    public boolean interrupted() { return busy()&&!gp.storySequenceManager.isActive(); }
    public String npcId(NPC npc) {
        if(!npc.storyId.isEmpty())return npc.storyId;
        for(String id:list("npcs")) {
            String p="npc."+id+".";
            if(get(p+"name").equals(npc.name) && (get(p+"x").isBlank() || number(p+"x",-1)==npc.worldX/gp.tileSize))return id;
        }
        return "";
    }
    public java.util.List<String> conversations(String npc,String channel) {
        java.util.List<String> result=new ArrayList<>();
        for(String id:list("conversations")) {
            String p="scene."+id+".";
            if(get(p+"npc").equals(npc)&&get(p+"channel").equals(channel)&&matches(get(p+"when")))result.add(id);
        }
        if(result.size()>1)result.removeIf(id->get("scene."+id+".fallback").equals("true"));
        return result;
    }
    public void menu(NPC npc,Runnable fallback,Runnable service) { menu(npc,fallback,service,null); }
    public void menu(NPC npc,Runnable fallback,Runnable service,Runnable quest) {
        String npcId=npcId(npc);
        java.util.List<ChoiceMenu.Option> options=new ArrayList<>();
        options.add(new ChoiceMenu.Option("Talk","",()->{
            var scenes=conversations(npcId,"talk");
            if(scenes.isEmpty()) { fallback.run(); } else play(scenes.getFirst());
        }));
        if(quest!=null)options.add(new ChoiceMenu.Option("Quest","",quest));
        var scenes=conversations(npcId,"quest");
        if(!scenes.isEmpty()) options.add(new ChoiceMenu.Option("Quest","",()->{
            if(scenes.size()==1)play(scenes.getFirst());
            else gp.choiceMenu.open(npc.name,scenes.stream().map(id->new ChoiceMenu.Option(get("scene."+id+".title"),"",()->play(id))).toList(),true);
        }));
        if(service!=null)options.add(new ChoiceMenu.Option("Trade / Training","",service));
        options.add(new ChoiceMenu.Option("Back","",()->{}));
        gp.choiceMenu.open(npc.name,options,true);
    }
    public void recordTalk(NPC npc) {
        String id=npcId(npc);if(!id.isEmpty())commit(()->state.set("talk."+id));
    }
    public int marker(NPC npc) {
        if(!enabled())return 0;
        var choices=conversations(npcId(npc),"quest");
        if(choices.isEmpty())return 0;
        return choices.stream().anyMatch(id->get("scene."+id+".marker").equals("?"))?2:1;
    }
    public record Marker(String label,String map,int x,int y,boolean npc) {}
    public java.util.List<Marker> markers() {
        java.util.List<Marker> result=new ArrayList<>();
        for(String id:list("markers")) {
            String p="marker."+id+".";
            if(matches(get(p+"when")))result.add(new Marker(get(p+"label"),get(p+"map"),number(p+"x",0),number(p+"y",0),false));
        }
        if(enabled() && gp.questLog.has(get("peopleQuest")))for(var npc:gp.entities.npcs()) {
            String id=npcId(npc);
            if(Arrays.asList(list("requiredPeople")).contains(id)&&!state.has("talk."+id))result.add(new Marker(npc.name,gp.tileM.currentMapPath(),npc.worldX/gp.tileSize,npc.worldY/gp.tileSize,true));
        }
        if(gp.campaign.inForest()&&state.has("service.witch.accepted")&&!state.has("service.witch.done")&&!state.has("service.witch.ready"))
            gp.entities.enemies().stream().filter(e->e.alive&&e instanceof entity.enemy.Enemy_Slime)
                .min(Comparator.comparingDouble(e->Math.hypot(e.worldX-gp.player.worldX,e.worldY-gp.player.worldY)))
                .ifPresent(e->result.add(new Marker(get("witch.marker"),gp.tileM.currentMapPath(),e.worldX/gp.tileSize,e.worldY/gp.tileSize,false)));
        if(gp.campaign.inForest())for(var enemy:gp.entities.enemies())if(enemy.alive&&gp.forest.dojoTarget(enemy.persistenceId))
            result.add(new Marker(get("dojo.marker"),gp.tileM.currentMapPath(),enemy.worldX/gp.tileSize,enemy.worldY/gp.tileSize,false));
        return result;
    }
    public void save(Properties p) { state.save(p); }
    public java.util.List<String> knownAnchors() { return Arrays.stream(list("anchors")).filter(id->state.has("anchor."+id)).toList(); }
    public void load(Properties p) {
        completion=null;running="";pendingInterior="";pendingTalk="";clockFrame=0;moving=false;
        gp.sceneCamera.reset();gp.narrativeView.reset();
        state.load(p);
        if(!p.containsKey("narrative.version") && Boolean.parseBoolean(p.getProperty("campaign.sage","false"))) {
            state.set("narrative.active");state.set("scene."+get("opening"));state.set("departure.ready");
        }
        if(enabled()) {
            gp.questLog.configureStages(data);
            for(String house:p.getProperty("quests.houses","").split(","))if(!house.isBlank())state.set("visit."+house);
        }
    }
    public void resume() {
        if(!enabled())return;
        String opening=get("opening");
        if(!state.has("scene."+opening)){play(opening);openingPopup();}
        for(Interior room:Interior.values())if(room.mapPath.equals(gp.tileM.currentMapPath()))pendingInterior=room.name();
        refresh();
    }
    public void debug(String action) {
        gp.forest.beginPreview();
        switch(action) {
            case "begin" -> { cancel();begin(); }
            case "visits" -> { for(String id:list("requiredRooms"))state.set("visit."+id);refresh(); }
            case "cancel" -> cancel();
            case "skip" -> skip();
            case "hour" -> state.advanceHour();
            case "beforeBoss", "afterBoss" -> {
                cancel();state.set("narrative.active");gp.questLog.configureStages(data);
                for(String fact:list("debug."+action+".facts"))state.set(fact);
                gp.storyFlags.completePrologueBytEncounter();gp.campaign.prepareDeparture();
                gp.campaign.debugPrepareChapter(action.equals("afterBoss"));refresh();
            }
            case "popup" -> gp.narrativeView.show(get("debug.title"),get("debug.text"),0,()->{});
            default -> { if(action.startsWith("scene:")){cancel();play(action.substring(6));} }
        }
    }
}
