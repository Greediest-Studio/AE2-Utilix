package com.ae2utilix.network;

import appeng.container.implementations.ContainerCraftingStatus;
import appeng.container.implementations.CraftingCPUStatus;
import com.ae2utilix.CpuAccessMode;
import com.ae2utilix.ICpuAccessModeHolder;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketSwitchCpuAccessMode implements IMessage {

    private int serial;
    private int modeId;

    public PacketSwitchCpuAccessMode() {}

    public PacketSwitchCpuAccessMode(int serial, CpuAccessMode mode) {
        this.serial = serial;
        this.modeId = mode.id;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        serial = buf.readInt();
        modeId = buf.readByte();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(serial);
        buf.writeByte(modeId);
    }

    public static class Handler implements IMessageHandler<PacketSwitchCpuAccessMode, IMessage> {
        @Override
        public IMessage onMessage(PacketSwitchCpuAccessMode message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                if (player.openContainer instanceof ContainerCraftingStatus) {
                    ContainerCraftingStatus container = (ContainerCraftingStatus) player.openContainer;
                    CpuAccessMode newMode = CpuAccessMode.fromId(message.modeId);
                    for (CraftingCPUStatus status : container.getCPUs()) {
                        if (status.getSerial() == message.serial) {
                            Object cpu = status.getServerCluster();
                            if (cpu instanceof ICpuAccessModeHolder) {
                                ((ICpuAccessModeHolder) cpu).ae2utilix$setAccessMode(newMode);
                            }
                            break;
                        }
                    }
                }
            });
            return null;
        }
    }
}
