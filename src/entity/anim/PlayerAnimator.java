package entity.anim;

import entity.Direction8;
import entity.weapon.MeleeWeapon;

public final class PlayerAnimator {

    public enum State { IDLE, WALK, SPRINT, SHOOT, ATTACK, HURT, DEATH }

    private static final String IDLE = "player/anim/player_idle";
    private static final String WALK = "player/anim/player_walk";
    private static final String SPRINT = "player/anim/player_sprint";
    private static final String HURT = "player/anim/player_hurt";
    private static final String DEATH = "player/anim/player_death";
    private final AnimationPlayer locomotion = new AnimationPlayer();
    private final AnimationPlayer reaction = new AnimationPlayer();

    private State locomotionState = State.IDLE;
    private Direction8 facing = Direction8.S;

    private State reactionState;
    private Direction8 reactionDirection;

    private State poseState;
    private SpriteSheet poseSheet;
    private int poseFrame;
    private Direction8 poseDirection;

    public void reset() {
        locomotion.restart(SpriteSheet.load(IDLE));
        locomotionState = State.IDLE;
        facing = Direction8.S;
        clearReaction();
        clearPose();
    }

    public void locomote(boolean moving, boolean sprinting, double artPixelsMoved, Direction8 facing) {
        SpriteSheet sprintSheet = SpriteSheet.loadOptional(SPRINT);
        SpriteSheet sheet = SpriteSheet.load(IDLE);
        State state = State.IDLE;
        if (moving && sprinting && sprintSheet != null) {
            sheet = sprintSheet;
            state = State.SPRINT;
        } else if (moving) {
            sheet = SpriteSheet.load(WALK);
            state = State.WALK;
        }
        locomotion.play(sheet, true);
        if (moving && sheet.distancePerFrame > 0) {
            locomotion.advance(artPixelsMoved / sheet.distancePerFrame);
        } else {
            locomotion.update();
        }
        locomotionState = state;
        if (moving && facing != null) {
            this.facing = facing;
        }
    }

    public void hurt(boolean active) {
        SpriteSheet hurtSheet = SpriteSheet.loadOptional(HURT);
        if (!active || hurtSheet == null) {
            if (reactionState == State.HURT) {
                clearReaction();
            }
            return;
        }
        if (reactionState != State.HURT) {
            reactionState = State.HURT;
            reactionDirection = facing;
            reaction.restart(hurtSheet);
        } else {
            reaction.update();
        }
    }

    public boolean hasDeathAnimation() {
        return SpriteSheet.loadOptional(DEATH) != null;
    }

    public void die() {
        SpriteSheet deathSheet = SpriteSheet.loadOptional(DEATH);
        if (deathSheet == null) {
            return;
        }
        reactionState = State.DEATH;
        reactionDirection = facing;
        reaction.restart(deathSheet);
    }

    public void updateDeath() {
        if (reactionState == State.DEATH) {
            reaction.update();
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
        facing = direction;
    }

    public void clearPose() {
        poseState = null;
        poseSheet = null;
        poseDirection = null;
    }

    private void clearReaction() {
        reactionState = null;
        reactionDirection = null;
        reaction.restart(null);
    }

    public State state() {
        if (reactionState == State.DEATH) {
            return State.DEATH;
        }
        if (poseSheet != null) {
            return poseState;
        }
        return reactionState != null ? reactionState : locomotionState;
    }

    public Direction8 direction() {
        if (reactionState == State.DEATH) {
            return reactionDirection;
        }
        if (poseSheet != null) {
            return poseDirection;
        }
        return reactionState != null ? reactionDirection : facing;
    }

    public SpriteSheet.Frame frame() {
        if (reactionState == State.DEATH) {
            return reaction.currentFrame(reactionDirection);
        }
        if (poseSheet != null) {
            return poseSheet.frame(poseDirection, poseFrame);
        }
        if (reactionState != null) {
            return reaction.currentFrame(reactionDirection);
        }
        SpriteSheet.Frame frame = locomotion.currentFrame(facing);
        return frame != null ? frame : SpriteSheet.load(IDLE).frame(facing, 0);
    }

    public static int attackFrame(MeleeWeapon weapon, int weaponFrame, int sheetFrames) {
        if (sheetFrames <= 1) {
            return 0;
        }
        int strikeStart = -1;
        int strikeEnd = -1;
        for (int f = 0; f < weapon.totalFrames(); f++) {
            if (weapon.isStrikeFrame(f)) {
                if (strikeStart < 0) {
                    strikeStart = f;
                }
                strikeEnd = f + 1;
            }
        }
        if (strikeStart < 0 || weaponFrame < strikeStart) {
            return 0;
        }
        if (weaponFrame >= strikeEnd) {
            return sheetFrames == 2 ? 0 : sheetFrames - 1;
        }
        if (sheetFrames <= 3) {
            return 1;
        }
        int strikeFrames = sheetFrames - 2;
        int index = (weaponFrame - strikeStart) * strikeFrames / (strikeEnd - strikeStart);
        return 1 + Math.min(strikeFrames - 1, index);
    }

    public static int shootFrame(SpriteSheet sheet, boolean recoil) {
        return recoil && sheet != null && sheet.frameCount > 1 ? 1 : 0;
    }
}
