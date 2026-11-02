package object;

import entity.Player;
import main.GamePanel;
import story.ChapterText;
import javax.imageio.ImageIO;
import java.io.IOException;
import java.util.Objects;

public final class EllisNotebook extends SuperObject {
    public EllisNotebook() {
        name=ChapterText.get("ellis.satchel");
        try(var stream=Objects.requireNonNull(getClass().getResourceAsStream("/tiles/backpack.png"))) {
            image=ImageIO.read(stream);
        }catch(IOException e){throw new IllegalStateException(e);}
    }
    @Override public boolean isInteractable(){return true;}
    @Override public boolean onPlayerInteract(GamePanel gp,Player player) {
        if(!gp.village.findNotebook())return true;
        gp.playSoundEffect(audio.Sound.PAPER);
        gp.entities.removeObject(this);
        gp.dialogueManager.startBlocking(new dialogue.DialogueSpeaker("",null,new java.awt.Color(0xb49865)),ChapterText.lines("ellis.notebook.found"));
        return true;
    }
}
