package com.ae2utilix.gui;

import appeng.api.config.Settings;
import appeng.api.config.YesNo;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.storage.data.IAEItemStack;
import appeng.client.me.ClientDCInternalInv;
import appeng.container.AEBaseContainer;
import appeng.container.guisync.GuiSync;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.PacketCompressedNBT;
import appeng.helpers.DualityInterface;
import appeng.helpers.IInterfaceHost;
import appeng.helpers.InventoryAction;
import appeng.me.helpers.MachineSource;
import appeng.tile.inventory.AppEngInternalInventory;
import appeng.tile.misc.TileInterface;
import appeng.util.InventoryAdaptor;
import appeng.util.Platform;
import appeng.util.item.AEItemStack;
import appeng.items.misc.ItemEncodedPattern;
import com.ae2utilix.AE2Utilix;
import com.ae2utilix.block.terminal.TileInterfaceTerminal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ContainerFullInterface extends AEBaseContainer {

    private final TileInterfaceTerminal terminal;
    private IGrid grid;
    private final MachineSource machineSrc;
    private final Map<IInterfaceHost, InvTracker> serverLinked = new LinkedHashMap<>();
    private final Map<Long, InvTracker> byId = new HashMap<>();
    private final List<InvTracker> clientLinked = new LinkedList<>();
    private boolean needsFullUpdate = false;
    private static long autoBase = Long.MIN_VALUE;

    @GuiSync(98)
    public boolean hasToolbox = false;

    public ContainerFullInterface(InventoryPlayer ip, TileInterfaceTerminal tile) {
        super(ip, tile);
        this.terminal = tile;
        this.machineSrc = new MachineSource(tile);

        if (Platform.isServer()) {
            this.grid = tile.getActionableNode().getGrid();
        }

        this.bindPlayerInventory(ip, 0, 0);
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return true;
    }

    @Override
    public void detectAndSendChanges() {
        if (this.grid == null && Platform.isServer()) {
            this.grid = this.terminal.getActionableNode().getGrid();
        }

        if (Platform.isServer() && this.grid != null) {
            final Map<IInterfaceHost, IInterfaceHost> whichInterfaces = new HashMap<>();

            for (final IGridNode gn : this.grid.getMachines(TileInterface.class)) {
                if (gn.isActive()) {
                    final IInterfaceHost ih = (IInterfaceHost) gn.getMachine();
                    final DualityInterface dual = ih.getInterfaceDuality();
                    if (dual.getConfigManager().getSetting(Settings.INTERFACE_TERMINAL) != YesNo.NO) {
                        whichInterfaces.put(ih, ih);
                    }
                }
            }

            for (final IInterfaceHost ih : whichInterfaces.values()) {
                final DualityInterface dual = ih.getInterfaceDuality();
                final IItemHandler patterns = dual.getPatterns();
                final IItemHandlerModifiable inv = (IItemHandlerModifiable) patterns;
                final String termName = dual.getTermName();

                InvTracker t = this.serverLinked.get(ih);
                if (t == null) {
                    t = new InvTracker(inv, termName, dual);
                    this.serverLinked.put(ih, t);
                    this.byId.put(t.which, t);
                    this.needsFullUpdate = true;
                } else if (!t.getName().equals(termName)) {
                    this.byId.remove(t.which);
                    t = new InvTracker(inv, termName, dual, t.getMemories());
                    this.serverLinked.put(ih, t);
                    this.byId.put(t.which, t);
                    this.needsFullUpdate = true;
                }
            }

            final Iterator<Map.Entry<IInterfaceHost, InvTracker>> iter = this.serverLinked.entrySet().iterator();
            while (iter.hasNext()) {
                final Map.Entry<IInterfaceHost, InvTracker> entry = iter.next();
                if (!whichInterfaces.containsKey(entry.getKey())) {
                    this.byId.remove(entry.getValue().which);
                    this.needsFullUpdate = true;
                    iter.remove();
                }
            }

            if (this.needsFullUpdate) {
                this.needsFullUpdate = false;
                this.sendFullUpdate();
            } else {
                // Incremental update: check for changed slots
                final NBTTagCompound data = new NBTTagCompound();
                for (final InvTracker inv : this.serverLinked.values()) {
                    final String key = '=' + Long.toString(inv.which, Character.MAX_RADIX);
                    final NBTTagCompound tag = new NBTTagCompound();
                    boolean changed = false;

                    for (int x = 0; x < inv.server.getSlots(); x++) {
                        final ItemStack serverStack = inv.server.getStackInSlot(x);
                        final ItemStack clientStack = inv.client.getStackInSlot(x);
                        if (!ItemStack.areItemStacksEqual(serverStack, clientStack)) {
                            if (!changed) {
                                tag.setLong("sortBy", inv.sortBy);
                                tag.setString("un", inv.getUniqueName());
                                tag.setTag("pos", NBTUtil.createPosTag(inv.pos));
                                tag.setInteger("dim", inv.dim);
                                tag.setInteger("numUpgrades", inv.numUpgrades);
                                changed = true;
                            }
                            final NBTTagCompound itemNBT = new NBTTagCompound();
                            if (!serverStack.isEmpty()) {
                                final IAEItemStack aeStack = AEItemStack.fromItemStack(serverStack);
                                if (aeStack != null) {
                                    aeStack.writeToNBT(itemNBT);
                                }
                            }
                            tag.setTag(Integer.toString(x), itemNBT);
                            inv.client.setStackInSlot(x, serverStack.isEmpty() ? ItemStack.EMPTY : serverStack.copy());
                        }
                    }

                    if (changed) {
                        data.setTag(key, tag);
                    }
                }

                if (data.getSize() > 0) {
                    try {
                        NetworkHandler.instance().sendTo(new PacketCompressedNBT(data),
                                (EntityPlayerMP) this.getPlayerInv().player);
                    } catch (IOException e) {
                        // ignore
                    }
                }
            }
        }

        super.detectAndSendChanges();
    }

    private void sendFullUpdate() {
        final NBTTagCompound data = new NBTTagCompound();
        data.setBoolean("clear", true);

        for (final InvTracker inv : this.serverLinked.values()) {
            final String key = '=' + Long.toString(inv.which, Character.MAX_RADIX);
            final NBTTagCompound tag = new NBTTagCompound();

            tag.setLong("sortBy", inv.sortBy);
            tag.setString("un", inv.getUniqueName());
            tag.setTag("pos", NBTUtil.createPosTag(inv.pos));
            tag.setInteger("dim", inv.dim);
            tag.setInteger("numUpgrades", inv.numUpgrades);

            for (int x = 0; x < inv.server.getSlots(); x++) {
                final ItemStack is = inv.server.getStackInSlot(x);
                final NBTTagCompound itemNBT = new NBTTagCompound();
                if (!is.isEmpty()) {
                    final IAEItemStack aeStack = AEItemStack.fromItemStack(is);
                    if (aeStack != null) {
                        aeStack.writeToNBT(itemNBT);
                    }
                }
                tag.setTag(Integer.toString(x), itemNBT);
                inv.client.setStackInSlot(x, is.isEmpty() ? ItemStack.EMPTY : is.copy());
            }

            data.setTag(key, tag);
        }

        try {
            NetworkHandler.instance().sendTo(new PacketCompressedNBT(data),
                    (EntityPlayerMP) this.getPlayerInv().player);
        } catch (IOException e) {
            // ignore
        }
    }

    public void postUpdate(final NBTTagCompound data) {
        if (data.hasKey("clear") && data.getBoolean("clear")) {
            this.clientLinked.clear();
        }

        for (final String key : data.getKeySet()) {
            if (key.startsWith("=")) {
                final NBTTagCompound tag = data.getCompoundTag(key);
                final long which = Long.parseLong(key.substring(1), Character.MAX_RADIX);
                final String uniqueName = tag.getString("un");

                InvTracker inv = null;
                for (final InvTracker tracker : this.clientLinked) {
                    if (tracker.which == which) {
                        inv = tracker;
                        break;
                    }
                }

                if (inv == null) {
                    inv = new InvTracker(null, uniqueName, which, tag.getLong("sortBy"));
                    this.clientLinked.add(inv);
                }

                if (tag.hasKey("sortBy")) {
                    inv.sortBy = tag.getLong("sortBy");
                }
                if (tag.hasKey("pos")) {
                    inv.pos = NBTUtil.getPosFromTag(tag.getCompoundTag("pos"));
                }
                if (tag.hasKey("dim")) {
                    inv.dim = tag.getInteger("dim");
                }
                if (tag.hasKey("numUpgrades")) {
                    inv.numUpgrades = tag.getInteger("numUpgrades");
                }

                // Update name from uniqueName
                inv.setName(uniqueName);

                // Resize client inventory based on numUpgrades
                final int totalSlots = 9 * (inv.numUpgrades + 1);
                inv.resizeClient(totalSlots);

                // Read slot data
                for (final String slotKey : tag.getKeySet()) {
                    try {
                        final int slot = Integer.parseInt(slotKey);
                        if (slot >= 0 && slot < inv.client.getSlots()) {
                            final NBTTagCompound itemNBT = tag.getCompoundTag(slotKey);
                            if (itemNBT.getSize() == 0) {
                                inv.client.setStackInSlot(slot, ItemStack.EMPTY);
                            } else {
                                final IAEItemStack stack = AEItemStack.fromNBT(itemNBT);
                                inv.client.setStackInSlot(slot, stack != null ? stack.createItemStack() : ItemStack.EMPTY);
                            }
                        }
                    } catch (NumberFormatException ignored) {
                        // Not a slot key, skip
                    }
                }
            }
        }
    }

    @Override
    public void doAction(EntityPlayerMP player, InventoryAction action, int slot, long id) {
        if (this.grid == null) {
            return;
        }

        final InvTracker inv = this.byId.get(id);
        if (inv == null || inv.server == null) {
            return;
        }

        if (slot < 0 || slot >= inv.server.getSlots()) {
            return;
        }

        final IItemHandlerModifiable invHandler = inv.server;
        final ItemStack is = invHandler.getStackInSlot(slot);
        final InventoryAdaptor playerInv = InventoryAdaptor.getAdaptor(player);
        final boolean hasItemInHand = !player.inventory.getItemStack().isEmpty();

        switch (action) {
            case PICKUP_OR_SET_DOWN:
                if (hasItemInHand) {
                    final ItemStack held = player.inventory.getItemStack();
                    if (!(held.getItem() instanceof ItemEncodedPattern)) {
                        break;
                    }
                    // Check for duplicate patterns (use areItemsEqual which ignores stack size)
                    boolean hasDuplicate = false;
                    for (int i = 0; i < invHandler.getSlots(); i++) {
                        final ItemStack existing = invHandler.getStackInSlot(i);
                        if (!existing.isEmpty() && ItemStack.areItemsEqual(held, existing) && ItemStack.areItemStackTagsEqual(held, existing)) {
                            hasDuplicate = true;
                            break;
                        }
                    }
                    if (hasDuplicate) {
                        break;
                    }
                    // Try to place item in slot
                    final ItemStack inSlot = invHandler.getStackInSlot(slot);
                    if (inSlot.isEmpty()) {
                        final ItemStack remainder = invHandler.insertItem(slot, held, false);
                        player.inventory.setItemStack(remainder);
                    } else {
                        // Swap
                        final ItemStack extracted = invHandler.extractItem(slot, Integer.MAX_VALUE, false);
                        final ItemStack remainder = invHandler.insertItem(slot, held, false);
                        player.inventory.setItemStack(extracted);
                        if (!remainder.isEmpty()) {
                            final ItemStack reExtracted = invHandler.extractItem(slot, remainder.getCount(), false);
                            player.inventory.setItemStack(reExtracted);
                        }
                    }
                } else {
                    // Pick up item from slot
                    final ItemStack extracted = invHandler.extractItem(slot, Integer.MAX_VALUE, false);
                    player.inventory.setItemStack(extracted);
                }
                break;
            case SHIFT_CLICK: {
                final ItemStack extracted = invHandler.extractItem(slot, Integer.MAX_VALUE, false);
                if (!extracted.isEmpty()) {
                    final ItemStack leftover = playerInv.addItems(extracted);
                    if (!leftover.isEmpty()) {
                        invHandler.insertItem(slot, leftover, false);
                    }
                }
                break;
            }
            case MOVE_REGION: {
                for (int x = 0; x < inv.server.getSlots(); x++) {
                    final ItemStack extracted = inv.server.extractItem(x, Integer.MAX_VALUE, false);
                    if (!extracted.isEmpty()) {
                        final ItemStack leftover = playerInv.addItems(extracted);
                        if (!leftover.isEmpty()) {
                            inv.server.insertItem(x, leftover, false);
                        }
                    }
                }
                break;
            }
            case CREATIVE_DUPLICATE:
                if (player.capabilities.isCreativeMode && !is.isEmpty()) {
                    player.inventory.setItemStack(is.copy());
                }
                break;
            default:
                break;
        }

        this.updateHeld(player);
    }

    public void handleButton(int buttonValue) {
        if (buttonValue == 0) {
            this.needsFullUpdate = true;
        }
    }

    void setToolbox(boolean hasToolbox) {
        this.hasToolbox = hasToolbox;
    }

    public List<InvTracker> getClientLinked() {
        return this.clientLinked;
    }

    public Map<Long, InvTracker> getById() {
        return this.byId;
    }

    public static class InvTracker {
        public final IItemHandlerModifiable server;
        public AppEngInternalInventory client;
        public ClientDCInternalInv dcInv;
        public long sortBy;
        public final long which;
        private String name;
        private final String uniqueName;
        private final Map<Integer, ItemStack> memories;
        public BlockPos pos;
        public int dim;
        public int numUpgrades;

        public InvTracker(IItemHandlerModifiable server, String name, DualityInterface dual) {
            this(server, name, dual, new HashMap<>());
        }

        public InvTracker(IItemHandlerModifiable server, String name, DualityInterface dual, Map<Integer, ItemStack> memories) {
            this.server = server;
            this.name = name;
            this.which = autoBase++;
            this.uniqueName = name;
            this.memories = memories;
            this.sortBy = dual.getSortValue();
            this.pos = dual.getLocation().getPos();
            this.dim = dual.getLocation().getWorld().provider.getDimension();
            this.numUpgrades = dual.getInstalledUpgrades(appeng.api.config.Upgrades.PATTERN_EXPANSION);
            final int totalSlots = server != null ? server.getSlots() : 9;
            this.client = new AppEngInternalInventory(null, totalSlots);
            this.dcInv = new ClientDCInternalInv(totalSlots, this.which, this.sortBy, name);
        }

        // Client-side constructor
        public InvTracker(IItemHandlerModifiable server, String name, long which, long sortBy) {
            this.server = server;
            this.name = name;
            this.which = which;
            this.uniqueName = name;
            this.sortBy = sortBy;
            this.memories = new HashMap<>();
            this.pos = BlockPos.ORIGIN;
            this.dim = 0;
            this.numUpgrades = 0;
            this.client = new AppEngInternalInventory(null, 9);
            this.dcInv = new ClientDCInternalInv(9, which, sortBy, name);
        }

        public String getName() {
            return this.name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getUniqueName() {
            return this.uniqueName;
        }

        public Map<Integer, ItemStack> getMemories() {
            return this.memories;
        }

        public void resizeClient(int totalSlots) {
            if (this.client.getSlots() != totalSlots) {
                final AppEngInternalInventory newInv = new AppEngInternalInventory(null, totalSlots);
                for (int x = 0; x < Math.min(this.client.getSlots(), totalSlots); x++) {
                    newInv.setStackInSlot(x, this.client.getStackInSlot(x));
                }
                this.client = newInv;
                this.dcInv = new ClientDCInternalInv(totalSlots, this.which, this.sortBy, this.name);
            }
        }
    }
}
