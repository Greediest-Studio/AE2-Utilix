package com.ae2utilix.mixin;

import com.ae2utilix.item.ItemFluidMark;
import com.ae2utilix.integration.BotaniaFluxIntegration;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderItem.class)
public abstract class MixinRenderItemOverlay {

    @Inject(method = "renderItemOverlayIntoGUI", at = @At("HEAD"),
            cancellable = true, remap = false, require = 0)
    private void ae2utilix$hideVirtualMarkerCount(FontRenderer font, ItemStack stack,
                                                   int x, int y, String text, CallbackInfo ci) {
        if (ItemFluidMark.isVirtualMark(stack) || BotaniaFluxIntegration.isVirtualPacket(stack)) {
            ci.cancel();
        }
    }

    @Inject(method = "func_175030_a", at = @At("HEAD"),
            cancellable = true, remap = false, require = 0)
    private void ae2utilix$hideVirtualMarkerCountLegacy(FontRenderer font, ItemStack stack,
                                                         int x, int y, CallbackInfo ci) {
        if (ItemFluidMark.isVirtualMark(stack) || BotaniaFluxIntegration.isVirtualPacket(stack)) {
            ci.cancel();
        }
    }
}
