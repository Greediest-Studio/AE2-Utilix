package com.ae2utilix.mixin;

import appeng.parts.automation.UpgradeInventory;
import com.ae2utilix.item.IAE2UtilixExtendedUpgradeInventory;
import com.ae2utilix.item.IAE2UtilixUpgradeModule;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "appeng.parts.automation.UpgradeInventory$UpgradeInvFilter", remap = false)
public class MixinUpgradeInvFilter {

    @Shadow
    @Final
    UpgradeInventory this$0;

    @Inject(method = "allowInsert", at = @At("HEAD"), cancellable = true, remap = false)
    private void ae2utilix$onAllowInsert(IItemHandler inv, int slot, ItemStack itemstack,
                                         CallbackInfoReturnable<Boolean> cir) {
        if (itemstack.isEmpty()) return;
        if (!(itemstack.getItem() instanceof IAE2UtilixUpgradeModule)) return;
        if (!(this.this$0 instanceof IAE2UtilixExtendedUpgradeInventory)) return;

        IAE2UtilixExtendedUpgradeInventory ext = (IAE2UtilixExtendedUpgradeInventory) this.this$0;
        IAE2UtilixUpgradeModule upgrade = (IAE2UtilixUpgradeModule) itemstack.getItem();
        int installed = ext.ae2utilix$getInstalledUpgrades(upgrade);
        int max = ext.ae2utilix$getMaxInstalled(upgrade);
        cir.setReturnValue(installed < max);
    }
}
