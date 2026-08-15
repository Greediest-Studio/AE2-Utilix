package com.ae2utilix.network;

import appeng.api.storage.data.IAEFluidStack;
import appeng.fluids.util.AEFluidStack;
import appeng.util.item.AEItemStack;
import com.ae2utilix.block.TileCommonInterfaceAlternate;
import com.ae2utilix.item.ItemFluidMark;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.items.IItemHandler;

public class PacketCommonInterfaceFluidMark implements IMessage {
    private int x;
    private int y;
    private int z;
    private int slot;
    private boolean fluid;
    private boolean extended;
    private String fluidName;
    private net.minecraft.nbt.NBTTagCompound fluidTag;
    private String gasName;
    private String aspectName;
    private int specialType;
    private ItemStack itemMark = ItemStack.EMPTY;
    private boolean fromJei;

    public PacketCommonInterfaceFluidMark() {
    }

    public PacketCommonInterfaceFluidMark(BlockPos pos, int slot, boolean fluid, boolean extended) {
        this.x = pos.getX();
        this.y = pos.getY();
        this.z = pos.getZ();
        this.slot = slot;
        this.fluid = fluid;
        this.extended = extended;
    }

    public PacketCommonInterfaceFluidMark(BlockPos pos, int slot, boolean fluid, boolean extended, FluidStack markedFluid) {
        this(pos, slot, fluid, extended);
        if (markedFluid != null && markedFluid.getFluid() != null) {
            this.fluidName = markedFluid.getFluid().getName();
            this.fluidTag = markedFluid.tag == null ? null : markedFluid.tag.copy();
        }
    }

    public static PacketCommonInterfaceFluidMark forJeiFluid(BlockPos pos, int slot,
                                                              boolean extended,
                                                              FluidStack markedFluid) {
        PacketCommonInterfaceFluidMark packet =
                new PacketCommonInterfaceFluidMark(pos, slot, true, extended, markedFluid);
        packet.fromJei = true;
        return packet;
    }

    public PacketCommonInterfaceFluidMark(BlockPos pos, int slot, boolean extended, String gasName) {
        this(pos, slot, false, extended);
        this.gasName = gasName;
    }

    public static PacketCommonInterfaceFluidMark forJeiGas(BlockPos pos, int slot,
                                                            boolean extended, String gasName) {
        PacketCommonInterfaceFluidMark packet =
                new PacketCommonInterfaceFluidMark(pos, slot, extended, gasName);
        packet.fromJei = true;
        return packet;
    }

    public PacketCommonInterfaceFluidMark(BlockPos pos, int slot, boolean extended,
                                          String aspectName, boolean essentia) {
        this(pos, slot, false, extended);
        if (essentia) this.aspectName = aspectName;
    }

    public static PacketCommonInterfaceFluidMark forJeiEssentia(BlockPos pos, int slot,
                                                                 boolean extended,
                                                                 String aspectName) {
        PacketCommonInterfaceFluidMark packet =
                new PacketCommonInterfaceFluidMark(pos, slot, extended, aspectName, true);
        packet.fromJei = true;
        return packet;
    }

    public PacketCommonInterfaceFluidMark(BlockPos pos, int slot, boolean extended, int specialType) {
        this(pos, slot, false, extended);
        this.specialType = specialType;
    }

    public static PacketCommonInterfaceFluidMark forJeiSpecial(BlockPos pos, int slot,
                                                               boolean extended, int specialType) {
        PacketCommonInterfaceFluidMark packet =
                new PacketCommonInterfaceFluidMark(pos, slot, extended, specialType);
        packet.fromJei = true;
        return packet;
    }

    public static PacketCommonInterfaceFluidMark forJeiItem(BlockPos pos, int slot,
                                                             boolean extended, ItemStack item) {
        PacketCommonInterfaceFluidMark packet =
                new PacketCommonInterfaceFluidMark(pos, slot, false, extended);
        packet.itemMark = item == null ? ItemStack.EMPTY : item.copy();
        if (!packet.itemMark.isEmpty()) packet.itemMark.setCount(1);
        packet.fromJei = true;
        return packet;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.x = buf.readInt();
        this.y = buf.readInt();
        this.z = buf.readInt();
        this.slot = buf.readInt();
        this.fluid = buf.readBoolean();
        this.extended = buf.readBoolean();
        this.fluidName = ByteBufUtils.readUTF8String(buf);
        this.fluidTag = ByteBufUtils.readTag(buf);
        this.gasName = ByteBufUtils.readUTF8String(buf);
        this.aspectName = ByteBufUtils.readUTF8String(buf);
        this.specialType = buf.readInt();
        this.itemMark = ByteBufUtils.readItemStack(buf);
        this.fromJei = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.x);
        buf.writeInt(this.y);
        buf.writeInt(this.z);
        buf.writeInt(this.slot);
        buf.writeBoolean(this.fluid);
        buf.writeBoolean(this.extended);
        ByteBufUtils.writeUTF8String(buf, this.fluidName == null ? "" : this.fluidName);
        ByteBufUtils.writeTag(buf, this.fluidTag);
        ByteBufUtils.writeUTF8String(buf, this.gasName == null ? "" : this.gasName);
        ByteBufUtils.writeUTF8String(buf, this.aspectName == null ? "" : this.aspectName);
        buf.writeInt(this.specialType);
        ByteBufUtils.writeItemStack(buf, this.itemMark == null ? ItemStack.EMPTY : this.itemMark);
        buf.writeBoolean(this.fromJei);
    }

    public static class Handler implements IMessageHandler<PacketCommonInterfaceFluidMark, IMessage> {
        @Override
        public IMessage onMessage(PacketCommonInterfaceFluidMark message, MessageContext context) {
            EntityPlayerMP player = context.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                if (player.getDistanceSq(message.x + 0.5D, message.y + 0.5D, message.z + 0.5D) > 64.0D) return;
                if (!(player.world.getTileEntity(new BlockPos(message.x, message.y, message.z)) instanceof TileCommonInterfaceAlternate)) return;
                TileCommonInterfaceAlternate tile = (TileCommonInterfaceAlternate) player.world.getTileEntity(new BlockPos(message.x, message.y, message.z));
                IItemHandler config = message.extended ? tile.getExtendedConfig() : tile.getConfig();
                IItemHandler storage = message.extended ? tile.getExtendedStorage() : tile.getStorage();
                if (message.slot < 0 || message.slot >= config.getSlots()) return;
                ItemStack held = player.inventory.getItemStack();
                if (!message.fromJei && held.isEmpty()) return;

                if (message.specialType == com.ae2utilix.integration.BotaniaFluxIntegration.MANA
                        || message.specialType == com.ae2utilix.integration.BotaniaFluxIntegration.FE) {
                    if (message.fromJei) {
                        boolean available = message.specialType == com.ae2utilix.integration.BotaniaFluxIntegration.MANA
                                ? com.ae2utilix.integration.BotaniaFluxIntegration.isManaIntegrationAvailable()
                                : com.ae2utilix.integration.BotaniaFluxIntegration.isFeIntegrationAvailable();
                        if (!available) return;
                    } else if (com.ae2utilix.integration.BotaniaFluxIntegration.getMarkedType(held)
                            != message.specialType) return;
                    config.extractItem(message.slot, 1, false);
                    ItemStack marker = message.specialType == com.ae2utilix.integration.BotaniaFluxIntegration.MANA
                            ? ItemFluidMark.createManaMark() : ItemFluidMark.createFeMark();
                    config.insertItem(message.slot, marker, false);
                    storage.extractItem(message.slot, Integer.MAX_VALUE, false);
                    tile.setStoredMana(message.extended, message.slot, 0);
                    tile.setStoredFe(message.extended, message.slot, 0);
                    if (message.specialType == com.ae2utilix.integration.BotaniaFluxIntegration.MANA) {
                        tile.setManaConfig(message.extended, message.slot, 1000);
                    } else {
                        tile.setFeConfig(message.extended, message.slot, 1000);
                    }
                    tile.saveChanges();
                    return;
                }

                if (message.fromJei && message.itemMark != null && !message.itemMark.isEmpty()) {
                    ItemStack marker = message.itemMark.copy();
                    marker.setCount(1);
                    config.extractItem(message.slot, Integer.MAX_VALUE, false);
                    config.insertItem(message.slot, marker, false);
                    storage.extractItem(message.slot, Integer.MAX_VALUE, false);
                    tile.clearVirtualConfig(message.extended, message.slot);
                    tile.saveChanges();
                    return;
                }

                if (message.gasName != null && !message.gasName.isEmpty()) {
                    if (message.fromJei) {
                        if (!com.ae2utilix.integration.MekanismEnergisticsIntegration
                                .isGasNameValid(message.gasName)) return;
                    } else {
                        String heldGas = com.ae2utilix.integration.MekanismEnergisticsIntegration
                                .getGasNameFromItem(held);
                        if (!message.gasName.equals(heldGas)) return;
                    }
                    config.extractItem(message.slot, 1, false);
                    config.insertItem(message.slot, ItemFluidMark.createGas(message.gasName), false);
                    storage.extractItem(message.slot, Integer.MAX_VALUE, false);
                    tile.setGasConfig(message.extended, message.slot, message.gasName, 1000);
                    tile.saveChanges();
                    return;
                }

                if (message.aspectName != null && !message.aspectName.isEmpty()) {
                    if (message.fromJei) {
                        if (!com.ae2utilix.integration.ThaumicEnergisticsIntegration
                                .isAspectTagValid(message.aspectName)) return;
                    } else {
                        String heldAspect = com.ae2utilix.integration.ThaumicEnergisticsIntegration
                                .getAspectTagFromItem(held);
                        if (!message.aspectName.equals(heldAspect)) return;
                    }
                    config.extractItem(message.slot, 1, false);
                    config.insertItem(message.slot, ItemFluidMark.createEssentia(message.aspectName), false);
                    storage.extractItem(message.slot, Integer.MAX_VALUE, false);
                    tile.setEssentiaConfig(message.extended, message.slot, message.aspectName, 1000);
                    tile.setStoredEssentia(message.extended, message.slot, null, 0);
                    tile.saveChanges();
                    return;
                }

                FluidStack heldFluid = null;
                if (!message.fromJei) {
                    heldFluid = net.minecraftforge.fluids.FluidUtil.getFluidContained(held);
                    if (heldFluid == null) heldFluid = ItemFluidMark.getFluid(held);
                    if (heldFluid == null && held.getItem() == net.minecraft.init.Items.WATER_BUCKET) {
                        heldFluid = new FluidStack(net.minecraftforge.fluids.FluidRegistry.WATER, 1000);
                    }
                }
                FluidStack fluidStack = message.fromJei
                        ? this.getMarkedFluid(message, null)
                        : this.getMarkedFluid(message, heldFluid);
                if (!message.fromJei && fluidStack == null
                        && held.getItem() == net.minecraft.init.Items.WATER_BUCKET) {
                    fluidStack = new FluidStack(net.minecraftforge.fluids.FluidRegistry.WATER, 1000);
                }
                if (fluidStack == null) return;
                fluidStack.amount = 1000;
                ItemStack marker = ItemFluidMark.create(fluidStack);
                config.extractItem(message.slot, 1, false);
                config.insertItem(message.slot, marker, false);
                storage.extractItem(message.slot, Integer.MAX_VALUE, false);
                tile.setFluidConfig(message.extended, message.slot, fluidStack);
                tile.saveChanges();
            });
            return null;
        }

        private FluidStack getMarkedFluid(PacketCommonInterfaceFluidMark message, FluidStack heldFluid) {
            if (message.fluidName == null || message.fluidName.isEmpty()) return heldFluid;
            net.minecraftforge.fluids.Fluid fluid = net.minecraftforge.fluids.FluidRegistry.getFluid(message.fluidName);
            if (fluid == null) return null;
            if (!message.fromJei && (heldFluid == null || heldFluid.getFluid() != fluid)) return null;
            FluidStack result = new FluidStack(fluid, 1000);
            result.tag = message.fluidTag == null ? null : message.fluidTag.copy();
            return result;
        }
    }
}
