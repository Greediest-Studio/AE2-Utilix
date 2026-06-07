package com.ae2utilix.mixin;

import appeng.core.sync.AppEngPacket;
import appeng.core.sync.network.INetworkInfo;
import appeng.core.sync.packets.PacketJEIRecipe;
import com.ae2utilix.gui.ContainerFullPattern;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.items.IItemHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import appeng.util.helpers.ItemHandlerUtil;

import java.util.List;

@Mixin(value = PacketJEIRecipe.class, remap = false)
public abstract class MixinPacketJEIRecipe {

    @Shadow
    private List<ItemStack[]> recipe;

    @Shadow
    private List<ItemStack> output;

    @Inject(method = "serverPacketData", at = @At("RETURN"), remap = false)
    private void ae2utilix$handleFullPatternOutput(INetworkInfo manager,
                                                    AppEngPacket packet,
                                                    EntityPlayer player, CallbackInfo ci) {
        Container con = player.openContainer;
        if (!(con instanceof ContainerFullPattern)) {
            return;
        }

        ContainerFullPattern cpt = (ContainerFullPattern) con;

        // Only fill outputs in processing mode
        if (cpt.isCraftingMode()) {
            return;
        }

        if (this.output == null || this.output.isEmpty()) {
            return;
        }

        // Fill output slots with JEI recipe outputs
        IItemHandler outputInv = cpt.getInventoryByName("output");
        // Clear existing output slots
        for (int i = 0; i < outputInv.getSlots(); i++) {
            ItemHandlerUtil.setStackInSlot(outputInv, i, ItemStack.EMPTY);
        }
        // Fill with recipe outputs
        for (int i = 0; i < this.output.size() && i < outputInv.getSlots(); i++) {
            ItemStack out = this.output.get(i);
            if (out == null || out.isEmpty()) {
                continue;
            }
            ItemHandlerUtil.setStackInSlot(outputInv, i, out);
        }

        // Also ensure inputs were filled correctly for processing mode
        // The original handler fills crafting slots based on slot index matching SlotFakeCraftingMatrix,
        // but our container has 81 input slots. We need to fill them correctly.
        if (this.recipe != null && !this.recipe.isEmpty()) {
            IItemHandler craftingInv = cpt.getInventoryByName("crafting");
            // Clear all crafting slots first
            for (int i = 0; i < craftingInv.getSlots(); i++) {
                ItemHandlerUtil.setStackInSlot(craftingInv, i, ItemStack.EMPTY);
            }
            // Fill crafting slots with recipe inputs
            for (int x = 0; x < this.recipe.size() && x < craftingInv.getSlots(); x++) {
                ItemStack[] options = this.recipe.get(x);
                if (options != null && options.length > 0) {
                    // Use the first option (displayed ingredient)
                    ItemStack toPlace = options[0].copy();
                    ItemHandlerUtil.setStackInSlot(craftingInv, x, toPlace);
                }
            }
        }

        cpt.detectAndSendChanges();
    }
}
