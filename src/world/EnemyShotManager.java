package world;

import entity.combat.Hit;
import entity.combat.HitResult;
import entity.combat.HitType;
import entity.enemy.EnemyShot;
import main.GamePanel;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

// Boss projectiles and hazards (bullet-hell patterns). Shots hit the player's small projectile hitbox; a rolling
// player passes through them untouched; the sword cuts SMALL shots.
public class EnemyShotManager {
    private final GamePanel gp;
    private final List<EnemyShot> shots = new ArrayList<>();
    private final List<EnemyShot> spawned = new ArrayList<>();
    private int tick = 0;

    public EnemyShotManager(GamePanel gp) {
        this.gp = gp;
    }

    public void add(EnemyShot shot) {
        spawned.add(shot);
    }

    public void clear() {
        shots.clear();
        spawned.clear();
    }

    public int count() {
        return shots.size() + spawned.size();
    }

    public int tick() {
        return tick;
    }

    public List<EnemyShot> shots() {
        return shots;
    }

    // Removes every shot of one owner, with a pop (a boss dying or changing phase).
    public void clearOwner(Object owner, boolean pop) {
        spawned.removeIf(s -> s.owner == owner);
        for (EnemyShot s : shots) {
            if (s.owner == owner && s.alive) {
                if (pop && (s.kind == EnemyShot.Kind.SMALL || s.kind == EnemyShot.Kind.HEAVY)) {
                    gp.effects.burst(s.x, s.y, 3, 1.2, impactColors(s));
                }
                s.alive = false;
            }
        }
    }

    public void update() {
        tick++;
        shots.addAll(spawned);
        spawned.clear();
        Rectangle hitbox = gp.player.getProjectileHitBox();
        for (EnemyShot s : shots) {
            if (!s.alive) {
                continue;
            }
            s.age++;
            if (s.age > s.maxAge) {
                expire(s);
                continue;
            }
            switch (s.kind) {
                case SMALL, HEAVY -> moveBall(s);
                case WAVE -> s.radius += s.growth;
                default -> { }
            }
            if (!s.alive || !s.armed()) {
                continue;
            }
            if (s.kind == EnemyShot.Kind.ZONE && s.age == s.delay) {
                gp.effects.ring(s.x, s.y, s.radius * 1.3, 14, s.core, true);
                gp.effects.burst(s.x, s.y, 6, 2.2, EffectsManager.GOO);
            }
            if (!s.hitPlayer && gp.player.alive && touches(s, hitbox)) {
                Hit hit = new Hit(s.damage, HitType.RANGED, s.owner, (int) s.x, (int) s.y, s.knockback, null);
                HitResult result = gp.player.receiveHit(hit);
                if (result == HitResult.APPLIED) {
                    s.hitPlayer = true;
                    if (s.kind == EnemyShot.Kind.SMALL || s.kind == EnemyShot.Kind.HEAVY) {
                        s.alive = false;
                        gp.effects.burst(s.x, s.y, 8, 2.0, impactColors(s));
                    }
                }
            }
        }
        shots.removeIf(s -> !s.alive);
    }

    private void moveBall(EnemyShot s) {
        if (!s.armed()) {
            return;
        }
        if (s.curve != 0) {
            double cos = Math.cos(s.curve), sin = Math.sin(s.curve);
            double nvx = s.vx * cos - s.vy * sin;
            s.vy = s.vx * sin + s.vy * cos;
            s.vx = nvx;
        }
        s.vx *= s.accel;
        s.vy *= s.accel;
        s.x += s.vx;
        s.y += s.vy;
        if (solidAt(s.x, s.y)) {
            expire(s);
        }
    }

    private boolean solidAt(double x, double y) {
        int col = (int) Math.floor(x / gp.tileSize);
        int row = (int) Math.floor(y / gp.tileSize);
        if (col < 0 || row < 0 || col >= gp.tileM.getWorldCols() || row >= gp.tileM.getWorldRows()) {
            return true;
        }
        return gp.tileM.isCollision(col, row);
    }

    private void expire(EnemyShot s) {
        s.alive = false;
        if (s.kind == EnemyShot.Kind.SMALL || s.kind == EnemyShot.Kind.HEAVY) {
            gp.effects.burst(s.x, s.y, s.kind == EnemyShot.Kind.HEAVY ? 8 : 3, 1.4, impactColors(s));
        }
        if (s.kind == EnemyShot.Kind.HEAVY && s.burstInto > 0) {
            gp.effects.ring(s.x, s.y, 40, 12, s.core, false);
            double offset = Math.random() * Math.PI * 2;
            for (int i = 0; i < s.burstInto; i++) {
                double angle = offset + i * Math.PI * 2 / s.burstInto;
                EnemyShot child = EnemyShot.small(s.owner, s.x, s.y, angle, 3.2);
                child.body = s.body;
                child.core = s.core;
                child.edge = s.edge;
                add(child);
            }
        }
    }

    private boolean touches(EnemyShot s, Rectangle box) {
        double nearestX = Math.max(box.x, Math.min(s.x, box.x + box.width));
        double nearestY = Math.max(box.y, Math.min(s.y, box.y + box.height));
        double d = Math.hypot(s.x - nearestX, s.y - nearestY);
        switch (s.kind) {
            case ZONE: {
                // zones and waves lie on the ground, drawn squashed to 60% height
                double ground = Math.hypot(s.x - nearestX, (s.y - nearestY) / 0.6);
                return s.age - s.delay < s.activeTicks && ground <= s.radius;
            }
            case WAVE: {
                double cx = box.getCenterX(), cy = box.getMaxY() - 4;
                double dist = Math.hypot(cx - s.x, (cy - s.y) / 0.6);
                return Math.abs(dist - s.radius) <= s.thickness;
            }
            default:
                return d <= s.radius * 0.85;
        }
    }

    // Sword swing: SMALL shots inside the swing are destroyed; HEAVY ones only spark. Returns how many were cut.
    public int cut(Shape area) {
        int cut = 0;
        boolean clanged = false;
        for (EnemyShot s : shots) {
            if (!s.alive || !s.armed()) {
                continue;
            }
            if (s.kind != EnemyShot.Kind.SMALL && s.kind != EnemyShot.Kind.HEAVY) {
                continue;
            }
            double r = s.radius;
            if (!area.intersects(s.x - r, s.y - r, r * 2, r * 2)) {
                continue;
            }
            if (s.kind == EnemyShot.Kind.SMALL) {
                s.alive = false;
                cut++;
                gp.effects.burst(s.x, s.y, 5, 2.4, impactColors(s));
                gp.effects.ring(s.x, s.y, 16, 8, s.core, false);
            } else if (!clanged) {
                clanged = true;
                gp.effects.sparks(s.x, s.y, -s.vx, -s.vy, 6, EffectsManager.SPARK);
            }
        }
        return cut;
    }

    public void draw(Graphics2D g2) {
        int scale = gp.scale;
        Composite oldComposite = g2.getComposite();
        Stroke oldStroke = g2.getStroke();
        for (EnemyShot s : shots) {
            if (!s.alive) continue;
            int sx = (int) Math.round(s.x - gp.player.worldX + gp.player.screenX);
            int sy = (int) Math.round(s.y - gp.player.worldY + gp.player.screenY);
            switch (s.kind) {
                case ZONE -> drawZone(g2, s, sx, sy);
                case WAVE -> drawWave(g2, s, sx, sy);
                default -> drawBall(g2, s, sx, sy, scale);
            }
        }
        g2.setComposite(oldComposite);
        g2.setStroke(oldStroke);
    }

    private void drawBall(Graphics2D g2, EnemyShot s, int sx, int sy, int scale) {
        if (s.spriteFrames != null && s.spriteFrames.length > 0 && s.armed()) {
            var frame = s.spriteFrames[(s.age / Math.max(1, s.spriteFrameTicks)) % s.spriteFrames.length];
            int width = s.spriteWorldSize > 0 ? s.spriteWorldSize : frame.getWidth() * scale;
            int height = s.spriteWorldSize > 0 ? s.spriteWorldSize : frame.getHeight() * scale;
            RenderingHints previousHints = g2.getRenderingHints();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g2.drawImage(frame, sx - width / 2, sy - height / 2, width, height, null);
            g2.setRenderingHints(previousHints);
            return;
        }
        if (!s.armed()) {
            // spawning: a growing blob with a blinking warning ring
            double t = s.age / (double) Math.max(1, s.delay);
            int r = snap(s.radius * (0.4 + 0.6 * t), scale);
            g2.setComposite(AlphaComposite.SrcOver.derive(0.5f));
            g2.setColor(s.body);
            g2.fillOval(sx - r, sy - r, r * 2, r * 2);
            g2.setComposite(AlphaComposite.SrcOver);
            return;
        }
        // trail for fast shots
        double speed = s.speed();
        if (speed > 3.5) {
            g2.setComposite(AlphaComposite.SrcOver.derive(0.35f));
            g2.setColor(s.body);
            int tr = snap(s.radius * 0.7, scale);
            int tx = (int) Math.round(sx - s.vx * 2.2), ty = (int) Math.round(sy - s.vy * 2.2);
            g2.fillOval(tx - tr, ty - tr, tr * 2, tr * 2);
            g2.setComposite(AlphaComposite.SrcOver);
        }
        int wobble = s.kind == EnemyShot.Kind.HEAVY ? (int) Math.round(Math.sin((tick + s.x) * 0.25) * scale) : 0;
        int r = snap(s.radius, scale);
        g2.setColor(s.edge);
        g2.fillOval(sx - r - scale, sy - r - scale + wobble / 2, (r + scale) * 2 + wobble, (r + scale) * 2 - wobble);
        g2.setColor(s.body);
        g2.fillOval(sx - r, sy - r + wobble / 2, r * 2 + wobble, r * 2 - wobble);
        int cr = Math.max(scale, snap(s.radius * 0.45, scale));
        g2.setColor(s.core);
        g2.fillOval(sx - cr - scale, sy - cr - scale, cr * 2, cr * 2);
    }

    private void drawZone(Graphics2D g2, EnemyShot s, int sx, int sy) {
        int r = (int) Math.round(s.radius);
        int ry = (int) Math.round(s.radius * 0.6);
        if (!s.armed()) {
            double t = s.age / (double) Math.max(1, s.delay);
            boolean blink = s.delay - s.age < 20 && (s.age / 3) % 2 == 0;
            g2.setComposite(AlphaComposite.SrcOver.derive((float) (0.15 + 0.25 * t)));
            g2.setColor(s.body);
            g2.fillOval(sx - r, sy - ry, r * 2, ry * 2);
            g2.setComposite(AlphaComposite.SrcOver.derive(blink ? 1f : 0.7f));
            g2.setStroke(new BasicStroke(gp.scale));
            g2.setColor(blink ? s.core : s.body);
            g2.drawOval(sx - r, sy - ry, r * 2, ry * 2);
            int inner = (int) Math.round(r * t), innerY = (int) Math.round(ry * t);
            g2.drawOval(sx - inner, sy - innerY, inner * 2, innerY * 2);
            g2.setComposite(AlphaComposite.SrcOver);
            return;
        }
        float fade = 1f - (s.age - s.delay) / (float) Math.max(1, s.activeTicks);
        g2.setComposite(AlphaComposite.SrcOver.derive(Math.max(0f, 0.8f * fade)));
        g2.setColor(s.core);
        g2.fillOval(sx - r, sy - ry, r * 2, ry * 2);
        g2.setComposite(AlphaComposite.SrcOver);
    }

    private void drawWave(Graphics2D g2, EnemyShot s, int sx, int sy) {
        int r = (int) Math.round(s.radius);
        int ry = (int) Math.round(s.radius * 0.6);
        float fade = 1f - s.age / (float) Math.max(1, s.maxAge) * 0.6f;
        g2.setComposite(AlphaComposite.SrcOver.derive(Math.max(0.2f, 0.85f * fade)));
        g2.setStroke(new BasicStroke((float) s.thickness, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(s.body);
        g2.drawOval(sx - r, sy - ry, r * 2, ry * 2);
        g2.setStroke(new BasicStroke(gp.scale));
        g2.setColor(s.core);
        g2.drawOval(sx - r, sy - ry, r * 2, ry * 2);
        g2.setComposite(AlphaComposite.SrcOver);
    }

    private static Color[] impactColors(EnemyShot shot) {
        return shot.impactColors == null ? EffectsManager.GOO : shot.impactColors;
    }

    private static int snap(double v, int scale) {
        return Math.max(scale, (int) Math.round(v / scale) * scale);
    }
}
