package com.ae2utilix.integration.jei;

import com.ae2utilix.gui.GuiCrystalGrowthChamber;
import mezz.jei.api.gui.IAdvancedGuiHandler;

public class CrystalGrowthChamberGuiHandler implements IAdvancedGuiHandler<GuiCrystalGrowthChamber> {

    @Override
    public Class<GuiCrystalGrowthChamber> getGuiContainerClass() {
        return GuiCrystalGrowthChamber.class;
    }
}
