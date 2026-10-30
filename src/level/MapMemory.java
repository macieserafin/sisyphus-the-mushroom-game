package level;

import entity.enemy.Enemy;
import main.GamePanel;
import object.*;
import save.Values;
import java.util.*;

public final class MapMemory {
    private MapMemory() {}

    public static Properties capture(GamePanel gp) {
        Properties p = new Properties();
        p.setProperty("captured", "true");
        for (Enemy e : gp.entities.enemies()) if (!e.persistenceId.isEmpty() && e.alive) {
            String key = "enemy." + e.persistenceId;
            p.setProperty(key, e.health + "," + e.worldX + "," + e.worldY);
        }
        int dynamic = 0;
        for (SuperObject o : gp.entities.objects()) {
            if (!o.persistenceId.isEmpty()) p.setProperty("object." + o.persistenceId, "true");
            else {
                Properties state = o.snapshot();
                if (state == null) continue;
                String prefix = "drop." + dynamic++ + ".";
                p.setProperty(prefix + "type", o.getClass().getSimpleName());
                state.forEach((k, v) -> p.setProperty(prefix + k, v.toString()));
            }
        }
        p.setProperty("drops", "" + dynamic);
        return p;
    }

    public static void restore(GamePanel gp, Properties p) {
        if (p == null || !Values.bool(p, "captured")) return;
        for (Enemy e : new ArrayList<>(gp.entities.enemies())) {
            String saved = p.getProperty("enemy." + e.persistenceId);
            if (saved == null) { gp.entities.removeEnemy(e); continue; }
            String[] fields = saved.split(",");
            try { e.restoreSnapshot(Integer.parseInt(fields[0]), Integer.parseInt(fields[1]), Integer.parseInt(fields[2])); }
            catch (RuntimeException ex) { throw new IllegalStateException("Invalid enemy snapshot", ex); }
        }
        for (SuperObject o : new ArrayList<>(gp.entities.objects())) {
            if (!o.persistenceId.isEmpty() && !Values.bool(p, "object." + o.persistenceId)) gp.entities.removeObject(o);
        }
        for (int i = 0, count = Math.min(2000, Math.max(0, Values.integer(p, "drops", 0))); i < count; i++) {
            String prefix = "drop." + i + ".";
            Properties state = new Properties();
            for (String k : p.stringPropertyNames()) if (k.startsWith(prefix)) state.setProperty(k.substring(prefix.length()), p.getProperty(k));
            double x = Values.decimal(state, "x", 0), y = Values.decimal(state, "y", 0);
            SuperObject o = switch (state.getProperty("type", "")) {
                case "MushroomPickup" -> new MushroomPickup(x, y, Math.max(0, Values.integer(state, "value", 0)));
                case "HeartPickup" -> new HeartPickup(x, y);
                case "PowerUp" -> new PowerUp(PowerUp.Kind.valueOf(state.getProperty("kind")), x, y);
                default -> null;
            };
            if (o != null) { o.restoreSnapshot(state); gp.entities.addFreeObject(o); }
        }
    }
}
