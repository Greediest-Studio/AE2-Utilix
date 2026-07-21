package com.ae2utilix.parts;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.networking.IGridNode;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.parts.IPart;
import appeng.api.parts.IPartCollisionHelper;
import appeng.api.parts.IPartModel;
import appeng.api.storage.IMEInventory;
import appeng.api.storage.channels.IFluidStorageChannel;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.util.AECableType;
import appeng.api.util.AEPartLocation;
import appeng.me.GridAccessException;
import appeng.me.helpers.MachineSource;
import appeng.parts.PartModel;
import appeng.parts.automation.PartUpgradeable;
import appeng.tile.inventory.AppEngInternalAEInventory;
import appeng.util.InventoryAdaptor;
import appeng.util.Platform;
import appeng.util.item.AEItemStack;
import com.ae2utilix.AE2Utilix;
import com.ae2utilix.integration.BotaniaFluxIntegration;
import com.ae2utilix.integration.MekanismEnergisticsIntegration;
import com.ae2utilix.item.ItemFluidMark;
import com.flux_applied.ae2.FluxStack;
import com.flux_applied.ae2.FluxStorageChannel;
import com.mekeng.github.common.me.data.IAEGasStack;
import mekanism.api.gas.Gas;
import mekanism.api.gas.GasRegistry;
import mekanism.api.gas.GasStack;
import mekanism.api.gas.IGasHandler;
import mekanism.common.capabilities.Capabilities;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
import nyonio.FluixPoolManaHelper;
import nyonio.IFluixManaReceiver;
import nyonio.ae2.ManaStack;
import nyonio.ae2.ManaStorageChannel;
import vazkii.botania.api.mana.IManaReceiver;
import vazkii.botania.api.mana.spark.ISparkAttachable;

import java.util.Arrays;

/**
 * Shared implementation for the item/resource import and export buses.
 *
 * The configuration is intentionally larger than the normal AE2 automation
 * bus. A non-empty normal item is an item filter; ItemFluidMark is a type-only
 * marker and selects one of the non-item storage channels.
 */
public abstract class PartCommonBus extends PartUpgradeable implements appeng.api.networking.ticking.IGridTickable,
        appeng.util.inv.IInventoryDestination {
    public static final int CONFIG_SLOTS = 63;
    public static final int VIRTUAL_TRANSFER = 1000;
    public static final int ITEM_TRANSFER = 64;
    public static final int MAX_VIRTUAL_AMOUNT = 512000;

    private final AppEngInternalAEInventory config = new AppEngInternalAEInventory(this, CONFIG_SLOTS);
    private final int[] virtualAmounts = new int[CONFIG_SLOTS];
    protected final IActionSource source = new MachineSource(this);

    protected PartCommonBus(ItemStack stack) {
        super(stack);
        Arrays.fill(this.virtualAmounts, VIRTUAL_TRANSFER);
    }

    protected abstract boolean isExportBus();

    protected abstract int getGuiBaseId();

    protected abstract IPartModel getOffModel();

    protected abstract IPartModel getOnModel();

    protected abstract IPartModel getHasChannelModel();

    @Override
    protected int getUpgradeSlots() {
        return 0;
    }

    public int availableUpgrades() {
        return 0;
    }

    public AppEngInternalAEInventory getConfigInventory() {
        return this.config;
    }

    public int getVirtualAmount(int slot) {
        if (slot < 0 || slot >= CONFIG_SLOTS) return VIRTUAL_TRANSFER;
        return Math.max(1, Math.min(MAX_VIRTUAL_AMOUNT, this.virtualAmounts[slot]));
    }

    public void setVirtualAmount(int slot, int amount) {
        if (slot < 0 || slot >= CONFIG_SLOTS) return;
        this.virtualAmounts[slot] = Math.max(1, Math.min(MAX_VIRTUAL_AMOUNT, amount));
        this.getHost().markForSave();
    }

    private int getVirtualResourceAmount(int slot) {
        return MAX_VIRTUAL_AMOUNT;
    }

    public void setMarker(int slot, ItemStack marker) {
        if (slot < 0 || slot >= CONFIG_SLOTS) return;
        this.config.extractItem(slot, Integer.MAX_VALUE, false);
        if (marker != null && !marker.isEmpty()) {
            ItemStack copy = marker.copy();
            copy.setCount(1);
            this.config.insertItem(slot, copy, false);
        }
        this.setVirtualAmount(slot, VIRTUAL_TRANSFER);
        this.getHost().markForSave();
        this.wakeBus();
    }

    @Override
    public IItemHandler getInventoryByName(String name) {
        if ("config".equals(name)) return this.config;
        return super.getInventoryByName(name);
    }

    @Override
    public void readFromNBT(net.minecraft.nbt.NBTTagCompound data) {
        super.readFromNBT(data);
        this.config.readFromNBT(data, "config");
        if (data.hasKey("virtualAmounts")) {
            int[] saved = data.getIntArray("virtualAmounts");
            for (int i = 0; i < CONFIG_SLOTS; i++) {
                this.virtualAmounts[i] = saved.length > i && saved[i] > 0
                        ? Math.min(MAX_VIRTUAL_AMOUNT, saved[i]) : VIRTUAL_TRANSFER;
            }
        }
    }

    @Override
    public void writeToNBT(net.minecraft.nbt.NBTTagCompound data) {
        super.writeToNBT(data);
        this.config.writeToNBT(data, "config");
        data.setIntArray("virtualAmounts", this.virtualAmounts);
    }

    @Override
    public void onChangeInventory(net.minecraftforge.items.IItemHandler inventory, int slot,
                                  appeng.util.inv.InvOperation operation, ItemStack removed, ItemStack added) {
        super.onChangeInventory(inventory, slot, operation, removed, added);
        if (inventory == this.config) {
            this.getHost().markForSave();
            this.wakeBus();
        }
    }

    @Override
    public void onNeighborChanged(IBlockAccess world, BlockPos pos, BlockPos neighbor) {
        this.wakeBus();
    }

    @Override
    public float getCableConnectionLength(AECableType cable) {
        return 5;
    }

    @Override
    public TickingRequest getTickingRequest(IGridNode node) {
        return new TickingRequest(5, 40, false, false);
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
        return this.doBusWork();
    }

    protected TickRateModulation doBusWork() {
        if (!this.getProxy().isActive() || !this.canDoBusWork()) return TickRateModulation.IDLE;

        boolean worked = false;
        for (int slot = 0; slot < CONFIG_SLOTS; slot++) {
            ItemStack marker = this.config.getStackInSlot(slot);
            if (marker.isEmpty()) continue;
            if (ItemFluidMark.isFluidMark(marker)) {
                worked |= this.isExportBus() ? this.exportFluid(marker, slot) : this.importFluid(marker, slot);
            } else if (ItemFluidMark.isGasMark(marker)) {
                if (MekanismEnergisticsIntegration.isAvailable()) {
                    worked |= this.isExportBus() ? this.exportGas(marker, slot) : this.importGas(marker, slot);
                }
            } else if (ItemFluidMark.isManaMark(marker)) {
                if (BotaniaFluxIntegration.isManaIntegrationAvailable()) {
                    worked |= this.isExportBus() ? this.exportMana(slot) : this.importMana(slot);
                }
            } else if (ItemFluidMark.isFeMark(marker)) {
                if (BotaniaFluxIntegration.isFeIntegrationAvailable()) {
                    worked |= this.isExportBus() ? this.exportFe(slot) : this.importFe(slot);
                }
            } else {
                worked |= this.isExportBus() ? this.exportItem(marker) : this.importItem(marker);
            }
        }

        // An entirely unconfigured import bus imports all resource channels.
        // Once one marker exists, empty slots stay inactive so an explicit
        // mana/FE marker cannot silently enable unrelated imports.
        if (!this.isExportBus() && !this.hasAnyMarker()) {
            worked |= this.importItem(null);
            worked |= this.importFluid(null, -1);
            if (MekanismEnergisticsIntegration.isAvailable()) {
                worked |= this.importGas(null, -1);
            }
            if (BotaniaFluxIntegration.isManaIntegrationAvailable()) {
                worked |= this.importMana(-1);
            }
            if (BotaniaFluxIntegration.isFeIntegrationAvailable()) {
                worked |= this.importFe(-1);
            }
        }
        return worked ? TickRateModulation.FASTER : TickRateModulation.SLOWER;
    }

    private boolean hasAnyMarker() {
        for (int i = 0; i < CONFIG_SLOTS; i++) {
            if (!this.config.getStackInSlot(i).isEmpty()) return true;
        }
        return false;
    }

    protected TileEntity getConnectedTile() {
        TileEntity self = this.getHost().getTile();
        if (self == null || self.getWorld() == null) return null;
        BlockPos targetPos = self.getPos().offset(this.getSide().getFacing());
        World world = self.getWorld();
        if (world.getChunkProvider().getLoadedChunk(targetPos.getX() >> 4, targetPos.getZ() >> 4) == null) return null;
        return world.getTileEntity(targetPos);
    }

    protected EnumFacing getTargetFace() {
        return this.getSide().getFacing().getOpposite();
    }

    protected boolean canDoBusWork() {
        return this.getConnectedTile() != null;
    }

    private InventoryAdaptor getItemTarget() {
        TileEntity target = this.getConnectedTile();
        return target == null ? null : InventoryAdaptor.getAdaptor(target, this.getTargetFace());
    }

    private boolean importItem(ItemStack filter) {
        InventoryAdaptor target = this.getItemTarget();
        if (target == null) return false;
        ItemStack simulated = filter == null
                ? target.simulateRemove(ITEM_TRANSFER, ItemStack.EMPTY, null)
                : target.simulateRemove(ITEM_TRANSFER, filter, null);
        if (simulated == null || simulated.isEmpty()) return false;

        IAEItemStack aeStack = AEItemStack.fromItemStack(simulated);
        if (aeStack == null) return false;
        long accepted = this.simulateItemInsert(aeStack);
        if (accepted <= 0) return false;
        ItemStack extracted = filter == null
                ? target.removeItems((int) Math.min(ITEM_TRANSFER, accepted), ItemStack.EMPTY, this)
                : target.removeItems((int) Math.min(ITEM_TRANSFER, accepted), filter, this);
        if (extracted.isEmpty()) return false;
        IAEItemStack failed = this.insertItems(AEItemStack.fromItemStack(extracted), Actionable.MODULATE);
        if (failed != null && failed.getStackSize() > 0) target.addItems(failed.createItemStack());
        return failed == null || failed.getStackSize() < extracted.getCount();
    }

    private boolean exportItem(ItemStack marker) {
        InventoryAdaptor target = this.getItemTarget();
        if (target == null) return false;
        IAEItemStack extracted = this.extractItems(marker, ITEM_TRANSFER);
        if (extracted == null || extracted.getStackSize() <= 0) return false;
        ItemStack failed = target.addItems(extracted.createItemStack());
        if (!failed.isEmpty()) this.insertItems(AEItemStack.fromItemStack(failed), Actionable.MODULATE);
        return failed.getCount() < extracted.getStackSize();
    }

    private long simulateItemInsert(IAEItemStack stack) {
        if (stack == null) return 0;
        IAEItemStack remainder = this.insertItems(stack, Actionable.SIMULATE);
        return stack.getStackSize() - (remainder == null ? 0 : remainder.getStackSize());
    }

    private IAEItemStack insertItems(IAEItemStack stack, Actionable mode) {
        if (stack == null) return null;
        try {
            IMEInventory<IAEItemStack> inventory = this.getProxy().getStorage()
                    .getInventory(AEApi.instance().storage().getStorageChannel(IItemStorageChannel.class));
            return Platform.poweredInsert(this.getProxy().getEnergy(), inventory, stack, this.source, mode);
        } catch (GridAccessException e) {
            return stack;
        }
    }

    private IAEItemStack extractItems(ItemStack marker, int amount) {
        try {
            IAEItemStack request = AEItemStack.fromItemStack(marker.copy());
            request.setStackSize(amount);
            IMEInventory<IAEItemStack> inventory = this.getProxy().getStorage()
                    .getInventory(AEApi.instance().storage().getStorageChannel(IItemStorageChannel.class));
            return Platform.poweredExtraction(this.getProxy().getEnergy(), inventory, request, this.source);
        } catch (GridAccessException e) {
            return null;
        }
    }

    private IFluidHandler getFluidTarget() {
        TileEntity target = this.getConnectedTile();
        return target == null ? null : target.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, this.getTargetFace());
    }

    private boolean importFluid(ItemStack marker, int slot) {
        IFluidHandler target = this.getFluidTarget();
        if (target == null) return false;
        FluidStack marked = marker == null ? null : ItemFluidMark.getFluid(marker);
        FluidStack request = marked == null
                ? target.drain(this.getVirtualAmount(slot), false)
                : marked.copy();
        if (request == null || request.amount <= 0) return false;
        if (marked != null) request.amount = this.getVirtualAmount(slot);
        FluidStack simulated = target.drain(request, false);
        if (simulated == null || simulated.amount <= 0
                || (marked != null && !simulated.isFluidEqual(marked))) return false;
        int accepted = this.simulateFluidInsert(simulated);
        if (accepted <= 0) return false;
        FluidStack actualRequest = simulated.copy();
        actualRequest.amount = accepted;
        FluidStack actual = target.drain(actualRequest, true);
        if (actual == null || actual.amount <= 0) return false;
        IAEFluidStack failed = this.insertFluid(actual, Actionable.MODULATE);
        if (failed != null && failed.getStackSize() > 0) target.fill(failed.getFluidStack(), true);
        return failed == null || failed.getStackSize() < actual.amount;
    }

    private boolean exportFluid(ItemStack marker, int slot) {
        IFluidHandler target = this.getFluidTarget();
        FluidStack marked = ItemFluidMark.getFluid(marker);
        if (target == null || marked == null) return false;
        FluidStack probe = marked.copy();
        probe.amount = this.getVirtualAmount(slot);
        int accepted = target.fill(probe, false);
        if (accepted <= 0) return false;
        IAEFluidStack extracted = this.extractFluid(marked, accepted, Actionable.MODULATE);
        if (extracted == null || extracted.getStackSize() <= 0) return false;
        FluidStack actual = extracted.getFluidStack();
        int filled = target.fill(actual, true);
        if (filled < actual.amount) {
            FluidStack remainder = actual.copy();
            remainder.amount = actual.amount - filled;
            this.insertFluid(remainder, Actionable.MODULATE);
        }
        return filled > 0;
    }

    private int simulateFluidInsert(FluidStack stack) {
        IAEFluidStack input = appeng.fluids.util.AEFluidStack.fromFluidStack(stack.copy());
        IAEFluidStack remainder = this.insertFluid(input.getFluidStack(), Actionable.SIMULATE);
        return (int) Math.max(0, input.getStackSize() - (remainder == null ? 0 : remainder.getStackSize()));
    }

    private IAEFluidStack insertFluid(FluidStack stack, Actionable mode) {
        try {
            IMEInventory<IAEFluidStack> inventory = this.getProxy().getStorage()
                    .getInventory(AEApi.instance().storage().getStorageChannel(IFluidStorageChannel.class));
            IAEFluidStack input = appeng.fluids.util.AEFluidStack.fromFluidStack(stack.copy());
            return Platform.poweredInsert(this.getProxy().getEnergy(), inventory, input, this.source, mode);
        } catch (GridAccessException e) {
            return appeng.fluids.util.AEFluidStack.fromFluidStack(stack.copy());
        }
    }

    private IAEFluidStack extractFluid(FluidStack stack, int amount, Actionable mode) {
        try {
            IMEInventory<IAEFluidStack> inventory = this.getProxy().getStorage()
                    .getInventory(AEApi.instance().storage().getStorageChannel(IFluidStorageChannel.class));
            IAEFluidStack request = appeng.fluids.util.AEFluidStack.fromFluidStack(stack.copy());
            request.setStackSize(amount);
            return Platform.poweredExtraction(this.getProxy().getEnergy(), inventory, request, this.source, mode);
        } catch (GridAccessException e) {
            return null;
        }
    }

    private IGasHandler getGasTarget() {
        TileEntity target = this.getConnectedTile();
        return target == null ? null : target.getCapability(Capabilities.GAS_HANDLER_CAPABILITY, this.getTargetFace());
    }

    private boolean importGas(ItemStack marker, int slot) {
        IGasHandler target = this.getGasTarget();
        if (target == null) return false;
        String gasName = marker == null ? null : ItemFluidMark.getGasName(marker);
        Gas gas = gasName == null ? null : GasRegistry.getGas(gasName);
        GasStack simulated = target.drawGas(this.getTargetFace(), this.getVirtualAmount(slot), false);
        if (simulated == null || simulated.amount <= 0
                || (gas != null && simulated.getGas() != gas)) return false;
        int accepted = this.simulateGasInsert(simulated);
        if (accepted <= 0) return false;
        GasStack actual = target.drawGas(this.getTargetFace(), accepted, true);
        if (actual == null || actual.amount <= 0) return false;
        IAEGasStack failed = this.insertGas(actual, Actionable.MODULATE);
        if (failed != null && failed.getStackSize() > 0) target.receiveGas(this.getTargetFace(), failed.getGasStack(), true);
        return failed == null || failed.getStackSize() < actual.amount;
    }

    private boolean exportGas(ItemStack marker, int slot) {
        IGasHandler target = this.getGasTarget();
        String gasName = ItemFluidMark.getGasName(marker);
        Gas gas = gasName == null ? null : GasRegistry.getGas(gasName);
        if (target == null || gas == null) return false;
        GasStack probe = new GasStack(gas, this.getVirtualAmount(slot));
        int accepted = target.receiveGas(this.getTargetFace(), probe, false);
        if (accepted <= 0) return false;
        IAEGasStack extracted = this.extractGas(gas, accepted, Actionable.MODULATE);
        if (extracted == null || extracted.getStackSize() <= 0) return false;
        GasStack actual = extracted.getGasStack();
        int inserted = target.receiveGas(this.getTargetFace(), actual, true);
        if (inserted < actual.amount) {
            this.insertGas(new GasStack(gas, actual.amount - inserted), Actionable.MODULATE);
        }
        return inserted > 0;
    }

    private int simulateGasInsert(GasStack stack) {
        IAEGasStack input = this.insertGas(stack, Actionable.SIMULATE);
        return input == null ? stack.amount : Math.max(0, stack.amount - (int) input.getStackSize());
    }

    private IAEGasStack insertGas(GasStack stack, Actionable mode) {
        try {
            IMEInventory<IAEGasStack> inventory = this.getProxy().getStorage().getInventory(
                    AEApi.instance().storage().getStorageChannel(com.mekeng.github.common.me.storage.IGasStorageChannel.class));
            com.mekeng.github.common.me.data.IAEGasStack input = com.mekeng.github.common.me.data.impl.AEGasStack.of(stack.copy());
            return Platform.poweredInsert(this.getProxy().getEnergy(), inventory, input, this.source, mode);
        } catch (GridAccessException e) {
            return com.mekeng.github.common.me.data.impl.AEGasStack.of(stack.copy());
        }
    }

    private IAEGasStack extractGas(Gas gas, int amount, Actionable mode) {
        try {
            IMEInventory<IAEGasStack> inventory = this.getProxy().getStorage().getInventory(
                    AEApi.instance().storage().getStorageChannel(com.mekeng.github.common.me.storage.IGasStorageChannel.class));
            com.mekeng.github.common.me.data.IAEGasStack request = com.mekeng.github.common.me.data.impl.AEGasStack.of(new GasStack(gas, amount));
            return Platform.poweredExtraction(this.getProxy().getEnergy(), inventory, request, this.source, mode);
        } catch (GridAccessException e) {
            return null;
        }
    }

    private boolean importMana(int slot) {
        TileEntity target = this.getConnectedTile();
        if (!(target instanceof IManaReceiver)) return false;
        long remaining = this.getVirtualResourceAmount(slot);
        boolean worked = false;
        while (remaining > 0) {
            int request = (int) Math.min(remaining, Integer.MAX_VALUE);
            long accepted = this.insertMana(request, Actionable.SIMULATE);
            if (accepted <= 0) break;
            int actual = this.extractManaFromTarget(target, (int) Math.min(request, accepted));
            if (actual <= 0) break;
            long failed = this.insertMana(actual, Actionable.MODULATE);
            long rejected = Math.max(0, Math.min((long) actual, failed));
            int inserted = (int) (actual - rejected);
            if (rejected > 0) this.insertManaIntoTarget(target, (int) rejected);
            if (inserted <= 0) break;
            remaining -= inserted;
            worked = true;
            if (failed > 0) break;
        }
        return worked;
    }

    private boolean exportMana(int slot) {
        TileEntity target = this.getConnectedTile();
        if (!(target instanceof ISparkAttachable)) return false;
        ISparkAttachable receiver = (ISparkAttachable) target;
        long remaining = this.getVirtualResourceAmount(slot);
        boolean worked = false;
        while (remaining > 0) {
            int amount = (int) Math.min(remaining, receiver.getAvailableSpaceForMana());
            if (amount <= 0) break;
            long extracted = this.extractMana(amount, Actionable.MODULATE);
            if (extracted <= 0) break;
            receiver.recieveMana((int) extracted);
            remaining -= extracted;
            worked = true;
        }
        return worked;
    }

    private int extractManaFromTarget(TileEntity target, int amount) {
        if (target instanceof IFluixManaReceiver) {
            return FluixPoolManaHelper.extract(target, amount);
        }
        if (!(target instanceof IManaReceiver)) return 0;
        IManaReceiver receiver = (IManaReceiver) target;
        int before = Math.max(0, receiver.getCurrentMana());
        int toExtract = Math.min(amount, before);
        if (toExtract <= 0) return 0;
        receiver.recieveMana(-toExtract);
        return Math.max(0, before - receiver.getCurrentMana());
    }

    private void insertManaIntoTarget(TileEntity target, int amount) {
        if (amount <= 0 || !(target instanceof IManaReceiver)) return;
        if (target instanceof IFluixManaReceiver) {
            FluixPoolManaHelper.insert(target, amount);
        } else {
            ((IManaReceiver) target).recieveMana(amount);
        }
    }

    private boolean importFe(int slot) {
        TileEntity target = this.getConnectedTile();
        IEnergyStorage energy = target == null ? null : target.getCapability(CapabilityEnergy.ENERGY, this.getTargetFace());
        if (energy == null) return false;
        long remaining = this.getVirtualResourceAmount(slot);
        boolean worked = false;
        while (remaining > 0) {
            int request = (int) Math.min(remaining, Integer.MAX_VALUE);
            int available = energy.extractEnergy(request, true);
            if (available <= 0) break;
            long accepted = this.insertFe(available, Actionable.SIMULATE);
            if (accepted <= 0) break;
            int actual = energy.extractEnergy((int) Math.min(available, accepted), false);
            if (actual <= 0) break;
            long failed = this.insertFe(actual, Actionable.MODULATE);
            long rejected = Math.max(0, Math.min((long) actual, failed));
            int inserted = (int) (actual - rejected);
            if (rejected > 0) energy.receiveEnergy((int) rejected, false);
            if (inserted <= 0) break;
            remaining -= inserted;
            worked = true;
            if (failed > 0) break;
        }
        return worked;
    }

    private boolean exportFe(int slot) {
        TileEntity target = this.getConnectedTile();
        IEnergyStorage energy = target == null ? null : target.getCapability(CapabilityEnergy.ENERGY, this.getTargetFace());
        if (energy == null) return false;
        long remaining = this.getVirtualResourceAmount(slot);
        boolean worked = false;
        while (remaining > 0) {
            int accepted = energy.receiveEnergy((int) Math.min(remaining, Integer.MAX_VALUE), true);
            if (accepted <= 0) break;
            long extracted = this.extractFe(accepted, Actionable.MODULATE);
            if (extracted <= 0) break;
            int inserted = energy.receiveEnergy((int) extracted, false);
            if (inserted < extracted) {
                this.insertFe(extracted - inserted, Actionable.MODULATE);
            }
            if (inserted <= 0) break;
            remaining -= inserted;
            worked = true;
            if (inserted < extracted) break;
        }
        return worked;
    }

    private long insertMana(long amount, Actionable mode) {
        try {
            IMEInventory<ManaStack> inventory = this.getProxy().getStorage().getInventory(ManaStorageChannel.INSTANCE);
            ManaStack remainder = inventory.injectItems(new ManaStack(amount), mode, this.source);
            return amount - (remainder == null ? 0 : remainder.getStackSize());
        } catch (GridAccessException e) {
            return amount;
        }
    }

    private long extractMana(long amount, Actionable mode) {
        try {
            IMEInventory<ManaStack> inventory = this.getProxy().getStorage().getInventory(ManaStorageChannel.INSTANCE);
            ManaStack extracted = inventory.extractItems(new ManaStack(amount), mode, this.source);
            return extracted == null ? 0 : extracted.getStackSize();
        } catch (GridAccessException e) {
            return 0;
        }
    }

    private long insertFe(long amount, Actionable mode) {
        try {
            IMEInventory<FluxStack> inventory = this.getProxy().getStorage().getInventory(FluxStorageChannel.INSTANCE);
            FluxStack remainder = inventory.injectItems(new FluxStack(amount), mode, this.source);
            return amount - (remainder == null ? 0 : remainder.getStackSize());
        } catch (GridAccessException e) {
            return amount;
        }
    }

    private long extractFe(long amount, Actionable mode) {
        try {
            IMEInventory<FluxStack> inventory = this.getProxy().getStorage().getInventory(FluxStorageChannel.INSTANCE);
            FluxStack extracted = inventory.extractItems(new FluxStack(amount), mode, this.source);
            return extracted == null ? 0 : extracted.getStackSize();
        } catch (GridAccessException e) {
            return 0;
        }
    }

    public static PartCommonBus findPart(TileEntity tile, EnumFacing facing) {
        if (!(tile instanceof appeng.tile.networking.TileCableBus) || facing == null) return null;
        IPart part = ((appeng.tile.networking.TileCableBus) tile).getPart(facing);
        return part instanceof PartCommonBus ? (PartCommonBus) part : null;
    }

    protected void wakeBus() {
        try {
            this.getProxy().getTick().alertDevice(this.getProxy().getNode());
        } catch (GridAccessException ignored) {
        }
    }

    @Override
    public boolean canInsert(ItemStack stack) {
        return stack != null && !stack.isEmpty() && this.simulateItemInsert(AEItemStack.fromItemStack(stack)) > 0;
    }

    @Override
    public boolean onPartActivate(EntityPlayer player, EnumHand hand, Vec3d pos) {
        if (!player.world.isRemote) {
            player.openGui(AE2Utilix.INSTANCE, this.getGuiBaseId() + this.getSide().getFacing().getIndex(),
                    player.world, this.getHost().getTile().getPos().getX(),
                    this.getHost().getTile().getPos().getY(), this.getHost().getTile().getPos().getZ());
        }
        return true;
    }

    @Override
    public String getCustomInventoryName() {
        return this.getItemStackRepresentation().getDisplayName();
    }

    @Override
    public IPartModel getStaticModels() {
        if (this.isActive() && this.isPowered()) return this.getHasChannelModel();
        return this.isPowered() ? this.getOnModel() : this.getOffModel();
    }

    public abstract ItemStack getItemStackRepresentation();
}
