package com.ae2utilix.integration;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.config.SecurityPermissions;
import appeng.api.features.IWirelessTermHandler;
import appeng.api.implementations.items.IMemoryCard;
import appeng.api.implementations.items.MemoryCardMessages;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.security.ISecurityGrid;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.helpers.WirelessTerminalGuiObject;
import appeng.util.item.AEItemStack;
import baubles.api.BaublesApi;
import baubles.api.cap.IBaublesItemHandler;
import com.ae2utilix.block.TileCrystalGrowthChamber;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nullable;

public class CGCMemoryCardHandler {

    private static final boolean HAS_BAUBLES = Loader.isModLoaded("baubles");

    private static final String DATA_EJECTING = "cgc_ejecting";
    private static final String DATA_FACE_EJECT = "cgc_faceEject";
    private static final String DATA_SPEED_CARDS = "cgc_speedCards";
    private static final String SETTINGS_NAME = "ae2_utilix.cgc_config";

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getWorld().isRemote) return;

        EntityPlayer player = event.getEntityPlayer();
        ItemStack heldItem = player.getHeldItem(event.getHand());

        TileEntity te = event.getWorld().getTileEntity(event.getPos());
        if (!(te instanceof TileCrystalGrowthChamber)) return;

        TileCrystalGrowthChamber cgc = (TileCrystalGrowthChamber) te;

        if (player.isSneaking() && isUpgradeCard(heldItem)) {
            event.setCanceled(true);
            insertSpeedCard(cgc, heldItem, player);
            return;
        }

        if (heldItem.isEmpty() || !(heldItem.getItem() instanceof IMemoryCard)) return;

        event.setCanceled(true);

        IMemoryCard memoryCard = (IMemoryCard) heldItem.getItem();

        if (player.isSneaking()) {
            copyConfig(memoryCard, heldItem, cgc, player);
        } else {
            pasteConfig(memoryCard, heldItem, cgc, player);
        }
    }

    private static boolean isUpgradeCard(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return AEApi.instance().definitions().materials().cardSpeed().isSameAs(stack)
                || stack.getItem() instanceof com.ae2utilix.item.ItemParallelCard;
    }

    private static void insertSpeedCard(TileCrystalGrowthChamber cgc, ItemStack heldItem, EntityPlayer player) {
        IItemHandler upgradeInv = cgc.getUpgradeInv();
        ItemStack template = heldItem.copy();
        template.setCount(1);

        for (int i = 0; i < upgradeInv.getSlots() && heldItem.getCount() > 0; i++) {
            if (upgradeInv.getStackInSlot(i).isEmpty() && upgradeInv.isItemValid(i, template)) {
                upgradeInv.insertItem(i, template, false);
                heldItem.shrink(1);
            }
        }
    }

    private static void copyConfig(IMemoryCard memoryCard, ItemStack card,
                                   TileCrystalGrowthChamber cgc, EntityPlayer player) {
        NBTTagCompound data = new NBTTagCompound();

        data.setBoolean(DATA_EJECTING, cgc.isEjecting());

        boolean[] faceEject = new boolean[6];
        for (int i = 0; i < 6; i++) {
            faceEject[i] = cgc.isFaceEjecting(i);
        }
        data.setByteArray(DATA_FACE_EJECT, packBooleans(faceEject));

        data.setInteger(DATA_SPEED_CARDS, cgc.getSpeedCardCount());

        memoryCard.setMemoryCardContents(card, SETTINGS_NAME, data);
        memoryCard.notifyUser(player, MemoryCardMessages.SETTINGS_SAVED);
    }

    private static void pasteConfig(IMemoryCard memoryCard, ItemStack card,
                                    TileCrystalGrowthChamber cgc, EntityPlayer player) {
        NBTTagCompound data = memoryCard.getData(card);
        if (data.hasNoTags()) {
            memoryCard.notifyUser(player, MemoryCardMessages.INVALID_MACHINE);
            return;
        }

        if (data.hasKey(DATA_EJECTING)) {
            cgc.setEjecting(data.getBoolean(DATA_EJECTING));
        }

        if (data.hasKey(DATA_FACE_EJECT)) {
            boolean[] faceEject = new boolean[6];
            unpackBooleans(data.getByteArray(DATA_FACE_EJECT), faceEject);
            for (int i = 0; i < 6; i++) {
                cgc.setFaceEjecting(i, faceEject[i]);
            }
        }

        if (data.hasKey(DATA_SPEED_CARDS)) {
            int targetCount = data.getInteger(DATA_SPEED_CARDS);
            adjustSpeedCards(cgc, targetCount, player);
        }

        memoryCard.notifyUser(player, MemoryCardMessages.SETTINGS_LOADED);
    }

    private static void adjustSpeedCards(TileCrystalGrowthChamber cgc, int targetCount, EntityPlayer player) {
        int currentCount = cgc.getSpeedCardCount();
        if (currentCount == targetCount) return;

        ItemStack speedCardTemplate = getSpeedCardTemplate();
        if (speedCardTemplate.isEmpty()) return;

        IItemHandler upgradeInv = cgc.getUpgradeInv();

        if (targetCount > currentCount) {
            int needed = targetCount - currentCount;
            ItemStack extracted = extractSpeedCardsFromSource(player, speedCardTemplate, needed);
            if (extracted.isEmpty()) return;

            int remaining = extracted.getCount();
            for (int i = 0; i < upgradeInv.getSlots() && remaining > 0; i++) {
                if (upgradeInv.getStackInSlot(i).isEmpty()) {
                    ItemStack toInsert = extracted.copy();
                    toInsert.setCount(Math.min(remaining, 1));
                    ItemStack leftover = upgradeInv.insertItem(i, toInsert, false);
                    if (leftover.isEmpty()) {
                        remaining--;
                    }
                }
            }
        } else {
            int toRemove = currentCount - targetCount;
            ItemStack removed = ItemStack.EMPTY;
            for (int i = upgradeInv.getSlots() - 1; i >= 0 && toRemove > 0; i--) {
                ItemStack slotStack = upgradeInv.getStackInSlot(i);
                if (!slotStack.isEmpty() && isUpgradeCard(slotStack)) {
                    ItemStack extracted = upgradeInv.extractItem(i, 1, false);
                    if (!extracted.isEmpty()) {
                        if (removed.isEmpty()) {
                            removed = extracted;
                        } else {
                            removed.grow(1);
                        }
                        toRemove--;
                    }
                }
            }
            if (!removed.isEmpty()) {
                insertSpeedCardsToDestination(player, removed);
            }
        }
    }

    private static ItemStack extractSpeedCardsFromSource(EntityPlayer player, ItemStack template, int needed) {
        WirelessTerminalGuiObject wTerminal = findWirelessTerminal(player);
        if (wTerminal != null) {
            ItemStack result = extractSpeedCardsFromNetwork(wTerminal, player, template, needed);
            if (!result.isEmpty()) return result;
        }
        return extractSpeedCardsFromInventory(player, template, needed);
    }

    private static void insertSpeedCardsToDestination(EntityPlayer player, ItemStack cards) {
        WirelessTerminalGuiObject wTerminal = findWirelessTerminal(player);
        if (wTerminal != null) {
            insertSpeedCardsToNetwork(wTerminal, player, cards);
            return;
        }
        player.inventory.placeItemBackInInventory(player.world, cards);
    }

    @Nullable
    private static WirelessTerminalGuiObject findWirelessTerminal(EntityPlayer player) {
        for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
            ItemStack item = player.inventory.getStackInSlot(i);
            WirelessTerminalGuiObject obj = tryCreateWirelessTerminal(item, player, i, 0);
            if (obj != null) return obj;
        }

        if (HAS_BAUBLES) {
            try {
                IBaublesItemHandler baublesHandler = BaublesApi.getBaublesHandler(player);
                for (int i = 0; i < baublesHandler.getSlots(); i++) {
                    ItemStack item = baublesHandler.getStackInSlot(i);
                    WirelessTerminalGuiObject obj = tryCreateWirelessTerminal(item, player, i, 1);
                    if (obj != null) return obj;
                }
            } catch (Exception ignored) {
            }
        }

        return null;
    }

    @Nullable
    private static WirelessTerminalGuiObject tryCreateWirelessTerminal(ItemStack item, EntityPlayer player,
                                                                        int slot, int isBauble) {
        if (item.isEmpty() || !(item.getItem() instanceof IWirelessTermHandler)) return null;
        IWirelessTermHandler handler = (IWirelessTermHandler) item.getItem();
        if (!handler.canHandle(item)) return null;
        try {
            WirelessTerminalGuiObject obj = new WirelessTerminalGuiObject(
                    handler, item, player, player.world, slot, isBauble, 0);
            if (obj.rangeCheck()) return obj;
        } catch (Exception ignored) {
        }
        return null;
    }

    private static ItemStack extractSpeedCardsFromNetwork(WirelessTerminalGuiObject wTerminal,
                                                          EntityPlayer player, ItemStack template, int needed) {
        try {
            IGridNode node = wTerminal.getActionableNode();
            if (node == null) return ItemStack.EMPTY;

            IGrid grid = node.getGrid();
            if (grid == null) return ItemStack.EMPTY;

            ISecurityGrid security = grid.getCache(ISecurityGrid.class);
            if (!security.hasPermission(player, SecurityPermissions.EXTRACT)) return ItemStack.EMPTY;

            IStorageGrid storageGrid = grid.getCache(IStorageGrid.class);
            IMEMonitor<IAEItemStack> itemMonitor = storageGrid.getInventory(
                    AEApi.instance().storage().getStorageChannel(IItemStorageChannel.class));

            IAEItemStack request = AEItemStack.fromItemStack(template);
            request.setStackSize(needed);

            IAEItemStack extracted = itemMonitor.extractItems(request, Actionable.MODULATE,
                    new appeng.me.helpers.PlayerSource(player, wTerminal));
            if (extracted == null || extracted.getStackSize() <= 0) return ItemStack.EMPTY;

            ItemStack result = extracted.createItemStack();
            result.setCount((int) extracted.getStackSize());

            if (extracted.getStackSize() < needed) {
                int remaining = needed - (int) extracted.getStackSize();
                ItemStack fromInv = extractSpeedCardsFromInventory(player, template, remaining);
                if (!fromInv.isEmpty()) {
                    result.grow(fromInv.getCount());
                }
            }

            return result;
        } catch (Exception e) {
            return ItemStack.EMPTY;
        }
    }

    private static void insertSpeedCardsToNetwork(WirelessTerminalGuiObject wTerminal,
                                                   EntityPlayer player, ItemStack cards) {
        try {
            IGridNode node = wTerminal.getActionableNode();
            if (node == null) {
                player.inventory.placeItemBackInInventory(player.world, cards);
                return;
            }

            IGrid grid = node.getGrid();
            if (grid == null) {
                player.inventory.placeItemBackInInventory(player.world, cards);
                return;
            }

            ISecurityGrid security = grid.getCache(ISecurityGrid.class);
            if (!security.hasPermission(player, SecurityPermissions.INJECT)) {
                player.inventory.placeItemBackInInventory(player.world, cards);
                return;
            }

            IStorageGrid storageGrid = grid.getCache(IStorageGrid.class);
            IMEMonitor<IAEItemStack> itemMonitor = storageGrid.getInventory(
                    AEApi.instance().storage().getStorageChannel(IItemStorageChannel.class));

            IAEItemStack aeStack = AEItemStack.fromItemStack(cards);
            IAEItemStack leftover = itemMonitor.injectItems(aeStack, Actionable.MODULATE,
                    new appeng.me.helpers.PlayerSource(player, wTerminal));

            if (leftover != null && leftover.getStackSize() > 0) {
                player.inventory.placeItemBackInInventory(player.world, leftover.createItemStack());
            }
        } catch (Exception e) {
            player.inventory.placeItemBackInInventory(player.world, cards);
        }
    }

    private static ItemStack extractSpeedCardsFromInventory(EntityPlayer player,
                                                            ItemStack template, int needed) {
        int extracted = 0;
        ItemStack result = ItemStack.EMPTY;

        for (int i = 0; i < player.inventory.getSizeInventory() && extracted < needed; i++) {
            ItemStack slotStack = player.inventory.getStackInSlot(i);
            if (slotStack.isEmpty()) continue;
            if (!isSameItem(slotStack, template)) continue;

            int toTake = Math.min(needed - extracted, slotStack.getCount());
            if (result.isEmpty()) {
                result = new ItemStack(slotStack.getItem(), toTake, slotStack.getMetadata());
                if (slotStack.hasTagCompound()) {
                    result.setTagCompound(slotStack.getTagCompound().copy());
                }
            } else {
                result.grow(toTake);
            }
            slotStack.shrink(toTake);
            if (slotStack.isEmpty()) {
                player.inventory.setInventorySlotContents(i, ItemStack.EMPTY);
            }
            extracted += toTake;
        }

        return result;
    }

    private static boolean isSameItem(ItemStack a, ItemStack b) {
        if (a.getItem() != b.getItem()) return false;
        if (a.getMetadata() != b.getMetadata()) return false;
        return ItemStack.areItemStackTagsEqual(a, b);
    }

    private static ItemStack getSpeedCardTemplate() {
        return AEApi.instance().definitions().materials().cardSpeed().maybeStack(1).orElse(ItemStack.EMPTY);
    }

    private static byte[] packBooleans(boolean[] values) {
        byte[] packed = new byte[(values.length + 7) / 8];
        for (int i = 0; i < values.length; i++) {
            if (values[i]) packed[i / 8] |= (byte) (1 << (i % 8));
        }
        return packed;
    }

    private static void unpackBooleans(byte[] packed, boolean[] values) {
        int len = Math.min(values.length, packed.length * 8);
        for (int i = 0; i < len; i++) {
            values[i] = ((packed[i / 8] >> (i % 8)) & 1) == 1;
        }
    }
}
