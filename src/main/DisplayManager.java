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

    private final GamePanel gp;
    private static final String MODE_KEY = "displayMode";

    private volatile Mode mode = Mode.WINDOWED;

    public DisplayManager(GamePanel gp) {
        this.gp = gp;
    }

    public Mode mode() {
        return mode;
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
                frame.pack();
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
