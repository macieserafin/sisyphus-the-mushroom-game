package story;

import dialogue.DialogueLine;
import dialogue.DialogueSpeaker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class StorySequenceStep {
    public enum Type {
        DIALOGUE,
        WAIT_FRAMES,
        ACTION
    }

    private final Type type;
    private final DialogueSpeaker speaker;
    private final List<DialogueSpeaker> sceneSpeakers;
    private final String visualNovelBackgroundPath;
    private final List<String> dialogueLines;
    private final DialogueLine.Presentation presentation;
    private final int frames;
    private final Runnable action;

    private StorySequenceStep(
            Type type,
            DialogueSpeaker speaker,
            List<DialogueSpeaker> sceneSpeakers,
            String visualNovelBackgroundPath,
            List<String> dialogueLines,
            DialogueLine.Presentation presentation,
            int frames,
            Runnable action
    ) {
        this.type = type;
        this.speaker = speaker;
        this.sceneSpeakers = sceneSpeakers == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(sceneSpeakers));
        this.visualNovelBackgroundPath = normalizePath(visualNovelBackgroundPath);
        this.dialogueLines = dialogueLines == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(dialogueLines));
        this.presentation = presentation == null ? DialogueLine.Presentation.BOTTOM_OVERLAY : presentation;
        this.frames = Math.max(0, frames);
        this.action = action;
    }

    public static StorySequenceStep dialogue(DialogueSpeaker speaker, String... lines) {
        return new StorySequenceStep(
                Type.DIALOGUE,
                speaker,
                Collections.singletonList(speaker),
                null,
                Arrays.asList(lines),
                DialogueLine.Presentation.BOTTOM_OVERLAY,
                0,
                null
        );
    }

    public static StorySequenceStep visualNovelDialogue(DialogueSpeaker speaker, String... lines) {
        return new StorySequenceStep(
                Type.DIALOGUE,
                speaker,
                Collections.emptyList(),
                null,
                Arrays.asList(lines),
                DialogueLine.Presentation.VISUAL_NOVEL,
                0,
                null
        );
    }

    public static StorySequenceStep visualNovelDialogueWithCast(
            DialogueSpeaker speaker,
            List<DialogueSpeaker> sceneSpeakers,
            String... lines
    ) {
        return visualNovelDialogueWithCastAndBackground(speaker, sceneSpeakers, null, lines);
    }

    public static StorySequenceStep visualNovelDialogueWithBackground(
            DialogueSpeaker speaker,
            String visualNovelBackgroundPath,
            String... lines
    ) {
        return new StorySequenceStep(
                Type.DIALOGUE,
                speaker,
                Collections.emptyList(),
                visualNovelBackgroundPath,
                Arrays.asList(lines),
                DialogueLine.Presentation.VISUAL_NOVEL,
                0,
                null
        );
    }

    public static StorySequenceStep visualNovelDialogueWithCastAndBackground(
            DialogueSpeaker speaker,
            List<DialogueSpeaker> sceneSpeakers,
            String visualNovelBackgroundPath,
            String... lines
    ) {
        return new StorySequenceStep(
                Type.DIALOGUE,
                speaker,
                sceneSpeakers,
                visualNovelBackgroundPath,
                Arrays.asList(lines),
                DialogueLine.Presentation.VISUAL_NOVEL,
                0,
                null
        );
    }

    public static StorySequenceStep waitFrames(int frames) {
        return new StorySequenceStep(Type.WAIT_FRAMES, null, null, null, null, null, frames, null);
    }

    public static StorySequenceStep action(Runnable action) {
        return new StorySequenceStep(Type.ACTION, null, null, null, null, null, 0, action);
    }

    public Type type() {
        return type;
    }

    public DialogueSpeaker speaker() {
        return speaker;
    }

    public List<DialogueSpeaker> sceneSpeakers() {
        return sceneSpeakers;
    }

    public String visualNovelBackgroundPath() {
        return visualNovelBackgroundPath;
    }

    public List<String> dialogueLines() {
        return dialogueLines;
    }

    public DialogueLine.Presentation presentation() {
        return presentation;
    }

    public int frames() {
        return frames;
    }

    public Runnable action() {
        return action;
    }

    private String normalizePath(String path) {
        if (path == null || path.trim().isEmpty()) {
            return null;
        }
        return path;
    }
}
