package com.ae2utilix.integration;

import appeng.api.AEApi;
import appeng.api.config.AccessRestriction;
import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.storage.IMEInventoryHandler;
import appeng.api.storage.IMEInventory;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.IStorageChannel;
import appeng.api.storage.data.IAEStack;
import appeng.me.GridAccessException;
import appeng.me.helpers.MEMonitorHandler;
import appeng.me.helpers.MachineSource;
import appeng.util.Platform;
import com.ae2utilix.block.TileCommonInterfaceAlternate;
import com.ae2utilix.item.ItemFluidMark;
import com.flux_applied.ae2.FluxStack;
import com.flux_applied.ae2.FluxStorageChannel;
import com.flux_applied.item.ItemFluxPacket;
import net.minecraft.item.ItemStack;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fml.common.Loader;
import nyonio.ae2.ManaStack;
import nyonio.ae2.ManaStorageChannel;
import nyonio.item.ItemManaPacket;
import vazkii.botania.api.mana.IManaItem;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.WeakHashMap;

/** Bridges the common interface to the independent Botania and Flux AE2 channels. */
public final class BotaniaFluxIntegration {
    public static final int MANA = 1;
    public static final int FE = 2;

    private static final Map<TileCommonInterfaceAlternate, IMEMonitor<ManaStack>> MANA_MONITORS = new WeakHashMap<>();
    private static final Map<TileCommonInterfaceAlternate, IMEMonitor<FluxStack>> FE_MONITORS = new WeakHashMap<>();
    private static final Map<TileCommonInterfaceAlternate, IEnergyStorage> ENERGY_HANDLERS = new WeakHashMap<>();

    private BotaniaFluxIntegration() {
    }

    public static boolean isManaIntegrationAvailable() {
        return Loader.isModLoaded("botania") && Loader.isModLoaded("botania_applie");
    }

    public static boolean isFeIntegrationAvailable() {
        return Loader.isModLoaded("flux_applied");
    }

    public static boolean isVirtualPacket(ItemStack stack) {
        return (isManaIntegrationAvailable() && ItemManaPacket.isManaPacket(stack))
                || (isFeIntegrationAvailable() && ItemFluxPacket.isFluxPacket(stack));
    }

    public static int getMarkedType(ItemStack stack) {
        if (isManaIntegrationAvailable()
                && (ItemManaPacket.isManaPacket(stack)
                || (!stack.isEmpty() && stack.getItem() instanceof IManaItem))) return MANA;
        if (isFeIntegrationAvailable()
                && (ItemFluxPacket.isFluxPacket(stack)
                || (!stack.isEmpty() && stack.hasCapability(CapabilityEnergy.ENERGY, null)))) return FE;
        return 0;
    }

    public static boolean isManaMark(ItemStack stack) {
        return ItemFluidMark.isManaMark(stack);
    }

    public static boolean isFeMark(ItemStack stack) {
        return ItemFluidMark.isFeMark(stack);
    }

    public static boolean isManaChannel(IStorageChannel<?> channel) {
        return isManaIntegrationAvailable()
                && channel == AEApi.instance().storage().getStorageChannel(ManaStorageChannel.class);
    }

    public static boolean isFeChannel(IStorageChannel<?> channel) {
        return isFeIntegrationAvailable()
                && channel == AEApi.instance().storage().getStorageChannel(FluxStorageChannel.class);
    }

    public static boolean hasManaConfig(TileCommonInterfaceAlternate tile) {
        return isManaIntegrationAvailable() && hasConfig(tile, MANA);
    }

    public static boolean hasFeConfig(TileCommonInterfaceAlternate tile) {
        return isFeIntegrationAvailable() && hasConfig(tile, FE);
    }

    private static boolean hasConfig(TileCommonInterfaceAlternate tile, int type) {
        return hasConfig(tile.getConfig(), type) || hasConfig(tile.getExtendedConfig(), type);
    }

    private static boolean hasConfig(net.minecraftforge.items.IItemHandler config, int type) {
        for (int i = 0; i < config.getSlots(); i++) {
            if (getConfiguredType(config.getStackInSlot(i)) == type) return true;
        }
        return false;
    }

    private static int getConfiguredType(ItemStack stack) {
        if (ItemFluidMark.isManaMark(stack)) return MANA;
        if (ItemFluidMark.isFeMark(stack)) return FE;
        return 0;
    }

    @Nullable
    public static String getDisplayName(int type) {
        if (type == MANA) return "Mana";
        if (type == FE) return "FE";
        return null;
    }

    public static String getStoredTooltip(int type, long amount) {
        String name = type == MANA ? "Mana" : "FE";
        return "\u00a78\u5b58\u50a8\u4e86 \u00a77" + name + " " + amount;
    }

    @Nullable
    public static IMEMonitor<ManaStack> getManaMonitor(TileCommonInterfaceAlternate tile) {
        if (!isManaIntegrationAvailable()) return null;
        if (!hasManaConfig(tile)) return getNetworkMonitor(tile, MANA);
        IMEMonitor<ManaStack> monitor = MANA_MONITORS.get(tile);
        if (monitor == null) {
            monitor = new MEMonitorHandler<>(new LocalStackHandler<ManaStack>(tile, MANA));
            MANA_MONITORS.put(tile, monitor);
        }
        return monitor;
    }

    @Nullable
    public static IMEMonitor<FluxStack> getFeMonitor(TileCommonInterfaceAlternate tile) {
        if (!isFeIntegrationAvailable()) return null;
        if (!hasFeConfig(tile)) return getNetworkMonitor(tile, FE);
        IMEMonitor<FluxStack> monitor = FE_MONITORS.get(tile);
        if (monitor == null) {
            monitor = new MEMonitorHandler<>(new LocalStackHandler<FluxStack>(tile, FE));
            FE_MONITORS.put(tile, monitor);
        }
        return monitor;
    }

    @SuppressWarnings("unchecked")
    private static <T extends IAEStack<T>> IMEMonitor<T> getNetworkMonitor(
            TileCommonInterfaceAlternate tile, int type) {
        try {
            if (!tile.getProxy().isActive()) return null;
            IStorageChannel<T> channel = (IStorageChannel<T>) (type == MANA
                    ? ManaStorageChannel.INSTANCE : FluxStorageChannel.INSTANCE);
            return tile.getProxy().getStorage().getInventory(channel);
        } catch (GridAccessException ignored) {
            return null;
        }
    }

    public static void requestMarked(TileCommonInterfaceAlternate tile, int type) {
        if ((type == MANA && !isManaIntegrationAvailable())
                || (type == FE && !isFeIntegrationAvailable())) return;
        if (tile.getWorld() == null || tile.getWorld().isRemote || !tile.getProxy().isActive()) return;
        for (boolean extended : new boolean[]{false, true}) {
            net.minecraftforge.items.IItemHandler config = extended ? tile.getExtendedConfig() : tile.getConfig();
            for (int slot = 0; slot < 9; slot++) {
                if (getConfiguredType(config.getStackInSlot(slot)) != type) continue;
                if (type == MANA
                        ? !tile.canStoreManaInSlot(extended, slot)
                        : !tile.canStoreFeInSlot(extended, slot)) continue;
                long stored = getStored(tile, extended, slot, type);
                int target = getConfiguredAmount(tile, extended, slot, type);
                if (stored > target) {
                    long excess = stored - target;
                    long inserted = insertNetwork(tile, type, excess, Actionable.MODULATE);
                    if (inserted > 0) {
                        setStored(tile, extended, slot, type, stored - inserted);
                        stored -= inserted;
                    }
                }
                if (stored >= target) continue;
                long extracted = extractNetwork(tile, type, target - stored, Actionable.MODULATE);
                if (extracted > 0) setStored(tile, extended, slot, type, stored + extracted);
            }
        }
    }

    public static void flushUnconfigured(TileCommonInterfaceAlternate tile, int type) {
        if ((type == MANA && !isManaIntegrationAvailable())
                || (type == FE && !isFeIntegrationAvailable())) return;
        if (!tile.getProxy().isActive()) return;
        for (boolean extended : new boolean[]{false, true}) {
            net.minecraftforge.items.IItemHandler config = extended ? tile.getExtendedConfig() : tile.getConfig();
            for (int slot = 0; slot < 9; slot++) {
                if (!config.getStackInSlot(slot).isEmpty()) continue;
                long stored = getStored(tile, extended, slot, type);
                if (stored <= 0) continue;
                long inserted = insertNetwork(tile, type, stored, Actionable.MODULATE);
                if (inserted > 0) setStored(tile, extended, slot, type, stored - inserted);
            }
        }
    }

    public static int getConfiguredAmount(TileCommonInterfaceAlternate tile, boolean extended, int slot, int type) {
        long amount = type == MANA ? tile.getManaConfigAmount(extended, slot)
                : tile.getFeConfigAmount(extended, slot);
        return (int) Math.max(1, Math.min(tile.getVirtualStorageCapacity(), amount));
    }

    public static long getStored(TileCommonInterfaceAlternate tile, boolean extended, int slot, int type) {
        return type == MANA ? tile.getStoredMana(extended, slot) : tile.getStoredFe(extended, slot);
    }

    private static void setStored(TileCommonInterfaceAlternate tile, boolean extended, int slot, int type, long amount) {
        if (type == MANA) tile.setStoredMana(extended, slot, amount);
        else tile.setStoredFe(extended, slot, amount);
    }

    private static long insertLocal(TileCommonInterfaceAlternate tile, int type, long amount,
            Actionable mode, boolean allowUnconfigured) {
        long remaining = Math.max(0, amount);
        for (boolean extended : new boolean[]{false, true}) {
            net.minecraftforge.items.IItemHandler config = extended ? tile.getExtendedConfig() : tile.getConfig();
            for (int pass = 0; pass < 2 && remaining > 0; pass++) {
                for (int slot = 0; slot < 9 && remaining > 0; slot++) {
                    ItemStack marker = config.getStackInSlot(slot);
                    int configuredType = getConfiguredType(marker);
                    if (type == MANA
                            ? !tile.canStoreManaInSlot(extended, slot)
                            : !tile.canStoreFeInSlot(extended, slot)) continue;
                    long stored = getStored(tile, extended, slot, type);
                    boolean candidate = configuredType == type && (pass == 0 ? stored > 0 : stored == 0);
                    if (!candidate && allowUnconfigured && configuredType == 0 && marker.isEmpty()
                            && (pass == 1 || stored > 0)) {
                        candidate = true;
                    }
                    if (!candidate) continue;
                    long accepted = Math.min(remaining, tile.getVirtualStorageCapacity() - stored);
                    if (accepted <= 0) continue;
                    if (mode == Actionable.MODULATE) setStored(tile, extended, slot, type, stored + accepted);
                    remaining -= accepted;
                }
            }
        }
        return amount - remaining;
    }

    private static long extractLocal(TileCommonInterfaceAlternate tile, int type, long amount,
            Actionable mode) {
        long remaining = Math.max(0, amount);
        long extracted = 0;
        for (boolean extended : new boolean[]{false, true}) {
            for (int slot = 0; slot < 9 && remaining > 0; slot++) {
                long stored = getStored(tile, extended, slot, type);
                if (stored <= 0) continue;
                long taken = Math.min(remaining, stored);
                if (mode == Actionable.MODULATE) setStored(tile, extended, slot, type, stored - taken);
                extracted += taken;
                remaining -= taken;
            }
        }
        return extracted;
    }

    public static long insertNetwork(TileCommonInterfaceAlternate tile, int type, long amount, Actionable mode) {
        if (amount <= 0) return 0;
        if ((type == MANA && !isManaIntegrationAvailable())
                || (type == FE && !isFeIntegrationAvailable())) return 0;
        try {
            if (type == MANA) {
                IMEInventory<ManaStack> inventory = getManaNetworkInventory(tile);
                if (inventory == null) return 0;
                ManaStack input = new ManaStack(amount);
                ManaStack remainder = Platform.poweredInsert(tile.getProxy().getEnergy(), inventory, input,
                        new MachineSource(tile), mode);
                return Math.max(0, Math.min(amount, amount - (remainder == null ? 0 : remainder.getStackSize())));
            }
            IMEInventory<FluxStack> inventory = getFeNetworkInventory(tile);
            if (inventory == null) return 0;
            FluxStack input = new FluxStack(amount);
            FluxStack remainder = Platform.poweredInsert(tile.getProxy().getEnergy(), inventory, input,
                    new MachineSource(tile), mode);
            return Math.max(0, Math.min(amount, amount - (remainder == null ? 0 : remainder.getStackSize())));
        } catch (Exception ignored) {
            return 0;
        }
    }

    public static long extractNetwork(TileCommonInterfaceAlternate tile, int type, long amount, Actionable mode) {
        if (amount <= 0) return 0;
        if ((type == MANA && !isManaIntegrationAvailable())
                || (type == FE && !isFeIntegrationAvailable())) return 0;
        try {
            if (type == MANA) {
                IMEInventory<ManaStack> inventory = getManaNetworkInventory(tile);
                if (inventory == null) return 0;
                ManaStack extracted = Platform.poweredExtraction(tile.getProxy().getEnergy(), inventory,
                        new ManaStack(amount), new MachineSource(tile), mode);
                return extracted == null ? 0 : Math.min(amount, extracted.getStackSize());
            }
            IMEInventory<FluxStack> inventory = getFeNetworkInventory(tile);
            if (inventory == null) return 0;
            FluxStack extracted = Platform.poweredExtraction(tile.getProxy().getEnergy(), inventory,
                    new FluxStack(amount), new MachineSource(tile), mode);
            return extracted == null ? 0 : Math.min(amount, extracted.getStackSize());
        } catch (Exception ignored) {
            return 0;
        }
    }

    private static IMEInventory<ManaStack> getManaNetworkInventory(
            TileCommonInterfaceAlternate tile) throws GridAccessException {
        if (!isManaIntegrationAvailable()) return null;
        return tile.getProxy().getStorage().getInventory(ManaStorageChannel.INSTANCE);
    }

    private static IMEInventory<FluxStack> getFeNetworkInventory(
            TileCommonInterfaceAlternate tile) throws GridAccessException {
        if (!isFeIntegrationAvailable()) return null;
        return tile.getProxy().getStorage().getInventory(FluxStorageChannel.INSTANCE);
    }

    public static long getCurrentMana(TileCommonInterfaceAlternate tile) {
        if (!isManaIntegrationAvailable()) return 0;
        return getCurrent(tile, MANA);
    }

    public static long getCurrentFe(TileCommonInterfaceAlternate tile) {
        if (!isFeIntegrationAvailable()) return 0;
        return getCurrent(tile, FE);
    }

    private static long getCurrent(TileCommonInterfaceAlternate tile, int type) {
        if (hasConfig(tile, type)) {
            long total = 0;
            for (boolean extended : new boolean[]{false, true}) {
                for (int slot = 0; slot < 9; slot++) total += getStored(tile, extended, slot, type);
            }
            return total;
        }
        long local = extractLocal(tile, type, Long.MAX_VALUE, Actionable.SIMULATE);
        return local + (tile.getProxy().isActive()
                ? extractNetwork(tile, type, Long.MAX_VALUE, Actionable.SIMULATE) : 0);
    }

    public static long getCapacity(TileCommonInterfaceAlternate tile, int type) {
        if ((type == MANA && !isManaIntegrationAvailable())
                || (type == FE && !isFeIntegrationAvailable())) return 0;
        int count = 0;
        for (boolean extended : new boolean[]{false, true}) {
            net.minecraftforge.items.IItemHandler config = extended ? tile.getExtendedConfig() : tile.getConfig();
            for (int slot = 0; slot < 9; slot++) if (getConfiguredType(config.getStackInSlot(slot)) == type) count++;
        }
        return count == 0 ? 18L * tile.getVirtualStorageCapacity()
                : (long) count * tile.getVirtualStorageCapacity();
    }

    public static int receiveMana(TileCommonInterfaceAlternate tile, int amount, boolean simulate) {
        return transfer(tile, MANA, amount, simulate, true);
    }

    public static int extractMana(TileCommonInterfaceAlternate tile, int amount, boolean simulate) {
        return transfer(tile, MANA, amount, simulate, false);
    }

    public static int receiveManaLocal(TileCommonInterfaceAlternate tile, int amount, boolean simulate) {
        if (!isManaIntegrationAvailable() || tile == null || amount <= 0) return 0;
        return (int) Math.min(amount, insertLocal(tile, MANA, amount,
                simulate ? Actionable.SIMULATE : Actionable.MODULATE, true));
    }

    public static int extractManaLocal(TileCommonInterfaceAlternate tile, int amount, boolean simulate) {
        if (!isManaIntegrationAvailable() || tile == null || amount <= 0) return 0;
        return (int) Math.min(amount, extractLocal(tile, MANA, amount,
                simulate ? Actionable.SIMULATE : Actionable.MODULATE));
    }

    public static int receiveFeLocal(TileCommonInterfaceAlternate tile, int amount, boolean simulate) {
        if (!isFeIntegrationAvailable() || tile == null || amount <= 0) return 0;
        return (int) Math.min(amount, insertLocal(tile, FE, amount,
                simulate ? Actionable.SIMULATE : Actionable.MODULATE, true));
    }

    public static int extractFeLocal(TileCommonInterfaceAlternate tile, int amount, boolean simulate) {
        if (!isFeIntegrationAvailable() || tile == null || amount <= 0) return 0;
        return (int) Math.min(amount, extractLocal(tile, FE, amount,
                simulate ? Actionable.SIMULATE : Actionable.MODULATE));
    }

    private static int transfer(TileCommonInterfaceAlternate tile, int type, int amount,
            boolean simulate, boolean receive) {
        if (amount <= 0) return 0;
        if (receive) {
            long transferred = insertNetwork(tile, type, amount,
                    simulate ? Actionable.SIMULATE : Actionable.MODULATE);
            long local = insertLocal(tile, type, amount - transferred,
                    simulate ? Actionable.SIMULATE : Actionable.MODULATE, true);
            transferred += local;
            return (int) Math.min(amount, transferred);
        }

        long transferred = extractLocal(tile, type, amount,
                simulate ? Actionable.SIMULATE : Actionable.MODULATE);
        if (transferred < amount) {
            transferred += extractNetwork(tile, type, amount - transferred,
                    simulate ? Actionable.SIMULATE : Actionable.MODULATE);
        }
        return (int) Math.min(amount, transferred);
    }

    public static IEnergyStorage getEnergyHandler(TileCommonInterfaceAlternate tile) {
        IEnergyStorage handler = ENERGY_HANDLERS.get(tile);
        if (handler == null) {
            handler = new InterfaceEnergyStorage(tile);
            ENERGY_HANDLERS.put(tile, handler);
        }
        return handler;
    }

    private static final class InterfaceEnergyStorage implements IEnergyStorage {
        private final TileCommonInterfaceAlternate tile;

        private InterfaceEnergyStorage(TileCommonInterfaceAlternate tile) {
            this.tile = tile;
        }

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            if (maxReceive <= 0) return 0;
            long network = insertNetwork(tile, FE, maxReceive, simulate ? Actionable.SIMULATE : Actionable.MODULATE);
            long local = insertLocal(tile, FE, maxReceive - network,
                    simulate ? Actionable.SIMULATE : Actionable.MODULATE, true);
            return (int) Math.min(maxReceive, network + local);
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            if (maxExtract <= 0) return 0;
            long local = extractLocal(tile, FE, maxExtract,
                    simulate ? Actionable.SIMULATE : Actionable.MODULATE);
            long network = extractNetwork(tile, FE, maxExtract - local,
                    simulate ? Actionable.SIMULATE : Actionable.MODULATE);
            return (int) Math.min(maxExtract, local + network);
        }

        @Override
        public int getEnergyStored() {
            return (int) Math.min(Integer.MAX_VALUE, getCurrentFe(tile));
        }

        @Override
        public int getMaxEnergyStored() {
            return (int) Math.min(Integer.MAX_VALUE, getCapacity(tile, FE));
        }

        @Override public boolean canExtract() { return true; }
        @Override public boolean canReceive() { return true; }
    }

    private static final class LocalStackHandler<T extends IAEStack<T>> implements IMEInventoryHandler<T> {
        private final TileCommonInterfaceAlternate tile;
        private final int type;

        private LocalStackHandler(TileCommonInterfaceAlternate tile, int type) {
            this.tile = tile;
            this.type = type;
        }

        @SuppressWarnings("unchecked")
        private T create(long amount) {
            return (T) (type == MANA ? new ManaStack(amount) : new FluxStack(amount));
        }

        private boolean accepts(T stack) {
            return (type == MANA && stack instanceof ManaStack) || (type == FE && stack instanceof FluxStack);
        }

        @Override
        public T injectItems(T input, Actionable mode, IActionSource source) {
            if (input == null || !accepts(input) || input.getStackSize() <= 0) return input;
            long remaining = input.getStackSize();
            for (boolean extended : new boolean[]{false, true}) {
                for (int pass = 0; pass < 2 && remaining > 0; pass++) {
                    net.minecraftforge.items.IItemHandler config = extended ? tile.getExtendedConfig() : tile.getConfig();
                    for (int slot = 0; slot < 9 && remaining > 0; slot++) {
                        if (getConfiguredType(config.getStackInSlot(slot)) != type) continue;
                        if (type == MANA
                                ? !tile.canStoreManaInSlot(extended, slot)
                                : !tile.canStoreFeInSlot(extended, slot)) continue;
                        long stored = getStored(tile, extended, slot, type);
                        if ((pass == 0) != (stored > 0)) continue;
                        long accepted = Math.min(remaining, tile.getVirtualStorageCapacity() - stored);
                        if (accepted > 0 && mode == Actionable.MODULATE) setStored(tile, extended, slot, type, stored + accepted);
                        remaining -= accepted;
                    }
                }
            }
            return remaining <= 0 ? null : create(remaining);
        }

        @Override
        public T extractItems(T request, Actionable mode, IActionSource source) {
            if (request == null || !accepts(request) || request.getStackSize() <= 0) return null;
            long remaining = request.getStackSize();
            long extracted = 0;
            for (boolean extended : new boolean[]{false, true}) {
                for (int slot = 0; slot < 9 && remaining > 0; slot++) {
                    long stored = getStored(tile, extended, slot, type);
                    if (stored <= 0) continue;
                    long taken = Math.min(remaining, stored);
                    if (mode == Actionable.MODULATE) setStored(tile, extended, slot, type, stored - taken);
                    extracted += taken;
                    remaining -= taken;
                }
            }
            return extracted <= 0 ? null : create(extracted);
        }

        @Override public appeng.api.storage.data.IItemList<T> getAvailableItems(appeng.api.storage.data.IItemList<T> out) {
            long amount = 0;
            for (boolean extended : new boolean[]{false, true}) for (int slot = 0; slot < 9; slot++) amount += getStored(tile, extended, slot, type);
            if (amount > 0) out.addStorage(create(amount));
            return out;
        }
        @Override public IStorageChannel<T> getChannel() { return (IStorageChannel<T>) (type == MANA ? ManaStorageChannel.INSTANCE : FluxStorageChannel.INSTANCE); }
        @Override public AccessRestriction getAccess() { return AccessRestriction.READ_WRITE; }
        @Override public boolean isPrioritized(T stack) { return true; }
        @Override public boolean canAccept(T stack) { return accepts(stack); }
        @Override public int getPriority() { return 0; }
        @Override public int getSlot() { return -1; }
        @Override public boolean validForPass(int pass) { return true; }
        @Override public boolean isSticky() { return false; }
    }
}
