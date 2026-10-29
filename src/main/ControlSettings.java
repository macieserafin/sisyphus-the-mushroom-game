package main;

import java.util.prefs.Preferences;

// Control options, saved between runs like the audio settings.
// Mode keys: TOGGLE = Q switches between melee and ranged; SEPARATE = Q selects melee, E selects ranged.
public class ControlSettings {
    public enum ModeKeys {
        TOGGLE("Q toggles"),
        SEPARATE("Q melee, E range");

        public final String label;

        ModeKeys(String label) {
            this.label = label;
        }

        public ModeKeys other() {
            return this == TOGGLE ? SEPARATE : TOGGLE;
        }
    }

    private static final String MODE_KEYS_KEY = "modeKeys";

    private final Preferences prefs = Preferences.userNodeForPackage(ControlSettings.class);
    private ModeKeys modeKeys = parse(prefs.get(MODE_KEYS_KEY, ModeKeys.TOGGLE.name()));

    public ModeKeys modeKeys() {
        return modeKeys;
    }

    public boolean separateModeKeys() {
        return modeKeys == ModeKeys.SEPARATE;
    }

    public void toggleModeKeys() {
        modeKeys = modeKeys.other();
        try {
            prefs.put(MODE_KEYS_KEY, modeKeys.name());
            prefs.flush();
        } catch (Exception e) {
            System.err.println("Could not save control settings: " + e.getMessage());
        }
    }

    private static ModeKeys parse(String value) {
        try {
            return ModeKeys.valueOf(value);
        } catch (IllegalArgumentException e) {
            return ModeKeys.TOGGLE;
        }
    }
}
