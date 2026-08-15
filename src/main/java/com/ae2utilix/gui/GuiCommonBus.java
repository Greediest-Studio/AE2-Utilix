package com.ae2utilix.gui;

import appeng.client.gui.AEBaseGui;
import appeng.container.interfaces.IJEIGhostIngredients;
import com.ae2utilix.AE2Utilix;
import com.ae2utilix.integration.BotaniaFluxIntegration;
import com.ae2utilix.integration.MekanismEnergisticsIntegration;
import com.ae2utilix.item.ItemFluidMark;
import com.ae2utilix.network.PacketCommonBusMark;
import com.ae2utilix.parts.PartCommonBus;
import com.ae2utilix.integration.jei.VirtualMarkJeiHelper;
import mezz.jei.api.gui.IGhostIngredientHandler;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.FluidUtil;
import org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GuiCommonBus extends AEBaseGui implements IJEIGhostIngredients {
    private static final ResourceLocation BACKGROUND =
            new ResourceLocation(AE2Utilix.MODID, "textures/guis/storagebus.png");
    private static final ResourceLocation UPGRADE_BACKGROUND =
            new ResourceLocation(AE2Utilix.MODID, "textures/guis/common_interface.png");

    private final ContainerCommonBus container;
    private final Set<Integer> markedSlots = new HashSet<>();
    private final Map<IGhostIngredientHandler.Target<?>, Object> jeiTargetMap = new HashMap<>();
    private boolean marking;

    public GuiCommonBus(InventoryPlayer inventory, PartCommonBus bus) {
        super(new ContainerCommonBus(inventory, bus));
        this.container = (ContainerCommonBus) this.inventorySlots;
        // Leave room for the four standard AE2 upgrade slots on the right,
        // just like GuiUpgradeable/GuiCommonInterface.
        this.xSize = 246;
        this.ySize = 251;
    }

    @Override
    public void drawBG(int offsetX, int offsetY, int mouseX, int mouseY) {
        this.mc.getTextureManager().bindTexture(BACKGROUND);
        this.drawTexturedModalRect(offsetX, offsetY, 0, 0, this.xSize, this.ySize);
        // storagebus.png contains the large 63-slot panel only.  Reuse the
        // four-slot upgrade strip from the common-interface material for the
        // additional card inventory.  The standalone upgrade_slot.png asset
        // is five slots and is intended for the crystal-growth chamber.
        this.mc.getTextureManager().bindTexture(UPGRADE_BACKGROUND);
        this.drawModalRectWithCustomSizedTexture(offsetX + 179, offsetY,
                179, 0, 32, 85, 256, 256);
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
        if (this.isConfigSlot(slot)) {
            java.util.ArrayList<String> tooltip = new java.util.ArrayList<>();
            ItemStack marker = slot.getStack();
            if (!marker.isEmpty() && ItemFluidMark.isFluidMark(marker)) {
                net.minecraftforge.fluids.FluidStack fluid = ItemFluidMark.getFluid(marker);
                tooltip.add(I18n.format("ae2_utilix.common_interface.marked_fluid", fluid.getLocalizedName()));
            } else if (!marker.isEmpty() && ItemFluidMark.isGasMark(marker)) {
                String gas = ItemFluidMark.getGasName(marker);
                String name = MekanismEnergisticsIntegration.getGasDisplayName(gas);
                tooltip.add(I18n.format("ae2_utilix.common_interface.marked_gas", name == null ? gas : name));
            } else if (!marker.isEmpty() && (ItemFluidMark.isManaMark(marker) || ItemFluidMark.isFeMark(marker))) {
                tooltip.add(I18n.format("ae2_utilix.common_interface.marked_item", marker.getDisplayName()));
            } else if (!marker.isEmpty() && ItemFluidMark.isEssentiaMark(marker)) {
                String aspect = ItemFluidMark.getAspectTag(marker);
                String name = com.ae2utilix.integration.ThaumicEnergisticsIntegration
                        .getAspectDisplayName(aspect);
                tooltip.add(I18n.format("ae2_utilix.common_interface.marked_essentia",
                        name == null ? aspect : name));
            } else if (!marker.isEmpty()) {
                tooltip.add(I18n.format("ae2_utilix.common_interface.marked_item", marker.getDisplayName()));
            }
            this.addHeldItemTooltip(tooltip);
            if (tooltip.isEmpty()) {
                super.renderHoveredToolTip(mouseX, mouseY);
                return;
            }
            this.drawHoveringText(tooltip, mouseX, mouseY);
            return;
        }
        super.renderHoveredToolTip(mouseX, mouseY);
    }

    private void addHeldItemTooltip(java.util.List<String> tooltip) {
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
                        String aspect = com.ae2utilix.integration.ThaumicEnergisticsIntegration
                                .getAspectTagFromItem(held);
                        if (aspect != null) {
                            String name = com.ae2utilix.integration.ThaumicEnergisticsIntegration
                                    .getAspectDisplayName(aspect);
                            tooltip.add(I18n.format("ae2_utilix.common_interface.left_click_item", held.getDisplayName()));
                            tooltip.add(I18n.format("ae2_utilix.common_interface.right_click_essentia",
                                    name == null ? aspect : name));
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
            }
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
        if (special == 0) {
            String aspect = com.ae2utilix.integration.ThaumicEnergisticsIntegration
                    .getAspectTagFromItem(held);
            if (aspect == null) return false;
            AE2Utilix.NETWORK.sendToServer(new PacketCommonBusMark(
                    this.container.getTilePosition(), side, slot.slotNumber, aspect, true));
            return true;
        }
        AE2Utilix.NETWORK.sendToServer(new PacketCommonBusMark(
                this.container.getTilePosition(), side, slot.slotNumber, special));
        return true;
    }

    @Override
    public List<IGhostIngredientHandler.Target<?>> getPhantomTargets(Object ingredient) {
        List<IGhostIngredientHandler.Target<?>> targets = new ArrayList<>();
        this.jeiTargetMap.clear();
        if (VirtualMarkJeiHelper.fromIngredient(ingredient) == null) return targets;

        for (Slot slot : this.inventorySlots.inventorySlots) {
            if (!this.isConfigSlot(slot)) continue;
            final Slot targetSlot = slot;
            IGhostIngredientHandler.Target<Object> target = new IGhostIngredientHandler.Target<Object>() {
                @Override
                public Rectangle getArea() {
                    return new Rectangle(getGuiLeft() + targetSlot.xPos,
                            getGuiTop() + targetSlot.yPos, 16, 16);
                }

                @Override
                public void accept(Object droppedIngredient) {
                    VirtualMarkJeiHelper.Mark mark =
                            VirtualMarkJeiHelper.fromIngredient(droppedIngredient);
                    if (mark == null) return;
                    if (mark.fluid != null) {
                        AE2Utilix.NETWORK.sendToServer(PacketCommonBusMark.forJeiFluid(
                                container.getTilePosition(), container.getSide(),
                                targetSlot.slotNumber, mark.fluid));
                    } else if (mark.gasName != null) {
                        AE2Utilix.NETWORK.sendToServer(PacketCommonBusMark.forJeiGas(
                                container.getTilePosition(), container.getSide(),
                                targetSlot.slotNumber, mark.gasName));
                    } else if (mark.aspectName != null) {
                        AE2Utilix.NETWORK.sendToServer(PacketCommonBusMark.forJeiEssentia(
                                container.getTilePosition(), container.getSide(),
                                targetSlot.slotNumber, mark.aspectName));
                    } else if (mark.specialType != 0) {
                        AE2Utilix.NETWORK.sendToServer(PacketCommonBusMark.forJeiSpecial(
                                container.getTilePosition(), container.getSide(),
                                targetSlot.slotNumber, mark.specialType));
                    } else if (mark.item != null && !mark.item.isEmpty()) {
                        AE2Utilix.NETWORK.sendToServer(PacketCommonBusMark.forJeiItem(
                                container.getTilePosition(), container.getSide(),
                                targetSlot.slotNumber, mark.item));
                    }
                }
            };
            targets.add(target);
            this.jeiTargetMap.put(target, targetSlot);
        }
        return targets;
    }

    @Override
    public Map<IGhostIngredientHandler.Target<?>, Object> getFakeSlotTargetMap() {
        return this.jeiTargetMap;
    }
}
