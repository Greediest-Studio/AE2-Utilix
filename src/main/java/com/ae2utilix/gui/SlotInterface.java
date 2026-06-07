package com.ae2utilix.gui;

import appeng.container.slot.AppEngSlot;
import appeng.items.misc.ItemEncodedPattern;
import appeng.util.Platform;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

/**
 * Custom slot for interface terminal that reads from InvTracker.client directly.
 * Similar to AE2 UEL's SlotDisconnected but uses our InvTracker for data.
 */
public class SlotInterface extends AppEngSlot {

    private final ContainerFullInterface.InvTracker tracker;

    public SlotInterface(final ContainerFullInterface.InvTracker tracker, final int slotIndex, final int x, final int y) {
        super(tracker.client, slotIndex, x, y);
        this.tracker = tracker;
    }

    @Override
    public boolean isItemValid(final ItemStack par1ItemStack) {
        return false;
    }

    @Override
    public void putStack(final ItemStack par1ItemStack) {
        // No-op: items are synced via PacketCompressedNBT
    }

    @Override
    public boolean canTakeStack(final EntityPlayer par1EntityPlayer) {
        return false;
    }

    @Override
    public ItemStack getDisplayStack() {
        if (Platform.isClient()) {
            final ItemStack is = super.getStack();
            if (!is.isEmpty() && is.getItem() instanceof ItemEncodedPattern) {
                final ItemEncodedPattern iep = (ItemEncodedPattern) is.getItem();
                final ItemStack out = iep.getOutput(is);
                if (!out.isEmpty()) {
                    return out;
                }
            }
        }
        return super.getStack();
    }

    @Override
    public boolean getHasStack() {
        return !this.getStack().isEmpty();
    }

    @Override
    public int getSlotStackLimit() {
        return 0;
    }

    @Override
    public ItemStack decrStackSize(final int par1) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean isHere(final IInventory inv, final int slotIn) {
        return false;
    }

    public ContainerFullInterface.InvTracker getTracker() {
        return this.tracker;
    }

    public long getId() {
        return this.tracker.which;
    }
}
