package com.ae2utilix.integration;

import appeng.api.AEApi;
import appeng.api.config.AccessRestriction;
import appeng.api.config.Actionable;
import appeng.api.networking.IGridNode;
import appeng.api.networking.energy.IEnergySource;
import appeng.api.networking.IGrid;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.storage.IMEInventory;
import appeng.api.storage.IMEInventoryHandler;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.IStorageChannel;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IItemList;
import appeng.me.GridAccessException;
import appeng.me.helpers.MEMonitorHandler;
import appeng.me.helpers.MachineSource;
import appeng.util.Platform;
import com.ae2utilix.block.TileCommonInterfaceAlternate;
import com.ae2utilix.item.ItemFluidMark;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fml.common.Loader;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectContainer;
import thaumcraft.api.aspects.IEssentiaContainerItem;
import thaumicenergistics.api.EssentiaStack;
import thaumicenergistics.api.ThEApi;
import thaumicenergistics.api.storage.IAEEssentiaStack;
import thaumicenergistics.api.storage.IEssentiaStorageChannel;
import thaumicenergistics.integration.appeng.AEEssentiaStack;
import thaumicenergistics.item.ItemDummyAspect;
import thaumicenergistics.util.AEUtil;

import javax.annotation.Nullable;
import java.util.Map;
import java.lang.reflect.Proxy;
import java.util.WeakHashMap;

/** Direct Thaumic Energistics implementation, only loaded when both mods exist. */
public final class ThaumicEnergisticsOptional {
    private static final Map<TileCommonInterfaceAlternate, IMEMonitor<IAEEssentiaStack>> MONITORS =
            new WeakHashMap<>();
    /**
     * The network monitor can contain the virtual handler belonging to the
     * same common interface.  Source identity is normally enough to avoid a
     * loop, but older AE2 storage caches may invoke a handler without
     * preserving the MachineSource.  Keep the return owner explicit for the
     * duration of an injection as a second, deterministic guard.
     */
    private static final ThreadLocal<TileCommonInterfaceAlternate> ACTIVE_NETWORK_RETURN =
            new ThreadLocal<>();

    private ThaumicEnergisticsOptional() {
    }

    public static boolean isAvailable() {
        return Loader.isModLoaded("thaumcraft") && Loader.isModLoaded("thaumicenergistics");
    }

    public static boolean isEssentiaChannel(IStorageChannel<?> channel) {
        return isAvailable() && channel == AEApi.instance().storage()
                .getStorageChannel(IEssentiaStorageChannel.class);
    }

    @Nullable
    public static String getAspectTagFromMarker(ItemStack stack) {
        if (!isAvailable() || stack == null || stack.isEmpty()) return null;
        if (stack.getItem() instanceof ItemDummyAspect) {
            Aspect aspect = ((ItemDummyAspect) stack.getItem()).getAspect(stack);
            return aspect == null ? null : aspect.getTag();
        }
        return null;
    }

    @Nullable
    public static String getAspectTagFromItem(ItemStack stack) {
        String markerAspect = getAspectTagFromMarker(stack);
        if (markerAspect != null) return markerAspect;
        if (!isAvailable() || stack == null || stack.isEmpty()
                || !(stack.getItem() instanceof IEssentiaContainerItem)) return null;

        // Match Thaumic Energistics' own interaction semantics: only a
        // container holding exactly one aspect can select an essentia type.
        // Multi-aspect containers are ambiguous and remain normal item marks.
        AspectList aspects = ((IEssentiaContainerItem) stack.getItem()).getAspects(stack);
        if (aspects == null || aspects.size() != 1) return null;
        Aspect[] values = aspects.getAspects();
        if (values.length != 1 || values[0] == null || aspects.getAmount(values[0]) <= 0) return null;
        return values[0].getTag();
    }

    @Nullable
    public static String getAspectTagFromIngredient(Object ingredient) {
        if (ingredient instanceof ItemStack) return getAspectTagFromItem((ItemStack) ingredient);
        if (ingredient instanceof EssentiaStack) return ((EssentiaStack) ingredient).getAspectTag();
        if (ingredient instanceof IAEEssentiaStack) {
            thaumcraft.api.aspects.Aspect aspect = ((IAEEssentiaStack) ingredient).getAspect();
            return aspect == null ? null : aspect.getTag();
        }
        if (ingredient instanceof thaumcraft.api.aspects.AspectList) {
            thaumcraft.api.aspects.Aspect[] aspects =
                    ((thaumcraft.api.aspects.AspectList) ingredient).getAspects();
            return aspects.length == 0 ? null : aspects[0].getTag();
        }
        return null;
    }

    @Nullable
    public static String getAspectDisplayName(String tag) {
        Aspect aspect = tag == null ? null : Aspect.getAspect(tag);
        return aspect == null ? tag : aspect.getName();
    }

    public static boolean isAspectTagValid(String tag) {
        return tag != null && !tag.isEmpty() && Aspect.getAspect(tag) != null;
    }

    public static ItemStack createAspectItem(String tag) {
        Aspect aspect = tag == null ? null : Aspect.getAspect(tag);
        if (!isAvailable() || aspect == null) return ItemStack.EMPTY;
        ItemStack stack = ThEApi.instance().items().dummyAspect()
                .maybeStack(1).orElse(ItemStack.EMPTY);
        if (stack.isEmpty() || !(stack.getItem() instanceof ItemDummyAspect)) {
            return ItemStack.EMPTY;
        }
        ((ItemDummyAspect) stack.getItem()).setAspect(stack, aspect);
        stack.setCount(1);
        return stack;
    }

    @Nullable
    public static IMEMonitor<IAEEssentiaStack> getMonitor(TileCommonInterfaceAlternate tile,
                                                           boolean configured) {
        if (!isAvailable() || tile == null) return null;
        if (!configured) {
            IStorageGrid storage = getNetworkStorage(tile);
            try {
                return storage == null ? null : storage.getInventory(getChannel());
            } catch (RuntimeException ignored) {
                return null;
            }
        }
        IMEMonitor<IAEEssentiaStack> monitor = MONITORS.get(tile);
        if (monitor == null) {
            monitor = new MEMonitorHandler<>(new LocalEssentiaHandler(tile));
            MONITORS.put(tile, monitor);
        }
        return monitor;
    }

    public static int insertNetwork(IStorageGrid storage, IEnergySource energy,
                                    IActionSource source, String aspectTag, int amount,
                                    Actionable mode) {
        Aspect aspect = getAspect(aspectTag);
        if (storage == null || aspect == null || amount <= 0) return 0;
        try {
            // Use the channel monitor just like Thaumic Energistics' native
            // essentia interface.  The monitor includes all real network
            // providers and its source-aware local handler rejects routing
            // the stack back into this same common interface.
            IMEMonitor<IAEEssentiaStack> monitor = storage.getInventory(getChannel());
            if (monitor == null) return 0;
            IAEEssentiaStack input = AEUtil.getAEStackFromAspect(aspect, amount);
            IAEEssentiaStack remainder = monitor.injectItems(input, mode, source);
            return Math.max(0, amount - (remainder == null ? 0 : (int) remainder.getStackSize()));
        } catch (RuntimeException ignored) {
            return 0;
        }
    }

    public static int extractNetwork(IStorageGrid storage, IEnergySource energy,
                                     IActionSource source, String aspectTag, int amount,
                                     Actionable mode) {
    Aspect aspect = getAspect(aspectTag);
    if (storage == null || aspect == null || amount <= 0) return 0;
    try {
        IMEMonitor<IAEEssentiaStack> monitor = storage.getInventory(getChannel());
        if (monitor == null) return 0;
        IAEEssentiaStack request = AEUtil.getAEStackFromAspect(aspect, amount);
        if (request == null) return 0;
        IAEEssentiaStack extracted = monitor.extractItems(request, mode, source);
        return extracted == null ? 0
                : Math.min(amount, (int) extracted.getStackSize());
    } catch (RuntimeException ignored) {
        return 0;
    }
}

    public static int insertNetwork(TileCommonInterfaceAlternate tile, String aspectTag,
                                    int amount, Actionable mode) {
        if (tile == null || !tile.getProxy().isActive()) return 0;
        IStorageGrid storage = getNetworkStorage(tile);
        if (storage == null) return 0;
        TileCommonInterfaceAlternate previous = ACTIVE_NETWORK_RETURN.get();
        ACTIVE_NETWORK_RETURN.set(tile);
        try {
            return insertNetwork(storage, tile.getProxy().getEnergy(),
            new MachineSource(tile), aspectTag, amount, mode);
        } catch (GridAccessException e) {
            return 0;
        } finally {
            if (previous == null) ACTIVE_NETWORK_RETURN.remove();
            else ACTIVE_NETWORK_RETURN.set(previous);
        }
    }

    public static int extractNetwork(TileCommonInterfaceAlternate tile, String aspectTag,
                                     int amount, Actionable mode) {
        if (tile == null || !tile.getProxy().isActive()) return 0;
        IStorageGrid storage = getNetworkStorage(tile);
        if (storage == null) return 0;
        try {
            return extractNetwork(storage, tile.getProxy().getEnergy(),
            new MachineSource(tile), aspectTag, amount, mode);
        } catch (GridAccessException e) {
            return 0;
        }
    }

    public static int receiveLocal(TileCommonInterfaceAlternate tile, String aspectTag,
                                   int amount, boolean simulate) {
        if (tile == null || amount <= 0 || getAspect(aspectTag) == null) return 0;
        return localInsert(tile, aspectTag, amount, simulate ? Actionable.SIMULATE : Actionable.MODULATE);
    }

    public static int extractLocal(TileCommonInterfaceAlternate tile, String aspectTag,
                                   int amount, boolean simulate) {
        if (tile == null || amount <= 0 || getAspect(aspectTag) == null) return 0;
        return localExtract(tile, aspectTag, amount, simulate ? Actionable.SIMULATE : Actionable.MODULATE);
    }

    @Nullable
    public static String findExtractableAspect(TileEntity target, @Nullable String filter) {
        if (!(target instanceof IAspectContainer)) return null;
        AspectList list = ((IAspectContainer) target).getAspects();
        if (list == null) return null;
        if (filter != null) {
            Aspect aspect = getAspect(filter);
            return aspect != null && list.getAmount(aspect) > 0 ? filter : null;
        }
        for (Aspect aspect : list.getAspects()) {
            if (aspect != null && list.getAmount(aspect) > 0) return aspect.getTag();
        }
        return null;
    }

    public static int extractFromTarget(TileEntity target, String aspectTag, int amount) {
        Aspect aspect = getAspect(aspectTag);
        if (!(target instanceof IAspectContainer) || aspect == null || amount <= 0) return 0;
        IAspectContainer container = (IAspectContainer) target;
        int available = Math.min(amount, container.containerContains(aspect));
        if (available <= 0 || !container.takeFromContainer(aspect, available)) return 0;
        return available;
    }

    @Nullable
    public static String findLocalExtractableAspect(TileCommonInterfaceAlternate tile,
                                                    @Nullable String filter) {
        if (tile == null) return null;
        for (boolean extended : new boolean[]{false, true}) {
            for (int slot = 0; slot < 9; slot++) {
                String tag = tile.getStoredEssentiaAspect(extended, slot);
                if (tag == null || tile.getStoredEssentiaAmount(extended, slot) <= 0) continue;
                if (filter == null || filter.equals(tag)) return tag;
            }
        }
        return null;
    }

    public static int insertIntoTarget(TileEntity target, String aspectTag, int amount) {
        Aspect aspect = getAspect(aspectTag);
        if (!(target instanceof IAspectContainer) || aspect == null || amount <= 0) return 0;
        IAspectContainer container = (IAspectContainer) target;
        if (!container.doesContainerAccept(aspect)) return 0;
        int notAdded = container.addToContainer(aspect, amount);
        return Math.max(0, amount - notAdded);
    }

    public static void requestMarkedEssentia(TileCommonInterfaceAlternate tile, boolean extended) {
        if (!isAvailable() || tile == null || tile.getWorld() == null || tile.getWorld().isRemote
                || !tile.getProxy().isActive()) return;
        net.minecraftforge.items.IItemHandler config = extended ? tile.getExtendedConfig() : tile.getConfig();
        for (int slot = 0; slot < 9; slot++) {
            // The persisted config arrays are authoritative. The native dummy-aspect
            // item is only a visual/interaction representation and may not be
            // resolvable on every client/server path.
            String aspectTag = tile.getEssentiaConfigAspect(extended, slot);
            // A marker is required for a request. Never fall back to a stale
            // stored aspect or an arbitrary item in an empty/unconfigured
            // slot: clearing the marker must stop network extraction.
            if (aspectTag == null || aspectTag.isEmpty()
                    || !tile.canStoreEssentiaInSlot(extended, slot)) continue;

            int target = Math.min(tile.getVirtualStorageCapacity(),
                    Math.max(0, tile.getEssentiaConfigAmount(extended, slot)));
            String storedTag = tile.getStoredEssentiaAspect(extended, slot);
            int stored = Math.max(0, tile.getStoredEssentiaAmount(extended, slot));

            if (storedTag != null && !aspectTag.equals(storedTag)) {
                int returned = insertNetwork(tile, storedTag, stored, Actionable.MODULATE);
                stored = Math.max(0, stored - returned);
                tile.setStoredEssentia(extended, slot, storedTag, stored);
                if (stored > 0) continue;
                storedTag = null;
            }

            if (stored > target) {
                int returned = insertNetwork(tile, storedTag == null ? aspectTag : storedTag,
                        stored - target, Actionable.MODULATE);
                if (returned > 0) {
                    stored -= returned;
                    tile.setStoredEssentia(extended, slot,
                            storedTag == null ? aspectTag : storedTag, stored);
                }
            }

            int needed = target - stored;
            if (needed <= 0) continue;

            // extractNetwork performs the same SIMULATE -> MODULATE sequence
            // as the native export bus and only commits the amount returned by
            // the simulated network extraction.
            int moved = extractNetwork(tile, aspectTag, needed, Actionable.MODULATE);
            if (moved > 0) {
                tile.setStoredEssentia(extended, slot, aspectTag,
                        Math.min(tile.getVirtualStorageCapacity(), stored + moved));
            }
        }
    }

    public static void flushUnconfiguredEssentiaToNetwork(TileCommonInterfaceAlternate tile) {
        if (!isAvailable() || tile == null || !tile.getProxy().isActive()) return;
        for (boolean extended : new boolean[]{false, true}) {
            net.minecraftforge.items.IItemHandler config = extended ? tile.getExtendedConfig() : tile.getConfig();
            for (int slot = 0; slot < 9; slot++) {
                if (tile.getEssentiaConfigAspect(extended, slot) != null) continue;
                String tag = tile.getStoredEssentiaAspect(extended, slot);
                int amount = tile.getStoredEssentiaAmount(extended, slot);
                if (tag == null || amount <= 0) continue;
                int moved = insertNetwork(tile, tag, amount, Actionable.MODULATE);
                if (moved > 0) tile.setStoredEssentia(extended, slot, tag, amount - moved);
            }
        }
    }

    public static boolean hasEssentiaWork(TileCommonInterfaceAlternate tile) {
    if (!isAvailable() || tile == null) return false;
    for (boolean extended : new boolean[]{false, true}) {
        net.minecraftforge.items.IItemHandler config = extended
                ? tile.getExtendedConfig() : tile.getConfig();
        for (int slot = 0; slot < 9; slot++) {
            if (tile.getEssentiaConfigAspect(extended, slot) != null
                    && tile.getStoredEssentiaAmount(extended, slot)
                    != tile.getEssentiaConfigAmount(extended, slot)) return true;
            if (config.getStackInSlot(slot).isEmpty()
                    && tile.getStoredEssentiaAmount(extended, slot) > 0) return true;
        }
    }
    return false;
}

    private static IStorageChannel<IAEEssentiaStack> getChannel() {
        return AEApi.instance().storage().getStorageChannel(IEssentiaStorageChannel.class);
    }

    @Nullable
    private static IStorageGrid getNetworkStorage(TileCommonInterfaceAlternate tile) {
    if (tile == null) return null;
    try {
        IStorageGrid storage = tile.getProxy().getStorage();
        if (storage != null) return storage;
    } catch (GridAccessException ignored) {
        // The proxy may be between grid transitions; try the live node cache.
    }
    try {
        IGridNode node = tile.getProxy().getNode();
        IGrid grid = node == null ? null : node.getGrid();
        if (grid != null) {
            IStorageGrid storage = grid.getCache(IStorageGrid.class);
            if (storage != null) return storage;
        }
    } catch (RuntimeException ignored) {
        // Fall through to the final proxy retry.
    }
    try {
        return tile.getProxy().getStorage();
    } catch (GridAccessException ignored) {
        return null;
    }
}

    @Nullable
    private static IMEInventory<IAEEssentiaStack> getInventory(IStorageGrid storage) {
        if (storage == null) return null;
        try {
            return storage.getInventory(getChannel());
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    @Nullable
    private static IMEMonitor<IAEEssentiaStack> getNetworkMonitor(IStorageGrid storage) {
        if (storage == null) return null;
        try {
            return storage.getInventory(getChannel());
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    @Nullable
    private static Aspect getAspect(String tag) {
        return tag == null ? null : Aspect.getAspect(tag);
    }

    @Nullable
    private static IAEEssentiaStack findAvailableEssentia(
            IMEInventory<IAEEssentiaStack> inventory, Aspect aspect, int amount) {
        IItemList<IAEEssentiaStack> available =
                inventory.getAvailableItems(getChannel().createList());
        for (IAEEssentiaStack candidate : available) {
            if (candidate == null || candidate.getAspect() == null
                    || !aspect.getTag().equals(candidate.getAspect().getTag())
                    || candidate.getStackSize() <= 0) continue;
            IAEEssentiaStack result = candidate.copy();
            result.setStackSize(Math.min((long) amount, result.getStackSize()));
            return result;
        }
        return null;
    }

    private static int localInsert(TileCommonInterfaceAlternate tile, String tag, int amount,
                                   Actionable mode) {
    int remaining = amount;
    for (int pass = 0; pass < 2 && remaining > 0; pass++) {
        for (boolean extended : new boolean[]{false, true}) {
            for (int slot = 0; slot < 9 && remaining > 0; slot++) {
                if (!tile.canStoreEssentiaInSlot(extended, slot)) continue;
                String configured = tile.getEssentiaConfigAspect(extended, slot);
                String storedTag = tile.getStoredEssentiaAspect(extended, slot);
                int stored = tile.getStoredEssentiaAmount(extended, slot);
                boolean match = tag.equals(storedTag);
                boolean empty = storedTag == null || stored <= 0;
                if (pass == 0 ? !match : !empty) continue;
                if (configured != null && !configured.equals(tag)) continue;
                int accepted = Math.min(remaining, tile.getVirtualStorageCapacity() - stored);
                if (accepted <= 0) continue;
                if (mode == Actionable.MODULATE) {
                    tile.setStoredEssentia(extended, slot, tag, stored + accepted);
                }
                remaining -= accepted;
            }
        }
    }
    return amount - remaining;
}

    private static int localExtract(TileCommonInterfaceAlternate tile, String tag, int amount,
                                    Actionable mode) {
        int remaining = amount;
        int extracted = 0;
        for (boolean extended : new boolean[]{false, true}) {
            for (int slot = 0; slot < 9 && remaining > 0; slot++) {
                if (!tag.equals(tile.getStoredEssentiaAspect(extended, slot))) continue;
                int stored = tile.getStoredEssentiaAmount(extended, slot);
                int taken = Math.min(remaining, stored);
                if (mode == Actionable.MODULATE) tile.setStoredEssentia(extended, slot, tag, stored - taken);
                extracted += taken;
                remaining -= taken;
            }
        }
        return extracted;
    }

    private static final class LocalEssentiaHandler implements IMEInventoryHandler<IAEEssentiaStack> {
        private final TileCommonInterfaceAlternate tile;

        private LocalEssentiaHandler(TileCommonInterfaceAlternate tile) {
            this.tile = tile;
        }

        private boolean isSelfSource(IActionSource source) {
            if (ACTIVE_NETWORK_RETURN.get() == this.tile) return true;
            return source != null && source.machine().isPresent()
                    && source.machine().get() == this.tile;
        }

        @Override
        public IAEEssentiaStack injectItems(IAEEssentiaStack input, Actionable mode, IActionSource src) {
            // The interface's network return path shares the network storage
            // collection with this local handler. Reject its own source here
            // so AE2 continues to an external drive/storage provider instead
            // of reinserting the resource into the same virtual slot.
            if (this.isSelfSource(src)) return input;
            if (input == null || input.getAspect() == null) return input;
            int accepted = localInsert(tile, input.getAspect().getTag(),
                    (int) Math.min(Integer.MAX_VALUE, input.getStackSize()), mode);
            return accepted >= input.getStackSize() ? null : input.copy().setStackSize(input.getStackSize() - accepted);
        }

        @Override
        public IAEEssentiaStack extractItems(IAEEssentiaStack request, Actionable mode, IActionSource src) {
            // Likewise, a request made by this interface must not extract
            // from its own local buffer and then appear to come from the
            // network.
            if (this.isSelfSource(src)) return null;
            if (request == null || request.getAspect() == null) return null;
            int amount = localExtract(tile, request.getAspect().getTag(),
                    (int) Math.min(Integer.MAX_VALUE, request.getStackSize()), mode);
            return amount <= 0 ? null : AEEssentiaStack.fromEssentiaStack(
                    new EssentiaStack(request.getAspect(), amount));
        }

        @Override public IItemList<IAEEssentiaStack> getAvailableItems(IItemList<IAEEssentiaStack> out) {
            for (boolean extended : new boolean[]{false, true}) for (int slot = 0; slot < 9; slot++) {
                String tag = tile.getStoredEssentiaAspect(extended, slot);
                int amount = tile.getStoredEssentiaAmount(extended, slot);
                Aspect aspect = getAspect(tag);
                if (aspect != null && amount > 0) out.add(AEEssentiaStack.fromEssentiaStack(new EssentiaStack(aspect, amount)));
            }
            return out;
        }
        @Override public AccessRestriction getAccess() { return AccessRestriction.READ_WRITE; }
        @Override public IStorageChannel<IAEEssentiaStack> getChannel() {
            return ThaumicEnergisticsOptional.getChannel();
        }
        @Override public boolean isPrioritized(IAEEssentiaStack input) { return false; }
        @Override public boolean canAccept(IAEEssentiaStack input) { return input != null && input.getAspect() != null; }
        @Override public int getPriority() { return 0; }
        @Override public int getSlot() { return 0; }
        @Override public boolean validForPass(int pass) { return true; }
    }
}
