package com.ae2utilix.integration;

import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nullable;

public class ExtractFaceHelper {

    @Nullable
    public static EnumFacing findOutputFace(TileEntity te, EnumFacing primaryFace, ItemStack expectedItem, int amount) {
        IItemHandler primaryHandler = te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, primaryFace);
        if (primaryHandler != null && canSimulateExtract(primaryHandler, expectedItem, amount)
                && !hasNonExpectedExtractable(primaryHandler, expectedItem)) {
            return primaryFace;
        }

        EnumFacing fallbackFace = null;

        for (EnumFacing face : EnumFacing.values()) {
            if (face == primaryFace) continue;
            IItemHandler handler = te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, face);
            if (handler == null) continue;

            if (!canSimulateExtract(handler, expectedItem, amount)) continue;

            if (!hasNonExpectedExtractable(handler, expectedItem)) {
                return face;
            }

            if (fallbackFace == null) {
                fallbackFace = face;
            }
        }

        if (fallbackFace != null) {
            return fallbackFace;
        }

        IItemHandler nullHandler = te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
        if (nullHandler != null && canSimulateExtract(nullHandler, expectedItem, amount)) {
            return null;
        }

        return null;
    }

    public static boolean hasNonExpectedExtractable(IItemHandler handler, ItemStack expectedItem) {
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack slotStack = handler.getStackInSlot(i);
            if (slotStack.isEmpty()) continue;
            if (slotStack.isItemEqual(expectedItem) && ItemStack.areItemStackTagsEqual(slotStack, expectedItem)) continue;
            ItemStack simulated = handler.extractItem(i, 1, true);
            if (!simulated.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public static boolean canSimulateExtract(IItemHandler handler, ItemStack expectedItem, int amount) {
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack slotStack = handler.getStackInSlot(i);
            if (!slotStack.isEmpty() && slotStack.isItemEqual(expectedItem) && ItemStack.areItemStackTagsEqual(slotStack, expectedItem)) {
                ItemStack simulated = handler.extractItem(i, amount, true);
                if (!simulated.isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }
}
