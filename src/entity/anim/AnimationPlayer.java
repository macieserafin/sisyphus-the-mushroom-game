package entity.anim;

import entity.Direction8;

public final class AnimationPlayer {
    private SpriteSheet sheet;
    private int frame;
    private double frameProgress;
    private boolean finished;

    public void play(SpriteSheet next, boolean keepPhase) {
        if (next == sheet) {
            return;
        }
        sheet = next;
        finished = false;
        if (!keepPhase) {
            frame = 0;
            frameProgress = 0;
        } else if (sheet != null) {
            frame = Math.floorMod(frame, sheet.frameCount);
        }
    }

    public void restart(SpriteSheet next) {
        sheet = next;
        frame = 0;
        frameProgress = 0;
        finished = false;
    }

    public void update() {
        if (sheet != null) {
            advance(1.0 / sheet.frameTicks);
        }
    }

    public void advance(double frames) {
        if (sheet == null || finished || frames <= 0) {
            return;
        }
        frameProgress += frames;
        while (frameProgress >= 1 - 1e-9) {
            frameProgress -= 1;
            if (frame + 1 < sheet.frameCount) {
                frame++;
            } else if (sheet.loop) {
                frame = 0;
            } else {
                finished = true;
                frameProgress = 0;
                return;
            }
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
