package com.ae2utilix.gui;

import appeng.api.storage.data.IAEFluidStack;
import appeng.api.implementations.items.IUpgradeModule;
import appeng.fluids.container.slots.IMEFluidSlot;
import com.ae2utilix.block.TileCommonInterfaceAlternate;
import net.minecraft.item.ItemStack;
import appeng.container.slot.SlotOversized;
import net.minecraftforge.items.IItemHandler;

public class SlotFluidStorage extends SlotOversized implements IMEFluidSlot {
    private final TileCommonInterfaceAlternate tile;
    private final boolean extended;
    private final int fluidSlot;

    public SlotFluidStorage(TileCommonInterfaceAlternate tile, boolean extended,
                            IItemHandler inventory, int slot, int xPos, int yPos) {
        super(inventory, slot, xPos, yPos);
        this.tile = tile;
        this.extended = extended;
        this.fluidSlot = slot;
    }

    @Override
    public boolean canTakeStack(net.minecraft.entity.player.EntityPlayer player) {
        return getStack().isEmpty() || !com.ae2utilix.item.ItemFluidMark.isVirtualMark(getStack());
    }

    @Override
    public boolean isItemValid(ItemStack stack) {
        return stack.isEmpty()
                || (!(stack.getItem() instanceof IUpgradeModule)
                && !com.ae2utilix.item.ItemFluidMark.isVirtualMark(stack)
                && !this.tile.hasVirtualStorage(this.extended, this.fluidSlot));
    }

    @Override
    public IAEFluidStack getAEFluidStack() {
        net.minecraftforge.fluids.FluidStack fluid = this.tile.getStoredFluid(this.extended, this.fluidSlot);
        return fluid == null ? null : appeng.fluids.util.AEFluidStack.fromFluidStack(fluid);
    }

    @Override
    public boolean shouldRenderAsFluid() {
        return this.getAEFluidStack() != null;
    }
}
