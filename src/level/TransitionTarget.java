package level;

public final class TransitionTarget {
    public enum Type {
        HUB,
        HUB_AT,
        INTERIOR,
        RUN_FLOOR,
        BOSS_ARENA,
        RETURN_FROM_BOSS,
        NEXT_FLOOR,
        FINISH_RUN
    }

    private final Type type;
    private final Interior interior;
    private final int col;
    private final int row;

    private TransitionTarget(Type type) {
        this(type, null, 0, 0);
    }

    private TransitionTarget(Type type, Interior interior, int col, int row) {
        if (type == null) {
            throw new IllegalArgumentException("Transition type is required");
        }
        this.type = type;
        this.interior = interior;
        this.col = col;
        this.row = row;
    }

    public static TransitionTarget hub() {
        return new TransitionTarget(Type.HUB);
    }

    public static TransitionTarget hubAt(int col, int row) {
        return new TransitionTarget(Type.HUB_AT, null, col, row);
    }

    public static TransitionTarget interior(Interior interior) {
        if (interior == null) {
            throw new IllegalArgumentException("Interior is required");
        }
        return new TransitionTarget(Type.INTERIOR, interior, 0, 0);
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

    public Interior interior() {
        return interior;
    }

    public int col() {
        return col;
    }

    public int row() {
        return row;
    }
}
