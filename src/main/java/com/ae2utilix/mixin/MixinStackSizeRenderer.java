package com.ae2utilix.mixin;

import appeng.api.storage.data.IAEItemStack;
import appeng.client.render.StackSizeRenderer;
import com.ae2utilix.AE2Utilix;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = StackSizeRenderer.class, remap = false)
public abstract class MixinStackSizeRenderer {

    @Inject(method = "renderStackSize", at = @At("HEAD"), cancellable = true, remap = false)
    private void ae2utilix$hideFluidMarkerCount( net.minecraft.client.gui.FontRenderer font,
                                                   IAEItemStack stack, int x, int y, CallbackInfo ci) {
        if (stack == null) return;
        ItemStack definition = stack.getDefinition();
        if (definition != null && !definition.isEmpty()
                && com.ae2utilix.item.ItemFluidMark.isVirtualMark(definition)) {
            ci.cancel();
        }
    }
}
