package com.ae2utilix.integration;

import appeng.api.parts.IPart;
import appeng.api.parts.IPartHost;
import appeng.helpers.IInterfaceHost;
import appeng.parts.misc.PartInterface;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.items.IItemHandler;

public class BMCInteractionHandler {

    public static void register() {
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(BMCInteractionHandler.class);
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!net.minecraftforge.fml.common.Loader.isModLoaded("ae2bettermagnetcard")) return;
        if (event.getWorld().isRemote) return;

        EntityPlayer player = event.getEntityPlayer();
        if (!player.isSneaking()) return;

        ItemStack heldItem = event.getItemStack();
        if (heldItem.isEmpty()) return;

        World world = event.getWorld();
        BlockPos pos = event.getPos();
        EnumFacing face = event.getFace();
        TileEntity te = world.getTileEntity(pos);

        IItemHandler upgrades = null;

        if (te instanceof IInterfaceHost) {
            upgrades = ((IInterfaceHost) te).getInterfaceDuality().getInventoryByName("upgrades");
        } else if (te instanceof IPartHost && face != null) {
            IPart part = ((IPartHost) te).getPart(face);
            if (part instanceof PartInterface) {
                upgrades = ((PartInterface) part).getInterfaceDuality().getInventoryByName("upgrades");
            }
        }

        if (upgrades != null && BMCMagnetHelper.tryInstallUpgrade(player, heldItem, upgrades)) {
            event.setCanceled(true);
        }
    }
}
