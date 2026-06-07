package com.ae2utilix.mixin;

import appeng.api.networking.crafting.ICraftingCPU;
import appeng.container.implementations.CraftingCPUStatus;
import com.ae2utilix.CpuAccessMode;
import com.ae2utilix.ICpuAccessModeHolder;
import com.ae2utilix.ICpuStatusAccessMode;
import net.minecraft.nbt.NBTTagCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CraftingCPUStatus.class, remap = false)
public class MixinCraftingCPUStatus implements ICpuStatusAccessMode {

    @Unique
    private CpuAccessMode ae2utilix$accessMode = CpuAccessMode.ALL;

    @Inject(method = "<init>(Lappeng/api/networking/crafting/ICraftingCPU;I)V", at = @At("RETURN"))
    private void ae2utilix$readFromCluster(ICraftingCPU cluster, int serial, CallbackInfo ci) {
        if (cluster instanceof ICpuAccessModeHolder) {
            this.ae2utilix$accessMode = ((ICpuAccessModeHolder) cluster).ae2utilix$getAccessMode();
        }
    }

    @Inject(method = "writeToNBT", at = @At("RETURN"))
    private void ae2utilix$writeAccessMode(NBTTagCompound i, CallbackInfo ci) {
        i.setInteger("ae2utilix:accessMode", ae2utilix$accessMode.id);
    }

    @Inject(method = "<init>(Lnet/minecraft/nbt/NBTTagCompound;)V", at = @At("RETURN"))
    private void ae2utilix$readFromNBT(NBTTagCompound i, CallbackInfo ci) {
        if (i.hasKey("ae2utilix:accessMode")) {
            this.ae2utilix$accessMode = CpuAccessMode.fromId(i.getInteger("ae2utilix:accessMode"));
        }
    }

    @Override
    public CpuAccessMode ae2utilix$getAccessMode() {
        return ae2utilix$accessMode;
    }
}
