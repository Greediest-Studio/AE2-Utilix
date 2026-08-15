package com.ae2utilix.item;

import com.ae2utilix.AE2Utilix;
import com.ae2utilix.integration.BotaniaFluxIntegration;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import javax.annotation.Nullable;
import java.util.List;

public class ItemFluidMark extends Item {

    public static final String MARK_TAG = "ae2utilix_fluid_mark";
    private static final String GAS_KEY = "Gas";
    private static final String MANA_KEY = "Mana";
    private static final String FE_KEY = "FE";
    private static final String ESSENTIA_KEY = "Essentia";

    public ItemFluidMark() {
        this.setUnlocalizedName(AE2Utilix.MODID + ".fluid_mark");
        this.setRegistryName("fluid_mark");
        this.setMaxStackSize(1);
    }

    public static ItemStack create(FluidStack fluid) {
        ItemStack stack = new ItemStack(AE2Utilix.FLUID_MARK);
        NBTTagCompound tag = stack.getOrCreateSubCompound(MARK_TAG);
        tag.setString("Fluid", fluid.getFluid().getName());
        if (fluid.tag != null) {
            tag.setTag("FluidTag", fluid.tag.copy());
        }
        return stack;
    }

    /**
     * Creates a type-only gas token. The requested amount is kept in the
     * interface state, rather than in this marker item.
     */
    public static ItemStack createGas(String gasName) {
        ItemStack stack = new ItemStack(AE2Utilix.FLUID_MARK);
        NBTTagCompound tag = stack.getOrCreateSubCompound(MARK_TAG);
        tag.setString(GAS_KEY, gasName == null ? "" : gasName);
        return stack;
    }

    /** Creates a type-only Botania Applie mana token. */
    public static ItemStack createManaMark() {
        ItemStack stack = new ItemStack(AE2Utilix.FLUID_MARK);
        stack.getOrCreateSubCompound(MARK_TAG).setBoolean(MANA_KEY, true);
        return stack;
    }

    /** Creates a type-only Flux Applied FE token. */
    public static ItemStack createFeMark() {
        ItemStack stack = new ItemStack(AE2Utilix.FLUID_MARK);
        stack.getOrCreateSubCompound(MARK_TAG).setBoolean(FE_KEY, true);
        return stack;
    }

    /** Creates a type-only Thaumic Energistics essentia marker. */
    public static ItemStack createEssentia(String aspectTag) {
        ItemStack nativeMarker = com.ae2utilix.integration.ThaumicEnergisticsIntegration
                .createAspectItem(aspectTag);
        if (!nativeMarker.isEmpty()) return nativeMarker;

        ItemStack stack = new ItemStack(AE2Utilix.FLUID_MARK);
        if (aspectTag != null && !aspectTag.isEmpty()) {
            stack.getOrCreateSubCompound(MARK_TAG).setString(ESSENTIA_KEY, aspectTag);
        }
        return stack;
    }

    /** Replaces legacy Utilix essentia tokens with ThE's native dummy item. */
    public static ItemStack upgradeEssentiaMarker(ItemStack stack) {
        if (stack == null || stack.isEmpty() || stack.getItem() != AE2Utilix.FLUID_MARK) {
            return stack == null ? ItemStack.EMPTY : stack;
        }
        String aspectTag = getAspectTag(stack);
        if (aspectTag == null) return stack;
        ItemStack nativeMarker = com.ae2utilix.integration.ThaumicEnergisticsIntegration
                .createAspectItem(aspectTag);
        return nativeMarker.isEmpty() ? stack : nativeMarker;
    }

    @Nullable
    public static FluidStack getFluid(ItemStack stack) {
        if (stack.isEmpty()) return null;

        if (stack.getItem() == AE2Utilix.FLUID_MARK && stack.hasTagCompound()) {
            NBTTagCompound tag = stack.getTagCompound().getCompoundTag(MARK_TAG);
            Fluid fluid = FluidRegistry.getFluid(tag.getString("Fluid"));
            if (fluid == null) return null;
            FluidStack result = new FluidStack(fluid, 1000);
            if (tag.hasKey("FluidTag", 10)) {
                result.tag = tag.getCompoundTag("FluidTag").copy();
            }
            return result;
        }

        if (stack.getItem() instanceof appeng.fluids.items.FluidDummyItem) {
            FluidStack result = ((appeng.fluids.items.FluidDummyItem) stack.getItem()).getFluidStack(stack);
            return result == null ? null : result.copy();
        }

        return null;
    }

    public static boolean isFluidMark(ItemStack stack) {
        return getFluid(stack) != null;
    }

    @Nullable
    public static String getGasName(ItemStack stack) {
        if (stack.isEmpty() || stack.getItem() != AE2Utilix.FLUID_MARK
                || !stack.hasTagCompound()) {
            return null;
        }

        NBTTagCompound tag = stack.getTagCompound().getCompoundTag(MARK_TAG);
        String gasName = tag.getString(GAS_KEY);
        return gasName.isEmpty() ? null : gasName;
    }

    public static boolean isGasMark(ItemStack stack) {
        return getGasName(stack) != null;
    }

    public static boolean isManaMark(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() == AE2Utilix.FLUID_MARK
                && stack.hasTagCompound()
                && stack.getTagCompound().getCompoundTag(MARK_TAG).getBoolean(MANA_KEY);
    }

    public static boolean isFeMark(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() == AE2Utilix.FLUID_MARK
                && stack.hasTagCompound()
                && stack.getTagCompound().getCompoundTag(MARK_TAG).getBoolean(FE_KEY);
    }

    @Nullable
    public static String getAspectTag(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        if (stack.getItem() == AE2Utilix.FLUID_MARK && stack.hasTagCompound()) {
            String tag = stack.getTagCompound().getCompoundTag(MARK_TAG).getString(ESSENTIA_KEY);
            return tag.isEmpty() ? null : tag;
        }
        return com.ae2utilix.integration.ThaumicEnergisticsIntegration.getAspectTagFromItem(stack);
    }

    public static boolean isEssentiaMark(ItemStack stack) {
        return getAspectTag(stack) != null;
    }

    public static boolean isVirtualMark(ItemStack stack) {
        return isFluidMark(stack) || isGasMark(stack) || isManaMark(stack) || isFeMark(stack)
                || isEssentiaMark(stack);
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        FluidStack fluid = getFluid(stack);
        if (fluid != null) {
            return fluid.getLocalizedName();
        }
        String gasName = getGasName(stack);
        if (gasName != null) {
            return I18n.translateToLocal("gas." + gasName);
        }
        if (isManaMark(stack)) {
            return BotaniaFluxIntegration.getPacketDisplayName(BotaniaFluxIntegration.MANA);
        }
        if (isFeMark(stack)) {
            return BotaniaFluxIntegration.getPacketDisplayName(BotaniaFluxIntegration.FE);
        }
        String aspectTag = getAspectTag(stack);
        if (aspectTag != null) {
            String name = com.ae2utilix.integration.ThaumicEnergisticsIntegration
                    .getAspectDisplayName(aspectTag);
            return name == null ? aspectTag : name;
        }
        return I18n.translateToLocal(this.getUnlocalizedName() + ".name");
    }

    @Override
    public boolean showDurabilityBar(ItemStack stack) {
        return false;
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flags) {
        FluidStack fluid = getFluid(stack);
        if (fluid != null) {
            tooltip.add(fluid.getLocalizedName());
            tooltip.add(I18n.translateToLocal("ae2_utilix.common_interface.fluid_mark.tooltip"));
            return;
        }

        String gasName = getGasName(stack);
        if (gasName != null) {
            tooltip.add(I18n.translateToLocal("gas." + gasName));
            tooltip.add(I18n.translateToLocal("ae2_utilix.common_interface.gas_mark.tooltip"));
            return;
        }
        if (isManaMark(stack)) {
            tooltip.add(BotaniaFluxIntegration.getPacketDisplayName(BotaniaFluxIntegration.MANA));
            tooltip.add(I18n.translateToLocal("ae2_utilix.common_interface.mana_mark.tooltip"));
            return;
        }
        if (isFeMark(stack)) {
            tooltip.add(BotaniaFluxIntegration.getPacketDisplayName(BotaniaFluxIntegration.FE));
            tooltip.add(I18n.translateToLocal("ae2_utilix.common_interface.fe_mark.tooltip"));
            return;
        }
        String aspectTag = getAspectTag(stack);
        if (aspectTag != null) {
            String name = com.ae2utilix.integration.ThaumicEnergisticsIntegration
                    .getAspectDisplayName(aspectTag);
            tooltip.add(name == null ? aspectTag : name);
            tooltip.add(I18n.translateToLocal("ae2_utilix.common_interface.essentia_mark.tooltip"));
        }
    }
}
