package entity.enemy;

import entity.combat.Hit;
import entity.combat.HitResult;
import entity.combat.HitType;
import main.GamePanel;
import main.UiSkin;
import object.HeartPickup;
import world.EffectsManager;

import java.awt.*;
import java.util.Map;
import java.util.Random;

// Guards do not die: their health is a stun bar. Emptied, a guard stays down (no moving, no attacking) until the King
// raises his barrier again; while both are down the barrier is broken (see Enemy_SlimeKing).
public class Enemy_RoyalGuard extends Enemy_Slime {
    public enum Kind { CRYSTAL, GEL }

    private enum Move { NONE, CHARGE_AIM, CHARGE, BONK, LEAP_CROUCH, LEAP_AIR, LEAP_LAND }

    private static final int CRYSTAL_STUN_BAR = 90;
    private static final int GEL_STUN_BAR = 40;
    private static final double HEART_FROM_HIT = 0.04;

    private static final int CHARGE_COOLDOWN = 300;
    private static final int CHARGE_AIM_TICKS = 45;
    private static final int CHARGE_LOCK_TICKS = 15;   // the direction stops following the player this long before the rush
    private static final int CHARGE_MAX_TICKS = 34;
    private static final double CHARGE_SPEED = 8.5;
    private static final int BONK_TICKS = 60;

    private static final int LEAP_COOLDOWN = 360;
    private static final int LEAP_CROUCH_TICKS = 24;
    private static final int LEAP_AIR_TICKS = 36;
    private static final int LEAP_LAND_TICKS = 30;
    private static final double LEAP_HEIGHT = 18;

    private static final int SPIT_PERIOD = 260;

    private static final Map<Integer, Integer> CRYSTAL_PALETTE = Map.of(
            0xFF08270A, 0xFF190922,
            0xFF0B4A10, 0xFF281235,
            0xFF107416, 0xFF3F1D5C,
            0xFF17931A, 0xFF592D7F,
            0xFF24AE21, 0xFF7842A2,
            0xFF50C73B, 0xFF9D6CC5,
            0xFFCDEFB9, 0xFFECE0F8,
            0xFF08400C, 0xFF190922,
            0xFF07360A, 0xFF190922
    );
    private static final Color[] CRYSTAL_SHARDS = {new Color(0xECE0F8), new Color(0x9D6CC5), new Color(0x592D7F)};

    public final Kind kind;
    private final Random random = new Random();
    private boolean stunned = false;
    private Move move = Move.NONE;
    private int moveTicks = 0;
    private int specialCooldown;
    private int spitTimer;
    private double dirX, dirY;
    private double fromX, fromY, toX, toY;

    public Enemy_RoyalGuard(GamePanel gp, Enemy_SlimeKing king, Kind kind) {
        super(gp, true);
        this.kind = kind;
        setupEnemyStats(kind == Kind.CRYSTAL ? "Crystal Guard" : "Gel Guard", kind == Kind.CRYSTAL ? CRYSTAL_STUN_BAR : GEL_STUN_BAR, 1, 1);
        mushroomValue = 3;
        experienceValue = 0; // Guards are stunned, then dissolved by the King, rather than killed.
        setupCollisionAreas(
                new Rectangle(-6, 20, 60, 28),
                new Rectangle(-11, -2, 70, 50)
        );
        artScale = 5;
        if (kind == Kind.CRYSTAL) {
            useVariant("crystal_guard", CRYSTAL_PALETTE, true);
        }
        specialCooldown = 120 + random.nextInt(120);
        spitTimer = 120;
    }

    @Override
    protected double resistance(HitType type) {
        if (kind == Kind.CRYSTAL) {
            return type == HitType.RANGED ? 0.1 : 1.0;
        }
        return type == HitType.MELEE ? 0.1 : 1.0;
    }

    public boolean isStunned() {
        return stunned;
    }

    public void recover() {
        if (!stunned || !alive) {
            return;
        }
        stunned = false;
        health = maxHealth;
        specialCooldown = 90 + random.nextInt(90);
        alert();
        Rectangle box = getWorldHitBox();
        Color main = kind == Kind.CRYSTAL ? new Color(0x9D6CC5) : new Color(0x50C73B);
        gp.effects.text(box.getCenterX(), box.y - 20, "RECOVERED", main, 1);
        gp.effects.ring(box.getCenterX(), box.getCenterY(), 60, 14, main, false);
    }

    // the stun bar never drops below 1: emptying it knocks the guard out instead of killing it
    @Override
    public void takeDamage(int damage) {
        if (stunned || damage <= 0) {
            return;
        }
        super.takeDamage(Math.min(damage, health - 1));
        if (health <= 1) {
            stunned = true;
            endMove();
            stun(Integer.MAX_VALUE / 4);
            Rectangle box = getWorldHitBox();
            gp.effects.text(box.getCenterX(), box.y - 20, "STUNNED!", new Color(0xF8D05F), 2);
            gp.effects.ring(box.getCenterX(), box.getCenterY(), 70, 16, new Color(0xF8D05F), false);
            gp.effects.burst(box.getCenterX(), box.getCenterY(), 16, 3.0, EffectsManager.GOLD);
            gp.effects.shake(8, 5);
        }
    }

    public void dissolve() {
        Rectangle box = getWorldHitBox();
        gp.effects.burst(box.getCenterX(), box.getCenterY(), 24, 4.0, kind == Kind.CRYSTAL ? CRYSTAL_SHARDS : EffectsManager.GOO);
        health = 0;
        alive = false;
    }

    @Override
    public HitResult receiveHit(Hit hit) {
        boolean wasStunned = stunned;
        HitResult result = super.receiveHit(hit);
        if (result == HitResult.APPLIED && hit.source() == gp.player && resistance(hit.type()) >= 1.0 && !wasStunned
                && random.nextDouble() < HEART_FROM_HIT) {
            Rectangle body = getWorldSolidArea();
            double side = random.nextBoolean() ? 1 : -1;
            gp.entities.addFreeObject(new HeartPickup(body.getCenterX() + side * 40, body.getMaxY() + 6));
            gp.effects.burst(body.getCenterX(), body.getCenterY(), 8, 2.4, EffectsManager.GOO);
        }
        return result;
    }

    @Override
    public void applyKnockbackFrom(int sourceWorldX, int sourceWorldY, double force) {
        super.applyKnockbackFrom(sourceWorldX, sourceWorldY, force * 0.35);
    }

    // a charge that runs into the player ends in a bonk instead of the usual pause after a contact hit
    @Override
    public void pauseMovement(int frames) {
        if (move == Move.CHARGE) {
            velX = 0;
            velY = 0;
            bonk();
            return;
        }
        super.pauseMovement(frames);
    }

    @Override
    protected void updateContactDamage() {
        if (move == Move.LEAP_AIR) {
            return;
        }
        super.updateContactDamage();
    }


    @Override
    public void setAction() {
        if (stunned) {
            endMove();
            super.setAction();
            return;
        }
        if (move != Move.NONE) {
            updateMove();
            return;
        }
        super.setAction();
        if (specialCooldown > 0) {
            specialCooldown--;
        } else {
            double d = distanceToPlayerCenter();
            if (kind == Kind.CRYSTAL && d > 110 && d < 420) {
                startMove(Move.CHARGE_AIM);
            } else if (kind == Kind.GEL && d > 80 && d < 460) {
                startLeap();
            }
        }
        if (kind == Kind.GEL && move == Move.NONE && ++spitTimer >= SPIT_PERIOD) {
            spitTimer = 0;
            spit();
        }
    }

    private void startMove(Move next) {
        move = next;
        moveTicks = 0;
    }

    private void endMove() {
        move = Move.NONE;
        moveTicks = 0;
        customFrame = -1;
        customLift = 0;
        customShake = 0;
    }

    private void updateMove() {
        moveTicks++;
        velX = 0;
        velY = 0;
        switch (move) {
            case CHARGE_AIM -> {
                if (moveTicks <= CHARGE_AIM_TICKS - CHARGE_LOCK_TICKS) {
                    double[] d = directionToPlayer();
                    dirX = d[0];
                    dirY = d[1];
                }
                customFrame = (moveTicks / 5) % 2 == 0 ? FRAME_WINDUP : FRAME_WINDUP_DEEP;
                customShake = (moveTicks / 2) % 2 == 0 ? -1 : 1;
                if (moveTicks >= CHARGE_AIM_TICKS) {
                    customShake = 0;
                    Rectangle box = getWorldHitBox();
                    gp.effects.dust(box.getCenterX(), box.getMaxY(), -dirX, -dirY, 8);
                    startMove(Move.CHARGE);
                }
            }
            case CHARGE -> {
                customFrame = FRAME_LUNGE;
                velX = dirX * CHARGE_SPEED;
                velY = dirY * CHARGE_SPEED;
                if (moveTicks % 3 == 0) {
                    Rectangle box = getWorldHitBox();
                    gp.effects.sparks(box.getCenterX(), box.getCenterY(), -dirX, -dirY, 2, CRYSTAL_SHARDS);
                }
                if (collisionOn && moveTicks > 2) {
                    bonk();
                } else if (moveTicks >= CHARGE_MAX_TICKS) {
                    specialCooldown = CHARGE_COOLDOWN;
                    endMove();
                }
            }
            case BONK -> {
                customFrame = FRAME_DAZED + (moveTicks / 8) % 2;
                customShake = moveTicks < 12 ? ((moveTicks / 2) % 2 == 0 ? -1 : 1) : 0;
                if (moveTicks >= BONK_TICKS) {
                    specialCooldown = CHARGE_COOLDOWN;
                    endMove();
                }
            }
            case LEAP_CROUCH -> {
                customFrame = FRAME_HOP_ANTICIPATION;
                customShake = (moveTicks / 2) % 2 == 0 ? -1 : 1;
                if (moveTicks >= LEAP_CROUCH_TICKS) {
                    customShake = 0;
                    startMove(Move.LEAP_AIR);
                }
            }
            case LEAP_AIR -> {
                double k = Math.min(1.0, moveTicks / (double) LEAP_AIR_TICKS);
                double ease = k < 0.5 ? 2 * k * k : 1 - Math.pow(-2 * k + 2, 2) / 2;
                setFeet(fromX + (toX - fromX) * ease, fromY + (toY - fromY) * ease);
                customLift = Math.sin(Math.PI * k) * LEAP_HEIGHT;
                customFrame = k < 0.3 ? FRAME_HOP_LAUNCH : k < 0.65 ? FRAME_HOP_AIR : FRAME_HOP_FALL;
                if (k >= 1.0) {
                    customLift = 0;
                    shovePlayerOut();
                    Rectangle box = getWorldSolidArea();
                    gp.effects.dust(box.getCenterX() - 30, box.getMaxY(), -1, 0, 6);
                    gp.effects.dust(box.getCenterX() + 30, box.getMaxY(), 1, 0, 6);
                    gp.effects.ring(box.getCenterX(), box.getMaxY(), 90, 16, new Color(0x81D866), true);
                    gp.effects.burst(box.getCenterX(), box.getCenterY(), 12, 3.0, EffectsManager.GOO);
                    gp.effects.shake(10, 6);
                    startMove(Move.LEAP_LAND);
                }
            }
            case LEAP_LAND -> {
                customFrame = moveTicks < LEAP_LAND_TICKS / 2 ? FRAME_HOP_LAND : FRAME_HOP_RECOVER;
                if (moveTicks >= LEAP_LAND_TICKS) {
                    specialCooldown = LEAP_COOLDOWN;
                    endMove();
                }
            }
            default -> endMove();
        }
    }

    private void bonk() {
        Rectangle box = getWorldHitBox();
        gp.effects.text(box.getCenterX(), box.y - 20, "BONK!", new Color(0xECE0F8), 2);
        gp.effects.burst(box.getCenterX() + dirX * 30, box.getCenterY() + dirY * 30, 14, 3.4, CRYSTAL_SHARDS);
        gp.effects.ring(box.getCenterX() + dirX * 30, box.getCenterY() + dirY * 30, 50, 12, new Color(0xECE0F8), false);
        gp.effects.shake(12, 8);
        startMove(Move.BONK);
    }

    private void startLeap() {
        double[] here = feet();
        fromX = here[0];
        fromY = here[1];
        double px = gp.player.worldX + gp.tileSize / 2.0;
        double py = gp.player.worldY + gp.tileSize;
        double minX = gp.tileSize * 2.0, maxX = gp.tileSize * (gp.tileM.getWorldCols() - 2.0);
        double minY = gp.tileSize * 2.2, maxY = gp.tileSize * (gp.tileM.getWorldRows() - 1.2);
        toX = Math.max(minX, Math.min(maxX, px));
        toY = Math.max(minY, Math.min(maxY, py));
        EnemyShot zone = EnemyShot.zone(this, toX, toY - 8, 70, LEAP_CROUCH_TICKS + LEAP_AIR_TICKS);
        zone.body = new Color(0x24AE21);
        zone.core = new Color(0xCDEFB9);
        gp.enemyShots.add(zone);
        startMove(Move.LEAP_CROUCH);
    }

    private void spit() {
        Rectangle box = getWorldHitBox();
        double cx = box.getCenterX(), cy = box.getCenterY();
        double[] d = directionToPlayer();
        double aim = Math.atan2(d[1], d[0]);
        for (int i = -1; i <= 1; i++) {
            gp.enemyShots.add(EnemyShot.small(this, cx, cy, aim + i * Math.toRadians(15), 3.0));
        }
        gp.effects.burst(cx, cy, 6, 2.0, EffectsManager.GOO);
    }


    private double[] feet() {
        return new double[]{worldX + gp.tileSize / 2.0, worldY + gp.tileSize};
    }

    private void setFeet(double x, double y) {
        worldXf = x - gp.tileSize / 2.0;
        worldYf = y - gp.tileSize;
        worldX = (int) Math.round(worldXf);
        worldY = (int) Math.round(worldYf);
    }

    private double[] directionToPlayer() {
        Rectangle self = getWorldSolidArea();
        Rectangle player = gp.player.getWorldHitBox();
        double dx = player.getCenterX() - self.getCenterX();
        double dy = player.getCenterY() - self.getCenterY();
        double len = Math.hypot(dx, dy);
        return len < 1e-6 ? new double[]{0, 1} : new double[]{dx / len, dy / len};
    }

    private double distanceToPlayerCenter() {
        Rectangle self = getWorldSolidArea();
        Rectangle player = gp.player.getWorldHitBox();
        return Math.hypot(player.getCenterX() - self.getCenterX(), player.getCenterY() - self.getCenterY());
    }


    @Override
    public void draw(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        if (alive && move == Move.CHARGE_AIM) {
            drawAimLine(g2);
        }
        super.draw(g);
        if (!alive) {
            return;
        }
        Rectangle box = getWorldHitBox();
        int cx = (int) Math.round(box.getCenterX()) - gp.player.worldX + gp.player.screenX;
        int top = box.y - gp.player.worldY + gp.player.screenY - 34 - (int) Math.round(customLift * artScale)
                + (int) Math.round(Math.sin(gp.enemyShots.tick() * 0.1) * 3);
        if (stunned) {
            drawStars(g2, cx, top + 14);
            return;
        }
        int size = 30;
        UiSkin.inset(g2, cx - size / 2, top - size / 2, size, size);
        g2.setColor(kind == Kind.CRYSTAL ? new Color(0x89201E) : new Color(0x1A4559));
        g2.fillRect(cx - size / 2 + 4, top - size / 2 + 4, size - 8, size - 8);
        UiSkin.icon(g2, kind == Kind.CRYSTAL ? UiSkin.Icon.SWORD : UiSkin.Icon.PISTOL, cx, top, 2, true);
    }

    private void drawAimLine(Graphics2D g2) {
        Rectangle box = getWorldHitBox();
        double sx = box.getCenterX() - gp.player.worldX + gp.player.screenX;
        double sy = box.getMaxY() - 10 - gp.player.worldY + gp.player.screenY;
        boolean locked = moveTicks > CHARGE_AIM_TICKS - CHARGE_LOCK_TICKS;
        if (locked && (moveTicks / 3) % 2 == 0) {
            return;
        }
        int s = gp.scale;
        Composite old = g2.getComposite();
        g2.setComposite(AlphaComposite.SrcOver.derive(locked ? 0.9f : 0.55f));
        g2.setColor(new Color(0xC23228));
        double length = CHARGE_SPEED * CHARGE_MAX_TICKS;
        for (double t = 20; t < length; t += 18) {
            int x = (int) Math.round(sx + dirX * t);
            int y = (int) Math.round(sy + dirY * t);
            g2.fillRect(x - s, y - s, 3 * s, 2 * s);
        }
        g2.setComposite(old);
    }

    private void drawStars(Graphics2D g2, int cx, int cy) {
        int s = gp.scale;
        int tick = gp.enemyShots.tick();
        for (int i = 0; i < 3; i++) {
            double a = tick * 0.14 + i * Math.PI * 2 / 3;
            int x = cx + (int) Math.round(Math.cos(a) * 30);
            int y = cy + (int) Math.round(Math.sin(a) * 8);
            g2.setColor(new Color(0x110B08));
            g2.fillRect(x - 2 * s, y - s, 4 * s, 2 * s);
            g2.fillRect(x - s, y - 2 * s, 2 * s, 4 * s);
            g2.setColor(new Color(0xF8D05F));
            g2.fillRect(x - s, y, 2 * s, s / 2 + 1);
            g2.fillRect(x - s / 2, y - s, s, 2 * s);
        }
    }
}
