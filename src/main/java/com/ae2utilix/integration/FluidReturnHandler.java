package com.ae2utilix.integration;

import appeng.api.storage.data.IAEItemStack;
import appeng.util.item.AEItemStack;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fml.common.Loader;

import javax.annotation.Nullable;

public class FluidReturnHandler {

    private static Item FLUID_DROP;
    private static Item FLUID_PACKET;
    private static boolean ae2fcChecked = false;

    public static boolean isFluidFakeItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.getItem() == com.ae2utilix.AE2Utilix.FLUID_MARK) {
            return com.ae2utilix.item.ItemFluidMark.isFluidMark(stack);
        }
        checkAE2FC();
        if (FLUID_DROP == null && FLUID_PACKET == null) return false;
        return stack.getItem() == FLUID_DROP || stack.getItem() == FLUID_PACKET;
    }

    @Nullable
    public static FluidStack getFluidFromAEStack(IAEItemStack stack) {
        if (stack == null) return null;
        ItemStack def = stack.getDefinition();
        if (def.isEmpty()) return null;
        return parseFluidDrop(def);
    }

    @Nullable
    private static FluidStack parseFluidDrop(ItemStack stack) {
        if (stack.getItem() == com.ae2utilix.AE2Utilix.FLUID_MARK) {
            return com.ae2utilix.item.ItemFluidMark.getFluid(stack);
        }
        if (!stack.hasTagCompound()) return null;
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null || !tag.hasKey("Fluid", 8)) return null;

        if (stack.getItem() == FLUID_DROP) {
            net.minecraftforge.fluids.Fluid fluid = FluidRegistry.getFluid(tag.getString("Fluid"));
            if (fluid == null) return null;
            FluidStack fs = new FluidStack(fluid, stack.getCount());
            if (tag.hasKey("FluidTag", 10)) {
                fs.tag = tag.getCompoundTag("FluidTag");
            }
            return fs;
        }

        if (stack.getItem() == FLUID_PACKET) {
            if (tag.hasKey("FluidStack", 10)) {
                return FluidStack.loadFluidStackFromNBT(tag.getCompoundTag("FluidStack"));
            }
        }

        return null;
    }

    @Nullable
    public static FluidStack extractFluid(TileEntity te, EnumFacing face, FluidStack expected, int maxDrain) {
        if (te == null || expected == null) return null;
        IFluidHandler handler = te.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, face);
        if (handler == null) return null;

        FluidStack simulated = handler.drain(new FluidStack(expected, maxDrain), false);
        if (simulated == null || simulated.amount <= 0) return null;

        return handler.drain(new FluidStack(expected, simulated.amount), true);
    }

    @Nullable
    public static IAEItemStack packFluid2AEDrops(FluidStack fluid) {
        if (fluid == null || fluid.amount <= 0) return null;
        checkAE2FC();
        if (FLUID_DROP == null) return null;

        int stackCount = Math.min(fluid.amount, 64);
        ItemStack itemStack = new ItemStack(FLUID_DROP, stackCount);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("Fluid", fluid.getFluid().getName());
        if (fluid.tag != null) {
            tag.setTag("FluidTag", fluid.tag);
        }
        itemStack.setTagCompound(tag);

        IAEItemStack aeStack = AEItemStack.fromItemStack(itemStack);
        if (aeStack == null) return null;
        aeStack.setStackSize(fluid.amount);
        return aeStack;
    }

    @Nullable
    public static EnumFacing findFluidOutputFace(TileEntity te, EnumFacing primaryFace, FluidStack expected, int maxDrain) {
        if (te == null || expected == null) return null;

        if (canDrainFluid(te, primaryFace, expected, maxDrain)) {
            return primaryFace;
        }

        for (EnumFacing face : EnumFacing.values()) {
            if (face == primaryFace) continue;
            if (canDrainFluid(te, face, expected, maxDrain)) {
                return face;
            }
        }

        if (canDrainFluid(te, null, expected, maxDrain)) {
            return null;
        }

        return null;
    }

    public static int fillFluid(TileEntity te, EnumFacing face, FluidStack fluid) {
        if (te == null || fluid == null || fluid.amount <= 0) return 0;
        IFluidHandler handler = te.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, face);
        if (handler == null) return 0;
        return handler.fill(fluid, true);
    }

    public static boolean hasAE2FC() {
        checkAE2FC();
        return FLUID_DROP != null;
    }

    private static void checkAE2FC() {
        if (ae2fcChecked) return;
        ae2fcChecked = true;
        if (!Loader.isModLoaded("ae2fc")) return;
        try {
            FLUID_DROP = Item.getByNameOrId("ae2fc:fluid_drop");
            FLUID_PACKET = Item.getByNameOrId("ae2fc:fluid_packet");
        } catch (Exception ignored) {
        }
    }

    private static boolean canDrainFluid(TileEntity te, EnumFacing face, FluidStack expected, int maxDrain) {
        IFluidHandler handler = te.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, face);
        if (handler == null) return false;
        FluidStack simulated = handler.drain(new FluidStack(expected, maxDrain), false);
        return simulated != null && simulated.amount > 0;
    }
}
