package com.ae2utilix.integration;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.security.IActionHost;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.me.GridAccessException;
import appeng.me.helpers.AENetworkProxy;
import appeng.me.helpers.MachineSource;
import appeng.util.Platform;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * Read-only Forge item capability backed by the ME network.
 */
public final class NetworkStorageItemHandler implements IItemHandler {
    private final AENetworkProxy proxy;
    private final IActionSource source;
    private IMEMonitor<IAEItemStack> monitor;
    private final List<IAEItemStack> cache = new ArrayList<>();

    public NetworkStorageItemHandler(AENetworkProxy proxy, IActionHost sourceHost) {
        this.proxy = proxy;
        this.source = new MachineSource(sourceHost);
    }

    private void refresh() {
        IMEMonitor<IAEItemStack> next = null;
        if (this.proxy.isActive()) {
            try {
                next = this.proxy.getStorage().getInventory(
                        AEApi.instance().storage().getStorageChannel(IItemStorageChannel.class));
            } catch (GridAccessException ignored) {
            }
        }

        this.monitor = next;
        this.cache.clear();
        if (this.monitor != null) {
            for (IAEItemStack stack : this.monitor.getStorageList()) {
                if (stack != null && stack.getStackSize() > 0) {
                    this.cache.add(stack.copy());
                }
            }
        }
    }

    public IMEMonitor<IAEItemStack> getMonitor() {
        this.refresh();
        return this.monitor;
    }

    @Override
    public int getSlots() {
        this.refresh();
        return this.cache.size();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        this.refresh();
        if (slot < 0 || slot >= this.cache.size()) {
            return ItemStack.EMPTY;
        }
        return this.cache.get(slot).createItemStack();
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        return stack;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (amount <= 0) {
            return ItemStack.EMPTY;
        }

        this.refresh();
        if (this.monitor == null || slot < 0 || slot >= this.cache.size()) {
            return ItemStack.EMPTY;
        }

        IAEItemStack request = this.cache.get(slot).copy().setStackSize(amount);
        try {
            IAEItemStack extracted = Platform.poweredExtraction(
                    this.proxy.getEnergy(),
                    this.monitor,
                    request,
                    this.source,
                    simulate ? Actionable.SIMULATE : Actionable.MODULATE);
            return extracted == null ? ItemStack.EMPTY : extracted.createItemStack();
        } catch (GridAccessException ignored) {
            return ItemStack.EMPTY;
        }
    }

    @Override
    public int getSlotLimit(int slot) {
        return Integer.MAX_VALUE;
    }

}
