package entity.enemy;

import entity.Entity;
import entity.combat.Hit;
import entity.combat.HitResult;
import main.GamePanel;
import main.Sound;

import java.awt.*;
import java.util.Random;

public class Enemy extends Entity {
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
    private final Random random = new Random();

    public Enemy(GamePanel gp) {
        super(gp);
    }

    protected void setupEnemyStats(String name, int maxHealth, int speed, int attackDamage) {
        this.name = name;
        this.maxHealth = maxHealth;
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
        if (damageFlashFramesLeft > 0) {
            damageFlashFramesLeft--;
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
        damageFlashFramesLeft = DAMAGE_FLASH_FRAMES;
    }

    @Override
    public HitResult receiveHit(Hit hit) {
        if (!alive) {
            return HitResult.IGNORED;
        }

        takeDamage(hit.damage());
        if (hit.knockbackForce() > 0) {
            applyKnockbackFrom(hit.sourceWorldX(), hit.sourceWorldY(), hit.knockbackForce());
        }
        return HitResult.APPLIED;
    }

    public void handleDeathOnce() {
        if (deathHandled) {
            return;
        }
        deathHandled = true;
        onDeath();
    }

    public boolean isReadyForRemoval() {
        return !alive && deathHandled;
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
    }

    private void drawDamageFlash(Graphics g2) {
        if (damageFlashFramesLeft <= 0 || !alive) {
            return;
        }

        int screenX = worldX - gp.player.worldX + gp.player.screenX;
        int screenY = worldY - gp.player.worldY + gp.player.screenY;

        Graphics2D g2d = (Graphics2D) g2;
        Composite oldComposite = g2d.getComposite();
        Color oldColor = g2d.getColor();

        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.42f));
        g2d.setColor(Color.WHITE);
        g2d.fillRect(screenX, screenY, gp.tileSize, gp.tileSize);

        g2d.setComposite(oldComposite);
        g2d.setColor(oldColor);
    }

    protected void drawHealthBar(Graphics g2) {
        if (maxHealth <= 0) {
            return;
        }

        int screenX = worldX - gp.player.worldX + gp.player.screenX;
        int screenY = worldY - gp.player.worldY + gp.player.screenY;
        int barWidth = 32;
        int barHeight = 4;
        int filledWidth = barWidth * health / maxHealth;

        Color oldColor = g2.getColor();

        g2.setColor(Color.RED);
        g2.fillRect(screenX + 8, screenY - 8, barWidth, barHeight);

        g2.setColor(Color.GREEN);
        g2.fillRect(screenX + 8, screenY - 8, filledWidth, barHeight);

        g2.setColor(oldColor);
    }
}
