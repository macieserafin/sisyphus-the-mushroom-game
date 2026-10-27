package main;

import javax.swing.*;
import java.awt.*;

/** Switches the game window between windowed, borderless full window and exclusive full screen. */
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
        SwingUtilities.invokeLater(() -> apply(next));
    }

    // Must run on the event thread: decorations can only change while the frame is not displayable.
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
