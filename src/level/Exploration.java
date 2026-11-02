package level;

import java.awt.Rectangle;
import java.util.*;

/** Monotonic discovery, independent of atmospheric fog and entity persistence. */
public final class Exploration {
    private final Map<String, BitSet> maps = new TreeMap<>();
    public boolean known(String map, int col, int row, int width) {
        BitSet bits=maps.get(map);
        return bits!=null && col>=0 && col<width && row>=0 && bits.get(row*width+col);
    }
    public void reveal(String map, int cols, int rows, Rectangle pixels, int size) {
        BitSet bits = maps.computeIfAbsent(map, k -> new BitSet());
        int left = Math.max(0, Math.floorDiv(pixels.x, size));
        int right = Math.min(cols, Math.floorDiv(pixels.x + pixels.width - 1, size) + 1);
        int top = Math.max(0, Math.floorDiv(pixels.y, size));
        int bottom = Math.min(rows, Math.floorDiv(pixels.y + pixels.height - 1, size) + 1);
        for (int y = top; y < bottom && right > left; y++) bits.set(y * cols + left, y * cols + right);
    }
    public void clear(String map) { maps.remove(map); }
    public void save(Properties p) {
        maps.forEach((id, bits) -> p.setProperty("explored." + id, Base64.getEncoder().encodeToString(bits.toByteArray())));
    }
    public void load(Properties p) {
        maps.clear();
        for (String key : p.stringPropertyNames()) if (key.startsWith("explored.")) {
            try { maps.put(key.substring(9), BitSet.valueOf(Base64.getDecoder().decode(p.getProperty(key)))); }
            catch (IllegalArgumentException ignored) { /* A broken discovery mask must not break the slot. */ }
        }
    }
}
