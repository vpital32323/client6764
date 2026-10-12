package com.nova.module.impl;

import com.nova.module.Category;
import com.nova.module.Module;
import com.nova.setting.NumberSetting;

/** Creative-style flight. Works in singleplayer / on your own server. */
public class Fly extends Module {
    private final NumberSetting speed = new NumberSetting("Speed", 0.05, 0.01, 0.3, 0.01);

    public Fly() {
        super("Fly", Category.MOVEMENT);
        addSettings(speed);
    }

    @Override
    public void onTick() {
        if (mc.player == null) return;
        mc.player.getAbilities().allowFlying = true;
        mc.player.getAbilities().flying = true;
        mc.player.getAbilities().setFlySpeed((float) speed.value);
    }

    @Override
    public void onDisable() {
        if (mc.player == null) return;
        mc.player.getAbilities().setFlySpeed(0.05f);
        if (!mc.player.isCreative() && !mc.player.isSpectator()) {
            mc.player.getAbilities().flying = false;
            mc.player.getAbilities().allowFlying = false;
        }
    }
}
