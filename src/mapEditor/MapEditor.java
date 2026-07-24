package mapEditor;

import tile.Tile;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
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

    private static final int BASE_TILE_SIZE = 32;
    private static final double MIN_ZOOM = 0.20;
    private static final double MAX_ZOOM = 5.00;
    private static final Path MAP_DIR = Path.of("res", "maps");

    private int rows;
    private int cols;

    private int selectedTile = 0;
    private int lastPaintedRow = -1;
    private int lastPaintedCol = -1;
    private int[][] map;
    private Path currentMapPath;

    private final Tile[] tiles = new Tile[6];
    private final String[] tileNames = {"Grass", "Wall", "Water", "Earth", "Tree", "Sand"};
    private final JFrame frame;
    private final MapCanvas mapCanvas;
    private final JScrollPane scrollPane;
    private final JLabel currentMapLabel = new JLabel();
    private final JLabel zoomLabel = new JLabel();
    private final JLabel sizeLabel = new JLabel();
    private final JLabel statusLabel = new JLabel("Ready");
    private final JToggleButton[] tileButtons = new JToggleButton[6];
    private final JCheckBox gridCheckBox = new JCheckBox("Grid", true);
    private double zoom = 1.0;
    private Point panStartMouse;
    private Point panStartView;

    public MapEditor(int rows, int cols) {
        this(createEmptyMap(rows, cols), null);
    }

    public MapEditor(int[][] map, Path currentMapPath) {
        this.map = map;
        this.rows = map.length;
        this.cols = map[0].length;
        this.currentMapPath = currentMapPath;

        initTiles();

        mapCanvas = new MapCanvas();
        scrollPane = new JScrollPane(mapCanvas);
        frame = new JFrame("Map Editor");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.setMinimumSize(new Dimension(760, 520));

        scrollPane.getViewport().setBackground(new Color(28, 30, 34));
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(BASE_TILE_SIZE);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(BASE_TILE_SIZE);

        frame.add(createHeaderPanel(), BorderLayout.NORTH);
        frame.add(scrollPane, BorderLayout.CENTER);
        frame.add(createToolsPanel(), BorderLayout.WEST);
        frame.add(createStatusPanel(), BorderLayout.SOUTH);

        installShortcuts();
        updateCurrentMapLabel();
        updateZoomLabel();
        updateMapSize();
        refreshCanvasSize();
        resizeFrame();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private static int[][] createEmptyMap(int rows, int cols) {
        return new int[rows][cols];
    }

    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout(12, 0));
        panel.setBorder(new EmptyBorder(10, 12, 10, 12));
        panel.setBackground(new Color(39, 43, 50));

        JPanel mapInfoPanel = new JPanel(new GridLayout(0, 1, 0, 2));
        mapInfoPanel.setOpaque(false);
        currentMapLabel.setForeground(Color.WHITE);
        currentMapLabel.setFont(currentMapLabel.getFont().deriveFont(Font.BOLD, 14f));
        sizeLabel.setForeground(new Color(194, 201, 212));
        mapInfoPanel.add(currentMapLabel);
        mapInfoPanel.add(sizeLabel);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actionPanel.setOpaque(false);
        actionPanel.add(createActionButton("Load", e -> loadMapWithChooser()));
        actionPanel.add(createActionButton("Save", e -> saveCurrentMap()));
        actionPanel.add(createActionButton("Export", e -> exportMap()));
        actionPanel.add(createActionButton("Reset", e -> resetMap()));

        panel.add(mapInfoPanel, BorderLayout.WEST);
        panel.add(actionPanel, BorderLayout.EAST);
        return panel;
    }

    private JPanel createToolsPanel() {
        JPanel toolsPanel = new JPanel();
        toolsPanel.setLayout(new BoxLayout(toolsPanel, BoxLayout.Y_AXIS));
        toolsPanel.setBorder(new EmptyBorder(12, 12, 12, 12));
        toolsPanel.setBackground(new Color(246, 247, 249));
        toolsPanel.setPreferredSize(new Dimension(190, 1));

        JLabel paletteLabel = createSectionLabel("Tiles");
        toolsPanel.add(paletteLabel);
        toolsPanel.add(Box.createVerticalStrut(8));

        ButtonGroup group = new ButtonGroup();
        for (int i = 0; i < tiles.length; i++) {
            JToggleButton button = createTileButton(i);
            tileButtons[i] = button;
            group.add(button);
            toolsPanel.add(button);
            toolsPanel.add(Box.createVerticalStrut(6));
        }
        tileButtons[selectedTile].setSelected(true);

        toolsPanel.add(Box.createVerticalStrut(16));
        toolsPanel.add(createSectionLabel("View"));
        toolsPanel.add(Box.createVerticalStrut(8));
        toolsPanel.add(createZoomPanel());
        toolsPanel.add(Box.createVerticalStrut(8));

        gridCheckBox.setOpaque(false);
        gridCheckBox.setFocusable(false);
        gridCheckBox.addActionListener(e -> mapCanvas.repaint());
        toolsPanel.add(gridCheckBox);

        toolsPanel.add(Box.createVerticalGlue());

        JTextArea hint = new JTextArea("Wheel zooms. Hold left mouse button to paint. Right click picks a tile. Middle drag pans. Keys 1-6 select tiles.");
        hint.setEditable(false);
        hint.setFocusable(false);
        hint.setLineWrap(true);
        hint.setWrapStyleWord(true);
        hint.setOpaque(false);
        hint.setForeground(new Color(88, 94, 105));
        hint.setFont(hint.getFont().deriveFont(12f));
        toolsPanel.add(hint);

        return toolsPanel;
    }

    private JPanel createStatusPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(6, 12, 6, 12));
        panel.setBackground(new Color(236, 239, 243));
        statusLabel.setForeground(new Color(55, 61, 70));
        panel.add(statusLabel, BorderLayout.WEST);
        return panel;
    }

    private JPanel createZoomPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 0, 6);
        gbc.gridy = 0;

        JButton zoomOutButton = createSmallButton("-", "Zoom out");
        zoomOutButton.addActionListener(e -> zoomAtCenter(zoom / 1.25));
        panel.add(zoomOutButton, gbc);

        gbc.gridx = 1;
        zoomLabel.setHorizontalAlignment(SwingConstants.CENTER);
        zoomLabel.setPreferredSize(new Dimension(58, 28));
        panel.add(zoomLabel, gbc);

        gbc.gridx = 2;
        JButton zoomInButton = createSmallButton("+", "Zoom in");
        zoomInButton.addActionListener(e -> zoomAtCenter(zoom * 1.25));
        panel.add(zoomInButton, gbc);

        gbc.gridx = 3;
        gbc.insets = new Insets(0, 6, 0, 0);
        JButton fitButton = createSmallButton("Fit", "Fit map to window");
        fitButton.addActionListener(e -> fitMapToWindow());
        panel.add(fitButton, gbc);

        return panel;
    }

    private JButton createActionButton(String text, java.awt.event.ActionListener listener) {
        JButton button = new JButton(text);
        button.setFocusable(false);
        button.addActionListener(listener);
        return button;
    }

    private JButton createSmallButton(String text, String tooltip) {
        JButton button = new JButton(text);
        button.setToolTipText(tooltip);
        button.setFocusable(false);
        button.setMargin(new Insets(4, 8, 4, 8));
        return button;
    }

    private JLabel createSectionLabel(String text) {
        JLabel label = new JLabel(text);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setForeground(new Color(48, 54, 64));
        label.setFont(label.getFont().deriveFont(Font.BOLD, 13f));
        return label;
    }

    private JToggleButton createTileButton(int tileId) {
        JToggleButton button = new JToggleButton(tileNames[tileId] + " (" + tileId + ")");
        button.setIcon(new ImageIcon(scaleImage(tiles[tileId].image, 24, 24)));
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setIconTextGap(10);
        button.setFocusable(false);
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        button.addActionListener(e -> {
            selectedTile = tileId;
            statusLabel.setText("Selected: " + tileNames[selectedTile] + " (" + selectedTile + ")");
        });
        return button;
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

    private Image scaleImage(BufferedImage source, int width, int height) {
        BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = scaled.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g2.drawImage(source, 0, 0, width, height, null);
        g2.dispose();
        return scaled;
    }

    private void resetMap() {
        int choice = JOptionPane.showConfirmDialog(
                frame,
                "Reset every tile to grass?",
                "Reset map",
                JOptionPane.YES_NO_OPTION);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                map[row][col] = 0;
            }
        }
        mapCanvas.repaint();
        statusLabel.setText("Map reset to grass.");
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

        updateCurrentMapLabel();
        updateMapSize();
        refreshCanvasSize();
        fitMapToWindow();
        statusLabel.setText("Loaded: " + mapPath.getFileName());
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
            statusLabel.setText("Saved: " + currentMapPath.getFileName());
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
            statusLabel.setText("Exported: " + fullPath.getFileName());
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
            currentMapLabel.setText("Current: new map");
        }
        else {
            currentMapLabel.setText("Current: " + currentMapPath.getFileName());
        }
    }

    private void updateMapSize() {
        sizeLabel.setText("Size: " + cols + " x " + rows + " tiles");
    }

    private void updateZoomLabel() {
        zoomLabel.setText(Math.round(zoom * 100) + "%");
    }

    private void resizeFrame() {
        frame.setSize(Math.min(cols * BASE_TILE_SIZE + 260, 1280), Math.min(rows * BASE_TILE_SIZE + 160, 900));
    }

    private void installShortcuts() {
        JRootPane root = frame.getRootPane();
        InputMap inputMap = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = root.getActionMap();

        for (int i = 0; i < tileButtons.length; i++) {
            int tileId = i;
            String actionName = "selectTile" + tileId;
            inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_1 + tileId, 0), actionName);
            actionMap.put(actionName, new AbstractAction() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    selectedTile = tileId;
                    tileButtons[tileId].setSelected(true);
                    statusLabel.setText("Selected: " + tileNames[selectedTile] + " (" + selectedTile + ")");
                }
            });
        }

        int shortcutMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, shortcutMask), "saveMap");
        actionMap.put("saveMap", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                saveCurrentMap();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_O, shortcutMask), "loadMap");
        actionMap.put("loadMap", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                loadMapWithChooser();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_0, shortcutMask), "fitMap");
        actionMap.put("fitMap", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                fitMapToWindow();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, shortcutMask), "zoomIn");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ADD, shortcutMask), "zoomIn");
        actionMap.put("zoomIn", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                zoomAtCenter(zoom * 1.25);
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, shortcutMask), "zoomOut");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_SUBTRACT, shortcutMask), "zoomOut");
        actionMap.put("zoomOut", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                zoomAtCenter(zoom / 1.25);
            }
        });
    }

    private int scaledTileSize() {
        return Math.max(2, (int) Math.round(BASE_TILE_SIZE * zoom));
    }

    private void refreshCanvasSize() {
        int tileSize = scaledTileSize();
        mapCanvas.setPreferredSize(new Dimension(cols * tileSize, rows * tileSize));
        mapCanvas.revalidate();
        mapCanvas.repaint();
        updateZoomLabel();
    }

    private void zoomAtCenter(double newZoom) {
        Rectangle view = scrollPane.getViewport().getViewRect();
        zoomAtPoint(new Point(view.x + view.width / 2, view.y + view.height / 2), newZoom);
    }

    private void zoomAtPoint(Point viewPoint, double newZoom) {
        double oldZoom = zoom;
        zoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, newZoom));
        if (Math.abs(oldZoom - zoom) < 0.001) {
            return;
        }

        double scale = zoom / oldZoom;
        JViewport viewport = scrollPane.getViewport();
        Rectangle view = viewport.getViewRect();
        int pointXInViewport = viewPoint.x - view.x;
        int pointYInViewport = viewPoint.y - view.y;
        int newX = (int) Math.round(viewPoint.x * scale - pointXInViewport);
        int newY = (int) Math.round(viewPoint.y * scale - pointYInViewport);

        refreshCanvasSize();

        Dimension size = mapCanvas.getPreferredSize();
        newX = Math.max(0, Math.min(newX, Math.max(0, size.width - view.width)));
        newY = Math.max(0, Math.min(newY, Math.max(0, size.height - view.height)));
        viewport.setViewPosition(new Point(newX, newY));
        statusLabel.setText("Zoom: " + Math.round(zoom * 100) + "%");
    }

    private void fitMapToWindow() {
        Dimension extent = scrollPane.getViewport().getExtentSize();
        if (extent.width <= 0 || extent.height <= 0 || cols <= 0 || rows <= 0) {
            return;
        }

        double widthZoom = extent.getWidth() / (cols * BASE_TILE_SIZE);
        double heightZoom = extent.getHeight() / (rows * BASE_TILE_SIZE);
        zoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, Math.min(widthZoom, heightZoom)));
        refreshCanvasSize();
        scrollPane.getViewport().setViewPosition(new Point(0, 0));
        statusLabel.setText("Fitted map to window.");
    }

    private void paintTileAt(Point point) {
        int tileSize = scaledTileSize();
        int col = point.x / tileSize;
        int row = point.y / tileSize;

        if (row < 0 || row >= rows || col < 0 || col >= cols) {
            return;
        }

        if (lastPaintedRow >= 0 && lastPaintedCol >= 0) {
            paintLine(lastPaintedCol, lastPaintedRow, col, row);
        }
        else {
            setMapTile(row, col);
        }

        lastPaintedRow = row;
        lastPaintedCol = col;
        statusLabel.setText("Painted " + tileNames[selectedTile] + " at " + col + ", " + row);
    }

    private void paintLine(int startCol, int startRow, int endCol, int endRow) {
        int dx = Math.abs(endCol - startCol);
        int dy = Math.abs(endRow - startRow);
        int stepCol = startCol < endCol ? 1 : -1;
        int stepRow = startRow < endRow ? 1 : -1;
        int error = dx - dy;

        int col = startCol;
        int row = startRow;
        while (true) {
            setMapTile(row, col);
            if (col == endCol && row == endRow) {
                break;
            }

            int doubledError = 2 * error;
            if (doubledError > -dy) {
                error -= dy;
                col += stepCol;
            }
            if (doubledError < dx) {
                error += dx;
                row += stepRow;
            }
        }
    }

    private void setMapTile(int row, int col) {
        if (map[row][col] == selectedTile) {
            return;
        }

        int tileSize = scaledTileSize();
        map[row][col] = selectedTile;
        mapCanvas.repaint(col * tileSize, row * tileSize, tileSize + 1, tileSize + 1);
    }

    private void pickTileAt(Point point) {
        int tileSize = scaledTileSize();
        int col = point.x / tileSize;
        int row = point.y / tileSize;

        if (row < 0 || row >= rows || col < 0 || col >= cols) {
            return;
        }

        selectedTile = map[row][col];
        tileButtons[selectedTile].setSelected(true);
        statusLabel.setText("Picked " + tileNames[selectedTile] + " (" + selectedTile + ")");
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

            String rowsInput = JOptionPane.showInputDialog("Rows (example: 50):");
            String colsInput = JOptionPane.showInputDialog("Columns (example: 50):");

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

    private class MapCanvas extends JPanel {
        private final Color backgroundColor = new Color(28, 30, 34);
        private final Color gridColor = new Color(0, 0, 0, 55);

        private MapCanvas() {
            setOpaque(true);
            setBackground(backgroundColor);
            int tileSize = scaledTileSize();
            setPreferredSize(new Dimension(cols * tileSize, rows * tileSize));

            MouseAdapter mouseHandler = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (SwingUtilities.isLeftMouseButton(e)) {
                        lastPaintedRow = -1;
                        lastPaintedCol = -1;
                        paintTileAt(e.getPoint());
                    }
                    else if (SwingUtilities.isRightMouseButton(e)) {
                        pickTileAt(e.getPoint());
                    }
                    else if (SwingUtilities.isMiddleMouseButton(e)) {
                        panStartMouse = e.getPoint();
                        panStartView = scrollPane.getViewport().getViewPosition();
                        setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                    }
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    lastPaintedRow = -1;
                    lastPaintedCol = -1;
                    panStartMouse = null;
                    panStartView = null;
                    setCursor(Cursor.getDefaultCursor());
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    if (SwingUtilities.isLeftMouseButton(e)) {
                        paintTileAt(e.getPoint());
                    }
                    else if (SwingUtilities.isMiddleMouseButton(e) && panStartMouse != null && panStartView != null) {
                        int dx = e.getX() - panStartMouse.x;
                        int dy = e.getY() - panStartMouse.y;
                        JViewport viewport = scrollPane.getViewport();
                        Dimension size = getPreferredSize();
                        Dimension extent = viewport.getExtentSize();
                        int newX = Math.max(0, Math.min(panStartView.x - dx, Math.max(0, size.width - extent.width)));
                        int newY = Math.max(0, Math.min(panStartView.y - dy, Math.max(0, size.height - extent.height)));
                        viewport.setViewPosition(new Point(newX, newY));
                    }
                }

                @Override
                public void mouseMoved(MouseEvent e) {
                    int tileSize = scaledTileSize();
                    int col = e.getX() / tileSize;
                    int row = e.getY() / tileSize;
                    if (row >= 0 && row < rows && col >= 0 && col < cols) {
                        statusLabel.setText("Tile: " + col + ", " + row + " | " + tileNames[map[row][col]] + " (" + map[row][col] + ")");
                    }
                }

                @Override
                public void mouseWheelMoved(MouseWheelEvent e) {
                    double factor = Math.pow(1.12, -e.getPreciseWheelRotation());
                    zoomAtPoint(e.getPoint(), zoom * factor);
                    e.consume();
                }
            };

            addMouseListener(mouseHandler);
            addMouseMotionListener(mouseHandler);
            addMouseWheelListener(mouseHandler);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

            int tileSize = scaledTileSize();
            Rectangle clip = g2.getClipBounds();
            int firstCol = Math.max(0, clip.x / tileSize);
            int firstRow = Math.max(0, clip.y / tileSize);
            int lastCol = Math.min(cols - 1, (clip.x + clip.width) / tileSize);
            int lastRow = Math.min(rows - 1, (clip.y + clip.height) / tileSize);

            for (int row = firstRow; row <= lastRow; row++) {
                for (int col = firstCol; col <= lastCol; col++) {
                    BufferedImage image = tiles[map[row][col]].image;
                    g2.drawImage(image, col * tileSize, row * tileSize, tileSize, tileSize, null);
                }
            }

            if (gridCheckBox.isSelected() && tileSize >= 8) {
                g2.setColor(gridColor);
                for (int col = firstCol; col <= lastCol + 1; col++) {
                    int x = col * tileSize;
                    g2.drawLine(x, firstRow * tileSize, x, Math.min(rows * tileSize, (lastRow + 1) * tileSize));
                }
                for (int row = firstRow; row <= lastRow + 1; row++) {
                    int y = row * tileSize;
                    g2.drawLine(firstCol * tileSize, y, Math.min(cols * tileSize, (lastCol + 1) * tileSize), y);
                }
            }

            g2.dispose();
        }
    }
}
