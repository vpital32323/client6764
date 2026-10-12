package com.nova.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.nova.util.Theme;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import org.joml.Matrix4f;

/** 2D drawing helpers for the GUI: rounded rectangles with gradients, outlines and soft shadows. */
public final class Render2D {
    private Render2D() {}

    private static final int SEG = 6; // smoothness of each rounded corner

    private static void begin() {
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
    }

    private static void finish() {
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
    }

    private static float[][] contour(float x, float y, float w, float h, float r) {
        r = Math.max(0f, Math.min(r, Math.min(w, h) / 2f));
        float[][] c = {{x + r, y + r}, {x + w - r, y + r}, {x + w - r, y + h - r}, {x + r, y + h - r}};
        double[] start = {Math.PI, 1.5 * Math.PI, 0, 0.5 * Math.PI};
        float[][] p = new float[(SEG + 1) * 4][2];
        int k = 0;
        for (int i = 0; i < 4; i++) {
            for (int s = 0; s <= SEG; s++) {
                double a = start[i] + (Math.PI / 2) * s / SEG;
                p[k][0] = c[i][0] + (float) Math.cos(a) * r;
                p[k][1] = c[i][1] + (float) Math.sin(a) * r;
                k++;
            }
        }
        return p;
    }

    private static int bil(int tl, int tr, int bl, int br, float u, float v) {
        u = Math.max(0, Math.min(1, u));
        v = Math.max(0, Math.min(1, v));
        return Theme.lerp(Theme.lerp(tl, tr, u), Theme.lerp(bl, br, u), v);
    }

    // ---------- filled ----------
    public static void rect(DrawContext ctx, float x, float y, float w, float h, float r, int c) {
        rect4(ctx, x, y, w, h, r, c, c, c, c);
    }

    public static void rectV(DrawContext ctx, float x, float y, float w, float h, float r, int top, int bottom) {
        rect4(ctx, x, y, w, h, r, top, top, bottom, bottom);
    }

    public static void rectH(DrawContext ctx, float x, float y, float w, float h, float r, int left, int right) {
        rect4(ctx, x, y, w, h, r, left, right, left, right);
    }

    public static void rect4(DrawContext ctx, float x, float y, float w, float h, float r,
                             int tl, int tr, int bl, int br) {
        if (w <= 0 || h <= 0) return;
        if ((tl >>> 24) == 0 && (tr >>> 24) == 0 && (bl >>> 24) == 0 && (br >>> 24) == 0) return;
        float[][] p = contour(x, y, w, h, r);
        ctx.draw(); // flush text/fills queued before so the order stays correct
        Matrix4f m = ctx.getMatrices().peek().getPositionMatrix();
        begin();
        BufferBuilder b = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
        b.vertex(m, x + w / 2f, y + h / 2f, 0).color(bil(tl, tr, bl, br, 0.5f, 0.5f));
        for (float[] pt : p) {
            b.vertex(m, pt[0], pt[1], 0).color(bil(tl, tr, bl, br, (pt[0] - x) / w, (pt[1] - y) / h));
        }
        b.vertex(m, p[0][0], p[0][1], 0).color(bil(tl, tr, bl, br, (p[0][0] - x) / w, (p[0][1] - y) / h));
        BufferRenderer.drawWithGlobalProgram(b.end());
        finish();
    }

    // ---------- outline (horizontal gradient c1 -> c2) ----------
    public static void outline(DrawContext ctx, float x, float y, float w, float h, float r, float t, int c1, int c2) {
        if (w <= 0 || h <= 0) return;
        if ((c1 >>> 24) == 0 && (c2 >>> 24) == 0) return;
        float[][] o = contour(x, y, w, h, r);
        float[][] in = contour(x + t, y + t, w - 2 * t, h - 2 * t, Math.max(0f, r - t));
        ctx.draw();
        Matrix4f m = ctx.getMatrices().peek().getPositionMatrix();
        begin();
        BufferBuilder b = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
        int n = o.length;
        for (int i = 0; i <= n; i++) {
            float[] po = o[i % n], pi = in[i % n];
            b.vertex(m, po[0], po[1], 0).color(Theme.lerp(c1, c2, (po[0] - x) / w));
            b.vertex(m, pi[0], pi[1], 0).color(Theme.lerp(c1, c2, (pi[0] - x) / w));
        }
        BufferRenderer.drawWithGlobalProgram(b.end());
        finish();
    }

    // ---------- soft shadow around a rounded rect (does not darken the inside) ----------
    public static void shadow(DrawContext ctx, float x, float y, float w, float h, float r, int rgb, int maxAlpha) {
        for (int i = 1; i <= 7; i++) {
            double k = 1.0 - i / 8.0;
            int a = (int) (maxAlpha * k * k * 0.5);
            outline(ctx, x - i, y - i + 2, w + 2 * i, h + 2 * i, r + i, 1f, Theme.argb(rgb, a), Theme.argb(rgb, a));
        }
    }
}
