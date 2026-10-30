package main;

import save.Settings;

import java.awt.event.KeyEvent;
import java.util.EnumMap;
import java.util.Map;

// Control options, saved in the settings file.
// Mode keys: TOGGLE = Q switches between melee and ranged; SEPARATE = Q selects melee, E selects ranged.
// Key bindings: the gameplay keys can be rebound; menus keep W/S/A/D, the arrows, ENTER and ESC.
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

    public enum Action {
        MOVE_UP("Move up", KeyEvent.VK_W),
        MOVE_DOWN("Move down", KeyEvent.VK_S),
        MOVE_LEFT("Move left", KeyEvent.VK_A),
        MOVE_RIGHT("Move right", KeyEvent.VK_D),
        AIM_UP("Attack up", KeyEvent.VK_UP),
        AIM_DOWN("Attack down", KeyEvent.VK_DOWN),
        AIM_LEFT("Attack left", KeyEvent.VK_LEFT),
        AIM_RIGHT("Attack right", KeyEvent.VK_RIGHT),
        MANEUVER("Roll / dash", KeyEvent.VK_SPACE),
        SPRINT("Sprint", KeyEvent.VK_SHIFT),
        MODE_A("Weapon mode (Q)", KeyEvent.VK_Q),
        MODE_B("Weapon mode (E)", KeyEvent.VK_E),
        JOURNAL("Quest journal", KeyEvent.VK_J);

        public final String label;
        public final int defaultKey;

        Action(String label, int defaultKey) {
            this.label = label;
            this.defaultKey = defaultKey;
        }
    }

    private static final String MODE_KEYS_KEY = "modeKeys";
    private static final String BINDING_PREFIX = "key.";

    private final Settings settings;
    private final Map<Action, Integer> bindings = new EnumMap<>(Action.class);
    private ModeKeys modeKeys;

    public ControlSettings(Settings settings) {
        this.settings = settings;
        modeKeys = parse(settings.getString(MODE_KEYS_KEY, ModeKeys.TOGGLE.name()));
        loadBindings();
    }

    public ModeKeys modeKeys() {
        return modeKeys;
    }

    public boolean separateModeKeys() {
        return modeKeys == ModeKeys.SEPARATE;
    }

    public void toggleModeKeys() {
        modeKeys = modeKeys.other();
        settings.put(MODE_KEYS_KEY, modeKeys.name());
    }

    // ------------------------------------------------------------------ key bindings

    public int key(Action action) {
        return bindings.get(action);
    }

    public String keyName(Action action) {
        return KeyEvent.getKeyText(key(action)).toUpperCase();
    }

    // Keys with a fixed job: ESC, ENTER, TAB, M (mute), 1 and 2 (weapon slots) and the function keys.
    public static boolean isReserved(int code) {
        return code == KeyEvent.VK_ESCAPE || code == KeyEvent.VK_ENTER || code == KeyEvent.VK_TAB
                || code == KeyEvent.VK_M || code == KeyEvent.VK_1 || code == KeyEvent.VK_2
                || (code >= KeyEvent.VK_F1 && code <= KeyEvent.VK_F12) || code == KeyEvent.VK_UNDEFINED;
    }

    // Binds the key; an action that already used it gets this action's old key (a swap), so no key does two things.
    // Returns false for a reserved key.
    public boolean rebind(Action action, int code) {
        if (isReserved(code)) {
            return false;
        }
        int old = key(action);
        for (Action other : Action.values()) {
            if (other != action && key(other) == code) {
                bindings.put(other, old);
                settings.putInt(BINDING_PREFIX + other.name(), old);
            }
        }
        bindings.put(action, code);
        settings.putInt(BINDING_PREFIX + action.name(), code);
        return true;
    }

    public void resetBindings() {
        for (Action action : Action.values()) {
            bindings.put(action, action.defaultKey);
        }
        settings.removePrefix(BINDING_PREFIX);
    }

    public void resetAll() {
        resetBindings();
        modeKeys = ModeKeys.TOGGLE;
        settings.put(MODE_KEYS_KEY, modeKeys.name());
    }

    private void loadBindings() {
        for (Action action : Action.values()) {
            bindings.put(action, settings.getInt(BINDING_PREFIX + action.name(), action.defaultKey));
        }
        // a hand-edited or damaged file must not leave two actions on one key or a reserved key in use
        boolean valid = true;
        java.util.Set<Integer> used = new java.util.HashSet<>();
        for (Action action : Action.values()) {
            int code = key(action);
            if (isReserved(code) || !used.add(code)) {
                valid = false;
            }
        }
        if (!valid) {
            for (Action action : Action.values()) {
                bindings.put(action, action.defaultKey);
            }
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
