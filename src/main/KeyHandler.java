package main;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class KeyHandler implements KeyListener {
    public boolean upPressed, downPressed, leftPressed, rightPressed;
    public boolean upShootPressed, downShootPressed, leftShootPressed, rightShootPressed;
    public boolean shiftPressed, spacePressed, enterPressed;
    public boolean qPressed, ePressed, onePressed, twoPressed;

    GamePanel gp;

    public KeyHandler(GamePanel gp) {
        this.gp = gp;
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
        synchronized (gp.frameLock) {
            handleKeyPressed(e.getKeyCode());
        }
    }

    private void handleKeyPressed(int code) {
        if (gp.gameState == GameState.MENU) {
            gp.titleMenu.keyPressed(code);
            return;
        }

        if (code == KeyEvent.VK_TAB && gp.gameState != GameState.MOD_MENU) {
            gp.requestPrologueSkip();
            return;
        }

        if (code == KeyEvent.VK_F1 && gp.gameState != GameState.MOD_MENU) {
            gp.modMenu.open();
            return;
        }
        if (gp.gameState == GameState.MOD_MENU) {
            gp.modMenu.keyPressed(code);
            return;
        }

        if(gp.gameState == GameState.PLAYING){
            if (code == gp.controls.key(ControlSettings.Action.MOVE_UP)) {
                upPressed = true;
            }
            if (code == gp.controls.key(ControlSettings.Action.MOVE_DOWN)) {
                downPressed = true;
            }
            if (code == gp.controls.key(ControlSettings.Action.MOVE_LEFT)) {
                leftPressed = true;
            }
            if (code == gp.controls.key(ControlSettings.Action.MOVE_RIGHT)) {
                rightPressed = true;
            }

            if (code == gp.controls.key(ControlSettings.Action.AIM_UP)) {
                upShootPressed = true;
            }
            if (code == gp.controls.key(ControlSettings.Action.AIM_DOWN)) {
                downShootPressed = true;
            }
            if (code == gp.controls.key(ControlSettings.Action.AIM_LEFT)) {
                leftShootPressed = true;
            }
            if (code == gp.controls.key(ControlSettings.Action.AIM_RIGHT)) {
                rightShootPressed = true;
            }

            if (code == gp.controls.key(ControlSettings.Action.MANEUVER)) {
                spacePressed = true;
            }
            if (code == gp.controls.key(ControlSettings.Action.SPRINT)) {
                shiftPressed = true;
            }
            if (code == gp.controls.key(ControlSettings.Action.MODE_A)) {
                qPressed = true;
            }
            if (code == gp.controls.key(ControlSettings.Action.MODE_B)) {
                ePressed = true;
            }
            if (code == KeyEvent.VK_1) {
                onePressed = true;
            }
            if (code == KeyEvent.VK_2) {
                twoPressed = true;
            }
            if (code == KeyEvent.VK_M) {
                gp.audio.toggleMute();
                gp.ui.showMessage(gp.audio.isMuted() ? "Sound muted" : "Sound on");
            }
            if (code == KeyEvent.VK_F3) {
                gp.debugSettings.toggleOverlay();
            }
            if (code == KeyEvent.VK_F7) {
                gp.ui.showMessage(gp.ambience.toggle() ? "Ambience on" : "Ambience off");
            }
            if (code == KeyEvent.VK_F6) {
                gp.ui.showMessage(gp.lighting.toggle() ? "Lighting on" : "Lighting off");
            }
            if (code == KeyEvent.VK_F4 && gp.debugSettings.isOverlayEnabled()) {
                gp.debugDamageActiveBoss(100);
            }
            if (code == KeyEvent.VK_F5 && gp.debugSettings.isOverlayEnabled()) {
                gp.debugReloadArt();
            }

            if (code == KeyEvent.VK_ESCAPE) {
                gp.pauseGame();
                return;
            }
            if (code == gp.controls.key(ControlSettings.Action.JOURNAL)) {
                gp.questMenu.open();
                return;
            }

            if (code == KeyEvent.VK_ENTER) {
                enterPressed = true;
            }
        }
        else if(gp.gameState == GameState.PAUSED) {
            gp.pauseMenu.keyPressed(code);
        }
        else if(gp.gameState == GameState.SHOP) {
            gp.shopMenu.keyPressed(code);
        }
        else if(gp.gameState == GameState.QUESTS) {
            gp.questMenu.keyPressed(code);
        }
        else if(gp.gameState == GameState.SKILLS) {
            gp.skillTreeMenu.keyPressed(code);
        }
        else if(gp.gameState == GameState.DIALOGUE) {
            if (code == KeyEvent.VK_ENTER) {
                gp.dialogueManager.advanceBlocking();
            }
        }
        else if(gp.gameState == GameState.CUTSCENE) {
            if (code == KeyEvent.VK_ESCAPE) {
                gp.storySequenceManager.skipActiveSequence();
            }
            else if (code == KeyEvent.VK_ENTER) {
                gp.dialogueManager.advanceBlocking();
            }
        }
        else if(gp.gameState == GameState.DEAD) {
            if (code == KeyEvent.VK_ENTER || code == KeyEvent.VK_SPACE) {
                gp.restartAfterDeath();
            }
            else if (code == KeyEvent.VK_W || code == KeyEvent.VK_UP || code == KeyEvent.VK_A || code == KeyEvent.VK_LEFT) {
                gp.moveDeathChoice(-1);
            }
            else if (code == KeyEvent.VK_S || code == KeyEvent.VK_DOWN || code == KeyEvent.VK_D || code == KeyEvent.VK_RIGHT) {
                gp.moveDeathChoice(1);
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int code = e.getKeyCode();

        if (code == gp.controls.key(ControlSettings.Action.MOVE_UP)) {
            upPressed = false;
        }
        if (code == gp.controls.key(ControlSettings.Action.MOVE_DOWN)) {
            downPressed = false;
        }
        if (code == gp.controls.key(ControlSettings.Action.MOVE_LEFT)) {
            leftPressed = false;
        }
        if (code == gp.controls.key(ControlSettings.Action.MOVE_RIGHT)) {
            rightPressed = false;
        }

        if (code == gp.controls.key(ControlSettings.Action.AIM_UP)) {
            upShootPressed = false;
        }
        if (code == gp.controls.key(ControlSettings.Action.AIM_DOWN)) {
            downShootPressed = false;
        }
        if (code == gp.controls.key(ControlSettings.Action.AIM_LEFT)) {
            leftShootPressed = false;
        }
        if (code == gp.controls.key(ControlSettings.Action.AIM_RIGHT)) {
            rightShootPressed = false;
        }

        if (code == gp.controls.key(ControlSettings.Action.MANEUVER)) {
            spacePressed = false;
        }
        if (code == gp.controls.key(ControlSettings.Action.SPRINT)) {
            shiftPressed = false;
        }

        if (code == gp.controls.key(ControlSettings.Action.MODE_A)) {
            qPressed = false;
        }
        if (code == gp.controls.key(ControlSettings.Action.MODE_B)) {
            ePressed = false;
        }

        if (code == KeyEvent.VK_1) {
            onePressed = false;
        }
        if (code == KeyEvent.VK_2) {
            twoPressed = false;
        }
        if (code == KeyEvent.VK_ENTER) {
            enterPressed = false;
        }
    }

    public void clearGameplayInput() {
        upPressed = false;
        downPressed = false;
        leftPressed = false;
        rightPressed = false;
        upShootPressed = false;
        downShootPressed = false;
        leftShootPressed = false;
        rightShootPressed = false;
        shiftPressed = false;
        spacePressed = false;
        enterPressed = false;
        qPressed = false;
        ePressed = false;
        onePressed = false;
        twoPressed = false;
    }
}
