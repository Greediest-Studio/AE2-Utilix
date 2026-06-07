package com.ae2utilix.mixin;

import appeng.api.networking.IGridHost;
import appeng.api.networking.IMachineSet;
import appeng.me.Grid;
import appeng.tile.misc.TileInterface;
import com.ae2utilix.block.TilePhaseInterface;
import com.ae2utilix.util.UnionMachineSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Grid.class)
public class MixinGrid {

    @Inject(method = "getMachines", at = @At("RETURN"), cancellable = true, remap = false, require = 0)
    private void ae2utilix$includePhaseInterfaces(Class<? extends IGridHost> c, CallbackInfoReturnable<IMachineSet> cir) {
        if (c == TileInterface.class) {
            IMachineSet original = cir.getReturnValue();
            IMachineSet phaseSet = ((Grid) (Object) this).getMachines(TilePhaseInterface.class);
            if (!phaseSet.isEmpty()) {
                cir.setReturnValue(new UnionMachineSet(TileInterface.class, original, phaseSet));
            }
        }
    }
}
