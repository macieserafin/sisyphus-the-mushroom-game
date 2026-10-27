package entity.anim;

import entity.Direction8;

public final class AnimationPlayer {
    private SpriteSheet sheet;
    private int frame;
    private int ticksInFrame;
    private boolean finished;

    public void play(SpriteSheet next, boolean keepPhase) {
        if (next == sheet) {
            return;
        }
        sheet = next;
        finished = false;
        if (!keepPhase) {
            frame = 0;
            ticksInFrame = 0;
        } else if (sheet != null) {
            frame = Math.floorMod(frame, sheet.frameCount);
        }
    }

    public void restart(SpriteSheet next) {
        sheet = next;
        frame = 0;
        ticksInFrame = 0;
        finished = false;
    }

    public void update() {
        if (sheet == null || finished) {
            return;
        }
        ticksInFrame++;
        if (ticksInFrame < sheet.frameTicks) {
            return;
        }
        ticksInFrame = 0;
        if (frame + 1 < sheet.frameCount) {
            frame++;
        } else if (sheet.loop) {
            frame = 0;
        } else {
            finished = true;
        }
    }

    public SpriteSheet sheet() {
        return sheet;
    }

    public int frameIndex() {
        return frame;
    }

    public boolean isFinished() {
        return finished;
    }

    public SpriteSheet.Frame currentFrame(Direction8 facing) {
        return sheet == null ? null : sheet.frame(facing, frame);
    }
}
