package main;

import entity.Entity;

import java.awt.*;

public class CollisionChecker {
    GamePanel gp;
    public CollisionChecker(GamePanel gp) {
        this.gp = gp;
    }

    private boolean isOutsideMap(int col, int row) {
        return col < 0 || col >= gp.tileM.getWorldCols() || row < 0 || row >= gp.tileM.getWorldRows();
    }

    private Rectangle getEntitySolidAreaAt(Entity entity, int worldX, int worldY) {
        return new Rectangle(
                worldX + entity.solidAreaDefaultX,
                worldY + entity.solidAreaDefaultY,
                entity.solidArea.width,
                entity.solidArea.height
        );
    }

    public boolean checkTileAt(Entity entity, int worldX, int worldY) {
        Rectangle area = getEntitySolidAreaAt(entity, worldX, worldY);

        int leftCol = Math.floorDiv(area.x, gp.tileSize);
        int rightCol = Math.floorDiv(area.x + area.width - 1, gp.tileSize);
        int topRow = Math.floorDiv(area.y, gp.tileSize);
        int bottomRow = Math.floorDiv(area.y + area.height - 1, gp.tileSize);

        if (isOutsideMap(leftCol, topRow) ||
                isOutsideMap(rightCol, topRow) ||
                isOutsideMap(leftCol, bottomRow) ||
                isOutsideMap(rightCol, bottomRow)) {
            return true;
        }

        return gp.tileM.tile[gp.tileM.mapTileNum[leftCol][topRow]].collision ||
                gp.tileM.tile[gp.tileM.mapTileNum[rightCol][topRow]].collision ||
                gp.tileM.tile[gp.tileM.mapTileNum[leftCol][bottomRow]].collision ||
                gp.tileM.tile[gp.tileM.mapTileNum[rightCol][bottomRow]].collision;
    }

    public boolean checkObjectAt(Entity entity, int worldX, int worldY) {
        return checkObjectAtIndex(entity, worldX, worldY, true) != 999;
    }

    public int checkObjectAtIndex(Entity entity, int worldX, int worldY, boolean collisionOnly) {
        Rectangle entityArea = getEntitySolidAreaAt(entity, worldX, worldY);

        for (int i = 0; i < gp.obj.length; i++) {
            if (gp.obj[i] != null && (!collisionOnly || gp.obj[i].collision)) {
                Rectangle objectArea = new Rectangle(
                        gp.obj[i].worldX + gp.obj[i].solidAreaDefaultX,
                        gp.obj[i].worldY + gp.obj[i].solidAreaDefaultY,
                        gp.obj[i].solidArea.width,
                        gp.obj[i].solidArea.height
                );

                if (entityArea.intersects(objectArea)) {
                    return i;
                }
            }
        }

        return 999;
    }

    public boolean checkEntityAt(Entity entity, Entity[] target, int worldX, int worldY) {
        return checkEntityAtIndex(entity, target, worldX, worldY) != 999;
    }

    public int checkEntityAtIndex(Entity entity, Entity[] target, int worldX, int worldY) {
        Rectangle entityArea = getEntitySolidAreaAt(entity, worldX, worldY);

        for (int i = 0; i < target.length; i++) {
            if (target[i] != null && target[i] != entity) {
                Rectangle targetArea = getEntitySolidAreaAt(target[i], target[i].worldX, target[i].worldY);

                if (entityArea.intersects(targetArea)) {
                    return i;
                }
            }
        }

        return 999;
    }

    public int checkObjectTouch(Entity entity) {
        Rectangle entityArea = getEntitySolidAreaAt(entity, entity.worldX, entity.worldY);

        for (int i = 0; i < gp.obj.length; i++) {
            if (gp.obj[i] != null) {
                Rectangle objectArea = new Rectangle(
                        gp.obj[i].worldX + gp.obj[i].solidAreaDefaultX,
                        gp.obj[i].worldY + gp.obj[i].solidAreaDefaultY,
                        gp.obj[i].solidArea.width,
                        gp.obj[i].solidArea.height
                );

                if (entityArea.intersects(objectArea)) {
                    return i;
                }
            }
        }

        return 999;
    }

    public int checkEntityTouch(Entity entity, Entity[] target) {
        Rectangle entityArea = getEntitySolidAreaAt(entity, entity.worldX, entity.worldY);

        for (int i = 0; i < target.length; i++) {
            if (target[i] != null && target[i] != entity) {
                Rectangle targetArea = getEntitySolidAreaAt(target[i], target[i].worldX, target[i].worldY);

                if (entityArea.intersects(targetArea)) {
                    return i;
                }
            }
        }

        return 999;
    }

    public boolean isPlayerPositionBlocked(Entity entity, int worldX, int worldY) {
        return checkTileAt(entity, worldX, worldY) ||
                checkObjectAt(entity, worldX, worldY) ||
                checkEntityAt(entity, gp.npc, worldX, worldY) ||
                checkEntityAt(entity, gp.enemy, worldX, worldY);
    }

    public boolean checkPlayerAt(Entity entity, int worldX, int worldY) {
        Rectangle entityArea = getEntitySolidAreaAt(entity, worldX, worldY);
        Rectangle playerArea = getEntitySolidAreaAt(gp.player, gp.player.worldX, gp.player.worldY);

        return entityArea.intersects(playerArea);
    }

    public void checkTile(Entity entity){
        int entityLeftWorldX = entity.worldX + entity.solidArea.x;
        int entityRightWorldX = entity.worldX + entity.solidArea.x + entity.solidArea.width;
        int entityTopWorldY = entity.worldY + entity.solidArea.y;
        int entityBottomWorldY = entity.worldY + entity.solidArea.y + entity.solidArea.height;

        int entityLeftCol = entityLeftWorldX/gp.tileSize;
        int entityRightCol = entityRightWorldX/gp.tileSize;
        int entityTopRow = entityTopWorldY/gp.tileSize;
        int entityBottomRow = entityBottomWorldY/gp.tileSize;

        int tileNum1, tileNum2;

        switch(entity.direction) {
            case "up":
                entityTopRow = (entityTopWorldY - entity.speed)/gp.tileSize;
                if (isOutsideMap(entityLeftCol, entityTopRow) || isOutsideMap(entityRightCol, entityTopRow)) {
                    entity.collisionOn = true;
                    break;
                }
                tileNum1 = gp.tileM.mapTileNum[entityLeftCol][entityTopRow];
                tileNum2 = gp.tileM.mapTileNum[entityRightCol][entityTopRow];
                if(gp.tileM.tile[tileNum1].collision || gp.tileM.tile[tileNum2].collision)entity.collisionOn = true;
                break;
                case "down":
                    entityBottomRow = (entityBottomWorldY + entity.speed)/gp.tileSize;
                    if (isOutsideMap(entityLeftCol, entityBottomRow) || isOutsideMap(entityRightCol, entityBottomRow)) {
                        entity.collisionOn = true;
                        break;
                    }
                    tileNum1 = gp.tileM.mapTileNum[entityLeftCol][entityBottomRow];
                    tileNum2 = gp.tileM.mapTileNum[entityRightCol][entityBottomRow];
                    if(gp.tileM.tile[tileNum1].collision || gp.tileM.tile[tileNum2].collision)entity.collisionOn = true;
                    break;
                    case "left":
                        entityLeftCol = (entityLeftWorldX - entity.speed)/gp.tileSize;
                        if (isOutsideMap(entityLeftCol, entityTopRow) || isOutsideMap(entityLeftCol, entityBottomRow)) {
                            entity.collisionOn = true;
                            break;
                        }
                        tileNum1 = gp.tileM.mapTileNum[entityLeftCol][entityTopRow];
                        tileNum2 = gp.tileM.mapTileNum[entityLeftCol][entityBottomRow];
                        if(gp.tileM.tile[tileNum1].collision || gp.tileM.tile[tileNum2].collision)entity.collisionOn = true;
                        break;
                        case "right":
                            entityRightCol = (entityRightWorldX + entity.speed)/gp.tileSize;
                            if (isOutsideMap(entityRightCol, entityTopRow) || isOutsideMap(entityRightCol, entityBottomRow)) {
                                entity.collisionOn = true;
                                break;
                            }
                            tileNum1 = gp.tileM.mapTileNum[entityRightCol][entityTopRow];
                            tileNum2 = gp.tileM.mapTileNum[entityRightCol][entityBottomRow];
                            if(gp.tileM.tile[tileNum1].collision || gp.tileM.tile[tileNum2].collision)entity.collisionOn = true;
                            break;
        }
    }

    public int checkObject(Entity entity, boolean player) {
        int index = 999;

        for (int i = 0; i < gp.obj.length; i++) {

            if (gp.obj[i] != null) {

                entity.solidArea.x = entity.worldX + entity.solidAreaDefaultX;
                entity.solidArea.y = entity.worldY + entity.solidAreaDefaultY;

                gp.obj[i].solidArea.x = gp.obj[i].worldX + gp.obj[i].solidAreaDefaultX;
                gp.obj[i].solidArea.y = gp.obj[i].worldY + gp.obj[i].solidAreaDefaultY;

                switch (entity.direction) {
                    case "up":    entity.solidArea.y -= entity.speed; break;
                    case "down":  entity.solidArea.y += entity.speed; break;
                    case "left":  entity.solidArea.x -= entity.speed; break;
                    case "right": entity.solidArea.x += entity.speed; break;
                }

                if (entity.solidArea.intersects(gp.obj[i].solidArea)) {
                    if (gp.obj[i].collision) {
                        entity.collisionOn = true;
                    }
                    if (player) {
                        index = i;
                    }
                }

                entity.solidArea.x = entity.solidAreaDefaultX;
                entity.solidArea.y = entity.solidAreaDefaultY;
                gp.obj[i].solidArea.x = gp.obj[i].solidAreaDefaultX;
                gp.obj[i].solidArea.y = gp.obj[i].solidAreaDefaultY;
            }
        }

        return index;
    }

    public int checkEntity(Entity entity, Entity[] target) {
        int index = 999;

        for (int i = 0; i < target.length; i++) {

            if (target[i] != null) {

                entity.solidArea.x = entity.worldX + entity.solidAreaDefaultX;
                entity.solidArea.y = entity.worldY + entity.solidAreaDefaultY;

                target[i].solidArea.x = target[i].worldX + target[i].solidAreaDefaultX;
                target[i].solidArea.y = target[i].worldY + target[i].solidAreaDefaultY;

                switch (entity.direction) {
                    case "up":    entity.solidArea.y -= entity.speed; break;
                    case "down":  entity.solidArea.y += entity.speed; break;
                    case "left":  entity.solidArea.x -= entity.speed; break;
                    case "right": entity.solidArea.x += entity.speed; break;
                }

                if (entity.solidArea.intersects(target[i].solidArea)) {
                        entity.collisionOn = true;
                        index = i;

                }

                entity.solidArea.x = entity.solidAreaDefaultX;
                entity.solidArea.y = entity.solidAreaDefaultY;
                target[i].solidArea.x = target[i].solidAreaDefaultX;
                target[i].solidArea.y = target[i].solidAreaDefaultY;
            }
        }

        return index;
    }

    public void checkPlayer(Entity entity) {
        entity.solidArea.x = entity.worldX + entity.solidAreaDefaultX;
        entity.solidArea.y = entity.worldY + entity.solidAreaDefaultY;

        gp.player.solidArea.x = gp.player.worldX + gp.player.solidAreaDefaultX;
        gp.player.solidArea.y = gp.player.worldY + gp.player.solidAreaDefaultY;

        switch (entity.direction) {
            case "up":    entity.solidArea.y -= entity.speed; break;
            case "down":  entity.solidArea.y += entity.speed; break;
            case "left":  entity.solidArea.x -= entity.speed; break;
            case "right": entity.solidArea.x += entity.speed; break;
        }

        if (entity.solidArea.intersects(gp.player.solidArea)) {
            entity.collisionOn = true;
        }

        entity.solidArea.x = entity.solidAreaDefaultX;
        entity.solidArea.y = entity.solidAreaDefaultY;
        gp.player.solidArea.x = gp.player.solidAreaDefaultX;
        gp.player.solidArea.y = gp.player.solidAreaDefaultY;
    }
}
