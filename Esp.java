package com.nova.module.impl;

import com.nova.util.Theme;
import com.nova.module.Category;
import com.nova.module.Module;
import com.nova.render.RenderUtil;
import com.nova.setting.BooleanSetting;
import com.nova.setting.NumberSetting;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/** Outlines other players with a box. */
public class Esp extends Module {
    private final BooleanSetting walls = new BooleanSetting("Through walls", true);
    private final BooleanSetting rainbow = new BooleanSetting("Rainbow", false);
    private final NumberSetting width = new NumberSetting("Width", 2.0, 1.0, 5.0, 0.5);
    private final NumberSetting distance = new NumberSetting("Distance", 128, 16, 256, 16);

    public Esp() {
        super("ESP", Category.RENDER);
        addSettings(walls, rainbow, width, distance);
    }

    @Override
    public void onRender3D(WorldRenderContext ctx) {
        if (mc.world == null || mc.player == null || ctx.matrixStack() == null) return;
        float td = mc.getRenderTickCounter().getTickDelta(false);
        Vec3d cam = ctx.camera().getPos();
        double maxSq = distance.value * distance.value;

        for (PlayerEntity p : mc.world.getPlayers()) {
            if (p == mc.player || !p.isAlive()) continue;
            if (mc.player.squaredDistanceTo(p) > maxSq) continue;
            Vec3d lerped = p.getLerpedPos(td);
            Box bb = p.getBoundingBox().offset(lerped.subtract(p.getPos())).expand(0.05);
            int color = rainbow.value ? RenderUtil.rainbow(0f) : Theme.A1;
            RenderUtil.boxOutline(ctx.matrixStack(), cam, bb, color, (float) width.value, walls.value);
        }
    }
}
