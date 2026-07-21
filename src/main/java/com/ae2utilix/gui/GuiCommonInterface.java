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
import java.util.Locale;
import java.util.Set;
import org.lwjgl.input.Keyboard;

public class GuiCommonInterface extends AEBaseGui {

    private static final ResourceLocation BACKGROUND =
            new ResourceLocation("ae2_utilix", "textures/guis/common_interface.png");

    private final ContainerCommonInterface container;
    private AmountTextField amountField;
    private Slot amountSlot;
    private boolean amountFieldActive;
    private final Set<Integer> fluidDragSlots = new HashSet<>();
    private boolean fluidMarkGestureActive;
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
        this.drawVirtualTypeIcons();
        this.drawFluidAmountOverlays();
        this.drawGasOverlays();
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
            int textX = renderX + 2;
            int cursorPosition = Math.min(this.amountField.getCursorPosition(), text.length());
            int selectionEnd = Math.min(this.amountField.getSelectionEnd(), text.length());
            int selectionStart = Math.min(cursorPosition, selectionEnd);
            int selectionFinish = Math.max(cursorPosition, selectionEnd);
            int selectionX = textX + this.fontRenderer.getStringWidth(text.substring(0, selectionStart));
            int selectionFinishX = textX + this.fontRenderer.getStringWidth(text.substring(0, selectionFinish));

            if (selectionStart != selectionFinish) {
                drawRect(selectionX, renderY,
                        selectionFinishX, renderY + this.fontRenderer.FONT_HEIGHT, 0xFFFFFFFF);
            }
            this.fontRenderer.drawString(text.substring(0, selectionStart), textX, renderY + 1, 0xFFFFFF);
            this.fontRenderer.drawString(text.substring(selectionStart, selectionFinish),
                    selectionX, renderY + 1, selectionStart == selectionFinish ? 0xFFFFFF : 0x0000FF);
            this.fontRenderer.drawString(text.substring(selectionFinish),
                    selectionFinishX, renderY + 1, 0xFFFFFF);
            if (this.amountField.isFocused()
                    && (this.mc.ingameGUI.getUpdateCounter() / 6) % 2 == 0) {
                int cursorX = renderX + 2 + this.fontRenderer.getStringWidth(text.substring(0, cursorPosition));
                String cursor = cursorPosition < text.length() ? "|" : "_";
                this.fontRenderer.drawString(cursor, cursorX, renderY + 1, 0xFFFFFF);
            }
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.enableLighting();
            GlStateManager.enableBlend();
            GlStateManager.enableDepth();
        }
        // Keep the custom virtual amount above both the packet icon and any
        // native item overlay rendered by the slot.
        this.drawManaAndFeOverlays();
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

    private void drawGasOverlays() {
        if (!com.ae2utilix.integration.MekanismEnergisticsIntegration.isAvailable()) return;
        for (int slotIndex = 0; slotIndex < 36; slotIndex++) {
            Slot slot = this.inventorySlots.getSlot(slotIndex);
            if (!this.isFluidConfigSlot(slot)) continue;
            boolean extended = slot.slotNumber % 4 >= 2;
            int configSlot = slot.slotNumber / 4;
            String gasName = com.ae2utilix.item.ItemFluidMark.getGasName(slot.getStack());
            if (gasName != null) {
                com.ae2utilix.client.MekanismEnergisticsClientRenderer.renderGasAmount(
                        this.fontRenderer, gasName,
                        this.container.getTile().getGasConfigAmount(extended, configSlot),
                        slot.xPos, slot.yPos);
            }
        }

        for (int slotIndex = 0; slotIndex < 36; slotIndex++) {
            Slot slot = this.inventorySlots.getSlot(slotIndex);
            if (slot.slotNumber % 4 != 1 && slot.slotNumber % 4 != 3) continue;
            boolean extended = slot.slotNumber % 4 >= 2;
            int storageSlot = slot.slotNumber / 4;
            String gasName = this.container.getTile().getStoredGasName(extended, storageSlot);
            if (gasName != null && this.container.getTile().getStoredGasAmount(extended, storageSlot) > 0) {
                com.ae2utilix.client.MekanismEnergisticsClientRenderer.renderGasSlot(
                        this.fontRenderer, gasName,
                        this.container.getTile().getStoredGasAmount(extended, storageSlot),
                        slot.xPos, slot.yPos);
            }
        }
    }

    private void drawVirtualTypeIcons() {
        for (int slotIndex = 0; slotIndex < 36; slotIndex++) {
            Slot slot = this.inventorySlots.getSlot(slotIndex);
            if (!this.isFluidStorageSlot(slot)) continue;
            boolean extended = slot.slotNumber % 4 >= 2;
            int configSlot = slot.slotNumber / 4;
            ItemStack icon = null;
            if (this.container.getTile().getStoredMana(extended, configSlot) > 0) {
                icon = com.ae2utilix.integration.BotaniaFluxIntegration
                        .getPacketStack(com.ae2utilix.integration.BotaniaFluxIntegration.MANA);
            } else if (this.container.getTile().getStoredFe(extended, configSlot) > 0) {
                icon = com.ae2utilix.integration.BotaniaFluxIntegration
                        .getPacketStack(com.ae2utilix.integration.BotaniaFluxIntegration.FE);
            }
            if (icon != null) {
                net.minecraft.client.renderer.RenderHelper.enableGUIStandardItemLighting();
                GlStateManager.enableDepth();
                this.mc.getRenderItem().renderItemAndEffectIntoGUI(icon, slot.xPos, slot.yPos);
                GlStateManager.disableDepth();
                net.minecraft.client.renderer.RenderHelper.disableStandardItemLighting();
            }
        }
    }

    private void drawManaAndFeOverlays() {
        for (int slotIndex = 0; slotIndex < 36; slotIndex++) {
            Slot slot = this.inventorySlots.getSlot(slotIndex);
            if (!this.isFluidConfigSlot(slot) && !this.isFluidStorageSlot(slot)) continue;
            boolean extended = slot.slotNumber % 4 >= 2;
            int configSlot = slot.slotNumber / 4;
            ItemStack marker = slot.getStack();
            if (com.ae2utilix.item.ItemFluidMark.isManaMark(marker)) {
                this.drawVirtualAmount(this.container.getTile().getManaConfigAmount(extended, configSlot), slot.xPos, slot.yPos);
            } else if (com.ae2utilix.item.ItemFluidMark.isFeMark(marker)) {
                this.drawVirtualAmount(this.container.getTile().getFeConfigAmount(extended, configSlot), slot.xPos, slot.yPos);
            } else if (slot.slotNumber % 4 == 1 || slot.slotNumber % 4 == 3) {
                long mana = this.container.getTile().getStoredMana(extended, configSlot);
                long fe = this.container.getTile().getStoredFe(extended, configSlot);
                if (mana > 0) this.drawVirtualAmount(mana, slot.xPos, slot.yPos);
                else if (fe > 0) this.drawVirtualAmount(fe, slot.xPos, slot.yPos);
            }
        }
    }

    private void drawVirtualAmount(long amount, int x, int y) {
        String text = this.formatVirtualAmount(amount);
        GlStateManager.disableDepth();
        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        this.zLevel = 300.0F;
        this.fontRenderer.drawStringWithShadow(text,
                x + 17 - this.fontRenderer.getStringWidth(text), y + 9, 0xFFFFFF);
        this.zLevel = 0.0F;
        GlStateManager.disableBlend();
        GlStateManager.enableLighting();
        GlStateManager.enableDepth();
    }

    private String formatVirtualAmount(long amount) {
        if (amount < 1000L) return String.valueOf(amount);

        long unit = 1000L;
        String suffix = "k";
        if (amount >= 1_000_000_000_000L) {
            unit = 1_000_000_000_000L;
            suffix = "T";
        } else if (amount >= 1_000_000_000L) {
            unit = 1_000_000_000L;
            suffix = "G";
        } else if (amount >= 1_000_000L) {
            unit = 1_000_000L;
            suffix = "M";
        }

        String value = String.format(Locale.ROOT, "%.2f", amount / (double) unit);
        while (value.endsWith("0")) value = value.substring(0, value.length() - 1);
        if (value.endsWith(".")) value = value.substring(0, value.length() - 1);
        return value + suffix;
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        this.fluidDragSlots.clear();
        this.fluidMarkGestureActive = false;
        if (mouseButton == 1) {
            if (this.amountFieldActive) {
                return;
            }
            Slot slot = this.getSlotUnderMouse();
            if (slot != null && slot.slotNumber < 36
                    && (slot.slotNumber % 4 == 0 || slot.slotNumber % 4 == 2)
                    && this.sendVirtualMark(slot)) {
                this.fluidDragSlots.add(slot.slotNumber);
                this.fluidMarkGestureActive = true;
                return;
            }
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
            int limit = com.ae2utilix.item.ItemFluidMark.isVirtualMark(this.amountSlot.getStack())
                    ? this.container.getTile().getVirtualStorageCapacity()
                    : this.container.getTile().getItemSlotCapacity();
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
    protected void mouseWheelEvent(int x, int y, int wheel) {
        Slot slot = this.getSlot(x, y);
        if (this.isFluidMarkSlot(slot) && this.adjustVirtualAmount(slot, wheel)) {
            return;
        }
        super.mouseWheelEvent(x, y, wheel);
    }

    private boolean adjustVirtualAmount(Slot slot, int wheel) {
        ItemStack marker = slot.getStack();
        if (!com.ae2utilix.item.ItemFluidMark.isVirtualMark(marker)) return false;
        if (!Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) && !Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) {
            return false;
        }

        boolean extended = slot.slotNumber % 4 >= 2;
        int configSlot = slot.slotNumber / 4;
        int current;
        net.minecraftforge.fluids.FluidStack fluid = com.ae2utilix.item.ItemFluidMark.getFluid(marker);
        if (fluid != null) {
            net.minecraftforge.fluids.FluidStack configured = this.container.getTile()
                    .getFluidConfig(extended, configSlot);
            current = configured == null ? 1000 : configured.amount;
        } else {
            if (com.ae2utilix.item.ItemFluidMark.isGasMark(marker)) {
                current = this.container.getTile().getGasConfigAmount(extended, configSlot);
            } else if (com.ae2utilix.item.ItemFluidMark.isManaMark(marker)) {
                current = this.container.getTile().getManaConfigAmount(extended, configSlot);
            } else {
                current = this.container.getTile().getFeConfigAmount(extended, configSlot);
            }
        }

        long next;
        if (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL)) {
            next = wheel > 0 ? (long) current * 2L : current / 2L;
        } else {
            next = current + (wheel > 0 ? 1L : -1L);
        }
        int amount = (int) Math.min(this.container.getTile().getVirtualStorageCapacity(),
                Math.max(1L, next));
        NetworkHandler.CHANNEL.sendToServer(new com.ae2utilix.network.PacketCommonInterfaceSetAmount(
                this.container.getTilePosition(), configSlot, amount, extended));
        if (this.amountFieldActive && this.amountSlot == slot) {
            this.amountField.setText(String.valueOf(amount));
            this.amountField.setCursorPositionEnd();
        }
        return true;
    }

    @Override
    protected void renderHoveredToolTip(int mouseX, int mouseY) {
        Slot slot = this.getSlotUnderMouse();
        if (slot != null && slot.slotNumber < 36
                && (slot.slotNumber % 4 == 1 || slot.slotNumber % 4 == 3)) {
            boolean extended = slot.slotNumber % 4 >= 2;
            int storageSlot = slot.slotNumber / 4;
            java.util.List<String> tips = new java.util.ArrayList<>();
            net.minecraftforge.fluids.FluidStack storedFluid = this.container.getTile()
                    .getStoredFluid(extended, storageSlot);
            if (storedFluid != null && storedFluid.amount > 0) {
                tips.add(I18n.format("ae2_utilix.common_interface.stored",
                        storedFluid.getLocalizedName(), storedFluid.amount));
            } else {
                String gasName = this.container.getTile().getStoredGasName(extended, storageSlot);
                int gasAmount = this.container.getTile().getStoredGasAmount(extended, storageSlot);
                if (gasName != null && gasAmount > 0) {
                    String displayName = com.ae2utilix.integration.MekanismEnergisticsIntegration
                            .getGasDisplayName(gasName);
                    tips.add(I18n.format("ae2_utilix.common_interface.stored",
                            displayName == null ? gasName : displayName, gasAmount));
                } else {
                    long mana = this.container.getTile().getStoredMana(extended, storageSlot);
                    long fe = this.container.getTile().getStoredFe(extended, storageSlot);
                    if (mana > 0) {
                        tips.add(com.ae2utilix.integration.BotaniaFluxIntegration.getStoredTooltip(
                                com.ae2utilix.integration.BotaniaFluxIntegration.MANA, mana));
                    } else if (fe > 0) {
                        tips.add(com.ae2utilix.integration.BotaniaFluxIntegration.getStoredTooltip(
                                com.ae2utilix.integration.BotaniaFluxIntegration.FE, fe));
                    }
                }
            }
            if (!tips.isEmpty()) {
                this.drawHoveringText(tips, mouseX, mouseY);
                return;
            }
        }
        if (slot != null && slot.slotNumber < 36
                && (slot.slotNumber % 4 == 0 || slot.slotNumber % 4 == 2)) {
            java.util.List<String> tips = new java.util.ArrayList<>();
            ItemStack marked = slot.getStack();
            if (!marked.isEmpty()) {
                net.minecraftforge.fluids.FluidStack markedFluid = com.ae2utilix.item.ItemFluidMark.getFluid(marked);
                if (markedFluid != null) {
                    tips.add(I18n.format("ae2_utilix.common_interface.marked_fluid", markedFluid.getLocalizedName()));
                } else if (com.ae2utilix.item.ItemFluidMark.isGasMark(marked)) {
                    String gasName = com.ae2utilix.item.ItemFluidMark.getGasName(marked);
                    String displayName = com.ae2utilix.integration.MekanismEnergisticsIntegration
                            .getGasDisplayName(gasName);
                    tips.add(I18n.format("ae2_utilix.common_interface.marked_gas", displayName == null ? gasName : displayName));
                } else if (com.ae2utilix.item.ItemFluidMark.isManaMark(marked)
                        || com.ae2utilix.item.ItemFluidMark.isFeMark(marked)) {
                    tips.add(I18n.format("ae2_utilix.common_interface.marked_item", marked.getDisplayName()));
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
                    String gasName = com.ae2utilix.integration.MekanismEnergisticsIntegration.getGasNameFromItem(held);
                    if (gasName != null) {
                        String displayName = com.ae2utilix.integration.MekanismEnergisticsIntegration
                                .getGasDisplayName(gasName);
                        tips.add(I18n.format("ae2_utilix.common_interface.left_click_item", held.getDisplayName()));
                        tips.add(I18n.format("ae2_utilix.common_interface.right_click_gas",
                                displayName == null ? gasName : displayName));
                    } else {
                        int specialType = com.ae2utilix.integration.BotaniaFluxIntegration.getMarkedType(held);
                        if (specialType != 0) {
                            tips.add(I18n.format("ae2_utilix.common_interface.left_click_item", held.getDisplayName()));
                            tips.add(I18n.format("ae2_utilix.common_interface.right_click_special",
                                    com.ae2utilix.integration.BotaniaFluxIntegration.getDisplayName(specialType)));
                        } else {
                            tips.add(I18n.format("ae2_utilix.common_interface.left_click_item", held.getDisplayName()));
                        }
                    }
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
            if (slot != null && this.isFluidMarkSlot(slot) && this.sendVirtualMark(slot)) {
                this.fluidDragSlots.add(slotIdx);
                this.fluidMarkGestureActive = true;
                return;
            }
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
            String gasName = com.ae2utilix.item.ItemFluidMark.getGasName(slot.getStack());
            boolean extended = slotIdx % 4 >= 2;
            int configSlot = slotIdx / 4;
            int amount;
            if (gasName != null) {
                amount = this.container.getTile().getGasConfigAmount(extended, configSlot);
            } else if (com.ae2utilix.item.ItemFluidMark.isManaMark(slot.getStack())) {
                amount = this.container.getTile().getManaConfigAmount(extended, configSlot);
            } else if (com.ae2utilix.item.ItemFluidMark.isFeMark(slot.getStack())) {
                amount = this.container.getTile().getFeConfigAmount(extended, configSlot);
            } else {
                amount = fluid == null ? slot.getStack().getCount() : fluid.amount;
            }
            this.amountField.setText(String.valueOf(amount));
            this.amountField.setVisible(true);
            this.amountField.setFocused(true);
            return;
        }
        super.handleMouseClick(slot, slotIdx, mouseButton, clickType);
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int mouseButton, long timeSinceClick) {
        if (mouseButton == 1 && !this.amountFieldActive && this.fluidMarkGestureActive) {
            Slot slot = this.getSlot(mouseX, mouseY);
            if (this.isFluidMarkSlot(slot) && !this.fluidDragSlots.contains(slot.slotNumber)
                    && this.sendVirtualMark(slot)) {
                this.fluidDragSlots.add(slot.slotNumber);
                return;
            }
            return;
        }
        super.mouseClickMove(mouseX, mouseY, mouseButton, timeSinceClick);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        boolean suppressNormalRightClick = state == 1 && this.fluidMarkGestureActive;
        this.fluidDragSlots.clear();
        this.fluidMarkGestureActive = false;
        if (suppressNormalRightClick) return;
        super.mouseReleased(mouseX, mouseY, state);
    }

    private boolean isFluidMarkSlot(Slot slot) {
        return slot != null && slot.slotNumber < 36
                && (slot.slotNumber % 4 == 0 || slot.slotNumber % 4 == 2);
    }

    private boolean isFluidConfigSlot(Slot slot) {
        return this.isFluidMarkSlot(slot);
    }

    private boolean isFluidStorageSlot(Slot slot) {
        return slot != null && slot.slotNumber < 36
                && (slot.slotNumber % 4 == 1 || slot.slotNumber % 4 == 3);
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

    private boolean sendVirtualMark(Slot slot) {
        if (!this.isFluidMarkSlot(slot)) return false;
        ItemStack held = this.mc.player.inventory.getItemStack();
        net.minecraftforge.fluids.FluidStack fluid = this.getHeldFluid(held);
        boolean extended = slot.slotNumber % 4 >= 2;
        int configSlot = slot.slotNumber / 4;
        if (fluid != null) {
            NetworkHandler.CHANNEL.sendToServer(new com.ae2utilix.network.PacketCommonInterfaceFluidMark(
                    this.container.getTilePosition(), configSlot, true, extended, fluid));
            return true;
        }

        String gasName = com.ae2utilix.integration.MekanismEnergisticsIntegration.getGasNameFromItem(held);
        if (gasName != null) {
            NetworkHandler.CHANNEL.sendToServer(new com.ae2utilix.network.PacketCommonInterfaceFluidMark(
                    this.container.getTilePosition(), configSlot, extended, gasName));
            return true;
        }

        int specialType = com.ae2utilix.integration.BotaniaFluxIntegration.getMarkedType(held);
        if (specialType == 0) return false;
        NetworkHandler.CHANNEL.sendToServer(new com.ae2utilix.network.PacketCommonInterfaceFluidMark(
                this.container.getTilePosition(), configSlot, extended, specialType));
        return true;
    }

}
