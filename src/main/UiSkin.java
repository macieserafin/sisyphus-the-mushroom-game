package main;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

public final class UiSkin {
    public static final int P = 3;

    public static final Color CREAM = new Color(0xE8D8B0);
    public static final Color TEXT = new Color(0xEADFC8);
    public static final Color TEXT_DIM = new Color(0xA8968C);
    public static final Color TEXT_RED = new Color(0xE05A48);
    public static final Color GOLD = new Color(0xE8B860);

    private static final int OUTLINE = 0x140A0C;
    private static final int FILL = 0x2A2024;
    private static final int FILL_SHADOW = 0x1F181B;
    private static final int INSET_FILL = 0x1A1316;
    private static final int INSET_DARK = 0x0F0A0C;
    private static final int INSET_LIGHT = 0x4A383B;

    public record Palette(int light, int mid, int dark, int speck, int spot, int spotShade, int fill) {
    }

    public static final Palette RED = new Palette(0xA8302A, 0x7E1D1F, 0x521215, 0x651719, 0xE6D6B0, 0xB8A582, FILL);
    public static final Palette DIM = new Palette(0x5E4A48, 0x47373A, 0x2F2427, 0x3B2D30, 0x8A7A6C, 0x6A5C52, 0x241B1E);
    public static final Palette GLOW = new Palette(0xD04A36, 0xA8302A, 0x6E1A1B, 0x8E2422, 0xF2E6C4, 0xC8B48C, 0x3A2226);

    private static final Map<String, BufferedImage> CACHE = new HashMap<>();

    private UiSkin() {
    }

    public static void frame(Graphics2D g2, int x, int y, int w, int h) {
        int aw = art(w), ah = art(h);
        blit(g2, cached("frame", aw, ah, RED, () -> buildFrame(aw, ah, RED, 3, true)), x, y);
    }

    public static void plaque(Graphics2D g2, int x, int y, int w, int h, String text, Palette palette) {
        int aw = art(w), ah = art(h);
        blit(g2, cached("plaque", aw, ah, palette, () -> buildFrame(aw, ah, palette, 2, false)), x, y);
        if (text != null && !text.isEmpty()) {
            Color color = palette == DIM ? TEXT_DIM : TEXT;
            int scale = ah >= 9 ? 2 : 1;
            int tx = x + (aw * P - PixelFont.width(text, scale)) / 2;
            int ty = y + (ah * P - 7 * scale) / 2;
            PixelFont.draw(g2, text, tx, ty, scale, color);
        }
    }

    public static void header(Graphics2D g2, int cx, int y, String text) {
        int w = PixelFont.width(text, 2) + 36;
        w += (P - w % P) % P;
        plaque(g2, cx - w / 2, y, w, 27, text, RED);
    }

    public static void inset(Graphics2D g2, int x, int y, int w, int h) {
        int aw = art(w), ah = art(h);
        blit(g2, cached("inset", aw, ah, RED, () -> buildInset(aw, ah)), x, y);
    }

    public static void badge(Graphics2D g2, int x, int y, int diameter, Palette palette) {
        int ad = art(diameter);
        blit(g2, cached("badge", ad, ad, palette, () -> buildBadge(ad, palette)), x, y);
    }

    public static void divider(Graphics2D g2, int x, int y, int w) {
        int aw = art(w);
        blit(g2, cached("divider", aw, 3, RED, () -> buildDivider(aw)), x, y);
    }

    private static final String[] HEART = {
            ".ooo...ooo.",
            "ohhro.orrdo",
            "ohrrrorrrdo",
            "orrrrrrrrdo",
            "orrrrrrrrdo",
            ".orrrrrrdo.",
            "..orrrrdo..",
            "...orrdo...",
            "....odo....",
            ".....o.....",
    };

    public static final int HEART_W = 11 * P;
    public static final int HEART_H = 10 * P;

    private static final String[] SHIELD = {
            ".ooooooooo.",
            "ohhhhhrrrdo",
            "ohhhhrrrrdo",
            "ohhrrrrrrdo",
            "ohrrrrrrrdo",
            ".orrrrrrdo.",
            ".orrrrrrdo.",
            "..orrrrdo..",
            "...orrdo...",
            "....odo....",
            ".....o.....",
    };

    public static final int SHIELD_W = 11 * P;
    public static final int SHIELD_H = 11 * P;

    public static void shield(Graphics2D g2, int x, int y, double fill) {
        BufferedImage full = cached("shield", 1, 0, RED, () -> sprite(SHIELD, Map.of(
                'o', OUTLINE, 'h', 0xCFF4FF, 'r', 0x5AB4E0, 'd', 0x2A6A9A)));
        BufferedImage empty = cached("shield", 0, 0, RED, () -> sprite(SHIELD, Map.of(
                'o', OUTLINE, 'h', 0x3A4652, 'r', 0x2A333E, 'd', 0x1E252E)));
        Composite old = g2.getComposite();
        g2.setComposite(AlphaComposite.SrcOver.derive(0.45f));
        g2.drawImage(cached("shield", 2, 0, RED, () -> sprite(SHIELD, Map.of(
                'o', 0, 'h', 0, 'r', 0, 'd', 0))), x + P, y + P, SHIELD_W, SHIELD_H, null);
        g2.setComposite(old);
        if (fill >= 1) {
            blit(g2, full, x, y);
            return;
        }
        blit(g2, empty, x, y);
        if (fill > 0) {
            int visible = (int) Math.round(SHIELD_H * fill / P) * P;
            Shape clip = g2.getClip();
            g2.clipRect(x, y + SHIELD_H - visible, SHIELD_W, visible);
            g2.setComposite(AlphaComposite.SrcOver.derive(0.7f));
            blit(g2, full, x, y);
            g2.setComposite(old);
            g2.setClip(clip);
        }
    }

    public static void heart(Graphics2D g2, int x, int y, boolean full) {
        BufferedImage img = full
                ? cached("heart", 1, 0, RED, () -> sprite(HEART, Map.of(
                'o', OUTLINE, 'h', 0xF08C80, 'r', 0xC8282E, 'd', 0x7A1418)))
                : cached("heart", 0, 0, RED, () -> sprite(HEART, Map.of(
                'o', OUTLINE, 'h', 0x4A3034, 'r', 0x34232A, 'd', 0x261A1E)));
        Composite old = g2.getComposite();
        g2.setComposite(AlphaComposite.SrcOver.derive(0.45f));
        g2.drawImage(cached("heart", 2, 0, RED, () -> sprite(HEART, Map.of(
                'o', 0, 'h', 0, 'r', 0, 'd', 0))), x + P, y + P, HEART_W, HEART_H, null);
        g2.setComposite(old);
        blit(g2, img, x, y);
    }

    public enum Icon { SWORD, HEAVY_SWORD, PISTOL, SHOTGUN, LOCK, PLAY, OPTIONS, CLOSE, MUSHROOM, DASH, ROLL }

    private static final Map<Icon, String[]> ICONS = Map.ofEntries(
            Map.entry(Icon.DASH, new String[]{
                    "....o....",
                    "...owo...",
                    "..owwwo..",
                    ".owwowwo.",
                    "owwo.owwo",
                    "ooo.o.ooo",
                    "...owo...",
                    "..owwwo..",
                    ".owwowwo.",
                    "owwo.owwo",
                    "ooo...ooo",
            }),
            Map.entry(Icon.ROLL, new String[]{
                    "...ooooo...",
                    "..owwwwwo..",
                    ".owwoooowo.",
                    "owwowwwoowo",
                    "owowooowowo",
                    "owowowowowo",
                    "owowowwowwo",
                    "owowoooowo.",
                    ".owowwwwo..",
                    "..owoooo...",
                    "...owwwwo..",
                    "....oooo...",
            }),
            Map.entry(Icon.PLAY, new String[]{
                    "oo.......",
                    "owoo.....",
                    "owwwoo...",
                    "owwwwwoo.",
                    "owwwwwwwo",
                    "owwwwwoo.",
                    "owwwoo...",
                    "owoo.....",
                    "oo.......",
            }),
            Map.entry(Icon.OPTIONS, new String[]{
                    "....o....",
                    "...owo...",
                    "..owwwo..",
                    ".owwowwo.",
                    "owwo.owwo",
                    ".owwowwo.",
                    "..owwwo..",
                    "...owo...",
                    "....o....",
            }),
            Map.entry(Icon.CLOSE, new String[]{
                    "oo.....oo",
                    "owo...owo",
                    ".owo.owo.",
                    "..owowo..",
                    "...owo...",
                    "..owowo..",
                    ".owo.owo.",
                    "owo...owo",
                    "oo.....oo",
            }),
            Map.entry(Icon.MUSHROOM, new String[]{
                    ".....oooooo.....",
                    "...oorrrrwrroo..",
                    "..orrwwrrrrrrro.",
                    ".orrrwwrrrrwwrro",
                    "orrrrrrrrrrwwrro",
                    "orwwrrrrwwrrrrdo",
                    "orwwrrrrwwrrrddo",
                    "oddrrrrrrrrrdddo",
                    ".ooddddddddddoo.",
                    "...occcccccco...",
                    ".....osssso.....",
                    ".....osssco.....",
                    "....osssssco....",
                    "....osssssco....",
                    "...ossssssccoo..",
                    "...oooooooooo...",
            }),
            Map.entry(Icon.SWORD, new String[]{
                    "........oo",
                    ".......oso",
                    "......osmo",
                    ".....osmo.",
                    ".o..osmo..",
                    ".ogosmo...",
                    "..oggo....",
                    ".obogo....",
                    "obo.o.....",
                    "oo........",
            }),
            Map.entry(Icon.HEAVY_SWORD, new String[]{
                    ".......ooo",
                    "......ossm",
                    ".....ossmo",
                    "....ossmo.",
                    ".o.ossmo..",
                    ".ogssmo...",
                    "..oggo....",
                    ".obogo....",
                    "obo.o.....",
                    "oo........",
            }),
            Map.entry(Icon.PISTOL, new String[]{
                    "..........",
                    "..........",
                    ".ooooooooo",
                    "ossssssss.",
                    "ommmmmmmmo",
                    ".oobbooooo",
                    "..obbo.o..",
                    ".obbbooo..",
                    ".obbo.....",
                    ".ooo......",
            }),
            Map.entry(Icon.SHOTGUN, new String[]{
                    "...........",
                    "...........",
                    "...ooooooooo",
                    "oooossssssso",
                    "obbommmmmmmo",
                    "obbbboooooo.",
                    "obbbo.o.....",
                    "ooo.o.o.....",
                    "....ooo.....",
                    "...........",
            }),
            Map.entry(Icon.LOCK, new String[]{
                    "...ooo...",
                    "..o...o..",
                    "..o...o..",
                    ".ooooooo.",
                    ".ogggggo.",
                    ".oggoggo.",
                    ".ogggggo.",
                    ".ooooooo.",
            })
    );

    public static void icon(Graphics2D g2, Icon icon, int cx, int cy, int scale, boolean bright) {
        String[] rows = ICONS.get(icon);
        BufferedImage img = cached("icon" + icon, bright ? 1 : 0, 0, RED, () -> sprite(rows, bright
                ? Map.of('o', OUTLINE, 's', 0xF2EEE4, 'm', 0xA8ACB4, 'g', 0xE8B860, 'b', 0x8A5A34,
                'w', 0xEADFC8, 'r', 0xA8302A, 'd', 0x6E1A1B, 'c', 0xB8A582)
                : Map.of('o', OUTLINE, 's', 0x8A7E78, 'm', 0x5E5450, 'g', 0x7A6448, 'b', 0x4E3A2C,
                'w', 0x8A7A6C, 'r', 0x5E4A48, 'd', 0x3B2D30, 'c', 0x6A5C52)));
        int w = img.getWidth() * scale;
        int h = img.getHeight() * scale;
        g2.drawImage(img, cx - w / 2, cy - h / 2, w, h, null);
    }

    private static BufferedImage buildFrame(int aw, int ah, Palette p, int band, boolean rivets) {
        BufferedImage img = new BufferedImage(aw, ah, BufferedImage.TYPE_INT_ARGB);
        int cut = rivets ? 2 : 3;
        for (int j = 0; j < ah; j++) {
            for (int i = 0; i < aw; i++) {
                int dx = Math.min(i, aw - 1 - i);
                int dy = Math.min(j, ah - 1 - j);
                if (dx + dy < cut - 1) {
                    continue;
                }
                int d = Math.min(dx, dy);
                int c;
                if (d == 0 || dx + dy == cut - 1) {
                    c = OUTLINE;
                } else if (d <= band) {
                    boolean topLeft = j <= band || i <= band;
                    boolean bottomRight = j >= ah - 1 - band || i >= aw - 1 - band;
                    if (d == 1 && topLeft && !bottomRight) {
                        c = p.light();
                    } else if (d == band && bottomRight && !topLeft) {
                        c = p.dark();
                    } else {
                        int hash = hash(i, j);
                        c = hash % 9 == 0 ? p.speck() : hash % 41 == 0 ? p.spotShade() : p.mid();
                    }
                } else if (d == band + 1) {
                    c = OUTLINE;
                } else if (d == band + 2 && (j <= band + 2 || i <= band + 2)) {
                    c = p == RED ? FILL_SHADOW : darker(p.fill());
                } else {
                    c = p.fill();
                }
                img.setRGB(i, j, 0xFF000000 | c);
            }
        }
        if (rivets) {
            spot(img, 2, 2, p);
            spot(img, aw - 3, 2, p);
            spot(img, 2, ah - 3, p);
            spot(img, aw - 3, ah - 3, p);
            if (aw > 60) {
                stud(img, aw / 2, 1, p);
                stud(img, aw / 2, ah - 2, p);
            }
            if (ah > 60) {
                stud(img, 1, ah / 2, p);
                stud(img, aw - 2, ah / 2, p);
            }
        }
        return img;
    }

    private static void spot(BufferedImage img, int cx, int cy, Palette p) {
        for (int dy = -2; dy <= 2; dy++) {
            for (int dx = -2; dx <= 2; dx++) {
                int r2 = dx * dx + dy * dy;
                int c;
                if (r2 <= 2) {
                    c = dx == -1 && dy == -1 ? 0xFFF8E8 : dx + dy >= 1 ? p.spotShade() : p.spot();
                } else if (r2 <= 5) {
                    c = OUTLINE;
                } else {
                    continue;
                }
                put(img, cx + dx, cy + dy, c);
            }
        }
    }

    private static void stud(BufferedImage img, int cx, int cy, Palette p) {
        put(img, cx - 1, cy, p.spotShade());
        put(img, cx, cy, p.spot());
        put(img, cx + 1, cy, p.spotShade());
    }

    private static BufferedImage buildInset(int aw, int ah) {
        BufferedImage img = new BufferedImage(aw, ah, BufferedImage.TYPE_INT_ARGB);
        for (int j = 0; j < ah; j++) {
            for (int i = 0; i < aw; i++) {
                int dx = Math.min(i, aw - 1 - i);
                int dy = Math.min(j, ah - 1 - j);
                if (dx == 0 && dy == 0) {
                    continue;
                }
                int c;
                if (dx == 0 || dy == 0) {
                    c = INSET_DARK;
                } else if ((i == aw - 2 || j == ah - 2)) {
                    c = INSET_LIGHT;
                } else if (i == 1 || j == 1) {
                    c = INSET_DARK;
                } else {
                    c = INSET_FILL;
                }
                img.setRGB(i, j, 0xFF000000 | c);
            }
        }
        return img;
    }

    private static BufferedImage buildBadge(int ad, Palette p) {
        BufferedImage img = new BufferedImage(ad, ad, BufferedImage.TYPE_INT_ARGB);
        double c = (ad - 1) / 2.0;
        double r = ad / 2.0;
        for (int j = 0; j < ad; j++) {
            for (int i = 0; i < ad; i++) {
                double dist = Math.hypot(i - c, j - c);
                if (dist > r - 0.2) {
                    continue;
                }
                double edge = r - dist;
                boolean upperLeft = (i - c) + (j - c) < 0;
                int col;
                if (edge < 1.2) {
                    col = OUTLINE;
                } else if (edge < 4.2) {
                    if (edge < 2.2 && upperLeft) {
                        col = p.light();
                    } else if (edge >= 3.2 && !upperLeft) {
                        col = p.dark();
                    } else {
                        col = hash(i, j) % 7 == 0 ? p.speck() : p.mid();
                    }
                } else if (edge < 5.2) {
                    col = OUTLINE;
                } else {
                    col = upperLeft && edge < 6.2 ? darker(p.fill()) : p.fill();
                }
                img.setRGB(i, j, 0xFF000000 | col);
            }
        }
        return img;
    }

    private static BufferedImage buildDivider(int aw) {
        BufferedImage img = new BufferedImage(aw, 3, BufferedImage.TYPE_INT_ARGB);
        for (int i = 1; i < aw - 1; i++) {
            img.setRGB(i, 1, 0xFF000000 | RED.mid());
            img.setRGB(i, 2, 0xFF000000 | OUTLINE);
        }
        int m = aw / 2;
        for (int i = m - 1; i <= m + 1; i++) {
            img.setRGB(i, 0, 0xFF000000 | OUTLINE);
            img.setRGB(i, 1, 0xFF000000 | RED.spot());
        }
        return img;
    }

    private static BufferedImage sprite(String[] rows, Map<Character, Integer> colors) {
        int w = 0;
        for (String row : rows) {
            w = Math.max(w, row.length());
        }
        BufferedImage img = new BufferedImage(w, rows.length, BufferedImage.TYPE_INT_ARGB);
        for (int j = 0; j < rows.length; j++) {
            for (int i = 0; i < rows[j].length(); i++) {
                Integer c = colors.get(rows[j].charAt(i));
                if (c != null) {
                    img.setRGB(i, j, 0xFF000000 | c);
                }
            }
        }
        return img;
    }

    private interface Builder {
        BufferedImage build();
    }

    private static BufferedImage cached(String kind, int w, int h, Palette p, Builder builder) {
        String key = kind + ':' + w + 'x' + h + ':' + System.identityHashCode(p);
        BufferedImage img = CACHE.get(key);
        if (img == null) {
            img = builder.build();
            CACHE.put(key, img);
        }
        return img;
    }

    private static void blit(Graphics2D g2, BufferedImage img, int x, int y) {
        Object old = g2.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g2.drawImage(img, x, y, img.getWidth() * P, img.getHeight() * P, null);
        if (old != null) {
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, old);
        }
    }

    private static void put(BufferedImage img, int x, int y, int c) {
        if (x >= 0 && y >= 0 && x < img.getWidth() && y < img.getHeight()) {
            img.setRGB(x, y, 0xFF000000 | c);
        }
    }

    private static int art(int screenPixels) {
        return Math.max(8, (screenPixels + P - 1) / P);
    }

    private static int hash(int i, int j) {
        int h = i * 73856093 ^ j * 19349663;
        h ^= h >>> 13;
        h *= 0x5bd1e995;
        h ^= h >>> 15;
        return h & 0x7fffffff;
    }

    private static int darker(int rgb) {
        int r = (rgb >> 16 & 0xFF) * 3 / 4;
        int g = (rgb >> 8 & 0xFF) * 3 / 4;
        int b = (rgb & 0xFF) * 3 / 4;
        return r << 16 | g << 8 | b;
    }

    public static final class PixelFont {
        private static final Map<Character, String[]> GLYPHS = new HashMap<>();

        static {
            def('A', ".###.", "#...#", "#...#", "#####", "#...#", "#...#", "#...#");
            def('B', "####.", "#...#", "#...#", "####.", "#...#", "#...#", "####.");
            def('C', ".###.", "#...#", "#....", "#....", "#....", "#...#", ".###.");
            def('D', "####.", "#...#", "#...#", "#...#", "#...#", "#...#", "####.");
            def('E', "#####", "#....", "#....", "####.", "#....", "#....", "#####");
            def('F', "#####", "#....", "#....", "####.", "#....", "#....", "#....");
            def('G', ".###.", "#...#", "#....", "#.###", "#...#", "#...#", ".####");
            def('H', "#...#", "#...#", "#...#", "#####", "#...#", "#...#", "#...#");
            def('I', "#####", "..#..", "..#..", "..#..", "..#..", "..#..", "#####");
            def('J', "..###", "...#.", "...#.", "...#.", "...#.", "#..#.", ".##..");
            def('K', "#...#", "#..#.", "#.#..", "##...", "#.#..", "#..#.", "#...#");
            def('L', "#....", "#....", "#....", "#....", "#....", "#....", "#####");
            def('M', "#...#", "##.##", "#.#.#", "#.#.#", "#...#", "#...#", "#...#");
            def('N', "#...#", "##..#", "#.#.#", "#..##", "#...#", "#...#", "#...#");
            def('O', ".###.", "#...#", "#...#", "#...#", "#...#", "#...#", ".###.");
            def('P', "####.", "#...#", "#...#", "####.", "#....", "#....", "#....");
            def('Q', ".###.", "#...#", "#...#", "#...#", "#.#.#", "#..#.", ".##.#");
            def('R', "####.", "#...#", "#...#", "####.", "#.#..", "#..#.", "#...#");
            def('S', ".####", "#....", "#....", ".###.", "....#", "....#", "####.");
            def('T', "#####", "..#..", "..#..", "..#..", "..#..", "..#..", "..#..");
            def('U', "#...#", "#...#", "#...#", "#...#", "#...#", "#...#", ".###.");
            def('V', "#...#", "#...#", "#...#", "#...#", "#...#", ".#.#.", "..#..");
            def('W', "#...#", "#...#", "#...#", "#.#.#", "#.#.#", "##.##", "#...#");
            def('X', "#...#", "#...#", ".#.#.", "..#..", ".#.#.", "#...#", "#...#");
            def('Y', "#...#", "#...#", ".#.#.", "..#..", "..#..", "..#..", "..#..");
            def('Z', "#####", "....#", "...#.", "..#..", ".#...", "#....", "#####");
            def('0', ".###.", "#...#", "#..##", "#.#.#", "##..#", "#...#", ".###.");
            def('1', "..#..", ".##..", "..#..", "..#..", "..#..", "..#..", ".###.");
            def('2', ".###.", "#...#", "....#", "...#.", "..#..", ".#...", "#####");
            def('3', "####.", "....#", "....#", ".###.", "....#", "....#", "####.");
            def('4', "...#.", "..##.", ".#.#.", "#..#.", "#####", "...#.", "...#.");
            def('5', "#####", "#....", "####.", "....#", "....#", "#...#", ".###.");
            def('6', ".###.", "#....", "#....", "####.", "#...#", "#...#", ".###.");
            def('7', "#####", "....#", "...#.", "..#..", ".#...", ".#...", ".#...");
            def('8', ".###.", "#...#", "#...#", ".###.", "#...#", "#...#", ".###.");
            def('9', ".###.", "#...#", "#...#", ".####", "....#", "....#", ".###.");
            def(' ', ".....", ".....", ".....", ".....", ".....", ".....", ".....");
            def('.', ".....", ".....", ".....", ".....", ".....", ".##..", ".##..");
            def(',', ".....", ".....", ".....", ".....", ".##..", "..#..", ".#...");
            def(':', ".....", ".##..", ".##..", ".....", ".##..", ".##..", ".....");
            def('!', "..#..", "..#..", "..#..", "..#..", "..#..", ".....", "..#..");
            def('?', ".###.", "#...#", "....#", "...#.", "..#..", ".....", "..#..");
            def('-', ".....", ".....", ".....", ".###.", ".....", ".....", ".....");
            def('+', ".....", "..#..", "..#..", "#####", "..#..", "..#..", ".....");
            def('/', "....#", "....#", "...#.", "..#..", ".#...", "#....", "#....");
            def('(', "...#.", "..#..", ".#...", ".#...", ".#...", "..#..", "...#.");
            def(')', ".#...", "..#..", "...#.", "...#.", "...#.", "..#..", ".#...");
            def('[', ".###.", ".#...", ".#...", ".#...", ".#...", ".#...", ".###.");
            def(']', ".###.", "...#.", "...#.", "...#.", "...#.", "...#.", ".###.");
            def('\'', "..#..", "..#..", ".#...", ".....", ".....", ".....", ".....");
            def('>', ".#...", "..#..", "...#.", "....#", "...#.", "..#..", ".#...");
            def('<', "...#.", "..#..", ".#...", "#....", ".#...", "..#..", "...#.");
        }

        private static void def(char c, String... rows) {
            GLYPHS.put(c, rows);
        }

        private PixelFont() {
        }

        private static char normalize(char c) {
            c = Character.toUpperCase(c);
            return switch (c) {
                case 'Ą' -> 'A';
                case 'Ć' -> 'C';
                case 'Ę' -> 'E';
                case 'Ł' -> 'L';
                case 'Ń' -> 'N';
                case 'Ó' -> 'O';
                case 'Ś' -> 'S';
                case 'Ź', 'Ż' -> 'Z';
                default -> GLYPHS.containsKey(c) ? c : '?';
            };
        }

        public static int width(String text, int scale) {
            return text.isEmpty() ? 0 : (text.length() * 6 - 1) * scale;
        }

        public static void draw(Graphics2D g2, String text, int x, int y, int scale, Color color) {
            drawGlyphs(g2, text, x + scale, y + scale, scale, new Color(OUTLINE));
            drawGlyphs(g2, text, x, y, scale, color);
        }

        public static void drawOutlined(Graphics2D g2, String text, int x, int y, int scale, Color color, Color shadow) {
            drawGlyphs(g2, text, x + scale, y + scale * 2, scale, shadow);
            Color outline = new Color(OUTLINE);
            for (int dy = -1; dy <= 1; dy++) {
                for (int dx = -1; dx <= 1; dx++) {
                    if (dx != 0 || dy != 0) {
                        drawGlyphs(g2, text, x + dx * scale, y + dy * scale, scale, outline);
                    }
                }
            }
            drawGlyphs(g2, text, x, y, scale, color);
        }

        private static void drawGlyphs(Graphics2D g2, String text, int x, int y, int scale, Color color) {
            g2.setColor(color);
            for (int k = 0; k < text.length(); k++) {
                String[] rows = GLYPHS.get(normalize(text.charAt(k)));
                for (int j = 0; j < 7; j++) {
                    for (int i = 0; i < 5; i++) {
                        if (rows[j].charAt(i) == '#') {
                            g2.fillRect(x + (k * 6 + i) * scale, y + j * scale, scale, scale);
                        }
                    }
                }
            }
        }
    }
}
