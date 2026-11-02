package level;

import entity.enemy.Enemy;
import main.GamePanel;
import save.Values;
import story.ChapterText;
import java.util.*;

public final class ForestProgress {
    private final GamePanel gp;
    private final Set<String> mined=new HashSet<>(),dojoDead=new HashSet<>();
    private boolean smithAsked,smithDone,witchAsked,witchDone,dojoAsked,dojoDone;
    private boolean documents,supplies,shortcut,anchorTalk,sample,reported;
    private int iron,slime;
    private Properties preview;
    private boolean pendingSave;
    public ForestProgress(GamePanel gp){this.gp=gp;}
    public boolean documents(){return documents;}
    public boolean shortcut(){return shortcut;}
    public boolean anchorTalk(){return anchorTalk;}
    public boolean sample(){return sample;}
    public boolean reported(){return reported;}
    public boolean supplies(){return supplies;}
    public boolean mined(String id){return mined.contains(id);}
    public int iron(){return iron;}
    public int slime(){return slime;}
    public int dojoCount(){return dojoDead.size();}
    public boolean dojoTarget(String id){return dojoAsked&&!dojoDone&&!dojoDead.contains(id)&&id.startsWith("forest1.dojo.");}
    public boolean trainingOpen(){return dojoDone||java.util.Arrays.stream(progression.Skill.values()).anyMatch(gp.progression::hasSkill);}
    public boolean isPreview(){return preview!=null;}
    public Properties previewOrigin(){return preview;}
    public void resumePreview(Properties origin){preview=origin;}
    public void beginPreview(){if(preview==null)preview=gp.saves.snapshot();}
    public void endPreview(){Properties data=preview;preview=null;if(data!=null)gp.saves.restoreSnapshot(data);}
    private void say(String key){gp.campaign.say(key,"");}
    public int marker(String service){
        boolean asked=switch(service){case "smith"->smithAsked;case "witch"->witchAsked;default->dojoAsked;};
        boolean done=switch(service){case "smith"->smithDone;case "witch"->witchDone;default->dojoDone;};
        boolean ready=switch(service){case "smith"->iron>=4;case "witch"->slime>=5;default->dojoDead.size()>=6;};
        return done?0:!asked?1:ready?3:2;
    }
    public void quest(String service){
        if(gp.campaign.inCombat()){say("forest.combat");return;}
        int status=marker(service);
        if(gp.narrative.enabled()) {
            String key="forest."+service+switch(status){case 0->".done";case 1->".request";case 3->".complete";default->".pending";};
            gp.narrative.dialogue("service."+service+"."+status,
                    new dialogue.DialogueSpeaker("",null,new java.awt.Color(0x91C6AD)),ChapterText.lines(key),()->{
                if(status==1) {switch(service){case "smith"->smithAsked=true;case "witch"->witchAsked=true;default->dojoAsked=true;}}
                if(status==3) {switch(service){case "smith"->{smithDone=true;iron-=4;}case "witch"->{witchDone=true;slime-=5;}default->dojoDone=true;}}
                gp.narrative.event("talk."+service);syncQuests();
            });
            return;
        }
        if(status==0){say("forest."+service+".done");return;}
        if(status==1){
            if(change(()->{switch(service){case "smith"->smithAsked=true;case "witch"->witchAsked=true;default->dojoAsked=true;}}))say("forest."+service+".request");
        }else if(status==3){
            if(change(()->{switch(service){case "smith"->{smithDone=true;iron-=4;}case "witch"->{witchDone=true;slime-=5;}default->dojoDone=true;}}))say("forest."+service+".complete");
        }else say("forest."+service+".pending");
    }
    public boolean change(Runnable action){
        if(gp.narrative.enabled())return gp.narrative.commit(()->{action.run();syncQuests();});
        if(isPreview()){action.run();syncQuests();return true;}
        Properties before=new Properties();save(before);gp.questLog.save(before);action.run();syncQuests();
        if(gp.saves.saveProgress())return true;
        gp.questLog.load(before);load(before);say("forest.save.failed");return false;
    }
    public void mine(String id){
        if(mined(id)){say("forest.ore.empty");return;}
        if(!smithAsked){say("forest.ore.tool");return;}
        if(change(()->{mined.add(id);iron+=2;})){gp.playSoundEffect(audio.Sound.MINE);gp.effects.burst(gp.player.worldX+24,gp.player.worldY+24,12,2,world.render.EffectsManager.SPARK);say("forest.ore.collected");}
    }
    public void readDocuments(){if(documents){say("forest.research.repeat");return;}if(change(()->documents=true)){gp.playSoundEffect(audio.Sound.PAPER);say("forest.research.read");}}
    public void takeSupplies(){
        if(supplies){say("forest.supplies.empty");return;}
        int hp=gp.player.health,shield=gp.player.shield();
        if(change(()->{supplies=true;gp.player.debugRestoreVitals();})){gp.playSoundEffect(audio.Sound.SUPPLIES);say("forest.supplies.take");}else gp.player.restoreVitals(hp,shield);
    }
    public void openShortcut(){if(shortcut)return;if(gp.player.worldY+gp.player.solidArea.y<25*gp.tileSize){say("forest.log.wrong");return;}if(change(()->shortcut=true)){gp.playSoundEffect(audio.Sound.SHORTCUT);say("forest.log.open");}}
    public void explainWithoutSave() { anchorTalk=true;syncQuests(); }
    public void reportWithoutSave() { sample=true;reported=true;syncQuests(); }
    public boolean talkAnchor(){return change(()->anchorTalk=true);}
    public boolean report(){return change(()->{sample=true;reported=true;});}
    public void bossSample(){sample=true;syncQuests();}
    public void enemyKilled(Enemy enemy){
        if(!gp.campaign.started()||!gp.campaign.inForest()||enemy.persistenceId.isEmpty())return;
        if(enemy.persistenceId.startsWith("forest1.dojo."))dojoDead.add(enemy.persistenceId);
        if(enemy instanceof entity.enemy.Enemy_Slime)slime++;
        pendingSave=true;syncQuests();
    }
    public void flushPending(){if(pendingSave&&gp.player.alive&&!isPreview()&&gp.saves.saveProgress())pendingSave=false;}
    public void syncQuests(){
        if(documents)gp.questLog.start("core_research");
        if(reported)gp.questLog.complete("core_research");
        String[] services={"smith","witch","dojo"};for(String s:services){int marker=marker(s);if(marker>=2)gp.questLog.start("forest_"+s);if(marker==0)gp.questLog.complete("forest_"+s);}
    }
    public void debugState(String key){
        beginPreview();switch(key){
            case "documents"->documents=!documents;
            case "supplies"->supplies=!supplies;
            case "shortcut"->shortcut=!shortcut;
            case "ore"->{mined.clear();iron=0;smithAsked=true;}
            case "dojo"->{dojoAsked=true;dojoDead.clear();dojoDone=false;}
            case "dojoDone"->{dojoAsked=true;for(int i=1;i<=6;i++)dojoDead.add("forest1.dojo."+i);dojoDone=true;}
            case "sample"->sample=true;
            default->throw new IllegalArgumentException(key);
        }syncQuests();
    }
    public void debugPrepareChapter(boolean afterBoss){documents=anchorTalk=true;sample=reported=afterBoss;syncQuests();}
    public String journalStatus(String id){return switch(id){
        case "forest_smith"->ChapterText.get("forest.iron.label")+": "+iron+" / 4";
        case "forest_witch"->ChapterText.get("forest.slime.label")+": "+slime+" / 5";
        case "forest_dojo"->ChapterText.get("forest.dojo.label")+": "+dojoDead.size()+" / 6";
        case "core_research"->ChapterText.get(reported?"forest.report.label":sample?"forest.sample.label":"forest.hypothesis.label");
        default->"";};}
    public void save(Properties p){
        boolean[] flags={smithAsked,smithDone,witchAsked,witchDone,dojoAsked,dojoDone,documents,supplies,shortcut,anchorTalk,sample,reported};
        String[] names={"smithAsked","smithDone","witchAsked","witchDone","dojoAsked","dojoDone","documents","supplies","shortcut","anchorTalk","sample","reported"};
        for(int i=0;i<names.length;i++)p.setProperty("forest."+names[i],""+flags[i]);
        p.setProperty("forest.iron",""+iron);p.setProperty("forest.slime",""+slime);
        p.setProperty("forest.mined",String.join(",",new TreeSet<>(mined)));p.setProperty("forest.dojoDead",String.join(",",new TreeSet<>(dojoDead)));
    }
    public void load(Properties p){
        preview=null;pendingSave=false;smithAsked=Values.bool(p,"forest.smithAsked");smithDone=Values.bool(p,"forest.smithDone");witchAsked=Values.bool(p,"forest.witchAsked");witchDone=Values.bool(p,"forest.witchDone");
        dojoAsked=Values.bool(p,"forest.dojoAsked");dojoDone=Values.bool(p,"forest.dojoDone");documents=Values.bool(p,"forest.documents");supplies=Values.bool(p,"forest.supplies");shortcut=Values.bool(p,"forest.shortcut");
        anchorTalk=Values.bool(p,"forest.anchorTalk")||!p.containsKey("forest.anchorTalk")&&Values.bool(p,"campaign.anchor");sample=Values.bool(p,"forest.sample")||Values.bool(p,"campaign.boss");reported=Values.bool(p,"forest.reported")||Values.bool(p,"campaign.signExplained");
        iron=Math.max(0,Values.integer(p,"forest.iron",0));slime=Math.max(0,Values.integer(p,"forest.slime",p.containsKey("forest.slime")?0:ForestScenes.legacyResidue(p)));mined.clear();dojoDead.clear();
        for(String id:p.getProperty("forest.mined","").split(","))if(!id.isBlank())mined.add(id);
        for(String id:p.getProperty("forest.dojoDead","").split(","))if(!id.isBlank())dojoDead.add(id);
        // Existing saves that already passed the anchor do not acquire a new historical gate.
        documents|=!p.containsKey("forest.documents")&&Values.bool(p,"campaign.anchor");syncQuests();
    }
}
