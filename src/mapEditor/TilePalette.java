package mapEditor;

import tile.Tile;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntConsumer;

/**
 * Tile picker: a searchable grid of tile thumbnails with a kind filter, a strip of recently
 * used tiles (keys 1-8) and a panel describing the tile under the mouse or the selected one.
 * A big object (house, well...) shows up once, and its id is the id of its top-left cell.
 */
class TilePalette extends JPanel {
    static final int RECENT_SLOTS = 8;
    static final Color ACCENT = new Color(0x4F, 0xA3, 0xFF);

    private static final Color BG = new Color(0x1F, 0x22, 0x27);
    private static final Color CELL_A = new Color(0x2E, 0x32, 0x39);
    private static final Color CELL_B = new Color(0x27, 0x2A, 0x30);
    private static final Color TEXT = new Color(0xE4, 0xE6, 0xEA);
    private static final Color TEXT_DIM = new Color(0x9A, 0xA0, 0xAB);
    private static final Color SOLID = new Color(0xF0, 0x7A, 0x6E);
    private static final Color WALKABLE = new Color(0x7A, 0xD0, 0x8A);
    private static final int CELL = 54;
    private static final int GAP = 4;
    private static final int THUMB = 46;

    private enum Filter {
        ALL("All"), GROUND("Ground"), OBJECT("Objects"), BIG("Buildings");

        final String label;

        Filter(String label) {
            this.label = label;
        }
    }

    private static final class Entry {
        final int id;
        final String name;
        final Filter kind;
        final int width;
        final int height;
        final boolean solid;
        final String search;
        final BufferedImage image;

        Entry(int id, String name, Filter kind, int width, int height, boolean solid, BufferedImage image) {
            this.id = id;
            this.name = name;
            this.kind = kind;
            this.width = width;
            this.height = height;
            this.solid = solid;
            this.image = image;
            String keywords = kind == Filter.GROUND ? "ground floor terrain"
                    : kind == Filter.OBJECT ? "object decoration prop"
                    : "building big large";
            this.search = (name + " " + keywords + " " + id + (solid ? " solid" : " walkable")).toLowerCase();
        }

        String kindLabel() {
            return kind == Filter.GROUND ? "Ground" : kind == Filter.OBJECT ? "Object" : "Building " + width + "x" + height;
        }
    }

    private final Entry[] byId;
    private final List<Entry> entries = new ArrayList<>();
    private final List<Entry> shown = new ArrayList<>();
    private final List<Integer> recent = new ArrayList<>();
    private final IntConsumer onUserSelect;
    private final Runnable onReturnFocus;

    private final HintField search = new HintField("Search tiles by name or number...");
    private final JLabel countLabel = new JLabel();
    private final Grid grid = new Grid();
    private final JScrollPane scroll = new JScrollPane(grid);
    private final RecentStrip recentStrip = new RecentStrip();
    private final InfoPanel info = new InfoPanel();
    private final Map<Filter, JToggleButton> chips = new EnumMap<>(Filter.class);

    private Filter filter = Filter.ALL;
    private int selected;
    private Entry hovered;

    /**
     * @param onUserSelect called when the user picks a tile in the palette (not for select())
     * @param onReturnFocus called when ESC is pressed in the empty search box
     */
    TilePalette(Tile[] tiles, IntConsumer onUserSelect, Runnable onReturnFocus) {
        super(new BorderLayout(0, 8));
        this.onUserSelect = onUserSelect;
        this.onReturnFocus = onReturnFocus;
        this.byId = new Entry[tiles.length];
        buildEntries(tiles);
        this.selected = entries.get(0).id;

        setBorder(new EmptyBorder(10, 10, 10, 6));
        add(buildHeader(), BorderLayout.NORTH);

        scroll.setBorder(BorderFactory.createLineBorder(new Color(0x15, 0x17, 0x1B)));
        scroll.getViewport().setBackground(BG);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(CELL + GAP);
        add(scroll, BorderLayout.CENTER);
        add(info, BorderLayout.SOUTH);

        applyFilter();
    }

    // ------------------------------------------------------------------ public API

    int selected() {
        return selected;
    }

    /** Selects a tile from outside the palette (the picker tool). Makes sure it is visible in the grid. */
    void select(int tileId) {
        Entry entry = entryFor(tileId);
        if (entry == null) {
            return;
        }
        if (!shown.contains(entry)) {
            search.setText("");
            setFilter(Filter.ALL);
        }
        setSelected(entry.id, true);
    }

    void selectRecent(int slot) {
        if (slot < 0 || slot >= recent.size()) {
            return;
        }
        int id = recent.get(slot);
        setSelected(id, false);
        onUserSelect.accept(id);
    }

    void focusSearch() {
        search.requestFocusInWindow();
        search.selectAll();
    }

    String describe(int tileId) {
        Entry entry = entryFor(tileId);
        return entry == null ? "?" : entry.name;
    }

    // ------------------------------------------------------------------ construction

    private void buildEntries(Tile[] tiles) {
        for (int id = 0; id < tiles.length; id++) {
            Tile tile = tiles[id];
            boolean big = tile.group != null;
            if (big && (tile.groupX != 0 || tile.groupY != 0)) {
                continue;
            }
            int w = big ? tile.groupWidth : 1;
            int h = big ? tile.groupHeight : 1;
            String name = big ? tile.name.substring(0, tile.name.lastIndexOf(' ')) : tile.name;
            Filter kind = big ? Filter.BIG : tile.objectLayer ? Filter.OBJECT : Filter.GROUND;

            boolean solid = false;
            BufferedImage image = new BufferedImage(w * 16, h * 16, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = image.createGraphics();
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    Tile cell = tiles[id + y * w + x];
                    solid |= cell.collision;
                    g.drawImage(cell.image, x * 16, y * 16, null);
                }
            }
            g.dispose();

            Entry entry = new Entry(id, name, kind, w, h, solid, image);
            entries.add(entry);
            byId[id] = entry;
        }
    }

    private JComponent buildHeader() {
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Tiles");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 15f));
        title.setAlignmentX(LEFT_ALIGNMENT);
        header.add(title);
        header.add(Box.createVerticalStrut(8));

        search.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                applyFilter();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                applyFilter();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                applyFilter();
            }
        });
        search.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "clearSearch");
        search.getActionMap().put("clearSearch", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (search.getText().isEmpty()) {
                    onReturnFocus.run();
                } else {
                    search.setText("");
                }
            }
        });
        search.addActionListener(e -> {
            // ENTER takes the first match
            if (!shown.isEmpty()) {
                userSelect(shown.get(0));
                onReturnFocus.run();
            }
        });
        search.setAlignmentX(LEFT_ALIGNMENT);
        search.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        header.add(search);
        header.add(Box.createVerticalStrut(6));

        JPanel chipRow = new JPanel(new GridLayout(1, Filter.values().length, 4, 0));
        chipRow.setAlignmentX(LEFT_ALIGNMENT);
        chipRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        ButtonGroup group = new ButtonGroup();
        for (Filter f : Filter.values()) {
            JToggleButton chip = new JToggleButton(f.label, f == filter);
            chip.setFocusable(false);
            chip.setMargin(new Insets(2, 4, 2, 4));
            chip.addActionListener(e -> setFilter(f));
            group.add(chip);
            chipRow.add(chip);
            chips.put(f, chip);
        }
        header.add(chipRow);
        header.add(Box.createVerticalStrut(8));

        JPanel recentHeader = new JPanel(new BorderLayout());
        recentHeader.setAlignmentX(LEFT_ALIGNMENT);
        recentHeader.setMaximumSize(new Dimension(Integer.MAX_VALUE, 18));
        JLabel recentLabel = new JLabel("Recent  (keys 1-" + RECENT_SLOTS + ")");
        recentLabel.setForeground(Color.GRAY);
        recentLabel.setFont(recentLabel.getFont().deriveFont(11f));
        countLabel.setForeground(Color.GRAY);
        countLabel.setFont(countLabel.getFont().deriveFont(11f));
        recentHeader.add(recentLabel, BorderLayout.WEST);
        recentHeader.add(countLabel, BorderLayout.EAST);
        header.add(recentHeader);
        header.add(Box.createVerticalStrut(3));

        recentStrip.setAlignmentX(LEFT_ALIGNMENT);
        header.add(recentStrip);
        return header;
    }

    // ------------------------------------------------------------------ state

    private Entry entryFor(int tileId) {
        if (tileId < 0 || tileId >= byId.length) {
            return null;
        }
        if (byId[tileId] != null) {
            return byId[tileId];
        }
        // a cell of a big object: walk back to the top-left cell
        for (int id = tileId; id >= 0; id--) {
            if (byId[id] != null) {
                return byId[id];
            }
        }
        return null;
    }

    private void setFilter(Filter next) {
        filter = next;
        chips.get(next).setSelected(true);
        applyFilter();
    }

    private void applyFilter() {
        String[] terms = search.getText().toLowerCase().trim().split("\\s+");
        shown.clear();
        for (Entry entry : entries) {
            if (filter != Filter.ALL && entry.kind != filter) {
                continue;
            }
            boolean matches = true;
            for (String term : terms) {
                if (!term.isEmpty() && !entry.search.contains(term)) {
                    matches = false;
                    break;
                }
            }
            if (matches) {
                shown.add(entry);
            }
        }
        countLabel.setText(shown.size() + " / " + entries.size());
        grid.revalidate();
        grid.repaint();
        scroll.getVerticalScrollBar().setValue(0);
    }

    private void setSelected(int id, boolean remember) {
        selected = id;
        if (remember && !recent.contains(id)) {
            recent.add(0, id);
            while (recent.size() > RECENT_SLOTS) {
                recent.remove(recent.size() - 1);
            }
        }
        Entry entry = byId[id];
        int index = shown.indexOf(entry);
        if (index >= 0) {
            int cols = grid.columns();
            Rectangle cell = grid.cellBounds(index, cols);
            grid.scrollRectToVisible(cell);
        }
        grid.repaint();
        recentStrip.repaint();
        info.repaint();
    }

    private void userSelect(Entry entry) {
        setSelected(entry.id, true);
        onUserSelect.accept(entry.id);
    }

    private void setHovered(Entry entry) {
        if (hovered != entry) {
            hovered = entry;
            info.repaint();
        }
    }

    private static void drawFit(Graphics2D g, BufferedImage image, int x, int y, int box) {
        double scale = Math.min((double) box / image.getWidth(), (double) box / image.getHeight());
        boolean crisp = scale >= 1;
        if (crisp) {
            scale = Math.floor(scale);
        }
        int w = Math.max(1, (int) Math.round(image.getWidth() * scale));
        int h = Math.max(1, (int) Math.round(image.getHeight() * scale));
        Object oldHint = g.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                crisp ? RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR : RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(image, x + (box - w) / 2, y + (box - h) / 2, w, h, null);
        if (oldHint != null) {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, oldHint);
        }
    }

    private static void drawChecker(Graphics2D g, int x, int y, int w, int h) {
        for (int cy = 0; cy < h; cy += 8) {
            for (int cx = 0; cx < w; cx += 8) {
                g.setColor(((cx + cy) / 8) % 2 == 0 ? CELL_A : CELL_B);
                g.fillRect(x + cx, y + cy, Math.min(8, w - cx), Math.min(8, h - cy));
            }
        }
    }

    // ------------------------------------------------------------------ components

    /** Text field that shows a grey hint while it is empty. */
    private static final class HintField extends JTextField {
        private final String hint;

        HintField(String hint) {
            this.hint = hint;
            setToolTipText("Ctrl+F focuses this box. ENTER picks the first match, ESC clears it.");
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (getText().isEmpty()) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setColor(Color.GRAY);
                g2.setFont(getFont().deriveFont(Font.ITALIC));
                Insets in = getInsets();
                g2.drawString(hint, in.left + 2, (getHeight() + g2.getFontMetrics().getAscent() - g2.getFontMetrics().getDescent()) / 2);
                g2.dispose();
            }
        }
    }

    private final class Grid extends JPanel implements Scrollable {
        Grid() {
            setBackground(BG);
            ToolTipManager.sharedInstance().registerComponent(this);
            MouseAdapter mouse = new MouseAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    setHovered(entryAt(e.getPoint()));
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    setHovered(null);
                    repaint();
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    Entry entry = entryAt(e.getPoint());
                    if (entry != null && SwingUtilities.isLeftMouseButton(e)) {
                        userSelect(entry);
                    }
                }
            };
            addMouseListener(mouse);
            addMouseMotionListener(mouse);
        }

        int columns() {
            Container parent = getParent();
            int width = parent != null && parent.getWidth() > 0 ? parent.getWidth() : 300;
            return Math.max(1, (width - GAP) / (CELL + GAP));
        }

        Rectangle cellBounds(int index, int columns) {
            return new Rectangle(GAP + (index % columns) * (CELL + GAP), GAP + (index / columns) * (CELL + GAP), CELL, CELL);
        }

        private Entry entryAt(Point p) {
            int columns = columns();
            int x = p.x - GAP, y = p.y - GAP;
            if (x < 0 || y < 0 || x % (CELL + GAP) >= CELL || y % (CELL + GAP) >= CELL) {
                return null;
            }
            int col = x / (CELL + GAP), row = y / (CELL + GAP);
            if (col >= columns) {
                return null;
            }
            int index = row * columns + col;
            return index < shown.size() ? shown.get(index) : null;
        }

        @Override
        public Dimension getPreferredSize() {
            int columns = columns();
            int rows = (shown.size() + columns - 1) / columns;
            Container parent = getParent();
            int width = parent != null && parent.getWidth() > 0 ? parent.getWidth() : 300;
            return new Dimension(width, Math.max(60, GAP + rows * (CELL + GAP)));
        }

        @Override
        public String getToolTipText(MouseEvent e) {
            Entry entry = entryAt(e.getPoint());
            if (entry == null) {
                return null;
            }
            return "<html><b>" + entry.name + "</b><br>#" + entry.id + "  -  " + entry.kindLabel()
                    + "<br>" + (entry.solid ? "solid" : "walkable") + "</html>";
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Rectangle clip = g2.getClipBounds();
            int columns = columns();

            if (shown.isEmpty()) {
                g2.setColor(TEXT_DIM);
                g2.setFont(getFont().deriveFont(Font.ITALIC, 13f));
                g2.drawString("No tile matches.", 14, 28);
            }

            for (int i = 0; i < shown.size(); i++) {
                Rectangle cell = cellBounds(i, columns);
                if (clip != null && !clip.intersects(cell)) {
                    continue;
                }
                Entry entry = shown.get(i);
                Shape shape = new java.awt.geom.RoundRectangle2D.Double(cell.x, cell.y, cell.width, cell.height, 8, 8);
                Shape oldClip = g2.getClip();
                g2.clip(shape);
                drawChecker(g2, cell.x, cell.y, cell.width, cell.height);
                g2.setClip(oldClip);

                drawFit(g2, entry.image, cell.x + (CELL - THUMB) / 2, cell.y + (CELL - THUMB) / 2, THUMB);

                if (entry.kind == Filter.BIG) {
                    String tag = entry.width + "x" + entry.height;
                    g2.setFont(getFont().deriveFont(Font.BOLD, 9f));
                    int tx = cell.x + cell.width - g2.getFontMetrics().stringWidth(tag) - 4;
                    int ty = cell.y + cell.height - 4;
                    g2.setColor(new Color(0, 0, 0, 200));
                    g2.drawString(tag, tx + 1, ty + 1);
                    g2.setColor(Color.WHITE);
                    g2.drawString(tag, tx, ty);
                }

                if (entry.id == selected) {
                    g2.setColor(ACCENT);
                    g2.setStroke(new BasicStroke(2.5f));
                    g2.draw(new java.awt.geom.RoundRectangle2D.Double(cell.x + 1, cell.y + 1, cell.width - 2, cell.height - 2, 8, 8));
                } else if (entry == hovered) {
                    g2.setColor(new Color(255, 255, 255, 140));
                    g2.setStroke(new BasicStroke(1.5f));
                    g2.draw(new java.awt.geom.RoundRectangle2D.Double(cell.x + 1, cell.y + 1, cell.width - 2, cell.height - 2, 8, 8));
                }
            }
            g2.dispose();
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return new Dimension(300, 400);
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return CELL + GAP;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return Math.max(CELL, visibleRect.height - CELL);
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    private final class RecentStrip extends JPanel {
        private static final int SLOT = 34;
        private static final int SLOT_GAP = 3;

        RecentStrip() {
            setOpaque(false);
            setPreferredSize(new Dimension(RECENT_SLOTS * (SLOT + SLOT_GAP), SLOT + 2));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, SLOT + 2));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    selectRecent(e.getX() / (SLOT + SLOT_GAP));
                }
            });
            ToolTipManager.sharedInstance().registerComponent(this);
        }

        @Override
        public String getToolTipText(MouseEvent e) {
            int slot = e.getX() / (SLOT + SLOT_GAP);
            return slot < recent.size() ? byId[recent.get(slot)].name + "  (key " + (slot + 1) + ")" : null;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            for (int slot = 0; slot < RECENT_SLOTS; slot++) {
                int x = slot * (SLOT + SLOT_GAP);
                Shape shape = new java.awt.geom.RoundRectangle2D.Double(x, 0, SLOT, SLOT, 6, 6);
                Shape oldClip = g2.getClip();
                g2.clip(shape);
                drawChecker(g2, x, 0, SLOT, SLOT);
                g2.setClip(oldClip);

                if (slot < recent.size()) {
                    Entry entry = byId[recent.get(slot)];
                    drawFit(g2, entry.image, x + 3, 3, SLOT - 6);
                    if (entry.id == selected) {
                        g2.setColor(ACCENT);
                        g2.setStroke(new BasicStroke(2f));
                        g2.draw(new java.awt.geom.RoundRectangle2D.Double(x + 1, 1, SLOT - 2, SLOT - 2, 6, 6));
                    }
                }
                g2.setFont(getFont().deriveFont(Font.BOLD, 9f));
                g2.setColor(new Color(0, 0, 0, 200));
                g2.drawString(String.valueOf(slot + 1), x + 4, 12);
                g2.setColor(new Color(255, 255, 255, 190));
                g2.drawString(String.valueOf(slot + 1), x + 3, 11);
            }
            g2.dispose();
        }
    }

    /** Preview and description of the hovered tile, or of the selected one. */
    private final class InfoPanel extends JPanel {
        InfoPanel() {
            setPreferredSize(new Dimension(100, 92));
            setBackground(BG);
            setBorder(BorderFactory.createLineBorder(new Color(0x15, 0x17, 0x1B)));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            Entry entry = hovered != null ? hovered : byId[selected];
            boolean isSelected = entry.id == selected;

            drawChecker(g2, 12, 12, 68, 68);
            drawFit(g2, entry.image, 14, 14, 64);
            g2.setColor(isSelected ? ACCENT : new Color(0x45, 0x4A, 0x54));
            g2.setStroke(new BasicStroke(2f));
            g2.drawRect(12, 12, 68, 68);

            int x = 94;
            g2.setFont(getFont().deriveFont(Font.BOLD, 14f));
            g2.setColor(TEXT);
            g2.drawString(entry.name, x, 28);
            g2.setFont(getFont().deriveFont(12f));
            g2.setColor(TEXT_DIM);
            g2.drawString("#" + entry.id + "   " + entry.kindLabel(), x, 46);
            g2.setColor(entry.solid ? SOLID : WALKABLE);
            g2.drawString(entry.solid ? "Solid (blocks movement)" : "Walkable", x, 63);
            g2.setColor(TEXT_DIM);
            g2.drawString(isSelected ? "Selected" : "Click to select", x, 80);
            g2.dispose();
        }
    }
}
