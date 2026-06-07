package com.ae2utilix.mixin;

import appeng.api.storage.ITerminalHost;
import appeng.client.gui.implementations.GuiCraftConfirm;
import appeng.core.sync.GuiBridge;
import com.ae2utilix.block.terminal.TileCraftingTerminal;
import com.ae2utilix.block.terminal.TilePatternTerminal;
import com.ae2utilix.block.terminal.TileStorageTerminal;
import net.minecraft.entity.player.InventoryPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GuiCraftConfirm.class, remap = false)
public class MixinGuiCraftConfirm {

    @Shadow
    private GuiBridge OriginalGui;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void ae2utilix$setOriginalGuiForFullTerminals(InventoryPlayer inventoryPlayer, ITerminalHost te, CallbackInfo ci) {
        if (this.OriginalGui == null) {
            if (te instanceof TileCraftingTerminal) {
                this.OriginalGui = GuiBridge.GUI_CRAFTING_TERMINAL;
            } else if (te instanceof TilePatternTerminal) {
                this.OriginalGui = GuiBridge.GUI_PATTERN_TERMINAL;
            } else if (te instanceof TileStorageTerminal) {
                this.OriginalGui = GuiBridge.GUI_ME;
            }
        }
    }
}
