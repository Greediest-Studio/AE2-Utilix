package com.ae2utilix.gui;

import appeng.client.gui.AEBaseGui;
import com.ae2utilix.AE2Utilix;
import com.ae2utilix.block.TileCommonInterfaceAlternate;
import com.ae2utilix.network.NetworkHandler;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;
import appeng.fluids.client.render.FluidStackSizeRenderer;
import appeng.fluids.util.AEFluidStack;
import net.minecraft.client.renderer.GlStateManager;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class GuiCommonInterface extends AEBaseGui {

    private static final ResourceLocation BACKGROUND =
            new ResourceLocation("ae2_utilix", "textures/guis/common_interface.png");

    private final ContainerCommonInterface container;
    private AmountTextField amountField;
    private Slot amountSlot;
    private boolean amountFieldActive;
    private final Set<Integer> fluidDragSlots = new HashSet<>();
    private final FluidStackSizeRenderer fluidAmountRenderer = new FluidStackSizeRenderer();

    private static final class AmountTextField extends GuiTextField {
        private AmountTextField(int id, net.minecraft.client.gui.FontRenderer fontRenderer,
                                int x, int y, int width, int height) {
            super(id, fontRenderer, x, y, width, height);
        }

        @Override
        public void drawTextBox() {
            // The field is rendered manually in drawFG to match the terminal style.
        }
    }

    public GuiCommonInterface(InventoryPlayer inventory, TileCommonInterfaceAlternate tile) {
        super(new ContainerCommonInterface(inventory, tile));
        this.container = (ContainerCommonInterface) this.inventorySlots;
        this.xSize = 246;
        this.ySize = 216;
    }

    @Override
    public void initGui() {
        super.initGui();
        this.amountField = new AmountTextField(0, this.fontRenderer, 0, 0,
                54, this.fontRenderer.FONT_HEIGHT);
        this.amountField.x = this.guiLeft + 8;
        this.amountField.y = this.guiTop + 6;
        this.amountField.setEnableBackgroundDrawing(false);
        this.amountField.setTextColor(0xFFFFFF);
        this.amountField.setMaxStringLength(6);
        this.amountField.setVisible(false);
    }

    @Override
    public void drawBG(int offsetX, int offsetY, int mouseX, int mouseY) {
        this.mc.getTextureManager().bindTexture(BACKGROUND);
        this.drawTexturedModalRect(offsetX, offsetY, 0, 0, this.xSize, this.ySize);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        this.drawBG(this.guiLeft, this.guiTop, mouseX, mouseY);
    }

    @Override
    public void drawFG(int offsetX, int offsetY, int mouseX, int mouseY) {
        this.fontRenderer.drawString(I18n.format("tile.ae2_utilix.common_interface.name"), 8, 6, 4210752);
        this.drawFluidAmountOverlays();
        if (this.amountFieldActive && this.amountField != null) {
            int renderX = this.amountField.x - this.guiLeft + 1;
            int renderY = this.amountField.y - this.guiTop - 1;
            GlStateManager.disableDepth();
            GlStateManager.disableBlend();
            GlStateManager.disableLighting();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            drawRect(renderX - 2, renderY - 2,
                    renderX + this.amountField.getWidth() + 2,
                    renderY + this.fontRenderer.FONT_HEIGHT + 2, 0xFF000000);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            String text = this.amountField.getText();
            this.fontRenderer.drawString(text, renderX + 2, renderY + 1, 0xFFFFFF);
            if (this.amountField.isFocused()
                    && (this.mc.ingameGUI.getUpdateCounter() / 6) % 2 == 0) {
                int cursorPosition = Math.min(this.amountField.getCursorPosition(), text.length());
                int cursorX = renderX + 2 + this.fontRenderer.getStringWidth(text.substring(0, cursorPosition));
                this.fontRenderer.drawString("|", cursorX, renderY + 1, 0xFFFFFF);
                this.fontRenderer.drawString("_", cursorX, renderY + this.fontRenderer.FONT_HEIGHT - 1, 0xFFFFFF);
            }
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.enableLighting();
            GlStateManager.enableBlend();
            GlStateManager.enableDepth();
        }
    }

    private void drawFluidAmountOverlays() {
        for (int slotIndex = 0; slotIndex < 36; slotIndex++) {
            Slot slot = this.inventorySlots.getSlot(slotIndex);
            if (!this.isFluidConfigSlot(slot) || !com.ae2utilix.item.ItemFluidMark.isFluidMark(slot.getStack())) {
                continue;
            }

            net.minecraftforge.fluids.FluidStack fluid = this.container.getTile().getFluidConfig(
                    slot.slotNumber % 4 >= 2, slot.slotNumber / 4);
            if (fluid == null) {
                continue;
            }

            this.fluidAmountRenderer.renderStackSize(this.fontRenderer,
                    AEFluidStack.fromFluidStack(fluid), slot.xPos, slot.yPos);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        this.fluidDragSlots.clear();
        if (mouseButton == 1) {
            if (this.amountFieldActive) {
                return;
            }
            Slot slot = this.getSlotUnderMouse();
            ItemStack held = this.mc.player.inventory.getItemStack();
            net.minecraftforge.fluids.FluidStack heldFluid = this.getHeldFluid(held);
            if (slot != null && slot.slotNumber < 36
                    && (slot.slotNumber % 4 == 0 || slot.slotNumber % 4 == 2)
                    && heldFluid != null) {
                boolean extended = slot.slotNumber % 4 >= 2;
                int configSlot = slot.slotNumber / 4;
                NetworkHandler.CHANNEL.sendToServer(new com.ae2utilix.network.PacketCommonInterfaceFluidMark(
                        this.container.getTilePosition(), configSlot, true, extended, heldFluid));
                this.fluidDragSlots.add(slot.slotNumber);
            }
            return;
        }
        if (this.amountFieldActive && this.amountField != null) {
            int fieldX = this.amountField.x;
            int fieldY = this.amountField.y;
            if (mouseX >= fieldX && mouseX <= fieldX + this.amountField.getWidth()
                    && mouseY >= fieldY && mouseY <= fieldY + this.fontRenderer.FONT_HEIGHT) {
                this.amountField.mouseClicked(mouseX, mouseY, mouseButton);
                return;
            }
            this.confirmAmountField();
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    private void confirmAmountField() {
        if (this.amountSlot == null || this.amountField == null) return;
        try {
            int amount = Integer.parseInt(this.amountField.getText());
            int slotIdx = this.amountSlot.slotNumber;
            boolean extended = slotIdx % 4 >= 2;
            int configSlot = slotIdx / 4;
            int limit = com.ae2utilix.item.ItemFluidMark.isFluidMark(this.amountSlot.getStack()) ? 512000 : 512;
            NetworkHandler.CHANNEL.sendToServer(new com.ae2utilix.network.PacketCommonInterfaceSetAmount(
                    this.container.getTilePosition(), configSlot, Math.min(limit, Math.max(1, amount)), extended));
        } catch (NumberFormatException ignored) {
        }
        this.amountFieldActive = false;
        this.amountField.setVisible(false);
        this.amountSlot = null;
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (this.amountFieldActive) {
            if (keyCode == org.lwjgl.input.Keyboard.KEY_RETURN || keyCode == org.lwjgl.input.Keyboard.KEY_NUMPADENTER) {
                this.confirmAmountField();
                return;
            }
            if (keyCode == org.lwjgl.input.Keyboard.KEY_ESCAPE) {
                this.amountFieldActive = false;
                this.amountField.setVisible(false);
                return;
            }
            if (this.amountField.textboxKeyTyped(typedChar, keyCode)) return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void renderHoveredToolTip(int mouseX, int mouseY) {
        Slot slot = this.getSlotUnderMouse();
        if (slot != null && slot.slotNumber < 36
                && (slot.slotNumber % 4 == 0 || slot.slotNumber % 4 == 2)) {
            java.util.List<String> tips = new java.util.ArrayList<>();
            ItemStack marked = slot.getStack();
            if (!marked.isEmpty()) {
                net.minecraftforge.fluids.FluidStack markedFluid = com.ae2utilix.item.ItemFluidMark.getFluid(marked);
                if (markedFluid != null) {
                    tips.add(I18n.format("ae2_utilix.common_interface.marked_fluid", markedFluid.getLocalizedName()));
                } else {
                    tips.add(I18n.format("ae2_utilix.common_interface.marked_item", marked.getDisplayName()));
                }
            }
            ItemStack held = this.mc.player.inventory.getItemStack();
            if (!held.isEmpty()) {
                net.minecraftforge.fluids.FluidStack fluid = this.getHeldFluid(held);
                if (fluid != null) {
                    tips.add(I18n.format("ae2_utilix.common_interface.left_click_item", held.getDisplayName()));
                    tips.add(I18n.format("ae2_utilix.common_interface.right_click_fluid", fluid.getLocalizedName()));
                } else {
                    tips.add(I18n.format("ae2_utilix.common_interface.left_click_item", held.getDisplayName()));
                }
            }
            if (!tips.isEmpty()) {
                this.drawHoveringText(tips, mouseX, mouseY);
                return;
            }
        }
        super.renderHoveredToolTip(mouseX, mouseY);
    }

    @Override
    protected void handleMouseClick(Slot slot, int slotIdx, int mouseButton, ClickType clickType) {
        if (mouseButton == 1) {
            if (slot != null && this.isFluidMarkSlot(slot) && this.getHeldFluid(this.mc.player.inventory.getItemStack()) != null) {
                boolean extended = slotIdx % 4 >= 2;
                net.minecraftforge.fluids.FluidStack fluid = this.getHeldFluid(this.mc.player.inventory.getItemStack());
                NetworkHandler.CHANNEL.sendToServer(new com.ae2utilix.network.PacketCommonInterfaceFluidMark(
                        this.container.getTilePosition(), slotIdx / 4, true, extended, fluid));
                this.fluidDragSlots.add(slotIdx);
            }
            return;
        }
        if (slot != null && mouseButton == 2 && (slotIdx % 4 == 0 || slotIdx % 4 == 2)) {
            this.amountSlot = slot;
            this.amountFieldActive = true;
            this.amountField.x = this.guiLeft + slot.xPos;
            this.amountField.y = this.guiTop + slot.yPos - 12;
            net.minecraftforge.fluids.FluidStack fluid = com.ae2utilix.item.ItemFluidMark.getFluid(slot.getStack());
            if (fluid != null) {
                fluid = this.container.getTile().getFluidConfig(slotIdx % 4 >= 2, slotIdx / 4);
            }
            this.amountField.setMaxStringLength(10);
            this.amountField.setText(String.valueOf(fluid == null ? slot.getStack().getCount() : fluid.amount));
            this.amountField.setVisible(true);
            this.amountField.setFocused(true);
            return;
        }
        super.handleMouseClick(slot, slotIdx, mouseButton, clickType);
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int mouseButton, long timeSinceClick) {
        if (mouseButton == 1 && !this.amountFieldActive) {
            Slot slot = this.getSlot(mouseX, mouseY);
            ItemStack held = this.mc.player.inventory.getItemStack();
            net.minecraftforge.fluids.FluidStack heldFluid = this.getHeldFluid(held);
            if (this.isFluidMarkSlot(slot) && heldFluid != null && this.fluidDragSlots.add(slot.slotNumber)) {
                boolean extended = slot.slotNumber % 4 >= 2;
                NetworkHandler.CHANNEL.sendToServer(new com.ae2utilix.network.PacketCommonInterfaceFluidMark(
                        this.container.getTilePosition(), slot.slotNumber / 4, true, extended, heldFluid));
                return;
            }
            return;
        }
        super.mouseClickMove(mouseX, mouseY, mouseButton, timeSinceClick);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        this.fluidDragSlots.clear();
        if (state == 1) {
            return;
        }
        super.mouseReleased(mouseX, mouseY, state);
    }

    private boolean isFluidMarkSlot(Slot slot) {
        return slot != null && slot.slotNumber < 36
                && (slot.slotNumber % 4 == 0 || slot.slotNumber % 4 == 2);
    }

    private boolean isFluidConfigSlot(Slot slot) {
        return this.isFluidMarkSlot(slot);
    }

    private net.minecraftforge.fluids.FluidStack getHeldFluid(ItemStack held) {
        if (held == null || held.isEmpty()) return null;
        net.minecraftforge.fluids.FluidStack fluid = FluidUtil.getFluidContained(held);
        if (fluid == null) fluid = com.ae2utilix.item.ItemFluidMark.getFluid(held);
        if (fluid == null && held.getItem() == net.minecraft.init.Items.WATER_BUCKET) {
            fluid = new net.minecraftforge.fluids.FluidStack(net.minecraftforge.fluids.FluidRegistry.WATER, 1000);
        }
        return fluid;
    }

}
