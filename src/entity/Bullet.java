package entity;

import entity.combat.Faction;
import entity.combat.Hit;
import entity.combat.HitResult;
import entity.combat.HitType;
import entity.enemy.Enemy;
import main.GamePanel;

import java.awt.*;
import java.awt.image.BufferedImage;

public class Bullet extends Entity {
    public static final int DEFAULT_SIZE = 8;
    public enum Appearance { PISTOL, SHOTGUN_PELLET }
    private Appearance appearance = Appearance.PISTOL;

    public double worldXf;
    public double worldYf;

    public double velX;
    public double velY;

    public double distanceTraveled = 0;
    public double maxDistance;

    public int damage = 1;
    private Faction faction = Faction.PLAYER;
    private Entity owner;

    public BufferedImage image;

    public Bullet(GamePanel gp) {
        super(gp);
        solidArea = new Rectangle(0, 0, DEFAULT_SIZE, DEFAULT_SIZE);
        solidAreaDefaultX = 0;
        solidAreaDefaultY = 0;

        direction = "right";
        speed = 10;
        maxDistance = 0;
    }

    public void setStartPosition(double x, double y) {
        worldXf = x;
        worldYf = y;

        worldX = (int)Math.round(worldXf);
        worldY = (int)Math.round(worldYf);
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public void setSpeed(int speed) {
        this.speed = speed;
    }

    public void setMaxDistancePixels(double maxDistancePixels) {
        this.maxDistance = maxDistancePixels;
    }

    public void setDamage(int damage) {
        this.damage = damage;
    }

    public void setImage(BufferedImage image) {
        this.image = image;
    }

    public void setOwner(Entity owner) {
        this.owner = owner;
    }

    public Entity getOwner() {
        return owner;
    }

    public void setFaction(Faction faction) {
        if (faction == null) {
            throw new IllegalArgumentException("Projectile faction is required");
        }
        this.faction = faction;
    }

    public Faction getFaction() {
        return faction;
    }

    public void setVelocity(double velocityX, double velocityY) {
        velX = velocityX;
        velY = velocityY;
        direction = directionFromVelocity(velocityX, velocityY);
        speed = (int)Math.round(Math.sqrt(velocityX * velocityX + velocityY * velocityY));
    }

    public void setVelocityFromDirection() {
        velX = 0;
        velY = 0;

        if (direction.equals("up")) {
            velY = -speed;
        }
        else if (direction.equals("down")) {
            velY = speed;
        }
        else if (direction.equals("left")) {
            velX = -speed;
        }
        else if (direction.equals("right")) {
            velX = speed;
        }
    }

    public void update(GamePanel gp) {
        double remainingX = velX;
        double remainingY = velY;
        int steps = Math.max(1, (int)Math.ceil(Math.sqrt(velX * velX + velY * velY) / Math.max(1, solidArea.width)));
        double stepX = remainingX / steps;
        double stepY = remainingY / steps;

        for (int i = 0; i < steps; i++) {
            double nextX = worldXf + stepX;
            double nextY = worldYf + stepY;
            if (gp.cChecker.checkTileAt(this, (int)Math.round(nextX), (int)Math.round(nextY))) {
                alive = false;
                return;
            }

            worldXf = nextX;
            worldYf = nextY;
            worldX = (int)Math.round(worldXf);
            worldY = (int)Math.round(worldYf);
            distanceTraveled += Math.sqrt(stepX * stepX + stepY * stepY);

            if (isOutsideWorld()) {
                alive = false;
                return;
            }
        }

        if (maxDistance > 0 && distanceTraveled >= maxDistance) {
            alive = false;
        }
    }

    public Hit createHit() {
        return new Hit(
                damage,
                HitType.RANGED,
                owner,
                worldX + solidArea.width / 2,
                worldY + solidArea.height / 2,
                3.5,
                this
        );
    }

    public boolean canHit(Entity target) {
        if (!alive || target == null || target == owner) {
            return false;
        }
        if (target instanceof Player) {
            return faction == Faction.ENEMY;
        }
        if (target instanceof Enemy) {
            return faction == Faction.PLAYER;
        }
        return false;
    }

    public void reactToHitResult(HitResult result) {
        if (result == HitResult.APPLIED || result == HitResult.BLOCKED) {
            alive = false;
        }
    }

    public void reflectFrom(Entity newOwner, Faction newFaction) {
        owner = newOwner;
        setFaction(newFaction);

        int ownerCenterX = newOwner.worldX + newOwner.solidArea.width / 2;
        int ownerCenterY = newOwner.worldY + newOwner.solidArea.height / 2;
        double dx = worldX - ownerCenterX;
        double dy = worldY - ownerCenterY;
        double length = Math.sqrt(dx * dx + dy * dy);

        if (length == 0) {
            velX = -velX;
            velY = -velY;
            return;
        }

        double speedLength = Math.sqrt(velX * velX + velY * velY);
        setVelocity((dx / length) * speedLength, (dy / length) * speedLength);
    }

    public static int reflectedDamage(int damage) {
        return damage / 2 + damage % 2;
    }

    public void reflectBySword(Entity player) {
        owner = player;
        faction = Faction.PLAYER;
        setVelocity(-velX, -velY);
        damage = reflectedDamage(damage);
        distanceTraveled = 0;
        maxDistance = Math.max(maxDistance, 5 * gp.tileSize);
    }

    public void draw(Graphics2D g2, int screenX, int screenY) {
        Color oldColor = g2.getColor();

        if (image != null) {
            g2.drawImage(image, screenX, screenY, null);
        }
        else {
            drawProjectile(g2, screenX, screenY);
        }

        if (gp.debugSettings.isOverlayEnabled()) {
            g2.setColor(Color.MAGENTA);
            g2.drawRect(
                    screenX + solidArea.x,
                    screenY + solidArea.y,
                    solidArea.width,
                    solidArea.height
            );
        }

        g2.setColor(oldColor);
    }

    private void drawProjectile(Graphics2D g2, int screenX, int screenY) {
        Graphics2D projectile = (Graphics2D) g2.create();
        try {
            projectile.translate(screenX + solidArea.x + solidArea.width / 2.0,
                    screenY + solidArea.y + solidArea.height / 2.0);
            projectile.rotate(Math.atan2(velY, velX));
            if (appearance == Appearance.SHOTGUN_PELLET) {
                projectile.setColor(new Color(39, 43, 49));
                projectile.fillOval(-3, -3, 6, 6);
                projectile.setColor(new Color(137, 146, 155));
                projectile.fillOval(-2, -2, 4, 4);
                projectile.setColor(new Color(225, 232, 235));
                projectile.fillRect(-1, -2, 2, 2);
                projectile.setColor(new Color(78, 87, 99));
                projectile.fillRect(-1, 1, 3, 1);
            } else {
                projectile.setColor(new Color(57, 36, 27));
                projectile.fillPolygon(new int[]{-7, 2, 5, 7, 7, 5, 2, -7},
                        new int[]{-3, -3, -2, -1, 1, 2, 3, 3}, 8);
                projectile.setColor(new Color(193, 119, 62));
                projectile.fillRect(-6, -2, 9, 4);
                projectile.fillRect(3, -1, 3, 2);
                projectile.setColor(new Color(245, 199, 128));
                projectile.fillRect(-5, -2, 7, 1);
                projectile.fillRect(2, -1, 3, 1);
                projectile.setColor(new Color(125, 68, 39));
                projectile.fillRect(-6, 1, 8, 1);
                projectile.fillRect(-5, -2, 1, 4);
            }
        } finally {
            projectile.dispose();
        }
    }

    private String directionFromVelocity(double velocityX, double velocityY) {
        if (Math.abs(velocityX) > Math.abs(velocityY)) {
            return velocityX >= 0 ? "right" : "left";
        }
        return velocityY >= 0 ? "down" : "up";
    }

    public static class Builder {
        private Bullet bullet;
        private int tileSize;

        public Builder(int tileSize, GamePanel gp) {
            this.tileSize = tileSize;
            this.bullet = new Bullet(gp);

            this.bullet.setSpeed(10);
            this.bullet.setMaxDistancePixels(5 * this.tileSize);
            this.bullet.setDamage(1);
        }

        public Builder startPosition(double x, double y) {
            bullet.setStartPosition(x, y);
            return this;
        }

        public Builder direction(String direction) {
            bullet.setDirection(direction);
            return this;
        }

        public Builder velocity(double velocityX, double velocityY) {
            bullet.setVelocity(velocityX, velocityY);
            return this;
        }

        public Builder speed(int speed) {
            bullet.setSpeed(speed);
            return this;
        }

        public Builder rangeTiles(int tiles) {
            bullet.setMaxDistancePixels(tiles * tileSize);
            return this;
        }

        public Builder damage(int damage) {
            bullet.setDamage(damage);
            return this;
        }

        public Builder image(BufferedImage image) {
            bullet.setImage(image);
            return this;
        }

        public Builder appearance(Appearance appearance) {
            bullet.appearance = java.util.Objects.requireNonNull(appearance);
            return this;
        }

        public Builder owner(Entity owner) {
            bullet.setOwner(owner);
            return this;
        }

        public Builder faction(Faction faction) {
            bullet.setFaction(faction);
            return this;
        }

        public Bullet build() {
            bullet.alive = true;
            bullet.distanceTraveled = 0;
            if (bullet.velX == 0 && bullet.velY == 0) {
                bullet.setVelocityFromDirection();
            }
            return bullet;
        }
    }
}
