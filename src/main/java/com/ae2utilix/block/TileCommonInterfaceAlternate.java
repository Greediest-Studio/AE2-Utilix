package com.ae2utilix.block;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.ICraftingLink;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.security.IActionSource;
import appeng.me.GridAccessException;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.storage.IMEInventory;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.IStorageChannel;
import appeng.api.storage.IStorageMonitorable;
import appeng.api.storage.IStorageMonitorableAccessor;
import appeng.api.storage.channels.IFluidStorageChannel;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.api.util.IConfigManager;
import appeng.api.util.AECableType;
import appeng.api.util.AEPartLocation;
import appeng.capabilities.Capabilities;
import appeng.helpers.DualityInterface;
import appeng.helpers.IInterfaceHost;
import appeng.helpers.IPriorityHost;
import appeng.helpers.InventoryAction;
import appeng.me.helpers.MachineSource;
import appeng.me.storage.MEMonitorIFluidHandler;
import appeng.tile.grid.AENetworkInvTile;
import appeng.util.Platform;
import appeng.util.IConfigManagerHost;
import appeng.util.inv.IInventoryDestination;
import appeng.util.inv.InvOperation;
import appeng.tile.inventory.AppEngInternalAEInventory;
import appeng.tile.inventory.AppEngInternalInventory;
import appeng.util.inv.filter.IAEItemFilter;
import com.google.common.collect.ImmutableSet;
import com.ae2utilix.integration.BotaniaFluxIntegration;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import net.minecraftforge.fluids.capability.FluidTankProperties;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.ByteBufUtils;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.EnumSet;
import java.util.List;

public class TileCommonInterfaceAlternate extends AENetworkInvTile
        implements IFluidHandler, IStorageMonitorable, IInterfaceHost, IGridTickable,
        IInventoryDestination, IConfigManagerHost, IPriorityHost, IPhaseLinkHost, ITickable {

    private static final int ITEM_CAPACITY = 512;
    private static final int VIRTUAL_STORAGE_CAPACITY = 512000;
    private static final int STORAGE_TYPE_NONE = 0;
    private static final int STORAGE_TYPE_FLUID = 1;
    private static final int STORAGE_TYPE_GAS = 2;
    private static final int STORAGE_TYPE_MANA = 3;
    private static final int STORAGE_TYPE_FE = 4;
    private static final int STORAGE_TYPE_ESSENTIA = 5;
    private static final int STORAGE_TYPE_ITEM = 6;
    private static final String NBT_LINK_DIM = "ae2utilix_link_dim";
    private static final String NBT_LINK_X = "ae2utilix_link_x";
    private static final String NBT_LINK_Y = "ae2utilix_link_y";
    private static final String NBT_LINK_Z = "ae2utilix_link_z";
    private static final String NBT_LINK_FACE = "ae2utilix_link_face";
    private final DualityInterface interfaceDuality = new DualityInterface(this.getProxy(), this);
    private final DualityInterface extendedDuality = new DualityInterface(this.getProxy(), this);
    private final IAEFluidStack[] interfaceFluids = new IAEFluidStack[9];
    private final IAEFluidStack[] extendedFluids = new IAEFluidStack[9];
    private final int[] interfaceFluidAmounts = new int[9];
    private final int[] extendedFluidAmounts = new int[9];
    private final IAEFluidStack[] interfaceStoredFluids = new IAEFluidStack[9];
    private final IAEFluidStack[] extendedStoredFluids = new IAEFluidStack[9];
    private final String[] interfaceGases = new String[9];
    private final String[] extendedGases = new String[9];
    private final int[] interfaceGasAmounts = new int[9];
    private final int[] extendedGasAmounts = new int[9];
    private final String[] interfaceStoredGases = new String[9];
    private final String[] extendedStoredGases = new String[9];
    private final int[] interfaceStoredGasAmounts = new int[9];
    private final int[] extendedStoredGasAmounts = new int[9];
    private final long[] interfaceManaConfigAmounts = new long[9];
    private final long[] extendedManaConfigAmounts = new long[9];
    private final long[] interfaceManaAmounts = new long[9];
    private final long[] extendedManaAmounts = new long[9];
    private final long[] interfaceFeConfigAmounts = new long[9];
    private final long[] extendedFeConfigAmounts = new long[9];
    private final long[] interfaceFeAmounts = new long[9];
    private final long[] extendedFeAmounts = new long[9];
    private final String[] interfaceEssentiaAspects = new String[9];
    private final String[] extendedEssentiaAspects = new String[9];
    private final int[] interfaceEssentiaConfigAmounts = new int[9];
    private final int[] extendedEssentiaConfigAmounts = new int[9];
    private final String[] interfaceStoredEssentiaAspects = new String[9];
    private final String[] extendedStoredEssentiaAspects = new String[9];
    private final int[] interfaceStoredEssentiaAmounts = new int[9];
    private final int[] extendedStoredEssentiaAmounts = new int[9];
    private final MEMonitorIFluidHandler fluidMonitor = new MEMonitorIFluidHandler(this);
    private final com.ae2utilix.integration.NetworkStorageItemHandler networkItemHandler =
            new com.ae2utilix.integration.NetworkStorageItemHandler(this.getProxy(), this);
    private final com.ae2utilix.integration.NetworkStorageFluidHandler networkFluidHandler =
            new com.ae2utilix.integration.NetworkStorageFluidHandler(this.getProxy(), this);
    private final IActionSource fluidRequestSource = new MachineSource(this);
    private final com.ae2utilix.integration.VirtualCraftingTracker virtualCrafting =
            new com.ae2utilix.integration.VirtualCraftingTracker(this);
    private final IStorageMonitorable storageMonitorable = new IStorageMonitorable() {
        @Override
        @SuppressWarnings("unchecked")
        public <T extends IAEStack<T>> IMEMonitor<T> getInventory(IStorageChannel<T> channel) {
            if (channel == AEApi.instance().storage().getStorageChannel(IFluidStorageChannel.class)) {
                if (hasFluidConfig()) {
                    refreshFluidMonitor();
                    return (IMEMonitor<T>) fluidMonitor;
                }
                return (IMEMonitor<T>) networkFluidHandler.getMonitor();
            }
            if (channel == AEApi.instance().storage().getStorageChannel(IItemStorageChannel.class)) {
                if (hasItemConfig()) {
                    return TileCommonInterfaceAlternate.this.getInterfaceDuality().getInventory(channel);
                }
                return (IMEMonitor<T>) networkItemHandler.getMonitor();
            }
            if (com.ae2utilix.integration.MekanismEnergisticsIntegration.isGasChannel(channel)) {
                return (IMEMonitor<T>) com.ae2utilix.integration.MekanismEnergisticsIntegration
                        .getMonitor(TileCommonInterfaceAlternate.this, hasGasConfig());
            }
            if (com.ae2utilix.integration.BotaniaFluxIntegration.isManaChannel(channel)) {
                return (IMEMonitor<T>) com.ae2utilix.integration.BotaniaFluxIntegration
                        .getManaMonitor(TileCommonInterfaceAlternate.this);
            }
            if (com.ae2utilix.integration.BotaniaFluxIntegration.isFeChannel(channel)) {
                return (IMEMonitor<T>) com.ae2utilix.integration.BotaniaFluxIntegration
                        .getFeMonitor(TileCommonInterfaceAlternate.this);
            }
            if (com.ae2utilix.integration.ThaumicEnergisticsIntegration.isEssentiaChannel(channel)) {
                return (IMEMonitor<T>) com.ae2utilix.integration.ThaumicEnergisticsIntegration
                        .getMonitor(TileCommonInterfaceAlternate.this, hasEssentiaConfig());
            }
            return null;
        }
    };
    private final IStorageMonitorableAccessor storageMonitorableAccessor = new IStorageMonitorableAccessor() {
        @Override
        public IStorageMonitorable getInventory(IActionSource src) {
            return Platform.canAccess(TileCommonInterfaceAlternate.this.getProxy(), src)
                    ? storageMonitorable : null;
        }
    };
    private boolean updatingFluidState;
    private int fluidRequestTicker;
    private int linkDim = Integer.MIN_VALUE;
    private net.minecraft.util.math.BlockPos linkPos;
    private EnumFacing linkFace;

    public TileCommonInterfaceAlternate() {
        this.getProxy().setValidSides(EnumSet.allOf(EnumFacing.class));
        this.installStorageItemFilter(this.interfaceDuality.getStorage(), false);
        this.installStorageItemFilter(this.extendedDuality.getStorage(), true);
    }

    private void installStorageItemFilter(IItemHandler storage, final boolean extended) {
        if (storage instanceof AppEngInternalInventory) {
            ((AppEngInternalInventory) storage).setFilter(new IAEItemFilter() {
                @Override
                public boolean allowExtract(IItemHandler inv, int slot, int amount) {
                    return true;
                }

                @Override
                public boolean allowInsert(IItemHandler inv, int slot, ItemStack stack) {
                    return !TileCommonInterfaceAlternate.this.hasVirtualStorage(extended, slot);
                }
            });
        }
    }

    public DualityInterface getInterfaceDuality() {
        return this.interfaceDuality;
    }

    public int getCapacityUpgradeCount() {
        return Math.min(4, this.getInstalledUpgrades(appeng.api.config.Upgrades.CAPACITY));
    }

    public int getItemSlotCapacity() {
        return ITEM_CAPACITY << this.getCapacityUpgradeCount();
    }

    public int getVirtualStorageCapacity() {
        return VIRTUAL_STORAGE_CAPACITY << this.getCapacityUpgradeCount();
    }

    public boolean hasItemStorage(boolean extended, int slot) {
        IItemHandler storage = extended ? this.extendedDuality.getStorage() : this.interfaceDuality.getStorage();
        return !storage.getStackInSlot(slot).isEmpty();
    }

    private int getConfiguredStorageType(boolean extended, int slot) {
        // Use the persisted aspect name first. This also works with native
        // Thaumic Energistics dummy markers when their ItemStack is unavailable
        // to the type check during a server tick.
        if (this.getEssentiaConfigAspect(extended, slot) != null) {
            return STORAGE_TYPE_ESSENTIA;
        }
        ItemStack config = (extended ? this.extendedDuality : this.interfaceDuality)
                .getConfig().getStackInSlot(slot);
        if (com.ae2utilix.item.ItemFluidMark.isFluidMark(config)) return STORAGE_TYPE_FLUID;
        if (com.ae2utilix.item.ItemFluidMark.isGasMark(config)) return STORAGE_TYPE_GAS;
        if (com.ae2utilix.item.ItemFluidMark.isManaMark(config)) return STORAGE_TYPE_MANA;
        if (com.ae2utilix.item.ItemFluidMark.isFeMark(config)) return STORAGE_TYPE_FE;
        if (com.ae2utilix.item.ItemFluidMark.isEssentiaMark(config)) return STORAGE_TYPE_ESSENTIA;
        return config.isEmpty() ? STORAGE_TYPE_NONE : STORAGE_TYPE_ITEM;
    }

    public boolean hasFluidStorage(boolean extended, int slot) {
        IAEFluidStack fluid = (extended ? this.extendedStoredFluids : this.interfaceStoredFluids)[slot];
        return fluid != null && fluid.getStackSize() > 0;
    }

    public boolean hasGasStorage(boolean extended, int slot) {
        String name = (extended ? this.extendedStoredGases : this.interfaceStoredGases)[slot];
        int amount = (extended ? this.extendedStoredGasAmounts : this.interfaceStoredGasAmounts)[slot];
        return name != null && !name.isEmpty() && amount > 0;
    }

    public boolean hasManaStorage(boolean extended, int slot) {
        return (extended ? this.extendedManaAmounts : this.interfaceManaAmounts)[slot] > 0;
    }

    public boolean hasFeStorage(boolean extended, int slot) {
        return (extended ? this.extendedFeAmounts : this.interfaceFeAmounts)[slot] > 0;
    }

    public boolean hasEssentiaStorage(boolean extended, int slot) {
        String name = (extended ? this.extendedStoredEssentiaAspects : this.interfaceStoredEssentiaAspects)[slot];
        return name != null && !name.isEmpty()
                && (extended ? this.extendedStoredEssentiaAmounts : this.interfaceStoredEssentiaAmounts)[slot] > 0;
    }

    public boolean hasVirtualStorage(boolean extended, int slot) {
        int configuredType = this.getConfiguredStorageType(extended, slot);
        return (configuredType >= STORAGE_TYPE_FLUID
                && configuredType <= STORAGE_TYPE_ESSENTIA)
                || this.hasFluidStorage(extended, slot)
                || this.hasGasStorage(extended, slot)
                || this.hasManaStorage(extended, slot)
                || this.hasFeStorage(extended, slot)
                || this.hasEssentiaStorage(extended, slot);
    }

    public boolean canStoreFluidInSlot(boolean extended, int slot) {
        int configuredType = this.getConfiguredStorageType(extended, slot);
        return configuredType != STORAGE_TYPE_ITEM
                && (configuredType == STORAGE_TYPE_NONE || configuredType == STORAGE_TYPE_FLUID)
                && !this.hasItemStorage(extended, slot)
                && !this.hasGasStorage(extended, slot)
                && !this.hasManaStorage(extended, slot)
                && !this.hasFeStorage(extended, slot)
                && !this.hasEssentiaStorage(extended, slot);
    }

    public boolean canStoreGasInSlot(boolean extended, int slot) {
        int configuredType = this.getConfiguredStorageType(extended, slot);
        return configuredType != STORAGE_TYPE_ITEM
                && (configuredType == STORAGE_TYPE_NONE || configuredType == STORAGE_TYPE_GAS)
                && !this.hasItemStorage(extended, slot)
                && !this.hasFluidStorage(extended, slot)
                && !this.hasManaStorage(extended, slot)
                && !this.hasFeStorage(extended, slot)
                && !this.hasEssentiaStorage(extended, slot);
    }

    public boolean canStoreManaInSlot(boolean extended, int slot) {
        int configuredType = this.getConfiguredStorageType(extended, slot);
        return configuredType != STORAGE_TYPE_ITEM
                && (configuredType == STORAGE_TYPE_NONE || configuredType == STORAGE_TYPE_MANA)
                && !this.hasItemStorage(extended, slot)
                && !this.hasFluidStorage(extended, slot)
                && !this.hasGasStorage(extended, slot)
                && !this.hasFeStorage(extended, slot)
                && !this.hasEssentiaStorage(extended, slot);
    }

    public boolean canStoreFeInSlot(boolean extended, int slot) {
        int configuredType = this.getConfiguredStorageType(extended, slot);
        return configuredType != STORAGE_TYPE_ITEM
                && (configuredType == STORAGE_TYPE_NONE || configuredType == STORAGE_TYPE_FE)
                && !this.hasItemStorage(extended, slot)
                && !this.hasFluidStorage(extended, slot)
                && !this.hasGasStorage(extended, slot)
                && !this.hasManaStorage(extended, slot)
                && !this.hasEssentiaStorage(extended, slot);
    }

    public boolean canStoreEssentiaInSlot(boolean extended, int slot) {
        int configuredType = this.getConfiguredStorageType(extended, slot);
        return configuredType != STORAGE_TYPE_ITEM
                && (configuredType == STORAGE_TYPE_NONE || configuredType == STORAGE_TYPE_ESSENTIA)
                && !this.hasItemStorage(extended, slot)
                && !this.hasFluidStorage(extended, slot)
                && !this.hasGasStorage(extended, slot)
                && !this.hasManaStorage(extended, slot)
                && !this.hasFeStorage(extended, slot);
    }

    private void refreshItemSlotCapacities() {
        final int itemCapacity = this.getItemSlotCapacity();
        this.refreshItemSlotCapacities(this.interfaceDuality, itemCapacity);
        this.refreshItemSlotCapacities(this.extendedDuality, itemCapacity);
    }

    private void refreshItemSlotCapacities(DualityInterface duality, int itemCapacity) {
        IItemHandler config = duality.getConfig();
        if (config instanceof AppEngInternalAEInventory) {
            ((AppEngInternalAEInventory) config).setMaxStackSize(itemCapacity);
        }

        IItemHandler storage = duality.getStorage();
        if (storage instanceof AppEngInternalInventory) {
            AppEngInternalInventory internal = (AppEngInternalInventory) storage;
            for (int slot = 0; slot < internal.getSlots(); slot++) {
                internal.setMaxStackSize(slot, itemCapacity);
            }
        }
    }

    public IItemHandler getConfig() {
        this.refreshItemSlotCapacities();
        return this.interfaceDuality.getConfig();
    }

    public IItemHandler getStorage() {
        this.refreshItemSlotCapacities();
        return this.interfaceDuality.getStorage();
    }

    public IItemHandler getExtendedConfig() {
        this.refreshItemSlotCapacities();
        return this.extendedDuality.getConfig();
    }

    public IItemHandler getExtendedStorage() {
        this.refreshItemSlotCapacities();
        return this.extendedDuality.getStorage();
    }

    public DualityInterface getExtendedDuality() {
        return this.extendedDuality;
    }

    public void updateRedstoneState() {
        this.interfaceDuality.updateRedstoneState();
        this.extendedDuality.updateRedstoneState();
    }

    @Override
    public IItemHandler getInternalInventory() {
        return this.interfaceDuality.getInternalInventory();
    }

    @Override
    public void gridChanged() {
        super.gridChanged();
        this.interfaceDuality.gridChanged();
        this.extendedDuality.gridChanged();
    }

    @Override
    public void onReady() {
        this.getProxy().setValidSides(EnumSet.allOf(EnumFacing.class));
        super.onReady();
        this.interfaceDuality.initialize();
        this.extendedDuality.initialize();
        this.fluidMonitor.setActionSource(this.fluidRequestSource);
    }

    @Override
    public void update() {
        if (this.getWorld() == null || this.getWorld().isRemote) {
            return;
        }

        // Keep fluid requests alive if the older AE2 tick manager leaves a
        // custom IGridTickable asleep after its configuration changes.
        if (++this.fluidRequestTicker >= 5) {
            this.fluidRequestTicker = 0;
            if (this.getProxy().isActive()) {
                this.flushUnconfiguredFluidsToNetwork();
                com.ae2utilix.integration.MekanismEnergisticsIntegration.flushUnconfiguredGasesToNetwork(this);
                this.requestMarkedFluids(this.getInterfaceDuality());
                this.requestMarkedFluids(this.extendedDuality);
                com.ae2utilix.integration.MekanismEnergisticsIntegration.requestMarkedGases(this, false);
                com.ae2utilix.integration.MekanismEnergisticsIntegration.requestMarkedGases(this, true);
                com.ae2utilix.integration.BotaniaFluxIntegration.flushUnconfigured(this, com.ae2utilix.integration.BotaniaFluxIntegration.MANA);
                com.ae2utilix.integration.BotaniaFluxIntegration.flushUnconfigured(this, com.ae2utilix.integration.BotaniaFluxIntegration.FE);
                com.ae2utilix.integration.BotaniaFluxIntegration.requestMarked(this, com.ae2utilix.integration.BotaniaFluxIntegration.MANA);
                com.ae2utilix.integration.BotaniaFluxIntegration.requestMarked(this, com.ae2utilix.integration.BotaniaFluxIntegration.FE);
                com.ae2utilix.integration.ThaumicEnergisticsIntegration.flushUnconfiguredEssentiaToNetwork(this);
                com.ae2utilix.integration.ThaumicEnergisticsIntegration.requestMarkedEssentia(this, false);
                com.ae2utilix.integration.ThaumicEnergisticsIntegration.requestMarkedEssentia(this, true);
            }
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound data) {
        super.writeToNBT(data);
        this.interfaceDuality.writeToNBT(data);
        NBTTagCompound extended = new NBTTagCompound();
        this.extendedDuality.writeToNBT(extended);
        data.setTag("ae2utilix_extended_interface", extended);
        this.writeFluidState(data, "interface", this.interfaceFluids, this.interfaceFluidAmounts);
        this.writeFluidState(data, "extended", this.extendedFluids, this.extendedFluidAmounts);
        this.writeFluidState(data, "interface_stored", this.interfaceStoredFluids, null);
        this.writeFluidState(data, "extended_stored", this.extendedStoredFluids, null);
        this.writeGasState(data, "interface", this.interfaceGases, this.interfaceGasAmounts);
        this.writeGasState(data, "extended", this.extendedGases, this.extendedGasAmounts);
        this.writeGasState(data, "interface_stored", this.interfaceStoredGases, this.interfaceStoredGasAmounts);
        this.writeGasState(data, "extended_stored", this.extendedStoredGases, this.extendedStoredGasAmounts);
        this.writeLongState(data, "interface_mana_config", this.interfaceManaConfigAmounts);
        this.writeLongState(data, "interface_mana_stored", this.interfaceManaAmounts);
        this.writeLongState(data, "extended_mana_config", this.extendedManaConfigAmounts);
        this.writeLongState(data, "extended_mana_stored", this.extendedManaAmounts);
        this.writeLongState(data, "interface_fe_config", this.interfaceFeConfigAmounts);
        this.writeLongState(data, "interface_fe_stored", this.interfaceFeAmounts);
        this.writeLongState(data, "extended_fe_config", this.extendedFeConfigAmounts);
        this.writeLongState(data, "extended_fe_stored", this.extendedFeAmounts);
        this.writeEssentiaState(data, "interface", this.interfaceEssentiaAspects,
                this.interfaceEssentiaConfigAmounts);
        this.writeEssentiaState(data, "extended", this.extendedEssentiaAspects,
                this.extendedEssentiaConfigAmounts);
        this.writeEssentiaState(data, "interface_stored", this.interfaceStoredEssentiaAspects,
                this.interfaceStoredEssentiaAmounts);
        this.writeEssentiaState(data, "extended_stored", this.extendedStoredEssentiaAspects,
                this.extendedStoredEssentiaAmounts);
        this.virtualCrafting.writeToNBT(data);
        if (this.hasLinkData()) {
            data.setInteger(NBT_LINK_DIM, this.linkDim);
            data.setInteger(NBT_LINK_X, this.linkPos.getX());
            data.setInteger(NBT_LINK_Y, this.linkPos.getY());
            data.setInteger(NBT_LINK_Z, this.linkPos.getZ());
            data.setInteger(NBT_LINK_FACE, this.linkFace.ordinal());
        }
        return data;
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);
        this.interfaceDuality.readFromNBT(data);
        if (data.hasKey("ae2utilix_extended_interface")) {
            this.extendedDuality.readFromNBT(data.getCompoundTag("ae2utilix_extended_interface"));
        }
        this.upgradeEssentiaMarkers(this.interfaceDuality.getConfig());
        this.upgradeEssentiaMarkers(this.extendedDuality.getConfig());
        this.readFluidState(data, "interface", this.interfaceFluids, this.interfaceFluidAmounts);
        this.readFluidState(data, "extended", this.extendedFluids, this.extendedFluidAmounts);
        this.readFluidState(data, "interface_stored", this.interfaceStoredFluids, null);
        this.readFluidState(data, "extended_stored", this.extendedStoredFluids, null);
        this.readGasState(data, "interface", this.interfaceGases, this.interfaceGasAmounts);
        this.readGasState(data, "extended", this.extendedGases, this.extendedGasAmounts);
        this.readGasState(data, "interface_stored", this.interfaceStoredGases, this.interfaceStoredGasAmounts);
        this.readGasState(data, "extended_stored", this.extendedStoredGases, this.extendedStoredGasAmounts);
        this.readEnergyState(data, "interface", false, BotaniaFluxIntegration.MANA,
                this.interfaceManaConfigAmounts, this.interfaceManaAmounts);
        this.readEnergyState(data, "extended", true, BotaniaFluxIntegration.MANA,
                this.extendedManaConfigAmounts, this.extendedManaAmounts);
        this.readEnergyState(data, "interface", false, BotaniaFluxIntegration.FE,
                this.interfaceFeConfigAmounts, this.interfaceFeAmounts);
        this.readEnergyState(data, "extended", true, BotaniaFluxIntegration.FE,
                this.extendedFeConfigAmounts, this.extendedFeAmounts);
        this.readEssentiaState(data, "interface", this.interfaceEssentiaAspects,
                this.interfaceEssentiaConfigAmounts);
        this.readEssentiaState(data, "extended", this.extendedEssentiaAspects,
                this.extendedEssentiaConfigAmounts);
        this.readEssentiaState(data, "interface_stored", this.interfaceStoredEssentiaAspects,
                this.interfaceStoredEssentiaAmounts);
        this.readEssentiaState(data, "extended_stored", this.extendedStoredEssentiaAspects,
                this.extendedStoredEssentiaAmounts);
        this.virtualCrafting.readFromNBT(data);
        if (data.hasKey(NBT_LINK_DIM)) {
            this.linkDim = data.getInteger(NBT_LINK_DIM);
            this.linkPos = new net.minecraft.util.math.BlockPos(
                    data.getInteger(NBT_LINK_X), data.getInteger(NBT_LINK_Y), data.getInteger(NBT_LINK_Z));
            int ordinal = data.getInteger(NBT_LINK_FACE);
            this.linkFace = ordinal >= 0 && ordinal < EnumFacing.values().length
                    ? EnumFacing.values()[ordinal] : null;
        }
    }

    private void upgradeEssentiaMarkers(IItemHandler config) {
        if (!(config instanceof IItemHandlerModifiable)) return;
        IItemHandlerModifiable mutable = (IItemHandlerModifiable) config;
        for (int slot = 0; slot < mutable.getSlots(); slot++) {
            ItemStack current = mutable.getStackInSlot(slot);
            ItemStack upgraded = com.ae2utilix.item.ItemFluidMark.upgradeEssentiaMarker(current);
            if (upgraded != current) mutable.setStackInSlot(slot, upgraded);
        }
    }

    public void setFluidConfig(boolean extended, int slot, FluidStack fluid) {
        IAEFluidStack[] fluids = extended ? this.extendedFluids : this.interfaceFluids;
        int[] amounts = extended ? this.extendedFluidAmounts : this.interfaceFluidAmounts;
        String[] gases = extended ? this.extendedGases : this.interfaceGases;
        int[] gasAmounts = extended ? this.extendedGasAmounts : this.interfaceGasAmounts;
        if (fluid == null) {
            fluids[slot] = null;
            amounts[slot] = 0;
        } else {
            FluidStack configured = fluid.copy();
            configured.amount = Math.max(1,
                    Math.min(this.getVirtualStorageCapacity(), configured.amount));
            fluids[slot] = appeng.fluids.util.AEFluidStack.fromFluidStack(configured);
            amounts[slot] = configured.amount;
        }
        gases[slot] = null;
        gasAmounts[slot] = 0;
        this.markDirty();
        this.saveChanges();
        this.markForUpdate();
        this.refreshFluidMonitor();
        this.wakeFluidRequests();
    }

    public void setGasConfig(boolean extended, int slot, String gasName, int amount) {
        IAEFluidStack[] fluids = extended ? this.extendedFluids : this.interfaceFluids;
        int[] fluidAmounts = extended ? this.extendedFluidAmounts : this.interfaceFluidAmounts;
        String[] gases = extended ? this.extendedGases : this.interfaceGases;
        int[] gasAmounts = extended ? this.extendedGasAmounts : this.interfaceGasAmounts;
        fluids[slot] = null;
        fluidAmounts[slot] = 0;
        gases[slot] = gasName == null || gasName.isEmpty() ? null : gasName;
        gasAmounts[slot] = gases[slot] == null ? 0 : Math.max(1, Math.min(this.getVirtualStorageCapacity(), amount));
        this.markDirty();
        this.saveChanges();
        this.markForUpdate();
        this.wakeFluidRequests();
    }

    public String getEssentiaConfigAspect(boolean extended, int slot) {
    String[] aspects = extended ? this.extendedEssentiaAspects : this.interfaceEssentiaAspects;
    String tag = aspects[slot];
    if (tag != null && !tag.isEmpty()) return tag;
    IItemHandler config = extended ? this.extendedDuality.getConfig() : this.getConfig();
    ItemStack stack = config.getStackInSlot(slot);
    String itemTag = com.ae2utilix.item.ItemFluidMark.getAspectTag(stack);
    if (itemTag != null && !itemTag.isEmpty()) return itemTag;
    if (!net.minecraftforge.fml.common.Loader.isModLoaded("thaumicenergistics")) return null;
    // Thaumic Energistics uses its own dummy-aspect item as the marker.
    return com.ae2utilix.integration.ThaumicEnergisticsOptional.getAspectTagFromItem(stack);
}

    public int getEssentiaConfigAmount(boolean extended, int slot) {
        int[] amounts = extended ? this.extendedEssentiaConfigAmounts : this.interfaceEssentiaConfigAmounts;
        return Math.max(1, Math.min(this.getVirtualStorageCapacity(), amounts[slot] <= 0 ? 1000 : amounts[slot]));
    }

    public void setEssentiaConfig(boolean extended, int slot, String aspectTag, int amount) {
        String[] aspects = extended ? this.extendedEssentiaAspects : this.interfaceEssentiaAspects;
        int[] amounts = extended ? this.extendedEssentiaConfigAmounts : this.interfaceEssentiaConfigAmounts;
        aspects[slot] = aspectTag == null || aspectTag.isEmpty() ? null : aspectTag;
        amounts[slot] = aspects[slot] == null ? 0
                : Math.max(1, Math.min(this.getVirtualStorageCapacity(), amount));
        this.markDirty();
        this.saveChanges();
        this.markForUpdate();
        this.wakeFluidRequests();
    }

    public String getStoredEssentiaAspect(boolean extended, int slot) {
        return (extended ? this.extendedStoredEssentiaAspects : this.interfaceStoredEssentiaAspects)[slot];
    }

    public int getStoredEssentiaAmount(boolean extended, int slot) {
        return (extended ? this.extendedStoredEssentiaAmounts : this.interfaceStoredEssentiaAmounts)[slot];
    }

    public void setStoredEssentia(boolean extended, int slot, String aspectTag, int amount) {
        String[] aspects = extended ? this.extendedStoredEssentiaAspects : this.interfaceStoredEssentiaAspects;
        int[] amounts = extended ? this.extendedStoredEssentiaAmounts : this.interfaceStoredEssentiaAmounts;
        if (aspectTag == null || aspectTag.isEmpty() || amount <= 0) {
            aspects[slot] = null;
            amounts[slot] = 0;
        } else {
            aspects[slot] = aspectTag;
            amounts[slot] = Math.min(this.getVirtualStorageCapacity(), amount);
        }
        this.markDirty();
        this.saveChanges();
        this.markForUpdate();
    }

    public String getGasConfigName(boolean extended, int slot) {
        String[] gases = extended ? this.extendedGases : this.interfaceGases;
        String gasName = gases[slot];
        if (gasName != null) return gasName;
        IItemHandler config = extended ? this.extendedDuality.getConfig() : this.getConfig();
        return com.ae2utilix.item.ItemFluidMark.getGasName(config.getStackInSlot(slot));
    }

    public int getGasConfigAmount(boolean extended, int slot) {
        int[] amounts = extended ? this.extendedGasAmounts : this.interfaceGasAmounts;
        return Math.max(1, Math.min(this.getVirtualStorageCapacity(),
                amounts[slot] <= 0 ? 1000 : amounts[slot]));
    }

    public String getStoredGasName(boolean extended, int slot) {
        return (extended ? this.extendedStoredGases : this.interfaceStoredGases)[slot];
    }

    public int getStoredGasAmount(boolean extended, int slot) {
        return (extended ? this.extendedStoredGasAmounts : this.interfaceStoredGasAmounts)[slot];
    }

    public int getManaConfigAmount(boolean extended, int slot) {
        long[] values = extended ? this.extendedManaConfigAmounts : this.interfaceManaConfigAmounts;
        return (int) Math.max(1, Math.min(this.getVirtualStorageCapacity(),
                values[slot] <= 0 ? 1000L : values[slot]));
    }

    public int getFeConfigAmount(boolean extended, int slot) {
        long[] values = extended ? this.extendedFeConfigAmounts : this.interfaceFeConfigAmounts;
        return (int) Math.max(1, Math.min(this.getVirtualStorageCapacity(),
                values[slot] <= 0 ? 1000L : values[slot]));
    }

    public long getStoredMana(boolean extended, int slot) {
        return (extended ? this.extendedManaAmounts : this.interfaceManaAmounts)[slot];
    }

    public long getStoredFe(boolean extended, int slot) {
        return (extended ? this.extendedFeAmounts : this.interfaceFeAmounts)[slot];
    }

    public void setManaConfig(boolean extended, int slot, int amount) {
        long[] values = extended ? this.extendedManaConfigAmounts : this.interfaceManaConfigAmounts;
        values[slot] = Math.max(1, Math.min(this.getVirtualStorageCapacity(), amount));
        this.markDirty();
        this.saveChanges();
        this.markForUpdate();
    }

    public void setFeConfig(boolean extended, int slot, int amount) {
        long[] values = extended ? this.extendedFeConfigAmounts : this.interfaceFeConfigAmounts;
        values[slot] = Math.max(1, Math.min(this.getVirtualStorageCapacity(), amount));
        this.markDirty();
        this.saveChanges();
        this.markForUpdate();
    }

    /**
     * Clears all virtual-resource state for a slot when it is changed back to
     * a normal item filter.  Virtual channels keep their amounts outside the
     * item handler, so merely replacing the visible marker would otherwise
     * leave stale fluid/gas/energy/essentia storage active.
     */
    public void clearVirtualConfig(boolean extended, int slot) {
        if (slot < 0 || slot >= 9) return;
        IAEFluidStack[] fluids = extended ? this.extendedFluids : this.interfaceFluids;
        int[] fluidAmounts = extended ? this.extendedFluidAmounts : this.interfaceFluidAmounts;
        IAEFluidStack[] storedFluids = extended ? this.extendedStoredFluids : this.interfaceStoredFluids;
        String[] gases = extended ? this.extendedGases : this.interfaceGases;
        int[] gasAmounts = extended ? this.extendedGasAmounts : this.interfaceGasAmounts;
        String[] storedGases = extended ? this.extendedStoredGases : this.interfaceStoredGases;
        int[] storedGasAmounts = extended ? this.extendedStoredGasAmounts : this.interfaceStoredGasAmounts;
        long[] manaConfig = extended ? this.extendedManaConfigAmounts : this.interfaceManaConfigAmounts;
        long[] manaStored = extended ? this.extendedManaAmounts : this.interfaceManaAmounts;
        long[] feConfig = extended ? this.extendedFeConfigAmounts : this.interfaceFeConfigAmounts;
        long[] feStored = extended ? this.extendedFeAmounts : this.interfaceFeAmounts;
        String[] essentia = extended ? this.extendedEssentiaAspects : this.interfaceEssentiaAspects;
        int[] essentiaConfig = extended ? this.extendedEssentiaConfigAmounts : this.interfaceEssentiaConfigAmounts;
        String[] storedEssentia = extended ? this.extendedStoredEssentiaAspects : this.interfaceStoredEssentiaAspects;
        int[] storedEssentiaAmounts = extended ? this.extendedStoredEssentiaAmounts : this.interfaceStoredEssentiaAmounts;

        fluids[slot] = null;
        fluidAmounts[slot] = 0;
        storedFluids[slot] = null;
        gases[slot] = null;
        gasAmounts[slot] = 0;
        storedGases[slot] = null;
        storedGasAmounts[slot] = 0;
        manaConfig[slot] = 0;
        manaStored[slot] = 0;
        feConfig[slot] = 0;
        feStored[slot] = 0;
        essentia[slot] = null;
        essentiaConfig[slot] = 0;
        storedEssentia[slot] = null;
        storedEssentiaAmounts[slot] = 0;
        this.markDirty();
        this.saveChanges();
        this.markForUpdate();
        this.refreshFluidMonitor();
        this.wakeFluidRequests();
    }

    public void setStoredMana(boolean extended, int slot, long amount) {
        long[] values = extended ? this.extendedManaAmounts : this.interfaceManaAmounts;
        values[slot] = Math.max(0, Math.min(this.getVirtualStorageCapacity(), amount));
        this.markDirty();
        this.saveChanges();
        this.markForUpdate();
    }

    public void setStoredFe(boolean extended, int slot, long amount) {
        long[] values = extended ? this.extendedFeAmounts : this.interfaceFeAmounts;
        values[slot] = Math.max(0, Math.min(this.getVirtualStorageCapacity(), amount));
        this.markDirty();
        this.saveChanges();
        this.markForUpdate();
    }

    public void setStoredGas(boolean extended, int slot, String gasName, int amount) {
        String[] names = extended ? this.extendedStoredGases : this.interfaceStoredGases;
        int[] amounts = extended ? this.extendedStoredGasAmounts : this.interfaceStoredGasAmounts;
        if (gasName == null || gasName.isEmpty() || amount <= 0) {
            names[slot] = null;
            amounts[slot] = 0;
        } else {
            names[slot] = gasName;
            amounts[slot] = Math.min(this.getVirtualStorageCapacity(), amount);
        }
        this.markDirty();
        this.saveChanges();
        this.markForUpdate();
    }

    @Override
    public void setLinkData(int dimension, net.minecraft.util.math.BlockPos position, EnumFacing face) {
        this.linkDim = dimension;
        this.linkPos = position;
        this.linkFace = face;
        this.saveChanges();
    }

    @Override
    public void clearLinkData() {
        this.linkDim = Integer.MIN_VALUE;
        this.linkPos = null;
        this.linkFace = null;
        this.saveChanges();
    }

    @Override
    public boolean hasLinkData() {
        return this.linkPos != null && this.linkFace != null;
    }

    @Override
    public Integer getLinkDimension() {
        return this.hasLinkData() ? this.linkDim : null;
    }

    @Override
    public net.minecraft.util.math.BlockPos getLinkPos() {
        return this.linkPos;
    }

    @Override
    public EnumFacing getLinkFace() {
        return this.linkFace;
    }

    @Override
    public boolean isLinkValid() {
        if (!this.hasLinkData() || this.getWorld() == null) return false;
        if (this.linkDim != this.getWorld().provider.getDimension()) return false;
        int dx = Math.abs(this.linkPos.getX() - this.getPos().getX());
        int dy = Math.abs(this.linkPos.getY() - this.getPos().getY());
        int dz = Math.abs(this.linkPos.getZ() - this.getPos().getZ());
        return dx <= 16 && dy <= 16 && dz <= 16;
    }

    @Override
    public String ae2utilix$getTermNameKey() {
        return new ItemStack(com.ae2utilix.AE2Utilix.BLOCK_COMMON_INTERFACE_ALTERNATE)
                .getUnlocalizedName() + ".name";
    }

    public FluidStack getFluidConfig(boolean extended, int slot) {
        IAEFluidStack fluid = (extended ? this.extendedFluids : this.interfaceFluids)[slot];
        if (fluid != null) {
            return fluid.getFluidStack();
        }

        IItemHandler config = extended ? this.extendedDuality.getConfig() : this.getInterfaceDuality().getConfig();
        FluidStack marked = com.ae2utilix.item.ItemFluidMark.getFluid(config.getStackInSlot(slot));
        return marked == null ? null : marked.copy();
    }

    @Nullable
    public FluidStack getStoredFluid(boolean extended, int slot) {
        IAEFluidStack fluid = (extended ? this.extendedStoredFluids : this.interfaceStoredFluids)[slot];
        return fluid == null ? null : fluid.getFluidStack();
    }

    public void setStoredFluid(boolean extended, int slot, FluidStack fluid) {
        IAEFluidStack[] stored = extended ? this.extendedStoredFluids : this.interfaceStoredFluids;
        if (fluid == null || fluid.amount <= 0 || fluid.getFluid() == null) {
            stored[slot] = null;
        } else {
            FluidStack copy = fluid.copy();
            copy.amount = Math.min(this.getVirtualStorageCapacity(), copy.amount);
            stored[slot] = appeng.fluids.util.AEFluidStack.fromFluidStack(copy);
        }
        this.markDirty();
        this.saveChanges();
        this.markForUpdate();
        this.refreshFluidMonitor();
        this.wakeVirtualCrafting();
    }

    public void requestVirtualFluidCrafting(boolean extended, int slot, FluidStack fluid, int amount) {
        if (fluid == null || amount <= 0 || this.getInstalledUpgrades(appeng.api.config.Upgrades.CRAFTING) <= 0
                || !com.ae2utilix.integration.FluidReturnHandler.hasAE2FC()) {
            return;
        }
        FluidStack output = fluid.copy();
        output.amount = Math.min(this.getVirtualStorageCapacity(), amount);
        IAEItemStack fake = com.ae2utilix.integration.FluidReturnHandler.packFluid2AEDrops(output);
        if (fake != null) {
            this.virtualCrafting.request(this.virtualCraftingSlot(false, extended, slot), fake);
        }
    }

    public void requestVirtualGasCrafting(boolean extended, int slot, String gasName, int amount) {
        if (gasName == null || gasName.isEmpty() || amount <= 0
                || this.getInstalledUpgrades(appeng.api.config.Upgrades.CRAFTING) <= 0
                || !com.ae2utilix.integration.MekanismEnergisticsIntegration.isAvailable()
                || !com.ae2utilix.integration.GasReturnHandler.hasGasSupport()) {
            return;
        }
        IAEItemStack fake = com.ae2utilix.integration.GasReturnHandler.packGas2AEDrops(
                gasName, Math.min(this.getVirtualStorageCapacity(), amount));
        if (fake != null) {
            this.virtualCrafting.request(this.virtualCraftingSlot(true, extended, slot), fake);
        }
    }

    public IAEItemStack acceptVirtualCraftedItems(int craftingSlot, IAEItemStack stack, Actionable mode) {
        if (stack == null || stack.getStackSize() <= 0) {
            return stack;
        }

        boolean gas = craftingSlot >= 18;
        int slot = gas ? craftingSlot - 18 : craftingSlot;
        boolean extended = slot >= 9;
        if (extended) slot -= 9;
        if (slot < 0 || slot >= 9) return stack;

        long accepted;
        if (gas) {
            String gasName = com.ae2utilix.integration.GasReturnHandler.getGasNameFromAEStack(stack);
            String expected = this.getGasConfigName(extended, slot);
            if (gasName == null || expected == null || !expected.equals(gasName)) return stack;
            long stored = this.getStoredGasAmount(extended, slot);
            accepted = Math.min(stack.getStackSize(),
                    Math.max(0L, this.getVirtualStorageCapacity() - stored));
            if (mode == Actionable.MODULATE && accepted > 0) {
                this.setStoredGas(extended, slot, gasName, (int) (stored + accepted));
            }
        } else {
            FluidStack produced = com.ae2utilix.integration.FluidReturnHandler.getFluidFromAEStack(stack);
            FluidStack expected = this.getFluidConfig(extended, slot);
            if (produced == null || expected == null || !expected.isFluidEqual(produced)) return stack;
            FluidStack storedFluid = this.getStoredFluid(extended, slot);
            if (storedFluid != null && !storedFluid.isFluidEqual(produced)) return stack;
            long stored = storedFluid == null ? 0 : storedFluid.amount;
            accepted = Math.min(stack.getStackSize(),
                    Math.max(0L, this.getVirtualStorageCapacity() - stored));
            if (mode == Actionable.MODULATE && accepted > 0) {
                FluidStack next = storedFluid == null ? produced.copy() : storedFluid.copy();
                next.amount = (int) (stored + accepted);
                this.setStoredFluid(extended, slot, next);
            }
        }

        if (accepted >= stack.getStackSize()) return null;
        IAEItemStack remainder = stack.copy();
        remainder.setStackSize(stack.getStackSize() - accepted);
        return remainder;
    }

    public void wakeVirtualCrafting() {
        try {
            if (!this.getProxy().getTick().alertDevice(this.getProxy().getNode())) {
                this.getProxy().getTick().wakeDevice(this.getProxy().getNode());
            }
        } catch (GridAccessException ignored) {
        }
    }

    private int virtualCraftingSlot(boolean gas, boolean extended, int slot) {
        return (gas ? 18 : 0) + (extended ? 9 : 0) + slot;
    }

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY
                || capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY
                || capability == CapabilityEnergy.ENERGY
                || capability == Capabilities.STORAGE_MONITORABLE_ACCESSOR
                || com.ae2utilix.integration.MekanismEnergisticsIntegration.isGasCapability(capability)
                || super.hasCapability(capability, facing);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            if (hasItemConfig()) {
                return super.getCapability(capability, facing);
            }
            return (T) this.networkItemHandler;
        }
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            return (T) this;
        }
        if (capability == CapabilityEnergy.ENERGY) {
            return (T) com.ae2utilix.integration.BotaniaFluxIntegration.getEnergyHandler(this);
        }
        if (com.ae2utilix.integration.MekanismEnergisticsIntegration.isGasCapability(capability)) {
            return (T) com.ae2utilix.integration.MekanismEnergisticsIntegration.getGasHandler(this);
        }
        if (capability == Capabilities.STORAGE_MONITORABLE_ACCESSOR) {
            return (T) this.storageMonitorableAccessor;
        }
        return super.getCapability(capability, facing);
    }

    @Override
    public <T extends IAEStack<T>> IMEMonitor<T> getInventory(IStorageChannel<T> channel) {
        return this.storageMonitorable.getInventory(channel);
    }

    @Override
    public TickingRequest getTickingRequest(IGridNode node) {
        TickingRequest primary = this.interfaceDuality.getTickingRequest(node);
        TickingRequest extended = this.extendedDuality.getTickingRequest(node);
        return new TickingRequest(Math.min(primary.minTickRate, extended.minTickRate),
                Math.min(primary.maxTickRate, extended.maxTickRate),
                primary.isSleeping && extended.isSleeping && !this.hasFluidWork()
                        && !com.ae2utilix.integration.MekanismEnergisticsIntegration.hasGasWork(this)
                        && !com.ae2utilix.integration.BotaniaFluxIntegration.hasManaConfig(this)
                        && !com.ae2utilix.integration.BotaniaFluxIntegration.hasFeConfig(this)
                        && !com.ae2utilix.integration.ThaumicEnergisticsIntegration.hasEssentiaWork(this)
                        && !this.virtualCrafting.hasWork(), true);
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
        this.requestMarkedFluids(this.getInterfaceDuality());
        this.requestMarkedFluids(this.extendedDuality);
        com.ae2utilix.integration.MekanismEnergisticsIntegration.flushUnconfiguredGasesToNetwork(this);
        com.ae2utilix.integration.MekanismEnergisticsIntegration.requestMarkedGases(this, false);
        com.ae2utilix.integration.MekanismEnergisticsIntegration.requestMarkedGases(this, true);
        com.ae2utilix.integration.BotaniaFluxIntegration.flushUnconfigured(this, com.ae2utilix.integration.BotaniaFluxIntegration.MANA);
        com.ae2utilix.integration.BotaniaFluxIntegration.flushUnconfigured(this, com.ae2utilix.integration.BotaniaFluxIntegration.FE);
        com.ae2utilix.integration.BotaniaFluxIntegration.requestMarked(this, com.ae2utilix.integration.BotaniaFluxIntegration.MANA);
        com.ae2utilix.integration.BotaniaFluxIntegration.requestMarked(this, com.ae2utilix.integration.BotaniaFluxIntegration.FE);
        com.ae2utilix.integration.ThaumicEnergisticsIntegration.flushUnconfiguredEssentiaToNetwork(this);
        com.ae2utilix.integration.ThaumicEnergisticsIntegration.requestMarkedEssentia(this, false);
        com.ae2utilix.integration.ThaumicEnergisticsIntegration.requestMarkedEssentia(this, true);
        TickRateModulation primary = this.interfaceDuality.tickingRequest(node, ticksSinceLastCall);
        TickRateModulation extended = this.extendedDuality.tickingRequest(node, ticksSinceLastCall);
        if (primary == TickRateModulation.URGENT || extended == TickRateModulation.URGENT) return TickRateModulation.URGENT;
        if (primary == TickRateModulation.FASTER || extended == TickRateModulation.FASTER) return TickRateModulation.FASTER;
        if (primary == TickRateModulation.SLOWER || extended == TickRateModulation.SLOWER) return TickRateModulation.SLOWER;
        if (primary == TickRateModulation.SLEEP && extended == TickRateModulation.SLEEP) {
            return this.hasFluidWork() || com.ae2utilix.integration.BotaniaFluxIntegration.hasManaConfig(this)
                    || com.ae2utilix.integration.BotaniaFluxIntegration.hasFeConfig(this)
                    || com.ae2utilix.integration.ThaumicEnergisticsIntegration.hasEssentiaWork(this)
                    || this.virtualCrafting.hasWork()
                    ? TickRateModulation.SLOWER : TickRateModulation.SLEEP;
        }
        return TickRateModulation.SAME;
    }

    @Override
    public void onChangeInventory(IItemHandler inv, int slot, InvOperation operation, ItemStack removed, ItemStack added) {
        if (inv == this.getConfig() || inv == this.getStorage()) {
            this.interfaceDuality.onChangeInventory(inv, slot, operation, removed, added);
        }
        if (inv == this.getExtendedConfig() || inv == this.getExtendedStorage()) {
            this.extendedDuality.onChangeInventory(inv, slot, operation, removed, added);
        }
    }

    @Override
    public void onStackReturnNetwork(IAEItemStack stack) {
        this.interfaceDuality.onStackReturnedToNetwork(stack);
        this.extendedDuality.onStackReturnedToNetwork(stack);
    }

    @Override
    public EnumSet<EnumFacing> getTargets() {
        return EnumSet.allOf(EnumFacing.class);
    }

    @Override
    public TileEntity getTileEntity() {
        return this;
    }

    @Override
    public AECableType getCableConnectionType(AEPartLocation dir) {
        return this.interfaceDuality.getCableConnectionType(dir);
    }

    @Override
    public appeng.api.util.DimensionalCoord getLocation() {
        return this.interfaceDuality.getLocation();
    }

    @Override
    public boolean canInsert(ItemStack stack) {
        return this.interfaceDuality.canInsert(stack);
    }

    @Override
    public int getInstalledUpgrades(appeng.api.config.Upgrades upgrade) {
        return this.interfaceDuality.getInstalledUpgrades(upgrade);
    }

    @Override
    public IItemHandler getInventoryByName(String name) {
        return this.interfaceDuality.getInventoryByName(name);
    }

    @Override
    public IConfigManager getConfigManager() {
        return this.interfaceDuality.getConfigManager();
    }

    @Override
    public void updateSetting(IConfigManager manager, Enum settingName, Enum newValue) {
        this.interfaceDuality.updateSetting(manager, settingName, newValue);
    }

    @Override
    public void provideCrafting(appeng.api.networking.crafting.ICraftingProviderHelper helper) {
        this.interfaceDuality.provideCrafting(helper);
        this.extendedDuality.provideCrafting(helper);
    }

    @Override
    public boolean pushPattern(ICraftingPatternDetails details, net.minecraft.inventory.InventoryCrafting table) {
        return this.interfaceDuality.pushPattern(details, table)
                || this.extendedDuality.pushPattern(details, table);
    }

    @Override
    public boolean isBusy() {
        return this.interfaceDuality.isBusy() || this.extendedDuality.isBusy()
                || this.virtualCrafting.hasWork();
    }

    @Override
    public ImmutableSet<ICraftingLink> getRequestedJobs() {
        ImmutableSet.Builder<ICraftingLink> jobs = ImmutableSet.builder();
        jobs.addAll(this.interfaceDuality.getRequestedJobs());
        jobs.addAll(this.extendedDuality.getRequestedJobs());
        jobs.addAll(this.virtualCrafting.getRequestedJobs());
        return jobs.build();
    }

    @Override
    public IAEItemStack injectCraftedItems(ICraftingLink link, IAEItemStack stack, Actionable mode) {
        if (this.virtualCrafting.getSlot(link) >= 0) {
            return this.virtualCrafting.injectCraftedItems(link, stack, mode);
        }
        if (this.interfaceDuality.getRequestedJobs().contains(link)) {
            return this.interfaceDuality.injectCraftedItems(link, stack, mode);
        }
        return this.extendedDuality.injectCraftedItems(link, stack, mode);
    }

    @Override
    public void jobStateChange(ICraftingLink link) {
        if (this.virtualCrafting.getSlot(link) >= 0) {
            this.virtualCrafting.jobStateChange(link);
        } else if (this.interfaceDuality.getRequestedJobs().contains(link)) {
            this.interfaceDuality.jobStateChange(link);
        } else {
            this.extendedDuality.jobStateChange(link);
        }
    }

    @Override
    public int getPriority() {
        return this.interfaceDuality.getPriority();
    }

    public void setPriority(int priority) {
        this.interfaceDuality.setPriority(priority);
    }

    @Override
    public appeng.core.sync.GuiBridge getGuiBridge() {
        return appeng.core.sync.GuiBridge.GUI_INTERFACE;
    }

    private void requestMarkedFluids(DualityInterface duality) {
        if (this.getWorld() == null || this.getWorld().isRemote || !this.getProxy().isActive()) return;

        IItemHandler config = duality.getConfig();
        boolean extended = duality == this.extendedDuality;
        IAEFluidStack[] storedFluids = extended ? this.extendedStoredFluids : this.interfaceStoredFluids;
        IMEMonitor<IAEFluidStack> inventory = this.networkFluidHandler.getMonitor();
        boolean canCraft = this.getInstalledUpgrades(appeng.api.config.Upgrades.CRAFTING) > 0
                && com.ae2utilix.integration.FluidReturnHandler.hasAE2FC();
        if (inventory == null && !canCraft) return;

        for (int slot = 0; slot < config.getSlots(); slot++) {
            FluidStack markedFluid = com.ae2utilix.item.ItemFluidMark.getFluid(config.getStackInSlot(slot));
            if (markedFluid == null) continue;

            FluidStack configuredFluid = this.getFluidConfig(extended, slot);
            IAEFluidStack[] configuredFluids = extended ? this.extendedFluids : this.interfaceFluids;
            if (configuredFluid == null || configuredFluids[slot] == null) {
                configuredFluid = markedFluid.copy();
                configuredFluid.amount = 1000;
                this.setFluidConfig(extended, slot, configuredFluid);
            }

            IAEFluidStack storedFluid = storedFluids[slot];
            if (storedFluid != null && !storedFluid.getFluidStack().isFluidEqual(markedFluid)) {
                if (inventory == null) continue;
                this.returnStoredFluidToNetwork(inventory, storedFluids, slot);
                storedFluid = storedFluids[slot];
            }

            int requestedAmount = Math.min(this.getVirtualStorageCapacity(), Math.max(1, configuredFluid.amount));
            int storedAmount = storedFluid == null ? 0
                    : (int) Math.min(Integer.MAX_VALUE, storedFluid.getStackSize());
            if (storedFluid != null && storedAmount > requestedAmount && inventory != null) {
                storedFluid = this.trimStoredFluidToNetwork(
                        inventory, storedFluids, slot, requestedAmount);
                storedAmount = storedFluid == null ? 0
                        : (int) Math.min(Integer.MAX_VALUE, storedFluid.getStackSize());
            }
            int amount = requestedAmount - storedAmount;
            if (amount <= 0) continue;

            IAEFluidStack extracted = inventory == null ? null
                    : this.extractFluidFromNetwork(markedFluid, amount);
            if (extracted != null && extracted.getStackSize() > 0) {
                int extractedAmount = (int) Math.min(Integer.MAX_VALUE, extracted.getStackSize());
                int newStoredAmount = Math.min(this.getVirtualStorageCapacity(), storedAmount + extractedAmount);
                FluidStack storedStack = extracted.getFluidStack().copy();
                storedStack.amount = newStoredAmount;
                storedFluids[slot] = appeng.fluids.util.AEFluidStack.fromFluidStack(storedStack);
                storedAmount = newStoredAmount;
                this.markDirty();
                this.saveChanges();
                this.markForUpdate();
                this.refreshFluidMonitor();
            }

            if (storedAmount < requestedAmount) {
                this.requestVirtualFluidCrafting(extended, slot, markedFluid, requestedAmount - storedAmount);
            }
        }
    }

    private void flushUnconfiguredFluidsToNetwork() {
        if (!this.getProxy().isActive()) return;

        IMEMonitor<IAEFluidStack> inventory = this.networkFluidHandler.getMonitor();
        if (inventory == null) return;

        this.flushUnconfiguredFluidsToNetwork(
                this.getConfig(), this.interfaceStoredFluids, inventory);
        this.flushUnconfiguredFluidsToNetwork(
                this.getExtendedConfig(), this.extendedStoredFluids, inventory);
    }

    private void flushUnconfiguredFluidsToNetwork(IItemHandler config,
            IAEFluidStack[] storedFluids, IMEInventory<IAEFluidStack> inventory) {
        boolean changed = false;
        for (int slot = 0; slot < storedFluids.length; slot++) {
            // A non-empty configuration slot owns the corresponding storage
            // slot. Only unconfigured slots behave as offline fluid tanks.
            if (!config.getStackInSlot(slot).isEmpty()) continue;

            IAEFluidStack stored = storedFluids[slot];
            if (stored == null || stored.getStackSize() <= 0) {
                if (stored != null) {
                    storedFluids[slot] = null;
                    changed = true;
                }
                continue;
            }

            IAEFluidStack remainder = this.insertFluidIntoNetwork(inventory, stored.copy());
            if (remainder == null || remainder.getStackSize() <= 0) {
                storedFluids[slot] = null;
                changed = true;
            } else if (remainder.getStackSize() != stored.getStackSize()) {
                storedFluids[slot] = remainder;
                changed = true;
            }
        }

        if (changed) {
            this.markDirty();
            this.saveChanges();
            this.markForUpdate();
            this.refreshFluidMonitor();
        }
    }

    private IAEFluidStack insertFluidIntoNetwork(IMEInventory<IAEFluidStack> inventory,
            IAEFluidStack fluid) {
        try {
            return Platform.poweredInsert(this.getProxy().getEnergy(), inventory, fluid,
                    this.fluidRequestSource, Actionable.MODULATE);
        } catch (GridAccessException e) {
            return fluid;
        }
    }

    private void returnStoredFluidToNetwork(IMEInventory<IAEFluidStack> inventory,
            IAEFluidStack[] storedFluids, int slot) {
        IAEFluidStack stored = storedFluids[slot];
        if (stored == null || stored.getStackSize() <= 0) {
            storedFluids[slot] = null;
            return;
        }

        IAEFluidStack remainder = this.insertFluidIntoNetwork(inventory, stored.copy());
        if (remainder == null || remainder.getStackSize() <= 0) {
            storedFluids[slot] = null;
        } else {
            storedFluids[slot] = remainder;
        }
        this.markDirty();
        this.saveChanges();
        this.markForUpdate();
        this.refreshFluidMonitor();
    }

    private IAEFluidStack trimStoredFluidToNetwork(IMEInventory<IAEFluidStack> inventory,
            IAEFluidStack[] storedFluids, int slot, int targetAmount) {
        IAEFluidStack stored = storedFluids[slot];
        if (stored == null || stored.getStackSize() <= targetAmount) return stored;

        long storedAmount = stored.getStackSize();
        IAEFluidStack excess = stored.copy();
        excess.setStackSize(storedAmount - targetAmount);
        IAEFluidStack remainder = this.insertFluidIntoNetwork(inventory, excess);
        long remainingExcess = remainder == null ? 0 : remainder.getStackSize();
        long newAmount = Math.min(this.getVirtualStorageCapacity(), targetAmount + remainingExcess);
        if (newAmount >= storedAmount) return stored;

        if (newAmount <= 0) {
            storedFluids[slot] = null;
        } else {
            FluidStack trimmed = stored.getFluidStack().copy();
            trimmed.amount = (int) newAmount;
            storedFluids[slot] = appeng.fluids.util.AEFluidStack.fromFluidStack(trimmed);
        }
        this.markDirty();
        this.saveChanges();
        this.markForUpdate();
        this.refreshFluidMonitor();
        return storedFluids[slot];
    }

    private boolean hasFluidWork() {
        return hasFluidWork(this.getInterfaceDuality(), false)
                || hasFluidWork(this.extendedDuality, true);
    }

    private boolean hasFluidWork(DualityInterface duality, boolean extended) {
        IItemHandler config = duality.getConfig();
        IAEFluidStack[] stored = extended ? this.extendedStoredFluids : this.interfaceStoredFluids;
        for (int slot = 0; slot < config.getSlots(); slot++) {
            if (!com.ae2utilix.item.ItemFluidMark.isFluidMark(config.getStackInSlot(slot))) continue;
            FluidStack wanted = this.getFluidConfig(extended, slot);
            if (wanted == null) return true;
            long storedAmount = stored[slot] == null ? 0 : stored[slot].getStackSize();
            long targetAmount = Math.min(this.getVirtualStorageCapacity(), Math.max(1, wanted.amount));
            if (storedAmount != targetAmount) return true;
        }
        return false;
    }

    private boolean hasItemConfig() {
        return hasItemConfig(this.getInterfaceDuality().getConfig())
                || hasItemConfig(this.extendedDuality.getConfig());
    }

    private boolean hasItemConfig(IItemHandler config) {
        for (int slot = 0; slot < config.getSlots(); slot++) {
            ItemStack stack = config.getStackInSlot(slot);
            if (!stack.isEmpty() && !com.ae2utilix.item.ItemFluidMark.isVirtualMark(stack)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasFluidConfig() {
        return hasFluidConfig(this.getInterfaceDuality().getConfig())
                || hasFluidConfig(this.extendedDuality.getConfig());
    }

    private boolean hasFluidConfig(IItemHandler config) {
        for (int slot = 0; slot < config.getSlots(); slot++) {
            if (com.ae2utilix.item.ItemFluidMark.isFluidMark(config.getStackInSlot(slot))) {
                return true;
            }
        }
        return false;
    }

    private boolean hasGasConfig() {
        return hasGasConfig(this.getConfig()) || hasGasConfig(this.getExtendedConfig());
    }

    private boolean hasGasConfig(IItemHandler config) {
        for (int slot = 0; slot < config.getSlots(); slot++) {
            if (com.ae2utilix.item.ItemFluidMark.isGasMark(config.getStackInSlot(slot))) {
                return true;
            }
        }
        return false;
    }

    private boolean hasEssentiaConfig() {
        for (boolean extended : new boolean[]{false, true}) {
            for (int slot = 0; slot < 9; slot++) {
                if (this.getEssentiaConfigAspect(extended, slot) != null) return true;
            }
        }
        return false;
    }

    private void wakeFluidRequests() {
        try {
            if (!this.getProxy().getTick().alertDevice(this.getProxy().getNode())) {
                this.getProxy().getTick().wakeDevice(this.getProxy().getNode());
            }
        } catch (GridAccessException ignored) {
        }
    }

    private IAEFluidStack extractFluidFromNetwork(FluidStack fluid, int amount) {
        if (fluid == null || amount <= 0 || !this.getProxy().isActive()) {
            return null;
        }

        IAEFluidStack request = appeng.fluids.util.AEFluidStack.fromFluidStack(fluid.copy());
        request.setStackSize(amount);
        return this.networkFluidHandler.extract(request, Actionable.MODULATE);
    }

    private void refreshFluidMonitor() {
        this.fluidMonitor.onTick();
    }

    @Override
    protected void writeToStream(ByteBuf data) throws IOException {
        super.writeToStream(data);
        this.writeFluidConfigStream(data, this.interfaceFluids, this.interfaceFluidAmounts);
        this.writeFluidConfigStream(data, this.extendedFluids, this.extendedFluidAmounts);
        this.writeFluidStateStream(data, this.interfaceStoredFluids);
        this.writeFluidStateStream(data, this.extendedStoredFluids);
        this.writeGasConfigStream(data, this.interfaceGases, this.interfaceGasAmounts);
        this.writeGasConfigStream(data, this.extendedGases, this.extendedGasAmounts);
        this.writeGasStateStream(data, this.interfaceStoredGases, this.interfaceStoredGasAmounts);
        this.writeGasStateStream(data, this.extendedStoredGases, this.extendedStoredGasAmounts);
        this.writeLongStateStream(data, this.interfaceManaConfigAmounts);
        this.writeLongStateStream(data, this.interfaceManaAmounts);
        this.writeLongStateStream(data, this.extendedManaConfigAmounts);
        this.writeLongStateStream(data, this.extendedManaAmounts);
        this.writeLongStateStream(data, this.interfaceFeConfigAmounts);
        this.writeLongStateStream(data, this.interfaceFeAmounts);
        this.writeLongStateStream(data, this.extendedFeConfigAmounts);
        this.writeLongStateStream(data, this.extendedFeAmounts);
        this.writeEssentiaConfigStream(data, this.interfaceEssentiaAspects, this.interfaceEssentiaConfigAmounts);
        this.writeEssentiaConfigStream(data, this.extendedEssentiaAspects, this.extendedEssentiaConfigAmounts);
        this.writeEssentiaConfigStream(data, this.interfaceStoredEssentiaAspects, this.interfaceStoredEssentiaAmounts);
        this.writeEssentiaConfigStream(data, this.extendedStoredEssentiaAspects, this.extendedStoredEssentiaAmounts);
    }

    @Override
    protected boolean readFromStream(ByteBuf data) throws IOException {
        boolean changed = super.readFromStream(data);
        changed |= this.readFluidConfigStream(data, this.interfaceFluids, this.interfaceFluidAmounts);
        changed |= this.readFluidConfigStream(data, this.extendedFluids, this.extendedFluidAmounts);
        changed |= this.readFluidStateStream(data, this.interfaceStoredFluids);
        changed |= this.readFluidStateStream(data, this.extendedStoredFluids);
        changed |= this.readGasConfigStream(data, this.interfaceGases, this.interfaceGasAmounts);
        changed |= this.readGasConfigStream(data, this.extendedGases, this.extendedGasAmounts);
        changed |= this.readGasStateStream(data, this.interfaceStoredGases, this.interfaceStoredGasAmounts);
        changed |= this.readGasStateStream(data, this.extendedStoredGases, this.extendedStoredGasAmounts);
        changed |= this.readLongStateStream(data, this.interfaceManaConfigAmounts);
        changed |= this.readLongStateStream(data, this.interfaceManaAmounts);
        changed |= this.readLongStateStream(data, this.extendedManaConfigAmounts);
        changed |= this.readLongStateStream(data, this.extendedManaAmounts);
        changed |= this.readLongStateStream(data, this.interfaceFeConfigAmounts);
        changed |= this.readLongStateStream(data, this.interfaceFeAmounts);
        changed |= this.readLongStateStream(data, this.extendedFeConfigAmounts);
        changed |= this.readLongStateStream(data, this.extendedFeAmounts);
        changed |= this.readEssentiaConfigStream(data, this.interfaceEssentiaAspects, this.interfaceEssentiaConfigAmounts);
        changed |= this.readEssentiaConfigStream(data, this.extendedEssentiaAspects, this.extendedEssentiaConfigAmounts);
        changed |= this.readEssentiaConfigStream(data, this.interfaceStoredEssentiaAspects, this.interfaceStoredEssentiaAmounts);
        changed |= this.readEssentiaConfigStream(data, this.extendedStoredEssentiaAspects, this.extendedStoredEssentiaAmounts);
        return changed;
    }

    private void writeFluidConfigStream(ByteBuf data, IAEFluidStack[] fluids, int[] amounts) throws IOException {
        for (int i = 0; i < fluids.length; i++) {
            IAEFluidStack fluid = fluids[i];
            data.writeBoolean(fluid != null);
            if (fluid != null) {
                IAEFluidStack copy = fluid.copy();
                copy.setStackSize(amounts[i]);
                copy.writeToPacket(data);
            }
        }
    }

    private boolean readFluidConfigStream(ByteBuf data, IAEFluidStack[] fluids, int[] amounts) throws IOException {
        boolean changed = false;
        for (int i = 0; i < fluids.length; i++) {
            boolean present = data.readBoolean();
            IAEFluidStack next = present ? appeng.fluids.util.AEFluidStack.fromPacket(data) : null;
            int nextAmount = next == null ? 0
                    : (int) Math.min(this.getVirtualStorageCapacity(), next.getStackSize());
            if (next != null) next.setStackSize(nextAmount);
            if (fluids[i] == null ? next != null : next == null || !fluids[i].equals(next)
                    || amounts[i] != nextAmount) {
                changed = true;
            }
            fluids[i] = next;
            amounts[i] = nextAmount;
        }
        return changed;
    }

    private void writeFluidStateStream(ByteBuf data, IAEFluidStack[] fluids) throws IOException {
        for (IAEFluidStack fluid : fluids) {
            data.writeBoolean(fluid != null);
            if (fluid != null) {
                fluid.writeToPacket(data);
            }
        }
    }

    private boolean readFluidStateStream(ByteBuf data, IAEFluidStack[] fluids) throws IOException {
        boolean changed = false;
        for (int i = 0; i < fluids.length; i++) {
            boolean present = data.readBoolean();
            IAEFluidStack next = present ? appeng.fluids.util.AEFluidStack.fromPacket(data) : null;
            if (next != null) {
                next.setStackSize(Math.min(this.getVirtualStorageCapacity(), next.getStackSize()));
            }
            IAEFluidStack previous = fluids[i];
            if (previous == null ? next != null : next == null
                    || !previous.equals(next) || previous.getStackSize() != next.getStackSize()) {
                changed = true;
            }
            fluids[i] = next;
        }
        return changed;
    }

    private void writeFluidState(NBTTagCompound data, String key, IAEFluidStack[] fluids, int[] amounts) {
        NBTTagCompound state = new NBTTagCompound();
        for (int i = 0; i < fluids.length; i++) {
            IAEFluidStack fluid = fluids[i];
            if (fluid == null) continue;
            IAEFluidStack copy = fluid.copy();
            if (amounts != null) copy.setStackSize(amounts[i]);
            NBTTagCompound slot = new NBTTagCompound();
            copy.writeToNBT(slot);
            state.setTag(String.valueOf(i), slot);
        }
        data.setTag("ae2utilix_fluid_" + key, state);
    }

    private void readFluidState(NBTTagCompound data, String key, IAEFluidStack[] fluids, int[] amounts) {
        NBTTagCompound state = data.getCompoundTag("ae2utilix_fluid_" + key);
        for (int i = 0; i < fluids.length; i++) {
            if (!state.hasKey(String.valueOf(i))) continue;
            IAEFluidStack fluid = appeng.fluids.util.AEFluidStack.fromNBT(state.getCompoundTag(String.valueOf(i)));
            fluid.setStackSize(Math.min(this.getVirtualStorageCapacity(), fluid.getStackSize()));
            fluids[i] = fluid;
            if (amounts != null) amounts[i] = (int) fluid.getStackSize();
        }
    }

    private void writeGasState(NBTTagCompound data, String key, String[] names, int[] amounts) {
        NBTTagCompound state = new NBTTagCompound();
        for (int i = 0; i < names.length; i++) {
            if (names[i] == null || names[i].isEmpty() || amounts[i] <= 0) continue;
            NBTTagCompound slot = new NBTTagCompound();
            slot.setString("Name", names[i]);
            slot.setInteger("Amount", amounts[i]);
            state.setTag(String.valueOf(i), slot);
        }
        data.setTag("ae2utilix_gas_" + key, state);
    }

    private void readGasState(NBTTagCompound data, String key, String[] names, int[] amounts) {
        NBTTagCompound state = data.getCompoundTag("ae2utilix_gas_" + key);
        for (int i = 0; i < names.length; i++) {
            names[i] = null;
            amounts[i] = 0;
            if (!state.hasKey(String.valueOf(i), 10)) continue;
            NBTTagCompound slot = state.getCompoundTag(String.valueOf(i));
            String name = slot.getString("Name");
            int amount = slot.getInteger("Amount");
            if (!name.isEmpty() && amount > 0) {
                names[i] = name;
                amounts[i] = Math.min(this.getVirtualStorageCapacity(), amount);
            }
        }
    }

    private void writeEssentiaState(NBTTagCompound data, String key, String[] aspects, int[] amounts) {
        NBTTagCompound state = new NBTTagCompound();
        for (int i = 0; i < aspects.length; i++) {
            if (aspects[i] == null || aspects[i].isEmpty() || amounts[i] <= 0) continue;
            NBTTagCompound slot = new NBTTagCompound();
            slot.setString("Aspect", aspects[i]);
            slot.setInteger("Amount", amounts[i]);
            state.setTag(String.valueOf(i), slot);
        }
        data.setTag("ae2utilix_essentia_" + key, state);
    }

    private void readEssentiaState(NBTTagCompound data, String key, String[] aspects, int[] amounts) {
        NBTTagCompound state = data.getCompoundTag("ae2utilix_essentia_" + key);
        for (int i = 0; i < aspects.length; i++) {
            aspects[i] = null;
            amounts[i] = 0;
            if (!state.hasKey(String.valueOf(i), 10)) continue;
            NBTTagCompound slot = state.getCompoundTag(String.valueOf(i));
            String aspect = slot.getString("Aspect");
            int amount = slot.getInteger("Amount");
            if (!aspect.isEmpty() && amount > 0) {
                aspects[i] = aspect;
                amounts[i] = Math.min(this.getVirtualStorageCapacity(), amount);
            }
        }
    }

    private void writeGasConfigStream(ByteBuf data, String[] names, int[] amounts) {
        for (int i = 0; i < names.length; i++) {
            boolean present = names[i] != null && !names[i].isEmpty();
            data.writeBoolean(present);
            if (present) {
                ByteBufUtils.writeUTF8String(data, names[i]);
                data.writeInt(amounts[i]);
            }
        }
    }

    private boolean readGasConfigStream(ByteBuf data, String[] names, int[] amounts) {
        boolean changed = false;
        for (int i = 0; i < names.length; i++) {
            boolean present = data.readBoolean();
            String nextName = present ? ByteBufUtils.readUTF8String(data) : null;
            int nextAmount = present
                    ? Math.max(0, Math.min(this.getVirtualStorageCapacity(), data.readInt())) : 0;
            if (names[i] == null ? nextName != null : !names[i].equals(nextName)
                    || amounts[i] != nextAmount) {
                changed = true;
            }
            names[i] = nextName;
            amounts[i] = nextAmount;
        }
        return changed;
    }

    private void writeGasStateStream(ByteBuf data, String[] names, int[] amounts) {
        this.writeGasConfigStream(data, names, amounts);
    }

    private boolean readGasStateStream(ByteBuf data, String[] names, int[] amounts) {
        return this.readGasConfigStream(data, names, amounts);
    }

    private void writeEssentiaConfigStream(ByteBuf data, String[] aspects, int[] amounts) {
        for (int i = 0; i < aspects.length; i++) {
            boolean present = aspects[i] != null && !aspects[i].isEmpty();
            data.writeBoolean(present);
            if (present) {
                ByteBufUtils.writeUTF8String(data, aspects[i]);
                data.writeInt(amounts[i]);
            }
        }
    }

    private boolean readEssentiaConfigStream(ByteBuf data, String[] aspects, int[] amounts) {
        boolean changed = false;
        for (int i = 0; i < aspects.length; i++) {
            boolean present = data.readBoolean();
            String nextAspect = present ? ByteBufUtils.readUTF8String(data) : null;
            int nextAmount = present
                    ? Math.max(0, Math.min(this.getVirtualStorageCapacity(), data.readInt())) : 0;
            if (aspects[i] == null ? nextAspect != null : !aspects[i].equals(nextAspect)
                    || amounts[i] != nextAmount) changed = true;
            aspects[i] = nextAspect;
            amounts[i] = nextAmount;
        }
        return changed;
    }

    private void writeLongState(NBTTagCompound data, String key, long[] values) {
        NBTTagCompound state = new NBTTagCompound();
        for (int i = 0; i < values.length; i++) {
            if (values[i] > 0) state.setLong(String.valueOf(i), values[i]);
        }
        data.setTag("ae2utilix_energy_" + key, state);
    }

    private void readLongState(NBTTagCompound data, String key, long[] values) {
        NBTTagCompound state = data.getCompoundTag("ae2utilix_energy_" + key);
        for (int i = 0; i < values.length; i++) {
            values[i] = Math.max(0, Math.min(this.getVirtualStorageCapacity(), state.getLong(String.valueOf(i))));
        }
    }

    private void readEnergyState(NBTTagCompound data, String side, boolean extended, int type,
            long[] configValues, long[] storedValues) {
        String name = type == BotaniaFluxIntegration.MANA ? "mana" : "fe";
        String prefix = "ae2utilix_energy_" + side + "_" + name;
        boolean hasConfig = data.hasKey(prefix + "_config");
        boolean hasStored = data.hasKey(prefix + "_stored");
        if (hasConfig || hasStored) {
            this.readLongState(data, side + "_" + name + "_config", configValues);
            this.readLongState(data, side + "_" + name + "_stored", storedValues);
            return;
        }

        String legacyKey = side + "_" + name;
        if (!data.hasKey("ae2utilix_energy_" + legacyKey)) return;

        long[] legacyValues = new long[storedValues.length];
        this.readLongState(data, legacyKey, legacyValues);
        IItemHandler config = extended ? this.extendedDuality.getConfig() : this.interfaceDuality.getConfig();
        for (int slot = 0; slot < legacyValues.length; slot++) {
            ItemStack marker = config.getStackInSlot(slot);
            boolean marked = type == BotaniaFluxIntegration.MANA
                    ? com.ae2utilix.item.ItemFluidMark.isManaMark(marker)
                    : com.ae2utilix.item.ItemFluidMark.isFeMark(marker);
            if (marked) configValues[slot] = legacyValues[slot];
            else storedValues[slot] = legacyValues[slot];
        }
    }

    private void writeLongStateStream(ByteBuf data, long[] values) {
        for (long value : values) data.writeLong(value);
    }

    private boolean readLongStateStream(ByteBuf data, long[] values) {
        boolean changed = false;
        for (int i = 0; i < values.length; i++) {
            long next = Math.max(0, Math.min(this.getVirtualStorageCapacity(), data.readLong()));
            if (values[i] != next) changed = true;
            values[i] = next;
        }
        return changed;
    }

    @Override
    public IFluidTankProperties[] getTankProperties() {
        java.util.List<IFluidTankProperties> properties = new java.util.ArrayList<>();
        addTankProperties(properties, this.getConfig(), interfaceStoredFluids);
        addTankProperties(properties, this.getExtendedConfig(), extendedStoredFluids);
        IFluidTankProperties[] networkProperties = this.networkFluidHandler.getTankProperties();
        if (networkProperties != null) {
            java.util.Collections.addAll(properties, networkProperties);
        }
        return properties.toArray(new IFluidTankProperties[0]);
    }

    private void addTankProperties(java.util.List<IFluidTankProperties> properties,
            IItemHandler config, IAEFluidStack[] storedFluids) {
        for (int i = 0; i < storedFluids.length; i++) {
            ItemStack configStack = config.getStackInSlot(i);
            if (!configStack.isEmpty()
                    && !com.ae2utilix.item.ItemFluidMark.isFluidMark(configStack)) continue;
            IAEFluidStack stored = storedFluids[i];
            FluidStack fluid = stored == null ? null : stored.getFluidStack();
            properties.add(new FluidTankProperties(fluid, this.getVirtualStorageCapacity(), true, true));
        }
    }

    @Override
    public int fill(FluidStack resource, boolean doFill) {
        if (resource == null || resource.amount <= 0) return 0;

        // Treat the interface as a network-facing fluid handler first. This
        // lets external fluid logistics inject into the ME network whenever
        // the network has room, while retaining configured slots as a local
        // overflow buffer when the network rejects part of the stack.
        int networkFilled = this.networkFluidHandler.fill(resource, doFill);
        if (networkFilled >= resource.amount) {
            return resource.amount;
        }

        int remainingAmount = resource.amount - networkFilled;
        FluidStack remaining = resource.copy();
        remaining.amount = remainingAmount;

        int localFilled = fillLocal(remaining, doFill);
        return networkFilled + localFilled;
    }

    /** Fills only the fluid buffers owned by this interface. */
    public int fillLocal(FluidStack resource, boolean doFill) {
        if (resource == null || resource.amount <= 0) return 0;
        int localFilled = fillLocal(resource, doFill, false);
        if (localFilled < resource.amount) {
            FluidStack extendedRemaining = resource.copy();
            extendedRemaining.amount = resource.amount - localFilled;
            localFilled += fillLocal(extendedRemaining, doFill, true);
        }
        return localFilled;
    }

    private int fillLocal(FluidStack resource, boolean doFill, boolean extended) {
        IItemHandler config = extended ? this.getExtendedConfig() : this.getConfig();
        IAEFluidStack[] storedFluids = extended ? extendedStoredFluids : interfaceStoredFluids;
        boolean[] used = new boolean[storedFluids.length];
        int total = 0;
        FluidStack remaining = resource.copy();

        // Fill existing matching marked tanks first. Each slot is independent,
        // so a full tank does not prevent a later slot from holding the same fluid.
        for (int i = 0; i < storedFluids.length && remaining.amount > 0; i++) {
            if (!this.canStoreFluidInSlot(extended, i)) continue;
            ItemStack configStack = config.getStackInSlot(i);
            if (!com.ae2utilix.item.ItemFluidMark.isFluidMark(configStack)) continue;
            IAEFluidStack stored = storedFluids[i];
            if (stored == null || !stored.getFluidStack().isFluidEqual(resource)) continue;
            used[i] = true;
            int accepted = fillLocalSlot(storedFluids, i, remaining, doFill, extended);
            total += accepted;
            remaining.amount -= accepted;
        }

        // Preserve the marker's priority over an unconfigured offline tank.
        for (int i = 0; i < storedFluids.length && remaining.amount > 0; i++) {
            if (used[i]) continue;
            if (!this.canStoreFluidInSlot(extended, i)) continue;
            ItemStack configStack = config.getStackInSlot(i);
            if (!configStack.isEmpty() || storedFluids[i] == null
                    || !storedFluids[i].getFluidStack().isFluidEqual(resource)) continue;
            used[i] = true;
            int accepted = fillLocalSlot(storedFluids, i, remaining, doFill, extended);
            total += accepted;
            remaining.amount -= accepted;
        }

        // Then use empty slots explicitly marked for this fluid, including
        // multiple markers for the same fluid in one interface group.
        for (int i = 0; i < storedFluids.length && remaining.amount > 0; i++) {
            if (used[i]) continue;
            if (!this.canStoreFluidInSlot(extended, i)) continue;
            ItemStack configStack = config.getStackInSlot(i);
            if (!com.ae2utilix.item.ItemFluidMark.isFluidMark(configStack)) continue;
            FluidStack marked = com.ae2utilix.item.ItemFluidMark.getFluid(configStack);
            if (marked == null || !marked.isFluidEqual(resource) || storedFluids[i] != null) continue;
            used[i] = true;
            int accepted = fillLocalSlot(storedFluids, i, remaining, doFill, extended);
            total += accepted;
            remaining.amount -= accepted;
        }

        // Finally, an empty configuration slot behaves as an unrestricted
        // offline tank and can hold any fluid type.
        for (int i = 0; i < storedFluids.length && remaining.amount > 0; i++) {
            if (used[i]) continue;
            if (!this.canStoreFluidInSlot(extended, i)) continue;
            ItemStack configStack = config.getStackInSlot(i);
            if (!configStack.isEmpty() || storedFluids[i] != null) continue;
            used[i] = true;
            int accepted = fillLocalSlot(storedFluids, i, remaining, doFill, extended);
            total += accepted;
            remaining.amount -= accepted;
        }

        return total;
    }

    private int fillLocalSlot(IAEFluidStack[] storedFluids, int slot,
            FluidStack resource, boolean doFill, boolean extended) {
        if (!this.canStoreFluidInSlot(extended, slot)) return 0;
        IAEFluidStack stored = storedFluids[slot];
        int current = stored == null ? 0 : (int) stored.getStackSize();
        int accepted = Math.min(resource.amount,
                Math.max(0, this.getVirtualStorageCapacity() - current));
        if (doFill && accepted > 0) {
            FluidStack storedStack = resource.copy();
            storedStack.amount = current + accepted;
            storedFluids[slot] = appeng.fluids.util.AEFluidStack.fromFluidStack(storedStack);
            markDirty();
            saveChanges();
            markForUpdate();
            refreshFluidMonitor();
            wakeFluidRequests();
        }
        return Math.max(0, accepted);
    }

    @Override
    public FluidStack drain(int maxDrain, boolean doDrain) {
        FluidStack local = this.drainLocal(maxDrain, doDrain);
        return local == null ? this.networkFluidHandler.drain(maxDrain, doDrain) : local;
    }

    /**
     * Drains only the fluid buffers owned by this interface. This is used by
     * adjacent automation that must not tunnel through the interface into its
     * ME network.
     */
    public FluidStack drainLocal(int maxDrain, boolean doDrain) {
        for (int i = 0; i < interfaceStoredFluids.length; i++) {
            IAEFluidStack stored = interfaceStoredFluids[i];
            if (stored != null && stored.getStackSize() > 0) return drain(false, i, maxDrain, doDrain);
        }
        for (int i = 0; i < extendedStoredFluids.length; i++) {
            IAEFluidStack stored = extendedStoredFluids[i];
            if (stored != null && stored.getStackSize() > 0) return drain(true, i, maxDrain, doDrain);
        }
        return null;
    }

    @Override
    public FluidStack drain(FluidStack resource, boolean doDrain) {
        if (resource == null) return null;
        FluidStack local = this.drainLocal(resource, doDrain);
        return local == null ? this.networkFluidHandler.drain(resource, doDrain) : local;
    }

    /** Drains only local fluid buffers for the requested fluid type. */
    public FluidStack drainLocal(FluidStack resource, boolean doDrain) {
        if (resource == null) return null;
        for (int i = 0; i < interfaceStoredFluids.length; i++) {
            IAEFluidStack stored = interfaceStoredFluids[i];
            FluidStack storedStack = stored == null ? null : stored.getFluidStack();
            if (storedStack != null && stored.getStackSize() > 0 && storedStack.isFluidEqual(resource)) {
                return drain(false, i, resource.amount, doDrain);
            }
        }
        for (int i = 0; i < extendedStoredFluids.length; i++) {
            IAEFluidStack stored = extendedStoredFluids[i];
            FluidStack storedStack = stored == null ? null : stored.getFluidStack();
            if (storedStack != null && stored.getStackSize() > 0 && storedStack.isFluidEqual(resource)) {
                return drain(true, i, resource.amount, doDrain);
            }
        }
        return null;
    }

    private FluidStack drain(boolean extended, int slot, int amount, boolean doDrain) {
        IAEFluidStack[] storedFluids = extended ? extendedStoredFluids : interfaceStoredFluids;
        IAEFluidStack stored = storedFluids[slot];
        if (stored == null) return null;
        int drained = Math.min(amount, (int) stored.getStackSize());
        FluidStack result = stored.getFluidStack();
        result.amount = drained;
        if (doDrain) {
            int left = (int) stored.getStackSize() - result.amount;
            if (left <= 0) {
                storedFluids[slot] = null;
            } else {
                FluidStack remaining = stored.getFluidStack();
                remaining.amount = left;
                storedFluids[slot] = appeng.fluids.util.AEFluidStack.fromFluidStack(remaining);
            }
            saveChanges();
            markForUpdate();
            refreshFluidMonitor();
            wakeFluidRequests();
        }
        return result;
    }

    private boolean returnFluidToNetwork(FluidStack fluid) {
        try {
            IStorageGrid grid = this.getProxy().getStorage();
            IMEInventory<IAEFluidStack> inventory = grid.getInventory(
                    AEApi.instance().storage().getStorageChannel(IFluidStorageChannel.class));
            if (inventory == null) return false;
            IAEFluidStack aeFluid = appeng.fluids.util.AEFluidStack.fromFluidStack(fluid);
            IAEFluidStack remainder = inventory.injectItems(aeFluid, Actionable.MODULATE, new MachineSource(this));
            return remainder == null || remainder.getStackSize() <= 0;
        } catch (GridAccessException e) {
            return false;
        }
    }

    @Override
    public ItemStack getItemStackRepresentation() {
        return new ItemStack(com.ae2utilix.AE2Utilix.BLOCK_COMMON_INTERFACE_ALTERNATE);
    }

    @Override
    public String getCustomInventoryName() {
        return new ItemStack(com.ae2utilix.AE2Utilix.BLOCK_COMMON_INTERFACE_ALTERNATE).getDisplayName();
    }

    @Override
    public void getDrops(net.minecraft.world.World world,
            net.minecraft.util.math.BlockPos pos, List<ItemStack> drops) {
        this.interfaceDuality.addDrops(drops);
        this.extendedDuality.addDrops(drops);
    }
}
