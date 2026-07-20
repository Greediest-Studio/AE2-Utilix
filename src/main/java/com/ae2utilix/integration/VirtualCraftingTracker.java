package com.ae2utilix.integration;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.networking.crafting.ICraftingGrid;
import appeng.api.networking.crafting.ICraftingJob;
import appeng.api.networking.crafting.ICraftingLink;
import appeng.api.networking.security.IActionSource;
import appeng.api.storage.data.IAEItemStack;
import appeng.me.GridAccessException;
import appeng.me.helpers.MachineSource;
import com.ae2utilix.block.TileCommonInterfaceAlternate;
import com.google.common.collect.ImmutableSet;
import net.minecraft.nbt.NBTTagCompound;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Tracks crafting jobs whose result is represented by an AE2FluidCraft fake
 * item, but is ultimately stored in a virtual resource slot.
 */
public final class VirtualCraftingTracker {
    private static final int SLOT_COUNT = 36;

    private final TileCommonInterfaceAlternate owner;
    private final Future<ICraftingJob>[] jobs;
    private final ICraftingLink[] links;

    @SuppressWarnings("unchecked")
    public VirtualCraftingTracker(TileCommonInterfaceAlternate owner) {
        this.owner = owner;
        this.jobs = new Future[SLOT_COUNT];
        this.links = new ICraftingLink[SLOT_COUNT];
    }

    public void request(int slot, IAEItemStack output) {
        if (slot < 0 || slot >= SLOT_COUNT || output == null || output.getStackSize() <= 0
                || this.links[slot] != null) {
            return;
        }

        try {
            ICraftingGrid crafting = this.owner.getProxy().getCrafting();
            IActionSource source = new MachineSource(this.owner);
            Future<ICraftingJob> pending = this.jobs[slot];
            if (pending == null) {
                this.jobs[slot] = crafting.beginCraftingJob(
                        this.owner.getWorld(), this.owner.getProxy().getGrid(), source, output, null);
                return;
            }

            if (!pending.isDone()) {
                return;
            }

            ICraftingJob job = pending.get();
            this.jobs[slot] = null;
            if (job != null && !job.isSimulation()) {
                this.links[slot] = crafting.submitJob(job, this.owner, null, false, source);
            }
        } catch (GridAccessException ignored) {
            // The network may disappear between ticks; retry while the slot is short.
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
            this.jobs[slot] = null;
        } catch (ExecutionException ignored) {
            this.jobs[slot] = null;
        }
    }

    public boolean hasWork() {
        for (int i = 0; i < SLOT_COUNT; i++) {
            if (this.jobs[i] != null || this.links[i] != null) {
                return true;
            }
        }
        return false;
    }

    public boolean hasWork(int slot) {
        return slot >= 0 && slot < SLOT_COUNT
                && (this.jobs[slot] != null || this.links[slot] != null);
    }

    public ImmutableSet<ICraftingLink> getRequestedJobs() {
        List<ICraftingLink> active = new ArrayList<>();
        for (int i = 0; i < SLOT_COUNT; i++) {
            ICraftingLink link = this.links[i];
            if (link != null && !link.isCanceled() && !link.isDone()) {
                active.add(link);
            } else if (link != null) {
                this.links[i] = null;
            }
        }
        return active.isEmpty() ? ImmutableSet.<ICraftingLink>of() : ImmutableSet.copyOf(active);
    }

    public int getSlot(@Nullable ICraftingLink link) {
        if (link == null) {
            return -1;
        }
        for (int i = 0; i < SLOT_COUNT; i++) {
            if (this.links[i] == link) {
                return i;
            }
        }
        return -1;
    }

    public IAEItemStack injectCraftedItems(ICraftingLink link, IAEItemStack stack, Actionable mode) {
        int slot = this.getSlot(link);
        return slot < 0 ? stack : this.owner.acceptVirtualCraftedItems(slot, stack, mode);
    }

    public void jobStateChange(ICraftingLink link) {
        int slot = this.getSlot(link);
        if (slot >= 0) {
            this.links[slot] = null;
            this.owner.wakeVirtualCrafting();
        }
    }

    public void cancel(int slot) {
        if (slot < 0 || slot >= SLOT_COUNT) {
            return;
        }
        if (this.links[slot] != null) {
            this.links[slot].cancel();
            this.links[slot] = null;
        }
        if (this.jobs[slot] != null) {
            this.jobs[slot].cancel(true);
            this.jobs[slot] = null;
        }
    }

    public void writeToNBT(NBTTagCompound parent) {
        NBTTagCompound data = new NBTTagCompound();
        for (int i = 0; i < SLOT_COUNT; i++) {
            ICraftingLink link = this.links[i];
            if (link != null && !link.isCanceled() && !link.isDone()) {
                NBTTagCompound linkData = new NBTTagCompound();
                link.writeToNBT(linkData);
                data.setTag("link-" + i, linkData);
            }
        }
        parent.setTag("ae2utilix_virtual_crafting", data);
    }

    public void readFromNBT(NBTTagCompound parent) {
        NBTTagCompound data = parent.getCompoundTag("ae2utilix_virtual_crafting");
        for (int i = 0; i < SLOT_COUNT; i++) {
            NBTTagCompound linkData = data.getCompoundTag("link-" + i);
            if (!linkData.hasNoTags()) {
                this.links[i] = AEApi.instance().storage().loadCraftingLink(linkData, this.owner);
            }
        }
    }
}
