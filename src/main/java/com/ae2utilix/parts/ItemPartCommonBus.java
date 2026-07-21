package com.ae2utilix.parts;

import appeng.api.parts.IPart;
import appeng.api.parts.IPartItem;
import com.ae2utilix.AE2Utilix;
import net.minecraft.client.util.ITooltipFlag;
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

public class ItemPartCommonBus extends Item implements IPartItem {
    private final boolean export;

    public ItemPartCommonBus(boolean export) {
        this.export = export;
        String name = export ? "common_export_bus" : "common_import_bus";
        this.setRegistryName(AE2Utilix.MODID, name);
        this.setUnlocalizedName(AE2Utilix.MODID + "." + name);
        this.setCreativeTab(AE2Utilix.AE2_UTILIX_TAB);
    }

    @Nullable
    @Override
    public IPart createPartFromItemStack(ItemStack stack) {
        return this.export ? new PartCommonExportBus(stack) : new PartCommonImportBus(stack);
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand,
                                      EnumFacing facing, float hitX, float hitY, float hitZ) {
        return appeng.api.AEApi.instance().partHelper().placeBus(
                player.getHeldItem(hand), pos, facing, player, hand, world);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flags) {
        tooltip.add(net.minecraft.client.resources.I18n.format(
                "item.ae2_utilix." + (this.export ? "common_export_bus" : "common_import_bus") + ".tooltip"));
    }
}
