package com.ae2utilix.integration;

import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import appeng.client.gui.widgets.GuiImgButton;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidStack;
import appeng.tile.inventory.AppEngInternalInventory;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Reflection-based compatibility layer for AE2FCRU.
 * All AE2FCRU class access goes through this class to prevent NoClassDefFoundError
 * when old AE2FC (without the required API) is installed.
 */
public class AE2FCRUCompat {

    private static Boolean loaded = null;

    public static boolean isLoaded() {
        if (loaded != null) return loaded;
        try {
            Class.forName("com.glodblock.github.interfaces.FCFluidPatternContainer");
            Class.forName("com.glodblock.github.interfaces.FCFluidPatternPart");
            Class.forName("com.glodblock.github.common.item.fake.FakeFluids");
            Class.forName("com.glodblock.github.loader.FCItems");
            Class.forName("com.glodblock.github.util.Util");
            Class.forName("com.glodblock.github.FluidCraft");
            loaded = true;
        } catch (ClassNotFoundException e) {
            loaded = false;
        }
        return loaded;
    }

    // ---- FakeFluids ----

    public static boolean isFluidFakeItem(ItemStack stack) {
        if (!isLoaded() || stack.isEmpty()) return false;
        try {
            Class<?> cls = Class.forName("com.glodblock.github.common.item.fake.FakeFluids");
            Method m = cls.getMethod("isFluidFakeItem", ItemStack.class);
            return (boolean) m.invoke(null, stack);
        } catch (Exception e) {
            return false;
        }
    }

    @Nullable
    public static ItemStack packFluid2Drops(FluidStack fluid) {
        if (!isLoaded() || fluid == null) return null;
        try {
            Class<?> cls = Class.forName("com.glodblock.github.common.item.fake.FakeFluids");
            Method m = cls.getMethod("packFluid2Drops", FluidStack.class);
            return (ItemStack) m.invoke(null, fluid);
        } catch (Exception e) {
            return null;
        }
    }

    // ---- Util ----

    @Nullable
    public static FluidStack getFluidFromItem(ItemStack stack) {
        if (!isLoaded() || stack.isEmpty()) return null;
        try {
            Class<?> cls = Class.forName("com.glodblock.github.util.Util");
            Method m = cls.getMethod("getFluidFromItem", ItemStack.class);
            return (FluidStack) m.invoke(null, stack);
        } catch (Exception e) {
            return null;
        }
    }

    public static void clearItemInventory(AppEngInternalInventory inv) {
        for (int i = 0; i < inv.getSlots(); i++) {
            inv.setStackInSlot(i, ItemStack.EMPTY);
        }
    }

    public static int findMax(it.unimi.dsi.fastutil.ints.IntSet keys) {
        int max = 0;
        for (int key : keys) {
            if (key > max) max = key;
        }
        return max;
    }

    public static void fuzzyTransferItems(int index, ItemStack[] src, ItemStack[] dest, IItemList<IAEItemStack> storageList) {
        if (!isLoaded()) return;
        try {
            Class<?> cls = Class.forName("com.glodblock.github.util.Util");
            Method m = cls.getMethod("fuzzyTransferItems", int.class, ItemStack[].class, ItemStack[].class, IItemList.class);
            m.invoke(null, index, src, dest, storageList);
        } catch (Exception ignored) {
        }
    }

    public static ItemStack[] compress(ItemStack[] items) {
        if (!isLoaded()) return items;
        try {
            Class<?> cls = Class.forName("com.glodblock.github.util.Util");
            Method m = cls.getMethod("compress", ItemStack[].class);
            return (ItemStack[]) m.invoke(null, (Object) items);
        } catch (Exception e) {
            return items;
        }
    }

    // ---- FCItems ----

    @Nullable
    public static Item getDenseCraftEncodedPattern() {
        if (!isLoaded()) return null;
        try {
            Class<?> cls = Class.forName("com.glodblock.github.loader.FCItems");
            Field f = cls.getField("DENSE_CRAFT_ENCODED_PATTERN");
            return (Item) f.get(null);
        } catch (Exception e) {
            return null;
        }
    }

    @Nullable
    public static Item getDenseEncodedPattern() {
        if (!isLoaded()) return null;
        try {
            Class<?> cls = Class.forName("com.glodblock.github.loader.FCItems");
            Field f = cls.getField("DENSE_ENCODED_PATTERN");
            return (Item) f.get(null);
        } catch (Exception e) {
            return null;
        }
    }

    // ---- Pattern type checks ----

    public static boolean isFluidEncodedPattern(ItemStack stack) {
        if (!isLoaded() || stack.isEmpty()) return false;
        try {
            Class<?> cls = Class.forName("com.glodblock.github.common.item.ItemFluidEncodedPattern");
            if (cls.isInstance(stack.getItem())) return true;
        } catch (Exception ignored) {
        }
        try {
            Class<?> cls = Class.forName("com.glodblock.github.common.item.ItemFluidCraftEncodedPattern");
            if (cls.isInstance(stack.getItem())) return true;
        } catch (Exception ignored) {
        }
        try {
            Class<?> cls = Class.forName("com.glodblock.github.common.item.ItemLargeEncodedPattern");
            if (cls.isInstance(stack.getItem())) return true;
        } catch (Exception ignored) {
        }
        return false;
    }

    public static boolean isFluidCraftingPatternDetails(Object obj) {
        if (!isLoaded() || obj == null) return false;
        try {
            Class<?> cls = Class.forName("com.glodblock.github.util.FluidCraftingPatternDetails");
            return cls.isInstance(obj);
        } catch (Exception e) {
            return false;
        }
    }

    @Nullable
    public static IAEItemStack[] getOriginInputs(Object details) {
        if (details == null) return null;
        try {
            Method m = details.getClass().getMethod("getOriginInputs");
            return (IAEItemStack[]) m.invoke(details);
        } catch (Exception e) {
            return null;
        }
    }

    // ---- FluidCraftingPatternDetails ----

    @Nullable
    public static Object getFluidCraftingPatternDetails(ItemStack patternStack, World world) {
        if (!isLoaded()) return null;
        try {
            Class<?> cls = Class.forName("com.glodblock.github.util.FluidCraftingPatternDetails");
            Method m = cls.getMethod("GetFluidPattern", ItemStack.class, World.class);
            return m.invoke(null, patternStack, world);
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean isFluidPatternNecessary(Object details) {
        if (details == null) return false;
        try {
            Method m = details.getClass().getMethod("isNecessary");
            return (boolean) m.invoke(details);
        } catch (Exception e) {
            return false;
        }
    }

    // ---- FluidPatternDetails (encoding) ----

    @Nullable
    public static ItemStack encodeFluidPattern(ItemStack patternStack, IAEItemStack[] inputs, IAEItemStack[] outputs, UUID encoder) {
        if (!isLoaded()) return null;
        try {
            Class<?> cls = Class.forName("com.glodblock.github.util.FluidPatternDetails");
            Object pattern = cls.getConstructor(ItemStack.class).newInstance(patternStack);
            Method setInputs = cls.getMethod("setInputs", IAEItemStack[].class);
            Method setOutputs = cls.getMethod("setOutputs", IAEItemStack[].class);
            Method setEncoder = cls.getMethod("setEncoder", UUID.class);
            Method writeToStack = cls.getMethod("writeToStack");
            setInputs.invoke(pattern, (Object) inputs);
            setOutputs.invoke(pattern, (Object) outputs);
            setEncoder.invoke(pattern, encoder);
            return (ItemStack) writeToStack.invoke(pattern);
        } catch (Exception e) {
            return null;
        }
    }

    // ---- Network packets ----

    public static void sendFluidPatternBtns(String name, String value) {
        if (!isLoaded()) return;
        try {
            Class<?> fluidCraftCls = Class.forName("com.glodblock.github.FluidCraft");
            Object proxy = fluidCraftCls.getField("proxy").get(null);
            Object netHandler = proxy.getClass().getField("netHandler").get(proxy);
            Class<?> packetCls = Class.forName("com.glodblock.github.network.CPacketFluidPatternTermBtns");
            Object packet = packetCls.getConstructor(String.class, String.class).newInstance(name, value);
            Method sendMethod = netHandler.getClass().getMethod("sendToServer", appeng.core.sync.AppEngPacket.class);
            sendMethod.invoke(netHandler, packet);
        } catch (Exception ignored) {
        }
    }

    // ---- GUI buttons ----

    @Nullable
    public static GuiImgButton createGuiFCImgButton(int x, int y, String buttonType, String actionType) {
        if (!isLoaded()) return null;
        try {
            Class<?> cls = Class.forName("com.glodblock.github.client.button.GuiFCImgButton");
            Object btn = cls.getConstructor(int.class, int.class, String.class, String.class).newInstance(x, y, buttonType, actionType);
            return (GuiImgButton) btn;
        } catch (Exception e) {
            return null;
        }
    }

    // ---- JEI recipe transfer ----

    public static void sendRecipeTransfer(Object container, boolean fluidFirst, boolean combine,
                                          Object recipeLayout, boolean craftMode) {
        if (!isLoaded()) return;
        try {
            Class<?> builderCls = Class.forName("com.glodblock.github.integration.jei.RecipeTransferBuilder");
            Object builder = builderCls.getConstructor(Object.class).newInstance(recipeLayout);
            Method clearEmptySlot = builderCls.getMethod("clearEmptySlot", boolean.class);
            clearEmptySlot.invoke(builder, !craftMode);
            Method putFluidFirst = builderCls.getMethod("putFluidFirst", boolean.class);
            putFluidFirst.invoke(builder, fluidFirst);
            Method build = builderCls.getMethod("build");
            Object transfer = build.invoke(builder);

            Method getInput = transfer.getClass().getMethod("getInput");
            Method getOutput = transfer.getClass().getMethod("getOutput");
            Object input = getInput.invoke(transfer);
            Object output = getOutput.invoke(transfer);

            Class<?> fluidCraftCls = Class.forName("com.glodblock.github.FluidCraft");
            Object proxy = fluidCraftCls.getField("proxy").get(null);
            Object netHandler = proxy.getClass().getField("netHandler").get(proxy);
            Class<?> packetCls = Class.forName("com.glodblock.github.network.CPacketLoadPattern");
            Object packet = packetCls.getConstructor(input.getClass(), output.getClass(), boolean.class)
                    .newInstance(input, output, combine);
            Method sendMethod = netHandler.getClass().getMethod("sendToServer", appeng.core.sync.AppEngPacket.class);
            sendMethod.invoke(netHandler, packet);
        } catch (Exception ignored) {
        }
    }
}
