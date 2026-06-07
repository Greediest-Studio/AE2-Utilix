package com.ae2utilix.mixin;

import appeng.me.cluster.implementations.CraftingCPUCluster;
import com.ae2utilix.CpuAccessMode;
import com.ae2utilix.ICpuAccessModeHolder;
import net.minecraft.nbt.NBTTagCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CraftingCPUCluster.class, remap = false)
public abstract class MixinCraftingCPUCluster implements ICpuAccessModeHolder {

    @Shadow
    private void markDirty() {}

    @Unique
    private CpuAccessMode ae2utilix$accessMode = CpuAccessMode.ALL;

    @Override
    public CpuAccessMode ae2utilix$getAccessMode() {
        return ae2utilix$accessMode;
    }

    @Override
    public void ae2utilix$setAccessMode(CpuAccessMode mode) {
        this.ae2utilix$accessMode = mode != null ? mode : CpuAccessMode.ALL;
        this.markDirty();
    }

    @Inject(method = "writeToNBT", at = @At("RETURN"))
    private void ae2utilix$writeAccessMode(NBTTagCompound data, CallbackInfo ci) {
        data.setInteger("ae2utilix:accessMode", ae2utilix$accessMode.id);
    }

    @Inject(method = "readFromNBT", at = @At("RETURN"))
    private void ae2utilix$readAccessMode(NBTTagCompound data, CallbackInfo ci) {
        if (data.hasKey("ae2utilix:accessMode")) {
            ae2utilix$accessMode = CpuAccessMode.fromId(data.getInteger("ae2utilix:accessMode"));
        }
    }
}
