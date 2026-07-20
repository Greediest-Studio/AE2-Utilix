package com.ae2utilix.block;

import appeng.block.AEBaseTileBlock;
import appeng.util.Platform;
import com.ae2utilix.AE2Utilix;
import com.ae2utilix.gui.FullTerminalGuiHandler;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public class BlockCommonInterfaceAlternate extends AEBaseTileBlock {

    private static final PropertyBool OMNIDIRECTIONAL = PropertyBool.create("omnidirectional");

    public BlockCommonInterfaceAlternate() {
        super(Material.IRON);
        this.setTileEntity(TileCommonInterfaceAlternate.class);
        this.setCreativeTab(com.ae2utilix.AE2Utilix.AE2_UTILIX_TAB);
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileCommonInterfaceAlternate();
    }

    @Override
    protected IProperty[] getAEStates() {
        return new IProperty[]{OMNIDIRECTIONAL};
    }

    @Override
    public IBlockState getActualState(IBlockState state, IBlockAccess world, BlockPos pos) {
        return state.withProperty(OMNIDIRECTIONAL, true);
    }

    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos,
        net.minecraft.block.Block blockIn, BlockPos fromPos) {
        TileCommonInterfaceAlternate tileEntity = this.getTileEntity(world, pos);
        if (tileEntity != null) {
            tileEntity.updateRedstoneState();
        }
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
