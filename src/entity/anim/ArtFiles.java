package entity.anim;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

final class ArtFiles {
    private static final File SOURCE_ROOT = new File("res");

    private ArtFiles() {
    }

    static boolean exists(String path) {
        return new File(SOURCE_ROOT, path).isFile() || ArtFiles.class.getClassLoader().getResource(path) != null;
    }

    static InputStream open(String path) throws IOException {
        File source = new File(SOURCE_ROOT, path);
        if (source.isFile()) {
            return new FileInputStream(source);
        }
        return ArtFiles.class.getClassLoader().getResourceAsStream(path);
    }
}
