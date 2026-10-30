package main;

import java.util.Properties;

// One wallet. The checkpoint owns rollback; travel and death never bank a fraction.
public class MushroomWallet {
    public static final double KEPT_ON_DEATH = 0;

    private static final String BANK_KEY = "wallet.banked";

    private int banked;

    private Runnable onChange;

    public static MushroomWallet unsaved() {
        return new MushroomWallet();
    }

    public void setOnChange(Runnable onChange) {
        this.onChange = onChange;
    }

    public void save(Properties out) {
        out.setProperty("wallet.amount", Integer.toString(banked));
    }

    public void load(Properties in) {
        banked = parse(in.getProperty("wallet.amount", in.getProperty(BANK_KEY)));

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
        return banked;
    }

    public void startRun() {
    }

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
            banked += amount;
            changed();
        }
    }

    public void debugAdjustBanked(int delta) {
        banked = Math.max(0, banked + delta);
        changed();
    }

    public void debugAdjustRun(int delta) {
        debugAdjustBanked(delta);
    }

    public int bankRun() { return 0; }
    public int loseRun() { return 0; }

    private void changed() {
        if (onChange != null) {
            onChange.run();
        }
    }
}
