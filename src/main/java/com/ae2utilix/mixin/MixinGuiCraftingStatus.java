package com.ae2utilix.mixin;

import appeng.api.storage.ITerminalHost;
import appeng.client.gui.implementations.GuiCraftingStatus;
import appeng.core.sync.GuiBridge;
import com.ae2utilix.AE2Utilix;
import com.ae2utilix.block.terminal.TileCraftingTerminal;
import com.ae2utilix.block.terminal.TilePatternTerminal;
import com.ae2utilix.block.terminal.TileStorageTerminal;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GuiCraftingStatus.class, remap = false)
public class MixinGuiCraftingStatus {

    @Shadow
    private GuiBridge originalGui;

    @Shadow
    private ItemStack myIcon;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void ae2utilix$setOriginalGuiForFullTerminals(InventoryPlayer inventoryPlayer, ITerminalHost te, CallbackInfo ci) {
        if (this.originalGui == null) {
            if (te instanceof TileCraftingTerminal) {
                this.originalGui = GuiBridge.GUI_CRAFTING_TERMINAL;
                this.myIcon = new ItemStack(AE2Utilix.BLOCK_CRAFTING_TERMINAL);
            } else if (te instanceof TilePatternTerminal) {
                this.originalGui = GuiBridge.GUI_PATTERN_TERMINAL;
                this.myIcon = new ItemStack(AE2Utilix.BLOCK_PATTERN_TERMINAL);
            } else if (te instanceof TileStorageTerminal) {
                this.originalGui = GuiBridge.GUI_ME;
                this.myIcon = new ItemStack(AE2Utilix.BLOCK_STORAGE_TERMINAL);
            }
        }
    }
}
