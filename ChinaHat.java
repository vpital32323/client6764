package com.nova.module.impl;

import com.nova.module.Category;
import com.nova.module.Module;
import com.nova.render.RenderUtil;
import com.nova.setting.BooleanSetting;
import com.nova.setting.NumberSetting;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

/** Cone-shaped "China hat" above the head. Visible in third person. */
public class ChinaHat extends Module {
    private final BooleanSetting rainbow = new BooleanSetting("Rainbow", true);
    private final BooleanSetting others = new BooleanSetting("Others", false);
    private final NumberSetting size = new NumberSetting("Size", 0.55, 0.3, 1.0, 0.05);
    private final NumberSetting opacity = new NumberSetting("Opacity", 110, 30, 255, 10);

    public ChinaHat() {
        super("ChinaHat", Category.RENDER);
        addSettings(rainbow, others, size, opacity);
    }

    @Override
    public void onRender3D(WorldRenderContext ctx) {
        if (mc.world == null || mc.player == null || ctx.matrixStack() == null) return;
        float td = mc.getRenderTickCounter().getTickDelta(false);
        Vec3d cam = ctx.camera().getPos();

        for (PlayerEntity p : mc.world.getPlayers()) {
            boolean self = p == mc.player;
            if (self && mc.options.getPerspective().isFirstPerson()) continue;
            if (!self && !others.value) continue;
            if (!p.isAlive()) continue;

            Vec3d pos = p.getLerpedPos(td);
            Vec3d base = new Vec3d(pos.x, pos.y + p.getHeight() + 0.02, pos.z);
            RenderUtil.cone(ctx.matrixStack(), cam, base, size.value, size.value * 0.5,
                    40, rainbow.value, (int) opacity.value, 2f);
        }
    }
}
