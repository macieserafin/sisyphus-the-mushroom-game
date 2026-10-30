package story;

import java.util.Properties;

public class StoryFlags {
    private boolean introCutsceneCompleted = false;
    private boolean prologueTutorialCompleted = false;
    private boolean prologueBytEncounterStarted = false;
    private boolean prologueBytEncounterCompleted = false;

    public boolean isIntroCutsceneCompleted() {
        return introCutsceneCompleted;
    }

    public void completeIntroCutscene() {
        introCutsceneCompleted = true;
    }

    public boolean isPrologueTutorialCompleted() {
        return prologueTutorialCompleted;
    }

    public void completePrologueTutorial() {
        prologueTutorialCompleted = true;
    }

    public boolean isPrologueBytEncounterStarted() {
        return prologueBytEncounterStarted;
    }

    public void startPrologueBytEncounter() {
        prologueBytEncounterStarted = true;
    }

    public boolean isPrologueBytEncounterCompleted() {
        return prologueBytEncounterCompleted;
    }

    public void completePrologueBytEncounter() {
        prologueBytEncounterCompleted = true;
    }

    public void save(Properties out) {
        out.setProperty("story.intro", Boolean.toString(introCutsceneCompleted));
        out.setProperty("story.tutorial", Boolean.toString(prologueTutorialCompleted));
        out.setProperty("story.bytStarted", Boolean.toString(prologueBytEncounterStarted));
        out.setProperty("story.bytCompleted", Boolean.toString(prologueBytEncounterCompleted));
    }

    public void load(Properties in) {
        introCutsceneCompleted = flag(in, "story.intro");
        prologueTutorialCompleted = flag(in, "story.tutorial");
        prologueBytEncounterStarted = flag(in, "story.bytStarted");
        prologueBytEncounterCompleted = flag(in, "story.bytCompleted");
    }

    private static boolean flag(Properties in, String key) {
        return Boolean.parseBoolean(in.getProperty(key, "false").trim());
    }
}
