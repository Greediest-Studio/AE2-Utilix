package com.ae2utilix.client;

import com.ae2utilix.AE2Utilix;
import com.ae2utilix.item.ItemMatterDecomposer;
import com.ae2utilix.network.NetworkHandler;
import com.ae2utilix.network.PacketSwitchDecomposerMode;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod.EventBusSubscriber(modid = AE2Utilix.MODID, value = Side.CLIENT)
public class DecomposerScrollHandler {

    @SubscribeEvent
    public static void onMouseEvent(MouseEvent event) {
        if (event.getDwheel() == 0) return;

        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.player;
        if (player == null || !player.isSneaking()) return;

        ItemStack heldItem = player.getHeldItemMainhand();
        if (heldItem.getItem() != AE2Utilix.MATTER_DECOMPOSER) return;

        int direction = event.getDwheel() > 0 ? 1 : -1;
        NetworkHandler.CHANNEL.sendToServer(new PacketSwitchDecomposerMode(direction));

        DecomposerHUD.setTimer();
        event.setCanceled(true);
    }
}
