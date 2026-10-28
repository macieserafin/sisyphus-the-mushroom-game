package level;

public enum Interior {
    SMITHY("maps/interior_smithy.txt", VillageLayout.SMITHY),
    SHOP("maps/interior_shop.txt", VillageLayout.SHOP),
    DOJO("maps/interior_dojo.txt", VillageLayout.DOJO),
    MYCOLOGIST("maps/interior_mycologist.txt", VillageLayout.MYCOLOGIST),
    SAGE("maps/interior_sage.txt", VillageLayout.SAGE);

    public static final int COLS = 15;
    public static final int ROWS = 11;
    public static final int EXIT_COL = 7;
    public static final int EXIT_ROW = ROWS - 1;

    public static final int DOJO_WEAPON_RACK_COL = 2;
    public static final int DOJO_WEAPON_RACK_ROW = 2;
    public static final int DOJO_GUN_CABINET_COL = 12;
    public static final int DOJO_GUN_CABINET_ROW = 2;

    public static final VillageLayout.Readable[] SAGE_NOTES = {
            new VillageLayout.Readable("escape_map", 7, 1, "A hand-drawn map of the village. Every road out is crossed in red.", "In the corner, very small: \"the gate only opens one way\"."),
            new VillageLayout.Readable("tally_wall", 11, 1, "Tally marks. Thousands of them. They cover the wall and carry on behind the bookshelves."),
            new VillageLayout.Readable("dug_hole", 2, 7, "A hole dug straight down through the floorboards.", "At the bottom: more floorboards. The same floorboards."),
            new VillageLayout.Readable("papers_floor", 8, 4, "Notes in a shaky hand: \"Tried the river. Tried the lake. Tried the forest, twelve hundred times.\"", "\"Tried dying on purpose. Woke on the plaza. Every time.\""),
            new VillageLayout.Readable("old_musket", 13, 2, "A musket, rusted solid. Someone carried it in from your world, a very long time ago.", "The stock is carved with a date. The year hasn't happened yet."),
            new VillageLayout.Readable("scrolls_floor", 4, 7, "Scrolls in a language that changes while you read it.", "One phrase stays put: \"the door is a mouth\"."),
    };

    public final String mapPath;
    public final VillageLayout.Building building;

    Interior(String mapPath, VillageLayout.Building building) {
        this.mapPath = mapPath;
        this.building = building;
    }

    public int spawnCol() {
        return EXIT_COL;
    }

    public int spawnRow() {
        return EXIT_ROW - 1;
    }
}
