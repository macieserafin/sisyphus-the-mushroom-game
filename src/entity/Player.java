package entity;

import main.GamePanel;
import main.KeyHandler;
import main.Skill;
import main.Sound;
import entity.combat.Hit;
import entity.combat.HitResult;
import entity.combat.HitType;
import entity.enemy.Enemy;
import entity.npc.NPC;
import object.SuperObject;
import entity.anim.PlayerAnimator;
import entity.anim.WeaponRenderer;
import world.EffectsManager;
import entity.anim.SpriteSheet;
import entity.weapon.CombatLoadout;
import entity.weapon.MeleeWeapon;
import entity.weapon.RangedWeapon;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class Player extends Entity {
    private static final double BASE_MAX_SPEED = 4.15;
    private static final double SPRINT_SPEED_BONUS = 1.85;
    private static final double ACCELERATION = 0.58;
    private static final double DECELERATION = 0.72;
    private static final double TURN_ACCELERATION = 1.18;
    private static final double STOP_EPSILON = 0.04;
    private static final int HURT_INVINCIBLE_FRAMES = 45;
    private static final int HURT_KNOCKBACK_FRAMES = 10;
    private static final double ENTITY_HURT_KNOCKBACK_FORCE = 12.0;
    private static final double HURT_KNOCKBACK_MAX_SPEED = 12.0;
    private static final double HURT_KNOCKBACK_DAMPING = 0.84;
    private static final double INTERACT_RANGE_TILES = 1.3;
    private static final int AIM_CHORD_TICKS = 2;
    private static final int MUZZLE_FLASH_TICKS = 4;
    private static final int RECOIL_KICK_TICKS = 3;
    private static final int HURT_FLASH_TICKS = 8;
    private static final int DEATH_TICKS = 60;
    private static final int DEATH_FADE_TICKS = 30;
    private static final double SQUASH_DECAY = 0.72;
    private static final int SQUASH_ART_PIXELS = 3;
    private static final double STOP_SQUASH_SPEED = 2.5;
    private static final double TURN_DUST_SPEED = 2.5;
    private static final int TURN_DUST_COOLDOWN_TICKS = 14;
    private static final int SPRINT_DUST_INTERVAL_TICKS = 9;
    private static final double FACING_HYSTERESIS_RADIANS = Math.toRadians(10);
    // The slash trail reaches past the blade tip, matching the melee hitbox reach.
    private static final int SMEAR_REACH_ART_PIXELS = 3;
    private static final double AIM_EASE = 0.45;
    private static final int ROLL_TICKS = 28;
    private static final double ROLL_START_SPEED = 6.4;
    private static final double ROLL_END_SPEED = 2.6;
    private static final int ROLL_COOLDOWN_TICKS = 180;
    private static final int ROLL_DAMAGE = 4;
    private static final double ROLL_KNOCKBACK = 10.0;
    private static final int ROLL_BOUNCE_INVINCIBLE_TICKS = 24;
    private static final int BASE_MAX_SHIELD = 1;
    private static final int BASE_MAX_HEALTH = 2;
    private static final int SHIELD_REGEN_DELAY = 240;
    private static final int SHIELD_REGEN_TICKS = 180;
    private static final Color[] SHIELD_SHARDS = {new Color(0xE8FAFF), new Color(0x9AD8F0), new Color(0x2A6A9A)};
    // the first hit within this window after switching modes is a swap strike (+50%)
    private static final int SWAP_BONUS_TICKS = 30;
    private static final double SWAP_BONUS = 1.5;
    private static final int DASH_TICKS = 9;
    private static final double DASH_SPEED = 13.0;
    private static final double DASH_END_SPEED = 5.0;
    private static final int DASH_COOLDOWN_TICKS = 60;
    private static final int DASH_TRAIL_TICKS = 8;

    KeyHandler keyH;

    public int screenX;
    public int screenY;
    private double coveredByDialogue = 0;
    private static final int CAMERA_SLACK_X = 20;
    private static final int CAMERA_SLACK_Y = 16;
    private static final int CAMERA_JUMP = 400;
    private int followX = Integer.MIN_VALUE;
    private int followY = Integer.MIN_VALUE;

    private double velX = 0.0;
    private double velY = 0.0;
    private double maxSpeed = BASE_MAX_SPEED;
    // Floor-only bonus. Travel preserves it; a new floor resets it and rebirth restores it from the checkpoint.
    private double runDamageMultiplier = 1.0;

    private double worldXf;
    private double worldYf;

    private Direction8 facing = Direction8.S;
    private boolean moving;
    private static final double STEP_SOUND_DISTANCE = 56;
    private double stepDistance;
    private boolean stepFlip;
    private Direction8 aim;
    private Direction8 heldAim;
    private Direction8 lastAim;
    private int aimHeldTicks;
    private int muzzleFlashTicks;
    private int recoilKickTicks;
    private double weaponAngle = Double.NaN;
    private int hurtFlashTicks;
    private int deathTicks;
    private double squash;
    private boolean wasMoving;
    private double lastSpeed;
    private int turnDustCooldown;
    private int sprintDustCounter;

    private int rollTicks;
    private int dashTicks;
    private int rollCooldown;
    private int dashCooldown;
    private double maneuverDirX;
    private double maneuverDirY;
    private boolean spaceHeldLastFrame;
    private final Set<Enemy> rollHitEnemies = new HashSet<>();
    private int swapBonusTicks;
    private CombatState pendingMode;
    private int maxShield = BASE_MAX_SHIELD;
    private int shield = BASE_MAX_SHIELD;
    private int ticksSinceHit;
    private int shieldRegenTicks;
    // dash afterimages: world x, world y, age in ticks
    private final java.util.List<int[]> dashTrail = new java.util.ArrayList<>();

    private int shotCooldownCounter = 0;

    public CombatState combatState = CombatState.RANGE;
    private final CombatLoadout combatLoadout = new CombatLoadout();
    private MeleeWeapon meleeWeapon = combatLoadout.currentMeleeWeapon();
    private RangedWeapon rangedWeapon = combatLoadout.currentRangedWeapon();

    private boolean meleeAttacking = false;
    private int meleeFrame = 0;
    private boolean meleeAttackBuffered = false;
    private int meleeInputBufferCounter = 0;
    private int meleeCooldownCounter = 0;
    private boolean meleeAttackInputHeldLastFrame = false;
    private final Set<Enemy> meleeHitEnemies = new HashSet<>();

    public int hasKey = 0;
    private int hurtInvincibleCounter = 0;
    private int hurtKnockbackCounter = 0;

    private int startingMeleeSlot = 0;
    private int startingRangedSlot = 0;

    private boolean weaponsLockedByArea = false;
    private boolean weaponLockOverride = false;

    private boolean scriptedHold = false;
    private boolean scriptedHidden = false;
    private int scriptedDrawOffsetX = 0;
    private int scriptedDrawOffsetY = 0;

    private final PlayerAnimator animator = new PlayerAnimator();

    public Player(GamePanel gp, KeyHandler keyH) {
        super(gp);
        this.keyH = keyH;

        screenX = gp.screenWidth / 2 - (gp.tileSize / 2);
        screenY = gp.screenHeight / 2 - (gp.tileSize / 2);

        solidArea = new Rectangle(9, 29, 28, 20);
        solidAreaDefaultX = solidArea.x;
        solidAreaDefaultY = solidArea.y;

        hitBox = new Rectangle(7, -2, 33, 52);

        setDefaultValues();
    }

    public void setDefaultValues() {
        setWorldPosition(gp.tileSize * 23, gp.tileSize * 21);

        maxSpeed = BASE_MAX_SPEED + gp.progression.bonusSpeed();
        speed = (int)Math.round(maxSpeed);
        runDamageMultiplier = 1.0;
        maxHealth = BASE_MAX_HEALTH + gp.progression.healthUpgrades();
        maxShield = BASE_MAX_SHIELD + gp.progression.shieldUpgrades();
        health = maxHealth;
        alive = true;
        hasKey = 0;

        direction = "idle";
        resetFacingAndAim();
        combatState = CombatState.RANGE;
        meleeWeapon = combatLoadout.selectMeleeSlot(startingMeleeSlot);
        rangedWeapon = combatLoadout.selectRangedSlot(startingRangedSlot);
        shotCooldownCounter = 0;
        hurtInvincibleCounter = 0;
        hurtKnockbackCounter = 0;
        pendingMode = null;
        rollTicks = 0;
        dashTicks = 0;
        shield = maxShield;
        ticksSinceHit = 0;
        shieldRegenTicks = 0;
        rollCooldown = 0;
        dashCooldown = 0;
        dashTrail.clear();
        meleeAttacking = false;
        meleeFrame = 0;
        meleeAttackBuffered = false;
        meleeInputBufferCounter = 0;
        meleeCooldownCounter = 0;
        meleeAttackInputHeldLastFrame = false;
        resetMeleeHitEnemies();
        hurtFlashTicks = 0;
        deathTicks = 0;
        animator.reset();
        endScriptedHold();
    }


    public void saveCheckpoint(java.util.Properties p) {
        p.setProperty("player.x", "" + worldX); p.setProperty("player.y", "" + worldY);
        p.setProperty("player.health", "" + health); p.setProperty("player.maxHealth", "" + maxHealth);
        p.setProperty("player.shield", "" + shield); p.setProperty("player.maxShield", "" + maxShield);
        p.setProperty("player.speed", "" + maxSpeed); p.setProperty("player.damage", "" + runDamageMultiplier);
        p.setProperty("player.keys", "" + hasKey); p.setProperty("player.mode", combatState.name());
        p.setProperty("player.melee", "" + currentMeleeSlot()); p.setProperty("player.ranged", "" + currentRangedSlot());
        p.setProperty("player.startMelee", "" + startingMeleeSlot); p.setProperty("player.startRanged", "" + startingRangedSlot);
        p.setProperty("player.heavy", "" + ownsMeleeSlot(1)); p.setProperty("player.shotgun", "" + ownsRangedSlot(1));
    }

    public void restoreVitals(int hp, int sp) {
        health = Math.max(1, Math.min(maxHealth, hp)); shield = Math.max(0, Math.min(maxShield, sp));
    }

    public void loadCheckpoint(java.util.Properties p) {
        int spawnX = worldX, spawnY = worldY;
        setDefaultValues(); resetLoadout(); gp.progression.applyWeapons(this);
        setWorldPosition(spawnX,spawnY);
        if (!p.containsKey("player.x")) return;
        maxHealth = Math.max(1, Math.min(99, save.Values.integer(p,"player.maxHealth",maxHealth)));
        maxShield = Math.max(0, Math.min(99, save.Values.integer(p,"player.maxShield",maxShield)));
        restoreVitals(save.Values.integer(p,"player.health",maxHealth),save.Values.integer(p,"player.shield",maxShield));
        maxSpeed = Math.max(1, Math.min(20, save.Values.decimal(p,"player.speed",maxSpeed))); speed=(int)Math.round(maxSpeed);
        runDamageMultiplier=Math.max(1,Math.min(20,save.Values.decimal(p,"player.damage",1)));
        hasKey=Math.max(0,save.Values.integer(p,"player.keys",0));
        setMeleeSlotOwned(1,save.Values.bool(p,"player.heavy")); setRangedSlotOwned(1,save.Values.bool(p,"player.shotgun"));
        startingMeleeSlot=save.Values.integer(p,"player.startMelee",0)==1 && ownsMeleeSlot(1) ? 1 : 0;
        startingRangedSlot=save.Values.integer(p,"player.startRanged",0)==1 && ownsRangedSlot(1) ? 1 : 0;
        int m=save.Values.integer(p,"player.melee",0)==1 && ownsMeleeSlot(1) ? 1 : 0;
        int r=save.Values.integer(p,"player.ranged",0)==1 && ownsRangedSlot(1) ? 1 : 0;
        setMeleeWeapon(combatLoadout.selectMeleeSlot(m)); setRangedWeapon(combatLoadout.selectRangedSlot(r));
        combatState="MELEE".equals(p.getProperty("player.mode")) ? CombatState.MELEE : CombatState.RANGE;
        setWorldPosition(save.Values.integer(p,"player.x",worldX),save.Values.integer(p,"player.y",worldY));
    }

    public void updateCamera() {
        coveredByDialogue += (gp.ui.dialogueOverlayHeight() - coveredByDialogue) * 0.15;
        int visibleHeight = gp.screenHeight - (int) Math.round(coveredByDialogue);

        int centerX = gp.screenWidth / 2 - gp.tileSize / 2;
        int centerY = visibleHeight / 2 - gp.tileSize / 2;
        int wantedX = worldX - centerX;
        int wantedY = worldY - centerY;
        if (followX == Integer.MIN_VALUE || gp.visuals.reduceMotion()
                || Math.abs(wantedX - followX) > CAMERA_JUMP || Math.abs(wantedY - followY) > CAMERA_JUMP) {
            followX = wantedX;
            followY = wantedY;
        } else {
            followX = follow(followX, wantedX, CAMERA_SLACK_X);
            followY = follow(followY, wantedY, CAMERA_SLACK_Y);
        }
        int cameraX = cameraOrigin(followX, gp.tileM.getWorldCols() * gp.tileSize, gp.screenWidth);
        int cameraY = cameraOrigin(followY, gp.tileM.getWorldRows() * gp.tileSize, visibleHeight);
        screenX = worldX - cameraX;
        screenY = worldY - cameraY;
    }

    private static int follow(int current, int wanted, int slack) {
        if (wanted - current > slack) {
            return wanted - slack;
        }
        if (current - wanted > slack) {
            return wanted + slack;
        }
        return current + Integer.signum(wanted - current);
    }

    private static int cameraOrigin(int wanted, int mapSize, int screenSize) {
        if (mapSize <= 0) {
            return wanted;
        }
        if (mapSize <= screenSize) {
            return -(screenSize - mapSize) / 2;
        }
        return Math.max(0, Math.min(wanted, mapSize - screenSize));
    }

    public String cycleStartingMeleeWeapon() {
        do {
            startingMeleeSlot = (startingMeleeSlot + 1) % combatLoadout.meleeWeaponCount();
        } while (!combatLoadout.ownsMelee(startingMeleeSlot));
        setMeleeWeapon(combatLoadout.selectMeleeSlot(startingMeleeSlot));
        return combatLoadout.meleeWeaponName(startingMeleeSlot);
    }

    public String cycleStartingRangedWeapon() {
        do {
            startingRangedSlot = (startingRangedSlot + 1) % combatLoadout.rangedWeaponCount();
        } while (!combatLoadout.ownsRanged(startingRangedSlot));
        setRangedWeapon(combatLoadout.selectRangedSlot(startingRangedSlot));
        return combatLoadout.rangedWeaponName(startingRangedSlot);
    }

    public void beginScriptedHold() {
        scriptedHold = true;
        velX = 0;
        velY = 0;
        hurtKnockbackCounter = 0;
        meleeAttacking = false;
        meleeFrame = 0;
        meleeAttackBuffered = false;
        meleeInputBufferCounter = 0;
        direction = "idle";
        resetFacingAndAim();
    }

    private void resetFacingAndAim() {
        facing = Direction8.S;
        moving = false;
        aim = null;
        heldAim = null;
        lastAim = null;
        aimHeldTicks = 0;
        muzzleFlashTicks = 0;
        recoilKickTicks = 0;
        squash = 0;
        wasMoving = false;
        lastSpeed = 0;
        turnDustCooldown = 0;
        sprintDustCounter = 0;
    }

    public void endScriptedHold() {
        scriptedHold = false;
        scriptedHidden = false;
        scriptedDrawOffsetX = 0;
        scriptedDrawOffsetY = 0;
    }

    public boolean isScriptedHold() {
        return scriptedHold;
    }

    public void setScriptedHidden(boolean scriptedHidden) {
        this.scriptedHidden = scriptedHidden;
    }

    public void setScriptedDrawOffset(int offsetX, int offsetY) {
        scriptedDrawOffsetX = offsetX;
        scriptedDrawOffsetY = offsetY;
    }

    public void setWorldPosition(int worldX, int worldY) {
        this.worldX = worldX;
        this.worldY = worldY;
        worldXf = worldX;
        worldYf = worldY;
        velX = 0;
        velY = 0;
        collisionOn = false;
    }

    public void update() {
        if (!alive) {
            updateDeath();
            return;
        }

        if (scriptedHold) {
            velX = 0;
            velY = 0;
            moving = false;
            updateAnimation();
            return;
        }

        tickTimers();

        boolean hurtKnockbackActive = hurtKnockbackCounter > 0;
        if (hurtKnockbackActive) {
            hurtKnockbackCounter--;
        }

        double[] moveInput = getMoveInputVector();
        if (!gp.prologueTutorialManager.canMove()) {
            moveInput[0] = 0;
            moveInput[1] = 0;
        }
        boolean movingInput = vectorLength(moveInput[0], moveInput[1]) > 0;

        updateManeuverInput(moveInput, hurtKnockbackActive);
        if (rollTicks > 0 || dashTicks > 0) {
            updateManeuver();
            updateFacingFromVelocity();
            resolveInteractionsAfterMovement();
            updateAnimation();
            return;
        }

        updateModeKeys();
        updateWeaponSwitchInput();
        updateAimInput();

        boolean attackInput = hasAllowedAttackInput();
        updateCombat(attackInput);

        double currentMaxSpeed = getCurrentMovementMaxSpeed(attackInput, hurtKnockbackActive);
        applyMovementInput(moveInput[0], moveInput[1], currentMaxSpeed, hurtKnockbackActive);

        if (meleeAttacking) {
            updateMeleeAttack();
            applyMeleeDamping();
        } else if (hurtKnockbackActive) {
            velX *= HURT_KNOCKBACK_DAMPING;
            velY *= HURT_KNOCKBACK_DAMPING;
        }

        clampVelocity(getCurrentVelocityCap(currentMaxSpeed, attackInput, hurtKnockbackActive));
        updateFacingFromVelocity();
        updateMovementFeedback(moveInput[0], moveInput[1]);
        resolveInteractionsAfterMovement();
        updateAnimation();
    }

    private void updateModeKeys() {
        boolean q = keyH.qPressed;
        boolean e = keyH.ePressed;
        keyH.qPressed = false;
        keyH.ePressed = false;
        if ((q || e) && gp.prologueTutorialManager.canSwitchCombatMode()) {
            CombatState base = pendingMode != null ? pendingMode : combatState;
            if (gp.controls.separateModeKeys()) {
                pendingMode = q ? CombatState.MELEE : CombatState.RANGE;
            } else if (q) {
                pendingMode = base.next();
            }
        }
        // a switch pressed during a sword swing waits until the swing is over
        if (pendingMode == null || meleeAttacking) {
            return;
        }
        CombatState target = pendingMode;
        pendingMode = null;
        if (target == combatState) {
            return;
        }
        combatState = target;
        gp.playSoundEffect(Sound.SWAP);
        swapBonusTicks = SWAP_BONUS_TICKS;
        gp.effects.ring(worldX + gp.tileSize / 2.0, worldY + gp.tileSize / 2.0, 30, 10,
                combatState == CombatState.MELEE ? new Color(0xF2C46A) : new Color(0x9AD8F0), false);
        meleeAttackInputHeldLastFrame = false;
        meleeAttackBuffered = false;
        meleeInputBufferCounter = 0;
    }

    private void updateManeuverInput(double[] moveInput, boolean hurtKnockbackActive) {
        boolean spaceJustPressed = keyH.spacePressed && !spaceHeldLastFrame;
        spaceHeldLastFrame = keyH.spacePressed;
        if (!spaceJustPressed || rollTicks > 0 || dashTicks > 0 || hurtKnockbackActive || meleeAttacking
                || !gp.prologueTutorialManager.canMove()) {
            return;
        }

        double dirX = moveInput[0];
        double dirY = moveInput[1];
        if (dirX == 0 && dirY == 0) {
            dirX = facing.unitX;
            dirY = facing.unitY;
        }
        if (combatState == CombatState.MELEE && rollCooldown == 0 && gp.progression.hasSkill(Skill.ROLL)) {
            startManeuver(dirX, dirY);
            rollHitEnemies.clear();
            rollTicks = ROLL_TICKS;
            rollCooldown = ROLL_COOLDOWN_TICKS;
            squash = 0.6;
            gp.playSoundEffect(Sound.ROLL);
            spawnFootDust(-dirX, -dirY, 5);
        } else if (combatState == CombatState.RANGE && dashCooldown == 0 && gp.progression.hasSkill(Skill.DASH)) {
            startManeuver(dirX, dirY);
            dashTicks = DASH_TICKS;
            dashCooldown = DASH_COOLDOWN_TICKS;
            gp.playSoundEffect(Sound.DASH);
            spawnFootDust(-dirX, -dirY, 7);
        }
    }

    private void startManeuver(double dirX, double dirY) {
        maneuverDirX = dirX;
        maneuverDirY = dirY;
        aim = null;
        meleeAttackBuffered = false;
        meleeInputBufferCounter = 0;
        facing = Direction8.fromVector(dirX, dirY, facing, 0);
    }

    private void updateManeuver() {
        if (dashTicks > 0 && hurtKnockbackCounter > 0) {
            // a hit during a dash ends it; the knockback takes over
            dashTicks = 0;
            return;
        }
        if (rollTicks > 0) {
            double t = 1.0 - rollTicks / (double) ROLL_TICKS;
            double speed = ROLL_START_SPEED + (ROLL_END_SPEED - ROLL_START_SPEED) * t;
            velX = maneuverDirX * speed;
            velY = maneuverDirY * speed;
            if (rollIntoEnemies()) {
                return;
            }
            rollTicks--;
            if (rollTicks == 0) {
                squash = 0.5;
                spawnFootDust(0, 0, 3);
            }
        } else {
            double speed = dashTicks > 2 ? DASH_SPEED : DASH_END_SPEED;
            velX = maneuverDirX * speed;
            velY = maneuverDirY * speed;
            dashTrail.add(new int[]{worldX, worldY, 0});
            dashTicks--;
        }
    }

    // Returns true when a boss bounced the player and the roll ended.
    private boolean rollIntoEnemies() {
        Rectangle ahead = getWorldSolidArea();
        ahead.translate((int) Math.round(velX * 1.5), (int) Math.round(velY * 1.5));
        ahead.grow(2, 2);
        double playerCenterX = ahead.getCenterX();
        double playerCenterY = ahead.getCenterY();

        for (Enemy enemy : gp.entities.enemies()) {
            if (!enemy.alive || rollHitEnemies.contains(enemy)) {
                continue;
            }
            Rectangle body = enemy.getWorldSolidArea();
            if (!ahead.intersects(body)) {
                continue;
            }
            if (enemy == gp.levelManager.getActiveBoss()) {
                bounceOffBoss(body);
                return true;
            }

            rollHitEnemies.add(enemy);
            Hit hit = new Hit(ROLL_DAMAGE, HitType.MELEE, this, (int) playerCenterX, (int) playerCenterY, 0, null);
            if (enemy.receiveHit(hit) == HitResult.IGNORED || !enemy.alive) {
                continue;
            }
            // throw the enemy out of the roll's path: sideways, a little forward
            double toX = body.getCenterX() - playerCenterX;
            double toY = body.getCenterY() - playerCenterY;
            double side = Math.signum(maneuverDirX * toY - maneuverDirY * toX);
            if (side == 0) {
                side = 1;
            }
            double pushX = -maneuverDirY * side + maneuverDirX * 0.35;
            double pushY = maneuverDirX * side + maneuverDirY * 0.35;
            double length = vectorLength(pushX, pushY);
            pushX /= length;
            pushY /= length;
            enemy.applyKnockbackFrom(
                    (int) Math.round(enemy.worldX + gp.tileSize / 2.0 - pushX * 20),
                    (int) Math.round(enemy.worldY + gp.tileSize / 2.0 - pushY * 20),
                    ROLL_KNOCKBACK
            );
            gp.startHitStop(2);
        }
        return false;
    }

    private void bounceOffBoss(Rectangle bossBody) {
        rollTicks = 0;
        applyKnockbackFrom((int) bossBody.getCenterX(), (int) bossBody.getCenterY(), ENTITY_HURT_KNOCKBACK_FORCE);
        hurtInvincibleCounter = Math.max(hurtInvincibleCounter, ROLL_BOUNCE_INVINCIBLE_TICKS);
        squash = 0.8;
        spawnFootDust(-maneuverDirX, -maneuverDirY, 6);
    }

    public int shield() {
        return shield;
    }

    public int maxShield() {
        return maxShield;
    }

    // 0..1 progress of the next shield point (0 while still waiting after a hit)
    public double shieldRegenProgress() {
        if (shield >= maxShield || ticksSinceHit <= SHIELD_REGEN_DELAY) {
            return 0;
        }
        return shieldRegenTicks / (double) SHIELD_REGEN_TICKS;
    }

    // Returns false when already at full health (the pickup then stays on the ground).
    public boolean heal(int amount) {
        if (!alive || health >= maxHealth) {
            return false;
        }
        health = Math.min(maxHealth, health + amount);
        return true;
    }

    public void addMaxHealth(int amount) {
        maxHealth += amount;
        health = Math.min(maxHealth, health + amount);
    }

    public void addMaxShield(int amount) {
        maxShield += amount;
        shield = Math.min(maxShield, shield + amount);
    }

    public double runDamageMultiplier() {
        return runDamageMultiplier;
    }

    public void addRunDamage(double fraction) {
        runDamageMultiplier += fraction;
    }

    // Damage multiplier for a hit the player just landed: the first one right after a mode switch is a swap strike.
    public double consumeSwapBonus() {
        if (swapBonusTicks <= 0) {
            return 1.0;
        }
        swapBonusTicks = 0;
        return SWAP_BONUS;
    }

    public void drawProjectileCore(Graphics2D g2) {
        if (!alive || scriptedHidden || gp.enemyShots.count() == 0) {
            return;
        }
        Rectangle core = getProjectileHitBox();
        int s = gp.scale;
        int cx = (int) Math.round(core.getCenterX()) - worldX + screenX;
        int cy = (int) Math.round(core.getCenterY()) - worldY + screenY;
        boolean pulse = (gp.enemyShots.tick() / 6) % 2 == 0;
        g2.setColor(new Color(0x120C09));
        g2.fillRect(cx - 2 * s, cy - 2 * s, 4 * s, 4 * s);
        g2.setColor(isHurtInvincible() ? new Color(0x9AD8F0) : pulse ? new Color(0xFFF6D0) : new Color(0xE05A48));
        g2.fillRect(cx - s, cy - s, 2 * s, 2 * s);
    }

    // Enemy projectiles only hit this small core, so dense patterns can be threaded (bullet-hell style).
    public Rectangle getProjectileHitBox() {
        return new Rectangle(worldX + 16, worldY + 14, 16, 20);
    }

    public boolean isRolling() {
        return rollTicks > 0;
    }

    public boolean isDashing() {
        return dashTicks > 0;
    }

    // 0 = just used, 1 = ready
    public double rollReadiness() {
        return 1.0 - rollCooldown / (double) ROLL_COOLDOWN_TICKS;
    }

    public double dashReadiness() {
        return 1.0 - dashCooldown / (double) DASH_COOLDOWN_TICKS;
    }

    private void tickTimers() {
        if (shotCooldownCounter > 0) {
            shotCooldownCounter--;
        }
        if (rollCooldown > 0) {
            rollCooldown--;
        }
        if (swapBonusTicks > 0) {
            swapBonusTicks--;
        }
        ticksSinceHit++;
        if (shield < maxShield && ticksSinceHit > SHIELD_REGEN_DELAY && ++shieldRegenTicks >= SHIELD_REGEN_TICKS) {
            shield++;
            shieldRegenTicks = 0;
            double cx = worldX + gp.tileSize / 2.0, cy = worldY + gp.tileSize / 2.0;
            gp.effects.ring(cx, cy, 40, 14, new Color(0x9AD8F0), false);
            gp.effects.text(cx, worldY - 10, "SHIELD", new Color(0x9AD8F0), 1);
        }
        if (dashCooldown > 0) {
            dashCooldown--;
        }
        for (int i = dashTrail.size() - 1; i >= 0; i--) {
            if (++dashTrail.get(i)[2] > DASH_TRAIL_TICKS) {
                dashTrail.remove(i);
            }
        }
        if (hurtInvincibleCounter > 0) {
            hurtInvincibleCounter--;
        }
        if (meleeCooldownCounter > 0) {
            meleeCooldownCounter--;
        }
        if (muzzleFlashTicks > 0) {
            muzzleFlashTicks--;
        }
        if (recoilKickTicks > 0) {
            recoilKickTicks--;
        }
        if (hurtFlashTicks > 0) {
            hurtFlashTicks--;
        }
        if (turnDustCooldown > 0) {
            turnDustCooldown--;
        }
        squash *= SQUASH_DECAY;
        if (Math.abs(squash) < 0.05) {
            squash = 0;
        }
    }

    private void updateCombat(boolean attackInput) {
        if (combatState == CombatState.RANGE) {
            updateRangedCombat(attackInput);
        } else if (combatState == CombatState.MELEE) {
            updateMeleeInputBuffer(attackInput);
        }
    }

    private void updateWeaponSwitchInput() {
        if (!gp.prologueTutorialManager.canSwitchWeaponSlot()) {
            keyH.onePressed = false;
            keyH.twoPressed = false;
            return;
        }

        if (keyH.onePressed) {
            selectWeaponSlot(0);
            keyH.onePressed = false;
        }
        if (keyH.twoPressed) {
            selectWeaponSlot(1);
            keyH.twoPressed = false;
        }
    }

    private void selectWeaponSlot(int slot) {
        boolean owned = combatState == CombatState.MELEE ? combatLoadout.ownsMelee(slot) : combatLoadout.ownsRanged(slot);
        if (!owned) {
            return;
        }
        if (combatState == CombatState.MELEE) {
            if (meleeAttacking) {
                return;
            }
            setMeleeWeapon(combatLoadout.selectMeleeSlot(slot));
            return;
        }

        setRangedWeapon(combatLoadout.selectRangedSlot(slot));
    }

    private void updateRangedCombat(boolean attackInput) {
        meleeAttackInputHeldLastFrame = false;

        if (!attackInput || heldAim == null) {
            aim = null;
            return;
        }

        aim = heldAim;
        if (!isAimSettled()) {
            return;
        }

        if (shotCooldownCounter == 0) {
            fireBullet(aim);
            shotCooldownCounter = rangedWeapon.shotCooldownFrames();
        }
    }

    private void updateMeleeInputBuffer(boolean attackInput) {
        boolean attackJustPressed = attackInput && !meleeAttackInputHeldLastFrame;
        meleeAttackInputHeldLastFrame = attackInput;

        if (attackJustPressed && heldAim != null) {
            meleeAttackBuffered = true;
            meleeInputBufferCounter = meleeWeapon.inputBufferFrames();
        } else if (meleeInputBufferCounter > 0) {
            meleeInputBufferCounter--;
        }

        if (!meleeAttacking &&
                meleeCooldownCounter == 0 &&
                meleeInputBufferCounter > 0 &&
                meleeAttackBuffered &&
                isAimSettled()) {
            aim = heldAim != null ? heldAim : lastAim;
            meleeAttackBuffered = false;
            meleeInputBufferCounter = 0;
            startMeleeAttack();
        } else if (!meleeAttacking) {
            aim = null;
        }
    }

    private void updateAimInput() {
        int x = (keyH.rightShootPressed ? 1 : 0) - (keyH.leftShootPressed ? 1 : 0);
        int y = (keyH.downShootPressed ? 1 : 0) - (keyH.upShootPressed ? 1 : 0);
        heldAim = Direction8.fromVector(x, y);
        if (heldAim != null) {
            aimHeldTicks++;
            lastAim = heldAim;
        } else {
            aimHeldTicks = 0;
        }
    }

    private boolean isAimSettled() {
        return heldAim == null || heldAim.isDiagonal() || aimHeldTicks > AIM_CHORD_TICKS;
    }

    private double[] getMoveInputVector() {
        double inputX = 0;
        double inputY = 0;

        if (keyH.leftPressed && !keyH.rightPressed) {
            inputX = -1;
        } else if (keyH.rightPressed && !keyH.leftPressed) {
            inputX = 1;
        }

        if (keyH.upPressed && !keyH.downPressed) {
            inputY = -1;
        } else if (keyH.downPressed && !keyH.upPressed) {
            inputY = 1;
        }

        double length = vectorLength(inputX, inputY);
        if (length > 0) {
            inputX /= length;
            inputY /= length;
        }

        return new double[]{inputX, inputY};
    }

    private double getCurrentMovementMaxSpeed(boolean attackInput, boolean hurtKnockbackActive) {
        if (hurtKnockbackActive) {
            return HURT_KNOCKBACK_MAX_SPEED;
        }

        double currentMaxSpeed = maxSpeed;
        if (keyH.shiftPressed) {
            currentMaxSpeed += SPRINT_SPEED_BONUS;
        }
        if (combatState == CombatState.RANGE && attackInput) {
            currentMaxSpeed *= rangedWeapon.movementMultiplier();
        }

        return currentMaxSpeed;
    }

    private void applyMovementInput(double inputX, double inputY, double currentMaxSpeed, boolean hurtKnockbackActive) {
        if (hurtKnockbackActive) {
            return;
        }

        double targetX = inputX * currentMaxSpeed;
        double targetY = inputY * currentMaxSpeed;
        boolean hasInput = vectorLength(inputX, inputY) > 0;

        double maxDelta;
        if (!hasInput) {
            maxDelta = DECELERATION;
        } else {
            double dot = velX * targetX + velY * targetY;
            maxDelta = dot < 0 ? TURN_ACCELERATION : ACCELERATION;
            if (meleeAttacking) {
                maxDelta *= meleeWeapon.controlAccelMultiplier();
            }
        }

        moveVelocityToward(targetX, targetY, maxDelta);
    }

    private void moveVelocityToward(double targetX, double targetY, double maxDelta) {
        double deltaX = targetX - velX;
        double deltaY = targetY - velY;
        double distance = vectorLength(deltaX, deltaY);

        if (distance <= maxDelta || distance == 0) {
            velX = targetX;
            velY = targetY;
            return;
        }

        velX += (deltaX / distance) * maxDelta;
        velY += (deltaY / distance) * maxDelta;
    }

    private void clampVelocity(double maxLength) {
        double length = vectorLength(velX, velY);
        if (length > maxLength) {
            velX = (velX / length) * maxLength;
            velY = (velY / length) * maxLength;
        }

        if (Math.abs(velX) < STOP_EPSILON) {
            velX = 0;
        }
        if (Math.abs(velY) < STOP_EPSILON) {
            velY = 0;
        }
    }

    private double getCurrentVelocityCap(double currentMaxSpeed, boolean attackInput, boolean hurtKnockbackActive) {
        if (hurtKnockbackActive) {
            return HURT_KNOCKBACK_MAX_SPEED;
        }

        double cap = currentMaxSpeed;
        if (meleeAttacking) {
            cap = Math.max(cap, getMeleeMaxSpeed());
        }
        if (combatState == CombatState.RANGE && attackInput) {
            cap = Math.max(cap, rangedWeapon.recoilMaxSpeed());
        }

        return cap;
    }

    private void updateMovementFeedback(double inputX, double inputY) {
        double speed = vectorLength(velX, velY);
        boolean hasInput = inputX != 0 || inputY != 0;

        if (hasInput && rollTicks == 0 && dashTicks == 0) {
            stepDistance += speed;
            if (stepDistance >= STEP_SOUND_DISTANCE) {
                stepDistance = 0;
                stepFlip = !stepFlip;
                gp.playSoundEffect(stepFlip ? Sound.STEP_1 : Sound.STEP_2);
            }
        } else if (!hasInput) {
            stepDistance = STEP_SOUND_DISTANCE * 0.6;
        }

        if (moving && !wasMoving && hasInput) {
            squash = -0.7;
            spawnFootDust(-inputX, -inputY, 4);
        } else if (!moving && wasMoving && lastSpeed > STOP_SQUASH_SPEED) {
            squash = 0.8;
            spawnFootDust(0, 0, 4);
        }

        if (hasInput && speed > TURN_DUST_SPEED && turnDustCooldown == 0 && velX * inputX + velY * inputY < 0) {
            spawnFootDust(velX / speed, velY / speed, 7);
            squash = 0.5;
            turnDustCooldown = TURN_DUST_COOLDOWN_TICKS;
        }

        if (moving && hasInput && keyH.shiftPressed) {
            sprintDustCounter++;
            if (sprintDustCounter >= SPRINT_DUST_INTERVAL_TICKS) {
                sprintDustCounter = 0;
                spawnFootDust(-velX / Math.max(speed, 1e-6), -velY / Math.max(speed, 1e-6), 3);
            }
        } else {
            sprintDustCounter = 0;
        }

        wasMoving = moving;
        lastSpeed = speed;
    }

    private void spawnFootDust(double dirX, double dirY, int count) {
        gp.effects.dust(worldX + gp.tileSize / 2.0, worldY + gp.tileSize - 6, dirX, dirY, count);
    }

    private void updateDeath() {
        if (deathTicks <= 0) {
            return;
        }
        if (hurtFlashTicks > 0) {
            hurtFlashTicks--;
        }
        deathTicks--;
        animator.updateDeath();
        if (deathTicks % 12 == 0 && deathTicks > DEATH_FADE_TICKS / 2) {
            gp.effects.burst(worldX + gp.tileSize / 2.0, worldY + gp.tileSize / 2.0, 4, 1.2, EffectsManager.SPORES);
        }
        if (deathTicks == 0) {
            gp.onPlayerDied();
        }
    }

    private void updateFacingFromVelocity() {
        moving = Math.abs(velX) >= STOP_EPSILON || Math.abs(velY) >= STOP_EPSILON;
        if (moving) {
            facing = Direction8.fromVector(velX, velY, facing, FACING_HYSTERESIS_RADIANS);
        }
    }

    private void resolveInteractionsAfterMovement() {
        int nextWorldX = (int)Math.round(worldXf + velX);
        int nextWorldY = (int)Math.round(worldYf + velY);

        int objIndex = gp.cChecker.checkObjectAtIndex(this, nextWorldX, nextWorldY, false);
        int npcIndex = gp.cChecker.checkEntityAtIndex(this, gp.entities.npcs(), nextWorldX, nextWorldY);

        applySmoothCollisionMovement();

        if (objIndex == 999) {
            objIndex = gp.cChecker.checkObjectTouch(this);
        }
        pickUpObject(objIndex);

        if (npcIndex == 999) {
            npcIndex = gp.cChecker.checkEntityTouch(this, gp.entities.npcs());
        }
        interactNPC(npcIndex);
    }

    private void updateAnimation() {
        animator.hurt(!scriptedHold && (hurtFlashTicks > 0 || hurtKnockbackCounter > 0));
        animator.locomote(moving, keyH.shiftPressed, vectorLength(velX, velY) / gp.scale, facing);
        Direction8 maneuverDirection = Direction8.fromVector(maneuverDirX, maneuverDirY, facing, 0);
        if (rollTicks > 0) {
            SpriteSheet sheet = PlayerAnimator.rollSheet();
            int elapsed = ROLL_TICKS - rollTicks;
            int last = sheet == null ? 0 : sheet.frameCount - 1;
            int poseFrame = elapsed < 4 ? 0 : rollTicks <= 4 ? last : 1 + ((elapsed - 4) / 4) % Math.max(1, last - 1);
            animator.holdPose(PlayerAnimator.State.ROLL, sheet, poseFrame, maneuverDirection);
        } else if (dashTicks > 0) {
            SpriteSheet sheet = PlayerAnimator.dashSheet();
            int poseFrame = sheet == null ? 0 : ((DASH_TICKS - dashTicks) / 3) % sheet.frameCount;
            animator.holdPose(PlayerAnimator.State.DASH, sheet, poseFrame, maneuverDirection);
        } else if (aim == null) {
            animator.clearPose();
        } else if (combatState == CombatState.RANGE) {
            SpriteSheet sheet = rangedWeapon.animations().shoot();
            animator.holdPose(PlayerAnimator.State.SHOOT, sheet, PlayerAnimator.shootFrame(sheet, recoilKickTicks > 0), aim);
        } else if (combatState == CombatState.MELEE && meleeAttacking) {
            SpriteSheet sheet = meleeWeapon.animations().attack();
            int poseFrame = sheet == null ? 0 : PlayerAnimator.attackFrame(meleeWeapon, meleeFrame, sheet.frameCount);
            animator.holdPose(PlayerAnimator.State.ATTACK, sheet, poseFrame, aim);
        } else {
            animator.clearPose();
        }
        updateWeaponAngle();
    }

    private void updateWeaponAngle() {
        if (aim == null || combatState != CombatState.RANGE) {
            weaponAngle = Double.NaN;
            return;
        }
        double target = aim.angle();
        if (Double.isNaN(weaponAngle) || !gp.debugSettings.isAimSmoothing()) {
            weaponAngle = target;
            return;
        }
        double diff = Math.atan2(Math.sin(target - weaponAngle), Math.cos(target - weaponAngle));
        weaponAngle = Math.abs(diff) < 0.02 ? target : weaponAngle + diff * AIM_EASE;
    }

    private double rangedAngle() {
        return Double.isNaN(weaponAngle) ? aim.angle() : weaponAngle;
    }

    private double vectorLength(double x, double y) {
        return Math.sqrt(x * x + y * y);
    }

    private void fireBullet(Direction8 shootDirection) {
        muzzleFlashTicks = MUZZLE_FLASH_TICKS;
        recoilKickTicks = RECOIL_KICK_TICKS;
        gp.lighting.pulse(worldX + gp.tileSize / 2.0, worldY + gp.tileSize / 2.0, 120, world.Palette.FIRE, 0.9f, 5);
        addMovementImpulse(rangedWeapon.fire(
                gp.tileSize,
                gp.bulletManager,
                () -> gp.playSoundEffect(Sound.GUNSHOT),
                this,
                worldX,
                worldY,
                shootDirection
        ));
    }

    private void addMovementImpulse(MovementImpulse impulse) {
        velX += impulse.x;
        velY += impulse.y;
        clampVelocity(impulse.maxSpeed);
    }

    private void applySmoothCollisionMovement() {
        collisionOn = false;

        int nextWorldX = (int)Math.round(worldXf + velX);
        if (!gp.cChecker.isPlayerPositionBlocked(this, nextWorldX, worldY)) {
            worldXf += velX;
            worldX = (int)Math.round(worldXf);
        }
        else {
            collisionOn = true;
            worldXf = worldX;
            velX = 0;
        }

        int nextWorldY = (int)Math.round(worldYf + velY);
        if (!gp.cChecker.isPlayerPositionBlocked(this, worldX, nextWorldY)) {
            worldYf += velY;
            worldY = (int)Math.round(worldYf);
        }
        else {
            collisionOn = true;
            worldYf = worldY;
            velY = 0;
        }
    }

    public void interactNPC(int i){
        if (gp.keyH.enterPressed) {
            if (i != 999) {
                gp.entities.npcs().get(i).speak();
            }
            else {
                interactNearby();
            }
        }
        gp.keyH.enterPressed = false;
    }

    private void interactNearby() {
        double playerCenterX = worldX + gp.tileSize / 2.0;
        double playerCenterY = worldY + gp.tileSize / 2.0;

        NPC closestNpc = null;
        double closestNpcDistance = Double.MAX_VALUE;
        for (NPC npc : gp.entities.npcs()) {
            double distance = Math.hypot(npc.worldX + gp.tileSize / 2.0 - playerCenterX, npc.worldY + gp.tileSize / 2.0 - playerCenterY);
            if (distance <= npc.talkRangeTiles * gp.tileSize && distance < closestNpcDistance) {
                closestNpc = npc;
                closestNpcDistance = distance;
            }
        }
        if (closestNpc != null) {
            closestNpc.speak();
            return;
        }

        SuperObject closestObject = null;
        double closestObjectDistance = INTERACT_RANGE_TILES * gp.tileSize;
        for (SuperObject object : gp.entities.objects()) {
            if (!object.isInteractable()) continue;
            double distance = Math.hypot(object.worldX + gp.tileSize / 2.0 - playerCenterX, object.worldY + gp.tileSize / 2.0 - playerCenterY);
            if (distance <= closestObjectDistance) {
                closestObject = object;
                closestObjectDistance = distance;
            }
        }
        if (closestObject != null) {
            closestObject.onPlayerInteract(gp, this);
        }
    }

    public void pickUpObject(int i) {
        if(i != 999){
            gp.entities.objects().get(i).onPlayerTouch(gp, this, i);
        }
    }

    public void addMaxSpeed(double amount) {
        maxSpeed += amount;
        speed = (int)Math.round(maxSpeed);
    }

    public void draw(Graphics2D g2){
        if (scriptedHidden || (!alive && deathTicks <= 0)) {
            return;
        }

        SpriteSheet.Frame frame = animator.frame();
        int footX = screenX + gp.tileSize / 2 + scriptedDrawOffsetX;
        int footY = screenY + gp.tileSize + scriptedDrawOffsetY;

        if (alive && !dashTrail.isEmpty()) {
            Composite before = g2.getComposite();
            for (int[] ghost : dashTrail) {
                float ghostAlpha = 0.4f * (1f - ghost[2] / (float) (DASH_TRAIL_TICKS + 1));
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, ghostAlpha));
                frame.draw(g2, footX + ghost[0] - worldX, footY + ghost[1] - worldY, gp.scale, 0, 0);
            }
            g2.setComposite(before);
        }

        int extraWidth = (int) Math.round(squash * SQUASH_ART_PIXELS);
        int extraHeight = -extraWidth;
        float alpha = 1f;
        if (!alive) {
            if (!animator.hasDeathAnimation()) {
                double fallen = 1 - deathTicks / (double) DEATH_TICKS;
                extraWidth += (int) Math.round(4 * fallen);
                extraHeight -= (int) Math.round(10 * fallen);
            }
            alpha = Math.min(1f, deathTicks / (float) DEATH_FADE_TICKS);
        } else if (hurtFlashTicks == 0 && hurtInvincibleCounter > 0 && (hurtInvincibleCounter / 5) % 2 == 0) {
            alpha = 0.6f;
        }

        Composite oldComposite = g2.getComposite();
        if (alpha < 1f) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        }
        boolean weaponVisible = alive && isWeaponDrawn(frame);
        boolean weaponBehind = weaponVisible && aim.dy < 0;
        if (weaponBehind) {
            drawWeapon(g2, frame, footX, footY, extraWidth, extraHeight);
        }
        frame.draw(g2, footX, footY, gp.scale, extraWidth, extraHeight);
        if (hurtFlashTicks > 0) {
            float flash = hurtFlashTicks > HURT_FLASH_TICKS / 2 ? 1f : 0.55f;
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, flash * alpha));
            frame.drawWhite(g2, footX, footY, gp.scale, extraWidth, extraHeight);
            g2.setComposite(alpha < 1f ? AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha) : oldComposite);
        }
        if (weaponVisible && !weaponBehind) {
            drawWeapon(g2, frame, footX, footY, extraWidth, extraHeight);
        }
        g2.setComposite(oldComposite);

        if (gp.debugSettings.isOverlayEnabled()) {
            g2.setColor(Color.MAGENTA);
            Rectangle core = getProjectileHitBox();
            g2.drawRect(core.x - worldX + screenX, core.y - worldY + screenY, core.width, core.height);
            g2.setColor(Color.GREEN);
            g2.drawRect(screenX + solidArea.x, screenY + solidArea.y, solidArea.width, solidArea.height);

            g2.setColor(Color.RED);
            g2.drawRect(screenX + hitBox.x, screenY + hitBox.y, hitBox.width, hitBox.height);

            drawMeleeHitBox(g2);
            drawAnimationDebug(g2, frame, footX, footY, extraWidth, extraHeight);
        }
    }

    private void drawAnimationDebug(Graphics2D g2, SpriteSheet.Frame frame, int footX, int footY, int extraWidth, int extraHeight) {
        Color oldColor = g2.getColor();
        Font oldFont = g2.getFont();
        int scale = gp.scale;

        g2.setColor(Color.CYAN);
        g2.drawLine(footX - 2 * scale, footY, footX + 2 * scale, footY);
        g2.drawLine(footX, footY - 2 * scale, footX, footY + 2 * scale);

        if (frame.hasHand()) {
            int handX = frame.handScreenX(footX, scale, extraWidth);
            int handY = frame.handScreenY(footY, scale, extraHeight);
            g2.setColor(Color.MAGENTA);
            g2.drawRect(handX, handY, scale - 1, scale - 1);
            if (alive && isWeaponDrawn(frame)) {
                double angle = combatState == CombatState.RANGE ? rangedAngle() : WeaponRenderer.swingPose(meleeWeapon, meleeFrame, aim).bladeAngle;
                int push = combatState == CombatState.RANGE && recoilKickTicks > 0 ? -1 : 0;
                int gripX = handX + (int) Math.round(Math.cos(angle) * push) * scale;
                int gripY = handY + (int) Math.round(Math.sin(angle) * push) * scale;
                g2.setColor(Color.YELLOW);
                g2.drawRect(gripX - 1, gripY - 1, scale + 1, scale + 1);
            }
        }

        if (frame.hasShoulder()) {
            g2.setColor(Color.ORANGE);
            g2.drawRect(frame.shoulderScreenX(footX, scale, extraWidth), frame.shoulderScreenY(footY, scale, extraHeight), scale - 1, scale - 1);
        }

        String sheetName = frame.sheet.name.substring(frame.sheet.name.lastIndexOf('/') + 1);
        boolean chordOpen = heldAim != null && !isAimSettled();
        String[] lines = {
                animator.state() + "  " + sheetName + "  #" + frame.index + "/" + frame.sheet.frameCount,
                "dir " + animator.direction() + (frame.mirrored ? " (mirrored)" : "") + "  facing " + facing,
                "aim " + aim + "  held " + heldAim + (chordOpen ? "  chord " + aimHeldTicks + "/" + AIM_CHORD_TICKS : ""),
                String.format(Locale.ROOT, "squash %.2f  (%+d px)", squash, extraWidth)
        };
        g2.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        FontMetrics metrics = g2.getFontMetrics();
        int lineHeight = metrics.getHeight();
        int width = 0;
        for (String line : lines) {
            width = Math.max(width, metrics.stringWidth(line));
        }
        int boxX = screenX + gp.tileSize + 6;
        int boxY = screenY - gp.tileSize / 2;
        g2.setColor(new Color(0, 0, 0, 170));
        g2.fillRect(boxX - 3, boxY - 2, width + 6, lineHeight * lines.length + 4);
        g2.setColor(Color.WHITE);
        for (int i = 0; i < lines.length; i++) {
            g2.drawString(lines[i], boxX, boxY + metrics.getAscent() + i * lineHeight);
        }

        g2.setFont(oldFont);
        g2.setColor(oldColor);
    }

    private boolean isWeaponDrawn(SpriteSheet.Frame frame) {
        if (aim == null || !frame.hasHand()) {
            return false;
        }
        return combatState == CombatState.RANGE || (combatState == CombatState.MELEE && meleeAttacking);
    }

    private void drawWeapon(Graphics2D g2, SpriteSheet.Frame frame, int footX, int footY, int extraWidth, int extraHeight) {
        int scale = gp.scale;
        if (frame.hasShoulder()) {
            drawWeaponWithArm(g2, frame, footX, footY, extraWidth, extraHeight);
            return;
        }
        int handX = frame.handScreenX(footX, scale, extraWidth);
        int handY = frame.handScreenY(footY, scale, extraHeight);
        if (combatState == CombatState.RANGE) {
            double angle = rangedAngle();
            int kick = recoilKickTicks > 0 ? -1 : 0;
            WeaponRenderer.drawHeld(g2, rangedWeapon.sprite(), angle, handX, handY, scale, kick);
            if (muzzleFlashTicks > 0) {
                WeaponRenderer.drawMuzzleFlash(g2, rangedWeapon.sprite(), angle, handX, handY, scale, kick, muzzleFlashTicks);
            }
            return;
        }
        WeaponRenderer.SwingPose pose = WeaponRenderer.swingPose(meleeWeapon, meleeFrame, aim);
        double bladeLength = meleeWeapon.sprite().length();
        WeaponRenderer.drawSmear(g2, pose, bladeLength * 0.6, bladeLength + 1 + SMEAR_REACH_ART_PIXELS, handX, handY, scale);
        WeaponRenderer.drawHeld(g2, meleeWeapon.sprite(), pose.bladeAngle, handX, handY, scale, 0);
    }

    private void drawWeaponWithArm(Graphics2D g2, SpriteSheet.Frame frame, int footX, int footY, int extraWidth, int extraHeight) {
        int scale = gp.scale;
        int shoulderX = frame.shoulderScreenX(footX, scale, extraWidth);
        int shoulderY = frame.shoulderScreenY(footY, scale, extraHeight);
        double reach = frame.sheet.armReach;
        if (combatState == CombatState.RANGE) {
            double angle = rangedAngle();
            int[] hand = WeaponRenderer.armHand(angle, reach, recoilKickTicks > 0 ? 1 : 0);
            int handX = shoulderX + hand[0] * scale;
            int handY = shoulderY + hand[1] * scale;
            WeaponRenderer.drawArm(g2, frame.sheet, shoulderX, shoulderY, hand, scale);
            WeaponRenderer.drawHeld(g2, rangedWeapon.sprite(), angle, handX, handY, scale, 0);
            if (muzzleFlashTicks > 0) {
                WeaponRenderer.drawMuzzleFlash(g2, rangedWeapon.sprite(), angle, handX, handY, scale, 0, muzzleFlashTicks);
            }
            WeaponRenderer.drawFist(g2, frame.sheet, shoulderX, shoulderY, hand, scale);
            return;
        }
        WeaponRenderer.SwingPose pose = WeaponRenderer.swingPose(meleeWeapon, meleeFrame, aim);
        double bladeLength = meleeWeapon.sprite().length();
        int[] hand = WeaponRenderer.armHand(pose.bladeAngle, reach, 0);
        int handX = shoulderX + hand[0] * scale;
        int handY = shoulderY + hand[1] * scale;
        WeaponRenderer.drawSmear(g2, pose, reach + bladeLength * 0.6, reach + bladeLength + SMEAR_REACH_ART_PIXELS, shoulderX, shoulderY, scale);
        WeaponRenderer.drawArm(g2, frame.sheet, shoulderX, shoulderY, hand, scale);
        WeaponRenderer.drawHeld(g2, meleeWeapon.sprite(), pose.bladeAngle, handX, handY, scale, 0);
        WeaponRenderer.drawFist(g2, frame.sheet, shoulderX, shoulderY, hand, scale);
    }

    private void drawMeleeHitBox(Graphics2D g2) {
        if (!meleeAttacking || combatState != CombatState.MELEE || aim == null) {
            return;
        }

        Shape onScreen = AffineTransform.getTranslateInstance(screenX - worldX, screenY - worldY)
                .createTransformedShape(getMeleeAttackArea());

        Color oldColor = g2.getColor();
        g2.setColor(Color.ORANGE);
        g2.draw(onScreen);
        g2.setColor(oldColor);
    }

    private void startMeleeAttack() {
        meleeAttacking = true;
        meleeFrame = 0;
        resetMeleeHitEnemies();
        gp.playSoundEffect(Sound.SWORD);
        squash = 0.45;
        spawnFootDust(-aim.unitX, -aim.unitY, 2);

        addMovementImpulse(meleeWeapon.impulseFor(aim));
    }

    private void stopMeleeAttack() {
        meleeAttacking = false;
        meleeFrame = 0;
        meleeCooldownCounter = meleeWeapon.cooldownFrames();
        aim = null;
    }

    private void updateMeleeAttack() {
        if (aim == null || combatState != CombatState.MELEE) {
            stopMeleeAttack();
            return;
        }
        meleeFrame++;

        int total = meleeWeapon.totalFrames();

        if (isMeleeStrikeFrame()) {
            meleeDamage();
        }

        if (meleeFrame >= total) {
            stopMeleeAttack();
        }
    }

    private void applyMeleeDamping() {
        double meleeDamping = getMeleeVelocityDamping();
        velX *= meleeDamping;
        velY *= meleeDamping;
    }

    private void meleeDamage() {
        Shape attackArea = getMeleeAttackArea();
        resetMeleeHitEnemiesIfNeeded();

        boolean hitSomething = false;
        for (Enemy enemy : gp.entities.enemies()) {
            if (enemy.alive) {
                if (!meleeHitEnemies.contains(enemy) && attackArea.intersects(enemy.getWorldHitBox())) {
                    Hit hit = new Hit(
                            meleeWeapon.damage(),
                            HitType.MELEE,
                            this,
                            worldX + gp.tileSize / 2,
                            worldY + gp.tileSize / 2,
                            meleeWeapon.enemyKnockback(),
                            null
                    );
                    HitResult result = enemy.receiveHit(hit);
                    if (result != HitResult.IGNORED) {
                        meleeHitEnemies.add(enemy);
                    }
                    hitSomething = true;
                }
            }
        }

        if (gp.progression.hasSkill(Skill.PROJECTILE_CUT)) {
            boolean reflect = gp.progression.hasSkill(Skill.PROJECTILE_REFLECT);
            int cut = reflect ? gp.bulletManager.reflectEnemyBullets(attackArea) + gp.enemyShots.reflect(attackArea)
                    : gp.bulletManager.cutEnemyBullets(attackArea) + gp.enemyShots.cut(attackArea);
            for (Enemy enemy : gp.entities.enemies()) {
                cut += reflect ? enemy.reflectProjectiles(attackArea) : enemy.cutProjectiles(attackArea);
            }
            if (cut > 0) {
                hitSomething = true;
            }
        }

        if (hitSomething) {
            gp.startHitStop(meleeWeapon.hitStopFrames());
        }
    }

    private Shape getMeleeAttackArea() {
        int centerX = worldX + gp.tileSize / 2;
        int centerY = worldY + gp.tileSize / 2;
        return meleeWeapon.attackArea(aim, meleeFrame, centerX, centerY);
    }

    private boolean hasAttackInput() {
        return keyH.upShootPressed || keyH.downShootPressed || keyH.leftShootPressed || keyH.rightShootPressed;
    }

    private boolean hasAllowedAttackInput() {
        if (!hasAttackInput() || isWeaponsLocked()) {
            return false;
        }

        if (combatState == CombatState.RANGE) {
            return gp.prologueTutorialManager.canUseRangedAttack();
        }

        if (combatState == CombatState.MELEE) {
            return gp.prologueTutorialManager.canUseMeleeAttack();
        }

        return false;
    }

    private boolean isMeleeStrikeFrame() {
        return meleeWeapon.isStrikeFrame(meleeFrame);
    }

    private double getMeleeMaxSpeed() {
        return meleeWeapon.maxSpeedForFrame(meleeFrame, maxSpeed);
    }

    private double getMeleeVelocityDamping() {
        return meleeWeapon.dampingForFrame(meleeFrame);
    }

    private void resetMeleeHitEnemies() {
        meleeHitEnemies.clear();
    }

    private void resetMeleeHitEnemiesIfNeeded() {
    }

    public void setMeleeWeapon(MeleeWeapon meleeWeapon) {
        if (meleeWeapon == null) {
            throw new IllegalArgumentException("Melee weapon is required");
        }
        this.meleeWeapon = meleeWeapon;
        resetMeleeHitEnemies();
    }

    public void setRangedWeapon(RangedWeapon rangedWeapon) {
        if (rangedWeapon == null) {
            throw new IllegalArgumentException("Ranged weapon is required");
        }
        this.rangedWeapon = rangedWeapon;
        shotCooldownCounter = 0;
    }

    public boolean isWeaponsLocked() {
        return weaponsLockedByArea && !weaponLockOverride;
    }

    public void setWeaponsLockedByArea(boolean locked) {
        weaponsLockedByArea = locked;
    }

    public boolean isWeaponLockOverridden() {
        return weaponLockOverride;
    }

    public void toggleWeaponLockOverride() {
        weaponLockOverride = !weaponLockOverride;
    }

    public int currentMeleeSlot() {
        return combatLoadout.currentMeleeSlot();
    }

    public int currentRangedSlot() {
        return combatLoadout.currentRangedSlot();
    }

    public boolean ownsMeleeSlot(int slot) {
        return combatLoadout.ownsMelee(slot);
    }

    public boolean ownsRangedSlot(int slot) {
        return combatLoadout.ownsRanged(slot);
    }

    public void setMeleeSlotOwned(int slot, boolean owned) {
        combatLoadout.setMeleeOwned(slot, owned);
        syncWeaponsToLoadout();
    }

    public void setRangedSlotOwned(int slot, boolean owned) {
        combatLoadout.setRangedOwned(slot, owned);
        syncWeaponsToLoadout();
    }

    public void resetLoadout() {
        combatLoadout.resetOwnership();
        startingMeleeSlot = 0;
        startingRangedSlot = 0;
        syncWeaponsToLoadout();
    }

    private void syncWeaponsToLoadout() {
        if (!combatLoadout.ownsRanged(startingRangedSlot)) {
            startingRangedSlot = 0;
        }
        if (!combatLoadout.ownsMelee(startingMeleeSlot)) {
            startingMeleeSlot = 0;
        }
        setMeleeWeapon(combatLoadout.currentMeleeWeapon());
        setRangedWeapon(combatLoadout.currentRangedWeapon());
    }

    public RangedWeapon currentRangedWeapon() {
        return rangedWeapon;
    }

    public String currentWeaponName() {
        if (combatState == CombatState.MELEE) {
            return meleeWeapon.displayName();
        }
        return rangedWeapon.displayName();
    }

    public String currentMeleeWeaponName() {
        return meleeWeapon.displayName();
    }

    public String currentRangedWeaponName() {
        return rangedWeapon.displayName();
    }

    public String meleeSlotLabel(int slot) {
        return combatLoadout.meleeSlotLabel(slot);
    }

    public String rangedSlotLabel(int slot) {
        return combatLoadout.rangedSlotLabel(slot);
    }

    @Override
    public void takeDamage(int damage) {
        if (damage <= 0 || isHurtInvincible() || !alive) {
            return;
        }

        ticksSinceHit = 0;
        shieldRegenTicks = 0;
        int absorbed = Math.min(shield, damage);
        if (absorbed > 0) {
            shield -= absorbed;
            damage -= absorbed;
            double cx = worldX + gp.tileSize / 2.0, cy = worldY + gp.tileSize / 2.0;
            gp.effects.burst(cx, cy, 12, 3.0, SHIELD_SHARDS);
            gp.effects.ring(cx, cy, 44, 12, new Color(0x9AD8F0), false);
            gp.effects.text(cx, worldY - 10, shield == 0 ? "SHIELD BROKEN" : "BLOCKED", new Color(0x9AD8F0), 1);
            gp.playSoundEffect(shield == 0 ? Sound.SHIELD_BREAK : Sound.SHIELD_BLOCK);
            if (damage == 0) {
                hurtInvincibleCounter = HURT_INVINCIBLE_FRAMES;
                squash = 0.6;
                gp.effects.shake(6, 4);
                return;
            }
        }

        super.takeDamage(damage);

        if (!alive) {
            die();
            return;
        }

        hurtInvincibleCounter = HURT_INVINCIBLE_FRAMES;
        gp.playSoundEffect(randomHurtSound());
        hurtFlashTicks = HURT_FLASH_TICKS;
        squash = 1.0;
        gp.effects.shake(10, 6);
        gp.effects.burst(worldX + gp.tileSize / 2.0, worldY + gp.tileSize / 2.0, 8, 2.5, EffectsManager.HURT);
    }

    @Override
    public HitResult receiveHit(Hit hit) {
        if (isHurtInvincible() || !alive) {
            return HitResult.IGNORED;
        }
        takeDamage(hit.damage());
        if (alive && hit.knockbackForce() > 0) {
            applyKnockbackFrom(hit.sourceWorldX(), hit.sourceWorldY(), hit.knockbackForce());
        }
        return HitResult.APPLIED;
    }

    public boolean isHurtInvincible() {
        return gp.debugSettings.isGodMode() || hurtInvincibleCounter > 0 || rollTicks > 0;
    }

    public void debugRestoreVitals() {
        if (!alive) setDefaultValues();
        health = maxHealth;
        shield = maxShield;
        ticksSinceHit = 0;
        shieldRegenTicks = 0;
    }

    public int getHurtInvincibleFrames() {
        return HURT_INVINCIBLE_FRAMES;
    }

    public boolean takeDamageFromEntity(Entity source, int damage) {
        if (isHurtInvincible()) {
            return false;
        }

        takeDamage(damage);
        if (!alive) {
            return true;
        }

        applyKnockbackFrom(
                source.worldX + gp.tileSize / 2,
                source.worldY + gp.tileSize / 2,
                ENTITY_HURT_KNOCKBACK_FORCE
        );
        return true;
    }

    public void applyKnockbackFrom(int sourceWorldX, int sourceWorldY, double force) {
        int playerCenterX = worldX + gp.tileSize / 2;
        int playerCenterY = worldY + gp.tileSize / 2;

        double dx = playerCenterX - sourceWorldX;
        double dy = playerCenterY - sourceWorldY;
        double length = vectorLength(dx, dy);

        if (length == 0) {
            dx = 1;
            dy = 0;
            length = 1;
        }

        velX = (dx / length) * force;
        velY = (dy / length) * force;
        hurtKnockbackCounter = HURT_KNOCKBACK_FRAMES;
    }

    private int randomHurtSound() {
        return ThreadLocalRandom.current().nextBoolean()
                ? Sound.PLAYER_HURT_1
                : Sound.PLAYER_HURT_2;
    }

    private void die() {
        health = 0;
        alive = false;
        velX = 0;
        velY = 0;
        hurtInvincibleCounter = 0;
        hurtKnockbackCounter = 0;
        meleeAttacking = false;
        meleeFrame = 0;
        meleeAttackBuffered = false;
        meleeInputBufferCounter = 0;
        meleeCooldownCounter = 0;
        meleeAttackInputHeldLastFrame = false;
        direction = "idle";
        resetFacingAndAim();
        gp.keyH.clearGameplayInput();
        gp.playSoundEffect(Sound.PLAYER_DEATH);
        deathTicks = DEATH_TICKS;
        hurtFlashTicks = HURT_FLASH_TICKS;
        animator.die();
        gp.effects.shake(16, 9);
        gp.effects.burst(worldX + gp.tileSize / 2.0, worldY + gp.tileSize / 2.0, 26, 3.0, EffectsManager.SPORES);
    }
}
