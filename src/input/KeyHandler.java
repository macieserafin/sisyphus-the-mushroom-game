package input;

import audio.Sound;
import main.GamePanel;
import main.GameState;
import settings.ControlSettings;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class KeyHandler implements KeyListener {
    public boolean upPressed, downPressed, leftPressed, rightPressed;
    public boolean upShootPressed, downShootPressed, leftShootPressed, rightShootPressed;
    public boolean shiftPressed, spacePressed, enterPressed;
    public boolean qPressed, ePressed, onePressed, twoPressed;

    private boolean confirmHeld;
    private boolean muteHeld;
    private boolean documentEscapeHeld;
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
        if (code == KeyEvent.VK_ESCAPE && documentEscapeHeld) return;
        if (code == ControlSettings.MUTE_KEY) {
            if (muteHeld) return;
            muteHeld = true;
            gp.audio.toggleMute();
            gp.ui.showMessage(gp.audio.isMuted() ? "Sound muted" : "Sound on");
            return;
        }
        if (gp.isBlinkTransition() || gp.campaign.victoryInProgress() && gp.gameState == GameState.PLAYING) return;
        if (gp.gameState == GameState.MENU) {
            gp.titleMenu.keyPressed(code);
            return;
        }
        if(gp.gameState==GameState.PLAYING&&gp.campaign.started()
                &&(!gp.player.alive||gp.campaign.rebirthing())&&code!=KeyEvent.VK_F1)return;
        if(gp.debugSettings.timeKeys()&&(gp.gameState==GameState.PLAYING||gp.gameState==GameState.MOD_MENU)
                &&(code==KeyEvent.VK_OPEN_BRACKET||code==KeyEvent.VK_CLOSE_BRACKET)
                &&gp.player.alive&&!gp.campaign.rebirthing()&&!gp.narrative.busy()&&!gp.storySequenceManager.isActive()&&!gp.narrativeView.modal()) {
            gp.narrative.state.shiftMinutes(code==KeyEvent.VK_CLOSE_BRACKET?30:-30);
            gp.saves.requestProgressSave();return;
        }

        if(code==KeyEvent.VK_F1 && gp.gameState!=GameState.MOD_MENU){gp.modMenu.open();return;}
        if(code==KeyEvent.VK_ENTER) {
            if(confirmHeld && (gp.narrativeView.modal() || gp.gameState==GameState.DIALOGUE || gp.gameState==GameState.CUTSCENE || gp.gameState==GameState.CHOICE))return;
            confirmHeld=true;
        }
        if (code == KeyEvent.VK_ESCAPE && gp.narrativeView.documentVisible()
                && gp.gameState != GameState.MOD_MENU && gp.gameState != GameState.CHOICE) {
            documentEscapeHeld = true;
            gp.narrativeView.closeDocument();
            return;
        }
        if (gp.narrativeView.modal() && gp.gameState != GameState.MOD_MENU) { if (code == KeyEvent.VK_ENTER) gp.narrativeView.confirm(); return; }
        if (gp.narrativeView.documentVisible() && gp.gameState != GameState.MOD_MENU && gp.gameState != GameState.CHOICE) {
            boolean up = code == KeyEvent.VK_UP || code == gp.controls.key(ControlSettings.Action.MOVE_UP);
            boolean down = code == KeyEvent.VK_DOWN || code == gp.controls.key(ControlSettings.Action.MOVE_DOWN);
            if (up || down) { gp.narrativeView.page(up ? -1 : 1); return; }
            if (gp.narrativeView.reading()) { if (code == KeyEvent.VK_ENTER || code == KeyEvent.VK_SPACE || code == KeyEvent.VK_ESCAPE) gp.narrativeView.readConfirm(code == KeyEvent.VK_ESCAPE); return; }
        }
        if (gp.gameState == GameState.MAP) { gp.worldMap.key(code); return; }
        if (gp.gameState == GameState.CHOICE) { gp.choiceMenu.handle(code); return; }
        if(code==KeyEvent.VK_ESCAPE && gp.narrative.busy()){gp.narrative.requestSkip();return;}
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
            if(code==gp.controls.key(ControlSettings.Action.MINIMAP_ZOOM_IN)
                    || code==KeyEvent.VK_ADD && gp.controls.key(ControlSettings.Action.MINIMAP_ZOOM_IN)==KeyEvent.VK_EQUALS) {
                gp.minimapView.adjust(1);return;
            }
            if(code==gp.controls.key(ControlSettings.Action.MINIMAP_ZOOM_OUT)
                    || code==KeyEvent.VK_SUBTRACT && gp.controls.key(ControlSettings.Action.MINIMAP_ZOOM_OUT)==KeyEvent.VK_MINUS) {
                gp.minimapView.adjust(-1);return;
            }
            if (code == gp.controls.key(ControlSettings.Action.MAP)) { gp.worldMap.open(); return; }
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

            if (code == gp.controls.key(ControlSettings.Action.INVENTORY)) { gp.relics.open(); return; }
            if (code == gp.controls.key(ControlSettings.Action.NOTES)) { gp.questMenu.openNotes(); return; }
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
        if(e.getKeyCode()==KeyEvent.VK_ENTER)confirmHeld=false;
        if(e.getKeyCode()==ControlSettings.MUTE_KEY)muteHeld=false;
        if(e.getKeyCode()==KeyEvent.VK_ESCAPE)documentEscapeHeld=false;
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
