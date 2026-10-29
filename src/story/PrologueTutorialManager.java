package story;

import dialogue.DialogueLine;
import dialogue.DialogueSpeaker;
import entity.CombatState;
import entity.enemy.Enemy;
import entity.enemy.Enemy_Slime;
import main.GamePanel;
import main.GameState;
import object.TutorialSword;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PrologueTutorialManager {
    private enum Step {
        INTRO,
        WAIT_FOR_MOVE,
        WAIT_FOR_RANGED,
        WAIT_FOR_SWORD_PICKUP,
        WAIT_FOR_SWITCH,
        WAIT_FOR_MELEE,
        PRACTICE,
        PRACTICE_CLEARED,
        COMPLETE
    }

    private static final int PRACTICE_SLIME_COUNT = 5;
    private static final int PRACTICE_SLIME_MIN_DISTANCE_TILES = 4;
    private static final int NEXT_LINE_DELAY_FRAMES = 60 * 2;
    private static final int TUTORIAL_DIALOGUE_FRAMES = Integer.MAX_VALUE;
    private static final int[][] DEFAULT_SWORD_OFFSETS = {
            {0, -3}, {3, 0}, {0, 3}, {-3, 0},
            {2, -2}, {-2, -2}, {2, 2}, {-2, 2},
            {0, -4}, {4, 0}, {0, 4}, {-4, 0}
    };

    private final GamePanel gp;
    private final DialogueSpeaker bytSpeaker = new DialogueSpeaker(
            "BYT",
            null,
            new Color(190, 80, 230)
    );

    private Step step = Step.INTRO;
    private boolean instructionActionCompleted = false;
    private int nextLineDelayLeft = -1;
    private TutorialSword tutorialSword = null;
    private final List<Enemy> practiceSlimes = new ArrayList<>();
    private int lastPracticeSlimesLeft = -1;

    public PrologueTutorialManager(GamePanel gp) {
        this.gp = gp;
    }

    public void update() {
        if (!shouldRun()) {
            return;
        }

        switch (step) {
            case INTRO:
                say("There. Legs. Use them before they forget you.");
                step = Step.WAIT_FOR_MOVE;
                break;
            case WAIT_FOR_MOVE:
                if (isInstructionComplete(hasMovementInput())) {
                    say("Ah. A pistol in the jacket. Of course. You woke in an impossible forest and still brought a fucking pistol. [Arrow keys fire it.]");
                    step = Step.WAIT_FOR_RANGED;
                    resetInstructionProgress();
                }
                break;
            case WAIT_FOR_RANGED:
                if (isInstructionComplete(hasAttackInput() && gp.player.combatState == CombatState.RANGE)) {
                    say("Good. Lead makes such brave promises. It will not be enough here. Take a sword.");
                    spawnTutorialSword();
                    step = Step.WAIT_FOR_SWORD_PICKUP;
                    resetInstructionProgress();
                }
                break;
            case WAIT_FOR_SWORD_PICKUP:
                if (isInstructionComplete(false)) {
                    say(gp.controls.separateModeKeys()
                            ? "There. A sharper lie. [Q] is the blade, [E] is the gun."
                            : "There. A sharper lie. [Q] changes the shape: blade, gun, blade again.");
                    step = Step.WAIT_FOR_SWITCH;
                    resetInstructionProgress();
                }
                break;
            case WAIT_FOR_SWITCH:
                if (isInstructionComplete(gp.player.combatState == CombatState.MELEE)) {
                    say("Closer. Now take a swing. [Use the arrow keys]");
                    step = Step.WAIT_FOR_MELEE;
                    resetInstructionProgress();
                }
                break;
            case WAIT_FOR_MELEE:
                if (isInstructionComplete(gp.player.combatState == CombatState.MELEE && hasAttackInput())) {
                    say("There it is. A tiny blade against a very old hunger. Keep moving. I want to watch.");
                    spawnPracticeSlimes();
                    step = Step.PRACTICE;
                    resetInstructionProgress();
                }
                break;
            case PRACTICE:
                int slimesLeft = countPracticeSlimesAlive();
                if (slimesLeft != lastPracticeSlimesLeft) {
                    lastPracticeSlimesLeft = slimesLeft;
                    if (slimesLeft > 0) {
                        gp.ui.showMessage("Slimes left: " + slimesLeft);
                    }
                }
                if (slimesLeft > 0) {
                    break;
                }
                practiceSlimes.clear();
                gp.ui.messageOn = false;
                gp.dialogueManager.clear();
                gp.keyH.clearGameplayInput();
                gp.bulletManager.clearBullets();
                gp.enemyShots.clear();
                gp.player.beginScriptedHold();
                nextLineDelayLeft = 75;
                step = Step.PRACTICE_CLEARED;
                break;
            case PRACTICE_CLEARED:
                // Let the last slime finish dissolving before BYT interrupts the silence.
                if (--nextLineDelayLeft > 0) break;
                gp.player.health = gp.player.maxHealth;
                gp.storyFlags.completePrologueTutorial();
                gp.storyFlags.startPrologueBytEncounter();
                gp.storySequenceManager.start(StorySequences.prologueBytEncounterIntro(gp));
                step = Step.COMPLETE;
                break;
            case COMPLETE:
            default:
                break;
        }
    }

    public boolean canMove() {
        return !isTutorialLockActive() || step.ordinal() >= Step.WAIT_FOR_MOVE.ordinal();
    }

    public boolean canUseRangedAttack() {
        if (!isTutorialLockActive()) {
            return true;
        }

        switch (step) {
            case WAIT_FOR_RANGED:
            case WAIT_FOR_SWORD_PICKUP:
            case WAIT_FOR_SWITCH:
            case WAIT_FOR_MELEE:
            case PRACTICE:
                return true;
            default:
                return false;
        }
    }

    public boolean canSwitchCombatMode() {
        if (!isTutorialLockActive() || step == Step.PRACTICE) {
            return true;
        }

        return step == Step.WAIT_FOR_SWITCH && !instructionActionCompleted;
    }

    public boolean canUseMeleeAttack() {
        if (!isTutorialLockActive() || step == Step.PRACTICE) {
            return true;
        }

        return step == Step.WAIT_FOR_MELEE;
    }

    public boolean canSwitchWeaponSlot() {
        return !isTutorialLockActive();
    }

    public boolean canPickUpTutorialSword(TutorialSword sword) {
        return !isTutorialLockActive() ||
                step == Step.WAIT_FOR_SWORD_PICKUP && sword == tutorialSword;
    }

    public void onTutorialSwordPickedUp(TutorialSword sword) {
        if (!isTutorialLockActive() || step != Step.WAIT_FOR_SWORD_PICKUP || sword != tutorialSword) {
            return;
        }

        tutorialSword = null;
        instructionActionCompleted = true;
    }

    public boolean isFightingPracticeSlimes() {
        return isTutorialLockActive() && step == Step.PRACTICE;
    }

    // Dying to the practice slimes just puts the player back at the start; the slimes keep their progress.
    public void respawnPlayerForPractice() {
        gp.player.setDefaultValues();
        gp.levelManager.placePlayerAtPrologueSpawn();
        say("Up. The hunger is patient, and so am I.");
    }

    private void spawnPracticeSlimes() {
        practiceSlimes.clear();
        lastPracticeSlimesLeft = -1;

        int playerCol = Math.floorDiv(gp.player.worldX + gp.tileSize / 2, gp.tileSize);
        int playerRow = Math.floorDiv(gp.player.worldY + gp.tileSize / 2, gp.tileSize);

        List<int[]> candidates = new ArrayList<>();
        for (int col = 0; col < gp.tileM.getWorldCols(); col++) {
            for (int row = 0; row < gp.tileM.getWorldRows(); row++) {
                int distance = Math.max(Math.abs(col - playerCol), Math.abs(row - playerRow));
                if (distance >= PRACTICE_SLIME_MIN_DISTANCE_TILES && isOpenTile(col, row) && !hasObjectAt(col, row)) {
                    candidates.add(new int[]{col, row});
                }
            }
        }
        Collections.shuffle(candidates);

        for (int[] tile : candidates) {
            if (practiceSlimes.size() >= PRACTICE_SLIME_COUNT) {
                break;
            }
            if (isNextToPracticeSlime(tile[0], tile[1])) {
                continue;
            }
            practiceSlimes.add(gp.entities.addEnemy(new Enemy_Slime(gp), tile[0], tile[1]));
        }
    }

    private boolean isNextToPracticeSlime(int col, int row) {
        for (Enemy slime : practiceSlimes) {
            int slimeCol = slime.worldX / gp.tileSize;
            int slimeRow = slime.worldY / gp.tileSize;
            if (Math.abs(slimeCol - col) <= 1 && Math.abs(slimeRow - row) <= 1) {
                return true;
            }
        }
        return false;
    }

    private int countPracticeSlimesAlive() {
        int alive = 0;
        for (Enemy slime : practiceSlimes) {
            if (slime.alive) {
                alive++;
            }
        }
        return alive;
    }

    private boolean isInstructionComplete(boolean actionCompletedThisFrame) {
        if (actionCompletedThisFrame) {
            instructionActionCompleted = true;
        }

        if (!instructionActionCompleted || !gp.dialogueManager.isLineFullyVisible()) {
            nextLineDelayLeft = -1;
            return false;
        }

        if (nextLineDelayLeft < 0) {
            nextLineDelayLeft = NEXT_LINE_DELAY_FRAMES;
        }

        if (nextLineDelayLeft > 0) {
            nextLineDelayLeft--;
            return false;
        }

        return true;
    }

    private void resetInstructionProgress() {
        instructionActionCompleted = false;
        nextLineDelayLeft = -1;
    }

    private boolean isTutorialLockActive() {
        return gp.gameState == GameState.PLAYING &&
                gp.storyFlags.isIntroCutsceneCompleted() &&
                !gp.storyFlags.isPrologueTutorialCompleted();
    }

    private void spawnTutorialSword() {
        if (tutorialSword != null) {
            return;
        }

        int playerCol = Math.floorDiv(gp.player.worldX + gp.tileSize / 2, gp.tileSize);
        int playerRow = Math.floorDiv(gp.player.worldY + gp.tileSize / 2, gp.tileSize);

        int[] frontOffset = attackDirectionOffset();
        if (frontOffset != null && trySpawnTutorialSword(playerCol + frontOffset[0], playerRow + frontOffset[1])) {
            return;
        }

        for (int[] offset : DEFAULT_SWORD_OFFSETS) {
            if (trySpawnTutorialSword(playerCol + offset[0], playerRow + offset[1])) {
                return;
            }
        }

        tutorialSword = (TutorialSword) gp.entities.addObject(new TutorialSword(), playerCol, playerRow);
    }

    private boolean trySpawnTutorialSword(int col, int row) {
        if (!isOpenTile(col, row) || hasObjectAt(col, row)) {
            return false;
        }

        tutorialSword = (TutorialSword) gp.entities.addObject(new TutorialSword(), col, row);
        return true;
    }

    private boolean isOpenTile(int col, int row) {
        if (col < 0 || row < 0 || col >= gp.tileM.getWorldCols() || row >= gp.tileM.getWorldRows()) {
            return false;
        }

        return !gp.tileM.isCollision(col, row);
    }

    private boolean hasObjectAt(int col, int row) {
        int worldX = col * gp.tileSize;
        int worldY = row * gp.tileSize;
        for (int i = 0; i < gp.entities.objects().size(); i++) {
            if (gp.entities.objects().get(i).worldX == worldX &&
                    gp.entities.objects().get(i).worldY == worldY) {
                return true;
            }
        }

        return false;
    }

    private int[] attackDirectionOffset() {
        if (gp.keyH.upShootPressed) {
            return new int[]{0, -3};
        }
        if (gp.keyH.downShootPressed) {
            return new int[]{0, 3};
        }
        if (gp.keyH.leftShootPressed) {
            return new int[]{-3, 0};
        }
        if (gp.keyH.rightShootPressed) {
            return new int[]{3, 0};
        }

        return null;
    }

    private boolean shouldRun() {
        return gp.gameState == GameState.PLAYING &&
                gp.storyFlags.isIntroCutsceneCompleted() &&
                !gp.storyFlags.isPrologueTutorialCompleted() &&
                !gp.storySequenceManager.isActive();
    }

    private boolean hasMovementInput() {
        return gp.keyH.upPressed ||
                gp.keyH.downPressed ||
                gp.keyH.leftPressed ||
                gp.keyH.rightPressed;
    }

    private boolean hasAttackInput() {
        return gp.keyH.upShootPressed ||
                gp.keyH.downShootPressed ||
                gp.keyH.leftShootPressed ||
                gp.keyH.rightShootPressed;
    }

    private void say(String text) {
        gp.dialogueManager.showAmbient(bytSpeaker, text, DialogueLine.Priority.HIGH, TUTORIAL_DIALOGUE_FRAMES);
    }
}
