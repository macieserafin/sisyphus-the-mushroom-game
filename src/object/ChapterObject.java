package object;

import entity.Player;
import main.GamePanel;
import javax.imageio.ImageIO;
import java.awt.*;
import java.io.IOException;

public final class ChapterObject extends SuperObject {
    public enum Kind { ANCHOR, REST, NOTE, SIGN, DESCENT, WORKSHOP }
    private final Kind kind;
    private int tick;
    public ChapterObject(Kind kind) {
        this.kind = kind;
        name = switch (kind) { case ANCHOR -> "Mycelium anchor"; case REST -> "Resting place";
            case NOTE -> "Traveller's note"; case SIGN -> "Traveller's seal"; case DESCENT -> "Descent"; case WORKSHOP -> "Mara's workshop"; };
        collision = false;
        try (var stream = getClass().getResourceAsStream("/objects/" + (kind == Kind.ANCHOR ? "anchor" : kind == Kind.REST ? "rest" : kind == Kind.WORKSHOP ? "workshop" : "traveller_note") + ".png")) {
            if (stream == null) throw new IllegalStateException("Missing chapter object artwork");
            image = ImageIO.read(stream);
        } catch (IOException e) { throw new IllegalStateException(e); }
    }
    @Override public void update(GamePanel gp) { tick++; }
    @Override public boolean isInteractable() { return true; }
    public String interactionLabel(GamePanel gp) {
        if (kind == Kind.ANCHOR) return gp.campaign.inCombat() ? "Anchor locked during combat" :
                gp.campaign.requiredRemaining() > 0 ? "Main encounters remaining: " + gp.campaign.requiredRemaining() : "ENTER: Bind pattern / Save";
        return "ENTER: " + name;
    }
    @Override public boolean onPlayerInteract(GamePanel gp, Player player) {
        if (gp.campaign.inCombat()) { gp.ui.showMessage("In combat: interaction locked"); return true; }
        switch (kind) {
            case ANCHOR -> gp.campaign.bindAnchor();
            case REST -> { player.debugRestoreVitals(); gp.ui.showMessage("Rested. Health and shield restored. Bind at the anchor to save"); }
            case NOTE -> gp.campaign.say(gp.campaign.floor() == 2 ? "floor2.arrival" : "note.forest", "");
            case SIGN -> gp.campaign.foundSign();
            case DESCENT -> gp.campaign.requestDescent();
            case WORKSHOP -> gp.campaign.say("warrior.built", story.ChapterText.get("warrior.name"));
        }
        return true;
    }
    @Override public void draw(Graphics graphics, GamePanel gp) {
        if (kind == Kind.WORKSHOP) {
            graphics.drawImage(image, worldX-gp.player.worldX+gp.player.screenX-24,
                    worldY-gp.player.worldY+gp.player.screenY-48, 96, 96, null);
        } else super.draw(graphics,gp);
        if (kind != Kind.ANCHOR) return;
        int x=worldX-gp.player.worldX+gp.player.screenX, y=worldY-gp.player.worldY+gp.player.screenY;
        Graphics2D g=(Graphics2D)graphics;
        g.setColor(gp.campaign.anchorOpen() ? new Color(0x91C6AD) : new Color(0x736D7D));
        if (!gp.visuals.reduceMotion() && tick/25%2==0 || gp.visuals.reduceMotion()) g.fillRect(x+21,y+15,6,9);
    }
    @Override public void addLights(world.Lighting lighting) {
        if (kind==Kind.ANCHOR) lighting.add(worldX+24,worldY+24,90,new Color(0x91C6AD),0.65f,0.04f);
    }
}
