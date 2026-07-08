package com.ae2utilix.gui;

import appeng.api.config.ActionItems;
import appeng.api.config.ItemSubstitution;
import appeng.api.config.Settings;
import appeng.api.storage.ITerminalHost;
import appeng.client.gui.implementations.GuiMEMonitorable;
import appeng.client.gui.widgets.GuiImgButton;
import appeng.client.gui.widgets.GuiScrollbar;
import appeng.client.gui.widgets.GuiTabButton;
import appeng.container.interfaces.IJEIGhostIngredients;
import appeng.container.slot.SlotFake;
import appeng.core.localization.GuiText;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.PacketInventoryAction;
import appeng.core.sync.packets.PacketValueConfig;
import appeng.helpers.InventoryAction;
import appeng.util.item.AEItemStack;
import com.ae2utilix.AE2Utilix;
import com.ae2utilix.block.terminal.TilePatternTerminal;
import com.ae2utilix.integration.AE2FCRUCompat;
import mezz.jei.api.gui.IGhostIngredientHandler;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.FluidStack;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.awt.*;
import java.io.IOException;
import java.util.*;

public class GuiFullPattern extends GuiMEMonitorable implements IJEIGhostIngredients {

    private final ContainerFullPattern container;
    private GuiTabButton tabCraftButton;
    private GuiTabButton tabProcessButton;
    private GuiImgButton substitutionsEnabledBtn;
    private GuiImgButton substitutionsDisabledBtn;
    private GuiImgButton encodeBtn;
    private GuiImgButton clearBtn;
    private GuiImgButton x2Btn;
    private GuiImgButton x3Btn;
    private GuiImgButton plusOneBtn;
    private GuiImgButton divTwoBtn;
    private GuiImgButton divThreeBtn;
    private GuiImgButton minusOneBtn;
    private GuiScrollbar patternScrollbar;

    // AE2FCRU buttons
    private GuiImgButton combineEnableBtn;
    private GuiImgButton combineDisableBtn;
    private GuiImgButton fluidEnableBtn;
    private GuiImgButton fluidDisableBtn;
    private GuiImgButton craftingFluidBtn;

    // Middle-click quantity input
    private GuiTextField amountField;
    private SlotFake amountTargetSlot;
    private boolean amountFieldActive = false;

    // JEI ghost ingredient support
    public Map<IGhostIngredientHandler.Target<?>, Object> mapTargetSlot = new HashMap<>();

    public GuiFullPattern(InventoryPlayer inventoryPlayer, ITerminalHost te) {
        super(inventoryPlayer, te, new ContainerFullPattern(inventoryPlayer, te));
        this.container = (ContainerFullPattern) this.inventorySlots;
        ((com.ae2utilix.mixin.MixinGuiMEMonitorableAccessor) this).ae2utilix$setReservedSpace(81);
    }

    @Override
    public void initGui() {
        super.initGui();

        this.tabCraftButton = new GuiTabButton(this.guiLeft + 173, this.guiTop + this.ySize - 177,
                new ItemStack(Blocks.CRAFTING_TABLE), GuiText.CraftingPattern.getLocal(), this.itemRender);
        this.buttonList.add(this.tabCraftButton);

        this.tabProcessButton = new GuiTabButton(this.guiLeft + 173, this.guiTop + this.ySize - 177,
                new ItemStack(Blocks.FURNACE), GuiText.ProcessingPattern.getLocal(), this.itemRender);
        this.buttonList.add(this.tabProcessButton);

        this.substitutionsEnabledBtn = new GuiImgButton(this.guiLeft + 84, this.guiTop + this.ySize - 163,
                Settings.ACTIONS, ItemSubstitution.ENABLED);
        this.substitutionsEnabledBtn.setHalfSize(true);
        this.buttonList.add(this.substitutionsEnabledBtn);

        this.substitutionsDisabledBtn = new GuiImgButton(this.guiLeft + 84, this.guiTop + this.ySize - 163,
                Settings.ACTIONS, ItemSubstitution.DISABLED);
        this.substitutionsDisabledBtn.setHalfSize(true);
        this.buttonList.add(this.substitutionsDisabledBtn);

        this.clearBtn = new GuiImgButton(this.guiLeft + 74, this.guiTop + this.ySize - 163,
                Settings.ACTIONS, ActionItems.CLOSE);
        this.clearBtn.setHalfSize(true);
        this.buttonList.add(this.clearBtn);

        this.encodeBtn = new GuiImgButton(this.guiLeft + 147, this.guiTop + this.ySize - 142,
                Settings.ACTIONS, ActionItems.ENCODE);
        this.buttonList.add(this.encodeBtn);

        // Processing mode quantity adjustment buttons (same layout as AE2 UEL GuiPatternTerm)
        this.x3Btn = new GuiImgButton(this.guiLeft + 128, this.guiTop + this.ySize - 158,
                Settings.ACTIONS, ActionItems.MULTIPLY_BY_THREE);
        this.x3Btn.setHalfSize(true);
        this.buttonList.add(this.x3Btn);

        this.x2Btn = new GuiImgButton(this.guiLeft + 128, this.guiTop + this.ySize - 148,
                Settings.ACTIONS, ActionItems.MULTIPLY_BY_TWO);
        this.x2Btn.setHalfSize(true);
        this.buttonList.add(this.x2Btn);

        this.plusOneBtn = new GuiImgButton(this.guiLeft + 128, this.guiTop + this.ySize - 138,
                Settings.ACTIONS, ActionItems.INCREASE_BY_ONE);
        this.plusOneBtn.setHalfSize(true);
        this.buttonList.add(this.plusOneBtn);

        this.divThreeBtn = new GuiImgButton(this.guiLeft + 100, this.guiTop + this.ySize - 158,
                Settings.ACTIONS, ActionItems.DIVIDE_BY_THREE);
        this.divThreeBtn.setHalfSize(true);
        this.buttonList.add(this.divThreeBtn);

        this.divTwoBtn = new GuiImgButton(this.guiLeft + 100, this.guiTop + this.ySize - 148,
                Settings.ACTIONS, ActionItems.DIVIDE_BY_TWO);
        this.divTwoBtn.setHalfSize(true);
        this.buttonList.add(this.divTwoBtn);

        this.minusOneBtn = new GuiImgButton(this.guiLeft + 100, this.guiTop + this.ySize - 138,
                Settings.ACTIONS, ActionItems.DECREASE_BY_ONE);
        this.minusOneBtn.setHalfSize(true);
        this.buttonList.add(this.minusOneBtn);

        // Middle-click quantity input text field (hidden by default)
        this.amountField = new GuiTextField(0, this.fontRenderer, 0, 0, 66, this.fontRenderer.FONT_HEIGHT);
        this.amountField.setEnableBackgroundDrawing(false);
        this.amountField.setMaxStringLength(10);
        this.amountField.setTextColor(0xFFFFFF);
        this.amountField.setVisible(false);
        this.amountField.setFocused(false);

        // Processing mode paging scrollbar
        // Position: left of input slots, aligned vertically with the 3x3 grid
        // Input slots are at x=18, y=-74 (relative to reserved space area)
        // After repositionSlot: yPos = -74 + ySize - 78 - 3 = ySize - 155
        // Scrollbar: x=5 (left of input), y=ySize-155, height=54 (3 rows * 18)
        this.patternScrollbar = new GuiScrollbar();
        this.patternScrollbar.setHeight(54);
        this.patternScrollbar.setRange(0, TilePatternTerminal.PAGE_COUNT - 1, 1);

        // AE2FCRU buttons (only when AE2FCRU is loaded)
        if (AE2FCRUCompat.isLoaded()) {
            this.combineEnableBtn = AE2FCRUCompat.createGuiFCImgButton(this.guiLeft + 84, this.guiTop + this.ySize - 163, "FORCE_COMBINE", "DO_COMBINE");
            if (this.combineEnableBtn != null) {
                this.combineEnableBtn.setHalfSize(true);
                this.buttonList.add(this.combineEnableBtn);
            }

            this.combineDisableBtn = AE2FCRUCompat.createGuiFCImgButton(this.guiLeft + 84, this.guiTop + this.ySize - 163, "NOT_COMBINE", "DONT_COMBINE");
            if (this.combineDisableBtn != null) {
                this.combineDisableBtn.setHalfSize(true);
                this.buttonList.add(this.combineDisableBtn);
            }

            this.fluidEnableBtn = AE2FCRUCompat.createGuiFCImgButton(this.guiLeft + 74, this.guiTop + this.ySize - 153, "FLUID_FIRST", "FLUID");
            if (this.fluidEnableBtn != null) {
                this.fluidEnableBtn.setHalfSize(true);
                this.buttonList.add(this.fluidEnableBtn);
            }

            this.fluidDisableBtn = AE2FCRUCompat.createGuiFCImgButton(this.guiLeft + 74, this.guiTop + this.ySize - 153, "ORIGIN_ORDER", "ITEM");
            if (this.fluidDisableBtn != null) {
                this.fluidDisableBtn.setHalfSize(true);
                this.buttonList.add(this.fluidDisableBtn);
            }

            this.craftingFluidBtn = AE2FCRUCompat.createGuiFCImgButton(this.guiLeft + 110, this.guiTop + this.ySize - 115, "CRAFT_FLUID", "ENCODE");
            if (this.craftingFluidBtn != null) {
                this.buttonList.add(this.craftingFluidBtn);
            }
        }
    }

    private void updateScrollbarPosition() {
        // Input slots after repositionSlot: yPos = ySize - 155
        // Scrollbar left of input slots at x=5, moved up 3 pixels
        // Track at x=3 (left 1px from before), thumb at x=4 (right 1px from track)
        // Thumb scrollable range: top+1 to top+height-1 (exclude topmost and bottommost pixel of track)
        this.patternScrollbar.setLeft(4);
        this.patternScrollbar.setTop(this.ySize - 158 + 1);
        this.patternScrollbar.setHeight(54 - 2);
    }

    @Override
    public void drawFG(int offsetX, int offsetY, int mouseX, int mouseY) {
        if (this.container.isCraftingMode()) {
            this.tabCraftButton.visible = true;
            this.tabProcessButton.visible = false;

            if (this.container.isSubstitute()) {
                this.substitutionsEnabledBtn.visible = true;
                this.substitutionsDisabledBtn.visible = false;
            } else {
                this.substitutionsEnabledBtn.visible = false;
                this.substitutionsDisabledBtn.visible = true;
            }

            // Hide quantity buttons in crafting mode
            this.x2Btn.visible = false;
            this.x3Btn.visible = false;
            this.divTwoBtn.visible = false;
            this.divThreeBtn.visible = false;
            this.plusOneBtn.visible = false;
            this.minusOneBtn.visible = false;

            // AE2FCRU buttons in crafting mode
            if (this.combineEnableBtn != null) {
                this.combineEnableBtn.visible = false;
                this.combineDisableBtn.visible = false;
            }
            if (this.fluidEnableBtn != null) {
                if (this.container.fluidFirst) {
                    this.fluidEnableBtn.visible = true;
                    this.fluidDisableBtn.visible = false;
                } else {
                    this.fluidEnableBtn.visible = false;
                    this.fluidDisableBtn.visible = true;
                }
            }
            if (this.craftingFluidBtn != null) {
                this.craftingFluidBtn.visible = true;
            }
        } else {
            this.tabCraftButton.visible = false;
            this.tabProcessButton.visible = true;
            this.substitutionsEnabledBtn.visible = false;
            this.substitutionsDisabledBtn.visible = false;

            // Show quantity buttons in processing mode
            this.x2Btn.visible = true;
            this.x3Btn.visible = true;
            this.divTwoBtn.visible = true;
            this.divThreeBtn.visible = true;
            this.plusOneBtn.visible = true;
            this.minusOneBtn.visible = true;

            // AE2FCRU buttons in processing mode
            if (this.combineEnableBtn != null) {
                if (this.container.combine) {
                    this.combineEnableBtn.visible = true;
                    this.combineDisableBtn.visible = false;
                } else {
                    this.combineEnableBtn.visible = false;
                    this.combineDisableBtn.visible = true;
                }
            }
            if (this.fluidEnableBtn != null) {
                if (this.container.fluidFirst) {
                    this.fluidEnableBtn.visible = true;
                    this.fluidDisableBtn.visible = false;
                } else {
                    this.fluidEnableBtn.visible = false;
                    this.fluidDisableBtn.visible = true;
                }
            }
            if (this.craftingFluidBtn != null) {
                this.craftingFluidBtn.visible = false;
            }
        }

        super.drawFG(offsetX, offsetY, mouseX, mouseY);
        this.fontRenderer.drawString(GuiText.PatternTerminal.getLocal(), 8, this.ySize - 96 + 2 - 81, 4210752);

        // Show page number and draw scrollbar in processing mode
        if (!this.container.isCraftingMode()) {
            int page = this.container.getCurrentPage() + 1;
            this.fontRenderer.drawString(page + "/" + TilePatternTerminal.PAGE_COUNT, 80, this.ySize - 96 + 2 - 81, 4210752);

            if (this.patternScrollbar != null) {
                this.updateScrollbarPosition();
                // Draw scrollbar track at x=3 (1px left of thumb), full height including non-scrollable edges
                int trackX = this.patternScrollbar.getLeft() - 1;
                int trackY = this.patternScrollbar.getTop() - 1;
                int trackWidth = 14;
                int trackHeight = this.patternScrollbar.getHeight() + 2;
                this.bindTexture(this.getBackground());
                GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
                this.drawTexturedModalRect(trackX, trackY, 174, 17, trackWidth, trackHeight);
                // Draw scrollbar thumb at x=4 (setLeft)
                this.patternScrollbar.draw(this);
            }
        }

        // Draw amount input field overlay on top of everything
        // drawFG has GL translation of (guiLeft, guiTop) already applied,
        // so use relative coordinates
        if (this.amountFieldActive && this.amountField != null) {
            int fx = this.amountField.x - this.guiLeft;
            int fy = this.amountField.y - this.guiTop;
            // Disable depth test so the field renders on top of item icons
            GlStateManager.disableDepth();
            drawRect(fx - 2, fy - 2,
                    fx + this.amountField.getWidth() + 2,
                    fy + this.fontRenderer.FONT_HEIGHT + 2, 0xFF000000);
            this.amountField.x = fx;
            this.amountField.y = fy;
            this.amountField.drawTextBox();
            // Restore absolute coordinates for mouse input handling
            this.amountField.x = fx + this.guiLeft;
            this.amountField.y = fy + this.guiTop;
            GlStateManager.enableDepth();
        }
    }

    @Override
    public void drawBG(int offsetX, int offsetY, int mouseX, int mouseY) {
        super.drawBG(offsetX, offsetY, mouseX, mouseY);
    }

    @Override
    protected void actionPerformed(GuiButton btn) throws IOException {
        super.actionPerformed(btn);
        if (btn == this.tabCraftButton || btn == this.tabProcessButton) {
            NetworkHandler.instance().sendToServer(new PacketValueConfig("PatternTerminal.CraftMode",
                    this.tabProcessButton == btn ? "1" : "0"));
        } else if (btn == this.substitutionsEnabledBtn || btn == this.substitutionsDisabledBtn) {
            NetworkHandler.instance().sendToServer(new PacketValueConfig("PatternTerminal.Substitute",
                    this.container.isSubstitute() ? "0" : "1"));
        } else if (btn == this.encodeBtn) {
            if (isShiftKeyDown()) {
                NetworkHandler.instance().sendToServer(new PacketValueConfig("PatternTerminal.Encode", "2"));
            } else {
                NetworkHandler.instance().sendToServer(new PacketValueConfig("PatternTerminal.Encode", "1"));
            }
        } else if (btn == this.clearBtn) {
            NetworkHandler.instance().sendToServer(new PacketValueConfig("PatternTerminal.Clear", "1"));
        } else if (btn == this.x2Btn) {
            NetworkHandler.instance().sendToServer(new PacketValueConfig("PatternTerminal.MultiplyByTwo", "1"));
        } else if (btn == this.x3Btn) {
            NetworkHandler.instance().sendToServer(new PacketValueConfig("PatternTerminal.MultiplyByThree", "1"));
        } else if (btn == this.divTwoBtn) {
            NetworkHandler.instance().sendToServer(new PacketValueConfig("PatternTerminal.DivideByTwo", "1"));
        } else if (btn == this.divThreeBtn) {
            NetworkHandler.instance().sendToServer(new PacketValueConfig("PatternTerminal.DivideByThree", "1"));
        } else if (btn == this.plusOneBtn) {
            NetworkHandler.instance().sendToServer(new PacketValueConfig("PatternTerminal.IncreaseByOne", "1"));
        } else if (btn == this.minusOneBtn) {
            NetworkHandler.instance().sendToServer(new PacketValueConfig("PatternTerminal.DecreaseByOne", "1"));
        } else if (this.combineDisableBtn == btn || this.combineEnableBtn == btn) {
            AE2FCRUCompat.sendFluidPatternBtns("PatternTerminal.Combine", this.combineDisableBtn == btn ? "1" : "0");
        } else if (this.fluidDisableBtn == btn || this.fluidEnableBtn == btn) {
            AE2FCRUCompat.sendFluidPatternBtns("PatternTerminal.Fluid", this.fluidDisableBtn == btn ? "1" : "0");
        } else if (this.craftingFluidBtn == btn) {
            NetworkHandler.instance().sendToServer(new PacketValueConfig("PatternTerminal.FluidCraft", "0"));
        }
    }

    private void syncPageFromScrollbar() {
        int newPage = this.patternScrollbar.getCurrentScroll();
        if (newPage != this.container.getCurrentPage()) {
            this.container.setCurrentPage(newPage);
            try {
                NetworkHandler.instance().sendToServer(new PacketValueConfig("PatternTerminal.Page", String.valueOf(newPage)));
            } catch (IOException ignored) {
            }
        }
    }

    @Override
    protected void mouseClicked(int xCoord, int yCoord, int btn) throws IOException {
        // If amount field is active, check if click is inside it
        if (this.amountFieldActive && this.amountField != null) {
            int fx = this.amountField.x;
            int fy = this.amountField.y;
            int fw = this.amountField.getWidth();
            int fh = this.fontRenderer.FONT_HEIGHT;
            if (xCoord >= fx && xCoord <= fx + fw && yCoord >= fy && yCoord <= fy + fh) {
                // Click inside the field, let it handle focus
                this.amountField.mouseClicked(xCoord, yCoord, btn);
                return;
            } else {
                // Click outside, confirm and close
                this.confirmAmountField();
            }
        }

        super.mouseClicked(xCoord, yCoord, btn);

        // Check scrollbar click/drag in processing mode
        if (!this.container.isCraftingMode() && this.patternScrollbar != null) {
            this.updateScrollbarPosition();
            this.patternScrollbar.click(this, xCoord - this.guiLeft, yCoord - this.guiTop);
            this.syncPageFromScrollbar();
        }
    }

    @Override
    protected void mouseClickMove(int xCoord, int yCoord, int clickedMouseButton, long timeSinceLastClick) {
        super.mouseClickMove(xCoord, yCoord, clickedMouseButton, timeSinceLastClick);

        // Handle scrollbar drag
        if (!this.container.isCraftingMode() && this.patternScrollbar != null) {
            this.updateScrollbarPosition();
            this.patternScrollbar.click(this, xCoord - this.guiLeft, yCoord - this.guiTop);
            this.syncPageFromScrollbar();
        }
    }

    @Override
    protected String getBackground() {
        if (this.container.isCraftingMode()) {
            return "guis/pattern.png";
        }
        return "guis/pattern2.png";
    }

    @Override
    protected void handleMouseClick(Slot slot, int slotIdx, int mouseButton, ClickType clickType) {
        if (slot instanceof SlotFake && mouseButton == 2) {
            // Middle-click on SlotFake: open quantity input
            ItemStack stack = slot.getStack();
            if (!stack.isEmpty()) {
                this.amountTargetSlot = (SlotFake) slot;
                this.amountFieldActive = true;
                // Position the text field near the slot
                this.amountField.x = this.guiLeft + slot.xPos;
                this.amountField.y = this.guiTop + slot.yPos - this.fontRenderer.FONT_HEIGHT - 4;
                this.amountField.setText(String.valueOf(stack.getCount()));
                this.amountField.setVisible(true);
                this.amountField.setFocused(true);
                this.amountField.setSelectionPos(0);
            }
            return;
        }
        super.handleMouseClick(slot, slotIdx, mouseButton, clickType);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (this.amountFieldActive && this.amountField != null) {
            if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
                // Confirm: send the new count to server
                this.confirmAmountField();
                return;
            } else if (keyCode == Keyboard.KEY_ESCAPE) {
                // Cancel
                this.closeAmountField();
                return;
            }
            if (this.amountField.textboxKeyTyped(typedChar, keyCode)) {
                return;
            }
        }
        super.keyTyped(typedChar, keyCode);
    }

    private void confirmAmountField() {
        if (this.amountTargetSlot != null && this.amountField != null) {
            try {
                int newCount = Integer.parseInt(this.amountField.getText());
                if (newCount >= 1) {
                    NetworkHandler.instance().sendToServer(new PacketValueConfig(
                            "PatternTerminal.SetSlotCount",
                            this.amountTargetSlot.slotNumber + ":" + newCount));
                }
            } catch (NumberFormatException ignored) {
            } catch (IOException ignored) {
            }
        }
        this.closeAmountField();
    }

    private void closeAmountField() {
        this.amountFieldActive = false;
        this.amountTargetSlot = null;
        if (this.amountField != null) {
            this.amountField.setVisible(false);
            this.amountField.setFocused(false);
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        // Check if mouse is over a SlotFake with items and there's a scroll event
        int dwheel = Mouse.getEventDWheel();
        if (dwheel != 0 && !this.amountFieldActive) {
            int x = Mouse.getEventX() * this.width / this.mc.displayWidth;
            int y = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;
            Slot slot = this.getSlot(x, y);
            if (slot instanceof SlotFake && !slot.getStack().isEmpty()) {
                // Handle scroll on SlotFake directly (no Shift required)
                InventoryAction action;
                if (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL)) {
                    action = dwheel > 0 ? InventoryAction.DOUBLE : InventoryAction.HALVE;
                } else {
                    action = dwheel > 0 ? InventoryAction.PLACE_SINGLE : InventoryAction.PICKUP_SINGLE;
                }
                PacketInventoryAction p = new PacketInventoryAction(action, slot.slotNumber, 0);
                NetworkHandler.instance().sendToServer(p);
                return;
            }
        }
        super.handleMouseInput();
    }

    // AE2FCRU: Show tooltip when holding fluid container over SlotFake in processing mode
    @Override
    protected void renderHoveredToolTip(int mouseX, int mouseY) {
        Slot slot = this.getSlotUnderMouse();
        if (!this.container.isCraftingMode() && slot instanceof SlotFake) {
            if (!AE2FCRUCompat.isLoaded()) {
                super.renderHoveredToolTip(mouseX, mouseY);
                return;
            }
            ItemStack heldItem = this.mc.player.inventory.getItemStack();
            if (!heldItem.isEmpty() && heldItem.hasCapability(
                    net.minecraftforge.fluids.capability.CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null)) {
                net.minecraftforge.fluids.FluidStack fluid = AE2FCRUCompat.getFluidFromItem(heldItem);
                if (fluid != null) {
                    java.util.List<String> tips = new java.util.ArrayList<>();
                    tips.add(net.minecraft.client.resources.I18n.format("ae2fc.tooltip.fluid_pattern.tooltip",
                            net.minecraft.client.settings.GameSettings.getKeyDisplayString(-100),
                            fluid.getLocalizedName()));
                    tips.add(net.minecraft.client.resources.I18n.format("ae2fc.tooltip.fluid_pattern.tooltip",
                            net.minecraft.client.settings.GameSettings.getKeyDisplayString(-99),
                            heldItem.getDisplayName()));
                    this.drawHoveringText(tips, mouseX, mouseY);
                    return;
                }
            }
        }
        super.renderHoveredToolTip(mouseX, mouseY);
    }

    // IJEIGhostIngredients implementation
    @Override
    public java.util.List<IGhostIngredientHandler.Target<?>> getPhantomTargets(Object ingredient) {
        java.util.List<IGhostIngredientHandler.Target<?>> targets = new ArrayList<>();

        // Handle ItemStack ingredients (normal items)
        if (ingredient instanceof ItemStack) {
            ItemStack itemStack = (ItemStack) ingredient;
            for (Slot slot : this.inventorySlots.inventorySlots) {
                if (slot instanceof SlotFake) {
                    IGhostIngredientHandler.Target<Object> target = new IGhostIngredientHandler.Target<Object>() {
                        @Override
                        public Rectangle getArea() {
                            return new Rectangle(getGuiLeft() + slot.xPos, getGuiTop() + slot.yPos, 16, 16);
                        }

                        @Override
                        public void accept(Object ingredient) {
                            try {
                                PacketInventoryAction p = new PacketInventoryAction(
                                        InventoryAction.PLACE_JEI_GHOST_ITEM,
                                        (SlotFake) slot,
                                        AEItemStack.fromItemStack(itemStack));
                                NetworkHandler.instance().sendToServer(p);
                            } catch (IOException e) {
                                AE2Utilix.LOGGER.error("Failed to send ghost item packet", e);
                            }
                        }
                    };
                    targets.add(target);
                    mapTargetSlot.putIfAbsent(target, slot);
                }
            }
        }
        // Handle FluidStack ingredients from JEI (AE2FCRU support)
        else if (ingredient instanceof net.minecraftforge.fluids.FluidStack) {
            if (!AE2FCRUCompat.isLoaded()) return targets;
            net.minecraftforge.fluids.FluidStack fluidStack = (net.minecraftforge.fluids.FluidStack) ingredient;
            // In processing mode, convert fluid to fake fluid item; in crafting mode, convert to filled bucket
            if (!this.container.isCraftingMode()) {
                // Processing mode: use AE2FCRUCompat to create virtual fluid item
                ItemStack fakeFluidItem = AE2FCRUCompat.packFluid2Drops(fluidStack.copy());
                if (fakeFluidItem == null) return targets;
                for (Slot slot : this.inventorySlots.inventorySlots) {
                    if (slot instanceof SlotFake) {
                        IGhostIngredientHandler.Target<Object> target = new IGhostIngredientHandler.Target<Object>() {
                            @Override
                            public Rectangle getArea() {
                                return new Rectangle(getGuiLeft() + slot.xPos, getGuiTop() + slot.yPos, 16, 16);
                            }

                            @Override
                            public void accept(Object ingredient) {
                                try {
                                    PacketInventoryAction p = new PacketInventoryAction(
                                            InventoryAction.PLACE_JEI_GHOST_ITEM,
                                            (SlotFake) slot,
                                            AEItemStack.fromItemStack(fakeFluidItem));
                                    NetworkHandler.instance().sendToServer(p);
                                } catch (IOException e) {
                                    AE2Utilix.LOGGER.error("Failed to send ghost fluid packet", e);
                                }
                            }
                        };
                        targets.add(target);
                        mapTargetSlot.putIfAbsent(target, slot);
                    }
                }
            } else {
                // Crafting mode: convert to filled bucket
                ItemStack bucketItem = net.minecraftforge.fluids.FluidUtil.getFilledBucket(fluidStack);
                if (!bucketItem.isEmpty()) {
                    for (Slot slot : this.inventorySlots.inventorySlots) {
                        if (slot instanceof SlotFake) {
                            ItemStack finalBucketItem = bucketItem;
                            IGhostIngredientHandler.Target<Object> target = new IGhostIngredientHandler.Target<Object>() {
                                @Override
                                public Rectangle getArea() {
                                    return new Rectangle(getGuiLeft() + slot.xPos, getGuiTop() + slot.yPos, 16, 16);
                                }

                                @Override
                                public void accept(Object ingredient) {
                                    try {
                                        PacketInventoryAction p = new PacketInventoryAction(
                                                InventoryAction.PLACE_JEI_GHOST_ITEM,
                                                (SlotFake) slot,
                                                AEItemStack.fromItemStack(finalBucketItem));
                                        NetworkHandler.instance().sendToServer(p);
                                    } catch (IOException e) {
                                        AE2Utilix.LOGGER.error("Failed to send ghost bucket packet", e);
                                    }
                                }
                            };
                            targets.add(target);
                            mapTargetSlot.putIfAbsent(target, slot);
                        }
                    }
                }
            }
        }
        return targets;
    }

    @Override
    public Map<IGhostIngredientHandler.Target<?>, Object> getFakeSlotTargetMap() {
        return this.mapTargetSlot;
    }
}
