package com.ae2utilix.block;

import appeng.block.misc.BlockInterface;
import appeng.util.Platform;
import com.ae2utilix.AE2Utilix;
import com.ae2utilix.gui.FullTerminalGuiHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public class BlockCommonInterfaceAlternate extends BlockInterface {

    public BlockCommonInterfaceAlternate() {
        super();
        this.setTileEntity(TileCommonInterfaceAlternate.class);
        this.setCreativeTab(com.ae2utilix.AE2Utilix.AE2_UTILIX_TAB);
    }

    @Override
    public boolean onActivated(World world, BlockPos pos, EntityPlayer player, EnumHand hand,
                               @Nullable ItemStack heldItem, EnumFacing side, float hitX, float hitY, float hitZ) {
        if (player.isSneaking()) return false;
        TileCommonInterfaceAlternate tile = (TileCommonInterfaceAlternate) this.getTileEntity(world, pos);
        if (tile != null && Platform.isServer()) {
            player.openGui(AE2Utilix.INSTANCE, FullTerminalGuiHandler.GUI_COMMON_INTERFACE,
                    world, pos.getX(), pos.getY(), pos.getZ());
        }
        return tile != null;
    }
}
