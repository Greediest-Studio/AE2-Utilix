package com.ae2utilix.gui;

import appeng.api.config.ActionItems;
import appeng.api.config.Settings;
import appeng.api.config.TerminalStyle;
import appeng.client.gui.AEBaseGui;
import appeng.client.gui.widgets.GuiImgButton;
import appeng.client.gui.widgets.GuiScrollbar;
import appeng.client.gui.widgets.ISortSource;
import appeng.client.gui.widgets.MEGuiTooltipTextField;
import appeng.container.slot.AppEngSlot;
import appeng.core.AEConfig;
import appeng.core.localization.ButtonToolTips;
import appeng.core.localization.GuiText;
import com.ae2utilix.block.terminal.TileInterfaceTerminal;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@SideOnly(Side.CLIENT)
public class GuiFullInterface extends AEBaseGui implements ISortSource {

    private static final int OFFSET_X = 21;
    private static final int MAGIC_HEIGHT_NUMBER = 151;
    private static final int MAX_LINES = 16;
    private static final int COLS = 9;

    private final ContainerFullInterface container;
    private final GuiScrollbar scrollbar = new GuiScrollbar();
    private int rows = 0;
    private int maxLines = MAX_LINES;

    private final List<Object> lines = new ArrayList<>();
    private final Map<String, List<ContainerFullInterface.InvTracker>> byName = new LinkedHashMap<>();
    private final Map<ContainerFullInterface.InvTracker, Integer> numUpgradesMap = new HashMap<>();
    private final Map<GuiButton, ContainerFullInterface.InvTracker> guiButtonHashMap = new HashMap<>();
    private final Map<ContainerFullInterface.InvTracker, BlockPos> blockPosHashMap = new HashMap<>();
    private final Map<ContainerFullInterface.InvTracker, Integer> dimHashMap = new HashMap<>();

    private GuiImgButton terminalStyleBox;
    private GuiImgButton guiButtonBrokenRecipes;
    private GuiImgButton guiButtonHideFull;
    private GuiImgButton guiButtonAssemblersOnly;

    private MEGuiTooltipTextField searchFieldInputs;
    private MEGuiTooltipTextField searchFieldOutputs;
    private MEGuiTooltipTextField searchFieldNames;

    private boolean onlyShowWithSpace = false;
    private boolean onlyMolecularAssemblers = false;
    private boolean onlyBrokenRecipes = false;

    public GuiFullInterface(InventoryPlayer inventoryPlayer, TileInterfaceTerminal te) {
        super(new ContainerFullInterface(inventoryPlayer, te));
        this.container = (ContainerFullInterface) this.inventorySlots;
        this.xSize = 208;
        this.ySize = 255;

        this.searchFieldInputs = new MEGuiTooltipTextField(86, 12, ButtonToolTips.SearchFieldInputs.getLocal());
        this.searchFieldInputs.setEnableBackgroundDrawing(false);
        this.searchFieldOutputs = new MEGuiTooltipTextField(86, 12, ButtonToolTips.SearchFieldOutputs.getLocal());
        this.searchFieldOutputs.setEnableBackgroundDrawing(false);
        this.searchFieldNames = new MEGuiTooltipTextField(71, 12, ButtonToolTips.SearchFieldNames.getLocal());
        this.searchFieldNames.setEnableBackgroundDrawing(false);
        this.searchFieldNames.setFocused(true);

        this.guiButtonAssemblersOnly = new GuiImgButton(0, 0, Settings.ACTIONS, null);
        this.guiButtonHideFull = new GuiImgButton(0, 0, Settings.ACTIONS, null);
        this.guiButtonBrokenRecipes = new GuiImgButton(0, 0, Settings.ACTIONS, null);
        this.terminalStyleBox = new GuiImgButton(0, 0, Settings.TERMINAL_STYLE, null);
    }

    @Override
    public void initGui() {
        final int extraSpace = this.height - MAGIC_HEIGHT_NUMBER;
        this.rows = extraSpace / 18;
        if (this.rows > this.maxLines) this.rows = this.maxLines;
        if (this.rows < 6) this.rows = 6;

        this.ySize = MAGIC_HEIGHT_NUMBER + this.rows * 18;
        super.initGui();

        final int unusedSpace = this.height - this.ySize;
        this.guiTop = (int) Math.floor(unusedSpace / (unusedSpace < 0 ? 3.8f : 2.0f));

        for (final Slot s : this.inventorySlots.inventorySlots) {
            if (s instanceof AppEngSlot && !(s instanceof SlotInterface)) {
                final AppEngSlot slot = (AppEngSlot) s;
                slot.yPos = slot.getY() + this.ySize - 78 - 7;
                slot.xPos = slot.getX() + 14;
            }
        }

        this.scrollbar.setTop(52);
        this.scrollbar.setLeft(189);
        this.scrollbar.setHeight(this.rows * 18 - 2);
        this.scrollbar.setRange(0, 0, 1);
        this.setScrollBar(this.scrollbar);

        int offset = this.guiTop + 8;
        this.terminalStyleBox.x = this.guiLeft - 18;
        this.terminalStyleBox.y = offset;
        this.terminalStyleBox.set(AEConfig.instance().getConfigManager().getSetting(Settings.TERMINAL_STYLE));
        offset += 20;
        this.guiButtonBrokenRecipes.x = this.guiLeft - 18;
        this.guiButtonBrokenRecipes.y = offset;
        this.guiButtonBrokenRecipes.set(ActionItems.TOGGLE_SHOW_ONLY_INVALID_PATTERNS_OFF);
        offset += 20;
        this.guiButtonHideFull.x = this.guiLeft - 18;
        this.guiButtonHideFull.y = offset;
        this.guiButtonHideFull.set(ActionItems.TOGGLE_SHOW_FULL_INTERFACES_ON);
        offset += 20;
        this.guiButtonAssemblersOnly.x = this.guiLeft - 18;
        this.guiButtonAssemblersOnly.y = offset;
        this.guiButtonAssemblersOnly.set(ActionItems.MOLECULAR_ASSEMBLERS_OFF);

        this.buttonList.add(this.terminalStyleBox);
        this.buttonList.add(this.guiButtonBrokenRecipes);
        this.buttonList.add(this.guiButtonHideFull);
        this.buttonList.add(this.guiButtonAssemblersOnly);

        this.searchFieldInputs.x = this.guiLeft + 32;
        this.searchFieldInputs.y = this.guiTop + 25;
        this.searchFieldOutputs.x = this.guiLeft + 32;
        this.searchFieldOutputs.y = this.guiTop + 38;
        this.searchFieldNames.x = this.guiLeft + 32 + 99;
        this.searchFieldNames.y = this.guiTop + 38;
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        this.drawBG(this.guiLeft, this.guiTop, mouseX, mouseY);
    }

    @Override
    public void drawFG(int offsetX, int offsetY, int mouseX, int mouseY) {
        this.fontRenderer.drawString(GuiText.InterfaceTerminal.getLocal(), OFFSET_X + 2, 6, 4210752);
        this.fontRenderer.drawString(GuiText.inventory.getLocal(), OFFSET_X + 2, this.ySize - 96, 4210752);

        final int currentScroll = this.getScrollBar().getCurrentScroll();
        int offset = 51;
        int linesDraw = 0;

        for (int x = 0; x < this.lines.size() && linesDraw < this.rows && currentScroll + x < this.lines.size(); x++) {
            final Object lineObj = this.lines.get(currentScroll + x);
            if (lineObj instanceof ContainerFullInterface.InvTracker) {
                final int extraLines = numUpgradesMap.getOrDefault(lineObj, 0);
                for (int row = 0; row < 1 + extraLines && linesDraw < this.rows; row++) {
                    linesDraw++;
                    offset += 18;
                }
            } else if (lineObj instanceof String) {
                String name = (String) lineObj;
                final int count = byName.getOrDefault(name, Collections.emptyList()).size();
                if (count > 1) {
                    name = name + " (" + count + ")";
                }
                while (name.length() > 2 && this.fontRenderer.getStringWidth(name) > 158) {
                    name = name.substring(0, name.length() - 1);
                }
                this.fontRenderer.drawString(name, OFFSET_X + 3, 6 + offset, 4210752);
                linesDraw++;
                offset += 18;
            }
        }
    }

    @Override
    public void drawBG(int offsetX, int offsetY, int mouseX, int mouseY) {
        this.bindTexture("guis/newinterfaceterminal.png");
        this.drawTexturedModalRect(offsetX, offsetY, 0, 0, this.xSize, 53);

        for (int x = 0; x < this.rows; x++) {
            this.drawTexturedModalRect(offsetX, offsetY + 53 + x * 18, 0, 52, this.xSize, 18);
        }

        int offset = 51;
        final int currentScroll = this.getScrollBar().getCurrentScroll();
        int linesDraw = 0;

        for (int x = 0; x < this.lines.size() && linesDraw < this.rows && currentScroll + x < this.lines.size(); x++) {
            final Object lineObj = this.lines.get(currentScroll + x);
            if (lineObj instanceof ContainerFullInterface.InvTracker) {
                GlStateManager.color(1, 1, 1, 1);
                final int extraLines = numUpgradesMap.getOrDefault(lineObj, 0);
                for (int row = 0; row < 1 + extraLines && linesDraw < this.rows; row++) {
                    this.drawTexturedModalRect(offsetX + 20, offsetY + offset, 20, 173, COLS * 18, 18);
                    linesDraw++;
                    offset += 18;
                }
            } else {
                linesDraw++;
                offset += 18;
            }
        }

        this.drawTexturedModalRect(offsetX, offsetY + 50 + this.rows * 18, 0, 158, this.xSize, 99);
        this.searchFieldInputs.drawTextBox();
        this.searchFieldOutputs.drawTextBox();
        this.searchFieldNames.drawTextBox();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.terminalStyleBox.set(AEConfig.instance().getConfigManager().getSetting(Settings.TERMINAL_STYLE));
        this.guiButtonAssemblersOnly.set(this.onlyMolecularAssemblers ? ActionItems.MOLECULAR_ASSEMBLERS_ON : ActionItems.MOLECULAR_ASSEMBLERS_OFF);
        this.guiButtonHideFull.set(this.onlyShowWithSpace ? ActionItems.TOGGLE_SHOW_FULL_INTERFACES_OFF : ActionItems.TOGGLE_SHOW_FULL_INTERFACES_ON);
        this.guiButtonBrokenRecipes.set(this.onlyBrokenRecipes ? ActionItems.TOGGLE_SHOW_ONLY_INVALID_PATTERNS_ON : ActionItems.TOGGLE_SHOW_ONLY_INVALID_PATTERNS_OFF);

        // Rebuild dynamic buttons and slots each frame
        this.buttonList.removeIf(b -> !(b instanceof GuiImgButton) || this.guiButtonHashMap.containsKey(b));
        this.guiButtonHashMap.clear();
        this.inventorySlots.inventorySlots.removeIf(slot -> slot instanceof SlotInterface);

        this.buttonList.add(this.guiButtonAssemblersOnly);
        this.buttonList.add(this.guiButtonHideFull);
        this.buttonList.add(this.guiButtonBrokenRecipes);
        this.buttonList.add(this.terminalStyleBox);

        int offset = 51;
        final int currentScroll = this.getScrollBar().getCurrentScroll();
        int linesDraw = 0;

        for (int x = 0; x < this.lines.size() && linesDraw < this.rows && currentScroll + x < this.lines.size(); x++) {
            final Object lineObj = this.lines.get(currentScroll + x);
            if (lineObj instanceof ContainerFullInterface.InvTracker) {
                final ContainerFullInterface.InvTracker inv = (ContainerFullInterface.InvTracker) lineObj;

                GuiButton guiButton = new GuiImgButton(this.guiLeft + 4, this.guiTop + offset + 1, Settings.ACTIONS, ActionItems.HIGHLIGHT_INTERFACE);
                this.guiButtonHashMap.put(guiButton, inv);
                this.buttonList.add(guiButton);

                final int extraLines = numUpgradesMap.getOrDefault(inv, 0);
                for (int row = 0; row < 1 + extraLines && linesDraw < this.rows; row++) {
                    for (int z = 0; z < COLS; z++) {
                        this.inventorySlots.inventorySlots.add(
                                new SlotInterface(inv, z + (row * COLS), z * 18 + 22, 1 + offset));
                    }
                    linesDraw++;
                    offset += 18;
                }
            } else {
                linesDraw++;
                offset += 18;
            }
        }

        for (Slot slot : this.inventorySlots.inventorySlots) {
            if (slot instanceof SlotInterface) {
                ((AppEngSlot) slot).setIsValid(AppEngSlot.hasCalculatedValidness.Valid);
            }
        }

        super.drawScreen(mouseX, mouseY, partialTicks);

        this.drawTooltip(searchFieldInputs, mouseX, mouseY);
        this.drawTooltip(searchFieldOutputs, mouseX, mouseY);
        this.drawTooltip(searchFieldNames, mouseX, mouseY);
    }

    public void postUpdate(NBTTagCompound data) {
        this.container.postUpdate(data);
        this.refreshList();
    }

    private void refreshList() {
        this.byName.clear();
        this.numUpgradesMap.clear();
        this.blockPosHashMap.clear();
        this.dimHashMap.clear();

        for (final ContainerFullInterface.InvTracker inv : this.container.getClientLinked()) {
            final String name = inv.getName();
            this.byName.computeIfAbsent(name, k -> new ArrayList<>()).add(inv);
            this.numUpgradesMap.put(inv, inv.numUpgrades);
            this.blockPosHashMap.put(inv, inv.pos);
            this.dimHashMap.put(inv, inv.dim);
        }

        this.lines.clear();
        for (final Map.Entry<String, List<ContainerFullInterface.InvTracker>> entry : this.byName.entrySet()) {
            this.lines.add(entry.getKey());
            this.lines.addAll(entry.getValue());
        }

        this.setScrollBar();
    }

    private void setScrollBar() {
        this.getScrollBar().setTop(52).setLeft(189).setHeight(this.rows * 18 - 2);
        this.getScrollBar().setRange(0, this.lines.size() - 1, 1);
    }

    @Override
    protected void mouseClicked(int xCoord, int yCoord, int btn) throws IOException {
        this.searchFieldInputs.mouseClicked(xCoord, yCoord, btn);
        this.searchFieldOutputs.mouseClicked(xCoord, yCoord, btn);
        this.searchFieldNames.mouseClicked(xCoord, yCoord, btn);
        super.mouseClicked(xCoord, yCoord, btn);
    }

    @Override
    protected void actionPerformed(GuiButton btn) throws IOException {
        super.actionPerformed(btn);

        if (this.guiButtonHashMap.containsKey(btn)) {
            final ContainerFullInterface.InvTracker inv = this.guiButtonHashMap.get(btn);
            final BlockPos blockPos = this.blockPosHashMap.get(inv);
            final int interfaceDim = this.dimHashMap.getOrDefault(inv, 0);
            final int playerDim = this.mc.world.provider.getDimension();

            if (blockPos != null && blockPos != BlockPos.ORIGIN) {
                if (playerDim != interfaceDim) {
                    this.mc.player.sendStatusMessage(
                            new net.minecraft.util.text.TextComponentString(
                                    net.minecraft.client.resources.I18n.format("ae2_utilix.gui.interface_other_dim")), false);
                } else {
                    final BlockPos playerPos = this.mc.player.getPosition();
                    appeng.client.render.BlockPosHighlighter.hilightBlock(
                            blockPos, System.currentTimeMillis() + 500 * Math.max(1, (int) blockPos.distanceSq(playerPos)), playerDim);
                    this.mc.player.sendStatusMessage(
                            new net.minecraft.util.text.TextComponentString(
                                    net.minecraft.client.resources.I18n.format("ae2_utilix.gui.interface_highlight",
                                            blockPos.getX(), blockPos.getY(), blockPos.getZ())), false);
                }
                this.mc.player.closeScreen();
            }
        } else if (btn == this.terminalStyleBox) {
            final TerminalStyle current = (TerminalStyle) AEConfig.instance().getConfigManager().getSetting(Settings.TERMINAL_STYLE);
            final TerminalStyle[] values = TerminalStyle.values();
            final TerminalStyle next = values[(current.ordinal() + 1) % values.length];
            AEConfig.instance().getConfigManager().putSetting(Settings.TERMINAL_STYLE, next);
            this.terminalStyleBox.set(next);
        } else if (btn == this.guiButtonBrokenRecipes) {
            this.onlyBrokenRecipes = !this.onlyBrokenRecipes;
        } else if (btn == this.guiButtonHideFull) {
            this.onlyShowWithSpace = !this.onlyShowWithSpace;
        } else if (btn == this.guiButtonAssemblersOnly) {
            this.onlyMolecularAssemblers = !this.onlyMolecularAssemblers;
        }
    }

    @Override
    protected void keyTyped(char character, int key) throws IOException {
        if (character == '\t') {
            if (this.searchFieldInputs.isFocused()) {
                this.searchFieldInputs.setFocused(false);
                if (isShiftKeyDown()) this.searchFieldNames.setFocused(true);
                else this.searchFieldOutputs.setFocused(true);
                return;
            } else if (this.searchFieldOutputs.isFocused()) {
                this.searchFieldOutputs.setFocused(false);
                if (isShiftKeyDown()) this.searchFieldInputs.setFocused(true);
                else this.searchFieldNames.setFocused(true);
                return;
            } else if (this.searchFieldNames.isFocused()) {
                this.searchFieldNames.setFocused(false);
                if (isShiftKeyDown()) this.searchFieldOutputs.setFocused(true);
                else this.searchFieldInputs.setFocused(true);
                return;
            }
        }

        if (this.searchFieldInputs.textboxKeyTyped(character, key)
                || this.searchFieldOutputs.textboxKeyTyped(character, key)
                || this.searchFieldNames.textboxKeyTyped(character, key)) {
            this.refreshList();
        } else {
            super.keyTyped(character, key);
        }
    }

    @Override
    public appeng.api.config.SortDir getSortDir() {
        return appeng.api.config.SortDir.ASCENDING;
    }

    @Override
    public appeng.api.config.SortOrder getSortBy() {
        return appeng.api.config.SortOrder.NAME;
    }

    @Override
    public appeng.api.config.ViewItems getSortDisplay() {
        return appeng.api.config.ViewItems.ALL;
    }
}
