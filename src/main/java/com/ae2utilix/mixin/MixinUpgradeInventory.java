package com.ae2utilix.mixin;

import appeng.parts.automation.UpgradeInventory;
import com.ae2utilix.item.IAE2UtilixExtendedUpgradeInventory;
import com.ae2utilix.item.IAE2UtilixUpgradeModule;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;

@Mixin(value = UpgradeInventory.class, remap = false)
public abstract class MixinUpgradeInventory implements IAE2UtilixExtendedUpgradeInventory {

    @Unique
    private final Map<String, Integer> ae2utilix$installedUpgrades = new HashMap<>();

    @Shadow
    private boolean cached;

    @Inject(method = "updateUpgradeInfo", at = @At("HEAD"))
    private void ae2utilix$clearCustomUpgrades(CallbackInfo ci) {
        this.ae2utilix$installedUpgrades.clear();
    }

    @Inject(method = "updateUpgradeInfo", at = @At("TAIL"))
    private void ae2utilix$countCustomUpgrades(CallbackInfo ci) {
        UpgradeInventory self = (UpgradeInventory) (Object) this;
        for (ItemStack is : self) {
            if (is == null || is.isEmpty()) continue;
            Item item = is.getItem();
            if (item instanceof IAE2UtilixUpgradeModule) {
                IAE2UtilixUpgradeModule mod = (IAE2UtilixUpgradeModule) item;
                String id = mod.getUpgradeTypeId();
                ae2utilix$installedUpgrades.put(id, ae2utilix$installedUpgrades.getOrDefault(id, 0) + 1);
            }
        }
    }

    @Override
    @Unique
    public int ae2utilix$getInstalledUpgrades(IAE2UtilixUpgradeModule upgrade) {
        if (!this.cached) {
            UpgradeInventory self = (UpgradeInventory) (Object) this;
            self.getInstalledUpgrades(appeng.api.config.Upgrades.CRAFTING);
        }
        return this.ae2utilix$installedUpgrades.getOrDefault(upgrade.getUpgradeTypeId(), 0);
    }

    @Override
    @Unique
    public int ae2utilix$getMaxInstalled(IAE2UtilixUpgradeModule upgrade) {
        return upgrade.getMaxInstalled();
    }
}
