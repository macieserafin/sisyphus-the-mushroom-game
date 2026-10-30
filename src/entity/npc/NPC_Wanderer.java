package entity.npc;

import main.GamePanel;
import story.ChapterText;
import java.awt.Color;

public final class NPC_Wanderer extends NPC_Villager {
    public NPC_Wanderer(GamePanel gp) {
        super(gp, ChapterText.get("warrior.name"), "wanderer_mara", new Color(0x91C6AD), false);
    }
    @Override public void speak() { gp.campaign.talkWarrior(); }
}
