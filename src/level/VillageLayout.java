package level;

/**
 * Where things stand in the village. The map builder (mapEditor.VillageBuilder) draws the
 * buildings at these spots and AssetSetter puts doors and NPCs on top of them, so both
 * always agree. Coordinates are tile columns and rows.
 */
public final class VillageLayout {
    public static final int COLS = 68;
    public static final int ROWS = 54;

    // Plaza with the old giant mushroom; the player wakes up here.
    public static final int PLAZA_LEFT = 26;
    public static final int PLAZA_TOP = 18;
    public static final int PLAZA_RIGHT = 37;
    public static final int PLAZA_BOTTOM = 28;
    public static final int GIANT_MUSHROOM_COL = 27;
    public static final int GIANT_MUSHROOM_ROW = 19;
    public static final int WELL_COL = 34;
    public static final int WELL_ROW = 24;
    public static final int SPAWN_COL = 31;
    public static final int SPAWN_ROW = 23;

    // Walled yard in front of the dojo hall: gravel, lanterns, pines, a roofed gate and stairs down.
    // The hall has a double door (doorCol and doorCol + 1), lined up with the 2-tile road.
    public static final int DOJO_YARD_LEFT = 25;
    public static final int DOJO_YARD_RIGHT = 38;
    public static final int DOJO_WALL_ROW = 14;
    public static final int DOJO_GATE_COL = 30;
    public static final int DOJO_GATE_ROW = 12;

    // Buildings: top-left tile and the door cell inside the building image.
    public static final Building DOJO = new Building("dojo", 27, 3, 10, 6, 4, 5);
    public static final Building MYCOLOGIST = new Building("mycologist_hut", 17, 9, 4, 4, 1, 3);
    public static final Building SHOP = new Building("shop", 10, 19, 5, 4, 2, 3);
    public static final Building SMITHY = new Building("smithy", 45, 19, 5, 4, 2, 3);
    // The old sage lives in a half-collapsed brick hut down in the south-west corner.
    public static final Building SAGE = new Building("house_ruined", 9, 39, 4, 4, 1, 3);
    public static final Building[] HOMES = {
            new Building("house_wood", 39, 8, 4, 4, 1, 3),
            new Building("house_brick", 44, 9, 4, 4, 2, 3),
            new Building("house_brick", 7, 32, 4, 4, 2, 3),
            new Building("house_wood", 12, 32, 4, 4, 1, 3),
            new Building("house_wood", 22, 32, 4, 4, 1, 3),
    };

    // The gate out to the forest: a 2-tile opening with a guard on each side.
    public static final int GATE_COL = 30;
    public static final int GATE_ROW = 43;
    public static final int GATE_OPENING_COL = 31;
    public static final int PALISADE_ROW = 45;
    public static final int GUARD_LEFT_COL = 30;
    public static final int GUARD_RIGHT_COL = 33;
    public static final int GUARD_ROW = 46;
    public static final int PORTAL_ROW = 50;

    // River running down the east side into the lake, with a pier for fishing later.
    public static final int RIVER_LEFT = 55;
    public static final int RIVER_RIGHT = 57;
    public static final int RIVER_BOTTOM = 34;
    public static final int LAKE_CENTER_COL = 52;
    public static final int LAKE_CENTER_ROW = 39;
    public static final int LAKE_RADIUS_COLS = 8;
    public static final int LAKE_RADIUS_ROWS = 5;
    public static final int PIER_COL = 50;
    public static final int PIER_TOP = 33;
    public static final int PIER_BOTTOM = 38;
    public static final int BRIDGE_ROW = 23;

    // Empty plots waiting for NPCs unlocked by bosses.
    public static final Plot[] PLOTS = {
            new Plot(6, 8, 6, 6, false),
            new Plot(37, 33, 6, 6, true),
            new Plot(59, 19, 6, 8, true),
    };

    // Things to read or look at. The builder places the tile, AssetSetter makes it readable.
    public static final Readable[] READABLES = {
            new Readable("sign_obj", 34, 15, "North: the dojo. Bow at the door.", "Someone has scratched underneath: \"he hits harder than the forest\"."),
            new Readable("sign_obj", 24, 22, "West: Spore & Sundry. Fair prices, mostly."),
            new Readable("sign_obj", 39, 22, "East: the forge. Mind the sparks."),
            new Readable("weird_sign", 25, 19, "WELCOME BACK", "Below it, rows and rows of tally marks. The newest ones are still wet."),
            new Readable("weird_sign", 22, 12, "DO NOT EAT THE QUIET ONES.", "The quiet ones are underlined three times."),
            new Readable("weird_sign", 28, 41, "THE FOREST REMEMBERS YOUR NAME.", "You never told anyone here your name."),
            new Readable("weird_sign", 54, 28, "NO SWIMMING. THE LAKE SWIMS BACK."),
            new Readable("backpack", 35, 41, "A school backpack. Inside: a bus ticket, a lighter, a phone charger.", "The ticket is from your city. The date is smudged."),
            new Readable("sneaker", 48, 30, "One sneaker, laces still tied. Your size."),
            new Readable("phone", 36, 29, "A cracked phone. 3% battery. 47 missed calls from \"Mom\".", "It dies while you're holding it."),
            new Readable("bicycle", 43, 11, "A rusty city bike. Grass has grown through the spokes.", "There is a sticker from a pizza place you used to order from."),
            new Readable("plot_sign", 9, 13, "RESERVED.", "For whoever comes back from the deep part of the forest."),
            new Readable("plot_sign", 40, 38, "RESERVED.", "The ruins are older than the village. Something will move in."),
            new Readable("weird_sign", 14, 40, "HE REMEMBERS THE FIRST ONE OF US.", "Under it, smaller: \"knock loud, he is deaf in the left ear\"."),
            new Readable("plot_sign", 61, 26, "RESERVED.", "A note is pinned to the post: \"not yet\"."),
    };

    private VillageLayout() {
    }

    public static final class Readable {
        public final String tile;
        public final int col;
        public final int row;
        public final String[] lines;

        public Readable(String tile, int col, int row, String... lines) {
            this.tile = tile;
            this.col = col;
            this.row = row;
            this.lines = lines;
        }
    }

    public static final class Building {
        public final String tile;
        public final int col;
        public final int row;
        public final int width;
        public final int height;
        public final int doorX;
        public final int doorY;

        Building(String tile, int col, int row, int width, int height, int doorX, int doorY) {
            this.tile = tile;
            this.col = col;
            this.row = row;
            this.width = width;
            this.height = height;
            this.doorX = doorX;
            this.doorY = doorY;
        }

        public int doorCol() {
            return col + doorX;
        }

        public int doorRow() {
            return row + doorY;
        }

        /** The tile just outside the door, where the player stands after leaving. */
        public int frontRow() {
            return doorRow() + 1;
        }
    }

    public static final class Plot {
        public final int col;
        public final int row;
        public final int width;
        public final int height;
        public final boolean ruined;

        Plot(int col, int row, int width, int height, boolean ruined) {
            this.col = col;
            this.row = row;
            this.width = width;
            this.height = height;
            this.ruined = ruined;
        }
    }
}
