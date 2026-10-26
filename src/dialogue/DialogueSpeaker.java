package dialogue;

import java.awt.*;
import java.awt.image.BufferedImage;

public class DialogueSpeaker {
    private final String name;
    private final BufferedImage portrait;
    private final Color accentColor;

    public DialogueSpeaker(String name, BufferedImage portrait, Color accentColor) {
        this.name = name == null || name.trim().isEmpty() ? "Unknown" : name;
        this.portrait = portrait;
        this.accentColor = accentColor == null ? Color.WHITE : accentColor;
    }

    public String name() {
        return name;
    }

    public BufferedImage portrait() {
        return portrait;
    }

    public Color accentColor() {
        return accentColor;
    }
}
