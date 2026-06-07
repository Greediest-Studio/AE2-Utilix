package com.ae2utilix.item;

import com.ae2utilix.AE2Utilix;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

public class ItemParallelCard extends Item implements IAE2UtilixUpgradeModule {

    public ItemParallelCard() {
        this.setUnlocalizedName(AE2Utilix.MODID + ".parallel_card");
        this.setRegistryName("parallel_card");
        this.setMaxStackSize(64);
        this.setCreativeTab(AE2Utilix.AE2_UTILIX_TAB);
    }

    @Override
    public String getUpgradeTypeId() {
        return AE2Utilix.UPGRADE_PARALLEL_CARD;
    }

    @Override
    public int getMaxInstalled() {
        return 5;
    }

    @Override
    public boolean matchesType(ItemStack stack) {
        return stack.getItem() instanceof ItemParallelCard;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.add(I18n.format("item.ae2_utilix.parallel_card.tooltip.1"));
        ItemProductReturnCard.addSupportedTooltip(getUpgradeTypeId(), tooltip);
    }
}
