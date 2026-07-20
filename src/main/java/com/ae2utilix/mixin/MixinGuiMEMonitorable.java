package com.ae2utilix.mixin;

import appeng.api.storage.ITerminalHost;
import appeng.client.gui.implementations.GuiMEMonitorable;
import appeng.container.implementations.ContainerMEMonitorable;
import appeng.core.localization.GuiText;
import net.minecraft.entity.player.InventoryPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GuiMEMonitorable.class, remap = false)
public class MixinGuiMEMonitorable {

    @Shadow
    private GuiText myName;

    @Inject(method = "drawFG", at = @At("HEAD"), remap = false, require = 0)
    private void ae2utilix$fixMyName(int offsetX, int offsetY, int mouseX, int mouseY, CallbackInfo ci) {
        if (this.myName == null) {
            this.myName = GuiText.Terminal;
        }
    }
}
