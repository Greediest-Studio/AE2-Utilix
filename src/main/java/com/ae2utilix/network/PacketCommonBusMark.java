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
    private int specialType;

    public PacketCommonBusMark() {
    }

    public PacketCommonBusMark(BlockPos pos, EnumFacing side, int slot, FluidStack fluid) {
        this(pos, side, slot);
        if (fluid != null && fluid.getFluid() != null) {
            this.fluidName = fluid.getFluid().getName();
            this.fluidTag = fluid.tag == null ? null : fluid.tag.copy();
        }
    }

    public PacketCommonBusMark(BlockPos pos, EnumFacing side, int slot, String gasName) {
        this(pos, side, slot);
        this.gasName = gasName;
    }

    public PacketCommonBusMark(BlockPos pos, EnumFacing side, int slot, int specialType) {
        this(pos, side, slot);
        this.specialType = specialType;
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
        this.specialType = buf.readInt();
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
        buf.writeInt(this.specialType);
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
                if (held.isEmpty()) return;

                if (message.specialType == com.ae2utilix.integration.BotaniaFluxIntegration.MANA
                        || message.specialType == com.ae2utilix.integration.BotaniaFluxIntegration.FE) {
                    if (com.ae2utilix.integration.BotaniaFluxIntegration.getMarkedType(held) != message.specialType) return;
                    bus.setMarker(message.slot, message.specialType == com.ae2utilix.integration.BotaniaFluxIntegration.MANA
                            ? ItemFluidMark.createManaMark() : ItemFluidMark.createFeMark());
                    return;
                }

                if (message.gasName != null && !message.gasName.isEmpty()) {
                    String heldGas = com.ae2utilix.integration.MekanismEnergisticsIntegration.getGasNameFromItem(held);
                    if (!message.gasName.equals(heldGas)) return;
                    bus.setMarker(message.slot, ItemFluidMark.createGas(message.gasName));
                    return;
                }

                FluidStack heldFluid = net.minecraftforge.fluids.FluidUtil.getFluidContained(held);
                if (heldFluid == null) heldFluid = ItemFluidMark.getFluid(held);
                if (heldFluid == null && held.getItem() == net.minecraft.init.Items.WATER_BUCKET) {
                    heldFluid = new FluidStack(FluidRegistry.WATER, 1000);
                }
                FluidStack marked = getMarkedFluid(message, heldFluid);
                if (marked == null) return;
                marked.amount = 1000;
                bus.setMarker(message.slot, ItemFluidMark.create(marked));
            });
            return null;
        }

        private FluidStack getMarkedFluid(PacketCommonBusMark message, FluidStack held) {
            if (message.fluidName == null || message.fluidName.isEmpty()) return held;
            net.minecraftforge.fluids.Fluid fluid = FluidRegistry.getFluid(message.fluidName);
            if (fluid == null || held == null || held.getFluid() != fluid) return null;
            FluidStack result = new FluidStack(fluid, 1000);
            result.tag = message.fluidTag == null ? null : message.fluidTag.copy();
            return result;
        }
    }
}
