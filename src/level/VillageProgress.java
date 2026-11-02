package level;

import main.GamePanel;
import story.ChapterText;
import story.QuestLog;
import save.Values;
import java.util.Properties;

public final class VillageProgress {
    private final GamePanel gp;
    private boolean ellisAsked, notebookFound, ellisSettled;
    private Boolean previewSettled;

    public VillageProgress(GamePanel gp) { this.gp = gp; }
    public boolean ellisAsked() { return ellisAsked; }
    public boolean notebookFound() { return notebookFound; }
    public boolean ellisSettled() { return previewSettled != null ? previewSettled : ellisSettled; }

    public void save(Properties data) {
        data.setProperty("village.ellis.asked", Boolean.toString(ellisAsked));
        data.setProperty("village.ellis.notebook", Boolean.toString(notebookFound));
        data.setProperty("village.ellis.settled", Boolean.toString(ellisSettled));
    }

    public void load(Properties data) {
        previewSettled = null;
        ellisSettled = Values.bool(data, "village.ellis.settled");
        notebookFound = ellisSettled || Values.bool(data, "village.ellis.notebook");
        ellisAsked = ellisSettled || Values.bool(data, "village.ellis.asked") || notebookFound && !data.containsKey("village.ellis.asked");
        syncQuest();
    }

    private void syncQuest() {
        if (ellisAsked) gp.questLog.start(QuestLog.ELLIS);
        if (ellisSettled) gp.questLog.complete(QuestLog.ELLIS);
    }

    private boolean persist(boolean asked, boolean found, boolean settled) {
        boolean a=ellisAsked, f=notebookFound, s=ellisSettled;
        ellisAsked=asked; notebookFound=found; ellisSettled=settled;
        if (!gp.saves.saveVillageProgress()) {
            ellisAsked=a; notebookFound=f; ellisSettled=s;
            gp.ui.showMessage(ChapterText.get("ellis.save.failed"));
            return false;
        }
        previewSettled = null;
        syncQuest();
        return true;
    }

    public boolean askEllis() { return persist(true,notebookFound,ellisSettled); }
    public void askEllisWithoutSave() { ellisAsked=true;syncQuest(); }
    public void settleEllisWithoutSave() { if(notebookFound){ellisAsked=ellisSettled=true;previewSettled=null;syncQuest();} }
    public void findNotebookWithoutSave() { notebookFound = true; previewSettled = null; syncQuest(); }
    public boolean findNotebook() { return persist(ellisAsked,true,ellisSettled); }
    public boolean settleEllis() { return notebookFound && persist(true,true,true); }

    public void debugEllisState(boolean asked, boolean found, boolean settled) {
        ellisAsked = asked; notebookFound = found; ellisSettled = settled; previewSettled = null; syncQuest();
    }

    public void debugEllis(boolean settled) {
        previewSettled=settled;
    }
}
