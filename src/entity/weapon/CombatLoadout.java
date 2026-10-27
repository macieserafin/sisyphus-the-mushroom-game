package entity.weapon;

public class CombatLoadout {
    private final MeleeWeapon[] meleeWeapons = {
            new Sword(),
            new HeavySword()
    };
    private final RangedWeapon[] rangedWeapons = {
            new Pistol(),
            new Shotgun()
    };

    // Weapons the player actually holds; the rest of the slots are locked until something grants them.
    private final boolean[] meleeOwned = {true, false};
    private final boolean[] rangedOwned = {true, false};

    private int meleeSlot = 0;
    private int rangedSlot = 0;

    public void resetOwnership() {
        for (int i = 0; i < meleeOwned.length; i++) {
            meleeOwned[i] = i == 0;
        }
        for (int i = 0; i < rangedOwned.length; i++) {
            rangedOwned[i] = i == 0;
        }
        meleeSlot = 0;
        rangedSlot = 0;
    }

    public boolean ownsMelee(int slot) {
        return meleeOwned[validSlot(slot, meleeWeapons.length)];
    }

    public boolean ownsRanged(int slot) {
        return rangedOwned[validSlot(slot, rangedWeapons.length)];
    }

    /** Taking away the weapon in hand falls back to slot 1, which is always kept. */
    public void setMeleeOwned(int slot, boolean owned) {
        meleeOwned[validSlot(slot, meleeWeapons.length)] = owned || slot == 0;
        if (!meleeOwned[meleeSlot]) {
            meleeSlot = 0;
        }
    }

    public void setRangedOwned(int slot, boolean owned) {
        rangedOwned[validSlot(slot, rangedWeapons.length)] = owned || slot == 0;
        if (!rangedOwned[rangedSlot]) {
            rangedSlot = 0;
        }
    }

    public MeleeWeapon selectMeleeSlot(int slot) {
        meleeSlot = validSlot(slot, meleeWeapons.length);
        return currentMeleeWeapon();
    }

    public RangedWeapon selectRangedSlot(int slot) {
        rangedSlot = validSlot(slot, rangedWeapons.length);
        return currentRangedWeapon();
    }

    public MeleeWeapon currentMeleeWeapon() {
        return meleeWeapons[meleeSlot];
    }

    public RangedWeapon currentRangedWeapon() {
        return rangedWeapons[rangedSlot];
    }

    public int currentMeleeSlot() {
        return meleeSlot;
    }

    public int currentRangedSlot() {
        return rangedSlot;
    }

    public int meleeWeaponCount() {
        return meleeWeapons.length;
    }

    public int rangedWeaponCount() {
        return rangedWeapons.length;
    }

    public String meleeWeaponName(int slot) {
        return meleeWeapons[validSlot(slot, meleeWeapons.length)].displayName();
    }

    public String rangedWeaponName(int slot) {
        return rangedWeapons[validSlot(slot, rangedWeapons.length)].displayName();
    }

    public String meleeSlotLabel(int slot) {
        return (slot + 1) + ": " + (ownsMelee(slot) ? meleeWeapons[slot].displayName() : "---");
    }

    public String rangedSlotLabel(int slot) {
        return (slot + 1) + ": " + (ownsRanged(slot) ? rangedWeapons[slot].displayName() : "---");
    }

    private int validSlot(int slot, int length) {
        if (slot < 0 || slot >= length) {
            throw new IllegalArgumentException("Weapon slot out of range: " + (slot + 1));
        }
        return slot;
    }
}
