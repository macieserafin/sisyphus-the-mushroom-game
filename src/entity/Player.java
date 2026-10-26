package entity;

import main.GamePanel;
import main.GameState;
import main.KeyHandler;
import main.Sound;
import entity.combat.Hit;
import entity.combat.HitResult;
import entity.combat.HitType;
import entity.enemy.Enemy;
import entity.weapon.CombatLoadout;
import entity.weapon.MeleeWeapon;
import entity.weapon.RangedWeapon;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashSet;
import java.util.Objects;
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

    KeyHandler keyH;

    public final int screenX;
    public final int screenY;

    private double velX = 0.0;
    private double velY = 0.0;
    private double maxSpeed = BASE_MAX_SPEED;

    private double worldXf;
    private double worldYf;

    public String combatDirection;

    private int shotCooldownCounter = 0;

    public CombatState combatState = CombatState.RANGE;
    private final CombatLoadout combatLoadout = new CombatLoadout();
    private MeleeWeapon meleeWeapon = combatLoadout.currentMeleeWeapon();
    private RangedWeapon rangedWeapon = combatLoadout.currentRangedWeapon();

    private boolean meleeAttacking = false;
    private int meleeFrame = 0;
    private String bufferedMeleeDirection = null;
    private int meleeInputBufferCounter = 0;
    private int meleeCooldownCounter = 0;
    private boolean meleeAttackInputHeldLastFrame = false;
    private final Set<Enemy> meleeHitEnemies = new HashSet<>();

    public int hasKey = 0;
    private int hurtInvincibleCounter = 0;
    private int hurtKnockbackCounter = 0;

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
        getPlayerImage();
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
        combatDirection = "idle";
        combatState = CombatState.RANGE;
        meleeWeapon = combatLoadout.selectMeleeSlot(0);
        rangedWeapon = combatLoadout.selectRangedSlot(0);
        shotCooldownCounter = 0;
        hurtInvincibleCounter = 0;
        hurtKnockbackCounter = 0;
        meleeAttacking = false;
        meleeFrame = 0;
        bufferedMeleeDirection = null;
        meleeInputBufferCounter = 0;
        meleeCooldownCounter = 0;
        meleeAttackInputHeldLastFrame = false;
        resetMeleeHitEnemies();
        spriteCounter = 0;
        spriteNum = 1;
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

    public void getPlayerImage() {
        try {
            up1 = ImageIO.read(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("player/player_up_1.png")));
            up2 = ImageIO.read(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("player/player_up_2.png")));
            down1 = ImageIO.read(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("player/player_down_1.png")));
            down2 = ImageIO.read(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("player/player_down_2.png")));
            left1 = ImageIO.read(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("player/player_left_1.png")));
            left2 = ImageIO.read(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("player/player_left_2.png")));
            right1 = ImageIO.read(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("player/player_right_1.png")));
            right2 = ImageIO.read(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("player/player_right_2.png")));

            idle1 = ImageIO.read(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("player/player_idle_1.png")));
            idle2 = ImageIO.read(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("player/player_idle_2.png")));

        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    public void update() {
        if (!alive) {
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
                bufferedMeleeDirection = null;
                meleeInputBufferCounter = 0;
            } else {
                keyH.qPressed = false;
            }
        }
        updateWeaponSwitchInput();

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
        updateDirectionFromVelocity();
        resolveInteractionsAfterMovement();
        updateSpriteAnimation(movingInput);
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
        String shootDirection = getAttackInputDirection();
        meleeAttackInputHeldLastFrame = false;

        if (!attackInput || shootDirection == null) {
            combatDirection = "idle";
            return;
        }

        combatDirection = shootDirection;

        if (shotCooldownCounter == 0) {
            fireBullet(shootDirection);
            shotCooldownCounter = rangedWeapon.shotCooldownFrames();
        }
    }

    private void updateMeleeInputBuffer(boolean attackInput) {
        String attackInputDirection = getAttackInputDirection();
        boolean attackJustPressed = attackInput && !meleeAttackInputHeldLastFrame;
        meleeAttackInputHeldLastFrame = attackInput;

        if (attackJustPressed && attackInputDirection != null) {
            bufferedMeleeDirection = attackInputDirection;
            meleeInputBufferCounter = meleeWeapon.inputBufferFrames();
        } else if (meleeInputBufferCounter > 0) {
            meleeInputBufferCounter--;
        }

        if (!meleeAttacking &&
                meleeCooldownCounter == 0 &&
                meleeInputBufferCounter > 0 &&
                bufferedMeleeDirection != null) {
            combatDirection = bufferedMeleeDirection;
            bufferedMeleeDirection = null;
            meleeInputBufferCounter = 0;
            startMeleeAttack();
        } else if (!meleeAttacking) {
            combatDirection = "idle";
        }
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

    private void updateDirectionFromVelocity() {
        if (Math.abs(velX) < STOP_EPSILON && Math.abs(velY) < STOP_EPSILON) {
            direction = "idle";
            return;
        }

        if (Math.abs(velX) > Math.abs(velY)) {
            direction = velX > 0 ? "right" : "left";
        } else {
            direction = velY > 0 ? "down" : "up";
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

    private void updateSpriteAnimation(boolean movingInput) {
        spriteCounter++;

        int spriteUpdate = movingInput ? 10 : 18;
        if (spriteCounter > spriteUpdate) {
            spriteNum = spriteNum == 1 ? 2 : 1;
            spriteCounter = 0;
        }
    }

    private double vectorLength(double x, double y) {
        return Math.sqrt(x * x + y * y);
    }

    private void fireBullet(String shootDirection) {
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

        if(i != 999){
            if(gp.keyH.enterPressed)
            {
                gp.entities.npcs().get(i).speak();
            }

        }
        gp.keyH.enterPressed = false;

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
        BufferedImage image = getCurrentPlayerImage();
        if (image == null) {
            image = idle1;
        }

        int drawW = image.getWidth() * gp.scale;
        int drawH = image.getHeight() * gp.scale;

        Point p = computeDrawPosition(drawW, drawH);
        Composite oldComposite = g2.getComposite();
        if (isHurtInvincible() && (hurtInvincibleCounter / 5) % 2 == 0) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.45f));
        }
        g2.drawImage(image, p.x, p.y, drawW, drawH, null);
        g2.setComposite(oldComposite);

        if (gp.debugSettings.isOverlayEnabled()) {
            g2.setColor(Color.GREEN);
            g2.drawRect(screenX + solidArea.x, screenY + solidArea.y, solidArea.width, solidArea.height);

            g2.setColor(Color.RED);
            g2.drawRect(screenX + hitBox.x, screenY + hitBox.y, hitBox.width, hitBox.height);

            drawMeleeHitBox(g2);
        }
    }

    private BufferedImage getCurrentPlayerImage() {
        if(combatDirection.equals("idle")) {
            if (direction.equals("idle")) {
                return spriteNum == 1 ? idle1 : idle2;
            }

            switch (direction) {
                case "up":
                    return spriteNum == 1 ? up1 : up2;
                case "left":
                    return spriteNum == 1 ? left1 : left2;
                case "right":
                    return spriteNum == 1 ? right1 : right2;
                case "down":
                    return spriteNum == 1 ? down1 : down2;
                default:
                    return idle1;
            }
        }

        if(combatState == CombatState.RANGE){
            BufferedImage rangedImage = rangedWeapon.animations().shootImage(combatDirection);
            return rangedImage != null ? rangedImage : idle1;
        }

        if (combatState == CombatState.MELEE && meleeAttacking) {
            boolean useAttack2 = meleeWeapon.useStrikeSprite(meleeFrame);
            BufferedImage meleeImage = meleeWeapon.animations().attackImage(combatDirection, useAttack2);
            return meleeImage != null ? meleeImage : idle1;
        }

        return idle1;
    }

    private void drawMeleeHitBox(Graphics2D g2) {
        if (!meleeAttacking || combatState != CombatState.MELEE || combatDirection.equals("idle")) {
            return;
        }

        Rectangle attackBox = getMeleeAttackBox();
        int screenAttackX = attackBox.x - worldX + screenX;
        int screenAttackY = attackBox.y - worldY + screenY;

        Color oldColor = g2.getColor();

        g2.setColor(Color.ORANGE);
        g2.drawRect(screenAttackX, screenAttackY, attackBox.width, attackBox.height);

        g2.setColor(oldColor);
    }

    private Point computeDrawPosition(int drawW, int drawH) {
        int anchorX = screenX + gp.tileSize / 2;
        int anchorY = screenY + gp.tileSize;

        int drawX = anchorX - drawW / 2;
        int drawY = anchorY - drawH;

        boolean isMeleeSprite = combatState == CombatState.MELEE && !"idle".equals(combatDirection);
        if (!isMeleeSprite) {
            return new Point(drawX, drawY);
        }

        int extraW = drawW - gp.tileSize;

        switch (combatDirection) {
            case "right":
                drawX = screenX;
                break;
            case "left":
                drawX = screenX - extraW;
                break;
            case "down":
                drawY = screenY;
                drawX = screenX - extraW / 2;
                break;
            case "up":
            default:
                break;
        }

        return new Point(drawX, drawY);
    }

    private void startMeleeAttack() {
        meleeAttacking = true;
        meleeFrame = 0;
        resetMeleeHitEnemies();
        gp.playSoundEffect(Sound.SWORD);

        addMovementImpulse(meleeWeapon.impulseFor(combatDirection));
    }

    private void stopMeleeAttack() {
        meleeAttacking = false;
        meleeFrame = 0;
        meleeCooldownCounter = meleeWeapon.cooldownFrames();
        combatDirection = "idle";
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
        Rectangle attackBox = getMeleeAttackBox();
        resetMeleeHitEnemiesIfNeeded();

        boolean hitSomething = false;
        for (Enemy enemy : gp.entities.enemies()) {
            if (enemy.alive) {
                if (!meleeHitEnemies.contains(enemy) && attackBox.intersects(enemy.getWorldHitBox())) {
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

    private Rectangle getMeleeAttackBox() {
        int centerX = worldX + gp.tileSize / 2;
        int centerY = worldY + gp.tileSize / 2;
        return meleeWeapon.attackBox(combatDirection, meleeFrame, centerX, centerY);
    }

    private boolean hasAttackInput() {
        return keyH.upShootPressed || keyH.downShootPressed || keyH.leftShootPressed || keyH.rightShootPressed;
    }

    private boolean hasAllowedAttackInput() {
        if (!hasAttackInput()) {
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

    private String getAttackInputDirection() {
        if (keyH.upShootPressed) return "up";
        if (keyH.downShootPressed) return "down";
        if (keyH.leftShootPressed) return "left";
        if (keyH.rightShootPressed) return "right";
        return null;
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
        bufferedMeleeDirection = null;
        meleeInputBufferCounter = 0;
        meleeCooldownCounter = 0;
        meleeAttackInputHeldLastFrame = false;
        direction = "idle";
        combatDirection = "idle";
        spriteCounter = 0;
        spriteNum = 1;
        gp.keyH.clearGameplayInput();
        gp.playSoundEffect(Sound.PLAYER_DEATH);
        gp.gameState = GameState.DEAD;
    }
}
