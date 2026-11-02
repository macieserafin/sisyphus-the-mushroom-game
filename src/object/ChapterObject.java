package object;

import entity.Player;
import main.GamePanel;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

public final class ChapterObject extends SuperObject {
    public enum Kind { ANCHOR, REST, NOTE, SIGN, DESCENT, WORKSHOP, VILLAGE_ANCHOR, BENCH, BED, REMAINS }
    private final Kind kind;
    private int tick;
    private boolean revealed, dormant;
    private BufferedImage moss, carved, repaired;
    public ChapterObject(Kind kind) {
        this.kind = kind;
        name = switch (kind) { case ANCHOR -> "Mycelium anchor"; case REST -> "Resting place";
            case NOTE -> "Traveller's note"; case SIGN -> "Traveller's seal"; case DESCENT -> "Descent"; case WORKSHOP -> "Mara's workshop";
            case VILLAGE_ANCHOR -> "Old stone"; case BENCH -> "Bench"; case BED -> "Bed"; case REMAINS -> "Broken core"; };
        collision = kind == Kind.VILLAGE_ANCHOR || kind == Kind.BENCH;
        if (kind == Kind.VILLAGE_ANCHOR) solidArea = new Rectangle(6, 24, 36, 24);
        if (kind == Kind.BENCH) solidArea = new Rectangle(0, 18, 96, 24);
        if (kind == Kind.BED) return;
        image = read(switch (kind) { case ANCHOR -> "anchor"; case REST -> "rest"; case WORKSHOP -> "workshop";
            case VILLAGE_ANCHOR -> "anchor_village"; case BENCH -> "bench_broken"; case REMAINS -> "king_core_broken"; default -> "traveller_note"; });
        if (kind == Kind.ANCHOR) { moss = read("stone_moss"); carved = read("stone_revealed"); }
        if (kind == Kind.BENCH) repaired = read("bench_repaired");
    }
    private static BufferedImage read(String name) {
        try (var stream = ChapterObject.class.getResourceAsStream("/objects/" + name + ".png")) {
            if (stream == null) throw new IllegalStateException("Missing chapter object artwork " + name);
            return ImageIO.read(stream);
        } catch (IOException e) { throw new IllegalStateException(e); }
    }
    public boolean isForestAnchor() { return kind == Kind.ANCHOR; }
    public void reveal() { revealed = true; }
    @Override public void update(GamePanel gp) { tick++; dormant = kind == Kind.ANCHOR && gp.narrative.enabled() && !gp.campaign.anchorOpen(); }
    @Override public boolean isInteractable() { return kind != Kind.REMAINS; }
    @Override public double interactionDistance(double x, double y, int tileSize) {
        if (kind != Kind.BENCH && kind != Kind.BED) return super.interactionDistance(x, y, tileSize);
        int width = tileSize * 2, height = tileSize;
        double xx = Math.max(worldX, Math.min(worldX + width, x)), yy = Math.max(worldY, Math.min(worldY + height, y));
        return Math.hypot(xx - x, yy - y);
    }
    public String interactionLabel(GamePanel gp) {
        if (gp.narrative.enabled()) {
            return switch (kind) {
                case ANCHOR, VILLAGE_ANCHOR -> gp.campaign.inCombat() ? "Anchor locked during combat" : gp.narrative.state.has("world.explained") ? "ENTER: Anchor" : "ENTER: Old stone";
                case BED -> "ENTER: Rest";
                default -> "ENTER: " + name;
            };
        }
        if (kind == Kind.ANCHOR) return gp.campaign.inCombat() ? "Anchor locked during combat" :
                gp.campaign.requiredRemaining() > 0 ? "Main encounters remaining: " + gp.campaign.requiredRemaining() : "ENTER: Bind return point / Save";
        return "ENTER: " + name;
    }
    @Override public boolean onPlayerInteract(GamePanel gp, Player player) {
        if (gp.campaign.inCombat()) { gp.ui.showMessage("In combat: interaction locked"); return true; }
        if (gp.narrative.enabled()) {
            switch (kind) {
                case ANCHOR -> { if (!gp.campaign.anchorOpen()) gp.narrative.play(gp.narrative.get("anchorScene")); else gp.chapterOne.anchorInteract(false); return true; }
                case VILLAGE_ANCHOR -> { gp.chapterOne.anchorInteract(true); return true; }
                case BED -> { gp.chapterOne.openBed(); return true; }
                case BENCH -> { if (!gp.chapterOne.benchRepaired()) gp.narrative.play("benchSeen"); return true; }
                default -> { }
            }
        }
        switch (kind) {
            case ANCHOR -> gp.campaign.bindAnchor();
            case REST -> { player.debugRestoreVitals();gp.saves.requestProgressSave();gp.playSoundEffect(audio.Sound.REST);gp.ui.showMessage("Rested. Health and shield restored"); }
            case NOTE -> gp.campaign.say(gp.campaign.floor() == 2 ? "floor2.arrival" : "note.forest", "");
            case SIGN -> gp.campaign.foundSign();
            case DESCENT -> gp.campaign.requestDescent();
            case WORKSHOP -> gp.campaign.say("warrior.built", story.ChapterText.get("warrior.name"));
            default -> { }
        }
        return true;
    }
    @Override public void draw(Graphics graphics, GamePanel gp) {
        int x = worldX - gp.player.worldX + gp.player.screenX, y = worldY - gp.player.worldY + gp.player.screenY;
        switch (kind) {
            case BED -> { return; }
            case REMAINS -> { graphics.drawImage(image, x - 24, y - 48, 96, 96, null); return; }
            case WORKSHOP -> { graphics.drawImage(image, x - 24, y - 48, 96, 96, null); return; }
            case VILLAGE_ANCHOR -> {
                graphics.drawImage(image, x - 24, y - 48, 96, 96, null);
                Graphics2D g = (Graphics2D) graphics;
                boolean lit = gp.narrative.state.has("world.explained");
                g.setColor(lit ? new Color(0x91C6AD) : new Color(0x628F85));
                if (!lit || gp.visuals.reduceMotion() || tick / 25 % 2 == 0) g.fillRect(x + 21, y - 15, 6, 12);
                return;
            }
            case BENCH -> { graphics.drawImage(gp.chapterOne.benchRepaired() ? repaired : image, x, y, 96, 48, null); return; }
            case ANCHOR -> {
                boolean dormant = gp.narrative.enabled() && !gp.campaign.anchorOpen();
                graphics.drawImage(dormant ? revealed ? carved : moss : image, x, y, gp.tileSize, gp.tileSize, null);
                if (dormant) return;
                Graphics2D g = (Graphics2D) graphics;
                g.setColor(gp.campaign.anchorOpen() ? new Color(0x91C6AD) : new Color(0x736D7D));
                if (!gp.visuals.reduceMotion() && tick/25%2==0 || gp.visuals.reduceMotion()) g.fillRect(x+21,y+15,6,9);
                return;
            }
            default -> super.draw(graphics, gp);
        }
    }
    @Override public void addLights(world.render.Lighting lighting) {
        if (kind==Kind.ANCHOR && !revealed && !dormant) lighting.add(worldX+24,worldY+24,90,new Color(0x91C6AD),0.65f,0.04f);
        if (kind==Kind.ANCHOR && revealed) lighting.add(worldX+24,worldY+24,110,new Color(0x91C6AD),0.8f,0.05f);
        if (kind==Kind.REMAINS) lighting.add(worldX+24,worldY,160,new Color(0xDEAE4C),1.0f,0.06f);
        if (kind==Kind.VILLAGE_ANCHOR) lighting.add(worldX+24,worldY-6,110,new Color(0x91C6AD),0.55f,0.04f);
    }
}
