package tile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class MapPatch {
    private MapPatch() {}

    public static void apply(TileManager map, String resource) {
        List<int[]> cells = new ArrayList<>();
        try (InputStream stream = Objects.requireNonNull(MapPatch.class.getClassLoader().getResourceAsStream(resource), resource);
             BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            for (String line; (line = reader.readLine()) != null;) {
                if (line.isBlank() || line.stripLeading().startsWith("#")) continue;
                String[] fields = line.trim().split("\\s+");
                if (fields.length != 4) throw new IllegalArgumentException("Invalid map patch cell");
                int[] cell = Arrays.stream(fields).mapToInt(Integer::parseInt).toArray();
                if (cell[0] < 0 || cell[0] >= map.getWorldCols() || cell[1] < 0 || cell[1] >= map.getWorldRows()
                        || cell[2] < 0 || cell[2] >= map.tile.length || cell[3] < -1 || cell[3] >= map.tile.length)
                    throw new IllegalArgumentException("Map patch cell outside map or tileset");
                cells.add(cell);
            }
        } catch (IOException e) { throw new IllegalStateException("Cannot read map patch", e); }
        for (int[] cell : cells) {
            map.mapTileNum[cell[0]][cell[1]] = cell[2];
            map.objectTileNum[cell[0]][cell[1]] = cell[3];
        }
    }
}
