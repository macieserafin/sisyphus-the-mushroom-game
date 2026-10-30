package main;

import entity.combat.HitType;
import story.ChapterText;
import java.util.*;

public final class Relics {
    public enum Item {
        CROWN(true), RESIN(false), EMBER(false), THREAD(false);
        public final boolean special;
        Item(boolean special) { this.special = special; }
        public String title() { return ChapterText.get("item." + name() + ".name"); }
        public String description() { return ChapterText.get("item." + name() + ".description"); }
    }

    private final GamePanel gp;
    private final EnumSet<Item> owned = EnumSet.noneOf(Item.class);
    private final EnumSet<Item> equipped = EnumSet.noneOf(Item.class);
    public Relics(GamePanel gp) { this.gp = gp; }
    public boolean owns(Item item) { return owned.contains(item); }
    public boolean active(Item item) { return equipped.contains(item); }
    public void grant(Item item) { owned.add(item); if (space(item)) equipped.add(item); }
    private boolean space(Item item) {
        return equipped.stream().filter(i -> i.special == item.special).count() < (item.special ? 2 : 5);
    }
    public boolean toggle(Item item) {
        if (gp.campaign.inCombat() || !owns(item)) return false;
        if (equipped.remove(item)) return true;
        if (!space(item)) return false;
        equipped.add(item); return true;
    }
    public double damage(HitType type) {
        return type == HitType.RANGED && active(Item.RESIN) ? 1.15 :
                type == HitType.MELEE && active(Item.EMBER) ? 1.15 : 1.0;
    }
    public int detonationPerMark() { return 30 + (active(Item.EMBER) ? 6 : 0) + (active(Item.RESIN) ? 3 : 0); }
    public int markLimit() { return 6 + (active(Item.THREAD) ? 2 : 0); }
    public void save(Properties out) {
        out.setProperty("relic.owned", names(owned)); out.setProperty("relic.equipped", names(equipped));
    }
    private static String names(Set<Item> items) { return String.join(",", items.stream().map(Enum::name).toList()); }
    public void load(Properties in) {
        owned.clear(); equipped.clear();
        for (Item item : Item.values()) {
            if (Arrays.asList(in.getProperty("relic.owned", "").split(",")).contains(item.name())) owned.add(item);
            if (owns(item) && Arrays.asList(in.getProperty("relic.equipped", "").split(",")).contains(item.name()) && space(item)) equipped.add(item);
        }
    }
    public void open() {
        if (gp.campaign.inCombat()) { gp.ui.showMessage("In combat: equipment locked"); return; }
        List<ChoiceMenu.Option> rows = new ArrayList<>();
        for (Item item : Item.values()) if (owns(item)) rows.add(new ChoiceMenu.Option(
                (active(item) ? "[x] " : "[ ] ") + item.title(), item.description(), () -> { toggle(item); open(); }));
        rows.add(new ChoiceMenu.Option("Back", "2 special slots / 5 other slots. Items combine across both categories.", () -> {}));
        gp.choiceMenu.open("Equipment  [I]", rows, true);
    }
}
