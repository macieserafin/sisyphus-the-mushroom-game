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
        Map<String, Runnable> actions = new HashMap<>();
        actions.put("complete_intro_cutscene", gp.storyFlags::completeIntroCutscene);
        return StoryScriptLoader.load("opening_intro", actions);
    }

    public static StorySequence prologueBytEncounterIntro(GamePanel gp) {
        Map<String, Runnable> actions = new HashMap<>();
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
        return StoryScriptLoader.load("prologue_byt_encounter_intro", actions);
    }

    public static StorySequence prologueBytScriptedDefeat(GamePanel gp, Runnable snap) {
        Map<String, Runnable> actions = new HashMap<>();
        actions.put("snap_execution", snap);
        return StoryScriptLoader.load("prologue_byt_scripted_defeat", actions);
    }

    public static StorySequence prologueBytAfterSnap(GamePanel gp) {
        return StorySequence.of("prologue_byt_after_snap",
                StorySequenceStep.action(gp.levelManager::finishPrologueBytEncounter));
    }

    public static StorySequence villageAwakening(GamePanel gp) {
        NPC[] sage = new NPC[1];
        Map<String, Runnable> actions = new HashMap<>();
        actions.put("start_awakening", () -> {
            gp.ui.blink(0, 40, 70);
            sage[0] = gp.aSetter.spawnAwakeningSage();
        });
        actions.put("blink_out", () -> gp.ui.blink(22, 26, 40));
        actions.put("remove_awakening_sage", () -> gp.entities.removeNPC(sage[0]));
        return StoryScriptLoader.load("village_awakening", actions);
    }

    public static StorySequence prologueBytPlayerFell(GamePanel gp) {
        Map<String, Runnable> actions = new HashMap<>();
        actions.put("finish_prologue_byt_encounter", gp.levelManager::finishPrologueBytEncounter);
        return StoryScriptLoader.load("prologue_byt_player_fell", actions);
    }
}
