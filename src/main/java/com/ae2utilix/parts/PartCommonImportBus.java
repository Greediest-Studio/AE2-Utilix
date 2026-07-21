package com.ae2utilix.parts;

import appeng.api.parts.IPartModel;
import appeng.parts.PartModel;
import com.ae2utilix.AE2Utilix;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

public class PartCommonImportBus extends PartCommonBus {
    public static final ResourceLocation MODEL_BASE = new ResourceLocation(AE2Utilix.MODID, "parts/common_import_bus_base");
    public static final IPartModel MODELS_OFF = new PartModel(MODEL_BASE, new ResourceLocation(AE2Utilix.MODID, "parts/common_import_bus_off"));
    public static final IPartModel MODELS_ON = new PartModel(MODEL_BASE, new ResourceLocation(AE2Utilix.MODID, "parts/common_import_bus_on"));
    public static final IPartModel MODELS_HAS_CHANNEL = new PartModel(MODEL_BASE, new ResourceLocation(AE2Utilix.MODID, "parts/common_import_bus_has_channel"));

    public PartCommonImportBus(ItemStack stack) {
        super(stack);
    }

    @Override protected boolean isExportBus() { return false; }
    @Override protected int getGuiBaseId() { return com.ae2utilix.gui.FullTerminalGuiHandler.GUI_COMMON_IMPORT_BUS; }
    @Override protected IPartModel getOffModel() { return MODELS_OFF; }
    @Override protected IPartModel getOnModel() { return MODELS_ON; }
    @Override protected IPartModel getHasChannelModel() { return MODELS_HAS_CHANNEL; }

    @Override
    public void getBoxes(appeng.api.parts.IPartCollisionHelper bch) {
        bch.addBox(6, 6, 11, 10, 10, 13);
        bch.addBox(5, 5, 13, 11, 11, 14);
        bch.addBox(4, 4, 14, 12, 12, 16);
    }

    @Override
    public ItemStack getItemStackRepresentation() {
        Item item = Item.getByNameOrId(AE2Utilix.MODID + ":common_import_bus");
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }
}
