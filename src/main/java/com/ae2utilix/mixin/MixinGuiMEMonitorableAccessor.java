package com.ae2utilix.mixin;

import appeng.client.gui.implementations.GuiMEMonitorable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(GuiMEMonitorable.class)
public interface MixinGuiMEMonitorableAccessor {

    @Invoker("setReservedSpace")
    void ae2utilix$setReservedSpace(int reservedSpace);
}
