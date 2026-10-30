package save;

import java.util.Properties;

public final class Values {
    private Values() {}

    public static int integer(Properties p, String key, int fallback) {
        try { return Integer.parseInt(p.getProperty(key, "").trim()); }
        catch (RuntimeException e) { return fallback; }
    }

    public static double decimal(Properties p, String key, double fallback) {
        try {
            double value = Double.parseDouble(p.getProperty(key, "").trim());
            return Double.isFinite(value) ? value : fallback;
        } catch (RuntimeException e) { return fallback; }
    }

    public static boolean bool(Properties p, String key) {
        return Boolean.parseBoolean(p.getProperty(key, "false"));
    }
}
