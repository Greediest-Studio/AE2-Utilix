package com.ae2utilix.block;

import appeng.block.misc.BlockInterface;
import appeng.core.sync.GuiBridge;
import appeng.util.Platform;
import appeng.api.util.AEPartLocation;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public class BlockPhaseInterface extends BlockInterface {

    public BlockPhaseInterface() {
        super();
        this.setTileEntity(TilePhaseInterface.class);
        this.setCreativeTab(com.ae2utilix.AE2Utilix.AE2_UTILIX_TAB);
    }

    @Override
    public boolean onActivated(World w, BlockPos pos, EntityPlayer p, EnumHand hand,
                               @Nullable ItemStack heldItem, EnumFacing side, float hitX, float hitY, float hitZ) {
        if (p.isSneaking()) return false;

        TilePhaseInterface te = (TilePhaseInterface) this.getTileEntity(w, pos);
        if (te != null) {
            if (Platform.isServer()) {
                Platform.openGUI(p, te, AEPartLocation.fromFacing(side), GuiBridge.GUI_INTERFACE);
            }
            return true;
        }
        return false;
    }
}
