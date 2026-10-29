package main;

import java.util.prefs.Preferences;

// Mushrooms are the currency. The ones picked up during a run are only safe once the run ends:
// finishing it banks all of them, dying keeps a quarter. The bank is saved between sessions.
public class MushroomWallet {
    public static final double KEPT_ON_DEATH = 0.25;

    private static final String BANK_KEY = "bankedMushrooms";

    private final Preferences prefs;
    private int banked;
    private int run = 0;

    public MushroomWallet() {
        this(Preferences.userNodeForPackage(MushroomWallet.class));
    }

    private MushroomWallet(Preferences prefs) {
        this.prefs = prefs;
        banked = prefs == null ? 0 : Math.max(0, prefs.getInt(BANK_KEY, 0));
    }

    // A wallet that starts empty and saves nothing (the demo).
    public static MushroomWallet unsaved() {
        return new MushroomWallet(null);
    }

    public int banked() {
        return banked;
    }

    public int run() {
        return run;
    }

    public void startRun() {
        run = 0;
    }

    // Spends banked mushrooms; returns false (and spends nothing) when the bank is short.
    public boolean spend(int amount) {
        if (amount < 0 || amount > banked) {
            return false;
        }
        banked -= amount;
        save();
        return true;
    }

    public void collect(int amount) {
        if (amount > 0) {
            run += amount;
        }
    }

    // Returns how many mushrooms went into the bank.
    public int bankRun() {
        return bank(run);
    }

    public int loseRun() {
        return bank((int) Math.floor(run * KEPT_ON_DEATH));
    }

    private int bank(int amount) {
        run = 0;
        if (amount > 0) {
            banked += amount;
            save();
        }
        return amount;
    }

    private void save() {
        if (prefs != null) {
            prefs.putInt(BANK_KEY, banked);
        }
    }
}
