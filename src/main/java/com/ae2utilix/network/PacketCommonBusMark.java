package com.ae2utilix.network;

import com.ae2utilix.item.ItemFluidMark;
import com.ae2utilix.parts.PartCommonBus;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.items.IItemHandler;

/** Writes a right-click resource marker into a common bus configuration slot. */
public class PacketCommonBusMark implements IMessage {
    private int x;
    private int y;
    private int z;
    private int side;
    private int slot;
    private String fluidName;
    private net.minecraft.nbt.NBTTagCompound fluidTag;
    private String gasName;
    private String aspectName;
    private int specialType;
    private ItemStack itemMark = ItemStack.EMPTY;
    private boolean fromJei;

    public PacketCommonBusMark() {
    }

    public PacketCommonBusMark(BlockPos pos, EnumFacing side, int slot, FluidStack fluid) {
        this(pos, side, slot);
        if (fluid != null && fluid.getFluid() != null) {
            this.fluidName = fluid.getFluid().getName();
            this.fluidTag = fluid.tag == null ? null : fluid.tag.copy();
        }
    }

    public static PacketCommonBusMark forJeiFluid(BlockPos pos, EnumFacing side, int slot,
                                                   FluidStack fluid) {
        PacketCommonBusMark packet = new PacketCommonBusMark(pos, side, slot, fluid);
        packet.fromJei = true;
        return packet;
    }

    public PacketCommonBusMark(BlockPos pos, EnumFacing side, int slot, String gasName) {
        this(pos, side, slot);
        this.gasName = gasName;
    }

    public static PacketCommonBusMark forJeiGas(BlockPos pos, EnumFacing side, int slot,
                                                String gasName) {
        PacketCommonBusMark packet = new PacketCommonBusMark(pos, side, slot, gasName);
        packet.fromJei = true;
        return packet;
    }

    public PacketCommonBusMark(BlockPos pos, EnumFacing side, int slot, String aspectName, boolean essentia) {
        this(pos, side, slot);
        if (essentia) this.aspectName = aspectName;
    }

    public static PacketCommonBusMark forJeiEssentia(BlockPos pos, EnumFacing side, int slot,
                                                     String aspectName) {
        PacketCommonBusMark packet = new PacketCommonBusMark(pos, side, slot, aspectName, true);
        packet.fromJei = true;
        return packet;
    }

    public PacketCommonBusMark(BlockPos pos, EnumFacing side, int slot, int specialType) {
        this(pos, side, slot);
        this.specialType = specialType;
    }

    public static PacketCommonBusMark forJeiSpecial(BlockPos pos, EnumFacing side, int slot,
                                                    int specialType) {
        PacketCommonBusMark packet = new PacketCommonBusMark(pos, side, slot, specialType);
        packet.fromJei = true;
        return packet;
    }

    public static PacketCommonBusMark forJeiItem(BlockPos pos, EnumFacing side, int slot,
                                                 ItemStack item) {
        PacketCommonBusMark packet = new PacketCommonBusMark(pos, side, slot);
        packet.itemMark = item == null ? ItemStack.EMPTY : item.copy();
        if (!packet.itemMark.isEmpty()) packet.itemMark.setCount(1);
        packet.fromJei = true;
        return packet;
    }

    private PacketCommonBusMark(BlockPos pos, EnumFacing side, int slot) {
        this.x = pos.getX();
        this.y = pos.getY();
        this.z = pos.getZ();
        this.side = side == null ? 0 : side.getIndex();
        this.slot = slot;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.x = buf.readInt();
        this.y = buf.readInt();
        this.z = buf.readInt();
        this.side = buf.readInt();
        this.slot = buf.readInt();
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
        buf.writeInt(this.side);
        buf.writeInt(this.slot);
        ByteBufUtils.writeUTF8String(buf, this.fluidName == null ? "" : this.fluidName);
        ByteBufUtils.writeTag(buf, this.fluidTag);
        ByteBufUtils.writeUTF8String(buf, this.gasName == null ? "" : this.gasName);
        ByteBufUtils.writeUTF8String(buf, this.aspectName == null ? "" : this.aspectName);
        buf.writeInt(this.specialType);
        ByteBufUtils.writeItemStack(buf, this.itemMark == null ? ItemStack.EMPTY : this.itemMark);
        buf.writeBoolean(this.fromJei);
    }

    public static class Handler implements IMessageHandler<PacketCommonBusMark, IMessage> {
        @Override
        public IMessage onMessage(PacketCommonBusMark message, MessageContext context) {
            EntityPlayerMP player = context.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                BlockPos pos = new BlockPos(message.x, message.y, message.z);
                if (player.getDistanceSq(message.x + 0.5D, message.y + 0.5D, message.z + 0.5D) > 64.0D) return;
                TileEntity tile = player.world.getTileEntity(pos);
                if (message.side < 0 || message.side >= EnumFacing.values().length) return;
                PartCommonBus bus = PartCommonBus.findPart(tile, EnumFacing.values()[message.side]);
                if (bus == null || message.slot < 0 || message.slot >= PartCommonBus.CONFIG_SLOTS) return;

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
                    bus.setMarker(message.slot, message.specialType == com.ae2utilix.integration.BotaniaFluxIntegration.MANA
                            ? ItemFluidMark.createManaMark() : ItemFluidMark.createFeMark());
                    return;
                }

                if (message.fromJei && message.itemMark != null && !message.itemMark.isEmpty()) {
                    ItemStack marker = message.itemMark.copy();
                    marker.setCount(1);
                    bus.setMarker(message.slot, marker);
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
                    bus.setMarker(message.slot, ItemFluidMark.createGas(message.gasName));
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
                    bus.setMarker(message.slot, ItemFluidMark.createEssentia(message.aspectName));
                    return;
                }

                FluidStack heldFluid = null;
                if (!message.fromJei) {
                    heldFluid = net.minecraftforge.fluids.FluidUtil.getFluidContained(held);
                    if (heldFluid == null) heldFluid = ItemFluidMark.getFluid(held);
                    if (heldFluid == null && held.getItem() == net.minecraft.init.Items.WATER_BUCKET) {
                        heldFluid = new FluidStack(FluidRegistry.WATER, 1000);
                    }
                }
                FluidStack marked = message.fromJei
                        ? getMarkedFluid(message, null)
                        : getMarkedFluid(message, heldFluid);
                if (marked == null) return;
                marked.amount = 1000;
                bus.setMarker(message.slot, ItemFluidMark.create(marked));
            });
            return null;
        }

        private FluidStack getMarkedFluid(PacketCommonBusMark message, FluidStack held) {
            if (message.fluidName == null || message.fluidName.isEmpty()) return held;
            net.minecraftforge.fluids.Fluid fluid = FluidRegistry.getFluid(message.fluidName);
            if (fluid == null) return null;
            if (!message.fromJei && (held == null || held.getFluid() != fluid)) return null;
            FluidStack result = new FluidStack(fluid, 1000);
            result.tag = message.fluidTag == null ? null : message.fluidTag.copy();
            return result;
        }
    }
}
