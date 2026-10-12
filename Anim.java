package com.nova.util;

/** Smooth value that glides to a target, independent of FPS. */
public class Anim {
    public double value;
    private long last = System.nanoTime();

    public Anim(double start) { this.value = start; }

    public double update(double target, double speed) {
        long now = System.nanoTime();
        double dt = Math.min((now - last) / 1e9, 0.1);
        last = now;
        value += (target - value) * (1 - Math.exp(-speed * dt));
        if (Math.abs(target - value) < 0.001) value = target;
        return value;
    }

    public static double ease(double t) {
        t = Math.max(0, Math.min(1, t));
        return 1 - Math.pow(1 - t, 3);
    }
}
