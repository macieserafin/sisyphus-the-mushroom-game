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
        p.setProperty("version", "2");
        for (Enemy e : gp.entities.enemies()) if (!e.persistenceId.isEmpty() && e.alive) {
            String key = "enemy." + e.persistenceId;
            p.setProperty(key, e.health + "," + e.worldX + "," + e.worldY);
            p.setProperty("maximum."+e.persistenceId,Integer.toString(e.maxHealth));
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
        restore(gp,p,false);
    }
    public static void restore(GamePanel gp, Properties p, boolean returned) {
        if (p == null || !Values.bool(p, "captured")) return;
        for (Enemy e : new ArrayList<>(gp.entities.enemies())) {
            String saved = p.getProperty("enemy." + e.persistenceId);
            if (saved == null) {
                if(e.persistenceId.startsWith("placed:"))continue;
                if(e.newInWorldVersion&&Values.integer(p,"version",1)<2)continue;
                gp.entities.removeEnemy(e); continue;
            }
            String[] fields = saved.split(",");
            try {
                int x=Integer.parseInt(fields[1]),y=Integer.parseInt(fields[2]);
                if(Values.integer(p,"version",1)<2&&e.relocatedSnapshot){x=e.worldX;y=e.worldY;}
                e.restoreSnapshot(returned||Values.bool(p,"regenerate")?e.maxHealth:Integer.parseInt(fields[0]),x,y);
                if(gp.cChecker.checkTileAt(e,e.worldX,e.worldY))e.restoreSnapshot(e.health,e.spawnWorldX,e.spawnWorldY);
            }
            catch (RuntimeException ex) { throw new IllegalStateException("Invalid enemy snapshot", ex); }
        }
        for (SuperObject o : new ArrayList<>(gp.entities.objects())) {
            if (!o.persistenceId.isEmpty() && !o.persistenceId.startsWith("placed:") && !Values.bool(p, "object." + o.persistenceId)) gp.entities.removeObject(o);
        }
        for (int i = 0, count = Math.min(2000, Math.max(0, Values.integer(p, "drops", 0))); i < count; i++) {
            String prefix = "drop." + i + ".";
            Properties state = new Properties();
            for (String k : p.stringPropertyNames()) if (k.startsWith(prefix)) state.setProperty(k.substring(prefix.length()), p.getProperty(k));
            double x = Values.decimal(state, "x", 0), y = Values.decimal(state, "y", 0);
            SuperObject o = switch (state.getProperty("type", "")) {
                case "MushroomPickup" -> new MushroomPickup(x, y, Math.max(0, Values.integer(state, "value", 0)));
                case "HeartPickup" -> new HeartPickup(x, y);
                default -> null;
            };
            if (o != null) { o.restoreSnapshot(state); gp.entities.addFreeObject(o); }
        }
    }

    /** Only survivors have enemy entries. Missing entries remain permanently dead. */
    public static void regenerateSurvivors(Properties world) {
        for(String key:new ArrayList<>(world.stringPropertyNames())) {
            if(!key.startsWith("world."))continue;
            int split=key.indexOf('#');if(split<0)continue;
            String prefix=key.substring(0,split+1),local=key.substring(split+1);
            if(local.equals("captured"))world.setProperty(prefix+"regenerate","true");
            if(local.startsWith("enemy.")) {
                String max=world.getProperty(prefix+"maximum."+local.substring(6));
                String[] fields=world.getProperty(key).split(",");
                if(max!=null&&fields.length==3)world.setProperty(key,max+","+fields[1]+","+fields[2]);
            }
        }
    }
}
