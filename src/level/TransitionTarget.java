package level;

public final class TransitionTarget {
    public enum Type {
        HUB,
        RUN_FLOOR,
        BOSS_ARENA,
        RETURN_FROM_BOSS,
        NEXT_FLOOR,
        FINISH_RUN
    }

    private final Type type;

    private TransitionTarget(Type type) {
        if (type == null) {
            throw new IllegalArgumentException("Transition type is required");
        }
        this.type = type;
    }

    public static TransitionTarget hub() {
        return new TransitionTarget(Type.HUB);
    }

    public static TransitionTarget runFloor() {
        return new TransitionTarget(Type.RUN_FLOOR);
    }

    public static TransitionTarget bossArena() {
        return new TransitionTarget(Type.BOSS_ARENA);
    }

    public static TransitionTarget returnFromBoss() {
        return new TransitionTarget(Type.RETURN_FROM_BOSS);
    }

    public static TransitionTarget nextFloor() {
        return new TransitionTarget(Type.NEXT_FLOOR);
    }

    public static TransitionTarget finishRun() {
        return new TransitionTarget(Type.FINISH_RUN);
    }

    public Type type() {
        return type;
    }
}
