package com.ae2utilix.item;

import appeng.api.config.Upgrades;
import appeng.api.implementations.items.IUpgradeModule;
import net.minecraft.item.ItemStack;

public interface IAE2UtilixUpgradeModule extends IUpgradeModule {

    String getUpgradeTypeId();

    int getMaxInstalled();

    boolean matchesType(ItemStack stack);

    @Override
    default Upgrades getType(ItemStack itemstack) {
        return Upgrades.STICKY;
    }
}
