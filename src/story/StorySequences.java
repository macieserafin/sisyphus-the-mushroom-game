package story;

import entity.npc.NPC;
import main.GamePanel;

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
        actions.put("enter_prologue_byt_arena", gp.levelManager::enterPrologueBytArena);
        return StoryScriptLoader.load("prologue_byt_encounter_intro", actions);
    }

    public static StorySequence prologueBytScriptedDefeat(GamePanel gp) {
        Map<String, Runnable> actions = new HashMap<>();
        actions.put("finish_prologue_byt_encounter", gp.levelManager::finishPrologueBytEncounter);
        return StoryScriptLoader.load("prologue_byt_scripted_defeat", actions);
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
