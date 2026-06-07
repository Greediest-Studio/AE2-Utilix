package com.ae2utilix.parts;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Maps;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import appeng.api.AEApi;
import appeng.api.config.AccessRestriction;
import appeng.api.config.Actionable;
import appeng.api.config.FuzzyMode;
import appeng.api.config.IncludeExclude;
import appeng.api.config.Settings;
import appeng.api.config.StorageFilter;
import appeng.api.config.Upgrades;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.storage.IBaseMonitor;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.parts.IPartModel;
import appeng.api.storage.IMEInventory;
import appeng.api.storage.IMEMonitorHandlerReceiver;
import appeng.api.storage.IStorageChannel;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import appeng.api.util.AEPartLocation;
import appeng.core.sync.GuiBridge;
import appeng.items.parts.PartModels;
import appeng.me.storage.ITickingMonitor;
import appeng.me.storage.MEInventoryHandler;
import appeng.parts.PartModel;
import appeng.parts.misc.PartStorageBus;
import appeng.util.Platform;
import appeng.util.item.AEItemStack;

import com.ae2utilix.mixin.MixinPartStorageBusAccessor;

public class PartBlockStorageBus extends PartStorageBus {

    public static final ResourceLocation MODEL_BASE = new ResourceLocation("ae2_utilix", "parts/block_storage_bus_base");

    @PartModels
    public static final IPartModel MODELS_OFF = new PartModel(MODEL_BASE,
            new ResourceLocation("ae2_utilix", "parts/block_storage_bus_off"));

    @PartModels
    public static final IPartModel MODELS_ON = new PartModel(MODEL_BASE,
            new ResourceLocation("ae2_utilix", "parts/block_storage_bus_on"));

    @PartModels
    public static final IPartModel MODELS_HAS_CHANNEL = new PartModel(MODEL_BASE,
            new ResourceLocation("ae2_utilix", "parts/block_storage_bus_has_channel"));

    public PartBlockStorageBus(ItemStack is) {
        super(is);
    }

    /**
     * Override getInternalHandler to provide custom inventory wrapper logic.
     * This is the main entry point that PartStorageBus uses to get its handler.
     * We reimplement the caching logic with our custom getInventoryWrapper behavior.
     */
    @Override
    public MEInventoryHandler<IAEItemStack> getInternalHandler() {
        if (this.cached) {
            return this.handler;
        }

        boolean wasSleeping = this.monitor == null;
        this.cached = true;

        TileEntity selfTe = getHost().getTile();
        World world = selfTe.getWorld();
        BlockPos targetPos = selfTe.getPos().offset(getSide().getFacing());
        TileEntity target = world.getTileEntity(targetPos);

        // Calculate hash for change detection
        int newHash = createBlockHandlerHash(target);
        if (newHash != 0 && newHash == this.handlerHash) {
            return this.handler;
        }
        this.handlerHash = newHash;

        // Clean up old handler/monitor
        this.handler = null;
        if (this.monitor != null) {
            if (this.monitor instanceof IBaseMonitor) {
                ((IBaseMonitor<IAEItemStack>) this.monitor).removeListener(this);
            }
        }
        this.monitor = null;

        // Get the inventory wrapper
        IMEInventory<IAEItemStack> wrapper = getBlockInventoryWrapper(target);

        if (wrapper == null) {
            // No wrapper available
            return finishHandlerSetup(wasSleeping);
        }

        // If wrapper is an ITickingMonitor, set it up
        if (wrapper instanceof ITickingMonitor) {
            this.monitor = (ITickingMonitor) wrapper;
            this.monitor.setActionSource(this.mySrc);
            this.monitor.setMode((StorageFilter) getConfigManager().getSetting(appeng.api.config.Settings.STORAGE_FILTER));
        }

        // Create MEInventoryHandler from the wrapper
        IStorageChannel<IAEItemStack> channel = AEApi.instance().storage().getStorageChannel(
                appeng.api.storage.channels.IItemStorageChannel.class);
        this.handler = new MEInventoryHandler<>(wrapper, channel);

        // Configure the handler
        this.handler.setBaseAccess((AccessRestriction) getConfigManager().getSetting(appeng.api.config.Settings.ACCESS));
        this.handler.setWhitelist(getInstalledUpgrades(Upgrades.INVERTER) > 0 ? IncludeExclude.BLACKLIST : IncludeExclude.WHITELIST);
        this.handler.setPriority(this.priority);
        this.handler.setStorageFilter((StorageFilter) getConfigManager().getSetting(appeng.api.config.Settings.STORAGE_FILTER));

        // Build partition list from config
        IItemList<IAEItemStack> priorityList = channel.createList();
        int slotsToUse = 18 + getInstalledUpgrades(Upgrades.CAPACITY) * 9;
        for (int x = 0; x < this.Config.getSlots() && x < slotsToUse; x++) {
            IAEItemStack is = this.Config.getAEStackInSlot(x);
            if (is != null) {
                priorityList.add(is);
            }
        }

        // Set sticky mode
        if (getInstalledUpgrades(Upgrades.STICKY) > 0) {
            this.handler.setSticky(true);
        }

        // Set partition list
        if (getInstalledUpgrades(Upgrades.FUZZY) > 0) {
            this.handler.setPartitionList(new appeng.util.prioritylist.FuzzyPriorityList(priorityList,
                    (FuzzyMode) getConfigManager().getSetting(appeng.api.config.Settings.FUZZY_MODE)));
        } else {
            this.handler.setPartitionList(new appeng.util.prioritylist.PrecisePriorityList(priorityList));
        }

        // Register as listener if wrapper is a monitor and we have read access
        if (wrapper instanceof IBaseMonitor) {
            AccessRestriction access = (AccessRestriction) getConfigManager().getSetting(appeng.api.config.Settings.ACCESS);
            if (access.hasPermission(AccessRestriction.READ)) {
                ((IBaseMonitor<IAEItemStack>) wrapper).addListener(this, this.handler);
            }
        }

        return finishHandlerSetup(wasSleeping);
    }

    private MEInventoryHandler<IAEItemStack> finishHandlerSetup(boolean wasSleeping) {
        // Update tick registration
        if (wasSleeping != (this.monitor == null)) {
            try {
                appeng.api.networking.ticking.ITickManager tm = getProxy().getTick();
                if (this.monitor == null) {
                    tm.sleepDevice(getProxy().getNode());
                } else {
                    tm.wakeDevice(getProxy().getNode());
                }
            } catch (appeng.me.GridAccessException e) {
                // ignore
            }
        }

        // Notify storage grid of changes
        try {
            appeng.me.cache.GridStorageCache gsc = (appeng.me.cache.GridStorageCache) getProxy().getGrid()
                    .getCache(appeng.api.networking.storage.IStorageGrid.class);
            gsc.cellUpdate(null);
        } catch (appeng.me.GridAccessException e) {
            // ignore
        }

        return this.handler;
    }

    /**
     * Custom getInventoryWrapper that checks for containers first,
     * then falls back to block+entity wrapper.
     */
    private IMEInventory<IAEItemStack> getBlockInventoryWrapper(TileEntity target) {
        if (target != null) {
            // Try the normal storage bus behavior first (container with IItemHandler)
            IMEInventory<IAEItemStack> superWrapper = ((MixinPartStorageBusAccessor) (Object) this)
                    .ae2utilix$invokeGetInventoryWrapper(target);
            if (superWrapper != null) {
                return superWrapper;
            }
        }

        // If no container TE (or TE has no IItemHandler), create a wrapper for the block + dropped items
        return new BlockAndEntityWrapper();
    }

    /**
     * Custom handler hash that only includes block state (not entity data).
     * Entity changes are detected by onTick() instead.
     */
    private int createBlockHandlerHash(TileEntity target) {
        if (target != null) {
            // Use the super hash for container TEs
            int superHash = ((MixinPartStorageBusAccessor) (Object) this)
                    .ae2utilix$invokeCreateHandlerHash(target);
            if (superHash != 0) {
                return superHash;
            }
        }

        // For no TE (or TE with no handler), hash based on block state only
        World world = getHost().getLocation().getWorld();
        if (world == null) {
            return 0;
        }
        BlockPos targetPos = getTargetPos();
        IBlockState state = world.getBlockState(targetPos);

        return state.getBlock().hashCode() ^ state.getBlock().getMetaFromState(state);
    }

    @Override
    public void onNeighborChanged(IBlockAccess w, BlockPos pos, BlockPos neighbor) {
        if (pos.offset(this.getSide().getFacing()).equals(neighbor)) {
            resetCache(true);
        }
    }

    /**
     * Override resetCache() to properly handle BlockAndEntityWrapper.
     * BlockAndEntityWrapper.getAvailableItems() always reads from the world in real-time,
     * so by the time resetCache() runs, the world has already changed and both
     * "before" and "after" would reflect the current state. We use cachedAvailable
     * as the "before" state instead, which reflects what the network currently thinks.
     */
    @Override
    protected void resetCache() {
        MixinPartStorageBusAccessor acc = (MixinPartStorageBusAccessor) (Object) this;
        final boolean fullReset = acc.ae2utilix$getResetCacheLogic() == 2;
        acc.ae2utilix$setResetCacheLogic((byte) 0);

        IItemList<IAEItemStack> before = AEApi.instance().storage().getStorageChannel(IItemStorageChannel.class).createList();

        // For BlockAndEntityWrapper, use cachedAvailable as "before" state
        // because getAvailableItems() reads from the world which has already changed
        BlockAndEntityWrapper oldWrapper = (this.monitor instanceof BlockAndEntityWrapper)
                ? (BlockAndEntityWrapper) this.monitor : null;

        if (oldWrapper != null && oldWrapper.cachedAvailable != null) {
            for (IAEItemStack stack : oldWrapper.cachedAvailable) {
                before.add(stack.copy());
            }
        } else if (this.handler != null) {
            // For non-BlockAndEntityWrapper handlers (e.g. container TEs), use standard approach
            if (acc.ae2utilix$isAccessChanged()) {
                AccessRestriction currentAccess = (AccessRestriction) ((appeng.util.ConfigManager) this.getConfigManager()).getSetting(Settings.ACCESS);
                AccessRestriction oldAccess = (AccessRestriction) ((appeng.util.ConfigManager) this.getConfigManager()).getOldSetting(Settings.ACCESS);
                if (oldAccess.hasPermission(AccessRestriction.READ) && !currentAccess.hasPermission(AccessRestriction.READ)) {
                    acc.ae2utilix$setReadOncePass(true);
                }
                this.handler.setBaseAccess(oldAccess);
                before = this.handler.getAvailableItems(before);
                this.handler.setBaseAccess(currentAccess);
                acc.ae2utilix$setAccessChanged(false);
            } else {
                before = this.handler.getAvailableItems(before);
            }
        }

        this.cached = false;
        if (fullReset) {
            this.handlerHash = 0;
        }

        final MEInventoryHandler<IAEItemStack> out = this.getInternalHandler();

        IItemList<IAEItemStack> after = AEApi.instance().storage().getStorageChannel(IItemStorageChannel.class).createList();
        if (out != null) {
            after = out.getAvailableItems(after);
        }
        Platform.postListChanges(before, after, this, this.mySrc);

        // Update cachedAvailable to match the current world state
        // so onTick() won't double-report the same change
        if (this.monitor instanceof BlockAndEntityWrapper) {
            ((BlockAndEntityWrapper) this.monitor).cachedAvailable = after;
        }
    }

    @Override
    public IPartModel getStaticModels() {
        if (isPowered()) {
            if (isActive()) {
                return MODELS_HAS_CHANNEL;
            } else {
                return MODELS_ON;
            }
        }
        return MODELS_OFF;
    }

    @Override
    public ItemStack getItemStackRepresentation() {
        return new ItemStack(Item.getByNameOrId("ae2_utilix:block_storage_bus"));
    }

    @Override
    public GuiBridge getGuiBridge() {
        return GuiBridge.GUI_STORAGEBUS;
    }

    /**
     * Gets the position in front of this storage bus.
     */
    private BlockPos getTargetPos() {
        AEPartLocation side = getSide();
        BlockPos selfPos = getHost().getLocation().getPos();
        return selfPos.offset(side.getFacing());
    }

    /**
     * Inner class that wraps the block at the target position and dropped EntityItems
     * as a virtual ME inventory.
     */
    private class BlockAndEntityWrapper implements IMEInventory<IAEItemStack>, IBaseMonitor<IAEItemStack>, ITickingMonitor {

        private final Map<IMEMonitorHandlerReceiver<IAEItemStack>, Object> listeners = Maps.newHashMap();
        private IItemList<IAEItemStack> cachedAvailable;
        private IActionSource actionSource;
        private StorageFilter mode = StorageFilter.EXTRACTABLE_ONLY;

        @Override
        public IAEItemStack extractItems(IAEItemStack request, Actionable mode, IActionSource src) {
            if (request == null) {
                return null;
            }

            World world = getHost().getLocation().getWorld();
            if (world == null) {
                return null;
            }

            BlockPos targetPos = getTargetPos();

            // Check if the requested item matches a dropped EntityItem
            AxisAlignedBB searchBox = new AxisAlignedBB(targetPos);
            List<EntityItem> items = world.getEntitiesWithinAABB(EntityItem.class, searchBox);

            for (EntityItem ei : items) {
                if (ei.isDead) {
                    continue;
                }

                ItemStack entityStack = ei.getItem();
                if (entityStack.isEmpty()) {
                    continue;
                }

                IAEItemStack entityAE = AEItemStack.fromItemStack(entityStack);
                if (entityAE != null && request.isSameType(entityAE)) {
                    long canExtract = Math.min(request.getStackSize(), entityStack.getCount());
                    if (canExtract <= 0) {
                        continue;
                    }

                    if (mode == Actionable.MODULATE) {
                        entityStack.shrink((int) canExtract);
                        if (entityStack.getCount() <= 0) {
                            ei.setDead();
                        } else {
                            ei.setItem(entityStack);
                        }
                    }

                    IAEItemStack result = request.copy();
                    result.setStackSize(canExtract);
                    return result;
                }
            }

            // Block items are read-only (can view in terminal but cannot extract the actual block)
            return null;
        }

        @Override
        public IAEItemStack injectItems(IAEItemStack input, Actionable mode, IActionSource src) {
            if (input == null) {
                return null;
            }

            World world = getHost().getLocation().getWorld();
            if (world == null) {
                return input;
            }

            BlockPos targetPos = getTargetPos();
            IBlockState state = world.getBlockState(targetPos);
            Block block = state.getBlock();

            // If facing a full block (isNormalCube and not replaceable) -> reject
            if (!block.isAir(state, world, targetPos) && state.isNormalCube() && state.getMaterial() != Material.AIR) {
                if (!block.isReplaceable(world, targetPos)) {
                    return input;
                }
            }

            // Otherwise, spawn as EntityItem (like formation plane drop mode)
            if (mode == Actionable.MODULATE) {
                ItemStack toSpawn = input.createItemStack();
                spawnEntityItem(world, targetPos, toSpawn);
            }

            return null;
        }

        @Override
        public IItemList<IAEItemStack> getAvailableItems(IItemList<IAEItemStack> out) {
            World world = getHost().getLocation().getWorld();
            if (world == null) {
                return out;
            }

            BlockPos targetPos = getTargetPos();
            IBlockState state = world.getBlockState(targetPos);

            // Add the block as an item (if it's not air)
            Block block = state.getBlock();
            if (!block.isAir(state, world, targetPos)) {
                Item item = Item.getItemFromBlock(block);
                if (item != null && item != net.minecraft.init.Items.AIR) {
                    int meta = block.damageDropped(state);
                    ItemStack blockItem = new ItemStack(item, 1, meta);
                    if (!blockItem.isEmpty()) {
                        IAEItemStack aeStack = AEItemStack.fromItemStack(blockItem);
                        if (aeStack != null) {
                            aeStack.setStackSize(1);
                            out.addStorage(aeStack);
                        }
                    }
                }
            }

            // Add all EntityItem stacks in the AABB in front
            AxisAlignedBB searchBox = new AxisAlignedBB(targetPos);
            List<EntityItem> items = world.getEntitiesWithinAABB(EntityItem.class, searchBox);
            for (EntityItem ei : items) {
                if (ei.isDead) {
                    continue;
                }
                ItemStack entityStack = ei.getItem();
                if (!entityStack.isEmpty()) {
                    IAEItemStack aeStack = AEItemStack.fromItemStack(entityStack);
                    if (aeStack != null) {
                        aeStack.setStackSize(entityStack.getCount());
                        out.addStorage(aeStack);
                    }
                }
            }

            return out;
        }

        @Override
        public IStorageChannel<IAEItemStack> getChannel() {
            return AEApi.instance().storage().getStorageChannel(IItemStorageChannel.class);
        }

        @Override
        public TickRateModulation onTick() {
            IItemList<IAEItemStack> current = getChannel().createList();
            getAvailableItems(current);

            if (cachedAvailable == null) {
                // First tick: just initialize cache, don't report changes.
                // The initial state is already known to the network via resetCache() or cellUpdate().
                cachedAvailable = current;
                return TickRateModulation.FASTER;
            }

            List<IAEItemStack> changes = new ArrayList<>();

            // Find added/changed items
            for (IAEItemStack stack : current) {
                IAEItemStack cached = cachedAvailable.findPrecise(stack);
                if (cached == null) {
                    changes.add(stack.copy());
                } else if (cached.getStackSize() != stack.getStackSize()) {
                    IAEItemStack diff = stack.copy();
                    diff.setStackSize(stack.getStackSize() - cached.getStackSize());
                    changes.add(diff);
                }
            }
            // Find removed items
            for (IAEItemStack cached : cachedAvailable) {
                IAEItemStack currentStack = current.findPrecise(cached);
                if (currentStack == null) {
                    IAEItemStack diff = cached.copy();
                    diff.setStackSize(-cached.getStackSize());
                    changes.add(diff);
                }
            }

            cachedAvailable = current;

            if (!changes.isEmpty()) {
                postDifference(changes);
                return TickRateModulation.URGENT;
            }

            return TickRateModulation.FASTER;
        }

        @Override
        public void addListener(final IMEMonitorHandlerReceiver<IAEItemStack> l, final Object verificationToken) {
            this.listeners.put(l, verificationToken);
        }

        @Override
        public void removeListener(final IMEMonitorHandlerReceiver<IAEItemStack> l) {
            this.listeners.remove(l);
        }

        private void postDifference(Iterable<IAEItemStack> a) {
            final Iterator<Map.Entry<IMEMonitorHandlerReceiver<IAEItemStack>, Object>> i = this.listeners.entrySet().iterator();
            while (i.hasNext()) {
                final Map.Entry<IMEMonitorHandlerReceiver<IAEItemStack>, Object> l = i.next();
                final IMEMonitorHandlerReceiver<IAEItemStack> key = l.getKey();
                if (key.isValid(l.getValue())) {
                    key.postChange(this, a, this.actionSource);
                } else {
                    i.remove();
                }
            }
        }

        @Override
        public void setActionSource(IActionSource source) {
            this.actionSource = source;
        }

        @Override
        public void setMode(StorageFilter mode) {
            this.mode = mode;
        }
    }

    /**
     * Spawns an EntityItem in front of the bus, similar to formation plane drop mode.
     */
    private void spawnEntityItem(World w, BlockPos targetPos, ItemStack is) {
        if (is.isEmpty()) {
            return;
        }

        AEPartLocation side = getSide();
        TileEntity te = getTile();

        double x = (side.xOffset != 0 ? 0 : .7 * (Platform.getRandomFloat() - .5)) + side.xOffset + .5 + te.getPos().getX();
        double y = (side.yOffset != 0 ? 0 : .7 * (Platform.getRandomFloat() - .5)) + side.yOffset + .5 + te.getPos().getY();
        double z = (side.zOffset != 0 ? 0 : .7 * (Platform.getRandomFloat() - .5)) + side.zOffset + .5 + te.getPos().getZ();

        EntityItem ei = new EntityItem(w, x, y, z, is.copy());
        ei.motionX = side.xOffset * 0.2;
        ei.motionY = side.yOffset * 0.2;
        ei.motionZ = side.zOffset * 0.2;

        ei.setPickupDelay(10);

        w.spawnEntity(ei);
    }
}
