package main;

import save.SaveStore;
import save.Settings;
import story.QuestLog;
import story.StoryFlags;

import java.util.Properties;
import java.util.prefs.Preferences;

// Slot storage. Campaign progress is committed only at explicit checkpoint boundaries.
public class GameSaves {
    public record Summary(boolean exists, long playSeconds, long savedAt, int banked, String stage, int upgrades,
                          int skills) {
        static final Summary EMPTY = new Summary(false, 0, 0, 0, "", 0, 0);
    }

    public static final int SLOTS = SaveStore.SLOTS;

    private static final int VERSION = 2;
    private static final int TICKS_PER_SECOND = 60;
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
    private Properties lastFull = new Properties();

    public GameSaves(GamePanel gp) {
        this.gp = gp;
        this.store = gp.store;
        this.settings = gp.settings;
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
                data.containsKey("wallet.amount") ? number(data, "wallet.amount") : number(data, "wallet.banked"), data.getProperty(STAGE_KEY, ""), upgrades,
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
        checkpoint();
    }

    public boolean load(int slot) {
        Properties data = store.read(file(slot));
        if (data == null) {
            return false;
        }
        apply(data);
        playTicks = (long) number(data, PLAY_SECONDS_KEY) * TICKS_PER_SECOND;
        activate(slot);
        lastFull = data;
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
        active = 0;
        apply(new Properties());
    }

    private void activate(int slot) {
        active = slot;
        settings.putInt(LAST_SLOT_KEY, slot);
    }

    private void apply(Properties data) {
        gp.storyFlags = new StoryFlags();
        gp.storyFlags.load(data);
        gp.wallet.load(data);
        gp.progression.load(data);
        gp.questLog.load(data);
        gp.relics.load(data);
        gp.campaign.load(data);
    }


    public void tick() {
        if (active > 0 && gp.gameState == GameState.PLAYING) playTicks++;
    }

    public Properties snapshot() { return compose(); }

    public synchronized boolean checkpoint() {
        if (gp.campaign.inCombat() || !gp.player.alive) return false;
        Properties data = compose();
        return writeCheckpoint(data);
    }

    public synchronized boolean checkpointAt(String map, int col, int row) {
        if (gp.campaign.inCombat() || !gp.player.alive) return false;
        Properties data = compose();
        data.setProperty("campaign.map", map);
        data.setProperty("player.x", Integer.toString(col * gp.tileSize));
        data.setProperty("player.y", Integer.toString(row * gp.tileSize));
        return writeCheckpoint(data);
    }

    private boolean writeCheckpoint(Properties data) {
        if (active > 0 && !store.write(file(active), data)) {
            gp.ui.showMessage("Pattern could not be saved. Stay here and try again");
            return false;
        }
        lastFull = data;
        return true;
    }

    public synchronized boolean saveNow() {
        return !gp.campaign.started() && checkpoint();
    }

    public boolean saveManually() { return saveNow(); }

    public void resumeWorld() { gp.campaign.restoreLocation(lastFull, false); }

    public void restoreSnapshot(Properties data) {
        apply(data);
        gp.campaign.restoreLocation(data, false);
    }

    public synchronized boolean respawn() {
        Properties data = active > 0 ? store.read(file(active)) : lastFull;
        if (data == null || !data.containsKey("campaign.version")) {
            gp.ui.showMessage("No bound pattern found. Continue from the title screen");
            return false;
        }
        apply(data);
        lastFull = data;
        gp.campaign.restoreLocation(data, true);
        return true;
    }

    public synchronized void abandon() {
        active = 0;
        apply(new Properties());
    }

    private Properties compose() {
        Properties data = new Properties();
        gp.storyFlags.save(data);
        gp.wallet.save(data);
        gp.progression.save(data);
        gp.questLog.save(data);
        gp.relics.save(data);
        gp.campaign.save(data);
        gp.player.saveCheckpoint(data);
        data.setProperty(VERSION_KEY, Integer.toString(VERSION));
        data.setProperty(STAGE_KEY, stage());
        data.setProperty(PLAY_SECONDS_KEY, Long.toString(playTicks / TICKS_PER_SECOND));
        data.setProperty(SAVED_AT_KEY, Long.toString(System.currentTimeMillis()));
        return data;
    }

    private String stage() {
        if (gp.campaign.started()) return "Floor " + gp.campaign.floor() + (gp.campaign.bossCleared() ? " - boss cleared" : " - exploring");
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
