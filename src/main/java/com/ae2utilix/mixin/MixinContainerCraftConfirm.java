package com.ae2utilix.mixin;

import appeng.api.networking.crafting.ICraftingCPU;
import appeng.api.util.AEPartLocation;
import appeng.container.ContainerOpenContext;
import appeng.container.implementations.ContainerCraftConfirm;
import appeng.core.sync.GuiBridge;
import appeng.util.Platform;
import com.ae2utilix.AE2UtilixConfig;
import com.ae2utilix.CpuAccessMode;
import com.ae2utilix.ICpuAccessModeHolder;
import com.ae2utilix.block.terminal.TileCraftingTerminal;
import com.ae2utilix.block.terminal.TilePatternTerminal;
import com.ae2utilix.block.terminal.TileStorageTerminal;
import com.ae2utilix.gui.FullTerminalGuiHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ContainerCraftConfirm.class, remap = false)
public class MixinContainerCraftConfirm {

    @Inject(method = "cpuMatches", at = @At("RETURN"), cancellable = true)
    private void ae2utilix$filterByAccessMode(ICraftingCPU c, CallbackInfoReturnable<Boolean> cir) {
        if (!AE2UtilixConfig.enableCpuAccessMode) return;
        if (cir.getReturnValue() && c instanceof ICpuAccessModeHolder) {
            CpuAccessMode mode = ((ICpuAccessModeHolder) c).ae2utilix$getAccessMode();
            if (!mode.allowsPlayer()) {
                cir.setReturnValue(false);
            }
        }
    }

    /**
     * Modify the originalGui local variable in startJob so that it is non-null
     * for our fullblock terminals. This ensures the Platform.openGUI branch
     * is reached, where our @Redirect can then intercept it.
     */
    @ModifyVariable(method = "startJob", name = "originalGui", at = @At(value = "LOAD", ordinal = 0), ordinal = 0)
    private GuiBridge ae2utilix$setOriginalGui(GuiBridge originalGui) {
        if (originalGui == null) {
            ContainerCraftConfirm self = (ContainerCraftConfirm) (Object) this;
            ContainerOpenContext context = self.getOpenContext();
            if (context != null) {
                TileEntity te = context.getTile();
                if (te instanceof TileCraftingTerminal) {
                    return GuiBridge.GUI_CRAFTING_TERMINAL;
                } else if (te instanceof TilePatternTerminal) {
                    return GuiBridge.GUI_PATTERN_TERMINAL;
                } else if (te instanceof TileStorageTerminal) {
                    return GuiBridge.GUI_ME;
                }
            }
        }
        return originalGui;
    }

    /**
     * Redirect Platform.openGUI in startJob to handle fullblock terminals.
     * When the open context points to our custom terminals, we use our own
     * IGuiHandler instead of AE2's GuiBridge.
     */
    @Redirect(method = "startJob", at = @At(value = "INVOKE", target = "Lappeng/util/Platform;openGUI(Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/tileentity/TileEntity;Lappeng/api/util/AEPartLocation;Lappeng/core/sync/GuiBridge;)V"))
    private void ae2utilix$redirectOpenGUI(EntityPlayer player, TileEntity te, AEPartLocation side, GuiBridge originalGui) {
        if (te instanceof TileCraftingTerminal) {
            player.openGui(com.ae2utilix.AE2Utilix.INSTANCE, FullTerminalGuiHandler.GUI_CRAFTING_TERMINAL,
                    te.getWorld(), te.getPos().getX(), te.getPos().getY(), te.getPos().getZ());
        } else if (te instanceof TilePatternTerminal) {
            player.openGui(com.ae2utilix.AE2Utilix.INSTANCE, FullTerminalGuiHandler.GUI_PATTERN_TERMINAL,
                    te.getWorld(), te.getPos().getX(), te.getPos().getY(), te.getPos().getZ());
        } else if (te instanceof TileStorageTerminal) {
            player.openGui(com.ae2utilix.AE2Utilix.INSTANCE, FullTerminalGuiHandler.GUI_STORAGE_TERMINAL,
                    te.getWorld(), te.getPos().getX(), te.getPos().getY(), te.getPos().getZ());
        } else {
            Platform.openGUI(player, te, side, originalGui);
        }
    }
}
