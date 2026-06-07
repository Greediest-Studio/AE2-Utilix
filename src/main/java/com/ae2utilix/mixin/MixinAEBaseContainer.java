package com.ae2utilix.mixin;

import appeng.api.AEApi;
import appeng.container.AEBaseContainer;
import appeng.container.slot.AppEngSlot;
import appeng.container.slot.SlotRestrictedInput;
import com.ae2utilix.item.IAE2UtilixUpgradeModule;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = AEBaseContainer.class, remap = false)
public abstract class MixinAEBaseContainer {

    @Unique
    private static boolean ae2utilix$isUpgradeCard(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.getItem() instanceof IAE2UtilixUpgradeModule) return true;
        return AEApi.instance().definitions().materials().cardMagnet().isSameAs(stack);
    }

    @Inject(method = {"transferStackInSlot", "func_82846_b"}, at = @At("HEAD"), cancellable = true)
    private void ae2utilix$onTransferStack(EntityPlayer player, int idx, CallbackInfoReturnable<ItemStack> cir) {
        AEBaseContainer self = (AEBaseContainer) (Object) this;

        Slot clickSlot = idx >= 0 && idx < self.inventorySlots.size() ? (Slot) self.inventorySlots.get(idx) : null;
        if (clickSlot == null || !clickSlot.getHasStack()) return;

        ItemStack tis = clickSlot.getStack();
        if (!ae2utilix$isUpgradeCard(tis)) return;

        boolean isPlayerSide = clickSlot instanceof AppEngSlot && ((AppEngSlot) clickSlot).isPlayerSide();
        if (!isPlayerSide) return;

        for (Slot slot : self.inventorySlots) {
            if (slot instanceof SlotRestrictedInput) {
                SlotRestrictedInput sri = (SlotRestrictedInput) slot;
                if (sri.getPlaceableItemType() == SlotRestrictedInput.PlacableItemType.UPGRADES) {
                    if (slot.isItemValid(tis) && !slot.getHasStack()) {
                        ItemStack oneCard = tis.copy();
                        oneCard.setCount(1);
                        slot.putStack(oneCard);
                        tis.shrink(1);
                        if (tis.getCount() <= 0) {
                            clickSlot.putStack(ItemStack.EMPTY);
                        } else {
                            clickSlot.onSlotChanged();
                        }
                        cir.setReturnValue(ItemStack.EMPTY);
                        return;
                    }
                }
            }
        }

        cir.setReturnValue(ItemStack.EMPTY);
    }

    @Inject(method = {"slotClick", "func_184996_a"}, at = @At("HEAD"), cancellable = true)
    private void ae2utilix$onSlotClick(int slotId, int dragType, ClickType clickTypeIn, EntityPlayer player, CallbackInfoReturnable<ItemStack> cir) {
        if (clickTypeIn != ClickType.PICKUP || dragType != 1) return;

        AEBaseContainer self = (AEBaseContainer) (Object) this;
        if (slotId < 0 || slotId >= self.inventorySlots.size()) return;

        Slot slot = self.inventorySlots.get(slotId);
        if (!(slot instanceof SlotRestrictedInput)) return;

        SlotRestrictedInput sri = (SlotRestrictedInput) slot;
        if (sri.getPlaceableItemType() != SlotRestrictedInput.PlacableItemType.UPGRADES) return;

        ItemStack stack = slot.getStack();
        if (stack.isEmpty()) return;

        if (!AEApi.instance().definitions().materials().cardMagnet().isSameAs(stack)) return;

        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            stack.setTagCompound(tag);
        }
        if (tag.hasKey("enabled")) {
            tag.setBoolean("enabled", !tag.getBoolean("enabled"));
        } else {
            tag.setBoolean("enabled", false);
        }
        slot.onSlotChanged();

        cir.setReturnValue(ItemStack.EMPTY);
    }
}
