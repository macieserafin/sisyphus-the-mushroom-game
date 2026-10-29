package world;

import entity.Bullet;
import entity.combat.Faction;
import entity.combat.HitResult;
import entity.enemy.Enemy;
import main.GamePanel;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class BulletManager {

    private final GamePanel gp;
    private final List<Bullet> bullets = new ArrayList<>();

    public BulletManager(GamePanel gp) {
        this.gp = gp;
    }

    public void addBullet(Bullet bullet) {
        bullets.add(bullet);
    }

    public void clearBullets() {
        bullets.clear();
    }

    public int cutEnemyBullets(Shape area) {
        int cut = 0;
        Iterator<Bullet> iterator = bullets.iterator();

        while (iterator.hasNext()) {
            Bullet bullet = iterator.next();
            if (bullet.alive && bullet.getFaction() == Faction.ENEMY && area.intersects(bullet.getWorldSolidArea())) {
                Rectangle box = bullet.getWorldSolidArea();
                gp.effects.sparks(box.getCenterX(), box.getCenterY(), -bullet.velX, -bullet.velY, 5, EffectsManager.SPARK);
                bullet.alive = false;
                iterator.remove();
                cut++;
            }
        }
        return cut;
    }

    public void update() {
        Iterator<Bullet> iterator = bullets.iterator();

        while (iterator.hasNext()) {
            Bullet bullet = iterator.next();

            bullet.update(gp);
            resolveBulletHits(bullet);

            if (!bullet.alive) {
                iterator.remove();
            }
        }
    }

    private void resolveBulletHits(Bullet bullet) {
        if (!bullet.alive) {
            return;
        }

        Rectangle bulletBox = bullet.getWorldSolidArea();

        for (Enemy enemy : gp.entities.enemies()) {
            if (enemy.alive && bullet.canHit(enemy) && bulletBox.intersects(enemy.getWorldHitBox())) {
                HitResult result = enemy.receiveHit(bullet.createHit());
                bullet.reactToHitResult(result);
                if (!bullet.alive || result != HitResult.IGNORED) {
                    return;
                }
            }
        }

        if (bullet.alive && bullet.canHit(gp.player) && bulletBox.intersects(gp.player.getWorldHitBox())) {
            HitResult result = gp.player.receiveHit(bullet.createHit());
            bullet.reactToHitResult(result);
        }
    }

    public void draw(Graphics2D g2) {
        for (Bullet bullet : bullets) {
            int screenX = bullet.worldX - gp.player.worldX + gp.player.screenX;
            int screenY = bullet.worldY - gp.player.worldY + gp.player.screenY;

            bullet.draw(g2, screenX, screenY);
        }
    }
}
