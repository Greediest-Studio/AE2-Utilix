package com.ae2utilix.item;

import appeng.api.config.Upgrades;
import com.ae2utilix.AE2Utilix;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class ItemOverflowDestructionCard extends Item implements IAE2UtilixUpgradeModule {

    public ItemOverflowDestructionCard() {
        this.setUnlocalizedName(AE2Utilix.MODID + ".overflow_destruction_card");
        this.setRegistryName("overflow_destruction_card");
        this.setMaxStackSize(64);
        this.setCreativeTab(AE2Utilix.AE2_UTILIX_TAB);
    }

    @Override
    public Upgrades getType(ItemStack itemstack) {
        return Upgrades.CAPACITY;
    }

    @Override
    public String getUpgradeTypeId() {
        return AE2Utilix.UPGRADE_OVERFLOW_DESTRUCTION;
    }

    @Override
    public int getMaxInstalled() {
        return 1;
    }

    @Override
    public boolean matchesType(ItemStack stack) {
        return stack.getItem() instanceof ItemOverflowDestructionCard;
    }
}
