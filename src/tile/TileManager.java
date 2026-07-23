package tile;

import main.GamePanel;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TileManager {

    GamePanel gp;
    public Tile[] tile;
    public int[][] mapTileNum;
    private int worldCols;
    private int worldRows;

    public TileManager(GamePanel gp) {
        this.gp = gp;

        tile = new Tile[10];
        mapTileNum = new int[0][0];
        worldCols = 0;
        worldRows = 0;

        getTileImage();
    }

    public void getTileImage() {
        try {

            tile[0] = new Tile();
            tile[0].image = ImageIO.read(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("tiles/grass.png")));

            tile[1] = new Tile();
            tile[1].image = ImageIO.read(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("tiles/wall.png")));
            tile[1].collision = true;

            tile[2] = new Tile();
            tile[2].image = ImageIO.read(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("tiles/water.png")));
            tile[2].collision = true;

            tile[3] = new Tile();
            tile[3].image = ImageIO.read(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("tiles/earth.png")));

            tile[4] = new Tile();
            tile[4].image = ImageIO.read(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("tiles/tree.png")));
            tile[4].collision = true;

            tile[5] = new Tile();
            tile[5].image = ImageIO.read(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("tiles/sand.png")));



        }catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadMap(String filePath){
        try {
            InputStream is = Objects.requireNonNull(
                    getClass().getClassLoader().getResourceAsStream(filePath),
                    "Missing map file: " + filePath
            );

            try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
                List<int[]> rows = new ArrayList<>();
                int expectedCols = -1;
                String line;

                while ((line = br.readLine()) != null) {
                    if (line.trim().isEmpty()) {
                        continue;
                    }

                    String[] numbers = line.trim().split("\\s+");
                    if (expectedCols == -1) {
                        expectedCols = numbers.length;
                    }
                    else if (numbers.length != expectedCols) {
                        throw new IllegalArgumentException("Map rows must have the same length: " + filePath);
                    }

                    int[] rowTiles = new int[numbers.length];
                    for (int col = 0; col < numbers.length; col++) {
                        int num = Integer.parseInt(numbers[col]);
                        if (num < 0 || num >= tile.length || tile[num] == null) {
                            throw new IllegalArgumentException("Unknown tile id " + num + " in " + filePath);
                        }
                        rowTiles[col] = num;
                    }
                    rows.add(rowTiles);
                }

                if (rows.isEmpty()) {
                    throw new IllegalArgumentException("Map file is empty: " + filePath);
                }

                worldRows = rows.size();
                worldCols = expectedCols;
                mapTileNum = new int[worldCols][worldRows];

                for (int row = 0; row < worldRows; row++) {
                    int[] rowTiles = rows.get(row);
                    for (int col = 0; col < worldCols; col++) {
                        mapTileNum[col][row] = rowTiles[col];
                    }
                }
            }

        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int getWorldCols() {
        return worldCols;
    }

    public int getWorldRows() {
        return worldRows;
    }

    public void draw(Graphics2D g2) {

        int worldCol = 0;
        int worldRow = 0;

        while(worldCol < worldCols && worldRow < worldRows) {

            int tileNum = mapTileNum[worldCol][worldRow];

            int worldX = worldCol * gp.tileSize;
            int worldY = worldRow * gp.tileSize;
            int screenX = worldX - gp.player.worldX + gp.player.screenX;
            int screenY = worldY - gp.player.worldY + gp.player.screenY;

            if(     worldX + gp.tileSize > gp.player.worldX - gp.player.screenX &&
                    worldX - gp.tileSize < gp.player.worldX + gp.player.screenX &&
                    worldY + gp.tileSize > gp.player.worldY - gp.player.screenY &&
                    worldY - gp.tileSize < gp.player.worldY + gp.player.screenY){

                g2.drawImage(tile[tileNum].image ,screenX , screenY, gp.tileSize, gp.tileSize, null);
            }

            worldCol++;


            if(worldCol == worldCols) {
                worldCol = 0;
                worldRow++;
            }
        }
    }
}
