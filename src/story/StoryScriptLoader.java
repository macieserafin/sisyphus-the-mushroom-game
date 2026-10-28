package story;

import dialogue.DialogueSpeaker;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class StoryScriptLoader {
    private static final String SCRIPT_DIR = "story/";
    private static final String SCRIPT_EXTENSION = ".story";

    private StoryScriptLoader() {
    }

    public static StorySequence load(String id, Map<String, Runnable> actions) {
        List<String> lines = readLines(SCRIPT_DIR + id + SCRIPT_EXTENSION);
        return new Parser(id, lines, actions).parse();
    }

    private static List<String> readLines(String path) {
        try (InputStream stream = StoryScriptLoader.class.getClassLoader().getResourceAsStream(path)) {
            if (stream != null) {
                return readAll(new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8)));
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could not read story script " + path, e);
        }

        File file = new File("res", path);
        try {
            return Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Missing story script: " + path, e);
        }
    }

    private static List<String> readAll(BufferedReader reader) throws IOException {
        List<String> lines = new ArrayList<>();
        String line;
        while ((line = reader.readLine()) != null) {
            lines.add(line);
        }
        return lines;
    }

    private static BufferedImage loadImage(String path) {
        try (InputStream stream = StoryScriptLoader.class.getClassLoader().getResourceAsStream(path)) {
            if (stream != null) {
                return ImageIO.read(stream);
            }
        } catch (IOException e) {
            System.err.println("Could not load story image " + path + ": " + e.getMessage());
            return null;
        }

        File file = new File("res", path);
        if (!file.isFile()) {
            System.err.println("Missing story image: " + path);
            return null;
        }

        try {
            return ImageIO.read(file);
        } catch (IOException e) {
            System.err.println("Could not load story image file " + file + ": " + e.getMessage());
            return null;
        }
    }

    private static final class Parser {
        private final String id;
        private final List<String> source;
        private final Map<String, Runnable> actions;
        private final Map<String, DialogueSpeaker> speakers = new LinkedHashMap<>();
        private final List<StorySequenceStep> steps = new ArrayList<>();

        private boolean overlay;
        private String background;
        private List<DialogueSpeaker> cast;

        private DialogueSpeaker currentSpeaker;
        private final List<String> currentLines = new ArrayList<>();
        private int lineNumber;

        Parser(String id, List<String> source, Map<String, Runnable> actions) {
            this.id = id;
            this.source = source;
            this.actions = actions;
        }

        StorySequence parse() {
            for (String raw : source) {
                lineNumber++;
                String line = raw.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                if (line.startsWith("speaker ")) {
                    parseSpeaker(line.substring("speaker ".length()));
                } else if (line.startsWith("!action ")) {
                    flush();
                    steps.add(StorySequenceStep.action(resolveAction(line.substring("!action ".length()).trim())));
                } else if (line.startsWith("!wait ")) {
                    flush();
                    steps.add(StorySequenceStep.waitFrames(parseInt(line.substring("!wait ".length()).trim())));
                } else if (line.equals("---")) {
                    flush();
                } else if (line.startsWith("[") && line.endsWith("]")) {
                    flush();
                    parseDirectives(line.substring(1, line.length() - 1));
                } else {
                    parseDialogueLine(line);
                }
            }
            flush();
            return new StorySequence(id, steps);
        }

        private void parseSpeaker(String definition) {
            int eq = definition.indexOf('=');
            if (eq < 0) {
                throw error("Speaker needs 'id = Name | portrait | #RRGGBB'");
            }
            String speakerId = definition.substring(0, eq).trim();
            String[] parts = definition.substring(eq + 1).split("\\|");
            if (parts.length != 3) {
                throw error("Speaker needs 'Name | portrait | #RRGGBB'");
            }
            String portraitPath = parts[1].trim();
            BufferedImage portrait = portraitPath.equals("-") ? null : loadImage(portraitPath);
            Color color;
            try {
                color = Color.decode(parts[2].trim());
            } catch (NumberFormatException e) {
                throw error("Bad color '" + parts[2].trim() + "'");
            }
            speakers.put(speakerId, new DialogueSpeaker(parts[0].trim(), portrait, color));
        }

        private void parseDirectives(String content) {
            overlay = false;
            background = null;
            cast = null;
            for (String directive : content.split("\\|")) {
                String d = directive.trim();
                if (d.equals("overlay")) {
                    overlay = true;
                } else if (d.startsWith("bg ")) {
                    background = d.substring(3).trim();
                } else if (d.startsWith("cast ")) {
                    cast = new ArrayList<>();
                    for (String name : d.substring(5).split(",")) {
                        cast.add(speaker(name.trim()));
                    }
                } else {
                    throw error("Unknown directive '" + d + "'");
                }
            }
        }

        private void parseDialogueLine(String line) {
            int colon = line.indexOf(": ");
            if (colon < 0) {
                throw error("Expected 'speaker: text'");
            }
            DialogueSpeaker speaker = speaker(line.substring(0, colon).trim());
            if (currentSpeaker != null && speaker != currentSpeaker) {
                flush();
            }
            currentSpeaker = speaker;
            currentLines.add(line.substring(colon + 2).trim());
        }

        private void flush() {
            if (currentSpeaker != null) {
                String[] lines = currentLines.toArray(new String[0]);
                if (overlay) {
                    steps.add(StorySequenceStep.dialogue(currentSpeaker, lines));
                } else if (cast != null) {
                    steps.add(StorySequenceStep.visualNovelDialogueWithCastAndBackground(
                            currentSpeaker, cast, background, lines));
                } else if (background != null) {
                    steps.add(StorySequenceStep.visualNovelDialogueWithBackground(
                            currentSpeaker, background, lines));
                } else {
                    steps.add(StorySequenceStep.visualNovelDialogue(currentSpeaker, lines));
                }
                currentSpeaker = null;
                currentLines.clear();
                overlay = false;
                background = null;
                cast = null;
            }
        }

        private DialogueSpeaker speaker(String speakerId) {
            DialogueSpeaker speaker = speakers.get(speakerId);
            if (speaker == null) {
                throw error("Unknown speaker '" + speakerId + "'");
            }
            return speaker;
        }

        private Runnable resolveAction(String name) {
            Runnable action = actions.get(name);
            if (action == null) {
                throw error("Unknown action '" + name + "'");
            }
            return action;
        }

        private int parseInt(String value) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                throw error("Expected a number, got '" + value + "'");
            }
        }

        private IllegalStateException error(String message) {
            return new IllegalStateException("Story script '" + id + "' line " + lineNumber + ": " + message);
        }
    }
}
