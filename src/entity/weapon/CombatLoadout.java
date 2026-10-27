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

    private int meleeSlot = 0;
    private int rangedSlot = 0;

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
        return (slot + 1) + ": " + meleeWeapons[validSlot(slot, meleeWeapons.length)].displayName();
    }

    public String rangedSlotLabel(int slot) {
        return (slot + 1) + ": " + rangedWeapons[validSlot(slot, rangedWeapons.length)].displayName();
    }

    private int validSlot(int slot, int length) {
        if (slot < 0 || slot >= length) {
            throw new IllegalArgumentException("Weapon slot out of range: " + (slot + 1));
        }
        return slot;
    }
}
