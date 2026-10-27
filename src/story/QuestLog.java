package story;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Active quests shown in the HUD quest panel. */
public class QuestLog {

    public record Quest(String id, String title, String description) {
    }

    private final List<Quest> active = new ArrayList<>();

    public void add(String id, String title, String description) {
        if (!has(id)) {
            active.add(new Quest(id, title, description));
        }
    }

    public void complete(String id) {
        active.removeIf(quest -> quest.id().equals(id));
    }

    public boolean has(String id) {
        return active.stream().anyMatch(quest -> quest.id().equals(id));
    }

    public void clear() {
        active.clear();
    }

    public List<Quest> active() {
        return Collections.unmodifiableList(active);
    }
}
