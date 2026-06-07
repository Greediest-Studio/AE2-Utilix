package com.ae2utilix.block.terminal;

import appeng.tile.inventory.AppEngInternalInventory;
import appeng.util.inv.InvOperation;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.items.IItemHandler;

public class TileCraftingTerminal extends TileStorageTerminal {

    private static final String NBT_CRAFTING = "craftingGrid";

    private final AppEngInternalInventory craftingGrid = new AppEngInternalInventory(this, 9);

    @Override
    public IItemHandler getInventoryByName(String name) {
        if ("crafting".equals(name)) {
            return this.craftingGrid;
        }
        return super.getInventoryByName(name);
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);
        this.craftingGrid.readFromNBT(data, NBT_CRAFTING);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound data) {
        super.writeToNBT(data);
        this.craftingGrid.writeToNBT(data, NBT_CRAFTING);
        return data;
    }

    @Override
    public void onChangeInventory(IItemHandler inv, int slot, InvOperation mc,
                                  ItemStack removedStack, ItemStack addedStack) {
    }
}
