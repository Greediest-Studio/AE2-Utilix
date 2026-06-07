package com.ae2utilix.mixin;

import appeng.client.gui.AEBaseGui;
import appeng.client.me.SlotDisconnected;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.PacketInventoryAction;
import appeng.helpers.InventoryAction;
import com.ae2utilix.gui.SlotInterface;
import net.minecraft.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Add SlotInterface support to AEBaseGui.handleMouseClick.
 * AE2 UEL only handles SlotDisconnected, we need to also handle our SlotInterface.
 */
@Mixin(value = AEBaseGui.class, remap = false)
public abstract class MixinAEBaseGuiHandleClick {

    @Inject(method = {"handleMouseClick", "func_184098_a"}, at = @At("HEAD"), cancellable = true, remap = false)
    private void onHandleMouseClick(Slot slot, int slotIdx, int mouseButton, net.minecraft.inventory.ClickType clickType, CallbackInfo ci) {
        if (slot instanceof SlotInterface) {
            final SlotInterface si = (SlotInterface) slot;

            InventoryAction action = null;
            switch (clickType) {
                case PICKUP:
                    if (mouseButton == 1) {
                        action = InventoryAction.SPLIT_OR_PLACE_SINGLE;
                    } else {
                        action = InventoryAction.PICKUP_OR_SET_DOWN;
                    }
                    break;
                case QUICK_MOVE:
                    action = (mouseButton == 1) ? InventoryAction.PICKUP_SINGLE : InventoryAction.SHIFT_CLICK;
                    break;
                case CLONE:
                    action = InventoryAction.CREATIVE_DUPLICATE;
                    break;
                case THROW:
                default:
                    break;
            }

            if (action != null) {
                final PacketInventoryAction p = new PacketInventoryAction(action, slot.getSlotIndex(), si.getId());
                NetworkHandler.instance().sendToServer(p);
            }

            ci.cancel();
        }
    }
}
