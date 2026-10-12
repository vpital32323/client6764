package com.nova.module;

import com.nova.module.impl.*;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class ModuleManager {
    public final List<Module> modules = new ArrayList<>();
    private final boolean[] keyDown = new boolean[GLFW.GLFW_KEY_LAST + 1];

    public ModuleManager() {
        modules.add(new Sprint());
        modules.add(new Fly());
        modules.add(new Fullbright());
        modules.add(new Esp());
        modules.add(new ChinaHat());
        modules.add(new Hud());
    }

    public List<Module> byCategory(Category c) {
        return modules.stream().filter(m -> m.category == c).toList();
    }

    public Module get(Class<? extends Module> cls) {
        for (Module m : modules) if (cls.isInstance(m)) return m;
        return null;
    }

    public void tick(long window, boolean screenOpen) {
        for (Module m : modules) {
            if (m.key > 0 && !screenOpen) {
                boolean down = GLFW.glfwGetKey(window, m.key) == GLFW.GLFW_PRESS;
                if (down && !keyDown[m.key]) m.toggle();
                keyDown[m.key] = down;
            }
            if (m.isEnabled()) m.onTick();
        }
    }

    public void render3D(WorldRenderContext ctx) {
        for (Module m : modules) if (m.isEnabled()) m.onRender3D(ctx);
    }
}
