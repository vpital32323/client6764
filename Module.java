package com.nova.module;

import com.nova.setting.Setting;
import com.nova.util.Anim;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.List;

public abstract class Module {
    protected static final MinecraftClient mc = MinecraftClient.getInstance();

    public final String name;
    public final Category category;
    public int key = 0; // GLFW key code, 0 = none
    public final List<Setting> settings = new ArrayList<>();
    public final Anim hudAnim = new Anim(0); // slide animation in the HUD list
    private boolean enabled;

    protected Module(String name, Category category) {
        this.name = name;
        this.category = category;
    }

    public boolean isEnabled() { return enabled; }

    public void toggle() { setEnabled(!enabled); }

    public void setEnabled(boolean value) {
        if (enabled == value) return;
        enabled = value;
        if (enabled) onEnable(); else onDisable();
    }

    protected void addSettings(Setting... s) { settings.addAll(List.of(s)); }

    public void onEnable() {}
    public void onDisable() {}
    public void onTick() {}
    public void onRender3D(WorldRenderContext ctx) {}
}
