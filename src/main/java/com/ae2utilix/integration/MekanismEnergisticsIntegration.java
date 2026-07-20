package com.ae2utilix.integration;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.storage.IMEInventory;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.IStorageChannel;
import appeng.me.GridAccessException;
import appeng.me.helpers.MachineSource;
import appeng.util.Platform;
import com.ae2utilix.block.TileCommonInterfaceAlternate;
import com.ae2utilix.item.ItemFluidMark;
import com.mekeng.github.common.ItemAndBlocks;
import com.mekeng.github.common.me.data.IAEGasStack;
import com.mekeng.github.common.me.data.impl.AEGasStack;
import com.mekeng.github.common.me.inventory.IExtendedGasHandler;
import com.mekeng.github.common.me.storage.IGasStorageChannel;
import com.mekeng.github.common.me.storage.impl.MEMonitorIGasHandler;
import mekanism.api.gas.Gas;
import mekanism.api.gas.GasRegistry;
import mekanism.api.gas.GasStack;
import mekanism.api.gas.GasTank;
import mekanism.api.gas.GasTankInfo;
import mekanism.api.gas.IGasHandler;
import mekanism.common.capabilities.Capabilities;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fml.common.Loader;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Optional Mekanism Energistics support for the common interface.
 *
 * The main tile intentionally stores only gas names and amounts. This keeps
 * the tile loadable without Mekanism Energistics; this class is resolved only
 * after the optional mod has been detected.
 */
public final class MekanismEnergisticsIntegration {

    private static final int CAPACITY = 512000;
    private static final Map<TileCommonInterfaceAlternate, InterfaceGasHandler> HANDLERS = new WeakHashMap<>();
    private static final Map<TileCommonInterfaceAlternate, IMEMonitor<IAEGasStack>> MONITORS = new WeakHashMap<>();

    private MekanismEnergisticsIntegration() {
    }

    public static boolean isAvailable() {
        return Loader.isModLoaded("mekeng") && Loader.isModLoaded("mekanism");
    }

    public static boolean isGasChannel(IStorageChannel<?> channel) {
        if (!isAvailable() || channel == null) return false;
        return channel == AEApi.instance().storage().getStorageChannel(IGasStorageChannel.class);
    }

    public static boolean isGasCapability(Capability<?> capability) {
        if (!isAvailable() || capability == null) return false;
        return capability == Capabilities.GAS_HANDLER_CAPABILITY;
    }

    @Nullable
    public static String getGasNameFromItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        String markerGas = ItemFluidMark.getGasName(stack);
        if (markerGas != null) return markerGas;
        if (!isAvailable()) return null;
        GasStack gas = null;
        if (stack.getItem() == ItemAndBlocks.DUMMY_GAS) {
            gas = ItemAndBlocks.DUMMY_GAS.getGasStack(stack);
        } else {
            gas = com.mekeng.github.util.Utils.getGasFromItem(stack);
        }
        return gas == null || gas.getGas() == null ? null : gas.getGas().getName();
    }

    @Nullable
    public static String getGasDisplayName(String gasName) {
        if (!isAvailable() || gasName == null) return null;
        Gas gas = GasRegistry.getGas(gasName);
        return gas == null ? gasName : gas.getLocalizedName();
    }

    @Nullable
    public static IMEMonitor<IAEGasStack> getMonitor(TileCommonInterfaceAlternate tile, boolean configured) {
        if (!isAvailable() || tile == null) return null;
        if (!configured) return getNetworkMonitor(tile);

        IMEMonitor<IAEGasStack> monitor = MONITORS.get(tile);
        if (monitor == null) {
            monitor = new MEMonitorIGasHandler(new InterfaceGasHandler(tile, false), null);
            ((MEMonitorIGasHandler) monitor).setActionSource(new MachineSource(tile));
            MONITORS.put(tile, monitor);
        }
        ((MEMonitorIGasHandler) monitor).onTick();
        return monitor;
    }

    @Nullable
    public static IGasHandler getGasHandler(TileCommonInterfaceAlternate tile) {
        if (!isAvailable() || tile == null) return null;
        InterfaceGasHandler handler = HANDLERS.get(tile);
        if (handler == null) {
            handler = new InterfaceGasHandler(tile, true);
            HANDLERS.put(tile, handler);
        }
        return handler;
    }

    public static void requestMarkedGases(TileCommonInterfaceAlternate tile, boolean extended) {
        if (!isAvailable() || tile == null || tile.getWorld() == null || tile.getWorld().isRemote
                || !tile.getProxy().isActive()) return;

        for (int slot = 0; slot < 9; slot++) {
            ItemStack marker = (extended ? tile.getExtendedConfig() : tile.getConfig()).getStackInSlot(slot);
            String gasName = ItemFluidMark.getGasName(marker);
            if (gasName == null) continue;

            int configuredAmount = tile.getGasConfigAmount(extended, slot);
            if (tile.getGasConfigName(extended, slot) == null) {
                tile.setGasConfig(extended, slot, gasName, configuredAmount);
            }

            String storedName = tile.getStoredGasName(extended, slot);
            int storedAmount = tile.getStoredGasAmount(extended, slot);
            if (storedName != null && !gasName.equals(storedName)) {
                if (!returnStoredGas(tile, extended, slot, storedName, storedAmount)) {
                    continue;
                }
                storedName = null;
                storedAmount = 0;
            }

            int amount = Math.min(CAPACITY, Math.max(1, configuredAmount)) - storedAmount;
            if (amount <= 0) continue;

            IAEGasStack extracted = extractFromNetwork(tile, gasName, amount);
            if (extracted == null || extracted.getStackSize() <= 0) continue;
            GasStack stack = extracted.getGasStack();
            tile.setStoredGas(extended, slot, stack.getGas().getName(),
                    storedAmount + Math.min(CAPACITY - storedAmount, stack.amount));
        }
    }

    public static boolean hasGasWork(TileCommonInterfaceAlternate tile) {
        if (!isAvailable() || tile == null) return false;
        for (boolean extended : new boolean[]{false, true}) {
            for (int slot = 0; slot < 9; slot++) {
                ItemStack marker = (extended ? tile.getExtendedConfig() : tile.getConfig()).getStackInSlot(slot);
                String gasName = ItemFluidMark.getGasName(marker);
                if (gasName == null) continue;
                if (tile.getStoredGasAmount(extended, slot) < tile.getGasConfigAmount(extended, slot)) {
                    return true;
                }
            }
        }
        return hasUnconfiguredGas(tile, false) || hasUnconfiguredGas(tile, true);
    }

    public static void flushUnconfiguredGasesToNetwork(TileCommonInterfaceAlternate tile) {
        if (!isAvailable() || tile == null || !tile.getProxy().isActive()) return;
        flushUnconfiguredGasesToNetwork(tile, false);
        flushUnconfiguredGasesToNetwork(tile, true);
    }

    private static void flushUnconfiguredGasesToNetwork(TileCommonInterfaceAlternate tile, boolean extended) {
        for (int slot = 0; slot < 9; slot++) {
            ItemStack config = (extended ? tile.getExtendedConfig() : tile.getConfig()).getStackInSlot(slot);
            if (!config.isEmpty()) continue;

            String gasName = tile.getStoredGasName(extended, slot);
            int amount = tile.getStoredGasAmount(extended, slot);
            if (gasName == null || amount <= 0 || GasRegistry.getGas(gasName) == null) continue;

            IAEGasStack remainder = insertIntoNetwork(tile, gasName, amount);
            int remaining = remainder == null ? 0 : (int) Math.min(CAPACITY, remainder.getStackSize());
            if (remaining != amount) {
                tile.setStoredGas(extended, slot, gasName, remaining);
            }
        }
    }

    @Nullable
    private static IMEMonitor<IAEGasStack> getNetworkMonitor(TileCommonInterfaceAlternate tile) {
        try {
            IStorageGrid grid = tile.getProxy().getStorage();
            return grid.getInventory(AEApi.instance().storage().getStorageChannel(IGasStorageChannel.class));
        } catch (GridAccessException e) {
            return null;
        }
    }

    @Nullable
    private static IMEInventory<IAEGasStack> getNetworkInventory(TileCommonInterfaceAlternate tile) {
        return getNetworkMonitor(tile);
    }

    @Nullable
    private static IAEGasStack extractFromNetwork(TileCommonInterfaceAlternate tile, String gasName, int amount) {
        Gas gas = GasRegistry.getGas(gasName);
        IMEInventory<IAEGasStack> inventory = getNetworkInventory(tile);
        if (gas == null || inventory == null || amount <= 0) return null;
        try {
            IAEGasStack request = AEGasStack.of(new GasStack(gas, amount));
            return Platform.poweredExtraction(tile.getProxy().getEnergy(), inventory, request,
                    new MachineSource(tile), Actionable.MODULATE);
        } catch (GridAccessException e) {
            return null;
        }
    }

    @Nullable
    private static IAEGasStack insertIntoNetwork(TileCommonInterfaceAlternate tile, String gasName, int amount) {
        Gas gas = GasRegistry.getGas(gasName);
        IMEInventory<IAEGasStack> inventory = getNetworkInventory(tile);
        if (gas == null || inventory == null || amount <= 0) return null;
        try {
            IAEGasStack input = AEGasStack.of(new GasStack(gas, amount));
            return Platform.poweredInsert(tile.getProxy().getEnergy(), inventory, input,
                    new MachineSource(tile), Actionable.MODULATE);
        } catch (GridAccessException e) {
            return inputOrNull(gas, amount);
        }
    }

    private static IAEGasStack inputOrNull(Gas gas, int amount) {
        return AEGasStack.of(new GasStack(gas, amount));
    }

    private static boolean returnStoredGas(TileCommonInterfaceAlternate tile, boolean extended,
                                           int slot, String gasName, int amount) {
        if (gasName == null || GasRegistry.getGas(gasName) == null) return false;
        IAEGasStack remainder = insertIntoNetwork(tile, gasName, amount);
        if (remainder == null || remainder.getStackSize() <= 0) {
            tile.setStoredGas(extended, slot, null, 0);
            return true;
        }
        tile.setStoredGas(extended, slot, gasName,
                (int) Math.min(CAPACITY, remainder.getStackSize()));
        return false;
    }

    private static boolean hasUnconfiguredGas(TileCommonInterfaceAlternate tile, boolean extended) {
        for (int slot = 0; slot < 9; slot++) {
            ItemStack config = (extended ? tile.getExtendedConfig() : tile.getConfig()).getStackInSlot(slot);
            if (config.isEmpty() && tile.getStoredGasAmount(extended, slot) > 0) return true;
        }
        return false;
    }

    private static final class InterfaceGasHandler implements IExtendedGasHandler {
        private final TileCommonInterfaceAlternate tile;
        private final boolean networkFirst;

        private InterfaceGasHandler(TileCommonInterfaceAlternate tile, boolean networkFirst) {
            this.tile = tile;
            this.networkFirst = networkFirst;
        }

        @Override
        public int receiveGas(EnumFacing side, GasStack stack, boolean doTransfer) {
            if (stack == null || stack.getGas() == null || stack.amount <= 0) return 0;
            int accepted = 0;
            if (this.networkFirst && tile.getProxy().isActive()) {
                IAEGasStack remainder = insertIntoNetwork(tile, stack.getGas().getName(), stack.amount,
                        doTransfer ? Actionable.MODULATE : Actionable.SIMULATE);
                accepted = stack.amount - (remainder == null ? 0 : (int) remainder.getStackSize());
            }
            int remaining = stack.amount - accepted;
            if (remaining > 0) {
                accepted += receiveLocal(stack.getGas(), remaining, doTransfer);
            }
            return accepted;
        }

        @Override
        public GasStack drawGas(EnumFacing side, int amount, boolean doTransfer) {
            if (amount <= 0) return null;
            GasStack local = firstLocalGas();
            if (local != null) return drawGas(side, new GasStack(local.getGas(), amount), doTransfer);
            if (this.networkFirst && tile.getProxy().isActive()) {
                IAEGasStack available = firstNetworkGas();
                if (available != null) return drawNetwork(available.getGas(), amount, doTransfer);
            }
            return null;
        }

        @Override
        public GasStack drawGas(EnumFacing side, GasStack requested, boolean doTransfer) {
            if (requested == null || requested.getGas() == null || requested.amount <= 0) return null;
            if (this.networkFirst && tile.getProxy().isActive()) {
                GasStack network = drawNetwork(requested.getGas(), requested.amount, doTransfer);
                if (network != null) return network;
            }
            return drawLocal(requested.getGas(), requested.amount, doTransfer);
        }

        @Override
        public boolean canReceiveGas(EnumFacing side, Gas type) {
            if (type == null) return false;
            if (this.networkFirst && tile.getProxy().isActive()) {
                IAEGasStack remainder = insertIntoNetwork(tile, type.getName(), 1, Actionable.SIMULATE);
                if (remainder == null || remainder.getStackSize() < 1) return true;
            }
            return receiveLocal(type, 1, false) > 0;
        }

        @Override
        public boolean canDrawGas(EnumFacing side, Gas type) {
            if (type == null) return false;
            return (this.networkFirst && tile.getProxy().isActive()
                    && drawNetwork(type, 1, false) != null) || drawLocal(type, 1, false) != null;
        }

        @Nonnull
        @Override
        public GasTankInfo[] getTankInfo() {
            GasTankInfo[] result = new GasTankInfo[18];
            for (int i = 0; i < result.length; i++) {
                boolean extended = i >= 9;
                int slot = i % 9;
                GasTank tank = new GasTank(CAPACITY);
                String name = tile.getStoredGasName(extended, slot);
                int amount = tile.getStoredGasAmount(extended, slot);
                Gas gas = name == null ? null : GasRegistry.getGas(name);
                if (gas != null && amount > 0) tank.setGas(new GasStack(gas, amount));
                result[i] = tank;
            }
            return result;
        }

        private int receiveLocal(Gas gas, int amount, boolean doTransfer) {
            int total = 0;
            int[] order = new int[18];
            for (int i = 0; i < 18; i++) order[i] = i;
            for (int pass = 0; pass < 3 && total < amount; pass++) {
                for (int index : order) {
                    boolean extended = index >= 9;
                    int slot = index % 9;
                    ItemStack config = (extended ? tile.getExtendedConfig() : tile.getConfig()).getStackInSlot(slot);
                    if (!isGasCompatible(config, gas.getName(), pass)) continue;
                    String storedName = tile.getStoredGasName(extended, slot);
                    int storedAmount = tile.getStoredGasAmount(extended, slot);
                    if (pass == 0 && (storedName == null || !gas.getName().equals(storedName))) continue;
                    if (pass > 0 && storedName != null) continue;
                    int accepted = Math.min(amount - total, Math.max(0, CAPACITY - storedAmount));
                    if (accepted <= 0) continue;
                    if (doTransfer) tile.setStoredGas(extended, slot, gas.getName(), storedAmount + accepted);
                    total += accepted;
                    if (total >= amount) return total;
                }
            }
            return total;
        }

        private boolean isGasCompatible(ItemStack config, String gasName, int pass) {
            if (!config.isEmpty() && ItemFluidMark.isFluidMark(config)) return false;
            if (!config.isEmpty() && !ItemFluidMark.isGasMark(config)) return false;
            if (pass == 0) {
                return config.isEmpty() || gasName.equals(ItemFluidMark.getGasName(config));
            }
            if (config.isEmpty()) return pass == 2;
            return pass == 1 && gasName.equals(ItemFluidMark.getGasName(config));
        }

        @Nullable
        private GasStack drawLocal(Gas gas, int amount, boolean doTransfer) {
            int total = 0;
            for (int index = 0; index < 18 && total < amount; index++) {
                boolean extended = index >= 9;
                int slot = index % 9;
                String name = tile.getStoredGasName(extended, slot);
                if (!gas.getName().equals(name)) continue;
                int stored = tile.getStoredGasAmount(extended, slot);
                int taken = Math.min(amount - total, stored);
                if (doTransfer) tile.setStoredGas(extended, slot, name, stored - taken);
                total += taken;
            }
            return total <= 0 ? null : new GasStack(gas, total);
        }

        @Nullable
        private GasStack firstLocalGas() {
            for (int index = 0; index < 18; index++) {
                String name = tile.getStoredGasName(index >= 9, index % 9);
                Gas gas = name == null ? null : GasRegistry.getGas(name);
                if (gas != null && tile.getStoredGasAmount(index >= 9, index % 9) > 0) {
                    return new GasStack(gas, 1);
                }
            }
            return null;
        }

        @Nullable
        private GasStack drawNetwork(Gas gas, int amount, boolean doTransfer) {
            IAEGasStack extracted = extractFromNetwork(tile, gas.getName(), amount,
                    doTransfer ? Actionable.MODULATE : Actionable.SIMULATE);
            return extracted == null ? null : extracted.getGasStack();
        }

        @Nullable
        private IAEGasStack firstNetworkGas() {
            IMEMonitor<IAEGasStack> monitor = getNetworkMonitor(tile);
            if (monitor == null) return null;
            for (IAEGasStack stack : monitor.getStorageList()) {
                if (stack != null && stack.getStackSize() > 0) return stack;
            }
            return null;
        }
    }

    @Nullable
    private static IAEGasStack insertIntoNetwork(TileCommonInterfaceAlternate tile, String gasName,
                                                   int amount, Actionable mode) {
        Gas gas = GasRegistry.getGas(gasName);
        IMEInventory<IAEGasStack> inventory = getNetworkInventory(tile);
        if (gas == null || inventory == null || amount <= 0) return inputOrNull(gas, amount);
        try {
            IAEGasStack input = AEGasStack.of(new GasStack(gas, amount));
            return Platform.poweredInsert(tile.getProxy().getEnergy(), inventory, input,
                    new MachineSource(tile), mode);
        } catch (GridAccessException e) {
            return inputOrNull(gas, amount);
        }
    }

    @Nullable
    private static IAEGasStack extractFromNetwork(TileCommonInterfaceAlternate tile, String gasName,
                                                   int amount, Actionable mode) {
        Gas gas = GasRegistry.getGas(gasName);
        IMEInventory<IAEGasStack> inventory = getNetworkInventory(tile);
        if (gas == null || inventory == null || amount <= 0) return null;
        try {
            return Platform.poweredExtraction(tile.getProxy().getEnergy(), inventory,
                    AEGasStack.of(new GasStack(gas, amount)), new MachineSource(tile), mode);
        } catch (GridAccessException e) {
            return null;
        }
    }
}
