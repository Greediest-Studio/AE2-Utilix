package com.ae2utilix.item;

import appeng.api.parts.IPartHost;
import appeng.api.parts.SelectedPart;
import com.ae2utilix.AE2Utilix;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nullable;
import java.util.List;

public class ItemPhaseCard extends Item implements IAE2UtilixUpgradeModule {

    private static final String NBT_TAG = "ae2utilix_phase_face";

    public ItemPhaseCard() {
        this.setUnlocalizedName(AE2Utilix.MODID + ".phase_card");
        this.setRegistryName("phase_card");
        this.setMaxStackSize(64);
        this.setCreativeTab(AE2Utilix.AE2_UTILIX_TAB);
    }

    @Override
    public String getUpgradeTypeId() {
        return AE2Utilix.UPGRADE_PHASE_CARD;
    }

    @Override
    public int getMaxInstalled() {
        return 1;
    }

    @Override
    public boolean matchesType(ItemStack stack) {
        return stack.getItem() instanceof ItemPhaseCard;
    }

    public static void clearFace(ItemStack stack) {
        if (stack.hasTagCompound()) {
            NBTTagCompound tag = stack.getTagCompound();
            tag.removeTag(NBT_TAG);
            if (tag.hasNoTags()) {
                stack.setTagCompound(null);
            }
        }
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack held = player.getHeldItem(hand);
        if (player.isSneaking() && !world.isRemote && held.getItem() == this) {
            if (getFace(held) != null) {
                clearFace(held);
                return new ActionResult<>(EnumActionResult.SUCCESS, held);
            }
        }
        return new ActionResult<>(EnumActionResult.PASS, held);
    }

    @Override
    public EnumActionResult onItemUseFirst(EntityPlayer player, World world, BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ, EnumHand hand) {
        if (!player.isSneaking()) {
            return EnumActionResult.PASS;
        }

        ItemStack held = player.getHeldItem(hand);
        if (held.isEmpty() || held.getItem() != this) {
            return EnumActionResult.PASS;
        }

        IItemHandler upgrades = null;

        if (isInterfaceAt(world, pos, hitX, hitY, hitZ)) {
            upgrades = getUpgradeInventory(world, pos, hitX, hitY, hitZ);
        }

        if (upgrades != null) {
            if (world.isRemote) {
                return EnumActionResult.SUCCESS;
            }

            ItemStack oneCard = new ItemStack(this, 1);
            setFace(oneCard, side);

            for (int i = 0; i < upgrades.getSlots(); i++) {
                if (upgrades.isItemValid(i, oneCard) && upgrades.getStackInSlot(i).isEmpty()) {
                    upgrades.insertItem(i, oneCard, false);
                    held.shrink(1);
                    return EnumActionResult.SUCCESS;
                }
            }
            return EnumActionResult.PASS;
        }

        setFace(held, side);
        return EnumActionResult.SUCCESS;
    }

    private boolean isInterfaceAt(World world, BlockPos pos, float hitX, float hitY, float hitZ) {
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof IPartHost) {
            SelectedPart sp = ((IPartHost) te).selectPart(new Vec3d(hitX, hitY, hitZ));
            return sp.part instanceof appeng.api.implementations.IUpgradeableHost;
        }
        return te instanceof appeng.api.implementations.IUpgradeableHost;
    }

    private IItemHandler getUpgradeInventory(World world, BlockPos pos, float hitX, float hitY, float hitZ) {
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof IPartHost) {
            SelectedPart sp = ((IPartHost) te).selectPart(new Vec3d(hitX, hitY, hitZ));
            if (sp.part instanceof appeng.api.implementations.IUpgradeableHost) {
                return ((appeng.api.implementations.IUpgradeableHost) sp.part).getInventoryByName("upgrades");
            }
        } else if (te instanceof appeng.api.implementations.IUpgradeableHost) {
            return ((appeng.api.implementations.IUpgradeableHost) te).getInventoryByName("upgrades");
        }
        return null;
    }

    public static void setFace(ItemStack stack, EnumFacing face) {
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
        stack.getTagCompound().setInteger(NBT_TAG, face.ordinal());
    }

    @Nullable
    public static EnumFacing getFace(ItemStack stack) {
        if (stack.hasTagCompound() && stack.getTagCompound().hasKey(NBT_TAG)) {
            int ordinal = stack.getTagCompound().getInteger(NBT_TAG);
            if (ordinal >= 0 && ordinal < EnumFacing.values().length) {
                return EnumFacing.values()[ordinal];
            }
        }
        return null;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        EnumFacing face = getFace(stack);
        if (face != null) {
            tooltip.add(I18n.format("item.ae2_utilix.phase_card.tooltip.1", I18n.format("enumfacing." + face.getName())));
        } else {
            tooltip.add(I18n.format("item.ae2_utilix.phase_card.tooltip.2"));
        }
        ItemProductReturnCard.addSupportedTooltip(getUpgradeTypeId(), tooltip);
    }
}
