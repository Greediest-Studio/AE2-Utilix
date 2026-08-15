package com.ae2utilix.integration.jei;

import com.ae2utilix.integration.BotaniaFluxIntegration;
import com.ae2utilix.integration.MekanismEnergisticsIntegration;
import com.ae2utilix.integration.ThaumicEnergisticsIntegration;
import com.ae2utilix.item.ItemFluidMark;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;

import javax.annotation.Nullable;

/** Converts JEI ingredients into the type-only marker values used by Utilix. */
public final class VirtualMarkJeiHelper {
    private VirtualMarkJeiHelper() {
    }

    public static final class Mark {
        public final FluidStack fluid;
        /** A normal item ingredient (used by JEI ghost-drag item filters). */
        public final ItemStack item;
        public final String gasName;
        public final String aspectName;
        public final int specialType;

        private Mark(FluidStack fluid, ItemStack item, String gasName, String aspectName, int specialType) {
            this.fluid = fluid;
            this.item = item;
            this.gasName = gasName;
            this.aspectName = aspectName;
            this.specialType = specialType;
        }

        public static Mark fluid(FluidStack fluid) {
            return new Mark(fluid, null, null, null, 0);
        }

        public static Mark item(ItemStack item) {
            return new Mark(null, item, null, null, 0);
        }

        public static Mark gas(String gasName) {
            return new Mark(null, null, gasName, null, 0);
        }

        public static Mark essentia(String aspectName) {
            return new Mark(null, null, null, aspectName, 0);
        }

        public static Mark special(int specialType) {
            return new Mark(null, null, null, null, specialType);
        }
    }

    /**
     * Handles both normal JEI item ingredients and the custom GasStack
     * ingredients exposed by Mekanism's JEI plugin.
     */
    @Nullable
    public static Mark fromIngredient(Object ingredient) {
        if (ingredient instanceof FluidStack) {
            FluidStack fluid = ((FluidStack) ingredient).copy();
            return fluid.getFluid() == null ? null : Mark.fluid(fluid);
        }

        if (ingredient instanceof ItemStack) {
            ItemStack stack = (ItemStack) ingredient;
            if (stack.isEmpty()) return null;

            FluidStack fluid = FluidUtil.getFluidContained(stack);
            if (fluid == null) fluid = ItemFluidMark.getFluid(stack);
            if (fluid == null && stack.getItem() == Items.WATER_BUCKET) {
                fluid = new FluidStack(FluidRegistry.WATER, 1000);
            }
            if (fluid != null && fluid.getFluid() != null) {
                return Mark.fluid(fluid.copy());
            }

            String gas = MekanismEnergisticsIntegration.getGasNameFromItem(stack);
            if (gas != null && !gas.isEmpty()) return Mark.gas(gas);

            int special = BotaniaFluxIntegration.getMarkedType(stack);
            if (special != 0) return Mark.special(special);

            String aspect = ItemFluidMark.getAspectTag(stack);
            if (aspect == null) {
                aspect = ThaumicEnergisticsIntegration.getAspectTagFromItem(stack);
            }
            if (aspect != null && !aspect.isEmpty()) return Mark.essentia(aspect);

            // JEI also supplies ordinary item ingredients.  They are valid
            // item filters for a common bus/interface even when they do not
            // represent one of the virtual resource types above.  Keep a
            // single, independent copy so the server never trusts a mutable
            // JEI stack/count.
            ItemStack item = stack.copy();
            item.setCount(1);
            return Mark.item(item);
        }

        // Botania Applie's/Flux Applied's AE2 channel stacks are not ItemStacks,
        // but some JEI integrations expose them directly as ingredients.
        if (ingredient != null) {
            String typeName = ingredient.getClass().getName();
            if ("nyonio.ae2.ManaStack".equals(typeName)
                    && BotaniaFluxIntegration.isManaIntegrationAvailable()) {
                return Mark.special(BotaniaFluxIntegration.MANA);
            }
            if ("com.flux_applied.ae2.FluxStack".equals(typeName)
                    && BotaniaFluxIntegration.isFeIntegrationAvailable()) {
                return Mark.special(BotaniaFluxIntegration.FE);
            }
        }

        String gas = MekanismEnergisticsIntegration.getGasNameFromIngredient(ingredient);
        if (gas != null && !gas.isEmpty()) return Mark.gas(gas);

        String aspect = ThaumicEnergisticsIntegration.getAspectTagFromIngredient(ingredient);
        return aspect == null || aspect.isEmpty() ? null : Mark.essentia(aspect);
    }
}
