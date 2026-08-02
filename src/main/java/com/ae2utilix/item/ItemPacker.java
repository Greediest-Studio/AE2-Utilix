package com.ae2utilix.item;

import appeng.api.config.Actionable;
import appeng.api.implementations.parts.IPartCable;
import appeng.api.networking.IGridHost;
import appeng.api.parts.IPart;
import appeng.api.parts.IPartHost;
import appeng.api.parts.PartItemStack;
import appeng.api.parts.SelectedPart;
import appeng.api.util.AEPartLocation;
import appeng.api.util.DimensionalCoord;
import appeng.items.tools.powered.powersink.AEBasePoweredItem;
import appeng.util.Platform;
import com.ae2utilix.AE2Utilix;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/** Powered tool used to move an AE block or panel without losing its state. */
public class ItemPacker extends AEBasePoweredItem {

    public static final double MAX_POWER = 1_000_000.0;
    public static final double POWER_PER_PACK = 20_000.0;

    public ItemPacker() {
        super(MAX_POWER);
        this.setUnlocalizedName(AE2Utilix.MODID + ".packer");
        this.setRegistryName(AE2Utilix.MODID, "packer");
        this.setCreativeTab(AE2Utilix.AE2_UTILIX_TAB);
    }

    @Override
    public EnumActionResult onItemUseFirst(EntityPlayer player, World world, BlockPos pos, EnumFacing side,
            float hitX, float hitY, float hitZ, EnumHand hand) {
        if (!player.isSneaking()) {
            return EnumActionResult.PASS;
        }

        ItemStack held = player.getHeldItem(hand);
        if (held.isEmpty() || held.getItem() != this) {
            return EnumActionResult.PASS;
        }

        TileEntity tile = world.getTileEntity(pos);
        SelectedPart selected = null;
        if (tile instanceof IPartHost) {
            selected = ((IPartHost) tile).selectPart(new Vec3d(hitX, hitY, hitZ));
            if (selected == null || selected.part == null || selected.part instanceof IPartCable) {
                return EnumActionResult.PASS;
            }
        } else if (!(tile instanceof IGridHost)) {
            return EnumActionResult.PASS;
        }

        if (world.isRemote) {
            return EnumActionResult.SUCCESS;
        }
        if (!Platform.hasPermissions(new DimensionalCoord(world, pos), player)
                || !player.canPlayerEdit(pos, side, held)) {
            return EnumActionResult.FAIL;
        }
        if (extractAEPower(held, POWER_PER_PACK, Actionable.SIMULATE) < POWER_PER_PACK) {
            return EnumActionResult.FAIL;
        }

        if (selected != null) {
            return packPart(player, world, pos, selected, held, hand);
        }
        return packBlock(player, world, pos, side, tile, held, hand);
    }

    private EnumActionResult packPart(EntityPlayer player, World world, BlockPos pos, SelectedPart selected,
            ItemStack packer, EnumHand hand) {
        IPartHost host = (IPartHost) world.getTileEntity(pos);
        IPart part = selected.part;
        ItemStack partStack = part.getItemStack(PartItemStack.WORLD);
        if (partStack == null || partStack.isEmpty() || selected.side == AEPartLocation.INTERNAL) {
            return EnumActionResult.FAIL;
        }

        NBTTagCompound partData = new NBTTagCompound();
        part.writeToNBT(partData);
        ItemStack packageStack = ItemDevicePackage.createPartPackage(partStack, partData, selected.side.ordinal());

        host.removePart(selected.side, false);
        if (host.getPart(selected.side) == part) {
            return EnumActionResult.FAIL;
        }
        host.markForSave();
        host.markForUpdate();
        if (host.isEmpty()) {
            host.cleanup();
        }

        extractAEPower(packer, POWER_PER_PACK, Actionable.MODULATE);
        givePackage(player, packageStack);
        return EnumActionResult.SUCCESS;
    }

    private EnumActionResult packBlock(EntityPlayer player, World world, BlockPos pos, EnumFacing side,
            TileEntity tile, ItemStack packer, EnumHand hand) {
        if (!(tile instanceof IGridHost)) {
            return EnumActionResult.FAIL;
        }

        net.minecraft.block.state.IBlockState state = world.getBlockState(pos);
        net.minecraft.block.Block block = state.getBlock();
        net.minecraft.item.Item item = net.minecraft.item.Item.getItemFromBlock(block);
        if (item == null || item == net.minecraft.item.Item.getItemFromBlock(net.minecraft.init.Blocks.AIR)) {
            return EnumActionResult.FAIL;
        }

        ItemStack blockStack = new ItemStack(item, 1, block.getMetaFromState(state));
        NBTTagCompound tileData = new NBTTagCompound();
        tile.writeToNBT(tileData);
        ItemStack packageStack = ItemDevicePackage.createBlockPackage(blockStack, tileData);

        // Remove the TileEntity first so AEBaseTileBlock.breakBlock cannot emit
        // a second ordinary drop when the block is replaced with air.
        world.removeTileEntity(pos);
        if (!world.setBlockToAir(pos)) {
            return EnumActionResult.FAIL;
        }

        extractAEPower(packer, POWER_PER_PACK, Actionable.MODULATE);
        givePackage(player, packageStack);
        return EnumActionResult.SUCCESS;
    }

    private static void givePackage(EntityPlayer player, ItemStack packageStack) {
        if (!player.inventory.addItemStackToInventory(packageStack)) {
            EntityItem entity = new EntityItem(player.world, player.posX, player.posY, player.posZ, packageStack);
            entity.setPickupDelay(10);
            player.world.spawnEntity(entity);
        }
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void addCheckedInformation(ItemStack stack, @Nullable World world, List<String> tooltip,
            ITooltipFlag flag) {
        super.addCheckedInformation(stack, world, tooltip, flag);
        tooltip.add(I18n.format("item.ae2_utilix.packer.tooltip"));
    }
}
