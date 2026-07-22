package main;

import entity.Bullet;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Iterator;

public class BulletManager {

    private GamePanel gp;
    private ArrayList<Bullet> bullets = new ArrayList<Bullet>();

    public BulletManager(GamePanel gp) {
        this.gp = gp;
    }

    public void addBullet(Bullet bullet) {
        bullets.add(bullet);
    }

    public void update() {

        Iterator<Bullet> iterator = bullets.iterator();

        while (iterator.hasNext()) {
            Bullet bullet = iterator.next();

            bullet.update(gp);
            checkEnemyHit(bullet);

            if (!bullet.alive) {
                iterator.remove();
            }
        }
    }

    private void checkEnemyHit(Bullet bullet) {
        if (!bullet.alive) {
            return;
        }

        Rectangle bulletBox = bullet.getWorldSolidArea();

        for (int i = 0; i < gp.enemy.length; i++) {
            if (gp.enemy[i] != null && gp.enemy[i].alive) {
                if (bulletBox.intersects(gp.enemy[i].getWorldHitBox())) {
                    gp.enemy[i].takeDamage(bullet.damage);
                    bullet.alive = false;
                    return;
                }
            }
        }
    }

    public void draw(Graphics2D g2) {

        for (int i = 0; i < bullets.size(); i++) {
            Bullet bullet = bullets.get(i);

            int screenX = bullet.worldX - gp.player.worldX + gp.player.screenX;
            int screenY = bullet.worldY - gp.player.worldY + gp.player.screenY;

            bullet.draw(g2, screenX, screenY);
        }
    }
}
