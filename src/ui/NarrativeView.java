package ui;

import main.GamePanel;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import javax.imageio.ImageIO;

/** A document may stay visible across dialogue turns; notices wait for confirmation. */
public final class NarrativeView {
    private final GamePanel gp;
    private String title = "", body = "";
    private int lock, remaining;
    private Runnable confirmed;
    private BufferedImage document;
    private final Map<String, BufferedImage> images = new HashMap<>();
    public NarrativeView(GamePanel gp) { this.gp = gp; }
    public boolean modal() { return confirmed != null; }
    public boolean documentVisible() { return document != null; }
    public void show(String title, String body, int frames, Runnable confirmed) {
        this.title = title; this.body = body; this.confirmed = confirmed;
        lock = 20; remaining = frames>0?Math.max(21,frames):0; gp.keyH.clearGameplayInput();
    }
    public void tick() {
        if (lock > 0) lock--;
        if (modal() && remaining > 0 && --remaining == 0) confirm();
    }
    public void confirm() {
        if (!modal() || lock > 0) return;
        Runnable action = confirmed; confirmed = null; action.run(); gp.keyH.clearGameplayInput();
    }
    public void document(String path) {
        if (path.isBlank()) { document = null; return; }
        document = images.computeIfAbsent(path, p -> {
            try (var in = getClass().getClassLoader().getResourceAsStream(p)) {
                if (in == null) throw new IllegalArgumentException("Missing document: " + p);
                return ImageIO.read(in);
            } catch (java.io.IOException e) { throw new IllegalStateException(e); }
        });
    }
    public void reset() { confirmed = null; document = null; }
    public void draw(Graphics2D g) {
        if (document != null) {
            int maxW = gp.screenWidth - 100, maxH = gp.screenHeight - 215;
            double scale = Math.min(maxW / (double)document.getWidth(), maxH / (double)document.getHeight());
            int w = (int)(document.getWidth()*scale), h = (int)(document.getHeight()*scale);
            int x = (gp.screenWidth-w)/2, y = 22;
            UiSkin.frame(g, x-5,y-5,w+10,h+10); g.drawImage(document,x,y,w,h,null);
        }
        if (!modal()) return;
        g.setColor(new Color(4,9,13,210)); g.fillRect(0,0,gp.screenWidth,gp.screenHeight);
        int w = Math.min(620,gp.screenWidth-60), x=(gp.screenWidth-w)/2, y=gp.screenHeight/2-120;
        UiSkin.frame(g,x,y,w,240);
        g.setFont(UiFonts.of(Font.BOLD,21)); g.setColor(UiSkin.GOLD); g.drawString(title,x+25,y+38);
        g.setFont(UiFonts.of(Font.PLAIN,16)); g.setColor(UiSkin.TEXT);
        int lineY=y+75;
        for(String paragraph:body.split("\\|")) {
            String line="";
            for(String word:paragraph.split(" ")) {
                if(g.getFontMetrics().stringWidth(line+word)>w-50) { g.drawString(line,x+25,lineY);lineY+=22;line=""; }
                line+=word+" ";
            }
            g.drawString(line,x+25,lineY);lineY+=25;
        }
        g.setFont(UiFonts.of(Font.BOLD,12)); g.setColor(UiSkin.GOLD);
        g.drawString("ENTER  CONTINUE",x+25,y+215);
    }
}
