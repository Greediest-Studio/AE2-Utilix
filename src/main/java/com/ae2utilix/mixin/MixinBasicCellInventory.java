package com.ae2utilix.mixin;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.storage.data.IAEStack;
import appeng.me.storage.BasicCellInventory;
import com.ae2utilix.item.ItemOverflowDestructionCard;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * When an Overflow Destruction Card is installed in a storage cell,
 * any items that the cell accepts (passes canAccept/filter) but cannot store
 * due to quantity or type limits are destroyed instead of being returned
 * to the network.
 *
 * The acceptance is determined by the cell's partition list + inverter card:
 * - Empty config = accept all types
 * - Config with items + whitelist = accept only listed types
 * - Config with items + inverter card (blacklist) = accept all except listed types
 */
@Mixin(value = BasicCellInventory.class, remap = false)
public abstract class MixinBasicCellInventory {

    /**
     * After injectItems returns a non-null remainder (overflow), check if the
     * overflow destruction card is installed. If so, destroy the overflow
     * by returning null instead.
     */
    @Inject(method = "injectItems", at = @At("RETURN"), cancellable = true, remap = false)
    private void ae2utilix$destroyOverflow(IAEStack input, Actionable mode, IActionSource src,
                                            CallbackInfoReturnable<IAEStack> cir) {
        IAEStack remainder = cir.getReturnValue();
        if (remainder == null || remainder.getStackSize() <= 0) {
            return; // No overflow, nothing to destroy
        }

        BasicCellInventory self = (BasicCellInventory) (Object) this;

        // Check if the cell has an Overflow Destruction Card installed
        IItemHandler upgrades = self.getUpgradesInventory();
        boolean hasOverflowDestruction = false;
        for (int i = 0; i < upgrades.getSlots(); i++) {
            ItemStack upgradeStack = upgrades.getStackInSlot(i);
            if (!upgradeStack.isEmpty() && upgradeStack.getItem() instanceof ItemOverflowDestructionCard) {
                hasOverflowDestruction = true;
                break;
            }
        }
        if (!hasOverflowDestruction) {
            return; // No overflow destruction card, don't destroy
        }

        // The item has already passed canAccept (partition list + inverter check)
        // at the MEInventoryHandler level before reaching injectItems.
        // So if we get here with a remainder, it means the cell accepts this
        // type but is full. Destroy the overflow.
        cir.setReturnValue(null);
    }
}
