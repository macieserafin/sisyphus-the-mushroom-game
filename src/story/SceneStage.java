package story;

import entity.Entity;
import entity.npc.NPC;
import entity.npc.NPC_Villager;
import main.GamePanel;

import java.awt.Point;
import java.util.*;
import java.util.function.BooleanSupplier;

public final class SceneStage {
    private static final class Mover {
        final Deque<Point> path = new ArrayDeque<>();
        int speed, timeout, carry;
    }

    private final GamePanel gp;
    private final NarrativeDirector director;
    private final Map<String, NPC> spawned = new LinkedHashMap<>();
    private final Map<Entity, Mover> movers = new LinkedHashMap<>();
    private final Map<String, Runnable> tickers = new LinkedHashMap<>();
    private final Map<String, java.util.function.Consumer<java.awt.Graphics2D>> drawers = new LinkedHashMap<>();
    private final Set<String> kept = new HashSet<>();
    private String scene = "";
    private String animSheet = "";
    private int animFrame, animLast, animTicks, animCounter;
    private boolean animating;

    SceneStage(GamePanel gp, NarrativeDirector director) {
        this.gp = gp;
        this.director = director;
    }

    public void begin(String scene) { this.scene = scene; }

    public void ticker(String id, Runnable ticker) { if (ticker == null) tickers.remove(id); else tickers.put(id, ticker); }

    public void drawer(String id, java.util.function.Consumer<java.awt.Graphics2D> drawer) { if (drawer == null) drawers.remove(id); else drawers.put(id, drawer); }

    public void draw(java.awt.Graphics2D g) { for (var drawer : new ArrayList<>(drawers.values())) drawer.accept(g); }

    public boolean moving() { return !movers.isEmpty(); }

    public boolean moving(String actor) {
        Entity entity = actorOrNull(actor);
        return entity != null && movers.containsKey(entity);
    }

    public Point point(String name) {
        String value = director.get("scene." + scene + ".point." + name);
        if (value.isBlank()) value = director.get("point." + name);
        if (value.isBlank()) {
            Entity actor = actorOrNull(name);
            if (actor != null) return new Point(actor.worldX, actor.worldY);
            return null;
        }
        String[] parts = value.split(",");
        double x = Double.parseDouble(parts[0].trim()), y = Double.parseDouble(parts[1].trim());
        int dx = parts.length > 2 ? Integer.parseInt(parts[2].trim()) : 0, dy = parts.length > 3 ? Integer.parseInt(parts[3].trim()) : 0;
        return new Point((int) Math.round(x * gp.tileSize) + dx, (int) Math.round(y * gp.tileSize) + dy);
    }

    boolean pointDefined(String name) {
        return !director.get("scene." + scene + ".point." + name).isBlank() || !director.get("point." + name).isBlank()
                || name.equals("player") || spawnDefined(name) || Arrays.asList(director.list("npcs")).contains(name);
    }

    private boolean spawnDefined(String actor) { return !director.get("scene." + scene + ".spawn." + actor).isBlank(); }

    public Entity actorOrNull(String id) {
        if (id.isBlank() || id.equals("player")) return gp.player;
        NPC npc = spawned.get(id);
        if (npc != null) return npc;
        return gp.entities.npcs().stream().filter(n -> director.npcId(n).equals(id)).findFirst().orElse(null);
    }

    public Entity actor(String id) {
        Entity entity = actorOrNull(id);
        if (entity == null) System.err.println("Scene " + scene + ": actor " + id + " is not present");
        return entity;
    }

    public NPC spawn(String id) {
        String[] spec = director.get("scene." + scene + ".spawn." + id).split(",");
        if (spec.length < 3) throw new IllegalStateException("Scene " + scene + " has no spawn for " + id);
        despawn(id);
        NPC_Villager npc = NPC_Villager.silent(gp, spec[0].trim(), spec[1].trim());
        npc.storyId = id;
        Point at = point(spec[2].trim());
        gp.entities.addNPC(npc, 0, 0);
        npc.worldX = at.x;
        npc.worldY = at.y;
        if (spec.length > 3) npc.scriptedFace(npc.worldX + dx(spec[3].trim()), npc.worldY + dy(spec[3].trim()));
        spawned.put(id, npc);
        return npc;
    }

    public void keep(String id) { kept.add(id); }

    private static int dx(String facing) { return switch (facing) { case "left" -> -1; case "right" -> 1; default -> 0; }; }
    private static int dy(String facing) { return switch (facing) { case "up" -> -1; case "down" -> 1; default -> 0; }; }

    public void despawn(String id) {
        NPC npc = spawned.remove(id);
        if (npc != null) { movers.remove(npc); gp.entities.removeNPC(npc); }
    }

    public void move(String actorId, List<String> points, int speed) {
        Entity entity = actor(actorId);
        if (entity == null) return;
        Mover mover = new Mover();
        for (String name : points) mover.path.add(point(name));
        mover.speed = Math.max(1, speed);
        mover.timeout = 600;
        movers.put(entity, mover);
    }

    public void place(String actorId, String pointName) {
        Entity entity = actor(actorId);
        if (entity == null) return;
        Point at = point(pointName);
        movers.remove(entity);
        if (entity == gp.player) gp.player.setWorldPosition(at.x, at.y);
        else { entity.worldX = at.x; entity.worldY = at.y; }
    }

    public void face(String actorId, String target) {
        Entity entity = actor(actorId);
        Point at = point(target);
        if (entity != null && at != null) entity.scriptedFace(at.x, at.y);
    }

    public void face(String actorId, int dx, int dy) {
        Entity entity = actor(actorId);
        if (entity != null) entity.scriptedFace(entity.worldX + dx, entity.worldY + dy);
    }

    private static String playerSheet(String sheet) { return sheet.contains("/") ? sheet : "player/anim/" + sheet; }

    public void pose(String actorId, String sheet, int frame) {
        Entity entity = actor(actorId);
        if (entity == null) return;
        if (entity == gp.player && !sheet.equals("clear")) sheet = playerSheet(sheet);
        if (entity == gp.player) {
            animating = false;
            if (sheet.equals("clear")) gp.player.clearScriptedPose(); else gp.player.scriptedPose(sheet, frame);
        } else if (entity instanceof NPC npc) {
            if (sheet.equals("clear")) npc.clearPoseFrame(); else npc.poseFrame(sheet, frame);
        }
    }

    public void animate(String sheet, int from, int to, int ticks) {
        sheet = playerSheet(sheet);
        animSheet = sheet; animFrame = from; animLast = to; animTicks = Math.max(1, ticks); animCounter = 0; animating = true;
        gp.player.scriptedPose(sheet, from);
    }

    public BooleanSupplier animationDone() { return () -> !animating; }

    public void tick() {
        for (var entry : new ArrayList<>(movers.entrySet())) step(entry.getKey(), entry.getValue());
        if (animating && ++animCounter >= animTicks) {
            animCounter = 0;
            if (animFrame == animLast) animating = false;
            else { animFrame += Integer.signum(animLast - animFrame); gp.player.scriptedPose(animSheet, animFrame); }
        }
        for (Runnable ticker : new ArrayList<>(tickers.values())) ticker.run();
    }

    private void step(Entity entity, Mover mover) {
        Point target = mover.path.peekFirst();
        if (target == null) { movers.remove(entity); return; }
        int dx = target.x - entity.worldX, dy = target.y - entity.worldY;
        double distance = Math.hypot(dx, dy);
        int budget = mover.speed;
        int x = entity.worldX, y = entity.worldY;
        if (distance <= budget) { x = target.x; y = target.y; }
        else { x += (int) Math.round(dx / distance * budget); y += (int) Math.round(dy / distance * budget); }
        boolean blocked = entity == gp.player && (gp.cChecker.checkTileAt(entity, x, y) || gp.cChecker.checkObjectAt(entity, x, y));
        if (blocked || --mover.timeout <= 0) { finishBlocked(entity, mover); return; }
        entity.scriptedStep(x, y);
        if (x == target.x && y == target.y) {
            mover.path.removeFirst();
            if (mover.path.isEmpty()) { movers.remove(entity); entity.scriptedFace(x + Integer.signum(dx), y + Integer.signum(dy)); }
        }
    }

    private void finishBlocked(Entity entity, Mover mover) {
        Point last = mover.path.peekLast();
        movers.remove(entity);
        System.err.println("Scene " + scene + ": movement ended early for " + (entity == gp.player ? "player" : entity.getClass().getSimpleName()));
        if (last != null && entity != gp.player && !onScreen(entity) && !onScreen(last)) { entity.worldX = last.x; entity.worldY = last.y; }
    }

    private boolean onScreen(Entity entity) { return onScreen(new Point(entity.worldX, entity.worldY)); }

    private boolean onScreen(Point at) {
        var view = gp.sceneCamera.viewport();
        view.grow(gp.tileSize, gp.tileSize);
        return view.contains(at);
    }

    void reset() {
        for (Entity entity : movers.keySet()) if (entity != gp.player) entity.scriptedFace(entity.worldX, entity.worldY);
        movers.clear();
        tickers.clear();
        drawers.clear();
        animating = false;
        for (String id : new ArrayList<>(spawned.keySet())) if (!kept.contains(id)) despawn(id);
        kept.clear();
        for (NPC npc : gp.entities.npcs()) npc.clearPoseFrame();
        scene = "";
    }
}
