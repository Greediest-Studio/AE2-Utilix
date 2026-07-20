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
import com.google.common.collect.ImmutableSet;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import net.minecraftforge.fluids.capability.FluidTankProperties;
import net.minecraftforge.common.capabilities.Capability;
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

    private static final int FLUID_CAPACITY = 512000;
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
    private final MEMonitorIFluidHandler fluidMonitor = new MEMonitorIFluidHandler(this);
    private final com.ae2utilix.integration.NetworkStorageItemHandler networkItemHandler =
            new com.ae2utilix.integration.NetworkStorageItemHandler(this.getProxy(), this);
    private final com.ae2utilix.integration.NetworkStorageFluidHandler networkFluidHandler =
            new com.ae2utilix.integration.NetworkStorageFluidHandler(this.getProxy(), this);
    private final IActionSource fluidRequestSource = new MachineSource(this);
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
    }

    public DualityInterface getInterfaceDuality() {
        return this.interfaceDuality;
    }

    public IItemHandler getConfig() {
        return this.interfaceDuality.getConfig();
    }

    public IItemHandler getStorage() {
        return this.interfaceDuality.getStorage();
    }

    public IItemHandler getExtendedConfig() {
        return this.extendedDuality.getConfig();
    }

    public IItemHandler getExtendedStorage() {
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
        this.readFluidState(data, "interface", this.interfaceFluids, this.interfaceFluidAmounts);
        this.readFluidState(data, "extended", this.extendedFluids, this.extendedFluidAmounts);
        this.readFluidState(data, "interface_stored", this.interfaceStoredFluids, null);
        this.readFluidState(data, "extended_stored", this.extendedStoredFluids, null);
        this.readGasState(data, "interface", this.interfaceGases, this.interfaceGasAmounts);
        this.readGasState(data, "extended", this.extendedGases, this.extendedGasAmounts);
        this.readGasState(data, "interface_stored", this.interfaceStoredGases, this.interfaceStoredGasAmounts);
        this.readGasState(data, "extended_stored", this.extendedStoredGases, this.extendedStoredGasAmounts);
        if (data.hasKey(NBT_LINK_DIM)) {
            this.linkDim = data.getInteger(NBT_LINK_DIM);
            this.linkPos = new net.minecraft.util.math.BlockPos(
                    data.getInteger(NBT_LINK_X), data.getInteger(NBT_LINK_Y), data.getInteger(NBT_LINK_Z));
            int ordinal = data.getInteger(NBT_LINK_FACE);
            this.linkFace = ordinal >= 0 && ordinal < EnumFacing.values().length
                    ? EnumFacing.values()[ordinal] : null;
        }
    }

    public void setFluidConfig(boolean extended, int slot, FluidStack fluid) {
        IAEFluidStack[] fluids = extended ? this.extendedFluids : this.interfaceFluids;
        int[] amounts = extended ? this.extendedFluidAmounts : this.interfaceFluidAmounts;
        String[] gases = extended ? this.extendedGases : this.interfaceGases;
        int[] gasAmounts = extended ? this.extendedGasAmounts : this.interfaceGasAmounts;
        fluids[slot] = fluid == null ? null : appeng.fluids.util.AEFluidStack.fromFluidStack(fluid);
        amounts[slot] = fluid == null ? 0 : fluid.amount;
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
        gasAmounts[slot] = gases[slot] == null ? 0 : Math.max(1, Math.min(FLUID_CAPACITY, amount));
        this.markDirty();
        this.saveChanges();
        this.markForUpdate();
        this.wakeFluidRequests();
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
        return amounts[slot] <= 0 ? 1000 : amounts[slot];
    }

    public String getStoredGasName(boolean extended, int slot) {
        return (extended ? this.extendedStoredGases : this.interfaceStoredGases)[slot];
    }

    public int getStoredGasAmount(boolean extended, int slot) {
        return (extended ? this.extendedStoredGasAmounts : this.interfaceStoredGasAmounts)[slot];
    }

    public void setStoredGas(boolean extended, int slot, String gasName, int amount) {
        String[] names = extended ? this.extendedStoredGases : this.interfaceStoredGases;
        int[] amounts = extended ? this.extendedStoredGasAmounts : this.interfaceStoredGasAmounts;
        if (gasName == null || gasName.isEmpty() || amount <= 0) {
            names[slot] = null;
            amounts[slot] = 0;
        } else {
            names[slot] = gasName;
            amounts[slot] = Math.min(FLUID_CAPACITY, amount);
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

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY
                || capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY
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
                        && !com.ae2utilix.integration.MekanismEnergisticsIntegration.hasGasWork(this), true);
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
        this.requestMarkedFluids(this.getInterfaceDuality());
        this.requestMarkedFluids(this.extendedDuality);
        com.ae2utilix.integration.MekanismEnergisticsIntegration.flushUnconfiguredGasesToNetwork(this);
        com.ae2utilix.integration.MekanismEnergisticsIntegration.requestMarkedGases(this, false);
        com.ae2utilix.integration.MekanismEnergisticsIntegration.requestMarkedGases(this, true);
        TickRateModulation primary = this.interfaceDuality.tickingRequest(node, ticksSinceLastCall);
        TickRateModulation extended = this.extendedDuality.tickingRequest(node, ticksSinceLastCall);
        if (primary == TickRateModulation.URGENT || extended == TickRateModulation.URGENT) return TickRateModulation.URGENT;
        if (primary == TickRateModulation.FASTER || extended == TickRateModulation.FASTER) return TickRateModulation.FASTER;
        if (primary == TickRateModulation.SLOWER || extended == TickRateModulation.SLOWER) return TickRateModulation.SLOWER;
        if (primary == TickRateModulation.SLEEP && extended == TickRateModulation.SLEEP) {
            return this.hasFluidWork() ? TickRateModulation.SLOWER : TickRateModulation.SLEEP;
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
    }

    @Override
    public boolean pushPattern(ICraftingPatternDetails details, net.minecraft.inventory.InventoryCrafting table) {
        return this.interfaceDuality.pushPattern(details, table);
    }

    @Override
    public boolean isBusy() {
        return this.interfaceDuality.isBusy();
    }

    @Override
    public ImmutableSet<ICraftingLink> getRequestedJobs() {
        return this.interfaceDuality.getRequestedJobs();
    }

    @Override
    public IAEItemStack injectCraftedItems(ICraftingLink link, IAEItemStack stack, Actionable mode) {
        return this.interfaceDuality.injectCraftedItems(link, stack, mode);
    }

    @Override
    public void jobStateChange(ICraftingLink link) {
        this.interfaceDuality.jobStateChange(link);
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
        if (inventory == null) return;

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
                this.returnStoredFluidToNetwork(inventory, storedFluids, slot);
                storedFluid = storedFluids[slot];
            }

            int requestedAmount = Math.min(FLUID_CAPACITY, Math.max(1, configuredFluid.amount));
            int storedAmount = storedFluid == null ? 0
                    : (int) Math.min(Integer.MAX_VALUE, storedFluid.getStackSize());
            int amount = requestedAmount - storedAmount;
            if (amount <= 0) continue;

            IAEFluidStack extracted = this.extractFluidFromNetwork(markedFluid, amount);
            if (extracted == null || extracted.getStackSize() <= 0) continue;

            int extractedAmount = (int) Math.min(Integer.MAX_VALUE, extracted.getStackSize());
            int newStoredAmount = Math.min(FLUID_CAPACITY, storedAmount + extractedAmount);
            FluidStack storedStack = extracted.getFluidStack().copy();
            storedStack.amount = newStoredAmount;
            storedFluids[slot] = appeng.fluids.util.AEFluidStack.fromFluidStack(storedStack);
            this.markDirty();
            this.saveChanges();
            this.markForUpdate();
            this.refreshFluidMonitor();
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
            long targetAmount = Math.min(FLUID_CAPACITY, Math.max(1, wanted.amount));
            if (storedAmount < targetAmount) return true;
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
            int nextAmount = next == null ? 0 : (int) next.getStackSize();
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
                amounts[i] = Math.min(FLUID_CAPACITY, amount);
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
            int nextAmount = present ? data.readInt() : 0;
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
            properties.add(new FluidTankProperties(fluid, FLUID_CAPACITY, true, true));
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

        int localFilled = fillLocal(remaining, doFill, false);
        if (localFilled < remainingAmount) {
            FluidStack extendedRemaining = remaining.copy();
            extendedRemaining.amount = remainingAmount - localFilled;
            localFilled += fillLocal(extendedRemaining, doFill, true);
        }
        return networkFilled + localFilled;
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
            ItemStack configStack = config.getStackInSlot(i);
            if (!com.ae2utilix.item.ItemFluidMark.isFluidMark(configStack)) continue;
            IAEFluidStack stored = storedFluids[i];
            if (stored == null || !stored.getFluidStack().isFluidEqual(resource)) continue;
            used[i] = true;
            int accepted = fillLocalSlot(storedFluids, i, remaining, doFill);
            total += accepted;
            remaining.amount -= accepted;
        }

        // Preserve the marker's priority over an unconfigured offline tank.
        for (int i = 0; i < storedFluids.length && remaining.amount > 0; i++) {
            if (used[i]) continue;
            ItemStack configStack = config.getStackInSlot(i);
            if (!configStack.isEmpty() || storedFluids[i] == null
                    || !storedFluids[i].getFluidStack().isFluidEqual(resource)) continue;
            used[i] = true;
            int accepted = fillLocalSlot(storedFluids, i, remaining, doFill);
            total += accepted;
            remaining.amount -= accepted;
        }

        // Then use empty slots explicitly marked for this fluid, including
        // multiple markers for the same fluid in one interface group.
        for (int i = 0; i < storedFluids.length && remaining.amount > 0; i++) {
            if (used[i]) continue;
            ItemStack configStack = config.getStackInSlot(i);
            if (!com.ae2utilix.item.ItemFluidMark.isFluidMark(configStack)) continue;
            FluidStack marked = com.ae2utilix.item.ItemFluidMark.getFluid(configStack);
            if (marked == null || !marked.isFluidEqual(resource) || storedFluids[i] != null) continue;
            used[i] = true;
            int accepted = fillLocalSlot(storedFluids, i, remaining, doFill);
            total += accepted;
            remaining.amount -= accepted;
        }

        // Finally, an empty configuration slot behaves as an unrestricted
        // offline tank and can hold any fluid type.
        for (int i = 0; i < storedFluids.length && remaining.amount > 0; i++) {
            if (used[i]) continue;
            ItemStack configStack = config.getStackInSlot(i);
            if (!configStack.isEmpty() || storedFluids[i] != null) continue;
            used[i] = true;
            int accepted = fillLocalSlot(storedFluids, i, remaining, doFill);
            total += accepted;
            remaining.amount -= accepted;
        }

        return total;
    }

    private int fillLocalSlot(IAEFluidStack[] storedFluids, int slot,
            FluidStack resource, boolean doFill) {
        IAEFluidStack stored = storedFluids[slot];
        int current = stored == null ? 0 : (int) stored.getStackSize();
        int accepted = Math.min(resource.amount, Math.max(0, FLUID_CAPACITY - current));
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
        for (int i = 0; i < interfaceStoredFluids.length; i++) {
            if (interfaceStoredFluids[i] != null) return drain(false, i, maxDrain, doDrain);
        }
        for (int i = 0; i < extendedStoredFluids.length; i++) {
            if (extendedStoredFluids[i] != null) return drain(true, i, maxDrain, doDrain);
        }
        return this.networkFluidHandler.drain(maxDrain, doDrain);
    }

    @Override
    public FluidStack drain(FluidStack resource, boolean doDrain) {
        if (resource == null) return null;
        for (int i = 0; i < interfaceStoredFluids.length; i++) {
            IAEFluidStack stored = interfaceStoredFluids[i];
            if (stored != null && stored.getFluidStack().isFluidEqual(resource)) return drain(false, i, resource.amount, doDrain);
        }
        for (int i = 0; i < extendedStoredFluids.length; i++) {
            IAEFluidStack stored = extendedStoredFluids[i];
            if (stored != null && stored.getFluidStack().isFluidEqual(resource)) return drain(true, i, resource.amount, doDrain);
        }
        return this.networkFluidHandler.drain(resource, doDrain);
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
}
