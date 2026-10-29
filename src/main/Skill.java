package main;

// Techniques taught by Master Kuro in the dojo. A skill can be learned once the skill it grows from is known
// (null = straight from the root). Paid with banked mushrooms for now; skill points will replace that later.
public enum Skill {
    ROLL("Roll", "Sword mode: SPACE rolls. Nothing can hit you while rolling, and you cut through what you roll into.",
            UiSkin.Icon.ROLL, 50, null),
    DASH("Dash", "Gun mode: SPACE dashes, a quick burst of speed. It does not protect you.",
            UiSkin.Icon.DASH, 50, null);

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
