package com.ae2utilix.gui;

import appeng.api.config.ActionItems;
import appeng.api.config.Settings;
import appeng.api.storage.ITerminalHost;
import appeng.client.gui.implementations.GuiMEMonitorable;
import appeng.client.gui.widgets.GuiImgButton;
import appeng.client.gui.widgets.GuiTabButton;
import appeng.core.localization.GuiText;
import appeng.core.sync.GuiBridge;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.PacketInventoryAction;
import appeng.core.sync.packets.PacketSwitchGuis;
import appeng.helpers.InventoryAction;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.entity.player.InventoryPlayer;

import java.io.IOException;

public class GuiFullCrafting extends GuiMEMonitorable {

    private final ContainerFullCrafting container;
    private GuiImgButton clearBtn;
    private GuiTabButton craftingStatusBtn;

    public GuiFullCrafting(InventoryPlayer inventoryPlayer, ITerminalHost te) {
        super(inventoryPlayer, te, new ContainerFullCrafting(inventoryPlayer, te));
        this.container = (ContainerFullCrafting) this.inventorySlots;
        ((com.ae2utilix.mixin.MixinGuiMEMonitorableAccessor) this).ae2utilix$setReservedSpace(73);
    }

    @Override
    public void initGui() {
        super.initGui();
        this.buttonList.add(this.clearBtn = new GuiImgButton(this.guiLeft + 92, this.guiTop + this.ySize - 156, Settings.ACTIONS, ActionItems.STASH));
        this.clearBtn.setHalfSize(true);

        // Add crafting status button (normally only shown for IViewCellStorage)
        this.buttonList.add(this.craftingStatusBtn = new GuiTabButton(this.guiLeft + 170, this.guiTop - 4, 2 + 11 * 16, GuiText.CraftingStatus.getLocal(), this.itemRender));
        this.craftingStatusBtn.setHideEdge(13);
    }

    @Override
    public void drawFG(int offsetX, int offsetY, int mouseX, int mouseY) {
        super.drawFG(offsetX, offsetY, mouseX, mouseY);
        this.fontRenderer.drawString(GuiText.CraftingTerminal.getLocal(), 8, this.ySize - 96 + 1 - 73, 4210752);
    }

    @Override
    protected void actionPerformed(GuiButton btn) throws IOException {
        super.actionPerformed(btn);
        if (btn == this.clearBtn && this.container != null) {
            for (int i = this.container.inventorySlots.size() - 1; i >= 0; i--) {
                if (this.container.getSlot(i) instanceof appeng.container.slot.SlotCraftingMatrix) {
                    NetworkHandler.instance().sendToServer(new PacketInventoryAction(InventoryAction.MOVE_REGION, i, 0));
                    break;
                }
            }
        } else if (btn == this.craftingStatusBtn) {
            NetworkHandler.instance().sendToServer(new PacketSwitchGuis(GuiBridge.GUI_CRAFTING_STATUS));
        }
    }

    @Override
    protected String getBackground() {
        return "guis/crafting.png";
    }
}
