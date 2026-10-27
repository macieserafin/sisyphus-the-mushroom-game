package tile;

import java.awt.image.BufferedImage;

public class Tile {

    public String name = "";
    public BufferedImage image;
    public boolean collision = false;
    // Object tiles have transparency and belong on the object layer (furniture, buildings, decor).
    public boolean objectLayer = false;
    // For cells of a big object (house, well...): which cell this is and how big the whole object is.
    public String group;
    public int groupX, groupY, groupWidth, groupHeight;

    // Animation frames; frames[0] is the same image as `image`.
    public BufferedImage[] frames;

    // Terrain blending: a tile spills a soft edge onto neighbours with a lower priority.
    // -1 means the tile neither spills nor receives edges (walls, floors, bridges).
    public int blendPriority = -1;
    // Tile whose texture is used for the spilled edge (decor tiles spill their ground, not the object).
    public int blendSource = -1;

    public BufferedImage frame(int animationTick) {
        if (frames == null || frames.length <= 1) {
            return image;
        }
        return frames[(animationTick / TileSet.ANIMATION_FRAME_TICKS) % frames.length];
    }
}
