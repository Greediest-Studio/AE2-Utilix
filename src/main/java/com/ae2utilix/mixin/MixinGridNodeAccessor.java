package com.ae2utilix.mixin;

import appeng.me.GridNode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = GridNode.class, remap = false)
public interface MixinGridNodeAccessor {

    @Accessor("compressedData")
    int ae2utilix$getCompressedData();

    @Accessor("compressedData")
    void ae2utilix$setCompressedData(int value);
}
