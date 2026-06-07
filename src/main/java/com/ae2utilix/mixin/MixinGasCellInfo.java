package com.ae2utilix.mixin;

import appeng.api.storage.ICellInventory;
import appeng.api.storage.ICellInventoryHandler;
import appeng.items.contents.CellUpgrades;
import com.ae2utilix.item.ItemOverflowDestructionCard;
import com.mekeng.github.util.helpers.GasCellInfo;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Add overflow destruction card tooltip to MekEng gas cells.
 * Injects at the tail of GasCellInfo.addCellInformation() to append
 * "已安装溢出销毁卡" to the partition line or as a new line.
 */
@Mixin(value = GasCellInfo.class, remap = false)
public class MixinGasCellInfo {

    @Inject(method = "addCellInformation", at = @At("TAIL"), remap = false)
    private static void ae2utilix$addOverflowDestructionTooltip(ICellInventoryHandler handler, List<String> lines, CallbackInfo ci) {
        ICellInventory cellInventory = handler.getCellInv();
        if (cellInventory == null) return;

        ItemStack cellStack = cellInventory.getItemStack();
        if (cellStack.isEmpty()) return;

        // Check upgrade slots for overflow destruction card
        CellUpgrades upgrades = new CellUpgrades(cellStack, 2);
        boolean hasOverflowDestruction = false;
        for (int i = 0; i < upgrades.getSlots(); i++) {
            ItemStack stack = upgrades.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() instanceof ItemOverflowDestructionCard) {
                hasOverflowDestruction = true;
                break;
            }
        }

        if (!hasOverflowDestruction) return;

        // Try to find and append to the partition line
        String partitionPrefix = "[" + appeng.core.localization.GuiText.Partitioned.getLocal() + "]";
        boolean appended = false;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.startsWith(partitionPrefix)) {
                lines.set(i, line + " - " + net.minecraft.client.resources.I18n.format("ae2_utilix.tooltip.overflow_destruction_installed"));
                appended = true;
                break;
            }
        }

        // If no partition line found, add as a new line
        if (!appended) {
            lines.add(net.minecraft.client.resources.I18n.format("ae2_utilix.tooltip.overflow_destruction_installed"));
        }
    }
}
