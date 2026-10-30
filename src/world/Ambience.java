package world;

import main.GamePanel;
import main.VisualSettings;
import tile.Tile;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class Ambience {
    private static final int MARGIN = 80;
    private static final int MAX_PARTICLES = 200;
    private static final int MAX_EMITTED = 90;
    private static final int MAX_ACTORS = 32;
    private static final int ALPHA_LEVELS = 8;
    private static final Set<String> WATER = Set.of("Water", "Deep water", "Lily pad");
    private static final Set<String> FIRES = Set.of("Campfire", "Cave wall (torch)", "Void brazier");

    private enum Kind {
        POLLEN(false), LEAF(false), DUST(false), FIREFLY(true), SPORE(true), EMBER(true),
        SPARKLE(false), RIPPLE(false), SMOKE(false), SPARK(true);

        final boolean glows;

        Kind(boolean glows) {
            this.glows = glows;
        }

        boolean onWater() {
            return this == SPARKLE || this == RIPPLE;
        }
    }

    private static final class Profile {
        int pollen, leaves, dust, fireflies, spores, embers, sparkles, ripples;
        boolean clouds;
        Color sporeColor = Palette.GOO_LIGHT;
        float wind = 1f;
        float cloudShade = 0.16f;
    }

    private static final class Particle {
        Kind kind;
        double x, y, vx, vy;
        int age, life;
        double phase;
        int color;
    }

    private static final class Cloud {
        double x, y, speed;
        int sprite;
        float alpha;
    }

    private final GamePanel gp;
    private final Random random = new Random(11);
    private final Particle[] particles = new Particle[MAX_PARTICLES];
    private final Particle[] emitted = new Particle[MAX_EMITTED];
    private final Cloud[] clouds = new Cloud[10];
    private final BufferedImage[] cloudSprites = new BufferedImage[5];
    private final Map<Long, Color> colors = new HashMap<>();
    private final double[] actorX = new double[MAX_ACTORS];
    private final double[] actorY = new double[MAX_ACTORS];

    private Profile profile = new Profile();
    private String profileMap = "";
    private float[] swayByTile = new float[0];
    private boolean[] waterByTile = new boolean[0];
    private int emittedNext;
    private int actors;
    private int actorsTick = -1;
    private int tick;

    private static final double CLOUD_PERIOD_X = 2200;
    private static final double CLOUD_PERIOD_Y = 1500;

    public Ambience(GamePanel gp) {
        this.gp = gp;
        for (int i = 0; i < cloudSprites.length; i++) {
            cloudSprites[i] = makeCloudSprite(i);
        }
        for (int i = 0; i < clouds.length; i++) {
            Cloud c = new Cloud();
            c.x = random.nextDouble() * CLOUD_PERIOD_X;
            c.y = random.nextDouble() * CLOUD_PERIOD_Y;
            boolean streak = i >= 7;
            c.sprite = streak ? 4 : random.nextInt(4);
            c.speed = streak ? 0.9 + random.nextDouble() * 0.4 : 0.25 + random.nextDouble() * 0.2;
            c.alpha = streak ? 0.5f : 1f;
            clouds[i] = c;
        }
    }

    public boolean enabled() {
        return gp.visuals.detail() != VisualSettings.Detail.OFF;
    }

    public boolean toggle() {
        gp.visuals.setDetail(enabled() ? VisualSettings.Detail.OFF : VisualSettings.Detail.HIGH);
        return enabled();
    }

    private boolean moving() {
        return enabled() && !gp.visuals.reduceMotion();
    }

    private int scaled(int count) {
        return gp.visuals.detail() == VisualSettings.Detail.LOW ? count / 2 : count;
    }


    private double gust() {
        return 0.65 + 0.35 * Math.sin(tick * 0.0045) * Math.sin(tick * 0.0017 + 1.3);
    }

    private double sway(double phaseKey, double amplitude, double hf) {
        double wave = 0.6 * Math.sin(tick * 0.045 + phaseKey) + 0.4 * Math.sin(tick * 0.093 + phaseKey * 1.7);
        double lean = 0.55 * gust() * profile.wind;
        return amplitude * hf * (lean + wave * (0.45 + 0.55 * gust()) * profile.wind);
    }


    public void update() {
        tick++;
        updateProfile();
        if (!enabled()) {
            return;
        }
        int cameraX = gp.player.worldX - gp.player.screenX;
        int cameraY = gp.player.worldY - gp.player.screenY;
        int minX = cameraX - MARGIN, maxX = cameraX + gp.screenWidth + MARGIN;
        int minY = cameraY - MARGIN, maxY = cameraY + gp.screenHeight + MARGIN;

        int slot = 0;
        slot = fill(slot, Kind.POLLEN, scaled(profile.pollen), minX, maxX, minY, maxY);
        slot = fill(slot, Kind.LEAF, scaled(profile.leaves), minX, maxX, minY, maxY);
        slot = fill(slot, Kind.DUST, scaled(profile.dust), minX, maxX, minY, maxY);
        slot = fill(slot, Kind.FIREFLY, scaled(profile.fireflies), minX, maxX, minY, maxY);
        slot = fill(slot, Kind.SPORE, scaled(profile.spores), minX, maxX, minY, maxY);
        slot = fill(slot, Kind.EMBER, scaled(profile.embers), minX, maxX, minY, maxY);
        slot = fill(slot, Kind.SPARKLE, scaled(profile.sparkles), minX, maxX, minY, maxY);
        slot = fill(slot, Kind.RIPPLE, scaled(profile.ripples), minX, maxX, minY, maxY);
        for (int i = slot; i < particles.length; i++) {
            particles[i] = null;
        }
        updateEmitted(cameraX, cameraY);

        double wind = gust() * profile.wind;
        for (int i = 0; i < slot; i++) {
            Particle p = particles[i];
            p.age++;
            p.phase += 0.05;
            switch (p.kind) {
                case POLLEN -> {
                    p.x += wind * 0.35 + Math.sin(p.phase) * 0.25;
                    p.y += -0.08 + Math.cos(p.phase * 0.8) * 0.2;
                }
                case LEAF -> {
                    p.x += wind * 1.5 + Math.sin(p.phase * 1.3) * 0.7;
                    p.y += 0.45 + Math.cos(p.phase * 1.6) * 0.5;
                }
                case DUST -> {
                    p.x += Math.sin(p.phase * 0.7) * 0.15 + wind * 0.1;
                    p.y += Math.cos(p.phase * 0.6) * 0.12 - 0.03;
                }
                case FIREFLY -> {
                    p.vx += (random.nextDouble() - 0.5) * 0.12;
                    p.vy += (random.nextDouble() - 0.5) * 0.12;
                    p.vx *= 0.96;
                    p.vy *= 0.96;
                    p.x += p.vx;
                    p.y += p.vy;
                }
                case SPORE -> {
                    p.x += Math.sin(p.phase * 0.9) * 0.2;
                    p.y += -0.22;
                }
                case EMBER -> {
                    p.x += Math.sin(p.phase * 1.4) * 0.45 + wind * 0.2;
                    p.y += -0.55;
                }
                default -> {
                }
            }
            if (!p.kind.onWater() && solidAt(p.x, p.y)) {
                p.age = p.life;
            }
        }
    }

    private void updateEmitted(int cameraX, int cameraY) {
        double wind = gust() * profile.wind;
        for (Particle p : emitted) {
            if (p == null || p.age >= p.life) {
                continue;
            }
            p.age++;
            p.phase += 0.08;
            if (p.kind == Kind.SMOKE) {
                p.x += wind * 0.3 + Math.sin(p.phase) * 0.2;
                p.y -= 0.35;
            } else {
                p.x += Math.sin(p.phase * 2) * 0.5 + wind * 0.2;
                p.y -= 1.1;
            }
        }
        if (tick % 5 != 0) {
            return;
        }
        int size = gp.tileSize;
        int startCol = Math.max(0, cameraX / size - 1);
        int endCol = Math.min(gp.tileM.getWorldCols() - 1, (cameraX + gp.screenWidth) / size + 1);
        int startRow = Math.max(0, cameraY / size - 1);
        int endRow = Math.min(gp.tileM.getWorldRows() - 1, (cameraY + gp.screenHeight) / size + 1);
        double chance = gp.visuals.detail() == VisualSettings.Detail.LOW ? 0.5 : 1.0;
        for (int row = startRow; row <= endRow; row++) {
            for (int col = startCol; col <= endCol; col++) {
                if (!isFire(col, row)) {
                    continue;
                }
                double x = (col + 0.5) * size + (random.nextDouble() - 0.5) * 16;
                double y = (row + 0.3) * size;
                if (random.nextDouble() < 0.55 * chance) {
                    spawnEmitted(Kind.SMOKE, x, y, 90 + random.nextInt(50),
                            profileMap.contains("byt") || profileMap.contains("void") ? 0x5A4A6E : 0x5C5652);
                }
                if (random.nextDouble() < 0.3 * chance) {
                    spawnEmitted(Kind.SPARK, x, y, 24 + random.nextInt(18),
                            random.nextBoolean() ? Palette.FIRE.getRGB() & 0xFFFFFF : Palette.EMBER.getRGB() & 0xFFFFFF);
                }
            }
        }
    }

    private boolean isFire(int col, int row) {
        Tile ground = gp.tileM.tile[gp.tileM.mapTileNum[col][row]];
        if (FIRES.contains(ground.name)) {
            return true;
        }
        int object = gp.tileM.objectTileNum[col][row];
        if (object < 0) {
            return false;
        }
        Tile t = gp.tileM.tile[object];
        return FIRES.contains(t.name)
                || (("fireplace".equals(t.group) || "forge".equals(t.group)) && t.groupY == 0);
    }

    private void spawnEmitted(Kind kind, double x, double y, int life, int color) {
        Particle p = emitted[emittedNext];
        if (p == null) {
            p = new Particle();
            emitted[emittedNext] = p;
        }
        emittedNext = (emittedNext + 1) % emitted.length;
        p.kind = kind;
        p.x = x;
        p.y = y;
        p.age = 0;
        p.life = life;
        p.phase = random.nextDouble() * 6.28;
        p.color = color;
    }

    private boolean waterAt(double x, double y) {
        int col = (int) Math.floor(x / gp.tileSize);
        int row = (int) Math.floor(y / gp.tileSize);
        if (col < 0 || row < 0 || col >= gp.tileM.getWorldCols() || row >= gp.tileM.getWorldRows()) {
            return false;
        }
        return waterByTile[gp.tileM.mapTileNum[col][row]];
    }

    private boolean solidAt(double x, double y) {
        int col = (int) Math.floor(x / gp.tileSize);
        int row = (int) Math.floor(y / gp.tileSize);
        if (col < 0 || row < 0 || col >= gp.tileM.getWorldCols() || row >= gp.tileM.getWorldRows()) {
            return true;
        }
        return gp.tileM.isCollision(col, row);
    }

    private int fill(int slot, Kind kind, int want, int minX, int maxX, int minY, int maxY) {
        for (int n = 0; n < want && slot < particles.length; n++, slot++) {
            Particle p = particles[slot];
            boolean stale = p == null || p.kind != kind || p.age >= p.life
                    || p.x < minX || p.x > maxX || p.y < minY || p.y > maxY;
            if (stale) {
                p = respawn(p, kind, minX, maxX, minY, maxY);
                particles[slot] = p;
            }
        }
        return slot;
    }

    private Particle respawn(Particle old, Kind kind, int minX, int maxX, int minY, int maxY) {
        Particle p = old != null ? old : new Particle();
        p.kind = kind;
        boolean placed = false;
        for (int attempt = 0; attempt < 10 && !placed; attempt++) {
            p.x = minX + random.nextDouble() * (maxX - minX);
            p.y = minY + random.nextDouble() * (maxY - minY);
            placed = kind.onWater() ? waterAt(p.x, p.y) : !solidAt(p.x, p.y);
        }
        p.vx = 0;
        p.vy = 0;
        p.age = 0;
        p.phase = random.nextDouble() * 6.28;
        if (!placed && kind.onWater()) {
            p.life = 1;
            p.age = 1;
            return p;
        }
        switch (kind) {
            case SPARKLE -> {
                p.life = 40 + random.nextInt(40);
                p.color = 0xD8F0FF;
            }
            case RIPPLE -> {
                p.life = 70 + random.nextInt(40);
                p.color = 0xA8D8F0;
            }
            case POLLEN -> {
                p.life = 360 + random.nextInt(300);
                p.color = 0xF4E9A8;
            }
            case LEAF -> {
                p.life = 300 + random.nextInt(200);
                int[] leafColors = {0x4F8A3C, 0x7C9A3A, 0xA06B2C};
                p.color = leafColors[random.nextInt(leafColors.length)];
            }
            case DUST -> {
                p.life = 420 + random.nextInt(300);
                p.color = 0xB9B09C;
            }
            case FIREFLY -> {
                p.life = 500 + random.nextInt(400);
                p.color = Palette.FIREFLY.getRGB() & 0xFFFFFF;
            }
            case SPORE -> {
                p.life = 400 + random.nextInt(300);
                p.color = profile.sporeColor.getRGB() & 0xFFFFFF;
            }
            case EMBER -> {
                p.life = 260 + random.nextInt(200);
                p.color = random.nextBoolean() ? Palette.FIRE.getRGB() & 0xFFFFFF : Palette.VOID.getRGB() & 0xFFFFFF;
                if (profileMap.contains("byt_arena") || profileMap.contains("prologue_void")) {
                    p.color = random.nextBoolean() ? Palette.VOID.getRGB() & 0xFFFFFF : Palette.PALE.getRGB() & 0xFFFFFF;
                }
            }
        }
        return p;
    }

    private void updateProfile() {
        String path = gp.tileM.currentMapPath();
        if (path.equals(profileMap)) {
            return;
        }
        profileMap = path;
        profile = profileFor(path);
        swayByTile = swayFor(gp.tileM.tile);
        waterByTile = new boolean[gp.tileM.tile.length];
        for (int id = 0; id < waterByTile.length; id++) {
            waterByTile[id] = WATER.contains(gp.tileM.tile[id].name);
        }
        for (int i = 0; i < particles.length; i++) {
            particles[i] = null;
        }
        for (int i = 0; i < emitted.length; i++) {
            emitted[i] = null;
        }
    }

    private static Profile profileFor(String path) {
        Profile p = new Profile();
        if (path.contains("village")) {
            p.pollen = 26;
            p.leaves = 8;
            p.fireflies = 14;
            p.sparkles = 16;
            p.ripples = 5;
            p.clouds = true;
            p.cloudShade = 0.24f;
        } else if (path.contains("level1_forest")) {
            p.pollen = 30;
            p.leaves = 10;
            p.fireflies = 10;
            p.sparkles = 20;
            p.ripples = 6;
            p.clouds = true;
            p.cloudShade = 0.26f;
        } else if (path.contains("boss_slime_arena")) {
            p.dust = 24;
            p.spores = 14;
            p.sporeColor = Palette.GOO_LIGHT;
            p.wind = 0.3f;
        } else if (path.contains("byt_arena")) {
            p.dust = 12;
            p.embers = 22;
            p.wind = 0.3f;
        } else if (path.contains("prologue_void")) {
            p.pollen = 12;
            p.fireflies = 12;
            p.spores = 10;
            p.sparkles = 10;
            p.ripples = 4;
            p.sporeColor = Palette.TEAL;
        } else if (path.contains("interior_")) {
            p.dust = 12;
            p.wind = 0.1f;
        }
        return p;
    }

    private static float[] swayFor(Tile[] tiles) {
        Map<String, Float> byName = new HashMap<>();
        byName.put("Flower patch", 2.0f);
        byName.put("Reeds", 3.5f);
        byName.put("Bush", 2.0f);
        byName.put("Berry bush", 1.8f);
        byName.put("Fern", 2.5f);
        byName.put("Wide fern", 2.5f);
        byName.put("Grass tuft", 2.5f);
        byName.put("Purple flowers", 2.5f);
        byName.put("White flowers", 2.5f);
        byName.put("Glow bells", 1.6f);
        byName.put("Mushroom crop", 1.0f);
        byName.put("Mushroom crop (violet)", 1.0f);
        float[] result = new float[tiles.length];
        for (int id = 0; id < tiles.length; id++) {
            Tile tile = tiles[id];
            if (!tile.objectLayer) {
                continue;
            }
            Float amp = byName.get(tile.name);
            if (amp != null) {
                result[id] = amp;
            }
            if ("forest_tree".equals(tile.group) || "forest_tree_dark".equals(tile.group)) {
                result[id] = 3.5f;
            } else if ("pine".equals(tile.group)) {
                result[id] = 2.5f;
            } else if ("oak".equals(tile.group)) {
                result[id] = 3.0f;
            }
        }
        return result;
    }


    public void drawTile(Graphics2D g2, BufferedImage image, int tileId, Tile tile, int col, int row, int screenX, int screenY) {
        float amplitude = moving() && tileId < swayByTile.length ? swayByTile[tileId] : 0f;
        if (amplitude <= 0f) {
            g2.drawImage(image, screenX, screenY, gp.tileSize, gp.tileSize, null);
            return;
        }
        boolean grouped = tile.group != null;
        int groupH = grouped ? Math.max(1, tile.groupHeight) : 1;
        int groupY = grouped ? tile.groupY : 0;
        int originCol = col - (grouped ? tile.groupX : 0);
        int originRow = row - groupY;
        double phaseKey = originCol * 0.9 + originRow * 0.6;
        double push = grouped ? 0 : pushFrom(col, row);

        int rows = image.getHeight();
        int stripHeight = gp.tileSize / rows;
        int runStart = 0;
        int runOffset = offsetFor(phaseKey, amplitude, push, groupH, groupY, 0, rows);
        for (int r = 1; r <= rows; r++) {
            int offset = r < rows ? offsetFor(phaseKey, amplitude, push, groupH, groupY, r, rows) : Integer.MIN_VALUE;
            if (offset != runOffset) {
                g2.drawImage(image,
                        screenX + runOffset, screenY + runStart * stripHeight,
                        screenX + runOffset + gp.tileSize, screenY + r * stripHeight,
                        0, runStart, image.getWidth(), r, null);
                runStart = r;
                runOffset = offset;
            }
        }
    }

    private int offsetFor(double phaseKey, float amplitude, double push, int groupH, int groupY, int strip, int rows) {
        double cellsAboveRoots = (groupH - groupY) - (strip + 0.5) / rows;
        double hf = Math.max(0, Math.min(1, cellsAboveRoots / groupH));
        return (int) Math.round(sway(phaseKey, amplitude, hf * hf) + push * hf);
    }

    private double pushFrom(int col, int row) {
        if (actorsTick != tick) {
            actorsTick = tick;
            actors = 0;
            addActor(gp.player.worldX + gp.tileSize / 2.0, gp.player.worldY + gp.tileSize);
            for (entity.enemy.Enemy enemy : gp.entities.enemies()) {
                if (enemy.alive && actors < MAX_ACTORS) {
                    Rectangle box = enemy.getWorldSolidArea();
                    addActor(box.getCenterX(), box.getMaxY());
                }
            }
        }
        double tileX = (col + 0.5) * gp.tileSize;
        double tileBase = (row + 1) * gp.tileSize;
        double push = 0;
        for (int i = 0; i < actors; i++) {
            double dx = tileX - actorX[i];
            double dy = tileBase - actorY[i];
            if (Math.abs(dx) < 44 && Math.abs(dy) < 36) {
                push += Math.signum(dx == 0 ? 1 : dx) * (1 - Math.abs(dx) / 44) * (1 - Math.abs(dy) / 36) * 6;
            }
        }
        return push;
    }

    private void addActor(double x, double y) {
        actorX[actors] = x;
        actorY[actors] = y;
        actors++;
    }

    public boolean hasShadows() {
        return moving() && profile.clouds;
    }

    public void drawShadows(Graphics2D g, int cameraX, int cameraY, int div) {
        if (!hasShadows()) {
            return;
        }
        for (Cloud c : clouds) {
            BufferedImage sprite = cloudSprites[c.sprite];
            double x = c.x + tick * c.speed * profile.wind;
            int sx = (int) Math.floorMod(Math.round(x - cameraX), (long) CLOUD_PERIOD_X) - sprite.getWidth() * div;
            int sy = (int) Math.floorMod(Math.round(c.y - cameraY), (long) CLOUD_PERIOD_Y) - sprite.getHeight() * div;
            if (sx >= gp.screenWidth || sy >= gp.screenHeight
                    || sx + sprite.getWidth() * div <= 0 || sy + sprite.getHeight() * div <= 0) {
                continue;
            }
            g.setComposite(AlphaComposite.SrcOver.derive(Math.min(1f, profile.cloudShade * c.alpha)));
            g.drawImage(sprite, sx / div, sy / div, null);
        }
    }

    private static BufferedImage makeCloudSprite(int variant) {
        Random r = new Random(100 + variant);
        boolean streak = variant == 4;
        int w = streak ? 340 : 300;
        int h = streak ? 34 : 180;
        int blobs = streak ? 3 : 5;
        double[][] e = new double[blobs][4];
        for (int i = 0; i < blobs; i++) {
            e[i][0] = w * (0.25 + 0.5 * r.nextDouble());
            e[i][1] = h * (0.35 + 0.3 * r.nextDouble());
            e[i][2] = w * (streak ? 0.3 : 0.18 + 0.12 * r.nextDouble());
            e[i][3] = h * (streak ? 0.4 : 0.25 + 0.2 * r.nextDouble());
        }
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                double density = 0;
                for (double[] b : e) {
                    double dx = (x - b[0]) / b[2];
                    double dy = (y - b[1]) / b[3];
                    density = Math.max(density, 1 - Math.sqrt(dx * dx + dy * dy));
                }
                if (density <= 0) {
                    continue;
                }
                double f = Math.min(1, density * 1.6);
                int alpha = (int) Math.round(255 * f * f * (3 - 2 * f));
                image.setRGB(x, y, (alpha << 24) | 0x0B1A24);
            }
        }
        return image;
    }


    public void drawLow(Graphics2D g2) {
        drawParticles(g2, false);
    }

    public void drawGlow(Graphics2D g2) {
        drawParticles(g2, true);
    }

    public void addLights(Lighting lighting) {
        if (!enabled()) {
            return;
        }
        int lit = 0;
        for (Particle p : particles) {
            if (p != null && p.kind == Kind.FIREFLY && lit < 8) {
                lighting.add(p.x, p.y, 44, Palette.FIREFLY, 0.4f * twinkle(p), 0f);
                lit++;
            }
        }
    }

    private float twinkle(Particle p) {
        double fade = Math.sin(Math.PI * Math.min(1.0, p.age / (double) p.life));
        double blink = p.kind == Kind.FIREFLY ? 0.55 + 0.45 * Math.sin(p.phase * 2.2) : 0.7 + 0.3 * Math.sin(p.phase * 3);
        return (float) Math.max(0, fade * blink);
    }

    private void drawParticles(Graphics2D g2, boolean glowing) {
        if (!enabled()) {
            return;
        }
        drawSet(g2, particles, glowing);
        drawSet(g2, emitted, glowing);
    }

    private void drawSet(Graphics2D g2, Particle[] set, boolean glowing) {
        int cameraX = gp.player.worldX - gp.player.screenX;
        int cameraY = gp.player.worldY - gp.player.screenY;
        int s = gp.scale;
        for (Particle p : set) {
            if (p == null || p.kind.glows != glowing || p.age >= p.life) {
                continue;
            }
            int sx = (int) Math.round(p.x - cameraX);
            int sy = (int) Math.round(p.y - cameraY);
            if (sx < -10 || sy < -10 || sx > gp.screenWidth + 10 || sy > gp.screenHeight + 10) {
                continue;
            }
            float level = twinkle(p) * baseAlpha(p.kind);
            int step = Math.round(level * ALPHA_LEVELS);
            if (step <= 0) {
                continue;
            }
            g2.setColor(color(p.color, step));
            switch (p.kind) {
                case LEAF -> g2.fillRect(sx, sy, s * 2, s);
                case SMOKE -> {
                    int size = s * (1 + (int) (2.0 * p.age / p.life));
                    g2.fillRect(sx - size / 2, sy - size / 2, size, size);
                }
                case RIPPLE -> {
                    double t = p.age / (double) p.life;
                    int rx = (int) Math.round(s * (1 + 5 * t)) / s * s;
                    int ry = Math.max(s, rx / 2 / s * s);
                    g2.fillRect(sx - rx, sy, s, s);
                    g2.fillRect(sx + rx - s, sy, s, s);
                    g2.fillRect(sx - rx / 2, sy - ry, rx, s);
                    g2.fillRect(sx - rx / 2, sy + ry, rx, s);
                }
                case SPARKLE -> {
                    g2.fillRect(sx, sy, s, s);
                    if (step > ALPHA_LEVELS / 2) {
                        g2.setColor(color(p.color, step / 2));
                        g2.fillRect(sx - s, sy, s * 3, s);
                    }
                }
                case FIREFLY, EMBER, SPARK -> {
                    g2.fillRect(sx, sy, s, s);
                    g2.setColor(color(p.color, Math.max(1, step / 3)));
                    g2.fillRect(sx - s, sy, s * 3, s);
                    g2.fillRect(sx, sy - s, s, s * 3);
                }
                default -> g2.fillRect(sx, sy, s, s);
            }
        }
    }

    private static float baseAlpha(Kind kind) {
        return switch (kind) {
            case POLLEN -> 0.6f;
            case LEAF -> 0.9f;
            case DUST -> 0.35f;
            case FIREFLY -> 1f;
            case SPORE -> 0.8f;
            case EMBER -> 0.9f;
            case SPARKLE -> 0.9f;
            case RIPPLE -> 0.45f;
            case SMOKE -> 0.35f;
            case SPARK -> 1f;
        };
    }

    private Color color(int rgb, int step) {
        long key = ((long) rgb << 8) | step;
        return colors.computeIfAbsent(key, k -> new Color((rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255,
                Math.min(255, step * 255 / ALPHA_LEVELS)));
    }
}
