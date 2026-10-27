package tile;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/**
 * Soft terrain transitions: when a neighbouring tile has a higher blend priority,
 * a jagged strip of its ground texture is drawn over this tile's edge or corner.
 */
class TileEdges {
    private static final int SIZE = 16;
    // How far (in source pixels) the spilled edge reaches at each position along the side.
    private static final int[] JAG = {3, 2, 3, 4, 3, 2, 2, 3, 4, 3, 2, 3, 3, 4, 2, 3};
    private static final float TIP_SHADE = 0.78f;

    private static final int NORTH = 0, EAST = 1, SOUTH = 2, WEST = 3;
    private static final int NORTH_EAST = 4, SOUTH_EAST = 5, SOUTH_WEST = 6, NORTH_WEST = 7;
    private static final int[][] OFFSETS = {
            {0, -1}, {1, 0}, {0, 1}, {-1, 0},
            {1, -1}, {1, 1}, {-1, 1}, {-1, -1}
    };

    private final Tile[] tiles;
    // blend source tile id -> one overlay per direction
    private final Map<Integer, BufferedImage[]> overlays = new HashMap<>();
    private final int[] pendingDirections = new int[8];
    private final int[] pendingPriorities = new int[8];

    TileEdges(Tile[] tiles) {
        this.tiles = tiles;
        for (Tile tile : tiles) {
            if (tile.blendPriority >= 0 && !overlays.containsKey(tile.blendSource)) {
                overlays.put(tile.blendSource, buildOverlays(tiles[tile.blendSource].image));
            }
        }
    }

    void drawEdges(Graphics2D g2, TileManager map, int col, int row, int screenX, int screenY, int tileSize) {
        Tile self = tiles[map.mapTileNum[col][row]];
        if (self.blendPriority < 0) {
            return;
        }

        int count = 0;
        for (int dir = 0; dir < 8; dir++) {
            Tile neighbour = neighbour(map, col, row, dir);
            if (neighbour == null || neighbour.blendPriority <= self.blendPriority) {
                continue;
            }
            // A corner only needs its own blob when neither adjacent side already spills the same terrain.
            if (dir >= NORTH_EAST && coveredBySides(map, col, row, dir, neighbour.blendPriority)) {
                continue;
            }
            pendingDirections[count] = dir;
            pendingPriorities[count] = neighbour.blendPriority;
            count++;
        }

        // Lower priorities first so the highest terrain ends up on top.
        for (int i = 1; i < count; i++) {
            for (int j = i; j > 0 && pendingPriorities[j - 1] > pendingPriorities[j]; j--) {
                swap(j, j - 1);
            }
        }

        for (int i = 0; i < count; i++) {
            int dir = pendingDirections[i];
            Tile neighbour = neighbour(map, col, row, dir);
            BufferedImage overlay = overlays.get(neighbour.blendSource)[dir];
            g2.drawImage(overlay, screenX, screenY, tileSize, tileSize, null);
        }
    }

    private boolean coveredBySides(TileManager map, int col, int row, int corner, int priority) {
        int sideA;
        int sideB;
        switch (corner) {
            case NORTH_EAST: sideA = NORTH; sideB = EAST; break;
            case SOUTH_EAST: sideA = SOUTH; sideB = EAST; break;
            case SOUTH_WEST: sideA = SOUTH; sideB = WEST; break;
            default: sideA = NORTH; sideB = WEST; break;
        }
        return spillsAtLeast(neighbour(map, col, row, sideA), priority) ||
                spillsAtLeast(neighbour(map, col, row, sideB), priority);
    }

    private static boolean spillsAtLeast(Tile tile, int priority) {
        return tile != null && tile.blendPriority >= priority;
    }

    private Tile neighbour(TileManager map, int col, int row, int dir) {
        int c = col + OFFSETS[dir][0];
        int r = row + OFFSETS[dir][1];
        if (c < 0 || r < 0 || c >= map.getWorldCols() || r >= map.getWorldRows()) {
            return null;
        }
        return tiles[map.mapTileNum[c][r]];
    }

    private void swap(int a, int b) {
        int dir = pendingDirections[a];
        pendingDirections[a] = pendingDirections[b];
        pendingDirections[b] = dir;
        int priority = pendingPriorities[a];
        pendingPriorities[a] = pendingPriorities[b];
        pendingPriorities[b] = priority;
    }

    private static BufferedImage[] buildOverlays(BufferedImage texture) {
        BufferedImage[] result = new BufferedImage[8];
        for (int dir = 0; dir < 8; dir++) {
            boolean[][] mask = new boolean[SIZE][SIZE];
            for (int y = 0; y < SIZE; y++) {
                for (int x = 0; x < SIZE; x++) {
                    mask[x][y] = inMask(dir, x, y);
                }
            }

            BufferedImage overlay = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < SIZE; y++) {
                for (int x = 0; x < SIZE; x++) {
                    if (!mask[x][y]) {
                        continue;
                    }
                    int rgb = texture.getRGB(x, y);
                    if (isTip(mask, x, y)) {
                        rgb = shade(rgb, TIP_SHADE);
                    }
                    overlay.setRGB(x, y, 0xFF000000 | (rgb & 0xFFFFFF));
                }
            }
            result[dir] = overlay;
        }
        return result;
    }

    private static boolean inMask(int dir, int x, int y) {
        int fromRight = SIZE - 1 - x;
        int fromBottom = SIZE - 1 - y;
        switch (dir) {
            case NORTH: return y < JAG[x];
            case SOUTH: return fromBottom < JAG[x];
            case WEST: return x < JAG[y];
            case EAST: return fromRight < JAG[y];
            case NORTH_EAST: return fromRight * fromRight + y * y <= 10;
            case SOUTH_EAST: return fromRight * fromRight + fromBottom * fromBottom <= 10;
            case SOUTH_WEST: return x * x + fromBottom * fromBottom <= 10;
            default: return x * x + y * y <= 10;
        }
    }

    // Pixels on the inner boundary of the spilled strip get a darker tint, which reads as a soft lip.
    private static boolean isTip(boolean[][] mask, int x, int y) {
        int[][] around = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] d : around) {
            int nx = x + d[0];
            int ny = y + d[1];
            if (nx >= 0 && ny >= 0 && nx < SIZE && ny < SIZE && !mask[nx][ny]) {
                return true;
            }
        }
        return false;
    }

    private static int shade(int rgb, float factor) {
        int r = (int) (((rgb >> 16) & 255) * factor);
        int g = (int) (((rgb >> 8) & 255) * factor);
        int b = (int) ((rgb & 255) * factor);
        return (r << 16) | (g << 8) | b;
    }
}
