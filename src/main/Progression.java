package main;

import entity.Player;

import java.util.EnumSet;
import java.util.prefs.Preferences;

// Permanent upgrades bought in the village. They survive deaths, runs and restarts of the game.
public class Progression {
    public static final int MAX_HEALTH_UPGRADES = 3;
    public static final int MAX_SHIELD_UPGRADES = 2;
    public static final int MAX_SPEED_ELIXIRS = 2;
    public static final double SPEED_PER_ELIXIR = 1.0;

    private static final String HEALTH_KEY = "upgradeHealth";
    private static final String SHIELD_KEY = "upgradeShield";
    private static final String SPEED_KEY = "upgradeSpeed";
    private static final String SHOTGUN_KEY = "ownsShotgun";
    private static final String HEAVY_SWORD_KEY = "ownsHeavySword";
    private static final String SKILLS_KEY = "skills";

    // Loadout slots of the weapons sold in the village (see CombatLoadout).
    private static final int SHOTGUN_SLOT = 1;
    private static final int HEAVY_SWORD_SLOT = 1;

    private final Preferences prefs;
    private int health;
    private int shield;
    private int speed;
    private boolean shotgun;
    private boolean heavySword;
    private final EnumSet<Skill> skills = EnumSet.noneOf(Skill.class);

    public Progression() {
        this(Preferences.userNodeForPackage(Progression.class));
    }

    private Progression(Preferences prefs) {
        this.prefs = prefs;
        if (prefs != null) {
            health = prefs.getInt(HEALTH_KEY, 0);
            shield = prefs.getInt(SHIELD_KEY, 0);
            speed = prefs.getInt(SPEED_KEY, 0);
            shotgun = prefs.getBoolean(SHOTGUN_KEY, false);
            heavySword = prefs.getBoolean(HEAVY_SWORD_KEY, false);
            for (String name : prefs.get(SKILLS_KEY, "").split(",")) {
                for (Skill skill : Skill.values()) {
                    if (skill.name().equals(name)) {
                        skills.add(skill);
                    }
                }
            }
        }
    }

    // Base stats, nothing saved, every skill known (the demo plays this way and teaches roll and dash).
    public static Progression unsaved() {
        Progression progression = new Progression(null);
        progression.skills.addAll(EnumSet.allOf(Skill.class));
        return progression;
    }

    public boolean hasSkill(Skill skill) {
        return skills.contains(skill);
    }

    // A skill can be learned when it is new and the skill it grows from is known.
    public boolean canLearn(Skill skill) {
        return !hasSkill(skill) && (skill.requires == null || hasSkill(skill.requires));
    }

    public void learn(Skill skill) {
        skills.add(skill);
        save();
    }

    public int healthUpgrades() {
        return health;
    }

    public int shieldUpgrades() {
        return shield;
    }

    public int speedElixirs() {
        return speed;
    }

    public double bonusSpeed() {
        return speed * SPEED_PER_ELIXIR;
    }

    public boolean ownsShotgun() {
        return shotgun;
    }

    public boolean ownsHeavySword() {
        return heavySword;
    }

    public void buyHealth(Player player) {
        health++;
        save();
        player.addMaxHealth(1);
    }

    public void buyShield(Player player) {
        shield++;
        save();
        player.addMaxShield(1);
    }

    public void buySpeedElixir(Player player) {
        speed++;
        save();
        player.addMaxSpeed(SPEED_PER_ELIXIR);
    }

    public void buyShotgun(Player player) {
        shotgun = true;
        save();
        applyWeapons(player);
    }

    public void buyHeavySword(Player player) {
        heavySword = true;
        save();
        applyWeapons(player);
    }

    private void save() {
        if (prefs == null) {
            return;
        }
        prefs.putInt(HEALTH_KEY, health);
        prefs.putInt(SHIELD_KEY, shield);
        prefs.putInt(SPEED_KEY, speed);
        prefs.putBoolean(SHOTGUN_KEY, shotgun);
        prefs.putBoolean(HEAVY_SWORD_KEY, heavySword);
        StringBuilder known = new StringBuilder();
        for (Skill skill : skills) {
            known.append(known.length() == 0 ? "" : ",").append(skill.name());
        }
        prefs.put(SKILLS_KEY, known.toString());
    }

    // The prologue resets the loadout, so bought weapons are handed back whenever the village loads.
    public void applyWeapons(Player player) {
        if (ownsShotgun() && !player.ownsRangedSlot(SHOTGUN_SLOT)) {
            player.setRangedSlotOwned(SHOTGUN_SLOT, true);
        }
        if (ownsHeavySword() && !player.ownsMeleeSlot(HEAVY_SWORD_SLOT)) {
            player.setMeleeSlotOwned(HEAVY_SWORD_SLOT, true);
        }
    }
}
