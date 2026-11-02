package level;

import java.io.*;
import java.util.*;

public record EntityPlacement(String id, Type type, int col, int row, int dx, int dy,
                              String when, Map<String, String> properties) {
    public enum Type { KEY, CHEST, DOCUMENT, PORTAL, NPC, SLIME, SKELETON }

    public EntityPlacement {
        if (id == null || !id.matches("[A-Za-z0-9_.-]+")) throw new IllegalArgumentException("Invalid entity ID: " + id);
        Objects.requireNonNull(type, "Entity type");
        when = when == null ? "" : when;
        properties = Map.copyOf(properties);
    }
    public String value(String key, String fallback) { return properties.getOrDefault(key, fallback); }
    public EntityPlacement at(int x, int y) { return new EntityPlacement(id, type, x, y, dx, dy, when, properties); }
    public EntityPlacement copy(String nextId) { return new EntityPlacement(nextId, type, col, row, dx, dy, when, properties); }
    public static String path(String map) {
        return map.endsWith(".txt") ? map.substring(0, map.length() - 4) + "_entities.properties" : map + "_entities.properties";
    }
    public static List<EntityPlacement> read(Reader reader) throws IOException {
        Properties p = new Properties(); p.load(reader);
        if (!p.getProperty("version", "1").equals("1")) throw new IllegalArgumentException("Unsupported entity format");
        List<EntityPlacement> result = new ArrayList<>(); Set<String> ids = new HashSet<>();
        for (String raw : p.getProperty("entities", "").split(",")) {
            String id = raw.trim(); if (id.isEmpty()) continue;
            if (!ids.add(id)) throw new IllegalArgumentException("Duplicate entity ID: " + id);
            String prefix = "entity." + id + ".";
            Map<String, String> fields = new TreeMap<>();
            for (String key : p.stringPropertyNames()) if (key.startsWith(prefix + "prop.")) fields.put(key.substring((prefix + "prop.").length()), p.getProperty(key));
            result.add(new EntityPlacement(id, Type.valueOf(p.getProperty(prefix + "type", "")),
                    number(p, prefix + "col", 0), number(p, prefix + "row", 0), number(p, prefix + "dx", 0), number(p, prefix + "dy", 0), p.getProperty(prefix + "when", ""), fields));
        }
        return List.copyOf(result);
    }
    private static int number(Properties p, String key, int fallback) { return Integer.parseInt(p.getProperty(key, "" + fallback)); }
    public static String write(List<EntityPlacement> entities) {
        StringBuilder out = new StringBuilder("version=1\nentities=");
        out.append(String.join(",", entities.stream().map(EntityPlacement::id).toList())).append('\n');
        Set<String> ids = new HashSet<>();
        for (var e : entities) {
            if (!ids.add(e.id)) throw new IllegalArgumentException("Duplicate entity ID: " + e.id);
            String prefix = "entity." + e.id + ".";
            line(out, prefix + "type", e.type.name()); line(out, prefix + "col", "" + e.col); line(out, prefix + "row", "" + e.row);
            line(out, prefix + "dx", "" + e.dx); line(out, prefix + "dy", "" + e.dy); line(out, prefix + "when", e.when);
            new TreeMap<>(e.properties).forEach((k,v) -> line(out, prefix + "prop." + k, v));
        }
        return out.toString();
    }
    private static void line(StringBuilder out, String key, String value) {
        if (!key.matches("[A-Za-z0-9_.-]+")) throw new IllegalArgumentException("Invalid property: " + key);
        out.append(key).append('=');
        for (int i=0;i<value.length();i++) {
            char c=value.charAt(i);
            switch(c) {
                case '\\' -> out.append("\\\\"); case '\n' -> out.append("\\n"); case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t"); case ' ' -> out.append(i==0?"\\ ":" "); default -> out.append(c);
            }
        }
        out.append('\n');
    }
    public static List<String> validate(List<EntityPlacement> entities, int cols, int rows) {
        List<String> errors = new ArrayList<>(); Set<String> ids = new HashSet<>();
        for (var e : entities) {
            if (!ids.add(e.id)) errors.add(e.id + ": duplicate ID");
            if (e.col < 0 || e.row < 0 || e.col >= cols || e.row >= rows) errors.add(e.id + ": outside map");
            if (Math.abs((long)e.dx)>47 || Math.abs((long)e.dy)>47) errors.add(e.id + ": pixel offsets must be -47..47");
            if (e.type==Type.DOCUMENT && e.value("document", "").isBlank()) errors.add(e.id + ": document is required");
            if (e.type==Type.NPC && e.value("sprite", "").isBlank()) errors.add(e.id + ": sprite is required");
            if (e.type==Type.PORTAL) {
                try { TransitionTarget.Type.valueOf(e.value("target", "HUB"));
                    if(e.value("target", "HUB").equals("INTERIOR"))Interior.valueOf(e.value("interior", ""));
                    Integer.parseInt(e.value("targetCol", "0"));Integer.parseInt(e.value("targetRow", "0"));
                } catch(RuntimeException ex) { errors.add(e.id + ": invalid portal target"); }
            }
            if(e.type==Type.CHEST)try {int amount=Integer.parseInt(e.value("mushrooms","10"));if(amount<0||amount>100000)throw new IllegalArgumentException();}
                catch(RuntimeException ex){errors.add(e.id + ": mushrooms must be 0..100000");}
        }
        return List.copyOf(errors);
    }
}
