package entity;

public enum Direction8 {
    S(0, 1),
    SE(1, 1),
    E(1, 0),
    NE(1, -1),
    N(0, -1),
    NW(-1, -1),
    W(-1, 0),
    SW(-1, 1);

    public final int dx;
    public final int dy;
    public final double unitX;
    public final double unitY;

    Direction8(int dx, int dy) {
        this.dx = dx;
        this.dy = dy;
        double length = Math.sqrt(dx * dx + dy * dy);
        this.unitX = dx / length;
        this.unitY = dy / length;
    }

    public double angle() {
        return Math.atan2(dy, dx);
    }

    public boolean isWestward() {
        return dx < 0;
    }

    public Direction8 mirroredToEast() {
        switch (this) {
            case W: return E;
            case NW: return NE;
            case SW: return SE;
            default: return this;
        }
    }

    public boolean isDiagonal() {
        return dx != 0 && dy != 0;
    }

    public static Direction8 fromVector(double x, double y) {
        if (Math.abs(x) < 1e-9 && Math.abs(y) < 1e-9) {
            return null;
        }
        int octant = (int) Math.round(Math.atan2(y, x) / (Math.PI / 4));
        switch (Math.floorMod(octant, 8)) {
            case 0: return E;
            case 1: return SE;
            case 2: return S;
            case 3: return SW;
            case 4: return W;
            case 5: return NW;
            case 6: return N;
            default: return NE;
        }
    }

    public static Direction8 fromVector(double x, double y, Direction8 current, double hysteresisRadians) {
        Direction8 nearest = fromVector(x, y);
        if (nearest == null || current == null || nearest == current) {
            return nearest == null ? current : nearest;
        }
        double off = Math.abs(angleBetween(Math.atan2(y, x), current.angle()));
        return off > Math.PI / 8 + hysteresisRadians ? nearest : current;
    }

    public static Direction8 cardinalFromVector(double x, double y) {
        if (Math.abs(x) < 1e-9 && Math.abs(y) < 1e-9) {
            return null;
        }
        if (Math.abs(x) > Math.abs(y)) {
            return x > 0 ? E : W;
        }
        return y > 0 ? S : N;
    }

    public static Direction8 fromLegacyName(String name) {
        if (name == null) {
            return null;
        }
        switch (name) {
            case "up": return N;
            case "down": return S;
            case "left": return W;
            case "right": return E;
            default: return null;
        }
    }

    private static double angleBetween(double a, double b) {
        double d = a - b;
        while (d > Math.PI) {
            d -= 2 * Math.PI;
        }
        while (d < -Math.PI) {
            d += 2 * Math.PI;
        }
        return d;
    }
}
