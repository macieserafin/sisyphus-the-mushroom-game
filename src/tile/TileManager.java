package tile;

import entity.enemy.Enemy;
import main.GamePanel;

import java.awt.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.io.InputStream;
import java.util.Objects;

public class TileManager {
    GamePanel gp;
    public Tile[] tile;
    // Both layers are indexed [col][row]. The object layer uses MapFile.EMPTY for empty cells.
    public int[][] mapTileNum;
    public int[][] objectTileNum;
    private int worldCols;
    private int worldRows;
    private final TileEdges edges;
    private int animationTick = 0;
    private String currentMapPath = "";
    private final boolean[] overhead;

    public TileManager(GamePanel gp) {
        this.gp = gp;

        mapTileNum = new int[0][0];
        objectTileNum = new int[0][0];
        worldCols = 0;
        worldRows = 0;

        tile = TileSet.loadTiles();
        edges = new TileEdges(tile);
        overhead = findOverheadTiles(tile);
    }

    private static boolean[] findOverheadTiles(Tile[] tiles) {
        Map<String, Integer> firstSolidRow = new HashMap<>();
        for (Tile t : tiles) {
            if (t.objectLayer && t.group != null && t.collision) {
                firstSolidRow.merge(t.group, t.groupY, Math::min);
            }
        }
        boolean[] result = new boolean[tiles.length];
        for (int id = 0; id < tiles.length; id++) {
            Tile t = tiles[id];
            Integer solid = t.objectLayer && t.group != null ? firstSolidRow.get(t.group) : null;
            result[id] = solid != null && t.groupY < solid;
        }
        return result;
    }

    public void loadMap(String filePath){
        try {
            InputStream groundStream = Objects.requireNonNull(
                    getClass().getClassLoader().getResourceAsStream(filePath),
                    "Missing map file: " + filePath
            );
            int[][] ground = MapFile.read(groundStream, tile.length, false, filePath);

            int rows = ground.length;
            int cols = ground[0].length;
            int[][] objects = null;

            String objectPath = MapFile.objectLayerPath(filePath);
            InputStream objectStream = getClass().getClassLoader().getResourceAsStream(objectPath);
            if (objectStream != null) {
                objects = MapFile.read(objectStream, tile.length, true, objectPath);
                if (objects.length != rows || objects[0].length != cols) {
                    throw new IllegalArgumentException("Object layer " + objectPath + " must be " + cols + "x" + rows);
                }
            }

            int[][] nextGround = new int[cols][rows];
            int[][] nextObjects = new int[cols][rows];
            for (int row = 0; row < rows; row++) {
                for (int col = 0; col < cols; col++) {
                    nextGround[col][row] = ground[row][col];
                    nextObjects[col][row] = objects == null ? MapFile.EMPTY : objects[row][col];
                }
            }

            mapTileNum = nextGround;
            objectTileNum = nextObjects;
            worldCols = cols;
            worldRows = rows;
            currentMapPath = filePath;
        } catch (IOException | RuntimeException e) {
            throw new IllegalStateException("Could not load map '" + filePath + "': " + e.getMessage(), e);
        }
    }

    public String currentMapPath() {
        return currentMapPath;
    }

    public int getWorldCols() {
        return worldCols;
    }

    public int getWorldRows() {
        return worldRows;
    }

    public boolean isCollision(int col, int row) {
        if (tile[mapTileNum[col][row]].collision) {
            return true;
        }
        int object = objectTileNum[col][row];
        return object != MapFile.EMPTY && tile[object].collision;
    }

    public void draw(Graphics2D g2) {
        animationTick++;

        int startCol = Math.max(0, (gp.player.worldX - gp.player.screenX) / gp.tileSize - 1);
        int endCol = Math.min(worldCols - 1, (gp.player.worldX + gp.screenWidth - gp.player.screenX) / gp.tileSize + 1);
        int startRow = Math.max(0, (gp.player.worldY - gp.player.screenY) / gp.tileSize - 1);
        int endRow = Math.min(worldRows - 1, (gp.player.worldY + gp.screenHeight - gp.player.screenY) / gp.tileSize + 1);

        for (int worldRow = startRow; worldRow <= endRow; worldRow++) {
            for (int worldCol = startCol; worldCol <= endCol; worldCol++) {
                int screenX = worldCol * gp.tileSize - gp.player.worldX + gp.player.screenX;
                int screenY = worldRow * gp.tileSize - gp.player.worldY + gp.player.screenY;

                g2.drawImage(tile[mapTileNum[worldCol][worldRow]].frame(animationTick), screenX, screenY, gp.tileSize, gp.tileSize, null);
                edges.drawEdges(g2, this, worldCol, worldRow, screenX, screenY, gp.tileSize);
            }
        }

        for (int worldRow = startRow; worldRow <= endRow; worldRow++) {
            for (int worldCol = startCol; worldCol <= endCol; worldCol++) {
                int object = objectTileNum[worldCol][worldRow];
                if (object == MapFile.EMPTY || overhead[object]) {
                    continue;
                }
                int screenX = worldCol * gp.tileSize - gp.player.worldX + gp.player.screenX;
                int screenY = worldRow * gp.tileSize - gp.player.worldY + gp.player.screenY;
                gp.ambience.drawTile(g2, tile[object].frame(animationTick), object, tile[object], worldCol, worldRow, screenX, screenY);
            }
        }
    }

    public void drawOverhead(Graphics2D g2) {
        int startCol = Math.max(0, (gp.player.worldX - gp.player.screenX) / gp.tileSize - 1);
        int endCol = Math.min(worldCols - 1, (gp.player.worldX + gp.screenWidth - gp.player.screenX) / gp.tileSize + 1);
        int startRow = Math.max(0, (gp.player.worldY - gp.player.screenY) / gp.tileSize - 1);
        int endRow = Math.min(worldRows - 1, (gp.player.worldY + gp.screenHeight - gp.player.screenY) / gp.tileSize + 1);

        Composite old = g2.getComposite();
        for (int worldRow = startRow; worldRow <= endRow; worldRow++) {
            for (int worldCol = startCol; worldCol <= endCol; worldCol++) {
                int object = objectTileNum[worldCol][worldRow];
                if (object == MapFile.EMPTY || !overhead[object]) {
                    continue;
                }
                int screenX = worldCol * gp.tileSize - gp.player.worldX + gp.player.screenX;
                int screenY = worldRow * gp.tileSize - gp.player.worldY + gp.player.screenY;
                g2.setComposite(AlphaComposite.SrcOver.derive(overheadOpacity(screenX, screenY)));
                gp.ambience.drawTile(g2, tile[object].frame(animationTick), object, tile[object], worldCol, worldRow, screenX, screenY);
            }
        }
        g2.setComposite(old);
    }

    private float overheadOpacity(int screenX, int screenY) {
        int size = gp.tileSize;
        double distance = gapTo(screenX, screenY, gp.player.screenX, gp.player.screenY, size, size);
        int cameraX = gp.player.worldX - gp.player.screenX;
        int cameraY = gp.player.worldY - gp.player.screenY;
        for (Enemy enemy : gp.entities.enemies()) {
            if (enemy.alive && distance > 0) {
                Rectangle box = enemy.getWorldHitBox();
                distance = Math.min(distance, gapTo(screenX, screenY, box.x - cameraX, box.y - cameraY, box.width, box.height));
            }
        }
        return (float) (0.35 + 0.65 * Math.min(1.0, distance / 60.0));
    }

    private double gapTo(int tileX, int tileY, int x, int y, int width, int height) {
        int size = gp.tileSize;
        int dx = Math.max(0, Math.max(tileX + 6 - (x + width), x - (tileX + size - 6)));
        int dy = Math.max(0, Math.max(tileY + 6 - (y + height), y - (tileY + size - 6)));
        return Math.hypot(dx, dy);
    }
}
