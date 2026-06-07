package com.ae2utilix;

import net.minecraft.item.ItemStack;

import java.util.*;

public class AE2UtilixUpgrades {

    private static final Map<String, Map<ItemStack, Integer>> SUPPORTED = new HashMap<>();

    public static void registerItem(String upgradeTypeId, ItemStack machine, int maxSupported) {
        if (machine == null || machine.isEmpty()) return;
        SUPPORTED.computeIfAbsent(upgradeTypeId, k -> new LinkedHashMap<>()).put(machine, maxSupported);
    }

    public static Map<ItemStack, Integer> getSupported(String upgradeTypeId) {
        return SUPPORTED.getOrDefault(upgradeTypeId, Collections.emptyMap());
    }
}
