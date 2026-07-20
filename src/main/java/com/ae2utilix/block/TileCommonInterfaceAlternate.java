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
                this.requestMarkedFluids(this.getInterfaceDuality());
                this.requestMarkedFluids(this.extendedDuality);
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
        IAEFluidStack[] storedFluids = extended ? this.extendedStoredFluids : this.interfaceStoredFluids;
        int[] amounts = extended ? this.extendedFluidAmounts : this.interfaceFluidAmounts;
        FluidStack previous = fluids[slot] == null ? null : fluids[slot].getFluidStack();
        if (previous == null || fluid == null || !previous.isFluidEqual(fluid)) {
            storedFluids[slot] = null;
        }
        fluids[slot] = fluid == null ? null : appeng.fluids.util.AEFluidStack.fromFluidStack(fluid);
        amounts[slot] = fluid == null ? 0 : fluid.amount;
        this.markDirty();
        this.saveChanges();
        this.markForUpdate();
        this.refreshFluidMonitor();
        this.wakeFluidRequests();
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

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY
                || capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY
                || capability == Capabilities.STORAGE_MONITORABLE_ACCESSOR
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
                primary.isSleeping && extended.isSleeping && !this.hasFluidWork(), true);
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
        this.requestMarkedFluids(this.getInterfaceDuality());
        this.requestMarkedFluids(this.extendedDuality);
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

    private void returnStoredFluidToNetwork(IMEInventory<IAEFluidStack> inventory,
            IAEFluidStack[] storedFluids, int slot) {
        IAEFluidStack stored = storedFluids[slot];
        if (stored == null || stored.getStackSize() <= 0) {
            storedFluids[slot] = null;
            return;
        }

        IAEFluidStack remainder = inventory.injectItems(stored.copy(), Actionable.MODULATE, this.fluidRequestSource);
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
            if (!stack.isEmpty() && !com.ae2utilix.item.ItemFluidMark.isFluidMark(stack)) {
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

        try {
            IStorageGrid storage = this.getProxy().getStorage();
            IMEInventory<IAEFluidStack> inventory = storage.getInventory(
                    AEApi.instance().storage().getStorageChannel(IFluidStorageChannel.class));
            if (inventory == null) {
                return null;
            }

            IAEFluidStack request = appeng.fluids.util.AEFluidStack.fromFluidStack(fluid.copy());
            request.setStackSize(amount);
            return Platform.poweredExtraction(this.getProxy().getEnergy(), inventory, request,
                    this.fluidRequestSource, Actionable.MODULATE);
        } catch (GridAccessException ignored) {
            return null;
        }
    }

    private void refreshFluidMonitor() {
        this.fluidMonitor.onTick();
    }

    @Override
    protected void writeToStream(ByteBuf data) throws IOException {
        super.writeToStream(data);
        this.writeFluidConfigStream(data, this.interfaceFluids, this.interfaceFluidAmounts);
        this.writeFluidConfigStream(data, this.extendedFluids, this.extendedFluidAmounts);
    }

    @Override
    protected boolean readFromStream(ByteBuf data) throws IOException {
        boolean changed = super.readFromStream(data);
        changed |= this.readFluidConfigStream(data, this.interfaceFluids, this.interfaceFluidAmounts);
        changed |= this.readFluidConfigStream(data, this.extendedFluids, this.extendedFluidAmounts);
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

    @Override
    public IFluidTankProperties[] getTankProperties() {
        java.util.List<IFluidTankProperties> properties = new java.util.ArrayList<>();
        addTankProperties(properties, interfaceFluids, interfaceStoredFluids);
        addTankProperties(properties, extendedFluids, extendedStoredFluids);
        IFluidTankProperties[] networkProperties = this.networkFluidHandler.getTankProperties();
        if (networkProperties != null) {
            java.util.Collections.addAll(properties, networkProperties);
        }
        return properties.toArray(new IFluidTankProperties[0]);
    }

    private void addTankProperties(java.util.List<IFluidTankProperties> properties,
            IAEFluidStack[] configuredFluids, IAEFluidStack[] storedFluids) {
        for (int i = 0; i < configuredFluids.length; i++) {
            if (configuredFluids[i] == null) continue;
            IAEFluidStack stored = storedFluids[i];
            FluidStack fluid = stored == null ? null : stored.getFluidStack();
            properties.add(new FluidTankProperties(fluid, FLUID_CAPACITY, true, true));
        }
    }

    @Override
    public int fill(FluidStack resource, boolean doFill) {
        if (resource == null || resource.amount <= 0) return 0;
        int filled = fillConfigured(resource, doFill, false);
        if (filled > 0) return filled;
        filled = fillConfigured(resource, doFill, true);
        return filled > 0 ? filled : this.networkFluidHandler.fill(resource, doFill);
    }

    private int fillConfigured(FluidStack resource, boolean doFill, boolean extended) {
        IAEFluidStack[] storedFluids = extended ? extendedStoredFluids : interfaceStoredFluids;
        for (int i = 0; i < storedFluids.length; i++) {
            FluidStack configured = getFluidConfig(extended, i);
            if (configured != null && configured.isFluidEqual(resource)) {
                IAEFluidStack stored = storedFluids[i];
                int current = stored == null ? 0 : (int) stored.getStackSize();
                int accepted = Math.min(resource.amount, FLUID_CAPACITY - current);
                if (doFill && accepted > 0) {
                    FluidStack storedStack = resource.copy();
                    storedStack.amount = current + accepted;
                    storedFluids[i] = appeng.fluids.util.AEFluidStack.fromFluidStack(storedStack);
                    markDirty();
                    saveChanges();
                    markForUpdate();
                    refreshFluidMonitor();
                    wakeFluidRequests();
                }
                return Math.max(0, accepted);
            }
        }
        return 0;
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
