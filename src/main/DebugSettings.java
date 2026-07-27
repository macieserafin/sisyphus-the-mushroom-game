package main;

public class DebugSettings {
    private boolean overlayEnabled = false;

    public boolean isOverlayEnabled() {
        return overlayEnabled;
    }

    public void toggleOverlay() {
        overlayEnabled = !overlayEnabled;
        System.out.println("Debug overlay: " + (overlayEnabled ? "ON" : "OFF"));
    }
}
