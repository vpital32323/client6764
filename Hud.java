package com.nova.module.impl;

import com.nova.Nova;
import com.nova.module.Category;
import com.nova.module.Module;
import com.nova.setting.BooleanSetting;
import com.nova.util.Anim;
import com.nova.util.Theme;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Hud extends Module {
    public final BooleanSetting watermark = new BooleanSetting("Watermark", true);
    public final BooleanSetting arrayList = new BooleanSetting("ArrayList", true);
    public final BooleanSetting coords = new BooleanSetting("Coords", true);
    public final BooleanSetting fps = new BooleanSetting("FPS", true);

    public Hud() {
        super("HUD", Category.MISC);
        addSettings(watermark, arrayList, coords, fps);
        setEnabled(true);
    }

    public void render(DrawContext ctx) {
        if (!isEnabled() || mc.player == null || mc.options.hudHidden) return;
        TextRenderer tr = mc.textRenderer;

        if (watermark.value) ctx.drawTextWithShadow(tr, "Nova", 4, 4, Theme.A1);

        if (arrayList.value) {
            List<Module> visible = new ArrayList<>();
            for (Module m : Nova.modules.modules) {
                if (m == this) continue;
                m.hudAnim.update(m.isEnabled() ? 1 : 0, 12);
                if (m.hudAnim.value > 0.01) visible.add(m);
            }
            visible.sort(Comparator.comparingInt((Module m) -> tr.getWidth(m.name)).reversed());

            int w = ctx.getScaledWindowWidth();
            double y = 4;
            int idx = 0;
            for (Module m : visible) {
                double an = m.hudAnim.value;
                double e = Anim.ease(an);
                int tw = tr.getWidth(m.name);
                int a = (int) (255 * an);
                int tx = w - tw - 6 + (int) ((1 - e) * (tw + 10)); // slides in from the right
                int iy = (int) y;
                int color = Theme.lerp(Theme.A1, Theme.A2, visible.size() <= 1 ? 0 : idx / (double) (visible.size() - 1));
                if (a >= 8) {
                    ctx.fill(w - 2, iy, w, iy + 10, Theme.argb(color, a));
                    ctx.drawTextWithShadow(tr, m.name, tx, iy + 1, Theme.argb(color, a));
                }
                y += 10 * an; // list closes the gap smoothly
                idx++;
            }
        }

        int by = ctx.getScaledWindowHeight() - 12;
        if (coords.value) {
            String s = String.format("XYZ: %.1f %.1f %.1f", mc.player.getX(), mc.player.getY(), mc.player.getZ());
            ctx.drawTextWithShadow(tr, s, 4, by, 0xFFFFFFFF);
            by -= 10;
        }
        if (fps.value) ctx.drawTextWithShadow(tr, mc.getCurrentFps() + " FPS", 4, by, 0xFFFFFFFF);
    }
}
