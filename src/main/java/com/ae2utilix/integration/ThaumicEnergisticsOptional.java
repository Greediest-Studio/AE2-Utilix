package com.ae2utilix.integration;

import appeng.api.AEApi;
import appeng.api.config.AccessRestriction;
import appeng.api.config.Actionable;
import appeng.api.networking.energy.IEnergySource;
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
import thaumicenergistics.api.EssentiaStack;
import thaumicenergistics.api.ThEApi;
import thaumicenergistics.api.storage.IAEEssentiaStack;
import thaumicenergistics.api.storage.IEssentiaStorageChannel;
import thaumicenergistics.integration.appeng.AEEssentiaStack;
import thaumicenergistics.item.ItemDummyAspect;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.WeakHashMap;

/** Direct Thaumic Energistics implementation, only loaded when both mods exist. */
public final class ThaumicEnergisticsOptional {
    private static final Map<TileCommonInterfaceAlternate, IMEMonitor<IAEEssentiaStack>> MONITORS =
            new WeakHashMap<>();

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
    public static String getAspectTagFromItem(ItemStack stack) {
        if (!isAvailable() || stack == null || stack.isEmpty()) return null;
        if (stack.getItem() instanceof ItemDummyAspect) {
            Aspect aspect = ((ItemDummyAspect) stack.getItem()).getAspect(stack);
            return aspect == null ? null : aspect.getTag();
        }
        return null;
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
            try {
                return tile.getProxy().getStorage().getInventory(getChannel());
            } catch (GridAccessException ignored) {
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
        IMEInventory<IAEEssentiaStack> inventory = getInventory(storage);
        Aspect aspect = getAspect(aspectTag);
        if (inventory == null || aspect == null || amount <= 0) return 0;
        try {
            IAEEssentiaStack input = AEEssentiaStack.fromEssentiaStack(new EssentiaStack(aspect, amount));
            IAEEssentiaStack remainder = Platform.poweredInsert(energy, inventory, input, source, mode);
            return Math.max(0, amount - (remainder == null ? 0 : (int) remainder.getStackSize()));
        } catch (RuntimeException ignored) {
            return 0;
        }
    }

    public static int extractNetwork(IStorageGrid storage, IEnergySource energy,
                                     IActionSource source, String aspectTag, int amount,
                                     Actionable mode) {
        IMEInventory<IAEEssentiaStack> inventory = getInventory(storage);
        Aspect aspect = getAspect(aspectTag);
        if (inventory == null || aspect == null || amount <= 0) return 0;
        try {
            IAEEssentiaStack extracted = Platform.poweredExtraction(energy, inventory,
                    AEEssentiaStack.fromEssentiaStack(new EssentiaStack(aspect, amount)), source, mode);
            return extracted == null ? 0 : Math.min(amount, (int) extracted.getStackSize());
        } catch (RuntimeException ignored) {
            return 0;
        }
    }

    public static int insertNetwork(TileCommonInterfaceAlternate tile, String aspectTag,
                                    int amount, Actionable mode) {
        if (tile == null || !tile.getProxy().isActive()) return 0;
        try {
            return insertNetwork(tile.getProxy().getStorage(), tile.getProxy().getEnergy(),
                    new MachineSource(tile), aspectTag, amount, mode);
        } catch (GridAccessException ignored) {
            return 0;
        }
    }

    public static int extractNetwork(TileCommonInterfaceAlternate tile, String aspectTag,
                                     int amount, Actionable mode) {
        if (tile == null || !tile.getProxy().isActive()) return 0;
        try {
            return extractNetwork(tile.getProxy().getStorage(), tile.getProxy().getEnergy(),
                    new MachineSource(tile), aspectTag, amount, mode);
        } catch (GridAccessException ignored) {
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
            ItemStack marker = config.getStackInSlot(slot);
            String aspectTag = ItemFluidMark.getAspectTag(marker);
            if (aspectTag == null || !tile.canStoreEssentiaInSlot(extended, slot)) continue;
            int target = tile.getEssentiaConfigAmount(extended, slot);
            String storedTag = tile.getStoredEssentiaAspect(extended, slot);
            int stored = tile.getStoredEssentiaAmount(extended, slot);
            if (storedTag != null && !aspectTag.equals(storedTag)) {
                int moved = insertNetwork(tile, storedTag, stored, Actionable.MODULATE);
                if (moved < stored) continue;
                tile.setStoredEssentia(extended, slot, null, 0);
                storedTag = null;
                stored = 0;
            }
            if (stored > target) {
                int moved = insertNetwork(tile, aspectTag, stored - target, Actionable.MODULATE);
                if (moved > 0) {
                    stored -= moved;
                    tile.setStoredEssentia(extended, slot, aspectTag, stored);
                }
            }
            if (stored < target) {
                int moved = extractNetwork(tile, aspectTag, target - stored, Actionable.MODULATE);
                if (moved > 0) tile.setStoredEssentia(extended, slot, aspectTag, stored + moved);
            }
        }
    }

    public static void flushUnconfiguredEssentiaToNetwork(TileCommonInterfaceAlternate tile) {
        if (!isAvailable() || tile == null || !tile.getProxy().isActive()) return;
        for (boolean extended : new boolean[]{false, true}) {
            net.minecraftforge.items.IItemHandler config = extended ? tile.getExtendedConfig() : tile.getConfig();
            for (int slot = 0; slot < 9; slot++) {
                if (!config.getStackInSlot(slot).isEmpty()) continue;
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
            for (int slot = 0; slot < 9; slot++) {
                if (ItemFluidMark.getAspectTag((extended ? tile.getExtendedConfig() : tile.getConfig())
                        .getStackInSlot(slot)) != null
                        && tile.getStoredEssentiaAmount(extended, slot)
                        != tile.getEssentiaConfigAmount(extended, slot)) return true;
                if ((extended ? tile.getExtendedConfig() : tile.getConfig()).getStackInSlot(slot).isEmpty()
                        && tile.getStoredEssentiaAmount(extended, slot) > 0) return true;
            }
        }
        return false;
    }

    private static IStorageChannel<IAEEssentiaStack> getChannel() {
        return AEApi.instance().storage().getStorageChannel(IEssentiaStorageChannel.class);
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
    private static Aspect getAspect(String tag) {
        return tag == null ? null : Aspect.getAspect(tag);
    }

    private static int localInsert(TileCommonInterfaceAlternate tile, String tag, int amount,
                                   Actionable mode) {
        int remaining = amount;
        for (int pass = 0; pass < 2 && remaining > 0; pass++) {
            for (boolean extended : new boolean[]{false, true}) {
                for (int slot = 0; slot < 9 && remaining > 0; slot++) {
                    if (!tile.canStoreEssentiaInSlot(extended, slot)) continue;
                    String configured = ItemFluidMark.getAspectTag(
                            (extended ? tile.getExtendedConfig() : tile.getConfig()).getStackInSlot(slot));
                    String storedTag = tile.getStoredEssentiaAspect(extended, slot);
                    int stored = tile.getStoredEssentiaAmount(extended, slot);
                    boolean match = tag.equals(storedTag);
                    boolean empty = storedTag == null || stored <= 0;
                    if (pass == 0 ? !match : !empty) continue;
                    if (configured != null && !configured.equals(tag)) continue;
                    int accepted = Math.min(remaining, tile.getVirtualStorageCapacity() - stored);
                    if (accepted <= 0) continue;
                    if (mode == Actionable.MODULATE) tile.setStoredEssentia(extended, slot, tag, stored + accepted);
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

        @Override
        public IAEEssentiaStack injectItems(IAEEssentiaStack input, Actionable mode, IActionSource src) {
            if (input == null || input.getAspect() == null) return input;
            int accepted = localInsert(tile, input.getAspect().getTag(),
                    (int) Math.min(Integer.MAX_VALUE, input.getStackSize()), mode);
            return accepted >= input.getStackSize() ? null : input.copy().setStackSize(input.getStackSize() - accepted);
        }

        @Override
        public IAEEssentiaStack extractItems(IAEEssentiaStack request, Actionable mode, IActionSource src) {
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
