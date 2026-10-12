package com.nova.setting;

public class NumberSetting extends Setting {
    public double value;
    public final double min, max, step;
    public NumberSetting(String name, double value, double min, double max, double step) {
        super(name); this.value = value; this.min = min; this.max = max; this.step = step;
    }
    public void add(int dir) {
        value = Math.max(min, Math.min(max, Math.round((value + dir * step) / step) * step));
    }
    public double fraction() { return (value - min) / (max - min); }
    /** Set value from slider position 0..1 (snaps to step). */
    public void setFraction(double f) {
        f = Math.max(0, Math.min(1, f));
        double v = min + (max - min) * f;
        value = Math.max(min, Math.min(max, Math.round(v / step) * step));
    }
}
