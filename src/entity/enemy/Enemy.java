package entity.enemy;

import entity.Entity;
import entity.anim.Silhouette;
import entity.combat.Hit;
import entity.combat.HitResult;
import entity.combat.HitType;
import entity.CombatState;
import main.GamePanel;
import main.Progression;
import main.Sound;
import object.MushroomPickup;
import story.QuestLog;
import world.EffectsManager;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Random;

public class Enemy extends Entity {
    public String persistenceId = "";
    public boolean isEngaged() { return alive && gp.levelManager.getActiveBoss() == this; }
    public void restoreSnapshot(int hp, int x, int y) {
        health = Math.max(1, Math.min(maxHealth, hp)); alive = true;
        worldX = x; worldY = y; worldXf = x; worldYf = y;
        floatingPositionInitialized = true;
    }
    private static final int DAMAGE_FLASH_FRAMES = 7;

    public String name;
    public int attackDamage = 1;
    protected double worldXf;
    protected double worldYf;
    protected double velX = 0;
    protected double velY = 0;
    private boolean floatingPositionInitialized = false;
    private int patrolFramesLeft = 0;
    private double patrolVelX = 0;
    private double patrolVelY = 0;
    private int recoilFramesLeft = 0;
    private final int recoilDurationFrames = 12;
    private int movementPauseFramesLeft = 0;
    private boolean touchedPlayerThisFrame = false;
    private int contactDamageCooldownCounter = 0;
    private final int contactDamageCooldownFrames = 60;
    private int damageFlashFramesLeft = 0;
    private boolean deathHandled = false;
    protected int mushroomValue = 1;
    protected int experienceValue = 2;
    private final Random random = new Random();

    // Marks: player shots stack marks (bosses), a sword hit detonates them for heavy damage.
    public static final int MAX_MARKS = 6;
    private static final int MARK_DECAY_TICKS = 360;
    private static final int DETONATION_PER_MARK = 30;
    protected boolean usesMarks = false;
    private int marks = 0;
    private int markTimer = 0;
    private int markCooldown = 0;
    private static final int ADAPT_START_TICKS = 720;
    private static final int ADAPT_FULL_TICKS = 1080;
    private static final int ADAPT_IDLE_TICKS = 180;
    private static final double ADAPT_MIN_MULTIPLIER = 0.3;
    protected boolean adapts = false;
    protected double adaptSpeed = 1.0;
    private HitType adaptType = null;
    private double adaptStreak = 0;
    private int adaptIdle = 0;

    public Enemy(GamePanel gp) {
        super(gp);
    }

    protected void setupEnemyStats(String name, int maxHealth, int speed, int attackDamage) {
        this.name = name;
        this.maxHealth = maxHealth;
        this.experienceValue = Math.max(1, Math.min(100, (int) Math.round(maxHealth / 6.0)));
        this.health = maxHealth;
        this.speed = speed;
        this.attackDamage = attackDamage;
        this.alive = true;
    }

    protected void setupCollisionAreas(Rectangle solidArea, Rectangle hitBox) {
        this.solidArea = solidArea;
        this.solidAreaDefaultX = solidArea.x;
        this.solidAreaDefaultY = solidArea.y;
        this.hitBox = hitBox;
    }

    @Override
    public void update() {
        if (!alive) {
            handleDeathOnce();
            return;
        }
        initializeFloatingPosition();
        tickCombatModifiers();
        if (damageFlashFramesLeft > 0) {
            damageFlashFramesLeft--;
        }
        touchedPlayerThisFrame = false;
        if (movementPauseFramesLeft > 0) {
            movementPauseFramesLeft--;
            stopMoving();
        }
        else if (recoilFramesLeft > 0) {
            recoilFramesLeft--;
        }
        else {
            setAction();
        }
        applySmoothMovement();
        updateSpriteDirection();
        updateSpriteAnimation();
        updateContactDamage();
    }

    @Override
    public void setAction() {
        patrolRandomly();
    }

    protected void updateContactDamage() {
        if (contactDamageCooldownCounter > 0) {
            contactDamageCooldownCounter--;
        }

        if (!gp.player.alive || gp.player.isHurtInvincible()) {
            return;
        }

        if (contactDamageCooldownCounter == 0 &&
                (touchedPlayerThisFrame || getWorldSolidArea().intersects(gp.player.getWorldHitBox()))) {
            if (gp.player.takeDamageFromEntity(this, attackDamage)) {
                pauseMovement(gp.player.getHurtInvincibleFrames());
                contactDamageCooldownCounter = contactDamageCooldownFrames;
            }
        }
    }

    protected void patrolRandomly() {
        if (patrolFramesLeft > 0) {
            patrolFramesLeft--;
            velX = patrolVelX;
            velY = patrolVelY;
            return;
        }

        if (random.nextInt(100) < 35) {
            patrolVelX = 0;
            patrolVelY = 0;
            velX = 0;
            velY = 0;
            direction = "idle";
            patrolFramesLeft = random.nextInt(45) + 30;
            return;
        }

        double angle = random.nextDouble() * Math.PI * 2;
        patrolVelX = Math.cos(angle) * speed;
        patrolVelY = Math.sin(angle) * speed;
        velX = patrolVelX;
        velY = patrolVelY;
        patrolFramesLeft = random.nextInt(45) + 45;
    }

    protected void moveToward(int targetWorldX, int targetWorldY, double moveSpeed) {
        int enemyCenterX = worldX + gp.tileSize / 2;
        int enemyCenterY = worldY + gp.tileSize / 2;

        double dx = targetWorldX - enemyCenterX;
        double dy = targetWorldY - enemyCenterY;
        double length = Math.sqrt(dx * dx + dy * dy);

        if (length == 0) {
            velX = 0;
            velY = 0;
            return;
        }

        velX = (dx / length) * moveSpeed;
        velY = (dy / length) * moveSpeed;
    }

    protected void stopMoving() {
        velX = 0;
        velY = 0;
        direction = "idle";
    }

    public void applyKnockbackFrom(int sourceWorldX, int sourceWorldY, double force) {
        applyKnockbackFrom(sourceWorldX, sourceWorldY, force, recoilDurationFrames);
    }

    public void applyKnockbackFrom(int sourceWorldX, int sourceWorldY, double force, int recoilFrames) {
        int enemyCenterX = worldX + gp.tileSize / 2;
        int enemyCenterY = worldY + gp.tileSize / 2;

        double dx = enemyCenterX - sourceWorldX;
        double dy = enemyCenterY - sourceWorldY;
        double length = Math.sqrt(dx * dx + dy * dy);

        if (length == 0) {
            dx = 1;
            dy = 0;
            length = 1;
        }

        velX = (dx / length) * force;
        velY = (dy / length) * force;
        recoilFramesLeft = recoilFrames;
        patrolFramesLeft = 0;
    }

    public void pauseMovement(int frames) {
        movementPauseFramesLeft = frames;
        recoilFramesLeft = 0;
        patrolFramesLeft = 0;
        stopMoving();
    }

    @Override
    public void takeDamage(int damage) {
        super.takeDamage(damage);
        if (damage > 0 && gp.campaign != null) gp.campaign.engage();
        damageFlashFramesLeft = DAMAGE_FLASH_FRAMES;
        if (damage > 0) {
            gp.playSoundEffectOf(Sound.HIT_1, Sound.HIT_2, Sound.HIT_3);
        }
    }

    protected void playNear(int sound) {
        double range = gp.tileSize * 10.0;
        if (Math.hypot(worldX - gp.player.worldX, worldY - gp.player.worldY) <= range) {
            gp.playSoundEffect(sound);
        }
    }

    @Override
    public HitResult receiveHit(Hit hit) {
        if (!alive) {
            return HitResult.IGNORED;
        }

        takeDamage(resolveDamage(hit));
        spawnHitSparks(hit);
        if (hit.knockbackForce() > 0) {
            applyKnockbackFrom(hit.sourceWorldX(), hit.sourceWorldY(), hit.knockbackForce());
        }
        return HitResult.APPLIED;
    }

    protected void shovePlayerOut() {
        Rectangle body = getWorldSolidArea();
        Rectangle player = gp.player.getWorldSolidArea();
        if (!body.intersects(player)) {
            return;
        }
        double dx = player.getCenterX() - body.getCenterX();
        double dy = player.getCenterY() - body.getCenterY();
        double len = Math.hypot(dx, dy);
        if (len < 1e-6) {
            dx = 1;
            dy = 0;
            len = 1;
        }
        dx /= len;
        dy /= len;
        for (int step = 4; step < 240; step += 4) {
            int nx = (int) Math.round(gp.player.worldX + dx * step);
            int ny = (int) Math.round(gp.player.worldY + dy * step);
            Rectangle moved = new Rectangle(player);
            moved.translate(nx - gp.player.worldX, ny - gp.player.worldY);
            if (!moved.intersects(body) && !gp.cChecker.checkTileAt(gp.player, nx, ny)) {
                gp.player.setWorldPosition(nx, ny);
                return;
            }
        }
    }

    // Damage multiplier for a hit type; bosses with a changing shell override it.
    protected double resistance(HitType type) {
        return 1.0;
    }

    // The attack mode this enemy currently wants (shown next to the boss bar), or null.
    public CombatState wantedMode() {
        return null;
    }

    // Health fractions where the boss changes phase, for notches on the boss bar.
    public double[] phaseThresholds() {
        return new double[0];
    }

    protected int resolveDamage(Hit hit) {
        boolean fromPlayer = hit.source() == gp.player;
        if (!fromPlayer) {
            return hit.damage();
        }
        Rectangle box = getWorldHitBox();
        double textX = box.getCenterX();
        double textY = box.y - 6;

        double multiplier = gp.player.consumeSwapBonus();
        boolean swap = multiplier > 1.0;
        double resist = resistance(hit.type());
        multiplier *= resist;
        multiplier *= adaptationMultiplier(hit.type());
        multiplier *= gp.player.runDamageMultiplier();
        multiplier *= gp.relics.damage(hit.type());
        multiplier *= gp.debugSettings.playerDamageMultiplier();
        registerAdaptation(hit.type());
        int damage = (int) Math.round(hit.damage() * multiplier);
        if (hit.damage() > 0) {
            damage = Math.max(1, damage);
        }

        int detonation = 0;
        if (usesMarks || gp.relics.active(main.Relics.Item.CROWN)) {
            if (hit.type() == HitType.RANGED) {
                if (markCooldown == 0 && marks < gp.relics.markLimit()) {
                    marks++;
                    markCooldown = 4;
                    gp.effects.ring(box.getCenterX(), box.getCenterY(), 22 + marks * 4, 10, new Color(0xF2C46A), false);
                }
                markTimer = MARK_DECAY_TICKS;
            } else if (hit.type() == HitType.MELEE && marks > 0) {
                detonation = gp.debugSettings.scalePlayerDamage(marks * gp.relics.detonationPerMark());
                detonate(box, marks, detonation);
                marks = 0;
            }
        }

        if (swap) {
            gp.effects.text(textX, textY - 22, "SWAP!", new Color(0x9AD8F0), 2);
            gp.effects.ring(box.getCenterX(), box.getCenterY(), 50, 12, new Color(0x9AD8F0), false);
        }
        if (resist < 0.5) {
            gp.effects.text(textX, textY - 12, "RESISTED", new Color(0x8A7E78), 1);
        }
        gp.effects.text(textX, textY, String.valueOf(damage), resist < 0.5 ? new Color(0xA8A29A) : Color.WHITE, 2);
        return damage + detonation;
    }

    private void detonate(Rectangle box, int count, int damage) {
        double cx = box.getCenterX(), cy = box.getCenterY();
        gp.lighting.pulse(cx, cy, 190, world.Palette.GOLD[1], 1f, 14);
        gp.screenFx.shockwave(cx, cy);
        gp.screenFx.aberration(8);
        gp.effects.ring(cx, cy, 90, 18, new Color(0xF2C46A), false);
        gp.effects.ring(cx, cy, 130, 24, new Color(0xFFF6D0), true);
        gp.effects.burst(cx, cy, 18 + count * 3, 4.0, EffectsManager.GOLD);
        gp.effects.sparks(cx, cy, 0, -1, 10, EffectsManager.SPARK);
        gp.effects.text(cx, box.y - 40, "DETONATE X" + count, new Color(0xF2C46A), 2);
        gp.effects.text(cx, box.y - 20, String.valueOf(damage), new Color(0xFFD86A), 3);
        gp.effects.shake(10, 6 + count);
        gp.effects.flash(5, new Color(0xFFE8A0), 0.18f + count * 0.02f);
        gp.startHitStop(3 + count / 2);
    }

    private double adaptationMultiplier(HitType type) {
        return 1.0 - (1.0 - ADAPT_MIN_MULTIPLIER) * adaptationLevel(type);
    }

    private void registerAdaptation(HitType type) {
        if (!adapts) {
            return;
        }
        if (adaptType != type) {
            if (adaptType != null && adaptationLevel(adaptType) > 0) {
                Rectangle box = getWorldHitBox();
                gp.effects.text(box.getCenterX(), box.y - 34, "ADAPTATION BROKEN", new Color(0x9AD8F0), 1);
            }
            adaptType = type;
            adaptStreak = 0;
        }
        adaptIdle = 0;
    }

    private void tickCombatModifiers() {
        if (markCooldown > 0) {
            markCooldown--;
        }
        if (marks > 0 && --markTimer <= 0) {
            marks = 0;
            Rectangle box = getWorldHitBox();
            gp.effects.burst(box.getCenterX(), box.y, 6, 1.2, EffectsManager.GOLD);
        }
        if (adapts && adaptType != null) {
            if (++adaptIdle > ADAPT_IDLE_TICKS) {
                adaptStreak = Math.max(0, adaptStreak - 4);
            } else {
                adaptStreak += adaptSpeed;
            }
        }
    }

    public int marks() {
        return marks;
    }

    public boolean markExpiring() {
        return marks > 0 && markTimer < 90;
    }

    public boolean adapts() {
        return adapts;
    }

    // 0..1: how far the current streak of this type is toward adaptation (0 for the other type)
    public double adaptationProgress(HitType type) {
        if (!adapts || adaptType != type) {
            return 0;
        }
        return Math.min(1.0, adaptStreak / ADAPT_START_TICKS);
    }

    // 0..1: how strongly this type is already resisted
    public double adaptationLevel(HitType type) {
        if (!adapts || adaptType != type) {
            return 0;
        }
        return Math.max(0, Math.min(1.0, (adaptStreak - ADAPT_START_TICKS) / (ADAPT_FULL_TICKS - ADAPT_START_TICKS)));
    }

    // Removes this enemy's own projectiles that overlap a melee swing and returns how many were cut.
    public int cutProjectiles(Shape area) {
        return 0;
    }

    public int reflectProjectiles(Shape area) {
        return 0;
    }

    public void handleDeathOnce() {
        if (deathHandled) {
            return;
        }
        deathHandled = true;
        Rectangle box = getWorldHitBox();
        if (experienceValue > 0 && gp.levelManager.isRunActive() && gp.progression.level() < Progression.MAX_LEVEL) {
            int levels = gp.progression.gainExperience(experienceValue);
            gp.effects.text(box.getCenterX(), box.y - 24, "+" + experienceValue + " XP", new Color(0x8FD0C8), 1);
            if (levels > 0) {
                gp.ui.showMessage("Level " + gp.progression.level() + "! +" + levels
                        + (levels == 1 ? " skill point" : " skill points"));
                Rectangle playerBox = gp.player.getWorldHitBox();
                gp.effects.text(playerBox.getCenterX(), playerBox.y - 32, "LEVEL UP!", EffectsManager.GOLD[0], 2);
                gp.effects.ring(playerBox.getCenterX(), playerBox.getCenterY(), 64, 24, EffectsManager.GOLD[1], false);
                gp.playSoundEffect(Sound.SKILL_LEARN);
            }
        }
        gp.effects.burst(box.getCenterX(), box.getCenterY(), 14, 2.2, EffectsManager.SPORES);
        if (mushroomValue > 0 && gp.levelManager.isRunActive()) {
            MushroomPickup.scatter(gp, box.getCenterX(), box.getMaxY(), mushroomValue);
        }
        if (gp.levelManager.isRunActive()) {
            gp.questLog.advance(QuestLog.PESTS, 1);
        }
        onDeath();
    }

    protected void spawnHitSparks(Hit hit) {
        Rectangle box = getWorldHitBox();
        double centerX = box.getCenterX();
        double centerY = box.getCenterY();
        double dirX = centerX - hit.sourceWorldX();
        double dirY = centerY - hit.sourceWorldY();
        double length = Math.max(1e-6, Math.hypot(dirX, dirY));
        double edge = Math.min(box.width, box.height) / 2.0;
        gp.effects.sparks(centerX - dirX / length * edge, centerY - dirY / length * edge, dirX, dirY, 6, EffectsManager.SPARK);
    }

    public boolean isReadyForRemoval() {
        return !alive && deathHandled;
    }

    // Dead enemies may finish visual animations without running movement or attacks.
    public void updateDeathAnimation() {
    }

    protected void onDeath() {
        gp.playSoundEffect(Sound.PLAYER_HAPPY);
    }

    protected boolean isRecoiling() {
        return recoilFramesLeft > 0;
    }

    private void initializeFloatingPosition() {
        if (!floatingPositionInitialized) {
            worldXf = worldX;
            worldYf = worldY;
            floatingPositionInitialized = true;
        }
    }

    private void applySmoothMovement() {
        collisionOn = false;

        int nextWorldX = (int)Math.round(worldXf + velX);
        if (!isEnemyPositionBlocked(nextWorldX, worldY)) {
            worldXf += velX;
            worldX = (int)Math.round(worldXf);
        }
        else {
            collisionOn = true;
            worldXf = worldX;
            velX = 0;
        }

        int nextWorldY = (int)Math.round(worldYf + velY);
        if (!isEnemyPositionBlocked(worldX, nextWorldY)) {
            worldYf += velY;
            worldY = (int)Math.round(worldYf);
        }
        else {
            collisionOn = true;
            worldYf = worldY;
            velY = 0;
        }
    }

    private boolean isEnemyPositionBlocked(int nextWorldX, int nextWorldY) {
        if (gp.cChecker.checkPlayerAt(this, nextWorldX, nextWorldY)) {
            touchedPlayerThisFrame = true;
            return true;
        }

        return gp.cChecker.checkTileAt(this, nextWorldX, nextWorldY) ||
                gp.cChecker.checkObjectAt(this, nextWorldX, nextWorldY);
    }

    private void updateSpriteDirection() {
        if (Math.abs(velX) < 0.05 && Math.abs(velY) < 0.05) {
            direction = "idle";
            return;
        }

        if (Math.abs(velX) > Math.abs(velY)) {
            direction = velX > 0 ? "right" : "left";
        }
        else {
            direction = velY > 0 ? "down" : "up";
        }
    }

    private void updateSpriteAnimation() {
        spriteCounter++;

        if(spriteCounter > 12){
            if(spriteNum == 1){
                spriteNum = 2;
            }
            else if(spriteNum == 2){
                spriteNum = 1;
            }
            spriteCounter = 0;
        }
    }

    @Override
    public void draw(Graphics g2) {
        super.draw(g2);
        drawDamageFlash(g2);
        drawHealthBar(g2);
        drawMarks((Graphics2D) g2);
    }

    protected void drawMarks(Graphics2D g2) {
        if ((!usesMarks && !gp.relics.active(main.Relics.Item.CROWN)) || marks == 0 || !alive) {
            return;
        }
        if (markExpiring() && (markTimer / 6) % 2 == 0) {
            return;
        }
        int s = gp.scale;
        int pip = 3 * s;
        int gap = s;
        int limit = gp.relics.markLimit();
        int width = limit * pip + (limit - 1) * gap;
        Rectangle box = getWorldHitBox();
        int x = (int) Math.round(box.getCenterX()) - gp.player.worldX + gp.player.screenX - width / 2;
        int y = box.y - gp.player.worldY + gp.player.screenY - 8 * s;
        for (int i = 0; i < limit; i++) {
            int px = x + i * (pip + gap);
            g2.setColor(new Color(0x120C09));
            g2.fillRect(px - s, y - s, pip + 2 * s, pip + 2 * s);
            g2.setColor(i < marks ? new Color(0xF2C46A) : new Color(0x3B2D30));
            g2.fillRect(px, y, pip, pip);
            if (i < marks) {
                g2.setColor(new Color(0xFFF6D0));
                g2.fillRect(px, y, s, s);
            }
        }
    }

    // Drawn after the player, for effects that must appear on top of everything in the world.
    public void drawOverlay(Graphics2D g2) {
    }

    public void addLights(world.Lighting lighting) {
    }

    private void drawDamageFlash(Graphics g2) {
        if (damageFlashFramesLeft <= 0 || !alive) {
            return;
        }
        BufferedImage image = currentImage();
        if (image == null) {
            return;
        }

        Graphics2D g2d = (Graphics2D) g2;
        Composite oldComposite = g2d.getComposite();
        float strength = damageFlashFramesLeft > DAMAGE_FLASH_FRAMES / 2 ? 0.9f : 0.45f;
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, strength));
        g2d.drawImage(Silhouette.white(image), getDrawScreenX(), getDrawScreenY(), drawWidth, drawHeight, null);
        g2d.setComposite(oldComposite);
    }

    protected void drawHealthBar(Graphics g2) {
        if (maxHealth <= 0) {
            return;
        }

        int screenX = getDrawScreenX();
        int screenY = getDrawScreenY();
        int barWidth = Math.max(32, drawWidth - 16);
        int barHeight = 4;
        int filledWidth = barWidth * health / maxHealth;
        int barX = screenX + (drawWidth - barWidth) / 2;

        Color oldColor = g2.getColor();

        g2.setColor(Color.RED);
        g2.fillRect(barX, screenY - 8, barWidth, barHeight);

        g2.setColor(Color.GREEN);
        g2.fillRect(barX, screenY - 8, filledWidth, barHeight);

        g2.setColor(oldColor);
    }
}
