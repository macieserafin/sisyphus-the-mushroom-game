package entity;

import main.GamePanel;
import main.KeyHandler;
import main.Sound;
import entity.combat.Hit;
import entity.combat.HitResult;
import entity.combat.HitType;
import entity.enemy.Enemy;
import entity.npc.NPC;
import object.SuperObject;
import entity.anim.PlayerAnimator;
import entity.anim.WeaponRenderer;
import entity.anim.SpriteSheet;
import entity.weapon.CombatLoadout;
import entity.weapon.MeleeWeapon;
import entity.weapon.RangedWeapon;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.util.HashSet;
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
    private static final double FACING_HYSTERESIS_RADIANS = Math.toRadians(10);

    KeyHandler keyH;

    // Where the player is drawn on screen; updated by updateCamera() so the view stops at map edges.
    public int screenX;
    public int screenY;
    private double coveredByDialogue = 0;

    private double velX = 0.0;
    private double velY = 0.0;
    private double maxSpeed = BASE_MAX_SPEED;

    private double worldXf;
    private double worldYf;

    private Direction8 facing = Direction8.S;
    private boolean moving;
    private Direction8 aim;
    private Direction8 heldAim;
    private Direction8 lastAim;
    private int aimHeldTicks;
    private int muzzleFlashTicks;
    private int recoilKickTicks;

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

    // Chosen at the dojo rack; every new life starts with these weapons in hand.
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

        maxSpeed = BASE_MAX_SPEED;
        speed = (int)Math.round(maxSpeed);
        maxHealth = 3;
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
        meleeAttacking = false;
        meleeFrame = 0;
        meleeAttackBuffered = false;
        meleeInputBufferCounter = 0;
        meleeCooldownCounter = 0;
        meleeAttackInputHeldLastFrame = false;
        resetMeleeHitEnemies();
        animator.reset();
        endScriptedHold();
    }

    /**
     * Follows the player but stops at the map edges, and centres maps smaller than the screen
     * (interiors) instead of scrolling them.
     */
    public void updateCamera() {
        // While a dialogue box covers the bottom of the screen, frame the player in the space above it.
        // The shift eases in and out so the view doesn't jump when a line appears.
        coveredByDialogue += (gp.ui.dialogueOverlayHeight() - coveredByDialogue) * 0.15;
        int visibleHeight = gp.screenHeight - (int) Math.round(coveredByDialogue);

        int centerX = gp.screenWidth / 2 - gp.tileSize / 2;
        int centerY = visibleHeight / 2 - gp.tileSize / 2;
        int cameraX = cameraOrigin(worldX - centerX, gp.tileM.getWorldCols() * gp.tileSize, gp.screenWidth);
        int cameraY = cameraOrigin(worldY - centerY, gp.tileM.getWorldRows() * gp.tileSize, visibleHeight);
        screenX = worldX - cameraX;
        screenY = worldY - cameraY;
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

    /** Picks the next starting melee weapon and equips it right away. Returns its name. */
    public String cycleStartingMeleeWeapon() {
        do {
            startingMeleeSlot = (startingMeleeSlot + 1) % combatLoadout.meleeWeaponCount();
        } while (!combatLoadout.ownsMelee(startingMeleeSlot));
        setMeleeWeapon(combatLoadout.selectMeleeSlot(startingMeleeSlot));
        return combatLoadout.meleeWeaponName(startingMeleeSlot);
    }

    /** Picks the next starting ranged weapon and equips it right away. Returns its name. */
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

        if (keyH.qPressed) {
            if (gp.prologueTutorialManager.canSwitchCombatMode()) {
                combatState = combatState.next();
                keyH.qPressed = false;
                meleeAttackInputHeldLastFrame = false;
                meleeAttackBuffered = false;
                meleeInputBufferCounter = 0;
            } else {
                keyH.qPressed = false;
            }
        }
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
        resolveInteractionsAfterMovement();
        updateAnimation();
    }

    private void tickTimers() {
        if (shotCooldownCounter > 0) {
            shotCooldownCounter--;
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
        animator.locomote(moving, vectorLength(velX, velY) / gp.scale, facing);
        if (aim == null) {
            animator.clearPose();
        } else if (combatState == CombatState.RANGE) {
            animator.holdPose(PlayerAnimator.State.SHOOT, rangedWeapon.animations().shoot(), 0, aim);
        } else if (combatState == CombatState.MELEE && meleeAttacking) {
            int poseFrame = meleeWeapon.useStrikeSprite(meleeFrame) ? 1 : 0;
            animator.holdPose(PlayerAnimator.State.ATTACK, meleeWeapon.animations().attack(), poseFrame, aim);
        } else {
            animator.clearPose();
        }
    }

    private double vectorLength(double x, double y) {
        return Math.sqrt(x * x + y * y);
    }

    private void fireBullet(Direction8 shootDirection) {
        muzzleFlashTicks = MUZZLE_FLASH_TICKS;
        recoilKickTicks = RECOIL_KICK_TICKS;
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

    // ENTER without bumping into anyone: talk to an NPC within reach (e.g. across a counter),
    // otherwise use the closest interactive object next to the player.
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
        if (scriptedHidden) {
            return;
        }

        SpriteSheet.Frame frame = animator.frame();
        int footX = screenX + gp.tileSize / 2 + scriptedDrawOffsetX;
        int footY = screenY + gp.tileSize + scriptedDrawOffsetY;

        Composite oldComposite = g2.getComposite();
        if (isHurtInvincible() && (hurtInvincibleCounter / 5) % 2 == 0) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.45f));
        }
        boolean weaponVisible = isWeaponDrawn(frame);
        boolean weaponBehind = weaponVisible && aim.dy < 0;
        if (weaponBehind) {
            drawWeapon(g2, frame, footX, footY);
        }
        frame.draw(g2, footX, footY, gp.scale);
        if (weaponVisible && !weaponBehind) {
            drawWeapon(g2, frame, footX, footY);
        }
        g2.setComposite(oldComposite);

        if (gp.debugSettings.isOverlayEnabled()) {
            g2.setColor(Color.GREEN);
            g2.drawRect(screenX + solidArea.x, screenY + solidArea.y, solidArea.width, solidArea.height);

            g2.setColor(Color.RED);
            g2.drawRect(screenX + hitBox.x, screenY + hitBox.y, hitBox.width, hitBox.height);

            drawMeleeHitBox(g2);
        }
    }

    private boolean isWeaponDrawn(SpriteSheet.Frame frame) {
        if (aim == null || !frame.hasHand()) {
            return false;
        }
        return combatState == CombatState.RANGE || (combatState == CombatState.MELEE && meleeAttacking);
    }

    private void drawWeapon(Graphics2D g2, SpriteSheet.Frame frame, int footX, int footY) {
        int scale = gp.scale;
        int handX = frame.originX(footX, scale) + frame.handX() * scale;
        int handY = frame.originY(footY, scale) + frame.handY() * scale;
        if (combatState == CombatState.RANGE) {
            double angle = aim.angle();
            int kick = recoilKickTicks > 0 ? -1 : 0;
            WeaponRenderer.drawHeld(g2, rangedWeapon.sprite(), angle, handX, handY, scale, kick);
            if (muzzleFlashTicks > 0) {
                WeaponRenderer.drawMuzzleFlash(g2, rangedWeapon.sprite(), angle, handX, handY, scale, kick, muzzleFlashTicks);
            }
            return;
        }
        WeaponRenderer.SwingPose pose = WeaponRenderer.swingPose(meleeWeapon, meleeFrame, aim);
        double bladeLength = meleeWeapon.sprite().length();
        WeaponRenderer.drawSmear(g2, pose, bladeLength * 0.6, bladeLength + 1, handX, handY, scale);
        WeaponRenderer.drawHeld(g2, meleeWeapon.sprite(), pose.bladeAngle, handX, handY, scale, 0);
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

        addMovementImpulse(meleeWeapon.impulseFor(aim));
    }

    private void stopMeleeAttack() {
        meleeAttacking = false;
        meleeFrame = 0;
        meleeCooldownCounter = meleeWeapon.cooldownFrames();
        aim = null;
    }

    private void updateMeleeAttack() {
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

        super.takeDamage(damage);

        if (!alive) {
            die();
            return;
        }

        hurtInvincibleCounter = HURT_INVINCIBLE_FRAMES;
        gp.playSoundEffect(randomHurtSound());
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
        return hurtInvincibleCounter > 0;
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
        gp.onPlayerDied();
    }
}
