package ui;

import main.GamePanel;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import javax.imageio.ImageIO;

public final class NarrativeView {
    private final GamePanel gp;
    private String title = "", body = "";
    private int lock, remaining;
    private Runnable confirmed;
    private BufferedImage document;
    private String documentTitle = "", documentText = "";
    private int page, pageCount = 1, readLock;
    private boolean reading;
    private boolean illustrated=true;
    private Runnable readDone;
    private final Map<String, BufferedImage> images = new HashMap<>();
    public NarrativeView(GamePanel gp) { this.gp = gp; }
    public boolean modal() { return confirmed != null; }
    public boolean documentVisible() { return document != null || !documentText.isBlank(); }
    public boolean reading() { return reading; }
    public void show(String title, String body, int frames, Runnable confirmed) {
        this.title = title; this.body = body; this.confirmed = confirmed;
        lock = 20; remaining = frames>0?Math.max(21,frames):0; gp.keyH.clearGameplayInput();
    }
    public void tick() {
        if (lock > 0) lock--;
        if (readLock > 0) readLock--;
        if (modal() && remaining > 0 && --remaining == 0) confirm();
    }
    public void confirm() {
        if (!modal() || lock > 0) return;
        Runnable action = confirmed; confirmed = null; action.run(); gp.keyH.clearGameplayInput();
    }
    public void document(String path) { document(path, "", ""); }
    public void document(String path, String title, String text) {
        illustrated=true;
        document = path == null || path.isBlank() ? null : image(path);
        documentTitle = title == null ? "" : title;
        documentText = text == null ? "" : text;
        page = 0; pageCount = 1;
        if (document == null && documentText.isBlank()) { reading = false; readDone = null; }
    }
    public void illustration(boolean visible) { illustrated=visible;paginate(); }
    public void read(String path, String title, String text, Runnable done) {
        document(path, title, text);
        reading = true; paginate(); readDone = done; readLock = 15; gp.keyH.clearGameplayInput();
    }
    public void page(int delta) {
        if (!documentVisible() || readLock > 0) return;
        paginate();
        int next = Math.max(0, Math.min(pageCount - 1, page + delta));
        if (next != page) { page = next; gp.playSoundEffect(audio.Sound.PAPER); }
    }
    public void readConfirm() { readConfirm(false); }
    public void closeDocument() {
        if (!documentVisible()) return;
        Runnable done = readDone;
        document("");
        readLock = 0;
        if (done != null) done.run();
        gp.keyH.clearGameplayInput();
    }
    public void readConfirm(boolean close) {
        if (!reading || readLock > 0) return;
        paginate();
        if (!close && page < pageCount - 1) { page(1); readLock = 8; return; }
        reading = false; readLock = 8;
        Runnable done = readDone; readDone = null;
        if (done != null) done.run();
        gp.keyH.clearGameplayInput();
    }
    private BufferedImage image(String path) {
        return images.computeIfAbsent(path, p -> {
            try (var in = getClass().getClassLoader().getResourceAsStream(p)) {
                if (in == null) throw new IllegalArgumentException("Missing document: " + p);
                return ImageIO.read(in);
            } catch (java.io.IOException e) { throw new IllegalStateException(e); }
        });
    }
    public void reset() { confirmed = null; document = null; documentTitle = ""; documentText = ""; reading = false; readDone = null; page = 0; }
    public void draw(Graphics2D g) {
        if (documentVisible()) drawDocument(g);
        if (!modal()) return;
        g.setColor(new Color(4,9,13,210)); g.fillRect(0,0,gp.screenWidth,gp.screenHeight);
        int w = Math.min(620,gp.screenWidth-60), x=(gp.screenWidth-w)/2, y=gp.screenHeight/2-120;
        UiSkin.frame(g,x,y,w,240);
        g.setFont(UiFonts.of(Font.BOLD,21)); g.setColor(UiSkin.GOLD); g.drawString(title,x+25,y+38);
        g.setFont(UiFonts.of(Font.PLAIN,16)); g.setColor(UiSkin.TEXT);
        int lineY=y+75;
        for(String line:wrap(g,body,w-50)) { if(line.isEmpty()){lineY+=6;continue;} g.drawString(line,x+25,lineY);lineY+=22; }
        g.setFont(UiFonts.of(Font.BOLD,12)); g.setColor(UiSkin.GOLD);
        g.drawString("ENTER  CONTINUE",x+25,y+215);
    }
    private int paperWidth() { return Math.min(760,gp.screenWidth-48); }
    private int paperBottom() { return reading ? gp.screenHeight-22 : gp.screenHeight-gp.ui.dialogueReserve()-12; }
    private int illustrationWidth() { return document!=null && illustrated ? paperWidth()/3 : 0; }
    private List<String> documentLines(Graphics2D g) {
        g.setFont(new Font(Font.SERIF,Font.PLAIN,18));
        return wrap(g,documentText,paperWidth()-72-illustrationWidth());
    }
    private int linesPerPage() { return Math.max(2,(paperBottom()-24-118)/25); }
    private void paginate() {
        if(documentText.isBlank()) { pageCount=1;return; }
        Graphics2D g=new BufferedImage(1,1,BufferedImage.TYPE_INT_RGB).createGraphics();
        int count=documentLines(g).size();g.dispose();
        pageCount=Math.max(1,(count+linesPerPage()-1)/linesPerPage());
        page=Math.min(page,pageCount-1);
    }
    private void drawDocument(Graphics2D original) {
        Graphics2D g=(Graphics2D)original.create();
        try {
            g.setColor(new Color(5,10,12,185));g.fillRect(0,0,gp.screenWidth,gp.screenHeight);
            int x=(gp.screenWidth-paperWidth())/2,y=24,w=paperWidth(),h=paperBottom()-y;
            g.setColor(new Color(0,0,0,100));g.fillRoundRect(x+7,y+8,w,h,8,8);
            g.setPaint(new GradientPaint(x,y,new Color(240,224,184),x+w,y+h,new Color(210,187,139)));
            g.fillRect(x,y,w,h);
            g.setColor(new Color(115,88,51,100));g.drawRect(x+5,y+5,w-11,h-11);
            g.setColor(new Color(133,100,62,45));
            for(int row=y+12;row<y+h-8;row+=9)g.drawLine(x+9,row,x+w-10,row);
            g.setColor(new Color(75,51,33));g.setFont(new Font(Font.SERIF,Font.BOLD,22));
            g.drawString(documentTitle,x+36,y+38);
            g.setColor(new Color(117,84,46,140));g.drawLine(x+36,y+53,x+w-36,y+53);
            int imageW=illustrationWidth(),textX=x+36+imageW;
            if(document!=null && (imageW>0 || documentText.isBlank())) {
                int areaW=documentText.isBlank()?w-72:imageW-18,areaH=h-115;
                double scale=Math.min(areaW/(double)document.getWidth(),areaH/(double)document.getHeight());
                int iw=(int)(document.getWidth()*scale),ih=(int)(document.getHeight()*scale);
                g.drawImage(document,x+36+(areaW-iw)/2,y+68+(areaH-ih)/2,iw,ih,null);
            }
            if(!documentText.isBlank()) {
                List<String> lines=documentLines(g);int perPage=linesPerPage();
                pageCount=Math.max(1,(lines.size()+perPage-1)/perPage);page=Math.min(page,pageCount-1);
                g.setColor(new Color(59,43,30));int lineY=y+86;
                for(String line:lines.subList(page*perPage,Math.min(lines.size(),(page+1)*perPage))) {
                    g.drawString(line,textX,lineY);lineY+=25;
                }
            }
            g.setColor(new Color(117,84,46,140));g.drawLine(x+36,y+h-39,x+w-36,y+h-39);
            g.setFont(UiFonts.of(Font.BOLD,11));g.setColor(new Color(74,53,34));
            String hint=(pageCount>1?"PAGE "+(page+1)+" / "+pageCount+"   W/S  TURN   ":"")
                    +(reading?(page<pageCount-1?"ENTER  NEXT":"ENTER  DONE")+"   ":"")+"ESC  CLOSE";
            g.drawString(hint,x+36,y+h-19);
        } finally {g.dispose();}
    }
    private static List<String> wrap(Graphics2D g, String text, int width) {
        List<String> out = new ArrayList<>();
        FontMetrics metrics = g.getFontMetrics();
        String[] paragraphs = text.split("\\|");
        for (int i = 0; i < paragraphs.length; i++) {
            String line = "";
            for (String word : paragraphs[i].trim().split(" ")) {
                String next = line.isEmpty() ? word : line + " " + word;
                if (metrics.stringWidth(next) > width && !line.isEmpty()) { out.add(line); line = word; } else line = next;
            }
            out.add(line);
            if (i < paragraphs.length - 1) out.add("");
        }
        return out;
    }
}
