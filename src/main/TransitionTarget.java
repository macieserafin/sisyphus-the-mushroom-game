package main;

public final class TransitionTarget {
    private final TransitionType type;

    private TransitionTarget(TransitionType type) {
        if (type == null) {
            throw new IllegalArgumentException("Transition type is required");
        }
        this.type = type;
    }

    public static TransitionTarget hub() {
        return new TransitionTarget(TransitionType.HUB);
    }

    public static TransitionTarget runFloor() {
        return new TransitionTarget(TransitionType.RUN_FLOOR);
    }

    public TransitionType type() {
        return type;
    }
}
