package entity.enemy;

import dialogue.DialogueLine;
import dialogue.DialogueSpeaker;
import entity.combat.Hit;
import entity.combat.HitResult;
import entity.combat.HitType;
import main.GamePanel;
import main.Sound;
import story.StorySequences;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class Enemy_BYT extends Enemy {
    private enum Attack {
        RING_BURST,
        SPIRAL,
        LANCES,
        GRASP,
        BLINK,
        ECLIPSE
    }

    private static final int MAX_HEALTH = 400;
    private static final int MAX_TIER = 5;
    private static final double DRIFT_SPEED = 0.8;
    private static final int CONTACT_DAMAGE = 1;
    private static final int SHOT_DAMAGE = 1;
    private static final double SHOT_KNOCKBACK = 7.0;
    private static final int PLAYER_HITBOX_INSET = 6;

    private static final String SPRITE_PATH = "splash/BYT_shadow.png";
    private static final int SPRITE_WIDTH = 96;
    private static final int SPRITE_HEIGHT = 144;

    // Palette sampled from the BYT splash art.
    private static final Color VOID = new Color(14, 10, 24);
    private static final Color DEEP = new Color(52, 30, 86);
    private static final Color VIOLET = new Color(120, 78, 190);
    private static final Color LAVENDER = new Color(190, 170, 235);
    private static final Color PALE = new Color(242, 236, 255);

    private static final int EXECUTION_FADE_OUT_START = 50;
    private static final int EXECUTION_TELEPORT = 80;
    private static final int EXECUTION_GROW_END = 120;
    private static final int EXECUTION_TENDRILS_END = 190;
    private static final int EXECUTION_LIFT_END = 240;
    private static final int EXECUTION_CRUSH = 262;
    private static final int EXECUTION_BLACKOUT_START = 290;
    private static final int EXECUTION_BLACKOUT_END = 340;
    private static final int EXECUTION_END = 360;

    // Said when the fight reaches tiers 1..5 (90%, 80%, 70%, 60%, 55% health).
    private static final String[] TIER_TAUNTS = {
            "You scratched me. The sea feels pebbles the same way.",
            "Hm. Then let me hold you a little closer.",
            "Still standing? How strange. Let me correct that.",
            "Stop. You are making the story uncomfortable.",
            "Enough. No more pretending this is a fight."
    };

    private static final String[] HURT_TAUNTS = {
            "There it is.",
            "Count them. Your breaths, I mean.",
            "You flinch so honestly.",
            "Again. I like that sound.",
            "Your body knows. Your hands are slower."
    };

    private final DialogueSpeaker speaker = new DialogueSpeaker("BYT", null, new Color(190, 80, 230));
    private final Random random = new Random();

    private BufferedImage sprite;
    private BufferedImage flashSprite;

    private final List<Orb> orbs = new ArrayList<>();
    private final List<Lance> lances = new ArrayList<>();
    private final List<Grasp> grasps = new ArrayList<>();
    private final List<Particle> particles = new ArrayList<>();
    private final Deque<Attack> attackBag = new ArrayDeque<>();

    private Attack currentAttack = null;
    private Attack lastAttack = null;
    private int attackFrame = 0;
    private int restFrames = 90;
    private double spiralAngle = 0;
    private int spiralSpin = 1;
    private double eclipseGapCenter = 0;

    private int frameCounter = 0;
    private float bodyAlpha = 1f;
    private double drawScale = 1.0;
    private int hitFlashFrames = 0;
    private int shakeAmount = 0;

    private boolean introTauntDone = false;
    private int hurtTauntCooldown = 0;
    private int currentTier = 0;

    private boolean executionPending = false;
    private boolean executing = false;
    private boolean executionFinished = false;
    private int executionFrame = 0;
    private float executionFlash = 0f;

    public Enemy_BYT(GamePanel gp) {
        super(gp);
        direction = "idle";
        setupEnemyStats("BYT", MAX_HEALTH, 1, CONTACT_DAMAGE);
        setupDrawArea(
                SPRITE_WIDTH,
                SPRITE_HEIGHT,
                (gp.tileSize - SPRITE_WIDTH) / 2,
                gp.tileSize - SPRITE_HEIGHT
        );
        setupCollisionAreas(
                new Rectangle(-4, 22, 56, 26),
                new Rectangle(-16, -84, 80, 132)
        );
        loadSprite();
    }

    private void loadSprite() {
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(SPRITE_PATH)) {
            if (stream == null) {
                System.err.println("Missing BYT sprite: " + SPRITE_PATH);
                return;
            }
            BufferedImage source = ImageIO.read(stream);
            sprite = new BufferedImage(SPRITE_WIDTH, SPRITE_HEIGHT, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = sprite.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(source, 0, 0, SPRITE_WIDTH, SPRITE_HEIGHT, null);
            g.dispose();
            flashSprite = createFlashSprite(sprite);
        } catch (IOException e) {
            System.err.println("Could not load BYT sprite: " + e.getMessage());
        }
    }

    private static BufferedImage createFlashSprite(BufferedImage source) {
        BufferedImage flash = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        int paleRgb = PALE.getRGB() & 0x00FFFFFF;
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                int alpha = (source.getRGB(x, y) >>> 24) * 4 / 5;
                flash.setRGB(x, y, (alpha << 24) | paleRgb);
            }
        }
        return flash;
    }

    // ---------------------------------------------------------------- update

    @Override
    public void update() {
        frameCounter++;
        if (hitFlashFrames > 0) {
            hitFlashFrames--;
        }
        if (hurtTauntCooldown > 0) {
            hurtTauntCooldown--;
        }
        updateParticles();

        if (executionPending && !executing) {
            startExecution();
        }
        if (executing) {
            updateExecution();
            return;
        }

        int playerHealthBefore = gp.player.health;

        super.update();
        updateAttacks();
        updateOrbs();
        updateLances();
        updateGrasps();
        updateTaunts(playerHealthBefore);
    }

    @Override
    public void setAction() {
        if (currentAttack == Attack.LANCES || currentAttack == Attack.BLINK) {
            stopMoving();
            return;
        }

        double speed = currentAttack == null ? DRIFT_SPEED * 1.6 : DRIFT_SPEED;
        moveToward(playerCenterX(), playerCenterY(), speed);
    }

    @Override
    public HitResult receiveHit(Hit hit) {
        if (!alive || executing || executionPending) {
            return HitResult.IGNORED;
        }

        // BYT never dies here: damage stops at the execution threshold.
        int damage = Math.min(hit.damage(), health - executionHealth());
        if (damage > 0) {
            takeDamage(damage);
        }
        hitFlashFrames = 6;
        spawnHitSparks(hit);

        // Hits can arrive while bullets are being iterated, so the execution starts on the next update.
        if (health <= executionHealth()) {
            executionPending = true;
        }
        return HitResult.APPLIED;
    }

    private int executionHealth() {
        return maxHealth / 2;
    }

    // 0 at full health, +1 for every 10% lost, 5 for the last 5% before the execution.
    private int tier() {
        double fraction = health / (double) maxHealth;
        if (fraction <= 0.55) {
            return MAX_TIER;
        }
        return Math.min(MAX_TIER - 1, (int) ((1.0 - fraction) * 10));
    }

    private double intensity() {
        return tier() / (double) MAX_TIER;
    }

    // Interpolates between the gentle opening value and the final-tier value.
    private double scaled(double easy, double crazy) {
        return easy + (crazy - easy) * intensity();
    }

    private int scaledInt(double easy, double crazy) {
        return (int) Math.round(scaled(easy, crazy));
    }

    // --------------------------------------------------------------- attacks

    private void updateAttacks() {
        int tier = tier();
        if (tier != currentTier) {
            onTierChanged(tier);
        }

        if (currentAttack == null) {
            if (restFrames > 0) {
                restFrames--;
                if (tier >= 1 && restFrames % 28 == 0) {
                    fireAimedShards(1 + tier / 2, 0.28, scaled(3.0, 3.6));
                }
                return;
            }
            startNextAttack();
        }

        attackFrame++;
        switch (currentAttack) {
            case RING_BURST:
                updateRingBurst();
                break;
            case SPIRAL:
                updateSpiral();
                break;
            case LANCES:
                updateLanceAttack();
                break;
            case GRASP:
                updateGraspAttack();
                break;
            case BLINK:
                updateBlink();
                break;
            case ECLIPSE:
                updateEclipse();
                break;
            default:
                finishAttack();
                break;
        }
    }

    // New attacks unlock as the tiers go up.
    private List<Attack> availableAttacks() {
        List<Attack> attacks = new ArrayList<>();
        Collections.addAll(attacks, Attack.RING_BURST, Attack.SPIRAL, Attack.LANCES);
        int tier = tier();
        if (tier >= 1) {
            attacks.add(Attack.GRASP);
        }
        if (tier >= 2) {
            attacks.add(Attack.BLINK);
        }
        if (tier >= 3) {
            attacks.add(Attack.ECLIPSE);
        }
        return attacks;
    }

    private void onTierChanged(int tier) {
        currentTier = tier;
        attackBag.clear();
        if (tier == 0) {
            return;
        }

        shakeAmount = 4 + tier;
        spawnBurstParticles(centerX(), centerY(), 16 + tier * 6, 3.0);
        if (tier - 1 < TIER_TAUNTS.length) {
            say(TIER_TAUNTS[tier - 1], DialogueLine.Priority.HIGH);
        }
    }

    private void startNextAttack() {
        if (attackBag.isEmpty()) {
            List<Attack> attacks = availableAttacks();
            Collections.shuffle(attacks, random);
            if (attacks.get(0) == lastAttack) {
                Collections.swap(attacks, 0, attacks.size() - 1);
            }
            attackBag.addAll(attacks);
        }

        currentAttack = attackBag.poll();
        attackFrame = 0;
        spiralAngle = random.nextDouble() * Math.PI * 2;
        spiralSpin = random.nextBoolean() ? 1 : -1;
    }

    private void finishAttack() {
        lastAttack = currentAttack;
        currentAttack = null;
        attackFrame = 0;
        bodyAlpha = 1f;
        restFrames = scaledInt(110, 28);
    }

    private void updateRingBurst() {
        int bursts = scaledInt(2, 4);
        int interval = scaledInt(42, 32);
        int burst = (attackFrame - 1) / interval;
        if ((attackFrame - 1) % interval == 0 && burst < bursts) {
            int count = scaledInt(10, 30);
            double offset = burst * Math.PI / count;
            fireRing(count, scaled(1.8, 2.3) + burst * scaled(0.3, 0.55), offset, 9);
            shakeAmount = 3;
        }
        if (attackFrame > bursts * interval + 30) {
            finishAttack();
        }
    }

    private void updateSpiral() {
        int interval = scaledInt(8, 3);
        int arms = scaledInt(2, 5);
        int duration = scaledInt(140, 200);

        if (attackFrame % interval == 0) {
            for (int arm = 0; arm < arms; arm++) {
                double angle = spiralAngle + arm * Math.PI * 2 / arms;
                Orb orb = spawnOrb(centerX(), centerY(), angle, scaled(2.2, 2.7), 7);
                orb.curve = 0.004 * spiralSpin;
            }
            spiralAngle += 0.17 * spiralSpin;
        }
        if (attackFrame == duration / 2) {
            spiralSpin = -spiralSpin;
            if (tier() >= 2) {
                fireRing(16, 2.0, spiralAngle, 10);
            }
        }
        if (attackFrame > duration) {
            finishAttack();
        }
    }

    private void updateLanceAttack() {
        int volleys = scaledInt(2, 4);
        int interval = scaledInt(60, 45);
        int telegraph = scaledInt(60, 42);
        int volley = (attackFrame - 1) / interval;
        if ((attackFrame - 1) % interval == 0 && volley < volleys) {
            double aim = angleToPlayer(centerX(), centerY());
            int count = scaledInt(3, 5);
            double spread = 0.32;
            // From tier 2 every other volley shifts by half a gap, closing the safe lanes of the previous one.
            double shift = (tier() >= 2 && volley % 2 == 1) ? spread / 2 : 0;
            for (int i = 0; i < count; i++) {
                double angle = aim + (i - (count - 1) / 2.0) * spread + shift;
                lances.add(new Lance(centerX(), centerY(), angle, 1100, scaled(24, 30), telegraph, 16));
            }
        }
        if (attackFrame > volleys * interval + telegraph + 30) {
            finishAttack();
        }
    }

    private void updateGraspAttack() {
        int grabs = scaledInt(3, 7);
        int interval = scaledInt(30, 20);
        int grab = (attackFrame - 1) / interval;
        if ((attackFrame - 1) % interval == 0 && grab < grabs) {
            double jitter = grab == 0 ? 0 : 22;
            double x = playerCenterX() + (random.nextDouble() * 2 - 1) * jitter;
            double y = playerCenterY() + (random.nextDouble() * 2 - 1) * jitter;
            grasps.add(new Grasp(x, y, scaled(40, 58), scaledInt(64, 48), 22));
        }
        if (tier() >= 3 && attackFrame % 40 == 0) {
            fireAimedShards(3, 0.35, 3.8);
        }
        if (attackFrame > grabs * interval + 90) {
            finishAttack();
        }
    }

    private void updateBlink() {
        int blinkLength = scaledInt(85, 70);
        int blinks = scaledInt(1, 3);
        int blink = attackFrame / blinkLength;
        int t = attackFrame % blinkLength;

        if (blink >= blinks) {
            bodyAlpha = 1f;
            if (t > 20) {
                finishAttack();
            }
            return;
        }

        if (t < 20) {
            bodyAlpha = 1f - t / 20f;
        }
        else if (t == 20) {
            spawnBurstParticles(centerX(), centerY(), 18, 2.5);
            teleportNearPlayer(150, 200);
            spawnBurstParticles(centerX(), centerY(), 18, 2.5);
        }
        else if (t < 32) {
            bodyAlpha = (t - 20) / 12f;
        }
        else if (t == 32) {
            bodyAlpha = 1f;
            fireRing(scaledInt(12, 30), scaled(2.0, 2.6), random.nextDouble(), 9);
            fireAimedShards(scaledInt(2, 5), 0.22, scaled(3.8, 5.0));
            shakeAmount = 4;
        }
    }

    private void updateEclipse() {
        int walls = scaledInt(2, 5);
        int interval = scaledInt(75, 50);
        int wall = (attackFrame - 1) / interval;
        if ((attackFrame - 1) % interval == 0 && wall < walls) {
            fireWall(random.nextInt(4));
        }
        if (attackFrame > walls * interval + 160) {
            finishAttack();
        }
    }

    // A full-width line of orbs crossing the arena, with one narrow gap placed near the player.
    private void fireWall(int side) {
        int tile = gp.tileSize;
        double minX = tile;
        double maxX = worldWidth() - tile;
        double minY = tile;
        double maxY = worldHeight() - tile;
        boolean horizontal = side < 2;
        double spacing = 24;
        double gapWidth = scaled(130, 64);
        double speed = scaled(2.0, 2.5);
        double gapJitter = scaled(60, 140);

        double playerAxis = horizontal ? playerCenterY() : playerCenterX();
        double axisMin = horizontal ? minY : minX;
        double axisMax = horizontal ? maxY : maxX;
        eclipseGapCenter = clamp(playerAxis + (random.nextDouble() * 2 - 1) * gapJitter, axisMin + gapWidth, axisMax - gapWidth);

        for (double a = axisMin + spacing / 2; a < axisMax; a += spacing) {
            if (Math.abs(a - eclipseGapCenter) < gapWidth / 2) {
                continue;
            }
            Orb orb;
            switch (side) {
                case 0:
                    orb = spawnOrb(minX + 4, a, 0, speed, 9);
                    break;
                case 1:
                    orb = spawnOrb(maxX - 4, a, Math.PI, speed, 9);
                    break;
                case 2:
                    orb = spawnOrb(a, minY + 4, Math.PI / 2, speed, 9);
                    break;
                default:
                    orb = spawnOrb(a, maxY - 4, -Math.PI / 2, speed, 9);
                    break;
            }
            orb.maxAge = 700;
        }
    }

    private void fireRing(int count, double speed, double offset, double radius) {
        for (int i = 0; i < count; i++) {
            double angle = offset + i * Math.PI * 2 / count;
            spawnOrb(centerX(), centerY(), angle, speed, radius);
        }
    }

    private void fireAimedShards(int count, double spread, double speed) {
        double aim = angleToPlayer(centerX(), centerY());
        for (int i = 0; i < count; i++) {
            double angle = aim + (i - (count - 1) / 2.0) * spread;
            Orb shard = spawnOrb(centerX(), centerY(), angle, speed, 6);
            shard.shard = true;
        }
    }

    private Orb spawnOrb(double x, double y, double angle, double speed, double radius) {
        Orb orb = new Orb(x, y, Math.cos(angle) * speed, Math.sin(angle) * speed, radius);
        orbs.add(orb);
        return orb;
    }

    private void teleportNearPlayer(double minDistance, double maxDistance) {
        for (int attempt = 0; attempt < 16; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double distance = minDistance + random.nextDouble() * (maxDistance - minDistance);
            int targetX = (int) (playerCenterX() + Math.cos(angle) * distance) - gp.tileSize / 2;
            int targetY = (int) (playerCenterY() + Math.sin(angle) * distance) + SPRITE_HEIGHT / 2 - gp.tileSize;
            if (!gp.cChecker.checkTileAt(this, targetX, targetY) && !gp.cChecker.checkPlayerAt(this, targetX, targetY)) {
                setPosition(targetX, targetY);
                return;
            }
        }
    }

    private void setPosition(int targetX, int targetY) {
        worldX = targetX;
        worldY = targetY;
        worldXf = targetX;
        worldYf = targetY;
        velX = 0;
        velY = 0;
    }

    // --------------------------------------------------------------- hazards

    private void updateOrbs() {
        Rectangle playerBox = playerDangerBox();
        Iterator<Orb> iterator = orbs.iterator();
        while (iterator.hasNext()) {
            Orb orb = iterator.next();
            orb.update();

            if (orb.age > orb.maxAge || isOutsideArena(orb.x, orb.y, orb.radius)) {
                iterator.remove();
                continue;
            }

            if (circleIntersects(orb.x, orb.y, orb.radius * 0.8, playerBox) && hitPlayer(orb.x, orb.y)) {
                spawnBurstParticles(orb.x, orb.y, 8, 1.8);
                iterator.remove();
            }
        }
    }

    private void updateLances() {
        double px = playerCenterX();
        double py = playerCenterY();
        Iterator<Lance> iterator = lances.iterator();
        while (iterator.hasNext()) {
            Lance lance = iterator.next();
            lance.age++;

            if (lance.age == lance.telegraphFrames) {
                shakeAmount = Math.max(shakeAmount, 2);
            }
            if (lance.isActive() && lance.distanceTo(px, py) < lance.width / 2 + 10) {
                hitPlayer(px, py);
            }
            if (lance.age > lance.telegraphFrames + lance.activeFrames + 10) {
                iterator.remove();
            }
        }
    }

    private void updateGrasps() {
        double px = playerCenterX();
        double py = playerCenterY();
        Iterator<Grasp> iterator = grasps.iterator();
        while (iterator.hasNext()) {
            Grasp grasp = iterator.next();
            grasp.age++;

            if (grasp.age == grasp.telegraphFrames) {
                spawnBurstParticles(grasp.x, grasp.y, 14, 2.2);
            }
            if (grasp.isActive() && Math.hypot(px - grasp.x, py - grasp.y) < grasp.radius * 0.85) {
                hitPlayer(grasp.x, grasp.y);
            }
            if (grasp.age > grasp.telegraphFrames + grasp.activeFrames) {
                iterator.remove();
            }
        }
    }

    private boolean hitPlayer(double sourceX, double sourceY) {
        if (!gp.player.alive || gp.player.isHurtInvincible()) {
            return false;
        }

        Hit hit = new Hit(SHOT_DAMAGE, HitType.RANGED, this, (int) sourceX, (int) sourceY, SHOT_KNOCKBACK, null);
        return gp.player.receiveHit(hit) == HitResult.APPLIED;
    }

    // ---------------------------------------------------------------- taunts

    private void updateTaunts(int playerHealthBefore) {
        if (!introTauntDone) {
            introTauntDone = true;
            say("Go on. Make it brief.", DialogueLine.Priority.HIGH);
            return;
        }

        if (gp.player.alive && gp.player.health < playerHealthBefore && hurtTauntCooldown == 0 && random.nextInt(2) == 0) {
            hurtTauntCooldown = 60 * 5;
            say(HURT_TAUNTS[random.nextInt(HURT_TAUNTS.length)], DialogueLine.Priority.NORMAL);
        }
    }

    private void say(String text, DialogueLine.Priority priority) {
        gp.dialogueManager.showAmbient(speaker, text, priority, 60 * 3);
    }

    // ------------------------------------------------------------- execution

    private void startExecution() {
        executing = true;
        executionFrame = 0;
        currentAttack = null;
        orbs.clear();
        lances.clear();
        grasps.clear();
        stopMoving();
        gp.bulletManager.clearBullets();
        gp.keyH.clearGameplayInput();
        gp.player.beginScriptedHold();
        spawnBurstParticles(centerX(), centerY(), 40, 4.0);
        executionFlash = 0.8f;
        say("...No.", DialogueLine.Priority.HIGH);
    }

    private void updateExecution() {
        executionFrame++;
        int t = executionFrame;
        executionFlash = Math.max(0f, executionFlash - 0.03f);

        if (t < EXECUTION_FADE_OUT_START) {
            shakeAmount = 3;
            // Everything around gets pulled into the shape.
            if (t % 2 == 0) {
                double angle = random.nextDouble() * Math.PI * 2;
                double distance = 160 + random.nextDouble() * 120;
                double x = centerX() + Math.cos(angle) * distance;
                double y = centerY() + Math.sin(angle) * distance;
                particles.add(new Particle(x, y, -Math.cos(angle) * 4, -Math.sin(angle) * 4, 40, 3, LAVENDER));
            }
        }
        else if (t < EXECUTION_TELEPORT) {
            bodyAlpha = 1f - (t - EXECUTION_FADE_OUT_START) / (float) (EXECUTION_TELEPORT - EXECUTION_FADE_OUT_START);
        }
        else if (t == EXECUTION_TELEPORT) {
            bodyAlpha = 0f;
            setPosition(gp.player.worldX, gp.player.worldY - 34);
            say("You were never meant to see this part.", DialogueLine.Priority.HIGH);
        }
        else if (t <= EXECUTION_GROW_END) {
            float progress = (t - EXECUTION_TELEPORT) / (float) (EXECUTION_GROW_END - EXECUTION_TELEPORT);
            bodyAlpha = progress;
            drawScale = 1.0 + 0.6 * easeOut(progress);
        }
        else if (t <= EXECUTION_TENDRILS_END) {
            bodyAlpha = 1f;
        }
        else if (t <= EXECUTION_LIFT_END) {
            float progress = (t - EXECUTION_TENDRILS_END) / (float) (EXECUTION_LIFT_END - EXECUTION_TENDRILS_END);
            gp.player.setScriptedDrawOffset(random.nextInt(3) - 1, (int) (-26 * easeOut(progress)));
        }
        else if (t < EXECUTION_CRUSH) {
            gp.player.setScriptedDrawOffset(random.nextInt(5) - 2, -26 + random.nextInt(3) - 1);
            shakeAmount = 2;
        }
        else if (t == EXECUTION_CRUSH) {
            gp.player.setScriptedHidden(true);
            gp.player.health = 0;
            gp.playSoundEffect(Sound.PLAYER_DEATH);
            executionFlash = 1f;
            shakeAmount = 8;
            double px = playerCenterX();
            double py = playerCenterY() - 26;
            spawnBurstParticles(px, py, 70, 5.5);
            for (int i = 0; i < 24; i++) {
                double angle = random.nextDouble() * Math.PI * 2;
                double speed = 1 + random.nextDouble() * 3;
                particles.add(new Particle(px, py, Math.cos(angle) * speed, Math.sin(angle) * speed, 70, 5, PALE));
            }
        }
        else if (t >= EXECUTION_END && !executionFinished) {
            executionFinished = true;
            gp.storySequenceManager.start(StorySequences.prologueBytScriptedDefeat(gp));
        }

        if (shakeAmount > 0 && t % 3 == 0) {
            shakeAmount--;
        }
    }

    private float tendrilProgress() {
        if (executionFrame <= EXECUTION_GROW_END) {
            return 0f;
        }
        return Math.min(1f, (executionFrame - EXECUTION_GROW_END) / (float) (EXECUTION_TENDRILS_END - EXECUTION_GROW_END));
    }

    private static float easeOut(float t) {
        return 1f - (1f - t) * (1f - t);
    }

    // ------------------------------------------------------------- particles

    private void spawnBurstParticles(double x, double y, int count, double maxSpeed) {
        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double speed = 0.5 + random.nextDouble() * maxSpeed;
            Color color = random.nextInt(3) == 0 ? LAVENDER : VIOLET;
            int life = 20 + random.nextInt(25);
            particles.add(new Particle(x, y, Math.cos(angle) * speed, Math.sin(angle) * speed, life, 2 + random.nextInt(3), color));
        }
    }

    private void updateParticles() {
        Iterator<Particle> iterator = particles.iterator();
        while (iterator.hasNext()) {
            Particle particle = iterator.next();
            particle.x += particle.vx;
            particle.y += particle.vy;
            particle.vx *= 0.94;
            particle.vy *= 0.94;
            particle.life--;
            if (particle.life <= 0) {
                iterator.remove();
            }
        }
    }

    // --------------------------------------------------------------- drawing

    @Override
    public void draw(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        for (Grasp grasp : grasps) {
            drawGraspTelegraph(g2, grasp);
        }
        for (Lance lance : lances) {
            if (!lance.isActive() && lance.age < lance.telegraphFrames) {
                drawLanceTelegraph(g2, lance);
            }
        }
        drawBody(g2);

        g2.dispose();
    }

    @Override
    public void drawOverlay(Graphics2D original) {
        Graphics2D g2 = (Graphics2D) original.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        for (Lance lance : lances) {
            if (lance.age >= lance.telegraphFrames) {
                drawLanceBeam(g2, lance);
            }
        }
        for (Grasp grasp : grasps) {
            if (grasp.age >= grasp.telegraphFrames) {
                drawGraspEruption(g2, grasp);
            }
        }
        for (Orb orb : orbs) {
            drawOrb(g2, orb);
        }
        drawParticles(g2);

        if (executing) {
            drawExecutionOverlay(g2);
        }

        g2.dispose();
    }

    private void drawBody(Graphics2D g2) {
        if (sprite == null || bodyAlpha <= 0f) {
            return;
        }

        int width = (int) (SPRITE_WIDTH * drawScale);
        int height = (int) (SPRITE_HEIGHT * drawScale);
        int bob = (int) Math.round(Math.sin(frameCounter * 0.05) * 4);
        int shakeX = shakeAmount > 0 ? random.nextInt(shakeAmount * 2 + 1) - shakeAmount : 0;
        int shakeY = shakeAmount > 0 ? random.nextInt(shakeAmount * 2 + 1) - shakeAmount : 0;

        int centerScreenX = screenX(worldX + gp.tileSize / 2.0) + shakeX;
        int bottomScreenY = screenY(worldY + gp.tileSize) + shakeY;
        int x = centerScreenX - width / 2;
        int y = bottomScreenY - height + bob;

        Composite oldComposite = g2.getComposite();

        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.45f * bodyAlpha));
        g2.setColor(VOID);
        g2.fillOval(centerScreenX - width / 3, bottomScreenY - 10, width * 2 / 3, 18);

        float pulse = 0.35f + 0.15f * (float) Math.sin(frameCounter * 0.08);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.min(1f, pulse * bodyAlpha)));
        float auraRadius = width * 0.85f;
        g2.setPaint(new RadialGradientPaint(
                new Point(centerScreenX, y + height / 2),
                auraRadius,
                new float[]{0f, 1f},
                new Color[]{withAlpha(VIOLET, 150), withAlpha(VIOLET, 0)}
        ));
        g2.fillOval((int) (centerScreenX - auraRadius), (int) (y + height / 2 - auraRadius), (int) (auraRadius * 2), (int) (auraRadius * 2));

        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, bodyAlpha));
        g2.drawImage(sprite, x, y, width, height, null);

        if (hitFlashFrames > 0 && flashSprite != null) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, bodyAlpha * hitFlashFrames / 6f));
            g2.drawImage(flashSprite, x, y, width, height, null);
        }

        g2.setComposite(oldComposite);
    }

    private void drawOrb(Graphics2D g2, Orb orb) {
        int x = screenX(orb.x);
        int y = screenY(orb.y);

        if (orb.shard) {
            AffineTransform oldTransform = g2.getTransform();
            g2.translate(x, y);
            g2.rotate(Math.atan2(orb.vy, orb.vx));
            double r = orb.radius;
            Path2D.Double shape = new Path2D.Double();
            shape.moveTo(r * 2.4, 0);
            shape.lineTo(0, -r * 0.8);
            shape.lineTo(-r * 1.6, 0);
            shape.lineTo(0, r * 0.8);
            shape.closePath();
            g2.setColor(withAlpha(VIOLET, 90));
            g2.fillOval((int) (-r * 2), (int) (-r * 1.4), (int) (r * 4), (int) (r * 2.8));
            g2.setColor(LAVENDER);
            g2.fill(shape);
            g2.setColor(VOID);
            g2.setStroke(new BasicStroke(1.5f));
            g2.draw(shape);
            g2.setTransform(oldTransform);
            return;
        }

        int r = (int) orb.radius;
        int glow = (int) (orb.radius * 1.8);
        g2.setColor(withAlpha(VIOLET, 70));
        g2.fillOval(x - glow, y - glow, glow * 2, glow * 2);
        g2.setColor(DEEP);
        g2.fillOval(x - r, y - r, r * 2, r * 2);
        g2.setColor(LAVENDER);
        g2.setStroke(new BasicStroke(2f));
        g2.drawOval(x - r, y - r, r * 2, r * 2);
        int core = Math.max(2, (int) (orb.radius * 0.35));
        g2.setColor(PALE);
        g2.fillOval(x - core, y - core, core * 2, core * 2);
    }

    private void drawLanceTelegraph(Graphics2D g2, Lance lance) {
        float progress = lance.age / (float) lance.telegraphFrames;
        int alpha = (int) (60 + 120 * progress * (0.6 + 0.4 * Math.sin(lance.age * 0.6)));
        int x1 = screenX(lance.x);
        int y1 = screenY(lance.y);
        int x2 = screenX(lance.endX());
        int y2 = screenY(lance.endY());

        g2.setColor(withAlpha(VIOLET, Math.max(0, Math.min(255, alpha / 3))));
        g2.setStroke(new BasicStroke((float) (lance.width * progress), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine(x1, y1, x2, y2);
        g2.setColor(withAlpha(LAVENDER, Math.max(0, Math.min(255, alpha))));
        g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 1f, new float[]{10f, 8f}, lance.age));
        g2.drawLine(x1, y1, x2, y2);
    }

    private void drawLanceBeam(Graphics2D g2, Lance lance) {
        int sinceStart = lance.age - lance.telegraphFrames;
        float fade = lance.isActive() ? 1f : Math.max(0f, 1f - (sinceStart - lance.activeFrames) / 10f);
        float swell = lance.isActive() ? Math.min(1f, sinceStart / 4f) : 1f;
        float width = (float) lance.width * swell;
        int x1 = screenX(lance.x);
        int y1 = screenY(lance.y);
        int x2 = screenX(lance.endX());
        int y2 = screenY(lance.endY());

        g2.setColor(withAlpha(VIOLET, (int) (120 * fade)));
        g2.setStroke(new BasicStroke(width * 1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine(x1, y1, x2, y2);
        g2.setColor(withAlpha(DEEP, (int) (230 * fade)));
        g2.setStroke(new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine(x1, y1, x2, y2);
        g2.setColor(withAlpha(LAVENDER, (int) (230 * fade)));
        g2.setStroke(new BasicStroke(width * 0.45f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine(x1, y1, x2, y2);
        g2.setColor(withAlpha(PALE, (int) (255 * fade)));
        g2.setStroke(new BasicStroke(Math.max(1f, width * 0.15f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine(x1, y1, x2, y2);
    }

    private void drawGraspTelegraph(Graphics2D g2, Grasp grasp) {
        if (grasp.age >= grasp.telegraphFrames) {
            return;
        }

        float progress = grasp.age / (float) grasp.telegraphFrames;
        int x = screenX(grasp.x);
        int y = screenY(grasp.y);
        int r = (int) grasp.radius;

        g2.setColor(withAlpha(VOID, (int) (40 + 140 * progress)));
        g2.fillOval(x - r, y - r / 2, r * 2, r);

        g2.setColor(withAlpha(VIOLET, (int) (120 + 100 * Math.abs(Math.sin(grasp.age * 0.3)))));
        g2.setStroke(new BasicStroke(2.5f));
        g2.drawOval(x - r, y - r / 2, r * 2, r);

        int inner = (int) (r * (1f - progress));
        g2.setColor(withAlpha(LAVENDER, 200));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawOval(x - inner, y - inner / 2, inner * 2, inner);
    }

    private void drawGraspEruption(Graphics2D g2, Grasp grasp) {
        int sinceStart = grasp.age - grasp.telegraphFrames;
        float progress = sinceStart / (float) grasp.activeFrames;
        float rise = progress < 0.3f ? progress / 0.3f : 1f - (progress - 0.3f) / 0.7f * 0.6f;
        int x = screenX(grasp.x);
        int y = screenY(grasp.y);
        int r = (int) grasp.radius;

        g2.setColor(withAlpha(VOID, (int) (220 * (1f - progress * 0.6f))));
        g2.fillOval(x - r, y - r / 2, r * 2, r);

        int tendrils = 7;
        for (int i = 0; i < tendrils; i++) {
            double baseAngle = i * Math.PI * 2 / tendrils + grasp.seed;
            double bx = x + Math.cos(baseAngle) * r * 0.7;
            double by = y + Math.sin(baseAngle) * r * 0.35;
            double tipX = x + Math.cos(baseAngle) * r * 0.25;
            double tipY = by - (50 + 20 * Math.sin(i * 1.7)) * rise;
            drawTendril(g2, bx, by, tipX, tipY, 9, grasp.age * 0.35 + i, 7f);
        }
    }

    private void drawTendril(Graphics2D g2, double x0, double y0, double x1, double y1, double amplitude, double phase, float width) {
        int segments = 14;
        double dx = x1 - x0;
        double dy = y1 - y0;
        double length = Math.hypot(dx, dy);
        if (length < 1) {
            return;
        }
        double nx = -dy / length;
        double ny = dx / length;

        Path2D.Double path = new Path2D.Double();
        for (int i = 0; i <= segments; i++) {
            double t = i / (double) segments;
            double wave = Math.sin(t * Math.PI * 2.2 + phase) * amplitude * Math.sin(t * Math.PI);
            double px = x0 + dx * t + nx * wave;
            double py = y0 + dy * t + ny * wave;
            if (i == 0) {
                path.moveTo(px, py);
            }
            else {
                path.lineTo(px, py);
            }
        }

        g2.setStroke(new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(VOID);
        g2.draw(path);
        g2.setStroke(new BasicStroke(width * 0.55f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(DEEP);
        g2.draw(path);
        g2.setStroke(new BasicStroke(Math.max(1f, width * 0.18f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(LAVENDER);
        g2.draw(path);
    }

    private void drawParticles(Graphics2D g2) {
        for (Particle particle : particles) {
            float alpha = Math.min(1f, particle.life / (float) particle.maxLife * 1.5f);
            g2.setColor(withAlpha(particle.color, (int) (255 * alpha)));
            int size = particle.size;
            g2.fillOval(screenX(particle.x) - size / 2, screenY(particle.y) - size / 2, size, size);
        }
    }

    private void drawExecutionOverlay(Graphics2D g2) {
        int t = executionFrame;
        int playerX = gp.player.screenX + gp.tileSize / 2;
        int playerY = gp.player.screenY + gp.tileSize / 2;

        // Darkness closing in around the player.
        float close = Math.min(1f, t / (float) EXECUTION_TENDRILS_END);
        float outer = Math.max(gp.screenWidth, gp.screenHeight);
        float radius = outer * (1.1f - 0.75f * close);
        g2.setPaint(new RadialGradientPaint(
                new Point(playerX, playerY),
                radius,
                new float[]{0f, 0.45f, 1f},
                new Color[]{withAlpha(VOID, 0), withAlpha(DEEP, (int) (90 * close)), withAlpha(VOID, (int) (235 * close))}
        ));
        g2.fillRect(0, 0, gp.screenWidth, gp.screenHeight);

        // Tendrils reaching from BYT around the player.
        float reach = tendrilProgress();
        if (reach > 0f && t < EXECUTION_CRUSH + 6) {
            double bytX = screenX(worldX + gp.tileSize / 2.0);
            double bytY = screenY(worldY + gp.tileSize - SPRITE_HEIGHT * drawScale * 0.45);
            int liftOffset = t > EXECUTION_TENDRILS_END ? -26 : 0;
            float squeeze = t > EXECUTION_LIFT_END
                    ? Math.min(1f, (t - EXECUTION_LIFT_END) / (float) (EXECUTION_CRUSH - EXECUTION_LIFT_END))
                    : 0f;
            int tendrils = 8;
            for (int i = 0; i < tendrils; i++) {
                double side = (i % 2 == 0) ? -1 : 1;
                double startX = bytX + side * (20 + i * 4);
                double startY = bytY + (i * 7) - 20;
                double wrapAngle = i * Math.PI * 2 / tendrils + t * 0.05;
                double wrapRadius = 30 * (1f - squeeze * 0.8f);
                double targetX = playerX + Math.cos(wrapAngle) * wrapRadius;
                double targetY = playerY + liftOffset + Math.sin(wrapAngle) * wrapRadius * 0.8;
                double tipX = startX + (targetX - startX) * reach;
                double tipY = startY + (targetY - startY) * reach;
                drawTendril(g2, startX, startY, tipX, tipY, 14 * (1f - squeeze), t * 0.18 + i, 8f);
            }
        }

        if (executionFlash > 0f) {
            g2.setColor(withAlpha(PALE, (int) (255 * Math.min(1f, executionFlash))));
            g2.fillRect(0, 0, gp.screenWidth, gp.screenHeight);
        }

        if (t >= EXECUTION_BLACKOUT_START) {
            float black = Math.min(1f, (t - EXECUTION_BLACKOUT_START) / (float) (EXECUTION_BLACKOUT_END - EXECUTION_BLACKOUT_START));
            g2.setColor(withAlpha(Color.BLACK, (int) (255 * black)));
            g2.fillRect(0, 0, gp.screenWidth, gp.screenHeight);
        }
    }

    // --------------------------------------------------------------- helpers

    private double centerX() {
        return worldX + gp.tileSize / 2.0;
    }

    private double centerY() {
        return worldY + gp.tileSize - SPRITE_HEIGHT / 2.0;
    }

    private int playerCenterX() {
        Rectangle box = gp.player.getWorldHitBox();
        return box.x + box.width / 2;
    }

    private int playerCenterY() {
        Rectangle box = gp.player.getWorldHitBox();
        return box.y + box.height / 2;
    }

    private Rectangle playerDangerBox() {
        Rectangle box = gp.player.getWorldHitBox();
        box.grow(-PLAYER_HITBOX_INSET, -PLAYER_HITBOX_INSET);
        return box;
    }

    private double angleToPlayer(double fromX, double fromY) {
        return Math.atan2(playerCenterY() - fromY, playerCenterX() - fromX);
    }

    private int screenX(double worldPosX) {
        return (int) Math.round(worldPosX - gp.player.worldX + gp.player.screenX);
    }

    private int screenY(double worldPosY) {
        return (int) Math.round(worldPosY - gp.player.worldY + gp.player.screenY);
    }

    private int worldWidth() {
        return gp.tileM.getWorldCols() * gp.tileSize;
    }

    private int worldHeight() {
        return gp.tileM.getWorldRows() * gp.tileSize;
    }

    private boolean isOutsideArena(double x, double y, double radius) {
        return x < -radius || y < -radius || x > worldWidth() + radius || y > worldHeight() + radius;
    }

    private static boolean circleIntersects(double cx, double cy, double radius, Rectangle box) {
        double nearestX = clamp(cx, box.x, box.x + box.width);
        double nearestY = clamp(cy, box.y, box.y + box.height);
        double dx = cx - nearestX;
        double dy = cy - nearestY;
        return dx * dx + dy * dy <= radius * radius;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static Color withAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.max(0, Math.min(255, alpha)));
    }

    // ------------------------------------------------------------ hazard data

    private static final class Orb {
        double x;
        double y;
        double vx;
        double vy;
        final double radius;
        double curve = 0;
        boolean shard = false;
        int age = 0;
        int maxAge = 420;

        Orb(double x, double y, double vx, double vy, double radius) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.radius = radius;
        }

        void update() {
            if (curve != 0) {
                double cos = Math.cos(curve);
                double sin = Math.sin(curve);
                double nextVx = vx * cos - vy * sin;
                vy = vx * sin + vy * cos;
                vx = nextVx;
            }
            x += vx;
            y += vy;
            age++;
        }
    }

    private static final class Lance {
        final double x;
        final double y;
        final double angle;
        final double length;
        final double width;
        final int telegraphFrames;
        final int activeFrames;
        int age = 0;

        Lance(double x, double y, double angle, double length, double width, int telegraphFrames, int activeFrames) {
            this.x = x;
            this.y = y;
            this.angle = angle;
            this.length = length;
            this.width = width;
            this.telegraphFrames = telegraphFrames;
            this.activeFrames = activeFrames;
        }

        boolean isActive() {
            return age >= telegraphFrames && age < telegraphFrames + activeFrames;
        }

        double endX() {
            return x + Math.cos(angle) * length;
        }

        double endY() {
            return y + Math.sin(angle) * length;
        }

        double distanceTo(double px, double py) {
            double dx = Math.cos(angle);
            double dy = Math.sin(angle);
            double along = clamp((px - x) * dx + (py - y) * dy, 0, length);
            return Math.hypot(px - (x + dx * along), py - (y + dy * along));
        }
    }

    private static final class Grasp {
        final double x;
        final double y;
        final double radius;
        final int telegraphFrames;
        final int activeFrames;
        final double seed = Math.random() * Math.PI;
        int age = 0;

        Grasp(double x, double y, double radius, int telegraphFrames, int activeFrames) {
            this.x = x;
            this.y = y;
            this.radius = radius;
            this.telegraphFrames = telegraphFrames;
            this.activeFrames = activeFrames;
        }

        boolean isActive() {
            return age >= telegraphFrames && age < telegraphFrames + activeFrames;
        }
    }

    private static final class Particle {
        double x;
        double y;
        double vx;
        double vy;
        int life;
        final int maxLife;
        final int size;
        final Color color;

        Particle(double x, double y, double vx, double vy, int life, int size, Color color) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.life = life;
            this.maxLife = life;
            this.size = size;
            this.color = color;
        }
    }
}
