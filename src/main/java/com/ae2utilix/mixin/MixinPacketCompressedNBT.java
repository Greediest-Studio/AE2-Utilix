package com.ae2utilix.mixin;

import appeng.core.sync.AppEngPacket;
import appeng.core.sync.network.INetworkInfo;
import appeng.core.sync.packets.PacketCompressedNBT;
import com.ae2utilix.gui.GuiFullInterface;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(value = PacketCompressedNBT.class, remap = false)
public abstract class MixinPacketCompressedNBT extends AppEngPacket {

    @Shadow
    private NBTTagCompound in;

    @Inject(method = "clientPacketData", at = @At("HEAD"), cancellable = true, remap = false)
    private void onClientPacketData(INetworkInfo network, AppEngPacket packet, EntityPlayer player, CallbackInfo ci) {
        final GuiScreen gs = Minecraft.getMinecraft().currentScreen;
        if (gs instanceof GuiFullInterface) {
            ((GuiFullInterface) gs).postUpdate(this.in);
            ci.cancel();
        }
    }
}
