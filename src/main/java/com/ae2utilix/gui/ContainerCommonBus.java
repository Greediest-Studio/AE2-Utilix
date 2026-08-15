package com.ae2utilix.gui;

import appeng.container.AEBaseContainer;
import appeng.container.slot.SlotFakeTypeOnly;
import appeng.container.slot.SlotRestrictedInput;
import com.ae2utilix.parts.PartCommonBus;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.items.IItemHandler;

public class ContainerCommonBus extends AEBaseContainer {
    private final PartCommonBus bus;

    public ContainerCommonBus(InventoryPlayer inventory, PartCommonBus bus) {
        super(inventory, bus.getHost().getTile(), bus);
        this.bus = bus;

        for (int row = 0; row < 7; row++) {
            for (int column = 0; column < 9; column++) {
                this.addSlotToContainer(new SlotFakeTypeOnly(bus.getConfigInventory(), row * 9 + column,
                        8 + column * 18, 29 + row * 18));
            }
        }
        IItemHandler upgrades = bus.getInventoryByName("upgrades");
        for (int i = 0; i < bus.availableUpgrades(); i++) {
            this.addSlotToContainer(new SlotRestrictedInput(
                    SlotRestrictedInput.PlacableItemType.UPGRADES,
                    upgrades, i, 187, 8 + i * 18, inventory).setNotDraggable());
        }
        this.bindPlayerInventory(inventory, 0, 169);
    }

    public PartCommonBus getBus() {
        return this.bus;
    }

    public net.minecraft.util.math.BlockPos getTilePosition() {
        return this.bus.getHost().getTile().getPos();
    }

    public EnumFacing getSide() {
        return this.bus.getSide().getFacing();
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return this.bus.getHost() != null && this.bus.getHost().getTile() != null
                && this.bus.getHost().getTile().getWorld() != null
                && this.bus.getHost().getTile().getWorld().getTileEntity(this.bus.getHost().getTile().getPos())
                == this.bus.getHost().getTile()
                && player.getDistanceSq(this.bus.getHost().getTile().getPos()) <= 64.0D;
    }
}
