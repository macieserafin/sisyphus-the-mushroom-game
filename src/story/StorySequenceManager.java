package story;

import dialogue.DialogueLine;
import dialogue.DialogueSpeaker;
import main.GamePanel;
import main.GameState;

import java.util.ArrayList;
import java.util.List;

public class StorySequenceManager {
    private final GamePanel gp;

    private StorySequence activeSequence;
    private StorySequenceStep currentStep;
    private int currentStepIndex = 0;
    private int waitFramesLeft = 0;
    private boolean stepStarted = false;
    private boolean visualNovelSequence = false;
    private GameState returnState = GameState.PLAYING;
    private List<DialogueSpeaker> visualNovelSceneSpeakers = new ArrayList<>();
    private String visualNovelBackgroundPath = null;

    public StorySequenceManager(GamePanel gp) {
        this.gp = gp;
    }

    public void start(StorySequence sequence) {
        if (sequence == null || sequence.stepCount() == 0) {
            return;
        }

        activeSequence = sequence;
        currentStep = null;
        currentStepIndex = 0;
        waitFramesLeft = 0;
        stepStarted = false;
        gp.keyH.clearGameplayInput();
        visualNovelSceneSpeakers.clear();
        visualNovelBackgroundPath = null;

        visualNovelSequence = sequence.usesVisualNovelPresentation();
        if (visualNovelSequence) {
            returnState = gp.gameState == GameState.CUTSCENE ? GameState.PLAYING : gp.gameState;
            gp.enterCutsceneMode();
        }
    }

    public void update() {
        if (activeSequence == null) {
            return;
        }

        if (!stepStarted) {
            startCurrentStep();
            if (activeSequence == null) {
                return;
            }
        }

        if (currentStep == null) {
            finishSequence();
            return;
        }

        switch (currentStep.type()) {
            case DIALOGUE:
                updateDialogueStep();
                break;
            case WAIT_FRAMES:
                updateWaitStep();
                break;
            case ACTION:
                advanceStep();
                break;
            default:
                advanceStep();
                break;
        }
    }

    public boolean isActive() {
        return activeSequence != null;
    }

    public boolean isVisualNovelSequenceActive() {
        return activeSequence != null && visualNovelSequence;
    }

    public boolean canSkipActiveSequence() {
        if (activeSequence == null || !visualNovelSequence) {
            return false;
        }

        String sequenceId = activeSequence.id();
        return "opening_intro".equals(sequenceId) || sequenceId.startsWith("prologue_");
    }

    public void skipActiveSequence() {
        if (!canSkipActiveSequence()) {
            return;
        }

        gp.dialogueManager.clear();
        runRemainingActions();
        finishSequence();
    }

    /** Drops the active sequence without running its remaining actions. */
    public void cancel() {
        if (activeSequence == null) {
            return;
        }
        gp.dialogueManager.clear();
        finishSequence();
    }

    public void completeDialogueStep() {
        if (activeSequence == null || currentStep == null || currentStep.type() != StorySequenceStep.Type.DIALOGUE) {
            return;
        }
        advanceStep();
    }

    private void startCurrentStep() {
        currentStep = activeSequence.stepAt(currentStepIndex);
        stepStarted = true;

        if (currentStep == null) {
            return;
        }

        switch (currentStep.type()) {
            case DIALOGUE:
                if (currentStep.presentation() == DialogueLine.Presentation.VISUAL_NOVEL) {
                    List<DialogueSpeaker> sceneSpeakers = sceneSpeakersForCurrentVisualNovelStep();
                    gp.dialogueManager.startVisualNovel(
                            currentStep.speaker(),
                            sceneSpeakers,
                            backgroundPathForCurrentVisualNovelStep(),
                            currentStep.dialogueLines()
                    );
                } else {
                    gp.dialogueManager.startBlocking(currentStep.speaker(), currentStep.dialogueLines());
                }
                break;
            case WAIT_FRAMES:
                waitFramesLeft = currentStep.frames();
                break;
            case ACTION:
                StorySequence sequence = activeSequence;
                if (currentStep.action() != null) {
                    currentStep.action().run();
                }
                // The action may have ended this sequence or started another one; only move on in our own.
                if (activeSequence == sequence) {
                    advanceStep();
                }
                break;
            default:
                break;
        }
    }

    private List<DialogueSpeaker> sceneSpeakersForCurrentVisualNovelStep() {
        if (!currentStep.sceneSpeakers().isEmpty()) {
            visualNovelSceneSpeakers = new ArrayList<>(currentStep.sceneSpeakers());
            return visualNovelSceneSpeakers;
        }

        addVisualNovelSceneSpeaker(currentStep.speaker());
        return visualNovelSceneSpeakers;
    }

    private String backgroundPathForCurrentVisualNovelStep() {
        if (currentStep.visualNovelBackgroundPath() != null) {
            visualNovelBackgroundPath = currentStep.visualNovelBackgroundPath();
        }
        return visualNovelBackgroundPath;
    }

    private void addVisualNovelSceneSpeaker(DialogueSpeaker speaker) {
        if (speaker == null) {
            return;
        }
        for (DialogueSpeaker sceneSpeaker : visualNovelSceneSpeakers) {
            if (sceneSpeaker == speaker || sceneSpeaker.name().equals(speaker.name())) {
                return;
            }
        }
        visualNovelSceneSpeakers.add(speaker);
    }

    private void updateDialogueStep() {
        if (!gp.dialogueManager.hasActiveLine()) {
            advanceStep();
        }
    }

    private void updateWaitStep() {
        if (waitFramesLeft > 0) {
            waitFramesLeft--;
            return;
        }
        advanceStep();
    }

    private void runRemainingActions() {
        if (activeSequence == null) {
            return;
        }

        int startIndex = stepStarted ? currentStepIndex + 1 : currentStepIndex;
        for (int i = startIndex; i < activeSequence.stepCount(); i++) {
            StorySequenceStep step = activeSequence.stepAt(i);
            if (step != null && step.type() == StorySequenceStep.Type.ACTION && step.action() != null) {
                step.action().run();
            }
        }
    }

    private void advanceStep() {
        if (activeSequence == null) {
            return;
        }
        currentStepIndex++;
        if (currentStepIndex >= activeSequence.stepCount()) {
            finishSequence();
            return;
        }

        currentStep = null;
        waitFramesLeft = 0;
        stepStarted = false;

        if (activeSequence != null) {
            startCurrentStep();
        }
    }

    private void finishSequence() {
        activeSequence = null;
        currentStep = null;
        currentStepIndex = 0;
        waitFramesLeft = 0;
        stepStarted = false;
        if (visualNovelSequence) {
            gp.exitCutsceneMode(returnState);
        }
        visualNovelSequence = false;
        visualNovelSceneSpeakers.clear();
        visualNovelBackgroundPath = null;
        returnState = GameState.PLAYING;
    }
}
