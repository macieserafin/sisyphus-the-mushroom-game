package world;

import entity.Bullet;
import entity.combat.Faction;
import entity.enemy.Enemy;
import entity.enemy.EnemyShot;
import main.GamePanel;
import object.SuperObject;
import tile.Tile;
import tile.TileManager;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class Lighting {
    private static final String SETTING_KEY = "lighting";
    private static final int DIV = 2;
    private static final int MAX_LIGHTS = 120;
    private static final int FOG_TILE = 96;
    private static final int NO_SHADOW = 0, STATIC_SHADOW = 1, MOVING_SHADOW = 2;

    private static final Set<String> NOT_OCCLUDING = Set.of("Rock", "Bush", "Stump", "Sign", "Lantern",
            "Fence (horizontal)", "Fence (vertical)", "Blue mushroom", "Glow mushrooms", "Red mushroom",
            "Water", "Deep water", "Lily pad");
    private static final Set<String> OCCLUDING_GROUPS = Set.of("house_brick", "house_wood", "house_ruined", "smithy",
            "shop", "dojo", "mycologist_hut", "ruin_wall", "cave_entrance");
    private static final Set<String> SHADED_GROUND = Set.of("Forest floor", "Forest floor (leaves)", "Moss", "Mycelium",
            "Glowing mycelium", "Dark grass", "Deep forest canopy", "Forest canopy", "Forest edge (trunks)");

    private static final class Ambient {
        float darkness;
        Color color = Palette.AMBIENT_FOREST;
        float vignette = 0.14f;
        Color grade;
        float gradeAlpha;
        Color fog;
        float fogAlpha;
        boolean zones;
    }

    private static final class Emitter {
        final Color color;
        final float radius;
        final float intensity;
        final float flicker;

        Emitter(Color color, float radius, float intensity, float flicker) {
            this.color = color;
            this.radius = radius;
            this.intensity = intensity;
            this.flicker = flicker;
        }
    }

    private static final class Pulse {
        double x, y, radius;
        Color color;
        float intensity;
        int life, maxLife;
    }

    private final GamePanel gp;
    private final BufferedImage layer;
    private final BufferedImage vignette;
    private final BufferedImage dangerVignette;
    private final float[] fogNoise = makeFogNoise();
    private final Map<Integer, BufferedImage> holes = new HashMap<>();
    private final Map<Long, BufferedImage> washes = new HashMap<>();
    private final Map<String, BufferedImage> shadowCache = new HashMap<>();
    private final Map<String, BufferedImage> movingCache = new LinkedHashMap<>(64, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, BufferedImage> eldest) {
            return size() > 48;
        }
    };
    private final Map<Integer, BufferedImage> fogSprites = new HashMap<>();
    private final List<Pulse> pulses = new ArrayList<>();

    private Emitter[] emitterByTile = new Emitter[0];
    private boolean[] occluderByTile = new boolean[0];
    private boolean[] shadedByTile = new boolean[0];
    private BufferedImage zoneImage;
    private String ambientMap = "";
    private Ambient ambient = new Ambient();
    private boolean enabled;
    private int tick;

    private final double[] lx = new double[MAX_LIGHTS];
    private final double[] ly = new double[MAX_LIGHTS];
    private final double[] lr = new double[MAX_LIGHTS];
    private final double[] lbase = new double[MAX_LIGHTS];
    private final float[] li = new float[MAX_LIGHTS];
    private final Color[] lc = new Color[MAX_LIGHTS];
    private final int[] lshadow = new int[MAX_LIGHTS];
    private int lights;

    public Lighting(GamePanel gp) {
        this.gp = gp;
        int width = gp.screenWidth / DIV;
        int height = gp.screenHeight / DIV;
        layer = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB_PRE);
        vignette = makeVignette(width, height, 0x000000);
        dangerVignette = makeVignette(width, height, 0x7A0C10);
        enabled = gp.settings.getBoolean(SETTING_KEY, true);
    }

    public boolean enabled() {
        return enabled;
    }

    public boolean toggle() {
        enabled = !enabled;
        gp.settings.putBoolean(SETTING_KEY, enabled);
        return enabled;
    }

    public void clear() {
        pulses.clear();
    }

    public void update() {
        tick++;
        pulses.removeIf(p -> --p.life <= 0);
    }

    public void pulse(double worldX, double worldY, double radius, Color color, float intensity, int ticks) {
        if (pulses.size() >= 24) {
            return;
        }
        Pulse p = new Pulse();
        p.x = worldX;
        p.y = worldY;
        p.radius = radius;
        p.color = color;
        p.intensity = intensity;
        p.life = p.maxLife = ticks;
        pulses.add(p);
    }

    public void add(double worldX, double worldY, double radius, Color color, float intensity, float flicker) {
        put(worldX, worldY, radius, color, intensity, flicker, NO_SHADOW);
    }

    public void addShadowed(double worldX, double worldY, double radius, Color color, float intensity, float flicker) {
        put(worldX, worldY, radius, color, intensity, flicker, MOVING_SHADOW);
    }

    private void put(double worldX, double worldY, double radius, Color color, float intensity, float flicker, int shadow) {
        if (lights >= MAX_LIGHTS) {
            return;
        }
        float wobble = flicker <= 0 ? 1f
                : 1f + flicker * (float) (Math.sin(tick * 0.31 + worldX * 0.07 + worldY * 0.05) * 0.6
                + Math.sin(tick * 0.77 + worldX * 0.03) * 0.4);
        lx[lights] = worldX;
        ly[lights] = worldY;
        lbase[lights] = radius;
        lr[lights] = shadow == NO_SHADOW ? radius * (0.96 + 0.04 * wobble) : radius;
        li[lights] = Math.max(0f, Math.min(1f, intensity * wobble));
        lc[lights] = color;
        lshadow[lights] = shadow;
        lights++;
    }

    public void draw(Graphics2D g2) {
        if (!enabled) {
            return;
        }
        updateAmbient();
        boolean isDark = ambient.darkness >= 0.01f;
        lights = 0;
        if (isDark) {
            collectLights();
        }

        int cameraX = gp.player.worldX - gp.player.screenX;
        int cameraY = gp.player.worldY - gp.player.screenY;

        Graphics2D g = layer.createGraphics();
        g.setComposite(AlphaComposite.Src);
        Color dark = ambient.color;
        g.setColor(new Color(dark.getRed(), dark.getGreen(), dark.getBlue(), Math.round(ambient.darkness * 255)));
        g.fillRect(0, 0, layer.getWidth(), layer.getHeight());
        if (isDark && ambient.zones) {
            drawZones(g, cameraX, cameraY);
        }
        if (ambient.grade != null) {
            g.setComposite(AlphaComposite.SrcOver.derive(ambient.gradeAlpha));
            g.setColor(ambient.grade);
            g.fillRect(0, 0, layer.getWidth(), layer.getHeight());
        }
        gp.ambience.drawShadows(g, cameraX, cameraY, DIV);
        if (ambient.fog != null && gp.ambience.enabled()) {
            drawFog(g, cameraX, cameraY);
        }

        for (int i = 0; i < lights; i++) {
            int r = Math.max(4, (int) Math.round(lr[i] / DIV));
            int x = (int) Math.round((lx[i] - cameraX) / DIV);
            int y = (int) Math.round((ly[i] - cameraY) / DIV);
            if (x + r < 0 || y + r < 0 || x - r > layer.getWidth() || y - r > layer.getHeight()) {
                lr[i] = 0;
                continue;
            }
            BufferedImage sprite = lshadow[i] == NO_SHADOW ? null : shadowed(i, r, false);
            g.setComposite(AlphaComposite.DstOut.derive(li[i]));
            g.drawImage(sprite != null ? sprite : hole(r), x - r, y - r, null);
        }

        float wash = Math.min(0.5f, 0.12f + ambient.darkness * 0.5f);
        for (int i = 0; i < lights; i++) {
            if (lr[i] == 0) {
                continue;
            }
            int full = Math.max(4, (int) Math.round(lr[i] / DIV));
            int x = (int) Math.round((lx[i] - cameraX) / DIV);
            int y = (int) Math.round((ly[i] - cameraY) / DIV);
            BufferedImage sprite = lshadow[i] == NO_SHADOW ? null : shadowed(i, full, true);
            int r = sprite != null ? full : Math.max(4, (int) Math.round(lr[i] / DIV * 0.8));
            g.setComposite(AlphaComposite.SrcOver.derive(Math.min(1f, li[i] * wash)));
            g.drawImage(sprite != null ? sprite : wash(r, lc[i]), x - r, y - r, null);
        }

        if (ambient.vignette > 0) {
            g.setComposite(AlphaComposite.SrcOver.derive(ambient.vignette));
            g.drawImage(vignette, 0, 0, null);
        }
        float danger = dangerLevel();
        if (danger > 0) {
            g.setComposite(AlphaComposite.SrcOver.derive(danger));
            g.drawImage(dangerVignette, 0, 0, null);
        }
        g.dispose();

        g2.drawImage(layer, 0, 0, gp.screenWidth, gp.screenHeight, null);
    }

    private float dangerLevel() {
        if (!gp.player.alive || gp.player.maxHealth < 2 || gp.player.health > 1) {
            return 0f;
        }
        return (float) (0.5 + 0.25 * Math.sin(tick * 0.14));
    }

    private void updateAmbient() {
        String path = gp.tileM.currentMapPath();
        if (path.equals(ambientMap)) {
            return;
        }
        ambientMap = path;
        ambient = ambientFor(path);
        Tile[] tiles = gp.tileM.tile;
        emitterByTile = emittersFor(tiles);
        occluderByTile = new boolean[tiles.length];
        shadedByTile = new boolean[tiles.length];
        for (int id = 0; id < tiles.length; id++) {
            Tile t = tiles[id];
            occluderByTile[id] = t.collision && (t.objectLayer
                    ? t.group != null && OCCLUDING_GROUPS.contains(t.group)
                    : !NOT_OCCLUDING.contains(t.name));
            shadedByTile[id] = !t.objectLayer && SHADED_GROUND.contains(t.name);
        }
        shadowCache.clear();
        movingCache.clear();
    }

    private static Ambient ambientFor(String path) {
        Ambient a = new Ambient();
        if (path.contains("boss_slime_arena")) {
            a.darkness = 0.38f;
            a.color = Palette.AMBIENT_ARENA;
            a.vignette = 0.34f;
            a.fog = Palette.GOO_LIGHT;
            a.fogAlpha = 0.07f;
        } else if (path.contains("byt_arena")) {
            a.darkness = 0.50f;
            a.color = Palette.AMBIENT_VOID;
            a.vignette = 0.40f;
            a.fog = Palette.PALE;
            a.fogAlpha = 0.14f;
        } else if (path.contains("prologue_void")) {
            a.darkness = 0.40f;
            a.color = Palette.AMBIENT_VOID;
            a.vignette = 0.32f;
            a.fog = Palette.PALE;
            a.fogAlpha = 0.10f;
            a.zones = true;
        } else if (path.contains("level1_forest")) {
            a.darkness = 0.14f;
            a.color = Palette.AMBIENT_FOREST;
            a.vignette = 0.22f;
            a.grade = new Color(0x5E9C7A);
            a.gradeAlpha = 0.03f;
            a.zones = true;
        } else if (path.contains("interior_")) {
            a.darkness = 0.16f;
            a.color = Palette.AMBIENT_WARM;
            a.vignette = 0.30f;
            a.grade = new Color(0xE8A060);
            a.gradeAlpha = 0.04f;
        } else if (path.contains("village")) {
            a.darkness = 0.11f;
            a.color = Palette.AMBIENT_DUSK;
            a.vignette = 0.16f;
            a.grade = new Color(0xF2B46A);
            a.gradeAlpha = 0.05f;
        }
        return a;
    }

    private static Emitter[] emittersFor(Tile[] tiles) {
        Map<String, Emitter> byName = new HashMap<>();
        byName.put("Blue mushroom", new Emitter(Palette.BLUE_CAP, 84, 0.75f, 0.05f));
        byName.put("Glow mushrooms", new Emitter(Palette.PURPLE_CAP, 84, 0.75f, 0.05f));
        byName.put("Glowing mushrooms", new Emitter(Palette.PURPLE_CAP, 90, 0.8f, 0.05f));
        byName.put("Glowcaps", new Emitter(Palette.TEAL, 76, 0.65f, 0.06f));
        byName.put("Glowing mycelium", new Emitter(Palette.PURPLE_CAP, 60, 0.45f, 0.05f));
        byName.put("Tiny glow spores", new Emitter(Palette.FIREFLY, 44, 0.45f, 0.2f));
        byName.put("Fireflies", new Emitter(Palette.FIREFLY, 70, 0.55f, 0.25f));
        byName.put("Lantern", new Emitter(Palette.FIRE, 120, 0.9f, 0.1f));
        byName.put("Stone lantern", new Emitter(Palette.FIRE, 120, 0.9f, 0.1f));
        byName.put("Lamp post", new Emitter(Palette.FIRE, 140, 0.95f, 0.08f));
        byName.put("Candles", new Emitter(Palette.FIRE, 84, 0.7f, 0.2f));
        byName.put("Void candles", new Emitter(Palette.VOID, 84, 0.7f, 0.2f));
        byName.put("Campfire", new Emitter(Palette.FIRE, 170, 1.0f, 0.16f));
        byName.put("Void brazier", new Emitter(Palette.VOID, 150, 0.95f, 0.14f));
        byName.put("Cave wall (torch)", new Emitter(Palette.FIRE, 150, 0.95f, 0.16f));
        byName.put("Emerald crystal", new Emitter(Palette.EMERALD, 100, 0.8f, 0.05f));
        byName.put("Amethyst crystal", new Emitter(Palette.PURPLE_CAP, 100, 0.8f, 0.05f));
        byName.put("Void crystal", new Emitter(Palette.VOID, 110, 0.85f, 0.06f));
        byName.put("Mushroom lamp", new Emitter(Palette.TEAL, 135, 0.95f, 0.06f));
        byName.put("Glow bells", new Emitter(Palette.TEAL, 56, 0.5f, 0.05f));

        Emitter[] result = new Emitter[tiles.length];
        for (int id = 0; id < tiles.length; id++) {
            Tile tile = tiles[id];
            result[id] = byName.get(tile.name);
            if ("fireplace".equals(tile.group) || "forge".equals(tile.group)) {
                result[id] = new Emitter(Palette.EMBER, 70, 0.8f, 0.14f);
            }
        }
        return result;
    }

    private void collectLights() {
        int cameraX = gp.player.worldX - gp.player.screenX;
        int cameraY = gp.player.worldY - gp.player.screenY;

        addShadowed(gp.player.worldX + gp.tileSize / 2.0, gp.player.worldY + gp.tileSize / 2.0, 150, Palette.FIRE, 0.6f, 0.03f);

        collectTileLights(cameraX, cameraY);
        gp.ambience.addLights(this);

        for (SuperObject object : gp.entities.objects()) {
            object.addLights(this);
        }
        for (Enemy enemy : gp.entities.enemies()) {
            if (enemy.alive) {
                enemy.addLights(this);
            }
        }
        for (Bullet bullet : gp.bulletManager.bullets()) {
            if (bullet.alive) {
                boolean mine = bullet.getFaction() == Faction.PLAYER;
                add(bullet.worldX + 4, bullet.worldY + 4, mine ? 64 : 56, mine ? Palette.FIRE : Palette.GOO_LIGHT, 0.85f, 0.1f);
            }
        }
        for (EnemyShot shot : gp.enemyShots.shots()) {
            if ((shot.kind == EnemyShot.Kind.SMALL || shot.kind == EnemyShot.Kind.HEAVY) && shot.age >= shot.delay) {
                add(shot.x, shot.y, shot.kind == EnemyShot.Kind.HEAVY ? 90 : 56, shot.body.brighter(), 0.8f, 0.08f);
            }
        }
        for (Pulse p : pulses) {
            add(p.x, p.y, p.radius, p.color, p.intensity * p.life / p.maxLife, 0f);
        }
    }

    private void collectTileLights(int cameraX, int cameraY) {
        TileManager map = gp.tileM;
        int size = gp.tileSize;
        int startCol = Math.max(0, cameraX / size - 3);
        int endCol = Math.min(map.getWorldCols() - 1, (cameraX + gp.screenWidth) / size + 3);
        int startRow = Math.max(0, cameraY / size - 3);
        int endRow = Math.min(map.getWorldRows() - 1, (cameraY + gp.screenHeight) / size + 3);
        for (int row = startRow; row <= endRow; row++) {
            for (int col = startCol; col <= endCol; col++) {
                emit(emitterByTile[map.mapTileNum[col][row]], col, row);
                int object = map.objectTileNum[col][row];
                if (object >= 0) {
                    emit(emitterByTile[object], col, row);
                }
            }
        }
    }

    private void emit(Emitter e, int col, int row) {
        if (e == null) {
            return;
        }
        float intensity = e.intensity;
        if (e.flicker <= 0.06f) {
            intensity *= 0.8f + 0.2f * (float) Math.sin(tick * 0.035 + col * 0.9 + row * 1.7);
        }
        put((col + 0.5) * gp.tileSize, (row + 0.5) * gp.tileSize, e.radius, e.color, intensity, e.flicker, STATIC_SHADOW);
    }

    private boolean occluder(int col, int row) {
        TileManager map = gp.tileM;
        if (col < 0 || row < 0 || col >= map.getWorldCols() || row >= map.getWorldRows()) {
            return false;
        }
        if (occluderByTile[map.mapTileNum[col][row]]) {
            return true;
        }
        int object = map.objectTileNum[col][row];
        return object >= 0 && occluderByTile[object];
    }

    private BufferedImage shadowed(int i, int r, boolean colored) {
        int color = colored ? lc[i].getRGB() : 0;
        boolean moving = lshadow[i] == MOVING_SHADOW;
        Map<String, BufferedImage> cache = moving ? movingCache : shadowCache;
        String key = moving
                ? Math.round(lx[i] / 8) + "," + Math.round(ly[i] / 8) + "," + r + "," + color
                : (int) lx[i] + "," + (int) ly[i] + "," + r + "," + color;
        if (cache.containsKey(key)) {
            return cache.get(key);
        }
        BufferedImage sprite = buildShadowed(lx[i], ly[i], lbase[i], r, colored ? color & 0xFFFFFF : 0xFFFFFF, colored);
        cache.put(key, sprite);
        return sprite;
    }

    private BufferedImage buildShadowed(double worldX, double worldY, double radius, int r, int rgb, boolean wash) {
        int size = gp.tileSize;
        int reach = (int) Math.ceil(radius / size) + 1;
        int n = reach * 2 + 1;
        int lightCol = (int) Math.floor(worldX / size);
        int lightRow = (int) Math.floor(worldY / size);
        float[] visible = new float[n * n];
        boolean anyBlocked = false;
        for (int gy = 0; gy < n; gy++) {
            for (int gx = 0; gx < n; gx++) {
                int col = lightCol - reach + gx;
                int row = lightRow - reach + gy;
                boolean clear = lineClear(worldX, worldY, (col + 0.5) * size, (row + 0.5) * size, lightCol, lightRow, col, row);
                visible[gy * n + gx] = clear ? 1f : 0f;
                anyBlocked |= !clear;
            }
        }
        if (!anyBlocked) {
            return null;
        }
        float[] soft = new float[n * n];
        for (int gy = 0; gy < n; gy++) {
            for (int gx = 0; gx < n; gx++) {
                float sum = 0;
                int count = 0;
                for (int oy = -1; oy <= 1; oy++) {
                    for (int ox = -1; ox <= 1; ox++) {
                        int x = gx + ox;
                        int y = gy + oy;
                        if (x >= 0 && y >= 0 && x < n && y < n) {
                            sum += visible[y * n + x] * (ox == 0 && oy == 0 ? 2 : 1);
                            count += ox == 0 && oy == 0 ? 2 : 1;
                        }
                    }
                }
                soft[gy * n + gx] = sum / count;
            }
        }
        visible = soft;
        int spriteSize = r * 2 + 1;
        double reachScale = wash ? 1.25 : 1.0;
        BufferedImage image = new BufferedImage(spriteSize, spriteSize, BufferedImage.TYPE_INT_ARGB);
        double originCol = lightCol - reach;
        double originRow = lightRow - reach;
        for (int y = 0; y < spriteSize; y++) {
            for (int x = 0; x < spriteSize; x++) {
                double d = Math.hypot(x - r, y - r) / r * reachScale;
                if (d >= 1) {
                    continue;
                }
                double u = (worldX + (x - r) * DIV) / size - 0.5 - originCol;
                double v = (worldY + (y - r) * DIV) / size - 0.5 - originRow;
                float factor = sample(visible, n, u, v);
                if (factor <= 0) {
                    continue;
                }
                double f = 1 - d;
                int alpha = (int) Math.round(255 * f * f * (3 - 2 * f) * factor);
                image.setRGB(x, y, (alpha << 24) | rgb);
            }
        }
        return image;
    }

    private boolean lineClear(double x0, double y0, double x1, double y1, int startCol, int startRow, int endCol, int endRow) {
        double distance = Math.hypot(x1 - x0, y1 - y0);
        int steps = Math.max(1, (int) Math.ceil(distance / 12));
        for (int s = 1; s < steps; s++) {
            double t = s / (double) steps;
            int col = (int) Math.floor((x0 + (x1 - x0) * t) / gp.tileSize);
            int row = (int) Math.floor((y0 + (y1 - y0) * t) / gp.tileSize);
            if ((col == startCol && row == startRow) || (col == endCol && row == endRow)) {
                continue;
            }
            if (occluder(col, row)) {
                return false;
            }
        }
        return true;
    }

    private static float sample(float[] grid, int n, double u, double v) {
        u = Math.max(0, Math.min(n - 1, u));
        v = Math.max(0, Math.min(n - 1, v));
        int x0 = (int) Math.floor(u);
        int y0 = (int) Math.floor(v);
        int x1 = Math.min(n - 1, x0 + 1);
        int y1 = Math.min(n - 1, y0 + 1);
        double fx = u - x0;
        double fy = v - y0;
        double top = grid[y0 * n + x0] * (1 - fx) + grid[y0 * n + x1] * fx;
        double bottom = grid[y1 * n + x0] * (1 - fx) + grid[y1 * n + x1] * fx;
        return (float) (top * (1 - fy) + bottom * fy);
    }

    private void drawZones(Graphics2D g, int cameraX, int cameraY) {
        TileManager map = gp.tileM;
        int size = gp.tileSize;
        int startCol = Math.max(0, cameraX / size - 1);
        int startRow = Math.max(0, cameraY / size - 1);
        int cols = Math.min(map.getWorldCols() - startCol, gp.screenWidth / size + 3);
        int rows = Math.min(map.getWorldRows() - startRow, gp.screenHeight / size + 3);
        if (cols <= 0 || rows <= 0) {
            return;
        }
        if (zoneImage == null || zoneImage.getWidth() != cols || zoneImage.getHeight() != rows) {
            zoneImage = new BufferedImage(cols, rows, BufferedImage.TYPE_INT_ARGB);
        }
        int rgb = ambient.color.getRGB() & 0xFFFFFF;
        int shade = (int) (255 * 0.14f);
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                boolean shaded = shadedByTile[map.mapTileNum[startCol + x][startRow + y]];
                zoneImage.setRGB(x, y, shaded ? (shade << 24) | rgb : 0);
            }
        }
        Object hint = g.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setComposite(AlphaComposite.SrcOver);
        int x = (startCol * size - cameraX) / DIV;
        int y = (startRow * size - cameraY) / DIV;
        g.drawImage(zoneImage, x, y, cols * size / DIV, rows * size / DIV, null);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                hint == null ? RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR : hint);
    }

    private void drawFog(Graphics2D g, int cameraX, int cameraY) {
        BufferedImage sprite = fogSprites.computeIfAbsent(ambient.fog.getRGB(), rgb -> fogSprite(rgb & 0xFFFFFF));
        drawFogLayer(g, sprite, cameraX, cameraY, 0.22, 0.05, ambient.fogAlpha);
        drawFogLayer(g, sprite, cameraX + 37, cameraY + 53, -0.14, 0.08, ambient.fogAlpha * 0.7f);
    }

    private void drawFogLayer(Graphics2D g, BufferedImage sprite, int cameraX, int cameraY, double speedX, double speedY, float alpha) {
        int step = FOG_TILE * 2;
        int ox = Math.floorMod((int) Math.round((tick * speedX - cameraX) / DIV), step);
        int oy = Math.floorMod((int) Math.round((tick * speedY - cameraY) / DIV), step);
        g.setComposite(AlphaComposite.SrcOver.derive(Math.min(1f, alpha)));
        for (int y = oy - step; y < layer.getHeight(); y += step) {
            for (int x = ox - step; x < layer.getWidth(); x += step) {
                g.drawImage(sprite, x, y, null);
            }
        }
    }

    private BufferedImage fogSprite(int rgb) {
        int size = FOG_TILE * 2;
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB_PRE);
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                float n = fogNoise[(y / 2) * FOG_TILE + x / 2];
                float f = Math.max(0f, Math.min(1f, (n - 0.3f) / 0.55f));
                int alpha = Math.round(255 * f * f * (3 - 2 * f));
                image.setRGB(x, y, (alpha << 24) | rgb);
            }
        }
        return image;
    }

    private static float[] makeFogNoise() {
        float[] result = new float[FOG_TILE * FOG_TILE];
        Random random = new Random(42);
        int[] cells = {4, 8};
        float[] weights = {0.65f, 0.35f};
        for (int octave = 0; octave < cells.length; octave++) {
            int c = cells[octave];
            float[] grid = new float[c * c];
            for (int i = 0; i < grid.length; i++) {
                grid[i] = random.nextFloat();
            }
            for (int y = 0; y < FOG_TILE; y++) {
                for (int x = 0; x < FOG_TILE; x++) {
                    double u = x * c / (double) FOG_TILE;
                    double v = y * c / (double) FOG_TILE;
                    int x0 = (int) u;
                    int y0 = (int) v;
                    double fx = smooth(u - x0);
                    double fy = smooth(v - y0);
                    float a = grid[(y0 % c) * c + x0 % c];
                    float b = grid[(y0 % c) * c + (x0 + 1) % c];
                    float d = grid[((y0 + 1) % c) * c + x0 % c];
                    float e = grid[((y0 + 1) % c) * c + (x0 + 1) % c];
                    double value = (a * (1 - fx) + b * fx) * (1 - fy) + (d * (1 - fx) + e * fx) * fy;
                    result[y * FOG_TILE + x] += (float) value * weights[octave];
                }
            }
        }
        return result;
    }

    private static double smooth(double t) {
        return t * t * (3 - 2 * t);
    }

    private static BufferedImage makeVignette(int width, int height, int rgb) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        double hw = width / 2.0;
        double hh = height / 2.0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                double dx = (x + 0.5 - hw) / hw;
                double dy = (y + 0.5 - hh) / hh;
                double d = Math.sqrt(dx * dx + dy * dy);
                double f = Math.max(0, Math.min(1, (d - 0.62) / 0.8));
                int alpha = (int) Math.round(255 * smooth(f));
                image.setRGB(x, y, (alpha << 24) | rgb);
            }
        }
        return image;
    }

    private BufferedImage hole(int radius) {
        return holes.computeIfAbsent(radius, r -> radialSprite(r, 0xFFFFFF));
    }

    private BufferedImage wash(int radius, Color color) {
        long key = ((long) radius << 32) | (color.getRGB() & 0xFFFFFFFFL);
        return washes.computeIfAbsent(key, k -> radialSprite(radius, color.getRGB() & 0xFFFFFF));
    }

    private static BufferedImage radialSprite(int radius, int rgb) {
        int size = radius * 2 + 1;
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                double d = Math.hypot(x - radius, y - radius) / radius;
                if (d >= 1) {
                    continue;
                }
                double f = 1 - d;
                int alpha = (int) Math.round(255 * f * f * (3 - 2 * f));
                image.setRGB(x, y, (alpha << 24) | rgb);
            }
        }
        return image;
    }
}
