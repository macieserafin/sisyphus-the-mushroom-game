package entity.enemy;

import main.GamePanel;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

// Keeps its distance and commits to an aimed, telegraphed bone throw.
public class Enemy_Skeleton extends Enemy {
    private enum Action { IDLE, WALK, THROW, HURT, DEATH }

    private static final BufferedImage[][] FRAMES = loadFrames();
    private static final BufferedImage[] BONES = loadBones();
    private static final Color[] BONE_COLORS = {
            new Color(0xE2CC99), new Color(0xAB9167), new Color(0x6F5A3E)
    };
    private Action action = Action.IDLE;
    private int actionTicks;
    private int facing;
    private int cooldown = 60;
    private boolean alerted;
    private double aim;

    public Enemy_Skeleton(GamePanel gp) {
        super(gp);
        setupEnemyStats("Skeleton", 24, 2, 1);
        setupCollisionAreas(new Rectangle(14, 36, 20, 12), new Rectangle(10, 0, 28, 48));
        // Native 16px art uses the same integer scale as the map tiles.
        setupDrawArea(gp.tileSize, gp.tileSize, 0, 0);
        direction = "down";
    }

    private static BufferedImage read(String path) {
        try (InputStream stream = Objects.requireNonNull(
                Enemy_Skeleton.class.getClassLoader().getResourceAsStream(path), "Missing sprite: " + path)) {
            return ImageIO.read(stream);
        } catch (IOException e) {
            throw new IllegalStateException("Could not load sprite: " + path, e);
        }
    }

    private static BufferedImage[][] loadFrames() {
        BufferedImage sheet = read("enemy/skeleton/skeleton_sheet.png");
        BufferedImage[][] frames = new BufferedImage[4][16];
        for (int d = 0; d < 4; d++) {
            for (int f = 0; f < 16; f++) {
                frames[d][f] = sheet.getSubimage((f % 8) * 16, (d * 2 + f / 8) * 16, 16, 16);
            }
        }
        return frames;
    }

    private static BufferedImage[] loadBones() {
        BufferedImage sheet = read("enemy/skeleton/bone_spin.png");
        BufferedImage[] frames = new BufferedImage[8];
        for (int i = 0; i < frames.length; i++) frames[i] = sheet.getSubimage(i * 16, 0, 16, 16);
        return frames;
    }

    private void enter(Action next) {
        if (action != next) {
            action = next;
            actionTicks = 0;
        }
    }

    @Override
    public void update() {
        if (!alive) {
            handleDeathOnce();
            updateDeathAnimation();
            return;
        }
        actionTicks++;
        if (cooldown > 0) cooldown--;
        super.update();
    }

    @Override
    public void setAction() {
        velX = velY = 0;
        if (action == Action.HURT) {
            if (actionTicks < 12) return;
            enter(Action.IDLE);
        }
        if (action == Action.THROW) {
            if (actionTicks == 16) throwBone();
            if (actionTicks >= 32) {
                cooldown = 105;
                enter(Action.IDLE);
            }
            return;
        }
        if (!gp.player.alive) {
            enter(Action.IDLE);
            return;
        }
        Rectangle body = getWorldHitBox();
        Rectangle player = gp.player.getWorldHitBox();
        double dx = player.getCenterX() - body.getCenterX();
        double dy = player.getCenterY() - body.getCenterY();
        double distance = Math.hypot(dx, dy);
        if (distance < 480) alerted = true;
        if (distance > 600) alerted = false;
        if (!alerted) {
            enter(Action.IDLE);
            return;
        }
        facing = Math.abs(dx) > Math.abs(dy) ? (dx > 0 ? 1 : 3) : (dy > 0 ? 0 : 2);
        if (distance <= 340 && cooldown == 0 && hasLineOfSight(body.getCenterX(), body.getCenterY(),
                player.getCenterX(), player.getCenterY())) {
            aim = Math.atan2(dy, dx);
            enter(Action.THROW);
            return;
        }
        if (distance > 230 || distance < 140) {
            double move = distance < 140 ? -1.5 : 1.8;
            velX = dx / Math.max(1, distance) * move;
            velY = dy / Math.max(1, distance) * move;
            if (collisionOn) {
                double x = velX;
                velX = -velY;
                velY = x;
            }
            enter(Action.WALK);
        } else {
            enter(Action.IDLE);
        }
    }

    private boolean hasLineOfSight(double x, double y, double targetX, double targetY) {
        int steps = Math.max(1, (int) Math.ceil(Math.hypot(targetX - x, targetY - y) / 12));
        for (int i = 0; i <= steps; i++) {
            int col = (int) Math.floor((x + (targetX - x) * i / steps) / gp.tileSize);
            int row = (int) Math.floor((y + (targetY - y) * i / steps) / gp.tileSize);
            if (col < 0 || row < 0 || col >= gp.tileM.getWorldCols() || row >= gp.tileM.getWorldRows()
                    || gp.tileM.isCollision(col, row)) return false;
        }
        return true;
    }

    private void throwBone() {
        Rectangle box = getWorldHitBox();
        EnemyShot shot = EnemyShot.small(this, box.getCenterX(), box.getCenterY(), aim, 3.6);
        shot.radius = 6;
        shot.maxAge = 150;
        shot.spriteFrames = BONES;
        shot.spriteFrameTicks = 5;
        shot.spriteWorldSize = gp.tileSize;
        shot.impactColors = BONE_COLORS;
        shot.body = BONE_COLORS[1];
        shot.core = BONE_COLORS[0];
        gp.enemyShots.add(shot);
    }

    @Override
    public void takeDamage(int damage) {
        if (!alive || damage <= 0) return;
        super.takeDamage(damage);
        enter(alive ? Action.HURT : Action.DEATH);
        actionTicks = 0;
        // A hit interrupts the windup; recovery cannot immediately start another throw.
        cooldown = Math.max(cooldown, 45);
    }

    @Override
    protected BufferedImage currentImage() {
        int frame = switch (action) {
            case IDLE -> (actionTicks / 12) % 2;
            case WALK -> 2 + (actionTicks / 8) % 4;
            case HURT -> 6 + Math.min(1, actionTicks / 6);
            case THROW -> 8 + Math.min(3, actionTicks / 8);
            case DEATH -> 12 + Math.min(3, actionTicks / 10);
        };
        return FRAMES[facing][frame];
    }

    @Override
    public void draw(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics;
        RenderingHints previousHints = g.getRenderingHints();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        super.draw(g);
        g.setRenderingHints(previousHints);
    }

    @Override
    protected void drawHealthBar(Graphics g) {
        if (alive && health < maxHealth) super.drawHealthBar(g);
    }

    @Override
    public boolean isReadyForRemoval() {
        return super.isReadyForRemoval() && actionTicks >= 60;
    }

    @Override
    public void updateDeathAnimation() {
        actionTicks++;
    }
}
