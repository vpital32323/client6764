package com.nova.render;

import com.nova.util.Theme;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

public final class RenderUtil {
    private RenderUtil() {}

    private static void begin(boolean depth, float lineWidth) {
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.lineWidth(lineWidth);
        RenderSystem.disableCull();
        if (depth) RenderSystem.enableDepthTest(); else RenderSystem.disableDepthTest();
    }

    private static void end() {
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.lineWidth(1f);
    }

    /** Rainbow colour (ARGB, opaque). */
    public static int rainbow(float offset) {
        float hue = (offset + (System.currentTimeMillis() % 4000L) / 4000f) % 1f;
        return 0xFF000000 | (MathHelper.hsvToRgb(hue, 0.7f, 1f) & 0xFFFFFF);
    }

    private static void line(BufferBuilder b, Matrix4f m, float x1, float y1, float z1, float x2, float y2, float z2, int c) {
        b.vertex(m, x1, y1, z1).color(c);
        b.vertex(m, x2, y2, z2).color(c);
    }

    /** 3D box outline. Coordinates are world coords, cam = camera position. */
    public static void boxOutline(MatrixStack ms, Vec3d cam, Box bb, int argb, float lineWidth, boolean throughWalls) {
        Matrix4f m = ms.peek().getPositionMatrix();
        float x1 = (float) (bb.minX - cam.x), y1 = (float) (bb.minY - cam.y), z1 = (float) (bb.minZ - cam.z);
        float x2 = (float) (bb.maxX - cam.x), y2 = (float) (bb.maxY - cam.y), z2 = (float) (bb.maxZ - cam.z);

        begin(!throughWalls, lineWidth);
        BufferBuilder b = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        // bottom
        line(b, m, x1, y1, z1, x2, y1, z1, argb);
        line(b, m, x2, y1, z1, x2, y1, z2, argb);
        line(b, m, x2, y1, z2, x1, y1, z2, argb);
        line(b, m, x1, y1, z2, x1, y1, z1, argb);
        // top
        line(b, m, x1, y2, z1, x2, y2, z1, argb);
        line(b, m, x2, y2, z1, x2, y2, z2, argb);
        line(b, m, x2, y2, z2, x1, y2, z2, argb);
        line(b, m, x1, y2, z2, x1, y2, z1, argb);
        // pillars
        line(b, m, x1, y1, z1, x1, y2, z1, argb);
        line(b, m, x2, y1, z1, x2, y2, z1, argb);
        line(b, m, x2, y1, z2, x2, y2, z2, argb);
        line(b, m, x1, y1, z2, x1, y2, z2, argb);
        BufferRenderer.drawWithGlobalProgram(b.end());
        end();
    }

    /** "China hat": translucent cone with an outline ring. base = centre of the cone's bottom. */
    public static void cone(MatrixStack ms, Vec3d cam, Vec3d base, double radius, double height,
                            int segments, boolean rainbow, int fillAlpha, float lineWidth) {
        Matrix4f m = ms.peek().getPositionMatrix();
        float bx = (float) (base.x - cam.x), by = (float) (base.y - cam.y), bz = (float) (base.z - cam.z);
        float r = (float) radius, h = (float) height;

        begin(true, lineWidth);

        BufferBuilder fan = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
        fan.vertex(m, bx, by + h, bz).color(shade(rainbow, 0f, fillAlpha));
        for (int i = 0; i <= segments; i++) {
            double a = Math.PI * 2 * i / segments;
            fan.vertex(m, bx + (float) Math.cos(a) * r, by, bz + (float) Math.sin(a) * r)
                    .color(shade(rainbow, (float) i / segments, fillAlpha));
        }
        BufferRenderer.drawWithGlobalProgram(fan.end());

        BufferBuilder ring = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
        for (int i = 0; i <= segments; i++) {
            double a = Math.PI * 2 * i / segments;
            ring.vertex(m, bx + (float) Math.cos(a) * r, by, bz + (float) Math.sin(a) * r)
                    .color(shade(rainbow, (float) i / segments, 255));
        }
        BufferRenderer.drawWithGlobalProgram(ring.end());

        end();
    }

    private static int shade(boolean rainbow, float offset, int alpha) {
        int rgb = rainbow ? (rainbow(offset) & 0xFFFFFF) : (Theme.A1 & 0xFFFFFF);
        return (alpha << 24) | rgb;
    }
}
