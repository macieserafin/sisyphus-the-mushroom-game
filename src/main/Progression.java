package main;

import entity.Player;

import java.util.EnumSet;
import java.util.Properties;

// Permanent upgrades bought in the village. They survive deaths, runs and restarts of the game.
public class Progression {
    public static final int MAX_HEALTH_UPGRADES = 3;
    public static final int MAX_SHIELD_UPGRADES = 2;
    public static final int MAX_SPEED_ELIXIRS = 2;
    public static final double SPEED_PER_ELIXIR = 1.0;

    private static final String HEALTH_KEY = "prog.health";
    private static final String SHIELD_KEY = "prog.shield";
    private static final String SPEED_KEY = "prog.speed";
    private static final String SHOTGUN_KEY = "prog.shotgun";
    private static final String HEAVY_SWORD_KEY = "prog.heavySword";
    private static final String SKILLS_KEY = "prog.skills";

    // Loadout slots of the weapons sold in the village (see CombatLoadout).
    private static final int SHOTGUN_SLOT = 1;
    private static final int HEAVY_SWORD_SLOT = 1;

    private int health;
    private int shield;
    private int speed;
    private boolean shotgun;
    private boolean heavySword;
    private final EnumSet<Skill> skills = EnumSet.noneOf(Skill.class);
    private Runnable onChange;

    // Base stats, no skills. The save slot fills it in through load().
    public Progression() {
    }

    // Base stats, every skill known (the demo plays this way and teaches roll and dash).
    public static Progression unsaved() {
        Progression progression = new Progression();
        progression.skills.addAll(EnumSet.allOf(Skill.class));
        return progression;
    }

    public void setOnChange(Runnable onChange) {
        this.onChange = onChange;
    }

    public void save(Properties out) {
        out.setProperty(HEALTH_KEY, Integer.toString(health));
        out.setProperty(SHIELD_KEY, Integer.toString(shield));
        out.setProperty(SPEED_KEY, Integer.toString(speed));
        out.setProperty(SHOTGUN_KEY, Boolean.toString(shotgun));
        out.setProperty(HEAVY_SWORD_KEY, Boolean.toString(heavySword));
        StringBuilder known = new StringBuilder();
        for (Skill skill : skills) {
            known.append(known.length() == 0 ? "" : ",").append(skill.name());
        }
        out.setProperty(SKILLS_KEY, known.toString());
    }

    // Replaces everything with what the save holds (missing or broken values fall back to the base stats).
    public void load(Properties in) {
        health = count(in, HEALTH_KEY, MAX_HEALTH_UPGRADES);
        shield = count(in, SHIELD_KEY, MAX_SHIELD_UPGRADES);
        speed = count(in, SPEED_KEY, MAX_SPEED_ELIXIRS);
        shotgun = Boolean.parseBoolean(in.getProperty(SHOTGUN_KEY, "false").trim());
        heavySword = Boolean.parseBoolean(in.getProperty(HEAVY_SWORD_KEY, "false").trim());
        skills.clear();
        for (String name : in.getProperty(SKILLS_KEY, "").split(",")) {
            for (Skill skill : Skill.values()) {
                if (skill.name().equals(name.trim())) {
                    skills.add(skill);
                }
            }
        }
    }

    private static int count(Properties in, String key, int max) {
        try {
            return Math.max(0, Math.min(max, Integer.parseInt(in.getProperty(key, "0").trim())));
        } catch (NumberFormatException e) {
            return 0;
        }
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

    // Developer menu: forgets a skill together with every skill that grows from it.
    public void forget(Skill skill) {
        skills.remove(skill);
        boolean removed = true;
        while (removed) {
            removed = skills.removeIf(s -> s.requires != null && !skills.contains(s.requires));
        }
        save();
    }

    // Developer menu: takes back every bought upgrade and weapon.
    public void resetUpgrades(Player player) {
        player.addMaxHealth(-health);
        player.addMaxShield(-shield);
        player.addMaxSpeed(-bonusSpeed());
        if (shotgun) {
            player.setRangedSlotOwned(SHOTGUN_SLOT, false);
        }
        if (heavySword) {
            player.setMeleeSlotOwned(HEAVY_SWORD_SLOT, false);
        }
        health = shield = speed = 0;
        shotgun = heavySword = false;
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

    // Tells the save slot something changed; it writes the slot soon after.
    private void save() {
        if (onChange != null) {
            onChange.run();
        }
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
