package tile;

import java.awt.image.BufferedImage;

public class Tile {
    public String name = "";
    public BufferedImage image;
    public boolean collision = false;
    public boolean objectLayer = false;
    public String group;
    public int groupX, groupY, groupWidth, groupHeight;

    public BufferedImage[] frames;

    // Terrain blending: a tile spills a soft edge onto neighbours with a lower priority.
    // -1 means the tile neither spills nor receives edges (walls, floors, bridges).
    public int blendPriority = -1;
    public int blendSource = -1;

    public BufferedImage frame(int animationTick) {
        if (frames == null || frames.length <= 1) {
            return image;
        }
        return frames[(animationTick / TileSet.ANIMATION_FRAME_TICKS) % frames.length];
    }
}
