package main;

import save.Settings;

public class VisualSettings {
    public enum Scaling {
        PIXEL_PERFECT("Pixel perfect"), SHARP("Sharp fill"), STRETCH("Stretch");

        public final String label;

        Scaling(String label) {
            this.label = label;
        }
    }

    public enum Detail {
        OFF("Off"), LOW("Low"), HIGH("High");

        public final String label;

        Detail(String label) {
            this.label = label;
        }
    }

    private static final String SCALING_KEY = "scaling";
    private static final String DETAIL_KEY = "ambienceDetail";
    private static final String MOTION_KEY = "reduceMotion";
    private static final String FONT_KEY = "pixelFont";

    private final Settings settings;
    private Scaling scaling;
    private Detail detail;
    private boolean reduceMotion;
    private boolean pixelFont;

    public VisualSettings(Settings settings) {
        this.settings = settings;
        scaling = parse(Scaling.class, settings.getString(SCALING_KEY, Scaling.PIXEL_PERFECT.name()), Scaling.PIXEL_PERFECT);
        detail = parse(Detail.class, settings.getString(DETAIL_KEY, Detail.HIGH.name()), Detail.HIGH);
        reduceMotion = settings.getBoolean(MOTION_KEY, false);
        pixelFont = settings.getBoolean(FONT_KEY, true);
        UiFonts.refresh(pixelFont);
    }

    private static <E extends Enum<E>> E parse(Class<E> type, String value, E fallback) {
        try {
            return Enum.valueOf(type, value);
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    private static <E extends Enum<E>> E step(E current, int direction) {
        E[] values = current.getDeclaringClass().getEnumConstants();
        return values[Math.floorMod(current.ordinal() + direction, values.length)];
    }

    public Scaling scaling() {
        return scaling;
    }

    public void cycleScaling(int direction) {
        scaling = step(scaling, direction);
        settings.put(SCALING_KEY, scaling.name());
    }

    public Detail detail() {
        return detail;
    }

    public void cycleDetail(int direction) {
        setDetail(step(detail, direction));
    }

    public void setDetail(Detail next) {
        detail = next;
        settings.put(DETAIL_KEY, detail.name());
    }

    public boolean reduceMotion() {
        return reduceMotion;
    }

    public void toggleReduceMotion() {
        reduceMotion = !reduceMotion;
        settings.putBoolean(MOTION_KEY, reduceMotion);
    }

    public boolean pixelFont() {
        return pixelFont;
    }

    public void togglePixelFont() {
        pixelFont = !pixelFont;
        settings.putBoolean(FONT_KEY, pixelFont);
    }

    public void reset() {
        scaling = Scaling.PIXEL_PERFECT;
        detail = Detail.HIGH;
        reduceMotion = false;
        pixelFont = true;
        settings.put(SCALING_KEY, scaling.name());
        settings.put(DETAIL_KEY, detail.name());
        settings.putBoolean(MOTION_KEY, reduceMotion);
        settings.putBoolean(FONT_KEY, pixelFont);
    }
}
