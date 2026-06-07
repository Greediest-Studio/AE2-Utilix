package com.ae2utilix.network;

import com.ae2utilix.ClientUtil;
import com.ae2utilix.item.ItemMatterDecomposer;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketSwitchDecomposerMode implements IMessage {

    private int direction;

    public PacketSwitchDecomposerMode() {
    }

    public PacketSwitchDecomposerMode(int direction) {
        this.direction = direction;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.direction = buf.readByte();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(this.direction);
    }

    public static class Handler implements IMessageHandler<PacketSwitchDecomposerMode, IMessage> {
        @Override
        public IMessage onMessage(PacketSwitchDecomposerMode message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServer().addScheduledTask(() -> {
                ItemStack heldItem = player.getHeldItemMainhand();
                if (heldItem.getItem() instanceof ItemMatterDecomposer) {
                    ItemMatterDecomposer.DecomposerMode current = ItemMatterDecomposer.getMode(heldItem);
                    ItemMatterDecomposer.DecomposerMode next;
                    if (message.direction > 0) {
                        next = current.next();
                    } else {
                        next = current.previous();
                    }
                    ItemMatterDecomposer.setMode(heldItem, next);
                    ClientUtil.sendActionBar(player, "item.ae2_utilix.matter_decomposer.mode." + next.name().toLowerCase());
                }
            });
            return null;
        }
    }
}
