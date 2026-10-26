package story;

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
}
