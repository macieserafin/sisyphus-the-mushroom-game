package mapEditor;

import tile.Tile;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

public class MapEditor {

    private static final int TILE_SIZE = 20;
    private static final Path MAP_DIR = Path.of("res", "maps");

    private int rows;
    private int cols;

    private int selectedTile = 0;
    private int[][] map;
    private JButton[][] buttons;
    private Path currentMapPath;

    private final Tile[] tiles = new Tile[6];
    private final JFrame frame;
    private final JPanel gridPanel = new JPanel();
    private final JLabel currentMapLabel = new JLabel();

    public MapEditor(int rows, int cols) {
        this(createEmptyMap(rows, cols), null);
    }

    public MapEditor(int[][] map, Path currentMapPath) {
        this.map = map;
        this.rows = map.length;
        this.cols = map[0].length;
        this.currentMapPath = currentMapPath;

        initTiles();

        frame = new JFrame("Map Editor");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        JScrollPane scrollPane = new JScrollPane(gridPanel);
        frame.add(scrollPane, BorderLayout.CENTER);
        frame.add(createToolsPanel(), BorderLayout.EAST);

        rebuildGrid();
        updateCurrentMapLabel();
        resizeFrame();
        frame.setVisible(true);
    }

    private static int[][] createEmptyMap(int rows, int cols) {
        int[][] emptyMap = new int[rows][cols];
        return emptyMap;
    }

    private JPanel createToolsPanel() {
        JPanel tilePanel = new JPanel(new GridLayout(0, 1, 6, 6));
        tilePanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JButton grassButton = new JButton("Grass (0)");
        JButton wallButton = new JButton("Wall (1)");
        JButton waterButton = new JButton("Water (2)");
        JButton earthButton = new JButton("Earth (3)");
        JButton treeButton = new JButton("Tree (4)");
        JButton sandButton = new JButton("Sand (5)");
        JButton loadButton = new JButton("Load Map");
        JButton saveButton = new JButton("Save Map");
        JButton resetButton = new JButton("Reset");
        JButton exportButton = new JButton("Export New Map");

        grassButton.addActionListener(e -> selectedTile = 0);
        wallButton.addActionListener(e -> selectedTile = 1);
        waterButton.addActionListener(e -> selectedTile = 2);
        earthButton.addActionListener(e -> selectedTile = 3);
        treeButton.addActionListener(e -> selectedTile = 4);
        sandButton.addActionListener(e -> selectedTile = 5);
        loadButton.addActionListener(e -> loadMapWithChooser());
        saveButton.addActionListener(e -> saveCurrentMap());
        resetButton.addActionListener(e -> resetMap());
        exportButton.addActionListener(e -> exportMap());

        tilePanel.add(currentMapLabel);
        tilePanel.add(grassButton);
        tilePanel.add(wallButton);
        tilePanel.add(waterButton);
        tilePanel.add(earthButton);
        tilePanel.add(treeButton);
        tilePanel.add(sandButton);
        tilePanel.add(loadButton);
        tilePanel.add(saveButton);
        tilePanel.add(resetButton);
        tilePanel.add(exportButton);

        return tilePanel;
    }

    private void initTiles() {
        tiles[0] = new Tile();
        tiles[0].image = loadTileImage("tiles/grass.png");

        tiles[1] = new Tile();
        tiles[1].image = loadTileImage("tiles/wall.png");
        tiles[1].collision = true;

        tiles[2] = new Tile();
        tiles[2].image = loadTileImage("tiles/water.png");
        tiles[2].collision = true;

        tiles[3] = new Tile();
        tiles[3].image = loadTileImage("tiles/earth.png");

        tiles[4] = new Tile();
        tiles[4].image = loadTileImage("tiles/tree.png");
        tiles[4].collision = true;

        tiles[5] = new Tile();
        tiles[5].image = loadTileImage("tiles/sand.png");
    }

    private BufferedImage loadTileImage(String resourcePath) {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IllegalStateException("Missing resource: " + resourcePath);
            }
            return ImageIO.read(in);
        } catch (IOException e) {
            throw new RuntimeException("Could not load image: " + resourcePath, e);
        }
    }

    private void rebuildGrid() {
        gridPanel.removeAll();
        gridPanel.setLayout(new GridLayout(rows, cols, 0, 0));
        buttons = new JButton[rows][cols];

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                JButton button = new JButton();
                button.setPreferredSize(new Dimension(TILE_SIZE, TILE_SIZE));
                button.setMargin(new Insets(0, 0, 0, 0));
                button.setFocusable(false);
                button.setBorderPainted(false);
                button.setContentAreaFilled(false);
                button.setOpaque(true);

                setButtonTile(button, map[row][col]);

                int finalRow = row;
                int finalCol = col;
                button.addActionListener(e -> {
                    map[finalRow][finalCol] = selectedTile;
                    setButtonTile(button, selectedTile);
                });

                buttons[row][col] = button;
                gridPanel.add(button);
            }
        }

        gridPanel.revalidate();
        gridPanel.repaint();
    }

    private void setButtonTile(JButton button, int tileType) {
        BufferedImage img = tiles[tileType].image;
        Image scaled = img.getScaledInstance(TILE_SIZE, TILE_SIZE, Image.SCALE_FAST);
        button.setIcon(new ImageIcon(scaled));
    }

    private void resetMap() {
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                map[row][col] = 0;
                setButtonTile(buttons[row][col], 0);
            }
        }
    }

    private void loadMapWithChooser() {
        JFileChooser chooser = createMapChooser();
        int result = chooser.showOpenDialog(frame);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        Path mapPath = chooser.getSelectedFile().toPath();
        try {
            loadMap(mapPath);
        } catch (IOException | IllegalArgumentException e) {
            JOptionPane.showMessageDialog(frame, "Could not load map: " + e.getMessage());
        }
    }

    private void loadMap(Path mapPath) throws IOException {
        int[][] loadedMap = readMap(mapPath);
        map = loadedMap;
        rows = loadedMap.length;
        cols = loadedMap[0].length;
        currentMapPath = mapPath;

        rebuildGrid();
        updateCurrentMapLabel();
        resizeFrame();
    }

    private int[][] readMap(Path mapPath) throws IOException {
        List<String> lines = Files.readAllLines(mapPath);
        List<int[]> loadedRows = new ArrayList<>();
        int expectedCols = -1;

        for (String line : lines) {
            if (line.trim().isEmpty()) {
                continue;
            }

            String[] values = line.trim().split("\\s+");
            if (expectedCols == -1) {
                expectedCols = values.length;
            }
            else if (values.length != expectedCols) {
                throw new IllegalArgumentException("Map rows must have the same length.");
            }

            int[] loadedRow = new int[values.length];
            for (int col = 0; col < values.length; col++) {
                int tileType = Integer.parseInt(values[col]);
                if (tileType < 0 || tileType >= tiles.length || tiles[tileType] == null) {
                    throw new IllegalArgumentException("Unknown tile id: " + tileType);
                }
                loadedRow[col] = tileType;
            }
            loadedRows.add(loadedRow);
        }

        if (loadedRows.isEmpty()) {
            throw new IllegalArgumentException("Map file is empty.");
        }

        return loadedRows.toArray(new int[0][]);
    }

    private void saveCurrentMap() {
        if (currentMapPath == null) {
            exportMap();
            return;
        }

        try {
            writeMap(currentMapPath);
            JOptionPane.showMessageDialog(frame, "Map saved:\n" + currentMapPath.toAbsolutePath());
        } catch (IOException e) {
            JOptionPane.showMessageDialog(frame, "Could not save map: " + e.getMessage());
        }
    }

    private void exportMap() {
        try {
            Files.createDirectories(MAP_DIR);

            Path fullPath = MAP_DIR.resolve(getNextFileName());
            writeMap(fullPath);
            currentMapPath = fullPath;
            updateCurrentMapLabel();

            JOptionPane.showMessageDialog(frame, "Map exported:\n" + fullPath.toAbsolutePath());
        } catch (IOException e) {
            JOptionPane.showMessageDialog(frame, "Could not export map: " + e.getMessage());
        }
    }

    private void writeMap(Path fullPath) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(
                fullPath,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE)) {
            for (int row = 0; row < rows; row++) {
                for (int col = 0; col < cols; col++) {
                    writer.write(map[row][col] + (col == cols - 1 ? "" : " "));
                }
                writer.newLine();
            }
        }
    }

    private String getNextFileName() {
        int mapNumber = 1;
        while (Files.exists(MAP_DIR.resolve(String.format("map%02d.txt", mapNumber)))) {
            mapNumber++;
        }
        return String.format("map%02d.txt", mapNumber);
    }

    private void updateCurrentMapLabel() {
        if (currentMapPath == null) {
            currentMapLabel.setText("<html>Current:<br>new map</html>");
        }
        else {
            currentMapLabel.setText("<html>Current:<br>" + currentMapPath.getFileName() + "</html>");
        }
    }

    private void resizeFrame() {
        frame.setSize(Math.min(cols * TILE_SIZE + 260, 1200), Math.min(rows * TILE_SIZE + 140, 900));
        frame.setLocationRelativeTo(null);
    }

    private static JFileChooser createMapChooser() {
        JFileChooser chooser = new JFileChooser(MAP_DIR.toFile());
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setDialogTitle("Choose map from res/maps");
        return chooser;
    }

    private static Path chooseExistingMap() {
        JFileChooser chooser = createMapChooser();
        int result = chooser.showOpenDialog(null);
        if (result == JFileChooser.APPROVE_OPTION) {
            return chooser.getSelectedFile().toPath();
        }
        return null;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            int choice = JOptionPane.showConfirmDialog(
                    null,
                    "Open existing map from res/maps?",
                    "Map Editor",
                    JOptionPane.YES_NO_OPTION);

            if (choice == JOptionPane.YES_OPTION) {
                Path mapPath = chooseExistingMap();
                if (mapPath == null) {
                    return;
                }

                try {
                    new MapEditor(readMapBeforeEditor(mapPath), mapPath);
                } catch (IOException | IllegalArgumentException e) {
                    JOptionPane.showMessageDialog(null, "Could not load map: " + e.getMessage());
                }
                return;
            }

            String rowsInput = JOptionPane.showInputDialog("Rows (example: 12):");
            String colsInput = JOptionPane.showInputDialog("Columns (example: 16):");

            try {
                int rows = Integer.parseInt(rowsInput);
                int cols = Integer.parseInt(colsInput);

                if (rows <= 0 || cols <= 0) {
                    JOptionPane.showMessageDialog(null, "Dimensions must be greater than zero.");
                    return;
                }

                new MapEditor(rows, cols);
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(null, "Invalid number format.");
            }
        });
    }

    private static int[][] readMapBeforeEditor(Path mapPath) throws IOException {
        List<String> lines = Files.readAllLines(mapPath);
        List<int[]> loadedRows = new ArrayList<>();
        int expectedCols = -1;

        for (String line : lines) {
            if (line.trim().isEmpty()) {
                continue;
            }

            String[] values = line.trim().split("\\s+");
            if (expectedCols == -1) {
                expectedCols = values.length;
            }
            else if (values.length != expectedCols) {
                throw new IllegalArgumentException("Map rows must have the same length.");
            }

            int[] loadedRow = new int[values.length];
            for (int col = 0; col < values.length; col++) {
                loadedRow[col] = Integer.parseInt(values[col]);
            }
            loadedRows.add(loadedRow);
        }

        if (loadedRows.isEmpty()) {
            throw new IllegalArgumentException("Map file is empty.");
        }

        return loadedRows.toArray(new int[0][]);
    }
}
