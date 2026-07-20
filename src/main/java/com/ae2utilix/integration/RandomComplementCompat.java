package com.ae2utilix.integration;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.Loader;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

public final class RandomComplementCompat {

    private static final String SETTINGS = "com.circulation.random_complement.client.RCSettings";
    private static final String AUTO_FILL = "com.circulation.random_complement.client.buttonsetting.PatternTermAutoFillPattern";
    private static final String MANAGER = "com.circulation.random_complement.common.util.RCConfigManager";

    private static boolean initialized;
    private static Class<?> settingsClass;
    private static Class<?> autoFillClass;
    private static Constructor<?> managerConstructor;
    private static Method registerSetting;
    private static Method getSetting;
    private static Method readFromNBT;
    private static Method writeToNBT;

    private RandomComplementCompat() {
    }

    private static void initialize() {
        if (initialized) return;
        initialized = true;
        if (!Loader.isModLoaded("random_complement")) return;
        try {
            settingsClass = Class.forName(SETTINGS);
            autoFillClass = Class.forName(AUTO_FILL);
            Class<?> managerClass = Class.forName(MANAGER);
            Class<?> hostClass = Class.forName("com.circulation.random_complement.common.interfaces.RCIConfigManagerHost");
            managerConstructor = managerClass.getConstructor(hostClass);
            registerSetting = managerClass.getMethod("registerSetting", settingsClass, Enum.class);
            getSetting = managerClass.getMethod("getSetting", settingsClass);
            readFromNBT = managerClass.getMethod("readFromNBT", NBTTagCompound.class);
            writeToNBT = managerClass.getMethod("writeToNBT", NBTTagCompound.class);
        } catch (ReflectiveOperationException ignored) {
            settingsClass = null;
            autoFillClass = null;
            managerConstructor = null;
        }
    }

    public static boolean isLoaded() {
        initialize();
        return managerConstructor != null;
    }

    public static Object createManager(final Object host) {
        initialize();
        if (managerConstructor == null) return null;
        try {
            Class<?> hostClass = managerConstructor.getParameterTypes()[0];
            Object hostProxy = Proxy.newProxyInstance(hostClass.getClassLoader(), new Class<?>[]{hostClass}, (proxy, method, args) -> {
                if ("r$updateSetting".equals(method.getName()) && host instanceof Runnable) {
                    ((Runnable) host).run();
                }
                return null;
            });
            return managerConstructor.newInstance(hostProxy);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    public static void registerAutoFill(Object manager) {
        if (manager == null || registerSetting == null) return;
        try {
            Object setting = Enum.valueOf((Class) settingsClass, "PatternTermAutoFillPattern");
            Object close = Enum.valueOf((Class) autoFillClass, "CLOSE");
            registerSetting.invoke(manager, setting, close);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    public static String getAutoFillName(Object manager) {
        if (manager == null || getSetting == null) return "CLOSE";
        try {
            Object setting = Enum.valueOf((Class) settingsClass, "PatternTermAutoFillPattern");
            Object value = getSetting.invoke(manager, setting);
            return value instanceof Enum ? ((Enum<?>) value).name() : "CLOSE";
        } catch (ReflectiveOperationException ignored) {
            return "CLOSE";
        }
    }

    public static void read(Object manager, NBTTagCompound tag) {
        if (manager == null || readFromNBT == null) return;
        try {
            readFromNBT.invoke(manager, tag);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    public static void write(Object manager, NBTTagCompound tag) {
        if (manager == null || writeToNBT == null) return;
        try {
            writeToNBT.invoke(manager, tag);
        } catch (ReflectiveOperationException ignored) {
        }
    }
}
