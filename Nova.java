package com.nova;

import com.nova.gui.ClickGui;
import com.nova.module.ModuleManager;
import com.nova.module.impl.Hud;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class Nova implements ClientModInitializer {
    public static ModuleManager modules;
    private static KeyBinding openGui;

    @Override
    public void onInitializeClient() {
        modules = new ModuleManager();

        openGui = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.nova.clickgui", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, "category.nova"));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (openGui.wasPressed()) mc.setScreen(new ClickGui());
            if (mc.player != null) modules.tick(mc.getWindow().getHandle(), mc.currentScreen != null);
        });

        HudRenderCallback.EVENT.register((ctx, tickCounter) ->
                ((Hud) modules.get(Hud.class)).render(ctx));

        WorldRenderEvents.LAST.register(modules::render3D);
    }
}
