package com.ae2utilix.mixin;

import com.ae2utilix.gui.ContainerFullPattern;
import com.glodblock.github.interfaces.FCFluidPatternContainer;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = ContainerFullPattern.class, remap = false)
public abstract class MixinContainerFullPatternAE2FC implements FCFluidPatternContainer {
}
