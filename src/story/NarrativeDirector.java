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
    public final SceneStage stage;
    private final Map<String,java.util.function.Function<String[],Runnable>> customActions=new HashMap<>();
    private final Map<String,java.util.function.Function<String[],BooleanSupplier>> customWaits=new HashMap<>();
    private final Map<String,Runnable> customEffects=new HashMap<>();
    public void registerAction(String name,java.util.function.Function<String[],Runnable> action){customActions.put(name,action);}
    public void registerWait(String name,java.util.function.Function<String[],BooleanSupplier> wait){customWaits.put(name,wait);}
    public void registerEffect(String name,Runnable effect){customEffects.put(name,effect);}
    private final Map<String,Runnable> customAfter=new HashMap<>();
    public void registerAfter(String name,Runnable after){customAfter.put(name,after);}
    private final Set<String> disarmed=new HashSet<>();
    private final Map<String,java.util.function.BooleanSupplier> derived=new LinkedHashMap<>();
    public void registerFact(String id,java.util.function.BooleanSupplier value){derived.put(id,value);}
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
        this.stage = new SceneStage(gp, this);
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
    public Properties definitions(){Properties copy=new Properties();copy.putAll(data);return copy;}
    public void previewDefinitions(Properties definitions) {
        if(gp.store.isPersistent())throw new IllegalStateException("Preview requires an isolated store");
        if(busy())cancel();data.clear();data.putAll(definitions);
    }
    public StorySequence compileSource(String id, java.util.List<String> source) {
        if(busy())throw new IllegalStateException("Pause and restart before compiling a different scene");
        stage.begin(id);
        java.util.List<String> lines=new ArrayList<>();
        for(String speaker:list("speakers"))lines.add("speaker "+speaker+" = "+get("speaker."+speaker));
        lines.addAll(source);
        return StoryScriptLoader.parse(id,lines,text->action(id,text),this::await,this::matches);
    }
    public void previewSource(String id, java.util.List<String> source) {
        if(gp.store.isPersistent())throw new IllegalStateException("Preview requires an isolated store");
        cancel();stage.begin(id);
        StorySequence sequence=compileSource(id,source);
        running=id;runningMap=gp.tileM.currentMapPath();gp.player.beginScriptedHold();gp.keyH.clearGameplayInput();
        gp.storySequenceManager.start(sequence);
    }
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
    public String running() { return running; }
    public java.util.Set<String> keys() { return data.stringPropertyNames(); }
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
        for(var entry:derived.entrySet())if(entry.getValue().getAsBoolean())state.set(entry.getKey());else state.remove(entry.getKey());
        gp.questLog.syncStages(state);
    }
    private void flag(String id, boolean value) { if(value)state.set(id); }
    public void tick() {
        if (gp.forest.isPreview() && gp.gameState == GameState.MOD_MENU) return;
        if (busy()) stage.tick(); else rearm();
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
        if(!pendingInterior.isEmpty()) {
            String room=pendingInterior, id="room."+room; pendingInterior="";
            if(!state.has("scene."+id) && playable(id)) { play(id); return; }
            for(String comment:list("room."+room+".comments"))
                if(!state.has("scene."+comment)&&matches(get("scene."+comment+".when"))&&playable(comment)) { play(comment); return; }
        }
        for(String id:list("triggers")) {
            String p="trigger."+id+".";
            if(!get(p+"map").equals(gp.tileM.currentMapPath()) || !matches(get(p+"when")))continue;
            Rectangle area=new Rectangle(number(p+"x",0)*gp.tileSize,number(p+"y",0)*gp.tileSize,number(p+"w",1)*gp.tileSize,number(p+"h",1)*gp.tileSize);
            if(area.intersects(new Rectangle(gp.player.worldX,gp.player.worldY,gp.tileSize,gp.tileSize)) && !gp.campaign.inCombat()) { play(get(p+"scene"));return; }
        }
        for(String id:list("autoScenes")) if(autoReady(id)) { play(id); return; }
        if(gp.player.mining()||gp.player.isScriptedHold())return;
        for(String id:list("popups")) if(!state.has("popup."+id) && matches(get("popup."+id+".when"))) {
            gp.narrativeView.show(get("popup."+id+".title"),get("popup."+id+".text"),number("popup."+id+".frames",0),()->commit(()->state.set("popup."+id)));
            return;
        }
    }
    private boolean autoReady(String id) {
        String p="scene."+id+".";
        if(state.has("scene."+id)||!matches(get(p+"when"))||!playable(id))return false;
        if(!get(p+"map").isBlank()&&!get(p+"map").equals(gp.tileM.currentMapPath()))return false;
        if(!get(p+"combat").equals("true")&&gp.campaign.inCombat())return false;
        if(gp.player.isScriptedHold())return false;
        String area=get(p+"area");
        if(area.isBlank())return true;
        String[] v=area.split(",");
        Rectangle r=new Rectangle((int)Math.round(Double.parseDouble(v[0].trim())*gp.tileSize),(int)Math.round(Double.parseDouble(v[1].trim())*gp.tileSize),
                (int)Math.round(Double.parseDouble(v[2].trim())*gp.tileSize),(int)Math.round(Double.parseDouble(v[3].trim())*gp.tileSize));
        return r.intersects(new Rectangle(gp.player.worldX,gp.player.worldY,gp.tileSize,gp.tileSize));
    }
    public boolean playable(String id) { return !get("scene."+id+".script").isBlank()||!get("scene."+id+".lines").isBlank(); }
    private int number(String key,int fallback) { try{return Integer.parseInt(get(key));}catch(NumberFormatException e){return fallback;} }
    public void begin() {
        state.set("narrative.active");
        gp.questLog.sync(gp);
        gp.questLog.configureStages(data);
        gp.clearQuestNotices();
        play(get("opening"));
        openingPopup();
    }
    private void openingPopup() {
        String id=get("initialPopup");
        if(!id.isBlank()&&!state.has("popup."+id))gp.narrativeView.show(get("popup."+id+".title"),get("popup."+id+".text"),0,()->commit(()->state.set("popup."+id)));
    }
    public boolean play(String id) {
        if(busy()||gp.storySequenceManager.isActive()||id.isBlank()||!playable(id))return false;
        stage.begin(id);
        StorySequence sequence=sequence(id);
        running=id; runningMap=gp.tileM.currentMapPath();
        gp.player.beginScriptedHold();gp.keyH.clearGameplayInput();
        gp.storySequenceManager.start(sequence);
        return true;
    }
    public StorySequence sequence(String id) {
        String script=get("scene."+id+".script");
        java.util.List<String> lines=new ArrayList<>();
        for(String speaker:list("speakers"))lines.add("speaker "+speaker+" = "+get("speaker."+speaker));
        if(script.isBlank()) for(String line:get("scene."+id+".lines").split("\\|")) { lines.add("[overlay]"); lines.add(line.contains(": ")?line:"player: "+line); }
        else lines.addAll(StoryScriptLoader.lines(script));
        return StoryScriptLoader.parse(script.isBlank()?id:script,lines,text->action(id,text),this::await,this::matches);
    }
    private Runnable action(String id,String text) {
        String[] a=text.trim().split("\\s+");
        String p="scene."+id+".";
        switch(a[0]) {
            case "camera" -> {
                if(a.length==1) return ()->gp.sceneCamera.move(number(p+"cameraX",7)*gp.tileSize,number(p+"cameraY",4)*gp.tileSize,45);
                requirePoint(a[1]); int frames=a.length>2?Integer.parseInt(a[2]):45;
                return ()->{var at=stage.point(a[1]);gp.sceneCamera.move(at.x+gp.tileSize/2,at.y+gp.tileSize/2,frames);};
            }
            case "camera_return" -> { int frames=a.length>1?Integer.parseInt(a[1]):35; return ()->gp.sceneCamera.returnToPlayer(frames); }
            case "document" -> {
                if(a.length==1) return ()->gp.narrativeView.document(get(p+"document"));
                String doc=a[1];
                if(get("document."+doc+".image").isBlank()&&get("document."+doc+".text").isBlank())throw new IllegalStateException("Unknown document "+doc+" in scene "+id);
                return ()->showDocument(doc);
            }
            case "document_close" -> { return ()->gp.narrativeView.document(""); }
            case "read" -> {
                String doc=a[1];
                if(get("document."+doc+".text").isBlank())throw new IllegalStateException("Unknown document "+doc+" in scene "+id);
                return ()->{
                    gp.narrativeView.read(get("document."+doc+".image"),get("document."+doc+".title"),get("document."+doc+".text"),null);
                    gp.narrativeView.illustration(!get("document."+doc+".layout").equals("letter"));
                };
            }
            case "move" -> {
                if(a.length==1) return ()->{actor=actor(id);moveX=number(p+"moveX",actor.worldX/gp.tileSize)*gp.tileSize;
                    moveY=number(p+"moveY",actor.worldY/gp.tileSize)*gp.tileSize;moveTimeout=300;moving=true;};
                int speed=Arrays.stream(a).filter(t->t.startsWith("speed=")).map(t->Integer.parseInt(t.substring(6))).findFirst().orElse(a[1].equals("player")?3:2);
                java.util.List<String> points=Arrays.stream(a).skip(2).filter(t->!t.startsWith("speed=")).toList();
                if(points.isEmpty())throw new IllegalStateException("move needs a point in scene "+id);
                points.forEach(this::requirePoint);
                return ()->stage.move(a[1],points,speed);
            }
            case "face" -> {
                if(a.length==1) return ()->actor(id).scriptedFace(number(p+"faceX",gp.player.worldX/gp.tileSize)*gp.tileSize,number(p+"faceY",gp.player.worldY/gp.tileSize)*gp.tileSize);
                String target=a[2];
                return switch(target) {
                    case "left" -> ()->stage.face(a[1],-1,0);
                    case "right" -> ()->stage.face(a[1],1,0);
                    case "up" -> ()->stage.face(a[1],0,-1);
                    case "down" -> ()->stage.face(a[1],0,1);
                    default -> { requirePoint(target); yield ()->stage.face(a[1],target); }
                };
            }
            case "pose" -> {
                if(a.length==1) return ()->{var entity=actor(id);int frame=number(p+"poseFrame",0);
                    if(entity==gp.player)gp.player.scriptedPose(get(p+"poseSheet"),frame);else entity.spriteNum=frame%2+1;};
                int frame=a.length>3?Integer.parseInt(a[3]):0;
                return ()->stage.pose(a[1],a[2],frame);
            }
            case "anim" -> {
                int from=Integer.parseInt(a[2]),to=Integer.parseInt(a[3]),ticks=a.length>4?Integer.parseInt(a[4]):8;
                return ()->stage.animate(a[1],from,to,ticks);
            }
            case "place" -> { requirePoint(a[2]); return ()->stage.place(a[1],a[2]); }
            case "spawn" -> { return ()->stage.spawn(a[1]); }
            case "despawn" -> { return ()->stage.despawn(a[1]); }
            case "keep" -> { return ()->stage.keep(a[1]); }
            case "title" -> {
                String title=get("title."+a[1]+".text");
                if(title.isBlank())throw new IllegalStateException("Unknown title "+a[1]+" in scene "+id);
                boolean keep=Arrays.asList(a).contains("keep"),black=Arrays.asList(a).contains("from_black");
                return ()->gp.titleCard.show(title,black,keep);
            }
            case "blackout" -> { return gp.titleCard::blackout; }
            case "music" -> { boolean hold=a[1].equals("hold"); return ()->musicHold=hold; }
            case "notice" -> {
                String title=get("notice."+a[1]+".title"),body=get("notice."+a[1]+".text");
                if(body.isBlank())throw new IllegalStateException("Unknown notice "+a[1]+" in scene "+id);
                return ()->gp.narrativeView.show(title,body,0,()->{});
            }
            case "title_release" -> { int frames=a.length>1?Integer.parseInt(a[1]):36; return ()->gp.titleCard.release(frames); }
            case "blink" -> { int open=a.length>1?Integer.parseInt(a[1]):30; return ()->gp.ui.blink(0,6,open); }
            case "sound" -> {
                int sound;
                try { sound=audio.Sound.class.getField(a[1]).getInt(null); } catch(ReflectiveOperationException e) { throw new IllegalStateException("Unknown sound "+a[1]+" in scene "+id); }
                return ()->gp.playSoundEffect(sound);
            }
            default -> {
                var custom=customActions.get(a[0]);
                return custom==null?null:custom.apply(Arrays.copyOfRange(a,1,a.length));
            }
        }
    }
    private void requirePoint(String name) { if(!stage.pointDefined(name))throw new IllegalStateException("Unknown scene point "+name); }
    private BooleanSupplier await(String text) {
        String[] a=text.trim().split("\\s+");
        return switch(a[0]) {
            case "camera" -> ()->!gp.sceneCamera.moving();
            case "move" -> a.length>1?()->!stage.moving(a[1]):()->!moving&&!stage.moving();
            case "anim" -> stage.animationDone();
            case "read" -> ()->!gp.narrativeView.reading();
            case "title" -> ()->!gp.titleCard.active();
            case "notice" -> ()->!gp.narrativeView.modal();
            default -> { var custom=customWaits.get(a[0]); yield custom==null?null:custom.apply(Arrays.copyOfRange(a,1,a.length)); }
        };
    }
    public void reread(String doc) {
        if(busy()||gp.storySequenceManager.isActive())return;
        gp.narrativeView.read(get("document."+doc+".image"),get("document."+doc+".title"),get("document."+doc+".text"),()->gp.narrativeView.document(""));
        gp.narrativeView.illustration(!get("document."+doc+".layout").equals("letter"));
    }
    public java.util.List<String> knownDocuments() {
        return Arrays.stream(list("documents")).filter(doc->state.has("doc."+doc)||matches(get("document."+doc+".when"))&&!get("document."+doc+".when").isBlank()).toList();
    }
    public void showDocument(String doc) {
        gp.narrativeView.document(get("document."+doc+".image"),get("document."+doc+".title"),get("document."+doc+".text"));
        gp.narrativeView.illustration(!get("document."+doc+".layout").equals("letter"));
    }
    private void finish() {
        String id=running;Runnable effect=completion;completion=null;running="";cleanup();
        commit(()->{
            state.set("scene."+id);
            for(String fact:list("scene."+id+".facts"))state.set(fact);
            for(String action:list("scene."+id+".effect")) switch(action) {
                case "prepare" -> gp.campaign.prepareDeparture();
                case "explain" -> gp.forest.explainWithoutSave();
                case "report" -> gp.forest.reportWithoutSave();
                case "anchor" -> { }
                default -> { var custom=customEffects.get(action); if(custom==null)throw new IllegalStateException("Unknown scene effect "+action); custom.run(); }
            }
            if(effect!=null)effect.run();
            String npc=get("scene."+id+".npc"),channel=get("scene."+id+".channel");
            if(!npc.isBlank()&&(channel.equals("talk")||channel.equals("intro")||channel.equals("quest")))state.set("talk."+npc);
            refresh();
        });
        if(state.has("scene."+id)) {
            String npc=get("scene."+id+".npc");
            if(get("scene."+id+".channel").equals("intro")&&!npc.isBlank())disarmed.add(npc);
            for(String after:list("scene."+id+".after")){var run=customAfter.get(after);if(run==null)throw new IllegalStateException("Unknown scene follow-up "+after);run.run();}
        }
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
    private void cleanup() { musicHold=false; stage.reset(); if(companion!=null){gp.entities.removeNPC(companion);companion=null;} moving=false;if(actor!=null&&actor!=gp.player)actor.scriptedFace(actor.worldX,actor.worldY);actor=null;gp.sceneCamera.reset();gp.narrativeView.reset();gp.player.endScriptedHold();gp.keyH.clearGameplayInput();if(gp.gameState==GameState.DIALOGUE||gp.gameState==GameState.CUTSCENE)gp.gameState=GameState.PLAYING; }
    public void onCancelled() { running="";completion=null;cleanup(); }
    public boolean skippable(String id) { return get("scene."+id+".skippable").equals("true")||Arrays.asList(list("skippable")).contains(id); }
    public void requestSkip() {
        if(!busy() || !skippable(running))return;
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
            if(get(p+"npc").equals(npc)&&get(p+"channel").equals(channel)&&matches(get(p+"when"))
                    &&!(channel.equals("intro")&&state.has("scene."+id)))result.add(id);
        }
        if(result.size()>1)result.removeIf(id->get("scene."+id+".fallback").equals("true"));
        return result;
    }
    public void menu(NPC npc,Runnable fallback,Runnable service) { menu(npc,fallback,service,null); }
    public void menu(NPC npc,Runnable fallback,Runnable service,Runnable quest) {
        String npcId=npcId(npc);
        java.util.List<ChoiceMenu.Option> options=new ArrayList<>();
        options.add(new ChoiceMenu.Option("Talk","",()->talk(npc,fallback)));
        var scenes=conversations(npcId,"quest");
        if(!scenes.isEmpty()) options.add(new ChoiceMenu.Option("Quest","",()->{
            if(scenes.size()==1)play(scenes.getFirst());
            else {
                Runnable parent=()->menu(npc,fallback,service,quest);
                var cases=new ArrayList<>(scenes.stream().map(id->new ChoiceMenu.Option(get("scene."+id+".title"),"",()->play(id))).toList());
                cases.add(new ChoiceMenu.Option("Back","",parent));
                gp.choiceMenu.openForNpc(npc,cases,parent);
            }
        }));
        else if(quest!=null)options.add(new ChoiceMenu.Option("Quest","",quest));
        String label=get("npc."+npcId+".service");
        if(service!=null)options.add(new ChoiceMenu.Option(label.isBlank()?"Shop":label,"",service));
        options.add(new ChoiceMenu.Option("Back","",()->{}));
        gp.choiceMenu.openForNpc(npc,options,null);
    }
    public boolean interact(NPC npc,Runnable fallback,Runnable service) {
        if(!enabled()||busy()||gp.storySequenceManager.isActive())return false;
        String id=npcId(npc);
        npc.facePlayer();
        if(!disarmed.contains(id)) {
            var intros=conversations(id,"intro");
            if(!intros.isEmpty()) { play(intros.getFirst()); return true; }
        }
        var talk=conversations(id,"talk");
        if(get("npc."+id+".menu").equals("false")) { talk(npc,fallback); return true; }
        menu(npc,fallback,service);
        return true;
    }
    public void talk(NPC npc,Runnable fallback) {
        String npcId=npcId(npc);
        var scenes=conversations(npcId,"talk");
        var direct=scenes.stream().filter(id->!get("scene."+id+".topic").equals("true")).toList();
        var topics=scenes.stream().filter(id->get("scene."+id+".topic").equals("true")).toList();
        if(!direct.isEmpty()) play(direct.getFirst());
        else if(topics.size()==1) play(topics.getFirst());
        else if(!topics.isEmpty()) {
            java.util.List<ChoiceMenu.Option> options=new ArrayList<>(topics.stream().map(id->new ChoiceMenu.Option(get("scene."+id+".title"),"",()->play(id))).toList());
            Runnable parent=npc::speak;
            options.add(new ChoiceMenu.Option("Back","",parent));
            gp.choiceMenu.openForNpc(npc,options,parent);
        }
        else fallback.run();
    }
    private boolean musicHold;
    public boolean musicHeld() { return musicHold && busy(); }
    public void rearmAll() { disarmed.clear(); }
    public Properties dataView() { return data; }
    public boolean armed(String npc) { return !disarmed.contains(npc); }
    private void rearm() {
        if(disarmed.isEmpty())return;
        disarmed.removeIf(id->{
            var npc=gp.entities.npcs().stream().filter(n->npcId(n).equals(id)).findFirst().orElse(null);
            if(npc==null)return true;
            double distance=Math.hypot(npc.worldX-gp.player.worldX,npc.worldY-gp.player.worldY);
            return distance>(npc.talkRangeTiles+0.5)*gp.tileSize;
        });
    }
    public void recordTalk(NPC npc) {
        String id=npcId(npc);if(!id.isEmpty())commit(()->state.set("talk."+id));
    }
    public int marker(NPC npc) {
        if(!enabled())return 0;
        String id=npcId(npc);
        if(!conversations(id,"intro").isEmpty())return 1;
        var choices=conversations(id,"quest");
        if(choices.stream().anyMatch(scene->get("scene."+scene+".marker").equals("?")))return 2;
        if(choices.stream().anyMatch(scene->get("scene."+scene+".marker").equals("dim")))return 3;
        return 0;
    }
    public record Marker(String label,String map,int x,int y,boolean npc) {}
    public java.util.List<Marker> mainQuestMarkers() {
        java.util.List<Marker> result=new ArrayList<>();
        for(String id:list("markers")) {
            String p="marker."+id+".";
            boolean activeMain=Arrays.stream(list(p+"quest")).map(gp.questLog::get)
                    .anyMatch(q->q!=null&&q.main&&q.state()==QuestLog.State.ACTIVE);
            if(activeMain&&matches(get(p+"when")))
                result.add(new Marker(get(p+"label"),get(p+"map"),number(p+"x",0),number(p+"y",0),false));
        }
        return result;
    }
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
        completion=null;running="";pendingInterior="";pendingTalk="";clockFrame=0;moving=false;disarmed.clear();
        gp.sceneCamera.reset();gp.narrativeView.reset();
        state.load(p);
        boolean legacy=!p.containsKey("narrative.version"), sage=Boolean.parseBoolean(p.getProperty("campaign.sage","false"));
        if(legacy && (sage || Boolean.parseBoolean(p.getProperty("story.bytCompleted","false")))) {
            state.set("narrative.active");state.set("scene."+get("opening"));
            for(String fact:new String[]{"chapter.1.started","talk.sage","doc.friendPage","popup.breathe"})state.set(fact);
            if(sage)state.set("departure.ready");
        }
        int version;
        try { version=Integer.parseInt(p.getProperty("narrative.version","0").trim()); } catch(NumberFormatException e) { version=0; }
        if(enabled() && version<2) migrateScenes(p);
        if(enabled()) {
            gp.questLog.configureStages(data);
            for(String house:p.getProperty("quests.houses","").split(","))if(!house.isBlank())state.set("visit."+house);
        }
    }
    private void migrateScenes(Properties p) {
        java.util.function.Predicate<String> on=key->Boolean.parseBoolean(p.getProperty(key,"false").trim());
        if(state.has("popup.guidance")||state.has("popup.chapterStart"))for(String fact:new String[]{"popup.breathe","popup.findingWay"})state.set(fact);
        if(on.test("campaign.started"))for(String fact:new String[]{"scene.threshold","popup.findingWay"})state.set(fact);
        if(on.test("forest.documents"))for(String fact:new String[]{"scene.stationArrival","scene.station","observe.regeneration"})state.set(fact);
        if(on.test("campaign.anchor"))for(String fact:new String[]{"scene.anchor","scene.arrival"})state.set(fact);
        if(on.test("forest.anchorTalk")||!p.containsKey("forest.anchorTalk")&&on.test("campaign.anchor"))for(String fact:new String[]{"scene.explain","popup.anchors"})state.set(fact);
        if(on.test("campaign.boss"))for(String fact:new String[]{"scene.kingIntro","scene.kingFall","scene.kingReturn"})state.set(fact);
        if(on.test("forest.reported")||on.test("campaign.signExplained"))for(String fact:new String[]{"scene.report","scene.chapterEnd","title.chapter1Complete","title.chapter2","notice.storyEnd"})state.set(fact);
        if(on.test("village.ellis.settled"))for(String fact:new String[]{"ellis.bench","scene.bench"})state.set(fact);
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
