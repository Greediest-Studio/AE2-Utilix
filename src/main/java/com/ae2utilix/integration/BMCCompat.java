package com.ae2utilix.integration;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Loader;

import java.lang.reflect.Method;

public final class BMCCompat {

    private static final String UPGRADE_MODULE = "me.emvoh.ae2bettermagnetcard.api.IBMCUpgradeModule";
    private static final String FILTERS = "me.emvoh.ae2bettermagnetcard.utils.MagnetCardFilters";

    private static Class<?> upgradeModuleClass;
    private static Method getTypeMethod;
    private static Method hasCustomFiltersMethod;
    private static Method passesPickupFilterMethod;
    private static boolean initialized;

    private BMCCompat() {
    }

    private static void initialize() {
        if (initialized) return;
        initialized = true;
        if (!Loader.isModLoaded("ae2bettermagnetcard")) return;
        try {
            upgradeModuleClass = Class.forName(UPGRADE_MODULE);
            getTypeMethod = upgradeModuleClass.getMethod("getType", ItemStack.class);
            Class<?> filtersClass = Class.forName(FILTERS);
            hasCustomFiltersMethod = filtersClass.getMethod("hasCustomFilters", ItemStack.class);
            passesPickupFilterMethod = filtersClass.getMethod("passesPickupFilter", ItemStack.class, ItemStack.class);
        } catch (ReflectiveOperationException ignored) {
            upgradeModuleClass = null;
            getTypeMethod = null;
            hasCustomFiltersMethod = null;
            passesPickupFilterMethod = null;
        }
    }

    public static boolean isAvailable() {
        initialize();
        return upgradeModuleClass != null;
    }

    public static boolean isUpgrade(ItemStack stack) {
        initialize();
        return !stack.isEmpty() && upgradeModuleClass != null && upgradeModuleClass.isInstance(stack.getItem());
    }

    public static boolean hasCustomFilters(ItemStack stack) {
        initialize();
        if (hasCustomFiltersMethod == null) return false;
        try {
            return (Boolean) hasCustomFiltersMethod.invoke(null, stack);
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    public static boolean passesPickupFilter(ItemStack magnetCard, ItemStack candidate) {
        initialize();
        if (passesPickupFilterMethod == null) return false;
        try {
            return (Boolean) passesPickupFilterMethod.invoke(null, magnetCard, candidate);
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    public static String getTypeName(ItemStack stack) {
        initialize();
        if (!isUpgrade(stack) || getTypeMethod == null) return "";
        try {
            Object type = getTypeMethod.invoke(stack.getItem(), stack);
            return type instanceof Enum ? ((Enum<?>) type).name() : "";
        } catch (ReflectiveOperationException ignored) {
            return "";
        }
    }
}
