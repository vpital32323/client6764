package com.nova.util;

/** Colours of the client. Change A1/A2 to re-colour the whole client. */
public final class Theme {
    private Theme() {}

    public static int A1 = 0xFF8B5CF6; // accent 1 (violet)
    public static int A2 = 0xFFEC4899; // accent 2 (pink)

    public static int argb(int rgb, int a) {
        return (Math.max(0, Math.min(255, a)) << 24) | (rgb & 0xFFFFFF);
    }

    /** Multiply the alpha channel. */
    public static int mul(int argb, double m) {
        int a = (int) Math.round(((argb >>> 24) & 255) * m);
        return argb(argb, a);
    }

    public static int lerp(int c1, int c2, double t) {
        t = Math.max(0, Math.min(1, t));
        int a = (int) (((c1 >>> 24) & 255) + (((c2 >>> 24) & 255) - ((c1 >>> 24) & 255)) * t);
        int r = (int) (((c1 >> 16) & 255) + (((c2 >> 16) & 255) - ((c1 >> 16) & 255)) * t);
        int g = (int) (((c1 >> 8) & 255) + (((c2 >> 8) & 255) - ((c1 >> 8) & 255)) * t);
        int b = (int) ((c1 & 255) + ((c2 & 255) - (c1 & 255)) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
