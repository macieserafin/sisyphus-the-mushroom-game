package main;

import java.util.Properties;

// Mushrooms are the currency. The ones picked up during a run are only safe once the run ends:
// finishing it banks all of them, dying keeps a quarter. The bank belongs to the save slot: GameSaves reads and
// writes it, and the change listener tells it when the bank moved.
public class MushroomWallet {
    public static final double KEPT_ON_DEATH = 0.25;

    private static final String BANK_KEY = "wallet.banked";

    private int banked;
    private int run = 0;
    private Runnable onChange;

    // An empty wallet that is not tied to any save (the demo uses it as is).
    public static MushroomWallet unsaved() {
        return new MushroomWallet();
    }

    public void setOnChange(Runnable onChange) {
        this.onChange = onChange;
    }

    public void save(Properties out) {
        out.setProperty(BANK_KEY, Integer.toString(banked));
    }

    public void load(Properties in) {
        banked = parse(in.getProperty(BANK_KEY));
        run = 0;
    }

    private static int parse(String value) {
        try {
            return Math.max(0, Integer.parseInt(value.trim()));
        } catch (RuntimeException e) {
            return 0;
        }
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
        changed();
        return true;
    }

    public void collect(int amount) {
        if (amount > 0) {
            run += amount;
        }
    }

    // Developer menu: adds (or takes, when negative) mushrooms straight to the bank or the current run.
    public void debugAdjustBanked(int delta) {
        banked = Math.max(0, banked + delta);
        changed();
    }

    public void debugAdjustRun(int delta) {
        run = Math.max(0, run + delta);
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
            changed();
        }
        return amount;
    }

    private void changed() {
        if (onChange != null) {
            onChange.run();
        }
    }
}
