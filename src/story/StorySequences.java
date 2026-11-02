package story;

import entity.npc.NPC;
import main.GamePanel;
import main.GameState;

import java.util.HashMap;
import java.util.Map;

public final class StorySequences {
    private StorySequences() {
    }

    public static StorySequence openingIntro(GamePanel gp) {
        return StoryScriptLoader.load("opening_intro", sourceActions(gp, null));
    }

    public static StorySequence prologueBytEncounterIntro(GamePanel gp) {
        return StoryScriptLoader.load("prologue_byt_encounter_intro", sourceActions(gp, null));
    }

    public static Map<String,Runnable> sourceActions(GamePanel gp,Runnable snap) {
        Map<String, Runnable> actions = new HashMap<>();
        actions.put("complete_intro_cutscene", gp.storyFlags::completeIntroCutscene);
        actions.put("prepare_training_exit", () -> {
            gp.dialogueManager.clear();
            gp.keyH.clearGameplayInput();
            gp.bulletManager.clearBullets();
            gp.enemyShots.clear();
            gp.player.beginScriptedHold();
        });
        actions.put("blink_to_cutscene", () -> gp.ui.blink(18, 14, 28));
        actions.put("blink_to_arena", () -> gp.ui.blink(18, 22, 32));
        actions.put("enter_prologue_byt_arena", () -> {
            gp.levelManager.enterPrologueBytArena();
            gp.exitCutsceneMode(GameState.PLAYING);
        });
        actions.put("release_arena_control", () -> {
            gp.ui.clearBlink();
            gp.player.endScriptedHold();
            gp.keyH.clearGameplayInput();
        });
        if(snap!=null)actions.put("snap_execution",snap);
        NPC[] sage = new NPC[1];
        actions.put("start_awakening", () -> {
            gp.ui.blink(0, 40, 70);
            sage[0] = gp.aSetter.spawnAwakeningSage();
        });
        actions.put("blink_out", () -> gp.ui.blink(22, 26, 40));
        actions.put("remove_awakening_sage", () -> gp.entities.removeNPC(sage[0]));
        actions.put("finish_prologue_byt_encounter", gp.levelManager::finishPrologueBytEncounter);
        return actions;
    }

    public static StorySequence prologueBytScriptedDefeat(GamePanel gp, Runnable snap) {
        return StoryScriptLoader.load("prologue_byt_scripted_defeat", sourceActions(gp,snap));
    }

    public static StorySequence prologueBytAfterSnap(GamePanel gp) {
        return StorySequence.of("prologue_byt_after_snap",
                StorySequenceStep.action(gp.levelManager::finishPrologueBytEncounter));
    }

    public static StorySequence villageAwakening(GamePanel gp) {
        return StoryScriptLoader.load("village_awakening",sourceActions(gp,null));
    }

    public static StorySequence prologueBytPlayerFell(GamePanel gp) {
        return StoryScriptLoader.load("prologue_byt_player_fell", sourceActions(gp,null));
    }
}
