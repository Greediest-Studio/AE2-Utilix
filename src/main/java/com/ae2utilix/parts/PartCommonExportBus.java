package com.ae2utilix.parts;

import appeng.api.parts.IPartModel;
import appeng.parts.PartModel;
import com.ae2utilix.AE2Utilix;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

public class PartCommonExportBus extends PartCommonBus {
    public static final ResourceLocation MODEL_BASE = new ResourceLocation(AE2Utilix.MODID, "parts/common_export_bus_base");
    public static final IPartModel MODELS_OFF = new PartModel(MODEL_BASE, new ResourceLocation(AE2Utilix.MODID, "parts/common_export_bus_off"));
    public static final IPartModel MODELS_ON = new PartModel(MODEL_BASE, new ResourceLocation(AE2Utilix.MODID, "parts/common_export_bus_on"));
    public static final IPartModel MODELS_HAS_CHANNEL = new PartModel(MODEL_BASE, new ResourceLocation(AE2Utilix.MODID, "parts/common_export_bus_has_channel"));

    public PartCommonExportBus(ItemStack stack) {
        super(stack);
    }

    @Override protected boolean isExportBus() { return true; }
    @Override protected int getGuiBaseId() { return com.ae2utilix.gui.FullTerminalGuiHandler.GUI_COMMON_EXPORT_BUS; }
    @Override protected IPartModel getOffModel() { return MODELS_OFF; }
    @Override protected IPartModel getOnModel() { return MODELS_ON; }
    @Override protected IPartModel getHasChannelModel() { return MODELS_HAS_CHANNEL; }

    @Override
    public void getBoxes(appeng.api.parts.IPartCollisionHelper bch) {
        bch.addBox(4, 4, 12, 12, 12, 14);
        bch.addBox(5, 5, 14, 11, 11, 15);
        bch.addBox(6, 6, 15, 10, 10, 16);
        bch.addBox(6, 6, 11, 10, 10, 12);
    }

    @Override
    public ItemStack getItemStackRepresentation() {
        Item item = Item.getByNameOrId(AE2Utilix.MODID + ":common_export_bus");
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }
}
