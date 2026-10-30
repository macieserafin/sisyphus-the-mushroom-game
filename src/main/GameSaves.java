package main;

import save.SaveStore;
import save.Settings;
import story.QuestLog;
import story.StoryFlags;

import java.util.Properties;
import java.util.prefs.Preferences;

// The three save slots. A slot holds the story flags, the quest log, the mushroom bank and everything bought in the
// village. The game always resumes in the village: a run through the forest is never saved half way.
// While a slot is active the game writes it by itself: right after the bank, purchases or skills change, and
// every few seconds when the story or quests moved. Without an active slot (title screen, demo, tests) it saves nothing.
public class GameSaves {
    public record Summary(boolean exists, long playSeconds, long savedAt, int banked, String stage, int upgrades,
                          int skills) {
        static final Summary EMPTY = new Summary(false, 0, 0, 0, "", 0, 0);
    }

    public static final int SLOTS = SaveStore.SLOTS;

    private static final int VERSION = 1;
    private static final int TICKS_PER_SECOND = 60;
    private static final int CHECK_TICKS = 300;
    private static final String LAST_SLOT_KEY = "lastSlot";
    private static final String LEGACY_PROFILE_KEY = "legacyProfileImported";
    private static final String STAGE_KEY = "meta.stage";
    private static final String SAVED_AT_KEY = "meta.savedAt";
    private static final String PLAY_SECONDS_KEY = "meta.playSeconds";
    private static final String VERSION_KEY = "meta.version";

    private final GamePanel gp;
    private final SaveStore store;
    private final Settings settings;
    private int active;
    private long playTicks;
    private volatile boolean dirty;
    private int sinceCheck;
    private Properties lastWritten = new Properties();
    private Properties lastFull = new Properties();
    // What "Quit without saving" goes back to: the slot as it was loaded or last saved by hand (null = a brand new slot).
    private Properties restorePoint;

    public GameSaves(GamePanel gp) {
        this.gp = gp;
        this.store = gp.store;
        this.settings = gp.settings;
        gp.wallet.setOnChange(this::markDirty);
        gp.progression.setOnChange(this::markDirty);
        if (store.isPersistent() && !settings.getBoolean(LEGACY_PROFILE_KEY, false)) {
            importLegacyProfile(Preferences.userRoot().node("main"));
            settings.putBoolean(LEGACY_PROFILE_KEY, true);
        }
    }

    static String file(int slot) {
        return "slot" + slot + ".sav";
    }


    public Summary summary(int slot) {
        if (slot < 1 || slot > SLOTS) {
            return Summary.EMPTY;
        }
        Properties data = store.read(file(slot));
        if (data == null) {
            return Summary.EMPTY;
        }
        int upgrades = number(data, "prog.health") + number(data, "prog.shield") + number(data, "prog.speed")
                + (Boolean.parseBoolean(data.getProperty("prog.shotgun", "false")) ? 1 : 0)
                + (Boolean.parseBoolean(data.getProperty("prog.heavySword", "false")) ? 1 : 0);
        String skills = data.getProperty("prog.skills", "");
        return new Summary(true, number(data, PLAY_SECONDS_KEY), longNumber(data, SAVED_AT_KEY),
                number(data, "wallet.banked"), data.getProperty(STAGE_KEY, ""), upgrades,
                skills.isBlank() ? 0 : skills.split(",").length);
    }

    public boolean hasSave(int slot) {
        return summary(slot).exists();
    }

    // The slot "Continue" opens: the one played last, or else the most recently saved one. 0 when there is none.
    public int continueSlot() {
        int last = settings.getInt(LAST_SLOT_KEY, 0);
        if (hasSave(last)) {
            return last;
        }
        int best = 0;
        long bestTime = -1;
        for (int slot = 1; slot <= SLOTS; slot++) {
            Summary summary = summary(slot);
            if (summary.exists() && summary.savedAt() > bestTime) {
                best = slot;
                bestTime = summary.savedAt();
            }
        }
        return best;
    }

    public void startNew(int slot) {
        store.delete(file(slot));
        apply(new Properties());
        playTicks = 0;
        activate(slot);
        restorePoint = null;
        saveNow();
    }

    public boolean load(int slot) {
        Properties data = store.read(file(slot));
        if (data == null) {
            return false;
        }
        apply(data);
        playTicks = (long) number(data, PLAY_SECONDS_KEY) * TICKS_PER_SECOND;
        activate(slot);
        restorePoint = data;
        return true;
    }

    public void delete(int slot) {
        store.delete(file(slot));
        if (active == slot) {
            active = 0;
        }
        if (settings.getInt(LAST_SLOT_KEY, 0) == slot) {
            settings.remove(LAST_SLOT_KEY);
        }
    }

    public boolean isActive() {
        return active > 0;
    }

    public int activeSlot() {
        return active;
    }

    public void deactivate() {
        if (active == 0) {
            return;
        }
        saveNow();
        active = 0;
        apply(new Properties());
    }

    private void activate(int slot) {
        active = slot;
        dirty = false;
        sinceCheck = 0;
        settings.putInt(LAST_SLOT_KEY, slot);
    }

    private void apply(Properties data) {
        gp.storyFlags = new StoryFlags();
        gp.storyFlags.load(data);
        gp.wallet.load(data);
        gp.progression.load(data);
        gp.questLog.load(data);
        lastWritten = content(data);
    }


    private void markDirty() {
        if (active > 0) {
            dirty = true;
        }
    }

    public void tick() {
        if (active == 0) {
            return;
        }
        if (gp.gameState != GameState.MENU && gp.gameState != GameState.PAUSED) {
            playTicks++;
        }
        if (dirty || ++sinceCheck >= CHECK_TICKS) {
            sinceCheck = 0;
            if (dirty || !content(compose()).equals(lastWritten)) {
                saveNow();
            }
        }
    }

    public synchronized boolean saveNow() {
        if (active == 0) {
            return false;
        }
        Properties data = compose();
        boolean written = store.write(file(active), data);
        if (written) {
            lastWritten = content(data);
            lastFull = data;
            dirty = false;
        }
        return written;
    }

    // The player's own "Save": writes the slot and makes this the point "Quit without saving" returns to.
    public synchronized boolean saveManually() {
        boolean written = saveNow();
        if (written) {
            restorePoint = lastFull;
        }
        return written;
    }

    // "Quit without saving": the slot goes back to how it was when it was loaded or last saved by hand (the automatic
    // saves in between are undone; a slot that was new this session becomes empty again), then the slot is let go of.
    public synchronized void abandon() {
        if (active == 0) {
            return;
        }
        if (restorePoint == null) {
            store.delete(file(active));
        } else {
            store.write(file(active), restorePoint);
        }
        active = 0;
        apply(new Properties());
    }

    private Properties compose() {
        Properties data = new Properties();
        gp.storyFlags.save(data);
        gp.wallet.save(data);
        gp.progression.save(data);
        gp.questLog.save(data);
        data.setProperty(VERSION_KEY, Integer.toString(VERSION));
        data.setProperty(STAGE_KEY, stage());
        data.setProperty(PLAY_SECONDS_KEY, Long.toString(playTicks / TICKS_PER_SECOND));
        data.setProperty(SAVED_AT_KEY, Long.toString(System.currentTimeMillis()));
        return data;
    }

    private String stage() {
        if (!gp.storyFlags.isPrologueBytEncounterCompleted()) {
            return "Prologue";
        }
        for (QuestLog.Quest quest : gp.questLog.active()) {
            if (quest.main) {
                return quest.title;
            }
        }
        return "The way home";
    }

    // The saved values without the ones that change every time (used to see whether anything moved).
    private static Properties content(Properties data) {
        Properties copy = new Properties();
        copy.putAll(data);
        copy.remove(SAVED_AT_KEY);
        copy.remove(PLAY_SECONDS_KEY);
        return copy;
    }


    // Earlier versions kept one global bank, upgrades and skills in the system preferences. They become slot 1, with
    // the prologue marked as done (these players had already bought things in the village).
    void importLegacyProfile(Preferences prefs) {
        if (hasSave(1)) {
            return;
        }
        Properties data = legacyProfile(prefs);
        if (data != null) {
            store.write(file(1), data);
        }
    }

    static Properties legacyProfile(Preferences prefs) {
        String[][] keys = {{"bankedMushrooms", "wallet.banked"}, {"upgradeHealth", "prog.health"},
                {"upgradeShield", "prog.shield"}, {"upgradeSpeed", "prog.speed"}, {"ownsShotgun", "prog.shotgun"},
                {"ownsHeavySword", "prog.heavySword"}, {"skills", "prog.skills"}};
        Properties data = new Properties();
        boolean any = false;
        for (String[] key : keys) {
            String value = prefs.get(key[0], null);
            if (value != null) {
                data.setProperty(key[1], value);
                any = true;
            }
        }
        if (!any) {
            return null;
        }
        data.setProperty("story.intro", "true");
        data.setProperty("story.tutorial", "true");
        data.setProperty("story.bytStarted", "true");
        data.setProperty("story.bytCompleted", "true");
        data.setProperty(VERSION_KEY, Integer.toString(VERSION));
        data.setProperty(STAGE_KEY, "A Place to Wake Up");
        data.setProperty(SAVED_AT_KEY, Long.toString(System.currentTimeMillis()));
        return data;
    }

    private static int number(Properties data, String key) {
        return (int) Math.max(0, Math.min(Integer.MAX_VALUE, longNumber(data, key)));
    }

    private static long longNumber(Properties data, String key) {
        try {
            return Long.parseLong(data.getProperty(key, "0").trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
