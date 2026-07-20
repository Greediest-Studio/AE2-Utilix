package com.ae2utilix.gui;

import net.minecraft.item.ItemStack;
import appeng.container.slot.SlotOversized;
import net.minecraftforge.items.IItemHandler;

public class SlotFluidStorage extends SlotOversized {
    public SlotFluidStorage(IItemHandler inventory, int slot, int xPos, int yPos) {
        super(inventory, slot, xPos, yPos);
    }

    @Override
    public boolean canTakeStack(net.minecraft.entity.player.EntityPlayer player) {
        return getStack().isEmpty() || !com.ae2utilix.item.ItemFluidMark.isFluidMark(getStack());
    }

    @Override
    public boolean isItemValid(ItemStack stack) {
        return stack.isEmpty() || !com.ae2utilix.item.ItemFluidMark.isFluidMark(stack);
    }
}
