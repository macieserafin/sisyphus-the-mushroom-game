package entity;

import main.GamePanel;

import java.awt.*;
import java.awt.image.BufferedImage;

public class Bullet extends Entity {

    public double worldXf;
    public double worldYf;

    public double velX;
    public double velY;

    public double distanceTraveled = 0;
    public double maxDistance; // pixels

    public int damage = 1;

    public BufferedImage image;

    public Bullet(GamePanel gp) {
        super(gp);
        solidArea = new Rectangle(0, 0, 8, 8);
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

        collisionOn = false;
        gp.cChecker.checkTile(this);

        if (collisionOn) {
            alive = false;
            return;
        }

        double dx = velX;
        double dy = velY;

        worldXf += dx;
        worldYf += dy;

        worldX = (int)Math.round(worldXf);
        worldY = (int)Math.round(worldYf);

        distanceTraveled += Math.sqrt(dx * dx + dy * dy);

        if (maxDistance > 0 && distanceTraveled >= maxDistance) {
            alive = false;
        }

        if (isOutsideWorld()) {
            alive = false;
        }
    }

    public void draw(Graphics2D g2, int screenX, int screenY) {
        Color oldColor = g2.getColor();

        if (image != null) {
            g2.drawImage(image, screenX, screenY, null);
        }
        else {
            // TEMP DEBUG BULLET (visual only)
            g2.setColor(Color.YELLOW);
            g2.fillRect(
                    screenX,
                    screenY,
                    solidArea.width,
                    solidArea.height
            );
        }

        g2.setColor(Color.MAGENTA);
        g2.drawRect(
                screenX + solidArea.x,
                screenY + solidArea.y,
                solidArea.width,
                solidArea.height
        );

        g2.setColor(oldColor);
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

        public Bullet build() {
            bullet.alive = true;
            bullet.distanceTraveled = 0;
            bullet.setVelocityFromDirection();
            return bullet;
        }
    }
}
