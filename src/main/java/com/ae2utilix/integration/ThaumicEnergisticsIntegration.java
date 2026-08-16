package com.ae2utilix.integration;

import appeng.api.config.Actionable;
import appeng.api.networking.energy.IEnergySource;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.IStorageChannel;
import com.ae2utilix.block.TileCommonInterfaceAlternate;
import com.ae2utilix.item.ItemFluidMark;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fml.common.Loader;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/** Optional, class-loader safe entry point for Thaumic Energistics. */
public final class ThaumicEnergisticsIntegration {
    private static final String OPTIONAL_IMPL =
            "com.ae2utilix.integration.ThaumicEnergisticsOptional";

    private ThaumicEnergisticsIntegration() {
    }

    public static boolean isAvailable() {
        return Loader.isModLoaded("thaumcraft") && Loader.isModLoaded("thaumicenergistics");
    }

    public static boolean isEssentiaChannel(IStorageChannel<?> channel) {
        return isAvailable() && Boolean.TRUE.equals(invoke("isEssentiaChannel", channel));
    }

    @Nullable
    public static String getAspectTagFromItem(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !isAvailable()) return null;
        Object value = invoke("getAspectTagFromItem", stack);
        return value instanceof String && !((String) value).isEmpty() ? (String) value : null;
    }

    /** Only accepts virtual aspect marker items, never filled containers. */
    @Nullable
    public static String getAspectTagFromMarker(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !isAvailable()) return null;
        Object value = invoke("getAspectTagFromMarker", stack);
        return value instanceof String && !((String) value).isEmpty() ? (String) value : null;
    }

    /** Resolves custom ThE JEI ingredients such as EssentiaStack as well as ItemStack. */
    @Nullable
    public static String getAspectTagFromIngredient(Object ingredient) {
        if (ingredient instanceof ItemStack) return getAspectTagFromItem((ItemStack) ingredient);
        if (!isAvailable() || ingredient == null) return null;
        Object value = invoke("getAspectTagFromIngredient", ingredient);
        return value instanceof String && !((String) value).isEmpty() ? (String) value : null;
    }

    @Nullable
    public static String getAspectDisplayName(String tag) {
        if (!isAvailable() || tag == null || tag.isEmpty()) return null;
        Object value = invoke("getAspectDisplayName", tag);
        return value instanceof String ? (String) value : null;
    }

    public static boolean isAspectTagValid(String tag) {
        if (!isAvailable() || tag == null || tag.isEmpty()) return false;
        return Boolean.TRUE.equals(invoke("isAspectTagValid", tag));
    }

    /**
     * Creates Thaumic Energistics' own dummy-aspect stack.  Using the native
     * stack lets its item renderer display the actual aspect glyph instead
     * of Utilix's generic virtual-resource texture.
     */
    public static ItemStack createAspectItem(String tag) {
        if (!isAvailable() || tag == null || tag.isEmpty()) return ItemStack.EMPTY;
        Object value = invoke("createAspectItem", tag);
        return value instanceof ItemStack ? ((ItemStack) value).copy() : ItemStack.EMPTY;
    }

    public static boolean isEssentiaMark(ItemStack stack) {
        return ItemFluidMark.isEssentiaMark(stack);
    }

    @Nullable
    public static String getAspectTag(ItemStack stack) {
        return ItemFluidMark.getAspectTag(stack);
    }

    @Nullable
    public static IMEMonitor<?> getMonitor(TileCommonInterfaceAlternate tile, boolean configured) {
        if (!isAvailable() || tile == null) return null;
        Object value = invoke("getMonitor", tile, configured);
        return value instanceof IMEMonitor ? (IMEMonitor<?>) value : null;
    }

    public static int insertNetwork(IStorageGrid storage, IEnergySource energy,
                                    IActionSource source, String aspectTag, int amount,
                                    Actionable mode) {
        if (!isAvailable() || storage == null || energy == null || source == null
                || aspectTag == null || amount <= 0) return 0;
        Object value = invoke("insertNetwork", storage, energy, source, aspectTag, amount, mode);
        return value instanceof Number ? Math.max(0, ((Number) value).intValue()) : 0;
    }

    public static int extractNetwork(IStorageGrid storage, IEnergySource energy,
                                     IActionSource source, String aspectTag, int amount,
                                     Actionable mode) {
        if (!isAvailable() || storage == null || energy == null || source == null
                || aspectTag == null || amount <= 0) return 0;
        Object value = invoke("extractNetwork", storage, energy, source, aspectTag, amount, mode);
        return value instanceof Number ? Math.max(0, ((Number) value).intValue()) : 0;
    }

    public static int insertNetwork(TileCommonInterfaceAlternate tile, String aspectTag,
                                    int amount, Actionable mode) {
        if (!isAvailable() || tile == null) return 0;
        Object value = invoke("insertNetwork", tile, aspectTag, amount, mode);
        return value instanceof Number ? Math.max(0, ((Number) value).intValue()) : 0;
    }

    public static int extractNetwork(TileCommonInterfaceAlternate tile, String aspectTag,
                                     int amount, Actionable mode) {
        if (!isAvailable() || tile == null) return 0;
        Object value = invoke("extractNetwork", tile, aspectTag, amount, mode);
        return value instanceof Number ? Math.max(0, ((Number) value).intValue()) : 0;
    }

    public static int receiveLocal(TileCommonInterfaceAlternate tile, String aspectTag,
                                   int amount, boolean simulate) {
        if (!isAvailable() || tile == null) return 0;
        Object value = invoke("receiveLocal", tile, aspectTag, amount, simulate);
        return value instanceof Number ? Math.max(0, ((Number) value).intValue()) : 0;
    }

    public static int extractLocal(TileCommonInterfaceAlternate tile, String aspectTag,
                                   int amount, boolean simulate) {
        if (!isAvailable() || tile == null) return 0;
        Object value = invoke("extractLocal", tile, aspectTag, amount, simulate);
        return value instanceof Number ? Math.max(0, ((Number) value).intValue()) : 0;
    }

    @Nullable
    public static String findExtractableAspect(TileEntity target, @Nullable String filter) {
        if (!isAvailable() || target == null) return null;
        Object value = invoke("findExtractableAspect", target, filter);
        return value instanceof String ? (String) value : null;
    }

    @Nullable
    public static String findLocalExtractableAspect(TileCommonInterfaceAlternate tile,
                                                    @Nullable String filter) {
        if (!isAvailable() || tile == null) return null;
        Object value = invoke("findLocalExtractableAspect", tile, filter);
        return value instanceof String ? (String) value : null;
    }

    public static int extractFromTarget(TileEntity target, String aspectTag, int amount) {
        if (!isAvailable() || target == null || aspectTag == null || amount <= 0) return 0;
        Object value = invoke("extractFromTarget", target, aspectTag, amount);
        return value instanceof Number ? Math.max(0, ((Number) value).intValue()) : 0;
    }

    public static int insertIntoTarget(TileEntity target, String aspectTag, int amount) {
        if (!isAvailable() || target == null || aspectTag == null || amount <= 0) return 0;
        Object value = invoke("insertIntoTarget", target, aspectTag, amount);
        return value instanceof Number ? Math.max(0, ((Number) value).intValue()) : 0;
    }

    public static void requestMarkedEssentia(TileCommonInterfaceAlternate tile, boolean extended) {
        if (isAvailable()) invoke("requestMarkedEssentia", tile, extended);
    }

    public static void flushUnconfiguredEssentiaToNetwork(TileCommonInterfaceAlternate tile) {
        if (isAvailable()) invoke("flushUnconfiguredEssentiaToNetwork", tile);
    }

    public static int getNetworkEssentiaAmount(TileCommonInterfaceAlternate tile,
                                               String aspectTag) {
        Object value = isAvailable()
                ? invoke("getNetworkEssentiaAmount", tile, aspectTag) : null;
        return value instanceof Number ? Math.max(0, ((Number) value).intValue()) : 0;
    }

    public static int extractNetworkEssentia(TileCommonInterfaceAlternate tile,
                                             String aspectTag, int amount,
                                             appeng.api.config.Actionable mode) {
        Object value = isAvailable()
                ? invoke("extractNetworkEssentia", tile, aspectTag, amount, mode) : null;
        return value instanceof Number ? Math.max(0, ((Number) value).intValue()) : 0;
    }

    public static boolean hasEssentiaWork(TileCommonInterfaceAlternate tile) {
        return isAvailable() && Boolean.TRUE.equals(invoke("hasEssentiaWork", tile));
    }

    private static Object invoke(String name, Object... args) {
        try {
            Class<?> type = Class.forName(OPTIONAL_IMPL);
            for (Method method : type.getMethods()) {
                if (!Modifier.isStatic(method.getModifiers()) || !method.getName().equals(name)
                        || method.getParameterTypes().length != args.length
                        || !matches(method.getParameterTypes(), args)) continue;
                return method.invoke(null, args);
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static boolean matches(Class<?>[] types, Object[] args) {
        for (int i = 0; i < types.length; i++) {
            if (args[i] == null) {
                if (types[i].isPrimitive()) return false;
            } else if (!wrap(types[i]).isAssignableFrom(args[i].getClass())) return false;
        }
        return true;
    }

    private static Class<?> wrap(Class<?> type) {
        if (!type.isPrimitive()) return type;
        if (type == boolean.class) return Boolean.class;
        if (type == int.class) return Integer.class;
        return type;
    }
}
