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
                if (held.isEmpty()) return;
                FluidStack heldFluid = net.minecraftforge.fluids.FluidUtil.getFluidContained(held);
                if (heldFluid == null) heldFluid = ItemFluidMark.getFluid(held);
                if (heldFluid == null && held.getItem() == net.minecraft.init.Items.WATER_BUCKET) {
                    heldFluid = new FluidStack(net.minecraftforge.fluids.FluidRegistry.WATER, 1000);
                }
                FluidStack fluidStack = this.getMarkedFluid(message, heldFluid);
                if (fluidStack == null && held.getItem() == net.minecraft.init.Items.WATER_BUCKET) {
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
            if (fluid == null || heldFluid == null || heldFluid.getFluid() != fluid) return null;
            FluidStack result = new FluidStack(fluid, 1000);
            result.tag = message.fluidTag == null ? null : message.fluidTag.copy();
            return result;
        }
    }
}
