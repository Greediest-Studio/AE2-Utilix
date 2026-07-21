package com.ae2utilix.integration;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.networking.energy.IEnergySource;
import appeng.api.storage.IMEMonitor;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.storage.IStorageChannel;
import com.ae2utilix.block.TileCommonInterfaceAlternate;
import com.ae2utilix.item.ItemFluidMark;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fml.common.Loader;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Safe entry point for Botania Applie and Flux Applied.
 *
 * The optional implementations are deliberately reached through reflection.
 * A missing optional mod must not make a common interface, its GUI, or its
 * marker item unloadable just because a method contains that mod's types.
 */
public final class BotaniaFluxIntegration {
    public static final int MANA = 1;
    public static final int FE = 2;

    private static final String OPTIONAL_IMPL =
            "com.ae2utilix.integration.BotaniaFluxOptional";

    private BotaniaFluxIntegration() {
    }

    public static boolean isManaIntegrationAvailable() {
        return Loader.isModLoaded("botania") && Loader.isModLoaded("botania_applie");
    }

    public static boolean isFeIntegrationAvailable() {
        return Loader.isModLoaded("flux_applied");
    }

    public static boolean isVirtualPacket(ItemStack stack) {
        return getMarkedType(stack) != 0 && isPacket(stack, getMarkedType(stack));
    }

    public static int getMarkedType(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;
        if (isManaIntegrationAvailable()
                && (isPacket(stack, MANA) || isInstance(stack, "vazkii.botania.api.mana.IManaItem"))) {
            return MANA;
        }
        if (isFeIntegrationAvailable()
                && (isPacket(stack, FE) || stack.hasCapability(CapabilityEnergy.ENERGY, null))) {
            return FE;
        }
        return 0;
    }

    public static boolean isManaMark(ItemStack stack) {
        return ItemFluidMark.isManaMark(stack);
    }

    public static boolean isFeMark(ItemStack stack) {
        return ItemFluidMark.isFeMark(stack);
    }

    public static boolean isManaChannel(IStorageChannel<?> channel) {
        return isChannel(channel, MANA, "nyonio.ae2.ManaStorageChannel");
    }

    public static boolean isFeChannel(IStorageChannel<?> channel) {
        return isChannel(channel, FE, "com.flux_applied.ae2.FluxStorageChannel");
    }

    public static boolean hasManaConfig(TileCommonInterfaceAlternate tile) {
        return isManaIntegrationAvailable() && invokeBoolean("hasManaConfig", tile);
    }

    public static boolean hasFeConfig(TileCommonInterfaceAlternate tile) {
        return isFeIntegrationAvailable() && invokeBoolean("hasFeConfig", tile);
    }

    @Nullable
    public static String getDisplayName(int type) {
        if (type == MANA) return "Mana";
        if (type == FE) return "FE";
        return null;
    }

    public static String getStoredTooltip(int type, long amount) {
        String name = type == MANA ? "Mana" : "FE";
        return "\u00a78\u5b58\u50a8\u4e86 \u00a77" + name + " " + amount;
    }

    @Nullable
    public static ItemStack getPacketStack(int type) {
        if ((type == MANA && !isManaIntegrationAvailable())
                || (type == FE && !isFeIntegrationAvailable())) return null;
        String className = type == MANA
                ? "nyonio.item.ItemManaPacket" : "com.flux_applied.item.ItemFluxPacket";
        try {
            Class<?> itemClass = Class.forName(className);
            Object result = itemClass.getMethod("create", long.class).invoke(null, 0L);
            return result instanceof ItemStack ? (ItemStack) result : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static String getPacketDisplayName(int type) {
        ItemStack stack = getPacketStack(type);
        return stack == null ? getDisplayName(type) : stack.getDisplayName();
    }

    @Nullable
    public static IMEMonitor<?> getManaMonitor(TileCommonInterfaceAlternate tile) {
        if (!isManaIntegrationAvailable()) return null;
        Object monitor = invoke("getManaMonitor", tile);
        return monitor instanceof IMEMonitor ? (IMEMonitor<?>) monitor : null;
    }

    @Nullable
    public static IMEMonitor<?> getFeMonitor(TileCommonInterfaceAlternate tile) {
        if (!isFeIntegrationAvailable()) return null;
        Object monitor = invoke("getFeMonitor", tile);
        return monitor instanceof IMEMonitor ? (IMEMonitor<?>) monitor : null;
    }

    public static void requestMarked(TileCommonInterfaceAlternate tile, int type) {
        if ((type == MANA && !isManaIntegrationAvailable())
                || (type == FE && !isFeIntegrationAvailable())) return;
        invoke("requestMarked", tile, type);
    }

    public static void flushUnconfigured(TileCommonInterfaceAlternate tile, int type) {
        if ((type == MANA && !isManaIntegrationAvailable())
                || (type == FE && !isFeIntegrationAvailable())) return;
        invoke("flushUnconfigured", tile, type);
    }

    public static long getCurrentMana(TileCommonInterfaceAlternate tile) {
        return isManaIntegrationAvailable() ? invokeLong("getCurrentMana", tile) : 0;
    }

    public static long getCurrentFe(TileCommonInterfaceAlternate tile) {
        return isFeIntegrationAvailable() ? invokeLong("getCurrentFe", tile) : 0;
    }

    public static long getCapacity(TileCommonInterfaceAlternate tile, int type) {
        if ((type == MANA && !isManaIntegrationAvailable())
                || (type == FE && !isFeIntegrationAvailable())) return 0;
        return invokeLong("getCapacity", tile, type);
    }

    public static long insertNetwork(TileCommonInterfaceAlternate tile, int type,
            long amount, Actionable mode) {
        if (amount <= 0 || !isAvailable(type)) return 0;
        return invokeLong("insertNetwork", tile, type, amount, mode);
    }

    public static long extractNetwork(TileCommonInterfaceAlternate tile, int type,
            long amount, Actionable mode) {
        if (amount <= 0 || !isAvailable(type)) return 0;
        return invokeLong("extractNetwork", tile, type, amount, mode);
    }

    public static long insertNetwork(IStorageGrid storage, IEnergySource energy,
            IActionSource source, int type, long amount, Actionable mode) {
        if (amount <= 0 || !isAvailable(type)) return 0;
        return invokeLong("insertNetwork", storage, energy, source, type, amount, mode);
    }

    public static long extractNetwork(IStorageGrid storage, IEnergySource energy,
            IActionSource source, int type, long amount, Actionable mode) {
        if (amount <= 0 || !isAvailable(type)) return 0;
        return invokeLong("extractNetwork", storage, energy, source, type, amount, mode);
    }

    public static int receiveMana(TileCommonInterfaceAlternate tile, int amount, boolean simulate) {
        return isManaIntegrationAvailable()
                ? invokeInt("receiveMana", tile, amount, simulate) : 0;
    }

    public static int extractMana(TileCommonInterfaceAlternate tile, int amount, boolean simulate) {
        return isManaIntegrationAvailable()
                ? invokeInt("extractMana", tile, amount, simulate) : 0;
    }

    public static int receiveManaLocal(TileCommonInterfaceAlternate tile, int amount, boolean simulate) {
        return isManaIntegrationAvailable()
                ? invokeInt("receiveManaLocal", tile, amount, simulate) : 0;
    }

    public static int extractManaLocal(TileCommonInterfaceAlternate tile, int amount, boolean simulate) {
        return isManaIntegrationAvailable()
                ? invokeInt("extractManaLocal", tile, amount, simulate) : 0;
    }

    public static int receiveFeLocal(TileCommonInterfaceAlternate tile, int amount, boolean simulate) {
        return isFeIntegrationAvailable()
                ? invokeInt("receiveFeLocal", tile, amount, simulate) : 0;
    }

    public static int extractFeLocal(TileCommonInterfaceAlternate tile, int amount, boolean simulate) {
        return isFeIntegrationAvailable()
                ? invokeInt("extractFeLocal", tile, amount, simulate) : 0;
    }

    public static IEnergyStorage getEnergyHandler(TileCommonInterfaceAlternate tile) {
        if (!isFeIntegrationAvailable()) return EmptyEnergyStorage.INSTANCE;
        Object handler = invoke("getEnergyHandler", tile);
        return handler instanceof IEnergyStorage ? (IEnergyStorage) handler : EmptyEnergyStorage.INSTANCE;
    }

    public static int extractManaFromTarget(TileEntity target, int amount) {
        if (target == null || amount <= 0) return 0;
        if (isFluixReceiver(target)) {
            Object result = invokeStatic("nyonio.FluixPoolManaHelper", "extract", target, amount);
            if (result instanceof Number) return Math.max(0, ((Number) result).intValue());
        }
        if (!(target instanceof vazkii.botania.api.mana.IManaReceiver)) return 0;
        vazkii.botania.api.mana.IManaReceiver receiver =
                (vazkii.botania.api.mana.IManaReceiver) target;
        int before = Math.max(0, receiver.getCurrentMana());
        int toExtract = Math.min(amount, before);
        if (toExtract <= 0) return 0;
        receiver.recieveMana(-toExtract);
        return Math.max(0, before - receiver.getCurrentMana());
    }

    public static void insertManaIntoTarget(TileEntity target, int amount) {
        if (target == null || amount <= 0) return;
        if (isFluixReceiver(target)
                && invokeStatic("nyonio.FluixPoolManaHelper", "insert", target, amount) != null) return;
        if (target instanceof vazkii.botania.api.mana.IManaReceiver) {
            ((vazkii.botania.api.mana.IManaReceiver) target).recieveMana(amount);
        }
    }

    private static boolean isAvailable(int type) {
        return type == MANA ? isManaIntegrationAvailable() : type == FE && isFeIntegrationAvailable();
    }

    private static boolean isChannel(IStorageChannel<?> channel, int type, String className) {
        if (!isAvailable(type) || channel == null) return false;
        try {
            Class<?> storageClass = Class.forName(className);
            @SuppressWarnings({"rawtypes", "unchecked"})
            IStorageChannel<?> expected = AEApi.instance().storage()
                    .getStorageChannel((Class) storageClass);
            return channel == expected;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean isPacket(ItemStack stack, int type) {
        String className = type == MANA
                ? "nyonio.item.ItemManaPacket" : "com.flux_applied.item.ItemFluxPacket";
        try {
            Class<?> itemClass = Class.forName(className);
            return Boolean.TRUE.equals(itemClass.getMethod(
                    type == MANA ? "isManaPacket" : "isFluxPacket", ItemStack.class)
                    .invoke(null, stack));
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean isInstance(ItemStack stack, String className) {
        try {
            return Class.forName(className).isInstance(stack.getItem());
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean isFluixReceiver(Object target) {
        try {
            return Class.forName("nyonio.IFluixManaReceiver").isInstance(target);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static Object invokeStatic(String className, String name, Object... args) {
        try {
            return invoke(Class.forName(className), name, args);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object invoke(String name, Object... args) {
        try {
            return invoke(Class.forName(OPTIONAL_IMPL), name, args);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object invoke(Class<?> type, String name, Object... args) throws Exception {
        for (Method method : type.getMethods()) {
            if (!Modifier.isStatic(method.getModifiers()) || !method.getName().equals(name)
                    || method.getParameterTypes().length != args.length) continue;
            if (!matches(method.getParameterTypes(), args)) continue;
            return method.invoke(null, args);
        }
        return null;
    }

    private static boolean invokeBoolean(String name, Object... args) {
        Object result = invoke(name, args);
        return Boolean.TRUE.equals(result);
    }

    private static int invokeInt(String name, Object... args) {
        Object result = invoke(name, args);
        return result instanceof Number ? ((Number) result).intValue() : 0;
    }

    private static long invokeLong(String name, Object... args) {
        Object result = invoke(name, args);
        return result instanceof Number ? ((Number) result).longValue() : 0;
    }

    private static boolean matches(Class<?>[] types, Object[] args) {
        for (int i = 0; i < types.length; i++) {
            if (args[i] == null) {
                if (types[i].isPrimitive()) return false;
                continue;
            }
            Class<?> expected = wrap(types[i]);
            if (!expected.isAssignableFrom(args[i].getClass())) return false;
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

    private enum EmptyEnergyStorage implements IEnergyStorage {
        INSTANCE;

        @Override public int receiveEnergy(int maxReceive, boolean simulate) { return 0; }
        @Override public int extractEnergy(int maxExtract, boolean simulate) { return 0; }
        @Override public int getEnergyStored() { return 0; }
        @Override public int getMaxEnergyStored() { return 0; }
        @Override public boolean canExtract() { return false; }
        @Override public boolean canReceive() { return false; }
    }
}
