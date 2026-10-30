package save;

import java.util.Properties;
import java.util.prefs.Preferences;

// Options shared by every save slot (display, audio, controls, comfort). Each change is written to settings.properties
// straight away. The first launch on a disk store imports whatever the older versions kept in the system preferences.
public final class Settings {
    public static final String FILE = "settings.properties";
    public static final String LEGACY_IMPORTED_KEY = "legacyImported";

    private static final String[] LEGACY_KEYS = {"displayMode", "musicVolume", "effectsVolume", "muted", "modeKeys"};

    private final SaveStore store;
    private final Properties data;
    private final boolean fresh;

    private Settings(SaveStore store, Properties data, boolean fresh) {
        this.store = store;
        this.data = data;
        this.fresh = fresh;
    }

    public static Settings load(SaveStore store) {
        Properties data = store.read(FILE);
        Settings settings = new Settings(store, data == null ? new Properties() : data, data == null);
        if (settings.fresh && store.isPersistent()) {
            settings.importLegacy(Preferences.userRoot().node("main"));
        }
        return settings;
    }

    // Copies the settings the previous versions saved in the system preferences (once).
    public void importLegacy(Preferences prefs) {
        if (getBoolean(LEGACY_IMPORTED_KEY, false)) {
            return;
        }
        for (String key : LEGACY_KEYS) {
            String value = prefs.get(key, null);
            if (value != null) {
                data.setProperty(key, value);
            }
        }
        data.setProperty(LEGACY_IMPORTED_KEY, "true");
        flush();
    }

    public SaveStore store() {
        return store;
    }

    public boolean isFresh() {
        return fresh;
    }

    public String getString(String key, String fallback) {
        return data.getProperty(key, fallback);
    }

    public int getInt(String key, int fallback) {
        try {
            return Integer.parseInt(data.getProperty(key, "").trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public boolean getBoolean(String key, boolean fallback) {
        String value = data.getProperty(key);
        return value == null ? fallback : Boolean.parseBoolean(value.trim());
    }

    public void put(String key, String value) {
        data.setProperty(key, value);
        flush();
    }

    public void putInt(String key, int value) {
        put(key, Integer.toString(value));
    }

    public void putBoolean(String key, boolean value) {
        put(key, Boolean.toString(value));
    }

    public void remove(String... keys) {
        for (String key : keys) {
            data.remove(key);
        }
        flush();
    }

    // Every key with the given prefix, for bulk resets.
    public void removePrefix(String prefix) {
        data.stringPropertyNames().stream().filter(k -> k.startsWith(prefix)).forEach(data::remove);
        flush();
    }

    private void flush() {
        store.write(FILE, data);
    }
}
