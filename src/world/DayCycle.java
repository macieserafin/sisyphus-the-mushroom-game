package world;

/** Continuous outdoor phases. Dawn 04–08, dusk 17–21; no discontinuity at midnight. */
public record DayCycle(float night,float dawn,float dusk) {
    public static final int SUNRISE_SECONDS = 6 * 60 * 60;
    public static DayCycle at(long seconds) {
        double hour=Math.floorMod(seconds,86400)/3600.0;
        float night=hour<12?1-smooth((hour-4)/4):smooth((hour-17)/4);
        return new DayCycle(night,bell(hour,4,6,8),bell(hour,17,19,21));
    }
    private static float bell(double h,double start,double peak,double end) {
        return smooth(h<peak?(h-start)/(peak-start):(end-h)/(end-peak));
    }
    private static float smooth(double t) { t=Math.max(0,Math.min(1,t));return (float)(t*t*(3-2*t)); }
}
