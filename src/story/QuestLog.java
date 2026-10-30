package story;

import main.GamePanel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

// All quests of the game. The main story quests unlock one after another; side quests open once the player
// reaches the village. Progress comes from game events (kills, chests, doors) or is read from the game state
// every frame (story flags, banked mushrooms, purchases), so debug teleports and saved data stay in sync.
public class QuestLog {
    public enum State { HIDDEN, ACTIVE, DONE }

    public static final class Quest {
        public final String id;
        public final String title;
        public final String description;
        public final String objective;
        public final boolean main;
        public final int target;
        private final String next;
        private int progress;
        private State state = State.HIDDEN;

        Quest(String id, boolean main, String title, String description, String objective, int target, String next) {
            this.id = id;
            this.main = main;
            this.title = title;
            this.description = description;
            this.objective = objective;
            this.target = target;
            this.next = next;
        }

        public int progress() {
            return progress;
        }

        public State state() {
            return state;
        }

        public boolean counted() {
            return target > 1;
        }
    }

    public static final String WAKE_UP = "wake_up";
    public static final String BYT = "byt";
    public static final String VILLAGE = "village";
    public static final String FOREST = "forest";
    public static final String KING = "king";
    public static final String HOME = "home";
    public static final String HOARD = "hoard";
    public static final String SHOPPING = "shopping";
    public static final String GROTTO = "grotto";
    public static final String PESTS = "pests";
    public static final String CHESTS = "chests";

    private static final int HOUSES = 5;

    private final Map<String, Quest> quests = new LinkedHashMap<>();
    private final List<Quest> completed = new ArrayList<>();
    private final Set<String> visitedHouses = new HashSet<>();
    private final List<String> notices = new ArrayList<>();

    public QuestLog() {
        reset();
    }

    public void reset() {
        quests.clear();
        completed.clear();
        visitedHouses.clear();
        notices.clear();
        main(WAKE_UP, "Legs, Then Lead",
                "You woke in an impossible forest. Something called BYT is watching, and it wants to see you move.",
                "Finish BYT's lesson: walk, shoot, take the sword and clear the slimes.", 1, BYT);
        main(BYT, "The Voice",
                "BYT has watched long enough. Now it wants a closer look.",
                "Face BYT.", 1, VILLAGE);
        main(VILLAGE, "A Place to Wake Up",
                "The village takes in whoever the forest spits out. Its people know more than they say.",
                "Visit the five houses of the village.", HOUSES, FOREST);
        main(FOREST, "Through the Gate",
                "Past the village gate the forest goes on for days: ravines, a maze of trees, a river.",
                "Cross the forest and find the cave at its far end.", 1, KING);
        main(KING, "The Slime King",
                "Something wears a crown in that cave, sitting on everyone else's gold.",
                "Defeat the Slime King.", 1, HOME);
        main(HOME, "Back Alive",
                "Mushrooms are only yours once you carry them home.",
                "Leave the cave through the portal and bank what you found.", 1, null);
        side(HOARD, "Mushroom Hoard",
                "The shops take mushrooms and nothing else.",
                "Have 100 mushrooms in the bank.", 100);
        side(SHOPPING, "Something for the Road",
                "The merchant, the witch, the smith and the old sage all sell something.",
                "Buy an upgrade or a weapon in the village.", 1);
        side(GROTTO, "The Locked Grotto",
                "A door in the trees by the river, and a key lost somewhere in the forest maze.",
                "Find the key and open the grotto.", 1);
        side(PESTS, "Pest Control",
                "The forest breeds faster than it dies. Thin it out.",
                "Defeat 100 creatures in the forest.", 100);
        side(CHESTS, "Treasure Hunter",
                "Old chests lie forgotten in the far corners of the forest.",
                "Open 3 chests in the forest.", 3);
        start(WAKE_UP);
        notices.clear();
    }

    public void clear() {
        quests.clear();
        completed.clear();
        visitedHouses.clear();
        notices.clear();
    }

    public void add(String id, String title, String objective) {
        if (!quests.containsKey(id)) {
            Quest quest = new Quest(id, true, title, objective, objective, 1, null);
            quest.state = State.ACTIVE;
            quests.put(id, quest);
        }
    }

    private void main(String id, String title, String description, String objective, int target, String next) {
        quests.put(id, new Quest(id, true, title, description, objective, target, next));
    }

    private void side(String id, String title, String description, String objective, int target) {
        quests.put(id, new Quest(id, false, title, description, objective, target, null));
    }


    public void start(String id) {
        Quest quest = quests.get(id);
        if (quest != null && quest.state == State.HIDDEN) {
            quest.state = State.ACTIVE;
            notices.add("New quest: " + quest.title);
            if (quest.progress >= quest.target) {
                finish(quest);
            }
        }
    }

    // Sets progress; completes the quest when it reaches the target. Hidden quests remember progress for later.
    public void setProgress(String id, int value) {
        Quest quest = quests.get(id);
        if (quest == null || quest.state == State.DONE) {
            return;
        }
        quest.progress = Math.max(0, Math.min(quest.target, value));
        if (quest.progress >= quest.target && quest.state == State.ACTIVE) {
            finish(quest);
        }
    }

    public void advance(String id, int amount) {
        Quest quest = quests.get(id);
        if (quest != null) {
            setProgress(id, quest.progress + amount);
        }
    }

    public void complete(String id) {
        Quest quest = quests.get(id);
        if (quest == null || quest.state == State.DONE) {
            return;
        }
        // finishing a later story step also closes the steps before it (e.g. after a debug teleport)
        for (Quest earlier : quests.values()) {
            if (earlier == quest) {
                break;
            }
            if (earlier.main && quest.main && earlier.state != State.DONE) {
                earlier.state = State.ACTIVE;
                earlier.progress = earlier.target;
                finish(earlier);
            }
        }
        quest.state = State.ACTIVE;
        quest.progress = quest.target;
        finish(quest);
    }

    private void finish(Quest quest) {
        quest.state = State.DONE;
        completed.add(0, quest);
        notices.add("Quest complete: " + quest.title);
        if (quest.next != null) {
            start(quest.next);
        }
        if (quest.id.equals(BYT)) {
            notices.add("New side quests in the journal [J]");
            for (Quest side : quests.values()) {
                if (!side.main) {
                    side.state = State.ACTIVE;
                    if (side.progress >= side.target) {
                        finish(side);
                    }
                }
            }
        }
    }

    public void visitHouse(String house) {
        visitedHouses.add(house);
        setProgress(VILLAGE, visitedHouses.size());
    }


    public void save(Properties out) {
        for (Quest quest : quests.values()) {
            out.setProperty("quest." + quest.id, quest.state.name() + "," + quest.progress);
        }
        StringBuilder done = new StringBuilder();
        for (Quest quest : completed) {
            done.append(done.length() == 0 ? "" : ",").append(quest.id);
        }
        out.setProperty("quests.completed", done.toString());
        out.setProperty("quests.houses", String.join(",", new java.util.TreeSet<>(visitedHouses)));
    }

    public void load(Properties in) {
        reset();
        for (Quest quest : quests.values()) {
            String[] saved = in.getProperty("quest." + quest.id, "").split(",");
            if (saved.length != 2) {
                continue;
            }
            try {
                quest.state = State.valueOf(saved[0].trim());
                quest.progress = Math.max(0, Math.min(quest.target, Integer.parseInt(saved[1].trim())));
            } catch (IllegalArgumentException e) {
            }
        }
        completed.clear();
        for (String id : in.getProperty("quests.completed", "").split(",")) {
            Quest quest = quests.get(id.trim());
            if (quest != null && quest.state == State.DONE && !completed.contains(quest)) {
                completed.add(quest);
            }
        }
        visitedHouses.clear();
        for (String house : in.getProperty("quests.houses", "").split(",")) {
            if (!house.isBlank()) {
                visitedHouses.add(house.trim());
            }
        }
        notices.clear();
    }

    // Messages about started and finished quests since the last call, oldest first.
    public List<String> drainNotices() {
        List<String> out = new ArrayList<>(notices);
        notices.clear();
        return out;
    }

    public void sync(GamePanel gp) {
        if (gp.storyFlags.isPrologueTutorialCompleted()) complete(WAKE_UP);
        if (gp.storyFlags.isPrologueBytEncounterCompleted()) complete(BYT);
        setProgress(HOARD, gp.wallet.banked());
        int bought = gp.progression.healthUpgrades() + gp.progression.shieldUpgrades() + gp.progression.speedElixirs()
                + (gp.progression.ownsShotgun() ? 1 : 0) + (gp.progression.ownsHeavySword() ? 1 : 0);
        setProgress(SHOPPING, bought);
    }


    public Quest get(String id) {
        return quests.get(id);
    }

    public boolean has(String id) {
        Quest quest = quests.get(id);
        return quest != null && quest.state == State.ACTIVE;
    }

    public List<Quest> active() {
        List<Quest> out = new ArrayList<>();
        for (Quest quest : quests.values()) {
            if (quest.state == State.ACTIVE) {
                out.add(quest);
            }
        }
        return Collections.unmodifiableList(out);
    }

    public List<Quest> completed() {
        return Collections.unmodifiableList(completed);
    }
}
