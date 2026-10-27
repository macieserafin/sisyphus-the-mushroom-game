package tile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads and writes map grids. A map is a ground file ("maps/hub.txt") plus an optional
 * object layer next to it ("maps/hub_objects.txt") where "." marks an empty cell.
 * Grids are returned as [row][col].
 */
public final class MapFile {
    public static final int EMPTY = -1;

    private MapFile() {
    }

    public static String objectLayerPath(String groundPath) {
        return groundPath.endsWith(".txt")
                ? groundPath.substring(0, groundPath.length() - 4) + "_objects.txt"
                : groundPath + "_objects";
    }

    public static int[][] read(InputStream stream, int tileCount, boolean allowEmpty, String name) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            List<int[]> rows = new ArrayList<>();
            int expectedCols = -1;
            String line;

            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }

                String[] cells = line.trim().split("\\s+");
                if (expectedCols == -1) {
                    expectedCols = cells.length;
                }
                else if (cells.length != expectedCols) {
                    throw new IllegalArgumentException("Map rows must have the same length: " + name);
                }

                int[] row = new int[cells.length];
                for (int col = 0; col < cells.length; col++) {
                    String cell = cells[col];
                    if (allowEmpty && (cell.equals(".") || cell.equals("-1"))) {
                        row[col] = EMPTY;
                        continue;
                    }
                    int id = Integer.parseInt(cell);
                    if (id < 0 || id >= tileCount) {
                        throw new IllegalArgumentException("Unknown tile id " + id + " in " + name);
                    }
                    row[col] = id;
                }
                rows.add(row);
            }

            if (rows.isEmpty()) {
                throw new IllegalArgumentException("Map file is empty: " + name);
            }
            return rows.toArray(new int[0][]);
        }
    }

    public static String write(int[][] grid, boolean objectLayer) {
        StringBuilder out = new StringBuilder();
        for (int[] row : grid) {
            for (int col = 0; col < row.length; col++) {
                if (col > 0) {
                    out.append(' ');
                }
                out.append(objectLayer && row[col] == EMPTY ? "." : String.valueOf(row[col]));
            }
            out.append(System.lineSeparator());
        }
        return out.toString();
    }

    public static boolean isEmpty(int[][] grid) {
        for (int[] row : grid) {
            for (int cell : row) {
                if (cell != EMPTY) {
                    return false;
                }
            }
        }
        return true;
    }
}
