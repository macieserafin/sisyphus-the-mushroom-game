package story;

import dialogue.DialogueLine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class StorySequence {
    private final String id;
    private final List<StorySequenceStep> steps;

    public StorySequence(String id, List<StorySequenceStep> steps) {
        this.id = id == null || id.trim().isEmpty() ? "story_sequence" : id;
        this.steps = steps == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(steps));
    }

    public static StorySequence of(String id, StorySequenceStep... steps) {
        return new StorySequence(id, Arrays.asList(steps));
    }

    public String id() {
        return id;
    }

    public StorySequenceStep stepAt(int index) {
        if (index < 0 || index >= steps.size()) {
            return null;
        }
        return steps.get(index);
    }

    public int stepCount() {
        return steps.size();
    }

    public boolean usesVisualNovelPresentation() {
        for (StorySequenceStep step : steps) {
            if (step.presentation() == DialogueLine.Presentation.VISUAL_NOVEL) {
                return true;
            }
        }
        return false;
    }
}
