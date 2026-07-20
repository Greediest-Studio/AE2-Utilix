package com.ae2utilix.integration;

import appeng.api.AEApi;
import appeng.items.contents.CellUpgrades;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

public class BMCMagnetHelper {

    public static boolean tryInstallUpgrade(EntityPlayer player, ItemStack heldItem, IItemHandler interfaceUpgrades) {
        if (!net.minecraftforge.fml.common.Loader.isModLoaded("ae2bettermagnetcard")) return false;
        if (heldItem.isEmpty()) return false;
        if (!isBMCUpgrade(heldItem)) return false;

        ItemStack magnetCard = ItemStack.EMPTY;
        for (int i = 0; i < interfaceUpgrades.getSlots(); i++) {
            ItemStack stack = interfaceUpgrades.getStackInSlot(i);
            if (!stack.isEmpty() && AEApi.instance().definitions().materials().cardMagnet().isSameAs(stack)) {
                magnetCard = stack;
                break;
            }
        }

        if (magnetCard.isEmpty()) return false;

        ItemStackHandler magnetUpgrades = getMagnetUpgrades(magnetCard);

        int existingSlot = -1;
        int emptySlot = -1;
        for (int i = 0; i < magnetUpgrades.getSlots(); i++) {
            ItemStack up = magnetUpgrades.getStackInSlot(i);
            if (up.isEmpty()) {
                if (emptySlot < 0) emptySlot = i;
            } else if (isBMCUpgrade(up)) {
                if (existingSlot < 0) existingSlot = i;
            }
        }

        if (existingSlot >= 0) {
            ItemStack existing = magnetUpgrades.getStackInSlot(existingSlot).copy();
            ItemStack toInsert = heldItem.copy();
            toInsert.setCount(1);
            magnetUpgrades.setStackInSlot(existingSlot, toInsert);
            player.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, existing);
        } else if (emptySlot >= 0) {
            ItemStack toInsert = heldItem.copy();
            toInsert.setCount(1);
            magnetUpgrades.setStackInSlot(emptySlot, toInsert);
            heldItem.shrink(1);
            if (heldItem.getCount() <= 0) {
                player.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, ItemStack.EMPTY);
            }
        } else {
            return false;
        }

        return true;
    }

    private static ItemStackHandler getMagnetUpgrades(ItemStack magnetCard) {
        return new CellUpgrades(magnetCard, 2);
    }

    private static boolean isBMCUpgrade(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return BMCCompat.isUpgrade(stack);
    }
}
