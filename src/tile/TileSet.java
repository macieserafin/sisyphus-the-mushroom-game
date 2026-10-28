package tile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The one list of tiles shared by the game and the map editor.
 * Ids are what map files store, so existing ids must never change; add new tiles at the end.
 *
 * Ground tiles fill the base layer. Object tiles (furniture, buildings, decorations) have
 * transparent pixels and go on the object layer, on top of any ground.
 * Big objects are drawn as one image and cut into a grid of tiles ("house_red@2,1").
 */
public final class TileSet {
    public static final int ANIMATION_FRAME_TICKS = 20;

    private static final int DEEP_WATER = 0;
    private static final int WATER = 1;
    private static final int SAND = 2;
    private static final int EARTH = 3;
    private static final int STONE_FLOOR = 4;
    private static final int MYCELIUM = 5;
    private static final int GRASS = 6;
    private static final int DARK_GRASS = 7;
    private static final int PLAZA = EARTH;
    private static final int NONE = -1;

    private static final int GRASS_ID = 0;
    private static final int WATER_ID = 2;
    private static final int STONE_FLOOR_ID = 19;
    private static final int MYCELIUM_ID = 25;

    private static final List<Def> DEFS = new ArrayList<>();
    private static final Map<String, Integer> IDS = new HashMap<>();

    static {
        ground("grass", "Grass", false, GRASS, GRASS_ID, "grass");
        ground("wall", "Brick wall", true, NONE, -1, "wall");
        ground("water", "Water", true, WATER, WATER_ID, "water", "water_2", "water_3");
        ground("earth", "Earth", false, EARTH, 3, "earth");
        ground("tree", "Red mushroom", true, GRASS, GRASS_ID, "tree");
        ground("sand", "Sand", false, SAND, 5, "sand");
        ground("grass_dark", "Dark grass", false, DARK_GRASS, 6, "grass_dark");
        ground("grass_flowers", "Flowers", false, GRASS, GRASS_ID, "grass_flowers");
        ground("grass_tall", "Tall grass", false, GRASS, GRASS_ID, "grass_tall");
        ground("grass_shrooms", "Tiny mushrooms", false, GRASS, GRASS_ID, "grass_shrooms");
        ground("mushroom_blue", "Blue mushroom", true, GRASS, GRASS_ID, "mushroom_blue");
        ground("mushroom_purple", "Glow mushrooms", true, GRASS, GRASS_ID, "mushroom_purple");
        ground("rock", "Rock", true, GRASS, GRASS_ID, "rock");
        ground("bush", "Bush", true, GRASS, GRASS_ID, "bush");
        ground("stump", "Stump", true, GRASS, GRASS_ID, "stump");
        ground("fence_h", "Fence (horizontal)", true, GRASS, GRASS_ID, "fence_h");
        ground("fence_v", "Fence (vertical)", true, GRASS, GRASS_ID, "fence_v");
        ground("sign", "Sign", true, GRASS, GRASS_ID, "sign");
        ground("lantern", "Lantern", true, GRASS, GRASS_ID, "lantern");
        ground("stone_floor", "Stone floor", false, STONE_FLOOR, STONE_FLOOR_ID, "stone_floor");
        ground("planks", "Planks", false, NONE, -1, "planks");
        ground("crate", "Crate", true, STONE_FLOOR, STONE_FLOOR_ID, "crate");
        ground("barrel", "Barrel", true, STONE_FLOOR, STONE_FLOOR_ID, "barrel");
        ground("wall_stone", "Stone wall", true, NONE, -1, "wall_stone");
        ground("wall_mossy", "Mossy wall", true, NONE, -1, "wall_mossy");
        ground("mycelium", "Mycelium", false, MYCELIUM, MYCELIUM_ID, "mycelium");
        ground("mycelium_glow", "Glowing mycelium", false, MYCELIUM, MYCELIUM_ID, "mycelium_glow");
        ground("water_deep", "Deep water", true, DEEP_WATER, 27, "water_deep", "water_deep_2", "water_deep_3");
        ground("lily_pad", "Lily pad", true, WATER, WATER_ID, "lily_pad", "lily_pad_2", "lily_pad_3");
        ground("bridge_h", "Bridge (horizontal)", false, NONE, -1, "bridge_h");
        ground("bridge_v", "Bridge (vertical)", false, NONE, -1, "bridge_v");

        ground("plaza", "Plaza stones", false, PLAZA, 31, "plaza");
        ground("plaza_ornament", "Plaza ornament", false, PLAZA, 31, "plaza_ornament");
        ground("path", "Village path", false, EARTH, 33, "path");
        ground("wood_floor", "Wood floor", false, NONE, -1, "wood_floor");
        ground("tatami", "Tatami", false, NONE, -1, "tatami");
        ground("rug_red", "Red rug", false, NONE, -1, "rug_red");
        ground("forge_floor", "Forge floor", false, NONE, -1, "forge_floor");
        ground("wall_interior", "Interior wall", true, NONE, -1, "wall_interior");
        ground("wall_interior_window", "Interior wall (window)", true, NONE, -1, "wall_interior_window");
        ground("wall_dojo", "Dojo wall", true, NONE, -1, "wall_dojo");
        ground("wall_forge", "Forge wall", true, NONE, -1, "wall_forge");
        ground("pier", "Pier planks", false, NONE, -1, "pier");
        ground("void", "Darkness", true, NONE, -1, "void");
        ground("gravel", "Raked gravel", false, NONE, -1, "gravel");
        ground("thicket", "Forest thicket", true, NONE, -1, "thicket");
        ground("stone_path", "Stone path", false, NONE, -1, "stone_path");
        ground("stairs", "Stone stairs", false, NONE, -1, "stairs");

        object("lamp_post", "Lamp post", true, "lamp_post");
        object("glow_shrooms", "Glowing mushrooms", true, "glow_shrooms");
        object("tiny_glow", "Tiny glow spores", false, "tiny_glow");
        object("flowers_obj", "Flower patch", false, "flowers_obj");
        object("reeds", "Reeds", true, "reeds");
        object("rock_small", "Pebbles", false, "rock_small");
        object("bush_obj", "Bush", true, "bush_obj");
        object("barrel_obj", "Barrel", true, "barrel_obj");
        object("crate_obj", "Crate", true, "crate_obj");
        object("sack", "Sack", true, "sack");
        object("sign_obj", "Sign", true, "sign_obj");
        object("weird_sign", "Weird sign", true, "weird_sign");
        object("backpack", "Abandoned backpack", true, "backpack");
        object("sneaker", "Lost sneaker", false, "sneaker");
        object("phone", "Cracked phone", false, "phone");
        object("bicycle", "Rusty bicycle", true, "bicycle");
        object("fence_post", "Fence post", true, "fence_post");
        object("fence_obj_h", "Fence", true, "fence_obj_h");
        object("fence_obj_v", "Fence (vertical)", true, "fence_obj_v");
        object("fence_broken", "Broken fence", true, "fence_broken");
        object("ruin_stone", "Ruin stones", true, "ruin_stone");
        object("ruin_pillar", "Broken pillar", true, "ruin_pillar");
        object("plot_sign", "Plot sign", true, "plot_sign");
        object("palisade", "Palisade", true, "palisade");
        object("palisade_v", "Palisade (vertical)", true, "palisade_v");
        object("mycelium_creep", "Creeping mycelium", false, "mycelium_creep");
        object("wall_mycelium", "Mycelium on wall", true, "wall_mycelium");
        object("pier_post", "Pier post", true, "pier_post");
        object("fishing_spot", "Ripples", false, "fishing_spot", "fishing_spot_2", "fishing_spot_3");
        object("anvil", "Anvil", true, "anvil");
        object("grindstone", "Grindstone", true, "grindstone");
        object("tool_rack", "Tool rack", true, "tool_rack");
        object("table", "Table", true, "table");
        object("chair", "Chair", false, "chair");
        object("stool", "Stool", false, "stool");
        object("bookshelf", "Bookshelf", true, "bookshelf");
        object("wardrobe", "Wardrobe", true, "wardrobe");
        object("cabinet", "Cabinet", true, "cabinet");
        object("pot_plant", "Potted plant", true, "pot_plant");
        object("counter", "Counter", true, "counter");
        object("counter_end", "Counter end", true, "counter_end");
        object("shelf_jars", "Jar shelf", true, "shelf_jars");
        object("shelf_goods", "Goods shelf", true, "shelf_goods");
        object("cauldron", "Cauldron", true, "cauldron", "cauldron_2", "cauldron_3");
        object("alchemy_table", "Alchemy table", true, "alchemy_table");
        object("herbs_hanging", "Drying herbs", true, "herbs_hanging");
        object("weapon_rack", "Weapon rack", true, "weapon_rack");
        object("training_dummy", "Training dummy", true, "training_dummy");
        object("dojo_scroll", "Wall scroll", true, "dojo_scroll");
        object("bonsai", "Bonsai", true, "bonsai");
        object("candle", "Candles", true, "candle", "candle_2");
        object("door_mat", "Door mat", false, "door_mat");
        object("rug_round", "Round rug", false, "rug_round");
        object("temple_wall", "Temple wall", true, "temple_wall");
        object("temple_wall_v", "Temple wall (vertical)", true, "temple_wall_v");
        object("stone_lantern", "Stone lantern", true, "stone_lantern");
        object("rope_post", "Rope posts", true, "rope_post");
        object("sandbags", "Sandbags", true, "sandbags");
        object("ammo_crate", "Ammo crate", true, "ammo_crate");
        object("scrap_fence", "Scrap metal fence", true, "scrap_fence");
        object("shell_casings", "Spent casings", false, "shell_casings");
        object("red_mushroom", "Red mushroom", true, "red_mushroom");
        object("red_mushrooms_small", "Small red mushrooms", false, "red_mushrooms_small");
        object("blue_mushroom", "Blue mushroom", true, "blue_mushroom");
        object("scrolls_floor", "Scattered scrolls", false, "scrolls_floor");
        object("papers_floor", "Scattered notes", false, "papers_floor");
        object("book_pile", "Pile of books", true, "book_pile");
        object("notes_wall", "Notes on the wall", true, "notes_wall");
        object("escape_map", "Crossed-out map", true, "escape_map");
        object("tally_wall", "Tally marks", true, "tally_wall");
        object("broken_weapons", "Broken weapons", false, "broken_weapons");
        object("old_musket", "Rusty musket", true, "old_musket");
        object("dug_hole", "Hole in the floor", true, "dug_hole");

        grid("bed", "Bed", 1, 2, "X", "X");
        grid("fireplace", "Fireplace", 2, 2, "XX", "XX");
        grid("forge", "Forge", 2, 2, "XX", "XX");
        grid("well", "Well", 2, 2, "XX", "XX");
        grid("giant_mushroom", "Old giant mushroom", 3, 3, "XXX", "XXX", ".X.");
        grid("house_wood", "Wooden hut", 4, 4, "XXXX", "XXXX", "XXXX", "XXXX");
        grid("house_brick", "Brick hut", 4, 4, "XXXX", "XXXX", "XXXX", "XXXX");
        grid("house_ruined", "Ruined brick hut", 4, 4, "XXXX", "XXXX", "XXXX", "XXXX");
        grid("smithy", "Smithy", 5, 4, "XXXXX", "XXXXX", "XXXXX", "XXXXX");
        grid("shop", "Shop", 5, 4, "XXXXX", "XXXXX", "XXXXX", "XXXXX");
        grid("dojo", "Dojo hall", 10, 6, "XXXXXXXXXX", "XXXXXXXXXX", "XXXXXXXXXX", "XXXXXXXXXX", "XXXXXXXXXX", "XXXXXXXXXX");
        grid("dojo_gate", "Dojo gate", 4, 3, "X..X", "X..X", "X..X");
        grid("pine", "Pine", 2, 3, "..", "..", "XX");
        grid("oak", "Old tree", 2, 2, "XX", "XX");
        grid("hay_bed", "Hay bed", 2, 1, "XX");
        grid("mycologist_hut", "Mycologist hut", 4, 4, "XXXX", "XXXX", "XXXX", "XXXX");
        grid("gate", "Village gate", 4, 3, "X..X", "X..X", "X..X");
        grid("market_stall", "Market stall", 3, 2, "XXX", "XXX");
        grid("dead_tree", "Dead tree", 2, 3, "..", "XX", "XX");
        grid("ruin_wall", "Ruined wall", 3, 2, "XXX", "XXX");
    }

    private TileSet() {
    }

    public static int count() {
        return DEFS.size();
    }

    public static int id(String name) {
        Integer id = IDS.get(name);
        if (id == null) {
            throw new IllegalArgumentException("Unknown tile: " + name);
        }
        return id;
    }

    public static int id(String name, int x, int y) {
        return id(name + "@" + x + "," + y);
    }

    public static Tile[] loadTiles() {
        Tile[] tiles = new Tile[DEFS.size()];
        Map<String, BufferedImage> bigImages = new HashMap<>();
        for (int id = 0; id < DEFS.size(); id++) {
            Def def = DEFS.get(id);
            Tile tile = new Tile();
            tile.name = def.label;
            tile.collision = def.collision;
            tile.blendPriority = def.blendPriority;
            tile.blendSource = def.blendSource;
            tile.objectLayer = def.objectLayer;

            if (def.bigImage != null) {
                BufferedImage big = bigImages.computeIfAbsent(def.bigImage, name -> loadImage("tiles/big/" + name + ".png"));
                tile.frames = new BufferedImage[]{big.getSubimage(def.cellX * 16, def.cellY * 16, 16, 16)};
                tile.group = def.bigImage;
                tile.groupX = def.cellX;
                tile.groupY = def.cellY;
                tile.groupWidth = def.gridWidth;
                tile.groupHeight = def.gridHeight;
            } else {
                tile.frames = new BufferedImage[def.files.length];
                for (int i = 0; i < def.files.length; i++) {
                    tile.frames[i] = loadImage("tiles/" + def.files[i] + ".png");
                }
            }
            tile.image = tile.frames[0];
            tiles[id] = tile;
        }
        return tiles;
    }

    private static void ground(String key, String label, boolean collision, int blendPriority, int blendSource, String... files) {
        add(key, new Def(label, collision, blendPriority, blendSource, false, files));
    }

    private static void object(String key, String label, boolean collision, String... files) {
        add(key, new Def(label, collision, NONE, -1, true, files));
    }

    // Collision rows use 'X' for solid cells and '.' for walkable ones.
    private static void grid(String key, String label, int width, int height, String... collisionRows) {
        if (collisionRows.length != height) {
            throw new IllegalStateException("Collision rows for " + key + " must match its height");
        }
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Def def = new Def(label + " " + x + "," + y, collisionRows[y].charAt(x) == 'X', NONE, -1, true);
                def.bigImage = key;
                def.cellX = x;
                def.cellY = y;
                def.gridWidth = width;
                def.gridHeight = height;
                add(key + "@" + x + "," + y, def);
            }
        }
    }

    private static void add(String key, Def def) {
        if (IDS.containsKey(key)) {
            throw new IllegalStateException("Duplicate tile " + key);
        }
        IDS.put(key, DEFS.size());
        DEFS.add(def);
    }

    private static BufferedImage loadImage(String path) {
        try (InputStream stream = Objects.requireNonNull(
                TileSet.class.getClassLoader().getResourceAsStream(path),
                "Missing tile image: " + path
        )) {
            return ImageIO.read(stream);
        } catch (IOException e) {
            throw new IllegalStateException("Could not load tile image " + path + ": " + e.getMessage(), e);
        }
    }

    private static final class Def {
        final String label;
        final boolean collision;
        final int blendPriority;
        final int blendSource;
        final boolean objectLayer;
        final String[] files;
        String bigImage;
        int cellX;
        int cellY;
        int gridWidth;
        int gridHeight;

        Def(String label, boolean collision, int blendPriority, int blendSource, boolean objectLayer, String... files) {
            this.label = label;
            this.collision = collision;
            this.blendPriority = blendPriority;
            this.blendSource = blendSource;
            this.objectLayer = objectLayer;
            this.files = files;
        }
    }
}
