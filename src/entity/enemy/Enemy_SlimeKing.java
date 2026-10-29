package entity.enemy;

import entity.CombatState;
import entity.combat.Faction;
import entity.combat.Hit;
import entity.combat.HitResult;
import entity.combat.HitType;
import main.GamePanel;
import object.HeartPickup;
import world.EffectsManager;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Random;

// The Slime King: a long bullet-hell fight that forces switching between melee and ranged.
// Phase 1: the shell alternates between CRYSTAL (bullets bounce, the blade works) and GEL (the blade sinks
// in, bullets work); shots mark him and a sword hit detonates the marks; he adapts to a mode used for too long.
// Phase 2: guards and a barrier. Phase 3: desperate body attacks, messy spits and exhaustion windows.
public class Enemy_SlimeKing extends Enemy {
    private static final String SHEET_PATH = "enemy/slime_king.png";
    private static final int FRAME_W = 40;
    private static final int FRAME_H = 34;
    private static final int SOURCE_SCALE = 2;
    private static final int BASE_ROW = 32;
    private static final int ART_SCALE = 5;

    // frames in each row of the sheet (row 0 gel, row 1 crystal)
    private static final int F_IDLE = 0;          // 0..3
    private static final int F_ANGRY = 4;
    private static final int F_CROUCH = 5;
    private static final int F_LAUNCH = 6;
    private static final int F_AIR = 7;
    private static final int F_FALL = 8;
    private static final int F_LAND = 9;
    private static final int F_PUFF = 10;
    private static final int F_SPIT = 11;
    private static final int F_DAZED = 12;        // 12..13
    private static final int F_HURT = 14;
    private static final int F_ROAR = 15;
    private static final int[] BODY_WIDTH = {30, 31, 30, 29, 30, 34, 24, 26, 27, 36, 33, 30, 32, 31, 32, 28};

    private static final int MAX_HEALTH = 3200;
    private static final int SHELL_PERIOD = 480;
    private static final int SHELL_WARNING = 60;
    private static final int MAX_MINIONS = 4;
    private static final double HEART_DROP_CHANCE = 0.5;
    // chance that a hit knocks a heart out of the King (much higher for a mark detonation)
    private static final double HEART_FROM_HIT = 0.02;
    private static final double HEART_FROM_DETONATION = 0.3;

    // phase 2 starts below 65% health, phase 3 below 30%
    private static final double PHASE_2_AT = 0.65;
    private static final double PHASE_3_AT = 0.30;
    // phase 2: once both guards are stunned the barrier is down this long
    private static final int EXPOSE_TICKS = 660;

    private enum Shell { GEL, CRYSTAL }

    private enum Attack { RING, FAN, SLAM, SPIRAL, SUMMON }

    private enum State {
        INTRO, REST, HOP, RING, FAN, SLAM, SPIRAL, SUMMON, DAZED, SPLIT, MERGE,
        PANIC_WINDUP, PANIC_CHARGE, PANIC_SKID, PANIC_LEAP, PANIC_CRASH, PANIC_EXHAUSTED
    }

    private enum PanicMove { CHARGE, LEAP, BODY_THROW }

    private PanicMove panicMove;
    private int panicActionsLeft;
    private int panicWindupTicks;
    private int panicChargeTicks;
    private int panicFlightTicks;
    private int panicCrashTicks;
    private int panicExhaustTicks;
    private int panicSpitClock;
    private double panicDirX, panicDirY, panicSpeed;
    private double panicTargetX, panicTargetY;

    private static BufferedImage[][] frames;

    private final Random random = new Random();
    private final Deque<Attack> bag = new ArrayDeque<>();
    private final List<Enemy> minions = new ArrayList<>();

    private Shell shell = Shell.GEL;
    private int shellTimer = SHELL_PERIOD;
    private boolean crystalHintShown = false;
    private boolean gelHintShown = false;

    private int phase = 1;
    private boolean phaseChangePending = false;
    private Enemy_RoyalGuard crystalGuard;
    private Enemy_RoyalGuard gelGuard;
    private int exposedTicks;
    private boolean merging;
    private boolean reflectHintShown;

    private State state = State.INTRO;
    private int t = 0;                // ticks in the current state
    private int restTicks = 0;
    private Attack lastAttack;

    // jumping: ground position from (startX, startY) to (targetX, targetY), lift in art pixels
    private double startX, startY, targetX, targetY;
    private int airTicks;
    private double lift = 0;
    private double peak = 0;
    private boolean airborne = false;

    private double spiralAngle = 0;
    private int frame = F_IDLE;
    private int animClock = 0;
    private int shake = 0;
    private int hurtTicks = 0;
    private int contactCooldown = 0;

    public Enemy_SlimeKing(GamePanel gp) {
        super(gp);
        direction = "idle";
        setupEnemyStats("Slime King", MAX_HEALTH, 1, 1);
        mushroomValue = 40;
        setupCollisionAreas(
                new Rectangle(-40, 8, 128, 40),
                new Rectangle(-48, -52, 144, 100)
        );
        usesMarks = true;
        adapts = true;
        loadFrames();
        updateDrawArea();
    }

    private static void loadFrames() {
        if (frames != null) {
            return;
        }
        try {
            BufferedImage sheet = ImageIO.read(Objects.requireNonNull(
                    Enemy_SlimeKing.class.getClassLoader().getResourceAsStream(SHEET_PATH)));
            int count = sheet.getWidth() / (FRAME_W * SOURCE_SCALE);
            frames = new BufferedImage[2][count];
            for (int row = 0; row < 2; row++) {
                for (int i = 0; i < count; i++) {
                    frames[row][i] = sheet.getSubimage(i * FRAME_W * SOURCE_SCALE,
                            row * FRAME_H * SOURCE_SCALE, FRAME_W * SOURCE_SCALE, FRAME_H * SOURCE_SCALE);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ---------------------------------------------------------------- rules

    @Override
    protected double resistance(HitType type) {
        if (phase == 2) {
            // behind the barrier he barely feels anything; broken open, he takes extra
            return exposedTicks > 0 ? 1.25 : 0.1;
        }
        if (phase == 3) {
            if (state == State.PANIC_EXHAUSTED) return 1.5;
            boolean wrongWeapon = shell == Shell.CRYSTAL ? type == HitType.RANGED : type == HitType.MELEE;
            return wrongWeapon ? 0.65 : 1.0;
        }
        if (state == State.DAZED) {
            return 1.25;
        }
        if (shell == Shell.CRYSTAL) {
            return type == HitType.RANGED ? 0.35 : 1.0;
        }
        return type == HitType.MELEE ? 0.35 : 1.0;
    }

    @Override
    public CombatState wantedMode() {
        if (state == State.INTRO || phase == 2 || state == State.SPLIT || state == State.MERGE
                || state == State.PANIC_EXHAUSTED) {
            return null;
        }
        return shell == Shell.CRYSTAL ? CombatState.MELEE : CombatState.RANGE;
    }

    @Override
    public double[] phaseThresholds() {
        return new double[]{0.30, 0.65};
    }

    @Override
    public HitResult receiveHit(Hit hit) {
        if (!alive || state == State.INTRO || state == State.SPLIT || state == State.MERGE) {
            return HitResult.IGNORED;
        }
        Rectangle box = getWorldHitBox();
        if (phase == 2 && exposedTicks == 0 && hit.type() == HitType.RANGED && hit.projectile() != null) {
            // the barrier throws shots back at the player (they are enemy bullets now: cut them or dodge)
            hit.projectile().reflectFrom(this, Faction.ENEMY);
            gp.effects.ring(hit.sourceWorldX(), hit.sourceWorldY(), 22, 8, new Color(0xF8D05F), false);
            gp.effects.sparks(hit.sourceWorldX(), hit.sourceWorldY(), hit.sourceWorldX() - box.getCenterX(),
                    hit.sourceWorldY() - box.getCenterY(), 4, EffectsManager.GOLD);
            if (!reflectHintShown) {
                reflectHintShown = true;
                gp.effects.text(box.getCenterX(), box.y - 30, "REFLECTED!", new Color(0xF8D05F), 2);
            }
            return HitResult.REFLECTED;
        }
        if (phase == 2 && exposedTicks == 0) {
            // the barrier: golden sparks off its surface
            gp.effects.sparks(hit.sourceWorldX(), hit.sourceWorldY(), hit.sourceWorldX() - box.getCenterX(),
                    hit.sourceWorldY() - box.getCenterY(), 4, EffectsManager.GOLD);
        } else if (hit.type() == HitType.RANGED && shell == Shell.CRYSTAL && state != State.DAZED
                && state != State.PANIC_EXHAUSTED && phase != 2) {
            // bullets ring off the crystal
            gp.effects.sparks(hit.sourceWorldX(), hit.sourceWorldY(), hit.sourceWorldX() - box.getCenterX(),
                    hit.sourceWorldY() - box.getCenterY(), 4, EffectsManager.SPARK);
        }
        int marksBefore = marks();
        takeDamage(resolveDamage(hit));
        if (hit.source() == gp.player && alive) {
            boolean detonated = marksBefore > 0 && marks() == 0 && hit.type() == HitType.MELEE;
            if (random.nextDouble() < (detonated ? HEART_FROM_DETONATION : HEART_FROM_HIT)) {
                dropHeartTowardPlayer();
            }
        }
        if (phase == 1 && health <= maxHealth * PHASE_2_AT) {
            phaseChangePending = true;
        } else if (phase == 2 && health <= maxHealth * PHASE_3_AT) {
            phaseChangePending = true;
        }
        spawnHitSparks(hit);
        hurtTicks = 6;
        if (hit.type() == HitType.MELEE) {
            gp.effects.burst(box.getCenterX(), box.getCenterY(), 4, 2.0,
                    shell == Shell.GEL ? EffectsManager.GOO : CRYSTAL_SHARDS);
        }
        return HitResult.APPLIED;
    }

    private static final Color[] CRYSTAL_SHARDS = {new Color(0xD5BFEB), new Color(0x8F55B5), new Color(0x552575)};

    // ---------------------------------------------------------------- update

    @Override
    public void update() {
        super.update();
        if (!alive) {
            return;
        }
        // Enemy's movement resolves collisions after setAction. React to the actual impact this frame.
        if (phase == 3 && collisionOn && (state == State.PANIC_CHARGE || state == State.PANIC_SKID)) {
            crashInPanic(true);
        }
        if (hurtTicks > 0) {
            hurtTicks--;
        }
        if (contactCooldown > 0) {
            contactCooldown--;
        }
        animClock++;
        for (Enemy minion : minions) {
            if (!minion.alive) {
                maybeDropHeart(minion);
            }
        }
        minions.removeIf(m -> !m.alive);
        updateGuards();
        pickFrame();
        updateDrawArea();
    }

    // ---------------------------------------------------------------- phase 2: the guards and the barrier

    // The barrier is down exactly while both guards are stunned; exposedTicks is how long until the first one gets up.
    private void updateGuards() {
        if (phase != 2 || state == State.SPLIT || state == State.MERGE || crystalGuard == null || gelGuard == null) {
            return;
        }
        // stunned guards stay down; once both are down the barrier breaks for EXPOSE_TICKS, then the King raises it
        // again and that is what gets the guards back up
        if (exposedTicks > 0) {
            if (--exposedTicks == 0) {
                Rectangle box = getWorldHitBox();
                gp.effects.ring(box.getCenterX(), box.getCenterY(), 140, 20, new Color(0xDEAE4C), false);
                gp.effects.text(box.getCenterX(), box.y - 30, "BARRIER UP", new Color(0xDEAE4C), 2);
                gp.effects.shake(8, 5);
                crystalGuard.recover();
                gelGuard.recover();
            }
        } else if (crystalGuard.isStunned() && gelGuard.isStunned()) {
            exposedTicks = EXPOSE_TICKS;
            Rectangle box = getWorldHitBox();
            double cx = box.getCenterX(), cy = box.getCenterY();
            gp.effects.burst(cx, cy, 30, 4.5, EffectsManager.GOLD);
            gp.effects.ring(cx, cy, 170, 22, new Color(0xF7EEC7), false);
            gp.effects.text(cx, box.y - 30, "BARRIER BROKEN!", new Color(0xF8D05F), 2);
            gp.effects.shake(12, 8);
            gp.effects.flash(6, new Color(0xF8E096), 0.25f);
        }
    }

    private Enemy_RoyalGuard spawnGuard(Enemy_RoyalGuard.Kind kind, int side) {
        Enemy_RoyalGuard guard = new Enemy_RoyalGuard(gp, this, kind);
        double[] f = footCenter();
        int col = (int) Math.floor((f[0] + side * 180) / gp.tileSize);
        int row = (int) Math.floor(f[1] / gp.tileSize) - 1;
        col = Math.max(2, Math.min(gp.tileM.getWorldCols() - 3, col));
        row = Math.max(2, Math.min(gp.tileM.getWorldRows() - 2, row));
        gp.entities.addEnemy(guard, col, row);
        guard.alert();
        double gx = guard.worldX + gp.tileSize / 2.0, gy = guard.worldY + gp.tileSize / 2.0;
        Color[] colors = kind == Enemy_RoyalGuard.Kind.CRYSTAL ? CRYSTAL_SHARDS : EffectsManager.GOO;
        gp.effects.burst(gx, gy, 24, 4.0, colors);
        gp.effects.ring(gx, guard.worldY + gp.tileSize, 80, 18, colors[1], true);
        return guard;
    }

    // SPLIT: the King roars, jumps to the middle and two guards tear out of his body
    private void updateSplit() {
        if (t == 1) {
            gp.enemyShots.clearOwner(this, true);
            roarEffects();
            gp.effects.flash(10, Color.WHITE, 0.4f);
            gp.ui.showMessage("Stun both guards to break the barrier!");
            double[] mid = arenaCenter();
            startJump(mid[0], mid[1], 30, 20);
        }
        updateJump();
        if (t == 50) {
            phase = 2;
            shell = Shell.GEL;
            adaptSpeed = 1.5;
            crystalGuard = spawnGuard(Enemy_RoyalGuard.Kind.CRYSTAL, -1);
            gelGuard = spawnGuard(Enemy_RoyalGuard.Kind.GEL, 1);
            gp.effects.shake(26, 12);
        }
        if (t >= 90) {
            restTicks = 80;
            enter(State.REST);
        }
    }

    // MERGE: swallowing the court fails to save him; his controlled patterns give way to panic.
    private void updateMerge() {
        if (t == 1) {
            merging = true;
            exposedTicks = 0;
            gp.enemyShots.clearOwner(this, true);
            for (Enemy_RoyalGuard guard : new Enemy_RoyalGuard[]{crystalGuard, gelGuard}) {
                if (guard != null && guard.alive) {
                    gp.enemyShots.clearOwner(guard, true);
                    Rectangle b = guard.getWorldHitBox();
                    gp.effects.burst(b.getCenterX(), b.getCenterY(), 24, 4.0, EffectsManager.GOO);
                    guard.dissolve();
                }
            }
            crystalGuard = gelGuard = null;

            for (Enemy minion : minions) {
                gp.enemyShots.clearOwner(minion, true);
                minion.takeDamage(minion.health);
            }
            minions.clear();

            roarEffects();
            gp.effects.flash(12, new Color(0xC23228), 0.35f);
            gp.ui.showMessage("The King swallows his guards!");
        }
        shake = (t / 2) % 2 == 0 ? -1 : 1;
        if (t >= 90) {
            phase = 3;
            merging = false;
            adapts = false;
            gp.enemyShots.clearOwner(this, true);
            gp.ui.showMessage("The King is panicking! Bait his rush, then punish his exhaustion.");
            gp.effects.burst(footCenter()[0], getWorldHitBox().y, 12, 3.0, EffectsManager.GOLD);
            beginPanicCombo();
        }
    }

    private double[] arenaCenter() {
        return new double[]{gp.tileM.getWorldCols() * gp.tileSize / 2.0, gp.tileM.getWorldRows() * gp.tileSize * 0.55};
    }

    @Override
    public void setAction() {
        velX = 0;
        velY = 0;
        t++;
        if (phaseChangePending && state != State.SPLIT && state != State.MERGE && !airborne) {
            phaseChangePending = false;
            lift = 0;
            enter(phase == 1 ? State.SPLIT : State.MERGE);
        }
        if (state != State.INTRO && state != State.SPLIT && state != State.MERGE && phase == 1) {
            updateShell();
        }
        switch (state) {
            case SPLIT -> updateSplit();
            case MERGE -> updateMerge();
            case INTRO -> updateIntro();
            case REST -> updateRest();
            case HOP -> updateHop();
            case RING -> updateRing();
            case FAN -> updateFan();
            case SLAM -> updateSlam();
            case SPIRAL -> updateSpiral();
            case SUMMON -> updateSummon();
            case DAZED -> updateDazed();
            case PANIC_WINDUP -> updatePanicWindup();
            case PANIC_CHARGE -> updatePanicCharge();
            case PANIC_SKID -> updatePanicSkid();
            case PANIC_LEAP -> updatePanicLeap();
            case PANIC_CRASH -> updatePanicCrash();
            case PANIC_EXHAUSTED -> updatePanicExhaustion();
        }
    }

    private void enter(State next) {
        state = next;
        t = 0;
        shake = 0;
    }

    private void updateIntro() {
        if (t == 1) {
            gp.ui.showMessage("Shots mark the King. The blade detonates the marks.");
        }
        if (t == 30) {
            roarEffects();
        }
        if (t >= 90) {
            restTicks = 60;
            enter(State.REST);
        }
    }

    private void updateShell() {
        if (state == State.DAZED) {
            return;
        }
        shellTimer--;
        if (shellTimer == SHELL_WARNING) {
            Rectangle box = getWorldHitBox();
            gp.effects.ring(box.getCenterX(), box.getCenterY(), 110, 30, nextShellColor(), false);
        }
        if (shellTimer <= 0) {
            shell = shell == Shell.GEL ? Shell.CRYSTAL : Shell.GEL;
            shellTimer = SHELL_PERIOD;
            Rectangle box = getWorldHitBox();
            Color[] colors = shell == Shell.CRYSTAL ? CRYSTAL_SHARDS : EffectsManager.GOO;
            gp.effects.burst(box.getCenterX(), box.getCenterY(), 26, 3.6, colors);
            gp.effects.ring(box.getCenterX(), box.getCenterY(), 150, 20, colors[1], false);
            gp.effects.text(box.getCenterX(), box.y - 30, shell == Shell.CRYSTAL ? "CRYSTAL SHELL" : "SOFT GEL", colors[0], 2);
            gp.effects.shake(8, 5);
            if (shell == Shell.CRYSTAL && !crystalHintShown) {
                crystalHintShown = true;
                gp.ui.showMessage("Crystal shell: bullets bounce off. Use the blade!");
            } else if (shell == Shell.GEL && !gelHintShown) {
                gelHintShown = true;
                gp.ui.showMessage("Soft gel: the blade sinks in. Shoot him!");
            }
        }
    }

    private Color nextShellColor() {
        return shell == Shell.GEL ? new Color(0x8F55B5) : new Color(0x42B83C);
    }

    private boolean shellFlicker() {
        return phase == 1 && shellTimer < SHELL_WARNING && (shellTimer / 4) % 2 == 0;
    }

    // ---------------------------------------------------------------- rest and hops

    private void updateRest() {
        int wait = phase == 2 ? restTicks * 3 / 2 : restTicks;
        if (t >= wait) {
            startAttack(nextAttack());
            return;
        }
        if (t == 8 && restTicks > 30 && phase != 2) {
            // reposition with a hop: keep a fighting distance from the player
            double[] p = playerCenter();
            double angle = random.nextDouble() * Math.PI * 2;
            double distance = 170 + random.nextDouble() * 80;
            startJump(p[0] + Math.cos(angle) * distance, p[1] + Math.sin(angle) * distance, 22, 9);
            enter(State.HOP);
        }
    }

    private void updateHop() {
        if (updateJump()) {
            landEffects(false);
            restTicks = 35 + random.nextInt(20);
            enter(State.REST);
            t = restTicks - 14;
        }
    }

    // ground-level positions are the King's feet centre; returns true on the landing tick
    private void startJump(double footX, double footY, int ticks, double height) {
        double[] here = footCenter();
        startX = here[0];
        startY = here[1];
        double[] clamped = clampToArena(footX, footY);
        targetX = clamped[0];
        targetY = clamped[1];
        airTicks = ticks;
        peak = height;
        airborne = false;
    }

    private boolean updateJump() {
        int crouch = 10;
        if (t <= crouch) {
            lift = 0;
            return false;
        }
        int airT = t - crouch;
        if (airT > airTicks) {
            // already landed: the landing tick fires only once
            airborne = false;
            lift = 0;
            return false;
        }
        airborne = true;
        double k = Math.min(1.0, airT / (double) airTicks);
        double ease = k < 0.5 ? 2 * k * k : 1 - Math.pow(-2 * k + 2, 2) / 2;
        setFoot(startX + (targetX - startX) * ease, startY + (targetY - startY) * ease);
        lift = Math.sin(Math.PI * k) * peak;
        if (k >= 1.0) {
            airborne = false;
            lift = 0;
            shovePlayerOut();
            return true;
        }
        return false;
    }

    // ---------------------------------------------------------------- phase 3: losing control

    private double desperation() {
        return Math.max(0, Math.min(1, 1 - health / (maxHealth * PHASE_3_AT)));
    }

    private void beginPanicCombo() {
        panicActionsLeft = health <= maxHealth * 0.10 ? 3 : 2;
        // A battered shell changes between bursts, never in the middle of a dodge.
        shell = shell == Shell.GEL ? Shell.CRYSTAL : Shell.GEL;
        shellTimer = SHELL_PERIOD;
        Rectangle box = getWorldHitBox();
        gp.effects.burst(box.getCenterX(), box.getCenterY(), 8, 2.3,
                shell == Shell.CRYSTAL ? CRYSTAL_SHARDS : EffectsManager.GOO);
        beginPanicMove(PanicMove.CHARGE);
    }

    private void beginPanicMove(PanicMove move) {
        panicMove = move;
        double[] here = footCenter(), player = playerFeet();
        double dx = player[0] - here[0], dy = player[1] - here[1];
        double distance = Math.hypot(dx, dy);
        // Aim once. He cannot steer after the warning, and overshoots instead of chasing perfectly.
        panicDirX = distance > 1 ? dx / distance : (random.nextBoolean() ? 1 : -1);
        panicDirY = distance > 1 ? dy / distance : 0;
        panicSpeed = 9.0 + 2.5 * desperation();
        panicWindupTicks = (int) Math.round(32 - 8 * desperation());
        panicSpitClock = 0;
        if (move == PanicMove.CHARGE) {
            panicChargeTicks = Math.max(24, Math.min(64, (int) Math.ceil((distance + 170) / panicSpeed)));
            double travel = panicChargeTicks * panicSpeed;
            double[] edge = clampToArena(here[0] + panicDirX * travel, here[1] + panicDirY * travel);
            if (Math.abs(panicDirX) > 0.001) travel = Math.min(travel, Math.max(0, (edge[0] - here[0]) / panicDirX));
            if (Math.abs(panicDirY) > 0.001) travel = Math.min(travel, Math.max(0, (edge[1] - here[1]) / panicDirY));
            panicTargetX = here[0] + panicDirX * travel;
            panicTargetY = here[1] + panicDirY * travel;
        } else {
            double travel = move == PanicMove.BODY_THROW ? Math.min(260, distance + 85) : distance + 35;
            double[] target = clampToArena(here[0] + panicDirX * travel, here[1] + panicDirY * travel);
            panicTargetX = target[0];
            panicTargetY = target[1];
            panicFlightTicks = move == PanicMove.BODY_THROW ? 14 : (int) Math.round(26 - 5 * desperation());
            // Existing jump animation includes ten crouch ticks before flight.
            gp.enemyShots.add(EnemyShot.zone(this, panicTargetX, panicTargetY - 8,
                    move == PanicMove.BODY_THROW ? 88 : 80, panicWindupTicks + 10 + panicFlightTicks));
        }
        enter(State.PANIC_WINDUP);
    }

    private void updatePanicWindup() {
        shake = (t / 2) % 2 == 0 ? -1 : 1;
        if (t >= panicWindupTicks) {
            if (panicMove == PanicMove.CHARGE) {
                enter(State.PANIC_CHARGE);
            } else {
                startJump(panicTargetX, panicTargetY, panicFlightTicks, panicMove == PanicMove.BODY_THROW ? 8 : 23);
                enter(State.PANIC_LEAP);
            }
        }
    }

    private void updatePanicCharge() {
        velX = panicDirX * panicSpeed;
        velY = panicDirY * panicSpeed;
        panicSpitWhileMoving();
        if (t % 4 == 0) {
            double[] feet = footCenter();
            gp.effects.dust(feet[0], feet[1], -panicDirX, -panicDirY, 3);
        }
        if (t >= panicChargeTicks) {
            enter(State.PANIC_SKID);
            velX *= 0.7;
            velY *= 0.7;
        }
    }

    private void updatePanicSkid() {
        double speed = panicSpeed * 0.7 * Math.pow(0.84, t);
        velX = panicDirX * speed;
        velY = panicDirY * speed;
        shake = t % 3 == 0 ? 1 : -1;
        if (t >= 15) crashInPanic(false);
    }

    private void updatePanicLeap() {
        if (updateJump()) {
            crashInPanic(false);
            return;
        }
        if (airborne) panicSpitWhileMoving();
    }

    private void panicSpitWhileMoving() {
        int interval = health <= maxHealth * 0.10 ? 10 : 13;
        if (++panicSpitClock >= interval) {
            panicSpitClock = 0;
            scatterPanicGoo(false);
        }
    }

    private void scatterPanicGoo(boolean impact) {
        double[] c = mouth();
        double direction = Math.atan2(panicDirY, panicDirX);
        Color[] colors = shell == Shell.CRYSTAL ? CRYSTAL_SHARDS : EffectsManager.GOO;
        int count = impact ? 3 + random.nextInt(3) : 1 + random.nextInt(3);
        for (int i = 0; i < count; i++) {
            double angle = impact ? random.nextDouble() * Math.PI * 2
                    : direction + (random.nextDouble() - 0.5) * 2.8;
            double speed = 1.8 + random.nextDouble() * 1.8;
            boolean heavy = random.nextDouble() < 0.15;
            EnemyShot shot = heavy ? EnemyShot.heavy(this, c[0], c[1], angle, speed * 0.75)
                    : EnemyShot.small(this, c[0], c[1], angle, speed);
            shot.radius = heavy ? 12 : 4 + random.nextInt(3);
            shot.maxAge = 50 + random.nextInt(26);
            shot.body = colors[1];
            shot.core = colors[0];
            shot.impactColors = colors;
            gp.enemyShots.add(shot);
        }
    }

    private void crashInPanic(boolean wall) {
        velX = velY = 0;
        airborne = false;
        lift = 0;
        panicCrashTicks = wall ? 34 : 18 + random.nextInt(9);
        enter(State.PANIC_CRASH);
        landEffects(wall || panicMove != PanicMove.CHARGE);
        scatterPanicGoo(true);
        Rectangle box = getWorldHitBox();
        gp.effects.burst(box.getCenterX(), box.getCenterY(), 10, 2.8, CRYSTAL_SHARDS);
        if (wall) {
            gp.effects.shake(12, 8);
            gp.effects.text(box.getCenterX(), box.y - 20, "CRASH!", EffectsManager.DUST[0], 2);
        }
    }

    private void updatePanicCrash() {
        shake = t < 10 ? ((t / 2) % 2 == 0 ? -1 : 1) : 0;
        if (t < panicCrashTicks) return;
        if (--panicActionsLeft > 0) {
            beginPanicMove(panicMove == PanicMove.CHARGE
                    ? (random.nextBoolean() ? PanicMove.LEAP : PanicMove.BODY_THROW) : PanicMove.CHARGE);
        } else {
            panicExhaustTicks = health <= maxHealth * 0.10 ? 96 : 114;
            enter(State.PANIC_EXHAUSTED);
            // Clear his own mess so both weapons get a real, approachable punish window.
            gp.enemyShots.clearOwner(this, true);
            Rectangle box = getWorldHitBox();
            gp.effects.text(box.getCenterX(), box.y - 25, "EXHAUSTED!", EffectsManager.GOLD[0], 2);
            gp.effects.ring(box.getCenterX(), box.getCenterY(), 65, 20, EffectsManager.GOLD[1], false);
        }
    }

    private void updatePanicExhaustion() {
        if (t >= panicExhaustTicks) beginPanicCombo();
    }

    // ---------------------------------------------------------------- phase 1/2 attacks

    private Attack nextAttack() {
        if (phase == 2) {
            // he stays in the middle behind his barrier: rings and spirals only
            lastAttack = lastAttack == Attack.RING ? Attack.SPIRAL : Attack.RING;
            return lastAttack;
        }
        if (bag.isEmpty()) {
            List<Attack> list = new ArrayList<>(List.of(Attack.RING, Attack.FAN, Attack.SLAM, Attack.SPIRAL, Attack.FAN, Attack.RING));
            if (minions.size() < 2) {
                list.add(Attack.SUMMON);
            }
            Collections.shuffle(list, random);
            if (!list.isEmpty() && list.get(0) == lastAttack) {
                Collections.swap(list, 0, list.size() - 1);
            }
            bag.addAll(list);
        }
        Attack next = bag.poll();
        if (next == Attack.SUMMON && minions.size() >= MAX_MINIONS) {
            next = Attack.RING;
        }
        lastAttack = next;
        return next;
    }

    private void startAttack(Attack attack) {
        switch (attack) {
            case RING -> {
                startJump(footCenter()[0], footCenter()[1], 20, 12);
                enter(State.RING);
            }
            case FAN -> enter(State.FAN);
            case SLAM -> {
                double[] p = playerFeet();
                startJump(p[0], p[1], 38, 26);
                enter(State.SLAM);
                gp.enemyShots.add(EnemyShot.zone(this, targetX, targetY, 92, 10 + 38));
            }
            case SPIRAL -> {
                spiralAngle = random.nextDouble() * Math.PI * 2;
                enter(State.SPIRAL);
            }
            case SUMMON -> enter(State.SUMMON);
        }
    }

    // RING: a hop on the spot, then two rings of goo balls
    private void updateRing() {
        if (t < 10) {
            shake = (t / 2) % 2 == 0 ? -1 : 1;
        } else {
            shake = 0;
        }
        if (updateJump()) {
            landEffects(true);
            ring(17, 2.7, 0);
        }
        if (t == 10 + 20 + 14) {
            ring(17, 2.2, Math.PI / 17);
        }
        if (t >= 10 + 20 + 40) {
            restTicks = 70;
            enter(State.REST);
        }
    }

    // FAN: three puffed-up spits aimed at the player; the last one has a heavy drop in the middle
    private void updateFan() {
        int volley = t / 36;
        int local = t % 36;
        if (volley >= 3) {
            restTicks = 65;
            enter(State.REST);
            return;
        }
        if (local == 16) {
            double[] c = mouth();
            double aim = angleToPlayer(c[0], c[1]);
            int count = 6;
            double spread = Math.toRadians(12);
            for (int i = 0; i < count; i++) {
                double a = aim + (i - (count - 1) / 2.0) * spread;
                gp.enemyShots.add(EnemyShot.small(this, c[0], c[1], a, 3.5));
            }
            if (volley == 2) {
                EnemyShot drop = EnemyShot.heavy(this, c[0], c[1], aim, 2.4);
                drop.burstInto = 8;
                drop.maxAge = 70;
                gp.enemyShots.add(drop);
            }
            gp.effects.burst(c[0], c[1], 8, 2.4, EffectsManager.GOO);
            gp.effects.shake(4, 3);
        }
    }

    // SLAM: a high jump onto the marked spot, a shockwave and heavy drops, then he is dazed (the punish window)
    private void updateSlam() {
        if (updateJump()) {
            double[] f = footCenter();
            gp.enemyShots.add(EnemyShot.wave(this, f[0], f[1], 5.0, 340));
            for (int i = 0; i < 6; i++) {
                EnemyShot drop = EnemyShot.heavy(this, f[0], f[1] - 30, i * Math.PI / 3 + Math.PI / 6, 2.2);
                drop.burstInto = 4;
                drop.maxAge = 80;
                gp.enemyShots.add(drop);
            }
            landEffects(true);
            gp.effects.shake(22, 12);
            gp.effects.flash(6, new Color(0xDED5AF), 0.25f);
            enter(State.DAZED);
        }
    }

    private void updateDazed() {
        if (t == 1) {
            Rectangle box = getWorldHitBox();
            gp.effects.text(box.getCenterX(), box.y - 30, "DAZED!", new Color(0xF8D05F), 2);
        }
        if (t >= 140) {
            restTicks = 30;
            enter(State.REST);
        }
    }

    // SPIRAL: three rotating arms of goo, reversing direction halfway
    private void updateSpiral() {
        int duration = 180;
        if (t % 7 == 0 && t < duration) {
            double[] c = mouth();
            for (int arm = 0; arm < 3; arm++) {
                gp.enemyShots.add(EnemyShot.small(this, c[0], c[1], spiralAngle + arm * Math.PI * 2 / 3, 2.4));
            }
            spiralAngle += t < duration / 2 ? 0.21 : -0.24;
        }
        shake = (t / 3) % 2 == 0 ? 0 : 1;
        if (t >= duration + 30) {
            restTicks = 60;
            enter(State.REST);
        }
    }

    // SUMMON: a roar, and two slimes from the court: one spiky (shoot it) and one reflecting (cut it)
    private void updateSummon() {
        if (t == 20) {
            roarEffects();
        }
        if (t == 40) {
            spawnMinion(new Enemy_SpikySlime(gp), -1);
            spawnMinion(new Enemy_Reflector(gp), 1);
        }
        if (t >= 70) {
            restTicks = 60;
            enter(State.REST);
        }
    }

    private void spawnMinion(Enemy_Slime minion, int side) {
        double[] f = footCenter();
        int col = (int) Math.floor((f[0] + side * 130) / gp.tileSize);
        int row = (int) Math.floor(f[1] / gp.tileSize) - 1;
        col = Math.max(1, Math.min(gp.tileM.getWorldCols() - 2, col));
        row = Math.max(1, Math.min(gp.tileM.getWorldRows() - 2, row));
        minion.mushroomValue = 0; // summoned without end, so they must not be farmed
        gp.entities.addEnemy(minion, col, row);
        minion.alert();
        minions.add(minion);
        gp.effects.burst(minion.worldX + gp.tileSize / 2.0, minion.worldY + gp.tileSize / 2.0, 14, 3.0, EffectsManager.GOO);
        gp.effects.ring(minion.worldX + gp.tileSize / 2.0, minion.worldY + gp.tileSize * 0.9, 50, 16, new Color(0xAED58D), true);
    }

    // Summoned slimes are the way to heal in this long fight: most of them drop a heart, always when the player is low.
    private void maybeDropHeart(Enemy minion) {
        boolean low = gp.player.health <= 2;
        if (!low && random.nextDouble() >= HEART_DROP_CHANCE) {
            return;
        }
        Rectangle body = minion.getWorldSolidArea();
        HeartPickup heart = new HeartPickup(body.getCenterX(), body.getMaxY());
        gp.entities.addFreeObject(heart);
    }

    // A heart knocked out of the King's jelly, landing on his side facing the player.
    private void dropHeartTowardPlayer() {
        Rectangle body = getWorldSolidArea();
        double[] p = playerFeet();
        double dx = p[0] - body.getCenterX();
        double dy = p[1] - body.getMaxY();
        double len = Math.max(1, Math.hypot(dx, dy));
        double x = body.getCenterX() + dx / len * 80;
        double y = body.getMaxY() + Math.max(-20, dy / len * 50);
        gp.entities.addFreeObject(new HeartPickup(x, y));
        gp.effects.burst(x, y - 30, 8, 2.4, EffectsManager.GOO);
    }

    private void ring(int count, double speed, double offset) {
        double[] c = center();
        for (int i = 0; i < count; i++) {
            gp.enemyShots.add(EnemyShot.small(this, c[0], c[1], offset + i * Math.PI * 2 / count, speed));
        }
    }

    // ---------------------------------------------------------------- effects

    private void landEffects(boolean big) {
        double[] f = footCenter();
        gp.effects.dust(f[0] - 50, f[1], -1, 0, big ? 8 : 4);
        gp.effects.dust(f[0] + 50, f[1], 1, 0, big ? 8 : 4);
        gp.effects.ring(f[0], f[1], big ? 160 : 90, big ? 20 : 14, new Color(0xC1AF91), true);
        gp.effects.shake(big ? 10 : 5, big ? 7 : 4);
    }

    private void roarEffects() {
        Rectangle box = getWorldHitBox();
        gp.effects.ring(box.getCenterX(), box.getCenterY(), 180, 26, new Color(0xDED5AF), false);
        gp.effects.ring(box.getCenterX(), box.getCenterY(), 120, 20, new Color(0xC23228), false);
        gp.effects.shake(24, 8);
    }

    @Override
    protected void onDeath() {
        Rectangle box = getWorldHitBox();
        double cx = box.getCenterX(), cy = box.getCenterY();
        gp.enemyShots.clearOwner(this, true);
        merging = true;
        for (Enemy minion : minions) {
            minion.takeDamage(minion.health);
        }
        for (Enemy_RoyalGuard guard : new Enemy_RoyalGuard[]{crystalGuard, gelGuard}) {
            if (guard != null && guard.alive) {
                gp.enemyShots.clearOwner(guard, true);
                guard.dissolve();
            }
        }
        gp.effects.flash(24, Color.WHITE, 0.6f);
        gp.effects.shake(40, 14);
        for (int i = 0; i < 4; i++) {
            gp.effects.ring(cx, cy, 120 + i * 70, 20 + i * 8, i % 2 == 0 ? new Color(0x42B83C) : new Color(0xDEAE4C), i % 2 == 1);
        }
        gp.effects.burst(cx, cy, 60, 6.0, EffectsManager.GOO);
        gp.effects.burst(cx, cy - 40, 24, 4.0, EffectsManager.GOLD);
        gp.effects.text(cx, box.y - 20, "THE KING FALLS", new Color(0xDEAE4C), 3);
        gp.playSoundEffect(main.Sound.SLIME_DEATH);
        gp.levelManager.onBossDefeated();
    }

    // ---------------------------------------------------------------- contact

    @Override
    protected void updateContactDamage() {
        if (airborne || !gp.player.alive || gp.player.isHurtInvincible() || contactCooldown > 0) {
            return;
        }
        if (phase == 3 && (state == State.PANIC_WINDUP || state == State.PANIC_CRASH
                || state == State.PANIC_EXHAUSTED || (state == State.PANIC_LEAP && t <= 10))) return;
        Rectangle body = getWorldSolidArea();
        // Movement stops before an overlap. Test the blocked charge step, rather than an enlarged hitbox.
        double contactSpeed = state == State.PANIC_SKID ? panicSpeed * 0.7 * Math.pow(0.84, t) : panicSpeed;
        boolean rushContact = phase == 3 && (state == State.PANIC_CHARGE || state == State.PANIC_SKID) && collisionOn
                && gp.cChecker.checkPlayerAt(this, (int) Math.round(worldX + panicDirX * contactSpeed),
                (int) Math.round(worldY + panicDirY * contactSpeed));
        if (rushContact || body.intersects(gp.player.getWorldHitBox())) {
            if (gp.player.takeDamageFromEntity(this, attackDamage)) {
                contactCooldown = 45;
            }
        }
    }

    // After a landing on top of the player, push the player out of the body so they are never stuck inside it.
    // ---------------------------------------------------------------- drawing

    private void pickFrame() {
        if (hurtTicks > 0 && (state == State.REST || state == State.DAZED || state == State.PANIC_EXHAUSTED)) {
            frame = F_HURT;
            return;
        }
        switch (state) {
            case INTRO -> frame = t < 30 ? F_IDLE + (animClock / 10) % 4 : F_ROAR;
            case SPLIT -> frame = t < 50 ? jumpFrame() : F_ROAR;
            case MERGE -> frame = (t / 8) % 2 == 0 ? F_ROAR : F_HURT;
            case DAZED -> frame = F_DAZED + (t / 12) % 2;
            case FAN -> frame = (t % 36) < 16 ? F_PUFF : (t % 36) < 26 ? F_SPIT : F_ANGRY;
            case SPIRAL -> frame = (t / 6) % 2 == 0 ? F_SPIT : F_ANGRY;
            case SUMMON -> frame = t < 15 ? F_CROUCH : F_ROAR;
            case HOP, RING, SLAM -> frame = jumpFrame();
            case PANIC_WINDUP -> frame = (t / 4) % 2 == 0 ? F_CROUCH : F_HURT;
            case PANIC_CHARGE -> frame = panicSpitClock < 3 ? F_SPIT : F_ANGRY;
            case PANIC_SKID -> frame = F_LAND;
            case PANIC_LEAP -> frame = jumpFrame();
            case PANIC_CRASH -> frame = t < 10 ? F_LAND : F_HURT;
            case PANIC_EXHAUSTED -> frame = (t / 12) % 3 == 0 ? F_PUFF : F_DAZED + (t / 12) % 2;
            default -> frame = F_IDLE + (animClock / 10) % 4;
        }
    }

    private int jumpFrame() {
        int crouch = 10;
        if (t <= crouch) {
            return F_CROUCH;
        }
        double k = (t - crouch) / (double) airTicks;
        if (k >= 1.0) {
            return t - crouch - airTicks < 8 ? F_LAND : F_ANGRY;
        }
        return k < 0.3 ? F_LAUNCH : k < 0.65 ? F_AIR : F_FALL;
    }

    private void updateDrawArea() {
        int width = FRAME_W * ART_SCALE;
        int height = FRAME_H * ART_SCALE;
        int offsetX = gp.tileSize / 2 - (FRAME_W / 2) * ART_SCALE + shake * ART_SCALE;
        int offsetY = gp.tileSize - (BASE_ROW + 1) * ART_SCALE - (int) Math.round(lift) * ART_SCALE;
        setupDrawArea(width, height, offsetX, offsetY);
    }

    @Override
    protected BufferedImage currentImage() {
        if (frames == null) {
            return null;
        }
        Shell shown = shellFlicker() ? (shell == Shell.GEL ? Shell.CRYSTAL : Shell.GEL) : shell;
        BufferedImage[] row = frames[shown == Shell.GEL ? 0 : 1];
        return frame < row.length ? row[frame] : null;
    }

    @Override
    public void draw(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        if (alive && phase == 3 && state == State.PANIC_WINDUP && panicMove == PanicMove.CHARGE) {
            drawPanicLane(g2);
        }
        if (alive) {
            drawShadow(g2);
        }
        if (alive && phase == 3) {
            Graphics2D body = (Graphics2D) g2.create();
            double[] feet = footCenter();
            double x = feet[0] - gp.player.worldX + gp.player.screenX;
            double y = feet[1] - gp.player.worldY + gp.player.screenY;
            double angle = Math.sin(animClock * 0.65) * 0.025;
            if (state == State.PANIC_CHARGE || state == State.PANIC_SKID) angle += panicDirX * 0.12;
            else if (state == State.PANIC_LEAP) angle += panicDirX * 0.16;
            body.rotate(angle, x, y);
            if (state == State.PANIC_CRASH) {
                body.translate(x, y);
                body.scale(1.10, 0.86);
                body.translate(-x, -y);
            }
            super.draw(body);
            drawPanicCore(body);
            body.dispose();
        } else {
            super.draw(g2);
        }
        if (alive && state == State.DAZED) {
            drawDizzyStars(g2);
        }
        if (alive && phase == 2) {
            drawBarrier(g2);
        }
    }

    private void drawPanicLane(Graphics2D graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        double[] feet = footCenter();
        double x = feet[0] - gp.player.worldX + gp.player.screenX;
        double y = feet[1] - gp.player.worldY + gp.player.screenY;
        double endX = panicTargetX - gp.player.worldX + gp.player.screenX;
        double endY = panicTargetY - gp.player.worldY + gp.player.screenY;
        int halfWidth = getWorldSolidArea().width / 2;
        double nx = -panicDirY * halfWidth, ny = panicDirX * halfWidth;
        Polygon lane = new Polygon(new int[]{(int) (x + nx), (int) (endX + nx), (int) (endX - nx), (int) (x - nx)},
                new int[]{(int) (y + ny), (int) (endY + ny), (int) (endY - ny), (int) (y - ny)}, 4);
        g.setColor(new Color(0xC23228));
        g.setComposite(AlphaComposite.SrcOver.derive(0.16f));
        g.fill(lane);
        boolean flash = panicWindupTicks - t < 10 && (t / 2) % 2 == 0;
        g.setComposite(AlphaComposite.SrcOver.derive(flash ? 1f : 0.65f));
        g.setColor(flash ? EffectsManager.GOLD[0] : new Color(0xE05A48));
        g.setStroke(new BasicStroke(gp.scale, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                10, new float[]{12, 8}, t % 20));
        g.draw(lane);
        double length = Math.hypot(endX - x, endY - y);
        for (double d = 35; d < length; d += 40) {
            int ax = (int) Math.round(x + panicDirX * d), ay = (int) Math.round(y + panicDirY * d);
            g.fillPolygon(new int[]{ax, (int) (ax - panicDirX * 12 - panicDirY * 7), (int) (ax - panicDirX * 12 + panicDirY * 7)},
                    new int[]{ay, (int) (ay - panicDirY * 12 + panicDirX * 7), (int) (ay - panicDirY * 12 - panicDirX * 7)}, 3);
        }
        g.dispose();
    }

    private void drawPanicCore(Graphics2D g) {
        Rectangle box = getWorldHitBox();
        int cx = (int) box.getCenterX() - gp.player.worldX + gp.player.screenX;
        int cy = (int) box.getCenterY() - gp.player.worldY + gp.player.screenY - (int) Math.round(lift * ART_SCALE);
        Stroke oldStroke = g.getStroke();
        Composite old = g.getComposite();
        boolean exposed = state == State.PANIC_EXHAUSTED;
        g.setComposite(AlphaComposite.SrcOver.derive(exposed ? 0.9f : 0.65f));
        g.setStroke(new BasicStroke(gp.scale));
        g.setColor(shell == Shell.CRYSTAL ? EffectsManager.GOLD[1] : CRYSTAL_SHARDS[1]);
        g.drawPolyline(new int[]{cx - 48, cx - 30, cx - 34, cx - 12},
                new int[]{cy - 2, cy + 5, cy + 16, cy + 22}, 4);
        g.drawPolyline(new int[]{cx + 44, cx + 28, cx + 34, cx + 10},
                new int[]{cy - 8, cy + 4, cy + 16, cy + 22}, 4);
        int radius = exposed ? 17 + (int) Math.round(Math.sin(animClock * 0.25) * 2) : 9;
        g.setColor(EffectsManager.GOLD[2]);
        g.fillOval(cx - radius, cy + 21 - radius, radius * 2, radius * 2);
        g.setColor(EffectsManager.GOLD[0]);
        g.fillOval(cx - radius / 2, cy + 21 - radius / 2, radius, radius);
        g.setComposite(old);
        g.setStroke(oldStroke);
    }

    // Phase 2: a golden dome while the barrier holds; once broken, the core blazes and a ring counts down.
    private void drawBarrier(Graphics2D g2) {
        Rectangle box = getWorldHitBox();
        int cx = (int) Math.round(box.getCenterX()) - gp.player.worldX + gp.player.screenX;
        int cy = (int) Math.round(box.getCenterY()) - gp.player.worldY + gp.player.screenY - (int) Math.round(lift) * ART_SCALE;
        int w = box.width + 50, h = box.height + 40;
        Composite old = g2.getComposite();
        Stroke oldStroke = g2.getStroke();
        if (exposedTicks == 0) {
            float pulse = 0.16f + 0.06f * (float) Math.sin(animClock * 0.12);
            g2.setComposite(AlphaComposite.SrcOver.derive(pulse));
            g2.setColor(new Color(0xDEAE4C));
            g2.fillOval(cx - w / 2, cy - h / 2, w, h);
            g2.setComposite(AlphaComposite.SrcOver.derive(0.75f));
            g2.setStroke(new BasicStroke(gp.scale));
            g2.setColor(new Color(0xF7EEC7));
            g2.drawOval(cx - w / 2, cy - h / 2, w, h);
            // little hexagon-ish glints running around the dome
            for (int i = 0; i < 6; i++) {
                double a = animClock * 0.04 + i * Math.PI / 3;
                int gx = cx + (int) Math.round(Math.cos(a) * w / 2.0);
                int gy = cy + (int) Math.round(Math.sin(a) * h / 2.0);
                g2.fillRect(gx - gp.scale, gy - gp.scale, gp.scale * 2, gp.scale * 2);
            }
        } else {
            // the countdown: an arc that shrinks as the window closes
            double left = exposedTicks / (double) EXPOSE_TICKS;
            g2.setComposite(AlphaComposite.SrcOver.derive(0.9f));
            g2.setStroke(new BasicStroke(gp.scale * 2));
            g2.setColor((animClock / 5) % 2 == 0 ? new Color(0xF8D05F) : new Color(0xC23228));
            g2.drawArc(cx - w / 2, cy - h / 2, w, h, 90, (int) Math.round(360 * left));
        }
        g2.setStroke(oldStroke);
        g2.setComposite(old);
    }

    @Override
    protected void drawHealthBar(Graphics g2) {
        // the boss bar at the top of the screen replaces the small bar
    }

    private void drawShadow(Graphics2D g2) {
        int bodyWidth = frame < BODY_WIDTH.length ? BODY_WIDTH[frame] : 30;
        double shrink = 1.0 - Math.min(0.45, lift * 0.025);
        int width = (int) Math.round(bodyWidth * shrink) * ART_SCALE;
        int height = 5 * ART_SCALE;
        int sx = worldX - gp.player.worldX + gp.player.screenX + gp.tileSize / 2 - width / 2;
        int sy = worldY - gp.player.worldY + gp.player.screenY + gp.tileSize - height + 2 * ART_SCALE;
        Composite old = g2.getComposite();
        g2.setComposite(AlphaComposite.SrcOver.derive((float) (0.35 * shrink)));
        g2.setColor(Color.BLACK);
        g2.fillOval(sx, sy, width, height);
        g2.setComposite(old);
    }

    private void drawDizzyStars(Graphics2D g2) {
        int s = gp.scale;
        int cx = getDrawScreenX() + (FRAME_W / 2) * ART_SCALE;
        int cy = getDrawScreenY() + 4 * ART_SCALE;
        for (int i = 0; i < 3; i++) {
            double a = animClock * 0.12 + i * Math.PI * 2 / 3;
            int x = cx + (int) Math.round(Math.cos(a) * 60);
            int y = cy + (int) Math.round(Math.sin(a) * 14);
            g2.setColor(new Color(0x110B08));
            g2.fillRect(x - 2 * s, y - s, 4 * s, 2 * s);
            g2.fillRect(x - s, y - 2 * s, 2 * s, 4 * s);
            g2.setColor(new Color(0xF8D05F));
            g2.fillRect(x - s, y, 2 * s, s / 2 + 1);
            g2.fillRect(x - s / 2, y - s, s, 2 * s);
        }
    }

    // ---------------------------------------------------------------- geometry

    private double[] footCenter() {
        return new double[]{worldX + gp.tileSize / 2.0, worldY + gp.tileSize};
    }

    private void setFoot(double footX, double footY) {
        worldXf = footX - gp.tileSize / 2.0;
        worldYf = footY - gp.tileSize;
        worldX = (int) Math.round(worldXf);
        worldY = (int) Math.round(worldYf);
    }

    private double[] center() {
        Rectangle box = getWorldHitBox();
        return new double[]{box.getCenterX(), box.getCenterY() - lift * ART_SCALE};
    }

    private double[] mouth() {
        double[] c = center();
        return new double[]{c[0], c[1] - 10};
    }

    private double[] playerCenter() {
        Rectangle p = gp.player.getProjectileHitBox();
        return new double[]{p.getCenterX(), p.getCenterY()};
    }

    private double[] playerFeet() {
        return new double[]{gp.player.worldX + gp.tileSize / 2.0, gp.player.worldY + gp.tileSize};
    }

    private double angleToPlayer(double x, double y) {
        double[] p = playerCenter();
        return Math.atan2(p[1] - y, p[0] - x);
    }

    // keeps the body (about 3 tiles wide) inside the walls of the arena
    private double[] clampToArena(double footX, double footY) {
        double minX = gp.tileSize * 2.6, maxX = gp.tileSize * (gp.tileM.getWorldCols() - 2.6);
        double minY = gp.tileSize * 2.4, maxY = gp.tileSize * (gp.tileM.getWorldRows() - 1.2);
        return new double[]{Math.max(minX, Math.min(maxX, footX)), Math.max(minY, Math.min(maxY, footY))};
    }
}
