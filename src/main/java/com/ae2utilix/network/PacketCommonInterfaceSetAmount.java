package com.ae2utilix.network;

import com.ae2utilix.AE2Utilix;
import com.ae2utilix.block.TileCommonInterfaceAlternate;
import com.ae2utilix.item.ItemFluidMark;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.items.IItemHandlerModifiable;

public class PacketCommonInterfaceSetAmount implements IMessage {
    private int x;
    private int y;
    private int z;
    private int slot;
    private int amount;
    private boolean extended;

    public PacketCommonInterfaceSetAmount() {
    }

    public PacketCommonInterfaceSetAmount(BlockPos pos, int slot, int amount, boolean extended) {
        this.x = pos.getX();
        this.y = pos.getY();
        this.z = pos.getZ();
        this.slot = slot;
        this.amount = amount;
        this.extended = extended;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.x = buf.readInt();
        this.y = buf.readInt();
        this.z = buf.readInt();
        this.slot = buf.readInt();
        this.amount = buf.readInt();
        this.extended = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.x);
        buf.writeInt(this.y);
        buf.writeInt(this.z);
        buf.writeInt(this.slot);
        buf.writeInt(this.amount);
        buf.writeBoolean(this.extended);
    }

    public static class Handler implements IMessageHandler<PacketCommonInterfaceSetAmount, IMessage> {
        @Override
        public IMessage onMessage(PacketCommonInterfaceSetAmount message, MessageContext context) {
            EntityPlayerMP player = context.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                BlockPos pos = new BlockPos(message.x, message.y, message.z);
                if (player.getDistanceSq(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) > 64.0D) return;
                if (!(player.world.getTileEntity(pos) instanceof TileCommonInterfaceAlternate)) return;
                TileCommonInterfaceAlternate tile = (TileCommonInterfaceAlternate) player.world.getTileEntity(pos);
                IItemHandlerModifiable config = (IItemHandlerModifiable) (message.extended ? tile.getExtendedConfig() : tile.getConfig());
                if (message.slot < 0 || message.slot >= config.getSlots()) return;
                ItemStack stack = config.getStackInSlot(message.slot);
                if (stack.isEmpty()) return;
                net.minecraftforge.fluids.FluidStack fluid = com.ae2utilix.item.ItemFluidMark.getFluid(stack);
                if (fluid != null) {
                    int amount = Math.max(1, Math.min(tile.getVirtualStorageCapacity(), message.amount));
                    net.minecraftforge.fluids.FluidStack configured = fluid.copy();
                    configured.amount = amount;
                    tile.setFluidConfig(message.extended, message.slot, configured);
                } else {
                    String gasName = com.ae2utilix.item.ItemFluidMark.getGasName(stack);
                    if (gasName != null) {
                        int amount = Math.max(1, Math.min(tile.getVirtualStorageCapacity(), message.amount));
                        tile.setGasConfig(message.extended, message.slot, gasName, amount);
                        tile.saveChanges();
                        return;
                    }

                    if (com.ae2utilix.item.ItemFluidMark.isManaMark(stack)) {
                        int amount = Math.max(1, Math.min(tile.getVirtualStorageCapacity(), message.amount));
                        tile.setManaConfig(message.extended, message.slot, amount);
                        tile.saveChanges();
                        return;
                    }
                    if (com.ae2utilix.item.ItemFluidMark.isFeMark(stack)) {
                        int amount = Math.max(1, Math.min(tile.getVirtualStorageCapacity(), message.amount));
                        tile.setFeConfig(message.extended, message.slot, amount);
                        tile.saveChanges();
                        return;
                    }

                    int amount = Math.max(1, Math.min(tile.getItemSlotCapacity(), message.amount));
                    stack = stack.copy();
                    stack.setCount(amount);
                    config.setStackInSlot(message.slot, stack);
                }
                tile.saveChanges();
            });
            return null;
        }
    }
}
