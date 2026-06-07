package com.ae2utilix.block.terminal;

import appeng.api.config.Settings;
import appeng.api.config.SortDir;
import appeng.api.config.SortOrder;
import appeng.api.config.ViewItems;
import appeng.api.implementations.tiles.IColorableTile;
import appeng.api.implementations.tiles.ISegmentedInventory;
import appeng.api.implementations.tiles.IViewCellStorage;
import appeng.api.networking.GridFlags;
import appeng.api.networking.events.MENetworkChannelsChanged;
import appeng.api.networking.events.MENetworkEventSubscribe;
import appeng.api.networking.events.MENetworkPowerStatusChange;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.IStorageChannel;
import appeng.api.storage.ITerminalHost;
import appeng.api.storage.data.IAEStack;
import appeng.api.util.AECableType;
import appeng.api.util.AEColor;
import appeng.api.util.AEPartLocation;
import appeng.api.util.IConfigManager;
import appeng.tile.grid.AENetworkTile;
import com.ae2utilix.AE2Utilix;
import com.ae2utilix.mixin.MixinGridNodeAccessor;
import appeng.tile.inventory.AppEngInternalInventory;
import appeng.util.ConfigManager;
import appeng.util.IConfigManagerHost;
import appeng.util.inv.IAEAppEngInventory;
import appeng.util.inv.InvOperation;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.items.IItemHandler;

import java.io.IOException;

import io.netty.buffer.ByteBuf;

public class TileFullTerminal extends AENetworkTile
        implements ITerminalHost, IViewCellStorage, IConfigManagerHost, IAEAppEngInventory, ISegmentedInventory, IColorableTile {

    private static final String NBT_VIEWCELL = "viewCell";
    private static final String NBT_CONFIG = "configManager";
    private static final String NBT_PAINTED_COLOR = "paintedColor";

    private void applyPaintedColorToProxy() {
        this.getProxy().setColor(this.paintedColor);
        updateGridNodeColor(this.paintedColor);
    }

    private void readPaintedColor(int ordinal) {
        AEColor[] colors = AEColor.values();
        if (ordinal >= 0 && ordinal < colors.length) {
            this.paintedColor = colors[ordinal];
        } else {
            this.paintedColor = AEColor.TRANSPARENT;
        }
    }

    private void updateGridNodeColor(AEColor colour) {
        try {
            Object node = this.getProxy().getNode();
            if (node == null) return;
            MixinGridNodeAccessor accessor = (MixinGridNodeAccessor) node;
            int data = accessor.ae2utilix$getCompressedData();
            data = (data & ~0xF8) | (colour.ordinal() << 3);
            accessor.ae2utilix$setCompressedData(data);
        } catch (Exception e) {
            AE2Utilix.LOGGER.warn("Failed to update grid node color", e);
        }
    }

    private final ConfigManager configManager = new ConfigManager(this);
    private final AppEngInternalInventory viewCellInv = new AppEngInternalInventory(this, 5);

    private boolean clientPowered = false;
    private boolean clientHasChannel = false;
    private AEColor paintedColor = AEColor.TRANSPARENT;

    public TileFullTerminal() {
        this.getProxy().setFlags(GridFlags.REQUIRE_CHANNEL);
        this.getProxy().setIdlePowerUsage(0.5);

        this.configManager.registerSetting(Settings.SORT_BY, SortOrder.NAME);
        this.configManager.registerSetting(Settings.VIEW_MODE, ViewItems.ALL);
        this.configManager.registerSetting(Settings.SORT_DIRECTION, SortDir.ASCENDING);
    }

    public boolean isClientPowered() {
        return this.clientPowered;
    }

    public boolean isClientHasChannel() {
        return this.clientHasChannel;
    }

    @Override
    public boolean canBeRotated() {
        return true;
    }

    @MENetworkEventSubscribe
    public void onPowerChange(final MENetworkPowerStatusChange event) {
        this.markForUpdate();
    }

    @MENetworkEventSubscribe
    public void onChannelChange(final MENetworkChannelsChanged event) {
        this.markForUpdate();
    }

    @Override
    protected void writeToStream(final ByteBuf data) throws IOException {
        super.writeToStream(data);
        boolean powered = false;
        boolean hasChannel = false;
        try {
            powered = this.getProxy().isPowered();
            hasChannel = this.getProxy().getNode().meetsChannelRequirements();
        } catch (Exception ignored) {
        }
        data.writeBoolean(powered);
        data.writeBoolean(hasChannel);
        data.writeByte(this.paintedColor.ordinal());
    }

    @Override
    protected boolean readFromStream(final ByteBuf data) throws IOException {
        final boolean changed = super.readFromStream(data);
        final boolean oldPowered = this.clientPowered;
        final boolean oldHasChannel = this.clientHasChannel;
        this.clientPowered = data.readBoolean();
        this.clientHasChannel = data.readBoolean();
        final AEColor oldColor = this.paintedColor;
        this.readPaintedColor(data.readUnsignedByte());
        if (oldColor != this.paintedColor) {
            this.applyPaintedColorToProxy();
        }
        return changed || oldPowered != this.clientPowered || oldHasChannel != this.clientHasChannel || oldColor != this.paintedColor;
    }

    @Override
    public <T extends IAEStack<T>> IMEMonitor<T> getInventory(IStorageChannel<T> channel) {
        try {
            return this.getProxy().getStorage().getInventory(channel);
        } catch (appeng.me.GridAccessException e) {
            return null;
        }
    }

    @Override
    public IConfigManager getConfigManager() {
        return this.configManager;
    }

    @Override
    public IItemHandler getViewCellStorage() {
        return this.viewCellInv;
    }

    @Override
    public IItemHandler getInventoryByName(String name) {
        return null;
    }

    @Override
    public AECableType getCableConnectionType(AEPartLocation dir) {
        return AECableType.SMART;
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);
        this.viewCellInv.readFromNBT(data, NBT_VIEWCELL);
        this.configManager.readFromNBT(data);
        if (data.hasKey(NBT_PAINTED_COLOR)) {
            this.readPaintedColor(data.getByte(NBT_PAINTED_COLOR));
            this.applyPaintedColorToProxy();
        }
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        NBTTagCompound tag = super.getUpdateTag();
        tag.setByte(NBT_PAINTED_COLOR, (byte) this.paintedColor.ordinal());
        return tag;
    }

    @Override
    public void handleUpdateTag(NBTTagCompound tag) {
        super.handleUpdateTag(tag);
        if (tag.hasKey(NBT_PAINTED_COLOR)) {
            this.readPaintedColor(tag.getByte(NBT_PAINTED_COLOR));
            this.applyPaintedColorToProxy();
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound data) {
        super.writeToNBT(data);
        this.viewCellInv.writeToNBT(data, NBT_VIEWCELL);
        this.configManager.writeToNBT(data);
        data.setByte(NBT_PAINTED_COLOR, (byte) this.paintedColor.ordinal());
        return data;
    }

    @Override
    public void onReady() {
        super.onReady();
        if (!this.getWorld().isRemote && this.paintedColor != AEColor.TRANSPARENT) {
            this.applyPaintedColorToProxy();
            this.getProxy().invalidate();
            this.getProxy().onReady();
        }
        if (!this.getWorld().isRemote) {
            this.markForUpdate();
        }
    }

    @Override
    public void saveChanges() {
        super.saveChanges();
    }

    @Override
    public void updateSetting(IConfigManager manager, Enum settingName, Enum newValue) {
    }

    @Override
    public void onChangeInventory(IItemHandler inv, int slot, InvOperation mc,
                                  ItemStack removedStack, ItemStack addedStack) {
    }

    // IColorableTile implementation
    @Override
    public AEColor getColor() {
        return this.paintedColor;
    }

    @Override
    public boolean recolourBlock(EnumFacing side, AEColor colour, EntityPlayer who) {
        if (this.paintedColor != colour) {
            this.paintedColor = colour;
            // Update proxy color
            this.getProxy().setColor(colour);
            // Update grid node's compressedData via reflection
            updateGridNodeColor(colour);
            // Force grid to re-evaluate connections
            this.getProxy().invalidate();
            this.getProxy().onReady();
            this.markForUpdate();
            return true;
        }
        return false;
    }
}
