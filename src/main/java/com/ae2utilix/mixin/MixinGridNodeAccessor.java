package com.ae2utilix.mixin;

import appeng.me.GridNode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GridNode.class)
public interface MixinGridNodeAccessor {

    @Accessor("compressedData")
    int ae2utilix$getCompressedData();

    @Accessor("compressedData")
    void ae2utilix$setCompressedData(int value);
}
