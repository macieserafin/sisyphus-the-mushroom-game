package mapEditor;

import tile.MapFile;
import tile.Tile;
import tile.TileSet;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Tile map editor with two layers: the ground and the objects standing on it.
 * Tools: brush, fill, rectangle, picker, eraser and hand. Run it with: java mapEditor.MapEditor
 */
public class MapEditor {
    private static final int BASE_TILE_SIZE = 32;
    private static final double MIN_ZOOM = 0.2;
    private static final double MAX_ZOOM = 6.0;
    private static final int MAX_UNDO = 300;
    private static final Path MAP_DIR = Path.of("res", "maps");
    private static final Color CANVAS_BG = new Color(0x1B, 0x1D, 0x21);
    private static final Color ACCENT = TilePalette.ACCENT;

    enum Tool {
        BRUSH("Brush", KeyEvent.VK_B, "Paint tiles. Drag to draw lines."),
        FILL("Fill", KeyEvent.VK_F, "Flood fill an area with the selected tile."),
        RECT("Rectangle", KeyEvent.VK_R, "Drag a rectangle and fill it with the selected tile."),
        PICKER("Picker", KeyEvent.VK_I, "Click a cell to pick its tile (also: right click with any tool)."),
        ERASER("Eraser", KeyEvent.VK_E, "Remove objects. A whole building goes away at once."),
        PAN("Hand", KeyEvent.VK_H, "Drag to move around (also: middle mouse button).");

        final String label;
        final int key;
        final String help;

        Tool(String label, int key, String help) {
            this.label = label;
            this.key = key;
            this.help = help;
        }

        boolean paints() {
            return this == BRUSH || this == FILL || this == RECT || this == ERASER;
        }
    }

    private static final class Change {
        final boolean object;
        final int row;
        final int col;
        final int before;
        final int after;

        Change(boolean object, int row, int col, int before, int after) {
            this.object = object;
            this.row = row;
            this.col = col;
            this.before = before;
            this.after = after;
        }
    }

    // ------------------------------------------------------------------ state

    private final Tile[] tiles = TileSet.loadTiles();
    private int rows;
    private int cols;
    private int[][] map;
    private int[][] objectMap;
    private Path currentMapPath;
    private boolean dirty;

    private Tool tool = Tool.BRUSH;
    private Tool lastPaintTool = Tool.BRUSH;
    private int brushSize = 1;
    private double zoom = 1.0;
    private boolean showGrid = true;
    private boolean showGround = true;
    private boolean showObjects = true;
    private boolean showCollisions = false;

    private final Deque<List<Change>> undoStack = new ArrayDeque<>();
    private final Deque<List<Change>> redoStack = new ArrayDeque<>();
    private List<Change> stroke;

    private int hoverRow = -1;
    private int hoverCol = -1;
    private int lastRow = -1;
    private int lastCol = -1;
    private boolean painting;
    private boolean rectDragging;
    private int rectStartRow;
    private int rectStartCol;
    private int rectEndRow;
    private int rectEndCol;
    private Point panStartMouse;
    private Point panStartView;

    // ------------------------------------------------------------------ components

    private final JFrame frame = new JFrame();
    private final TilePalette palette;
    private final MapCanvas canvas;
    private final JScrollPane scrollPane;
    private final JLabel messageLabel = new JLabel("Ready");
    private final JLabel cursorLabel = new JLabel(" ");
    private final JLabel zoomLabel = new JLabel("100%");
    private final Map<Tool, JToggleButton> toolButtons = new EnumMap<>(Tool.class);
    private Action saveAction;
    private Action undoAction;
    private Action redoAction;
    private ToggleAction gridAction;
    private ToggleAction groundAction;
    private ToggleAction objectsAction;
    private ToggleAction collisionsAction;

    public MapEditor(int rows, int cols) {
        this(createEmptyMap(rows, cols), createEmptyObjectMap(rows, cols), null, true);
    }

    public MapEditor(int[][] map, int[][] objectMap, Path currentMapPath) {
        this(map, objectMap, currentMapPath, true);
    }

    MapEditor(int[][] map, int[][] objectMap, Path currentMapPath, boolean visible) {
        this.map = map;
        this.objectMap = objectMap;
        this.rows = map.length;
        this.cols = map[0].length;
        this.currentMapPath = currentMapPath;

        palette = new TilePalette(tiles, this::onPaletteChosen, this::focusCanvas);
        canvas = new MapCanvas();
        scrollPane = new JScrollPane(canvas);
        scrollPane.getViewport().setBackground(CANVAS_BG);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(BASE_TILE_SIZE);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(BASE_TILE_SIZE);

        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                exit();
            }
        });
        frame.setLayout(new BorderLayout());
        frame.setJMenuBar(buildMenuBar());
        frame.add(buildToolBar(), BorderLayout.NORTH);

        palette.setPreferredSize(new Dimension(340, 10));
        JPanel center = new JPanel(new BorderLayout());
        center.add(scrollPane, BorderLayout.CENTER);
        frame.add(palette, BorderLayout.WEST);
        frame.add(center, BorderLayout.CENTER);
        frame.add(buildStatusBar(), BorderLayout.SOUTH);

        installShortcuts();
        updateUndoActions();
        updateTitle();
        setTool(Tool.BRUSH);

        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        frame.setMinimumSize(new Dimension(900, 600));
        frame.setSize(Math.min(1400, screen.width - 60), Math.min(880, screen.height - 80));
        frame.setLocationRelativeTo(null);
        if (visible) {
            frame.setVisible(true);
            SwingUtilities.invokeLater(this::fitMapToWindow);
        }
    }

    JFrame frame() {
        return frame;
    }

    // ------------------------------------------------------------------ building the window

    private JMenuBar buildMenuBar() {
        int mask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        saveAction = action("Save", KeyEvent.VK_S, mask, this::save);
        undoAction = action("Undo", KeyEvent.VK_Z, mask, () -> {
            if (!textFieldHasFocus()) {
                undo();
            }
        });
        redoAction = action("Redo", KeyEvent.VK_Y, mask, () -> {
            if (!textFieldHasFocus()) {
                redo();
            }
        });
        gridAction = new ToggleAction("Grid", showGrid, on -> {
            showGrid = on;
            canvas.repaint();
        });
        groundAction = new ToggleAction("Ground", showGround, on -> {
            showGround = on;
            canvas.repaint();
        });
        objectsAction = new ToggleAction("Objects", showObjects, on -> {
            showObjects = on;
            canvas.repaint();
        });
        collisionsAction = new ToggleAction("Collisions", showCollisions, on -> {
            showCollisions = on;
            canvas.repaint();
        });

        JMenuBar bar = new JMenuBar();

        JMenu file = new JMenu("File");
        file.setMnemonic('F');
        file.add(action("New map...", KeyEvent.VK_N, mask, this::newMap));
        file.add(action("Open...", KeyEvent.VK_O, mask, this::openMap));
        file.addSeparator();
        file.add(saveAction);
        file.add(action("Save as...", KeyEvent.VK_S, mask | InputEvent_SHIFT, this::saveAs));
        file.addSeparator();
        file.add(action("Exit", 0, 0, this::exit));
        bar.add(file);

        JMenu edit = new JMenu("Edit");
        edit.setMnemonic('E');
        edit.add(undoAction);
        edit.add(redoAction);
        edit.addSeparator();
        edit.add(action("Find tile", KeyEvent.VK_F, mask, palette::focusSearch));
        edit.addSeparator();
        edit.add(action("Clear all objects...", 0, 0, this::clearObjects));
        edit.add(action("Reset map to grass...", 0, 0, this::resetMap));
        bar.add(edit);

        JMenu view = new JMenu("View");
        view.setMnemonic('V');
        view.add(new JCheckBoxMenuItem(gridAction));
        view.add(new JCheckBoxMenuItem(groundAction));
        view.add(new JCheckBoxMenuItem(objectsAction));
        view.add(new JCheckBoxMenuItem(collisionsAction));
        view.addSeparator();
        view.add(action("Zoom in", KeyEvent.VK_EQUALS, mask, () -> zoomAtCenter(zoom * 1.25)));
        view.add(action("Zoom out", KeyEvent.VK_MINUS, mask, () -> zoomAtCenter(zoom / 1.25)));
        view.add(action("Actual size", KeyEvent.VK_1, mask, () -> zoomAtCenter(1.0)));
        view.add(action("Fit to window", KeyEvent.VK_0, mask, this::fitMapToWindow));
        bar.add(view);

        JMenu help = new JMenu("Help");
        help.setMnemonic('H');
        help.add(action("Shortcuts", 0, 0, this::showShortcuts));
        bar.add(help);
        return bar;
    }

    private static final int InputEvent_SHIFT = java.awt.event.InputEvent.SHIFT_DOWN_MASK;

    private JToolBar buildToolBar() {
        JToolBar bar = new JToolBar();
        bar.setFloatable(false);
        bar.setBorder(new MatteBorder(0, 0, 1, 0, new Color(0x00, 0x00, 0x00, 60)));
        bar.setBorder(BorderFactory.createCompoundBorder(bar.getBorder(), new EmptyBorder(4, 8, 4, 8)));

        bar.add(toolbarButton(saveAction));
        bar.add(toolbarButton(undoAction));
        bar.add(toolbarButton(redoAction));
        bar.addSeparator(new Dimension(14, 0));

        ButtonGroup group = new ButtonGroup();
        for (Tool t : Tool.values()) {
            JToggleButton button = new JToggleButton(t.label, new ToolIcon(t));
            button.setToolTipText(t.help + "  [" + KeyEvent.getKeyText(t.key) + "]");
            button.setFocusable(false);
            button.addActionListener(e -> setTool(t));
            group.add(button);
            bar.add(button);
            toolButtons.put(t, button);
        }

        bar.addSeparator(new Dimension(14, 0));
        JLabel sizeLabel = new JLabel("Brush size ");
        bar.add(sizeLabel);
        JComboBox<String> sizeBox = new JComboBox<>(new String[]{"1 x 1", "2 x 2", "3 x 3", "4 x 4", "5 x 5"});
        sizeBox.setFocusable(false);
        sizeBox.setMaximumSize(new Dimension(80, 28));
        sizeBox.setToolTipText("Size of the brush and the eraser");
        sizeBox.addActionListener(e -> {
            brushSize = sizeBox.getSelectedIndex() + 1;
            canvas.repaint();
        });
        bar.add(sizeBox);

        bar.add(Box.createHorizontalGlue());

        JButton zoomOut = smallButton("-", "Zoom out");
        zoomOut.addActionListener(e -> zoomAtCenter(zoom / 1.25));
        JButton zoomIn = smallButton("+", "Zoom in");
        zoomIn.addActionListener(e -> zoomAtCenter(zoom * 1.25));
        JButton fit = smallButton("Fit", "Fit the whole map in the window");
        fit.addActionListener(e -> fitMapToWindow());
        zoomLabel.setHorizontalAlignment(SwingConstants.CENTER);
        zoomLabel.setPreferredSize(new Dimension(54, 20));
        bar.add(new JLabel("Zoom "));
        bar.add(zoomOut);
        bar.add(zoomLabel);
        bar.add(zoomIn);
        bar.add(fit);
        return bar;
    }

    private JButton toolbarButton(Action action) {
        JButton button = new JButton(action);
        button.setFocusable(false);
        return button;
    }

    private JButton smallButton(String text, String tooltip) {
        JButton button = new JButton(text);
        button.setToolTipText(tooltip);
        button.setFocusable(false);
        button.setMargin(new Insets(2, 8, 2, 8));
        return button;
    }

    private JComponent buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout(12, 0));
        bar.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(1, 0, 0, 0, new Color(0x00, 0x00, 0x00, 60)), new EmptyBorder(4, 10, 4, 10)));

        bar.add(messageLabel, BorderLayout.CENTER);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);
        cursorLabel.setPreferredSize(new Dimension(320, 20));
        cursorLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        right.add(cursorLabel);
        for (ToggleAction a : new ToggleAction[]{gridAction, groundAction, objectsAction, collisionsAction}) {
            JCheckBox box = new JCheckBox(a);
            box.setFocusable(false);
            right.add(box);
        }
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    /** A menu/toolbar action with an optional accelerator. */
    private Action action(String name, int key, int modifiers, Runnable run) {
        AbstractAction action = new AbstractAction(name) {
            @Override
            public void actionPerformed(ActionEvent e) {
                run.run();
            }
        };
        if (key != 0) {
            action.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(key, modifiers));
        }
        return action;
    }

    /** A check box action usable from both the menu and the status bar, also from a key press. */
    private static final class ToggleAction extends AbstractAction {
        private final Consumer<Boolean> apply;

        ToggleAction(String name, boolean initial, Consumer<Boolean> apply) {
            super(name);
            this.apply = apply;
            putValue(Action.SELECTED_KEY, initial);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            apply.accept(Boolean.TRUE.equals(getValue(Action.SELECTED_KEY)));
        }

        void flip() {
            boolean on = !Boolean.TRUE.equals(getValue(Action.SELECTED_KEY));
            putValue(Action.SELECTED_KEY, on);
            apply.accept(on);
        }
    }

    private void installShortcuts() {
        JRootPane root = frame.getRootPane();
        InputMap inputs = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actions = root.getActionMap();

        for (Tool t : Tool.values()) {
            bind(inputs, actions, KeyStroke.getKeyStroke(t.key, 0), "tool." + t.name(), () -> setTool(t));
        }
        for (int i = 0; i < TilePalette.RECENT_SLOTS; i++) {
            int slot = i;
            bind(inputs, actions, KeyStroke.getKeyStroke(KeyEvent.VK_1 + i, 0), "recent." + i, () -> palette.selectRecent(slot));
        }
        bind(inputs, actions, KeyStroke.getKeyStroke(KeyEvent.VK_G, 0), "toggleGrid", () -> gridAction.flip());
        bind(inputs, actions, KeyStroke.getKeyStroke(KeyEvent.VK_C, 0), "toggleCollisions", () -> collisionsAction.flip());
        bind(inputs, actions, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancel", this::cancelOperation);

        int mask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        bind(inputs, actions, KeyStroke.getKeyStroke(KeyEvent.VK_Z, mask | InputEvent_SHIFT), "redoAlt", this::redo);
        bind(inputs, actions, KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, mask), "zoomInPlus", () -> zoomAtCenter(zoom * 1.25));
        bind(inputs, actions, KeyStroke.getKeyStroke(KeyEvent.VK_ADD, mask), "zoomInNum", () -> zoomAtCenter(zoom * 1.25));
        bind(inputs, actions, KeyStroke.getKeyStroke(KeyEvent.VK_SUBTRACT, mask), "zoomOutNum", () -> zoomAtCenter(zoom / 1.25));
    }

    /** Single-key shortcuts must not fire while the user types in the search box. */
    private void bind(InputMap inputs, ActionMap actions, KeyStroke key, String name, Runnable run) {
        inputs.put(key, name);
        actions.put(name, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!textFieldHasFocus()) {
                    run.run();
                }
            }
        });
    }

    private boolean textFieldHasFocus() {
        return frame.getFocusOwner() instanceof JTextComponent;
    }

    private void focusCanvas() {
        canvas.requestFocusInWindow();
    }

    private void cancelOperation() {
        if (rectDragging) {
            rectDragging = false;
            endStroke();
            canvas.repaint();
        } else if (tool != Tool.BRUSH) {
            setTool(Tool.BRUSH);
        }
    }

    // ------------------------------------------------------------------ tools and selection

    private void setTool(Tool next) {
        if (tool != next && tool.paints()) {
            lastPaintTool = tool;
        }
        tool = next;
        toolButtons.get(next).setSelected(true);
        canvas.setCursor(next == Tool.PAN ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                : next == Tool.PICKER ? Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR)
                : Cursor.getDefaultCursor());
        setMessage(next.label + ": " + next.help);
        canvas.repaint();
    }

    /** The user chose a tile in the palette: switch back to a painting tool. */
    private void onPaletteChosen(int tileId) {
        if (tool == Tool.ERASER || tool == Tool.PICKER || tool == Tool.PAN) {
            setTool(lastPaintTool == Tool.ERASER ? Tool.BRUSH : lastPaintTool);
        }
        setMessage("Selected " + palette.describe(tileId));
        canvas.repaint();
    }

    private int selectedTile() {
        return palette.selected();
    }

    // ------------------------------------------------------------------ editing primitives

    private boolean inBounds(int row, int col) {
        return row >= 0 && row < rows && col >= 0 && col < cols;
    }

    private void beginStroke() {
        stroke = new ArrayList<>();
    }

    private void endStroke() {
        if (stroke != null && !stroke.isEmpty()) {
            undoStack.push(stroke);
            while (undoStack.size() > MAX_UNDO) {
                undoStack.removeLast();
            }
            redoStack.clear();
            markDirty();
            updateUndoActions();
        }
        stroke = null;
    }

    /** Runs several edits as one undo step. */
    private void asOneStroke(Runnable edits) {
        beginStroke();
        edits.run();
        endStroke();
        canvas.repaint();
    }

    private void setCell(boolean object, int row, int col, int value) {
        int[][] grid = object ? objectMap : map;
        int old = grid[row][col];
        if (old == value) {
            return;
        }
        grid[row][col] = value;
        if (stroke != null) {
            stroke.add(new Change(object, row, col, old, value));
        }
    }

    private void markDirty() {
        if (!dirty) {
            dirty = true;
            updateTitle();
        }
    }

    private int originOf(int id) {
        Tile t = tiles[id];
        return t.group == null ? id : id - (t.groupY * t.groupWidth + t.groupX);
    }

    /** Removes the object at a cell; a building is removed whole, not just the clicked cell. */
    private void clearObjectInstance(int row, int col) {
        int value = objectMap[row][col];
        if (value == MapFile.EMPTY) {
            return;
        }
        Tile t = tiles[value];
        if (t.group == null) {
            setCell(true, row, col, MapFile.EMPTY);
            return;
        }
        int origin = value - (t.groupY * t.groupWidth + t.groupX);
        int row0 = row - t.groupY;
        int col0 = col - t.groupX;
        for (int y = 0; y < t.groupHeight; y++) {
            for (int x = 0; x < t.groupWidth; x++) {
                int r = row0 + y, c = col0 + x;
                if (inBounds(r, c) && objectMap[r][c] == origin + y * t.groupWidth + x) {
                    setCell(true, r, c, MapFile.EMPTY);
                }
            }
        }
    }

    private void placeTile(int row, int col, int id) {
        if (tiles[id].objectLayer) {
            clearObjectInstance(row, col);
            setCell(true, row, col, id);
        } else {
            setCell(false, row, col, id);
        }
    }

    private boolean stampBig(int row, int col) {
        int origin = originOf(selectedTile());
        Tile t = tiles[origin];
        int w = t.groupWidth, h = t.groupHeight;
        if (row + h > rows || col + w > cols) {
            setMessage("That does not fit here: " + palette.describe(origin) + " is " + w + " x " + h + " tiles.");
            return false;
        }
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                clearObjectInstance(row + y, col + x);
            }
        }
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                setCell(true, row + y, col + x, origin + y * w + x);
            }
        }
        return true;
    }

    private void forBrush(int row, int col, Consumer<int[]> action) {
        int start = -(brushSize - 1) / 2;
        for (int dy = 0; dy < brushSize; dy++) {
            for (int dx = 0; dx < brushSize; dx++) {
                int r = row + start + dy, c = col + start + dx;
                if (inBounds(r, c)) {
                    action.accept(new int[]{r, c});
                }
            }
        }
    }

    /** One application of the brush or the eraser at a cell. */
    private void paintAt(int row, int col) {
        if (tool == Tool.ERASER) {
            forBrush(row, col, p -> clearObjectInstance(p[0], p[1]));
            return;
        }
        if (tiles[selectedTile()].group != null) {
            stampBig(row, col);
            return;
        }
        int id = selectedTile();
        forBrush(row, col, p -> placeTile(p[0], p[1], id));
    }

    private void fillAt(int row, int col) {
        int id = selectedTile();
        if (tiles[id].group != null) {
            stampBig(row, col);
            return;
        }
        boolean object = tiles[id].objectLayer;
        int[][] grid = object ? objectMap : map;
        int target = grid[row][col];
        if (target == id) {
            return;
        }
        if (object && target != MapFile.EMPTY && tiles[target].group != null) {
            setMessage("Fill does not replace buildings. Use the eraser first.");
            return;
        }

        ArrayDeque<int[]> queue = new ArrayDeque<>();
        queue.add(new int[]{row, col});
        setCell(object, row, col, id);
        while (!queue.isEmpty()) {
            int[] cell = queue.poll();
            int[][] steps = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
            for (int[] step : steps) {
                int r = cell[0] + step[0], c = cell[1] + step[1];
                if (inBounds(r, c) && grid[r][c] == target) {
                    setCell(object, r, c, id);
                    queue.add(new int[]{r, c});
                }
            }
        }
    }

    private void applyRect(int row0, int col0, int row1, int col1) {
        int top = Math.min(row0, row1), bottom = Math.max(row0, row1);
        int left = Math.min(col0, col1), right = Math.max(col0, col1);
        int id = selectedTile();
        Tile t = tiles[id];
        if (t.group != null) {
            // repeat the building across the rectangle
            int w = t.groupWidth, h = t.groupHeight;
            for (int r = top; r + h - 1 <= bottom; r += h) {
                for (int c = left; c + w - 1 <= right; c += w) {
                    stampBig(r, c);
                }
            }
            return;
        }
        for (int r = top; r <= bottom; r++) {
            for (int c = left; c <= right; c++) {
                placeTile(r, c, id);
            }
        }
    }

    private void pickAt(int row, int col) {
        int id = objectMap[row][col] != MapFile.EMPTY ? objectMap[row][col] : map[row][col];
        palette.select(id);
        setMessage("Picked " + palette.describe(id));
    }

    // ------------------------------------------------------------------ undo / redo

    private void undo() {
        if (undoStack.isEmpty()) {
            return;
        }
        List<Change> changes = undoStack.pop();
        for (int i = changes.size() - 1; i >= 0; i--) {
            Change c = changes.get(i);
            (c.object ? objectMap : map)[c.row][c.col] = c.before;
        }
        redoStack.push(changes);
        markDirty();
        updateUndoActions();
        canvas.repaint();
        setMessage("Undo (" + changes.size() + " cells)");
    }

    private void redo() {
        if (redoStack.isEmpty()) {
            return;
        }
        List<Change> changes = redoStack.pop();
        for (Change c : changes) {
            (c.object ? objectMap : map)[c.row][c.col] = c.after;
        }
        undoStack.push(changes);
        markDirty();
        updateUndoActions();
        canvas.repaint();
        setMessage("Redo (" + changes.size() + " cells)");
    }

    private void updateUndoActions() {
        if (undoAction != null) {
            undoAction.setEnabled(!undoStack.isEmpty());
            redoAction.setEnabled(!redoStack.isEmpty());
        }
    }

    // ------------------------------------------------------------------ files

    private String currentName() {
        return currentMapPath == null ? "Untitled" : currentMapPath.getFileName().toString();
    }

    private void updateTitle() {
        frame.setTitle((dirty ? "* " : "") + currentName() + " - Map Editor  (" + cols + " x " + rows + ")");
    }

    private boolean confirmDiscard() {
        if (!dirty) {
            return true;
        }
        int choice = JOptionPane.showConfirmDialog(frame, "Save changes to " + currentName() + "?",
                "Unsaved changes", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice == JOptionPane.YES_OPTION) {
            return save();
        }
        return choice == JOptionPane.NO_OPTION;
    }

    private void exit() {
        if (confirmDiscard()) {
            frame.dispose();
            System.exit(0);
        }
    }

    private void newMap() {
        if (!confirmDiscard()) {
            return;
        }
        int[] size = askSize(frame);
        if (size == null) {
            return;
        }
        replaceMap(createEmptyMap(size[0], size[1]), createEmptyObjectMap(size[0], size[1]), null);
        setMessage("New map " + size[1] + " x " + size[0]);
    }

    private void openMap() {
        if (!confirmDiscard()) {
            return;
        }
        JFileChooser chooser = createMapChooser();
        if (chooser.showOpenDialog(frame) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        Path path = chooser.getSelectedFile().toPath();
        try {
            int[][][] layers = readMapFiles(path);
            replaceMap(layers[0], layers[1], path);
            setMessage("Opened " + path.getFileName());
        } catch (IOException | RuntimeException e) {
            JOptionPane.showMessageDialog(frame, "Could not open the map:\n" + e.getMessage(), "Open", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void replaceMap(int[][] newMap, int[][] newObjects, Path path) {
        map = newMap;
        objectMap = newObjects;
        rows = newMap.length;
        cols = newMap[0].length;
        currentMapPath = path;
        dirty = false;
        undoStack.clear();
        redoStack.clear();
        stroke = null;
        updateUndoActions();
        updateTitle();
        canvas.updateSize();
        SwingUtilities.invokeLater(this::fitMapToWindow);
    }

    private boolean save() {
        return currentMapPath == null ? saveAs() : writeTo(currentMapPath);
    }

    private boolean saveAs() {
        JFileChooser chooser = createMapChooser();
        chooser.setSelectedFile(currentMapPath != null ? currentMapPath.toFile() : MAP_DIR.resolve(nextFileName()).toFile());
        if (chooser.showSaveDialog(frame) != JFileChooser.APPROVE_OPTION) {
            return false;
        }
        Path path = chooser.getSelectedFile().toPath();
        if (!path.getFileName().toString().endsWith(".txt")) {
            path = path.resolveSibling(path.getFileName() + ".txt");
        }
        if (Files.exists(path) && !path.equals(currentMapPath)) {
            int overwrite = JOptionPane.showConfirmDialog(frame, path.getFileName() + " already exists. Replace it?",
                    "Save as", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (overwrite != JOptionPane.YES_OPTION) {
                return false;
            }
        }
        return writeTo(path);
    }

    private boolean writeTo(Path path) {
        try {
            Path parent = path.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(path, MapFile.write(map, false));
            // the object layer lives next to the map; skip it for maps that never had objects
            Path objectPath = objectLayerPath(path);
            if (!MapFile.isEmpty(objectMap) || Files.exists(objectPath)) {
                Files.writeString(objectPath, MapFile.write(objectMap, true));
            }
            currentMapPath = path;
            dirty = false;
            updateTitle();
            setMessage("Saved " + path.getFileName());
            return true;
        } catch (IOException e) {
            JOptionPane.showMessageDialog(frame, "Could not save the map:\n" + e.getMessage(), "Save", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    private String nextFileName() {
        int number = 1;
        while (Files.exists(MAP_DIR.resolve(String.format("map%02d.txt", number)))) {
            number++;
        }
        return String.format("map%02d.txt", number);
    }

    private void resetMap() {
        int choice = JOptionPane.showConfirmDialog(frame, "Fill the whole map with grass and remove every object?",
                "Reset map", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }
        asOneStroke(() -> {
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    setCell(false, r, c, 0);
                    setCell(true, r, c, MapFile.EMPTY);
                }
            }
        });
        setMessage("Map reset. Undo brings it back.");
    }

    private void clearObjects() {
        int choice = JOptionPane.showConfirmDialog(frame, "Remove every object and keep the ground?",
                "Clear objects", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }
        asOneStroke(() -> {
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    setCell(true, r, c, MapFile.EMPTY);
                }
            }
        });
        setMessage("Objects cleared. Undo brings them back.");
    }

    private void showShortcuts() {
        String text = "<html><body style='width:340px'>"
                + "<b>Tools</b><br>"
                + "B brush &nbsp; F fill &nbsp; R rectangle &nbsp; I picker &nbsp; E eraser &nbsp; H hand<br><br>"
                + "<b>Mouse</b><br>"
                + "Left button: use the tool &nbsp; Right button: pick the tile under the cursor<br>"
                + "Middle button: move the view &nbsp; Wheel: zoom<br><br>"
                + "<b>Tiles</b><br>"
                + "Ctrl+F: search &nbsp; 1-8: recent tiles &nbsp; ESC: back to the brush<br>"
                + "Buildings are placed whole from their top-left cell.<br><br>"
                + "<b>Editing</b><br>"
                + "Ctrl+Z undo &nbsp; Ctrl+Y redo &nbsp; Ctrl+S save &nbsp; Ctrl+Shift+S save as<br>"
                + "G grid &nbsp; C collisions &nbsp; Ctrl+0 fit &nbsp; Ctrl+1 actual size"
                + "</body></html>";
        JOptionPane.showMessageDialog(frame, text, "Shortcuts", JOptionPane.PLAIN_MESSAGE);
    }

    private void setMessage(String text) {
        messageLabel.setText(text);
    }

    // ------------------------------------------------------------------ zoom

    private int tileSize() {
        return Math.max(2, (int) Math.round(BASE_TILE_SIZE * zoom));
    }

    private void zoomAtCenter(double newZoom) {
        Rectangle view = scrollPane.getViewport().getViewRect();
        zoomAt(new Point(view.x + view.width / 2, view.y + view.height / 2), newZoom);
    }

    private void zoomAt(Point canvasPoint, double newZoom) {
        newZoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, newZoom));
        if (Math.abs(newZoom - zoom) < 0.001) {
            return;
        }
        JViewport viewport = scrollPane.getViewport();
        Rectangle view = viewport.getViewRect();
        double fx = canvasPoint.x / (double) tileSize();
        double fy = canvasPoint.y / (double) tileSize();

        zoom = newZoom;
        canvas.updateSize();

        int ts = tileSize();
        Dimension size = canvas.getPreferredSize();
        int x = (int) Math.round(fx * ts - (canvasPoint.x - view.x));
        int y = (int) Math.round(fy * ts - (canvasPoint.y - view.y));
        x = Math.max(0, Math.min(x, Math.max(0, size.width - view.width)));
        y = Math.max(0, Math.min(y, Math.max(0, size.height - view.height)));
        viewport.setViewPosition(new Point(x, y));
    }

    private void fitMapToWindow() {
        Dimension extent = scrollPane.getViewport().getExtentSize();
        if (extent.width <= 0 || extent.height <= 0) {
            return;
        }
        double fit = Math.min(extent.width / (double) (cols * BASE_TILE_SIZE), extent.height / (double) (rows * BASE_TILE_SIZE));
        zoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, fit));
        canvas.updateSize();
        scrollPane.getViewport().setViewPosition(new Point(0, 0));
    }

    // ------------------------------------------------------------------ the map view

    private boolean isSolid(int row, int col) {
        if (tiles[map[row][col]].collision) {
            return true;
        }
        int object = objectMap[row][col];
        return object != MapFile.EMPTY && tiles[object].collision;
    }

    private String describeCell(int row, int col) {
        String text = col + ", " + row + "   " + palette.describe(map[row][col]);
        if (objectMap[row][col] != MapFile.EMPTY) {
            text += "  +  " + palette.describe(objectMap[row][col]);
        }
        return text;
    }

    private final class MapCanvas extends JPanel {
        MapCanvas() {
            setOpaque(true);
            setBackground(CANVAS_BG);
            setFocusable(true);
            updateSize();

            MouseAdapter mouse = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    requestFocusInWindow();
                    onPress(e);
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    onRelease();
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    onDrag(e);
                }

                @Override
                public void mouseMoved(MouseEvent e) {
                    onHover(e.getPoint());
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hoverRow = hoverCol = -1;
                    cursorLabel.setText(" ");
                    repaint();
                }

                @Override
                public void mouseWheelMoved(MouseWheelEvent e) {
                    zoomAt(e.getPoint(), zoom * Math.pow(1.12, -e.getPreciseWheelRotation()));
                    e.consume();
                }
            };
            addMouseListener(mouse);
            addMouseMotionListener(mouse);
            addMouseWheelListener(mouse);
        }

        void updateSize() {
            int ts = tileSize();
            setPreferredSize(new Dimension(cols * ts, rows * ts));
            zoomLabel.setText(Math.round(zoom * 100) + "%");
            revalidate();
            repaint();
        }

        private int colAt(Point p) {
            return Math.floorDiv(p.x, tileSize());
        }

        private int rowAt(Point p) {
            return Math.floorDiv(p.y, tileSize());
        }

        private void onHover(Point p) {
            hoverCol = colAt(p);
            hoverRow = rowAt(p);
            cursorLabel.setText(inBounds(hoverRow, hoverCol) ? describeCell(hoverRow, hoverCol) : " ");
            repaint();
        }

        private void onPress(MouseEvent e) {
            Point p = e.getPoint();
            int row = rowAt(p), col = colAt(p);

            if (SwingUtilities.isMiddleMouseButton(e) || (SwingUtilities.isLeftMouseButton(e) && tool == Tool.PAN)) {
                panStartMouse = SwingUtilities.convertPoint(this, p, scrollPane.getViewport());
                panStartView = scrollPane.getViewport().getViewPosition();
                setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                return;
            }
            if (SwingUtilities.isRightMouseButton(e)) {
                if (inBounds(row, col)) {
                    pickAt(row, col);
                    repaint();
                }
                return;
            }
            if (!SwingUtilities.isLeftMouseButton(e) || !inBounds(row, col)) {
                return;
            }

            switch (tool) {
                case BRUSH:
                case ERASER:
                    beginStroke();
                    painting = true;
                    lastRow = row;
                    lastCol = col;
                    paintAt(row, col);
                    break;
                case FILL:
                    asOneStroke(() -> fillAt(row, col));
                    break;
                case RECT:
                    beginStroke();
                    rectDragging = true;
                    rectStartRow = rectEndRow = row;
                    rectStartCol = rectEndCol = col;
                    break;
                case PICKER:
                    pickAt(row, col);
                    setTool(lastPaintTool);
                    break;
                default:
                    break;
            }
            repaint();
        }

        private void onDrag(MouseEvent e) {
            Point p = e.getPoint();
            if (panStartMouse != null) {
                Point now = SwingUtilities.convertPoint(this, p, scrollPane.getViewport());
                JViewport viewport = scrollPane.getViewport();
                Dimension size = getPreferredSize();
                Dimension extent = viewport.getExtentSize();
                int x = Math.max(0, Math.min(panStartView.x - (now.x - panStartMouse.x), Math.max(0, size.width - extent.width)));
                int y = Math.max(0, Math.min(panStartView.y - (now.y - panStartMouse.y), Math.max(0, size.height - extent.height)));
                viewport.setViewPosition(new Point(x, y));
                return;
            }

            int row = rowAt(p), col = colAt(p);
            hoverRow = row;
            hoverCol = col;
            if (painting && inBounds(row, col)) {
                // a building is stamped once per click, not smeared along the drag
                boolean singleStamp = tool == Tool.BRUSH && tiles[selectedTile()].group != null;
                if (!singleStamp) {
                    paintLine(lastCol, lastRow, col, row);
                    lastRow = row;
                    lastCol = col;
                }
            } else if (rectDragging) {
                rectEndRow = Math.max(0, Math.min(rows - 1, row));
                rectEndCol = Math.max(0, Math.min(cols - 1, col));
            }
            if (inBounds(row, col)) {
                cursorLabel.setText(describeCell(row, col));
            }
            repaint();
        }

        private void paintLine(int startCol, int startRow, int endCol, int endRow) {
            int dx = Math.abs(endCol - startCol);
            int dy = Math.abs(endRow - startRow);
            int stepCol = startCol < endCol ? 1 : -1;
            int stepRow = startRow < endRow ? 1 : -1;
            int error = dx - dy;
            int col = startCol, row = startRow;
            while (true) {
                paintAt(row, col);
                if (col == endCol && row == endRow) {
                    break;
                }
                int doubled = 2 * error;
                if (doubled > -dy) {
                    error -= dy;
                    col += stepCol;
                }
                if (doubled < dx) {
                    error += dx;
                    row += stepRow;
                }
            }
        }

        private void onRelease() {
            if (panStartMouse != null) {
                panStartMouse = null;
                panStartView = null;
                setTool(tool);
            }
            if (painting) {
                painting = false;
                endStroke();
            }
            if (rectDragging) {
                rectDragging = false;
                applyRect(rectStartRow, rectStartCol, rectEndRow, rectEndCol);
                endStroke();
            }
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

            int ts = tileSize();
            Rectangle clip = g2.getClipBounds();
            if (clip == null) {
                clip = new Rectangle(0, 0, getWidth(), getHeight());
            }
            int firstCol = Math.max(0, clip.x / ts);
            int firstRow = Math.max(0, clip.y / ts);
            int lastCol = Math.min(cols - 1, (clip.x + clip.width) / ts);
            int lastRow = Math.min(rows - 1, (clip.y + clip.height) / ts);

            for (int row = firstRow; row <= lastRow; row++) {
                for (int col = firstCol; col <= lastCol; col++) {
                    if (showGround) {
                        g2.drawImage(tiles[map[row][col]].image, col * ts, row * ts, ts, ts, null);
                    } else {
                        g2.setColor(new Color(0x2A, 0x2D, 0x33));
                        g2.fillRect(col * ts, row * ts, ts, ts);
                    }
                }
            }
            if (showObjects) {
                for (int row = firstRow; row <= lastRow; row++) {
                    for (int col = firstCol; col <= lastCol; col++) {
                        int object = objectMap[row][col];
                        if (object != MapFile.EMPTY) {
                            g2.drawImage(tiles[object].image, col * ts, row * ts, ts, ts, null);
                        }
                    }
                }
            }
            if (showCollisions) {
                g2.setColor(new Color(0xFF, 0x30, 0x30, 95));
                for (int row = firstRow; row <= lastRow; row++) {
                    for (int col = firstCol; col <= lastCol; col++) {
                        if (isSolid(row, col)) {
                            g2.fillRect(col * ts, row * ts, ts, ts);
                        }
                    }
                }
            }
            if (showGrid && ts >= 8) {
                g2.setColor(new Color(0, 0, 0, 55));
                for (int col = firstCol; col <= lastCol + 1; col++) {
                    g2.drawLine(col * ts, firstRow * ts, col * ts, Math.min(rows * ts, (lastRow + 1) * ts));
                }
                for (int row = firstRow; row <= lastRow + 1; row++) {
                    g2.drawLine(firstCol * ts, row * ts, Math.min(cols * ts, (lastCol + 1) * ts), row * ts);
                }
            }
            g2.setColor(new Color(ACCENT.getRed(), ACCENT.getGreen(), ACCENT.getBlue(), 120));
            g2.drawRect(0, 0, cols * ts - 1, rows * ts - 1);

            paintToolPreview(g2, ts);
            g2.dispose();
        }

        private void outline(Graphics2D g2, int ts, int row, int col, int rowsSpan, int colsSpan, Color color, boolean fill) {
            if (fill) {
                g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 55));
                g2.fillRect(col * ts, row * ts, colsSpan * ts, rowsSpan * ts);
            }
            g2.setColor(color);
            g2.setStroke(new BasicStroke(2f));
            g2.drawRect(col * ts + 1, row * ts + 1, colsSpan * ts - 2, rowsSpan * ts - 2);
        }

        private void ghost(Graphics2D g2, int ts, int row, int col, int id) {
            Composite old = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.65f));
            g2.drawImage(tiles[id].image, col * ts, row * ts, ts, ts, null);
            g2.setComposite(old);
        }

        /** The ghost of what a click would do, plus the rectangle being dragged. */
        private void paintToolPreview(Graphics2D g2, int ts) {
            if (rectDragging) {
                int top = Math.min(rectStartRow, rectEndRow), left = Math.min(rectStartCol, rectEndCol);
                int h = Math.abs(rectEndRow - rectStartRow) + 1, w = Math.abs(rectEndCol - rectStartCol) + 1;
                outline(g2, ts, top, left, h, w, ACCENT, true);
                return;
            }
            if (!inBounds(hoverRow, hoverCol) || tool == Tool.PAN) {
                return;
            }

            int id = selectedTile();
            Tile t = tiles[id];
            switch (tool) {
                case BRUSH:
                    if (t.group != null) {
                        boolean fits = hoverRow + t.groupHeight <= rows && hoverCol + t.groupWidth <= cols;
                        for (int y = 0; y < t.groupHeight; y++) {
                            for (int x = 0; x < t.groupWidth; x++) {
                                if (inBounds(hoverRow + y, hoverCol + x)) {
                                    ghost(g2, ts, hoverRow + y, hoverCol + x, id + y * t.groupWidth + x);
                                }
                            }
                        }
                        outline(g2, ts, hoverRow, hoverCol, t.groupHeight, t.groupWidth, fits ? new Color(0x5C, 0xE0, 0x7A) : new Color(0xFF, 0x5A, 0x4A), false);
                    } else {
                        int start = -(brushSize - 1) / 2;
                        for (int dy = 0; dy < brushSize; dy++) {
                            for (int dx = 0; dx < brushSize; dx++) {
                                if (inBounds(hoverRow + start + dy, hoverCol + start + dx)) {
                                    ghost(g2, ts, hoverRow + start + dy, hoverCol + start + dx, id);
                                }
                            }
                        }
                        outline(g2, ts, hoverRow + start, hoverCol + start, brushSize, brushSize, ACCENT, false);
                    }
                    break;
                case ERASER: {
                    int start = -(brushSize - 1) / 2;
                    outline(g2, ts, hoverRow + start, hoverCol + start, brushSize, brushSize, new Color(0xFF, 0x5A, 0x4A), true);
                    break;
                }
                case FILL:
                case RECT:
                    if (t.group == null) {
                        ghost(g2, ts, hoverRow, hoverCol, id);
                    }
                    outline(g2, ts, hoverRow, hoverCol, 1, 1, ACCENT, false);
                    break;
                case PICKER:
                    outline(g2, ts, hoverRow, hoverCol, 1, 1, new Color(0xFF, 0xD0, 0x4A), true);
                    break;
                default:
                    break;
            }
        }
    }

    // ------------------------------------------------------------------ icons

    private static final class ToolIcon implements Icon {
        private final Tool tool;

        ToolIcon(Tool tool) {
            this.tool = tool;
        }

        @Override
        public int getIconWidth() {
            return 18;
        }

        @Override
        public int getIconHeight() {
            return 18;
        }

        @Override
        public void paintIcon(Component c, Graphics g0, int x, int y) {
            Graphics2D g = (Graphics2D) g0.create();
            g.translate(x, y);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(c.isEnabled() ? c.getForeground() : Color.GRAY);
            g.setStroke(new BasicStroke(1.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            switch (tool) {
                case BRUSH:
                    g.setStroke(new BasicStroke(3.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g.drawLine(8, 10, 14, 4);
                    g.setStroke(new BasicStroke(1.7f));
                    g.fillOval(2, 10, 6, 6);
                    break;
                case FILL:
                    g.drawPolygon(new int[]{3, 9, 14, 8}, new int[]{9, 3, 8, 14}, 4);
                    g.drawLine(14, 8, 15, 12);
                    g.fillOval(13, 12, 4, 4);
                    break;
                case RECT:
                    g.drawRect(3, 4, 12, 10);
                    g.fillRect(2, 3, 3, 3);
                    g.fillRect(13, 12, 3, 3);
                    break;
                case PICKER:
                    g.drawOval(10, 2, 6, 6);
                    g.drawLine(10, 8, 4, 14);
                    g.fillOval(2, 13, 3, 3);
                    break;
                case ERASER:
                    g.rotate(-0.6, 9, 9);
                    g.drawRect(3, 6, 12, 7);
                    g.fillRect(3, 10, 12, 3);
                    break;
                case PAN:
                    g.drawLine(9, 2, 9, 16);
                    g.drawLine(2, 9, 16, 9);
                    g.fillPolygon(new int[]{9, 6, 12}, new int[]{1, 5, 5}, 3);
                    g.fillPolygon(new int[]{9, 6, 12}, new int[]{17, 13, 13}, 3);
                    g.fillPolygon(new int[]{1, 5, 5}, new int[]{9, 6, 12}, 3);
                    g.fillPolygon(new int[]{17, 13, 13}, new int[]{9, 6, 12}, 3);
                    break;
                default:
                    break;
            }
            g.dispose();
        }
    }

    // ------------------------------------------------------------------ map files

    private static int[][] createEmptyMap(int rows, int cols) {
        return new int[rows][cols];
    }

    private static int[][] createEmptyObjectMap(int rows, int cols) {
        int[][] objects = new int[rows][cols];
        for (int[] row : objects) {
            Arrays.fill(row, MapFile.EMPTY);
        }
        return objects;
    }

    private static Path objectLayerPath(Path mapPath) {
        return mapPath.resolveSibling(MapFile.objectLayerPath(mapPath.getFileName().toString()));
    }

    private static int[][] readObjectMap(Path mapPath, int rows, int cols, int tileCount) throws IOException {
        Path objectPath = objectLayerPath(mapPath);
        if (!Files.exists(objectPath)) {
            return createEmptyObjectMap(rows, cols);
        }
        try (InputStream in = Files.newInputStream(objectPath)) {
            int[][] objects = MapFile.read(in, tileCount, true, objectPath.toString());
            if (objects.length != rows || objects[0].length != cols) {
                throw new IllegalArgumentException("The object layer has a different size than the map.");
            }
            return objects;
        }
    }

    /** Returns {ground, objects}, both indexed [row][col]. */
    static int[][][] readMapFiles(Path mapPath) throws IOException {
        int tileCount = TileSet.count();
        int[][] ground;
        try (InputStream in = Files.newInputStream(mapPath)) {
            ground = MapFile.read(in, tileCount, false, mapPath.toString());
        }
        return new int[][][]{ground, readObjectMap(mapPath, ground.length, ground[0].length, tileCount)};
    }

    private static JFileChooser createMapChooser() {
        JFileChooser chooser = new JFileChooser(MAP_DIR.toFile());
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setDialogTitle("Maps (res/maps)");
        chooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            @Override
            public boolean accept(java.io.File f) {
                String name = f.getName();
                return f.isDirectory() || (name.endsWith(".txt") && !name.endsWith("_objects.txt"));
            }

            @Override
            public String getDescription() {
                return "Map files (*.txt, without *_objects.txt)";
            }
        });
        return chooser;
    }

    private static int[] askSize(Component parent) {
        JSpinner rowsSpinner = new JSpinner(new SpinnerNumberModel(50, 1, 1000, 1));
        JSpinner colsSpinner = new JSpinner(new SpinnerNumberModel(50, 1, 1000, 1));
        JPanel panel = new JPanel(new GridLayout(2, 2, 10, 8));
        panel.add(new JLabel("Width (columns):"));
        panel.add(colsSpinner);
        panel.add(new JLabel("Height (rows):"));
        panel.add(rowsSpinner);
        int choice = JOptionPane.showConfirmDialog(parent, panel, "New map", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) {
            return null;
        }
        return new int[]{((Number) rowsSpinner.getValue()).intValue(), ((Number) colsSpinner.getValue()).intValue()};
    }

    // ------------------------------------------------------------------ start-up

    /** What the user chose in the start dialog: an existing map, or the size of a new one. */
    private static final class Startup {
        Path path;
        int rows;
        int cols;
    }

    private static Startup showStartDialog() {
        List<Path> maps = new ArrayList<>();
        if (Files.isDirectory(MAP_DIR)) {
            try (Stream<Path> files = Files.list(MAP_DIR)) {
                files.filter(p -> p.getFileName().toString().endsWith(".txt") && !p.getFileName().toString().endsWith("_objects.txt"))
                        .forEach(maps::add);
            } catch (IOException ignored) {
                // an unreadable folder just means an empty list
            }
        }
        Collections.sort(maps);

        Startup result = new Startup();
        boolean[] chosen = {false};

        JDialog dialog = new JDialog((Frame) null, "Map Editor", true);
        DefaultListModel<Path> model = new DefaultListModel<>();
        maps.forEach(model::addElement);
        JList<Path> list = new JList<>(model);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setFixedCellHeight(28);
        list.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> l, Object value, int index, boolean selected, boolean focus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(l, value, index, selected, focus);
                label.setText(((Path) value).getFileName().toString());
                label.setBorder(new EmptyBorder(0, 10, 0, 10));
                return label;
            }
        });
        if (!maps.isEmpty()) {
            list.setSelectedIndex(0);
        }

        JButton open = new JButton("Open");
        open.setEnabled(!maps.isEmpty());
        Runnable openSelected = () -> {
            if (list.getSelectedValue() != null) {
                result.path = list.getSelectedValue();
                chosen[0] = true;
                dialog.dispose();
            }
        };
        open.addActionListener(e -> openSelected.run());
        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    openSelected.run();
                }
            }
        });

        JButton browse = new JButton("Browse...");
        browse.addActionListener(e -> {
            JFileChooser chooser = createMapChooser();
            if (chooser.showOpenDialog(dialog) == JFileChooser.APPROVE_OPTION) {
                result.path = chooser.getSelectedFile().toPath();
                chosen[0] = true;
                dialog.dispose();
            }
        });
        JButton create = new JButton("New map...");
        create.addActionListener(e -> {
            int[] size = askSize(dialog);
            if (size != null) {
                result.rows = size[0];
                result.cols = size[1];
                chosen[0] = true;
                dialog.dispose();
            }
        });
        JButton quit = new JButton("Quit");
        quit.addActionListener(e -> dialog.dispose());

        JLabel title = new JLabel("Map Editor");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));
        JLabel hint = new JLabel("Open a map from res/maps or start a new one.");
        hint.setForeground(Color.GRAY);
        JPanel header = new JPanel(new GridLayout(2, 1, 0, 4));
        header.setBorder(new EmptyBorder(16, 18, 10, 18));
        header.add(title);
        header.add(hint);

        JScrollPane listScroll = new JScrollPane(list);
        listScroll.setBorder(BorderFactory.createCompoundBorder(new EmptyBorder(0, 18, 0, 18), BorderFactory.createLineBorder(new Color(0, 0, 0, 70))));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setBorder(new EmptyBorder(12, 18, 14, 18));
        buttons.add(quit);
        buttons.add(create);
        buttons.add(browse);
        buttons.add(open);

        dialog.setLayout(new BorderLayout());
        dialog.add(header, BorderLayout.NORTH);
        dialog.add(listScroll, BorderLayout.CENTER);
        dialog.add(buttons, BorderLayout.SOUTH);
        dialog.getRootPane().setDefaultButton(open);
        dialog.setSize(460, 440);
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
        return chosen[0] ? result : null;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
                // the default look and feel is fine too
            }

            Startup start = showStartDialog();
            if (start == null) {
                System.exit(0);
                return;
            }
            if (start.path == null) {
                new MapEditor(start.rows, start.cols);
                return;
            }
            try {
                int[][][] layers = readMapFiles(start.path);
                new MapEditor(layers[0], layers[1], start.path);
            } catch (IOException | RuntimeException e) {
                JOptionPane.showMessageDialog(null, "Could not open the map:\n" + e.getMessage(), "Map Editor", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        });
    }
}
