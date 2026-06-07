package com.ae2utilix.parts;

import appeng.api.parts.IPart;
import appeng.api.parts.IPartItem;
import com.ae2utilix.AE2Utilix;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

public class ItemPartBlockStorageBus extends Item implements IPartItem {

    public ItemPartBlockStorageBus() {
        this.setRegistryName(AE2Utilix.MODID, "block_storage_bus");
        this.setUnlocalizedName(AE2Utilix.MODID + ".block_storage_bus");
        this.setCreativeTab(AE2Utilix.AE2_UTILIX_TAB);
    }

    @Nullable
    @Override
    public IPart createPartFromItemStack(ItemStack is) {
        return new PartBlockStorageBus(is);
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        return appeng.api.AEApi.instance().partHelper().placeBus(
                player.getHeldItem(hand), pos, facing, player, hand, world);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.add(net.minecraft.client.resources.I18n.format("item.ae2_utilix.block_storage_bus.tooltip"));
    }
}
