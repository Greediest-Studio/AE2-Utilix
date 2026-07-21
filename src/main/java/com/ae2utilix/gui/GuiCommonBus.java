package com.ae2utilix.gui;

import appeng.client.gui.AEBaseGui;
import com.ae2utilix.AE2Utilix;
import com.ae2utilix.integration.BotaniaFluxIntegration;
import com.ae2utilix.integration.MekanismEnergisticsIntegration;
import com.ae2utilix.item.ItemFluidMark;
import com.ae2utilix.network.PacketCommonBusMark;
import com.ae2utilix.parts.PartCommonBus;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.FluidUtil;
import org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GuiCommonBus extends AEBaseGui {
    private static final ResourceLocation BACKGROUND =
            new ResourceLocation(AE2Utilix.MODID, "textures/guis/storagebus.png");

    private final ContainerCommonBus container;
    private final Set<Integer> markedSlots = new HashSet<>();
    private boolean marking;

    public GuiCommonBus(InventoryPlayer inventory, PartCommonBus bus) {
        super(new ContainerCommonBus(inventory, bus));
        this.container = (ContainerCommonBus) this.inventorySlots;
        this.xSize = 176;
        this.ySize = 251;
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
        String key = this.container.getBus() instanceof com.ae2utilix.parts.PartCommonExportBus
                ? "tile.ae2_utilix.common_export_bus.name"
                : "tile.ae2_utilix.common_import_bus.name";
        this.fontRenderer.drawString(I18n.format(key), 8, 6, 4210752);
        this.fontRenderer.drawString(I18n.format("container.inventory"), 8, this.ySize - 96 + 3, 4210752);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        this.markedSlots.clear();
        this.marking = false;
        if (mouseButton == 1) {
            Slot slot = this.getSlotUnderMouse();
            if (this.isConfigSlot(slot) && this.sendVirtualMark(slot)) {
                this.markedSlots.add(slot.slotNumber);
                this.marking = true;
                return;
            }
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int mouseButton, long timeSinceClick) {
        if (mouseButton == 1 && this.marking) {
            Slot slot = this.getSlot(mouseX, mouseY);
            if (this.isConfigSlot(slot) && !this.markedSlots.contains(slot.slotNumber)
                    && this.sendVirtualMark(slot)) {
                this.markedSlots.add(slot.slotNumber);
            }
            return;
        }
        super.mouseClickMove(mouseX, mouseY, mouseButton, timeSinceClick);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        boolean suppress = state == 1 && this.marking;
        this.markedSlots.clear();
        this.marking = false;
        if (suppress) return;
        super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    protected void renderHoveredToolTip(int mouseX, int mouseY) {
        Slot slot = this.getSlotUnderMouse();
        if (this.isConfigSlot(slot) && !slot.getStack().isEmpty()) {
            java.util.ArrayList<String> tooltip = new java.util.ArrayList<>();
            ItemStack marker = slot.getStack();
            if (ItemFluidMark.isFluidMark(marker)) {
                net.minecraftforge.fluids.FluidStack fluid = ItemFluidMark.getFluid(marker);
                tooltip.add(I18n.format("ae2_utilix.common_interface.marked_fluid", fluid.getLocalizedName()));
            } else if (ItemFluidMark.isGasMark(marker)) {
                String gas = ItemFluidMark.getGasName(marker);
                String name = MekanismEnergisticsIntegration.getGasDisplayName(gas);
                tooltip.add(I18n.format("ae2_utilix.common_interface.marked_gas", name == null ? gas : name));
            } else if (ItemFluidMark.isManaMark(marker) || ItemFluidMark.isFeMark(marker)) {
                tooltip.add(I18n.format("ae2_utilix.common_interface.marked_item", marker.getDisplayName()));
            } else {
                tooltip.add(I18n.format("ae2_utilix.common_interface.marked_item", marker.getDisplayName()));
            }
            ItemStack held = this.mc.player.inventory.getItemStack();
            if (!held.isEmpty()) {
                net.minecraftforge.fluids.FluidStack heldFluid = FluidUtil.getFluidContained(held);
                if (heldFluid == null) heldFluid = ItemFluidMark.getFluid(held);
                if (heldFluid != null) {
                    tooltip.add(I18n.format("ae2_utilix.common_interface.left_click_item", held.getDisplayName()));
                    tooltip.add(I18n.format("ae2_utilix.common_interface.right_click_fluid", heldFluid.getLocalizedName()));
                } else {
                    String gas = MekanismEnergisticsIntegration.getGasNameFromItem(held);
                    if (gas != null) {
                        String name = MekanismEnergisticsIntegration.getGasDisplayName(gas);
                        tooltip.add(I18n.format("ae2_utilix.common_interface.left_click_item", held.getDisplayName()));
                        tooltip.add(I18n.format("ae2_utilix.common_interface.right_click_gas", name == null ? gas : name));
                    } else {
                        int type = BotaniaFluxIntegration.getMarkedType(held);
                        if (type != 0) {
                            tooltip.add(I18n.format("ae2_utilix.common_interface.left_click_item", held.getDisplayName()));
                            tooltip.add(I18n.format("ae2_utilix.common_interface.right_click_special",
                                    BotaniaFluxIntegration.getDisplayName(type)));
                        } else {
                            tooltip.add(I18n.format("ae2_utilix.common_interface.left_click_item", held.getDisplayName()));
                        }
                    }
                }
            }
            this.drawHoveringText(tooltip, mouseX, mouseY);
            return;
        }
        super.renderHoveredToolTip(mouseX, mouseY);
    }

    private boolean isConfigSlot(Slot slot) {
        return slot != null && slot.slotNumber >= 0 && slot.slotNumber < PartCommonBus.CONFIG_SLOTS;
    }

    private boolean sendVirtualMark(Slot slot) {
        ItemStack held = this.mc.player.inventory.getItemStack();
        if (held.isEmpty()) return false;

        EnumFacing side = this.container.getSide();
        net.minecraftforge.fluids.FluidStack fluid = FluidUtil.getFluidContained(held);
        if (fluid == null) fluid = ItemFluidMark.getFluid(held);
        if (fluid == null && held.getItem() == net.minecraft.init.Items.WATER_BUCKET) {
            fluid = new net.minecraftforge.fluids.FluidStack(net.minecraftforge.fluids.FluidRegistry.WATER, 1000);
        }
        if (fluid != null) {
            AE2Utilix.NETWORK.sendToServer(new PacketCommonBusMark(
                    this.container.getTilePosition(), side, slot.slotNumber, fluid));
            return true;
        }

        String gas = MekanismEnergisticsIntegration.getGasNameFromItem(held);
        if (gas != null) {
            AE2Utilix.NETWORK.sendToServer(new PacketCommonBusMark(
                    this.container.getTilePosition(), side, slot.slotNumber, gas));
            return true;
        }

        int special = BotaniaFluxIntegration.getMarkedType(held);
        if (special == 0) return false;
        AE2Utilix.NETWORK.sendToServer(new PacketCommonBusMark(
                this.container.getTilePosition(), side, slot.slotNumber, special));
        return true;
    }
}
