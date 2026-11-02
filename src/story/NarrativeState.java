package story;

import java.util.*;

/** Slot-local knowledge. IDs come from resource definitions, never from displayed text. */
public final class NarrativeState {
    private final Set<String> facts = new TreeSet<>();
    private long ticks = 8 * 60 * 60L;
    public boolean has(String id) { return facts.contains(id); }
    public void set(String id) { if (!id.isBlank()) facts.add(id); }
    public void remove(String id) { facts.remove(id); }
    public Set<String> facts() { return Collections.unmodifiableSet(facts); }
    public boolean matches(String expression) {
        for(String alternative:expression.split("\\|\\|"))if(matchesAll(alternative))return true;
        return false;
    }
    private boolean matchesAll(String expression) {
        for (String term : expression.split("&")) {
            term = term.trim();
            if (term.isEmpty()) continue;
            if (term.startsWith("!") ? has(term.substring(1)) : !has(term)) return false;
        }
        return true;
    }
    public void tick() { ticks++; }
    public long ticks() { return ticks; }
    public void advanceHour() { ticks += 3600; }
    public void shiftMinutes(int minutes) {
        long next=ticks+minutes*60L;
        ticks=next<0?Math.floorMod(next,86400):next;
    }
    public void hour(int hour) { ticks = ticks / 86400 * 86400 + Math.floorMod(hour, 24) * 3600L; }
    public String time() { return String.format(Locale.ROOT, "%02d:%02d", ticks / 3600 % 24, ticks / 60 % 60); }
    public float night() {
        return world.DayCycle.at(ticks).night();
    }
    public void save(Properties p) {
        p.setProperty("narrative.version", "1");
        p.setProperty("narrative.facts", String.join(",", facts));
        p.setProperty("narrative.ticks", Long.toString(ticks));
    }
    public void load(Properties p) {
        facts.clear();
        for (String id : p.getProperty("narrative.facts", "").split(",")) if (!id.isBlank()) facts.add(id);
        try { ticks = Math.max(0, Long.parseLong(p.getProperty("narrative.ticks", "28800"))); }
        catch (NumberFormatException e) { ticks = 28800; }
    }
}
