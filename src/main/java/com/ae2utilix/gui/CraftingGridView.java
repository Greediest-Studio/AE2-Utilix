package com.ae2utilix.gui;

import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;

/**
 * A view wrapper that exposes only a range of slots from a parent IItemHandler.
 * Used to limit SlotPatternTerm's view of the 81-slot crafting grid to just 9 slots.
 */
public class CraftingGridView implements IItemHandler, IItemHandlerModifiable {

    private final IItemHandler parent;
    private int offset;
    private final int size;

    public CraftingGridView(IItemHandler parent, int offset, int size) {
        this.parent = parent;
        this.offset = offset;
        this.size = size;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    @Override
    public int getSlots() {
        return this.size;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        if (slot < 0 || slot >= this.size) {
            return ItemStack.EMPTY;
        }
        return this.parent.getStackInSlot(this.offset + slot);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (slot < 0 || slot >= this.size) {
            return stack;
        }
        return this.parent.insertItem(this.offset + slot, stack, simulate);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (slot < 0 || slot >= this.size) {
            return ItemStack.EMPTY;
        }
        return this.parent.extractItem(this.offset + slot, amount, simulate);
    }

    @Override
    public int getSlotLimit(int slot) {
        if (slot < 0 || slot >= this.size) {
            return 0;
        }
        return this.parent.getSlotLimit(this.offset + slot);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        if (slot < 0 || slot >= this.size) {
            return false;
        }
        return this.parent.isItemValid(this.offset + slot, stack);
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        if (slot < 0 || slot >= this.size) {
            return;
        }
        if (this.parent instanceof IItemHandlerModifiable) {
            ((IItemHandlerModifiable) this.parent).setStackInSlot(this.offset + slot, stack);
        }
    }
}
