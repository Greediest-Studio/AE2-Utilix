package com.ae2utilix.mixin;

import com.ae2utilix.block.terminal.TilePatternTerminal;
import com.circulation.random_complement.common.interfaces.RCIConfigManager;
import com.circulation.random_complement.common.interfaces.RCIConfigManagerHost;
import com.circulation.random_complement.common.interfaces.RCIConfigurableObject;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = TilePatternTerminal.class, remap = false)
public abstract class MixinTilePatternTerminalRandomComplement implements RCIConfigurableObject, RCIConfigManagerHost {

    @Override
    public RCIConfigManager r$getConfigManager() {
        return (RCIConfigManager) ((TilePatternTerminal) (Object) this).getRandomComplementConfigManager();
    }

    @Override
    public void r$updateSetting(RCIConfigManager manager, Enum<?> setting, Enum<?> value) {
        ((TilePatternTerminal) (Object) this).saveChanges();
    }
}
