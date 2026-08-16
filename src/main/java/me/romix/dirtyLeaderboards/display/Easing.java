package me.romix.dirtyLeaderboards.display;

public final class Easing {

    private Easing() {
    }

    public static double outCubic(double x) {
        return 1 - Math.pow(1 - x, 3);
    }

    public static double inCubic(double x) {
        return x * x * x;
    }
}
