package entity.npc;

import main.GamePanel;
import ui.ChoiceMenu;
import level.Campaign;
import story.ChapterText;
import tile.MapPatch;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.List;

public final class NPC_Ellis extends NPC_Villager {
    public NPC_Ellis(GamePanel gp) {
        super(gp,ChapterText.get("ellis.name"),"ellis",new Color(0xb49865),false);
        conversation(ChapterText.lines("ellis.settled.talk").toArray(String[]::new));
    }

    private void say(String key) {
        gp.dialogueManager.startBlocking(dialogueSpeaker(),ChapterText.lines(key));
    }

    @Override public void speak() {
        if(gp.village.ellisSettled()){super.talk();return;}
        gp.choiceMenu.open(ChapterText.get("ellis.name"),List.of(
                new ChoiceMenu.Option(ChapterText.get("interaction.talk"),ChapterText.get("ellis.talk.hint"),()->say("ellis.forest.talk")),
                new ChoiceMenu.Option(ChapterText.get("interaction.quest"),ChapterText.get(gp.village.notebookFound()?"ellis.return.hint":"ellis.quest.hint"),this::quest),
                new ChoiceMenu.Option(ChapterText.get("interaction.back"),"",()->{})),true);
    }

    private void quest() {
        if(gp.narrative.enabled()) {
            boolean found=gp.village.notebookFound();
            gp.narrative.dialogue("service.ellis."+(found?"return":"request"),dialogueSpeaker(),
                    ChapterText.lines(found?"ellis.return":"ellis.request"),()->{
                gp.narrative.event("talk.ellis");
                if(found) {
                    gp.village.settleEllisWithoutSave();gp.entities.removeNPC(this);
                    if(Campaign.FOREST.equals(gp.tileM.currentMapPath()))MapPatch.apply(gp.tileM,"maps/ellis_forest_empty.patch");
                } else gp.village.askEllisWithoutSave();
            });
            return;
        }
        if(gp.village.notebookFound()) {
            if(!gp.village.settleEllis())return;
            say("ellis.return");
            gp.entities.removeNPC(this);
            if(Campaign.FOREST.equals(gp.tileM.currentMapPath()))MapPatch.apply(gp.tileM,"maps/ellis_forest_empty.patch");
        } else if(gp.village.askEllis())say("ellis.request");
    }

    @Override public void draw(Graphics graphics) {
        super.draw(graphics);
        if(gp.village.ellisSettled())return;
        Graphics2D g=(Graphics2D)graphics.create();
        Color color=gp.village.notebookFound()||!gp.village.ellisAsked()?ui.UiSkin.GOLD:ui.UiSkin.TEXT_DIM;
        ui.UiSkin.PixelFont.draw(g,gp.village.ellisAsked()?"?":"!",
                worldX-gp.player.worldX+gp.player.screenX+18,
                worldY-gp.player.worldY+gp.player.screenY-20,2,color);
        g.dispose();
    }
}
