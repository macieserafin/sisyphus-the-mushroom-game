package entity.anim;

import entity.Direction8;

public final class PlayerAnimator {

    public enum State { IDLE, WALK, SHOOT, ATTACK }

    private final SpriteSheet idleSheet = SpriteSheet.load("player/anim/player_idle");
    private final SpriteSheet walkSheet = SpriteSheet.load("player/anim/player_walk");
    private final AnimationPlayer locomotion = new AnimationPlayer();

    private State locomotionState = State.IDLE;
    private Direction8 facing = Direction8.S;

    private State poseState;
    private SpriteSheet poseSheet;
    private int poseFrame;
    private Direction8 poseDirection;

    public void reset() {
        locomotion.restart(idleSheet);
        locomotionState = State.IDLE;
        facing = Direction8.S;
        clearPose();
    }

    public void locomote(boolean moving, double artPixelsMoved, Direction8 facing) {
        SpriteSheet sheet = moving ? walkSheet : idleSheet;
        locomotion.play(sheet, true);
        if (moving && sheet.distancePerFrame > 0) {
            locomotion.advance(artPixelsMoved / sheet.distancePerFrame);
        } else {
            locomotion.update();
        }
        locomotionState = moving ? State.WALK : State.IDLE;
        if (facing != null) {
            this.facing = facing;
        }
    }

    public void holdPose(State state, SpriteSheet sheet, int frame, Direction8 direction) {
        if (sheet == null || direction == null) {
            clearPose();
            return;
        }
        poseState = state;
        poseSheet = sheet;
        poseFrame = frame;
        poseDirection = direction;
    }

    public void clearPose() {
        poseState = null;
        poseSheet = null;
        poseDirection = null;
    }

    public State state() {
        return poseSheet != null ? poseState : locomotionState;
    }

    public Direction8 direction() {
        return poseSheet != null ? poseDirection : facing;
    }

    public SpriteSheet.Frame frame() {
        if (poseSheet != null) {
            return poseSheet.frame(poseDirection, poseFrame);
        }
        SpriteSheet.Frame frame = locomotion.currentFrame(facing);
        return frame != null ? frame : idleSheet.frame(facing, 0);
    }
}
