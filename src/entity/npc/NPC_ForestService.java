package entity.npc;

import main.GamePanel;
import ui.ChoiceMenu;
import ui.UiSkin;

import story.ChapterText;
import java.awt.*;
import java.util.List;

public final class NPC_ForestService extends NPC_Villager {
    private final String service;
    public NPC_ForestService(GamePanel gp,String name,String sprite,Color color,String service){super(gp,name,sprite,color,false);this.service=service;}
    @Override public void speak(){
        if(gp.narrative.enabled()){gp.narrative.menu(this,this::talk,()->super.openService(),()->gp.forest.quest(service));return;}
        gp.choiceMenu.open(name,List.of(
            new ChoiceMenu.Option(ChapterText.get("interaction.talk"),"",this::talk),
            new ChoiceMenu.Option(ChapterText.get("interaction.quest"),"",()->gp.forest.quest(service)),
            new ChoiceMenu.Option(ChapterText.get("forest.service"),"",()->super.openService()),
            new ChoiceMenu.Option(ChapterText.get("interaction.back"),"",()->{})),true);
    }
    @Override public void draw(Graphics g){super.draw(g);int m=gp.forest.marker(service);if(m>0)UiSkin.PixelFont.draw((Graphics2D)g,m==1?"!":"?",worldX-gp.player.worldX+gp.player.screenX+18,worldY-gp.player.worldY+gp.player.screenY-20,2,m==2?UiSkin.TEXT_DIM:UiSkin.GOLD);}
}
