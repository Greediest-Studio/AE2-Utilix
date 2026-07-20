package com.ae2utilix.integration;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.security.IActionHost;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.channels.IFluidStorageChannel;
import appeng.api.storage.data.IAEFluidStack;
import appeng.me.GridAccessException;
import appeng.me.helpers.AENetworkProxy;
import appeng.me.helpers.MachineSource;
import appeng.util.Platform;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.FluidTankProperties;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Read-only Forge fluid capability backed by the ME network.
 */
public final class NetworkStorageFluidHandler implements IFluidHandler {
    private static final IFluidTankProperties[] EMPTY_PROPERTIES = new IFluidTankProperties[0];

    private final AENetworkProxy proxy;
    private final IActionSource source;
    private IMEMonitor<IAEFluidStack> monitor;
    private final List<IAEFluidStack> cache = new ArrayList<>();

    public NetworkStorageFluidHandler(AENetworkProxy proxy, IActionHost sourceHost) {
        this.proxy = proxy;
        this.source = new MachineSource(sourceHost);
    }

    private void refresh() {
        IMEMonitor<IAEFluidStack> next = null;
        if (this.proxy.isActive()) {
            try {
                next = this.proxy.getStorage().getInventory(
                        AEApi.instance().storage().getStorageChannel(IFluidStorageChannel.class));
            } catch (GridAccessException ignored) {
            }
        }

        this.monitor = next;
        this.cache.clear();
        if (this.monitor != null) {
            for (IAEFluidStack stack : this.monitor.getStorageList()) {
                if (stack != null && stack.getStackSize() > 0) {
                    this.cache.add(stack.copy());
                }
            }
        }
    }

    public IMEMonitor<IAEFluidStack> getMonitor() {
        this.refresh();
        return this.monitor;
    }

    /**
     * Extracts a precise fluid stack from the network using the same monitor
     * that is exposed through the Forge fluid capability.
     */
    public IAEFluidStack extract(IAEFluidStack requested, Actionable mode) {
        if (requested == null || requested.getStackSize() <= 0) {
            return null;
        }

        this.refresh();
        if (this.monitor == null) {
            return null;
        }

        IAEFluidStack available = this.monitor.getStorageList().findPrecise(requested);
        if (available == null) {
            for (IAEFluidStack candidate : this.monitor.getStorageList()) {
                FluidStack candidateFluid = candidate.getFluidStack();
                if (candidateFluid != null && candidateFluid.isFluidEqual(requested.getFluidStack())) {
                    available = candidate;
                    break;
                }
            }
        }
        if (available == null || available.getStackSize() <= 0) {
            return null;
        }

        IAEFluidStack request = available.copy().setStackSize(
                Math.min(requested.getStackSize(), available.getStackSize()));
        try {
            return Platform.poweredExtraction(
                    this.proxy.getEnergy(), this.monitor, request, this.source, mode);
        } catch (GridAccessException ignored) {
            return null;
        }
    }

    @Override
    public IFluidTankProperties[] getTankProperties() {
        this.refresh();
        if (this.cache.isEmpty()) {
            return EMPTY_PROPERTIES;
        }

        IFluidTankProperties[] properties = new IFluidTankProperties[this.cache.size()];
        for (int i = 0; i < this.cache.size(); i++) {
            IAEFluidStack stack = this.cache.get(i);
            FluidStack fluid = stack.getFluidStack();
            int amount = (int) Math.min(Integer.MAX_VALUE, stack.getStackSize());
            properties[i] = new FluidTankProperties(fluid, amount, false, true);
        }
        return properties;
    }

    @Override
    public int fill(FluidStack resource, boolean doFill) {
        return 0;
    }

    @Override
    public FluidStack drain(FluidStack resource, boolean doDrain) {
        if (resource == null || resource.amount <= 0) {
            return null;
        }

        this.refresh();
        for (IAEFluidStack available : this.cache) {
            FluidStack availableFluid = available.getFluidStack();
            if (availableFluid != null && availableFluid.isFluidEqual(resource)) {
                return this.extract(available, resource.amount, doDrain);
            }
        }
        return null;
    }

    @Override
    public FluidStack drain(int maxDrain, boolean doDrain) {
        if (maxDrain <= 0) {
            return null;
        }

        this.refresh();
        return this.cache.isEmpty() ? null : this.extract(this.cache.get(0), maxDrain, doDrain);
    }

    private FluidStack extract(IAEFluidStack available, int amount, boolean doDrain) {
        if (this.monitor == null) {
            return null;
        }

        IAEFluidStack request = available.copy().setStackSize(amount);
        try {
            IAEFluidStack extracted = Platform.poweredExtraction(
                    this.proxy.getEnergy(),
                    this.monitor,
                    request,
                    this.source,
                    doDrain ? Actionable.MODULATE : Actionable.SIMULATE);
            return extracted == null ? null : extracted.getFluidStack();
        } catch (GridAccessException ignored) {
            return null;
        }
    }
}
