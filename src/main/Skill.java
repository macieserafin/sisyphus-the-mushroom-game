package main;

public enum Skill {
    ROLL("Roll", "Sword mode: SPACE rolls. Nothing can hit you while rolling, and you cut through what you roll into.",
            UiSkin.Icon.ROLL, 1, null),
    DASH("Dash", "Gun mode: SPACE dashes, a quick burst of speed. It does not protect you.",
            UiSkin.Icon.DASH, 1, null),
    PROJECTILE_CUT("Projectile Cut", "Sword swings destroy small enemy projectiles. Heavy shots and area attacks cannot be cut.",
            UiSkin.Icon.SWORD, 1, null),
    PROJECTILE_REFLECT("Projectile Reflect", "Sword swings send small enemy projectiles back with 50% damage (rounded up). Heavy shots cannot be reflected.",
            UiSkin.Icon.HEAVY_SWORD, 1, PROJECTILE_CUT);

    public final String title;
    public final String description;
    public final UiSkin.Icon icon;
    public final int cost;
    public final Skill requires;

    Skill(String title, String description, UiSkin.Icon icon, int cost, Skill requires) {
        this.title = title;
        this.description = description;
        this.icon = icon;
        this.cost = cost;
        this.requires = requires;
    }
}
