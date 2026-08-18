package com.ae2utilix.block.terminal;

import appeng.api.implementations.tiles.IColorableTile;
import appeng.api.util.AEColor;
import appeng.items.tools.powered.ToolColorApplicator;
import appeng.api.util.IOrientable;
import appeng.api.util.IOrientableBlock;
import appeng.block.AEBaseBlock;
import com.ae2utilix.AE2Utilix;
import com.ae2utilix.client.TerminalStateProperty;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.property.ExtendedBlockState;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.common.property.IUnlistedProperty;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public abstract class BlockFullTerminal extends AEBaseBlock implements IOrientableBlock {

    public static final PropertyDirection FACING = PropertyDirection.create("facing");
    private final Class<? extends TileFullTerminal> tileClass;

    public BlockFullTerminal(String registryName, Class<? extends TileFullTerminal> tileClass) {
        super(Material.IRON);
        this.tileClass = tileClass;
        this.setRegistryName(AE2Utilix.MODID, registryName);
        this.setUnlocalizedName(AE2Utilix.MODID + "." + registryName);
        this.setCreativeTab(AE2Utilix.AE2_UTILIX_TAB);
        this.setSoundType(SoundType.METAL);
        this.setHardness(2.2f);
        this.setResistance(11.0f);
        this.setDefaultState(this.blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH));
        this.setOpaque(false);
        this.setFullSize(false);
        this.fullBlock = false;
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        try {
            return tileClass.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create TileEntity for " + tileClass, e);
        }
    }

    private EnumFacing getUpForFacing(EnumFacing facing) {
        if (facing == EnumFacing.UP) {
            return EnumFacing.SOUTH;
        } else if (facing == EnumFacing.DOWN) {
            return EnumFacing.NORTH;
        }
        return EnumFacing.UP;
    }

    @SideOnly(Side.CLIENT)
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public boolean canRenderInLayer(IBlockState state, BlockRenderLayer layer) {
        return layer == BlockRenderLayer.CUTOUT;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    protected abstract int getGuiId();

    @Override
    public boolean onActivated(World world, BlockPos pos, EntityPlayer player,
                                EnumHand hand, ItemStack heldItem, EnumFacing side, float hitX, float hitY, float hitZ) {
        // If holding a color applicator, don't open GUI - let the item handle coloring
        if (!heldItem.isEmpty() && heldItem.getItem() instanceof ToolColorApplicator) {
            return false;
        }
        if (!world.isRemote) {
            TileEntity te = world.getTileEntity(pos);
            if (te instanceof TileFullTerminal) {
                player.openGui(AE2Utilix.INSTANCE, getGuiId(), world, pos.getX(), pos.getY(), pos.getZ());
            }
        }
        return true;
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player,
                                    EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
        return this.onActivated(world, pos, player, hand, player.getHeldItem(hand), side, hitX, hitY, hitZ);
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof TileFullTerminal) {
            ((TileFullTerminal) te).getProxy().invalidate();
        }
        super.breakBlock(world, pos, state);
    }

    @Override
    public boolean recolorBlock(World world, BlockPos pos, EnumFacing side, net.minecraft.item.EnumDyeColor color) {
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof IColorableTile) {
            // EnumDyeColor metadata (0-15) directly maps to AEColor ordinals (0-15), TRANSPARENT is 16
            int colorIndex = color.getMetadata();
            if (colorIndex >= 0 && colorIndex < AEColor.values().length) {
                AEColor newColor = AEColor.values()[colorIndex];
                return ((IColorableTile) te).recolourBlock(side, newColor, null);
            }
        }
        return false;
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof TileFullTerminal) {
            EnumFacing facing = state.getValue(FACING);
            te.setWorld(world);
            te.setPos(pos);
            ((TileFullTerminal) te).setOrientation(facing, getUpForFacing(facing));
        }
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY,
                                            float hitZ, int meta, EntityLivingBase placer, EnumHand hand) {
        float pitch = placer.rotationPitch;
        if (pitch > 60) {
            return this.getDefaultState().withProperty(FACING, EnumFacing.UP);
        } else if (pitch < -60) {
            return this.getDefaultState().withProperty(FACING, EnumFacing.DOWN);
        } else {
            return this.getDefaultState().withProperty(FACING, placer.getHorizontalFacing().getOpposite());
        }
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new ExtendedBlockState(this,
                new PropertyDirection[]{FACING},
                new IUnlistedProperty[]{
                        TerminalStateProperty.TILE_PROPERTY,
                        appeng.block.AEBaseTileBlock.FORWARD,
                        appeng.block.AEBaseTileBlock.UP
                });
    }

    @Override
    public IBlockState getExtendedState(IBlockState state, IBlockAccess world, BlockPos pos) {
        if (state instanceof IExtendedBlockState) {
            TileEntity te = world.getTileEntity(pos);
            if (te instanceof TileFullTerminal) {
                TileFullTerminal tile = (TileFullTerminal) te;
                IExtendedBlockState extState = (IExtendedBlockState) state;
                extState = extState.withProperty(TerminalStateProperty.TILE_PROPERTY, tile);
                extState = extState.withProperty(appeng.block.AEBaseTileBlock.FORWARD, tile.getForward());
                extState = extState.withProperty(appeng.block.AEBaseTileBlock.UP, tile.getUp());
                return extState;
            }
        }
        return state;
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState().withProperty(FACING, EnumFacing.getFront(meta));
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(FACING).getIndex();
    }

    @Override
    public boolean usesMetadata() {
        return true;
    }

    @Override
    public IOrientable getOrientable(IBlockAccess world, BlockPos pos) {
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof TileFullTerminal) {
            return (TileFullTerminal) te;
        }
        return null;
    }
}
