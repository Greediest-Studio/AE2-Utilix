package com.ae2utilix.item;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.features.INetworkEncodable;
import appeng.api.features.ILocatable;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.energy.IEnergyGrid;
import appeng.api.networking.security.IActionHost;
import appeng.items.tools.powered.powersink.AEBasePoweredItem;
import com.ae2utilix.AE2Utilix;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

public class ItemFluixResonancePivotCore extends AEBasePoweredItem implements INetworkEncodable {

    private static final double MAX_POWER = 800000000.0;
    private static final String NBT_ENCRYPTION_KEY = "encryptionKey";

    public ItemFluixResonancePivotCore() {
        super(MAX_POWER);
        this.setUnlocalizedName(AE2Utilix.MODID + ".fluix_resonance_pivot_core");
        this.setRegistryName("fluix_resonance_pivot_core");
        this.setCreativeTab(AE2Utilix.AE2_UTILIX_TAB);
    }

    @Override
    public String getEncryptionKey(ItemStack item) {
        NBTTagCompound tag = item.getTagCompound();
        if (tag != null && tag.hasKey(NBT_ENCRYPTION_KEY)) {
            return tag.getString(NBT_ENCRYPTION_KEY);
        }
        return "";
    }

    @Override
    public void setEncryptionKey(ItemStack item, String encKey, String name) {
        NBTTagCompound tag = item.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            item.setTagCompound(tag);
        }
        if (encKey != null && !encKey.isEmpty()) {
            tag.setString(NBT_ENCRYPTION_KEY, encKey);
        } else {
            tag.removeTag(NBT_ENCRYPTION_KEY);
        }
    }

    public static boolean isLinked(ItemStack stack) {
        if (stack.getItem() instanceof ItemFluixResonancePivotCore) {
            String key = ((ItemFluixResonancePivotCore) stack.getItem()).getEncryptionKey(stack);
            return key != null && !key.isEmpty();
        }
        return false;
    }

    private IEnergyGrid getEnergyGrid(ItemStack stack) {
        String key = this.getEncryptionKey(stack);
        if (key == null || key.isEmpty()) return null;

        try {
            long encKey = Long.parseLong(key);
            ILocatable obj = AEApi.instance().registries().locatable().getLocatableBy(encKey);
            if (obj instanceof IActionHost) {
                IGridNode node = ((IActionHost) obj).getActionableNode();
                if (node != null) {
                    IGrid grid = node.getGrid();
                    if (grid != null) {
                        return grid.getCache(IEnergyGrid.class);
                    }
                }
            }
        } catch (NumberFormatException ignored) {
        }
        return null;
    }

    @Override
    public void onUpdate(ItemStack stack, World worldIn, Entity entityIn, int itemSlot, boolean isSelected) {
        super.onUpdate(stack, worldIn, entityIn, itemSlot, isSelected);

        if (worldIn.isRemote) return;
        if (!(entityIn instanceof EntityPlayer)) return;
        if (entityIn.ticksExisted % 10 != 0) return;

        tickCore(stack, (EntityPlayer) entityIn);
    }

    public static void tickCore(ItemStack stack, EntityPlayer player) {
        ItemFluixResonancePivotCore item = (ItemFluixResonancePivotCore) stack.getItem();

        IEnergyGrid energyGrid = item.getEnergyGrid(stack);
        if (energyGrid != null) {
            double powerNeeded = MAX_POWER - item.getAECurrentPower(stack);
            if (powerNeeded > 0) {
                double extracted = energyGrid.extractAEPower(powerNeeded, Actionable.MODULATE, appeng.api.config.PowerMultiplier.ONE);
                if (extracted > 0) {
                    item.injectAEPower(stack, extracted, Actionable.MODULATE);
                }
            }
        }

        item.chargeInventory(stack, player);

        if (Loader.isModLoaded("baubles")) {
            com.ae2utilix.integration.BaublesIntegration.chargeBaublesInventory(stack, player);
        }
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        if (!world.isRemote && Loader.isModLoaded("baubles")) {
            ItemStack heldItem = player.getHeldItem(hand);
            if (com.ae2utilix.integration.BaublesIntegration.tryEquip(player, heldItem)) {
                return new ActionResult<>(EnumActionResult.SUCCESS, heldItem);
            }
        }
        return super.onItemRightClick(world, player, hand);
    }

    private void chargeInventory(ItemStack coreStack, EntityPlayer player) {
        double available = this.getAECurrentPower(coreStack);
        if (available <= 0) return;

        InventoryPlayer inv = player.inventory;
        for (int i = 0; i < inv.getSizeInventory(); i++) {
            if (available <= 0) break;

            ItemStack target = inv.getStackInSlot(i);
            if (target.isEmpty()) continue;
            if (target == coreStack) continue;
            if (!(target.getItem() instanceof AEBasePoweredItem)) continue;

            AEBasePoweredItem poweredItem = (AEBasePoweredItem) target.getItem();
            double targetCurrent = poweredItem.getAECurrentPower(target);
            double targetMax = poweredItem.getAEMaxPower(target);
            double targetNeeded = targetMax - targetCurrent;
            if (targetNeeded <= 0) continue;

            double toInject = Math.min(available, targetNeeded);
            double overflow = poweredItem.injectAEPower(target, toInject, Actionable.MODULATE);
            double actuallyUsed = toInject - overflow;
            if (actuallyUsed > 0) {
                this.extractAEPower(coreStack, actuallyUsed, Actionable.MODULATE);
                available -= actuallyUsed;
            }
        }
    }

    @Override
    public boolean isDamaged(ItemStack stack) {
        return false;
    }

    @Override
    public boolean showDurabilityBar(ItemStack stack) {
        return true;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addCheckedInformation(ItemStack stack, World world, List<String> lines, ITooltipFlag advancedTooltips) {
        super.addCheckedInformation(stack, world, lines, advancedTooltips);

        String linked = isLinked(stack)
                ? TextFormatting.GREEN + net.minecraft.client.resources.I18n.format("item.ae2_utilix.fluix_resonance_pivot_core.linked")
                : TextFormatting.RED + net.minecraft.client.resources.I18n.format("item.ae2_utilix.fluix_resonance_pivot_core.unlinked");
        lines.add(linked);
    }
}
