package main;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class KeyHandler implements KeyListener {
    public boolean upPressed, downPressed, leftPressed, rightPressed;
    public boolean upShootPressed, downShootPressed, leftShootPressed, rightShootPressed;
    public boolean shiftPressed, spacePressed, enterPressed;
    public boolean qPressed, pPressed, onePressed, twoPressed;

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

        // TAB jumps past the whole prologue straight to the village.
        if (code == KeyEvent.VK_TAB) {
            gp.requestPrologueSkip();
            return;
        }

        if(gp.gameState == GameState.PLAYING){
            if (code == KeyEvent.VK_W) {
                upPressed = true;
            }
            if (code == KeyEvent.VK_S) {
                downPressed = true;
            }
            if (code == KeyEvent.VK_A) {
                leftPressed = true;
            }
            if (code == KeyEvent.VK_D) {
                rightPressed = true;
            }

            if (code == KeyEvent.VK_UP) {
                upShootPressed = true;
            }
            if (code == KeyEvent.VK_DOWN) {
                downShootPressed = true;
            }
            if (code == KeyEvent.VK_LEFT) {
                leftShootPressed = true;
            }
            if (code == KeyEvent.VK_RIGHT) {
                rightShootPressed = true;
            }

            if (code == KeyEvent.VK_SPACE) {
                spacePressed = true;
            }
            if (code == KeyEvent.VK_SHIFT) {
                shiftPressed = true;
            }
            if (code == KeyEvent.VK_Q) {
                qPressed = true;
            }
            if (code == KeyEvent.VK_1) {
                onePressed = true;
            }
            if (code == KeyEvent.VK_2) {
                twoPressed = true;
            }
            if (code == KeyEvent.VK_F3) {
                gp.debugSettings.toggleOverlay();
            }
            if (code == KeyEvent.VK_F4 && gp.debugSettings.isOverlayEnabled()) {
                gp.debugDamageActiveBoss(100);
            }

            if (code == KeyEvent.VK_P) {
                pPressed = true;
                gp.gameState = GameState.PAUSED;
                gp.pauseMusic(0);
            }

            if (code == KeyEvent.VK_ENTER) {
                enterPressed = true;

            }
        }
        else if(gp.gameState == GameState.PAUSED) {
            if (code == KeyEvent.VK_P) {
                pPressed = true;
                gp.gameState = GameState.PLAYING;
                gp.playAreaMusic();
            }

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
            if (code == KeyEvent.VK_ENTER) {
                gp.restartAfterDeath();
            }
        }

    }

    @Override
    public void keyReleased(KeyEvent e) {
        int code = e.getKeyCode();

        if (code == KeyEvent.VK_W) {
            upPressed = false;
        }
        if (code == KeyEvent.VK_S) {
            downPressed = false;
        }
        if (code == KeyEvent.VK_A) {
            leftPressed = false;
        }
        if (code == KeyEvent.VK_D) {
            rightPressed = false;
        }

        if (code == KeyEvent.VK_UP) {
            upShootPressed = false;
        }
        if (code == KeyEvent.VK_DOWN) {
            downShootPressed = false;
        }
        if (code == KeyEvent.VK_LEFT) {
            leftShootPressed = false;
        }
        if (code == KeyEvent.VK_RIGHT) {
            rightShootPressed = false;
        }

        if (code == KeyEvent.VK_SPACE) {
            spacePressed = false;
        }
        if (code == KeyEvent.VK_SHIFT) {
            shiftPressed = false;
        }

        if (code == KeyEvent.VK_Q) {
            qPressed = false;
        }

        if (code == KeyEvent.VK_P) {
            pPressed = false;
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
        onePressed = false;
        twoPressed = false;
    }
}
