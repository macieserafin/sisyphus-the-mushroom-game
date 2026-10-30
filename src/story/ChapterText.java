package story;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public final class ChapterText {
    private static final Properties TEXT = load();
    private ChapterText() {}

    private static Properties load() {
        Properties p = new Properties();
        try (var input = ChapterText.class.getResourceAsStream("/story/chapter1.properties")) {
            if (input == null) throw new IllegalStateException("Missing chapter text");
            p.load(new InputStreamReader(input, StandardCharsets.UTF_8));
            return p;
        } catch (java.io.IOException e) { throw new IllegalStateException(e); }
    }

    public static String get(String key) { return TEXT.getProperty(key, key); }
    public static List<String> lines(String key) { return Arrays.asList(get(key).split("\\|")); }
}
