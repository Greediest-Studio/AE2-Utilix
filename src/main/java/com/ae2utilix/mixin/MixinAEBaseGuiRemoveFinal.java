package com.ae2utilix.mixin;

import appeng.client.gui.AEBaseGui;
import appeng.client.gui.widgets.GuiCustomSlot;
import appeng.container.slot.AppEngSlot;
import appeng.container.slot.IOptionalSlot;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

/**
 * Remove final from AEBaseGui.func_146976_a (drawGuiContainerBackgroundLayer)
 * so that GuiFullInterface can override it.
 * Extends GuiContainer to access inherited fields (guiLeft, guiTop).
 */
@Mixin(AEBaseGui.class)
public abstract class MixinAEBaseGuiRemoveFinal extends GuiContainer {

    @Shadow(remap = false)
    public abstract void drawBG(int offsetX, int offsetY, int mouseX, int mouseY);

    @Shadow(remap = false)
    private List<GuiCustomSlot> guiSlots;

    // Dummy constructor required by Java, never called
    private MixinAEBaseGuiRemoveFinal() {
        super(null);
    }

    /**
     * @author AE2Utilix
     * @reason Remove final modifier so GuiFullInterface can override drawGuiContainerBackgroundLayer
     */
    @Overwrite(remap = false)
    protected void func_146976_a(final float f, final int x, final int y) {
        final int ox = this.guiLeft;
        final int oy = this.guiTop;
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.drawBG(ox, oy, x, y);

        final List<Slot> slots = this.inventorySlots.inventorySlots;
        for (final Slot slot : slots) {
            if (slot instanceof IOptionalSlot) {
                final IOptionalSlot optionalSlot = (IOptionalSlot) slot;
                if (optionalSlot.isRenderDisabled()) {
                    final AppEngSlot aeSlot = (AppEngSlot) slot;
                    if (aeSlot.isSlotEnabled()) {
                        this.drawTexturedModalRect(ox + aeSlot.xPos - 1, oy + aeSlot.yPos - 1, optionalSlot.getSourceX() - 1, optionalSlot.getSourceY() - 1, 18, 18);
                    } else {
                        GlStateManager.color(1.0F, 1.0F, 1.0F, 0.4F);
                        GlStateManager.enableBlend();
                        this.drawTexturedModalRect(ox + aeSlot.xPos - 1, oy + aeSlot.yPos - 1, optionalSlot.getSourceX() - 1, optionalSlot.getSourceY() - 1, 18, 18);
                        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                    }
                }
            }
        }

        for (final GuiCustomSlot slot : this.guiSlots) {
            slot.drawBackground(ox, oy);
        }
    }
}
