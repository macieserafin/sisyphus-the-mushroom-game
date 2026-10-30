package save;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.zip.CRC32;

// Small named key/value files: one for the settings and one per save slot. Every file carries a format number and a
// checksum. A write goes to a temporary file first and replaces the old one in one step; the previous good version is
// kept as <name>.bak and used when the main file turns out to be damaged. Without a folder (in-memory) nothing touches
// the disk: tests and the demo use that.
public final class SaveStore {
    public static final int SLOTS = 3;

    private static final String FORMAT_KEY = "format";
    private static final String CRC_KEY = "crc";
    private static final String FORMAT = "1";

    private final Path root;
    private final Map<String, String> memory = new HashMap<>();
    private String warning = "";

    private SaveStore(Path root) {
        this.root = root;
    }

    public static SaveStore inMemory() {
        return new SaveStore(null);
    }

    public static SaveStore at(Path folder) {
        return new SaveStore(folder);
    }

    // -Dsisyphus.saveDir=memory keeps everything in memory, any other value is used as the folder.
    public static SaveStore createDefault() {
        String property = System.getProperty("sisyphus.saveDir");
        if ("memory".equals(property)) {
            return inMemory();
        }
        if (property != null && !property.isBlank()) {
            return at(Paths.get(property));
        }
        String appData = System.getenv("APPDATA");
        Path base = appData != null && !appData.isBlank()
                ? Paths.get(appData)
                : Paths.get(System.getProperty("user.home"), ".local", "share");
        return at(base.resolve("SisyphusTheMushroom"));
    }

    public boolean isPersistent() {
        return root != null;
    }

    public Path folder() {
        return root;
    }

    // Last problem met while reading or writing, empty when everything went fine.
    public synchronized String warning() {
        return warning;
    }

    public synchronized boolean exists(String name) {
        return read(name) != null;
    }

    // The file's contents, the backup when the file is damaged, or null when there is nothing usable.
    public synchronized Properties read(String name) {
        Properties data = parse(name);
        if (data != null) {
            return data;
        }
        data = parse(name + ".bak");
        if (data != null) {
            warning = name + " was damaged, restored from its backup";
        }
        return data;
    }

    public synchronized boolean write(String name, Properties data) {
        Properties copy = new Properties();
        copy.putAll(data);
        copy.remove(CRC_KEY);
        copy.setProperty(FORMAT_KEY, FORMAT);
        copy.setProperty(CRC_KEY, checksum(copy));
        String text;
        try (StringWriter out = new StringWriter()) {
            copy.store(out, "Sisyphus the Mushroom");
            text = out.toString();
        } catch (IOException e) {
            warning = "could not encode " + name;
            return false;
        }
        if (root == null) {
            if (parse(name) != null) {
                memory.put(name + ".bak", memory.get(name));
            }
            memory.put(name, text);
            return true;
        }
        try {
            Files.createDirectories(root);
            Path target = root.resolve(name);
            Path temp = root.resolve(name + ".tmp");
            Files.writeString(temp, text, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            if (Files.exists(target) && parse(name) != null) {
                Files.copy(target, root.resolve(name + ".bak"), StandardCopyOption.REPLACE_EXISTING);
            }
            try {
                Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (IOException e) {
            warning = "could not save " + name + ": " + e.getMessage();
            System.err.println(warning);
            return false;
        }
    }

    public synchronized void delete(String name) {
        if (root == null) {
            memory.remove(name);
            memory.remove(name + ".bak");
            return;
        }
        for (String file : new String[]{name, name + ".bak", name + ".tmp"}) {
            try {
                Files.deleteIfExists(root.resolve(file));
            } catch (IOException e) {
                warning = "could not delete " + file + ": " + e.getMessage();
            }
        }
    }

    private Properties parse(String file) {
        String text;
        if (root == null) {
            text = memory.get(file);
        } else {
            Path path = root.resolve(file);
            if (!Files.isRegularFile(path)) {
                return null;
            }
            try {
                text = Files.readString(path, StandardCharsets.UTF_8);
            } catch (IOException e) {
                return null;
            }
        }
        if (text == null) {
            return null;
        }
        Properties data = new Properties();
        try (StringReader in = new StringReader(text)) {
            data.load(in);
        } catch (IOException | IllegalArgumentException e) {
            return null;
        }
        if (data.getProperty(FORMAT_KEY) == null || !checksum(data).equals(data.getProperty(CRC_KEY))) {
            return null;
        }
        return data;
    }

    private static String checksum(Properties data) {
        List<String> keys = new ArrayList<>(data.stringPropertyNames());
        Collections.sort(keys);
        CRC32 crc = new CRC32();
        for (String key : keys) {
            if (!key.equals(CRC_KEY)) {
                crc.update((key + "=" + data.getProperty(key) + "\n").getBytes(StandardCharsets.UTF_8));
            }
        }
        return Long.toHexString(crc.getValue());
    }
}
