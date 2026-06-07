package com.ae2utilix.mixin;

import appeng.container.AEBaseContainer;
import appeng.core.sync.AppEngPacket;
import appeng.core.sync.network.INetworkInfo;
import appeng.core.sync.packets.PacketValueConfig;
import com.ae2utilix.gui.ContainerFullPattern;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PacketValueConfig.class, remap = false)
public class MixinPacketValueConfig {

    @Shadow
    private String Name;

    @Shadow
    private String Value;

    @Inject(method = "serverPacketData", at = @At("HEAD"), cancellable = true, remap = false)
    private void ae2utilix$handlePatternTerminal(INetworkInfo manager,
                                                  AppEngPacket packet,
                                                  EntityPlayer player, CallbackInfo ci) {
        Container c = player.openContainer;
        if (!(c instanceof ContainerFullPattern)) {
            return;
        }
        if (!this.Name.startsWith("PatternTerminal.")) {
            return;
        }

        ContainerFullPattern cpt = (ContainerFullPattern) c;

        switch (this.Name) {
            case "PatternTerminal.CraftMode":
                cpt.setCraftingMode(this.Value.equals("1"));
                break;
            case "PatternTerminal.Encode":
                if (this.Value.equals("2")) {
                    cpt.encodeAndMoveToInventory();
                } else {
                    cpt.encode();
                }
                break;
            case "PatternTerminal.Clear":
                cpt.clear();
                break;
            case "PatternTerminal.MultiplyByTwo":
                cpt.multiply(2);
                break;
            case "PatternTerminal.MultiplyByThree":
                cpt.multiply(3);
                break;
            case "PatternTerminal.DivideByTwo":
                cpt.divide(2);
                break;
            case "PatternTerminal.DivideByThree":
                cpt.divide(3);
                break;
            case "PatternTerminal.IncreaseByOne":
                cpt.increase(1);
                break;
            case "PatternTerminal.DecreaseByOne":
                cpt.decrease(1);
                break;
            case "PatternTerminal.SetSlotCount":
                // Format: "slotIndex:newCount"
                try {
                    String[] parts = this.Value.split(":");
                    if (parts.length == 2) {
                        int slotIndex = Integer.parseInt(parts[0]);
                        int newCount = Integer.parseInt(parts[1]);
                        cpt.setSlotCount(slotIndex, newCount);
                    }
                } catch (NumberFormatException ignored) {
                }
                break;
            case "PatternTerminal.Substitute":
                cpt.setSubstitute(this.Value.equals("1"));
                break;
            case "PatternTerminal.FluidCraft":
                cpt.encodeFluidCraftPattern();
                break;
            case "PatternTerminal.Page":
                try {
                    cpt.setCurrentPage(Integer.parseInt(this.Value));
                } catch (NumberFormatException ignored) {
                }
                break;
            default:
                return; // Don't cancel for unknown keys
        }

        ci.cancel();
    }
}
