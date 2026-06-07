package com.ae2utilix.mixin;

import appeng.container.AEBaseContainer;
import appeng.container.ContainerOpenContext;
import appeng.core.sync.AppEngPacket;
import appeng.core.sync.GuiBridge;
import appeng.core.sync.network.INetworkInfo;
import appeng.core.sync.packets.PacketSwitchGuis;
import com.ae2utilix.AE2Utilix;
import com.ae2utilix.block.terminal.TileCraftingTerminal;
import com.ae2utilix.block.terminal.TilePatternTerminal;
import com.ae2utilix.block.terminal.TileStorageTerminal;
import com.ae2utilix.gui.FullTerminalGuiHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PacketSwitchGuis.class, remap = false)
public class MixinPacketSwitchGuis {

    @Shadow
    private GuiBridge newGui;

    @Inject(method = "serverPacketData", at = @At("HEAD"), cancellable = true)
    private void ae2utilix$handleFullTerminalReturn(INetworkInfo manager, AppEngPacket packet, EntityPlayer player, CallbackInfo ci) {
        // Only intercept when switching back to a terminal GUI (not to crafting status/amount)
        if (this.newGui == GuiBridge.GUI_CRAFTING_STATUS || this.newGui == GuiBridge.GUI_CRAFTING_AMOUNT) {
            return;
        }

        if (player.openContainer instanceof AEBaseContainer) {
            AEBaseContainer bc = (AEBaseContainer) player.openContainer;
            ContainerOpenContext context = bc.getOpenContext();
            if (context != null) {
                TileEntity te = context.getTile();
                if (te instanceof TileCraftingTerminal) {
                    player.openGui(AE2Utilix.INSTANCE, FullTerminalGuiHandler.GUI_CRAFTING_TERMINAL,
                            te.getWorld(), te.getPos().getX(), te.getPos().getY(), te.getPos().getZ());
                    ci.cancel();
                } else if (te instanceof TilePatternTerminal) {
                    player.openGui(AE2Utilix.INSTANCE, FullTerminalGuiHandler.GUI_PATTERN_TERMINAL,
                            te.getWorld(), te.getPos().getX(), te.getPos().getY(), te.getPos().getZ());
                    ci.cancel();
                } else if (te instanceof TileStorageTerminal) {
                    player.openGui(AE2Utilix.INSTANCE, FullTerminalGuiHandler.GUI_STORAGE_TERMINAL,
                            te.getWorld(), te.getPos().getX(), te.getPos().getY(), te.getPos().getZ());
                    ci.cancel();
                }
            }
        }
    }
}
