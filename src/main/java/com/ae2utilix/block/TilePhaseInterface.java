package com.ae2utilix.block;

import appeng.helpers.ICustomNameObject;
import appeng.tile.misc.TileInterface;
import appeng.util.InventoryAdaptor;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public class TilePhaseInterface extends TileInterface implements ICustomNameObject {

    private static final String NBT_CUSTOM_NAME = "CustomName";
    private static final String NBT_LINK_DIM = "ae2utilix_link_dim";
    private static final String NBT_LINK_X = "ae2utilix_link_x";
    private static final String NBT_LINK_Y = "ae2utilix_link_y";
    private static final String NBT_LINK_Z = "ae2utilix_link_z";
    private static final String NBT_LINK_FACE = "ae2utilix_link_face";

    private String customName = null;
    private int linkDim = Integer.MIN_VALUE;
    private BlockPos linkPos = null;
    private EnumFacing linkFace = null;

    public TilePhaseInterface() {
        super();
    }

    @Override
    public ItemStack getItemStackRepresentation() {
        return new ItemStack(com.ae2utilix.AE2Utilix.BLOCK_PHASE_INTERFACE);
    }

    @Override
    public appeng.core.sync.GuiBridge getGuiBridge() {
        return appeng.core.sync.GuiBridge.GUI_INTERFACE;
    }

    @Override
    public String getCustomInventoryName() {
        String linkedName = ae2utilix$getLinkedBlockName();
        if (linkedName != null) return linkedName;
        if (this.customName != null && !this.customName.isEmpty()) return this.customName;
        return new ItemStack(com.ae2utilix.AE2Utilix.BLOCK_PHASE_INTERFACE).getDisplayName();
    }

    @Override
    public boolean hasCustomInventoryName() {
        return true;
    }

    @Override
    public void setCustomName(String name) {
        this.customName = name;
    }

    @Nullable
    private String ae2utilix$getLinkedBlockName() {
        if (!hasLinkData() || !isLinkValid()) return null;
        World world = getWorld();
        if (world == null) return null;

        TileEntity te = world.getTileEntity(linkPos);
        if (te == null) return null;

        EnumFacing face = linkFace;
        InventoryAdaptor adaptor = InventoryAdaptor.getAdaptor(te, face);
        if (adaptor == null && !(te instanceof appeng.api.implementations.tiles.ICraftingMachine)) return null;
        if (adaptor != null && !adaptor.hasSlots()) return null;

        IBlockState state = world.getBlockState(linkPos);
        Block block = state.getBlock();
        ItemStack what = ae2utilix$pickBlock(state, block, world);

        if (!what.isEmpty() && what.getItem() != net.minecraft.init.Items.AIR) {
            return what.getDisplayName();
        }

        Item item = Item.getItemFromBlock(block);
        if (item == net.minecraft.init.Items.AIR) {
            String key = block.getUnlocalizedName() + ".name";
            String translated = I18n.translateToLocal(key);
            if (!translated.equals(key)) return translated;
            return I18n.translateToLocal(block.getUnlocalizedName());
        }

        return null;
    }

    @Nullable
    public String ae2utilix$getTermNameKey() {
        if (!hasLinkData() || !isLinkValid()) {
            ItemStack stack = new ItemStack(com.ae2utilix.AE2Utilix.BLOCK_PHASE_INTERFACE);
            return stack.getUnlocalizedName() + ".name";
        }
        World world = getWorld();
        if (world == null) return null;

        TileEntity te = world.getTileEntity(linkPos);
        if (te == null) return null;

        EnumFacing face = linkFace;
        InventoryAdaptor adaptor = InventoryAdaptor.getAdaptor(te, face);
        if (adaptor == null && !(te instanceof appeng.api.implementations.tiles.ICraftingMachine)) return null;
        if (adaptor != null && !adaptor.hasSlots()) return null;

        IBlockState state = world.getBlockState(linkPos);
        Block block = state.getBlock();
        ItemStack what = ae2utilix$pickBlock(state, block, world);

        if (!what.isEmpty() && what.getItem() != net.minecraft.init.Items.AIR) {
            return what.getUnlocalizedName() + ".name";
        }

        Item item = Item.getItemFromBlock(block);
        if (item == net.minecraft.init.Items.AIR) {
            return block.getUnlocalizedName() + ".name";
        }

        ItemStack stack = new ItemStack(com.ae2utilix.AE2Utilix.BLOCK_PHASE_INTERFACE);
        return stack.getUnlocalizedName() + ".name";
    }

    private ItemStack ae2utilix$pickBlock(IBlockState state, Block block, World world) {
        ItemStack what = new ItemStack(block, 1, block.getMetaFromState(state));
        try {
            Vec3d from = new Vec3d(linkPos.getX() + 0.5, linkPos.getY() + 0.5, linkPos.getZ() + 0.5);
            from = from.addVector(linkFace.getFrontOffsetX() * 0.501, linkFace.getFrontOffsetY() * 0.501, linkFace.getFrontOffsetZ() * 0.501);
            Vec3d to = from.addVector(linkFace.getFrontOffsetX(), linkFace.getFrontOffsetY(), linkFace.getFrontOffsetZ());
            RayTraceResult mop = world.rayTraceBlocks(from, to, true);
            if (mop != null && mop.getBlockPos().equals(linkPos)) {
                ItemStack g = block.getPickBlock(state, mop, world, linkPos, null);
                if (!g.isEmpty()) {
                    what = g;
                }
            }
        } catch (Throwable ignored) {
        }
        return what;
    }

    public void setLinkData(int dim, BlockPos pos, EnumFacing face) {
        this.linkDim = dim;
        this.linkPos = pos;
        this.linkFace = face;
        this.saveChanges();
    }

    public void clearLinkData() {
        this.linkDim = Integer.MIN_VALUE;
        this.linkPos = null;
        this.linkFace = null;
        this.saveChanges();
    }

    public boolean hasLinkData() {
        return this.linkPos != null && this.linkFace != null;
    }

    @Nullable
    public Integer getLinkDimension() {
        return hasLinkData() ? this.linkDim : null;
    }

    @Nullable
    public BlockPos getLinkPos() {
        return this.linkPos;
    }

    @Nullable
    public EnumFacing getLinkFace() {
        return this.linkFace;
    }

    public BlockPos getEffectiveTargetPos() {
        if (!hasLinkData()) return null;
        return this.linkPos.offset(this.linkFace);
    }

    public boolean isLinkValid() {
        if (!hasLinkData()) return false;
        if (this.getWorld() == null) return false;
        if (this.linkDim != this.getWorld().provider.getDimension()) return false;
        BlockPos selfPos = this.getPos();
        int dx = Math.abs(this.linkPos.getX() - selfPos.getX());
        int dy = Math.abs(this.linkPos.getY() - selfPos.getY());
        int dz = Math.abs(this.linkPos.getZ() - selfPos.getZ());
        return dx <= 16 && dy <= 16 && dz <= 16;
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);
        if (data.hasKey(NBT_CUSTOM_NAME)) {
            this.customName = data.getString(NBT_CUSTOM_NAME);
        }
        if (data.hasKey(NBT_LINK_DIM)) {
            this.linkDim = data.getInteger(NBT_LINK_DIM);
            this.linkPos = new BlockPos(
                    data.getInteger(NBT_LINK_X),
                    data.getInteger(NBT_LINK_Y),
                    data.getInteger(NBT_LINK_Z));
            int ordinal = data.getInteger(NBT_LINK_FACE);
            if (ordinal >= 0 && ordinal < EnumFacing.values().length) {
                this.linkFace = EnumFacing.values()[ordinal];
            }
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound data) {
        super.writeToNBT(data);
        if (this.customName != null) {
            data.setString(NBT_CUSTOM_NAME, this.customName);
        }
        if (hasLinkData()) {
            data.setInteger(NBT_LINK_DIM, this.linkDim);
            data.setInteger(NBT_LINK_X, this.linkPos.getX());
            data.setInteger(NBT_LINK_Y, this.linkPos.getY());
            data.setInteger(NBT_LINK_Z, this.linkPos.getZ());
            data.setInteger(NBT_LINK_FACE, this.linkFace.ordinal());
        }
        return data;
    }
}
