package entity.enemy;

import main.GamePanel;
import main.Sound;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

public class Enemy_Slime extends Enemy {
    private static final String SHEET_PATH = "enemy/slime.png";
    private static final int FRAME_W = 20;
    private static final int FRAME_H = 17;
    private static final int BASE_ROW = 15;

    // frame indices in res/enemy/slime.png
    protected static final int FRAME_IDLE = 0;          // 0..3
    protected static final int FRAME_BLINK = 4;
    protected static final int FRAME_HOP_ANTICIPATION = 5;
    protected static final int FRAME_HOP_LAUNCH = 6;
    protected static final int FRAME_HOP_AIR = 7;
    protected static final int FRAME_HOP_FALL = 8;
    protected static final int FRAME_HOP_LAND = 9;
    protected static final int FRAME_HOP_RECOVER = 10;
    protected static final int FRAME_WINDUP = 11;
    protected static final int FRAME_WINDUP_DEEP = 12;
    protected static final int FRAME_LUNGE = 13;
    protected static final int FRAME_DAZED = 14;        // 14..15
    protected static final int FRAME_HURT = 16;
    protected static final int FRAME_ALERT = 17;
    // body width of each frame in art pixels, for the ground shadow
    private static final int[] FRAME_BODY_WIDTH = {14, 15, 14, 13, 14, 16, 11, 12, 13, 17, 15, 17, 18, 16, 16, 15, 16, 12};

    private static final int DETECT_RANGE = 168;
    private static final int LOSE_RANGE = 288;
    private static final int LOSE_INTEREST_FRAMES = 90;
    private static final int LUNGE_RANGE = 92;
    private static final int SEPARATION_RANGE = 40;

    private static final int ANTICIPATION_FRAMES = 6;
    private static final int LAND_FRAMES = 8;
    private static final int WANDER_AIR_FRAMES = 18;
    private static final int CHASE_AIR_FRAMES = 18;
    private static final double WANDER_HOP_SPEED = 1.3;
    private static final double CHASE_HOP_SPEED = 3.1;
    private static final double WANDER_HOP_HEIGHT = 3;
    private static final double CHASE_HOP_HEIGHT = 4;
    private static final int CHASE_GROUND_FRAMES = 10;
    private static final int ALERT_FRAMES = 28;
    private static final int WINDUP_FRAMES = 26;
    private static final int LUNGE_FRAMES = 14;
    private static final double LUNGE_SPEED = 7.5;
    private static final int DAZED_FRAMES = 36;
    private static final int LUNGE_COOLDOWN_FRAMES = 60;

    private static final Color[] GOO = {new Color(0x50C73B), new Color(0x24AE21), new Color(0x107416)};

    private enum State { WANDER, ALERT, CHASE, WINDUP, LUNGE, DAZED }

    private enum Hop { GROUND, ANTICIPATION, AIR, LAND }

    private BufferedImage[] frames;
    private static BufferedImage[] standardFrames;
    private static BufferedImage[] legacyFrames;
    private final boolean legacySprites;
    private static final Map<String, BufferedImage[]> VARIANTS = new HashMap<>();
    private BufferedImage[] variantFrames;

    private final Random random = new Random();
    // screen pixels per art pixel; bigger slimes (the Slime King) draw the same frames larger
    protected int artScale = 3;

    private State state = State.WANDER;
    private int stateFrames = 0;
    private Hop hop = Hop.GROUND;
    private int hopFrames = 0;
    private int groundWaitFrames = 30;
    private int airFrames = WANDER_AIR_FRAMES;
    private double hopVelX = 0;
    private double hopVelY = 0;
    private double hopHeight = WANDER_HOP_HEIGHT;
    private int lungeCooldown = 0;
    private int dazedFrames = DAZED_FRAMES;
    // set by subclasses that animate their own moves; customFrame < 0 means the normal hop animation
    protected int customFrame = -1;
    protected double customLift = 0;
    protected int customShake = 0;
    private int outOfRangeFrames = 0;
    private double lungeDirX = 0;
    private double lungeDirY = 0;

    private int frame = FRAME_IDLE;
    private double lift = 0;
    private int shake = 0;
    private int animClock = 0;
    private int blinkFrames = 0;
    private boolean aiRanThisTick = false;

    public Enemy_Slime(GamePanel gp) {
        this(gp, false);
    }

    @Override
    public void addLights(world.Lighting lighting) {
        Rectangle box = getWorldHitBox();
        lighting.add(box.getCenterX(), box.getCenterY(), 80, world.Palette.GOO_LIGHT, 0.45f, 0.06f);
    }

    protected Enemy_Slime(GamePanel gp, boolean legacySprites) {
        super(gp);
        this.legacySprites = legacySprites;

        setDefaultValues();
        getEnemyImage();
        animClock = random.nextInt(40);
        groundWaitFrames = 20 + random.nextInt(50);
        updateDrawArea();
    }

    public void setDefaultValues() {
        direction = "idle";
        setupEnemyStats("Slime", 10, 1, 1);
        setupCollisionAreas(
                new Rectangle(9, 29, 28, 20),
                new Rectangle(6, 26, 34, 26)
        );
    }

    public void getEnemyImage() {
        frames = legacySprites ? legacyFrames : standardFrames;
        if (frames != null) {
            return;
        }
        try {
            BufferedImage sheet = ImageIO.read(
                    Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream(
                            legacySprites ? SHEET_PATH : "enemy/slime_16.png"))
            );
            int count = legacySprites ? sheet.getWidth() / FRAME_W : 18;
            frames = new BufferedImage[count];
            for (int i = 0; i < count; i++) {
                frames[i] = legacySprites ? sheet.getSubimage(i * FRAME_W, 0, FRAME_W, FRAME_H)
                        : sheet.getSubimage((i % 6) * 16, (i / 6) * 16, 16, 16);
            }
            if (legacySprites) legacyFrames = frames;
            else standardFrames = frames;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void update() {
        aiRanThisTick = false;
        super.update();
        if (!alive) {
            return;
        }
        if (!aiRanThisTick) {
            // a subclass took over movement, the slime is paused or it is flying back from a hit
            hop = Hop.GROUND;
            lift = 0;
            shake = 0;
            if (isRecoiling() && (state == State.WANDER || state == State.ALERT || state == State.WINDUP)) {
                // a hit wakes it up, and interrupts a lunge that has not started yet
                enter(State.CHASE);
                groundWaitFrames = CHASE_GROUND_FRAMES;
            }
        }
        animClock++;
        if (customFrame >= 0) {
            frame = customFrame;
            lift = customLift;
            shake = customShake;
        } else {
            pickFrame();
        }
        updateDrawArea();
    }

    @Override
    public void setAction() {
        aiRanThisTick = true;
        stateFrames++;
        if (lungeCooldown > 0) {
            lungeCooldown--;
        }

        double distance = distanceToPlayer();
        switch (state) {
            case WANDER -> {
                if (distance <= DETECT_RANGE && gp.player.alive) {
                    enter(State.ALERT);
                } else {
                    updateHop(false);
                }
            }
            case ALERT -> {
                velX = 0;
                velY = 0;
                lift = Math.sin(Math.PI * Math.min(1.0, stateFrames / 16.0)) * 3;
                if (stateFrames >= ALERT_FRAMES) {
                    lift = 0;
                    hop = Hop.GROUND;
                    groundWaitFrames = 4;
                    enter(State.CHASE);
                }
            }
            case CHASE -> {
                outOfRangeFrames = distance > LOSE_RANGE ? outOfRangeFrames + 1 : 0;
                if (outOfRangeFrames > LOSE_INTEREST_FRAMES || !gp.player.alive) {
                    enter(State.WANDER);
                } else if (hop == Hop.GROUND && distance <= LUNGE_RANGE && lungeCooldown == 0) {
                    enter(State.WINDUP);
                } else {
                    updateHop(true);
                }
            }
            case WINDUP -> {
                velX = 0;
                velY = 0;
                shake = (stateFrames / 2) % 2 == 0 ? -1 : 1;
                if (stateFrames >= WINDUP_FRAMES) {
                    // commit to where the player is now: moving away during the tremble dodges it
                    double[] dir = directionToPlayer();
                    lungeDirX = dir[0];
                    lungeDirY = dir[1];
                    shake = 0;
                    enter(State.LUNGE);
                }
            }
            case LUNGE -> {
                double t = stateFrames / (double) LUNGE_FRAMES;
                double speed = LUNGE_SPEED * (1.0 - 0.45 * t);
                velX = lungeDirX * speed;
                velY = lungeDirY * speed;
                lift = Math.sin(Math.PI * t) * 2;
                if (stateFrames >= LUNGE_FRAMES || collisionOn) {
                    lift = 0;
                    landDust(4);
                    enter(State.DAZED);
                }
            }
            case DAZED -> {
                velX = 0;
                velY = 0;
                if (stateFrames >= dazedFrames) {
                    lungeCooldown = LUNGE_COOLDOWN_FRAMES;
                    hop = Hop.GROUND;
                    groundWaitFrames = CHASE_GROUND_FRAMES;
                    enter(State.CHASE);
                }
            }
        }
    }

    private void enter(State next) {
        dazedFrames = DAZED_FRAMES;
        state = next;
        stateFrames = 0;
        if (next == State.ALERT) {
            playNear(Sound.SLIME_ALERT);
        } else if (next == State.LUNGE) {
            playNear(Sound.SLIME_LUNGE);
        }
        if (next == State.WANDER) {
            hop = Hop.GROUND;
            groundWaitFrames = 30 + random.nextInt(40);
        }
    }

    private void updateHop(boolean chasing) {
        switch (hop) {
            case GROUND -> {
                velX = 0;
                velY = 0;
                lift = 0;
                if (--groundWaitFrames <= 0) {
                    hop = Hop.ANTICIPATION;
                    hopFrames = 0;
                }
            }
            case ANTICIPATION -> {
                velX = 0;
                velY = 0;
                if (++hopFrames >= ANTICIPATION_FRAMES) {
                    startJump(chasing);
                }
            }
            case AIR -> {
                hopFrames++;
                double t = hopFrames / (double) airFrames;
                velX = hopVelX;
                velY = hopVelY;
                double[] push = separation();
                velX += push[0];
                velY += push[1];
                lift = Math.sin(Math.PI * Math.min(1.0, t)) * hopHeight;
                if (hopFrames >= airFrames) {
                    hop = Hop.LAND;
                    hopFrames = 0;
                    lift = 0;
                    landDust(chasing ? 3 : 2);
                }
            }
            case LAND -> {
                velX = 0;
                velY = 0;
                if (++hopFrames >= LAND_FRAMES) {
                    hop = Hop.GROUND;
                    groundWaitFrames = chasing ? CHASE_GROUND_FRAMES + random.nextInt(6) : 35 + random.nextInt(60);
                }
            }
        }
    }

    private void startJump(boolean chasing) {
        double dirX;
        double dirY;
        if (chasing) {
            double[] dir = directionToPlayer();
            // a little wobble so a group of slimes does not move in lockstep
            double angle = Math.atan2(dir[1], dir[0]) + (random.nextDouble() - 0.5) * 0.5;
            dirX = Math.cos(angle);
            dirY = Math.sin(angle);
        } else {
            double angle = random.nextDouble() * Math.PI * 2;
            dirX = Math.cos(angle);
            dirY = Math.sin(angle);
        }
        double hopSpeed = chasing ? CHASE_HOP_SPEED : WANDER_HOP_SPEED;
        hopVelX = dirX * hopSpeed;
        hopVelY = dirY * hopSpeed;
        airFrames = chasing ? CHASE_AIR_FRAMES : WANDER_AIR_FRAMES;
        hopHeight = chasing ? CHASE_HOP_HEIGHT : WANDER_HOP_HEIGHT;
        hop = Hop.AIR;
        hopFrames = 0;
    }

    private double[] separation() {
        double pushX = 0;
        double pushY = 0;
        for (Enemy other : gp.entities.enemies()) {
            if (other == this || !other.alive) {
                continue;
            }
            double dx = worldX - other.worldX;
            double dy = worldY - other.worldY;
            double d = Math.hypot(dx, dy);
            if (d > 0 && d < SEPARATION_RANGE) {
                double strength = (SEPARATION_RANGE - d) / SEPARATION_RANGE * 0.9;
                pushX += dx / d * strength;
                pushY += dy / d * strength;
            }
        }
        return new double[]{pushX, pushY};
    }

    private void landDust(int count) {
        Rectangle box = getWorldSolidArea();
        gp.effects.dust(box.getCenterX() - 10, box.getMaxY(), -1, 0, count);
        gp.effects.dust(box.getCenterX() + 10, box.getMaxY(), 1, 0, count);
    }

    private double distanceToPlayer() {
        double[] d = vectorToPlayer();
        return Math.hypot(d[0], d[1]);
    }

    private double[] directionToPlayer() {
        double[] d = vectorToPlayer();
        double length = Math.hypot(d[0], d[1]);
        if (length == 0) {
            return new double[]{0, 1};
        }
        return new double[]{d[0] / length, d[1] / length};
    }

    private double[] vectorToPlayer() {
        Rectangle self = getWorldSolidArea();
        Rectangle player = gp.player.getWorldHitBox();
        return new double[]{player.getCenterX() - self.getCenterX(), player.getCenterY() - self.getCenterY()};
    }

    private void pickFrame() {
        if (isRecoiling()) {
            frame = FRAME_HURT;
            return;
        }
        if (!aiRanThisTick) {
            frame = idleFrame();
            return;
        }
        switch (state) {
            case ALERT -> frame = stateFrames < 4 ? FRAME_HOP_ANTICIPATION : FRAME_ALERT;
            case WINDUP -> frame = (stateFrames / 5) % 2 == 0 ? FRAME_WINDUP : FRAME_WINDUP_DEEP;
            case LUNGE -> frame = FRAME_LUNGE;
            case DAZED -> frame = FRAME_DAZED + (stateFrames / 9) % 2;
            default -> frame = hopFrame();
        }
    }

    private int hopFrame() {
        switch (hop) {
            case ANTICIPATION:
                return FRAME_HOP_ANTICIPATION;
            case AIR: {
                double t = hopFrames / (double) airFrames;
                if (t < 0.3) {
                    return FRAME_HOP_LAUNCH;
                }
                return t < 0.65 ? FRAME_HOP_AIR : FRAME_HOP_FALL;
            }
            case LAND:
                return hopFrames < LAND_FRAMES / 2 ? FRAME_HOP_LAND : FRAME_HOP_RECOVER;
            default:
                return idleFrame();
        }
    }

    private int idleFrame() {
        if (blinkFrames > 0) {
            blinkFrames--;
            return FRAME_BLINK;
        }
        if (random.nextInt(240) == 0) {
            blinkFrames = 6;
        }
        return FRAME_IDLE + (animClock / 10) % 4;
    }

    // The frame is anchored at the slime's feet; the draw area follows squash, hop height and the windup tremble,
    // so the base drawing, the hit flash and the health bar all line up with it.
    private void updateDrawArea() {
        if (!legacySprites) {
            int size = 16 * artScale;
            setupDrawArea(size, size, (gp.tileSize - size) / 2 + shake * artScale,
                    gp.tileSize - size - (int) Math.round(lift) * artScale);
            return;
        }
        int width = FRAME_W * artScale;
        int height = FRAME_H * artScale;
        int offsetX = gp.tileSize / 2 - (FRAME_W / 2) * artScale + shake * artScale;
        int offsetY = gp.tileSize - (BASE_ROW + 1) * artScale - (int) Math.round(lift) * artScale;
        setupDrawArea(width, height, offsetX, offsetY);
    }

    @Override
    protected BufferedImage currentImage() {
        BufferedImage[] sheet = variantFrames != null ? variantFrames : frames;
        if (sheet == null || frame >= sheet.length) {
            return null;
        }
        return sheet[frame];
    }

    // Recolours the shared frames for a slime variant (cached per key); spikes adds bone spikes along the top.
    protected void useVariant(String key, Map<Integer, Integer> swap, boolean spikes) {
        if (frames == null) {
            return;
        }
        variantFrames = VARIANTS.computeIfAbsent((legacySprites ? "legacy:" : "v2:") + key, k -> {
            BufferedImage[] out = new BufferedImage[frames.length];
            for (int i = 0; i < frames.length; i++) {
                out[i] = recolor(frames[i], swap, spikes);
            }
            return out;
        });
    }

    private static BufferedImage recolor(BufferedImage src, Map<Integer, Integer> swap, boolean spikes) {
        int w = src.getWidth(), h = src.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int p = src.getRGB(x, y);
                out.setRGB(x, y, swap.getOrDefault(p, p));
            }
        }
        if (spikes) {
            int outline = swap.getOrDefault(0xFF08270A, 0xFF08270A);
            for (int x = 1; x < w - 1; x += 3) {
                int top = -1;
                for (int y = 0; y < h; y++) {
                    if ((src.getRGB(x, y) >>> 24) != 0) {
                        top = y;
                        break;
                    }
                }
                if (top < 2 || top > h - 6) {
                    continue;
                }
                out.setRGB(x, top - 1, 0xFFE0D4BF);
                out.setRGB(x, top - 2, 0xFFECE9C7);
                if ((out.getRGB(x - 1, top - 1) >>> 24) == 0) out.setRGB(x - 1, top - 1, outline);
                if ((out.getRGB(x + 1, top - 1) >>> 24) == 0) out.setRGB(x + 1, top - 1, outline);
                if (top - 3 >= 0) out.setRGB(x, top - 3, outline);
            }
        }
        return out;
    }

    protected void stun(int ticks) {
        enter(State.DAZED);
        dazedFrames = ticks;
        hop = Hop.GROUND;
        lift = 0;
        shake = 0;
        velX = 0;
        velY = 0;
    }

    public void alert() {
        enter(State.CHASE);
        groundWaitFrames = 6;
    }

    @Override
    public void draw(Graphics g2) {
        Graphics2D graphics = (Graphics2D) g2;
        RenderingHints previous = graphics.getRenderingHints();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        if (alive) {
            drawShadow((Graphics2D) g2);
        }
        super.draw(g2);
        if (alive && state == State.ALERT && aiRanThisTick) {
            drawAlertMark((Graphics2D) g2);
        }
        graphics.setRenderingHints(previous);
    }

    private void drawShadow(Graphics2D g2) {
        int bodyWidth = frame < FRAME_BODY_WIDTH.length ? FRAME_BODY_WIDTH[frame] : 14;
        double shrink = 1.0 - Math.min(0.35, lift * 0.08);
        int width = (int) Math.round(bodyWidth * shrink) * artScale;
        int height = 3 * artScale;
        int screenX = worldX - gp.player.worldX + gp.player.screenX + gp.tileSize / 2 - width / 2;
        int screenY = worldY - gp.player.worldY + gp.player.screenY + gp.tileSize - height + artScale;
        Composite old = g2.getComposite();
        g2.setComposite(AlphaComposite.SrcOver.derive((float) (0.32 * shrink)));
        g2.setColor(Color.BLACK);
        g2.fillOval(screenX, screenY, width, height);
        g2.setComposite(old);
    }

    @Override
    protected void drawHealthBar(Graphics g) {
        if (legacySprites || (alive && health < maxHealth)) super.drawHealthBar(g);
    }

    private void drawAlertMark(Graphics2D g2) {
        int s = artScale;
        int x = getDrawScreenX() + drawWidth / 2 - s;
        int y = getDrawScreenY() - 2 * s;
        g2.setColor(new Color(0x08270A));
        g2.fillRect(x - s, y - s, 4 * s, 7 * s);
        g2.setColor(new Color(0xECE9C7));
        g2.fillRect(x, y, 2 * s, 3 * s);
        g2.fillRect(x, y + 4 * s, 2 * s, s);
    }

    @Override
    protected void onDeath() {
        super.onDeath();
        Rectangle box = getWorldHitBox();
        gp.effects.burst(box.getCenterX(), box.getCenterY(), 10, 2.6, GOO);
        gp.playSoundEffect(Sound.SLIME_DEATH);
    }
}
