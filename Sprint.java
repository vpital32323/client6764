package com.nova.module.impl;

import com.nova.module.Category;
import com.nova.module.Module;

public class Sprint extends Module {
    public Sprint() { super("Sprint", Category.MOVEMENT); }

    @Override
    public void onTick() {
        if (mc.player == null) return;
        if (mc.player.forwardSpeed > 0 && !mc.player.horizontalCollision && !mc.player.isSneaking()) {
            mc.player.setSprinting(true);
        }
    }
}
