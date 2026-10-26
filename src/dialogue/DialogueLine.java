package dialogue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DialogueLine {
    private static final int DEFAULT_TYPEWRITER_SPEED_FRAMES = 2;

    public enum Mode {
        BLOCKING,
        AMBIENT
    }

    public enum Priority {
        LOW,
        NORMAL,
        HIGH,
        STORY
    }

    public enum Presentation {
        BOTTOM_OVERLAY,
        VISUAL_NOVEL
    }

    private final DialogueSpeaker speaker;
    private final List<DialogueSpeaker> sceneSpeakers;
    private final String visualNovelBackgroundPath;
    private final String text;
    private final Mode mode;
    private final Priority priority;
    private final Presentation presentation;
    private final int durationFrames;
    private final int typewriterSpeedFrames;

    public DialogueLine(
            DialogueSpeaker speaker,
            List<DialogueSpeaker> sceneSpeakers,
            String visualNovelBackgroundPath,
            String text,
            Mode mode,
            Priority priority,
            Presentation presentation,
            int durationFrames,
            int typewriterSpeedFrames
    ) {
        if (speaker == null) {
            throw new IllegalArgumentException("Dialogue speaker is required");
        }
        this.speaker = speaker;
        this.sceneSpeakers = normalizeSceneSpeakers(speaker, sceneSpeakers);
        this.visualNovelBackgroundPath = normalizePath(visualNovelBackgroundPath);
        this.text = text == null ? "" : text;
        this.mode = mode == null ? Mode.BLOCKING : mode;
        this.priority = priority == null ? Priority.NORMAL : priority;
        this.presentation = presentation == null ? Presentation.BOTTOM_OVERLAY : presentation;
        this.durationFrames = Math.max(0, durationFrames);
        this.typewriterSpeedFrames = Math.max(0, typewriterSpeedFrames);
    }

    public static DialogueLine blocking(DialogueSpeaker speaker, String text) {
        return new DialogueLine(
                speaker,
                Collections.singletonList(speaker),
                null,
                text,
                Mode.BLOCKING,
                Priority.NORMAL,
                Presentation.BOTTOM_OVERLAY,
                0,
                DEFAULT_TYPEWRITER_SPEED_FRAMES
        );
    }

    public static DialogueLine visualNovel(DialogueSpeaker speaker, String text) {
        return visualNovel(speaker, Collections.singletonList(speaker), null, text);
    }

    public static DialogueLine visualNovel(DialogueSpeaker speaker, List<DialogueSpeaker> sceneSpeakers, String text) {
        return visualNovel(speaker, sceneSpeakers, null, text);
    }

    public static DialogueLine visualNovel(
            DialogueSpeaker speaker,
            List<DialogueSpeaker> sceneSpeakers,
            String visualNovelBackgroundPath,
            String text
    ) {
        return new DialogueLine(
                speaker,
                sceneSpeakers,
                visualNovelBackgroundPath,
                text,
                Mode.BLOCKING,
                Priority.STORY,
                Presentation.VISUAL_NOVEL,
                0,
                DEFAULT_TYPEWRITER_SPEED_FRAMES
        );
    }

    public static DialogueLine ambient(DialogueSpeaker speaker, String text, Priority priority, int durationFrames) {
        return new DialogueLine(
                speaker,
                Collections.singletonList(speaker),
                null,
                text,
                Mode.AMBIENT,
                priority,
                Presentation.BOTTOM_OVERLAY,
                durationFrames,
                DEFAULT_TYPEWRITER_SPEED_FRAMES
        );
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

    public String text() {
        return text;
    }

    public Mode mode() {
        return mode;
    }

    public Priority priority() {
        return priority;
    }

    public Presentation presentation() {
        return presentation;
    }

    public int durationFrames() {
        return durationFrames;
    }

    public int typewriterSpeedFrames() {
        return typewriterSpeedFrames;
    }

    private List<DialogueSpeaker> normalizeSceneSpeakers(DialogueSpeaker speaker, List<DialogueSpeaker> speakers) {
        List<DialogueSpeaker> normalized = new ArrayList<>();
        if (speakers != null) {
            for (DialogueSpeaker sceneSpeaker : speakers) {
                if (sceneSpeaker != null && !containsSpeaker(normalized, sceneSpeaker)) {
                    normalized.add(sceneSpeaker);
                }
            }
        }
        if (!containsSpeaker(normalized, speaker)) {
            normalized.add(speaker);
        }
        return Collections.unmodifiableList(normalized);
    }

    private boolean containsSpeaker(List<DialogueSpeaker> speakers, DialogueSpeaker speaker) {
        for (DialogueSpeaker existing : speakers) {
            if (existing == speaker || existing.name().equals(speaker.name())) {
                return true;
            }
        }
        return false;
    }

    private String normalizePath(String path) {
        if (path == null || path.trim().isEmpty()) {
            return null;
        }
        return path;
    }
}
