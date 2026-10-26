package dialogue;

import main.GamePanel;
import main.GameState;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;

public class DialogueManager {
    private static final int DEFAULT_AMBIENT_DURATION_FRAMES = 240;
    private static final int AMBIENT_COOLDOWN_FRAMES = 120;

    private final GamePanel gp;
    private final Queue<DialogueLine> queue = new ArrayDeque<>();
    private DialogueLine currentLine;
    private int ambientFramesLeft = 0;
    private int ambientCooldownFramesLeft = 0;
    private int visibleCharacters = 0;
    private int typewriterFrameCounter = 0;
    private GameState returnState = GameState.PLAYING;

    public DialogueManager(GamePanel gp) {
        this.gp = gp;
    }

    public void startBlocking(DialogueSpeaker speaker, String text) {
        queue.clear();
        beginLine(DialogueLine.blocking(speaker, text));
    }

    public void startVisualNovel(DialogueSpeaker speaker, List<String> lines) {
        startVisualNovel(speaker, null, lines);
    }

    public void startVisualNovel(DialogueSpeaker speaker, List<DialogueSpeaker> sceneSpeakers, List<String> lines) {
        startVisualNovel(speaker, sceneSpeakers, null, lines);
    }

    public void startVisualNovel(
            DialogueSpeaker speaker,
            List<DialogueSpeaker> sceneSpeakers,
            String visualNovelBackgroundPath,
            List<String> lines
    ) {
        startBlockingLines(speaker, sceneSpeakers, visualNovelBackgroundPath, lines, true);
    }

    public void startBlocking(DialogueSpeaker speaker, List<String> lines) {
        startBlockingLines(speaker, null, null, lines, false);
    }

    private void startBlockingLines(
            DialogueSpeaker speaker,
            List<DialogueSpeaker> sceneSpeakers,
            String visualNovelBackgroundPath,
            List<String> lines,
            boolean visualNovel
    ) {
        queue.clear();
        if (lines == null || lines.isEmpty()) {
            startBlocking(speaker, "...");
            return;
        }

        boolean firstLineStarted = false;
        for (String text : lines) {
            if (text == null || text.trim().isEmpty()) {
                continue;
            }

            DialogueLine line = visualNovel
                    ? DialogueLine.visualNovel(speaker, sceneSpeakers, visualNovelBackgroundPath, text)
                    : DialogueLine.blocking(speaker, text);
            if (!firstLineStarted) {
                beginLine(line);
                firstLineStarted = true;
            } else {
                queue.add(line);
            }
        }

        if (!firstLineStarted) {
            startBlocking(speaker, "...");
        }
    }

    public void queueBlocking(DialogueSpeaker speaker, String text) {
        DialogueLine line = DialogueLine.blocking(speaker, text);
        if (currentLine == null) {
            beginLine(line);
            return;
        }
        queue.add(line);
    }

    public void showAmbient(DialogueSpeaker speaker, String text, DialogueLine.Priority priority) {
        showAmbient(speaker, text, priority, DEFAULT_AMBIENT_DURATION_FRAMES);
    }

    public void showAmbient(DialogueSpeaker speaker, String text, DialogueLine.Priority priority, int durationFrames) {
        DialogueLine line = DialogueLine.ambient(
                speaker,
                text,
                priority,
                durationFrames
        );

        if (currentLine != null && currentLine.mode() == DialogueLine.Mode.BLOCKING) {
            return;
        }
        if (ambientCooldownFramesLeft > 0 && priority.ordinal() < DialogueLine.Priority.HIGH.ordinal()) {
            return;
        }
        if (currentLine != null && currentLine.priority().ordinal() > line.priority().ordinal()) {
            return;
        }

        beginLine(line);
        ambientFramesLeft = line.durationFrames();
        ambientCooldownFramesLeft = AMBIENT_COOLDOWN_FRAMES;
    }

    public void update() {
        if (ambientCooldownFramesLeft > 0) {
            ambientCooldownFramesLeft--;
        }
        updateTypewriter();

        if (currentLine == null || currentLine.mode() != DialogueLine.Mode.AMBIENT) {
            return;
        }

        if (ambientFramesLeft > 0) {
            ambientFramesLeft--;
        }
        if (ambientFramesLeft == 0) {
            currentLine = null;
        }
    }

    public void advanceBlocking() {
        if (currentLine == null || currentLine.mode() != DialogueLine.Mode.BLOCKING) {
            if (gp.storySequenceManager.isActive()) {
                return;
            }
            gp.gameState = returnState;
            return;
        }

        if (!isLineFullyVisible()) {
            revealCurrentLine();
            return;
        }

        DialogueLine nextLine = queue.poll();
        if (nextLine != null) {
            beginLine(nextLine);
            return;
        }

        currentLine = null;
        visibleCharacters = 0;
        typewriterFrameCounter = 0;

        if (gp.storySequenceManager.isActive()) {
            gp.storySequenceManager.completeDialogueStep();
            return;
        }

        if (!gp.storySequenceManager.isVisualNovelSequenceActive()) {
            gp.gameState = returnState;
        }
    }

    public DialogueLine currentLine() {
        return currentLine;
    }

    public boolean hasActiveLine() {
        return currentLine != null;
    }

    public String visibleText() {
        if (currentLine == null) {
            return "";
        }
        int endIndex = Math.min(visibleCharacters, currentLine.text().length());
        return currentLine.text().substring(0, endIndex);
    }

    public boolean isLineFullyVisible() {
        return currentLine == null || visibleCharacters >= currentLine.text().length();
    }

    public void clear() {
        queue.clear();
        currentLine = null;
        ambientFramesLeft = 0;
        visibleCharacters = 0;
        typewriterFrameCounter = 0;
    }

    private void beginLine(DialogueLine line) {
        currentLine = line;
        ambientFramesLeft = line == null ? 0 : line.durationFrames();
        visibleCharacters = line == null || line.text().isEmpty() ? 0 : 1;
        typewriterFrameCounter = 0;

        if (line == null) {
            return;
        }

        if (line.mode() == DialogueLine.Mode.BLOCKING) {
            if (gp.gameState != GameState.DIALOGUE && gp.gameState != GameState.CUTSCENE) {
                returnState = gp.gameState;
            }
            gp.keyH.clearGameplayInput();
            gp.gameState = line.presentation() == DialogueLine.Presentation.VISUAL_NOVEL
                    ? GameState.CUTSCENE
                    : GameState.DIALOGUE;
        }

        if (line.typewriterSpeedFrames() == 0) {
            revealCurrentLine();
        }
    }

    private void updateTypewriter() {
        if (currentLine == null || isLineFullyVisible()) {
            return;
        }

        int speed = currentLine.typewriterSpeedFrames();
        if (speed == 0) {
            revealCurrentLine();
            return;
        }

        typewriterFrameCounter++;
        if (typewriterFrameCounter >= speed) {
            visibleCharacters++;
            typewriterFrameCounter = 0;
        }
    }

    private void revealCurrentLine() {
        if (currentLine != null) {
            visibleCharacters = currentLine.text().length();
        }
    }
}
