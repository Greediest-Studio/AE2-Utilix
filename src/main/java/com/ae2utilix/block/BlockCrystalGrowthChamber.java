package com.ae2utilix.block;

import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

public class BlockCrystalGrowthChamber extends Block implements ITileEntityProvider {

    public static final PropertyDirection FACING = PropertyDirection.create("facing", EnumFacing.Plane.HORIZONTAL);

    private static final AxisAlignedBB AABB_NORTH = new AxisAlignedBB(0.125, 0.0, 0.0, 0.875, 0.875, 1.0);
    private static final AxisAlignedBB AABB_SOUTH = new AxisAlignedBB(0.125, 0.0, 0.0, 0.875, 0.875, 1.0);
    private static final AxisAlignedBB AABB_EAST  = new AxisAlignedBB(0.0, 0.0, 0.125, 1.0, 0.875, 0.875);
    private static final AxisAlignedBB AABB_WEST  = new AxisAlignedBB(0.0, 0.0, 0.125, 1.0, 0.875, 0.875);

    public BlockCrystalGrowthChamber() {
        super(Material.IRON);
        this.setCreativeTab(com.ae2utilix.AE2Utilix.AE2_UTILIX_TAB);
        this.setDefaultState(this.blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH));
        this.setHardness(3.0f);
        this.setResistance(8.0f);
        this.setLightOpacity(2);
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        switch (state.getValue(FACING)) {
            case NORTH: return AABB_NORTH;
            case SOUTH: return AABB_SOUTH;
            case EAST:  return AABB_EAST;
            case WEST:  return AABB_WEST;
            default:    return AABB_NORTH;
        }
    }

    @Override
    public void addCollisionBoxToList(IBlockState state, World world, BlockPos pos, AxisAlignedBB entityBox, List<AxisAlignedBB> collidingBoxes, @Nullable Entity entity, boolean isActualState) {
        addCollisionBoxToList(pos, entityBox, collidingBoxes, getBoundingBox(state, world, pos));
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer, EnumHand hand) {
        return this.getDefaultState().withProperty(FACING, placer.getHorizontalFacing().getOpposite());
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof TileCrystalGrowthChamber) {
            ((TileCrystalGrowthChamber) te).setOrientation(state.getValue(FACING), EnumFacing.UP);
        }
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        EnumFacing facing = EnumFacing.getHorizontal(meta & 3);
        return this.getDefaultState().withProperty(FACING, facing);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(FACING).getHorizontalIndex();
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING);
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
        if (!world.isRemote) {
            ItemStack heldItem = player.getHeldItem(hand);
            if (player.isSneaking() && !heldItem.isEmpty()) {
                boolean isSpeedCard = appeng.api.AEApi.instance().definitions().materials().cardSpeed().isSameAs(heldItem);
                boolean isParallelCard = heldItem.getItem() instanceof com.ae2utilix.item.ItemParallelCard;
                boolean isMemoryCard = heldItem.getItem() instanceof appeng.api.implementations.items.IMemoryCard;
                if (isSpeedCard || isParallelCard || isMemoryCard) {
                    return true;
                }
            }
            if (!heldItem.isEmpty() && heldItem.getItem() instanceof appeng.api.implementations.items.IMemoryCard) {
                return true;
            }
            TileEntity te = world.getTileEntity(pos);
            if (te instanceof TileCrystalGrowthChamber) {
                ((TileCrystalGrowthChamber) te).activate(player);
            }
        }
        return true;
    }

    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileCrystalGrowthChamber();
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof TileCrystalGrowthChamber) {
            ((TileCrystalGrowthChamber) te).dropItems();
        }
        super.breakBlock(world, pos, state);
    }
}
