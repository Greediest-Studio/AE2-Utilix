package com.ae2utilix.integration;

import appeng.api.storage.data.IAEItemStack;
import appeng.util.item.AEItemStack;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.Loader;

import javax.annotation.Nullable;

public class GasReturnHandler {

    private static Item GAS_DROP;
    private static Item GAS_PACKET;
    private static boolean checked = false;

    public static boolean isGasFakeItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        check();
        if (GAS_DROP == null && GAS_PACKET == null) return false;
        return stack.getItem() == GAS_DROP || stack.getItem() == GAS_PACKET;
    }

    @Nullable
    public static String getGasNameFromAEStack(IAEItemStack stack) {
        if (stack == null) return null;
        ItemStack def = stack.getDefinition();
        if (def.isEmpty() || !def.hasTagCompound()) return null;
        NBTTagCompound tag = def.getTagCompound();
        if (tag == null || !tag.hasKey("Gas", 8)) return null;
        return tag.getString("Gas");
    }

    public static long getGasAmountFromAEStack(IAEItemStack stack) {
        if (stack == null) return 0;
        return stack.getStackSize();
    }

    @Nullable
    public static IAEItemStack packGas2AEDrops(String gasName, long amount) {
        if (gasName == null || amount <= 0) return null;
        check();
        if (GAS_DROP == null) return null;

        int stackCount = (int) Math.min(amount, 64);
        ItemStack itemStack = new ItemStack(GAS_DROP, stackCount);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("Gas", gasName);
        itemStack.setTagCompound(tag);

        IAEItemStack aeStack = AEItemStack.fromItemStack(itemStack);
        if (aeStack == null) return null;
        aeStack.setStackSize(amount);
        return aeStack;
    }

    public static boolean hasGasSupport() {
        check();
        return GAS_DROP != null;
    }

    private static void check() {
        if (checked) return;
        checked = true;
        if (!Loader.isModLoaded("ae2fc")) return;
        try {
            GAS_DROP = Item.getByNameOrId("ae2fc:gas_drop");
            GAS_PACKET = Item.getByNameOrId("ae2fc:gas_packet");
        } catch (Exception ignored) {
        }
    }
}
