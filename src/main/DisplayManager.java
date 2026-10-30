package main;

import javax.swing.*;
import java.awt.*;

public class DisplayManager {
    public enum Mode {
        WINDOWED("Windowed"),
        BORDERLESS("Borderless"),
        FULLSCREEN("Fullscreen");

        public final String label;

        Mode(String label) {
            this.label = label;
        }

        public Mode next() {
            return values()[(ordinal() + 1) % values().length];
        }

        public Mode previous() {
            return values()[(ordinal() + values().length - 1) % values().length];
        }
    }

    public static final int MAX_SCALE = 4;

    private static final int DEFAULT_FRAME_INSETS_X = 16;
    private static final int DEFAULT_FRAME_INSETS_Y = 48;

    private final GamePanel gp;
    private static final String MODE_KEY = "displayMode";
    private static final String SCALE_KEY = "windowScale";

    private volatile Mode mode = Mode.WINDOWED;
    private int requestedScale;

    public DisplayManager(GamePanel gp) {
        this.gp = gp;
        requestedScale = Math.max(1, Math.min(MAX_SCALE, gp.settings.getInt(SCALE_KEY, 1)));
    }

    public Mode mode() {
        return mode;
    }


    public int windowScale() {
        return Math.min(requestedScale, maxScale());
    }

    public int windowedWidth() {
        return gp.screenWidth * windowScale();
    }

    public int windowedHeight() {
        return gp.screenHeight * windowScale();
    }

    public int maxScale() {
        if (GraphicsEnvironment.isHeadless()) {
            return MAX_SCALE;
        }
        Rectangle usable = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        int insetsX = DEFAULT_FRAME_INSETS_X;
        int insetsY = DEFAULT_FRAME_INSETS_Y;
        Window window = SwingUtilities.getWindowAncestor(gp);
        if (window instanceof Frame frame && !frame.isUndecorated()) {
            Insets in = window.getInsets();
            insetsX = in.left + in.right;
            insetsY = in.top + in.bottom;
        }
        int scale = 1;
        for (int s = 2; s <= MAX_SCALE; s++) {
            if (gp.screenWidth * s + insetsX <= usable.width && gp.screenHeight * s + insetsY <= usable.height) {
                scale = s;
            }
        }
        return scale;
    }

    public void changeWindowScale(int direction) {
        int max = maxScale();
        int next = windowScale() + direction;
        if (next > max) {
            next = 1;
        } else if (next < 1) {
            next = max;
        }
        setWindowScale(next);
    }

    public void setWindowScale(int scale) {
        requestedScale = Math.max(1, Math.min(MAX_SCALE, scale));
        gp.settings.putInt(SCALE_KEY, requestedScale);
        if (mode == Mode.WINDOWED) {
            SwingUtilities.invokeLater(() -> apply(Mode.WINDOWED));
        }
    }


    public void setMode(Mode next) {
        if (next == mode) {
            return;
        }
        mode = next;
        gp.settings.put(MODE_KEY, next.name());
        SwingUtilities.invokeLater(() -> apply(next));
    }

    public void restoreSaved() {
        Mode saved;
        try {
            saved = Mode.valueOf(gp.settings.getString(MODE_KEY, Mode.WINDOWED.name()));
        } catch (IllegalArgumentException e) {
            return;
        }
        if (saved != Mode.WINDOWED) {
            mode = saved;
            SwingUtilities.invokeLater(() -> apply(saved));
        }
    }

    private void apply(Mode next) {
        Window window = SwingUtilities.getWindowAncestor(gp);
        if (!(window instanceof JFrame frame)) {
            return;
        }
        GraphicsDevice device = frame.getGraphicsConfiguration().getDevice();
        if (device.getFullScreenWindow() == frame) {
            device.setFullScreenWindow(null);
        }

        boolean undecorated = next != Mode.WINDOWED;
        if (frame.isUndecorated() != undecorated) {
            frame.dispose();
            frame.setUndecorated(undecorated);
        }

        switch (next) {
            case WINDOWED -> {
                gp.setPreferredSize(new Dimension(windowedWidth(), windowedHeight()));
                frame.pack();
                // pack() sometimes keeps the old size when the window was already as large as a clamped request
                Insets in = frame.getInsets();
                frame.setSize(windowedWidth() + in.left + in.right, windowedHeight() + in.top + in.bottom);
                frame.validate();
                frame.setLocationRelativeTo(null);
            }
            case BORDERLESS -> frame.setBounds(device.getDefaultConfiguration().getBounds());
            case FULLSCREEN -> {
                if (device.isFullScreenSupported()) {
                    device.setFullScreenWindow(frame);
                } else {
                    frame.setBounds(device.getDefaultConfiguration().getBounds());
                }
            }
        }

        frame.setVisible(true);
        frame.toFront();
        gp.requestFocusInWindow();
    }
}
