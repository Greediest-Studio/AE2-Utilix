package com.ae2utilix.mixin;

import net.minecraft.tileentity.TileEntity;
import appeng.api.storage.IMEInventory;
import appeng.api.storage.data.IAEItemStack;
import appeng.parts.misc.PartStorageBus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(PartStorageBus.class)
public interface MixinPartStorageBusAccessor {

    @Invoker("getInventoryWrapper")
    IMEInventory<IAEItemStack> ae2utilix$invokeGetInventoryWrapper(TileEntity target);

    @Invoker("createHandlerHash")
    int ae2utilix$invokeCreateHandlerHash(TileEntity target);

    @Accessor("resetCacheLogic")
    byte ae2utilix$getResetCacheLogic();

    @Accessor("resetCacheLogic")
    void ae2utilix$setResetCacheLogic(byte value);

    @Accessor("accessChanged")
    boolean ae2utilix$isAccessChanged();

    @Accessor("accessChanged")
    void ae2utilix$setAccessChanged(boolean value);

    @Accessor("readOncePass")
    boolean ae2utilix$isReadOncePass();

    @Accessor("readOncePass")
    void ae2utilix$setReadOncePass(boolean value);
}
