package com.ae2utilix.mixin;

import com.ae2utilix.gui.ContainerFullPattern;
import com.circulation.random_complement.client.buttonsetting.PatternTermAutoFillPattern;
import com.circulation.random_complement.common.interfaces.PatternTermConfigs;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = ContainerFullPattern.class, remap = false)
public abstract class MixinContainerFullPatternRandomComplement implements PatternTermConfigs {

    @Override
    public PatternTermAutoFillPattern r$getAutoFillPattern() {
        return "OPEN".equals(((ContainerFullPattern) (Object) this).r$getAutoFillPatternName())
                ? PatternTermAutoFillPattern.OPEN : PatternTermAutoFillPattern.CLOSE;
    }
}
