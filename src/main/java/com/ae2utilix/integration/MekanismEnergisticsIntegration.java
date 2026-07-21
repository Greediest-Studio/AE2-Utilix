package com.ae2utilix.integration;

import appeng.api.networking.energy.IEnergySource;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.IStorageChannel;
import appeng.me.GridAccessException;
import com.ae2utilix.block.TileCommonInterfaceAlternate;
import com.ae2utilix.item.ItemFluidMark;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fml.common.Loader;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/** Safe facade for the optional Mekanism Energistics integration. */
public final class MekanismEnergisticsIntegration {
    private static final String OPTIONAL_IMPL =
            "com.ae2utilix.integration.MekanismEnergisticsOptional";

    private MekanismEnergisticsIntegration() {
    }

    public static boolean isAvailable() {
        return Loader.isModLoaded("mekanism") && Loader.isModLoaded("mekeng");
    }

    public static boolean isGasChannel(IStorageChannel<?> channel) {
        if (!isAvailable() || channel == null) return false;
        try {
            Class<?> type = Class.forName("com.mekeng.github.common.me.storage.IGasStorageChannel");
            @SuppressWarnings({"rawtypes", "unchecked"})
            IStorageChannel<?> expected = appeng.api.AEApi.instance().storage()
                    .getStorageChannel((Class) type);
            return channel == expected;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static boolean isGasCapability(Capability<?> capability) {
        if (!isAvailable() || capability == null) return false;
        try {
            Class<?> type = Class.forName("mekanism.common.capabilities.Capabilities");
            Field field = type.getField("GAS_HANDLER_CAPABILITY");
            return capability == field.get(null);
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Nullable
    public static String getGasNameFromItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        String marker = ItemFluidMark.getGasName(stack);
        if (marker != null) return marker;
        if (!isAvailable()) return null;
        Object result = invoke("getGasNameFromItem", stack);
        return result instanceof String ? (String) result : null;
    }

    @Nullable
    public static String getGasDisplayName(String gasName) {
        if (!isAvailable() || gasName == null) return null;
        Object result = invoke("getGasDisplayName", gasName);
        return result instanceof String ? (String) result : null;
    }

    @Nullable
    public static IMEMonitor<?> getMonitor(TileCommonInterfaceAlternate tile, boolean configured) {
        if (!isAvailable() || tile == null) return null;
        Object result = invoke("getMonitor", tile, configured);
        return result instanceof IMEMonitor ? (IMEMonitor<?>) result : null;
    }

    @Nullable
    public static Object getGasHandler(TileCommonInterfaceAlternate tile) {
        return isAvailable() && tile != null ? invoke("getGasHandler", tile) : null;
    }

    @Nullable
    public static Object drawLocalGas(TileCommonInterfaceAlternate tile, int amount, boolean doTransfer) {
        if (!isAvailable() || tile == null || amount <= 0) return null;
        return invoke("drawLocalGas", tile, amount, doTransfer);
    }

    @Nullable
    public static Object drawLocalGas(TileCommonInterfaceAlternate tile, Object requested,
            boolean doTransfer) {
        if (!isAvailable() || tile == null || requested == null) return null;
        return invoke("drawLocalGas", tile, requested, doTransfer);
    }

    public static int receiveLocalGas(TileCommonInterfaceAlternate tile, Object stack,
            boolean doTransfer) {
        if (!isAvailable() || tile == null || stack == null) return 0;
        Object result = invoke("receiveLocalGas", tile, stack, doTransfer);
        return result instanceof Number ? ((Number) result).intValue() : 0;
    }

    public static int insertGasToNetwork(IStorageGrid storage, IEnergySource energy,
            IActionSource source, Object stack, boolean simulate) {
        if (!isAvailable() || storage == null || energy == null || source == null || stack == null) return 0;
        Object result = invoke("insertGasToNetwork", storage, energy, source, stack, simulate);
        return result instanceof Number ? ((Number) result).intValue() : 0;
    }

    @Nullable
    public static Object extractGasFromNetwork(IStorageGrid storage, IEnergySource energy,
            IActionSource source, String gasName, int amount, boolean simulate) {
        if (!isAvailable() || storage == null || energy == null || source == null
                || gasName == null || amount <= 0) return null;
        Object gas = getGas(gasName);
        return gas == null ? null : invoke("extractGasFromNetwork", storage, energy, source,
                gas, amount, simulate);
    }

    public static void requestMarkedGases(TileCommonInterfaceAlternate tile, boolean extended) {
        if (isAvailable()) invoke("requestMarkedGases", tile, extended);
    }

    public static boolean hasGasWork(TileCommonInterfaceAlternate tile) {
        return isAvailable() && Boolean.TRUE.equals(invoke("hasGasWork", tile));
    }

    public static void flushUnconfiguredGasesToNetwork(TileCommonInterfaceAlternate tile) {
        if (isAvailable()) invoke("flushUnconfiguredGasesToNetwork", tile);
    }

    @Nullable
    private static Object getGas(String gasName) {
        try {
            Class<?> registry = Class.forName("mekanism.api.gas.GasRegistry");
            return registry.getMethod("getGas", String.class).invoke(null, gasName);
        } catch (Throwable ignored) {
            return null;
        }
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
                continue;
            }
            if (!wrap(types[i]).isAssignableFrom(args[i].getClass())) return false;
        }
        return true;
    }

    private static Class<?> wrap(Class<?> type) {
        if (!type.isPrimitive()) return type;
        if (type == boolean.class) return Boolean.class;
        if (type == int.class) return Integer.class;
        if (type == long.class) return Long.class;
        if (type == double.class) return Double.class;
        if (type == float.class) return Float.class;
        if (type == short.class) return Short.class;
        if (type == byte.class) return Byte.class;
        if (type == char.class) return Character.class;
        return type;
    }
}
