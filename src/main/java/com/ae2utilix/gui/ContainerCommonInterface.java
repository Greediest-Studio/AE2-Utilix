package com.ae2utilix.gui;

import appeng.api.config.Upgrades;
import appeng.container.AEBaseContainer;
import appeng.container.slot.SlotFake;
import appeng.container.slot.SlotOversized;
import appeng.container.slot.SlotRestrictedInput;
import appeng.util.Platform;
import com.ae2utilix.block.TileCommonInterfaceAlternate;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

public class ContainerCommonInterface extends AEBaseContainer {

    private final TileCommonInterfaceAlternate tile;

    public ContainerCommonInterface(InventoryPlayer inventory, TileCommonInterfaceAlternate tile) {
        super(inventory, tile);
        this.tile = tile;

        IItemHandler config = tile.getConfig();
        IItemHandler storage = tile.getStorage();
        IItemHandler extendedConfig = tile.getExtendedConfig();
        IItemHandler extendedStorage = tile.getExtendedStorage();
        IItemHandler upgrades = tile.getInventoryByName("upgrades");

        for (int column = 0; column < 9; column++) {
            this.addSlotToContainer(new SlotFake(config, column,
                    8 + column * 18, 27));
            this.addSlotToContainer(new SlotFluidStorage(storage, column,
                    8 + column * 18, 45));
            this.addSlotToContainer(new SlotFake(extendedConfig, column,
                    8 + column * 18, 79));
            this.addSlotToContainer(new SlotFluidStorage(extendedStorage, column,
                    8 + column * 18, 97));
        }
        for (int i = 0; i < 4; i++) {
            this.addSlotToContainer(new SlotRestrictedInput(SlotRestrictedInput.PlacableItemType.UPGRADES,
                    upgrades, i, 187, 8 + i * 18, inventory) {
                @Override
                public boolean isItemValid(ItemStack stack) {
                    return false;
                }
            }.setNotDraggable());
        }

        this.bindPlayerInventory(inventory, 0, 131);
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return this.tile.getWorld() != null
                && this.tile.getWorld().getTileEntity(this.tile.getPos()) == this.tile
                && player.getDistanceSq(this.tile.getPos()) <= 64.0D;
    }

    public net.minecraft.util.math.BlockPos getTilePosition() {
        return this.tile.getPos();
    }

    public TileCommonInterfaceAlternate getTile() {
        return this.tile;
    }

    public int getInstalledPatternExpansion() {
        return this.tile.getInstalledUpgrades(Upgrades.PATTERN_EXPANSION);
    }

    public boolean isNetworkReady() {
        return Platform.isServer() && this.tile.getActionableNode().getGrid() != null;
    }
}
