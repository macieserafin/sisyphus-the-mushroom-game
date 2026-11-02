package ui;

import save.Settings;

/** Zoom is a display preference; it never changes the exploration mask. */
public final class MinimapView {
    public static final int MAX_LEVEL = 3;
    private static final String KEY = "minimap.zoom";
    private final Settings settings;
    private int level;

    public MinimapView(Settings settings) { this.settings=settings;restore(); }
    public int level() { return level; }
    public boolean followsPlayer() { return level>0; }
    public String label() { return level==0?"ALL":(1<<level)+"x"; }
    public void restore() { level=clamp(settings.getInt(KEY,0)); }
    public void adjust(int delta) { set(level+delta,true); }
    public void preview(int delta) { set(level+delta,false); }
    private int clamp(int value) { return Math.max(0,Math.min(MAX_LEVEL,value)); }
    private void set(int value,boolean persist) {
        int next=clamp(value);if(next==level)return;
        level=next;if(persist)settings.putInt(KEY,level);
    }

    public record Transform(double scale,double x,double y) {}
    public Transform transform(int cols,int rows,int size,double playerCol,double playerRow) {
        if(cols<=0||rows<=0||size<=0)throw new IllegalArgumentException("Invalid minimap dimensions");
        double scale=size/(double)Math.max(cols,rows)*(1<<level);
        if(followsPlayer())return new Transform(scale,size/2.0-playerCol*scale,size/2.0-playerRow*scale);
        return new Transform(scale,(size-cols*scale)/2,(size-rows*scale)/2);
    }
}
